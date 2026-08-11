package com.flow.engine.controllers;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flow.engine.common.Result;
import com.flow.engine.entity.DictItem;
import com.flow.engine.entity.EmsEntrust;
import com.flow.engine.entity.EmsFileMeta;
import com.flow.engine.entity.EmsMonitorPoint;
import com.flow.engine.entity.EmsSampleParamConfig;
import com.flow.engine.entity.ProcessInstance;
import com.flow.engine.entity.User;
import com.flow.engine.mapper.EmsEntrustMapper;
import com.flow.engine.mapper.EmsFileMetaMapper;
import com.flow.engine.mapper.EmsMonitorPointMapper;
import com.flow.engine.mapper.EmsSampleParamConfigMapper;
import com.flow.engine.mapper.ProcessInstanceMapper;
import com.flow.engine.mapper.UserMapper;
import com.flow.engine.service.DictService;
import com.flow.engine.util.CodeGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 检测委托申请 Webhook 处理接口
 * <p>
 * 由 JCWTSQ（检测委托申请）流程的技术部审批节点(userTask_approve)
 * NODE_COMPLETED 事件触发回调：
 * - 从 formData 读取委托申请表单字段（entrustName/custId/source/sampleFreq/urgent/startDate/description）；
 * - 创建委托单：状态=已确认，委托单号=WT+yyyyMMdd+4位序号（如 WT202608100001）；
 * - 从 formData.points 子表逐行写入监测点位（t_monitor_point，关联新委托ID）；
 * - 创建人 = 流程发起人（按 processInstanceId 反查 wf_process_instance.start_user）；
 * - 以 process_instance_id 做幂等键，避免 Webhook 重试产生重复委托。
 */
@Slf4j
@RestController
@RequestMapping("/api/webhook/entrust")
public class EntrustWebhookController {

    private final EmsEntrustMapper entrustMapper;
    private final EmsMonitorPointMapper monitorPointMapper;
    private final ProcessInstanceMapper processInstanceMapper;
    private final UserMapper userMapper;
    private final EmsSampleParamConfigMapper sampleParamConfigMapper;
    private final EmsFileMetaMapper fileMetaMapper;
    private final DictService dictService;

    public EntrustWebhookController(EmsEntrustMapper entrustMapper,
                                    EmsMonitorPointMapper monitorPointMapper,
                                    ProcessInstanceMapper processInstanceMapper,
                                    UserMapper userMapper,
                                    EmsSampleParamConfigMapper sampleParamConfigMapper,
                                    EmsFileMetaMapper fileMetaMapper,
                                    DictService dictService) {
        this.entrustMapper = entrustMapper;
        this.monitorPointMapper = monitorPointMapper;
        this.processInstanceMapper = processInstanceMapper;
        this.userMapper = userMapper;
        this.sampleParamConfigMapper = sampleParamConfigMapper;
        this.fileMetaMapper = fileMetaMapper;
        this.dictService = dictService;
    }

