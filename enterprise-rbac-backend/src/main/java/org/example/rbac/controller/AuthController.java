package org.example.rbac.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.rbac.common.result.Result;
import org.example.rbac.config.OpenApiConfig;
import org.example.rbac.dto.auth.LoginDTO;
import org.example.rbac.dto.auth.RegisterDTO;
import org.example.rbac.service.AuthService;
import org.example.rbac.vo.auth.CurrentUserVO;
import org.example.rbac.vo.auth.LoginVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * /api/auth。登录、注册由 SecurityConfig 放行，退出、刷新和当前用户要求已认证。
 * 登录、注册同时用空的 SecurityRequirements 覆盖 OpenApiConfig 的全局 Bearer，调试这两个接口不必先 Authorize。
 * 其余接口沿用该 Bearer：Knife4j 把 LoginVO.token 写成 Authorization 请求头，JwtAuthenticationFilter 再还原登录态。
 * 这里只校验 DTO 并调用 AuthService，不访问 Mapper。出参是 LoginVO、CurrentUserVO，不返回 SysUser。
 */
@Tag(name = "认证授权")
@Validated
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 公开接口，不套用 OpenApiConfig 的全局 Bearer。 */
    @SecurityRequirements
    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success("登录成功", authService.login(dto));
    }

    /** 公开接口，不套用 OpenApiConfig 的全局 Bearer。 */
    @SecurityRequirements
    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDTO dto) {
        authService.register(dto);
        return Result.success("注册成功", null);
    }

    /** 需要 OpenApiConfig 的 Bearer。过滤器还原登录态后，AuthService 按 jti 写入 Redis 黑名单。 */
    @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME)
    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.success();
    }

    /** 需要已登录。旧令牌进入黑名单，响应里的新 token 仍是 LoginVO.token。 */
    @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME)
    @Operation(summary = "刷新 Token")
    @PostMapping("/refresh-token")
    public Result<LoginVO> refreshToken() {
        return Result.success(authService.refreshToken());
    }

    /** 需要已登录。当前用户来自 SecurityContext 里的 AuthUser，不是请求参数。 */
    @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME)
    @Operation(summary = "获取当前用户信息")
    @GetMapping("/user-info")
    public Result<CurrentUserVO> userInfo() {
        return Result.query(authService.currentUser());
    }
}
