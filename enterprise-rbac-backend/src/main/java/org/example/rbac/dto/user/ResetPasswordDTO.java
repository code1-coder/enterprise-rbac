package org.example.rbac.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * PUT /api/users/{id}/reset-password 的入参。文档没有旧密码，正文只有 newPassword。
 */
@Data
public class ResetPasswordDTO {

    @NotBlank(message = "新密码不能为空")
    @Size(max = 72, message = "新密码最长 72 位")
    private String newPassword;
}
