package com.yky.blog.admin.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yky.blog.admin.dto.AdminSaveDTO;
import com.yky.blog.admin.dto.AdminUpdateDTO;
import com.yky.blog.admin.service.AdminManageService;
import com.yky.blog.admin.vo.AdminVO;
import com.yky.blog.common.annotation.OperationLogRecord;
import com.yky.blog.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理员管理")
@RestController
@RequestMapping("/admin/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminManageService adminManageService;

    @Operation(summary = "分页查询管理员")
    @SaCheckPermission("admin:list")
    @GetMapping("/page")
    public Result<IPage<AdminVO>> page(@RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "10") int size,
                                       @RequestParam(required = false) String keyword) {
        return Result.success(adminManageService.pageAdmin(page, size, keyword));
    }

    @Operation(summary = "新增管理员")
    @SaCheckPermission("admin:add")
    @OperationLogRecord(module = "管理员管理", operation = "新增管理员")
    @PostMapping
    public Result<Long> save(@Valid @RequestBody AdminSaveDTO dto) {
        return Result.success(adminManageService.saveAdmin(dto));
    }

    @Operation(summary = "编辑管理员")
    @SaCheckPermission("admin:edit")
    @OperationLogRecord(module = "管理员管理", operation = "编辑管理员")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody AdminUpdateDTO dto) {
        adminManageService.updateAdmin(id, dto);
        return Result.success();
    }

    @Operation(summary = "启用/禁用管理员")
    @SaCheckPermission("admin:edit")
    @OperationLogRecord(module = "管理员管理", operation = "更新管理员状态")
    @PatchMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        adminManageService.updateStatus(id, status);
        return Result.success();
    }

    @Operation(summary = "删除管理员")
    @SaCheckPermission("admin:delete")
    @OperationLogRecord(module = "管理员管理", operation = "删除管理员")
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        adminManageService.removeAdmin(id);
        return Result.success();
    }
}
