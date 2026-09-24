package org.example.rbac.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.example.rbac.common.exception.BusinessException;
import org.example.rbac.dto.menu.MenuCreateDTO;
import org.example.rbac.dto.menu.MenuUpdateDTO;
import org.example.rbac.entity.SysMenu;
import org.example.rbac.entity.SysRole;
import org.example.rbac.entity.SysRoleMenu;
import org.example.rbac.entity.SysUserRole;
import org.example.rbac.mapper.SysMenuMapper;
import org.example.rbac.mapper.SysRoleMapper;
import org.example.rbac.mapper.SysRoleMenuMapper;
import org.example.rbac.mapper.SysUserRoleMapper;
import org.example.rbac.security.RedisAuthStore;
import org.example.rbac.service.AuthLookupService;
import org.example.rbac.vo.menu.MenuTreeVO;
import org.example.rbac.vo.menu.MenuVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MenuServiceManagementTest {

    @BeforeAll
    static void initializeMybatisMetadata() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "test");
        for (Class<?> entity : List.of(SysMenu.class, SysRole.class, SysRoleMenu.class, SysUserRole.class)) {
            if (TableInfoHelper.getTableInfo(entity) == null) {
                TableInfoHelper.initTableInfo(assistant, entity);
            }
        }
    }

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
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

    @Test
    void treeBuildsNodesAndSortsSiblings() {
        SysMenu parent = menu(1L, 0L);
        SysMenu later = menu(2L, 1L);
        later.setSort(20);
        SysMenu earlier = menu(3L, 1L);
        earlier.setSort(10);
        when(menuMapper.selectList(any())).thenReturn(List.of(parent, later, earlier));

        List<MenuTreeVO> result = service().tree();

        assertEquals(1, result.size());
        assertEquals(List.of(3L, 2L), result.get(0).getChildren().stream().map(MenuTreeVO::getId).toList());
    }

    @Test
    void createMenuMapsFieldsAndAppliesDefaults() {
        MenuCreateDTO dto = new MenuCreateDTO();
        dto.setParentId(0L);
        dto.setMenuName("  菜单管理  ");
        dto.setMenuType("C");
        dto.setPath(" /system/menu ");
        dto.setRemark(" 菜单备注 ");

        service().createMenu(dto);

        ArgumentCaptor<SysMenu> menu = ArgumentCaptor.forClass(SysMenu.class);
        verify(menuMapper).insert(menu.capture());
        assertEquals(0L, menu.getValue().getParentId());
        assertEquals("菜单管理", menu.getValue().getMenuName());
        assertEquals("/system/menu", menu.getValue().getPath());
        assertEquals("菜单备注", menu.getValue().getRemark());
        assertEquals(0, menu.getValue().getSort());
        assertEquals(1, menu.getValue().getVisible());
        assertEquals(1, menu.getValue().getStatus());
    }

    @Test
    void getMenuByIdReturnsViewWithoutEntity() {
        SysMenu menu = menu(12L, 0L);
        menu.setMenuName("系统管理");
        menu.setMenuType("M");
        menu.setRemark("管理目录");
        when(menuMapper.selectById(12L)).thenReturn(menu);

        MenuVO result = service().getMenuById(12L);

        assertEquals(12L, result.getId());
        assertEquals("系统管理", result.getMenuName());
        assertEquals("管理目录", result.getRemark());
    }

    @Test
    void updateRejectsSelfAsParent() {
        when(menuMapper.selectById(12L)).thenReturn(menu(12L, 0L));
        MenuUpdateDTO dto = new MenuUpdateDTO();
        dto.setParentId(12L);
        dto.setMenuName("系统管理");
        dto.setMenuType("M");

        assertThrows(BusinessException.class, () -> service().updateMenu(12L, dto));
    }

    @Test
    void updatePreservesNullableValuesAndEvictsAssignedUserPermissions() {
        SysMenu existing = menu(12L, 0L);
        existing.setSort(7);
        existing.setVisible(0);
        existing.setStatus(0);
        existing.setPath("/old");
        existing.setRemark("旧备注");
        when(menuMapper.selectById(12L)).thenReturn(existing);
        SysRoleMenu roleMenu = new SysRoleMenu();
        roleMenu.setRoleId(3L);
        when(roleMenuMapper.selectList(any())).thenReturn(List.of(roleMenu));
        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(5L);
        when(userRoleMapper.selectList(any())).thenReturn(List.of(userRole));
        MenuUpdateDTO dto = new MenuUpdateDTO();
        dto.setParentId(0L);
        dto.setMenuName("系统管理");
        dto.setMenuType("M");
        dto.setRemark(" ");

        service().updateMenu(12L, dto);

        ArgumentCaptor<SysMenu> menu = ArgumentCaptor.forClass(SysMenu.class);
        verify(menuMapper).updateById(menu.capture());
        assertEquals(7, menu.getValue().getSort());
        assertEquals(0, menu.getValue().getVisible());
        assertEquals(0, menu.getValue().getStatus());
        assertNull(menu.getValue().getPath());
        assertNull(menu.getValue().getRemark());
        verify(redisAuthStore).evictPermissions(5L);
    }

    @Test
    void deleteRejectsMenusWithChildren() {
        when(menuMapper.selectById(12L)).thenReturn(menu(12L, 0L));
        when(menuMapper.selectCount(any())).thenReturn(1L);

        assertThrows(BusinessException.class, () -> service().deleteMenu(12L));
    }

    @Test
    void deleteRemovesAssignmentsAndEvictsAffectedUserPermissions() {
        when(menuMapper.selectById(12L)).thenReturn(menu(12L, 0L));
        when(menuMapper.selectCount(any())).thenReturn(0L);
        SysRoleMenu roleMenu = new SysRoleMenu();
        roleMenu.setRoleId(3L);
        when(roleMenuMapper.selectList(any())).thenReturn(List.of(roleMenu));
        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(5L);
        when(userRoleMapper.selectList(any())).thenReturn(List.of(userRole));

        TransactionSynchronizationManager.initSynchronization();
        service().deleteMenu(12L);

        verify(roleMenuMapper).delete(any());
        verify(menuMapper).deleteById(12L);
        verify(redisAuthStore, never()).evictPermissions(5L);
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(redisAuthStore).evictPermissions(5L);
    }

    private MenuServiceImpl service() {
        return new MenuServiceImpl(menuMapper, roleMenuMapper, userRoleMapper, roleMapper,
                authLookupService, redisAuthStore);
    }

    private static SysMenu menu(Long id, Long parentId) {
        SysMenu menu = new SysMenu();
        menu.setId(id);
        menu.setParentId(parentId);
        menu.setSort(0);
        return menu;
    }
}
