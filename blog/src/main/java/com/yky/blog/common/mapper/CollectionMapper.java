package com.yky.blog.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yky.blog.admin.vo.CollectionVO;
import com.yky.blog.common.entity.Collection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CollectionMapper extends BaseMapper<Collection> {

    @Select("<script>" +
            "SELECT c.id, c.name, c.cover, c.description, c.sort, c.create_time, COUNT(a.id) AS article_count " +
            "FROM collection c " +
            "LEFT JOIN article a ON c.id = a.collection_id " +
            "<where>" +
            "<if test='keyword != null and keyword != \"\"'>" +
            "c.name LIKE CONCAT('%', #{keyword}, '%') " +
            "</if>" +
            "</where>" +
            "GROUP BY c.id, c.name, c.cover, c.description, c.sort, c.create_time " +
            "ORDER BY c.sort ASC, c.create_time DESC" +
            "</script>")
    IPage<CollectionVO> selectCollectionVOPage(IPage<CollectionVO> page, @Param("keyword") String keyword);
}
