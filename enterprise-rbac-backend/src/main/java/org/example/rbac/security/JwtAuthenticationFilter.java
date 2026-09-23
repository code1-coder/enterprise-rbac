
package org.example.rbac.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.rbac.config.JwtProperties;
import org.example.rbac.entity.SysUser;
import org.example.rbac.service.AuthLookupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 无 Session 的认证入口，由 SecurityConfig 放在 UsernamePasswordAuthenticationFilter 之前。
 * 请求头可以是 Bearer 令牌、不带前缀的三段 JWT，或重复的 Bearer。识别不到时直接放行，
 * 登录和注册仍由 authorizeHttpRequests 决定是否公开。
 * OpenApiConfig 让 Knife4j 的 Authorize 只填 LoginVO.token，并自动加上配置的前缀。
 * 解析成功后写入 SecurityContext，并保存到请求属性。AuthorizationFilter 和 SecuritySupport 读的是同一次登录态，
 * AuthServiceImpl 的当前用户、刷新和退出都从 SecuritySupport 拿 AuthUser，不再自己解析 JWT。
 * 令牌无效、在黑名单，或 sys_user 已删除、已停用时不写入登录态，受保护接口会落到 SecurityConfig 的 401。
 * 权限来自 Redis 的 user:permissions:{userId}，没有缓存时经 AuthLookupService 回源并回填。
 * 角色编码保留在令牌声明中，但不作为权限；接口授权只使用 sys_menu.permission。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final int ENABLED = 1;

    private final JwtProperties jwtProperties;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisAuthStore redisAuthStore;
    private final AuthLookupService authLookupService;
    /**
     * 与 SecurityConfig 使用同一个请求属性仓库。
     * SecurityContextHolderFilter 先放入延迟上下文；这里 saveContext 后，后续过滤器和 Controller 才能看到 AuthUser。
     */
    private final SecurityContextRepository securityContextRepository = new RequestAttributeSecurityContextRepository();

    public JwtAuthenticationFilter(JwtProperties jwtProperties,
                                   JwtTokenProvider jwtTokenProvider,
                                   RedisAuthStore redisAuthStore,
                                   AuthLookupService authLookupService) {
        this.jwtProperties = jwtProperties;
        this.jwtTokenProvider = jwtTokenProvider;
        this.redisAuthStore = redisAuthStore;
        this.authLookupService = authLookupService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null) {
            Authentication authentication = authenticate(request, token);
            if (authentication != null) {
                publish(authentication, request, response);
            }
        }
        filterChain.doFilter(request, response);
    }

    /**
     * SecurityContextHolderFilter 放进线程的是延迟上下文，请求属性里一开始是空的。
     * setContext 换成带 AuthUser 的上下文，saveContext 再写回请求属性，
     * AuthorizationFilter 和 SecuritySupport 才会把这次请求当成已登录。
     */
    private void publish(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }

    private Authentication authenticate(HttpServletRequest request, String token) {
        try {
            ParsedToken parsed = jwtTokenProvider.parse(token);
            if (redisAuthStore.isBlacklisted(parsed.tokenId())) {
                log.debug("jwt_blacklisted userId={}", parsed.userId());
                return null;
            }
            SysUser user = authLookupService.findById(parsed.userId());
            if (user == null || user.getStatus() == null || user.getStatus() != ENABLED) {
                log.debug("jwt_user_unavailable userId={}", parsed.userId());
                return null;
            }
            List<String> permissions = redisAuthStore.getPermissions(user.getId()).orElseGet(() -> {
                List<String> loaded = authLookupService.loadAccess(user.getId()).permissions();
                redisAuthStore.cachePermissions(user.getId(), loaded);
                return loaded;
            });
            Set<String> granted = new LinkedHashSet<>(permissions);
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            for (String code : granted) {
                if (code != null && !code.isBlank()) {
                    authorities.add(new SimpleGrantedAuthority(code));
                }
            }
            AuthUser authUser = new AuthUser(user.getId(), user.getUsername(), parsed.tokenId(), parsed.expiresAt());
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(authUser, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            return authentication;
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("jwt_rejected");
            return null;
        }
    }

    /**
     * 取出 JWT。Knife4j 按 HTTP Bearer 会写成 Authorization: Bearer <token>，token 只填 LoginVO.token。
     * 调试时也可能只粘贴三段 JWT，或把已经带前缀的整段再贴进 Authorize，变成 Bearer Bearer <token>。
     * 这三种都认。空头或无法识别时返回 null，不记录令牌本身；受保护接口随后由 SecurityConfig 返回 401。
     */
    private String resolveToken(HttpServletRequest request) {
        String headerName = jwtProperties.getHeader();
        if (headerName == null || headerName.isBlank()) {
            return null;
        }
        String header = request.getHeader(headerName);
        if (header == null || header.isBlank()) {
            return null;
        }
        String token = stripBearerPrefixes(header.trim());
        if (!isCompactJwt(token)) {
            log.debug("jwt_header_ignored");
            return null;
        }
        return token;
    }

    /** 去掉配置前缀，允许重复书写。前缀必须单独成词，避免误切 JWT 本身。 */
    private String stripBearerPrefixes(String value) {
        String prefix = jwtProperties.getPrefix();
        String marker = prefix == null || prefix.isBlank() ? "Bearer" : prefix.trim();
        String current = value;
        while (current.regionMatches(true, 0, marker, 0, marker.length())) {
            String rest = current.substring(marker.length());
            if (rest.isEmpty() || !Character.isWhitespace(rest.charAt(0))) {
                break;
            }
            current = rest.trim();
        }
        return current;
    }

    /** 紧凑 JWT 是 header.payload.signature 三段，中间没有空格。签名是否有效留给 JwtTokenProvider。 */
    private static boolean isCompactJwt(String value) {
        if (value == null || value.isEmpty() || value.indexOf(' ') >= 0) {
            return false;
        }
        int first = value.indexOf('.');
        if (first <= 0) {
            return false;
        }
        int second = value.indexOf('.', first + 1);
        return second > first + 1
                && second < value.length() - 1
                && value.indexOf('.', second + 1) < 0;
    }
}
