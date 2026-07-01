package com.yky.blog.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("article")
public class Article extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String cover;
    private String summary;
    private String content;
    private String contentMd;
    private String keywords;
    private String aiDescribe;
    private Long collectionId;
    private Integer status;
    private Integer isStick;
    private Integer isCarousel;
    private Integer carouselSort;
    private Integer isOriginal;
    private String originalUrl;
    private Long viewCount;
    private Long likeCount;
    // IGNORED：updateById 时总是写该列（含 null），使"定时改立即"能清空 publish_time
    @com.baomidou.mybatisplus.annotation.TableField(updateStrategy = com.baomidou.mybatisplus.annotation.FieldStrategy.IGNORED)
    private java.time.LocalDateTime publishTime;
}
