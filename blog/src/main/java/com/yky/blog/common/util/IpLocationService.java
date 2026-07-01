package com.yky.blog.common.util;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.xdb.Searcher;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.InputStream;

/**
 * 基于 ip2region 离线库的 IP 归属地解析。
 * 启动时把 xdb 全量读入内存，单例共享、线程安全；运行时不联网。
 */
@Slf4j
@Component
public class IpLocationService {

    private Searcher searcher;

    @PostConstruct
    public void init() {
        try (InputStream in = new ClassPathResource("ip2region.xdb").getInputStream()) {
            byte[] buff = in.readAllBytes();
            this.searcher = Searcher.newWithBuffer(buff);
            log.info("ip2region 离线库加载成功（{} 字节）", buff.length);
        } catch (Exception e) {
            log.error("ip2region 离线库加载失败，IP 归属地将为空：{}", e.getMessage());
        }
    }

    /**
     * 解析 IP 归属地。
     * @return 形如 "广东省 深圳市"；内网返回 "内网"；无法解析返回 null。
     */
    public String resolve(String ip) {
        if (!StringUtils.hasText(ip)) {
            return null;
        }
        if (isPrivate(ip.trim())) {
            return "内网";
        }
        if (searcher == null) {
            return null;
        }
        try {
            return format(searcher.search(ip.trim()));
        } catch (Exception e) {
            return null;
        }
    }

    /** ip2region 返回格式：国家|区域|省份|城市|ISP，空段为 "0"。 */
    private String format(String region) {
        if (!StringUtils.hasText(region)) {
            return null;
        }
        String[] p = region.split("\\|");
        String country = seg(p, 0);
        String province = seg(p, 2);
        String city = seg(p, 3);
        if ("中国".equals(country)) {
            StringBuilder sb = new StringBuilder();
            if (province != null) {
                sb.append(province);
            }
            if (city != null && !city.equals(province)) {
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                sb.append(city);
            }
            return sb.length() > 0 ? sb.toString() : "中国";
        }
        return country;
    }

    private String seg(String[] arr, int i) {
        if (arr.length <= i) {
            return null;
        }
        String v = arr[i];
        return (v == null || v.isEmpty() || "0".equals(v)) ? null : v;
    }

    private boolean isPrivate(String ip) {
        if (ip.equals("127.0.0.1") || ip.equals("::1") || ip.equals("0:0:0:0:0:0:0:1")) {
            return true;
        }
        if (ip.startsWith("10.") || ip.startsWith("192.168.")) {
            return true;
        }
        if (ip.startsWith("172.")) {
            try {
                int second = Integer.parseInt(ip.split("\\.")[1]);
                if (second >= 16 && second <= 31) {
                    return true;
                }
            } catch (Exception ignore) {
                // 非标准格式，忽略
            }
        }
        return ip.startsWith("fe80:") || ip.startsWith("fc") || ip.startsWith("fd");
    }
}
