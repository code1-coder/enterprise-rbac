package org.example.rbac.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * PUT /api/users/{id} 的入参，只改资料，不改密码，也不改 sys_user_role。
 */
@Data
public class UserUpdateDTO {

    @Size(max = 50, message = "昵称最长 50 位")
    private String nickname;

    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱最长 100 位")
    private String email;

    @Size(max = 20, message = "手机号最长 20 位")
    private String phone;

    @Min(value = 0, message = "状态只能是 0 或 1")
    @Max(value = 1, message = "状态只能是 0 或 1")
    private Integer status;
}
