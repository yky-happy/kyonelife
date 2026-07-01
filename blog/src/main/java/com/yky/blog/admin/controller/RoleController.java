package com.yky.blog.admin.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yky.blog.admin.dto.RoleMenuAssignDTO;
import com.yky.blog.admin.dto.RoleSaveDTO;
import com.yky.blog.admin.service.RoleManageService;
import com.yky.blog.common.annotation.OperationLogRecord;
import com.yky.blog.common.entity.Role;
import com.yky.blog.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "角色管理")
@RestController
@RequestMapping("/admin/role")
@RequiredArgsConstructor
public class RoleController {

    private final RoleManageService roleManageService;

    @Operation(summary = "分页查询角色")
    @SaCheckPermission("role:list")
    @GetMapping("/page")
    public Result<IPage<Role>> page(@RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "10") int size,
                                    @RequestParam(required = false) String keyword) {
        return Result.success(roleManageService.pageRole(page, size, keyword));
    }

    @Operation(summary = "全部角色（下拉用）")
    @SaCheckPermission("role:list")
    @GetMapping("/list")
    public Result<List<Role>> list() {
        return Result.success(roleManageService.listAll());
    }

    @Operation(summary = "新增角色")
    @SaCheckPermission("role:add")
    @OperationLogRecord(module = "角色管理", operation = "新增角色")
    @PostMapping
    public Result<Long> save(@Valid @RequestBody RoleSaveDTO dto) {
        return Result.success(roleManageService.saveRole(dto));
    }

    @Operation(summary = "编辑角色")
    @SaCheckPermission("role:edit")
    @OperationLogRecord(module = "角色管理", operation = "编辑角色")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody RoleSaveDTO dto) {
        roleManageService.updateRole(id, dto);
        return Result.success();
    }

    @Operation(summary = "删除角色")
    @SaCheckPermission("role:delete")
    @OperationLogRecord(module = "角色管理", operation = "删除角色")
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        roleManageService.removeRole(id);
        return Result.success();
    }

    @Operation(summary = "查询角色已分配的菜单ID")
    @SaCheckPermission("role:assign:menu")
    @GetMapping("/{id}/menus")
    public Result<List<Long>> menus(@PathVariable Long id) {
        return Result.success(roleManageService.getMenuIds(id));
    }

    @Operation(summary = "给角色分配菜单权限")
    @SaCheckPermission("role:assign:menu")
    @OperationLogRecord(module = "角色管理", operation = "分配角色权限")
    @PutMapping("/{id}/menus")
    public Result<Void> assignMenus(@PathVariable Long id, @RequestBody RoleMenuAssignDTO dto) {
        roleManageService.assignMenus(id, dto.getMenuIds());
        return Result.success();
    }
}
