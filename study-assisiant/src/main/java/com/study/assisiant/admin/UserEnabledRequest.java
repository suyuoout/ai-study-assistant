package com.study.assisiant.admin;

import jakarta.validation.constraints.NotNull;

public record UserEnabledRequest(
        @NotNull(message = "请提供启用状态")
        Boolean enabled
) {
}
