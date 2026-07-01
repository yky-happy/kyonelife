package com.yky.blog.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yky.blog.admin.dto.RoleSaveDTO;
import com.yky.blog.admin.service.RoleManageService;
import com.yky.blog.auth.service.PermissionService;
import com.yky.blog.common.entity.AdminRole;
import com.yky.blog.common.entity.Role;
import com.yky.blog.common.entity.RoleMenu;
import com.yky.blog.common.exception.BizException;
import com.yky.blog.common.mapper.AdminRoleMapper;
import com.yky.blog.common.mapper.RoleMapper;
import com.yky.blog.common.mapper.RoleMenuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RoleManageServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleManageService {

    private final RoleMenuMapper roleMenuMapper;
    private final AdminRoleMapper adminRoleMapper;
    private final PermissionService permissionService;

    @Override
    public IPage<Role> pageRole(int page, int size, String keyword) {
        return page(new Page<>(page, size), new LambdaQueryWrapper<Role>()
                .and(StringUtils.hasText(keyword), w -> w
                        .like(Role::getName, keyword).or().like(Role::getCode, keyword))
                .orderByAsc(Role::getId));
    }

    @Override
    public List<Role> listAll() {
        return list(new LambdaQueryWrapper<Role>().orderByAsc(Role::getId));
    }

    @Override
    public Long saveRole(RoleSaveDTO dto) {
        checkCodeDuplicate(dto.getCode(), null);
        Role role = new Role();
        BeanUtils.copyProperties(dto, role);
        save(role);
        return role.getId();
    }

    @Override
    public void updateRole(Long id, RoleSaveDTO dto) {
        if (getById(id) == null) {
            throw new BizException("角色不存在");
        }
        checkCodeDuplicate(dto.getCode(), id);
        Role role = new Role();
        BeanUtils.copyProperties(dto, role);
        role.setId(id);
        updateById(role);
        permissionService.clearAllCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeRole(Long id) {
        long bound = adminRoleMapper.selectCount(new LambdaQueryWrapper<AdminRole>().eq(AdminRole::getRoleId, id));
        if (bound > 0) {
            throw new BizException("该角色已分配给管理员，无法删除");
        }
        removeById(id);
        roleMenuMapper.delete(new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, id));
        permissionService.clearAllCache();
    }

    @Override
    public List<Long> getMenuIds(Long roleId) {
        return roleMenuMapper.selectList(new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, roleId))
                .stream().map(RoleMenu::getMenuId).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignMenus(Long roleId, List<Long> menuIds) {
        if (getById(roleId) == null) {
            throw new BizException("角色不存在");
        }
        roleMenuMapper.delete(new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, roleId));
        if (!CollectionUtils.isEmpty(menuIds)) {
            Set<Long> distinct = new LinkedHashSet<>(menuIds);
            for (Long menuId : distinct) {
                RoleMenu rm = new RoleMenu();
                rm.setRoleId(roleId);
                rm.setMenuId(menuId);
                roleMenuMapper.insert(rm);
            }
        }
        permissionService.clearAllCache();
    }

    private void checkCodeDuplicate(String code, Long excludeId) {
        boolean exists = lambdaQuery()
                .eq(Role::getCode, code)
                .ne(excludeId != null, Role::getId, excludeId)
                .exists();
        if (exists) {
            throw new BizException("角色编码已存在");
        }
    }
}
