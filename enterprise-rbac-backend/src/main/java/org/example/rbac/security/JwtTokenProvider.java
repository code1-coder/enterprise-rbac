
package org.example.rbac.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.example.rbac.config.JwtProperties;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 用 jjwt 和 jwt.secret 签发、解析 HS256 令牌。
 * AuthServiceImpl 在登录和刷新时调用 issue；JwtAuthenticationFilter 在每次请求时调用 parse。
 * 令牌只放 userId、用户名、jti 和角色编码，不放密码，也不放菜单 permission。
 */
@Component
public class JwtTokenProvider {

    static final String CLAIM_USER_ID = "uid";
    static final String CLAIM_ROLES = "roles";
    private static final int MIN_SECRET_BYTES = 32;

    private final JwtProperties properties;
    private SecretKey key;

    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        if (properties.getSecret() == null || properties.getSecret().isBlank()) {
            throw new IllegalStateException("jwt.secret 未配置");
        }
        byte[] secretBytes = properties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("jwt.secret 长度不足，HS256 至少需要 32 字节");
        }
        if (properties.getExpiration() <= 0) {
            throw new IllegalStateException("jwt.expiration 必须大于 0");
        }
        if (properties.getHeader() == null || properties.getHeader().isBlank()
                || properties.getPrefix() == null || properties.getPrefix().isBlank()) {
            throw new IllegalStateException("jwt.header 和 jwt.prefix 必须配置");
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
    }

    public long expiresInSeconds() {
        return properties.getExpiration() / 1000;
    }

    public String tokenType() {
        return properties.getPrefix().trim();
    }

    public IssuedToken issue(Long userId, String username, List<String> roleCodes) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusMillis(properties.getExpiration());
        String tokenId = UUID.randomUUID().toString().replace("-", "");
        String token = Jwts.builder()
                .id(tokenId)
                .subject(username)
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_ROLES, roleCodes == null ? List.of() : roleCodes)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();
        return new IssuedToken(token, tokenId, expiresAt);
    }

    public ParsedToken parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        Number userId = claims.get(CLAIM_USER_ID, Number.class);
        if (userId == null || claims.getSubject() == null || claims.getId() == null || claims.getExpiration() == null) {
            throw new MalformedJwtException("令牌缺少必要声明");
        }
        return new ParsedToken(
                userId.longValue(),
                claims.getSubject(),
                claims.getId(),
                claims.getExpiration().toInstant(),
                readRoles(claims.get(CLAIM_ROLES)));
    }

    private static List<String> readRoles(Object raw) {
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<String> roles = new ArrayList<>();
        for (Object item : list) {
            if (item != null && !item.toString().isBlank()) {
                roles.add(item.toString());
            }
        }
        return List.copyOf(roles);
    }
}
