package org.example.rbac.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.rbac.common.support.SkeletonSupport;
import org.example.rbac.dto.menu.MenuCreateDTO;
import org.example.rbac.dto.menu.MenuUpdateDTO;
import org.example.rbac.entity.SysMenu;
import org.example.rbac.entity.SysRole;
import org.example.rbac.entity.SysRoleMenu;
import org.example.rbac.entity.SysUserRole;
import org.example.rbac.mapper.SysMenuMapper;
import org.example.rbac.mapper.SysRoleMenuMapper;
import org.example.rbac.mapper.SysRoleMapper;
import org.example.rbac.mapper.SysUserRoleMapper;
import org.example.rbac.security.RedisAuthStore;
import org.example.rbac.security.SecuritySupport;
import org.example.rbac.service.AuthLookupService;
import org.example.rbac.service.MenuService;
import org.example.rbac.vo.menu.MenuTreeVO;
import org.example.rbac.vo.menu.MenuVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * 菜单用例：树、详情、创建、更新只使用 sys_menu。删除还要处理 sys_role_menu，避免留下悬空分配。
 * 我的菜单从 SecuritySupport 取当前用户，经 sys_user_role、启用的 sys_role、sys_role_menu 到 sys_menu。
 * 只保留目录 M 和菜单 C，按钮 F 不进导航树。我的权限不另写一套规则，直接用 AuthLookupService 的 permission，
 * 并回写 Redis，和登录、JwtAuthenticationFilter 用的是同一份缓存。
 */
@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {

    private static final int ENABLED = 1;
    private static final Set<String> NAV_TYPES = Set.of("M", "C");

    /** sys_menu。parent_id 为 0 是根，menu_type 区分目录、菜单、按钮和接口。 */
    private final SysMenuMapper menuMapper;
    /** sys_role_menu。按角色关联到菜单；删除菜单时同步处理。 */
    private final SysRoleMenuMapper roleMenuMapper;
    /** sys_user_role。只在“当前用户”查询里把登录用户关联到角色。 */
    private final SysUserRoleMapper userRoleMapper;
    /** sys_role。停用角色不参与我的菜单，和 AuthLookupService 的 status=1 条件一致。 */
    private final SysRoleMapper roleMapper;
    /** 当前用户的 permission 只从这里取，避免和登录响应各算一遍。 */
    private final AuthLookupService authLookupService;
    /** 读到权限后回填 user:permissions:{userId}，过滤器下次可直接使用。 */
    private final RedisAuthStore redisAuthStore;

    @Override
    public List<MenuTreeVO> tree() {
        throw SkeletonSupport.pending("菜单树查询", menuMapper);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuTreeVO> myMenus() {
        Long userId = SecuritySupport.requireUser().userId();
        List<SysMenu> menus = loadAssignedMenus(userId).stream()
                .filter(menu -> Integer.valueOf(ENABLED).equals(menu.getStatus()))
                .filter(menu -> NAV_TYPES.contains(menu.getMenuType()))
                .toList();
        return MenuTrees.build(menus);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> myPermissions() {
        Long userId = SecuritySupport.requireUser().userId();
        List<String> permissions = authLookupService.loadAccess(userId).permissions();
        redisAuthStore.cachePermissions(userId, permissions);
        return permissions;
    }

    /**
     * 当前用户被分配、且角色仍启用的菜单行。停用或已删除的角色不会贡献菜单。
     * 菜单自身的类型和状态由调用方再过滤；这里把目录、菜单、按钮都查出来。
     */
    private List<SysMenu> loadAssignedMenus(Long userId) {
        List<SysUserRole> links = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId));
        if (links.isEmpty()) {
            return List.of();
        }
        List<Long> roleIds = links.stream().map(SysUserRole::getRoleId).filter(id -> id != null).distinct().toList();
        if (roleIds.isEmpty()) {
            return List.of();
        }
        List<SysRole> roles = roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .in(SysRole::getId, roleIds)
                .eq(SysRole::getStatus, ENABLED));
        if (roles.isEmpty()) {
            return List.of();
        }
        List<Long> enabledRoleIds = roles.stream().map(SysRole::getId).toList();
        List<SysRoleMenu> roleMenus = roleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>()
                .in(SysRoleMenu::getRoleId, enabledRoleIds));
        if (roleMenus.isEmpty()) {
            return List.of();
        }
        List<Long> menuIds = roleMenus.stream().map(SysRoleMenu::getMenuId).filter(id -> id != null).distinct().toList();
        if (menuIds.isEmpty()) {
            return List.of();
        }
        return menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .in(SysMenu::getId, menuIds)
                .eq(SysMenu::getStatus, ENABLED));
    }

    @Override
    public MenuVO getMenuById(Long id) {
        throw SkeletonSupport.pending("菜单详情查询", menuMapper);
    }

    @Override
    public void createMenu(MenuCreateDTO dto) {
        throw SkeletonSupport.pending("创建菜单", menuMapper);
    }

    @Override
    public void updateMenu(Long id, MenuUpdateDTO dto) {
        throw SkeletonSupport.pending("更新菜单", menuMapper);
    }

    @Override
    public void deleteMenu(Long id) {
        throw SkeletonSupport.pending("删除菜单", menuMapper, roleMenuMapper);
    }
}
