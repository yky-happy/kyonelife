package com.yky.blog.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yky.blog.api.dto.ArchiveArticleVO;
import com.yky.blog.api.dto.ArchiveMonthVO;
import com.yky.blog.api.dto.ArticleCardVO;
import com.yky.blog.api.dto.ArticleWebDetailVO;
import com.yky.blog.api.dto.TagApiVO;
import com.yky.blog.api.service.ArticleApiService;
import com.yky.blog.common.entity.Article;
import com.yky.blog.common.entity.ArticleTag;
import com.yky.blog.common.entity.Collection;
import com.yky.blog.common.entity.Tag;
import com.yky.blog.common.exception.BizException;
import com.yky.blog.common.mapper.ArticleMapper;
import com.yky.blog.common.mapper.ArticleTagMapper;
import com.yky.blog.common.mapper.CollectionMapper;
import com.yky.blog.common.mapper.TagMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

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

    private final ArticleTagMapper articleTagMapper;
    private final CollectionMapper collectionMapper;
    private final TagMapper tagMapper;

    @Override
    public IPage<ArticleCardVO> pageArticle(int page, int size, String keyword, Long tagId, Long collectionId) {
        List<Long> tagArticleIds = getPublishedArticleIdsByTag(tagId);
        if (tagId != null && tagArticleIds.isEmpty()) {
            return emptyPage(page, size);
        }

        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<Article>()
                .eq(Article::getStatus, STATUS_PUBLISHED)
                .like(StringUtils.hasText(keyword), Article::getTitle, keyword)
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
        Article article = getById(id);
        if (article == null || !Objects.equals(article.getStatus(), STATUS_PUBLISHED)) {
            throw new BizException("文章不存在");
        }

        ArticleWebDetailVO vo = new ArticleWebDetailVO();
        BeanUtils.copyProperties(article, vo);
        fillCollectionAndTags(List.of(vo));
        increaseViewCount(article);
        vo.setViewCount(article.getViewCount() == null ? 1L : article.getViewCount() + 1);
        return vo;
    }

    @Override
    public List<ArchiveMonthVO> listArchive() {
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

    private void increaseViewCount(Article article) {
        Article update = new Article();
        update.setId(article.getId());
        update.setViewCount(article.getViewCount() == null ? 1L : article.getViewCount() + 1);
        updateById(update);
    }
}
