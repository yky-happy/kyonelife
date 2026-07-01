package com.yky.blog.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流注解，基于 Redis 滑动窗口实现，按客户端 IP 维度限流。
 * 标注在 Controller 方法上即可生效。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * 限流标识前缀，用于区分不同接口（拼入 Redis key）。
     */
    String name();

    /**
     * 滑动窗口时长，单位秒。
     */
    int window() default 10;

    /**
     * 窗口内允许的最大请求次数，超出即拒绝。
     */
    int limit() default 30;

    /**
     * 触发限流时返回给前端的提示信息。
     */
    String message() default "请求过于频繁，请稍后再试";
}
