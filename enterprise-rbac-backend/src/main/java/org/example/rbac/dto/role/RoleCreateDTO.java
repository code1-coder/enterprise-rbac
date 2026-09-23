package org.example.rbac.dto.role;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * POST /api/roles 的入参，写入 sys_role，此时还不包含菜单。
 */
@Data
public class RoleCreateDTO {

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 50, message = "角色名称最长 50 位")
    private String roleName;

    @NotBlank(message = "角色编码不能为空")
    @Size(max = 50, message = "角色编码最长 50 位")
    private String roleCode;

    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态只能是 0 或 1")
    @Max(value = 1, message = "状态只能是 0 或 1")
    private Integer status;

    @Min(value = 0, message = "排序号不能小于 0")
    private Integer sort;

    @Size(max = 500, message = "备注最长 500 位")
    private String remark;
}
