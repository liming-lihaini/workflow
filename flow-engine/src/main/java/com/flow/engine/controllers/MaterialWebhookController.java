package com.flow.engine.controllers;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flow.engine.common.Result;
import com.flow.engine.entity.EmsConsumable;
import com.flow.engine.entity.EmsMaterialFlow;
import com.flow.engine.entity.EmsStandardMaterial;
import com.flow.engine.mapper.EmsConsumableMapper;
import com.flow.engine.mapper.EmsMaterialFlowMapper;
import com.flow.engine.mapper.EmsStandardMaterialMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 物资（标准物质/耗材）入库/使用 Webhook 处理接口。
 * <p>
 * 由 WZRKSQ（入库申请）/ WZSYSQ（使用申请）流程的审批节点(userTask_approve)
 * NODE_COMPLETED 事件触发回调：
 * - 入库：按「名称+规格」查找物资，存在则累加库存量，否则新建物资；
 * - 使用：按「名称+规格」查找物资，存在则扣减库存量（不低于0），否则新建物资；
 * - 每次回调均写入 t_material_flow 流水（material_id 关联物资详情展示关联流程）；
 * - 以 process_instance_id 做幂等键，避免 Webhook 重试产生重复流水/重复增减库存。
 */
@Slf4j
@RestController
@RequestMapping("/api/webhook/material")
public class MaterialWebhookController {

    private static final String TYPE_MATERIAL = "标准物质";
    private static final String TYPE_CONSUMABLE = "耗材";

    private final EmsStandardMaterialMapper materialMapper;
    private final EmsConsumableMapper consumableMapper;
    private final EmsMaterialFlowMapper flowMapper;

    public MaterialWebhookController(EmsStandardMaterialMapper materialMapper,
                                     EmsConsumableMapper consumableMapper,
                                     EmsMaterialFlowMapper flowMapper) {
        this.materialMapper = materialMapper;
        this.consumableMapper = consumableMapper;
        this.flowMapper = flowMapper;
    }

    /** 入库审批通过：名称+规格已存在 -> 累加库存量；否则新建物资 */
    @PostMapping("/inbound")
    public Result<Map<String, Object>> inbound(@RequestBody Map<String, Object> body) {
        Map<String, Object> formData = mergeFormData(body);
        String name = str(formData, "name");
        if (!StringUtils.hasText(name)) {
            return Result.fail(400, "缺少必要字段 name，无法办理物资入库");
        }
        int qty = toInt(formData.get("qty"), 0);
        if (qty <= 0) {
            return Result.fail(400, "入库数量必须大于 0");
        }
        Long processInstanceId = toLong(body.get("processInstanceId"));
        if (duplicated(processInstanceId, "入库")) {
            log.info("[MaterialWebhook] 流程实例 {} 已办理入库，跳过（幂等）", processInstanceId);
            return Result.ok(buildResp("入库", name, qty, "幂等跳过", processInstanceId));
        }

        String materialType = resolveType(formData);
        String spec = str(formData, "spec");
        String applicant = resolveApplicant(formData, body);
        LocalDate now = LocalDate.now();

        Long materialId;
        int stockAfter;
        boolean created;
        if (TYPE_CONSUMABLE.equals(materialType)) {
            EmsConsumable c = findConsumable(name, spec);
            if (c != null) {
                c.setQty((c.getQty() == null ? 0 : c.getQty()) + qty);
                c.setUpdateTime(now);
                consumableMapper.updateById(c);
                materialId = c.getId();
                stockAfter = c.getQty();
                created = false;
            } else {
                c = new EmsConsumable();
                c.setName(name);
                c.setSpec(spec);
                c.setQty(qty);
                c.setExpireDate(toDate(formData.get("expireDate")));
                c.setStatus("在库");
                c.setRemark(str(formData, "remark"));
                c.setCreateBy(applicant);
                c.setCreateTime(now);
                c.setUpdateTime(now);
                consumableMapper.insert(c);
                materialId = c.getId();
                stockAfter = qty;
                created = true;
            }
        } else {
            EmsStandardMaterial m = findMaterial(name, spec);
            if (m != null) {
                m.setStock((m.getStock() == null ? 0 : m.getStock()) + qty);
                if (StringUtils.hasText(str(formData, "lotNo"))) m.setLotNo(str(formData, "lotNo"));
                if (StringUtils.hasText(str(formData, "certNo"))) m.setCertNo(str(formData, "certNo"));
                LocalDate expire = toDate(formData.get("expireDate"));
                if (expire != null) m.setExpireDate(expire);
                m.setUpdateTime(now);
                materialMapper.updateById(m);
                materialId = m.getId();
                stockAfter = m.getStock();
                created = false;
            } else {
                m = new EmsStandardMaterial();
                m.setName(name);
                m.setSpec(spec);
                m.setLotNo(str(formData, "lotNo"));
                m.setCertNo(str(formData, "certNo"));
                m.setExpireDate(toDate(formData.get("expireDate")));
                m.setStock(qty);
                m.setStatus("在库");
                m.setRemark(str(formData, "remark"));
                m.setCreateBy(applicant);
                m.setCreateTime(now);
                m.setUpdateTime(now);
                materialMapper.insert(m);
                materialId = m.getId();
                stockAfter = qty;
                created = true;
            }
        }

        recordFlow("入库", materialType, materialId, name, spec, str(formData, "lotNo"), qty, applicant, processInstanceId, str(formData, "remark"));
        log.info("[MaterialWebhook] 物资入库完成: type={}, name={}, spec={}, qty={}, created={}, stockAfter={}",
                materialType, name, spec, qty, created, stockAfter);
        Map<String, Object> resp = buildResp("入库", name, qty, created ? "新建物资" : "更新库存", processInstanceId);
        resp.put("materialId", materialId);
        resp.put("stockAfter", stockAfter);
        return Result.ok(resp);
    }

