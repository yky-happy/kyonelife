package com.yky.blog.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yky.blog.api.dto.AuthVO;
import com.yky.blog.api.dto.RegisterDTO;
import com.yky.blog.api.dto.UserVO;
import com.yky.blog.api.service.EmailCodeService;
import com.yky.blog.api.service.UserAuthService;
import com.yky.blog.common.entity.User;
import com.yky.blog.common.exception.BizException;
import com.yky.blog.common.mapper.UserMapper;
import com.yky.blog.common.redis.RedisKeys;
import com.yky.blog.common.satoken.StpUserUtil;
import cn.dev33.satoken.stp.SaLoginModel;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserAuthServiceImpl implements UserAuthService {

    private final StringRedisTemplate redis;
    private final EmailCodeService emailCodeService;
    private final UserMapper userMapper;

    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration SEND_INTERVAL = Duration.ofSeconds(60);
    private final SecureRandom random = new SecureRandom();

    @Override
    public void sendEmailCode(String email) {
        String limitKey = RedisKeys.cache("email-code-limit:" + email);
        if (Boolean.TRUE.equals(redis.hasKey(limitKey))) {
            throw new BizException("发送过于频繁，请 60 秒后再试");
        }
        String code = String.format("%06d", random.nextInt(1_000_000));
        redis.opsForValue().set(RedisKeys.cache("email-code:" + email), code, CODE_TTL);
        redis.opsForValue().set(limitKey, "1", SEND_INTERVAL);
        emailCodeService.sendCode(email, code);
    }

    @Override
    public AuthVO register(RegisterDTO dto) {
        String email = dto.getEmail().trim();
        // 校验验证码
        String key = RedisKeys.cache("email-code:" + email);
        String real = redis.opsForValue().get(key);
        if (real == null) {
            throw new BizException("验证码已过期，请重新获取");
        }
        if (!real.equals(dto.getCode())) {
            throw new BizException("验证码错误");
        }
        // 邮箱唯一
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (exists != null && exists > 0) {
            throw new BizException("该邮箱已注册，请直接登录");
        }
        redis.delete(key);

        User user = new User();
        user.setEmail(email);
        user.setPassword(BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt()));
        user.setNickname(StringUtils.hasText(dto.getNickname()) ? dto.getNickname().trim() : null);
        user.setStatus(1);
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.insert(user);

        // 分配账号：基于自增ID生成，确保唯一
        user.setAccount(String.format("1%07d", user.getId()));
        userMapper.updateById(user);

        StpUserUtil.login(user.getId(), rememberModel());
        return AuthVO.builder().token(StpUserUtil.getTokenValue()).user(toVO(user)).build();
    }

    @Override
    public AuthVO login(String identifier, String password) {
        String id = identifier.trim();
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, id).or().eq(User::getAccount, id).last("limit 1"));
        if (user == null || !StringUtils.hasText(user.getPassword())) {
            throw new BizException("账号或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException("账号已被禁用");
        }
        if (!BCrypt.checkpw(password, user.getPassword())) {
            throw new BizException("账号或密码错误");
        }
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);

        StpUserUtil.login(user.getId(), rememberModel());
        return AuthVO.builder().token(StpUserUtil.getTokenValue()).user(toVO(user)).build();
    }

    @Override
    public UserVO currentUser() {
        Object id = StpUserUtil.getLoginIdDefaultNull();
        if (id == null) {
            return null;
        }
        User user = userMapper.selectById(Long.valueOf(id.toString()));
        return user == null ? null : toVO(user);
    }

    @Override
    public void resetPassword(String email, String code, String password) {
        String mail = email.trim();
        String key = RedisKeys.cache("email-code:" + mail);
        String real = redis.opsForValue().get(key);
        if (real == null) {
            throw new BizException("验证码已过期，请重新获取");
        }
        if (!real.equals(code)) {
            throw new BizException("验证码错误");
        }
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, mail));
        if (user == null) {
            throw new BizException("该邮箱未注册");
        }
        redis.delete(key);
        user.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
        userMapper.updateById(user);
    }

    @Override
    public void logout() {
        StpUserUtil.logout();
    }

    /** 读者登录：30 天有效、不因不活跃而过期（记住登录） */
    private SaLoginModel rememberModel() {
        return new SaLoginModel().setTimeout(2592000L).setActiveTimeout(-1L);
    }

    private UserVO toVO(User u) {
        String display = StringUtils.hasText(u.getNickname()) ? u.getNickname() : u.getAccount();
        return UserVO.builder()
                .id(u.getId())
                .account(u.getAccount())
                .nickname(display)
                .avatar(u.getAvatar())
                .email(u.getEmail())
                .build();
    }
}
