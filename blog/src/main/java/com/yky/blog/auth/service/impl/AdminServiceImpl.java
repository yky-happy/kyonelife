package com.yky.blog.auth.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yky.blog.auth.dto.LoginDTO;
import com.yky.blog.auth.service.AdminService;
import com.yky.blog.auth.service.PermissionService;
import com.yky.blog.auth.vo.AdminInfoVO;
import com.yky.blog.auth.vo.LoginVO;
import com.yky.blog.common.entity.Admin;
import com.yky.blog.common.exception.BizException;
import com.yky.blog.common.mapper.AdminMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final AdminMapper adminMapper;
    private final PermissionService permissionService;

    @Override
    public LoginVO login(LoginDTO dto) {
        Admin admin = adminMapper.selectOne(
                new LambdaQueryWrapper<Admin>()
                        .eq(Admin::getUsername, dto.getUsername())
        );
        if (admin == null) {
            throw new BizException("用户名或密码错误");
        }

        if (admin.getStatus() == 0) {
            throw new BizException("账号已被禁用，请联系超级管理员");
        }

        if (!BCrypt.checkpw(dto.getPassword(), admin.getPassword())) {
            throw new BizException("用户名或密码错误");
        }

        StpUtil.login(admin.getId());
        // 登录时清除旧的权限缓存，确保角色变更后即时生效
        permissionService.clearCache(admin.getId());

        return LoginVO.builder()
                .token(StpUtil.getTokenValue())
                .id(admin.getId())
                .nickname(admin.getNickname())
                .build();
    }

    @Override
    public void logout() {
        Object adminId = StpUtil.getLoginIdDefaultNull();
        if (adminId != null) {
            permissionService.clearCache(adminId);
        }
        StpUtil.logout();
    }

    @Override
    public AdminInfoVO getCurrentInfo() {
        Object adminId = StpUtil.getLoginId();
        Admin admin = adminMapper.selectById(Long.valueOf(String.valueOf(adminId)));
        if (admin == null) {
            throw new BizException("管理员不存在");
        }
        return AdminInfoVO.builder()
                .id(admin.getId())
                .username(admin.getUsername())
                .nickname(admin.getNickname())
                .roles(permissionService.listRoles(adminId))
                .permissions(permissionService.listPermissions(adminId))
                .menus(permissionService.listMenuTree(adminId))
                .build();
    }
}
