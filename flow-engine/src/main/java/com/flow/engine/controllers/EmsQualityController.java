package com.flow.engine.controllers;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.flow.engine.common.Result;
import com.flow.engine.entity.*;
import com.flow.engine.service.EmsQualityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 环境监测 - 质量控制（TRD 5.5 / 5.12 / ISSUE-026）
 * 鉴权沿用全局拦截器（Authorization 头）。
 */
@RestController
@RequestMapping("/api/v1/ems/quality")
public class EmsQualityController {

    @Autowired
    private EmsQualityService qualityService;

    // ===================== 标准物质 =====================
    @PostMapping("/materials")
    public Result<?> saveMaterial(@RequestBody EmsStandardMaterial m) {
        return Result.ok(qualityService.saveMaterial(m));
    }
    @GetMapping("/materials")
    public Result<?> pageMaterials(@RequestParam(required = false) String keyword,
                                   @RequestParam(required = false) String status,
                                   @RequestParam(defaultValue = "1") int page,
                                   @RequestParam(defaultValue = "20") int size) {
        return Result.ok(qualityService.pageMaterials(keyword, status, page, size));
    }

    // ===================== 耗材 =====================
    @PostMapping("/consumables")
    public Result<?> saveConsumable(@RequestBody EmsConsumable c) {
        return Result.ok(qualityService.saveConsumable(c));
    }
    @GetMapping("/consumables")
    public Result<?> pageConsumables(@RequestParam(required = false) String keyword,
                                     @RequestParam(required = false) String status,
                                     @RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "20") int size) {
        return Result.ok(qualityService.pageConsumables(keyword, status, page, size));
    }

    // ===================== 危化品台账（审批状态机） =====================
    @PostMapping("/hazardous")
    public Result<?> saveHazardous(@RequestBody EmsHazardousLedger h) {
        return Result.ok(qualityService.saveHazardous(h));
    }
    @GetMapping("/hazardous")
    public Result<?> pageHazardous(@RequestParam(required = false) String keyword,
                                   @RequestParam(required = false) String status,
                                   @RequestParam(defaultValue = "1") int page,
                                   @RequestParam(defaultValue = "20") int size) {
        return Result.ok(qualityService.pageHazardous(keyword, status, page, size));
    }
    @PostMapping("/hazardous/{id}/apply")
    public Result<?> apply(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return Result.ok(qualityService.apply(id, body.get("applyBy"), body.get("applyReason"), body.get("targetStatus")));
    }
    @PostMapping("/hazardous/{id}/approve")
    public Result<?> approve(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        boolean ok = body.get("approve") instanceof Boolean ? (Boolean) body.get("approve")
                : !"false".equals(String.valueOf(body.get("approve")));
        return Result.ok(qualityService.approve(id, (String) body.get("approveBy"), ok, (String) body.get("approveOpinion")));
    }

    // ===================== 质控计划（状态机） =====================
    @PostMapping("/plans")
    public Result<?> savePlan(@RequestBody EmsQcPlan p) {
        return Result.ok(qualityService.savePlan(p));
    }
    @GetMapping("/plans")
    public Result<?> pagePlans(@RequestParam(required = false) String year,
                               @RequestParam(required = false) String status,
                               @RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "20") int size) {
        return Result.ok(qualityService.pagePlans(year, status, page, size));
    }
    @GetMapping("/plans/{id}")
    public Result<?> planDetail(@PathVariable Long id) {
        return Result.ok(qualityService.planDetail(id));
    }
    @PostMapping("/plans/{id}/submit")
    public Result<?> submitPlan(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        String approver = body != null && body.get("approver") != null ? String.valueOf(body.get("approver")) : "审批人";
        return Result.ok(qualityService.submitPlan(id, approver));
    }
    @PostMapping("/plans/{id}/approve")
    public Result<?> approvePlan(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        String approver = body != null && body.get("approver") != null ? String.valueOf(body.get("approver")) : "审批人";
        return Result.ok(qualityService.approvePlan(id, approver));
    }
    @PostMapping("/plans/{id}/complete")
    public Result<?> completePlan(@PathVariable Long id) {
        return Result.ok(qualityService.completePlan(id));
    }

    // ===================== 监控活动 =====================
    @PostMapping("/activities")
    public Result<?> saveActivity(@RequestBody EmsQcActivity a) {
        return Result.ok(qualityService.saveActivity(a));
    }
    @GetMapping("/activities")
    public Result<?> pageActivities(@RequestParam(required = false) Long planId,
                                    @RequestParam(required = false) String qcType,
                                    @RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        return Result.ok(qualityService.pageActivities(planId, qcType, page, size));
    }

    // ===================== 能力验证 =====================
    @PostMapping("/proficiency")
    public Result<?> saveProficiency(@RequestBody EmsProficiencyTest t) {
        return Result.ok(qualityService.saveProficiency(t));
    }
    @GetMapping("/proficiency")
    public Result<?> pageProficiency(@RequestParam(required = false) Long planId,
                                     @RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "20") int size) {
        return Result.ok(qualityService.pageProficiency(planId, page, size));
    }

    // ===================== 实验室间比对 =====================
    @PostMapping("/interlab")
    public Result<?> saveInterlab(@RequestBody EmsInterlabCompare c) {
        return Result.ok(qualityService.saveInterlab(c));
    }
    @GetMapping("/interlab")
    public Result<?> pageInterlab(@RequestParam(required = false) Long planId,
                                  @RequestParam(defaultValue = "1") int page,
                                  @RequestParam(defaultValue = "20") int size) {
        return Result.ok(qualityService.pageInterlab(planId, page, size));
    }

    // ===================== 重复性试验 =====================
    @PostMapping("/repeat")
    public Result<?> saveRepeat(@RequestBody EmsRepeatTest r) {
        return Result.ok(qualityService.saveRepeat(r));
    }
    @GetMapping("/repeat")
    public Result<?> pageRepeat(@RequestParam(required = false) Long planId,
                                @RequestParam(defaultValue = "1") int page,
                                @RequestParam(defaultValue = "20") int size) {
        return Result.ok(qualityService.pageRepeat(planId, page, size));
    }

    // ===================== 闸门校验（G2/G3） =====================
    @GetMapping("/gate/material")
    public Result<?> materialGate() {
        return Result.ok(qualityService.materialGate());
    }
    @GetMapping("/gate/instrument")
    public Result<?> instrumentGate() {
        return Result.ok(qualityService.instrumentGate());
    }
}
