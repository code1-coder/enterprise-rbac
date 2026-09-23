
package org.example.rbac.security;

import java.time.Instant;

/**
 * 放进 SecurityContext 的当前登录身份。
 * userId 对应 sys_user.id，供当前用户、刷新和退出使用；
 * tokenId 是 JWT 的 jti，退出或刷新时按它写入 Redis 黑名单，直到 expiresAt。
 */
public record AuthUser(Long userId, String username, String tokenId, Instant expiresAt) {
}
