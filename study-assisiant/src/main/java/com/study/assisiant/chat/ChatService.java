package com.study.assisiant.chat;

import com.study.assisiant.ai.AIService;
import com.study.assisiant.mapper.ChatMessageMapper;
import com.study.assisiant.mapper.ChatSessionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChatService {

    private static final int HISTORY_LIMIT = 20;

    @Autowired
    private ChatSessionMapper sessionMapper;

    @Autowired
    private ChatMessageMapper messageMapper;

    @Autowired
    private AIService aiService;

    public ChatMessage chat(Long userId, ChatRequest request) {
        ChatSession session = getOrCreateSession(userId, request.getSessionId());

        ChatMessage userMessage = new ChatMessage();
        userMessage.setSessionId(session.getId());
        userMessage.setRole("USER");
        userMessage.setContent(request.getMessage());
        messageMapper.insert(userMessage);

        List<ChatMessage> history = messageMapper.findBySessionId(session.getId());

        List<Map<String, String>> aiMessages = new ArrayList<>();
        for (ChatMessage m : history) {
            Map<String, String> item = new HashMap<>();
            item.put("role", "USER".equals(m.getRole()) ? "user" : "assistant");
            item.put("content", m.getContent());
            aiMessages.add(item);
        }
        if (aiMessages.size() > HISTORY_LIMIT) {
            aiMessages = aiMessages.subList(
                    aiMessages.size() - HISTORY_LIMIT, aiMessages.size());
        }

        String reply = aiService.chat(aiMessages);

        ChatMessage assistantMessage = new ChatMessage();
        assistantMessage.setSessionId(session.getId());
        assistantMessage.setRole("ASSISTANT");
        assistantMessage.setContent(reply);
        messageMapper.insert(assistantMessage);

        // 首次对话时用第一句作为会话标题
        if (history.size() == 1) {
            String title = request.getMessage().trim();
            if (title.length() > 20) {
                title = title.substring(0, 20);
            }
            sessionMapper.updateTitle(session.getId(), title);
        }

        return assistantMessage;
    }

    public List<ChatSession> sessions(Long userId) {
        return sessionMapper.findByUserId(userId);
    }

    public List<ChatMessage> messages(Long userId, Long sessionId) {
        ChatSession session = requireOwnedSession(userId, sessionId);
        return messageMapper.findBySessionId(session.getId());
    }

    public boolean delete(Long userId, Long sessionId) {
        return sessionMapper.delete(sessionId, userId) > 0;
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
        session.setChatType("GENERAL");
        session.setTitle("新对话");
        sessionMapper.insert(session);
        return session;
    }

    private ChatSession requireOwnedSession(Long userId, Long sessionId) {
        ChatSession session = sessionMapper.findById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "无权限访问该会话");
        }
        return session;
    }
}
