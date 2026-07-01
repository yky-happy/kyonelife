package com.yky.blog.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yky.blog.common.entity.LikeRecord;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface LikeRecordMapper extends BaseMapper<LikeRecord> {

    /**
     * 幂等插入：唯一索引冲突时被忽略，返回 1=实际插入、0=已存在。
     * 配合"仅影响行数>0 时才 +1"保证点赞数不会被并发重复点赞刷大。
     */
    @Insert("""
            INSERT IGNORE INTO like_record (article_id, visitor_id)
            VALUES (#{articleId}, #{visitorId})
            """)
    int insertIgnore(@Param("articleId") Long articleId, @Param("visitorId") String visitorId);

    /** 取消点赞，返回 1=实际删除、0=本就没赞。 */
    @Delete("""
            DELETE FROM like_record
            WHERE article_id = #{articleId} AND visitor_id = #{visitorId}
            """)
    int deleteOne(@Param("articleId") Long articleId, @Param("visitorId") String visitorId);

    /** 加载某文章的全部点赞访客（Redis 冷启动重建用）。 */
    @Select("SELECT visitor_id FROM like_record WHERE article_id = #{articleId}")
    List<String> selectVisitorIds(@Param("articleId") Long articleId);
}
