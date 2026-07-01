package com.yky.blog.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yky.blog.admin.vo.AnalyticsRankVO;
import com.yky.blog.common.entity.SearchKeywordStat;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface SearchKeywordStatMapper extends BaseMapper<SearchKeywordStat> {

    @Insert("""
            INSERT INTO search_keyword_stat (stat_date, keyword, search_count, visitor_count)
            SELECT DATE(create_time) AS stat_date,
                   TRIM(keyword) AS keyword,
                   COUNT(*) AS search_count,
                   COUNT(DISTINCT visitor_id) AS visitor_count
            FROM event_log
            WHERE event_type = 'search'
              AND keyword IS NOT NULL
              AND TRIM(keyword) <> ''
              AND create_time >= #{statDate}
              AND create_time < DATE_ADD(#{statDate}, INTERVAL 1 DAY)
            GROUP BY DATE(create_time), TRIM(keyword)
            ON DUPLICATE KEY UPDATE
              search_count = VALUES(search_count),
              visitor_count = VALUES(visitor_count),
              update_time = CURRENT_TIMESTAMP
            """)
    int upsertByDate(@Param("statDate") LocalDate statDate);

    @Delete("""
            DELETE FROM search_keyword_stat
            WHERE stat_date = #{statDate}
              AND NOT EXISTS (
                SELECT 1
                FROM event_log
                WHERE event_type = 'search'
                  AND keyword IS NOT NULL
                  AND TRIM(keyword) <> ''
                  AND create_time >= #{statDate}
                  AND create_time < DATE_ADD(#{statDate}, INTERVAL 1 DAY)
              )
            """)
    int deleteDateWhenNoSearches(@Param("statDate") LocalDate statDate);

    @Select("""
            SELECT NULL AS id,
                   keyword AS name,
                   SUM(search_count) AS count,
                   SUM(visitor_count) AS visitorCount
            FROM search_keyword_stat
            WHERE stat_date >= #{startDate}
              AND stat_date <= #{endDate}
            GROUP BY keyword
            ORDER BY count DESC
            LIMIT #{limit}
            """)
    List<AnalyticsRankVO> listHotKeywords(@Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate,
                                          @Param("limit") int limit);
}
