package com.yky.blog.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yky.blog.admin.vo.AnalyticsTrendVO;
import com.yky.blog.common.entity.EventDailyStat;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface EventDailyStatMapper extends BaseMapper<EventDailyStat> {

    @Insert("""
            INSERT INTO event_daily_stat (stat_date, event_type, pv, uv, duration_total)
            SELECT DATE(create_time) AS stat_date,
                   event_type,
                   COUNT(*) AS pv,
                   COUNT(DISTINCT visitor_id) AS uv,
                   COALESCE(SUM(duration), 0) AS duration_total
            FROM event_log
            WHERE create_time >= #{statDate}
              AND create_time < DATE_ADD(#{statDate}, INTERVAL 1 DAY)
            GROUP BY DATE(create_time), event_type
            ON DUPLICATE KEY UPDATE
              pv = VALUES(pv),
              uv = VALUES(uv),
              duration_total = VALUES(duration_total),
              update_time = CURRENT_TIMESTAMP
            """)
    int upsertByDate(@Param("statDate") LocalDate statDate);

    @Delete("""
            DELETE FROM event_daily_stat
            WHERE stat_date = #{statDate}
              AND NOT EXISTS (
                SELECT 1
                FROM event_log
                WHERE create_time >= #{statDate}
                  AND create_time < DATE_ADD(#{statDate}, INTERVAL 1 DAY)
              )
            """)
    int deleteDateWhenNoEvents(@Param("statDate") LocalDate statDate);

    @Select("""
            SELECT COALESCE(SUM(pv), 0)
            FROM event_daily_stat
            WHERE event_type = #{eventType}
              AND stat_date >= #{startDate}
              AND stat_date <= #{endDate}
            """)
    Long sumPv(@Param("eventType") String eventType,
               @Param("startDate") LocalDate startDate,
               @Param("endDate") LocalDate endDate);

    @Select("""
            SELECT COALESCE(SUM(uv), 0)
            FROM event_daily_stat
            WHERE event_type = #{eventType}
              AND stat_date >= #{startDate}
              AND stat_date <= #{endDate}
            """)
    Long sumUv(@Param("eventType") String eventType,
               @Param("startDate") LocalDate startDate,
               @Param("endDate") LocalDate endDate);

    @Select("""
            SELECT DATE_FORMAT(stat_date, '%Y-%m-%d') AS date,
                   pv,
                   uv
            FROM event_daily_stat
            WHERE event_type = 'page_view'
              AND stat_date >= #{startDate}
              AND stat_date <= #{endDate}
            ORDER BY stat_date
            """)
    List<AnalyticsTrendVO> listPvUvTrend(@Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate);
}
