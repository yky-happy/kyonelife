package com.yky.blog.api.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yky.blog.api.dto.ArchiveMonthVO;
import com.yky.blog.api.dto.ArticleCardVO;
import com.yky.blog.api.dto.ArticleWebDetailVO;
import com.yky.blog.api.dto.LikeDTO;
import com.yky.blog.api.dto.LikeStatusVO;
import com.yky.blog.api.service.ArticleApiService;
import com.yky.blog.api.service.ArticleLikeService;
import com.yky.blog.common.annotation.RateLimit;
import com.yky.blog.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "前台文章")
@RestController
@RequestMapping("/api/article")
@RequiredArgsConstructor
public class ArticleApiController {

    private final ArticleApiService articleApiService;
    private final ArticleLikeService articleLikeService;

    @Operation(summary = "前台文章分页")
    @GetMapping("/page")
    public Result<IPage<ArticleCardVO>> page(@RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "10") int size,
                                             @RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) Long tagId,
                                             @RequestParam(required = false) Long collectionId) {
        return Result.success(articleApiService.pageArticle(page, size, keyword, tagId, collectionId));
    }

    @Operation(summary = "前台文章详情")
    @GetMapping("/{id}")
    public Result<ArticleWebDetailVO> detail(@PathVariable Long id) {
        return Result.success(articleApiService.getArticleDetail(id));
    }

    @Operation(summary = "文章归档")
    @GetMapping("/archive")
    public Result<List<ArchiveMonthVO>> archive() {
        return Result.success(articleApiService.listArchive());
    }

    @Operation(summary = "热门文章（按阅读量）")
    @GetMapping("/hot")
    public Result<List<ArticleCardVO>> hot(@RequestParam(defaultValue = "6") int limit) {
        return Result.success(articleApiService.listHot(limit));
    }

    @Operation(summary = "相关文章推荐（按共同标签）")
    @GetMapping("/{id}/related")
    public Result<List<ArticleCardVO>> related(@PathVariable Long id,
                                               @RequestParam(defaultValue = "6") int limit) {
        return Result.success(articleApiService.listRelated(id, limit));
    }

    @Operation(summary = "查询点赞状态（当前访客是否已赞 + 点赞数）")
    @GetMapping("/{id}/like-status")
    public Result<LikeStatusVO> likeStatus(@PathVariable Long id,
                                           @RequestParam(required = false) String visitorId) {
        return Result.success(articleLikeService.getStatus(id, visitorId));
    }

    @Operation(summary = "点赞/取消点赞（toggle）")
    @RateLimit(name = "article-like", window = 10, limit = 20)
    @PostMapping("/{id}/like")
    public Result<LikeStatusVO> toggleLike(@PathVariable Long id, @Valid @RequestBody LikeDTO dto) {
        return Result.success(articleLikeService.toggle(id, dto.getVisitorId()));
    }
}

