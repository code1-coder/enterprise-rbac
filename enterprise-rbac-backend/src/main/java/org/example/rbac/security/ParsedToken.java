
package org.example.rbac.security;

import java.time.Instant;
import java.util.List;

/**
 * JwtTokenProvider 解析出的令牌内容。
 * roles 是签发时写入的 sys_role.role_code，不作为接口授权依据；
 * 权限标识不在令牌里，而在 Redis 的 user:permissions:{userId}。
 */
public record ParsedToken(Long userId, String username, String tokenId, Instant expiresAt, List<String> roles) {
}
