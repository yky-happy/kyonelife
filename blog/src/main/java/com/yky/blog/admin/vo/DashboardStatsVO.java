package com.yky.blog.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "后台仪表盘统计")
public class DashboardStatsVO {
    private long articleCount;
    private long publishedCount;
    private long draftCount;
    private long tagCount;
    private long collectionCount;
    private long userCount;
    @Schema(description = "文章总浏览量")
    private long totalViews;
}
