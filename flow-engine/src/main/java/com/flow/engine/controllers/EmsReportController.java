package com.flow.engine.controllers;

import com.flow.engine.common.Result;
import com.flow.engine.service.EmsReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 环境监测 - 报告生成与审核（TRD 5.11 / ISSUE-027）
 * 鉴权沿用全局拦截器（Authorization 头）。
 */
@RestController
@RequestMapping("/api/v1/ems/report")
public class EmsReportController {

    @Autowired
    private EmsReportService reportService;

    // ===================== 模板 =====================
    @GetMapping("/templates")
    public Result<?> listTemplates() {
        return Result.ok(reportService.listTemplates());
    }

    @PostMapping("/template")
    public Result<?> createTemplate(@RequestBody Map<String, String> body) {
        Long id = reportService.createTemplate(
                body.get("name"),
                body.get("type"),
                body.get("content"),
                body.get("remark"));
        return Result.ok(id);
    }

    // ===================== 报告生成 =====================
    @GetMapping("/pending-tasks")
    public Result<?> pendingTasks() {
        return Result.ok(reportService.pendingTasks());
    }

    @PostMapping("/generate")
    public Result<?> generate(@RequestBody Map<String, Object> body) {
        Long tplId = toLong(body.get("tplId"));
        String title = (String) body.get("title");
        String client = (String) body.get("client");
        String period = (String) body.get("period");
        String generator = (String) body.get("generator");
        List<Integer> rawIds = (List<Integer>) body.get("taskIds");
        List<Long> taskIds = new java.util.ArrayList<>();
        if (rawIds != null) {
            for (Integer i : rawIds) taskIds.add(i.longValue());
        }
        Long id = reportService.generateReport(tplId, title, client, period, taskIds, generator);
        return Result.ok(id);
    }

    // ===================== 报告列表 / 详情 =====================
    @GetMapping("/list")
    public Result<?> listReports(@RequestParam(required = false) String status) {
        return Result.ok(reportService.listReports(status));
    }

    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return Result.ok(reportService.getReportDetail(id));
    }

    // ===================== 审核 =====================
    @PostMapping("/{id}/approve")
    public Result<?> approve(@PathVariable Long id, @RequestBody Map<String, String> body) {
        reportService.approve(id, body.get("auditor"));
        return Result.ok("已发布");
    }

    @PostMapping("/{id}/reject")
    public Result<?> reject(@PathVariable Long id, @RequestBody Map<String, String> body) {
        reportService.reject(id, body.get("auditor"), body.get("opinion"));
        return Result.ok("已退回");
    }

    // ===================== 监测报告（委托驱动，req.md） =====================
    @PostMapping("/create")
    public Result<?> create(@RequestBody Map<String, Object> body) {
        boolean draft = Boolean.TRUE.equals(body.get("draft"));
        Long id = reportService.createMonitorReport(body, draft);
        return Result.ok(id);
    }

    @PostMapping("/{id}/submit")
    public Result<?> submit(@PathVariable Long id) {
        reportService.submitDraft(id);
        return Result.ok("已提交");
    }

    @GetMapping("/entrust/{entrustId}/tasks")
    public Result<?> entrustTasks(@PathVariable Long entrustId) {
        return Result.ok(reportService.entrustTasks(entrustId));
    }

    @GetMapping("/{id}/view")
    public Result<?> view(@PathVariable Long id) {
        return Result.ok(reportService.viewModel(id));
    }

    private Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).longValue();
        return Long.valueOf(o.toString());
    }
}
