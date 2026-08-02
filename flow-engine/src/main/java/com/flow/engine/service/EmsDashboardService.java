package com.flow.engine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flow.engine.entity.EmsDetectionResult;
import com.flow.engine.entity.EmsDetectionTask;
import com.flow.engine.entity.EmsEntrust;
import com.flow.engine.entity.EmsReport;
import com.flow.engine.mapper.EmsDetectionResultMapper;
import com.flow.engine.mapper.EmsDetectionTaskMapper;
import com.flow.engine.mapper.EmsEntrustMapper;
import com.flow.engine.mapper.EmsReportMapper;
import com.flow.engine.mapper.EmsReportTemplateMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 环境监测 - 监测数据驾驶舱与统计（ISSUE-028）
 * 聚合委托、检测任务/结果、报告等核心业务表的统计指标，供前端驾驶舱展示。
 */
@Service
public class EmsDashboardService {

    private final EmsEntrustMapper entrustMapper;
    private final EmsDetectionTaskMapper taskMapper;
    private final EmsDetectionResultMapper resultMapper;
    private final EmsReportMapper reportMapper;
    private final EmsReportTemplateMapper templateMapper;

    public EmsDashboardService(EmsEntrustMapper entrustMapper,
                                EmsDetectionTaskMapper taskMapper,
                                EmsDetectionResultMapper resultMapper,
                                EmsReportMapper reportMapper,
                                EmsReportTemplateMapper templateMapper) {
        this.entrustMapper = entrustMapper;
        this.taskMapper = taskMapper;
        this.resultMapper = resultMapper;
        this.reportMapper = reportMapper;
        this.templateMapper = templateMapper;
    }

    /** 概览统计。 */
    public Map<String, Object> overview() {
        Map<String, Object> data = new LinkedHashMap<>();

        // 1. 核心 KPI
        data.put("kpis", buildKpis());

        // 2. 委托状态分布
        data.put("entrustStatusDist", statusDist(
                entrustMapper.selectList(new LambdaQueryWrapper<>()),
                EmsEntrust::getStatus));

        // 3. 检测结论分布（达标/超标）
        data.put("conclusionDist", buildConclusionDist());

        // 4. 报告状态分布
        data.put("reportStatusDist", statusDist(
                reportMapper.selectList(new LambdaQueryWrapper<>()),
                EmsReport::getStatus));

        // 5. 近 6 个月报告/超标趋势
        data.put("monthlyTrend", buildMonthlyTrend());

        return data;
    }

    private Map<String, Object> buildKpis() {
        Map<String, Object> k = new LinkedHashMap<>();
        k.put("entrustTotal", entrustMapper.selectCount(new LambdaQueryWrapper<>()));
        k.put("taskTotal", taskMapper.selectCount(new LambdaQueryWrapper<>()));
        k.put("resultTotal", resultMapper.selectCount(new LambdaQueryWrapper<>()));
        k.put("reportTotal", reportMapper.selectCount(new LambdaQueryWrapper<>()));
        k.put("templateTotal", templateMapper.selectCount(new LambdaQueryWrapper<>()));
        // 待办：未复核的检测任务 + 待审核报告
        long pendingReview = taskMapper.selectCount(new LambdaQueryWrapper<EmsDetectionTask>()
                .in(EmsDetectionTask::getStatus, "待录入", "录入中", "已提交"));
        long pendingReport = reportMapper.selectCount(new LambdaQueryWrapper<EmsReport>()
                .eq(EmsReport::getStatus, "待审核"));
        k.put("pendingReview", pendingReview);
        k.put("pendingReport", pendingReport);
        // 累计超标项数
        k.put("exceedTotal", resultMapper.selectCount(new LambdaQueryWrapper<EmsDetectionResult>()
                .eq(EmsDetectionResult::getConclusion, "超标")));
        return k;
    }

    private Map<String, Object> buildConclusionDist() {
        Map<String, Object> dist = new LinkedHashMap<>();
        long reach = resultMapper.selectCount(new LambdaQueryWrapper<EmsDetectionResult>()
                .eq(EmsDetectionResult::getConclusion, "达标"));
        long exceed = resultMapper.selectCount(new LambdaQueryWrapper<EmsDetectionResult>()
                .eq(EmsDetectionResult::getConclusion, "超标"));
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(item("达标", reach));
        list.add(item("超标", exceed));
        dist.put("series", list);
        dist.put("exceedRate", reach + exceed == 0 ? 0 : Math.round(exceed * 1000.0 / (reach + exceed)) / 10.0);
        return dist;
    }

    private List<Map<String, Object>> buildMonthlyTrend() {
        // 近 6 个月（含当月），按报告 create_time 的 yyyy-MM 前缀聚合
        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDate now = LocalDate.now();
        DateTimeFormatter ym = DateTimeFormatter.ofPattern("yyyy-MM");
        for (int i = 5; i >= 0; i--) {
            LocalDate d = now.minusMonths(i);
            String month = d.format(ym);
            long reports = reportMapper.selectCount(new LambdaQueryWrapper<EmsReport>()
                    .likeRight(EmsReport::getCreateTime, month));
            long exceed = resultMapper.selectCount(new LambdaQueryWrapper<EmsDetectionResult>()
                    .eq(EmsDetectionResult::getConclusion, "超标")
                    .likeRight(EmsDetectionResult::getCreateTime, month));
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("month", month);
            m.put("reports", reports);
            m.put("exceed", exceed);
            trend.add(m);
        }
        return trend;
    }

    /** 通用状态分布统计。 */
    private <T> List<Map<String, Object>> statusDist(List<T> list, java.util.function.Function<T, String> statusGetter) {
        Map<String, Long> cnt = new LinkedHashMap<>();
        for (T t : list) {
            String s = statusGetter.apply(t);
            if (s == null || s.isEmpty()) s = "未知";
            cnt.put(s, cnt.getOrDefault(s, 0L) + 1);
        }
        List<Map<String, Object>> res = new ArrayList<>();
        cnt.forEach((k, v) -> res.add(item(k, v)));
        return res;
    }

    private Map<String, Object> item(String name, long value) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("value", value);
        return m;
    }
}
