package com.yky.blog.common.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("article_image")
public class ArticleImage {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long articleId;
    private String url;
    private Integer sort;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
