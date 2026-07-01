package com.yky.blog.common.mapper;

import com.yky.blog.common.entity.Menu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * RBAC 权限查询 Mapper。
 * 通过 admin -> admin_role -> role -> role_menu -> menu 关联链
 * 查询某个管理员拥有的权限标识、角色编码与可见菜单。
 */
@Mapper
public interface PermissionMapper {

    /**
     * 查询管理员拥有的全部权限标识（menu.perm，如 article:add）。
     */
    @Select("""
            SELECT DISTINCT m.perm
            FROM admin_role ar
            JOIN role_menu rm ON ar.role_id = rm.role_id
            JOIN menu m ON rm.menu_id = m.id
            WHERE ar.admin_id = #{adminId}
              AND m.perm IS NOT NULL
              AND m.perm <> ''
            """)
    List<String> selectPermsByAdminId(Long adminId);

    /**
     * 查询管理员拥有的全部角色编码（role.code，如 SUPER / REGULAR）。
     */
    @Select("""
            SELECT DISTINCT r.code
            FROM admin_role ar
            JOIN role r ON ar.role_id = r.id
            WHERE ar.admin_id = #{adminId}
            """)
    List<String> selectRoleCodesByAdminId(Long adminId);

    /**
     * 查询管理员可见的目录/菜单（不含按钮），用于前端动态渲染左侧菜单树。
     */
    @Select("""
            SELECT DISTINCT m.*
            FROM admin_role ar
            JOIN role_menu rm ON ar.role_id = rm.role_id
            JOIN menu m ON rm.menu_id = m.id
            WHERE ar.admin_id = #{adminId}
              AND m.type IN ('CATALOG', 'MENU')
              AND m.hidden = 0
            ORDER BY m.sort
            """)
    List<Menu> selectMenusByAdminId(Long adminId);
}
