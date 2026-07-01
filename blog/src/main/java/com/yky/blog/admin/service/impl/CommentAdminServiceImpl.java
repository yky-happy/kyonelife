package com.yky.blog.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yky.blog.admin.vo.AdminCommentVO;
import com.yky.blog.admin.service.CommentAdminService;
import com.yky.blog.common.entity.Article;
import com.yky.blog.common.entity.Comment;
import com.yky.blog.common.entity.User;
import com.yky.blog.common.exception.BizException;
import com.yky.blog.common.mapper.ArticleMapper;
import com.yky.blog.common.mapper.CommentMapper;
import com.yky.blog.common.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentAdminServiceImpl implements CommentAdminService {

    private final CommentMapper commentMapper;
    private final UserMapper userMapper;
    private final ArticleMapper articleMapper;

    @Override
    public IPage<AdminCommentVO> page(int page, int size, Long articleId, String keyword) {
        Page<Comment> p = commentMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Comment>()
                        .eq(articleId != null, Comment::getArticleId, articleId)
                        .like(StringUtils.hasText(keyword), Comment::getContent, keyword)
                        .orderByDesc(Comment::getCreateTime));
        Page<AdminCommentVO> result = new Page<>(p.getCurrent(), p.getSize(), p.getTotal());
        List<Comment> records = p.getRecords();
        if (CollectionUtils.isEmpty(records)) {
            result.setRecords(Collections.emptyList());
            return result;
        }
        Set<Long> articleIds = records.stream().map(Comment::getArticleId).collect(Collectors.toSet());
        Map<Long, String> titleMap = articleMapper.selectBatchIds(articleIds).stream()
                .collect(Collectors.toMap(Article::getId, Article::getTitle));
        Set<Long> userIds = records.stream().map(Comment::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        result.setRecords(records.stream().map(c -> {
            AdminCommentVO vo = new AdminCommentVO();
            vo.setId(c.getId());
            vo.setArticleId(c.getArticleId());
            vo.setArticleTitle(titleMap.get(c.getArticleId()));
            vo.setParentId(c.getParentId());
            vo.setUserId(c.getUserId());
            User u = userMap.get(c.getUserId());
            vo.setNickname(u == null ? "匿名用户"
                    : (StringUtils.hasText(u.getNickname()) ? u.getNickname() : u.getAccount()));
            vo.setContent(c.getContent());
            vo.setStatus(c.getStatus());
            vo.setCreateTime(c.getCreateTime());
            return vo;
        }).toList());
        return result;
    }

    @Override
    public void delete(Long id) {
        commentMapper.deleteById(id);
        // 连带删除其回复
        commentMapper.delete(new LambdaQueryWrapper<Comment>().eq(Comment::getParentId, id));
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException("状态值不正确");
        }
        Comment c = new Comment();
        c.setId(id);
        c.setStatus(status);
        commentMapper.updateById(c);
    }
}
