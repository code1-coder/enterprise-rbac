package org.example.rbac.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * POST /api/auth/register 的入参，校验通过后由 AuthService 写入 sys_user。
 */
@Data
public class RegisterDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名最长 50 位")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(max = 72, message = "密码最长 72 位")
    private String password;

    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱最长 100 位")
    private String email;

    @Size(max = 20, message = "手机号最长 20 位")
    private String phone;
}
