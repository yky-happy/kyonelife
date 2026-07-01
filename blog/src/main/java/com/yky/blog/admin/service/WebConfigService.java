package com.yky.blog.admin.service;

import com.yky.blog.admin.dto.WebConfigDTO;
import com.yky.blog.common.entity.WebConfig;

public interface WebConfigService {

    /** 获取网站配置（单行，不存在返回空对象）。 */
    WebConfig getConfig();

    /** 获取网站配置（前台用，带 Redis 缓存）。 */
    WebConfig getCachedConfig();

    /** 更新网站配置并失效缓存。 */
    void updateConfig(WebConfigDTO dto);
}
