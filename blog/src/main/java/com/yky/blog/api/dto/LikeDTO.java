package com.yky.blog.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "点赞请求")
public class LikeDTO {

    @NotBlank(message = "访客标识不能为空")
    @Size(max = 100, message = "访客标识长度不能超过100")
    @Schema(description = "访客标识 visitorId")
    private String visitorId;
}
