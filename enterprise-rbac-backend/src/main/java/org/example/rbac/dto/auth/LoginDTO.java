package org.example.rbac.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * POST /api/auth/login 的入参，对应 sys_user 的用户名和密码。
 */
@Data
public class LoginDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名最长 50 位")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(max = 72, message = "密码最长 72 位")
    private String password;
}
