package com.flow.engine.controllers;

import com.flow.engine.common.Result;
import com.flow.engine.entity.OperationLog;
import com.flow.engine.service.TripleAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 三员管理API（ISSUE-016, TRD §3.13）
 */
@RestController
@RequestMapping("/api/v1/system/admin")
@RequiredArgsConstructor
public class AdminController {

    private final TripleAdminService tripleAdminService;

    /**
     * 获取管理员类型
     */
    @GetMapping("/types")
    public Result<List<Map<String, Object>>> getAdminTypes() {
        return Result.ok(tripleAdminService.getAdminTypes());
    }

    /**
     * 获取三员用户列表
     */
    @GetMapping("/users")
    public Result<List<Map<String, Object>>> getTripleAdminUsers(
            @RequestParam(required = false) String adminType) {
        return Result.ok(tripleAdminService.getTripleAdminUsers(adminType));
    }

    /**
     * 获取三员操作审计日志
     */
    @GetMapping("/audit-logs")
    public Result<Map<String, Object>> getTripleAdminAuditLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        List<OperationLog> logs = tripleAdminService.getTripleAdminAuditLogs(
                userId, module, startTime, endTime, page, size);
        
        Map<String, Object> data = new HashMap<>();
        data.put("list", logs);
        data.put("page", page);
        data.put("size", size);
        
        return Result.ok(data);
    }
}
