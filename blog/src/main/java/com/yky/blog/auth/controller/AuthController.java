package com.yky.blog.auth.controller;

import com.yky.blog.auth.dto.LoginDTO;
import com.yky.blog.auth.service.AdminService;
import com.yky.blog.auth.vo.AdminInfoVO;
import com.yky.blog.auth.vo.LoginVO;
import com.yky.blog.common.annotation.OperationLogRecord;
import com.yky.blog.common.annotation.RateLimit;
import com.yky.blog.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理员认证")
@RestController
@RequestMapping("/admin/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AdminService adminService;

    @Operation(summary = "管理员登录")
    @RateLimit(name = "login", window = 60, limit = 10, message = "登录过于频繁，请稍后再试")
    @OperationLogRecord(module = "认证管理", operation = "管理员登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(adminService.login(dto));
    }

    @Operation(summary = "管理员退出登录")
    @OperationLogRecord(module = "认证管理", operation = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout() {
        adminService.logout();
        return Result.success();
    }

    @Operation(summary = "获取当前登录管理员信息（角色、权限、菜单）")
    @GetMapping("/me")
    public Result<AdminInfoVO> me() {
        return Result.success(adminService.getCurrentInfo());
    }
}
