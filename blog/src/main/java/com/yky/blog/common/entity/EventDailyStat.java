package com.yky.blog.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("event_daily_stat")
public class EventDailyStat {

    @TableId(type = IdType.AUTO)
    private Long id;
    private LocalDate statDate;
    private String eventType;
    private Long pv;
    private Long uv;
    private Long durationTotal;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
