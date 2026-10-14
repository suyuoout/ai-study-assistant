package com.study.assisiant.document;

import com.alibaba.fastjson2.JSONObject;
import com.study.assisiant.ai.AIService;
import com.study.assisiant.chat.ChatMessage;
import com.study.assisiant.chat.ChatSession;
import com.study.assisiant.mapper.ChatMessageMapper;
import com.study.assisiant.mapper.ChatSessionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RagService {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private AIService aiService;

    @Autowired
    private ChatSessionMapper sessionMapper;

    @Autowired
    private ChatMessageMapper messageMapper;

    public Map<String, Object> ragChat(Long userId, RagChatRequest request) {
        String question = request.getQuestion();
        if (question == null || question.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "问题不能为空");
        }

        List<Map<String, Object>> hits = documentService.retrieve(
                userId, question, request.getDocumentIds(), 5);

        String answer;
        List<Map<String, Object>> sources = new ArrayList<>();

        if (hits.isEmpty()) {
            answer = "未在您的学习资料中检索到与问题相关的足够内容，无法依据资料回答。";
        } else {
            StringBuilder context = new StringBuilder();
            int index = 1;
            for (Map<String, Object> hit : hits) {
                DocumentChunk chunk = (DocumentChunk) hit.get("chunk");
                context.append("[").append(index).append("] ")
                        .append(chunk.getContent()).append("\n");

                Map<String, Object> source = new LinkedHashMap<>();
                source.put("documentId", chunk.getDocumentId());
                source.put("documentTitle", hit.get("documentTitle"));
                source.put("pageNumber", chunk.getPageNumber());
                source.put("chunkId", chunk.getId());
                sources.add(source);
                index++;
            }

            String prompt = "请仅依据以下资料回答用户问题，"
                    + "并在答案中用 [1] [2] 等编号标注引用来源；"
                    + "资料不足以回答时请明确说明。\n\n"
                    + "资料：\n" + context + "\n"
                    + "用户问题：" + question;

            answer = aiService.chatWithSystem(
                    "你是一名严谨的学习助手，只依据给定资料回答，不编造内容。",
                    prompt);
        }

        ChatSession session = getOrCreateSession(
                userId, request.getSessionId());

        ChatMessage userMessage = new ChatMessage();
        userMessage.setSessionId(session.getId());
        userMessage.setRole("USER");
        userMessage.setContent(question);
        messageMapper.insert(userMessage);

        ChatMessage assistantMessage = new ChatMessage();
        assistantMessage.setSessionId(session.getId());
        assistantMessage.setRole("ASSISTANT");
        assistantMessage.setContent(answer);
        assistantMessage.setSourcesJson(JSONObject.toJSONString(sources));
        messageMapper.insert(assistantMessage);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sessionId", session.getId());
        data.put("answer", answer);
        data.put("sources", sources);
        return data;
    }

    private ChatSession getOrCreateSession(Long userId, Long sessionId) {
        if (sessionId != null) {
            ChatSession session = sessionMapper.findById(sessionId);
            if (session != null && session.getUserId().equals(userId)) {
                return session;
            }
        }

        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setChatType("RAG");
        session.setTitle("知识库问答");
        sessionMapper.insert(session);
        return session;
    }
}
