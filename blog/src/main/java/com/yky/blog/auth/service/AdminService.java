package com.yky.blog.auth.service;

import com.yky.blog.auth.dto.LoginDTO;
import com.yky.blog.auth.vo.AdminInfoVO;
import com.yky.blog.auth.vo.LoginVO;

public interface AdminService {

    LoginVO login(LoginDTO dto);

    void logout();

    /**
     * 获取当前登录管理员的信息，含角色、权限标识与可见菜单树。
     */
    AdminInfoVO getCurrentInfo();
}
