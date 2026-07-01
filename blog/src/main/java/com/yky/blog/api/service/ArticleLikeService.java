package com.yky.blog.api.service;

import com.yky.blog.api.dto.LikeStatusVO;

public interface ArticleLikeService {

    /** 点赞/取消点赞（toggle），返回最新状态与点赞数。 */
    LikeStatusVO toggle(Long articleId, String visitorId);

    /** 查询某访客对文章的点赞状态与点赞数。 */
    LikeStatusVO getStatus(Long articleId, String visitorId);
}
