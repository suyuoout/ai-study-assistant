package com.study.assisiant.document;

import com.alibaba.fastjson2.JSONObject;
import com.study.assisiant.ai.AIService;
import com.study.assisiant.ai.AiCallContext;
import com.study.assisiant.chat.ChatMessage;
import com.study.assisiant.chat.ChatSession;
import com.study.assisiant.common.SseWriter;
import com.study.assisiant.mapper.ChatMessageMapper;
import com.study.assisiant.mapper.ChatSessionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于个人资料库的问答（RAG）。
 */
@Service
public class RagService {

    private static final Logger log =
            LoggerFactory.getLogger(RagService.class);

    private static final int TOP_K = 5;
    private static final int TITLE_LIMIT = 20;

    private static final String SYSTEM_PROMPT =
            "你是一名严谨的学习助手，只依据给定资料回答，不编造内容。";

    private static final String NO_HIT_ANSWER =
            "未在您的学习资料中检索到与问题相关的足够内容，无法依据资料回答。"
                    + "可以换一种说法提问，或先上传相关资料。";

    private final DocumentService documentService;
    private final AIService aiService;
    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;

    public RagService(DocumentService documentService,
                      AIService aiService,
                      ChatSessionMapper sessionMapper,
                      ChatMessageMapper messageMapper) {
        this.documentService = documentService;
        this.aiService = aiService;
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
    }

    @Transactional
    public Map<String, Object> ragChat(Long userId, RagChatRequest request,
                                      AiCallContext ctx) {
        String question = request.question().trim();

        List<Map<String, Object>> hits = documentService.retrieve(
                userId, question, request.documentIds(), TOP_K);
        List<Map<String, Object>> sources = toSources(hits);

        String answer = hits.isEmpty()
                ? NO_HIT_ANSWER
                : aiService.chatWithSystem(SYSTEM_PROMPT,
                buildPrompt(hits, question), ctx);

        ChatSession session = getOrCreateSession(
                userId, request.sessionId(), question);
        saveMessages(session.getId(), question, answer, sources);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sessionId", session.getId());
        data.put("answer", answer);
        data.put("sources", sources);
        return data;
    }

    /** 流式版本：先推来源，再逐段推答案 */
    public void streamRagChat(Long userId, RagChatRequest request,
                              AiCallContext ctx, SseWriter sse) {
        String question = request.question().trim();
        ChatSession session = getOrCreateSession(
                userId, request.sessionId(), question);

        sse.event("session", Map.of("sessionId", session.getId()));

        try {
            ChatMessage userMessage = new ChatMessage();
            userMessage.setSessionId(session.getId());
            userMessage.setRole("USER");
            userMessage.setContent(question);
            messageMapper.insert(userMessage);
            sessionMapper.touch(session.getId());

            List<Map<String, Object>> hits = documentService.retrieve(
                    userId, question, request.documentIds(), TOP_K);
            List<Map<String, Object>> sources = toSources(hits);
            sse.event("sources", Map.of("sources", sources));

            String answer;
            if (hits.isEmpty()) {
                answer = NO_HIT_ANSWER;
                sse.event("delta", Map.of("text", answer));
            } else {
                List<Map<String, String>> messages = new ArrayList<>(2);
                Map<String, String> system = new HashMap<>(2);
                system.put("role", "system");
                system.put("content", SYSTEM_PROMPT);
                messages.add(system);
                Map<String, String> user = new HashMap<>(2);
                user.put("role", "user");
                user.put("content", buildPrompt(hits, question));
                messages.add(user);

                answer = aiService.chatStream(messages, ctx,
                        delta -> sse.event("delta", Map.of("text", delta)));
            }

            ChatMessage assistantMessage = new ChatMessage();
            assistantMessage.setSessionId(session.getId());
            assistantMessage.setRole("ASSISTANT");
            assistantMessage.setContent(answer);
            assistantMessage.setSourcesJson(
                    JSONObject.toJSONString(sources));
            messageMapper.insert(assistantMessage);

            sse.event("done", Map.of(
                    "sessionId", session.getId(),
                    "answer", answer,
                    "sources", sources));
        } catch (SseWriter.SseClosedException e) {
            log.info("客户端中断 RAG 流式回答 userId={}", userId);
        } catch (Exception e) {
            log.error("RAG 流式问答失败 userId={}", userId, e);
            sse.event("error", Map.of("message", "回答生成失败，请稍后重试"));
        }
    }

    private void saveMessages(Long sessionId, String question, String answer,
                              List<Map<String, Object>> sources) {
        ChatMessage userMessage = new ChatMessage();
        userMessage.setSessionId(sessionId);
        userMessage.setRole("USER");
        userMessage.setContent(question);
        messageMapper.insert(userMessage);

        ChatMessage assistantMessage = new ChatMessage();
        assistantMessage.setSessionId(sessionId);
        assistantMessage.setRole("ASSISTANT");
        assistantMessage.setContent(answer);
        assistantMessage.setSourcesJson(JSONObject.toJSONString(sources));
        messageMapper.insert(assistantMessage);

        sessionMapper.touch(sessionId);
    }

    private List<Map<String, Object>> toSources(
            List<Map<String, Object>> hits) {
        List<Map<String, Object>> sources = new ArrayList<>(hits.size());
        int index = 1;
        for (Map<String, Object> hit : hits) {
            DocumentChunk chunk = (DocumentChunk) hit.get("chunk");
            Map<String, Object> source = new LinkedHashMap<>();
            source.put("index", index++);
            source.put("documentId", chunk.getDocumentId());
            source.put("documentTitle", hit.get("documentTitle"));
            source.put("pageNumber", chunk.getPageNumber());
            source.put("chunkId", chunk.getId());
            String content = chunk.getContent() == null
                    ? "" : chunk.getContent();
            source.put("preview", content.length() > 200
                    ? content.substring(0, 200) + "…" : content);
            source.put("score", hit.get("score"));
            sources.add(source);
        }
        return sources;
    }

    private String buildPrompt(List<Map<String, Object>> hits,
                               String question) {
        StringBuilder context = new StringBuilder();
        int index = 1;
        for (Map<String, Object> hit : hits) {
            DocumentChunk chunk = (DocumentChunk) hit.get("chunk");
            context.append("[").append(index++).append("] ")
                    .append("《").append(hit.get("documentTitle")).append("》")
                    .append(chunk.getPageNumber() == null ? ""
                            : " 第" + chunk.getPageNumber() + "页")
                    .append("\n").append(chunk.getContent()).append("\n\n");
        }

        return "请仅依据以下资料回答用户问题，"
                + "并在答案中用 [1] [2] 等编号标注引用来源；"
                + "资料不足以回答时请明确说明。\n\n"
                + "资料：\n" + context + "\n"
                + "用户问题：" + question;
    }

    private ChatSession getOrCreateSession(Long userId, Long sessionId,
                                           String question) {
        if (sessionId != null) {
            ChatSession session = sessionMapper.findByIdAndUser(
                    sessionId, userId);
            if (session != null) {
                return session;
            }
        }

        String title = question.trim().replaceAll("\\s+", " ");
        if (title.length() > TITLE_LIMIT) {
            title = title.substring(0, TITLE_LIMIT);
        }

        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setChatType("RAG");
        session.setTitle(title);
        sessionMapper.insert(session);
        return session;
    }
}
