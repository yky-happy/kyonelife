package com.yky.blog.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yky.blog.api.dto.ArchiveArticleVO;
import com.yky.blog.api.dto.ArchiveMonthVO;
import com.yky.blog.api.dto.ArticleCardVO;
import com.yky.blog.api.dto.ArticleNavVO;
import com.yky.blog.api.dto.ArticleWebDetailVO;
import com.yky.blog.api.dto.TagApiVO;
import com.yky.blog.api.service.ArticleApiService;
import com.yky.blog.api.service.ArticleViewCountService;
import com.yky.blog.common.entity.Article;
import com.yky.blog.common.entity.ArticleImage;
import com.yky.blog.common.entity.ArticleTag;
import com.yky.blog.common.entity.Collection;
import com.yky.blog.common.entity.Tag;
import com.yky.blog.common.exception.BizException;
import com.yky.blog.common.mapper.ArticleImageMapper;
import com.yky.blog.common.mapper.ArticleMapper;
import com.yky.blog.common.mapper.ArticleTagMapper;
import com.yky.blog.common.mapper.CollectionMapper;
import com.yky.blog.common.mapper.TagMapper;
import com.yky.blog.common.redis.RedisCacheService;
import com.yky.blog.common.redis.RedisKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArticleApiServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleApiService {

    private static final int STATUS_PUBLISHED = 1;
    private static final DateTimeFormatter ARCHIVE_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final Duration ARTICLE_PAGE_CACHE_TTL = Duration.ofSeconds(60);
    private static final Duration ARCHIVE_CACHE_TTL = Duration.ofMinutes(3);
    private static final Duration ARTICLE_DETAIL_CACHE_TTL = Duration.ofMinutes(5);

    private final ArticleTagMapper articleTagMapper;
    private final ArticleImageMapper articleImageMapper;
    private final CollectionMapper collectionMapper;
    private final TagMapper tagMapper;
    private final ArticleViewCountService articleViewCountService;
    private final RedisCacheService redisCacheService;
    private final ObjectMapper objectMapper;

    @Override
    public IPage<ArticleCardVO> pageArticle(int page, int size, String keyword, Long tagId, Long collectionId) {
        // 分页参数保护，避免 ?size=999999 压垮数据库
        final int safePage = Math.max(page, 1);
        final int safeSize = Math.min(Math.max(size, 1), 50);
        String cacheKey = RedisKeys.cache("article:page:" + safePage + ":" + safeSize + ":"
                + normalize(keyword) + ":" + normalize(tagId) + ":" + normalize(collectionId));
        JavaType pageType = objectMapper.getTypeFactory().constructParametricType(Page.class, ArticleCardVO.class);
        return redisCacheService.getOrLoad(cacheKey, pageType, ARTICLE_PAGE_CACHE_TTL,
                () -> loadArticlePage(safePage, safeSize, keyword, tagId, collectionId));
    }

    private Page<ArticleCardVO> loadArticlePage(int page, int size, String keyword, Long tagId, Long collectionId) {
        List<Long> tagArticleIds = getPublishedArticleIdsByTag(tagId);
        if (tagId != null && tagArticleIds.isEmpty()) {
            return emptyPage(page, size);
        }

        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<Article>()
                .eq(Article::getStatus, STATUS_PUBLISHED)
                .and(StringUtils.hasText(keyword), w -> w
                        .like(Article::getTitle, keyword)
                        .or().like(Article::getSummary, keyword)
                        .or().like(Article::getContent, keyword))
                .eq(collectionId != null, Article::getCollectionId, collectionId)
                .in(tagId != null, Article::getId, tagArticleIds)
                .orderByDesc(Article::getIsStick)
                .orderByDesc(Article::getCreateTime);

        IPage<Article> articlePage = page(new Page<>(page, size), wrapper);
        List<ArticleCardVO> records = articlePage.getRecords()
                .stream()
                .map(this::toArticleCardVO)
                .toList();
        fillCollectionAndTags(records);

        Page<ArticleCardVO> voPage = new Page<>(articlePage.getCurrent(), articlePage.getSize(), articlePage.getTotal());
        voPage.setRecords(records);
        return voPage;
    }

    @Override
    public ArticleWebDetailVO getArticleDetail(Long id) {
        // 详情是高频热点 key：经增强后的 getOrLoad 自带防穿透（空值缓存）、防击穿（互斥重建）、防雪崩（TTL 抖动）
        JavaType detailType = objectMapper.getTypeFactory().constructType(ArticleWebDetailVO.class);
        ArticleWebDetailVO vo = redisCacheService.getOrLoad(
                RedisKeys.articleDetail(id), detailType, ARTICLE_DETAIL_CACHE_TTL,
                () -> loadArticleDetail(id));
        if (vo == null) {
            // 命中空值缓存或数据库确实不存在，统一抛出，避免每次请求都打库
            throw new BizException("文章不存在");
        }

        // 浏览量不进缓存基准之外：始终用 缓存内基础值 + Redis 实时增量，保证展示值实时
        long base = vo.getViewCount() == null ? 0L : vo.getViewCount();
        long delta = articleViewCountService.increaseAndGetDelta(id);
        vo.setViewCount(base + delta);
        return vo;
    }

    /**
     * 回源加载文章详情；文章不存在或未发布时返回 null（交由上层缓存为空值哨兵防穿透）。
     * 返回的 viewCount 为数据库基础值，实时增量在 {@link #getArticleDetail(Long)} 中叠加。
     */
    private ArticleWebDetailVO loadArticleDetail(Long id) {
        Article article = getById(id);
        if (article == null || !Objects.equals(article.getStatus(), STATUS_PUBLISHED)) {
            return null;
        }
        ArticleWebDetailVO vo = new ArticleWebDetailVO();
        BeanUtils.copyProperties(article, vo);
        fillCollectionAndTags(List.of(vo));
        vo.setViewCount(article.getViewCount() == null ? 0L : article.getViewCount());
        vo.setImages(articleImageMapper.selectList(new LambdaQueryWrapper<ArticleImage>()
                        .eq(ArticleImage::getArticleId, article.getId())
                        .orderByAsc(ArticleImage::getSort))
                .stream().map(ArticleImage::getUrl).toList());
        fillCollectionNav(vo, article);
        return vo;
    }

    /**
     * 文章若属于某合集，按合集内创建时间顺序填充上一篇（更早）/下一篇（更晚）。
     * 结果随详情一起缓存；文章增删改会失效文章缓存，故导航不会读到脏数据。
     */
    private void fillCollectionNav(ArticleWebDetailVO vo, Article article) {
        if (article.getCollectionId() == null || article.getCreateTime() == null) {
            return;
        }
        Article prev = lambdaQuery()
                .eq(Article::getCollectionId, article.getCollectionId())
                .eq(Article::getStatus, STATUS_PUBLISHED)
                .lt(Article::getCreateTime, article.getCreateTime())
                .orderByDesc(Article::getCreateTime)
                .last("LIMIT 1")
                .one();
        Article next = lambdaQuery()
                .eq(Article::getCollectionId, article.getCollectionId())
                .eq(Article::getStatus, STATUS_PUBLISHED)
                .gt(Article::getCreateTime, article.getCreateTime())
                .orderByAsc(Article::getCreateTime)
                .last("LIMIT 1")
                .one();
        if (prev != null) {
            vo.setPrevArticle(new ArticleNavVO(prev.getId(), prev.getTitle()));
        }
        if (next != null) {
            vo.setNextArticle(new ArticleNavVO(next.getId(), next.getTitle()));
        }
    }

    @Override
    public List<ArchiveMonthVO> listArchive() {
        JavaType archiveType = objectMapper.getTypeFactory()
                .constructCollectionType(List.class, ArchiveMonthVO.class);
        return redisCacheService.getOrLoad(RedisKeys.cache("article:archive"), archiveType, ARCHIVE_CACHE_TTL, this::loadArchive);
    }

    private List<ArchiveMonthVO> loadArchive() {
        List<Article> articles = list(new LambdaQueryWrapper<Article>()
                .eq(Article::getStatus, STATUS_PUBLISHED)
                .orderByDesc(Article::getCreateTime));
        if (CollectionUtils.isEmpty(articles)) {
            return Collections.emptyList();
        }

        Map<String, List<ArchiveArticleVO>> archiveMap = articles.stream()
                .filter(article -> article.getCreateTime() != null)
                .collect(Collectors.groupingBy(
                        article -> article.getCreateTime().format(ARCHIVE_MONTH_FORMATTER),
                        LinkedHashMap::new,
                        Collectors.mapping(this::toArchiveArticleVO, Collectors.toList())
                ));

        return archiveMap.entrySet()
                .stream()
                .map(entry -> {
                    ArchiveMonthVO vo = new ArchiveMonthVO();
                    vo.setMonth(entry.getKey());
                    vo.setArticles(entry.getValue());
                    return vo;
                })
                .toList();
    }

    @Override
    public List<ArticleCardVO> listHot(int limit) {
        int safe = Math.min(Math.max(limit, 1), 20);
        List<Article> articles = list(new LambdaQueryWrapper<Article>()
                .eq(Article::getStatus, STATUS_PUBLISHED)
                .orderByDesc(Article::getViewCount)
                .orderByDesc(Article::getCreateTime)
                .last("LIMIT " + safe));
        List<ArticleCardVO> records = articles.stream().map(this::toArticleCardVO).toList();
        fillCollectionAndTags(records);
        return records;
    }

    @Override
    public List<ArticleCardVO> listRelated(Long id, int limit) {
        int safe = Math.min(Math.max(limit, 1), 20);
        // 当前文章的标签
        List<Long> tagIds = articleTagMapper.selectList(new LambdaQueryWrapper<ArticleTag>()
                        .eq(ArticleTag::getArticleId, id))
                .stream().map(ArticleTag::getTagId).distinct().toList();
        if (tagIds.isEmpty()) {
            return Collections.emptyList();
        }
        // 共享这些标签、且非自身的文章ID
        List<Long> relatedIds = articleTagMapper.selectList(new LambdaQueryWrapper<ArticleTag>()
                        .in(ArticleTag::getTagId, tagIds)
                        .ne(ArticleTag::getArticleId, id))
                .stream().map(ArticleTag::getArticleId).distinct().toList();
        if (relatedIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Article> articles = list(new LambdaQueryWrapper<Article>()
                .eq(Article::getStatus, STATUS_PUBLISHED)
                .in(Article::getId, relatedIds)
                .orderByDesc(Article::getViewCount)
                .orderByDesc(Article::getCreateTime)
                .last("LIMIT " + safe));
        List<ArticleCardVO> records = articles.stream().map(this::toArticleCardVO).toList();
        fillCollectionAndTags(records);
        return records;
    }

    private List<Long> getPublishedArticleIdsByTag(Long tagId) {
        if (tagId == null) {
            return Collections.emptyList();
        }

        List<ArticleTag> articleTags = articleTagMapper.selectList(new LambdaQueryWrapper<ArticleTag>()
                .eq(ArticleTag::getTagId, tagId));
        if (CollectionUtils.isEmpty(articleTags)) {
            return Collections.emptyList();
        }

        List<Long> articleIds = articleTags.stream()
                .map(ArticleTag::getArticleId)
                .distinct()
                .toList();
        return list(new LambdaQueryWrapper<Article>()
                .select(Article::getId)
                .eq(Article::getStatus, STATUS_PUBLISHED)
                .in(Article::getId, articleIds))
                .stream()
                .map(Article::getId)
                .toList();
    }

    private Page<ArticleCardVO> emptyPage(int page, int size) {
        Page<ArticleCardVO> emptyPage = new Page<>(page, size, 0);
        emptyPage.setRecords(Collections.emptyList());
        return emptyPage;
    }

    private ArticleCardVO toArticleCardVO(Article article) {
        ArticleCardVO vo = new ArticleCardVO();
        BeanUtils.copyProperties(article, vo);
        return vo;
    }

    private ArchiveArticleVO toArchiveArticleVO(Article article) {
        ArchiveArticleVO vo = new ArchiveArticleVO();
        vo.setId(article.getId());
        vo.setTitle(article.getTitle());
        vo.setSummary(article.getSummary());
        vo.setCreateTime(article.getCreateTime());
        return vo;
    }

    private void fillCollectionAndTags(List<? extends ArticleCardVO> records) {
        if (CollectionUtils.isEmpty(records)) {
            return;
        }

        Set<Long> collectionIds = records.stream()
                .map(ArticleCardVO::getCollectionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Collection> collectionMap = collectionIds.isEmpty()
                ? Collections.emptyMap()
                : collectionMapper.selectBatchIds(collectionIds)
                .stream()
                .collect(Collectors.toMap(Collection::getId, Function.identity()));

        Set<Long> articleIds = records.stream()
                .map(ArticleCardVO::getId)
                .collect(Collectors.toSet());
        List<ArticleTag> articleTags = articleTagMapper.selectList(new LambdaQueryWrapper<ArticleTag>()
                .in(ArticleTag::getArticleId, articleIds));
        Set<Long> tagIds = articleTags.stream()
                .map(ArticleTag::getTagId)
                .collect(Collectors.toSet());
        Map<Long, Tag> tagMap = tagIds.isEmpty()
                ? Collections.emptyMap()
                : tagMapper.selectBatchIds(tagIds)
                .stream()
                .collect(Collectors.toMap(Tag::getId, Function.identity()));
        Map<Long, List<TagApiVO>> articleTagMap = buildArticleTagMap(articleTags, tagMap);

        records.forEach(record -> {
            Collection collection = collectionMap.get(record.getCollectionId());
            if (collection != null) {
                record.setCollectionName(collection.getName());
            }
            record.setTags(articleTagMap.getOrDefault(record.getId(), Collections.emptyList()));
        });
    }

    private Map<Long, List<TagApiVO>> buildArticleTagMap(List<ArticleTag> articleTags, Map<Long, Tag> tagMap) {
        if (CollectionUtils.isEmpty(articleTags)) {
            return Collections.emptyMap();
        }

        return articleTags.stream().collect(Collectors.groupingBy(
                ArticleTag::getArticleId,
                Collectors.collectingAndThen(Collectors.toList(), list -> {
                    List<TagApiVO> vos = new ArrayList<>();
                    for (ArticleTag articleTag : list) {
                        Tag tag = tagMap.get(articleTag.getTagId());
                        if (tag == null) {
                            continue;
                        }
                        TagApiVO vo = new TagApiVO();
                        vo.setId(tag.getId());
                        vo.setName(tag.getName());
                        vo.setColor(tag.getColor());
                        vo.setArticleCount(0L);
                        vos.add(vo);
                    }
                    return vos;
                })
        ));
    }

    private String normalize(Object value) {
        return value == null ? "_" : value.toString().trim();
    }
}
