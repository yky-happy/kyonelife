package com.yky.blog.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "菜单保存请求")
public class MenuSaveDTO {

    @Schema(description = "父菜单ID，0=顶级")
    private Long parentId;

    @NotBlank(message = "菜单名称不能为空")
    private String title;

    @NotBlank(message = "类型不能为空：CATALOG/MENU/BUTTON")
    private String type;

    private String path;
    private String component;
    private String perm;
    private String icon;
    private Integer sort;
    private Integer hidden;
}
