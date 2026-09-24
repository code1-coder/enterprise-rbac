package org.example.rbac.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.example.rbac.common.exception.BusinessException;
import org.example.rbac.dto.user.PasswordUpdateDTO;
import org.example.rbac.entity.SysUser;
import org.example.rbac.mapper.SysUserMapper;
import org.example.rbac.mapper.SysRoleMapper;
import org.example.rbac.mapper.SysUserRoleMapper;
import org.example.rbac.security.AuthUser;
import org.example.rbac.security.RedisAuthStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserPasswordServiceTest {

    @Mock
    private SysUserMapper userMapper;
    @Mock
    private SysUserRoleMapper userRoleMapper;
    @Mock
    private SysRoleMapper roleMapper;
    @Mock
    private RedisAuthStore redisAuthStore;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 单测不启动 Spring，MyBatis-Plus 不会扫描 SysUser。
     * LambdaUpdateWrapper.set 会立刻把方法引用解析成列名，所以要先登记 TableInfo。
     * 正式启动时由 MyBatis-Plus 自动完成，生产代码不需要这一步。
     */
    @BeforeAll
    static void initLambdaCache() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), SysUser.class);
    }

    @AfterEach
    void clearUser() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void changePasswordUpdatesOwnHash() {
        loginAs(3L);
        SysUser user = new SysUser();
        user.setId(3L);
        user.setStatus(1);
        user.setPassword(passwordEncoder.encode("admin123"));
        when(userMapper.selectById(3L)).thenReturn(user);
        UserServiceImpl service = service();

        service.changePassword(3L, passwords("admin123", "User@123456"));

        ArgumentCaptor<Wrapper<SysUser>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(userMapper).update(captor.capture());
        assertTrue(captor.getValue().getSqlSet().contains("password"));
    }

    @Test
    void changePasswordRejectsAnotherUser() {
        loginAs(3L);
        UserServiceImpl service = service();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.changePassword(4L, passwords("admin123", "User@123456")));

        assertEquals(403, ex.getCode());
        assertEquals("只能修改自己的密码", ex.getMessage());
        verify(userMapper, never()).selectById(any());
    }

    @Test
    void changePasswordRejectsWrongOldPassword() {
        loginAs(3L);
        SysUser user = new SysUser();
        user.setId(3L);
        user.setStatus(1);
        user.setPassword(passwordEncoder.encode("admin123"));
        when(userMapper.selectById(3L)).thenReturn(user);
        UserServiceImpl service = service();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.changePassword(3L, passwords("wrong", "User@123456")));

        assertEquals(400, ex.getCode());
        assertEquals("原密码错误", ex.getMessage());
        verify(userMapper, never()).update(any());
    }

    private UserServiceImpl service() {
        return new UserServiceImpl(userMapper, userRoleMapper, passwordEncoder, roleMapper, redisAuthStore);
    }

    private static void loginAs(Long userId) {
        AuthUser authUser = new AuthUser(userId, "user", "jti", Instant.now().plusSeconds(60));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(authUser, null));
    }

    private static PasswordUpdateDTO passwords(String oldPassword, String newPassword) {
        PasswordUpdateDTO dto = new PasswordUpdateDTO();
        dto.setOldPassword(oldPassword);
        dto.setNewPassword(newPassword);
        return dto;
    }
}
