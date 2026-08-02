package com.flow.engine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.flow.engine.entity.EmsDetectionResult;
import com.flow.engine.entity.EmsDetectionTask;
import com.flow.engine.entity.EmsReport;
import com.flow.engine.entity.EmsReportAudit;
import com.flow.engine.entity.EmsReportItem;
import com.flow.engine.entity.EmsReportTemplate;
import com.flow.engine.mapper.EmsDetectionResultMapper;
import com.flow.engine.mapper.EmsDetectionTaskMapper;
import com.flow.engine.mapper.EmsReportAuditMapper;
import com.flow.engine.mapper.EmsReportItemMapper;
import com.flow.engine.mapper.EmsReportMapper;
import com.flow.engine.mapper.EmsReportTemplateMapper;
import com.flow.engine.util.CodeGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmsReportService {

    private final EmsReportTemplateMapper templateMapper;
    private final EmsReportMapper reportMapper;
    private final EmsReportItemMapper reportItemMapper;
    private final EmsReportAuditMapper reportAuditMapper;
    private final EmsDetectionTaskMapper taskMapper;
    private final EmsDetectionResultMapper resultMapper;

    public EmsReportService(EmsReportTemplateMapper templateMapper,
                            EmsReportMapper reportMapper,
                            EmsReportItemMapper reportItemMapper,
                            EmsReportAuditMapper reportAuditMapper,
                            EmsDetectionTaskMapper taskMapper,
                            EmsDetectionResultMapper resultMapper) {
        this.templateMapper = templateMapper;
        this.reportMapper = reportMapper;
        this.reportItemMapper = reportItemMapper;
        this.reportAuditMapper = reportAuditMapper;
        this.taskMapper = taskMapper;
        this.resultMapper = resultMapper;
    }

    private String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /* ---------------- 模板 ---------------- */

    public List<EmsReportTemplate> listTemplates() {
        LambdaQueryWrapper<EmsReportTemplate> qw = new LambdaQueryWrapper<>();
        qw.eq(EmsReportTemplate::getEnabled, "1").orderByDesc(EmsReportTemplate::getId);
        return templateMapper.selectList(qw);
    }

    @Transactional
    public Long createTemplate(String name, String type, String content, String remark) {
        EmsReportTemplate tpl = new EmsReportTemplate();
        int tplSeq = (int) (templateMapper.selectCount(new LambdaQueryWrapper<>()) + 1);
        tpl.setTplNo(CodeGenerator.generate("RPT", tplSeq));
        tpl.setName(name);
        tpl.setType(type);
        tpl.setContent(content);
        tpl.setEnabled("1");
        tpl.setRemark(remark);
        LocalDateTime now = LocalDateTime.now();
        tpl.setCreateTime(now);
        tpl.setUpdateTime(now);
        templateMapper.insert(tpl);
        return tpl.getId();
    }

    /* ---------------- 报告生成 ---------------- */

    /** 待生成报告的可选检测任务：状态为 "已复核" 的任务 */
    public List<Map<String, Object>> pendingTasks() {
        LambdaQueryWrapper<EmsDetectionTask> qw = new LambdaQueryWrapper<>();
        qw.eq(EmsDetectionTask::getStatus, "已复核").orderByDesc(EmsDetectionTask::getId);
        List<EmsDetectionTask> tasks = taskMapper.selectList(qw);
        List<Map<String, Object>> list = new ArrayList<>();
        for (EmsDetectionTask t : tasks) {
            Map<String, Object> m = new HashMap<>();
            m.put("taskId", t.getId());
            m.put("taskNo", t.getTaskNo());
            m.put("sampleName", t.getSampleName());
            m.put("sampleCode", t.getBarcode());
            m.put("monitorItem", t.getMonitorItems());
            m.put("reviewBy", t.getReviewBy());
            list.add(m);
        }
        return list;
    }

    @Transactional
    public Long generateReport(Long tplId, String title, String client, String period,
                               List<Long> taskIds, String generator) {
        if (taskIds == null || taskIds.isEmpty()) {
            throw new IllegalArgumentException("请至少选择一个检测任务");
        }
        EmsReportTemplate tpl = templateMapper.selectById(tplId);
        if (tpl == null) {
            throw new IllegalArgumentException("报告模板不存在");
        }

        // 校验任务均为已复核
        for (Long tid : taskIds) {
            EmsDetectionTask t = taskMapper.selectById(tid);
            if (t == null) {
                throw new IllegalArgumentException("检测任务不存在: " + tid);
            }
            if (!"已复核".equals(t.getStatus())) {
                throw new IllegalArgumentException("检测任务未复核，不能生成报告: " + t.getTaskNo());
            }
        }

        int itemCount = 0;
        int exceedCount = 0;
        List<EmsReportItem> items = new ArrayList<>();
        for (Long tid : taskIds) {
            LambdaQueryWrapper<EmsDetectionResult> rqw = new LambdaQueryWrapper<>();
            rqw.eq(EmsDetectionResult::getTaskId, tid);
            List<EmsDetectionResult> results = resultMapper.selectList(rqw);
            for (EmsDetectionResult r : results) {
                EmsReportItem ri = new EmsReportItem();
                ri.setReportId(0L); // 暂置0，下方回填
                ri.setTaskId(tid);
                ri.setItem(r.getMonitorItem());
                ri.setSampleCode(String.valueOf(r.getSampleId()));
                ri.setResult(r.getValue());
                ri.setUnit(r.getUnit());
                ri.setStandardLimit(r.getLimitValue());
                ri.setConclusion(r.getConclusion());
                ri.setCreateTime(LocalDateTime.now());
                items.add(ri);
                itemCount++;
                if ("超标".equals(r.getConclusion())) {
                    exceedCount++;
                }
            }
        }

        EmsReport report = new EmsReport();
        int rpSeq = reportMapper.selectCount(new LambdaQueryWrapper<>()).intValue() + 1;
        report.setReportNo(CodeGenerator.generate("RP", rpSeq));
        report.setTitle(title);
        report.setTplId(tplId);
        report.setTplType(tpl.getType());
        report.setClient(client);
        report.setPeriod(period);
        report.setTaskIds(toString(taskIds));
        report.setItemCount(itemCount);
        report.setExceedCount(exceedCount);
        report.setStatus("待审核");
        report.setGenerator(generator);
        LocalDateTime now = LocalDateTime.now();
        report.setCreateTime(now);
        report.setUpdateTime(now);
        reportMapper.insert(report);

        for (EmsReportItem ri : items) {
            ri.setReportId(report.getId());
        }
        for (EmsReportItem ri : items) {
            reportItemMapper.insert(ri);
        }
        return report.getId();
    }

    private String toString(List<Long> ids) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(ids.get(i));
        }
        sb.append("]");
        return sb.toString();
    }

    public List<Map<String, Object>> listReports(String status) {
        QueryWrapper<EmsReport> qw = new QueryWrapper<>();
        qw.orderByDesc("id");
        if (StringUtils.hasText(status)) {
            qw.eq("status", status);
        }
        List<EmsReport> reports = reportMapper.selectList(qw);
        List<Map<String, Object>> list = new ArrayList<>();
        for (EmsReport r : reports) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", r.getId());
            m.put("reportNo", r.getReportNo());
            m.put("title", r.getTitle());
            m.put("tplType", r.getTplType());
            m.put("client", r.getClient());
            m.put("period", r.getPeriod());
            m.put("itemCount", r.getItemCount());
            m.put("exceedCount", r.getExceedCount());
            m.put("status", r.getStatus());
            m.put("antiFakeCode", r.getAntiFakeCode());
            m.put("generator", r.getGenerator());
            m.put("publishTime", r.getPublishTime());
            m.put("createTime", r.getCreateTime());
            list.add(m);
        }
        return list;
    }

    public Map<String, Object> getReportDetail(Long id) {
        EmsReport report = reportMapper.selectById(id);
        if (report == null) {
            throw new IllegalArgumentException("报告不存在");
        }
        LambdaQueryWrapper<EmsReportItem> iqw = new LambdaQueryWrapper<>();
        iqw.eq(EmsReportItem::getReportId, id).orderByAsc(EmsReportItem::getId);
        List<EmsReportItem> items = reportItemMapper.selectList(iqw);

        LambdaQueryWrapper<EmsReportAudit> aqw = new LambdaQueryWrapper<>();
        aqw.eq(EmsReportAudit::getReportId, id).orderByDesc(EmsReportAudit::getId);
        List<EmsReportAudit> audits = reportAuditMapper.selectList(aqw);

        Map<String, Object> m = new HashMap<>();
        m.put("report", report);
        m.put("items", items);
        m.put("audits", audits);
        return m;
    }

    /* ---------------- 审核 ---------------- */

    @Transactional
    public void approve(Long id, String auditor) {
        EmsReport report = reportMapper.selectById(id);
        if (report == null) {
            throw new IllegalArgumentException("报告不存在");
        }
        if (!"待审核".equals(report.getStatus())) {
            throw new IllegalArgumentException("仅待审核报告可审核");
        }
        report.setStatus("已发布");
        report.setAntiFakeCode(genAntiFake(report.getReportNo()));
        report.setPublishTime(now());
        report.setUpdateTime(LocalDateTime.now());
        reportMapper.updateById(report);

        EmsReportAudit audit = new EmsReportAudit();
        audit.setReportId(id);
        audit.setAuditor(auditor);
        audit.setDecision("通过");
        audit.setOpinion("审核通过，予以发布");
        audit.setCreateTime(LocalDateTime.now());
        reportAuditMapper.insert(audit);
    }

    @Transactional
    public void reject(Long id, String auditor, String opinion) {
        EmsReport report = reportMapper.selectById(id);
        if (report == null) {
            throw new IllegalArgumentException("报告不存在");
        }
        if (!"待审核".equals(report.getStatus())) {
            throw new IllegalArgumentException("仅待审核报告可审核");
        }
        report.setStatus("已退回");
        report.setUpdateTime(LocalDateTime.now());
        reportMapper.updateById(report);

        EmsReportAudit audit = new EmsReportAudit();
        audit.setReportId(id);
        audit.setAuditor(auditor);
        audit.setDecision("退回");
        audit.setOpinion(opinion);
        audit.setCreateTime(LocalDateTime.now());
        reportAuditMapper.insert(audit);
    }

    /** 防伪码：报告编号 + 8位随机大写字母数字 */
    private String genAntiFake(String reportNo) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return reportNo + "-" + sb;
    }
}
