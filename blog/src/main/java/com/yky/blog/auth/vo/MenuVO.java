package com.yky.blog.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "菜单树节点")
public class MenuVO {

    @Schema(description = "菜单ID")
    private Long id;

    @Schema(description = "父菜单ID，0 表示顶级")
    private Long parentId;

    @Schema(description = "菜单名称")
    private String title;

    @Schema(description = "类型：CATALOG=目录 MENU=菜单")
    private String type;

    @Schema(description = "前端路由路径")
    private String path;

    @Schema(description = "前端组件路径")
    private String component;

    @Schema(description = "菜单图标")
    private String icon;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "子菜单")
    private List<MenuVO> children = new ArrayList<>();
}
