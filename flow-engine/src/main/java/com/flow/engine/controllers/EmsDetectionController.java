package com.flow.engine.controllers;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.flow.engine.common.BusinessException;
import com.flow.engine.common.Result;
import com.flow.engine.entity.EmsDetectionResult;
import com.flow.engine.entity.EmsDetectionReview;
import com.flow.engine.entity.EmsDetectionTask;
import com.flow.engine.entity.EmsSample;
import com.flow.engine.service.EmsDetectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 环境监测 - 检测数据录入与复核（TRD 5.4 / ISSUE-025）
 */
@RestController
@RequestMapping("/api/v1/ems/base/detection")
public class EmsDetectionController {

    @Autowired
    private EmsDetectionService detectionService;

    // ===================== 录入工作台 =====================

    /** 待创建任务的已收样样品列表 */
    @GetMapping("/pending-samples")
    public Result<?> pendingSamples(@RequestParam("AuthToken") String token) {
        return Result.ok(detectionService.pendingSamples());
    }

    /** 为样品创建检测任务 */
    @PostMapping("/task")
    public Result<?> createTask(@RequestParam("AuthToken") String token,
                                @RequestBody Map<String, Object> body) {
        Long sampleId = Long.valueOf(String.valueOf(body.get("sampleId")));
        String monitorItems = body.get("monitorItems") == null ? null : String.valueOf(body.get("monitorItems"));
        String entryBy = body.get("entryBy") == null ? "录入员" : String.valueOf(body.get("entryBy"));
        return Result.ok(detectionService.createTask(sampleId, monitorItems, entryBy));
    }

    /** 检测任务分页（可按状态/关键字筛选） */
    @GetMapping("/tasks")
    public Result<?> pageTasks(@RequestParam("AuthToken") String token,
                               @RequestParam(required = false) String status,
                               @RequestParam(required = false) String keyword,
                               @RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "20") int size) {
        Page<EmsDetectionTask> p = detectionService.pageTasks(status, keyword, page, size);
        return Result.ok(p);
    }

    /** 任务详情（含结果明细与复核记录） */
    @GetMapping("/task/{id}")
    public Result<?> taskDetail(@RequestParam("AuthToken") String token, @PathVariable Long id) {
        return Result.ok(detectionService.taskDetail(id));
    }

    /** 保存检测结果 */
    @PostMapping("/task/{id}/results")
    public Result<?> saveResults(@RequestParam("AuthToken") String token,
                                 @PathVariable Long id,
                                 @RequestBody Map<String, Object> body) {
        List<EmsDetectionResult> results = parseResults(body.get("results"));
        String entryBy = body.get("entryBy") == null ? "录入员" : String.valueOf(body.get("entryBy"));
        return Result.ok(detectionService.saveResults(id, results, entryBy));
    }

    /** 提交复核 */
    @PostMapping("/task/{id}/submit")
    public Result<?> submit(@RequestParam("AuthToken") String token, @PathVariable Long id) {
        return Result.ok(detectionService.submit(id));
    }

    // ===================== 复核工作台 =====================

    /** 待复核任务列表（状态=已提交） */
    @GetMapping("/pending-review")
    public Result<?> pendingReview(@RequestParam("AuthToken") String token,
                                   @RequestParam(defaultValue = "1") int page,
                                   @RequestParam(defaultValue = "20") int size) {
        return Result.ok(detectionService.pagePendingReview(page, size));
    }

    /** 复核通过 */
    @PostMapping("/task/{id}/approve")
    public Result<?> approve(@RequestParam("AuthToken") String token,
                             @PathVariable Long id,
                             @RequestBody(required = false) Map<String, Object> body) {
        String reviewer = body != null && body.get("reviewer") != null ? String.valueOf(body.get("reviewer")) : "复核员";
        String opinion = body != null && body.get("opinion") != null ? String.valueOf(body.get("opinion")) : "";
        return Result.ok(detectionService.approve(id, reviewer, opinion));
    }

    /** 复核退回 */
    @PostMapping("/task/{id}/reject")
    public Result<?> reject(@RequestParam("AuthToken") String token,
                            @PathVariable Long id,
                            @RequestBody Map<String, Object> body) {
        String reviewer = body.get("reviewer") == null ? "复核员" : String.valueOf(body.get("reviewer"));
        String opinion = body.get("opinion") == null ? "" : String.valueOf(body.get("opinion"));
        return Result.ok(detectionService.reject(id, reviewer, opinion));
    }

    // ===================== 辅助 =====================

    @SuppressWarnings("unchecked")
    private List<EmsDetectionResult> parseResults(Object raw) {
        if (!(raw instanceof List)) throw new BusinessException("results 格式错误");
        List<Map<String, Object>> list = (List<Map<String, Object>>) raw;
        List<EmsDetectionResult> out = new java.util.ArrayList<>();
        for (Map<String, Object> m : list) {
            EmsDetectionResult r = new EmsDetectionResult();
            r.setMonitorItem(str(m.get("monitorItem")));
            r.setValue(str(m.get("value")));
            r.setUnit(str(m.get("unit")));
            r.setMethod(str(m.get("method")));
            r.setLimitValue(str(m.get("limitValue")));
            r.setConclusion(str(m.get("conclusion")));
            out.add(r);
        }
        return out;
    }

    private String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
