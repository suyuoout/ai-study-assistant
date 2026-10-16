package com.study.assisiant.task;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record StatusUpdateRequest(
        @NotBlank(message = "任务状态不能为空")
        @Pattern(regexp = "TODO|DOING|DONE",
                message = "无效的任务状态")
        String status,

        @Min(value = 0, message = "实际用时不能为负数")
        @Max(value = 1440, message = "实际用时最多 1440 分钟")
        Integer actualMinutes
) {
}
