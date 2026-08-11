package com.flow.engine.controllers;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.flow.engine.common.ErrorCode;
import com.flow.engine.common.Result;
import com.flow.engine.common.utils.JsonUtils;
import com.flow.engine.dto.ReceiveReq;
import com.flow.engine.dto.SampleCollectReq;
import com.flow.engine.dto.SampleDisposeReq;
import com.flow.engine.entity.EmsPhoto;
import com.flow.engine.entity.EmsRetain;
import com.flow.engine.entity.EmsSample;
import com.flow.engine.entity.EmsSampleLog;
import com.flow.engine.entity.EmsSampleQcBinding;
import com.flow.engine.entity.EmsSamplingRecord;
import com.flow.engine.entity.User;
import com.flow.engine.service.AuthService;
import com.flow.engine.service.EmsRetainService;
import com.flow.engine.service.EmsSamplingService;
import com.flow.engine.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
    @Autowired
    private UserService userService;

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

    /** 异常处置：仅「异常拒收」或「检测异常」样品可提交，补充处置方式/类型/说明 */
    @PostMapping("/samples/{id}/dispose")
    public Result<EmsSample> disposeSample(@PathVariable Long id,
                                           @RequestBody SampleDisposeReq req) {
        // 当前用户由统一认证过滤器(AuthContextFilter)解析后写入 RequestContext
        User operatorUser = currentUser();
        if (operatorUser == null) {
            return Result.error(ErrorCode.TOKEN_INVALID, "未获取到当前登录用户，请确认已登录");
        }
        String operator = operatorUser.getRealName();
        return Result.ok(samplingService.dispose(id, req, operator));
    }

    /**
     * 从统一认证上下文(RequestContext)获取当前用户。
     * 用户身份由 AuthContextFilter 在请求早期解析并写入，业务层无需感知具体认证方式。
     */
    private User currentUser() {
        String userId = com.flow.engine.common.RequestContext.current().getUserId();
        if (userId == null) {
            return null;
        }
        try {
            return userService.getUser(Long.valueOf(userId));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 手动收集样品：未通过微信小程序上报/采样流程，由收样员在收样工作台直接录入。
     * 不要求 samplingId/orderId，创建后直接进入已收样状态。
     */
    @PostMapping("/samples/manual-collect")
    public Result<EmsSample> manualCollect(@RequestBody EmsSample sample) {
        return Result.ok(samplingService.manualCollect(sample));
    }

    /**
     * 收样工作台-手动收集样品（采集版）：接收完整采集表单。
     * 关联采样派单/委托单/点位，保存检测类别/项目、采样参数值、固定剂、现场质控、留样与照片。
     */
    @PostMapping("/samples/collect")
    public Result<EmsSample> collectSample(@RequestBody SampleCollectReq req) {
        return Result.ok(samplingService.manualCollect(req));
    }

    /**
     * 现场照片批量上传：单文件上传，返回相对存储路径（dateDir/uuid.ext）。
     * 前端批量选择时多次调用，收集返回的路径列表存入 sample_photo。
     */
    @Value("${flow.sample.photo.dir:upload/samples}")
    private String samplePhotoDir;

    @PostMapping("/samples/photo-upload")
    public Result<Map<String, Object>> uploadSamplePhoto(@RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return Result.error(ErrorCode.PARAM_INVALID, "上传文件不能为空");
        }
        String originalName = StringUtils.hasText(file.getOriginalFilename())
                ? Paths.get(file.getOriginalFilename()).getFileName().toString() : "photo";
        String ext = "";
        int dotIdx = originalName.lastIndexOf('.');
        if (dotIdx >= 0 && dotIdx < originalName.length() - 1) {
            String rawExt = originalName.substring(dotIdx + 1);
            if (rawExt.matches("[A-Za-z0-9]{1,10}")) {
                ext = "." + rawExt.toLowerCase();
            }
        }
        String dateDir = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String storedName = UUID.randomUUID() + ext;
        Path dir = Paths.get(samplePhotoDir, dateDir);
        Files.createDirectories(dir);
        Path target = dir.resolve(storedName);
        file.transferTo(target.toAbsolutePath());

        String relativePath = dateDir + "/" + storedName;
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", originalName);
        data.put("path", relativePath);
        data.put("url", "/api/v1/ems/base/sampling/samples/photo/" + relativePath);
        data.put("size", file.getSize());
        return Result.ok(data);
    }

    /**
     * 现场照片访问：按相对路径返回图片二进制。
     */
    @GetMapping("/samples/photo/{dateDir}/{fileName}")
    public void getSamplePhoto(@PathVariable String dateDir,
                               @PathVariable String fileName,
                               HttpServletResponse response) throws IOException {
        Path file = Paths.get(samplePhotoDir, dateDir, fileName);
        if (!Files.isRegularFile(file)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String contentType = Files.probeContentType(file);
        if (contentType == null) contentType = "application/octet-stream";
        response.setContentType(contentType);
        response.setContentLengthLong(Files.size(file));
        Files.copy(file, response.getOutputStream());
        response.flushBuffer();
    }

    @PostMapping("/samples/{id}/receive")
    public Result<EmsSample> receive(@PathVariable Long id,
                                     @RequestBody ReceiveReq req) {
        return Result.ok(samplingService.receive(id, req));
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
                                    @RequestParam(required = false) String retainLocation,
                                    @RequestParam(required = false) String remark) {
        return Result.ok(samplingService.retain(id, retainDays, retainBy, retainTime, retainLocation, remark));
    }

    @GetMapping("/retains")
    public Result<Page<EmsRetain>> pageRetain(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(retainService.pageRetain(keyword, status, page, size));
    }

    @GetMapping("/retains/stats")
    public Result<java.util.Map<String, Object>> retainStats() {
        return Result.ok(samplingService.retainStats());
    }

    @PostMapping("/retains/{id}/dispose")
    public Result<Map<String, Object>> applyDispose(@PathVariable Long id,
                                                     @RequestParam String startUser,
                                                     @RequestBody(required = false) String rawBody) {
        Map<String, Object> formData = JsonUtils.parseBodyMapLoose(rawBody);
        return Result.ok(samplingService.applyDispose(id, startUser, formData));
    }

    @DeleteMapping("/retains/{id}")
    public Result<Void> deleteRetain(@PathVariable Long id) {
        retainService.removeById(id);
        return Result.ok();
    }

    @GetMapping("/retains/expiring")
    public Result<List<EmsRetain>> expiring(@RequestParam(defaultValue = "7") int thresholdDays) {
        return Result.ok(samplingService.expiringRetain(thresholdDays));
    }
}
