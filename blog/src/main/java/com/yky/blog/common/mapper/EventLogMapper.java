package com.yky.blog.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yky.blog.admin.vo.AnalyticsTrendVO;
import com.yky.blog.admin.vo.HotArticleVO;
import com.yky.blog.common.entity.EventLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface EventLogMapper extends BaseMapper<EventLog> {

    /**
     * 批量插入埋点明细（单条多值 INSERT），由事件流消费者攒批调用，降低写放大。
     */
    @Insert("""
            <script>
            INSERT INTO event_log
              (event_type, visitor_id, article_id, tag_id, collection_id, keyword,
               page_url, referrer, ip, ip_location, user_agent, device, browser, os, duration, create_time)
            VALUES
            <foreach collection='list' item='e' separator=','>
              (#{e.eventType}, #{e.visitorId}, #{e.articleId}, #{e.tagId}, #{e.collectionId}, #{e.keyword},
               #{e.pageUrl}, #{e.referrer}, #{e.ip}, #{e.ipLocation}, #{e.userAgent}, #{e.device}, #{e.browser}, #{e.os},
               #{e.duration}, #{e.createTime})
            </foreach>
            </script>
            """)
    int insertBatch(@Param("list") List<EventLog> list);

    @Select("""
            SELECT COUNT(*)
            FROM event_log
            WHERE event_type = #{eventType}
              AND create_time >= #{startTime}
              AND create_time < #{endTime}
            """)
    Long countByEventTypeBetween(@Param("eventType") String eventType,
                                 @Param("startTime") LocalDateTime startTime,
                                 @Param("endTime") LocalDateTime endTime);

    @Select("""
            SELECT COUNT(DISTINCT visitor_id)
            FROM event_log
            WHERE event_type = #{eventType}
              AND create_time >= #{startTime}
              AND create_time < #{endTime}
            """)
    Long countUvByEventTypeBetween(@Param("eventType") String eventType,
                                   @Param("startTime") LocalDateTime startTime,
                                   @Param("endTime") LocalDateTime endTime);

    @Select("""
            SELECT DATE(create_time) AS date,
                   COUNT(*) AS pv,
                   COUNT(DISTINCT visitor_id) AS uv
            FROM event_log
            WHERE event_type = 'page_view'
              AND create_time >= #{startTime}
              AND create_time < #{endTime}
            GROUP BY DATE(create_time)
            ORDER BY DATE(create_time)
            """)
    List<AnalyticsTrendVO> listPvUvTrend(@Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime);

    @Select("""
            SELECT e.article_id AS articleId,
                   COALESCE(a.title, CONCAT('文章#', e.article_id)) AS title,
                   COUNT(*) AS viewCount
            FROM event_log e
            LEFT JOIN article a ON a.id = e.article_id
            WHERE e.event_type = 'article_view'
              AND e.article_id IS NOT NULL
              AND e.create_time >= #{startTime}
              AND e.create_time < #{endTime}
            GROUP BY e.article_id, a.title
            ORDER BY viewCount DESC
            LIMIT #{limit}
            """)
    List<HotArticleVO> listHotArticles(@Param("startTime") LocalDateTime startTime,
                                       @Param("endTime") LocalDateTime endTime,
                                       @Param("limit") int limit);
}
