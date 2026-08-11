package com.flow.engine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.flow.engine.common.BusinessException;
import com.flow.engine.entity.EmsDetectionResult;
import com.flow.engine.entity.EmsDetectionReview;
import com.flow.engine.entity.EmsDetectionTask;
import com.flow.engine.entity.EmsSample;
import com.flow.engine.entity.EmsSamplingOrder;
import com.flow.engine.mapper.EmsDetectionResultMapper;
import com.flow.engine.mapper.EmsDetectionReviewMapper;
import com.flow.engine.mapper.EmsDetectionTaskMapper;
import com.flow.engine.mapper.EmsSampleMapper;
import com.flow.engine.service.EmsSamplingOrderService;
import com.flow.engine.mapper.EmsSampleParamConfigMapper;
import com.flow.engine.util.CodeGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 环境监测 - 检测数据录入与复核（TRD 5.4 / ISSUE-025）
 */
@Service
public class EmsDetectionService extends ServiceImpl<EmsDetectionTaskMapper, EmsDetectionTask> {

    @Autowired
    private EmsDetectionTaskMapper taskMapper;
    @Autowired
    private EmsDetectionResultMapper resultMapper;
    @Autowired
    private EmsDetectionReviewMapper reviewMapper;
    @Autowired
    private EmsSampleMapper sampleMapper;
    @Autowired
    private EmsSampleParamConfigMapper configMapper;
    @Autowired
    private EmsSamplingOrderService samplingOrderService;

    /** 为已收样样品创建检测任务（幂等：已存在则跳过）。 */
    public EmsDetectionTask createTask(Long sampleId, String monitorItems, String entryBy, String reviewBy) {
        LambdaQueryWrapper<EmsDetectionTask> q = new LambdaQueryWrapper<>();
        q.eq(EmsDetectionTask::getSampleId, sampleId);
        if (this.count(q) > 0) {
            return this.getOne(q);
        }
        EmsSample sample = sampleMapper.selectById(sampleId);
        if (sample == null) throw new BusinessException("样品不存在：" + sampleId);
        // 已收样 与 留样中 状态样品均可创建检测任务
        if (!Arrays.asList("已收样", "留样中").contains(sample.getStatus())) {
            throw new BusinessException("仅已收样/留样中样品可创建检测任务，当前状态：" + sample.getStatus());
        }
        int seq = (int) (this.count() + 1);
        EmsDetectionTask task = new EmsDetectionTask();
        task.setTaskNo(CodeGenerator.generate("DT", seq));
        task.setSampleId(sampleId);
        task.setBarcode(sample.getBarcode());
        task.setSampleName(sample.getName());
        task.setPointId(sample.getPointId());
        task.setMonitorItems(StringUtils.hasText(monitorItems) ? monitorItems : "pH,COD,氨氮");
        task.setEntryBy(entryBy);
        task.setReviewBy(reviewBy);
        task.setEntryTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        task.setStatus("录入中");
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        taskMapper.insert(task);
        // 检测任务创建完成，关联样品状态变更为「实验室监测中」
        if (sample != null) {
            updateSampleStatus(sampleId, "实验室监测中");
            // 同步关联采样任务状态为「实验室监测中」
            if (sample.getOrderId() != null) {
                updateOrderStatus(sample.getOrderId(), "实验室监测中");
            }
        }
        return task;
    }

    /** 联动更新关联样品状态。 */
    private void updateSampleStatus(Long sampleId, String status) {
        EmsSample sample = sampleMapper.selectById(sampleId);
        if (sample == null || status.equals(sample.getStatus())) return;
        com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<EmsSample> uw
                = new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
        uw.eq(EmsSample::getId, sampleId).set(EmsSample::getStatus, status);
        sampleMapper.update(null, uw);
    }

    /** 联动更新关联采样任务状态。 */
    private void updateOrderStatus(Long orderId, String status) {
        EmsSamplingOrder order = samplingOrderService.getById(orderId);
        if (order == null || status.equals(order.getStatus())) return;
        com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<EmsSamplingOrder> uw
                = new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
        uw.eq(EmsSamplingOrder::getId, orderId).set(EmsSamplingOrder::getStatus, status);
        samplingOrderService.update(null, uw);
    }

