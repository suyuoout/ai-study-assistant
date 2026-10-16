package com.study.assisiant.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RoleUpdateRequest(
        @NotBlank(message = "角色不能为空")
        @Pattern(regexp = "ADMIN|USER", message = "角色只能是 ADMIN 或 USER")
        String role
) {
}
