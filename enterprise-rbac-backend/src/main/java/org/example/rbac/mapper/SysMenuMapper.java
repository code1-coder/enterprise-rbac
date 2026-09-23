package org.example.rbac.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.example.rbac.entity.SysMenu;

/**
 * sys_menu 的访问口。菜单维护，以及把 permission 组装进当前用户的权限，都从这里读。
 */
public interface SysMenuMapper extends BaseMapper<SysMenu> {
}
