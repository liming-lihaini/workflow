package com.flow.engine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.flow.engine.common.BusinessException;
import com.flow.engine.entity.*;
import com.flow.engine.mapper.*;
import com.flow.engine.util.CodeGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 环境监测 - 质量控制（TRD 5.5 / 5.12 / ISSUE-026）
 * 涵盖：标准物质/耗材效期、危化品审批、质控计划与监控活动、能力验证/实验室间比对/重复性试验，以及 025/026 闸门校验。
 */
@Service
public class EmsQualityService extends ServiceImpl<EmsQcPlanMapper, EmsQcPlan> {

    @Autowired private EmsStandardMaterialMapper materialMapper;
    @Autowired private EmsConsumableMapper consumableMapper;
    @Autowired private EmsHazardousLedgerMapper hazardousMapper;
    @Autowired private EmsQcActivityMapper activityMapper;
    @Autowired private EmsProficiencyTestMapper proficiencyMapper;
    @Autowired private EmsInterlabCompareMapper interlabMapper;
    @Autowired private EmsRepeatTestMapper repeatMapper;
    @Autowired private EmsInstrumentMapper instrumentMapper;

    private static final int EXPIRE_WARN_DAYS = 30; // 临期阈值

    // ===================== 标准物质 =====================
    public EmsStandardMaterial saveMaterial(EmsStandardMaterial m) {
        m.setStatus(judgeExpiry(m.getExpireDate()));
        if (m.getId() == null) { m.setCreateTime(LocalDate.now()); }
        m.setUpdateTime(LocalDate.now());
        if (m.getId() == null) materialMapper.insert(m); else materialMapper.updateById(m);
        return m;
    }
    public Page<EmsStandardMaterial> pageMaterials(String keyword, String status, int page, int size) {
        LambdaQueryWrapper<EmsStandardMaterial> q = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) q.like(EmsStandardMaterial::getName, keyword);
        if (StringUtils.hasText(status)) q.eq(EmsStandardMaterial::getStatus, status);
        q.orderByDesc(EmsStandardMaterial::getCreateTime);
        return materialMapper.selectPage(new Page<>(page, size), q);
    }

    // ===================== 耗材 =====================
    public EmsConsumable saveConsumable(EmsConsumable c) {
        c.setStatus(judgeExpiry(c.getExpireDate()));
        if (c.getId() == null) c.setCreateTime(LocalDate.now());
        c.setUpdateTime(LocalDate.now());
        if (c.getId() == null) consumableMapper.insert(c); else consumableMapper.updateById(c);
        return c;
    }
    public Page<EmsConsumable> pageConsumables(String keyword, String status, int page, int size) {
        LambdaQueryWrapper<EmsConsumable> q = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) q.like(EmsConsumable::getName, keyword);
        if (StringUtils.hasText(status)) q.eq(EmsConsumable::getStatus, status);
        q.orderByDesc(EmsConsumable::getCreateTime);
        return consumableMapper.selectPage(new Page<>(page, size), q);
    }

    // ===================== 危化品台账（审批状态机） =====================
    /** 申请领用/报废：在库 → 待审批 */
    public EmsHazardousLedger apply(Long id, String applyBy, String reason, String targetStatus) {
        EmsHazardousLedger h = getHazardous(id);
        if (!"在库".equals(h.getStatus())) throw new BusinessException("仅【在库】危化品可申请，当前：" + h.getStatus());
        if (!Arrays.asList("已领用", "已报废").contains(targetStatus)) throw new BusinessException("目标状态非法：" + targetStatus);
        h.setApplyBy(applyBy);
        h.setApplyReason(reason);
        h.setApplyTime(LocalDate.now());
        h.setStatus("待审批");
        h.setUpdateTime(LocalDate.now());
        hazardousMapper.updateById(h);
        return h;
    }
    /** 审批：待审批 → 已领用/已报废（approve=true 通过，false 退回在库） */
    public EmsHazardousLedger approve(Long id, String approveBy, boolean approve, String opinion) {
        EmsHazardousLedger h = getHazardous(id);
        if (!"待审批".equals(h.getStatus())) throw new BusinessException("仅【待审批】可审批，当前：" + h.getStatus());
        if (approve) {
            h.setStatus(h.getApplyReason() != null && h.getApplyReason().contains("报废") ? "已报废" : "已领用");
        } else {
            h.setStatus("在库");
        }
        h.setApproveBy(approveBy);
        h.setApproveOpinion(opinion);
        h.setApproveTime(LocalDate.now());
        h.setUpdateTime(LocalDate.now());
        hazardousMapper.updateById(h);
        return h;
    }
    public EmsHazardousLedger saveHazardous(EmsHazardousLedger h) {
        if (h.getId() == null) { h.setStatus("在库"); h.setCreateTime(LocalDate.now()); }
        h.setUpdateTime(LocalDate.now());
        if (h.getId() == null) hazardousMapper.insert(h); else hazardousMapper.updateById(h);
        return h;
    }
    public Page<EmsHazardousLedger> pageHazardous(String keyword, String status, int page, int size) {
        LambdaQueryWrapper<EmsHazardousLedger> q = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) q.like(EmsHazardousLedger::getName, keyword).or().like(EmsHazardousLedger::getCasNo, keyword);
        if (StringUtils.hasText(status)) q.eq(EmsHazardousLedger::getStatus, status);
        q.orderByDesc(EmsHazardousLedger::getCreateTime);
        return hazardousMapper.selectPage(new Page<>(page, size), q);
    }

    // ===================== 质控计划（状态机） =====================
    public EmsQcPlan savePlan(EmsQcPlan p) {
        if (p.getId() == null) {
            p.setPlanNo(CodeGenerator.generate("QC", (int) (this.count() + 1)));
            p.setStatus("草稿");
            p.setCreateTime(LocalDate.now());
        } else {
            EmsQcPlan old = getPlan(p.getId());
            if (!"草稿".equals(old.getStatus())) throw new BusinessException("仅【草稿】可编辑，当前：" + old.getStatus());
        }
        p.setUpdateTime(LocalDate.now());
        if (p.getId() == null) baseMapper.insert(p); else baseMapper.updateById(p);
        return p;
    }
    public EmsQcPlan submitPlan(Long id, String approver) {
        EmsQcPlan p = getPlan(id);
        if (!"草稿".equals(p.getStatus())) throw new BusinessException("仅【草稿】可提交审批，当前：" + p.getStatus());
        p.setStatus("审批中");
        p.setUpdateTime(LocalDate.now());
        baseMapper.updateById(p);
        return p;
    }
    public EmsQcPlan approvePlan(Long id, String approver) {
        EmsQcPlan p = getPlan(id);
        if (!"审批中".equals(p.getStatus())) throw new BusinessException("仅【审批中】可审批，当前：" + p.getStatus());
        p.setStatus("执行中");
        p.setApprovedBy(approver);
        p.setApprovedAt(LocalDate.now());
        p.setUpdateTime(LocalDate.now());
        baseMapper.updateById(p);
        return p;
    }
    public EmsQcPlan completePlan(Long id) {
        EmsQcPlan p = getPlan(id);
        if (!"执行中".equals(p.getStatus())) throw new BusinessException("仅【执行中】可完成，当前：" + p.getStatus());
        p.setStatus("已完成");
        p.setUpdateTime(LocalDate.now());
        baseMapper.updateById(p);
        return p;
    }
    public Page<EmsQcPlan> pagePlans(String year, String status, int page, int size) {
        LambdaQueryWrapper<EmsQcPlan> q = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(year)) q.eq(EmsQcPlan::getYear, Integer.parseInt(year));
        if (StringUtils.hasText(status)) q.eq(EmsQcPlan::getStatus, status);
        q.orderByDesc(EmsQcPlan::getCreateTime);
        return this.page(new Page<>(page, size), q);
    }
    public EmsQcPlan planDetail(Long id) {
        EmsQcPlan p = getPlan(id);
        return p;
    }

    // ===================== 监控活动 / 能力验证 / 比对 / 重复 =====================
    public EmsQcActivity saveActivity(EmsQcActivity a) {
        if (a.getId() == null) a.setCreateTime(LocalDate.now());
        a.setUpdateTime(LocalDate.now());
        if (a.getId() == null) activityMapper.insert(a); else activityMapper.updateById(a);
        return a;
    }
    public EmsProficiencyTest saveProficiency(EmsProficiencyTest t) {
        if (t.getId() == null) t.setCreateTime(LocalDate.now());
        t.setUpdateTime(LocalDate.now());
        if (t.getId() == null) proficiencyMapper.insert(t); else proficiencyMapper.updateById(t);
        return t;
    }
    public EmsInterlabCompare saveInterlab(EmsInterlabCompare c) {
        if (c.getId() == null) c.setCreateTime(LocalDate.now());
        c.setUpdateTime(LocalDate.now());
        if (c.getId() == null) interlabMapper.insert(c); else interlabMapper.updateById(c);
        return c;
    }
    public EmsRepeatTest saveRepeat(EmsRepeatTest r) {
        if (r.getId() == null) r.setCreateTime(LocalDate.now());
        r.setUpdateTime(LocalDate.now());
        if (r.getId() == null) repeatMapper.insert(r); else repeatMapper.updateById(r);
        return r;
    }

    public Page<EmsQcActivity> pageActivities(Long planId, String qcType, int page, int size) {
        LambdaQueryWrapper<EmsQcActivity> q = new LambdaQueryWrapper<>();
        if (planId != null) q.eq(EmsQcActivity::getPlanId, planId);
        if (StringUtils.hasText(qcType)) q.eq(EmsQcActivity::getQcType, qcType);
        q.orderByDesc(EmsQcActivity::getActDate);
        return activityMapper.selectPage(new Page<>(page, size), q);
    }
    public Page<EmsProficiencyTest> pageProficiency(Long planId, int page, int size) {
        LambdaQueryWrapper<EmsProficiencyTest> q = new LambdaQueryWrapper<>();
        if (planId != null) q.eq(EmsProficiencyTest::getPlanId, planId);
        q.orderByDesc(EmsProficiencyTest::getTestDate);
        return proficiencyMapper.selectPage(new Page<>(page, size), q);
    }
    public Page<EmsInterlabCompare> pageInterlab(Long planId, int page, int size) {
        LambdaQueryWrapper<EmsInterlabCompare> q = new LambdaQueryWrapper<>();
        if (planId != null) q.eq(EmsInterlabCompare::getPlanId, planId);
        q.orderByDesc(EmsInterlabCompare::getCompareDate);
        return interlabMapper.selectPage(new Page<>(page, size), q);
    }
    public Page<EmsRepeatTest> pageRepeat(Long planId, int page, int size) {
        LambdaQueryWrapper<EmsRepeatTest> q = new LambdaQueryWrapper<>();
        if (planId != null) q.eq(EmsRepeatTest::getPlanId, planId);
        q.orderByDesc(EmsRepeatTest::getTestDate);
        return repeatMapper.selectPage(new Page<>(page, size), q);
    }

    // ===================== 闸门校验（G2/G3，联动 025/026） =====================
    /** 标准物质效期闸门：返回临近过期/已过期清单（G2） */
    public Map<String, Object> materialGate() {
        List<EmsStandardMaterial> all = materialMapper.selectList(null);
        List<EmsStandardMaterial> blocked = all.stream()
                .filter(m -> m.getStatus() != null && !"在库".equals(m.getStatus()))
                .collect(Collectors.toList());
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("pass", blocked.isEmpty());
        r.put("blocked", blocked);
        return r;
    }
    /** 仪器设备校准闸门：校准到期日超期不可用于检测（G3） */
    public Map<String, Object> instrumentGate() {
        List<EmsInstrument> all = instrumentMapper.selectList(null);
        LocalDate today = LocalDate.now();
        List<Map<String, String>> blocked = new ArrayList<>();
        for (EmsInstrument ins : all) {
            if (ins.getStatus() != null && "停用".equals(ins.getStatus())) continue;
            LocalDate due = ins.getCalibDue();
            if (due == null) {
                Map<String, String> m = new LinkedHashMap<>();
                m.put("id", String.valueOf(ins.getId()));
                m.put("name", ins.getName());
                m.put("reason", "缺失校准到期日");
                blocked.add(m);
                continue;
            }
            if (due.isBefore(today)) {
                Map<String, String> m = new LinkedHashMap<>();
                m.put("id", String.valueOf(ins.getId()));
                m.put("name", ins.getName());
                m.put("reason", "校准超期(" + due + ")");
                blocked.add(m);
            }
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("pass", blocked.isEmpty());
        r.put("blocked", blocked);
        return r;
    }

    // ===================== 内部 =====================
    private String judgeExpiry(LocalDate expire) {
        if (expire == null) return "在库";
        LocalDate today = LocalDate.now();
        if (expire.isBefore(today)) return "过期";
        if (!expire.isAfter(today.plusDays(EXPIRE_WARN_DAYS))) return "临期";
        return "在库";
    }
    private EmsHazardousLedger getHazardous(Long id) {
        EmsHazardousLedger h = hazardousMapper.selectById(id);
        if (h == null) throw new BusinessException("危化品记录不存在：" + id);
        return h;
    }
    private EmsQcPlan getPlan(Long id) {
        EmsQcPlan p = baseMapper.selectById(id);
        if (p == null) throw new BusinessException("质控计划不存在：" + id);
        return p;
    }
}
