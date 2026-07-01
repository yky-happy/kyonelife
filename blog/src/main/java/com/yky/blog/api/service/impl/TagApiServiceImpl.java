package com.yky.blog.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yky.blog.api.dto.TagApiVO;
import com.yky.blog.api.service.TagApiService;
import com.yky.blog.common.entity.Article;
import com.yky.blog.common.entity.ArticleTag;
import com.yky.blog.common.entity.Tag;
import com.yky.blog.common.mapper.ArticleMapper;
import com.yky.blog.common.mapper.ArticleTagMapper;
import com.yky.blog.common.mapper.TagMapper;
import com.yky.blog.common.redis.RedisCacheService;
import com.yky.blog.common.redis.RedisKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TagApiServiceImpl extends ServiceImpl<TagMapper, Tag> implements TagApiService {

    private static final int STATUS_PUBLISHED = 1;
    private static final Duration TAG_LIST_CACHE_TTL = Duration.ofMinutes(3);

    private final ArticleMapper articleMapper;
    private final ArticleTagMapper articleTagMapper;
    private final RedisCacheService redisCacheService;
    private final ObjectMapper objectMapper;

    @Override
    public List<TagApiVO> listTags() {
        JavaType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, TagApiVO.class);
        return redisCacheService.getOrLoad(RedisKeys.cache("tag:list"), listType, TAG_LIST_CACHE_TTL, this::loadTags);
    }

    private List<TagApiVO> loadTags() {
        List<Tag> tags = list(new LambdaQueryWrapper<Tag>().orderByDesc(Tag::getCreateTime));
        if (CollectionUtils.isEmpty(tags)) {
            return Collections.emptyList();
        }

        Map<Long, Long> articleCountMap = countPublishedArticlesByTag();
        return tags.stream()
                .map(tag -> {
                    TagApiVO vo = new TagApiVO();
                    vo.setId(tag.getId());
                    vo.setName(tag.getName());
                    vo.setColor(tag.getColor());
                    vo.setArticleCount(articleCountMap.getOrDefault(tag.getId(), 0L));
                    return vo;
                })
                .toList();
    }

    private Map<Long, Long> countPublishedArticlesByTag() {
        List<Article> articles = articleMapper.selectList(new LambdaQueryWrapper<Article>()
                .select(Article::getId)
                .eq(Article::getStatus, STATUS_PUBLISHED));
        if (CollectionUtils.isEmpty(articles)) {
            return Collections.emptyMap();
        }

        List<Long> articleIds = articles.stream().map(Article::getId).toList();
        List<ArticleTag> articleTags = articleTagMapper.selectList(new LambdaQueryWrapper<ArticleTag>()
                .in(ArticleTag::getArticleId, articleIds));
        if (CollectionUtils.isEmpty(articleTags)) {
            return Collections.emptyMap();
        }

        return articleTags.stream()
                .collect(Collectors.groupingBy(ArticleTag::getTagId, Collectors.counting()));
    }
}
