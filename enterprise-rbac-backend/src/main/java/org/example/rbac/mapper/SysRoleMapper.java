package org.example.rbac.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.example.rbac.entity.SysRole;

/**
 * sys_role 的访问口。角色维护用它，登录时也用它确认角色是否仍然有效。
 */
public interface SysRoleMapper extends BaseMapper<SysRole> {
}
