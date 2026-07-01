package com.yky.blog.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yky.blog.admin.vo.AdminUserVO;
import com.yky.blog.admin.service.UserAdminService;
import com.yky.blog.common.entity.Comment;
import com.yky.blog.common.entity.User;
import com.yky.blog.common.exception.BizException;
import com.yky.blog.common.mapper.CommentMapper;
import com.yky.blog.common.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserAdminServiceImpl implements UserAdminService {

    private final UserMapper userMapper;
    private final CommentMapper commentMapper;

    @Override
    public IPage<AdminUserVO> page(int page, int size, String keyword, Integer status) {
        Page<User> p = userMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<User>()
                        .and(StringUtils.hasText(keyword), w -> w
                                .like(User::getAccount, keyword)
                                .or().like(User::getEmail, keyword)
                                .or().like(User::getNickname, keyword))
                        .eq(status != null, User::getStatus, status)
                        .orderByDesc(User::getCreateTime));
        Page<AdminUserVO> result = new Page<>(p.getCurrent(), p.getSize(), p.getTotal());
        result.setRecords(p.getRecords().stream().map(u -> {
            AdminUserVO vo = new AdminUserVO();
            vo.setId(u.getId());
            vo.setAccount(u.getAccount());
            vo.setEmail(u.getEmail());
            vo.setNickname(u.getNickname());
            vo.setStatus(u.getStatus());
            vo.setIpLocation(u.getIpLocation());
            vo.setLastLoginTime(u.getLastLoginTime());
            vo.setCreateTime(u.getCreateTime());
            return vo;
        }).toList());
        return result;
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException("状态值不正确");
        }
        if (userMapper.selectById(id) == null) {
            throw new BizException("用户不存在");
        }
        User u = new User();
        u.setId(id);
        u.setStatus(status);
        userMapper.updateById(u);
    }

    @Override
    public void delete(Long id) {
        userMapper.deleteById(id);
        commentMapper.delete(new LambdaQueryWrapper<Comment>().eq(Comment::getUserId, id));
    }
}
