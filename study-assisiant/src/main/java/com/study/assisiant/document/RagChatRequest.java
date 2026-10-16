package com.study.assisiant.document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RagChatRequest(
        Long sessionId,

        @NotBlank(message = "问题不能为空")
        @Size(max = 1000, message = "问题最长 1000 个字符")
        String question,

        /** 只在指定的资料里检索；为空表示全部资料 */
        List<Long> documentIds
) {
}
