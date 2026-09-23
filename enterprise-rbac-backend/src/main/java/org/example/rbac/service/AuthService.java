
package org.example.rbac.service;

import org.example.rbac.dto.auth.LoginDTO;
import org.example.rbac.dto.auth.RegisterDTO;
import org.example.rbac.vo.auth.CurrentUserVO;
import org.example.rbac.vo.auth.LoginVO;

/**
 * 认证用例：登录、注册、退出、刷新令牌和当前用户。
 * 登录和刷新返回 LoginVO；当前用户返回 CurrentUserVO。
 * 角色和 permission 经 AuthLookupService 走用户、用户角色、角色、角色菜单、菜单。
 * 令牌由 JwtTokenProvider 签发，权限缓存和退出黑名单在 Redis。
 * 注册只写 sys_user，不签发令牌，也不分配角色；角色仍由用户管理接口写入 sys_user_role。
 */
public interface AuthService {

    LoginVO login(LoginDTO dto);

    void register(RegisterDTO dto);

    void logout();

    LoginVO refreshToken();

    CurrentUserVO currentUser();
}
