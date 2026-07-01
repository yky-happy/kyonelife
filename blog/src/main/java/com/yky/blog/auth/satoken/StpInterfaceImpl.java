package com.yky.blog.auth.satoken;

import cn.dev33.satoken.stp.StpInterface;
import com.yky.blog.auth.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Sa-Token 鉴权数据源实现。
 * Sa-Token 在执行 @SaCheckPermission / @SaCheckRole 时会回调本类，
 * 由此把数据库中的 RBAC 权限接入框架的权限校验体系。
 */
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final PermissionService permissionService;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return permissionService.listPermissions(loginId);
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return permissionService.listRoles(loginId);
    }
}
