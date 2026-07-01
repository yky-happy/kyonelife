package com.yky.blog.common.exception;

/**
 * 触发限流时抛出，由全局异常处理器转为 429 响应。
 */
public class RateLimitException extends RuntimeException {

    public RateLimitException(String message) {
        super(message);
    }
}
