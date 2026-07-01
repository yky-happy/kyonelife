package com.yky.blog.common.util;

import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 简易敏感词过滤：命中则用 * 替换（屏蔽而非拒绝）。
 * 词库可按需扩展；大数据量场景可换 DFA / 第三方库。
 */
@Component
public class SensitiveWordFilter {

    private static final Set<String> WORDS = Set.of(
            "法轮", "赌博", "博彩", "色情", "招嫖", "毒品", "枪支",
            "代开发票", "办证", "加我微信", "加微信", "刷单", "贷款"
    );

    public String filter(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String result = text;
        for (String w : WORDS) {
            if (result.contains(w)) {
                result = result.replace(w, "*".repeat(w.length()));
            }
        }
        return result;
    }
}
