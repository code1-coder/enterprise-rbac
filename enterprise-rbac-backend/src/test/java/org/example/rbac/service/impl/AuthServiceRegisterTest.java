package org.example.rbac.service.impl;

import org.example.rbac.common.exception.BusinessException;
import org.example.rbac.dto.auth.RegisterDTO;
import org.example.rbac.entity.SysUser;
import org.example.rbac.mapper.SysUserMapper;
import org.example.rbac.security.JwtTokenProvider;
import org.example.rbac.security.RedisAuthStore;
import org.example.rbac.service.AuthLookupService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceRegisterTest {

    @Mock
    private SysUserMapper userMapper;
    @Mock
    private AuthLookupService authLookupService;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private RedisAuthStore redisAuthStore;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void registerStoresBcryptUserWithoutToken() {
        when(authLookupService.findByUsername("newuser")).thenReturn(null);
        when(userMapper.insert(any(SysUser.class))).thenAnswer(invocation -> {
            SysUser inserted = invocation.getArgument(0);
            inserted.setId(8L);
            return 1;
        });
        AuthServiceImpl service = service();

        service.register(dto());

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(userMapper).insert(captor.capture());
        SysUser saved = captor.getValue();
        assertEquals(8L, saved.getId());
        assertEquals("newuser", saved.getUsername());
        assertEquals("newuser", saved.getNickname());
        assertEquals("user@example.com", saved.getEmail());
        assertNull(saved.getPhone());
        assertEquals(1, saved.getStatus());
        assertEquals(0, saved.getDeleted());
        assertTrue(passwordEncoder.matches("User@123456", saved.getPassword()));
        verify(jwtTokenProvider, never()).issue(any(), any(), any());
    }

    @Test
    void registerRejectsExistingUsername() {
        when(authLookupService.findByUsername("newuser")).thenReturn(new SysUser());
        AuthServiceImpl service = service();

        BusinessException ex = assertThrows(BusinessException.class, () -> service.register(dto()));

        assertEquals(400, ex.getCode());
        assertEquals("用户名已存在", ex.getMessage());
        verify(userMapper, never()).insert(any(SysUser.class));
    }

    @Test
    void registerRejectsDuplicateKeyFromDeletedUsername() {
        when(authLookupService.findByUsername("newuser")).thenReturn(null);
        when(userMapper.insert(any(SysUser.class))).thenThrow(new DuplicateKeyException("uk"));
        AuthServiceImpl service = service();

        BusinessException ex = assertThrows(BusinessException.class, () -> service.register(dto()));

        assertEquals(400, ex.getCode());
        assertEquals("用户名已存在", ex.getMessage());
    }

    private AuthServiceImpl service() {
        return new AuthServiceImpl(userMapper, authLookupService, passwordEncoder, jwtTokenProvider, redisAuthStore);
    }

    private static RegisterDTO dto() {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername(" newuser ");
        dto.setPassword("User@123456");
        dto.setEmail(" user@example.com ");
        dto.setPhone(" ");
        return dto;
    }
}
