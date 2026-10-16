package com.study.assisiant.chat;

import com.study.assisiant.ai.AIService;
import com.study.assisiant.ai.AiCallContext;
import com.study.assisiant.common.PageResult;
import com.study.assisiant.common.SseWriter;
import com.study.assisiant.mapper.ChatMessageMapper;
import com.study.assisiant.mapper.ChatSessionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChatService {

    private static final Logger log =
            LoggerFactory.getLogger(ChatService.class);

    /** 送给模型的历史消息条数上限 */
    private static final int HISTORY_LIMIT = 20;
    /** 会话标题长度上限 */
    private static final int TITLE_LIMIT = 20;

    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;
    private final AIService aiService;

    public ChatService(ChatSessionMapper sessionMapper,
                       ChatMessageMapper messageMapper,
                       AIService aiService) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.aiService = aiService;
    }

    /** 普通问答（一次性返回） */
    @Transactional
    public ChatMessage chat(Long userId, ChatRequest request,
                            AiCallContext ctx) {
        ChatSession session = getOrCreateSession(userId, request.sessionId());
        long existing = messageMapper.countBySession(session.getId());

        ChatMessage userMessage = new ChatMessage();
        userMessage.setSessionId(session.getId());
        userMessage.setRole("USER");
        userMessage.setContent(request.message().trim());
        messageMapper.insert(userMessage);
        sessionMapper.touch(session.getId());

        String reply = aiService.chat(
                recentMessages(session.getId()), ctx);

        ChatMessage assistantMessage = new ChatMessage();
        assistantMessage.setSessionId(session.getId());
        assistantMessage.setRole("ASSISTANT");
        assistantMessage.setContent(reply);
        messageMapper.insert(assistantMessage);

        if (existing == 0) {
            sessionMapper.updateTitle(session.getId(), userId,
                    titleOf(request.message()));
        }

        return assistantMessage;
    }

    /**
     * 流式问答：边生成边通过 SSE 推给前端，结束后落库。
     */
    public void streamChat(Long userId, ChatRequest request,
                           AiCallContext ctx, SseWriter sse) {

        ChatSession session = getOrCreateSession(userId, request.sessionId());
        long existing = messageMapper.countBySession(session.getId());

        sse.event("session", Map.of("sessionId", session.getId()));

        String question = request.message().trim();
        ChatMessage userMessage = new ChatMessage();
        userMessage.setSessionId(session.getId());
        userMessage.setRole("USER");
        userMessage.setContent(question);
        messageMapper.insert(userMessage);
        sessionMapper.touch(session.getId());

        try {
            String reply = aiService.chatStream(
                    recentMessages(session.getId()), ctx,
                    delta -> sse.event("delta", Map.of("text", delta)));

            ChatMessage assistantMessage = new ChatMessage();
            assistantMessage.setSessionId(session.getId());
            assistantMessage.setRole("ASSISTANT");
            assistantMessage.setContent(reply);
            messageMapper.insert(assistantMessage);

            if (existing == 0) {
                sessionMapper.updateTitle(session.getId(), userId,
                        titleOf(question));
            }

            sse.event("done", Map.of(
                    "sessionId", session.getId(),
                    "content", reply));
        } catch (SseWriter.SseClosedException e) {
            log.info("客户端在生成过程中断开 userId={} sessionId={}",
                    userId, session.getId());
        } catch (ResponseStatusException e) {
            sse.event("error", Map.of("message", String.valueOf(
                    e.getReason())));
        } catch (Exception e) {
            log.error("流式对话失败 userId={}", userId, e);
            sse.event("error", Map.of("message", "AI 生成失败，请稍后重试"));
        }
    }

    public List<ChatSession> sessions(Long userId) {
        return sessionMapper.findByUserId(userId);
    }

    /** 分页读取历史消息（page 从 1 开始，返回按时间正序） */
    public PageResult<ChatMessage> messages(Long userId, Long sessionId,
                                            int page, int size) {
        ChatSession session = requireOwnedSession(userId, sessionId);

        int pageNo = Math.max(1, page);
        int pageSize = Math.max(1, Math.min(size, 100));
        long total = messageMapper.countBySession(session.getId());
        int offset = Math.max(0, (int) total - pageNo * pageSize);

        List<ChatMessage> list = messageMapper.findPageDesc(
                session.getId(), offset, pageSize);
        return new PageResult<>(list, total, pageNo, pageSize);
    }

    public ChatSession rename(Long userId, Long sessionId, String title) {
        requireOwnedSession(userId, sessionId);
        sessionMapper.updateTitle(sessionId, userId,
                title.trim().length() > 50
                        ? title.trim().substring(0, 50) : title.trim());
        return sessionMapper.findByIdAndUser(sessionId, userId);
    }

    public void delete(Long userId, Long sessionId) {
        if (sessionMapper.delete(sessionId, userId) == 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "无权限删除该会话");
        }
    }

    private List<Map<String, String>> recentMessages(Long sessionId) {
        List<ChatMessage> history = messageMapper.findRecent(
                sessionId, HISTORY_LIMIT);
        List<Map<String, String>> messages = new ArrayList<>(history.size());
        for (ChatMessage message : history) {
            Map<String, String> item = new HashMap<>(2);
            item.put("role",
                    "USER".equals(message.getRole()) ? "user" : "assistant");
            item.put("content", message.getContent());
            messages.add(item);
        }
        return messages;
    }

    private String titleOf(String text) {
        String title = text.trim().replaceAll("\\s+", " ");
        return title.length() > TITLE_LIMIT
                ? title.substring(0, TITLE_LIMIT) : title;
    }

    private ChatSession getOrCreateSession(Long userId, Long sessionId) {
        if (sessionId != null) {
            ChatSession session = sessionMapper.findByIdAndUser(
                    sessionId, userId);
            if (session != null) {
                return session;
            }
        }

        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setChatType("GENERAL");
        session.setTitle("新对话");
        sessionMapper.insert(session);
        return session;
    }

    private ChatSession requireOwnedSession(Long userId, Long sessionId) {
        ChatSession session = sessionMapper.findByIdAndUser(
                sessionId, userId);
        if (session == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "无权限访问该会话");
        }
        return session;
    }
}
