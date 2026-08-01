package com.flow.engine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.flow.engine.common.BusinessException;
import com.flow.engine.entity.EmsDetectionResult;
import com.flow.engine.entity.EmsDetectionReview;
import com.flow.engine.entity.EmsDetectionTask;
import com.flow.engine.entity.EmsSample;
import com.flow.engine.mapper.EmsDetectionResultMapper;
import com.flow.engine.mapper.EmsDetectionReviewMapper;
import com.flow.engine.mapper.EmsDetectionTaskMapper;
import com.flow.engine.mapper.EmsSampleMapper;
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

    /** 为已收样样品创建检测任务（幂等：已存在则跳过）。 */
    public EmsDetectionTask createTask(Long sampleId, String monitorItems, String entryBy) {
        LambdaQueryWrapper<EmsDetectionTask> q = new LambdaQueryWrapper<>();
        q.eq(EmsDetectionTask::getSampleId, sampleId);
        if (this.count(q) > 0) {
            return this.getOne(q);
        }
        EmsSample sample = sampleMapper.selectById(sampleId);
        if (sample == null) throw new BusinessException("样品不存在：" + sampleId);
        if (!"已收样".equals(sample.getStatus())) {
            throw new BusinessException("仅已收样样品可创建检测任务，当前状态：" + sample.getStatus());
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
        task.setEntryTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        task.setStatus("录入中");
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        taskMapper.insert(task);
        return task;
    }

    /** 保存/更新检测结果明细（先删除旧结果再批量写入）。 */
    public EmsDetectionTask saveResults(Long taskId, List<EmsDetectionResult> results, String entryBy) {
        EmsDetectionTask task = getTask(taskId);
        if (!Arrays.asList("录入中", "已退回").contains(task.getStatus())) {
            throw new BusinessException("当前状态不可编辑：" + task.getStatus());
        }
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
        task.setEntryBy(entryBy);
        task.setEntryTime(now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        task.setStatus("录入中");
        task.setUpdateTime(now);
        taskMapper.updateById(task);
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
        return saveReview(task, reviewer, "退回", opinion);
    }

    // ===================== 查询 =====================

    public Page<EmsDetectionTask> pageTasks(String status, String keyword, int page, int size) {
        LambdaQueryWrapper<EmsDetectionTask> q = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) q.eq(EmsDetectionTask::getStatus, status);
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
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("task", task);
        map.put("results", results);
        map.put("reviews", reviews);
        return map;
    }

    public List<EmsSample> pendingSamples() {
        LambdaQueryWrapper<EmsSample> q = new LambdaQueryWrapper<>();
        q.eq(EmsSample::getStatus, "已收样");
        return sampleMapper.selectList(q);
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
