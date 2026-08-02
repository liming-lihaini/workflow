package com.flow.engine.controllers;

import com.flow.engine.common.Result;
import com.flow.engine.service.EmsDashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 环境监测 - 监测数据驾驶舱与统计（ISSUE-028）
 */
@RestController
@RequestMapping("/api/v1/ems/dashboard")
public class EmsDashboardController {

    private final EmsDashboardService dashboardService;

    public EmsDashboardController(EmsDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /** 驾驶舱概览统计。 */
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.ok(dashboardService.overview());
    }
}