    /**
     * 技术部审批通过后的委托单创建
     *
     * @param body webhook payload，关键字段：
     *             formData / variables：流程表单字段（entrustName、custId、source、points 子表等）
     *             processInstanceId：流程实例ID（幂等键 + 反查发起人）
     */
    @PostMapping("/confirm")
    public Result<Map<String, Object>> confirm(@RequestBody Map<String, Object> body) {
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

        String entrustName = str(formData, "entrustName");
        if (!StringUtils.hasText(entrustName)) {
            return Result.fail(400, "缺少必要字段 entrustName，无法创建委托单");
        }

        Long processInstanceId = toLong(body.get("processInstanceId"));

        // 幂等：同一流程实例只创建一次委托
        if (processInstanceId != null) {
            LambdaQueryWrapper<EmsEntrust> dupQ = new LambdaQueryWrapper<>();
            dupQ.eq(EmsEntrust::getProcessInstanceId, processInstanceId);
            EmsEntrust exist = entrustMapper.selectOne(dupQ);
            if (exist != null) {
                log.info("[EntrustWebhook] 流程实例 {} 已创建委托 {}，跳过（幂等）", processInstanceId, exist.getEntrustNo());
                return Result.ok(buildResp(exist, 0, 0));
            }
        }

        // 创建人 = 流程发起人（反查流程实例）；兜底 formData.applicant / system
        String createBy = null;
        if (processInstanceId != null) {
            ProcessInstance pi = processInstanceMapper.selectById(processInstanceId);
            if (pi != null) createBy = pi.getStartUser();
        }
        if (!StringUtils.hasText(createBy)) {
            createBy = str(formData, "applicant");
        }
        if (!StringUtils.hasText(createBy)) {
            Object su = body.get("startUser");
            createBy = su != null ? String.valueOf(su) : "system";
        }
        String createName = lookupRealName(createBy);

        // 创建时间 = 审批通过时间（Webhook 触发时刻）
        LocalDateTime approveTime = LocalDateTime.now();

        EmsEntrust entrust = new EmsEntrust();
        entrust.setEntrustName(entrustName);
        entrust.setCustId(toLong(formData.get("custId")));
        String source = str(formData, "source");
        entrust.setSource(source);
        entrust.setSourceName(dictName("moni_entrust_source", source));
        String sampleFreq = str(formData, "sampleFreq");
        entrust.setSampleFreq(sampleFreq);
        entrust.setSampleFreqName(dictName("moni_sample_freq", sampleFreq));
        entrust.setUrgent(toInt(formData.get("urgent"), 0));
        entrust.setStartDate(str(formData, "startDate"));
        entrust.setDescription(str(formData, "description"));
        entrust.setStatus("已确认");
        entrust.setSubmitBy(createBy);
        entrust.setCreateBy(createBy);
        entrust.setCreateName(createName);
        entrust.setUpdateBy(createBy);
        entrust.setUpdateName(createName);
        entrust.setCreateTime(approveTime);
        entrust.setUpdateTime(approveTime);
        entrust.setProcessInstanceId(processInstanceId);
        // 委托单号：WT + yyyyMMdd + 当日4位序号（与 EmsEntrustService.techConfirm 同规则）
        String prefix = "WT" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        LambdaQueryWrapper<EmsEntrust> cntQ = new LambdaQueryWrapper<>();
        cntQ.likeRight(EmsEntrust::getEntrustNo, prefix);
        long cnt = entrustMapper.selectCount(cntQ);
        entrust.setEntrustNo(CodeGenerator.generate("WT", (int) cnt + 1));

        entrustMapper.insert(entrust);
        log.info("[EntrustWebhook] 创建委托单: entrustNo={}, entrustName={}, createBy={}",
                entrust.getEntrustNo(), entrustName, createBy);

        // 监测点位子表（formData.points）逐行写入 t_monitor_point
        int pointCount = insertPoints(formData.get("points"), entrust, approveTime);

        // 流程附件（formData.attachments）归档到委托关联附件 t_file_meta（bizType=entrust）
        int attachmentCount = archiveAttachments(formData.get("attachments"), entrust, createBy, approveTime);

        return Result.ok(buildResp(entrust, pointCount, attachmentCount));
    }

    /** 写入监测点位子表行；返回写入条数 */
    private int insertPoints(Object points, EmsEntrust entrust, LocalDateTime approveTime) {
        if (!(points instanceof List)) return 0;
        int count = 0;
        for (Object row : (List<?>) points) {
            if (!(row instanceof Map)) continue;
            @SuppressWarnings("unchecked")
            Map<String, Object> rowMap = (Map<String, Object>) row;
            String pointName = str(rowMap, "pointName");
            if (!StringUtils.hasText(pointName)) continue;

            EmsMonitorPoint p = new EmsMonitorPoint();
            p.setEntrustId(entrust.getId());
            p.setCustId(entrust.getCustId());
            // 点位编号：表单传入优先，否则按委托内顺序生成 P001/P002...（与 EmsEntrustService 同规则）
            String pointNo = str(rowMap, "pointNo");
            p.setPointNo(StringUtils.hasText(pointNo) ? pointNo : String.format("P%03d", count + 1));
            p.setPointName(pointName);
            p.setLng(toDouble(rowMap.get("lng")));
            p.setLat(toDouble(rowMap.get("lat")));
            // 监测类别：字典 key 转换为字典文本后存储
            String pointType = str(rowMap, "pointType");
            String pointTypeName = dictName("moni_point_type", pointType);
            p.setPointType(StringUtils.hasText(pointTypeName) ? pointTypeName : pointType);
            p.setPointTypeName(StringUtils.hasText(pointTypeName) ? pointTypeName : pointType);
            // 监测因子：多选字典 key 逐个转换为字典文本后逗号拼接存储
            List<String> factorKeys = new ArrayList<>();
            Object factors = rowMap.get("factors");
            if (factors instanceof List) {
                for (Object f : (List<?>) factors) {
                    if (f != null && StringUtils.hasText(String.valueOf(f))) factorKeys.add(String.valueOf(f).trim());
                }
            } else if (factors != null && StringUtils.hasText(String.valueOf(factors))) {
                // 兼容逗号分隔字符串
                for (String s : String.valueOf(factors).split(",")) {
                    if (StringUtils.hasText(s)) factorKeys.add(s.trim());
                }
            }
            List<String> factorTexts = new ArrayList<>();
            for (String key : factorKeys) {
                String text = dictName("moni_monitor_factor", key);
                factorTexts.add(StringUtils.hasText(text) ? text : key);
            }
            if (!factorTexts.isEmpty()) {
                p.setFactors(String.join(",", factorTexts));
            }
            // 执行标准：按监测因子文本查询 t_sample_param_config.standard，多个逗号拼接
            String standardCode = resolveStandardCode(factorTexts);
            if (!StringUtils.hasText(standardCode)) {
                standardCode = str(rowMap, "standardCode");
            }
            p.setStandardCode(standardCode);
            p.setCondition(str(rowMap, "condition"));
            p.setCreateTime(approveTime);
            p.setUpdateTime(approveTime);
            monitorPointMapper.insert(p);
            count++;
        }
        log.info("[EntrustWebhook] 委托 {} 写入监测点位 {} 条", entrust.getEntrustNo(), count);
        return count;
    }

