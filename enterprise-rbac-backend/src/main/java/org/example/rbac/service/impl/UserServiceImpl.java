package org.example.rbac.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.example.rbac.common.exception.BusinessException;
import org.example.rbac.common.result.PageResult;
import org.example.rbac.entity.SysUser;
import org.example.rbac.entity.SysRole;
import org.example.rbac.entity.SysUserRole;
import org.example.rbac.dto.user.PasswordUpdateDTO;
import org.example.rbac.dto.user.ResetPasswordDTO;
import org.example.rbac.dto.user.UserCreateDTO;
import org.example.rbac.dto.user.UserQueryDTO;
import org.example.rbac.dto.user.UserUpdateDTO;
import org.example.rbac.mapper.SysUserMapper;
import org.example.rbac.mapper.SysRoleMapper;
import org.example.rbac.mapper.SysUserRoleMapper;
import org.example.rbac.security.AuthUser;
import org.example.rbac.security.SecuritySupport;
import org.example.rbac.security.RedisAuthStore;
import org.example.rbac.service.UserService;
import org.example.rbac.vo.user.UserVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private final SysRoleMapper roleMapper;
    private final RedisAuthStore redisAuthStore;

    @Override
    public PageResult<UserVO> pageList(UserQueryDTO query) {
        Page<SysUser> page = userMapper.selectPage(new Page<>(query.getPage(), query.getSize()),
                userViewQuery()
                        .like(query.getUsername() != null && !query.getUsername().isBlank(),
                                SysUser::getUsername, query.getUsername().trim())
                        .eq(query.getStatus() != null, SysUser::getStatus, query.getStatus())
                        .orderByDesc(SysUser::getCreateTime)
                        .orderByDesc(SysUser::getId));
        List<UserVO> records = toVOs(page.getRecords());
        PageResult<UserVO> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(page.getTotal());
        result.setSize(page.getSize());
        result.setCurrent(page.getCurrent());
        result.setPages(page.getPages());
        return result;
    }

    @Override
    public UserVO getUserById(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(400, "用户 ID 不合法");
        }
        SysUser user = userMapper.selectOne(userViewQuery().eq(SysUser::getId, id));
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return toVO(user);
    }

    @Override
    @Transactional
    public void createUser(UserCreateDTO dto) {
        String username = dto.getUsername().trim();
        if (userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)) > 0) {
            throw new BusinessException(400, "用户名已存在");
        }
        List<Long> roleIds = normalizeRoleIds(dto.getRoleIds());
        validateRoles(roleIds);
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(blankToNull(dto.getNickname()));
        user.setEmail(blankToNull(dto.getEmail()));
        user.setPhone(blankToNull(dto.getPhone()));
        user.setStatus(dto.getStatus() == null ? ENABLED : dto.getStatus());
        user.setDeleted(0);
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(400, "用户名已存在");
        }
        insertRoleLinks(user.getId(), roleIds);
    }

    @Override
    public void updateUser(Long id, UserUpdateDTO dto) {
        requireUser(id);
        if (dto.getStatus() == null) {
            throw new BusinessException(400, "状态不能为空");
        }
        userMapper.update(new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, id)
                .set(SysUser::getNickname, blankToNull(dto.getNickname()))
                .set(SysUser::getEmail, blankToNull(dto.getEmail()))
                .set(SysUser::getPhone, blankToNull(dto.getPhone()))
                .set(SysUser::getStatus, dto.getStatus()));
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(400, "用户 ID 不合法");
        }
        deleteUsers(List.of(id));
    }

    @Override
    @Transactional
    public void batchDeleteUsers(List<Long> ids) {
        deleteUsers(ids);
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
        requireUser(id);
        userMapper.update(new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, id)
                .set(SysUser::getPassword, passwordEncoder.encode(dto.getNewPassword())));
    }

    @Override
    @Transactional
    public void assignRoles(Long id, List<Long> roleIds) {
        requireUser(id);
        List<Long> normalized = normalizeRoleIds(roleIds);
        validateRoles(normalized);
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
        insertRoleLinks(id, normalized);
        evictPermissionsAfterCommit(id);
    }

    private void deleteUsers(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(400, "请选择要删除的用户");
        }
        if (ids.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new BusinessException(400, "用户 ID 不合法");
        }
        List<Long> distinctIds = ids.stream().distinct().toList();
        List<SysUser> users = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .select(SysUser::getId)
                .in(SysUser::getId, distinctIds));
        if (users.size() != distinctIds.size()) {
            throw new BusinessException(404, "用户不存在");
        }
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().in(SysUserRole::getUserId, distinctIds));
        userMapper.deleteBatchIds(distinctIds);
        distinctIds.forEach(this::evictPermissionsAfterCommit);
    }

    private SysUser requireUser(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(400, "用户 ID 不合法");
        }
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .select(SysUser::getId)
                .eq(SysUser::getId, id));
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return user;
    }

    private static LambdaQueryWrapper<SysUser> userViewQuery() {
        return new LambdaQueryWrapper<SysUser>()
                .select(SysUser::getId, SysUser::getUsername, SysUser::getNickname, SysUser::getEmail,
                        SysUser::getPhone, SysUser::getAvatar, SysUser::getStatus,
                        SysUser::getCreateTime, SysUser::getRemark);
    }

    private void validateRoles(List<Long> roleIds) {
        if (roleIds.isEmpty()) {
            return;
        }
        long existing = roleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                .in(SysRole::getId, roleIds));
        if (existing != roleIds.size()) {
            throw new BusinessException(400, "角色不存在或已删除");
        }
    }

    private void insertRoleLinks(Long userId, List<Long> roleIds) {
        for (Long roleId : roleIds) {
            SysUserRole link = new SysUserRole();
            link.setUserId(userId);
            link.setRoleId(roleId);
            userRoleMapper.insert(link);
        }
    }

    private List<UserVO> toVOs(List<SysUser> users) {
        if (users.isEmpty()) {
            return List.of();
        }
        List<Long> userIds = users.stream().map(SysUser::getId).toList();
        Map<Long, List<Long>> roleIdsByUser = userRoleMapper.selectList(
                        new LambdaQueryWrapper<SysUserRole>().in(SysUserRole::getUserId, userIds))
                .stream().collect(Collectors.groupingBy(SysUserRole::getUserId,
                        Collectors.mapping(SysUserRole::getRoleId, Collectors.toList())));
        List<Long> allRoleIds = roleIdsByUser.values().stream().flatMap(List::stream).distinct().toList();
        Map<Long, SysRole> rolesById = allRoleIds.isEmpty() ? Map.of()
                : roleMapper.selectBatchIds(allRoleIds).stream().collect(Collectors.toMap(SysRole::getId, Function.identity()));
        return users.stream().map(user -> {
            UserVO vo = baseVO(user);
            List<SysRole> roles = roleIdsByUser.getOrDefault(user.getId(), List.of()).stream()
                    .map(rolesById::get).filter(role -> role != null).toList();
            vo.setRoleIds(roles.stream().map(SysRole::getId).toList());
            vo.setRoles(roles.stream().map(SysRole::getRoleCode).toList());
            vo.setRoleNames(roles.stream().map(SysRole::getRoleName).toList());
            return vo;
        }).toList();
    }

    private UserVO toVO(SysUser user) {
        List<SysUserRole> links = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, user.getId()));
        List<Long> roleIds = links.stream().map(SysUserRole::getRoleId).distinct().toList();
        List<SysRole> roles = roleIds.isEmpty() ? List.of() : roleMapper.selectBatchIds(roleIds);
        UserVO vo = baseVO(user);
        vo.setRoleIds(roles.stream().map(SysRole::getId).toList());
        vo.setRoles(roles.stream().map(SysRole::getRoleCode).toList());
        vo.setRoleNames(roles.stream().map(SysRole::getRoleName).toList());
        return vo;
    }

    private static UserVO baseVO(SysUser user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setAvatar(user.getAvatar());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        vo.setRemark(user.getRemark());
        return vo;
    }

    private static List<Long> normalizeRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }
        if (roleIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new BusinessException(400, "角色 ID 不合法");
        }
        return new ArrayList<>(new LinkedHashSet<>(roleIds));
    }

    private static String blankToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void evictPermissionsAfterCommit(Long userId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            redisAuthStore.evictPermissions(userId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                redisAuthStore.evictPermissions(userId);
            }
        });
    }
}
