package com.yky.blog.api.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yky.blog.api.dto.ArticleCardVO;
import com.yky.blog.api.dto.CollectionApiVO;

import java.util.List;

public interface CollectionApiService {

    List<CollectionApiVO> listCollections();

    IPage<ArticleCardVO> pageCollectionArticles(Long id, int page, int size);
}
