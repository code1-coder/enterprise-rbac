package org.example.rbac.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.rbac.common.result.PageResult;
import org.example.rbac.common.support.SkeletonSupport;
import org.example.rbac.dto.role.RoleCreateDTO;
import org.example.rbac.dto.role.RoleQueryDTO;
import org.example.rbac.dto.role.RoleUpdateDTO;
import org.example.rbac.mapper.SysRoleMapper;
import org.example.rbac.mapper.SysRoleMenuMapper;
import org.example.rbac.mapper.SysUserRoleMapper;
import org.example.rbac.service.RoleService;
import org.example.rbac.vo.role.RoleVO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 角色用例的三张表：sys_role 是主表，sys_role_menu 保存分配的菜单，sys_user_role 表示用户仍占用该角色。
 * 删除时三者一起看；分配菜单和查询已分配菜单只需要角色表与角色菜单表。
 */
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    /** sys_role：角色主数据。 */
    private final SysRoleMapper roleMapper;
    /** sys_role_menu。分配权限写 menu_id，不是单独的权限表。 */
    private final SysRoleMenuMapper roleMenuMapper;
    /** sys_user_role。删除角色前用来判断或清理用户侧关系。 */
    private final SysUserRoleMapper userRoleMapper;

    @Override
    public PageResult<RoleVO> pageList(RoleQueryDTO query) {
        throw SkeletonSupport.pending("角色分页查询", roleMapper);
    }

    @Override
    public List<RoleVO> listAll() {
        throw SkeletonSupport.pending("角色下拉列表", roleMapper);
    }

    @Override
    public RoleVO getRoleById(Long id) {
        throw SkeletonSupport.pending("角色详情查询", roleMapper);
    }

    @Override
    public void createRole(RoleCreateDTO dto) {
        throw SkeletonSupport.pending("创建角色", roleMapper);
    }

    @Override
    public void updateRole(Long id, RoleUpdateDTO dto) {
        throw SkeletonSupport.pending("更新角色", roleMapper);
    }

    @Override
    public void deleteRole(Long id) {
        throw SkeletonSupport.pending("删除角色", roleMapper, roleMenuMapper, userRoleMapper);
    }

    @Override
    public void assignMenus(Long id, List<Long> menuIds) {
        throw SkeletonSupport.pending("给角色分配权限", roleMapper, roleMenuMapper);
    }

    @Override
    public List<Long> listMenuIds(Long id) {
        throw SkeletonSupport.pending("查询角色已分配菜单", roleMapper, roleMenuMapper);
    }
}
