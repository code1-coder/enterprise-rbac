package org.example.rbac.service;

import org.example.rbac.common.result.PageResult;
import org.example.rbac.dto.role.RoleCreateDTO;
import org.example.rbac.dto.role.RoleQueryDTO;
import org.example.rbac.dto.role.RoleUpdateDTO;
import org.example.rbac.vo.role.RoleVO;

import java.util.List;

/**
 * 角色主数据和菜单分配。角色在 sys_role，菜单关系在 sys_role_menu，用户占用在 sys_user_role。
 * 删除角色要同时考虑后两张关联表；分配权限写的是 menu_id。
 */
public interface RoleService {

    PageResult<RoleVO> pageList(RoleQueryDTO query);

    List<RoleVO> listAll();

    RoleVO getRoleById(Long id);

    void createRole(RoleCreateDTO dto);

    void updateRole(Long id, RoleUpdateDTO dto);

    void deleteRole(Long id);

    void assignMenus(Long id, List<Long> menuIds);

    List<Long> listMenuIds(Long id);
}
