package org.example.rbac.dto.menu;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * PUT /api/menus/{id} 的入参，按字段更新已有的 sys_menu。
 */
@Data
public class MenuUpdateDTO {

    @NotNull(message = "父菜单不能为空")
    private Long parentId;

    @NotBlank(message = "菜单名称不能为空")
    @Size(max = 50, message = "菜单名称最长 50 位")
    private String menuName;

    @NotBlank(message = "菜单类型不能为空")
    @Pattern(regexp = "[MCFA]", message = "菜单类型只能是 M、C、F、A")
    private String menuType;

    @Size(max = 200, message = "路由路径最长 200 位")
    private String path;

    @Size(max = 255, message = "组件路径最长 255 位")
    private String component;

    @Size(max = 100, message = "权限标识最长 100 位")
    private String permission;

    @Size(max = 100, message = "图标最长 100 位")
    private String icon;

    @Min(value = 0, message = "排序号不能小于 0")
    private Integer sort;

    @Min(value = 0, message = "可见状态只能是 0 或 1")
    @Max(value = 1, message = "可见状态只能是 0 或 1")
    private Integer visible;

    @Min(value = 0, message = "状态只能是 0 或 1")
    @Max(value = 1, message = "状态只能是 0 或 1")
    private Integer status;
}
