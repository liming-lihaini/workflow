package com.flow.engine.controllers;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flow.engine.common.Result;
import com.flow.engine.entity.EmsAssetScrap;
import com.flow.engine.entity.EmsConsumable;
import com.flow.engine.entity.EmsHazardousLedger;
import com.flow.engine.entity.EmsInstrument;
import com.flow.engine.entity.EmsStandardMaterial;
import com.flow.engine.mapper.EmsAssetScrapMapper;
import com.flow.engine.mapper.EmsConsumableMapper;
import com.flow.engine.mapper.EmsHazardousLedgerMapper;
import com.flow.engine.mapper.EmsInstrumentMapper;
import com.flow.engine.mapper.EmsStandardMaterialMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 资产报废（设备/标准物质/耗材/危化品）Webhook 处理接口。
 * <p>
 * 由 ZCBFSQ（资产报废申请）流程的审批节点(userTask_approve) NODE_COMPLETED 事件触发回调：
 * - 按「资产类型+资产ID」定位台账记录，将状态更新为报废
 *   （设备 -> 报废；标准物质/耗材/危化品 -> 已报废）；
 * - 写入 t_asset_scrap 报废流水（记录报废原因与处置方式）；
 * - 以 process_instance_id 做幂等键，避免 Webhook 重试产生重复报废记录。
 */
@Slf4j
@RestController
@RequestMapping("/api/webhook/asset")
public class AssetScrapWebhookController {

    private static final String TYPE_INSTRUMENT = "设备";
    private static final String TYPE_MATERIAL = "标准物质";
    private static final String TYPE_CONSUMABLE = "耗材";
    private static final String TYPE_HAZARDOUS = "危化品";

    private final EmsInstrumentMapper instrumentMapper;
    private final EmsStandardMaterialMapper materialMapper;
    private final EmsConsumableMapper consumableMapper;
    private final EmsHazardousLedgerMapper hazardousMapper;
    private final EmsAssetScrapMapper scrapMapper;

    public AssetScrapWebhookController(EmsInstrumentMapper instrumentMapper,
                                       EmsStandardMaterialMapper materialMapper,
                                       EmsConsumableMapper consumableMapper,
                                       EmsHazardousLedgerMapper hazardousMapper,
                                       EmsAssetScrapMapper scrapMapper) {
        this.instrumentMapper = instrumentMapper;
        this.materialMapper = materialMapper;
        this.consumableMapper = consumableMapper;
        this.hazardousMapper = hazardousMapper;
        this.scrapMapper = scrapMapper;
    }

    /** 报废审批通过：更新对应台账状态为报废，并写入报废流水 */
    @PostMapping("/scrap")
    public Result<Map<String, Object>> scrap(@RequestBody Map<String, Object> body) {
        Map<String, Object> formData = mergeFormData(body);
        String assetType = str(formData, "assetType");
        Long assetId = toLong(formData.get("assetId"));
        if (!StringUtils.hasText(assetType) || assetId == null) {
            return Result.fail(400, "缺少必要字段 assetType/assetId，无法办理资产报废");
        }
        String scrapReason = str(formData, "scrapReason");
        if (!StringUtils.hasText(scrapReason)) {
            return Result.fail(400, "缺少必要字段 scrapReason（报废原因）");
        }
        Long processInstanceId = toLong(body.get("processInstanceId"));
        if (duplicated(processInstanceId)) {
            log.info("[AssetScrapWebhook] 流程实例 {} 已办理报废，跳过（幂等）", processInstanceId);
            return Result.ok(buildResp(assetType, assetId, "幂等跳过", processInstanceId));
        }

        String applicant = resolveApplicant(formData, body);
        String statusAfter;
        switch (assetType) {
            case TYPE_INSTRUMENT -> {
                EmsInstrument inst = instrumentMapper.selectById(assetId);
                if (inst == null) return Result.fail(404, "设备不存在: id=" + assetId);
                inst.setStatus("报废");
                inst.setUpdateTime(LocalDateTime.now());
                instrumentMapper.updateById(inst);
                statusAfter = inst.getStatus();
            }
            case TYPE_MATERIAL -> {
                EmsStandardMaterial m = materialMapper.selectById(assetId);
                if (m == null) return Result.fail(404, "标准物质不存在: id=" + assetId);
                m.setStatus("已报废");
                m.setUpdateTime(LocalDate.now());
                materialMapper.updateById(m);
                statusAfter = m.getStatus();
            }
            case TYPE_CONSUMABLE -> {
                EmsConsumable c = consumableMapper.selectById(assetId);
                if (c == null) return Result.fail(404, "耗材不存在: id=" + assetId);
                c.setStatus("已报废");
                c.setUpdateTime(LocalDate.now());
                consumableMapper.updateById(c);
                statusAfter = c.getStatus();
            }
            case TYPE_HAZARDOUS -> {
                EmsHazardousLedger h = hazardousMapper.selectById(assetId);
                if (h == null) return Result.fail(404, "危化品不存在: id=" + assetId);
                h.setStatus("已报废");
                h.setApplyBy(applicant);
                h.setApplyReason(scrapReason);
                h.setApplyTime(LocalDate.now());
                h.setUpdateTime(LocalDate.now());
                hazardousMapper.updateById(h);
                statusAfter = h.getStatus();
            }
            default -> {
                return Result.fail(400, "不支持的资产类型: " + assetType);
            }
        }

        recordScrap(assetType, assetId, str(formData, "name"), str(formData, "spec"),
                scrapReason, str(formData, "disposeMethod"), applicant, processInstanceId);
        log.info("[AssetScrapWebhook] 资产报废完成: type={}, id={}, status={}, processInstanceId={}",
                assetType, assetId, statusAfter, processInstanceId);
        Map<String, Object> resp = buildResp(assetType, assetId, "报废成功", processInstanceId);
        resp.put("statusAfter", statusAfter);
        return Result.ok(resp);
    }

