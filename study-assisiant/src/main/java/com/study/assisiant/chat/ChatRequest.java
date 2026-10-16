package com.study.assisiant.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        Long sessionId,

        @NotBlank(message = "消息不能为空")
        @Size(max = 4000, message = "单条消息最长 4000 个字符")
        String message
) {
}
