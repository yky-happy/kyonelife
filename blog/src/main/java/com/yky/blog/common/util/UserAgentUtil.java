package com.yky.blog.common.util;

import java.util.Locale;

/**
 * 轻量 User-Agent 解析工具（设备/浏览器/操作系统），供埋点消费者使用。
 */
public final class UserAgentUtil {

    private UserAgentUtil() {
    }

    public static String device(String userAgent) {
        String ua = lower(userAgent);
        if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone")) {
            return "mobile";
        }
        if (ua.contains("ipad") || ua.contains("tablet")) {
            return "tablet";
        }
        return "desktop";
    }

    public static String browser(String userAgent) {
        String ua = lower(userAgent);
        if (ua.contains("edg/")) return "Edge";
        if (ua.contains("chrome/")) return "Chrome";
        if (ua.contains("firefox/")) return "Firefox";
        if (ua.contains("safari/")) return "Safari";
        return "Other";
    }

    public static String os(String userAgent) {
        String ua = lower(userAgent);
        if (ua.contains("windows")) return "Windows";
        if (ua.contains("mac os")) return "macOS";
        if (ua.contains("android")) return "Android";
        if (ua.contains("iphone") || ua.contains("ipad")) return "iOS";
        if (ua.contains("linux")) return "Linux";
        return "Other";
    }

    public static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
