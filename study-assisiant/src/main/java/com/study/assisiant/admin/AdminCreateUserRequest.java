package com.study.assisiant.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 管理员在后台创建账号（注册关闭时给同学开号用）。
 */
public record AdminCreateUserRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 50, message = "用户名长度需在 3-50 位之间")
        @Pattern(regexp = "^[A-Za-z0-9_]+$",
                message = "用户名只能包含字母、数字和下划线")
        String username,

        @NotBlank(message = "初始密码不能为空")
        @Size(min = 8, max = 64, message = "初始密码至少需要8位")
        String password,

        @Size(max = 50, message = "昵称最长 50 个字符")
        String nickname,

        @Email(message = "邮箱格式不正确")
        @Size(max = 100, message = "邮箱最长 100 个字符")
        String email,

        @Pattern(regexp = "ADMIN|USER", message = "角色只能是 ADMIN 或 USER")
        String role
) {
}
