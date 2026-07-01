package com.yky.blog.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("tag_daily_stat")
public class TagDailyStat {

    @TableId(type = IdType.AUTO)
    private Long id;
    private LocalDate statDate;
    private Long tagId;
    private Long clickCount;
    private Long visitorCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
