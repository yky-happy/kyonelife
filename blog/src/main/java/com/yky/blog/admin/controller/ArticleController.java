package com.yky.blog.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yky.blog.admin.dto.ArticleSaveDTO;
import com.yky.blog.admin.dto.ArticleStatusDTO;
import com.yky.blog.admin.service.ArticleService;
import com.yky.blog.admin.vo.ArticleDetailVO;
import com.yky.blog.admin.vo.ArticleVO;
import com.yky.blog.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "文章管理")
@RestController
@RequestMapping("/admin/article")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    @Operation(summary = "分页查询文章")
    @GetMapping("/page")
    public Result<IPage<ArticleVO>> page(@RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "10") int size,
                                         @RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) Integer status) {
        return Result.success(articleService.pageArticle(page, size, keyword, status));
    }

    @Operation(summary = "查询文章详情")
    @GetMapping("/{id}")
    public Result<ArticleDetailVO> detail(@PathVariable Long id) {
        return Result.success(articleService.getArticleDetail(id));
    }

    @Operation(summary = "新增文章")
    @PostMapping
    public Result<Long> save(@Valid @RequestBody ArticleSaveDTO dto) {
        return Result.success(articleService.saveArticle(dto));
    }

    @Operation(summary = "编辑文章")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ArticleSaveDTO dto) {
        articleService.updateArticle(id, dto);
        return Result.success();
    }

    @Operation(summary = "更新文章状态")
    @PatchMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody ArticleStatusDTO dto) {
        articleService.updateArticleStatus(id, dto.getStatus());
        return Result.success();
    }

    @Operation(summary = "删除文章")
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        articleService.removeArticle(id);
        return Result.success();
    }
}
