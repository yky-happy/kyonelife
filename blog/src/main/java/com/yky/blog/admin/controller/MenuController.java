package com.yky.blog.admin.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.yky.blog.admin.dto.MenuSaveDTO;
import com.yky.blog.admin.service.MenuManageService;
import com.yky.blog.admin.vo.MenuTreeVO;
import com.yky.blog.common.annotation.OperationLogRecord;
import com.yky.blog.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "菜单管理")
@RestController
@RequestMapping("/admin/menu")
@RequiredArgsConstructor
public class MenuController {

    private final MenuManageService menuManageService;

    @Operation(summary = "菜单树")
    @SaCheckPermission("menu:list")
    @GetMapping("/tree")
    public Result<List<MenuTreeVO>> tree() {
        return Result.success(menuManageService.tree());
    }

    @Operation(summary = "新增菜单")
    @SaCheckPermission("menu:add")
    @OperationLogRecord(module = "菜单管理", operation = "新增菜单")
    @PostMapping
    public Result<Long> save(@Valid @RequestBody MenuSaveDTO dto) {
        return Result.success(menuManageService.saveMenu(dto));
    }

    @Operation(summary = "编辑菜单")
    @SaCheckPermission("menu:edit")
    @OperationLogRecord(module = "菜单管理", operation = "编辑菜单")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody MenuSaveDTO dto) {
        menuManageService.updateMenu(id, dto);
        return Result.success();
    }

    @Operation(summary = "删除菜单")
    @SaCheckPermission("menu:delete")
    @OperationLogRecord(module = "菜单管理", operation = "删除菜单")
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        menuManageService.removeMenu(id);
        return Result.success();
    }
}
