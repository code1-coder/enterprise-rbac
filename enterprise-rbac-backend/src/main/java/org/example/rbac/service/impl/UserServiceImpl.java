package org.example.rbac.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.example.rbac.common.exception.BusinessException;
import org.example.rbac.common.result.PageResult;
import org.example.rbac.common.support.SkeletonSupport;
import org.example.rbac.entity.SysUser;
import org.example.rbac.dto.user.PasswordUpdateDTO;
import org.example.rbac.dto.user.ResetPasswordDTO;
import org.example.rbac.dto.user.UserCreateDTO;
import org.example.rbac.dto.user.UserQueryDTO;
import org.example.rbac.dto.user.UserUpdateDTO;
import org.example.rbac.mapper.SysUserMapper;
import org.example.rbac.mapper.SysUserRoleMapper;
import org.example.rbac.security.AuthUser;
import org.example.rbac.security.SecuritySupport;
import org.example.rbac.service.UserService;
import org.example.rbac.vo.user.UserVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户用例只碰两张表：sys_user 管账号，sys_user_role 管角色。
 * 列表、详情、创建、删除和分配角色需要两者；改资料和改密码只动用户表。
 * 修改密码没有菜单权限串。changePassword 用 SecuritySupport 里的当前用户核对路径 id，
 * 只比对并更新 sys_user.password，不改角色，也不作废当前令牌。
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    private static final int ENABLED = 1;

    /** sys_user。逻辑删除只把 deleted 置 1，不会触发外键级联。 */
    private final SysUserMapper userMapper;
    /** sys_user_role。分配角色会重写该用户的关系，删除用户时也要处理这些行。 */
    private final SysUserRoleMapper userRoleMapper;
    /** 与登录共用的 BCrypt。原密码比对和新密码保存都走它，不明文落库。 */
    private final PasswordEncoder passwordEncoder;

    @Override
    public PageResult<UserVO> pageList(UserQueryDTO query) {
        throw SkeletonSupport.pending("用户分页查询", userMapper, userRoleMapper);
    }

    @Override
    public UserVO getUserById(Long id) {
        throw SkeletonSupport.pending("用户详情查询", userMapper, userRoleMapper);
    }

    @Override
    public void createUser(UserCreateDTO dto) {
        throw SkeletonSupport.pending("创建用户", userMapper, userRoleMapper);
    }

    @Override
    public void updateUser(Long id, UserUpdateDTO dto) {
        throw SkeletonSupport.pending("更新用户", userMapper);
    }

    @Override
    public void deleteUser(Long id) {
        throw SkeletonSupport.pending("删除用户", userMapper, userRoleMapper);
    }

    @Override
    public void batchDeleteUsers(List<Long> ids) {
        throw SkeletonSupport.pending("批量删除用户", userMapper, userRoleMapper);
    }

    @Override
    public void changePassword(Long id, PasswordUpdateDTO dto) {
        AuthUser current = SecuritySupport.requireUser();
        if (id == null || !id.equals(current.userId())) {
            throw new BusinessException(403, "只能修改自己的密码");
        }
        SysUser user = userMapper.selectById(id);
        if (user == null || user.getStatus() == null || user.getStatus() != ENABLED) {
            throw new BusinessException(401, "未认证或登录已过期");
        }
        if (user.getPassword() == null || !passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException(400, "原密码错误");
        }
        // 只更新 password。不用 updateById，否则邮箱、昵称等非空字段会一起写回。
        // 列名由 MyBatis-Plus 在启动时建立的 Lambda 缓存解析，这里不写死字段名。
        userMapper.update(new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, id)
                .set(SysUser::getPassword, passwordEncoder.encode(dto.getNewPassword())));
        log.info("password_changed userId={}", id);
    }

    @Override
    public void resetPassword(Long id, ResetPasswordDTO dto) {
        throw SkeletonSupport.pending("重置用户密码", userMapper);
    }

    @Override
    public void assignRoles(Long id, List<Long> roleIds) {
        throw SkeletonSupport.pending("给用户分配角色", userMapper, userRoleMapper);
    }
}
