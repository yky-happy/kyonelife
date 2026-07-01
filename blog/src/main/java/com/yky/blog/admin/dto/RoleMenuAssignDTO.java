package com.yky.blog.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "角色分配菜单权限请求")
public class RoleMenuAssignDTO {

    @Schema(description = "菜单ID列表")
    private List<Long> menuIds;
}
