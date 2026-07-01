package com.yky.blog.api.service;

import com.yky.blog.api.dto.AuthVO;
import com.yky.blog.api.dto.RegisterDTO;
import com.yky.blog.api.dto.UserVO;

public interface UserAuthService {

    /** 发送邮箱验证码（注册用，含限流） */
    void sendEmailCode(String email);

    /** 验证码注册：设密码/昵称，分配账号 */
    AuthVO register(RegisterDTO dto);

    /** 邮箱或账号 + 密码登录 */
    AuthVO login(String identifier, String password);

    /** 忘记密码：邮箱验证码重置密码 */
    void resetPassword(String email, String code, String password);

    /** 当前登录读者，未登录返回 null */
    UserVO currentUser();

    void logout();
}
