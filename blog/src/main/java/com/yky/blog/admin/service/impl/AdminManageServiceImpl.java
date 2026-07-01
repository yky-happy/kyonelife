package com.yky.blog.admin.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yky.blog.admin.dto.AdminSaveDTO;
import com.yky.blog.admin.dto.AdminUpdateDTO;
import com.yky.blog.admin.service.AdminManageService;
import com.yky.blog.admin.vo.AdminVO;
import com.yky.blog.auth.service.PermissionService;
import com.yky.blog.common.entity.Admin;
import com.yky.blog.common.entity.AdminRole;
import com.yky.blog.common.entity.Role;
import com.yky.blog.common.exception.BizException;
import com.yky.blog.common.mapper.AdminMapper;
import com.yky.blog.common.mapper.AdminRoleMapper;
import com.yky.blog.common.mapper.RoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminManageServiceImpl extends ServiceImpl<AdminMapper, Admin> implements AdminManageService {

    private final AdminRoleMapper adminRoleMapper;
    private final RoleMapper roleMapper;
    private final PermissionService permissionService;

    @Override
    public IPage<AdminVO> pageAdmin(int page, int size, String keyword) {
        IPage<Admin> adminPage = page(new Page<>(page, size), new LambdaQueryWrapper<Admin>()
                .and(StringUtils.hasText(keyword), w -> w
                        .like(Admin::getUsername, keyword).or().like(Admin::getNickname, keyword))
                .orderByAsc(Admin::getId));

        List<AdminVO> records = adminPage.getRecords().stream().map(this::toVO).toList();
        fillRoles(records);

        Page<AdminVO> voPage = new Page<>(adminPage.getCurrent(), adminPage.getSize(), adminPage.getTotal());
        voPage.setRecords(records);
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveAdmin(AdminSaveDTO dto) {
        if (lambdaQuery().eq(Admin::getUsername, dto.getUsername()).exists()) {
            throw new BizException("用户名已存在");
        }
        Admin admin = new Admin();
        admin.setUsername(dto.getUsername());
        admin.setNickname(dto.getNickname());
        admin.setPassword(BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt()));
        admin.setStatus(1);
        save(admin);
        bindRoles(admin.getId(), dto.getRoleIds());
        return admin.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAdmin(Long id, AdminUpdateDTO dto) {
        Admin exist = getById(id);
        if (exist == null) {
            throw new BizException("管理员不存在");
        }
        Admin admin = new Admin();
        admin.setId(id);
        admin.setNickname(dto.getNickname());
        if (StringUtils.hasText(dto.getPassword())) {
            admin.setPassword(BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt()));
        }
        updateById(admin);
        // 重新绑定角色
        adminRoleMapper.delete(new LambdaQueryWrapper<AdminRole>().eq(AdminRole::getAdminId, id));
        bindRoles(id, dto.getRoleIds());
        permissionService.clearCache(id);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException("状态值不正确");
        }
        if (getById(id) == null) {
            throw new BizException("管理员不存在");
        }
        if (status == 0 && isSelf(id)) {
            throw new BizException("不能禁用自己");
        }
        Admin admin = new Admin();
        admin.setId(id);
        admin.setStatus(status);
        updateById(admin);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeAdmin(Long id) {
        if (getById(id) == null) {
            throw new BizException("管理员不存在");
        }
        if (isSelf(id)) {
            throw new BizException("不能删除自己");
        }
        removeById(id);
        adminRoleMapper.delete(new LambdaQueryWrapper<AdminRole>().eq(AdminRole::getAdminId, id));
        permissionService.clearCache(id);
    }

    private void bindRoles(Long adminId, List<Long> roleIds) {
        if (CollectionUtils.isEmpty(roleIds)) {
            return;
        }
        Set<Long> distinct = new LinkedHashSet<>(roleIds);
        for (Long roleId : distinct) {
            AdminRole ar = new AdminRole();
            ar.setAdminId(adminId);
            ar.setRoleId(roleId);
            adminRoleMapper.insert(ar);
        }
    }

    private void fillRoles(List<AdminVO> admins) {
        if (CollectionUtils.isEmpty(admins)) {
            return;
        }
        Set<Long> adminIds = admins.stream().map(AdminVO::getId).collect(Collectors.toSet());
        List<AdminRole> links = adminRoleMapper.selectList(
                new LambdaQueryWrapper<AdminRole>().in(AdminRole::getAdminId, adminIds));
        if (links.isEmpty()) {
            return;
        }
        Set<Long> roleIds = links.stream().map(AdminRole::getRoleId).collect(Collectors.toSet());
        Map<Long, Role> roleMap = roleMapper.selectBatchIds(roleIds).stream()
                .collect(Collectors.toMap(Role::getId, Function.identity()));
        Map<Long, List<AdminRole>> byAdmin = links.stream().collect(Collectors.groupingBy(AdminRole::getAdminId));
        for (AdminVO vo : admins) {
            List<AdminRole> mine = byAdmin.getOrDefault(vo.getId(), Collections.emptyList());
            vo.setRoleIds(mine.stream().map(AdminRole::getRoleId).toList());
            vo.setRoleNames(mine.stream()
                    .map(ar -> roleMap.get(ar.getRoleId()))
                    .filter(r -> r != null)
                    .map(Role::getName)
                    .toList());
        }
    }

    private AdminVO toVO(Admin admin) {
        AdminVO vo = new AdminVO();
        BeanUtils.copyProperties(admin, vo);
        vo.setRoleIds(Collections.emptyList());
        vo.setRoleNames(Collections.emptyList());
        return vo;
    }

    private boolean isSelf(Long id) {
        try {
            return id.equals(StpUtil.getLoginIdAsLong());
        } catch (Exception e) {
            return false;
        }
    }
}
