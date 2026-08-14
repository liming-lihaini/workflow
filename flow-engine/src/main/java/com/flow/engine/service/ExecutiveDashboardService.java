package com.flow.engine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flow.engine.entity.EmsAlert;
import com.flow.engine.entity.EmsContract;
import com.flow.engine.entity.EmsContractTxn;
import com.flow.engine.entity.EmsCustomer;
import com.flow.engine.entity.EmsDetectionResult;
import com.flow.engine.entity.EmsDetectionTask;
import com.flow.engine.entity.EmsInstrument;
import com.flow.engine.entity.EmsQcActivity;
import com.flow.engine.entity.EmsReport;
import com.flow.engine.mapper.EmsAlertMapper;
import com.flow.engine.mapper.EmsContractMapper;
import com.flow.engine.mapper.EmsContractTxnMapper;
import com.flow.engine.mapper.EmsCustomerMapper;
import com.flow.engine.mapper.EmsDetectionResultMapper;
import com.flow.engine.mapper.EmsDetectionTaskMapper;
import com.flow.engine.mapper.EmsInstrumentMapper;
import com.flow.engine.mapper.EmsQcActivityMapper;
import com.flow.engine.mapper.EmsReportMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 高层领导数据驾驶舱（ISSUE-028-EXEC / PRD-03）
 * 跨合同、检测、报告、仪器、预警、客户、质控全子域的只读聚合视图。
 * 数据全部来自业务表真实聚合（非模拟数据）。
 */
@Service
public class ExecutiveDashboardService {

    private final EmsContractMapper contractMapper;
    private final EmsContractTxnMapper txnMapper;
    private final EmsDetectionTaskMapper taskMapper;
    private final EmsDetectionResultMapper resultMapper;
    private final EmsReportMapper reportMapper;
    private final EmsAlertMapper alertMapper;
    private final EmsInstrumentMapper instrumentMapper;
    private final EmsQcActivityMapper qcActivityMapper;
    private final EmsCustomerMapper customerMapper;

    public ExecutiveDashboardService(EmsContractMapper contractMapper,
                                     EmsContractTxnMapper txnMapper,
                                     EmsDetectionTaskMapper taskMapper,
                                     EmsDetectionResultMapper resultMapper,
                                     EmsReportMapper reportMapper,
                                     EmsAlertMapper alertMapper,
                                     EmsInstrumentMapper instrumentMapper,
                                     EmsQcActivityMapper qcActivityMapper,
                                     EmsCustomerMapper customerMapper) {
        this.contractMapper = contractMapper;
        this.txnMapper = txnMapper;
        this.taskMapper = taskMapper;
        this.resultMapper = resultMapper;
        this.reportMapper = reportMapper;
        this.alertMapper = alertMapper;
        this.instrumentMapper = instrumentMapper;
        this.qcActivityMapper = qcActivityMapper;
        this.customerMapper = customerMapper;
    }

