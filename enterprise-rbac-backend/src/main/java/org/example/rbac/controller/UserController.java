package org.example.rbac.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.rbac.common.result.PageResult;
import org.example.rbac.common.result.Result;
import org.example.rbac.dto.user.AssignRolesDTO;
import org.example.rbac.dto.user.BatchIdsDTO;
import org.example.rbac.dto.user.PasswordUpdateDTO;
import org.example.rbac.dto.user.ResetPasswordDTO;
import org.example.rbac.dto.user.UserCreateDTO;
import org.example.rbac.dto.user.UserQueryDTO;
import org.example.rbac.dto.user.UserUpdateDTO;
import org.example.rbac.service.UserService;
import org.example.rbac.vo.user.UserVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * /api/users。查询和大多数写操作要求 sys_menu 中的 system:user:*；修改自己的密码没有权限串。
 * 入参来自 dto.user，出参是 UserVO。分配角色只提交 roleIds，由 UserService 写入 sys_user_role。
 */
@Tag(name = "用户管理")
@Validated
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "获取用户列表")
    @GetMapping
    @PreAuthorize("hasAuthority('system:user:list')")
    public Result<PageResult<UserVO>> list(@Valid UserQueryDTO query) {
        return Result.query(userService.pageList(query));
    }

    @Operation(summary = "批量删除用户")
    @DeleteMapping("/batch")
    @PreAuthorize("hasAuthority('system:user:delete')")
    public Result<Void> batchDelete(@Valid @RequestBody BatchIdsDTO dto) {
        userService.batchDeleteUsers(dto.getIds());
        return Result.success();
    }

    @Operation(summary = "获取用户详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:query')")
    public Result<UserVO> getById(@PathVariable Long id) {
        return Result.query(userService.getUserById(id));
    }

    @Operation(summary = "创建用户")
    @PostMapping
    @PreAuthorize("hasAuthority('system:user:add')")
    public Result<Void> create(@Valid @RequestBody UserCreateDTO dto) {
        userService.createUser(dto);
        return Result.success();
    }

    @Operation(summary = "更新用户")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:edit')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UserUpdateDTO dto) {
        userService.updateUser(id, dto);
        return Result.success();
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success();
    }

    @Operation(summary = "修改自己的密码")
    @PutMapping("/{id}/password")
    public Result<Void> changePassword(@PathVariable Long id, @Valid @RequestBody PasswordUpdateDTO dto) {
        userService.changePassword(id, dto);
        return Result.success();
    }

    @Operation(summary = "重置用户密码")
    @PutMapping("/{id}/reset-password")
    @PreAuthorize("hasAuthority('system:user:resetPwd')")
    public Result<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordDTO dto) {
        userService.resetPassword(id, dto);
        return Result.success();
    }

    @Operation(summary = "给用户分配角色")
    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('system:user:role')")
    public Result<Void> assignRoles(@PathVariable Long id, @Valid @RequestBody AssignRolesDTO dto) {
        userService.assignRoles(id, dto.getRoleIds());
        return Result.success();
    }
}
