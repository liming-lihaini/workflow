package com.flow.engine.controllers;

import com.flow.engine.common.Result;
import com.flow.engine.entity.AccessLog;
import com.flow.engine.entity.OperationLog;
import com.flow.engine.service.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 日志管理API（ISSUE-014, TRD §3.11）
 */
@RestController
@RequestMapping("/api/v1/system/logs")
@RequiredArgsConstructor
public class LogController {

    private final LogService logService;

    /**
     * 获取访问日志（分页）
     */
    @GetMapping("/access")
    public Result<Map<String, Object>> queryAccessLog(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String url,
            @RequestParam(required = false) Integer result,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        List<AccessLog> logs = logService.queryAccessLog(userId, username, url, result, startTime, endTime, page, size);
        long total = logService.countAccessLog(userId, username, url, result, startTime, endTime);
        
        Map<String, Object> data = new HashMap<>();
        data.put("list", logs);
        data.put("total", total);
        data.put("page", page);
        data.put("size", size);
        
        return Result.ok(data);
    }

    /**
     * 导出访问日志
     */
    @GetMapping("/access/export")
    public Result<List<AccessLog>> exportAccessLog(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String url,
            @RequestParam(required = false) Integer result,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        
        List<AccessLog> logs = logService.exportAccessLog(userId, username, url, result, startTime, endTime);
        return Result.ok(logs);
    }

    /**
     * 获取操作日志（分页）
     */
    @GetMapping("/operation")
    public Result<Map<String, Object>> queryOperationLog(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        List<OperationLog> logs = logService.queryOperationLog(userId, username, module, operation, startTime, endTime, page, size);
        long total = logService.countOperationLog(userId, username, module, operation, startTime, endTime);
        
        Map<String, Object> data = new HashMap<>();
        data.put("list", logs);
        data.put("total", total);
        data.put("page", page);
        data.put("size", size);
        
        return Result.ok(data);
    }

    /**
     * 导出操作日志
     */
    @GetMapping("/operation/export")
    public Result<List<OperationLog>> exportOperationLog(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        
        List<OperationLog> logs = logService.exportOperationLog(userId, username, module, operation, startTime, endTime);
        return Result.ok(logs);
    }

    /**
     * 清理过期日志
     */
    @DeleteMapping("/clean")
    public Result<Map<String, Integer>> cleanExpiredLogs(
            @RequestParam(defaultValue = "30") int retentionDays) {
        
        logService.cleanExpiredLogs(retentionDays);
        
        Map<String, Integer> data = new HashMap<>();
        data.put("retentionDays", retentionDays);
        
        return Result.ok(data);
    }
}
