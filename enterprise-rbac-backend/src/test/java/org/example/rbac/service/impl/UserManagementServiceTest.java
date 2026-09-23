package org.example.rbac.service.impl;

import org.example.rbac.common.exception.BusinessException;
import org.example.rbac.dto.user.UserCreateDTO;
import org.example.rbac.dto.user.UserUpdateDTO;
import org.example.rbac.entity.SysRole;
import org.example.rbac.entity.SysUser;
import org.example.rbac.entity.SysUserRole;
import org.example.rbac.mapper.SysRoleMapper;
import org.example.rbac.mapper.SysUserMapper;
import org.example.rbac.mapper.SysUserRoleMapper;
import org.example.rbac.security.RedisAuthStore;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceTest {

    @BeforeAll
    static void initializeMybatisMetadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), SysUser.class);
    }

    @Mock
    private SysUserMapper userMapper;
    @Mock
    private SysUserRoleMapper userRoleMapper;
    @Mock
    private SysRoleMapper roleMapper;
    @Mock
    private RedisAuthStore redisAuthStore;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void createUserHashesPasswordAndPersistsRoleLinks() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(roleMapper.selectCount(any())).thenReturn(1L);
        when(userMapper.insert(any(SysUser.class))).thenAnswer(invocation -> {
            ((SysUser) invocation.getArgument(0)).setId(20L);
            return 1;
        });
        UserCreateDTO dto = createDTO();
        dto.setRoleIds(List.of(4L));

        service().createUser(dto);

        ArgumentCaptor<SysUser> userCaptor = ArgumentCaptor.forClass(SysUser.class);
        verify(userMapper).insert(userCaptor.capture());
        assertEquals("zhangsan", userCaptor.getValue().getUsername());
        assertEquals(1, userCaptor.getValue().getStatus());
        assertTrue(passwordEncoder.matches("User@123456", userCaptor.getValue().getPassword()));
        ArgumentCaptor<SysUserRole> roleLinkCaptor = ArgumentCaptor.forClass(SysUserRole.class);
        verify(userRoleMapper).insert(roleLinkCaptor.capture());
        assertEquals(20L, roleLinkCaptor.getValue().getUserId());
        assertEquals(4L, roleLinkCaptor.getValue().getRoleId());
    }

    @Test
    void createUserRejectsUnknownRoleWithoutInsertingUser() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(roleMapper.selectCount(any())).thenReturn(0L);
        UserCreateDTO dto = createDTO();
        dto.setRoleIds(List.of(99L));

        BusinessException ex = assertThrows(BusinessException.class, () -> service().createUser(dto));

        assertEquals(400, ex.getCode());
        assertEquals("角色不存在或已删除", ex.getMessage());
        verify(userMapper, never()).insert(any(SysUser.class));
    }

    @Test
    void assignRolesReplacesLinksAndEvictsCachedPermissions() {
        SysUser user = new SysUser();
        user.setId(5L);
        when(userMapper.selectOne(any())).thenReturn(user);
        when(roleMapper.selectCount(any())).thenReturn(2L);

        service().assignRoles(5L, List.of(2L, 3L));

        verify(userRoleMapper).delete(any());
        verify(userRoleMapper, times(2)).insert(any(SysUserRole.class));
        verify(redisAuthStore).evictPermissions(5L);
    }

    @Test
    void updateUserExplicitlyClearsNullableFields() {
        SysUser existing = new SysUser();
        existing.setId(5L);
        when(userMapper.selectOne(any())).thenReturn(existing);
        UserUpdateDTO dto = new UserUpdateDTO();
        dto.setStatus(1);
        dto.setNickname(null);
        dto.setEmail(null);
        dto.setPhone("   ");

        service().updateUser(5L, dto);

        ArgumentCaptor<Wrapper<SysUser>> updateCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(userMapper).update(updateCaptor.capture());
        LambdaUpdateWrapper<SysUser> update = (LambdaUpdateWrapper<SysUser>) updateCaptor.getValue();
        assertTrue(update.getSqlSet().contains("nickname"));
        assertTrue(update.getSqlSet().contains("email"));
        assertTrue(update.getSqlSet().contains("phone"));
        assertTrue(update.getSqlSet().contains("status"));
        assertTrue(update.getParamNameValuePairs().containsValue(null));
        assertTrue(update.getParamNameValuePairs().containsValue(1));
    }

    @Test
    void updateUserRejectsMissingStatus() {
        SysUser existing = new SysUser();
        existing.setId(5L);
        when(userMapper.selectOne(any())).thenReturn(existing);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service().updateUser(5L, new UserUpdateDTO()));

        assertEquals(400, ex.getCode());
        assertEquals("状态不能为空", ex.getMessage());
        verify(userMapper, never()).update(any(Wrapper.class));
    }

    @Test
    void batchDeleteRejectsEmptyIds() {
        BusinessException ex = assertThrows(BusinessException.class, () -> service().batchDeleteUsers(List.of()));

        assertEquals(400, ex.getCode());
        assertEquals("请选择要删除的用户", ex.getMessage());
        verify(userMapper, never()).selectList(any());
    }

    private UserServiceImpl service() {
        return new UserServiceImpl(userMapper, userRoleMapper, passwordEncoder, roleMapper, redisAuthStore);
    }

    private static UserCreateDTO createDTO() {
        UserCreateDTO dto = new UserCreateDTO();
        dto.setUsername(" zhangsan ");
        dto.setPassword("User@123456");
        return dto;
    }
}
