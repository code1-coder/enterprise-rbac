
package org.example.rbac.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.rbac.entity.SysMenu;
import org.example.rbac.entity.SysRole;
import org.example.rbac.entity.SysRoleMenu;
import org.example.rbac.entity.SysUser;
import org.example.rbac.entity.SysUserRole;
import org.example.rbac.mapper.SysMenuMapper;
import org.example.rbac.mapper.SysRoleMapper;
import org.example.rbac.mapper.SysRoleMenuMapper;
import org.example.rbac.mapper.SysUserMapper;
import org.example.rbac.mapper.SysUserRoleMapper;
import org.example.rbac.service.AuthLookupService;
import org.example.rbac.service.UserAccess;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * AuthLookupService 的查询实现。五个 Mapper 都在这里，过滤器和 AuthServiceImpl 才能看到同一套关联结果。
 * sys_user、sys_role、sys_menu 的 deleted 由 MyBatis-Plus 逻辑删除处理；角色和菜单另外要求 status=1。
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthLookupServiceImpl implements AuthLookupService {

    private static final int ENABLED = 1;

    /** sys_user：账号、密码、状态和资料。 */
    private final SysUserMapper userMapper;
    /** sys_user_role：用户拥有哪些角色。 */
    private final SysUserRoleMapper userRoleMapper;
    /** sys_role：只要仍启用的角色编码和名称。 */
    private final SysRoleMapper roleMapper;
    /** sys_role_menu：这些角色被分配了哪些菜单。 */
    private final SysRoleMenuMapper roleMenuMapper;
    /** sys_menu：取出 permission，目录上的空权限会被丢掉。 */
    private final SysMenuMapper menuMapper;

    @Override
    public SysUser findByUsername(String username) {
        return userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .select(SysUser::getId, SysUser::getUsername, SysUser::getPassword,
                        SysUser::getNickname, SysUser::getEmail, SysUser::getStatus)
                .eq(SysUser::getUsername, username));
    }

    @Override
    public SysUser findById(Long userId) {
        return userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .select(SysUser::getId, SysUser::getUsername, SysUser::getNickname,
                        SysUser::getEmail, SysUser::getStatus)
                .eq(SysUser::getId, userId));
    }

    @Override
    public UserAccess loadAccess(Long userId) {
        if (userId == null) {
            return new UserAccess(List.of(), List.of(), List.of());
        }
        List<SysUserRole> userRoles = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId));
        if (userRoles.isEmpty()) {
            return new UserAccess(List.of(), List.of(), List.of());
        }
        List<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).distinct().toList();
        List<SysRole> roles = roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .in(SysRole::getId, roleIds)
                .eq(SysRole::getStatus, ENABLED)
                .orderByAsc(SysRole::getSort)
                .orderByAsc(SysRole::getId));
        if (roles.isEmpty()) {
            return new UserAccess(List.of(), List.of(), List.of());
        }
        List<String> roleCodes = roles.stream()
                .map(SysRole::getRoleCode)
                .filter(code -> code != null && !code.isBlank())
                .toList();
        List<String> roleNames = roles.stream()
                .map(SysRole::getRoleName)
                .toList();
        List<Long> enabledRoleIds = roles.stream().map(SysRole::getId).toList();
        List<SysRoleMenu> roleMenus = roleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>()
                .in(SysRoleMenu::getRoleId, enabledRoleIds));
        if (roleMenus.isEmpty()) {
            return new UserAccess(roleCodes, roleNames, List.of());
        }
        List<Long> menuIds = roleMenus.stream().map(SysRoleMenu::getMenuId).distinct().toList();
        List<SysMenu> menus = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .in(SysMenu::getId, menuIds)
                .eq(SysMenu::getStatus, ENABLED)
                .isNotNull(SysMenu::getPermission));
        List<String> permissions = menus.stream()
                .map(SysMenu::getPermission)
                .filter(permission -> permission != null && !permission.isBlank())
                .distinct()
                .sorted()
                .toList();
        return new UserAccess(roleCodes, roleNames, permissions);
    }
}
