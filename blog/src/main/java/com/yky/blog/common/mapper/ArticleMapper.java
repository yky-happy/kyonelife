package com.yky.blog.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yky.blog.common.entity.Article;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ArticleMapper extends BaseMapper<Article> {

    @Update("""
            UPDATE article
            SET view_count = COALESCE(view_count, 0) + #{delta}
            WHERE id = #{articleId}
            """)
    int incrementViewCount(@Param("articleId") Long articleId, @Param("delta") Long delta);

    @Update("""
            UPDATE article
            SET like_count = GREATEST(COALESCE(like_count, 0) + #{delta}, 0)
            WHERE id = #{articleId}
            """)
    int incrementLikeCount(@Param("articleId") Long articleId, @Param("delta") Long delta);

    @org.apache.ibatis.annotations.Select("SELECT COALESCE(SUM(view_count), 0) FROM article")
    long selectTotalViews();
}
