package com.study.assisiant.plan;

import jakarta.validation.constraints.Size;

public record PlanUpdateRequest(
        @Size(max = 200, message = "计划标题最长 200 个字符")
        String title,

        String status
) {
}
