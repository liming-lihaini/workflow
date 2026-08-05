package com.flow.engine.controllers;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flow.engine.common.Result;
import com.flow.engine.entity.EmsHazardousLedger;
import com.flow.engine.mapper.EmsHazardousLedgerMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 危化品台账 Webhook 处理接口（WXPRKSQ 流程-技术部门审批节点 afterComplete 触发）
 * <p>
 * 流程审批通过后，由流程引擎的 Webhook 机制回调本接口：
 * - 若危化品台账(t_hazardous_ledger)已存在相同 CAS 编号，则 库存数量 = 当前数量 + 入库数量（叠加）；
 * - 若不存在相同 CAS 编号，则新增一条危化品数据（入库数量作为初始库存，状态默认为【在库】）。
 */
@Slf4j
@RestController
@RequestMapping("/api/webhook/hazardous")
public class HazardousWebhookController {

    private final EmsHazardousLedgerMapper hazardousMapper;

    public HazardousWebhookController(EmsHazardousLedgerMapper hazardousMapper) {
        this.hazardousMapper = hazardousMapper;
    }

    /**
     * 入库审批通过后的危化品台账同步
     *
     * @param body webhook payload，关键字段：
     *             formData / variables：流程表单字段（casNo、qty、name、category、unit、location 等）
     *             nodeId、nodeName、processKey、processInstanceId：流程上下文
     */
    @PostMapping("/upsert")
    public Result<Map<String, Object>> upsert(@RequestBody Map<String, Object> body) {
        // 表单字段可能位于 formData 或 variables，兼容两种结构
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
            // variables 作为兜底，避免覆盖 formData 中已有的字段
            m.forEach((k, v) -> formData.putIfAbsent(k, v));
        }
        // 若都没有，则直接使用顶层字段
        if (formData.isEmpty() && !body.isEmpty()) {
            formData.putAll(body);
        }

        Object casObj = pick(formData, "casNo", "cas_no");
        Object qtyObj = pick(formData, "qty", "quantity", "inboundQty", "inbound_qty");
        if (casObj == null || qtyObj == null) {
            return Result.fail(400, "缺少必要字段 casNo 或 qty，无法同步危化品台账");
        }

        String casNo = String.valueOf(casObj).trim();
        BigDecimal inboundQty = toBigDecimal(qtyObj);
        if (casNo.isEmpty()) {
            return Result.fail(400, "casNo 为空，无法同步危化品台账");
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("casNo", casNo);
        resp.put("inboundQty", inboundQty.toPlainString());

        LambdaQueryWrapper<EmsHazardousLedger> q = new LambdaQueryWrapper<>();
        q.eq(EmsHazardousLedger::getCasNo, casNo);
        EmsHazardousLedger exist = hazardousMapper.selectOne(q);

        if (exist != null) {
            // 相同 CAS 编号：库存数量 = 当前数量 + 入库数量（叠加）
            BigDecimal currentQty = toBigDecimal(exist.getQty());
            BigDecimal total = currentQty.add(inboundQty);
            exist.setQty(total.toPlainString());
            exist.setStatus(exist.getStatus() == null ? "在库" : exist.getStatus());
            exist.setUpdateTime(LocalDate.now());
            hazardousMapper.updateById(exist);

            resp.put("action", "MERGE");
            resp.put("ledgerId", exist.getId());
            resp.put("currentQty", currentQty.toPlainString());
            resp.put("newQty", total.toPlainString());
            log.info("[HazardousWebhook] CAS={} 已存在，库存叠加 {} -> {}", casNo, currentQty, total);
        } else {
            // 不存在相同 CAS 编号：新增一条危化品数据
            EmsHazardousLedger h = new EmsHazardousLedger();
            h.setName(str(formData, "name"));
            h.setCasNo(casNo);
            h.setCategory(str(formData, "category"));
            h.setQty(inboundQty.toPlainString());
            h.setUnit(str(formData, "unit"));
            h.setStatus("在库");
            Object location = pick(formData, "location", "remark", "storageLocation");
            h.setRemark(location != null ? String.valueOf(location) : null);
            LocalDate today = LocalDate.now();
            h.setCreateTime(today);
            h.setUpdateTime(today);
            hazardousMapper.insert(h);

            resp.put("action", "INSERT");
            resp.put("ledgerId", h.getId());
            resp.put("newQty", inboundQty.toPlainString());
            log.info("[HazardousWebhook] CAS={} 不存在，新增危化品台账记录 id={}", casNo, h.getId());
        }

        return Result.ok(resp);
    }

    private Object pick(Map<String, Object> map, String... keys) {
        for (String k : keys) {
            if (map.containsKey(k) && map.get(k) != null) {
                return map.get(k);
            }
        }
        return null;
    }

    private String str(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? String.valueOf(v) : null;
    }

    private BigDecimal toBigDecimal(Object v) {
        if (v == null) return BigDecimal.ZERO;
        try {
            return new BigDecimal(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
