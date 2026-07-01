package com.yky.blog.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminUserVO {
    private Long id;
    private String account;
    private String email;
    private String nickname;
    private Integer status;
    private String ipLocation;
    private LocalDateTime lastLoginTime;
    private LocalDateTime createTime;
}
