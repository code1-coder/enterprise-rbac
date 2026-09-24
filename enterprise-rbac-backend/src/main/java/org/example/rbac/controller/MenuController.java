package org.example.rbac.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.rbac.common.result.Result;
import org.example.rbac.dto.menu.MenuCreateDTO;
import org.example.rbac.dto.menu.MenuUpdateDTO;
import org.example.rbac.service.MenuService;
import org.example.rbac.vo.menu.MenuTreeVO;
import org.example.rbac.vo.menu.MenuVO;
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
 * /api/menus。目录、菜单、按钮、接口都在 sys_menu，用 menu_type 区分。
 * 管理接口要求 system:menu:*；我的菜单和我的权限只要求已登录，再按当前用户的角色过滤。
 */
@Tag(name = "菜单权限管理")
@Validated
@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @Operation(summary = "获取菜单树")
    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('system:menu:list')")
    public Result<List<MenuTreeVO>> tree() {
        return Result.query(menuService.tree());
    }

    @Operation(summary = "获取当前用户的菜单")
    @GetMapping("/my-menus")
    public Result<List<MenuTreeVO>> myMenus() {
        return Result.query(menuService.myMenus());
    }

    @Operation(summary = "获取当前用户的按钮权限")
    @GetMapping("/my-permissions")
    public Result<List<String>> myPermissions() {
        return Result.query(menuService.myPermissions());
    }

    @Operation(summary = "获取菜单详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:menu:query')")
    public Result<MenuVO> getById(@PathVariable("id") Long id) {
        return Result.query(menuService.getMenuById(id));
    }

    @Operation(summary = "创建菜单")
    @PostMapping
    @PreAuthorize("hasAuthority('system:menu:add')")
    public Result<Void> create(@Valid @RequestBody MenuCreateDTO dto) {
        menuService.createMenu(dto);
        return Result.success();
    }

    @Operation(summary = "更新菜单")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:menu:edit')")
    public Result<Void> update(@PathVariable("id") Long id, @Valid @RequestBody MenuUpdateDTO dto) {
        menuService.updateMenu(id, dto);
        return Result.success();
    }

    @Operation(summary = "删除菜单")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:menu:delete')")
    public Result<Void> delete(@PathVariable("id") Long id) {
        menuService.deleteMenu(id);
        return Result.success();
    }
}
