package org.example.rbac.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.example.rbac.entity.SysUser;

/**
 * sys_user 的 MyBatis-Plus 访问口。AuthService 与 UserService 通过它读写用户，不在 Controller 中调用。
 */
public interface SysUserMapper extends BaseMapper<SysUser> {
}
