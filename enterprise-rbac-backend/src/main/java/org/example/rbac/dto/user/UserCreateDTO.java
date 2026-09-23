package org.example.rbac.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * POST /api/users 的入参。用户写入 sys_user，roleIds 同时写入 sys_user_role。
 */
@Data
public class UserCreateDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名最长 50 位")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(max = 72, message = "密码最长 72 位")
    private String password;

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

    private List<Long> roleIds;
}
