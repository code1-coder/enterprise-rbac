package org.example.rbac.service;

import org.example.rbac.dto.menu.MenuCreateDTO;
import org.example.rbac.dto.menu.MenuUpdateDTO;
import org.example.rbac.vo.menu.MenuTreeVO;
import org.example.rbac.vo.menu.MenuVO;

import java.util.List;

/**
 * 菜单维护，以及当前用户可见菜单和按钮权限。
 * 全量树和详情只读 sys_menu；我的菜单、我的权限还要经 sys_user_role、sys_role_menu 过滤到当前用户。
 * 返回的 permission 就是 Controller 上 hasAuthority 使用的字符串。
 */
public interface MenuService {

    List<MenuTreeVO> tree();

    List<MenuTreeVO> myMenus();

    List<String> myPermissions();

    MenuVO getMenuById(Long id);

    void createMenu(MenuCreateDTO dto);

    void updateMenu(Long id, MenuUpdateDTO dto);

    void deleteMenu(Long id);
}
