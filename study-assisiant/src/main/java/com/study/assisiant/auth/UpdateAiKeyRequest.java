package com.study.assisiant.auth;

import jakarta.validation.constraints.Size;

/**
 * 保存或清除个人 AI Key：传空字符串表示清除，恢复使用系统默认 Key。
 */
public record UpdateAiKeyRequest(
        @Size(max = 200, message = "API Key 过长")
        String aiKey
) {
}
