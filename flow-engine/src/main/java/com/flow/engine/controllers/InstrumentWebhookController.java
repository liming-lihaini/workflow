package com.flow.engine.controllers;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flow.engine.common.Result;
import com.flow.engine.entity.EmsInstrument;
import com.flow.engine.mapper.EmsInstrumentMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 新设备入库 Webhook 处理接口
 * <p>
 * 由 SBTKRKSQ（单台）/ SBTKRKSQ_PL（批量）流程的审批节点(userTask_approve)
 * NODE_COMPLETED 事件触发回调：
 * - 单台流程：从 formData 平铺字段写入 1 条设备记录；
 * - 批量流程：从 formData.devices 子表逐行写入设备记录；
 * - 创建人(create_by) = 流程申请人(formData.applicant)；
 * - 创建时间(create_time) = 审批通过时间（回调触发时刻）；
 * - 相同仪器编号已存在时跳过（幂等，避免重试产生重复数据）。
 */
@Slf4j
@RestController
@RequestMapping("/api/webhook/instrument")
public class InstrumentWebhookController {

    private final EmsInstrumentMapper instrumentMapper;

    public InstrumentWebhookController(EmsInstrumentMapper instrumentMapper) {
        this.instrumentMapper = instrumentMapper;
    }

    /**
     * 入库审批通过后的设备台账登记
     *
     * @param body webhook payload，关键字段：
     *             formData / variables：流程表单字段（单台为平铺字段；批量含 devices 子表数组、applicant 申请人）
     */
    @PostMapping("/inbound")
    public Result<Map<String, Object>> inbound(@RequestBody Map<String, Object> body) {
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
            m.forEach((k, v) -> formData.putIfAbsent(k, v));
        }
        if (formData.isEmpty() && !body.isEmpty()) {
            formData.putAll(body);
        }

        // 创建人 = 申请人（表单 user 字段）；兜底取流程发起人
        String createBy = str(formData, "applicant");
        if (createBy == null || createBy.isEmpty()) {
            Object su = body.get("startUser");
            createBy = su != null ? String.valueOf(su) : "system";
        }
        // 创建时间 = 审批通过时间（Webhook 触发时刻）
        LocalDateTime approveTime = LocalDateTime.now();

        List<String> inserted = new ArrayList<>();
        List<String> skipped = new ArrayList<>();

        Object devices = formData.get("devices");
        if (devices instanceof List && !((List<?>) devices).isEmpty()) {
            // 批量入库：遍历子表行
            for (Object row : (List<?>) devices) {
                if (!(row instanceof Map)) continue;
                @SuppressWarnings("unchecked")
                Map<String, Object> rowMap = (Map<String, Object>) row;
                inboundOne(rowMap, createBy, approveTime, inserted, skipped);
            }
        } else {
            // 单台入库：平铺字段
            inboundOne(formData, createBy, approveTime, inserted, skipped);
        }

        if (inserted.isEmpty() && skipped.isEmpty()) {
            return Result.fail(400, "未解析到可入库的设备数据（单台需填写仪器编号；批量需在设备清单中录入明细）");
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("createBy", createBy);
        resp.put("approveTime", approveTime.toString());
        resp.put("inserted", inserted);
        resp.put("skipped", skipped);
        log.info("[InstrumentWebhook] 设备入库完成: createBy={}, inserted={}, skipped={}", createBy, inserted, skipped);
        return Result.ok(resp);
    }

    /** 写入单条设备记录；编号缺失或已存在时跳过 */
    private void inboundOne(Map<String, Object> src, String createBy, LocalDateTime approveTime,
                            List<String> inserted, List<String> skipped) {
        String code = str(src, "code");
        if (code == null || code.isEmpty()) {
            log.warn("[InstrumentWebhook] 跳过缺少仪器编号的设备行: {}", src);
            return;
        }
        LambdaQueryWrapper<EmsInstrument> q = new LambdaQueryWrapper<>();
        q.eq(EmsInstrument::getCode, code);
        if (instrumentMapper.selectCount(q) > 0) {
            skipped.add(code);
            log.info("[InstrumentWebhook] 仪器编号 {} 已存在，跳过（幂等）", code);
            return;
        }

        EmsInstrument ins = new EmsInstrument();
        ins.setCode(code);
        ins.setName(str(src, "name"));
        ins.setModel(str(src, "model"));
        ins.setManufacturer(str(src, "manufacturer"));
        ins.setPurchaseDate(toDate(src.get("purchaseDate")));
        ins.setLastCalibDate(toDate(src.get("lastCalibDate")));
        ins.setCalibDue(toDate(src.get("calibDue")));
        ins.setCertNo(str(src, "certNo"));
        String status = str(src, "status");
        ins.setStatus(status == null || status.isEmpty() ? "在用" : status);
        ins.setRemark(str(src, "remark"));
        ins.setCreateBy(createBy);
        ins.setCreateTime(approveTime);
        ins.setUpdateTime(approveTime);
        instrumentMapper.insert(ins);
        inserted.add(code);
        log.info("[InstrumentWebhook] 新增设备台账: code={}, name={}, id={}", code, ins.getName(), ins.getId());
    }

    private String str(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? String.valueOf(v) : null;
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
            log.warn("[InstrumentWebhook] 日期解析失败: {}", v);
            return null;
        }
    }
}
