package com.yky.blog.api.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yky.blog.api.dto.ArchiveMonthVO;
import com.yky.blog.api.dto.ArticleCardVO;
import com.yky.blog.api.dto.ArticleWebDetailVO;

import java.util.List;

public interface ArticleApiService {

    IPage<ArticleCardVO> pageArticle(int page, int size, String keyword, Long tagId, Long collectionId);

    ArticleWebDetailVO getArticleDetail(Long id);

    List<ArchiveMonthVO> listArchive();

    /** 热门文章（按阅读量倒序）。 */
    List<ArticleCardVO> listHot(int limit);

    /** 相关文章推荐（按共同标签）。 */
    List<ArticleCardVO> listRelated(Long id, int limit);
}
