package org.example.rbac.vo.menu;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 菜单详情出参，对应 sys_menu 的一行，不带子节点。
 */
@Data
public class MenuVO {

    private Long id;
    private Long parentId;
    private String menuName;
    private String menuType;
    private String path;
    private String component;
    private String permission;
    private String icon;
    private Integer sort;
    private Integer visible;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
}
