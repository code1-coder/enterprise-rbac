package org.example.rbac.dto.role;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * GET /api/roles 的查询条件，用于分页读取 sys_role。
 */
@Data
public class RoleQueryDTO {

    @Min(value = 1, message = "页码必须大于 0")
    private Integer page = 1;

    @Min(value = 1, message = "每页数量必须大于 0")
    @Max(value = 100, message = "每页数量不能超过 100")
    private Integer size = 10;

    @Size(max = 50, message = "角色名称最长 50 位")
    private String roleName;
}
