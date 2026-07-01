package com.yky.blog.admin.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yky.blog.admin.dto.BannerSaveDTO;
import com.yky.blog.admin.service.BannerService;
import com.yky.blog.common.annotation.OperationLogRecord;
import com.yky.blog.common.entity.Banner;
import com.yky.blog.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "轮播管理")
@RestController
@RequestMapping("/admin/banner")
@RequiredArgsConstructor
public class BannerController {

    private final BannerService bannerService;

    @Operation(summary = "分页查询轮播")
    @SaCheckPermission("banner:list")
    @GetMapping("/page")
    public Result<IPage<Banner>> page(@RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "10") int size) {
        return Result.success(bannerService.pageBanner(page, size));
    }

    @Operation(summary = "新增轮播")
    @SaCheckPermission("banner:add")
    @OperationLogRecord(module = "轮播管理", operation = "新增轮播")
    @PostMapping
    public Result<Long> save(@Valid @RequestBody BannerSaveDTO dto) {
        return Result.success(bannerService.saveBanner(dto));
    }

    @Operation(summary = "编辑轮播")
    @SaCheckPermission("banner:edit")
    @OperationLogRecord(module = "轮播管理", operation = "编辑轮播")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody BannerSaveDTO dto) {
        bannerService.updateBanner(id, dto);
        return Result.success();
    }

    @Operation(summary = "启用/禁用轮播")
    @SaCheckPermission("banner:edit")
    @PatchMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        bannerService.updateStatus(id, status);
        return Result.success();
    }

    @Operation(summary = "删除轮播")
    @SaCheckPermission("banner:delete")
    @OperationLogRecord(module = "轮播管理", operation = "删除轮播")
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        bannerService.removeBanner(id);
        return Result.success();
    }
}
