package org.example.rbac.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.example.rbac.entity.SysUserRole;

/**
 * sys_user_role 的访问口。用户分配角色时写入；从用户反查角色时也读它。
 */
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {
}
