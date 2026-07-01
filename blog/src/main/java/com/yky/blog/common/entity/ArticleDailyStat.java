package com.yky.blog.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("article_daily_stat")
public class ArticleDailyStat {

    @TableId(type = IdType.AUTO)
    private Long id;
    private LocalDate statDate;
    private Long articleId;
    private Long viewCount;
    private Long visitorCount;
    private Long durationTotal;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
