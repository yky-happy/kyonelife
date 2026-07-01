package com.yky.blog.auth.service;

import com.yky.blog.auth.vo.MenuVO;

import java.util.List;

/**
 * RBAC 权限服务：提供权限标识、角色编码、可见菜单的查询，并对结果做 Redis 缓存。
 */
public interface PermissionService {

    /** 超级管理员角色编码，拥有全部权限。 */
    String ROLE_SUPER = "SUPER";

    /** 通配权限标识，命中任意权限校验。 */
    String PERM_WILDCARD = "*";

    /**
     * 获取管理员的权限标识列表（带缓存）。
     * 若拥有 SUPER 角色，返回 ["*"]，由 Sa-Token 通配匹配放行全部权限。
     */
    List<String> listPermissions(Object adminId);

    /**
     * 获取管理员的角色编码列表（带缓存）。
     */
    List<String> listRoles(Object adminId);

    /**
     * 获取管理员可见的菜单树（目录/菜单，不含按钮）。
     */
    List<MenuVO> listMenuTree(Object adminId);

    /**
     * 清除某管理员的权限/角色缓存（登录、角色变更后调用）。
     */
    void clearCache(Object adminId);

    /**
     * 清除全部管理员的权限/角色缓存（菜单、角色权限、角色分配等变更后调用）。
     */
    void clearAllCache();
}
