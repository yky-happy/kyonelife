package com.yky.blog.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "管理员登录响应")
public class LoginVO {

    @Schema(description = "Token")
    private String token;

    @Schema(description = "管理员ID")
    private Long id;

    @Schema(description = "昵称")
    private String nickname;
}
