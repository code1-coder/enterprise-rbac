package org.example.rbac.service.impl;

import org.example.rbac.entity.SysMenu;
import org.example.rbac.entity.SysRole;
import org.example.rbac.entity.SysRoleMenu;
import org.example.rbac.entity.SysUserRole;
import org.example.rbac.mapper.SysMenuMapper;
import org.example.rbac.mapper.SysRoleMapper;
import org.example.rbac.mapper.SysRoleMenuMapper;
import org.example.rbac.mapper.SysUserRoleMapper;
import org.example.rbac.security.AuthUser;
import org.example.rbac.security.RedisAuthStore;
import org.example.rbac.service.AuthLookupService;
import org.example.rbac.service.UserAccess;
import org.example.rbac.vo.menu.MenuTreeVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MenuServiceAuthzTest {

    @Mock
    private SysMenuMapper menuMapper;
    @Mock
    private SysRoleMenuMapper roleMenuMapper;
    @Mock
    private SysUserRoleMapper userRoleMapper;
    @Mock
    private SysRoleMapper roleMapper;
    @Mock
    private AuthLookupService authLookupService;
    @Mock
    private RedisAuthStore redisAuthStore;

    @AfterEach
    void clearUser() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void myMenusKeepsDirectoryAndMenuButDropsButtons() {
        loginAs(1L);
        SysUserRole link = new SysUserRole();
        link.setUserId(1L);
        link.setRoleId(1L);
        when(userRoleMapper.selectList(any())).thenReturn(List.of(link));
        SysRole role = new SysRole();
        role.setId(1L);
        role.setStatus(1);
        when(roleMapper.selectList(any())).thenReturn(List.of(role));
        when(roleMenuMapper.selectList(any())).thenReturn(List.of(link(1L), link(2L), link(3L)));
        when(menuMapper.selectList(any())).thenReturn(List.of(menu(2L, 1L, "用户管理", "C", 1),
                menu(3L, 2L, "用户新增", "F", 2),
                menu(1L, 0L, "系统管理", "M", 1)));

        List<MenuTreeVO> tree = service().myMenus();

        assertEquals(1, tree.size());
        assertEquals("系统管理", tree.get(0).getMenuName());
        assertEquals("M", tree.get(0).getMenuType());
        assertEquals(1, tree.get(0).getChildren().size());
        assertEquals("用户管理", tree.get(0).getChildren().get(0).getMenuName());
        assertTrue(tree.get(0).getChildren().get(0).getChildren().isEmpty());
    }

    @Test
    void orphanMenuBecomesRootWhenDirectoryWasNotAssigned() {
        SysMenu menu = menu(2L, 1L, "用户管理", "C", 2);
        SysMenu later = menu(4L, 1L, "角色管理", "C", 1);

        List<MenuTreeVO> tree = MenuTrees.build(List.of(menu, later));

        assertEquals(List.of(4L, 2L), tree.stream().map(MenuTreeVO::getId).toList());
        assertTrue(tree.stream().allMatch(node -> node.getChildren().isEmpty()));
    }

    @Test
    void myPermissionsUsesLoginAccessAndRefreshesCache() {
        loginAs(1L);
        List<String> permissions = List.of("system:user:list", "system:user:add");
        when(authLookupService.loadAccess(1L))
                .thenReturn(new UserAccess(List.of("ROLE_ADMIN"), List.of("超级管理员"), permissions));

        List<String> actual = service().myPermissions();

        assertEquals(permissions, actual);
        verify(redisAuthStore).cachePermissions(1L, permissions);
    }

    private MenuServiceImpl service() {
        return new MenuServiceImpl(menuMapper, roleMenuMapper, userRoleMapper, roleMapper,
                authLookupService, redisAuthStore);
    }

    private static SysRoleMenu link(Long menuId) {
        SysRoleMenu link = new SysRoleMenu();
        link.setRoleId(1L);
        link.setMenuId(menuId);
        return link;
    }

    private static SysMenu menu(Long id, Long parentId, String name, String type, int sort) {
        SysMenu menu = new SysMenu();
        menu.setId(id);
        menu.setParentId(parentId);
        menu.setMenuName(name);
        menu.setMenuType(type);
        menu.setSort(sort);
        menu.setStatus(1);
        return menu;
    }

    private static void loginAs(Long userId) {
        AuthUser authUser = new AuthUser(userId, "admin", "jti", Instant.now().plusSeconds(60));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(authUser, null));
    }
}
