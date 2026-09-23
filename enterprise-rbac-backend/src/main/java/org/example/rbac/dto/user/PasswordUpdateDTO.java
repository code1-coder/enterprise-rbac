package org.example.rbac.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * PUT /api/users/{id}/password 的入参，只更新 sys_user.password；接口无权限串，服务层限制为本人。
 */
@Data
public class PasswordUpdateDTO {

    @NotBlank(message = "原密码不能为空")
    @Size(max = 72, message = "原密码最长 72 位")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(max = 72, message = "新密码最长 72 位")
    private String newPassword;
}