    /**
     * 按监测因子文本查询 t_sample_param_config（item 匹配），
     * 收集执行标准 standard，去重后逗号拼接；无匹配返回 null
     */
    private String resolveStandardCode(List<String> factorTexts) {
        if (factorTexts == null || factorTexts.isEmpty()) return null;
        List<String> standards = new ArrayList<>();
        for (String text : factorTexts) {
            if (!StringUtils.hasText(text)) continue;
            try {
                LambdaQueryWrapper<EmsSampleParamConfig> q = new LambdaQueryWrapper<>();
                q.eq(EmsSampleParamConfig::getItem, text);
                List<EmsSampleParamConfig> configs = sampleParamConfigMapper.selectList(q);
                for (EmsSampleParamConfig c : configs) {
                    if (StringUtils.hasText(c.getStandard()) && !standards.contains(c.getStandard())) {
                        standards.add(c.getStandard());
                    }
                }
            } catch (Exception e) {
                log.warn("[EntrustWebhook] 执行标准查询失败, factor={}: {}", text, e.getMessage());
            }
        }
        return standards.isEmpty() ? null : String.join(",", standards);
    }

    /**
     * 归档流程附件到委托关联附件（t_file_meta, bizType=entrust）。
     * 表单文件字段值为 [{name, path, size}]；按 filePath 幂等，避免重复归档。
     * 返回归档条数
     */
    private int archiveAttachments(Object attachments, EmsEntrust entrust, String uploadBy, LocalDateTime time) {
        if (!(attachments instanceof List)) return 0;
        int count = 0;
        for (Object item : (List<?>) attachments) {
            if (!(item instanceof Map)) continue;
            @SuppressWarnings("unchecked")
            Map<String, Object> m = (Map<String, Object>) item;
            String path = str(m, "path");
            if (!StringUtils.hasText(path)) continue;
            // 幂等：同一委托下相同路径不重复归档
            LambdaQueryWrapper<EmsFileMeta> dupQ = new LambdaQueryWrapper<>();
            dupQ.eq(EmsFileMeta::getBizType, "entrust")
                .eq(EmsFileMeta::getBizId, entrust.getId())
                .eq(EmsFileMeta::getFilePath, path);
            if (fileMetaMapper.selectCount(dupQ) > 0) continue;
            String name = str(m, "name");
            EmsFileMeta f = new EmsFileMeta();
            f.setBizType("entrust");
            f.setBizId(entrust.getId());
            f.setFileName(StringUtils.hasText(name) ? name : path);
            f.setFilePath(path);
            f.setSize(toLong(m.get("size")));
            f.setUploadBy(uploadBy);
            f.setCreateTime(time);
            fileMetaMapper.insert(f);
            count++;
        }
        if (count > 0) {
            log.info("[EntrustWebhook] 委托 {} 归档附件 {} 个", entrust.getEntrustNo(), count);
        }
        return count;
    }

    private Map<String, Object> buildResp(EmsEntrust entrust, int pointCount, int attachmentCount) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("entrustId", entrust.getId());
        resp.put("entrustNo", entrust.getEntrustNo());
        resp.put("status", entrust.getStatus());
        resp.put("createBy", entrust.getCreateBy());
        resp.put("pointCount", pointCount);
        resp.put("attachmentCount", attachmentCount);
        return resp;
    }

    /** 按字典 code + itemValue 查字典名称；未命中返回 null */
    private String dictName(String dictCode, String itemValue) {
        if (!StringUtils.hasText(dictCode) || !StringUtils.hasText(itemValue)) return null;
        try {
            List<DictItem> items = dictService.getDictItemsByCode(dictCode);
            for (DictItem it : items) {
                if (itemValue.equals(it.getItemValue())) return it.getItemText();
            }
        } catch (Exception e) {
            log.warn("[EntrustWebhook] 字典 {} 查询失败: {}", dictCode, e.getMessage());
        }
        return null;
    }

    private String lookupRealName(String username) {
        if (!StringUtils.hasText(username)) return null;
        try {
            LambdaQueryWrapper<User> q = new LambdaQueryWrapper<>();
            q.eq(User::getUsername, username);
            User u = userMapper.selectOne(q);
            return u != null ? u.getRealName() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String str(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v == null) return null;
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? null : s;
    }

    private Long toLong(Object v) {
        if (v == null) return null;
        try {
            return Long.parseLong(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer toInt(Object v, int def) {
        if (v == null) return def;
        try {
            return (int) Double.parseDouble(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private Double toDouble(Object v) {
        if (v == null) return null;
        try {
            return Double.parseDouble(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
