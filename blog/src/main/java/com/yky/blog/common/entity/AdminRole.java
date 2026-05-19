package com.yky.blog.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("admin_role")
public class AdminRole {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long adminId;
    private Long roleId;
}
