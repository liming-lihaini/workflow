package com.flow.engine.controllers;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.flow.engine.common.Result;
import com.flow.engine.entity.EmsPhoto;
import com.flow.engine.entity.EmsRetain;
import com.flow.engine.entity.EmsSample;
import com.flow.engine.entity.EmsSampleLog;
import com.flow.engine.entity.EmsSampleQcBinding;
import com.flow.engine.entity.EmsSamplingRecord;
import com.flow.engine.service.EmsRetainService;
import com.flow.engine.service.EmsSamplingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 环境监测 - 采样与样品管理（TRD 5.3 / ISSUE-024）
 * 路径前缀与 EmsBaseDataController 保持一致：/ems/base
 */
@RestController
@RequestMapping("/api/v1/ems/base/sampling")
public class EmsSamplingController {

    @Autowired
    private EmsSamplingService samplingService;
    @Autowired
    private EmsRetainService retainService;

    // ===================== 采样记录 =====================

    @PostMapping("/records")
    public Result<EmsSamplingRecord> createRecord(@RequestBody EmsSamplingRecord rec) {
        return Result.ok(samplingService.createRecord(rec));
    }

    @PutMapping("/records/{id}")
    public Result<EmsSamplingRecord> updateRecord(@PathVariable Long id, @RequestBody EmsSamplingRecord rec) {
        return Result.ok(samplingService.updateRecord(id, rec));
    }

    @PostMapping("/records/{id}/complete")
    public Result<EmsSamplingRecord> completeRecord(@PathVariable Long id) {
        return Result.ok(samplingService.completeRecord(id));
    }

    @GetMapping("/records")
    public Result<Page<EmsSamplingRecord>> pageRecords(
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(samplingService.pageRecords(orderId, status, keyword, page, size));
    }

    @DeleteMapping("/records/{id}")
    public Result<Void> deleteRecord(@PathVariable Long id) {
        samplingService.deleteRecord(id);
        return Result.ok();
    }

    // ===================== 样品 =====================

    @PostMapping("/samples")
    public Result<EmsSample> createSample(@RequestBody EmsSample sample) {
        return Result.ok(samplingService.createSample(sample));
    }

    @PutMapping("/samples/{id}")
    public Result<EmsSample> updateSample(@PathVariable Long id, @RequestBody EmsSample sample) {
        return Result.ok(samplingService.updateSample(id, sample));
    }

    /**
     * 手动收集样品：未通过微信小程序上报/采样流程，由收样员在收样工作台直接录入。
     * 不要求 samplingId/orderId，创建后直接进入已收样状态。
     */
    @PostMapping("/samples/manual-collect")
    public Result<EmsSample> manualCollect(@RequestBody EmsSample sample) {
        return Result.ok(samplingService.manualCollect(sample));
    }

    @PostMapping("/samples/{id}/receive")
    public Result<EmsSample> receive(@PathVariable Long id,
                                     @RequestParam(required = false) String receiveBy,
                                     @RequestParam(required = false) String receiveTime,
                                     @RequestParam(required = false) String remark) {
        return Result.ok(samplingService.receive(id, receiveBy, receiveTime, remark));
    }

    @PostMapping("/samples/{id}/dispatch")
    public Result<EmsSample> dispatchSample(@PathVariable Long id,
                                            @RequestParam(required = false) String dispatchTime) {
        return Result.ok(samplingService.dispatch(id, dispatchTime));
    }

    @GetMapping("/samples")
    public Result<Page<EmsSample>> pageSamples(
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) Long samplingId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(samplingService.pageSamples(orderId, samplingId, status, keyword, page, size));
    }

    @GetMapping("/samples/{id}/detail")
    public Result<Map<String, Object>> sampleDetail(@PathVariable Long id) {
        return Result.ok(samplingService.sampleDetail(id));
    }

    @DeleteMapping("/samples/{id}")
    public Result<Void> deleteSample(@PathVariable Long id) {
        samplingService.deleteSample(id);
        return Result.ok();
    }

    // ===================== 质控样 / 照片 / 日志 =====================

    @PostMapping("/samples/{id}/qc")
    public Result<EmsSampleQcBinding> bindQc(@PathVariable Long id, @RequestBody EmsSampleQcBinding binding) {
        binding.setSampleId(id);
        return Result.ok(samplingService.bindQc(binding));
    }

    @DeleteMapping("/qc/{id}")
    public Result<Void> unbindQc(@PathVariable Long id) {
        samplingService.unbindQc(id);
        return Result.ok();
    }

    @PostMapping("/photos")
    public Result<EmsPhoto> addPhoto(@RequestBody EmsPhoto photo) {
        return Result.ok(samplingService.addPhoto(photo));
    }

    @GetMapping("/photos")
    public Result<List<EmsPhoto>> listPhotos(@RequestParam String bizType, @RequestParam Long bizId) {
        return Result.ok(samplingService.listPhotos(bizType, bizId));
    }

    @GetMapping("/samples/{id}/logs")
    public Result<List<EmsSampleLog>> listLogs(@PathVariable Long id) {
        return Result.ok(samplingService.listLogs(id));
    }

    // ===================== 收样工作台 =====================

    @GetMapping("/workbench")
    public Result<Map<String, Object>> workbench(@RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "50") int size) {
        return Result.ok(samplingService.receiveWorkbench(page, size));
    }

    // ===================== 留样库 =====================

    @PostMapping("/samples/{id}/retain")
    public Result<EmsRetain> retain(@PathVariable Long id,
                                    @RequestParam Integer retainDays,
                                    @RequestParam(required = false) String retainBy,
                                    @RequestParam(required = false) String retainTime,
                                    @RequestParam(required = false) String remark) {
        return Result.ok(samplingService.retain(id, retainDays, retainBy, retainTime, remark));
    }

    @PostMapping("/retains/{id}/dispose")
    public Result<EmsRetain> dispose(@PathVariable Long id,
                                     @RequestParam(required = false) String disposeBy,
                                     @RequestParam(required = false) String disposeTime) {
        return Result.ok(samplingService.dispose(id, disposeBy, disposeTime));
    }

    @GetMapping("/retains")
    public Result<Page<EmsRetain>> pageRetain(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(retainService.pageRetain(keyword, status, page, size));
    }

    @GetMapping("/retains/expiring")
    public Result<List<EmsRetain>> expiring(@RequestParam(defaultValue = "7") int thresholdDays) {
        return Result.ok(samplingService.expiringRetain(thresholdDays));
    }
}
