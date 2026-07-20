package com.flow.engine.controllers;

import com.flow.engine.common.Result;
import com.flow.engine.service.ProcessMonitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 流程监控API（ISSUE-017）
 */
@RestController
@RequestMapping("/api/v1/monitor")
@RequiredArgsConstructor
public class MonitorController {

    private final ProcessMonitorService processMonitorService;

    /**
     * 获取执行轨迹/历史
     */
    @GetMapping("/instances/{id}/history")
    public Result<List<Map<String, Object>>> getExecutionHistory(@PathVariable Long id) {
        return Result.ok(processMonitorService.getExecutionHistory(id));
    }

    /**
     * 获取变量历史
     */
    @GetMapping("/instances/{id}/variables")
    public Result<List<Map<String, Object>>> getVariableHistory(@PathVariable Long id) {
        return Result.ok(processMonitorService.getVariableHistory(id));
    }

    /**
     * 获取耗时统计
     */
    @GetMapping("/instances/{id}/statistics")
    public Result<Map<String, Object>> getStatistics(@PathVariable Long id) {
        return Result.ok(processMonitorService.getStatistics(id));
    }

    /**
     * 获取运行中的流程列表
     */
    @GetMapping("/running")
    public Result<List<Map<String, Object>>> getRunningProcesses(
            @RequestParam(required = false) String processKey,
            @RequestParam(required = false) String startUser,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        return Result.ok(processMonitorService.getRunningProcesses(
                processKey, startUser, startTime, endTime, page, size));
    }

    /**
     * 导出实例数据
     */
    @GetMapping("/instances/{id}/export")
    public Result<Map<String, Object>> exportInstanceData(@PathVariable Long id) {
        return Result.ok(processMonitorService.exportInstanceData(id));
    }

    /**
     * 管理员干预：强制推进到指定节点
     */
    @PostMapping("/instances/{id}/intervene")
    public Result<Void> intervene(
            @PathVariable Long id,
            @RequestParam String targetNodeId,
            @RequestParam Long operatorId,
            @RequestParam(required = false) String reason) {
        
        processMonitorService.intervene(id, targetNodeId, operatorId, reason);
        return Result.ok(null);
    }
}
