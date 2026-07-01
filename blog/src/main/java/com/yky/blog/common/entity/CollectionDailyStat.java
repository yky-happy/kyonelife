package com.yky.blog.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("collection_daily_stat")
public class CollectionDailyStat {

    @TableId(type = IdType.AUTO)
    private Long id;
    private LocalDate statDate;
    private Long collectionId;
    private Long clickCount;
    private Long visitorCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
