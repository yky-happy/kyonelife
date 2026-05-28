package com.yky.blog.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yky.blog.admin.dto.ArticleSaveDTO;
import com.yky.blog.admin.vo.ArticleDetailVO;
import com.yky.blog.admin.vo.ArticleVO;
import com.yky.blog.common.entity.Article;

public interface ArticleService extends IService<Article> {

    IPage<ArticleVO> pageArticle(int page, int size, String keyword, Integer status);

    ArticleDetailVO getArticleDetail(Long id);

    Long saveArticle(ArticleSaveDTO dto);

    void updateArticle(Long id, ArticleSaveDTO dto);

    void updateArticleStatus(Long id, Integer status);

    void removeArticle(Long id);
}
