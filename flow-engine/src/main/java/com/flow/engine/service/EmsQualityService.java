package com.flow.engine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.flow.engine.common.BusinessException;
import com.flow.engine.common.RequestContext;
import com.flow.engine.dto.EmsHazardousDetailVO;
import com.flow.engine.dto.EmsResourceDetailVO;
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
    @Autowired private EmsQcHistoryMapper historyMapper;
    @Autowired private EmsProficiencyTestMapper proficiencyMapper;
    @Autowired private EmsInterlabCompareMapper interlabMapper;
    @Autowired private EmsRepeatTestMapper repeatMapper;
    @Autowired private EmsInstrumentMapper instrumentMapper;
    @Autowired private EmsMaterialFlowMapper materialFlowMapper;
    @Autowired private ProcessInstanceMapper processInstanceMapper;
    @Autowired private VariableMapper variableMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private PermissionEvaluator permissionEvaluator;

    /** 质控计划模块数据权限 Key（{模块}:data-all 表示查看模块全部数据） */
    private static final String MODULE_PERM_KEY = "ems:quality";

    private static final int EXPIRE_WARN_DAYS = 30; // 临期阈值

    /** 物资相关流程：入库申请 WZRKSQ / 使用申请 WZSYSQ / 报废申请 ZCBFSQ */
    private static final List<String> MATERIAL_PROCESS_KEYS = List.of("WZRKSQ", "WZSYSQ", "ZCBFSQ");

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

    // ===================== 物资详情（含关联流程） =====================
    /** 标准物质详情：基本信息 + 关联流程（入库申请 WZRKSQ / 使用申请 WZSYSQ） */
    public EmsResourceDetailVO materialDetail(Long id) {
        EmsStandardMaterial m = materialMapper.selectById(id);
        if (m == null) return null;
        EmsResourceDetailVO vo = new EmsResourceDetailVO();
        vo.setId(m.getId());
        vo.setType("标准物质");
        vo.setName(m.getName());
        vo.setSpec(m.getSpec());
        vo.setLotNo(m.getLotNo());
        vo.setCertNo(m.getCertNo());
        vo.setExpireDate(m.getExpireDate() == null ? null : m.getExpireDate().toString());
        vo.setStock(m.getStock());
        vo.setStatus(m.getStatus());
        vo.setRemark(m.getRemark());
        vo.setCreateBy(m.getCreateBy());
        vo.setCreateTime(m.getCreateTime() == null ? null : m.getCreateTime().toString());
        vo.setRelatedProcesses(listMaterialRelatedProcesses("标准物质", id, m.getName()));
        return vo;
    }

    /** 耗材详情：基本信息 + 关联流程（入库申请 WZRKSQ / 使用申请 WZSYSQ） */
    public EmsResourceDetailVO consumableDetail(Long id) {
        EmsConsumable c = consumableMapper.selectById(id);
        if (c == null) return null;
        EmsResourceDetailVO vo = new EmsResourceDetailVO();
        vo.setId(c.getId());
        vo.setType("耗材");
        vo.setName(c.getName());
        vo.setSpec(c.getSpec());
        vo.setExpireDate(c.getExpireDate() == null ? null : c.getExpireDate().toString());
        vo.setStock(c.getQty());
        vo.setStatus(c.getStatus());
        vo.setRemark(c.getRemark());
        vo.setCreateBy(c.getCreateBy());
        vo.setCreateTime(c.getCreateTime() == null ? null : c.getCreateTime().toString());
        vo.setRelatedProcesses(listMaterialRelatedProcesses("耗材", id, c.getName()));
        return vo;
    }

    /**
     * 查询与物资相关的流程实例：
     * 1) 已审批完成：流水表（t_material_flow）中 material_id 关联的 process_instance_id，携带入库/出库类型与数量；
     * 2) 进行中：流程变量 name 与物资同名的 WZRKSQ/WZSYSQ 实例（审批中，尚未产生流水）；
     * 3) 报废申请（ZCBFSQ）：流程变量 assetType=物资类型 且 assetId=物资ID。
     */
    private List<EmsResourceDetailVO.RelatedProcess> listMaterialRelatedProcesses(String materialType, Long materialId, String name) {
        // 流水记录：processInstanceId -> (bizType, qty)
        Map<Long, EmsMaterialFlow> flowMap = new LinkedHashMap<>();
        List<EmsMaterialFlow> flows = materialFlowMapper.selectList(new LambdaQueryWrapper<EmsMaterialFlow>()
                .eq(EmsMaterialFlow::getMaterialId, materialId)
                .eq(EmsMaterialFlow::getMaterialType, materialType));
        for (EmsMaterialFlow f : flows) {
            if (f.getProcessInstanceId() != null) flowMap.put(f.getProcessInstanceId(), f);
        }

        List<ProcessInstance> instances = processInstanceMapper.selectList(
                new LambdaQueryWrapper<ProcessInstance>()
                        .in(ProcessInstance::getProcessKey, MATERIAL_PROCESS_KEYS)
                        .orderByDesc(ProcessInstance::getStartTime));
        if (instances.isEmpty()) return new ArrayList<>();

        // 变量匹配：入库/使用按 name 同名；报废按 assetType+assetId
        List<Long> instanceIds = instances.stream().map(ProcessInstance::getId).toList();
        Set<Long> matchedIds = new HashSet<>(flowMap.keySet());
        List<Variable> matched = variableMapper.selectList(new LambdaQueryWrapper<Variable>()
                .in(Variable::getProcessInstanceId, instanceIds)
                .isNull(Variable::getTaskId)
                .in(Variable::getVariableKey, "name", "assetType", "assetId"));
        Map<Long, String> nameByInst = new HashMap<>();
        Map<Long, String> typeByInst = new HashMap<>();
        Map<Long, String> idByInst = new HashMap<>();
        for (Variable v : matched) {
            switch (v.getVariableKey()) {
                case "name" -> nameByInst.put(v.getProcessInstanceId(), v.getVariableValue());
                case "assetType" -> typeByInst.put(v.getProcessInstanceId(), v.getVariableValue());
                case "assetId" -> idByInst.put(v.getProcessInstanceId(), v.getVariableValue());
                default -> { }
            }
        }
        for (ProcessInstance inst : instances) {
            if ("ZCBFSQ".equals(inst.getProcessKey())) {
                if (materialType.equals(typeByInst.get(inst.getId()))
                        && String.valueOf(materialId).equals(idByInst.get(inst.getId()))) {
                    matchedIds.add(inst.getId());
                }
            } else if (StringUtils.hasText(name) && name.equals(nameByInst.get(inst.getId()))) {
                matchedIds.add(inst.getId());
            }
        }

        List<EmsResourceDetailVO.RelatedProcess> result = new ArrayList<>();
        for (ProcessInstance inst : instances) {
            if (!matchedIds.contains(inst.getId())) continue;
            EmsResourceDetailVO.RelatedProcess rp = new EmsResourceDetailVO.RelatedProcess();
            rp.setProcessInstanceId(inst.getId());
            rp.setInstanceNo(inst.getInstanceNo());
            rp.setProcessKey(inst.getProcessKey());
            rp.setProcessName(inst.getProcessName());
            rp.setStatusText(materialProcessStatusText(inst.getStatus()));
            rp.setStartUser(inst.getStartUser());
            rp.setStartTime(inst.getStartTime() == null ? null : inst.getStartTime().toString());
            EmsMaterialFlow flow = flowMap.get(inst.getId());
            if (flow != null) {
                rp.setBizType(flow.getBizType());
                rp.setQty(flow.getQty());
            } else if ("ZCBFSQ".equals(inst.getProcessKey())) {
                rp.setBizType("报废");
            }
            result.add(rp);
        }
        return result;
    }

    /** 流程实例状态码 → 文案 */
    private String materialProcessStatusText(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 0 -> "运行中";
            case 1 -> "已完成";
            case 2 -> "已暂停";
            case 3 -> "已终止";
            default -> "未知";
        };
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

    /** 危化品详情：基本信息 + 关联流程（报废申请 ZCBFSQ，按 assetType+assetId 变量匹配） */
    public EmsHazardousDetailVO hazardousDetail(Long id) {
        EmsHazardousLedger h = hazardousMapper.selectById(id);
        if (h == null) return null;
        EmsHazardousDetailVO vo = new EmsHazardousDetailVO();
        vo.setId(h.getId());
        vo.setName(h.getName());
        vo.setCasNo(h.getCasNo());
        vo.setCategory(h.getCategory());
        vo.setQty(h.getQty());
        vo.setUnit(h.getUnit());
        vo.setStatus(h.getStatus());
        vo.setApplyBy(h.getApplyBy());
        vo.setApplyReason(h.getApplyReason());
        vo.setApplyTime(h.getApplyTime() == null ? null : h.getApplyTime().toString());
        vo.setRemark(h.getRemark());
        vo.setCreateTime(h.getCreateTime() == null ? null : h.getCreateTime().toString());

        List<ProcessInstance> instances = processInstanceMapper.selectList(
                new LambdaQueryWrapper<ProcessInstance>()
                        .eq(ProcessInstance::getProcessKey, "ZCBFSQ")
                        .orderByDesc(ProcessInstance::getStartTime));
        List<EmsResourceDetailVO.RelatedProcess> result = new ArrayList<>();
        if (!instances.isEmpty()) {
            List<Long> instanceIds = instances.stream().map(ProcessInstance::getId).toList();
            List<Variable> vars = variableMapper.selectList(new LambdaQueryWrapper<Variable>()
                    .in(Variable::getProcessInstanceId, instanceIds)
                    .isNull(Variable::getTaskId)
                    .in(Variable::getVariableKey, "assetType", "assetId"));
            Map<Long, String> typeByInst = new HashMap<>();
            Map<Long, String> idByInst = new HashMap<>();
            for (Variable v : vars) {
                if ("assetType".equals(v.getVariableKey())) typeByInst.put(v.getProcessInstanceId(), v.getVariableValue());
                else idByInst.put(v.getProcessInstanceId(), v.getVariableValue());
            }
            for (ProcessInstance inst : instances) {
                if (!"危化品".equals(typeByInst.get(inst.getId()))
                        || !String.valueOf(id).equals(idByInst.get(inst.getId()))) continue;
                EmsResourceDetailVO.RelatedProcess rp = new EmsResourceDetailVO.RelatedProcess();
                rp.setProcessInstanceId(inst.getId());
                rp.setInstanceNo(inst.getInstanceNo());
                rp.setProcessKey(inst.getProcessKey());
                rp.setProcessName(inst.getProcessName());
                rp.setStatusText(materialProcessStatusText(inst.getStatus()));
                rp.setStartUser(inst.getStartUser());
                rp.setStartTime(inst.getStartTime() == null ? null : inst.getStartTime().toString());
                rp.setBizType("报废");
                result.add(rp);
            }
        }
        vo.setRelatedProcesses(result);
        return vo;
    }

    // ===================== 质控计划（状态机） =====================
    public EmsQcPlan savePlan(EmsQcPlan p, String opBy, String opName) {
        if (p.getId() == null) {
            p.setPlanNo(CodeGenerator.generate("QC", (int) (this.count() + 1)));
            p.setStatus("草稿");
            p.setCreatedBy(opBy);
            p.setCreatedName(opName);
            p.setCreateTime(LocalDate.now());
            p.setUpdateTime(LocalDate.now());
            baseMapper.insert(p);
            recordHistory("plan", p.getId(), "新建", "创建质控计划【" + p.getTitle() + "】", opBy, opName);
        } else {
            EmsQcPlan old = getPlan(p.getId());
            if (!"草稿".equals(old.getStatus())) throw new BusinessException("仅【草稿】可编辑，当前：" + old.getStatus());
            List<String> changes = new ArrayList<>();
            diff(changes, "计划名称", old.getTitle(), p.getTitle());
            diff(changes, "年度", old.getYear(), p.getYear());
            diff(changes, "季度", old.getQuarter(), p.getQuarter());
            diff(changes, "类型", old.getType(), p.getType());
            diff(changes, "责任人", old.getResponsibleId(), p.getResponsibleId());
            // 请求未携带的创建人字段保留库中原值，避免被覆盖为 null
            if (p.getCreatedBy() == null) p.setCreatedBy(old.getCreatedBy());
            if (p.getCreatedName() == null) p.setCreatedName(old.getCreatedName());
            p.setUpdateTime(LocalDate.now());
            baseMapper.updateById(p);
            if (!changes.isEmpty()) recordHistory("plan", p.getId(), "编辑", String.join("；", changes), opBy, opName);
        }
        return p;
    }
    public EmsQcPlan submitPlan(Long id, String approver, String opBy, String opName) {
        EmsQcPlan p = getPlan(id);
        if (!"草稿".equals(p.getStatus())) throw new BusinessException("仅【草稿】可提交审批，当前：" + p.getStatus());
        p.setStatus("审批中");
        p.setUpdateTime(LocalDate.now());
        baseMapper.updateById(p);
        recordHistory("plan", id, "状态变更", "状态: 草稿 → 审批中（提交审批）", opBy, opName);
        return p;
    }
    public EmsQcPlan approvePlan(Long id, String approver, String opBy, String opName) {
        EmsQcPlan p = getPlan(id);
        if (!"审批中".equals(p.getStatus())) throw new BusinessException("仅【审批中】可审批，当前：" + p.getStatus());
        p.setStatus("执行中");
        p.setApprovedBy(approver);
        p.setApprovedAt(LocalDate.now());
        p.setUpdateTime(LocalDate.now());
        baseMapper.updateById(p);
        recordHistory("plan", id, "状态变更", "状态: 审批中 → 执行中（审批通过）", opBy, opName);
        return p;
    }
    public EmsQcPlan completePlan(Long id, String opBy, String opName) {
        EmsQcPlan p = getPlan(id);
        if (!"执行中".equals(p.getStatus())) throw new BusinessException("仅【执行中】可完成，当前：" + p.getStatus());
        p.setStatus("已完成");
        p.setUpdateTime(LocalDate.now());
        baseMapper.updateById(p);
        recordHistory("plan", id, "状态变更", "状态: 执行中 → 已完成", opBy, opName);
        return p;
    }
    /** 删除计划（仅草稿可删），级联删除其下监控活动，并记录处置历史 */
    public void deletePlan(Long id, String opBy, String opName) {
        EmsQcPlan p = getPlan(id);
        if (!"草稿".equals(p.getStatus())) throw new BusinessException("仅【草稿】可删除，当前：" + p.getStatus());
        LambdaQueryWrapper<EmsQcActivity> aq = new LambdaQueryWrapper<>();
        aq.eq(EmsQcActivity::getPlanId, id);
        activityMapper.delete(aq);
        baseMapper.deleteById(id);
        recordHistory("plan", id, "删除", "删除质控计划【" + p.getTitle() + "】及其监控活动", opBy, opName);
    }
    public Page<EmsQcPlan> pagePlans(String keyword, String year, String status, int page, int size) {
        LambdaQueryWrapper<EmsQcPlan> q = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            q.and(w -> w.like(EmsQcPlan::getTitle, keyword).or().like(EmsQcPlan::getPlanNo, keyword));
        }
        if (StringUtils.hasText(year)) q.eq(EmsQcPlan::getYear, Integer.parseInt(year));
        if (StringUtils.hasText(status)) q.eq(EmsQcPlan::getStatus, status);
        applyPlanDataScope(q);
        q.orderByDesc(EmsQcPlan::getCreateTime);
        Page<EmsQcPlan> result = this.page(new Page<>(page, size), q);
        fillTaskProgress(result.getRecords());
        fillResponsibleName(result.getRecords());
        return result;
    }

    /** 聚合计划任务进度：taskTotal=监控活动总数，taskDone=任务状态为「已完成」的数量 */
    private void fillTaskProgress(java.util.List<EmsQcPlan> planList) {
        if (planList == null || planList.isEmpty()) return;
        java.util.List<Long> planIds = planList.stream().map(EmsQcPlan::getId).collect(java.util.stream.Collectors.toList());
        LambdaQueryWrapper<EmsQcActivity> q = new LambdaQueryWrapper<>();
        q.in(EmsQcActivity::getPlanId, planIds);
        q.select(EmsQcActivity::getPlanId, EmsQcActivity::getTaskStatus);
        java.util.List<EmsQcActivity> acts = activityMapper.selectList(q);
        java.util.Map<Long, int[]> stat = new java.util.HashMap<>();
        for (EmsQcActivity a : acts) {
            int[] c = stat.computeIfAbsent(a.getPlanId(), k -> new int[2]);
            c[0]++;
            if ("已完成".equals(a.getTaskStatus())) c[1]++;
        }
        for (EmsQcPlan p : planList) {
            int[] c = stat.getOrDefault(p.getId(), new int[2]);
            p.setTaskTotal(c[0]);
            p.setTaskDone(c[1]);
        }
    }
    /** 回填责任人姓名：按 responsibleId 批量关联用户表（无对应用户时保留空值，前端回退展示账号） */
    private void fillResponsibleName(java.util.List<EmsQcPlan> planList) {
        if (planList == null || planList.isEmpty()) return;
        java.util.List<String> ids = planList.stream()
                .map(EmsQcPlan::getResponsibleId)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
        if (ids.isEmpty()) return;
        List<User> users = userMapper.selectList(
                new LambdaQueryWrapper<User>().in(User::getUsername, ids));
        java.util.Map<String, String> nameMap = new java.util.HashMap<>();
        for (User u : users) {
            if (StringUtils.hasText(u.getRealName())) nameMap.put(u.getUsername(), u.getRealName());
        }
        for (EmsQcPlan p : planList) {
            if (StringUtils.hasText(p.getResponsibleId())) {
                p.setResponsibleName(nameMap.get(p.getResponsibleId()));
            }
        }
    }
    public EmsQcPlan planDetail(Long id) {
        EmsQcPlan p = getPlan(id);
        // 详情页同样聚合任务进度（taskTotal/taskDone），避免展示 0/0
        if (p != null) fillTaskProgress(java.util.Collections.singletonList(p));
        if (p != null) fillResponsibleName(java.util.Collections.singletonList(p));
        return p;
    }

    // ===================== 监控活动 / 能力验证 / 比对 / 重复 =====================
    public EmsQcActivity saveActivity(EmsQcActivity a, String opBy, String opName) {
        // 活动日期校验：结束日期 >= 开始日期；活动日期不能超出所属计划的日期范围
        if (a.getStartDate() != null && a.getEndDate() != null && a.getEndDate().isBefore(a.getStartDate())) {
            throw new BusinessException("结束日期必须大于等于开始日期");
        }
        if (a.getPlanId() != null) {
            EmsQcPlan plan = baseMapper.selectById(a.getPlanId());
            if (plan != null) {
                if (plan.getStartDate() != null && a.getStartDate() != null && a.getStartDate().isBefore(plan.getStartDate())) {
                    throw new BusinessException("活动开始日期不能早于计划开始日期（" + plan.getStartDate() + "）");
                }
                if (plan.getEndDate() != null && a.getEndDate() != null && a.getEndDate().isAfter(plan.getEndDate())) {
                    throw new BusinessException("活动结束日期不能晚于计划结束日期（" + plan.getEndDate() + "）");
                }
            }
        }
        if (a.getId() == null) {
            a.setTaskNo(generateTaskNo());
            a.setCreatedBy(opBy);
            a.setCreatedName(opName);
            a.setCreateTime(LocalDate.now());
            a.setUpdateTime(LocalDate.now());
            activityMapper.insert(a);
            recordHistory("activity", a.getId(), "新建",
                    "添加监控活动【" + nvl(a.getQcType()) + "-" + nvl(a.getItem()) + "】，任务编号 " + a.getTaskNo(), opBy, opName);
        } else {
            EmsQcActivity old = getActivity(a.getId());
            // 已完成的任务不允许再修改任务状态
            if ("已完成".equals(old.getTaskStatus()) && a.getTaskStatus() != null
                    && !"已完成".equals(a.getTaskStatus())) {
                throw new BusinessException("已完成的任务不允许修改任务状态");
            }
            // 请求未携带的系统字段保留库中原值，避免被覆盖为 null
            if (a.getTaskNo() == null) a.setTaskNo(old.getTaskNo());
            if (a.getCreatedBy() == null) a.setCreatedBy(old.getCreatedBy());
            if (a.getCreatedName() == null) a.setCreatedName(old.getCreatedName());
            if (a.getCreateTime() == null) a.setCreateTime(old.getCreateTime());
            if (a.getActDate() == null) a.setActDate(old.getActDate());
            List<String> changes = new ArrayList<>();
            diff(changes, "活动类型", old.getQcType(), a.getQcType());
            diff(changes, "检测项目", old.getItem(), a.getItem());
            diff(changes, "活动执行人", old.getOperatorName() != null ? old.getOperatorName() : old.getOperatorId(),
                    a.getOperatorName() != null ? a.getOperatorName() : a.getOperatorId());
            diff(changes, "任务状态", old.getTaskStatus(), a.getTaskStatus());
            diff(changes, "开始日期", old.getStartDate(), a.getStartDate());
            diff(changes, "结束日期", old.getEndDate(), a.getEndDate());
            diff(changes, "活动描述", old.getDescription(), a.getDescription());
            a.setUpdateTime(LocalDate.now());
            activityMapper.updateById(a);
            if (!changes.isEmpty()) {
                // 仅任务状态变化时记为状态变更，其余记为编辑
                String action = changes.size() == 1 && changes.get(0).startsWith("任务状态") ? "状态变更" : "编辑";
                recordHistory("activity", a.getId(), action, String.join("；", changes), opBy, opName);
            }
        }
        return a;
    }
    /** 删除监控活动并记录处置历史 */
    public void deleteActivity(Long id, String opBy, String opName) {
        EmsQcActivity a = getActivity(id);
        activityMapper.deleteById(id);
        recordHistory("activity", id, "删除",
                "删除监控活动【" + nvl(a.getQcType()) + "-" + nvl(a.getItem()) + "】", opBy, opName);
    }
    public EmsQcActivity activityDetail(Long id) {
        return getActivity(id);
    }

    // ===================== 处置历史 =====================
    public List<EmsQcHistory> listHistory(String bizType, Long bizId) {
        LambdaQueryWrapper<EmsQcHistory> q = new LambdaQueryWrapper<>();
        q.eq(EmsQcHistory::getBizType, bizType).eq(EmsQcHistory::getBizId, bizId);
        q.orderByDesc(EmsQcHistory::getId);
        return historyMapper.selectList(q);
    }
    private void recordHistory(String bizType, Long bizId, String action, String content, String opBy, String opName) {
        EmsQcHistory h = new EmsQcHistory();
        h.setBizType(bizType);
        h.setBizId(bizId);
        h.setAction(action);
        h.setContent(content);
        h.setOperatorId(opBy);
        h.setOperatorName(opName);
        h.setCreateTime(LocalDate.now());
        historyMapper.insert(h);
    }
    private static String nvl(Object v) { return v == null ? "" : String.valueOf(v); }
    private static void diff(List<String> changes, String label, Object oldV, Object newV) {
        String o = oldV == null ? "" : String.valueOf(oldV);
        String n = newV == null ? "" : String.valueOf(newV);
        if (!Objects.equals(o, n)) {
            changes.add(label + ": " + (o.isEmpty() ? "(空)" : o) + " → " + (n.isEmpty() ? "(空)" : n));
        }
    }
    private EmsQcActivity getActivity(Long id) {
        EmsQcActivity a = activityMapper.selectById(id);
        if (a == null) throw new BusinessException("监控活动不存在：" + id);
        return a;
    }
    /** 任务编号生成：T+yyyyMMdd+4位当日递增序号，如 T202608080001 */
    private String generateTaskNo() {
        String prefix = "T" + LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        LambdaQueryWrapper<EmsQcActivity> q = new LambdaQueryWrapper<>();
        q.likeRight(EmsQcActivity::getTaskNo, prefix);
        q.orderByDesc(EmsQcActivity::getTaskNo);
        q.last("LIMIT 1");
        EmsQcActivity top = activityMapper.selectOne(q);
        int seq = 1;
        if (top != null && top.getTaskNo() != null && top.getTaskNo().length() > prefix.length()) {
            try { seq = Integer.parseInt(top.getTaskNo().substring(prefix.length())) + 1; } catch (Exception ignored) { }
        }
        return prefix + String.format("%04d", seq);
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

    public Page<EmsQcActivity> pageActivities(Long planId, String qcType, String keyword,
                                              String operatorId, String taskStatus,
                                              String startDateFrom, String startDateTo, int page, int size) {
        LambdaQueryWrapper<EmsQcActivity> q = new LambdaQueryWrapper<>();
        if (planId != null) q.eq(EmsQcActivity::getPlanId, planId);
        if (StringUtils.hasText(qcType)) q.eq(EmsQcActivity::getQcType, qcType);
        if (StringUtils.hasText(operatorId)) q.eq(EmsQcActivity::getOperatorId, operatorId);
        if (StringUtils.hasText(taskStatus)) q.eq(EmsQcActivity::getTaskStatus, taskStatus);
        if (StringUtils.hasText(keyword)) {
            q.and(w -> w.like(EmsQcActivity::getTaskNo, keyword)
                    .or().like(EmsQcActivity::getQcType, keyword)
                    .or().like(EmsQcActivity::getItem, keyword)
                    .or().like(EmsQcActivity::getOperatorName, keyword)
                    .or().like(EmsQcActivity::getOperatorId, keyword)
                    .or().like(EmsQcActivity::getTaskStatus, keyword));
        }
        // 时间范围：按活动开始日期过滤
        if (StringUtils.hasText(startDateFrom)) q.ge(EmsQcActivity::getStartDate, LocalDate.parse(startDateFrom));
        if (StringUtils.hasText(startDateTo)) q.le(EmsQcActivity::getStartDate, LocalDate.parse(startDateTo));
        applyActivityDataScope(q);
        q.orderByDesc(EmsQcActivity::getId);
        Page<EmsQcActivity> result = activityMapper.selectPage(new Page<>(page, size), q);
        fillPlanTitle(result.getRecords());
        return result;
    }

    /**
     * 质控计划数据范围过滤：
     * 1. 系统管理员（角色数据范围=全部）或授权 ems:quality:data-all → 不过滤，查看全部数据；
     * 2. 其余用户仅可见：计划责任人为本人（responsibleId）、本人创建（createdBy）的计划，
     *    以及本人作为活动执行人参与的计划（上级计划）。
     */
    private void applyPlanDataScope(LambdaQueryWrapper<EmsQcPlan> q) {
        RequestContext ctx = RequestContext.current();
        String userIdStr = ctx.getUserId();
        if (!StringUtils.hasText(userIdStr)) {
            q.apply("1 = 0");
            return;
        }
        Long userId;
        try {
            userId = Long.valueOf(userIdStr);
        } catch (NumberFormatException e) {
            q.apply("1 = 0");
            return;
        }
        if (permissionEvaluator.canViewAllModuleData(userId, MODULE_PERM_KEY)) {
            return;
        }
        String username = ctx.getUsername();
        if (!StringUtils.hasText(username)) {
            q.apply("1 = 0");
            return;
        }
        // 本人作为活动执行人参与的上级计划
        List<Long> operatorPlanIds = activityMapper.selectList(new LambdaQueryWrapper<EmsQcActivity>()
                .select(EmsQcActivity::getPlanId)
                .eq(EmsQcActivity::getOperatorId, username))
                .stream().map(EmsQcActivity::getPlanId)
                .filter(java.util.Objects::nonNull)
                .distinct().collect(Collectors.toList());
        if (operatorPlanIds.isEmpty()) {
            q.and(w -> w.eq(EmsQcPlan::getResponsibleId, username)
                    .or().eq(EmsQcPlan::getCreatedBy, username));
        } else {
            q.and(w -> w.eq(EmsQcPlan::getResponsibleId, username)
                    .or().eq(EmsQcPlan::getCreatedBy, username)
                    .or().in(EmsQcPlan::getId, operatorPlanIds));
        }
    }

    /**
     * 监控活动（计划下任务）数据范围过滤：
     * 1. 系统管理员或授权 ems:quality:data-all → 不过滤；
     * 2. 其余用户可见：活动执行人为本人（operatorId）、本人创建（createdBy）的活动，
     *    以及本人可见计划（责任人/创建人）下的全部活动。
     */
    private void applyActivityDataScope(LambdaQueryWrapper<EmsQcActivity> q) {
        RequestContext ctx = RequestContext.current();
        String userIdStr = ctx.getUserId();
        if (!StringUtils.hasText(userIdStr)) {
            q.apply("1 = 0");
            return;
        }
        Long userId;
        try {
            userId = Long.valueOf(userIdStr);
        } catch (NumberFormatException e) {
            q.apply("1 = 0");
            return;
        }
        if (permissionEvaluator.canViewAllModuleData(userId, MODULE_PERM_KEY)) {
            return;
        }
        String username = ctx.getUsername();
        if (!StringUtils.hasText(username)) {
            q.apply("1 = 0");
            return;
        }
        // 本人可见的计划集合（责任人或创建人为本人）
        List<Long> visiblePlanIds = this.list(new LambdaQueryWrapper<EmsQcPlan>()
                .select(EmsQcPlan::getId)
                .and(w -> w.eq(EmsQcPlan::getResponsibleId, username)
                        .or().eq(EmsQcPlan::getCreatedBy, username)))
                .stream().map(EmsQcPlan::getId).collect(Collectors.toList());
        if (visiblePlanIds.isEmpty()) {
            q.and(w -> w.eq(EmsQcActivity::getOperatorId, username)
                    .or().eq(EmsQcActivity::getCreatedBy, username));
        } else {
            q.and(w -> w.eq(EmsQcActivity::getOperatorId, username)
                    .or().eq(EmsQcActivity::getCreatedBy, username)
                    .or().in(EmsQcActivity::getPlanId, visiblePlanIds));
        }
    }

    /** 批量回填活动所属计划名称（供任务视图/待办展示） */
    private void fillPlanTitle(List<EmsQcActivity> list) {
        if (list == null || list.isEmpty()) return;
        Set<Long> planIds = list.stream().map(EmsQcActivity::getPlanId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (planIds.isEmpty()) return;
        Map<Long, String> titleMap = new HashMap<>();
        for (EmsQcPlan p : baseMapper.selectBatchIds(planIds)) {
            titleMap.put(p.getId(), p.getTitle());
        }
        list.forEach(a -> a.setPlanTitle(titleMap.get(a.getPlanId())));
    }

    /**
     * 质控活动待办：指定活动执行人名下、任务状态非「已完成/已取消」的活动（状态为空的旧数据一并视为待办），
     * 供工作台待办任务展示；附带所属计划名称
     */
    public List<EmsQcActivity> listTodoActivities(String operatorId) {
        if (!StringUtils.hasText(operatorId)) return new ArrayList<>();
        LambdaQueryWrapper<EmsQcActivity> q = new LambdaQueryWrapper<>();
        q.eq(EmsQcActivity::getOperatorId, operatorId);
        q.and(w -> w.isNull(EmsQcActivity::getTaskStatus)
                .or().notIn(EmsQcActivity::getTaskStatus, "已完成", "已取消"));
        q.orderByDesc(EmsQcActivity::getId);
        List<EmsQcActivity> list = activityMapper.selectList(q);
        fillPlanTitle(list);
        return list;
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