    /** 保存/更新检测结果明细（先删除旧结果再批量写入），并记录环境条件与综合结论。 */
    public EmsDetectionTask saveResults(Long taskId, List<EmsDetectionResult> results, String entryBy,
                                        String envTemp, String envHumidity, String conclusion, String remark, String attachments) {
        EmsDetectionTask task = getTask(taskId);
        if (!Arrays.asList("录入中", "已退回").contains(task.getStatus())) {
            throw new BusinessException("当前状态不可编辑：" + task.getStatus());
        }
        // 更新任务级信息（检测责任人、环境、综合结论、整体备注、附件）
        task.setEntryBy(entryBy);
        task.setEnvTemp(envTemp);
        task.setEnvHumidity(envHumidity);
        if (StringUtils.hasText(attachments)) task.setAttachments(attachments);
        if (StringUtils.hasText(remark)) task.setRemark(remark);
        if (StringUtils.hasText(conclusion)) task.setConclusion(conclusion);
        LambdaQueryWrapper<EmsDetectionResult> q = new LambdaQueryWrapper<>();
        q.eq(EmsDetectionResult::getTaskId, taskId);
        resultMapper.delete(q);
        LocalDateTime now = LocalDateTime.now();
        for (EmsDetectionResult r : results) {
            r.setTaskId(taskId);
            r.setSampleId(task.getSampleId());
            if (!StringUtils.hasText(r.getConclusion())) {
                r.setConclusion(judgeConclusion(r.getValue(), r.getLimitValue()));
            }
            r.setCreateTime(now);
            r.setUpdateTime(now);
            resultMapper.insert(r);
        }
        task.setEntryTime(now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        task.setStatus("录入中");
        task.setUpdateTime(now);
        taskMapper.updateById(task);
        // 仅保存录入数据，样品仍处于「实验室监测中」，待提交复核后再变更状态
        return task;
    }

    /** 提交复核。 */
    public EmsDetectionTask submit(Long taskId) {
        EmsDetectionTask task = getTask(taskId);
        if (!Arrays.asList("录入中", "已退回").contains(task.getStatus())) {
            throw new BusinessException("仅录入中/已退回的任务可提交，当前状态：" + task.getStatus());
        }
        LambdaQueryWrapper<EmsDetectionResult> q = new LambdaQueryWrapper<>();
        q.eq(EmsDetectionResult::getTaskId, taskId);
        if (resultMapper.selectCount(q) == 0) {
            throw new BusinessException("请先录入至少一条检测结果");
        }
        task.setStatus("已提交");
        task.setUpdateTime(LocalDateTime.now());
        taskMapper.updateById(task);
        // 提交检测数据后，关联样品状态变更为「检测数据复核中」
        if (task.getSampleId() != null) {
            updateSampleStatus(task.getSampleId(), "检测数据复核中");
        }
        return task;
    }

    /** 复核通过。 */
    public EmsDetectionReview approve(Long taskId, String reviewer, String opinion) {
        EmsDetectionTask task = getTask(taskId);
        if (!"已提交".equals(task.getStatus())) {
            throw new BusinessException("仅已提交的任务可复核，当前状态：" + task.getStatus());
        }
        task.setStatus("已复核");
        task.setReviewBy(reviewer);
        task.setReviewTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        task.setReviewOpinion(opinion);
        task.setUpdateTime(LocalDateTime.now());
        taskMapper.updateById(task);
        // 复核通过，关联样品状态变更为「已完成」
        if (task.getSampleId() != null) {
            updateSampleStatus(task.getSampleId(), "已完成");
        }
        return saveReview(task, reviewer, "通过", opinion);
    }

    /** 复核退回。 */
    public EmsDetectionReview reject(Long taskId, String reviewer, String opinion) {
        EmsDetectionTask task = getTask(taskId);
        if (!"已提交".equals(task.getStatus())) {
            throw new BusinessException("仅已提交的任务可复核，当前状态：" + task.getStatus());
        }
        if (!StringUtils.hasText(opinion)) {
            throw new BusinessException("退回必须填写复核意见");
        }
        task.setStatus("已退回");
        task.setReviewBy(reviewer);
        task.setReviewTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        task.setReviewOpinion(opinion);
        task.setUpdateTime(LocalDateTime.now());
        taskMapper.updateById(task);
        // 复核不通过（退回），关联样品状态变更为「检测异常」
        if (task.getSampleId() != null) {
            updateSampleStatus(task.getSampleId(), "检测异常");
        }
        return saveReview(task, reviewer, "退回", opinion);
    }

    // ===================== 查询 =====================

    public Page<EmsDetectionTask> pageTasks(String status, String keyword, String entryBy, int page, int size) {
        LambdaQueryWrapper<EmsDetectionTask> q = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) q.eq(EmsDetectionTask::getStatus, status);
        if (StringUtils.hasText(entryBy)) q.eq(EmsDetectionTask::getEntryBy, entryBy);
        if (StringUtils.hasText(keyword)) {
            q.like(EmsDetectionTask::getBarcode, keyword)
             .or().like(EmsDetectionTask::getSampleName, keyword)
             .or().like(EmsDetectionTask::getTaskNo, keyword);
        }
        q.orderByDesc(EmsDetectionTask::getCreateTime);
        return this.page(new Page<>(page, size), q);
    }

    public Page<EmsDetectionTask> pagePendingReview(int page, int size) {
        LambdaQueryWrapper<EmsDetectionTask> q = new LambdaQueryWrapper<>();
        q.eq(EmsDetectionTask::getStatus, "已提交");
        q.orderByAsc(EmsDetectionTask::getCreateTime);
        return this.page(new Page<>(page, size), q);
    }

