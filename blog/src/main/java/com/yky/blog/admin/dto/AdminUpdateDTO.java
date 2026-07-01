package com.yky.blog.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "编辑管理员请求")
public class AdminUpdateDTO {

    @NotBlank(message = "昵称不能为空")
    private String nickname;

    @Schema(description = "新密码，留空表示不修改")
    private String password;

    @Schema(description = "分配的角色ID列表")
    private List<Long> roleIds;
}
