package org.example.rbac.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.example.rbac.common.exception.BusinessException;
import org.example.rbac.dto.role.RoleCreateDTO;
import org.example.rbac.dto.role.RoleUpdateDTO;
import org.example.rbac.entity.SysMenu;
import org.example.rbac.entity.SysRole;
import org.example.rbac.entity.SysRoleMenu;
import org.example.rbac.entity.SysUserRole;
import org.example.rbac.mapper.SysMenuMapper;
import org.example.rbac.mapper.SysRoleMapper;
import org.example.rbac.mapper.SysRoleMenuMapper;
import org.example.rbac.mapper.SysUserRoleMapper;
import org.example.rbac.security.RedisAuthStore;
import org.example.rbac.service.RoleService;
import org.example.rbac.vo.role.RoleVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleManagementServiceTest {

    @BeforeAll
    static void initializeMybatisMetadata() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "test");
        for (Class<?> entity : List.of(SysRole.class, SysMenu.class, SysRoleMenu.class, SysUserRole.class)) {
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
    private SysRoleMapper roleMapper;
    @Mock
    private SysRoleMenuMapper roleMenuMapper;
    @Mock
    private SysUserRoleMapper userRoleMapper;
    @Mock
    private SysMenuMapper menuMapper;
    @Mock
    private RedisAuthStore redisAuthStore;

    @Test
    void createRoleTrimsFieldsAndUsesDefaultSort() {
        when(roleMapper.selectCount(any())).thenReturn(0L);

        service().createRole(createDTO());

        ArgumentCaptor<SysRole> roleCaptor = ArgumentCaptor.forClass(SysRole.class);
        verify(roleMapper).insert(roleCaptor.capture());
        assertEquals("部门经理", roleCaptor.getValue().getRoleName());
        assertEquals("ROLE_MANAGER", roleCaptor.getValue().getRoleCode());
        assertEquals(1, roleCaptor.getValue().getStatus());
        assertEquals(0, roleCaptor.getValue().getSort());
        assertEquals("经理角色", roleCaptor.getValue().getRemark());
    }

    @Test
    void createRoleRejectsDuplicateNameOrCode() {
        when(roleMapper.selectCount(any())).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service().createRole(createDTO()));

        assertEquals(400, exception.getCode());
        assertEquals("角色名称或编码已存在", exception.getMessage());
        verify(roleMapper, never()).insert(any(SysRole.class));
    }

    @Test
    void pageListMapsRecordsAndPagination() {
        SysRole role = role(2L, "部门经理", "ROLE_MANAGER", 1);
        Page<SysRole> page = new Page<>(2, 10, 21);
        page.setRecords(List.of(role));
        when(roleMapper.selectPage(any(), any())).thenReturn(page);

        var result = service().pageList(new org.example.rbac.dto.role.RoleQueryDTO());

        assertEquals(21, result.getTotal());
        assertEquals(2, result.getCurrent());
        assertEquals("ROLE_MANAGER", result.getRecords().get(0).getRoleCode());
    }

    @Test
    void updateRoleEvictsUsersWhenStatusChanges() {
        when(roleMapper.selectOne(any())).thenReturn(role(7L, "经理", "ROLE_MANAGER", 1));
        when(roleMapper.selectCount(any())).thenReturn(0L);
        when(userRoleMapper.selectList(any())).thenReturn(List.of(userRole(9L, 7L)));
        RoleUpdateDTO dto = new RoleUpdateDTO();
        dto.setRoleName("部门经理");
        dto.setRoleCode("ROLE_MANAGER");
        dto.setStatus(0);
        dto.setRemark("   ");

        service().updateRole(7L, dto);

        ArgumentCaptor<SysRole> roleCaptor = ArgumentCaptor.forClass(SysRole.class);
        verify(roleMapper).updateById(roleCaptor.capture());
        assertEquals(0, roleCaptor.getValue().getStatus());
        assertEquals(0, roleCaptor.getValue().getSort());
        assertEquals(null, roleCaptor.getValue().getRemark());
        verify(redisAuthStore).evictPermissions(9L);
    }

    @Test
    void assignMenusDeduplicatesAndEvictsOnlyAfterCommit() {
        when(roleMapper.selectOne(any())).thenReturn(role(7L, "经理", "ROLE_MANAGER", 1));
        when(menuMapper.selectCount(any())).thenReturn(2L);
        when(userRoleMapper.selectList(any())).thenReturn(List.of(userRole(9L, 7L)));
        TransactionSynchronizationManager.initSynchronization();

        service().assignMenus(7L, List.of(3L, 4L, 3L));

        verify(roleMenuMapper).delete(any());
        ArgumentCaptor<SysRoleMenu> menuCaptor = ArgumentCaptor.forClass(SysRoleMenu.class);
        verify(roleMenuMapper, times(2)).insert(menuCaptor.capture());
        assertEquals(List.of(3L, 4L), menuCaptor.getAllValues().stream().map(SysRoleMenu::getMenuId).toList());
        verify(redisAuthStore, never()).evictPermissions(9L);
        TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCommit());
        verify(redisAuthStore).evictPermissions(9L);
    }

    @Test
    void assignMenusRejectsUnknownMenuBeforeReplacingAssignments() {
        when(roleMapper.selectOne(any())).thenReturn(role(7L, "经理", "ROLE_MANAGER", 1));
        when(menuMapper.selectCount(any())).thenReturn(0L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service().assignMenus(7L, List.of(999L)));

        assertEquals("菜单不存在或已删除", exception.getMessage());
        verify(roleMenuMapper, never()).delete(any());
    }

    @Test
    void deleteRoleClearsRelationsAndEvictsAssignedUsers() {
        when(roleMapper.selectOne(any())).thenReturn(role(7L, "经理", "ROLE_MANAGER", 1));
        when(userRoleMapper.selectList(any())).thenReturn(List.of(userRole(9L, 7L)));

        service().deleteRole(7L);

        verify(roleMenuMapper).delete(any());
        verify(userRoleMapper).delete(any());
        verify(roleMapper).deleteById(7L);
        verify(redisAuthStore).evictPermissions(9L);
    }

    private RoleService service() {
        return new RoleServiceImpl(roleMapper, roleMenuMapper, userRoleMapper, menuMapper, redisAuthStore);
    }

    private static RoleCreateDTO createDTO() {
        RoleCreateDTO dto = new RoleCreateDTO();
        dto.setRoleName(" 部门经理 ");
        dto.setRoleCode(" ROLE_MANAGER ");
        dto.setStatus(1);
        dto.setRemark(" 经理角色 ");
        return dto;
    }

    private static SysRole role(Long id, String name, String code, int status) {
        SysRole role = new SysRole();
        role.setId(id);
        role.setRoleName(name);
        role.setRoleCode(code);
        role.setStatus(status);
        role.setSort(0);
        return role;
    }

    private static SysUserRole userRole(Long userId, Long roleId) {
        SysUserRole link = new SysUserRole();
        link.setUserId(userId);
        link.setRoleId(roleId);
        return link;
    }
}
