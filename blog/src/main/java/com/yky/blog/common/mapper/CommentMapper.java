package com.yky.blog.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yky.blog.common.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
}
