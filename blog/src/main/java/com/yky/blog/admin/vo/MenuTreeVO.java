package com.yky.blog.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "菜单树节点（管理用，含全部字段）")
public class MenuTreeVO {

    private Long id;
    private Long parentId;
    private String title;
    private String type;
    private String path;
    private String component;
    private String perm;
    private String icon;
    private Integer sort;
    private Integer hidden;
    private List<MenuTreeVO> children = new ArrayList<>();
}
