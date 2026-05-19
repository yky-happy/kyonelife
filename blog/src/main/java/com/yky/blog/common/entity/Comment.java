package com.yky.blog.common.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("comment")
public class Comment {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long articleId;
    private Long userId;
    private String content;
    private Long parentId;
    private Long replyUserId;
    private String ip;
    private String ipLocation;
    private String browser;
    private String os;
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
