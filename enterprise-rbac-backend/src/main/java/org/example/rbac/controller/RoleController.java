package org.example.rbac.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.rbac.common.result.PageResult;
import org.example.rbac.common.result.Result;
import org.example.rbac.dto.role.AssignMenusDTO;
import org.example.rbac.dto.role.RoleCreateDTO;
import org.example.rbac.dto.role.RoleQueryDTO;
import org.example.rbac.dto.role.RoleUpdateDTO;
import org.example.rbac.service.RoleService;
import org.example.rbac.vo.role.RolePermissionVO;
import org.example.rbac.vo.role.RoleVO;
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

import java.util.List;

/**
 * /api/roles。角色主数据对应 sys_role；分配权限的正文是 menuIds，不是权限字符串。
 * Controller 把 menuIds 交给 RoleService 维护 sys_role_menu。下拉 /list 只要求已登录。
 */
@Tag(name = "角色管理")
@Validated
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @Operation(summary = "获取角色列表")
    @GetMapping
    @PreAuthorize("hasAuthority('system:role:list')")
    public Result<PageResult<RoleVO>> list(@Valid RoleQueryDTO query) {
        return Result.query(roleService.pageList(query));
    }

    /** 已登录管理端表单需要加载角色选项；实际角色变更仍由带权限校验的分配接口执行。 */
    @Operation(summary = "获取所有角色")
    @GetMapping("/list")
    public Result<List<RoleVO>> listAll() {
        return Result.query(roleService.listAll());
    }

    @Operation(summary = "获取角色已分配的权限")
    @GetMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('system:role:query')")
    public Result<RolePermissionVO> listPermissions(@PathVariable("id") Long id) {
        RolePermissionVO vo = new RolePermissionVO();
        vo.setMenuIds(roleService.listMenuIds(id));
        return Result.query(vo);
    }

    @Operation(summary = "给角色分配权限")
    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('system:role:assign')")
    public Result<Void> assignPermissions(@PathVariable("id") Long id, @Valid @RequestBody AssignMenusDTO dto) {
        roleService.assignMenus(id, dto.getMenuIds());
        return Result.success();
    }

    @Operation(summary = "获取角色详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:query')")
    public Result<RoleVO> getById(@PathVariable("id") Long id) {
        return Result.query(roleService.getRoleById(id));
    }

    @Operation(summary = "创建角色")
    @PostMapping
    @PreAuthorize("hasAuthority('system:role:add')")
    public Result<Void> create(@Valid @RequestBody RoleCreateDTO dto) {
        roleService.createRole(dto);
        return Result.success();
    }

    @Operation(summary = "更新角色")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:edit')")
    public Result<Void> update(@PathVariable("id") Long id, @Valid @RequestBody RoleUpdateDTO dto) {
        roleService.updateRole(id, dto);
        return Result.success();
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:delete')")
    public Result<Void> delete(@PathVariable("id") Long id) {
        roleService.deleteRole(id);
        return Result.success();
    }
}
