package com.study.assisiant.chat;

import com.study.assisiant.ai.AiCallContext;
import com.study.assisiant.auth.JwtInterceptor;
import com.study.assisiant.common.PageResult;
import com.study.assisiant.common.Result;
import com.study.assisiant.common.SseWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /** 普通问答（等 AI 生成完一次性返回） */
    @PostMapping("/ai/chat")
    public Result<Map<String, Object>> chat(
            @Valid @RequestBody ChatRequest request,
            HttpServletRequest httpRequest) {

        Long userId = userId(httpRequest);
        ChatMessage reply = chatService.chat(userId, request,
                AiCallContext.of(httpRequest, "CHAT"));

        return Result.success(Map.of(
                "sessionId", reply.getSessionId(),
                "reply", reply.getContent()
        ));
    }

    /**
     * 流式问答（SSE）。前端用 fetch 读取分片，
     * 事件类型：session / delta / done / error。
     */
    @PostMapping(value = "/ai/chat/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> stream(
            @Valid @RequestBody ChatRequest request,
            HttpServletRequest httpRequest) {

        Long userId = userId(httpRequest);
        AiCallContext ctx = AiCallContext.of(httpRequest, "CHAT");

        StreamingResponseBody body = outputStream ->
                chatService.streamChat(userId, request, ctx,
                        new SseWriter(outputStream));

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .header("Cache-Control", "no-cache")
                .header("X-Accel-Buffering", "no")
                .body(body);
    }

    @GetMapping("/chat/sessions")
    public Result<Object> sessions(HttpServletRequest request) {
        return Result.success(
                chatService.sessions(userId(request)));
    }

    @GetMapping("/chat/sessions/{id}/messages")
    public Result<PageResult<ChatMessage>> messages(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            HttpServletRequest request) {
        return Result.success(
                chatService.messages(userId(request), id, page, size));
    }

    @PutMapping("/chat/sessions/{id}")
    public Result<ChatSession> rename(
            @PathVariable Long id,
            @Valid @RequestBody SessionRenameRequest body,
            HttpServletRequest request) {
        return Result.success("修改成功",
                chatService.rename(userId(request), id, body.title()));
    }

    @DeleteMapping("/chat/sessions/{id}")
    public Result<Void> delete(
            @PathVariable Long id,
            HttpServletRequest request) {
        chatService.delete(userId(request), id);
        return Result.success("删除成功", null);
    }

    private Long userId(HttpServletRequest request) {
        return (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID);
    }
}
