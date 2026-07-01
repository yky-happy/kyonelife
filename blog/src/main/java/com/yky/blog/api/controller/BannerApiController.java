package com.yky.blog.api.controller;

import com.yky.blog.admin.service.BannerService;
import com.yky.blog.common.entity.Banner;
import com.yky.blog.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "前台轮播")
@RestController
@RequestMapping("/api/banner")
@RequiredArgsConstructor
public class BannerApiController {

    private final BannerService bannerService;

    @Operation(summary = "前台首页启用的轮播列表")
    @GetMapping("/list")
    public Result<List<Banner>> list() {
        return Result.success(bannerService.listEnabled());
    }
}
