package org.example.rbac.security;

import jakarta.servlet.FilterChain;
import org.example.rbac.config.JwtProperties;
import org.example.rbac.entity.SysUser;
import org.example.rbac.service.AuthLookupService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 过滤器本身不启动 Web。SecurityContextHolderFilter 在前，模拟 STATELESS 下的延迟上下文；
 * 登录态要在链条内部读取，因为该过滤器结束时会清掉 ThreadLocal。
 */
class JwtAuthenticationFilterTest {

    private JwtTokenProvider provider;
    private JwtAuthenticationFilter filter;
    private SecurityContextHolderFilter contextFilter;
    private String token;

    @BeforeEach
    void setUp() {
        JwtProperties properties = properties();
        provider = new JwtTokenProvider(properties);
        provider.init();
        token = provider.issue(1L, "tester", List.of("ROLE_USER")).token();

        AuthLookupService lookup = mock(AuthLookupService.class);
        RedisAuthStore redis = mock(RedisAuthStore.class);
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("tester");
        user.setStatus(1);
        when(lookup.findById(1L)).thenReturn(user);
        when(redis.isBlacklisted(org.mockito.ArgumentMatchers.any())).thenReturn(false);
        when(redis.getPermissions(1L)).thenReturn(Optional.of(List.of("user:info")));

        filter = new JwtAuthenticationFilter(properties, provider, redis, lookup);
        contextFilter = new SecurityContextHolderFilter(new RequestAttributeSecurityContextRepository());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void bearerHeaderAuthenticates() throws Exception {
        Authentication authentication = capture("Bearer " + token);
        AuthUser user = assertInstanceOf(AuthUser.class, authentication.getPrincipal());
        assertEquals(1L, user.userId());
        assertEquals("tester", user.username());
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_USER".equals(authority.getAuthority())));
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(authority -> "user:info".equals(authority.getAuthority())));
    }

    @Test
    void rawTokenAuthenticates() throws Exception {
        assertEquals(1L, authenticate(token).userId());
    }

    @Test
    void repeatedBearerAuthenticates() throws Exception {
        assertEquals(1L, authenticate("Bearer Bearer " + token).userId());
    }

    @Test
    void missingHeaderStaysAnonymous() throws Exception {
        assertNull(capture(null));
    }

    private AuthUser authenticate(String authorization) throws Exception {
        Authentication authentication = capture(authorization);
        assertNotNull(authentication);
        return assertInstanceOf(AuthUser.class, authentication.getPrincipal());
    }

    /**
     * 先走 SecurityContextHolderFilter，再走 JwtAuthenticationFilter。
     * 内层同时调用 SecuritySupport.requireUser，这是当前用户、刷新和退出共用的入口。
     * 外层过滤器返回后会清掉 ThreadLocal，所以登录态必须在链条内部成立。
     */
    private Authentication capture(String authorization) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/user-info");
        if (authorization != null) {
            request.addHeader("Authorization", authorization);
        }
        Authentication[] captured = new Authentication[1];
        FilterChain afterJwt = (req, res) -> {
            captured[0] = SecurityContextHolder.getContext().getAuthentication();
            if (captured[0] != null) {
                assertEquals(captured[0].getPrincipal(), SecuritySupport.requireUser());
            }
        };
        contextFilter.doFilter(request, new MockHttpServletResponse(),
                (req, res) -> filter.doFilter(req, res, afterJwt));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        return captured[0];
    }

    private static JwtProperties properties() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("rbacSystemSecretKey2026ChangeThisInProduction");
        properties.setExpiration(86400000L);
        properties.setHeader("Authorization");
        properties.setPrefix("Bearer");
        return properties;
    }
}
