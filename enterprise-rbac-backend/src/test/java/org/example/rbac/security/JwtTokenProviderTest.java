
package org.example.rbac.security;

import io.jsonwebtoken.JwtException;
import org.example.rbac.config.JwtProperties;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenProviderTest {

    @Test
    void issueAndParseRoundTrip() {
        JwtTokenProvider provider = provider();
        IssuedToken issued = provider.issue(1L, "admin", List.of("ROLE_ADMIN"));
        ParsedToken parsed = provider.parse(issued.token());
        assertEquals(1L, parsed.userId());
        assertEquals("admin", parsed.username());
        assertEquals(issued.tokenId(), parsed.tokenId());
        assertEquals(List.of("ROLE_ADMIN"), parsed.roles());
        assertEquals(86400L, provider.expiresInSeconds());
        assertEquals("Bearer", provider.tokenType());
        assertTrue(parsed.expiresAt().isAfter(Instant.now()));
    }

    @Test
    void rejectsTamperedToken() {
        JwtTokenProvider provider = provider();
        IssuedToken issued = provider.issue(1L, "admin", List.of());
        assertThrows(JwtException.class, () -> provider.parse(issued.token() + "x"));
    }

    @Test
    void rejectsShortSecret() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("too-short");
        properties.setExpiration(1000L);
        properties.setHeader("Authorization");
        properties.setPrefix("Bearer");
        JwtTokenProvider provider = new JwtTokenProvider(properties);
        assertThrows(IllegalStateException.class, provider::init);
    }

    @Test
    void seedAdminPasswordMatches() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        assertTrue(encoder.matches("admin123",
                "$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2"));
    }

    private static JwtTokenProvider provider() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("rbacSystemSecretKey2026ChangeThisInProduction");
        properties.setExpiration(86400000L);
        properties.setHeader("Authorization");
        properties.setPrefix("Bearer");
        JwtTokenProvider provider = new JwtTokenProvider(properties);
        provider.init();
        return provider;
    }
}
