package org.example.rbac.service.impl;

import org.example.rbac.entity.SysMenu;
import org.example.rbac.vo.menu.MenuTreeVO;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 把 sys_menu 行组装成 MenuTreeVO。parent_id 为 0 或父节点不在本次结果里时，该节点作为根。
 * 这样只分到子菜单、没有分到目录的用户仍能看到菜单。children 按 sort、id 排序，不写回数据库。
 */
final class MenuTrees {

    private MenuTrees() {
    }

    static List<MenuTreeVO> build(List<SysMenu> menus) {
        Map<Long, MenuTreeVO> nodes = new LinkedHashMap<>();
        for (SysMenu menu : menus) {
            if (menu == null || menu.getId() == null) {
                continue;
            }
            nodes.put(menu.getId(), toNode(menu));
        }
        List<MenuTreeVO> roots = new ArrayList<>();
        for (MenuTreeVO node : nodes.values()) {
            Long parentId = node.getParentId();
            MenuTreeVO parent = parentId == null ? null : nodes.get(parentId);
            if (parentId == null || parentId == 0L || parent == null || parent == node) {
                roots.add(node);
            } else {
                parent.getChildren().add(node);
            }
        }
        sortNodes(roots);
        return roots;
    }

    private static MenuTreeVO toNode(SysMenu menu) {
        MenuTreeVO node = new MenuTreeVO();
        node.setId(menu.getId());
        node.setParentId(menu.getParentId());
        node.setMenuName(menu.getMenuName());
        node.setMenuType(menu.getMenuType());
        node.setPath(menu.getPath());
        node.setComponent(menu.getComponent());
        node.setPermission(menu.getPermission());
        node.setIcon(menu.getIcon());
        node.setSort(menu.getSort());
        node.setVisible(menu.getVisible());
        node.setStatus(menu.getStatus());
        return node;
    }

    private static void sortNodes(List<MenuTreeVO> nodes) {
        nodes.sort(Comparator
                .comparingInt((MenuTreeVO node) -> node.getSort() == null ? 0 : node.getSort())
                .thenComparingLong(node -> node.getId() == null ? 0L : node.getId()));
        for (MenuTreeVO node : nodes) {
            sortNodes(node.getChildren());
        }
    }
}