    /** 使用审批通过：名称+规格已存在 -> 扣减库存量（不低于0）；否则新建物资 */
    @PostMapping("/usage")
    public Result<Map<String, Object>> usage(@RequestBody Map<String, Object> body) {
        Map<String, Object> formData = mergeFormData(body);
        String name = str(formData, "name");
        if (!StringUtils.hasText(name)) {
            return Result.fail(400, "缺少必要字段 name，无法办理物资使用");
        }
        int qty = toInt(formData.get("qty"), 0);
        if (qty <= 0) {
            return Result.fail(400, "使用数量必须大于 0");
        }
        Long processInstanceId = toLong(body.get("processInstanceId"));
        if (duplicated(processInstanceId, "出库")) {
            log.info("[MaterialWebhook] 流程实例 {} 已办理出库，跳过（幂等）", processInstanceId);
            return Result.ok(buildResp("出库", name, qty, "幂等跳过", processInstanceId));
        }

        String materialType = resolveType(formData);
        String spec = str(formData, "spec");
        String applicant = resolveApplicant(formData, body);
        LocalDate now = LocalDate.now();

        Long materialId;
        int stockAfter;
        boolean created;
        if (TYPE_CONSUMABLE.equals(materialType)) {
            EmsConsumable c = findConsumable(name, spec);
            if (c != null) {
                int before = c.getQty() == null ? 0 : c.getQty();
                c.setQty(Math.max(0, before - qty));
                c.setUpdateTime(now);
                consumableMapper.updateById(c);
                materialId = c.getId();
                stockAfter = c.getQty();
                created = false;
            } else {
                c = new EmsConsumable();
                c.setName(name);
                c.setSpec(spec);
                c.setQty(0);
                c.setStatus("在库");
                c.setRemark(str(formData, "remark"));
                c.setCreateBy(applicant);
                c.setCreateTime(now);
                c.setUpdateTime(now);
                consumableMapper.insert(c);
                materialId = c.getId();
                stockAfter = 0;
                created = true;
            }
        } else {
            EmsStandardMaterial m = findMaterial(name, spec);
            if (m != null) {
                int before = m.getStock() == null ? 0 : m.getStock();
                m.setStock(Math.max(0, before - qty));
                m.setUpdateTime(now);
                materialMapper.updateById(m);
                materialId = m.getId();
                stockAfter = m.getStock();
                created = false;
            } else {
                m = new EmsStandardMaterial();
                m.setName(name);
                m.setSpec(spec);
                m.setStock(0);
                m.setStatus("在库");
                m.setRemark(str(formData, "remark"));
                m.setCreateBy(applicant);
                m.setCreateTime(now);
                m.setUpdateTime(now);
                materialMapper.insert(m);
                materialId = m.getId();
                stockAfter = 0;
                created = true;
            }
        }

        recordFlow("出库", materialType, materialId, name, spec, str(formData, "lotNo"), qty, applicant, processInstanceId, str(formData, "remark"));
        log.info("[MaterialWebhook] 物资使用完成: type={}, name={}, spec={}, qty={}, created={}, stockAfter={}",
                materialType, name, spec, qty, created, stockAfter);
        return Result.ok(buildResp("出库", name, qty, created ? "新建物资" : "更新库存", processInstanceId));
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

    /** 幂等：同一流程实例同一流水类型只处理一次 */
    private boolean duplicated(Long processInstanceId, String bizType) {
        if (processInstanceId == null) return false;
        LambdaQueryWrapper<EmsMaterialFlow> q = new LambdaQueryWrapper<>();
        q.eq(EmsMaterialFlow::getProcessInstanceId, processInstanceId)
         .eq(EmsMaterialFlow::getBizType, bizType);
        return flowMapper.selectCount(q) > 0;
    }

    /** 按名称+规格查找标准物质 */
    private EmsStandardMaterial findMaterial(String name, String spec) {
        LambdaQueryWrapper<EmsStandardMaterial> q = new LambdaQueryWrapper<>();
        q.eq(EmsStandardMaterial::getName, name);
        if (StringUtils.hasText(spec)) q.eq(EmsStandardMaterial::getSpec, spec);
        q.last("LIMIT 1");
        return materialMapper.selectOne(q);
    }

    /** 按名称+规格查找耗材 */
    private EmsConsumable findConsumable(String name, String spec) {
        LambdaQueryWrapper<EmsConsumable> q = new LambdaQueryWrapper<>();
        q.eq(EmsConsumable::getName, name);
        if (StringUtils.hasText(spec)) q.eq(EmsConsumable::getSpec, spec);
        q.last("LIMIT 1");
        return consumableMapper.selectOne(q);
    }

    /** 写入入库/出库流水 */
    private void recordFlow(String bizType, String materialType, Long materialId, String name, String spec,
                            String lotNo, int qty, String applicant, Long processInstanceId, String remark) {
        EmsMaterialFlow flow = new EmsMaterialFlow();
        flow.setBizType(bizType);
        flow.setMaterialType(materialType);
        flow.setMaterialId(materialId);
        flow.setName(name);
        flow.setSpec(spec);
        flow.setLotNo(lotNo);
        flow.setQty(qty);
        flow.setApplicant(applicant);
        flow.setProcessInstanceId(processInstanceId);
        flow.setRemark(remark);
        flow.setCreateTime(LocalDate.now());
        flowMapper.insert(flow);
    }

    private String resolveType(Map<String, Object> formData) {
        String t = str(formData, "materialType");
        return TYPE_CONSUMABLE.equals(t) ? TYPE_CONSUMABLE : TYPE_MATERIAL;
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

    private Map<String, Object> buildResp(String bizType, String name, int qty, String action, Long processInstanceId) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("bizType", bizType);
        resp.put("name", name);
        resp.put("qty", qty);
        resp.put("action", action);
        resp.put("processInstanceId", processInstanceId);
        return resp;
    }

    private String str(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? String.valueOf(v) : null;
    }

    private int toInt(Object v, int def) {
        if (v == null) return def;
        try {
            return (int) Math.round(Double.parseDouble(String.valueOf(v).trim()));
        } catch (Exception e) {
            return def;
        }
    }

    private Long toLong(Object v) {
        if (v == null) return null;
        try {
            return Long.parseLong(String.valueOf(v).trim());
        } catch (Exception e) {
            return null;
        }
    }

    /** 兼容 yyyy-MM-dd / yyyy-MM-dd HH:mm:ss / ISO 等日期格式 */
    private LocalDate toDate(Object v) {
        if (v == null) return null;
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) return null;
        try {
            if (s.contains(" ")) s = s.split(" ")[0];
            if (s.contains("T")) s = s.split("T")[0];
            return LocalDate.parse(s);
        } catch (Exception e) {
            log.warn("[MaterialWebhook] 日期解析失败: {}", v);
            return null;
        }
    }
}
