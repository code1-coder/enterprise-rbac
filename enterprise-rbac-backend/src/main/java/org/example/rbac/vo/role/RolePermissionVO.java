package org.example.rbac.vo.role;

import lombok.Data;

import java.util.List;

/**
 * GET /api/roles/{id}/permissions 的出参，只返回该角色在 sys_role_menu 中的 menuIds。
 */
@Data
public class RolePermissionVO {

    private List<Long> menuIds;
}
