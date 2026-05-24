package com.yky.blog.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yky.blog.admin.dto.CollectionSaveDTO;
import com.yky.blog.admin.service.CollectionService;
import com.yky.blog.admin.vo.CollectionVO;
import com.yky.blog.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "合集管理")
@RestController
@RequestMapping("/admin/collection")
@RequiredArgsConstructor
public class CollectionController {

    private final CollectionService collectionService;

    @Operation(summary = "分页查询合集")
    @GetMapping("/page")
    public Result<IPage<CollectionVO>> page(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(required = false) String keyword) {
        return Result.success(collectionService.pageCollection(page, size, keyword));
    }

    @Operation(summary = "新增合集")
    @PostMapping
    public Result<Long> save(@Valid @RequestBody CollectionSaveDTO dto) {
        return Result.success(collectionService.saveCollection(dto));
    }

    @Operation(summary = "编辑合集")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CollectionSaveDTO dto) {
        collectionService.updateCollection(id, dto);
        return Result.success();
    }

    @Operation(summary = "删除合集")
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        collectionService.removeCollection(id);
        return Result.success();
    }
}
