package com.flow.engine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.flow.engine.entity.EmsCustomer;
import com.flow.engine.entity.EmsDetectionResult;
import com.flow.engine.entity.EmsDetectionTask;
import com.flow.engine.entity.EmsEntrust;
import com.flow.engine.entity.EmsMonitorPoint;
import com.flow.engine.entity.EmsReport;
import com.flow.engine.entity.EmsReportAudit;
import com.flow.engine.entity.EmsReportItem;
import com.flow.engine.entity.EmsReportTemplate;
import com.flow.engine.entity.EmsSample;
import com.flow.engine.mapper.EmsCustomerMapper;
import com.flow.engine.mapper.EmsDetectionResultMapper;
import com.flow.engine.mapper.EmsDetectionTaskMapper;
import com.flow.engine.mapper.EmsEntrustMapper;
import com.flow.engine.mapper.EmsMonitorPointMapper;
import com.flow.engine.mapper.EmsReportAuditMapper;
import com.flow.engine.mapper.EmsReportItemMapper;
import com.flow.engine.mapper.EmsReportMapper;
import com.flow.engine.mapper.EmsReportTemplateMapper;
import com.flow.engine.mapper.EmsSampleMapper;
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
    private final EmsSampleMapper sampleMapper;
    private final EmsMonitorPointMapper pointMapper;
    private final EmsEntrustMapper entrustMapper;
    private final EmsCustomerMapper customerMapper;

    public EmsReportService(EmsReportTemplateMapper templateMapper,
                            EmsReportMapper reportMapper,
                            EmsReportItemMapper reportItemMapper,
                            EmsReportAuditMapper reportAuditMapper,
                            EmsDetectionTaskMapper taskMapper,
                            EmsDetectionResultMapper resultMapper,
                            EmsSampleMapper sampleMapper,
                            EmsMonitorPointMapper pointMapper,
                            EmsEntrustMapper entrustMapper,
                            EmsCustomerMapper customerMapper) {
        this.templateMapper = templateMapper;
        this.reportMapper = reportMapper;
        this.reportItemMapper = reportItemMapper;
        this.reportAuditMapper = reportAuditMapper;
        this.taskMapper = taskMapper;
        this.resultMapper = resultMapper;
        this.sampleMapper = sampleMapper;
        this.pointMapper = pointMapper;
        this.entrustMapper = entrustMapper;
        this.customerMapper = customerMapper;
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
            m.put("cmaCertNo", r.getCmaCertNo());
            m.put("reviewer", r.getReviewer());
            m.put("approver", r.getApprover());
            m.put("entrustId", r.getEntrustId());
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

    /* ---------------- 监测报告（委托驱动，req.md） ---------------- */

    /** 委托 → 关联样品的检测任务清单（报告生成表单勾选用） */
    public List<Map<String, Object>> entrustTasks(Long entrustId) {
        LambdaQueryWrapper<EmsSample> sqw = new LambdaQueryWrapper<>();
        sqw.eq(EmsSample::getEntrustId, entrustId).orderByAsc(EmsSample::getId);
        List<EmsSample> samples = sampleMapper.selectList(sqw);
        List<Map<String, Object>> list = new ArrayList<>();
        for (EmsSample s : samples) {
            LambdaQueryWrapper<EmsDetectionTask> tqw = new LambdaQueryWrapper<>();
            tqw.eq(EmsDetectionTask::getSampleId, s.getId()).orderByAsc(EmsDetectionTask::getId);
            for (EmsDetectionTask t : taskMapper.selectList(tqw)) {
                Map<String, Object> m = new HashMap<>();
                m.put("taskId", t.getId());
                m.put("taskNo", t.getTaskNo());
                m.put("barcode", t.getBarcode());
                m.put("sampleName", t.getSampleName());
                m.put("monitorItems", t.getMonitorItems());
                m.put("status", t.getStatus());
                list.add(m);
            }
        }
        return list;
    }

    /** 创建监测报告：draft=true 存草稿，否则直接待复核 */
    @Transactional
    public Long createMonitorReport(Map<String, Object> body, boolean draft) {
        String title = str(body.get("name"));
        String cmaCertNo = str(body.get("cmaCertNo"));
        String reviewer = str(body.get("reviewer"));
        String approver = str(body.get("approver"));
        String generator = str(body.get("generator"));
        Long entrustId = toLong(body.get("entrustId"));
        List<Long> taskIds = toLongList(body.get("taskIds"));
        if (!StringUtils.hasText(title)) throw new IllegalArgumentException("请填写报告名称");
        if (!StringUtils.hasText(cmaCertNo)) throw new IllegalArgumentException("请填写 CMA 资质认定证书号");
        if (!StringUtils.hasText(reviewer)) throw new IllegalArgumentException("请选择复核人");
        if (!StringUtils.hasText(approver)) throw new IllegalArgumentException("请选择批准人");
        if (entrustId == null) throw new IllegalArgumentException("请选择检测委托");
        if (taskIds.isEmpty()) throw new IllegalArgumentException("请至少勾选一个检测任务");
        EmsEntrust entrust = entrustMapper.selectById(entrustId);
        if (entrust == null) throw new IllegalArgumentException("检测委托不存在");

        int itemCount = 0;
        int exceedCount = 0;
        for (Long tid : taskIds) {
            LambdaQueryWrapper<EmsDetectionResult> rqw = new LambdaQueryWrapper<>();
            rqw.eq(EmsDetectionResult::getTaskId, tid);
            for (EmsDetectionResult r : resultMapper.selectList(rqw)) {
                itemCount++;
                if ("超标".equals(r.getConclusion())) exceedCount++;
            }
        }

        EmsReport report = new EmsReport();
        String year = String.valueOf(LocalDateTime.now().getYear());
        QueryWrapper<EmsReport> nqw = new QueryWrapper<>();
        nqw.likeRight("report_no", "HJ-JC-" + year);
        int seq = reportMapper.selectCount(nqw).intValue() + 1;
        report.setReportNo(String.format("HJ-JC-%s-%04d", year, seq));
        report.setTitle(title);
        report.setTplType("委托");
        if (entrust.getCustId() != null) {
            EmsCustomer cust = customerMapper.selectById(entrust.getCustId());
            if (cust != null) report.setClient(cust.getCustName());
        }
        report.setEntrustId(entrustId);
        report.setCmaCertNo(cmaCertNo);
        report.setReviewer(reviewer);
        report.setApprover(approver);
        report.setTaskIds(toString(taskIds));
        report.setItemCount(itemCount);
        report.setExceedCount(exceedCount);
        report.setStatus(draft ? "草稿" : "待审核");
        report.setGenerator(generator);
        LocalDateTime now = LocalDateTime.now();
        report.setCreateTime(now);
        report.setUpdateTime(now);
        reportMapper.insert(report);
        return report.getId();
    }

    /** 草稿提交 → 待审核 */
    @Transactional
    public void submitDraft(Long id) {
        EmsReport report = reportMapper.selectById(id);
        if (report == null) throw new IllegalArgumentException("报告不存在");
        if (!"草稿".equals(report.getStatus())) throw new IllegalArgumentException("仅草稿状态报告可提交");
        report.setStatus("待审核");
        report.setUpdateTime(LocalDateTime.now());
        reportMapper.updateById(report);
    }

    /** 报告页视图模型：抬头信息 + 结果明细行 + 签署区 */
    public Map<String, Object> viewModel(Long id) {
        EmsReport report = reportMapper.selectById(id);
        if (report == null) throw new IllegalArgumentException("报告不存在");
        Map<String, Object> m = new HashMap<>();
        m.put("report", report);

        EmsEntrust entrust = report.getEntrustId() != null ? entrustMapper.selectById(report.getEntrustId()) : null;
        if (entrust != null && entrust.getCustId() != null) {
            EmsCustomer cust = customerMapper.selectById(entrust.getCustId());
            m.put("clientName", cust != null ? cust.getCustName() : report.getClient());
        } else {
            m.put("clientName", report.getClient());
        }

        List<Long> taskIds = parseLongList(report.getTaskIds());
        List<Map<String, Object>> rows = new ArrayList<>();
        String category = null;
        String sampleDate = null;
        String analysisFrom = null;
        String analysisTo = null;
        for (Long tid : taskIds) {
            EmsDetectionTask t = taskMapper.selectById(tid);
            if (t == null) continue;
            String pointLabel = "";
            if (t.getPointId() != null) {
                EmsMonitorPoint p = pointMapper.selectById(t.getPointId());
                if (p != null) {
                    pointLabel = ((p.getPointNo() == null ? "" : p.getPointNo()) + " " + (p.getPointName() == null ? "" : p.getPointName())).trim();
                }
            }
            EmsSample sample = t.getSampleId() != null ? sampleMapper.selectById(t.getSampleId()) : null;
            if (sample != null) {
                if (category == null && StringUtils.hasText(sample.getCategory())) category = sample.getCategory();
                String sd = firstDate(sample.getSampleTime(), sample.getReceiveTime());
                if (sd != null && (sampleDate == null || sd.compareTo(sampleDate) < 0)) sampleDate = sd;
            }
            LambdaQueryWrapper<EmsDetectionResult> rqw = new LambdaQueryWrapper<>();
            rqw.eq(EmsDetectionResult::getTaskId, tid).orderByAsc(EmsDetectionResult::getId);
            for (EmsDetectionResult r : resultMapper.selectList(rqw)) {
                Map<String, Object> row = new HashMap<>();
                row.put("point", pointLabel);
                row.put("item", r.getMonitorItem());
                row.put("value", r.getValue());
                row.put("unit", r.getUnit());
                row.put("limit", r.getLimitValue());
                row.put("conclusion", r.getConclusion());
                rows.add(row);
                if (r.getCreateTime() != null) {
                    String d = r.getCreateTime().toLocalDate().toString();
                    if (analysisFrom == null || d.compareTo(analysisFrom) < 0) analysisFrom = d;
                    if (analysisTo == null || d.compareTo(analysisTo) > 0) analysisTo = d;
                }
            }
        }
        m.put("rows", rows);
        m.put("category", category);
        m.put("sampleDate", sampleDate);
        String analysis = "";
        if (analysisFrom != null && analysisTo != null) {
            analysis = analysisFrom.equals(analysisTo) ? analysisFrom
                    : analysisFrom + " ~ " + (analysisTo.length() > 7 && analysisTo.substring(0, 4).equals(analysisFrom.substring(0, 4))
                    ? analysisTo.substring(5) : analysisTo);
        }
        m.put("analysisDate", analysis);
        return m;
    }

    /* ---------------- 私有工具 ---------------- */

    private String str(Object o) {
        return o == null ? null : String.valueOf(o).trim();
    }

    private Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).longValue();
        try { return Long.valueOf(o.toString().trim()); } catch (Exception e) { return null; }
    }

    private List<Long> toLongList(Object o) {
        List<Long> list = new ArrayList<>();
        if (o instanceof List) {
            for (Object e : (List<?>) o) {
                Long v = toLong(e);
                if (v != null) list.add(v);
            }
        }
        return list;
    }

    private List<Long> parseLongList(String json) {
        List<Long> list = new ArrayList<>();
        if (json == null) return list;
        String s = json.trim();
        if (s.startsWith("[")) s = s.substring(1);
        if (s.endsWith("]")) s = s.substring(0, s.length() - 1);
        for (String part : s.split(",")) {
            String p = part.trim();
            if (!p.isEmpty()) {
                try { list.add(Long.valueOf(p)); } catch (Exception ignored) { }
            }
        }
        return list;
    }

    /** 取两个时间串中首个非空者的日期部分（yyyy-MM-dd） */
    private String firstDate(String a, String b) {
        String s = StringUtils.hasText(a) ? a : (StringUtils.hasText(b) ? b : null);
        if (s == null) return null;
        return s.length() >= 10 ? s.substring(0, 10) : s;
    }
}
