package org.example.rbac.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.example.rbac.common.exception.BusinessException;
import org.example.rbac.common.result.PageResult;
import org.example.rbac.dto.role.RoleCreateDTO;
import org.example.rbac.dto.role.RoleQueryDTO;
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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * 角色用例的三张表：sys_role 是主表，sys_role_menu 保存分配的菜单，sys_user_role 表示用户仍占用该角色。
 * 删除时三者一起看；分配菜单和查询已分配菜单只需要角色表与角色菜单表。
 */
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private static final int DEFAULT_SORT = 0;

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysMenuMapper menuMapper;
    private final RedisAuthStore redisAuthStore;

    @Override
    @Transactional(readOnly = true)
    public PageResult<RoleVO> pageList(RoleQueryDTO query) {
        LambdaQueryWrapper<SysRole> wrapper = roleViewQuery();
        if (query.getRoleName() != null && !query.getRoleName().isBlank()) {
            wrapper.like(SysRole::getRoleName, query.getRoleName().trim());
        }
        Page<SysRole> page = roleMapper.selectPage(new Page<>(query.getPage(), query.getSize()),
                wrapper.orderByAsc(SysRole::getSort)
                        .orderByAsc(SysRole::getId));
        PageResult<RoleVO> result = new PageResult<>();
        result.setRecords(page.getRecords().stream().map(RoleServiceImpl::toVO).toList());
        result.setTotal(page.getTotal());
        result.setSize(page.getSize());
        result.setCurrent(page.getCurrent());
        result.setPages(page.getPages());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleVO> listAll() {
        return roleMapper.selectList(roleViewQuery()
                        .orderByAsc(SysRole::getSort)
                        .orderByAsc(SysRole::getId))
                .stream().map(RoleServiceImpl::toVO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RoleVO getRoleById(Long id) {
        return toVO(requireRole(id));
    }

    @Override
    public void createRole(RoleCreateDTO dto) {
        String roleName = dto.getRoleName().trim();
        String roleCode = dto.getRoleCode().trim();
        ensureUniqueRole(roleName, roleCode, null);
        SysRole role = new SysRole();
        role.setRoleName(roleName);
        role.setRoleCode(roleCode);
        role.setStatus(dto.getStatus());
        role.setSort(dto.getSort() == null ? DEFAULT_SORT : dto.getSort());
        role.setRemark(blankToNull(dto.getRemark()));
        try {
            roleMapper.insert(role);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(400, "角色名称或编码已存在");
        }
    }

    @Override
    @Transactional
    public void updateRole(Long id, RoleUpdateDTO dto) {
        SysRole role = requireRole(id);
        String roleName = dto.getRoleName().trim();
        String roleCode = dto.getRoleCode().trim();
        ensureUniqueRole(roleName, roleCode, id);
        boolean statusChanged = !Objects.equals(role.getStatus(), dto.getStatus());
        role.setRoleName(roleName);
        role.setRoleCode(roleCode);
        role.setStatus(dto.getStatus());
        if (dto.getSort() != null) {
            role.setSort(dto.getSort());
        }
        if (dto.getRemark() != null) {
            role.setRemark(blankToNull(dto.getRemark()));
        }
        try {
            roleMapper.updateById(role);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(400, "角色名称或编码已存在");
        }
        if (statusChanged) {
            evictAssignedUsers(id);
        }
    }

    @Override
    @Transactional
    public void deleteRole(Long id) {
        requireRole(id);
        List<Long> userIds = findUserIdsForRole(id);
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, id));
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, id));
        roleMapper.deleteById(id);
        evictPermissionsAfterCommit(userIds);
    }

    @Override
    @Transactional
    public void assignMenus(Long id, List<Long> menuIds) {
        requireRole(id);
        List<Long> normalizedMenuIds = normalizeMenuIds(menuIds);
        validateMenus(normalizedMenuIds);
        List<Long> userIds = findUserIdsForRole(id);
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, id));
        for (Long menuId : normalizedMenuIds) {
            SysRoleMenu roleMenu = new SysRoleMenu();
            roleMenu.setRoleId(id);
            roleMenu.setMenuId(menuId);
            roleMenuMapper.insert(roleMenu);
        }
        evictPermissionsAfterCommit(userIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> listMenuIds(Long id) {
        requireRole(id);
        return roleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>()
                        .select(SysRoleMenu::getMenuId)
                        .eq(SysRoleMenu::getRoleId, id)
                        .orderByAsc(SysRoleMenu::getMenuId))
                .stream().map(SysRoleMenu::getMenuId).toList();
    }

    private SysRole requireRole(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(400, "角色 ID 不合法");
        }
        SysRole role = roleMapper.selectOne(roleViewQuery().eq(SysRole::getId, id));
        if (role == null) {
            throw new BusinessException(404, "角色不存在");
        }
        return role;
    }

    private void ensureUniqueRole(String roleName, String roleCode, Long excludedRoleId) {
        LambdaQueryWrapper<SysRole> nameQuery = new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleName, roleName);
        LambdaQueryWrapper<SysRole> codeQuery = new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, roleCode);
        if (excludedRoleId != null) {
            nameQuery.ne(SysRole::getId, excludedRoleId);
            codeQuery.ne(SysRole::getId, excludedRoleId);
        }
        if (roleMapper.selectCount(nameQuery) > 0 || roleMapper.selectCount(codeQuery) > 0) {
            throw new BusinessException(400, "角色名称或编码已存在");
        }
    }

    private void validateMenus(List<Long> menuIds) {
        if (!menuIds.isEmpty() && menuMapper.selectCount(new LambdaQueryWrapper<SysMenu>()
                .in(SysMenu::getId, menuIds)) != menuIds.size()) {
            throw new BusinessException(400, "菜单不存在或已删除");
        }
    }

    private List<Long> findUserIdsForRole(Long roleId) {
        return userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                        .select(SysUserRole::getUserId)
                        .eq(SysUserRole::getRoleId, roleId))
                .stream().map(SysUserRole::getUserId).filter(Objects::nonNull).distinct().toList();
    }

    private void evictAssignedUsers(Long roleId) {
        evictPermissionsAfterCommit(findUserIdsForRole(roleId));
    }

    private void evictPermissionsAfterCommit(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            userIds.forEach(redisAuthStore::evictPermissions);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                userIds.forEach(redisAuthStore::evictPermissions);
            }
        });
    }

    private static LambdaQueryWrapper<SysRole> roleViewQuery() {
        return new LambdaQueryWrapper<SysRole>()
                .select(SysRole::getId, SysRole::getRoleName, SysRole::getRoleCode,
                        SysRole::getStatus, SysRole::getSort, SysRole::getRemark, SysRole::getCreateTime);
    }

    private static RoleVO toVO(SysRole role) {
        RoleVO vo = new RoleVO();
        vo.setId(role.getId());
        vo.setRoleName(role.getRoleName());
        vo.setRoleCode(role.getRoleCode());
        vo.setStatus(role.getStatus());
        vo.setSort(role.getSort());
        vo.setRemark(role.getRemark());
        vo.setCreateTime(role.getCreateTime());
        return vo;
    }

    private static List<Long> normalizeMenuIds(List<Long> menuIds) {
        if (menuIds == null || menuIds.isEmpty()) {
            return List.of();
        }
        if (menuIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new BusinessException(400, "菜单 ID 不合法");
        }
        return new ArrayList<>(new LinkedHashSet<>(menuIds));
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
