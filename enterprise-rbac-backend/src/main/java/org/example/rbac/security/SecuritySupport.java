
package org.example.rbac.security;

import org.example.rbac.common.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 从 SecurityContext 取出 JwtAuthenticationFilter 放入的 AuthUser。
 * AuthServiceImpl 的当前用户、刷新和退出都从这里拿 userId 和 jti，不从请求参数读用户。
 */
public final class SecuritySupport {

    private SecuritySupport() {
    }

    public static AuthUser requireUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthUser authUser)) {
            throw new BusinessException(401, "未认证或登录已过期");
        }
        return authUser;
    }
}