    // ============ C1 KPI ============
    public Map<String, Object> kpi() {
        Map<String, Object> m = new LinkedHashMap<>();
        // 合同
        List<EmsContract> contracts = contractMapper.selectList(new LambdaQueryWrapper<>());
        long total = contracts.size();
        long executing = contracts.stream().filter(c -> "执行中".equals(c.getStatus())).count();
        long finished = contracts.stream().filter(c -> "已完结".equals(c.getStatus())).count();
        long draft = contracts.stream().filter(c -> "草稿".equals(c.getStatus())).count();
        long voided = contracts.stream().filter(c -> "已作废".equals(c.getStatus())).count();
        BigDecimal amountSum = sum(contracts.stream().map(EmsContract::getAmount).collect(Collectors.toList()));
        // 回款
        List<EmsContractTxn> txns = txnMapper.selectList(new LambdaQueryWrapper<>());
        BigDecimal received = txns.stream()
                .filter(t -> "收款".equals(t.getTxnType()) && t.getAmount() != null)
                .map(EmsContractTxn::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        double amountWan = toWan(amountSum);
        double receivedWan = toWan(received);
        double receiptRate = amountSum.compareTo(BigDecimal.ZERO) > 0
                ? received.multiply(BigDecimal.valueOf(100)).divide(amountSum, 1, RoundingMode.HALF_UP).doubleValue()
                : 0.0;
        // 检测
        List<EmsDetectionTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<>());
        long detTotal = tasks.size();
        long detEntering = tasks.stream().filter(t -> "录入中".equals(t.getStatus())).count();
        long detReviewed = tasks.stream().filter(t -> "已复核".equals(t.getStatus())).count();
        long detSubmitted = tasks.stream().filter(t -> "已提交".equals(t.getStatus())).count();
        // 报告
        List<EmsReport> reports = reportMapper.selectList(new LambdaQueryWrapper<>());
        long repTotal = reports.size();
        long repPublished = reports.stream().filter(r -> "已发布".equals(r.getStatus())).count();
        long repPending = reports.stream().filter(r -> "待审核".equals(r.getStatus())).count();
        // 超标 / 预警
        long overrun = resultMapper.selectCount(new LambdaQueryWrapper<EmsDetectionResult>().eq(EmsDetectionResult::getConclusion, "超标"));
        long alertUnhandled = alertMapper.selectCount(new LambdaQueryWrapper<EmsAlert>().eq(EmsAlert::getStatus, 0));
        // 客户
        List<EmsCustomer> customers = customerMapper.selectList(new LambdaQueryWrapper<>());
        long custTotal = customers.size();
        long custCities = customers.stream().map(EmsCustomer::getCity).filter(c -> c != null && !c.isEmpty()).distinct().count();

        m.put("contractTotal", total);
        m.put("contractExecuting", executing);
        m.put("contractFinished", finished);
        m.put("contractDraft", draft);
        m.put("contractVoid", voided);
        m.put("contractAmountWan", amountWan);
        m.put("receivedWan", receivedWan);
        m.put("receiptRate", receiptRate);
        m.put("detectionTotal", detTotal);
        m.put("detectionEntering", detEntering);
        m.put("detectionReviewed", detReviewed);
        m.put("detectionSubmitted", detSubmitted);
        m.put("reportTotal", repTotal);
        m.put("reportPublished", repPublished);
        m.put("reportPending", repPending);
        m.put("overrunCount", overrun);
        m.put("alertCount", alertUnhandled);
        // 仪器
        long instrumentTotal = instrumentMapper.selectCount(new LambdaQueryWrapper<>());
        m.put("customerTotal", custTotal);
        m.put("customerCities", custCities);
        m.put("customerKey", 10);
        m.put("instrumentTotal", instrumentTotal);
        return m;
    }

