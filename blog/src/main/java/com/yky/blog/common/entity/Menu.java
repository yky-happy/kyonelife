package com.yky.blog.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("menu")
public class Menu extends BaseEntity {

    @TableId(type = IdType.AUTO)
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
}
