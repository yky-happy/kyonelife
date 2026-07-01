package com.yky.blog.auth.service.impl;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yky.blog.auth.service.PermissionService;
import com.yky.blog.auth.vo.MenuVO;
import com.yky.blog.common.entity.Menu;
import com.yky.blog.common.mapper.PermissionMapper;
import com.yky.blog.common.redis.RedisCacheService;
import com.yky.blog.common.redis.RedisKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    /** 权限/角色缓存有效期，权限不常变更，缓存 30 分钟即可。 */
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    private final PermissionMapper permissionMapper;
    private final RedisCacheService redisCacheService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public List<String> listPermissions(Object adminId) {
        // 超级管理员直接返回通配权限，避免逐条比对，也无需为其维护权限明细
        if (listRoles(adminId).contains(ROLE_SUPER)) {
            return List.of(PERM_WILDCARD);
        }
        return redisCacheService.getOrLoad(
                RedisKeys.adminPerms(adminId),
                stringListType(),
                CACHE_TTL,
                () -> permissionMapper.selectPermsByAdminId(toAdminId(adminId)));
    }

    @Override
    public List<String> listRoles(Object adminId) {
        return redisCacheService.getOrLoad(
                RedisKeys.adminRoles(adminId),
                stringListType(),
                CACHE_TTL,
                () -> permissionMapper.selectRoleCodesByAdminId(toAdminId(adminId)));
    }

    @Override
    public List<MenuVO> listMenuTree(Object adminId) {
        List<Menu> menus = permissionMapper.selectMenusByAdminId(toAdminId(adminId));
        return buildTree(menus);
    }

    @Override
    public void clearCache(Object adminId) {
        stringRedisTemplate.delete(RedisKeys.adminPerms(adminId));
        stringRedisTemplate.delete(RedisKeys.adminRoles(adminId));
    }

    @Override
    public void clearAllCache() {
        redisCacheService.evictByPrefix("kyonelife:rbac:");
    }

    /**
     * 将扁平菜单列表组装为树形结构。
     */
    private List<MenuVO> buildTree(List<Menu> menus) {
        Map<Long, MenuVO> idToNode = new LinkedHashMap<>();
        for (Menu menu : menus) {
            idToNode.put(menu.getId(), toVO(menu));
        }
        List<MenuVO> roots = new ArrayList<>();
        for (MenuVO node : idToNode.values()) {
            MenuVO parent = node.getParentId() == null ? null : idToNode.get(node.getParentId());
            if (parent != null) {
                parent.getChildren().add(node);
            } else {
                roots.add(node);
            }
        }
        return roots;
    }

    private MenuVO toVO(Menu menu) {
        MenuVO vo = new MenuVO();
        vo.setId(menu.getId());
        vo.setParentId(menu.getParentId());
        vo.setTitle(menu.getTitle());
        vo.setType(menu.getType());
        vo.setPath(menu.getPath());
        vo.setComponent(menu.getComponent());
        vo.setIcon(menu.getIcon());
        vo.setSort(menu.getSort());
        return vo;
    }

    private Long toAdminId(Object adminId) {
        return Long.valueOf(String.valueOf(adminId));
    }

    private JavaType stringListType() {
        return objectMapper.getTypeFactory().constructCollectionType(List.class, String.class);
    }
}
