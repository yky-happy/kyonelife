package com.yky.blog.api.controller;

import com.yky.blog.api.dto.CommentAddDTO;
import com.yky.blog.api.dto.CommentVO;
import com.yky.blog.api.dto.LikeDTO;
import com.yky.blog.api.dto.LikeStatusVO;
import com.yky.blog.api.service.CommentService;
import com.yky.blog.common.annotation.RateLimit;
import com.yky.blog.common.result.Result;
import com.yky.blog.common.satoken.StpUserUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "前台评论")
@RestController
@RequestMapping("/api/comment")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "文章评论列表（公开，顶级分页 + 嵌套回复与点赞）")
    @GetMapping("/list")
    public Result<IPage<CommentVO>> list(@RequestParam Long articleId,
                                         @RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "10") int size,
                                         @RequestParam(required = false) String visitorId) {
        return Result.success(commentService.list(articleId, page, size, visitorId));
    }

    @Operation(summary = "发表评论 / 回复（需读者登录）")
    @RateLimit(name = "comment-add", window = 60, limit = 10, message = "评论过于频繁，请稍后再试")
    @PostMapping
    public Result<CommentVO> add(@Valid @RequestBody CommentAddDTO dto) {
        StpUserUtil.checkLogin();
        long userId = StpUserUtil.getLoginIdAsLong();
        return Result.success(commentService.add(dto.getArticleId(), dto.getParentId(), userId, dto.getContent()));
    }

    @Operation(summary = "评论点赞/取消（任何访客均可）")
    @RateLimit(name = "comment-like", window = 10, limit = 30, message = "操作过于频繁，请稍后再试")
    @PostMapping("/{id}/like")
    public Result<LikeStatusVO> like(@PathVariable Long id, @RequestBody LikeDTO dto) {
        return Result.success(commentService.toggleLike(id, dto.getVisitorId()));
    }
}
