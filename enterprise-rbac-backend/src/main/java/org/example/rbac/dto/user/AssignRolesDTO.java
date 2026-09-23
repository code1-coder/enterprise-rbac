package org.example.rbac.dto.user;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * PUT /api/users/{id}/roles 的入参。roleIds 由 UserService 重写到 sys_user_role。
 */
@Data
public class AssignRolesDTO {

    @NotNull(message = "角色列表不能为空")
    private List<Long> roleIds;
}
