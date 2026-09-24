package org.example.rbac.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.rbac.common.exception.BusinessException;
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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
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
    private static final int DEFAULT_SORT = 0;
    private static final int DEFAULT_VISIBLE = 1;
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

    /** 返回管理端完整菜单树，供菜单维护和角色授权使用。 */
    @Override
    @Transactional(readOnly = true)
    public List<MenuTreeVO> tree() {
        return MenuTrees.build(menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .orderByAsc(SysMenu::getParentId, SysMenu::getSort, SysMenu::getId)));
    }

    /** 仅返回当前登录用户已分配且启用的目录和菜单，不将按钮或接口权限作为导航节点。 */
    @Override
    @Transactional(readOnly = true)
    public List<MenuTreeVO> myMenus() {
        Long userId = SecuritySupport.requireUser().userId();
        // 导航只显示 M/C 类型；F/A 类型用于按钮鉴权或接口权限，不进入页面树。
        List<SysMenu> menus = loadAssignedMenus(userId).stream()
                .filter(menu -> Integer.valueOf(ENABLED).equals(menu.getStatus()))
                .filter(menu -> NAV_TYPES.contains(menu.getMenuType()))
                .toList();
        return MenuTrees.build(menus);
    }

    /** 使用认证模块的统一权限计算结果，并回填与认证过滤器共享的 Redis 权限缓存。 */
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

    /** 按菜单 ID 查询详情并显式映射为接口 VO，避免暴露持久化实体。 */
    @Override
    @Transactional(readOnly = true)
    public MenuVO getMenuById(Long id) {
        SysMenu menu = requireMenu(id);
        MenuVO vo = new MenuVO();
        vo.setId(menu.getId());
        vo.setParentId(menu.getParentId());
        vo.setMenuName(menu.getMenuName());
        vo.setMenuType(menu.getMenuType());
        vo.setPath(menu.getPath());
        vo.setComponent(menu.getComponent());
        vo.setPermission(menu.getPermission());
        vo.setIcon(menu.getIcon());
        vo.setSort(menu.getSort());
        vo.setVisible(menu.getVisible());
        vo.setStatus(menu.getStatus());
        vo.setRemark(menu.getRemark());
        vo.setCreateTime(menu.getCreateTime());
        return vo;
    }

    /** 创建菜单；可选数字字段使用接口默认值，空白文本统一保存为 NULL。 */
    @Override
    @Transactional
    public void createMenu(MenuCreateDTO dto) {
        requireParent(dto.getParentId());
        SysMenu menu = new SysMenu();
        menu.setParentId(dto.getParentId());
        menu.setMenuName(dto.getMenuName().trim());
        menu.setMenuType(dto.getMenuType());
        menu.setPath(blankToNull(dto.getPath()));
        menu.setComponent(blankToNull(dto.getComponent()));
        menu.setPermission(blankToNull(dto.getPermission()));
        menu.setIcon(blankToNull(dto.getIcon()));
        // 新建时的默认值与接口约定一致，避免依赖数据库隐式默认行为。
        menu.setSort(dto.getSort() == null ? DEFAULT_SORT : dto.getSort());
        menu.setVisible(dto.getVisible() == null ? DEFAULT_VISIBLE : dto.getVisible());
        menu.setStatus(dto.getStatus() == null ? ENABLED : dto.getStatus());
        menu.setRemark(blankToNull(dto.getRemark()));
        menuMapper.insert(menu);
    }

    /** 更新核心字段，并按 DTO 是否提供保留可选数值和备注字段；文本字段空白表示清空。 */
    @Override
    @Transactional
    public void updateMenu(Long id, MenuUpdateDTO dto) {
        SysMenu menu = requireMenu(id);
        validateParent(dto.getParentId(), id);
        menu.setParentId(dto.getParentId());
        menu.setMenuName(dto.getMenuName().trim());
        menu.setMenuType(dto.getMenuType());
        menu.setPath(blankToNull(dto.getPath()));
        menu.setComponent(blankToNull(dto.getComponent()));
        menu.setPermission(blankToNull(dto.getPermission()));
        menu.setIcon(blankToNull(dto.getIcon()));
        // 可选字段未提交时保留原值，明确传入的空白备注仍表示清空。
        if (dto.getSort() != null) {
            menu.setSort(dto.getSort());
        }
        if (dto.getVisible() != null) {
            menu.setVisible(dto.getVisible());
        }
        if (dto.getStatus() != null) {
            menu.setStatus(dto.getStatus());
        }
        if (dto.getRemark() != null) {
            menu.setRemark(blankToNull(dto.getRemark()));
        }
        menuMapper.updateById(menu);
        evictAssignedUsers(List.of(id));
    }

    /** 拒绝删除仍有子节点的菜单，并在同一事务清理分配关系后逻辑删除菜单。 */
    @Override
    @Transactional
    public void deleteMenu(Long id) {
        SysMenu menu = requireMenu(id);
        if (menuMapper.selectCount(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getParentId, id)) > 0) {
            throw new BusinessException(400, "存在子菜单，不能删除");
        }
        List<Long> userIds = findAssignedUserIds(List.of(id));
        // 关联表没有逻辑删除列；关系清理和菜单逻辑删除必须一起提交或回滚。
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getMenuId, id));
        menuMapper.deleteById(id);
        evictPermissionsAfterCommit(userIds);
    }

    /** 校验菜单主键并统一处理非法 ID 与不存在资源的业务错误。 */
    private SysMenu requireMenu(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(400, "菜单 ID 不合法");
        }
        SysMenu menu = menuMapper.selectById(id);
        if (menu == null) {
            throw new BusinessException(404, "菜单不存在");
        }
        return menu;
    }

    /** 根节点使用 0，其余父节点必须指向仍存在的菜单。 */
    private void requireParent(Long parentId) {
        if (parentId == null || parentId < 0) {
            throw new BusinessException(400, "父菜单 ID 不合法");
        }
        if (parentId != 0 && menuMapper.selectById(parentId) == null) {
            throw new BusinessException(400, "父菜单不存在或已删除");
        }
    }

    /** 验证新父节点不是当前节点或其后代，并阻止已有脏数据形成的祖先环。 */
    private void validateParent(Long parentId, Long menuId) {
        requireParent(parentId);
        if (Objects.equals(parentId, menuId)) {
            throw new BusinessException(400, "父菜单不能是自身或其子菜单");
        }
        Set<Long> visited = new LinkedHashSet<>();
        Long ancestorId = parentId;
        // 沿父链向上检查，既阻止移动到自身后代，也避免环形数据导致无限遍历。
        while (ancestorId != null && ancestorId != 0) {
            if (!visited.add(ancestorId) || Objects.equals(ancestorId, menuId)) {
                throw new BusinessException(400, "父菜单不能是自身或其子菜单");
            }
            SysMenu ancestor = menuMapper.selectById(ancestorId);
            if (ancestor == null) {
                throw new BusinessException(400, "父菜单不存在或已删除");
            }
            ancestorId = ancestor.getParentId();
        }
    }

    /** 找出受菜单变更影响的用户，并统一安排权限缓存失效。 */
    private void evictAssignedUsers(List<Long> menuIds) {
        evictPermissionsAfterCommit(findAssignedUserIds(menuIds));
    }

    /** 沿菜单—角色—用户关联查找缓存受影响的用户 ID，并去除重复关联。 */
    private List<Long> findAssignedUserIds(List<Long> menuIds) {
        List<Long> roleIds = roleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>()
                        .select(SysRoleMenu::getRoleId)
                        .in(SysRoleMenu::getMenuId, menuIds))
                .stream().map(SysRoleMenu::getRoleId).filter(Objects::nonNull).distinct().toList();
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                        .select(SysUserRole::getUserId)
                        .in(SysUserRole::getRoleId, roleIds))
                .stream().map(SysUserRole::getUserId).filter(Objects::nonNull).distinct().toList();
    }

    /** 事务提交后清理受影响用户的权限缓存，避免回滚事务造成缓存与数据库不一致。 */
    private void evictPermissionsAfterCommit(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            // 非事务代理场景（如直接调用）没有提交回调，只能立即失效缓存。
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

    /** 裁剪可选文本，并将空白字符串转换为数据库 NULL。 */
    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