    public Map<String, Object> taskDetail(Long taskId) {
        EmsDetectionTask task = getTask(taskId);
        LambdaQueryWrapper<EmsDetectionResult> rq = new LambdaQueryWrapper<>();
        rq.eq(EmsDetectionResult::getTaskId, taskId);
        List<EmsDetectionResult> results = resultMapper.selectList(rq);
        LambdaQueryWrapper<EmsDetectionReview> vq = new LambdaQueryWrapper<>();
        vq.eq(EmsDetectionReview::getTaskId, taskId);
        vq.orderByAsc(EmsDetectionReview::getCreateTime);
        List<EmsDetectionReview> reviews = reviewMapper.selectList(vq);
        // 操作记录：基于任务/复核数据推导关键操作轨迹
        List<Map<String, Object>> operations = new java.util.ArrayList<>();
        if (task.getCreateTime() != null) {
            Map<String, Object> op = new LinkedHashMap<>();
            op.put("action", "创建检测任务");
            op.put("operator", task.getEntryBy());
            op.put("time", task.getCreateTime());
            op.put("detail", "任务号 " + task.getTaskNo());
            operations.add(op);
        }
        if (StringUtils.hasText(task.getEntryTime())) {
            Map<String, Object> op = new LinkedHashMap<>();
            op.put("action", "录入监测数据");
            op.put("operator", task.getEntryBy());
            op.put("time", task.getEntryTime());
            op.put("detail", "录入员 " + task.getEntryBy());
            operations.add(op);
        }
        for (EmsDetectionReview r : reviews) {
            Map<String, Object> op = new LinkedHashMap<>();
            op.put("action", "复核-" + r.getDecision());
            op.put("operator", r.getReviewer());
            op.put("time", r.getCreateTime());
            op.put("detail", r.getOpinion());
            operations.add(op);
        }
        // 关联样品基础信息
        EmsSample sample = task.getSampleId() != null ? sampleMapper.selectById(task.getSampleId()) : null;
        // 为每个检测结果明细补充内控限值（来自配置表：type=样品类别, item=监测项目）
        String category = sample != null ? sample.getCategory() : null;
        if (StringUtils.hasText(category)) {
            for (EmsDetectionResult r : results) {
                LambdaQueryWrapper<com.flow.engine.entity.EmsSampleParamConfig> cq = new LambdaQueryWrapper<>();
                cq.eq(com.flow.engine.entity.EmsSampleParamConfig::getType, category)
                  .eq(com.flow.engine.entity.EmsSampleParamConfig::getItem, r.getMonitorItem());
                com.flow.engine.entity.EmsSampleParamConfig cfg = configMapper.selectOne(cq);
                if (cfg != null) r.setInnerLimit(cfg.getInnerLimit());
            }
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("task", task);
        map.put("sample", sample);
        map.put("results", results);
        map.put("reviews", reviews);
        map.put("operations", operations);
        return map;
    }

    public List<EmsSample> pendingSamples() {
        // 已收样 与 留样中 状态、且尚未创建检测任务的样品
        LambdaQueryWrapper<EmsSample> q = new LambdaQueryWrapper<>();
        q.in(EmsSample::getStatus, "已收样", "留样中");
        // 排除已经存在检测任务的样品（一个样品仅一个检测任务）
        q.notInSql(EmsSample::getId, "SELECT sample_id FROM t_detection_task WHERE sample_id IS NOT NULL");
        q.orderByDesc(EmsSample::getCreateTime);
        return sampleMapper.selectList(q);
    }

    public Map<String, Object> taskStat() {
        Map<String, Object> stat = new LinkedHashMap<>();
        for (String s : new String[]{ "录入中", "已提交", "已复核", "已退回" }) {
            LambdaQueryWrapper<EmsDetectionTask> q = new LambdaQueryWrapper<>();
            q.eq(EmsDetectionTask::getStatus, s);
            stat.put(s, taskMapper.selectCount(q));
        }
        LambdaQueryWrapper<EmsDetectionTask> all = new LambdaQueryWrapper<>();
        stat.put("total", taskMapper.selectCount(all));
        return stat;
    }

    // ===================== 内部 =====================

    private EmsDetectionTask getTask(Long taskId) {
        EmsDetectionTask task = taskMapper.selectById(taskId);
        if (task == null) throw new BusinessException("检测任务不存在：" + taskId);
        return task;
    }

    private EmsDetectionReview saveReview(EmsDetectionTask task, String reviewer, String decision, String opinion) {
        EmsDetectionReview review = new EmsDetectionReview();
        review.setTaskId(task.getId());
        review.setSampleId(task.getSampleId());
        review.setBarcode(task.getBarcode());
        review.setReviewer(reviewer);
        review.setDecision(decision);
        review.setOpinion(opinion);
        review.setCreateTime(LocalDateTime.now());
        reviewMapper.insert(review);
        return review;
    }

    /** 依据检测值与限值粗略判定达标/超标（数值可比较时）。 */
    private String judgeConclusion(String value, String limit) {
        if (!StringUtils.hasText(value) || !StringUtils.hasText(limit)) return "未判定";
        try {
            double v = Double.parseDouble(value.trim());
            double l = Double.parseDouble(limit.trim());
            return v <= l ? "达标" : "超标";
        } catch (NumberFormatException e) {
            return "未判定";
        }
    }
}
