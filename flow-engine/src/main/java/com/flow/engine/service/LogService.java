package com.flow.engine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flow.engine.entity.AccessLog;
import com.flow.engine.entity.OperationLog;
import com.flow.engine.mapper.AccessLogMapper;
import com.flow.engine.mapper.OperationLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 日志服务（ISSUE-014, TRD §5.5.4）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogService {

    private final AccessLogMapper accessLogMapper;
    private final OperationLogMapper operationLogMapper;

    /**
     * 记录访问日志
     */
    public void recordAccessLog(AccessLog accessLog) {
        if (accessLog.getAccessTime() == null) {
            accessLog.setAccessTime(LocalDateTime.now());
        }
        accessLogMapper.insert(accessLog);
        log.debug("[LogService] 记录访问日志: url={}, method={}, result={}", 
                accessLog.getUrl(), accessLog.getMethod(), accessLog.getResult());
    }

    /**
     * 记录操作日志
     */
    public void recordOperationLog(OperationLog operationLog) {
        if (operationLog.getOperationTime() == null) {
            operationLog.setOperationTime(LocalDateTime.now());
        }
        operationLogMapper.insert(operationLog);
        log.debug("[LogService] 记录操作日志: module={}, operation={}, user={}", 
                operationLog.getModule(), operationLog.getOperation(), operationLog.getUsername());
    }

    /**
     * 查询访问日志（支持分页和过滤）
     */
    public List<AccessLog> queryAccessLog(Long userId, String username, String url, 
                                          Integer result, LocalDateTime startTime, LocalDateTime endTime,
                                          int page, int size) {
        LambdaQueryWrapper<AccessLog> wrapper = new LambdaQueryWrapper<>();
        
        if (userId != null) {
            wrapper.eq(AccessLog::getUserId, userId);
        }
        if (StringUtils.hasText(username)) {
            wrapper.like(AccessLog::getUsername, username);
        }
        if (StringUtils.hasText(url)) {
            wrapper.like(AccessLog::getUrl, url);
        }
        if (result != null) {
            wrapper.eq(AccessLog::getResult, result);
        }
        if (startTime != null) {
            wrapper.ge(AccessLog::getAccessTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(AccessLog::getAccessTime, endTime);
        }
        
        wrapper.orderByDesc(AccessLog::getAccessTime);
        
        // 简单分页
        int offset = (page - 1) * size;
        wrapper.last("LIMIT " + size + " OFFSET " + offset);
        
        return accessLogMapper.selectList(wrapper);
    }

    /**
     * 查询操作日志（支持分页和过滤）
     */
    public List<OperationLog> queryOperationLog(Long userId, String username, String module, 
                                                String operation, LocalDateTime startTime, LocalDateTime endTime,
                                                int page, int size) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        
        if (userId != null) {
            wrapper.eq(OperationLog::getUserId, userId);
        }
        if (StringUtils.hasText(username)) {
            wrapper.like(OperationLog::getUsername, username);
        }
        if (StringUtils.hasText(module)) {
            wrapper.like(OperationLog::getModule, module);
        }
        if (StringUtils.hasText(operation)) {
            wrapper.like(OperationLog::getOperation, operation);
        }
        if (startTime != null) {
            wrapper.ge(OperationLog::getOperationTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(OperationLog::getOperationTime, endTime);
        }
        
        wrapper.orderByDesc(OperationLog::getOperationTime);
        
        // 简单分页
        int offset = (page - 1) * size;
        wrapper.last("LIMIT " + size + " OFFSET " + offset);
        
        return operationLogMapper.selectList(wrapper);
    }

    /**
     * 导出访问日志（返回全部符合条件的记录）
     */
    public List<AccessLog> exportAccessLog(Long userId, String username, String url, 
                                           Integer result, LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<AccessLog> wrapper = new LambdaQueryWrapper<>();
        
        if (userId != null) {
            wrapper.eq(AccessLog::getUserId, userId);
        }
        if (StringUtils.hasText(username)) {
            wrapper.like(AccessLog::getUsername, username);
        }
        if (StringUtils.hasText(url)) {
            wrapper.like(AccessLog::getUrl, url);
        }
        if (result != null) {
            wrapper.eq(AccessLog::getResult, result);
        }
        if (startTime != null) {
            wrapper.ge(AccessLog::getAccessTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(AccessLog::getAccessTime, endTime);
        }
        
        wrapper.orderByDesc(AccessLog::getAccessTime);
        
        return accessLogMapper.selectList(wrapper);
    }

    /**
     * 导出操作日志（返回全部符合条件的记录）
     */
    public List<OperationLog> exportOperationLog(Long userId, String username, String module, 
                                                 String operation, LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        
        if (userId != null) {
            wrapper.eq(OperationLog::getUserId, userId);
        }
        if (StringUtils.hasText(username)) {
            wrapper.like(OperationLog::getUsername, username);
        }
        if (StringUtils.hasText(module)) {
            wrapper.like(OperationLog::getModule, module);
        }
        if (StringUtils.hasText(operation)) {
            wrapper.like(OperationLog::getOperation, operation);
        }
        if (startTime != null) {
            wrapper.ge(OperationLog::getOperationTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(OperationLog::getOperationTime, endTime);
        }
        
        wrapper.orderByDesc(OperationLog::getOperationTime);
        
        return operationLogMapper.selectList(wrapper);
    }

    /**
     * 清理过期日志
     */
    public void cleanExpiredLogs(int retentionDays) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(retentionDays);
        
        // 清理访问日志
        int deletedAccess = accessLogMapper.delete(
                new LambdaQueryWrapper<AccessLog>().lt(AccessLog::getAccessTime, cutoffTime)
        );
        
        // 清理操作日志
        int deletedOperation = operationLogMapper.delete(
                new LambdaQueryWrapper<OperationLog>().lt(OperationLog::getOperationTime, cutoffTime)
        );
        
        log.info("[LogService] 清理过期日志: 保留天数={}, 删除访问日志{}条, 删除操作日志{}条", 
                retentionDays, deletedAccess, deletedOperation);
    }

    /**
     * 统计访问日志数量
     */
    public long countAccessLog(Long userId, String username, String url, 
                               Integer result, LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<AccessLog> wrapper = new LambdaQueryWrapper<>();
        
        if (userId != null) {
            wrapper.eq(AccessLog::getUserId, userId);
        }
        if (StringUtils.hasText(username)) {
            wrapper.like(AccessLog::getUsername, username);
        }
        if (StringUtils.hasText(url)) {
            wrapper.like(AccessLog::getUrl, url);
        }
        if (result != null) {
            wrapper.eq(AccessLog::getResult, result);
        }
        if (startTime != null) {
            wrapper.ge(AccessLog::getAccessTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(AccessLog::getAccessTime, endTime);
        }
        
        return accessLogMapper.selectCount(wrapper);
    }

    /**
     * 统计操作日志数量
     */
    public long countOperationLog(Long userId, String username, String module, 
                                  String operation, LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        
        if (userId != null) {
            wrapper.eq(OperationLog::getUserId, userId);
        }
        if (StringUtils.hasText(username)) {
            wrapper.like(OperationLog::getUsername, username);
        }
        if (StringUtils.hasText(module)) {
            wrapper.like(OperationLog::getModule, module);
        }
        if (StringUtils.hasText(operation)) {
            wrapper.like(OperationLog::getOperation, operation);
        }
        if (startTime != null) {
            wrapper.ge(OperationLog::getOperationTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(OperationLog::getOperationTime, endTime);
        }
        
        return operationLogMapper.selectCount(wrapper);
    }
}
