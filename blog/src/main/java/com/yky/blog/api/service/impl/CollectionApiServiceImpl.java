package com.yky.blog.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yky.blog.api.dto.ArticleCardVO;
import com.yky.blog.api.dto.CollectionApiVO;
import com.yky.blog.api.service.ArticleApiService;
import com.yky.blog.api.service.CollectionApiService;
import com.yky.blog.common.entity.Article;
import com.yky.blog.common.entity.Collection;
import com.yky.blog.common.exception.BizException;
import com.yky.blog.common.mapper.ArticleMapper;
import com.yky.blog.common.mapper.CollectionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CollectionApiServiceImpl extends ServiceImpl<CollectionMapper, Collection> implements CollectionApiService {

    private static final int STATUS_PUBLISHED = 1;

    private final ArticleMapper articleMapper;
    private final ArticleApiService articleApiService;

    @Override
    public List<CollectionApiVO> listCollections() {
        List<Collection> collections = list(new LambdaQueryWrapper<Collection>()
                .orderByAsc(Collection::getSort)
                .orderByDesc(Collection::getCreateTime));
        if (CollectionUtils.isEmpty(collections)) {
            return Collections.emptyList();
        }

        Map<Long, Long> articleCountMap = countPublishedArticlesByCollection();
        return collections.stream()
                .map(collection -> {
                    CollectionApiVO vo = new CollectionApiVO();
                    BeanUtils.copyProperties(collection, vo);
                    vo.setArticleCount(articleCountMap.getOrDefault(collection.getId(), 0L));
                    return vo;
                })
                .toList();
    }

    @Override
    public IPage<ArticleCardVO> pageCollectionArticles(Long id, int page, int size) {
        if (getById(id) == null) {
            throw new BizException("合集不存在");
        }
        return articleApiService.pageArticle(page, size, null, null, id);
    }

    private Map<Long, Long> countPublishedArticlesByCollection() {
        List<Article> articles = articleMapper.selectList(new LambdaQueryWrapper<Article>()
                .select(Article::getCollectionId)
                .eq(Article::getStatus, STATUS_PUBLISHED)
                .isNotNull(Article::getCollectionId));
        if (CollectionUtils.isEmpty(articles)) {
            return Collections.emptyMap();
        }

        return articles.stream()
                .collect(Collectors.groupingBy(Article::getCollectionId, Collectors.counting()));
    }
}
