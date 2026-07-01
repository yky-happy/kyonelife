package com.yky.blog.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yky.blog.admin.vo.ArticleTrendVO;
import com.yky.blog.admin.vo.HotArticleVO;
import com.yky.blog.common.entity.ArticleDailyStat;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface ArticleDailyStatMapper extends BaseMapper<ArticleDailyStat> {

    @Insert("""
            INSERT INTO article_daily_stat (stat_date, article_id, view_count, visitor_count, duration_total)
            SELECT DATE(create_time) AS stat_date,
                   article_id,
                   COUNT(*) AS view_count,
                   COUNT(DISTINCT visitor_id) AS visitor_count,
                   COALESCE(SUM(duration), 0) AS duration_total
            FROM event_log
            WHERE event_type = 'article_view'
              AND article_id IS NOT NULL
              AND create_time >= #{statDate}
              AND create_time < DATE_ADD(#{statDate}, INTERVAL 1 DAY)
            GROUP BY DATE(create_time), article_id
            ON DUPLICATE KEY UPDATE
              view_count = VALUES(view_count),
              visitor_count = VALUES(visitor_count),
              duration_total = VALUES(duration_total),
              update_time = CURRENT_TIMESTAMP
            """)
    int upsertByDate(@Param("statDate") LocalDate statDate);

    @Delete("""
            DELETE FROM article_daily_stat
            WHERE stat_date = #{statDate}
              AND NOT EXISTS (
                SELECT 1
                FROM event_log
                WHERE event_type = 'article_view'
                  AND article_id IS NOT NULL
                  AND create_time >= #{statDate}
                  AND create_time < DATE_ADD(#{statDate}, INTERVAL 1 DAY)
              )
            """)
    int deleteDateWhenNoArticleViews(@Param("statDate") LocalDate statDate);

    @Select("""
            SELECT COALESCE(SUM(view_count), 0)
            FROM article_daily_stat
            WHERE stat_date >= #{startDate}
              AND stat_date <= #{endDate}
            """)
    Long sumViewCount(@Param("startDate") LocalDate startDate,
                      @Param("endDate") LocalDate endDate);

    @Select("""
            SELECT s.article_id AS articleId,
                   COALESCE(a.title, CONCAT('文章#', s.article_id)) AS title,
                   SUM(s.view_count) AS viewCount
            FROM article_daily_stat s
            LEFT JOIN article a ON a.id = s.article_id
            WHERE s.stat_date >= #{startDate}
              AND s.stat_date <= #{endDate}
            GROUP BY s.article_id, a.title
            ORDER BY viewCount DESC
            LIMIT #{limit}
            """)
    List<HotArticleVO> listHotArticles(@Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate,
                                       @Param("limit") int limit);

    @Select("""
            SELECT DATE_FORMAT(stat_date, '%Y-%m-%d') AS date,
                   COALESCE(SUM(view_count), 0) AS viewCount,
                   COALESCE(SUM(visitor_count), 0) AS visitorCount
            FROM article_daily_stat
            WHERE stat_date >= #{startDate}
              AND stat_date <= #{endDate}
            GROUP BY stat_date
            ORDER BY stat_date
            """)
    List<ArticleTrendVO> listArticleTrend(@Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);
}
