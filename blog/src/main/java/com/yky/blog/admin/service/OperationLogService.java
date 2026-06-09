package com.yky.blog.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yky.blog.common.entity.OperationLog;

import java.time.LocalDateTime;

public interface OperationLogService {

    void saveLog(OperationLog log);

    IPage<OperationLog> pageLog(int page,
                                int size,
                                String module,
                                String operation,
                                Integer success,
                                LocalDateTime startTime,
                                LocalDateTime endTime);
}
