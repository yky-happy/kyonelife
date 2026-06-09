package com.yky.blog.admin.vo;

import lombok.Data;

@Data
public class RuntimeLogSlowRequestVO {

    private String time;

    private String method;

    private String path;

    private Integer status;

    private Long costTime;

    private String ip;
}
