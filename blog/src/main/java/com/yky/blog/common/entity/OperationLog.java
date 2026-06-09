package com.yky.blog.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("operation_log")
public class OperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long adminId;
    private String adminName;
    private String module;
    private String operation;
    private String requestMethod;
    private String requestPath;
    private String requestParams;
    private Integer responseCode;
    private String responseMessage;
    private String ip;
    private String userAgent;
    private Long costTime;
    private Integer success;
    private String errorMessage;
    private LocalDateTime createTime;
}
