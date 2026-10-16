package com.study.assisiant.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SessionRenameRequest(
        @NotBlank(message = "会话标题不能为空")
        @Size(max = 50, message = "会话标题最长 50 个字符")
        String title
) {
}
