package com.yky.blog.common.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yky.blog.api.dto.TagApiVO;
import com.yky.blog.api.service.ArticleViewCountService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RedisFeatureTests {

    private static final String CACHE_KEY = RedisKeys.cache("test:tag");
    private static final Long ARTICLE_ID = 999996L;

    @Autowired
    private RedisCacheService redisCacheService;

    @Autowired
    private ArticleViewCountService articleViewCountService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @AfterEach
    void cleanUp() {
        stringRedisTemplate.delete(CACHE_KEY);
        stringRedisTemplate.opsForHash().delete(RedisKeys.ARTICLE_VIEW_DELTA, ARTICLE_ID.toString());
        stringRedisTemplate.opsForZSet().remove(RedisKeys.REALTIME_HOT_ARTICLES, ARTICLE_ID.toString());
    }

    @Test
    void cacheShouldReadFromRedisAfterFirstLoad() {
        AtomicInteger loadCount = new AtomicInteger();
        TagApiVO first = redisCacheService.getOrLoad(
                CACHE_KEY,
                objectMapper.getTypeFactory().constructType(TagApiVO.class),
                Duration.ofMinutes(1),
                () -> {
                    loadCount.incrementAndGet();
                    TagApiVO vo = new TagApiVO();
                    vo.setId(ARTICLE_ID);
                    vo.setName("RedisTest");
                    vo.setColor("#ffffff");
                    vo.setArticleCount(1L);
                    return vo;
                });
        TagApiVO second = redisCacheService.getOrLoad(
                CACHE_KEY,
                objectMapper.getTypeFactory().constructType(TagApiVO.class),
                Duration.ofMinutes(1),
                () -> {
                    loadCount.incrementAndGet();
                    return new TagApiVO();
                });

        assertThat(first.getName()).isEqualTo("RedisTest");
        assertThat(second.getName()).isEqualTo("RedisTest");
        assertThat(loadCount).hasValue(1);
    }

    @Test
    void articleViewCountShouldWriteDeltaAndHotArticleToRedis() {
        long first = articleViewCountService.increaseAndGetDelta(ARTICLE_ID);
        long second = articleViewCountService.increaseAndGetDelta(ARTICLE_ID);

        Object delta = stringRedisTemplate.opsForHash().get(RedisKeys.ARTICLE_VIEW_DELTA, ARTICLE_ID.toString());
        Double score = stringRedisTemplate.opsForZSet().score(RedisKeys.REALTIME_HOT_ARTICLES, ARTICLE_ID.toString());

        assertThat(first).isEqualTo(1L);
        assertThat(second).isEqualTo(2L);
        assertThat(delta).hasToString("2");
        assertThat(score).isEqualTo(2D);
    }
}
