package org.example.rbac.dto.role;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * PUT /api/roles/{id}/permissions 的入参。menuIds 由 RoleService 重写到 sys_role_menu。
 */
@Data
public class AssignMenusDTO {

    @NotNull(message = "菜单列表不能为空")
    private List<Long> menuIds;
}
