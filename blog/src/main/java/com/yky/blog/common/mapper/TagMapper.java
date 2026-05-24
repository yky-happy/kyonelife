package com.yky.blog.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yky.blog.admin.vo.TagVO;
import com.yky.blog.common.entity.Tag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TagMapper extends BaseMapper<Tag> {

    @Select("<script>" +
            "SELECT t.id, t.name, t.color, t.create_time, COUNT(at.article_id) AS article_count " +
            "FROM tag t " +
            "LEFT JOIN article_tag at ON t.id = at.tag_id " +
            "<where>" +
            "<if test='keyword != null and keyword != \"\"'>" +
            "t.name LIKE CONCAT('%', #{keyword}, '%') " +
            "</if>" +
            "</where>" +
            "GROUP BY t.id, t.name, t.color, t.create_time " +
            "ORDER BY t.create_time DESC" +
            "</script>")
    IPage<TagVO> selectTagVOPage(IPage<TagVO> page, @Param("keyword") String keyword);
}
