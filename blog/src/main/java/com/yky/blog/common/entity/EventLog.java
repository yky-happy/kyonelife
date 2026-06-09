package com.yky.blog.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("event_log")
public class EventLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String eventType;
    private String visitorId;
    private Long articleId;
    private Long tagId;
    private Long collectionId;
    private String keyword;
    private String pageUrl;
    private String referrer;
    private String ip;
    private String userAgent;
    private String device;
    private String browser;
    private String os;
    private Long duration;
    private LocalDateTime createTime;
}
