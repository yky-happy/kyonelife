package com.yky.blog.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentAddDTO {
    @NotNull(message = "文章ID不能为空")
    private Long articleId;

    /** 回复某条评论时传父评论ID，顶级评论为空 */
    private Long parentId;

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 1000, message = "评论内容过长")
    private String content;
}
