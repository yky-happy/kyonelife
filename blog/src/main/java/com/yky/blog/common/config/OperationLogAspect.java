package com.yky.blog.common.config;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yky.blog.admin.service.OperationLogService;
import com.yky.blog.common.annotation.OperationLogRecord;
import com.yky.blog.common.entity.Admin;
import com.yky.blog.common.entity.OperationLog;
import com.yky.blog.common.mapper.AdminMapper;
import com.yky.blog.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private static final int MAX_TEXT_LENGTH = 2000;

    private final OperationLogService operationLogService;
    private final AdminMapper adminMapper;
    private final ObjectMapper objectMapper;

    @Around("@annotation(operationLogRecord)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLogRecord operationLogRecord) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = null;
        Throwable error = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable throwable) {
            error = throwable;
            throw throwable;
        } finally {
            OperationLog logEntity = buildLog(joinPoint, operationLogRecord, result, error, System.currentTimeMillis() - start);
            operationLogService.saveLog(logEntity);
        }
    }

    private OperationLog buildLog(ProceedingJoinPoint joinPoint,
                                  OperationLogRecord operationLogRecord,
                                  Object result,
                                  Throwable error,
                                  long costTime) {
        HttpServletRequest request = currentRequest();
        OperationLog logEntity = new OperationLog();
        logEntity.setModule(operationLogRecord.module());
        logEntity.setOperation(operationLogRecord.operation());
        logEntity.setRequestMethod(request == null ? null : request.getMethod());
        logEntity.setRequestPath(request == null ? null : request.getRequestURI());
        logEntity.setRequestParams(buildRequestParams(joinPoint, request));
        logEntity.setIp(request == null ? null : getClientIp(request));
        logEntity.setUserAgent(request == null ? null : truncate(request.getHeader("User-Agent")));
        logEntity.setCostTime(costTime);
        logEntity.setSuccess(error == null ? 1 : 0);
        logEntity.setCreateTime(LocalDateTime.now());

        fillAdmin(logEntity);
        fillResponse(logEntity, result, error);
        return logEntity;
    }

    private void fillAdmin(OperationLog logEntity) {
        try {
            if (!StpUtil.isLogin()) {
                return;
            }
            Long adminId = StpUtil.getLoginIdAsLong();
            logEntity.setAdminId(adminId);
            Admin admin = adminMapper.selectById(adminId);
            if (admin != null) {
                logEntity.setAdminName(StringUtils.hasText(admin.getNickname()) ? admin.getNickname() : admin.getUsername());
            }
        } catch (Exception ignored) {
            // 登录失败时可能还没有 Sa-Token 上下文，管理员信息允许为空。
        }
    }

    private void fillResponse(OperationLog logEntity, Object result, Throwable error) {
        if (error != null) {
            logEntity.setResponseCode(500);
            logEntity.setResponseMessage("操作失败");
            logEntity.setErrorMessage(truncate(error.getMessage()));
            return;
        }
        if (result instanceof Result<?> response) {
            logEntity.setResponseCode(response.getCode());
            logEntity.setResponseMessage(truncate(response.getMessage()));
        } else {
            logEntity.setResponseCode(200);
            logEntity.setResponseMessage("操作成功");
        }
    }

    private String buildRequestParams(ProceedingJoinPoint joinPoint, HttpServletRequest request) {
        Map<String, Object> data = new LinkedHashMap<>();
        if (request != null) {
            request.getParameterMap().forEach((key, value) -> data.put(key, maskIfSensitive(key, value.length == 1 ? value[0] : value)));
        }
        Object[] args = joinPoint.getArgs();
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg == null || shouldSkipArg(arg)) {
                continue;
            }
            data.put("arg" + i, sanitizeObject(arg));
        }
        return truncate(toJson(data));
    }

    @SuppressWarnings("unchecked")
    private Object sanitizeObject(Object value) {
        Map<String, Object> raw;
        try {
            raw = objectMapper.convertValue(value, Map.class);
        } catch (IllegalArgumentException e) {
            return value;
        }
        raw.replaceAll(this::maskIfSensitive);
        return raw;
    }

    private Object maskIfSensitive(String key, Object value) {
        String lowerKey = key == null ? "" : key.toLowerCase();
        if (lowerKey.contains("password") || lowerKey.contains("token") || lowerKey.contains("secret")) {
            return "***";
        }
        return value;
    }

    private boolean shouldSkipArg(Object arg) {
        String className = arg.getClass().getName();
        return arg instanceof MultipartFile
                || arg instanceof HttpServletRequest
                || className.startsWith("jakarta.servlet.")
                || className.startsWith("org.springframework.web.multipart.");
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

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private String truncate(String value) {
        if (value == null || value.length() <= MAX_TEXT_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_TEXT_LENGTH);
    }
}
