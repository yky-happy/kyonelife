package com.yky.blog.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserLoginDTO {
    /** 邮箱或账号 */
    @NotBlank(message = "请输入邮箱或账号")
    private String identifier;

    @NotBlank(message = "密码不能为空")
    private String password;
}
