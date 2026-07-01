package com.yky.blog.api.controller;

import com.yky.blog.api.dto.*;
import com.yky.blog.api.service.UserAuthService;
import com.yky.blog.common.annotation.RateLimit;
import com.yky.blog.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "前台读者认证")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserAuthController {

    private final UserAuthService userAuthService;

    @Operation(summary = "发送邮箱验证码（注册）")
    @RateLimit(name = "email-code", window = 60, limit = 5, message = "操作过于频繁，请稍后再试")
    @PostMapping("/email/code")
    public Result<Void> sendCode(@Valid @RequestBody EmailCodeDTO dto) {
        userAuthService.sendEmailCode(dto.getEmail());
        return Result.success();
    }

    @Operation(summary = "注册")
    @RateLimit(name = "user-register", window = 60, limit = 10, message = "操作过于频繁，请稍后再试")
    @PostMapping("/register")
    public Result<AuthVO> register(@Valid @RequestBody RegisterDTO dto) {
        return Result.success(userAuthService.register(dto));
    }

    @Operation(summary = "登录（邮箱或账号 + 密码）")
    @RateLimit(name = "user-login", window = 60, limit = 10, message = "登录过于频繁，请稍后再试")
    @PostMapping("/login")
    public Result<AuthVO> login(@Valid @RequestBody UserLoginDTO dto) {
        return Result.success(userAuthService.login(dto.getIdentifier(), dto.getPassword()));
    }

    @Operation(summary = "忘记密码：邮箱验证码重置")
    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@Valid @RequestBody RegisterDTO dto) {
        userAuthService.resetPassword(dto.getEmail(), dto.getCode(), dto.getPassword());
        return Result.success();
    }

    @Operation(summary = "当前登录读者")
    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.success(userAuthService.currentUser());
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout() {
        userAuthService.logout();
        return Result.success();
    }
}
