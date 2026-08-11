package com.flow.engine.controllers;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.flow.engine.common.Result;
import com.flow.engine.common.utils.JsonUtils;
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
    /** 标准物质详情（含关联流程：入库申请/使用申请） */
    @GetMapping("/materials/{id}/detail")
    public Result<?> materialDetail(@PathVariable Long id) {
        return Result.ok(qualityService.materialDetail(id));
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
    /** 耗材详情（含关联流程：入库申请/使用申请） */
    @GetMapping("/consumables/{id}/detail")
    public Result<?> consumableDetail(@PathVariable Long id) {
        return Result.ok(qualityService.consumableDetail(id));
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
    /** 危化品详情（含关联流程：报废申请 ZCBFSQ） */
    @GetMapping("/hazardous/{id}/detail")
    public Result<?> hazardousDetail(@PathVariable Long id) {
        return Result.ok(qualityService.hazardousDetail(id));
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
    public Result<?> savePlan(@RequestBody EmsQcPlan p,
                              @RequestParam(required = false) String opBy,
                              @RequestParam(required = false) String opName) {
        return Result.ok(qualityService.savePlan(p, opBy, opName));
    }
    @GetMapping("/plans")
    public Result<?> pagePlans(@RequestParam(required = false) String keyword,
                               @RequestParam(required = false) String year,
                               @RequestParam(required = false) String status,
                               @RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "20") int size) {
        return Result.ok(qualityService.pagePlans(keyword, year, status, page, size));
    }
    @GetMapping("/plans/{id}")
    public Result<?> planDetail(@PathVariable Long id) {
        return Result.ok(qualityService.planDetail(id));
    }
    @DeleteMapping("/plans/{id}")
    public Result<?> deletePlan(@PathVariable Long id,
                                @RequestParam(required = false) String opBy,
                                @RequestParam(required = false) String opName) {
        qualityService.deletePlan(id, opBy, opName);
        return Result.ok();
    }
    @PostMapping("/plans/{id}/submit")
    public Result<?> submitPlan(@PathVariable Long id,
                                @RequestParam(required = false) String opBy,
                                @RequestParam(required = false) String opName,
                                @RequestBody(required = false) String rawBody) {
        Map<String, Object> body = JsonUtils.parseBodyMapLoose(rawBody);
        String approver = body != null && body.get("approver") != null ? String.valueOf(body.get("approver")) : "审批人";
        return Result.ok(qualityService.submitPlan(id, approver, opBy, opName));
    }
    @PostMapping("/plans/{id}/approve")
    public Result<?> approvePlan(@PathVariable Long id,
                                 @RequestParam(required = false) String opBy,
                                 @RequestParam(required = false) String opName,
                                 @RequestBody(required = false) String rawBody) {
        Map<String, Object> body = JsonUtils.parseBodyMapLoose(rawBody);
        String approver = body != null && body.get("approver") != null ? String.valueOf(body.get("approver")) : "审批人";
        return Result.ok(qualityService.approvePlan(id, approver, opBy, opName));
    }
    @PostMapping("/plans/{id}/complete")
    public Result<?> completePlan(@PathVariable Long id,
                                  @RequestParam(required = false) String opBy,
                                  @RequestParam(required = false) String opName) {
        return Result.ok(qualityService.completePlan(id, opBy, opName));
    }

    // ===================== 监控活动 =====================
    @PostMapping("/activities")
    public Result<?> saveActivity(@RequestBody EmsQcActivity a,
                                  @RequestParam(required = false) String opBy,
                                  @RequestParam(required = false) String opName) {
        return Result.ok(qualityService.saveActivity(a, opBy, opName));
    }
    @GetMapping("/activities")
    public Result<?> pageActivities(@RequestParam(required = false) Long planId,
                                    @RequestParam(required = false) String qcType,
                                    @RequestParam(required = false) String keyword,
                                    @RequestParam(required = false) String operatorId,
                                    @RequestParam(required = false) String taskStatus,
                                    @RequestParam(required = false) String startDateFrom,
                                    @RequestParam(required = false) String startDateTo,
                                    @RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        return Result.ok(qualityService.pageActivities(planId, qcType, keyword, operatorId, taskStatus, startDateFrom, startDateTo, page, size));
    }
    /** 质控活动待办（工作台）：活动执行人名下未完成/未取消的活动 */
    @GetMapping("/activities/todos")
    public Result<?> todoActivities(@RequestParam(required = false) String username) {
        return Result.ok(qualityService.listTodoActivities(username));
    }
    @GetMapping("/activities/{id}")
    public Result<?> activityDetail(@PathVariable Long id) {
        return Result.ok(qualityService.activityDetail(id));
    }
    @DeleteMapping("/activities/{id}")
    public Result<?> deleteActivity(@PathVariable Long id,
                                    @RequestParam(required = false) String opBy,
                                    @RequestParam(required = false) String opName) {
        qualityService.deleteActivity(id, opBy, opName);
        return Result.ok();
    }

    // ===================== 处置历史 =====================
    @GetMapping("/history")
    public Result<?> history(@RequestParam String bizType, @RequestParam Long bizId) {
        return Result.ok(qualityService.listHistory(bizType, bizId));
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
