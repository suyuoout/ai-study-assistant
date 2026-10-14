package com.study.assisiant.chat;

import com.study.assisiant.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @PostMapping("/ai/chat")
    public Result chat(
            @RequestBody ChatRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            return Result.error(400, "消息不能为空");
        }

        ChatMessage reply = chatService.chat(userId, request);
        return Result.success(Map.of(
                "sessionId", reply.getSessionId(),
                "reply", reply.getContent()
        ));
    }

    @GetMapping("/chat/sessions")
    public Result sessions(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(chatService.sessions(userId));
    }

    @GetMapping("/chat/sessions/{id}/messages")
    public Result messages(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(chatService.messages(userId, id));
    }

    @DeleteMapping("/chat/sessions/{id}")
    public Result delete(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        Long userId = (Long) request.getAttribute("userId");
        boolean success = chatService.delete(userId, id);
        if (!success) {
            return Result.error(403, "无权限删除该会话");
        }
        return Result.success(null);
    }
}
