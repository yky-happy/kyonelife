package com.yky.blog.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("search_keyword_stat")
public class SearchKeywordStat {

    @TableId(type = IdType.AUTO)
    private Long id;
    private LocalDate statDate;
    private String keyword;
    private Long searchCount;
    private Long visitorCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
