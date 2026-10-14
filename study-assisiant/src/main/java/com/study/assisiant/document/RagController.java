package com.study.assisiant.document;

import com.study.assisiant.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class RagController {

    @Autowired
    private RagService ragService;

    @PostMapping("/rag/chat")
    public Result ragChat(
            @RequestBody RagChatRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return Result.success(ragService.ragChat(userId, request));
    }
}