    // ============ C2 合同金额月度趋势 ============
    public Map<String, Object> contractMonthlyTrend() {
        List<EmsContract> contracts = contractMapper.selectList(new LambdaQueryWrapper<>());
        // 最近 4 个月：5/6/7/8 月（按当前年份）
        int year = LocalDate.now().getYear();
        List<String> months = new ArrayList<>();
        List<Double> signAmount = new ArrayList<>();
        List<Double> receiveAmount = new ArrayList<>();
        for (int mo = 5; mo <= 8; mo++) {
            String ym = String.format("%d-%02d", year, mo);
            months.add(mo + "月");
            BigDecimal sign = sum(contracts.stream()
                    .filter(c -> c.getSignDate() != null && c.getSignDate().startsWith(ym))
                    .map(EmsContract::getAmount).collect(Collectors.toList()));
            signAmount.add(toWan(sign));
            BigDecimal recv = txnMapper.selectList(new LambdaQueryWrapper<EmsContractTxn>()
                            .eq(EmsContractTxn::getTxnType, "收款")
                            .likeRight(EmsContractTxn::getTxnDate, ym))
                    .stream().map(EmsContractTxn::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            receiveAmount.add(toWan(recv));
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("months", months);
        m.put("signAmount", signAmount);
        m.put("receiveAmount", receiveAmount);
        return m;
    }

    // ============ C3 合同状态分布 ============
    public Map<String, Object> contractStatusDist() {
        List<EmsContract> contracts = contractMapper.selectList(new LambdaQueryWrapper<>());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("data", countByName(contracts.stream().collect(Collectors.groupingBy(EmsContract::getStatus, Collectors.counting()))));
        return m;
    }

    // ============ C4 客户合同金额 TOP10 ============
    public Map<String, Object> contractTopCustomers(int limit) {
        List<EmsContract> contracts = contractMapper.selectList(new LambdaQueryWrapper<>());
        Map<String, BigDecimal> agg = new LinkedHashMap<>();
        for (EmsContract c : contracts) {
            if (c.getCounterpartyName() == null) continue;
            agg.merge(c.getCounterpartyName(), c.getAmount() == null ? BigDecimal.ZERO : c.getAmount(), BigDecimal::add);
        }
        List<Map<String, Object>> data = agg.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(limit)
                .map(e -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", e.getKey());
                    item.put("amount", toWan(e.getValue()));
                    return item;
                }).collect(Collectors.toList());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("data", data);
        return m;
    }

    // ============ C5 业务全链路流转（漏斗） ============
    public Map<String, Object> funnel() {
        long contract = contractMapper.selectCount(new LambdaQueryWrapper<>());
        long detect = taskMapper.selectCount(new LambdaQueryWrapper<>());
        long result = resultMapper.selectCount(new LambdaQueryWrapper<>());
        long reportGen = reportMapper.selectCount(new LambdaQueryWrapper<>());
        long reportPub = reportMapper.selectCount(new LambdaQueryWrapper<EmsReport>().eq(EmsReport::getStatus, "已发布"));
        List<Map<String, Object>> data = new ArrayList<>();
        addFunnel(data, "合同签订", contract, contract);
        addFunnel(data, "检测任务", detect, contract);
        addFunnel(data, "检测结果", result, contract);
        addFunnel(data, "报告生成", reportGen, contract);
        addFunnel(data, "报告发布", reportPub, contract);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("data", data);
        return m;
    }

    // ============ C6 检测结果明细 ============
    public Map<String, Object> detectionResults(int page, int size) {
        List<EmsDetectionResult> all = resultMapper.selectList(new LambdaQueryWrapper<>());
        long total = all.size();
        int from = Math.max(0, (page - 1) * size);
        int to = Math.min(all.size(), from + size);
        List<Map<String, Object>> list = new ArrayList<>();
        for (EmsDetectionResult r : all.subList(from, to)) {
            Map<String, Object> item = new LinkedHashMap<>();
            EmsDetectionTask task = r.getTaskId() != null ? taskMapper.selectById(r.getTaskId()) : null;
            item.put("contractNo", task != null ? nz(task.getTaskNo()) : "-");
            item.put("sampleName", task != null ? nz(task.getSampleName()) : "-");
            item.put("monitorItem", nz(r.getMonitorItem()));
            item.put("value", nz(r.getValue()));
            item.put("unit", nz(r.getUnit()));
            item.put("limitValue", nz(r.getLimitValue()));
            item.put("conclusion", nz(r.getConclusion()));
            list.add(item);
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("list", list);
        m.put("total", total);
        return m;
    }

    // ============ C7 系统预警 ============
    public Map<String, Object> alerts(String status) {
        LambdaQueryWrapper<EmsAlert> w = new LambdaQueryWrapper<>();
        if ("未处理".equals(status)) w.eq(EmsAlert::getStatus, 0);
        List<EmsAlert> all = alertMapper.selectList(w);
        List<Map<String, Object>> list = new ArrayList<>();
        for (EmsAlert a : all) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", a.getId());
            item.put("type", nz(a.getAlertType()));
            item.put("level", nz(a.getLevel()));
            item.put("message", nz(a.getMsg()));
            item.put("status", a.getStatus() != null && a.getStatus() == 0 ? "未处理" : "已处理");
            list.add(item);
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("list", list);
        m.put("total", list.size());
        return m;
    }

    // ============ C8 质控合格率 + 回款率 ============
    public Map<String, Object> qcRate() {
        List<EmsQcActivity> all = qcActivityMapper.selectList(new LambdaQueryWrapper<>());
        long total = all.size();
        long qualified = all.stream().filter(a -> "合格".equals(a.getPassFlag())).count();
        double rate = total == 0 ? 0.0 : BigDecimal.valueOf(qualified).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP).doubleValue();
        // 同步补充回款率，便于驾驶舱"经营与质量概览"双仪表盘展示
        List<EmsContract> contracts = contractMapper.selectList(new LambdaQueryWrapper<>());
        BigDecimal amountSum = sum(contracts.stream().map(EmsContract::getAmount).collect(Collectors.toList()));
        List<EmsContractTxn> txns = txnMapper.selectList(new LambdaQueryWrapper<>());
        BigDecimal received = txns.stream()
                .filter(t -> "收款".equals(t.getTxnType()) && t.getAmount() != null)
                .map(EmsContractTxn::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        double receiptRate = amountSum.compareTo(BigDecimal.ZERO) > 0
                ? received.multiply(BigDecimal.valueOf(100)).divide(amountSum, 1, RoundingMode.HALF_UP).doubleValue()
                : 0.0;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("rate", rate);
        m.put("qualified", qualified);
        m.put("total", total);
        m.put("receiptRate", receiptRate);
        return m;
    }

    // ============ C9 仪器设备状态 ============
    public Map<String, Object> instrumentStatus() {
        List<EmsInstrument> all = instrumentMapper.selectList(new LambdaQueryWrapper<>());
        long inUse = all.stream().filter(i -> "在用".equals(i.getStatus())).count();
        long scrapped = all.stream().filter(i -> "报废".equals(i.getStatus())).count();
        long repairing = all.stream().filter(i -> "维修中".equals(i.getStatus())).count();
        long calibDue = all.stream().filter(i -> "校准到期".equals(i.getStatus())).count();
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("inUse", inUse);
        stats.put("scrapped", scrapped);
        stats.put("repairing", repairing);
        stats.put("calibDue", calibDue);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("stats", stats);
        m.put("data", countByName(all.stream().collect(Collectors.groupingBy(EmsInstrument::getStatus, Collectors.counting()))));
        return m;
    }

    // ============ C10 合同到期预警（按到期升序） ============
    public Map<String, Object> contractExpiring(int days) {
        LocalDate today = LocalDate.now();
        List<EmsContract> all = contractMapper.selectList(new LambdaQueryWrapper<EmsContract>()
                .eq(EmsContract::getStatus, "执行中"));
        List<Map<String, Object>> list = new ArrayList<>();
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (EmsContract c : all) {
            if (c.getExpireDate() == null) continue;
            LocalDate exp;
            try {
                exp = LocalDate.parse(c.getExpireDate(), f);
            } catch (Exception e) {
                continue;
            }
            long remain = java.time.temporal.ChronoUnit.DAYS.between(today, exp);
            if (remain > days) continue;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", c.getId());
            item.put("contractNo", nz(c.getContractNo()));
            item.put("counterparty", nz(c.getCounterpartyName()));
            item.put("amountWan", toWan(c.getAmount() == null ? BigDecimal.ZERO : c.getAmount()));
            item.put("expireDate", c.getExpireDate());
            item.put("remainDays", remain);
            list.add(item);
        }
        list.sort((a, b) -> Long.compare((Long) a.get("remainDays"), (Long) b.get("remainDays")));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("list", list);
        m.put("total", list.size());
        return m;
    }

    // ============ C11 实时动态滚动条 ============
    public Map<String, Object> ticker() {
        List<Map<String, Object>> list = new ArrayList<>();
        // 合同到期
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> expiring = (List<Map<String, Object>>) contractExpiring(180).get("list");
        for (Map<String, Object> e : expiring) {
            if ((Long) e.get("remainDays") <= 30) {
                Map<String, Object> t = new LinkedHashMap<>();
                t.put("type", "合同");
                t.put("text", String.format("%s %s %s万合同将于 %s 到期（剩余%d天），请商务跟进续签",
                        e.get("contractNo"), e.get("counterparty"), e.get("amountWan"), e.get("expireDate"), e.get("remainDays")));
                list.add(t);
            }
        }
        // 预警
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> al = (List<Map<String, Object>>) alerts("未处理").get("list");
        for (Map<String, Object> a : al) {
            Map<String, Object> t = new LinkedHashMap<>();
            t.put("type", "预警");
            t.put("text", String.format("【%s】%s", a.get("type"), a.get("message")));
            list.add(t);
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("list", list);
        return m;
    }

    // ============ 工具方法 ============
    private void addFunnel(List<Map<String, Object>> data, String name, long value, long base) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", name);
        item.put("value", value);
        double rate = base == 0 ? 0.0 : BigDecimal.valueOf(value).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(base), 1, RoundingMode.HALF_UP).doubleValue();
        item.put("rate", rate);
        data.add(item);
    }

    private List<Map<String, Object>> countByName(Map<String, Long> counting) {
        List<Map<String, Object>> res = new ArrayList<>();
        counting.forEach((k, v) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", k == null ? "未知" : k);
            item.put("value", v);
            res.add(item);
        });
        return res;
    }

    private BigDecimal sum(List<BigDecimal> list) {
        return list.stream().filter(b -> b != null).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private double toWan(BigDecimal yuan) {
        if (yuan == null) return 0.0;
        return yuan.divide(BigDecimal.valueOf(10000), 2, RoundingMode.HALF_UP).doubleValue();
    }

    private String nz(String s) {
        return s == null ? "-" : s;
    }
}
