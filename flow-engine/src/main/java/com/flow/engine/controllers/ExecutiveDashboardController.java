package com.flow.engine.controllers;

import com.flow.engine.common.Result;
import com.flow.engine.service.ExecutiveDashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 高层领导数据驾驶舱（ISSUE-028-EXEC / PRD-03）
 * 路由前缀：/api/v1/ems/dashboard/executive
 * 全部为只读聚合接口，数据来自业务表真实聚合。
 */
@RestController
@RequestMapping("/api/v1/ems/dashboard/executive")
public class ExecutiveDashboardController {

    private final ExecutiveDashboardService service;

    public ExecutiveDashboardController(ExecutiveDashboardService service) {
        this.service = service;
    }

    /** C1 顶部 KPI 汇总 */
    @GetMapping("/kpi")
    public Result<Map<String, Object>> kpi() {
        return Result.ok(service.kpi());
    }

    /** C2 合同金额月度趋势（签约额 + 收款额） */
    @GetMapping("/contract/monthly-trend")
    public Result<Map<String, Object>> monthlyTrend() {
        return Result.ok(service.contractMonthlyTrend());
    }

    /** C3 合同状态分布 */
    @GetMapping("/contract/status-dist")
    public Result<Map<String, Object>> statusDist() {
        return Result.ok(service.contractStatusDist());
    }

    /** C4 客户合同金额 TOP */
    @GetMapping("/contract/top-customers")
    public Result<Map<String, Object>> topCustomers(@RequestParam(defaultValue = "10") int limit) {
        return Result.ok(service.contractTopCustomers(limit));
    }

    /** C5 业务全链路流转漏斗 */
    @GetMapping("/funnel")
    public Result<Map<String, Object>> funnel() {
        return Result.ok(service.funnel());
    }

    /** C6 检测结果明细（分页） */
    @GetMapping("/detection/results")
    public Result<Map<String, Object>> detectionResults(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "8") int size) {
        return Result.ok(service.detectionResults(page, size));
    }

    /** C7 系统预警列表 */
    @GetMapping("/alerts")
    public Result<Map<String, Object>> alerts(@RequestParam(required = false) String status) {
        return Result.ok(service.alerts(status));
    }

    /** C8 质控合格率（双仪表盘） */
    @GetMapping("/quality/qc-rate")
    public Result<Map<String, Object>> qcRate() {
        return Result.ok(service.qcRate());
    }

    /** C9 仪器设备状态分布 */
    @GetMapping("/instrument/status")
    public Result<Map<String, Object>> instrumentStatus() {
        return Result.ok(service.instrumentStatus());
    }

    /** C10 合同到期预警（按剩余天数升序） */
    @GetMapping("/contract/expiring")
    public Result<Map<String, Object>> contractExpiring(@RequestParam(defaultValue = "180") int days) {
        return Result.ok(service.contractExpiring(days));
    }

    /** C11 实时动态滚动条 */
    @GetMapping("/ticker")
    public Result<Map<String, Object>> ticker() {
        return Result.ok(service.ticker());
    }
}
