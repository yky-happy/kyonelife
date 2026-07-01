package com.yky.blog.common.config;

import com.yky.blog.common.annotation.RateLimit;
import com.yky.blog.common.exception.RateLimitException;
import com.yky.blog.common.redis.RateLimiter;
import com.yky.blog.common.redis.RedisKeys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

/**
 * 限流切面：拦截标注 {@link RateLimit} 的方法，按客户端 IP 做滑动窗口限流。
 *
 * <p>选用 IP 作为限流维度而非 visitorId：visitorId 由前端生成、可被轻易伪造/轮换，
 * 无法防住刷量；IP 才是真实成本维度，对污染统计的批量请求更有约束力。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RateLimiter rateLimiter;

    @Before("@annotation(rateLimit)")
    public void check(RateLimit rateLimit) {
        HttpServletRequest request = currentRequest();
        String ip = request == null ? "unknown" : getClientIp(request);
        String key = RedisKeys.rateLimit(rateLimit.name(), ip);
        boolean allowed = rateLimiter.tryAcquire(
                key, rateLimit.limit(), Duration.ofSeconds(rateLimit.window()));
        if (!allowed) {
            log.warn("触发限流: name={}, ip={}, limit={}/{}s",
                    rateLimit.name(), ip, rateLimit.limit(), rateLimit.window());
            throw new RateLimitException(rateLimit.message());
        }
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        return StringUtils.hasText(realIp) ? realIp : request.getRemoteAddr();
    }
}