    // ===================== 内部工具 =====================

    /** 表单字段可能位于 formData 或 variables，兼容两种结构 */
    private Map<String, Object> mergeFormData(Map<String, Object> body) {
        Map<String, Object> formData = new LinkedHashMap<>();
        Object fd = body.get("formData");
        Object vars = body.get("variables");
        if (fd instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> m = (Map<String, Object>) fd;
            formData.putAll(m);
        }
        if (vars instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> m = (Map<String, Object>) vars;
            m.forEach((k, v) -> formData.putIfAbsent(k, v));
        }
        if (formData.isEmpty() && !body.isEmpty()) {
            formData.putAll(body);
        }
        return formData;
    }

    /** 幂等：同一流程实例只办理一次报废 */
    private boolean duplicated(Long processInstanceId) {
        if (processInstanceId == null) return false;
        LambdaQueryWrapper<EmsAssetScrap> q = new LambdaQueryWrapper<>();
        q.eq(EmsAssetScrap::getProcessInstanceId, processInstanceId);
        return scrapMapper.selectCount(q) > 0;
    }

    /** 写入报废流水 */
    private void recordScrap(String assetType, Long assetId, String name, String spec,
                             String scrapReason, String disposeMethod, String applicant, Long processInstanceId) {
        EmsAssetScrap scrap = new EmsAssetScrap();
        scrap.setAssetType(assetType);
        scrap.setAssetId(assetId);
        scrap.setName(name);
        scrap.setSpec(spec);
        scrap.setScrapReason(scrapReason);
        scrap.setDisposeMethod(disposeMethod);
        scrap.setApplicant(applicant);
        scrap.setProcessInstanceId(processInstanceId);
        scrap.setCreateTime(LocalDate.now());
        scrapMapper.insert(scrap);
    }

    /** 申请人：表单 applicant 优先，兜底流程发起人 */
    private String resolveApplicant(Map<String, Object> formData, Map<String, Object> body) {
        String applicant = str(formData, "applicant");
        if (!StringUtils.hasText(applicant)) {
            Object su = body.get("startUser");
            applicant = su != null ? String.valueOf(su) : "system";
        }
        return applicant;
    }

    private Map<String, Object> buildResp(String assetType, Long assetId, String action, Long processInstanceId) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("assetType", assetType);
        resp.put("assetId", assetId);
        resp.put("action", action);
        resp.put("processInstanceId", processInstanceId);
        return resp;
    }

    private String str(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? String.valueOf(v) : null;
    }

    private Long toLong(Object v) {
        if (v == null) return null;
        try {
            return (long) Double.parseDouble(String.valueOf(v).trim());
        } catch (Exception e) {
            return null;
        }
    }
}
