
package org.example.rbac.security;

import java.time.Instant;

/**
 * 新签发的 JWT。token 返回给前端，tokenId 和 expiresAt 留给后续把旧令牌写入黑名单。
 */
public record IssuedToken(String token, String tokenId, Instant expiresAt) {
}
