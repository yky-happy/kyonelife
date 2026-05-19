package com.yky.blog.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("banner")
public class Banner extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String imageUrl;
    private String linkUrl;
    private String title;
    private Integer sort;
    private Integer status;
}
