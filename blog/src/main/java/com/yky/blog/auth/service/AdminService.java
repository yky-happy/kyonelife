package com.yky.blog.auth.service;

import com.yky.blog.auth.dto.LoginDTO;
import com.yky.blog.auth.vo.LoginVO;

public interface AdminService {

    LoginVO login(LoginDTO dto);

    void logout();
}
