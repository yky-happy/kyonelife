package com.yky.blog.api.service;

public interface ArticleViewCountService {

    long increaseAndGetDelta(Long articleId);

    long getDelta(Long articleId);

    void flushToDb();
}
