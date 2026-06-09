package com.yky.blog.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yky.blog.admin.service.OperationLogService;
import com.yky.blog.common.entity.OperationLog;
import com.yky.blog.common.mapper.OperationLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl implements OperationLogService {

    private final OperationLogMapper operationLogMapper;

    @Async
    @Override
    public void saveLog(OperationLog logEntity) {
        try {
            operationLogMapper.insert(logEntity);
        } catch (Exception e) {
            log.warn("保存操作日志失败: {}", e.getMessage());
        }
    }

    @Override
    public IPage<OperationLog> pageLog(int page,
                                       int size,
                                       String module,
                                       String operation,
                                       Integer success,
                                       LocalDateTime startTime,
                                       LocalDateTime endTime) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(module), OperationLog::getModule, module)
                .like(StringUtils.hasText(operation), OperationLog::getOperation, operation)
                .eq(success != null, OperationLog::getSuccess, success)
                .ge(startTime != null, OperationLog::getCreateTime, startTime)
                .le(endTime != null, OperationLog::getCreateTime, endTime)
                .orderByDesc(OperationLog::getCreateTime);
        return operationLogMapper.selectPage(new Page<>(page, size), wrapper);
    }
}
