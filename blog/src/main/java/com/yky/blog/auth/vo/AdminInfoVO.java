package com.yky.blog.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "当前登录管理员信息")
public class AdminInfoVO {

    @Schema(description = "管理员ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "角色编码列表，如 [SUPER]")
    private List<String> roles;

    @Schema(description = "权限标识列表，超级管理员为 [*]")
    private List<String> permissions;

    @Schema(description = "可见菜单树")
    private List<MenuVO> menus;
}
