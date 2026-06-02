package com.yky.blog.api.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yky.blog.api.dto.ArticleCardVO;
import com.yky.blog.api.dto.CollectionApiVO;
import com.yky.blog.api.service.CollectionApiService;
import com.yky.blog.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "前台合集")
@RestController
@RequestMapping("/api/collection")
@RequiredArgsConstructor
public class CollectionApiController {

    private final CollectionApiService collectionApiService;

    @Operation(summary = "前台合集列表")
    @GetMapping("/list")
    public Result<List<CollectionApiVO>> list() {
        return Result.success(collectionApiService.listCollections());
    }

    @Operation(summary = "合集下文章分页")
    @GetMapping("/{id}/articles")
    public Result<IPage<ArticleCardVO>> articles(@PathVariable Long id,
                                                 @RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "10") int size) {
        return Result.success(collectionApiService.pageCollectionArticles(id, page, size));
    }
}
