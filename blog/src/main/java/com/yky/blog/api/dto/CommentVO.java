package com.yky.blog.api.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CommentVO {
    private Long id;
    private Long parentId;
    private String content;
    private Long userId;
    private String nickname;
    private String avatar;
    private LocalDateTime createTime;
    private int likeCount;
    private boolean liked;
    /** 回复列表（仅顶级评论携带） */
    private List<CommentVO> replies;
}
