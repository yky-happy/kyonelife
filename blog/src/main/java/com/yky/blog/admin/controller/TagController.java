package com.yky.blog.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yky.blog.admin.dto.TagSaveDTO;
import com.yky.blog.admin.service.TagService;
import com.yky.blog.admin.vo.TagVO;
import com.yky.blog.common.annotation.OperationLogRecord;
import com.yky.blog.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@io.swagger.v3.oas.annotations.tags.Tag(name = "标签管理")
@RestController
@RequestMapping("/admin/tag")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @Operation(summary = "分页查询标签")
    @GetMapping("/page")
    public Result<IPage<TagVO>> page(@RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "10") int size,
                                     @RequestParam(required = false) String keyword) {
        return Result.success(tagService.pageTag(page, size, keyword));
    }

    @Operation(summary = "新增标签")
    @OperationLogRecord(module = "标签管理", operation = "新增标签")
    @PostMapping
    public Result<Long> save(@Valid @RequestBody TagSaveDTO dto) {
        return Result.success(tagService.saveTag(dto));
    }

    @Operation(summary = "编辑标签")
    @OperationLogRecord(module = "标签管理", operation = "编辑标签")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody TagSaveDTO dto) {
        tagService.updateTag(id, dto);
        return Result.success();
    }

    @Operation(summary = "删除标签")
    @OperationLogRecord(module = "标签管理", operation = "删除标签")
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        tagService.removeTag(id);
        return Result.success();
    }
}
