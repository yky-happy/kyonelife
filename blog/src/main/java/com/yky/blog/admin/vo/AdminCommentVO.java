package com.yky.blog.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminCommentVO {
    private Long id;
    private Long articleId;
    private String articleTitle;
    private Long parentId;
    private Long userId;
    private String nickname;
    private String content;
    private Integer status;
    private LocalDateTime createTime;
}
