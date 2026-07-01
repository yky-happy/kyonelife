package com.yky.blog.admin.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yky.blog.admin.service.UserAdminService;
import com.yky.blog.admin.vo.AdminUserVO;
import com.yky.blog.common.annotation.OperationLogRecord;
import com.yky.blog.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "后台读者用户管理")
@RestController
@RequestMapping("/admin/user")
@RequiredArgsConstructor
public class UserAdminController {

    private final UserAdminService userAdminService;

    @Operation(summary = "分页查询读者")
    @SaCheckPermission("user:list")
    @GetMapping("/page")
    public Result<IPage<AdminUserVO>> page(@RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) Integer status) {
        return Result.success(userAdminService.page(page, size, keyword, status));
    }

    @Operation(summary = "封禁/解封读者")
    @SaCheckPermission("user:edit")
    @OperationLogRecord(module = "读者管理", operation = "封禁/解封")
    @PatchMapping("/{id}/status")
    public Result<Void> status(@PathVariable Long id, @RequestParam Integer status) {
        userAdminService.updateStatus(id, status);
        return Result.success();
    }

    @Operation(summary = "删除读者")
    @SaCheckPermission("user:delete")
    @OperationLogRecord(module = "读者管理", operation = "删除读者")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userAdminService.delete(id);
        return Result.success();
    }
}
