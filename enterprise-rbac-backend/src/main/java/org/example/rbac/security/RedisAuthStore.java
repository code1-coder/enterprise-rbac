
package org.example.rbac.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.rbac.common.exception.BusinessException;
import org.example.rbac.config.JwtProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 登录态用到的两处 Redis 数据。
 * user:permissions:{userId} 保存权限标识列表，登录、刷新和当前用户写入，过滤器未命中时再查库回填。
 * auth:token:blacklist:{jti} 保存退出或刷新后作废的令牌，存活到原令牌过期。
 * 权限缓存写失败时仍允许登录，过滤器会回源数据库；黑名单写失败则让退出或刷新失败，避免客户端以为令牌已经作废。
 */
@Service
public class RedisAuthStore {

    static final String PERMISSION_PREFIX = "user:permissions:";
    static final String BLACKLIST_PREFIX = "auth:token:blacklist:";

    private static final Logger log = LoggerFactory.getLogger(RedisAuthStore.class);
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final JwtProperties jwtProperties;

    public RedisAuthStore(StringRedisTemplate redis, ObjectMapper objectMapper, JwtProperties jwtProperties) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.jwtProperties = jwtProperties;
    }

    public Optional<List<String>> getPermissions(Long userId) {
        try {
            String json = redis.opsForValue().get(PERMISSION_PREFIX + userId);
            if (json == null || json.isBlank()) {
                return Optional.empty();
            }
            List<String> permissions = objectMapper.readValue(json, STRING_LIST);
            if (permissions == null) {
                return Optional.empty();
            }
            return Optional.of(permissions);
        } catch (Exception ex) {
            log.warn("permission_cache_read_failed userId={}", userId, ex);
            return Optional.empty();
        }
    }

    public void cachePermissions(Long userId, List<String> permissions) {
        try {
            List<String> safe = permissions == null ? List.of() : permissions;
            String json = objectMapper.writeValueAsString(safe);
            redis.opsForValue().set(
                    PERMISSION_PREFIX + userId,
                    json,
                    Duration.ofMillis(jwtProperties.getExpiration()));
        } catch (Exception ex) {
            log.warn("permission_cache_write_failed userId={}", userId, ex);
        }
    }

    public void evictPermissions(Long userId) {
        try {
            redis.delete(PERMISSION_PREFIX + userId);
        } catch (RuntimeException ex) {
            log.warn("permission_cache_evict_failed userId={}", userId, ex);
        }
    }

    public boolean isBlacklisted(String tokenId) {
        if (tokenId == null || tokenId.isBlank()) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(redis.hasKey(BLACKLIST_PREFIX + tokenId));
        } catch (Exception ex) {
            log.error("token_blacklist_read_failed", ex);
            return false;
        }
    }

    public void blacklist(String tokenId, Instant expiresAt) {
        Duration ttl = Duration.between(Instant.now(), expiresAt);
        if (ttl.isZero() || ttl.isNegative()) {
            return;
        }
        try {
            redis.opsForValue().set(BLACKLIST_PREFIX + tokenId, "1", ttl);
        } catch (RuntimeException ex) {
            log.error("token_blacklist_write_failed", ex);
            throw new BusinessException(500, "登录状态更新失败，请稍后重试");
        }
    }
}
