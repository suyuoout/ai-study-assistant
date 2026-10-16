package com.study.assisiant.document;

import com.study.assisiant.ai.AiCallContext;
import com.study.assisiant.auth.JwtInterceptor;
import com.study.assisiant.common.Result;
import com.study.assisiant.common.SseWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.Map;

@RestController
@RequestMapping("/api/ai/rag")
public class RagController {

    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/chat")
    public Result<Map<String, Object>> ragChat(
            @Valid @RequestBody RagChatRequest request,
            HttpServletRequest httpRequest) {
        return Result.success(ragService.ragChat(userId(httpRequest),
                request, AiCallContext.of(httpRequest, "RAG")));
    }

    /** 流式版本：事件类型 session / sources / delta / done / error */
    @PostMapping(value = "/chat/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> stream(
            @Valid @RequestBody RagChatRequest request,
            HttpServletRequest httpRequest) {

        Long userId = userId(httpRequest);
        AiCallContext ctx = AiCallContext.of(httpRequest, "RAG");

        StreamingResponseBody body = outputStream ->
                ragService.streamRagChat(userId, request, ctx,
                        new SseWriter(outputStream));

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .header("Cache-Control", "no-cache")
                .header("X-Accel-Buffering", "no")
                .body(body);
    }

    private Long userId(HttpServletRequest request) {
        return (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID);
    }
}
