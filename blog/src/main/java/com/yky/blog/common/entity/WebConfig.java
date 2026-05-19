package com.yky.blog.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("web_config")
public class WebConfig extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String siteName;
    private String logo;
    private String summary;
    private String author;
    private String authorAvatar;
    private String signature;
    private String github;
    private String email;
    private String aboutMe;
    private String icpNumber;
    private String bulletin;
    private Integer openComment;
}
