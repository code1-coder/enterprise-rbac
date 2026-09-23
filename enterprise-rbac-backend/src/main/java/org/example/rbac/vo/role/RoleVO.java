package org.example.rbac.vo.role;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色出参，对应 sys_role。菜单明细不在这里，分配结果见 RolePermissionVO。
 */
@Data
public class RoleVO {

    private Long id;
    private String roleName;
    private String roleCode;
    private Integer status;
    private Integer sort;
    private String remark;
    private LocalDateTime createTime;
}
