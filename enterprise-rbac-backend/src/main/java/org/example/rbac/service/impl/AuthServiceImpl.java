
package org.example.rbac.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.example.rbac.common.exception.BusinessException;
import org.example.rbac.dto.auth.LoginDTO;
import org.example.rbac.dto.auth.RegisterDTO;
import org.example.rbac.entity.SysUser;
import org.example.rbac.mapper.SysUserMapper;
import org.example.rbac.security.AuthUser;
import org.example.rbac.security.IssuedToken;
import org.example.rbac.security.JwtTokenProvider;
import org.example.rbac.security.RedisAuthStore;
import org.example.rbac.security.SecuritySupport;
import org.example.rbac.service.AuthLookupService;
import org.example.rbac.service.AuthService;
import org.example.rbac.service.UserAccess;
import org.example.rbac.vo.auth.CurrentUserVO;
import org.example.rbac.vo.auth.LoginUserVO;
import org.example.rbac.vo.auth.LoginVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * 认证用例。登录校验 sys_user 的密码和状态，再经 AuthLookupService 组装角色与 permission，
 * 写入 Redis 权限缓存，并由 JwtTokenProvider 签发令牌。密码只参与 BCrypt 比对，不进入 LoginVO。
 * 之后的请求由 JwtAuthenticationFilter 还原登录态；刷新和退出按 jti 把旧令牌放进黑名单。
 * 注册把账号写入 sys_user，密码只存 BCrypt。这里不写 sys_user_role，也不签发令牌；
 * 登录时 AuthLookupService 找不到角色，roles 和 permissions 就都是空的。
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final int ENABLED = 1;
    private static final int IP_MAX_LENGTH = 50;
    /** 用户不存在时也走一次 BCrypt，避免按响应时间判断用户名是否存在。这不是登录口令。 */
    private static final String DUMMY_PASSWORD_HASH =
            "$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2";

    /** sys_user：登录成功后回写最后登录时间和 IP。密码校验用的用户由 AuthLookupService 读取。 */
    private final SysUserMapper userMapper;
    private final AuthLookupService authLookupService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisAuthStore redisAuthStore;

    @Override
    @Transactional
    public LoginVO login(LoginDTO dto) {
        SysUser user = authLookupService.findByUsername(dto.getUsername());
        if (user == null) {
            passwordEncoder.matches(dto.getPassword(), DUMMY_PASSWORD_HASH);
            throw new BusinessException(401, "用户名或密码错误");
        }
        if (user.getPassword() == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != ENABLED) {
            log.info("login_disabled userId={}", user.getId());
            throw new BusinessException(401, "账号已禁用");
        }
        userMapper.update(new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, user.getId())
                .set(SysUser::getLastLoginTime, LocalDateTime.now())
                .set(SysUser::getLastLoginIp, clientIp()));
        LoginVO login = issue(user);
        log.info("user_login userId={}", user.getId());
        return login;
    }

    @Override
    @Transactional
    public void register(RegisterDTO dto) {
        String username = dto.getUsername().trim();
        if (authLookupService.findByUsername(username) != null) {
            throw new BusinessException(400, "用户名已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(username);
        user.setEmail(blankToNull(dto.getEmail()));
        user.setPhone(blankToNull(dto.getPhone()));
        user.setStatus(ENABLED);
        user.setDeleted(0);
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(400, "用户名已存在");
        }
        log.info("user_register userId={}", user.getId());
    }

    @Override
    public void logout() {
        AuthUser current = SecuritySupport.requireUser();
        redisAuthStore.blacklist(current.tokenId(), current.expiresAt());
    }

    @Override
    public LoginVO refreshToken() {
        AuthUser current = SecuritySupport.requireUser();
        SysUser user = requireEnabled(current.userId());
        LoginVO login = issue(user);
        redisAuthStore.blacklist(current.tokenId(), current.expiresAt());
        return login;
    }

    @Override
    @Transactional(readOnly = true)
    public CurrentUserVO currentUser() {
        AuthUser current = SecuritySupport.requireUser();
        SysUser user = requireEnabled(current.userId());
        UserAccess access = authLookupService.loadAccess(user.getId());
        redisAuthStore.cachePermissions(user.getId(), access.permissions());
        CurrentUserVO vo = new CurrentUserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setRoles(access.roleCodes());
        vo.setRoleNames(access.roleNames());
        vo.setPermissions(access.permissions());
        return vo;
    }

    private LoginVO issue(SysUser user) {
        UserAccess access = authLookupService.loadAccess(user.getId());
        IssuedToken issued = jwtTokenProvider.issue(user.getId(), user.getUsername(), access.roleCodes());
        redisAuthStore.cachePermissions(user.getId(), access.permissions());
        LoginUserVO userInfo = new LoginUserVO();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setNickname(user.getNickname());
        userInfo.setRoles(access.roleCodes());
        userInfo.setPermissions(access.permissions());
        LoginVO vo = new LoginVO();
        vo.setToken(issued.token());
        vo.setTokenType(jwtTokenProvider.tokenType());
        vo.setExpiresIn(jwtTokenProvider.expiresInSeconds());
        vo.setUserInfo(userInfo);
        return vo;
    }

    private SysUser requireEnabled(Long userId) {
        SysUser user = authLookupService.findById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() != ENABLED) {
            throw new BusinessException(401, "未认证或登录已过期");
        }
        return user;
    }

    private static String clientIp() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            return null;
        }
        String ip = servletAttributes.getRequest().getRemoteAddr();
        if (ip != null && ip.length() > IP_MAX_LENGTH) {
            return ip.substring(0, IP_MAX_LENGTH);
        }
        return ip;
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
