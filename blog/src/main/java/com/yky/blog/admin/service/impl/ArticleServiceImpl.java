package com.yky.blog.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yky.blog.admin.dto.ArticleSaveDTO;
import com.yky.blog.admin.service.ArticleService;
import com.yky.blog.admin.vo.ArticleDetailVO;
import com.yky.blog.admin.vo.ArticleVO;
import com.yky.blog.admin.vo.TagVO;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService {

    private final ArticleTagMapper articleTagMapper;
    private final CollectionMapper collectionMapper;
    private final TagMapper tagMapper;

    @Override
    public IPage<ArticleVO> pageArticle(int page, int size, String keyword, Integer status) {
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<Article>()
                .like(StringUtils.hasText(keyword), Article::getTitle, keyword)
                .eq(status != null, Article::getStatus, status)
                .orderByDesc(Article::getIsStick)
                .orderByDesc(Article::getCreateTime);

        IPage<Article> articlePage = page(new Page<>(page, size), wrapper);
        List<ArticleVO> records = articlePage.getRecords()
                .stream()
                .map(this::toArticleVO)
                .toList();
        fillCollectionAndTags(records);

        Page<ArticleVO> voPage = new Page<>(articlePage.getCurrent(), articlePage.getSize(), articlePage.getTotal());
        voPage.setRecords(records);
        return voPage;
    }

    @Override
    public ArticleDetailVO getArticleDetail(Long id) {
        Article article = getById(id);
        if (article == null) {
            throw new BizException("文章不存在");
        }

        ArticleDetailVO vo = new ArticleDetailVO();
        BeanUtils.copyProperties(article, vo);
        fillCollectionAndTags(List.of(vo));
        vo.setTagIds(vo.getTags() == null
                ? Collections.emptyList()
                : vo.getTags().stream().map(TagVO::getId).toList());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveArticle(ArticleSaveDTO dto) {
        checkStatus(dto.getStatus());
        checkOriginal(dto);

        Article article = new Article();
        BeanUtils.copyProperties(dto, article);
        fillDefaults(article);
        save(article);
        saveArticleTags(article.getId(), dto.getTagIds());
        return article.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateArticle(Long id, ArticleSaveDTO dto) {
        if (getById(id) == null) {
            throw new BizException("文章不存在");
        }
        checkStatus(dto.getStatus());
        checkOriginal(dto);

        Article article = new Article();
        BeanUtils.copyProperties(dto, article);
        article.setId(id);
        fillDefaults(article);
        updateById(article);
        deleteArticleTags(id);
        saveArticleTags(id, dto.getTagIds());
    }

    @Override
    public void updateArticleStatus(Long id, Integer status) {
        if (getById(id) == null) {
            throw new BizException("文章不存在");
        }
        checkStatus(status);
        Article article = new Article();
        article.setId(id);
        article.setStatus(status);
        updateById(article);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeArticle(Long id) {
        if (getById(id) == null) {
            throw new BizException("文章不存在");
        }
        deleteArticleTags(id);
        removeById(id);
    }

    private ArticleVO toArticleVO(Article article) {
        ArticleVO vo = new ArticleVO();
        BeanUtils.copyProperties(article, vo);
        return vo;
    }

    private void fillCollectionAndTags(List<? extends ArticleVO> records) {
        if (CollectionUtils.isEmpty(records)) {
            return;
        }

        Set<Long> collectionIds = records.stream()
                .map(ArticleVO::getCollectionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Collection> collectionMap = collectionIds.isEmpty()
                ? Collections.emptyMap()
                : collectionMapper.selectBatchIds(collectionIds)
                .stream()
                .collect(Collectors.toMap(Collection::getId, Function.identity()));

        Set<Long> articleIds = records.stream()
                .map(ArticleVO::getId)
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
        Map<Long, List<TagVO>> articleTagMap = buildArticleTagMap(articleTags, tagMap);

        records.forEach(record -> {
            Collection collection = collectionMap.get(record.getCollectionId());
            if (collection != null) {
                record.setCollectionName(collection.getName());
            }
            record.setTags(articleTagMap.getOrDefault(record.getId(), Collections.emptyList()));
        });
    }

    private Map<Long, List<TagVO>> buildArticleTagMap(List<ArticleTag> articleTags, Map<Long, Tag> tagMap) {
        if (CollectionUtils.isEmpty(articleTags)) {
            return Collections.emptyMap();
        }
        return articleTags.stream().collect(Collectors.groupingBy(
                ArticleTag::getArticleId,
                Collectors.collectingAndThen(Collectors.toList(), list -> {
                    List<TagVO> vos = new ArrayList<>();
                    for (ArticleTag articleTag : list) {
                        Tag tag = tagMap.get(articleTag.getTagId());
                        if (tag == null) {
                            continue;
                        }
                        TagVO vo = new TagVO();
                        vo.setId(tag.getId());
                        vo.setName(tag.getName());
                        vo.setColor(tag.getColor());
                        vo.setCreateTime(tag.getCreateTime());
                        vos.add(vo);
                    }
                    return vos;
                })
        ));
    }

    private void saveArticleTags(Long articleId, List<Long> tagIds) {
        if (CollectionUtils.isEmpty(tagIds)) {
            return;
        }
        Set<Long> distinctTagIds = new LinkedHashSet<>(tagIds);
        distinctTagIds.forEach(tagId -> {
            ArticleTag articleTag = new ArticleTag();
            articleTag.setArticleId(articleId);
            articleTag.setTagId(tagId);
            articleTagMapper.insert(articleTag);
        });
    }

    private void deleteArticleTags(Long articleId) {
        articleTagMapper.delete(new LambdaQueryWrapper<ArticleTag>()
                .eq(ArticleTag::getArticleId, articleId));
    }

    private void fillDefaults(Article article) {
        if (article.getContent() == null) {
            article.setContent(article.getContentMd());
        }
        if (article.getIsStick() == null) {
            article.setIsStick(0);
        }
        if (article.getIsCarousel() == null) {
            article.setIsCarousel(0);
        }
        if (article.getCarouselSort() == null) {
            article.setCarouselSort(0);
        }
        if (article.getViewCount() == null) {
            article.setViewCount(0L);
        }
    }

    private void checkStatus(Integer status) {
        if (status == null || (status != 0 && status != 1 && status != 2)) {
            throw new BizException("文章状态不正确");
        }
    }

    private void checkOriginal(ArticleSaveDTO dto) {
        if (dto.getIsOriginal() == null || (dto.getIsOriginal() != 0 && dto.getIsOriginal() != 1)) {
            throw new BizException("原创状态不正确");
        }
        if (dto.getIsOriginal() == 0 && !StringUtils.hasText(dto.getOriginalUrl())) {
            throw new BizException("转载文章请填写原文链接");
        }
    }
}
