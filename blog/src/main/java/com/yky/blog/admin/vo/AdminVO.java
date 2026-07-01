package com.yky.blog.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "管理员（含角色）")
public class AdminVO {

    private Long id;
    private String username;
    private String nickname;
    private Integer status;
    private LocalDateTime lastLoginTime;
    private LocalDateTime createTime;

    @Schema(description = "已分配角色ID")
    private List<Long> roleIds;

    @Schema(description = "已分配角色名称（展示用）")
    private List<String> roleNames;
}
