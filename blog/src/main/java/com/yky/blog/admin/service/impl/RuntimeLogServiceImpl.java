package com.yky.blog.admin.service.impl;

import com.yky.blog.admin.service.RuntimeLogService;
import com.yky.blog.admin.vo.RuntimeLogApiMetricVO;
import com.yky.blog.admin.vo.RuntimeLogLineVO;
import com.yky.blog.admin.vo.RuntimeLogSlowRequestVO;
import com.yky.blog.admin.vo.RuntimeLogSummaryVO;
import com.yky.blog.common.exception.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class RuntimeLogServiceImpl implements RuntimeLogService {

    private static final int DEFAULT_MAX_LINES = 200;
    private static final int MAX_READ_LINES = 5000;
    private static final Pattern LOG_PATTERN = Pattern.compile(
            "^(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\s+(\\w+)\\s+\\[([^]]+)]\\s+(.+?)\\s+-\\s+(.*)$"
    );
    private static final Pattern HTTP_PATTERN = Pattern.compile(
            "^HTTP\\s+(\\w+)\\s+(.+?)\\s+->\\s+(\\d{3})\\s+\\((\\d+)\\s+ms,\\s+ip=([^)]*)\\)$"
    );

    @Value("${logging.file.name:logs/kyonelife-blog.log}")
    private String logFileName;

    @Override
    public List<RuntimeLogLineVO> recent(int lines, String level) {
        int limit = normalizeLimit(lines);
        List<RuntimeLogLineVO> matched = readRecentLines(MAX_READ_LINES).stream()
                .map(this::parseLine)
                .filter(log -> matchLevel(log, level))
                .toList();
        return tail(matched, limit);
    }

    @Override
    public List<RuntimeLogLineVO> search(String keyword, String level, int lines) {
        int limit = normalizeLimit(lines);
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        if (!StringUtils.hasText(normalizedKeyword)) {
            return recent(limit, level);
        }

        List<RuntimeLogLineVO> matched = readRecentLines(MAX_READ_LINES).stream()
                .map(this::parseLine)
                .filter(log -> matchLevel(log, level))
                .filter(log -> log.getRaw() != null && log.getRaw().toLowerCase(Locale.ROOT).contains(normalizedKeyword))
                .toList();
        return tail(matched, limit);
    }

    @Override
    public RuntimeLogSummaryVO summary(long slowThreshold) {
        long threshold = normalizeThreshold(slowThreshold);
        LocalDate today = LocalDate.now();
        List<RuntimeLogLineVO> logs = readRecentLines(MAX_READ_LINES).stream()
                .map(this::parseLine)
                .toList();

        RuntimeLogSummaryVO vo = new RuntimeLogSummaryVO();
        vo.setTodayRequestCount(logs.stream().filter(log -> isToday(log, today)).filter(this::isHttpLog).count());
        vo.setWarnCount(logs.stream().filter(log -> isToday(log, today)).filter(log -> "WARN".equals(log.getLevel())).count());
        vo.setErrorCount(logs.stream().filter(log -> isToday(log, today)).filter(log -> "ERROR".equals(log.getLevel())).count());
        vo.setSlowRequestCount(logs.stream()
                .filter(log -> isToday(log, today))
                .filter(this::isHttpLog)
                .filter(log -> log.getCostTime() != null && log.getCostTime() >= threshold)
                .count());
        vo.setLastStartTime(findLastTime(logs, "Started "));
        vo.setLastShutdownTime(findLastTime(logs, "Shutdown completed"));
        vo.setLogFilePath(logPath().toString());
        return vo;
    }

    @Override
    public List<RuntimeLogSlowRequestVO> slowRequests(long threshold, int limit) {
        long slowThreshold = normalizeThreshold(threshold);
        int size = normalizeLimit(limit);
        return readRecentLines(MAX_READ_LINES).stream()
                .map(this::parseLine)
                .filter(this::isHttpLog)
                .filter(log -> log.getCostTime() != null && log.getCostTime() >= slowThreshold)
                .sorted(Comparator.comparing(RuntimeLogLineVO::getCostTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(size)
                .map(this::toSlowRequestVO)
                .toList();
    }

    @Override
    public List<RuntimeLogApiMetricVO> topApis(int limit) {
        int size = normalizeLimit(limit);
        Map<String, List<RuntimeLogLineVO>> apiMap = readRecentLines(MAX_READ_LINES).stream()
                .map(this::parseLine)
                .filter(this::isHttpLog)
                .collect(Collectors.groupingBy(this::apiKey, LinkedHashMap::new, Collectors.toList()));

        return apiMap.values().stream()
                .map(this::toApiMetricVO)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(RuntimeLogApiMetricVO::getRequestCount).reversed())
                .limit(size)
                .toList();
    }

    private List<String> readRecentLines(int maxLines) {
        Path path = logPath();
        if (!Files.exists(path)) {
            return Collections.emptyList();
        }
        try {
            List<String> lines = Files.readAllLines(path);
            return tail(lines, maxLines);
        } catch (IOException e) {
            throw new BizException("读取运行日志失败");
        }
    }

    private RuntimeLogLineVO parseLine(String line) {
        RuntimeLogLineVO vo = new RuntimeLogLineVO();
        vo.setRaw(line);

        Matcher matcher = LOG_PATTERN.matcher(line);
        if (!matcher.matches()) {
            vo.setMessage(line);
            return vo;
        }

        vo.setTime(matcher.group(1));
        vo.setLevel(matcher.group(2));
        vo.setThread(matcher.group(3));
        vo.setLogger(matcher.group(4));
        vo.setMessage(matcher.group(5));

        Matcher httpMatcher = HTTP_PATTERN.matcher(vo.getMessage());
        if (httpMatcher.matches()) {
            vo.setMethod(httpMatcher.group(1));
            vo.setPath(httpMatcher.group(2));
            vo.setStatus(Integer.parseInt(httpMatcher.group(3)));
            vo.setCostTime(Long.parseLong(httpMatcher.group(4)));
            vo.setIp(httpMatcher.group(5));
        }
        return vo;
    }

    private RuntimeLogSlowRequestVO toSlowRequestVO(RuntimeLogLineVO log) {
        RuntimeLogSlowRequestVO vo = new RuntimeLogSlowRequestVO();
        vo.setTime(log.getTime());
        vo.setMethod(log.getMethod());
        vo.setPath(log.getPath());
        vo.setStatus(log.getStatus());
        vo.setCostTime(log.getCostTime());
        vo.setIp(log.getIp());
        return vo;
    }

    private RuntimeLogApiMetricVO toApiMetricVO(List<RuntimeLogLineVO> logs) {
        Optional<RuntimeLogLineVO> first = logs.stream().findFirst();
        if (first.isEmpty()) {
            return null;
        }
        RuntimeLogApiMetricVO vo = new RuntimeLogApiMetricVO();
        vo.setMethod(first.get().getMethod());
        vo.setPath(normalizePath(first.get().getPath()));
        vo.setRequestCount((long) logs.size());
        vo.setAverageCostTime(Math.round(logs.stream()
                .map(RuntimeLogLineVO::getCostTime)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .average()
                .orElse(0)));
        vo.setMaxCostTime(logs.stream()
                .map(RuntimeLogLineVO::getCostTime)
                .filter(Objects::nonNull)
                .max(Long::compareTo)
                .orElse(0L));
        return vo;
    }

    private String apiKey(RuntimeLogLineVO log) {
        return log.getMethod() + " " + normalizePath(log.getPath());
    }

    private String normalizePath(String path) {
        if (path == null) {
            return "";
        }
        int queryIndex = path.indexOf('?');
        return queryIndex >= 0 ? path.substring(0, queryIndex) : path;
    }

    private String findLastTime(List<RuntimeLogLineVO> logs, String keyword) {
        for (int i = logs.size() - 1; i >= 0; i--) {
            RuntimeLogLineVO log = logs.get(i);
            if (log.getMessage() != null && log.getMessage().contains(keyword)) {
                return log.getTime();
            }
        }
        return null;
    }

    private boolean isHttpLog(RuntimeLogLineVO log) {
        return log.getMethod() != null && log.getPath() != null;
    }

    private boolean isToday(RuntimeLogLineVO log, LocalDate today) {
        return log.getTime() != null && log.getTime().startsWith(today.toString());
    }

    private boolean matchLevel(RuntimeLogLineVO log, String level) {
        return !StringUtils.hasText(level) || level.equalsIgnoreCase(log.getLevel());
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_MAX_LINES;
        }
        return Math.min(limit, MAX_READ_LINES);
    }

    private long normalizeThreshold(long threshold) {
        return threshold <= 0 ? 100L : threshold;
    }

    private Path logPath() {
        return Paths.get(logFileName).toAbsolutePath().normalize();
    }

    private <T> List<T> tail(List<T> list, int limit) {
        if (list.size() <= limit) {
            return new ArrayList<>(list);
        }
        return new ArrayList<>(list.subList(list.size() - limit, list.size()));
    }
}
