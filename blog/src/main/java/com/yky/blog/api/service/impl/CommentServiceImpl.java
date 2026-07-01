package com.yky.blog.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yky.blog.api.dto.CommentVO;
import com.yky.blog.api.dto.LikeStatusVO;
import com.yky.blog.api.service.CommentService;
import com.yky.blog.common.entity.Comment;
import com.yky.blog.common.entity.CommentLike;
import com.yky.blog.common.entity.User;
import com.yky.blog.common.mapper.CommentLikeMapper;
import com.yky.blog.common.mapper.CommentMapper;
import com.yky.blog.common.mapper.UserMapper;
import com.yky.blog.common.util.SensitiveWordFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private static final int MAX_PAGE_SIZE = 50;

    private final CommentMapper commentMapper;
    private final CommentLikeMapper commentLikeMapper;
    private final UserMapper userMapper;
    private final SensitiveWordFilter sensitiveWordFilter;

    @Override
    public IPage<CommentVO> list(Long articleId, int page, int size, String visitorId) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        // 顶级评论分页
        Page<Comment> topPage = commentMapper.selectPage(new Page<>(Math.max(page, 1), safeSize),
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getArticleId, articleId)
                        .eq(Comment::getStatus, 1)
                        .isNull(Comment::getParentId)
                        .orderByDesc(Comment::getCreateTime));
        List<Comment> tops = topPage.getRecords();
        Page<CommentVO> result = new Page<>(topPage.getCurrent(), topPage.getSize(), topPage.getTotal());
        if (CollectionUtils.isEmpty(tops)) {
            result.setRecords(Collections.emptyList());
            return result;
        }

        List<Long> topIds = tops.stream().map(Comment::getId).toList();
        // 这些顶级评论下的回复
        List<Comment> replies = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getStatus, 1)
                .in(Comment::getParentId, topIds)
                .orderByAsc(Comment::getCreateTime));

        List<Comment> all = new ArrayList<>(tops);
        all.addAll(replies);

        // 用户
        Set<Long> userIds = all.stream().map(Comment::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        // 点赞
        List<Long> allIds = all.stream().map(Comment::getId).toList();
        List<CommentLike> likes = commentLikeMapper.selectList(new LambdaQueryWrapper<CommentLike>()
                .in(CommentLike::getCommentId, allIds));
        Map<Long, Long> countMap = likes.stream()
                .collect(Collectors.groupingBy(CommentLike::getCommentId, Collectors.counting()));
        Set<Long> likedByMe = new HashSet<>();
        if (StringUtils.hasText(visitorId)) {
            for (CommentLike l : likes) {
                if (visitorId.equals(l.getVisitorId())) {
                    likedByMe.add(l.getCommentId());
                }
            }
        }

        Map<Long, CommentVO> voMap = new LinkedHashMap<>();
        for (Comment c : all) {
            CommentVO vo = toVO(c, userMap.get(c.getUserId()));
            vo.setLikeCount(countMap.getOrDefault(c.getId(), 0L).intValue());
            vo.setLiked(likedByMe.contains(c.getId()));
            voMap.put(c.getId(), vo);
        }
        // 回复挂到父评论
        for (Comment r : replies) {
            CommentVO parent = voMap.get(r.getParentId());
            if (parent != null) {
                if (parent.getReplies() == null) {
                    parent.setReplies(new ArrayList<>());
                }
                parent.getReplies().add(voMap.get(r.getId()));
            }
        }
        // 按顶级评论分页顺序输出
        result.setRecords(tops.stream().map(c -> voMap.get(c.getId())).toList());
        return result;
    }

    @Override
    public CommentVO add(Long articleId, Long parentId, Long userId, String content) {
        Comment c = new Comment();
        c.setArticleId(articleId);
        c.setParentId(parentId);
        c.setUserId(userId);
        c.setContent(sensitiveWordFilter.filter(content));
        c.setStatus(1);
        c.setCreateTime(LocalDateTime.now());
        commentMapper.insert(c);
        CommentVO vo = toVO(c, userMapper.selectById(userId));
        vo.setLikeCount(0);
        vo.setLiked(false);
        return vo;
    }

    @Override
    public long count(Long articleId) {
        return commentMapper.selectCount(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getArticleId, articleId)
                .eq(Comment::getStatus, 1));
    }

    @Override
    public LikeStatusVO toggleLike(Long commentId, String visitorId) {
        if (!StringUtils.hasText(visitorId)) {
            long cnt = commentLikeMapper.selectCount(new LambdaQueryWrapper<CommentLike>()
                    .eq(CommentLike::getCommentId, commentId));
            return new LikeStatusVO(false, cnt);
        }
        CommentLike existing = commentLikeMapper.selectOne(new LambdaQueryWrapper<CommentLike>()
                .eq(CommentLike::getCommentId, commentId)
                .eq(CommentLike::getVisitorId, visitorId)
                .last("limit 1"));
        boolean liked;
        if (existing != null) {
            commentLikeMapper.deleteById(existing.getId());
            liked = false;
        } else {
            CommentLike cl = new CommentLike();
            cl.setCommentId(commentId);
            cl.setVisitorId(visitorId);
            cl.setCreateTime(LocalDateTime.now());
            try {
                commentLikeMapper.insert(cl);
            } catch (Exception ignore) {
                // 并发唯一冲突，按已点赞处理
            }
            liked = true;
        }
        long cnt = commentLikeMapper.selectCount(new LambdaQueryWrapper<CommentLike>()
                .eq(CommentLike::getCommentId, commentId));
        return new LikeStatusVO(liked, cnt);
    }

    private CommentVO toVO(Comment c, User u) {
        CommentVO vo = new CommentVO();
        vo.setId(c.getId());
        vo.setParentId(c.getParentId());
        vo.setContent(c.getContent());
        vo.setUserId(c.getUserId());
        vo.setCreateTime(c.getCreateTime());
        if (u != null) {
            vo.setNickname(StringUtils.hasText(u.getNickname()) ? u.getNickname() : u.getAccount());
            vo.setAvatar(u.getAvatar());
        } else {
            vo.setNickname("匿名用户");
        }
        return vo;
    }
}
