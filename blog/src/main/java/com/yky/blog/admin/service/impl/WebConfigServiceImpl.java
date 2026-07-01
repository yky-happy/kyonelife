package com.yky.blog.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yky.blog.admin.dto.WebConfigDTO;
import com.yky.blog.admin.service.WebConfigService;
import com.yky.blog.common.entity.WebConfig;
import com.yky.blog.common.mapper.WebConfigMapper;
import com.yky.blog.common.redis.RedisCacheService;
import com.yky.blog.common.redis.RedisKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class WebConfigServiceImpl extends ServiceImpl<WebConfigMapper, WebConfig> implements WebConfigService {

    private static final String CACHE_KEY = RedisKeys.cache("web-config");
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private final RedisCacheService redisCacheService;
    private final ObjectMapper objectMapper;

    @Override
    public WebConfig getConfig() {
        WebConfig config = lambdaQuery().orderByAsc(WebConfig::getId).last("LIMIT 1").one();
        return config == null ? new WebConfig() : config;
    }

    @Override
    public WebConfig getCachedConfig() {
        JavaType type = objectMapper.getTypeFactory().constructType(WebConfig.class);
        return redisCacheService.getOrLoad(CACHE_KEY, type, CACHE_TTL, this::getConfig);
    }

    @Override
    public void updateConfig(WebConfigDTO dto) {
        WebConfig existing = getConfig();
        WebConfig config = new WebConfig();
        BeanUtils.copyProperties(dto, config);
        if (existing.getId() != null) {
            config.setId(existing.getId());
            updateById(config);
        } else {
            save(config);
        }
        redisCacheService.evict(CACHE_KEY);
    }
}
