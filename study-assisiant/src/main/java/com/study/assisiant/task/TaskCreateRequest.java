package com.study.assisiant.task;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record TaskCreateRequest(
        @NotNull(message = "请选择所属学习计划")
        Long planId,

        @NotBlank(message = "任务标题不能为空")
        @Size(max = 200, message = "任务标题最长 200 个字符")
        String title,

        @Size(max = 1000, message = "任务说明最长 1000 个字符")
        String description,

        /** 为空时默认今天 */
        LocalDate taskDate,

        @Min(value = 5, message = "预计用时至少 5 分钟")
        @Max(value = 720, message = "预计用时最多 720 分钟")
        Integer estimatedMinutes
) {
}
