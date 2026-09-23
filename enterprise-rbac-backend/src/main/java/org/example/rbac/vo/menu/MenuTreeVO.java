package org.example.rbac.vo.menu;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单树节点，字段来自 sys_menu。children 按 parent_id 在内存中组装，不单独落库。
 */
@Data
public class MenuTreeVO {

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
    private List<MenuTreeVO> children = new ArrayList<>();
}
