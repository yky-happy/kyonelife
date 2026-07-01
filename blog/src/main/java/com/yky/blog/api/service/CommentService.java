package com.yky.blog.api.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yky.blog.api.dto.CommentVO;
import com.yky.blog.api.dto.LikeStatusVO;

public interface CommentService {

    /** 文章评论：顶级评论分页 + 每条携带其回复；visitorId 用于标记当前访客是否点赞 */
    IPage<CommentVO> list(Long articleId, int page, int size, String visitorId);

    CommentVO add(Long articleId, Long parentId, Long userId, String content);

    long count(Long articleId);

    /** 评论点赞/取消（任何访客均可，按 visitorId 去重） */
    LikeStatusVO toggleLike(Long commentId, String visitorId);
}
