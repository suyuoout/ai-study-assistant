package com.study.assisiant.plan;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record PlanGenerateRequest(
        @NotBlank(message = "学习目标不能为空")
        @Size(max = 200, message = "学习目标最长 200 个字符")
        String goal,

        @Size(max = 200, message = "当前水平最长 200 个字符")
        String currentLevel,

        @NotNull(message = "请填写开始日期")
        LocalDate startDate,

        @NotNull(message = "请填写截止日期")
        LocalDate endDate,

        @NotNull(message = "请填写每日可用时间")
        @Min(value = 10, message = "每日可用时间至少 10 分钟")
        @Max(value = 720, message = "每日可用时间最多 720 分钟")
        Integer dailyMinutes,

        List<String> availableDays
) {
}
