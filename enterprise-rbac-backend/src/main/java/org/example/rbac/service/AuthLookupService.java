
package org.example.rbac.service;

import org.example.rbac.entity.SysUser;

/**
 * 登录和 JWT 过滤器共用的只读查询。
 * 链路是 sys_user，经 sys_user_role 到 status=1 的 sys_role，再经 sys_role_menu 到 status=1 且 permission 非空的 sys_menu。
 * 不签发令牌，也不改最后登录时间。停用角色不会进入 UserAccess，因此也不会贡献 permission。
 */
public interface AuthLookupService {

    /**
     * 按用户名查未删除用户，包含密码，仅供登录校验。找不到返回 null。
     */
    SysUser findByUsername(String username);

    /**
     * 按主键查未删除用户，不读取密码。已逻辑删除时返回 null。
     */
    SysUser findById(Long userId);

    /**
     * 组装当前仍生效的角色编码、角色名和权限标识。用户没有可用角色时，三个列表都为空。
     */
    UserAccess loadAccess(Long userId);
}
