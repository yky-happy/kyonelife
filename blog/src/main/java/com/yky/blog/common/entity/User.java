package com.yky.blog.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user")
public class User extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String account;
    private String phone;
    private String email;
    private String password;
    private String nickname;
    private String avatar;
    private Integer status;
    private String ip;
    private String ipLocation;
    private String browser;
    private String os;
    private LocalDateTime lastLoginTime;
}
