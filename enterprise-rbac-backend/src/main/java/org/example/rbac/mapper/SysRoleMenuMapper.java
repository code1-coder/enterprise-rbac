package org.example.rbac.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.example.rbac.entity.SysRoleMenu;

/**
 * sys_role_menu 的访问口。角色分配菜单时写入；从角色反查菜单和权限时也读它。
 */
public interface SysRoleMenuMapper extends BaseMapper<SysRoleMenu> {
}
