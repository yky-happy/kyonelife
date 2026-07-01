package com.yky.blog.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "文章点赞状态")
public class LikeStatusVO {

    @Schema(description = "当前访客是否已点赞")
    private boolean liked;

    @Schema(description = "文章点赞总数")
    private long likeCount;
}
