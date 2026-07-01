package com.yky.blog.admin.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yky.blog.admin.vo.DashboardStatsVO;
import com.yky.blog.common.entity.Article;
import com.yky.blog.common.mapper.ArticleMapper;
import com.yky.blog.common.mapper.CollectionMapper;
import com.yky.blog.common.mapper.TagMapper;
import com.yky.blog.common.mapper.UserMapper;
import com.yky.blog.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "仪表盘")
@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final ArticleMapper articleMapper;
    private final TagMapper tagMapper;
    private final CollectionMapper collectionMapper;
    private final UserMapper userMapper;

    @Operation(summary = "仪表盘统计概览")
    @SaCheckPermission("dashboard:list")
    @GetMapping("/stats")
    public Result<DashboardStatsVO> stats() {
        long published = articleMapper.selectCount(new LambdaQueryWrapper<Article>().eq(Article::getStatus, 1));
        long draft = articleMapper.selectCount(new LambdaQueryWrapper<Article>().eq(Article::getStatus, 0));
        return Result.success(DashboardStatsVO.builder()
                .articleCount(articleMapper.selectCount(null))
                .publishedCount(published)
                .draftCount(draft)
                .tagCount(tagMapper.selectCount(null))
                .collectionCount(collectionMapper.selectCount(null))
                .userCount(userMapper.selectCount(null))
                .totalViews(articleMapper.selectTotalViews())
                .build());
    }
}
