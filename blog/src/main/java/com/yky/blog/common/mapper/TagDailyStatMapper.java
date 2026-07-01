package com.yky.blog.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yky.blog.admin.vo.AnalyticsRankVO;
import com.yky.blog.common.entity.TagDailyStat;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface TagDailyStatMapper extends BaseMapper<TagDailyStat> {

    @Insert("""
            INSERT INTO tag_daily_stat (stat_date, tag_id, click_count, visitor_count)
            SELECT DATE(create_time) AS stat_date,
                   tag_id,
                   COUNT(*) AS click_count,
                   COUNT(DISTINCT visitor_id) AS visitor_count
            FROM event_log
            WHERE event_type = 'tag_click'
              AND tag_id IS NOT NULL
              AND create_time >= #{statDate}
              AND create_time < DATE_ADD(#{statDate}, INTERVAL 1 DAY)
            GROUP BY DATE(create_time), tag_id
            ON DUPLICATE KEY UPDATE
              click_count = VALUES(click_count),
              visitor_count = VALUES(visitor_count),
              update_time = CURRENT_TIMESTAMP
            """)
    int upsertByDate(@Param("statDate") LocalDate statDate);

    @Delete("""
            DELETE FROM tag_daily_stat
            WHERE stat_date = #{statDate}
              AND NOT EXISTS (
                SELECT 1
                FROM event_log
                WHERE event_type = 'tag_click'
                  AND tag_id IS NOT NULL
                  AND create_time >= #{statDate}
                  AND create_time < DATE_ADD(#{statDate}, INTERVAL 1 DAY)
              )
            """)
    int deleteDateWhenNoTagClicks(@Param("statDate") LocalDate statDate);

    @Select("""
            SELECT s.tag_id AS id,
                   COALESCE(t.name, CONCAT('标签#', s.tag_id)) AS name,
                   SUM(s.click_count) AS count,
                   SUM(s.visitor_count) AS visitorCount
            FROM tag_daily_stat s
            LEFT JOIN tag t ON t.id = s.tag_id
            WHERE s.stat_date >= #{startDate}
              AND s.stat_date <= #{endDate}
            GROUP BY s.tag_id, t.name
            ORDER BY count DESC
            LIMIT #{limit}
            """)
    List<AnalyticsRankVO> listHotTags(@Param("startDate") LocalDate startDate,
                                      @Param("endDate") LocalDate endDate,
                                      @Param("limit") int limit);
}
