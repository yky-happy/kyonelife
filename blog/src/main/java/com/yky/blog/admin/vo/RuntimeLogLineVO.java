package com.yky.blog.admin.vo;

import lombok.Data;

@Data
public class RuntimeLogLineVO {

    private String time;

    private String level;

    private String thread;

    private String logger;

    private String message;

    private String raw;

    private String method;

    private String path;

    private Integer status;

    private Long costTime;

    private String ip;
}
