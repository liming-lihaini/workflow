package com.flow.engine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.flow.engine.common.BusinessException;
import com.flow.engine.dto.EmsDispatchDetailVO;
import com.flow.engine.entity.*;
import com.flow.engine.mapper.EmsDispatchMapper;
import com.flow.engine.mapper.EmsSampleMapper;
import com.flow.engine.service.EmsMonitorPointService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 环境监测 - 调度派单服务（TRD 5.2，BR-023-03 资质闸门 / BR-023-04 资源冲突）
 * 复用 EmsEmployee(EmsVehicle 同族) / EmsInstrument / EmsDispatch* 实体，不造通用派单引擎。
 */
@Service
public class EmsDispatchService extends ServiceImpl<EmsDispatchMapper, EmsDispatch> {

    @Autowired
    private EmsSamplingOrderService samplingOrderService;
    @Autowired
    private UserService userService;
    @Autowired
    private EmsInstrumentService instrumentService;
    @Autowired
    private EmsDispatchMemberService dispatchMemberService;
    @Autowired
    private EmsDispatchDeviceService dispatchDeviceService;
    @Autowired
    private EmsVehicleService vehicleService;
    @Autowired
    private EmsEntrustService entrustService;
    @Autowired
    private EmsMonitorPointService monitorPointService;
    @Autowired
    private EmsSampleMapper sampleMapper;

    @Autowired(required = false)
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    /**
     * 派单：资质闸门 + 冲突检测通过后落库，订单转「已派单」。
     * 人员来源为后台用户(sys_user)，负责人 leadId 标记为 LEAD。
     * @return 派单记录
     */
    @Transactional
    public EmsDispatch dispatch(Long orderId, Long vehicleId, Long leadId, List<Long> empIds, List<Long> instrumentIds,
                                LocalDateTime planStart, LocalDateTime planEnd, String note) {
        return dispatch(orderId, vehicleId, leadId, empIds, instrumentIds, planStart, planEnd, note, null);
    }

    /**
     * 派单：资质闸门 + 冲突检测通过后落库，订单转「已派单」。
     * excludeOrderIds 用于批量派单：同一批次内已落库的派单互相之间不算资源冲突。
     * 人员来源为后台用户(sys_user)，负责人 leadId 标记为 LEAD。
     * @param excludeOrderIds 冲突检测时排除的订单（同一批次内其它派单），可为 null
     * @return 派单记录
     */
    @Transactional
    public EmsDispatch dispatch(Long orderId, Long vehicleId, Long leadId, List<Long> empIds, List<Long> instrumentIds,
                                LocalDateTime planStart, LocalDateTime planEnd, String note, java.util.Set<Long> excludeOrderIds) {
        EmsSamplingOrder order = samplingOrderService.getById(orderId);
        if (order == null) {
            throw new BusinessException("采样任务不存在");
        }
        if (!"待派单".equals(order.getStatus())) {
            throw new BusinessException("仅待派单状态可派单，当前：" + order.getStatus());
        }
        // 计划时间校验：结束时间必须大于等于开始时间
        if (planStart != null && planEnd != null && planEnd.isBefore(planStart)) {
            throw new BusinessException("结束时间必须大于等于开始时间");
        }
        // 派单以委托单为主体，点位在采样执行环节选择，故不再校验 pointId（原 BR-023-07 已移除）
        // BR-023-03 资质闸门：人员状态有效 + 设备校准有效期
        gateCheck(leadId, empIds, instrumentIds);
        // BR-023-04 资源冲突检测（返回详细冲突信息）
        List<Long> resources = new java.util.ArrayList<>();
        if (vehicleId != null) resources.add(-vehicleId); // 车辆以负 id 区分
        if (leadId != null) resources.add(leadId);
        if (empIds != null) resources.addAll(empIds);
        if (instrumentIds != null) resources.addAll(instrumentIds);
        List<Map<String, Object>> conflicts = findConflicts(planStart, planEnd, resources, excludeOrderIds);
        if (!conflicts.isEmpty()) {
            throw new BusinessException(buildConflictMessage(conflicts));
        }
        // ISSUE-036 维修保养拦截：车辆在检车时间区间内处于维修保养中则不可派单
        if (vehicleId != null && vehicleService.isUnderMaintenance(vehicleId, planStart, planEnd)) {
            throw new BusinessException("车辆在维修保养期间不可派单，请调整计划时间或选择其他车辆(ISSUE-036)");
        }

        EmsDispatch d = new EmsDispatch();
        d.setOrderId(orderId);
        d.setVehicleId(vehicleId);
        d.setStatus("已派单");
        d.setDispatchTime(LocalDateTime.now());
        d.setPlanStart(planStart);
        d.setPlanEnd(planEnd);
        d.setNote(note);
        d.setCreateTime(LocalDateTime.now());
        d.setUpdateTime(LocalDateTime.now());
        this.save(d);

        // 负责人 LEAD + 组员 MEMBER
        if (leadId != null) {
            EmsDispatchMember lead = new EmsDispatchMember();
            lead.setDispatchId(d.getId());
            lead.setEmpId(leadId);
            lead.setRole("LEAD");
            dispatchMemberService.save(lead);
        }
        if (empIds != null) {
            for (Long empId : empIds) {
                if (empId.equals(leadId)) continue; // 负责人不重复
                EmsDispatchMember m = new EmsDispatchMember();
                m.setDispatchId(d.getId());
                m.setEmpId(empId);
                m.setRole("MEMBER");
                dispatchMemberService.save(m);
            }
        }
        if (instrumentIds != null) {
            for (Long insId : instrumentIds) {
                EmsDispatchDevice dev = new EmsDispatchDevice();
                dev.setDispatchId(d.getId());
                dev.setInstrumentId(insId);
                dispatchDeviceService.save(dev);
            }
        }
        order.setStatus("已派单");
        order.setUpdateTime(LocalDateTime.now());
        samplingOrderService.updateById(order);
        // 记录「派单」操作历史（操作人取当前请求上下文）
        samplingOrderService.recordOrderHistory(orderId, "派单",
                describeDispatch(order, leadId, empIds, vehicleId, instrumentIds, planStart, planEnd),
                currentOperator());
        return d;
    }

    /**
     * 编辑派单信息：负责人/组员/车辆/设备/计划区间/备注。
     * 复用资质闸门、资源冲突（排除本订单自身派单）与维修保养校验；
     * 成员与设备关联全量重建，并将字段变更明细记入采样任务操作历史。
     */
    @Transactional
    public EmsDispatch updateDispatch(Long dispatchId, Long vehicleId, Long leadId, List<Long> empIds, List<Long> instrumentIds,
                                      LocalDateTime planStart, LocalDateTime planEnd, String note, User operator) {
        EmsDispatch d = this.getById(dispatchId);
        if (d == null) {
            throw new BusinessException("派单不存在: " + dispatchId);
        }
        EmsSamplingOrder order = samplingOrderService.getById(d.getOrderId());
        if (order == null) {
            throw new BusinessException("采样任务不存在");
        }
        // 计划时间校验：结束时间必须大于等于开始时间
        if (planStart != null && planEnd != null && planEnd.isBefore(planStart)) {
            throw new BusinessException("结束时间必须大于等于开始时间");
        }
        // BR-023-03 资质闸门 / BR-023-04 资源冲突（排除本订单自身派单）/ ISSUE-036 维修保养拦截
        gateCheck(leadId, empIds, instrumentIds);
        List<Long> resources = new java.util.ArrayList<>();
        if (vehicleId != null) resources.add(-vehicleId);
        if (leadId != null) resources.add(leadId);
        if (empIds != null) resources.addAll(empIds);
        if (instrumentIds != null) resources.addAll(instrumentIds);
        java.util.Set<Long> excludeSelf = new java.util.HashSet<>();
        excludeSelf.add(order.getId());
        List<Map<String, Object>> conflicts = findConflicts(planStart, planEnd, resources, excludeSelf);
        if (!conflicts.isEmpty()) {
            throw new BusinessException(buildConflictMessage(conflicts));
        }
        if (vehicleId != null && vehicleService.isUnderMaintenance(vehicleId, planStart, planEnd)) {
            throw new BusinessException("车辆在维修保养期间不可派单，请调整计划时间或选择其他车辆(ISSUE-036)");
        }

        // 变更明细（改前 vs 改后），无变化也记录一条编辑轨迹
        String diff = buildEditDiff(d, leadId, empIds, vehicleId, instrumentIds, planStart, planEnd, note);

        d.setVehicleId(vehicleId);
        d.setPlanStart(planStart);
        d.setPlanEnd(planEnd);
        d.setNote(note);
        d.setUpdateTime(LocalDateTime.now());
        this.updateById(d);

        // 成员全量重建：负责人 LEAD + 组员 MEMBER
        dispatchMemberService.remove(new LambdaQueryWrapper<EmsDispatchMember>().eq(EmsDispatchMember::getDispatchId, dispatchId));
        if (leadId != null) {
            EmsDispatchMember lead = new EmsDispatchMember();
            lead.setDispatchId(dispatchId);
            lead.setEmpId(leadId);
            lead.setRole("LEAD");
            dispatchMemberService.save(lead);
        }
        if (empIds != null) {
            for (Long empId : empIds) {
                if (empId.equals(leadId)) continue;
                EmsDispatchMember m = new EmsDispatchMember();
                m.setDispatchId(dispatchId);
                m.setEmpId(empId);
                m.setRole("MEMBER");
                dispatchMemberService.save(m);
            }
        }
        // 设备关联全量重建
        dispatchDeviceService.remove(new LambdaQueryWrapper<EmsDispatchDevice>().eq(EmsDispatchDevice::getDispatchId, dispatchId));
        if (instrumentIds != null) {
            for (Long insId : instrumentIds) {
                EmsDispatchDevice dev = new EmsDispatchDevice();
                dev.setDispatchId(dispatchId);
                dev.setInstrumentId(insId);
                dispatchDeviceService.save(dev);
            }
        }

        samplingOrderService.recordOrderHistory(order.getId(), "编辑",
                "采样任务【" + order.getOrderNo() + "】派单信息编辑：" + diff, operator);
        return d;
    }

    /** 派单历史内容：负责人/组员/车辆/设备数量/计划区间 */
    private String describeDispatch(EmsSamplingOrder order, Long leadId, List<Long> empIds, Long vehicleId,
                                    List<Long> instrumentIds, LocalDateTime planStart, LocalDateTime planEnd) {
        List<String> parts = new java.util.ArrayList<>();
        if (leadId != null) parts.add("负责人：" + userName(leadId));
        if (empIds != null) {
            String members = empIds.stream().filter(id -> !id.equals(leadId)).map(this::userName)
                    .collect(java.util.stream.Collectors.joining("、"));
            if (!members.isEmpty()) parts.add("组员：" + members);
        }
        if (vehicleId != null) {
            EmsVehicle v = vehicleService.getById(vehicleId);
            parts.add("车辆：" + (v != null && v.getPlateNo() != null ? v.getPlateNo() : String.valueOf(vehicleId)));
        }
        if (instrumentIds != null && !instrumentIds.isEmpty()) parts.add("设备：" + instrumentIds.size() + " 台");
        if (planStart != null && planEnd != null) {
            parts.add("计划：" + planStart.toLocalDate() + " ~ " + planEnd.toLocalDate());
        }
        return "采样任务【" + order.getOrderNo() + "】已派单" + (parts.isEmpty() ? "" : "（" + String.join("；", parts) + "）");
    }

    /** 编辑变更明细：逐字段对比改前/改后，仅列出有变化的项 */
    private String buildEditDiff(EmsDispatch old, Long leadId, List<Long> empIds, Long vehicleId,
                                 List<Long> instrumentIds, LocalDateTime planStart, LocalDateTime planEnd, String note) {
        List<EmsDispatchMember> oldMembers = dispatchMemberService.listByDispatch(old.getId());
        Long oldLeadId = oldMembers.stream().filter(m -> "LEAD".equals(m.getRole()))
                .map(EmsDispatchMember::getEmpId).findFirst().orElse(null);
        String oldMemberNames = oldMembers.stream().filter(m -> "MEMBER".equals(m.getRole()))
                .map(m -> userName(m.getEmpId())).sorted().collect(java.util.stream.Collectors.joining("、"));
        List<Long> newMemberIds = empIds == null ? java.util.Collections.emptyList()
                : empIds.stream().filter(id -> !id.equals(leadId)).collect(java.util.stream.Collectors.toList());
        String newMemberNames = newMemberIds.stream().map(this::userName).sorted().collect(java.util.stream.Collectors.joining("、"));

        String oldVehicle = vehicleName(old.getVehicleId());
        String newVehicle = vehicleName(vehicleId);
        List<EmsDispatchDevice> oldDevices = dispatchDeviceService.listByDispatch(old.getId());
        String oldInsNames = oldDevices.stream().map(dev -> instrumentName(dev.getInstrumentId())).sorted()
                .collect(java.util.stream.Collectors.joining("、"));
        String newInsNames = (instrumentIds == null ? java.util.Collections.<Long>emptyList() : instrumentIds).stream()
                .map(this::instrumentName).sorted().collect(java.util.stream.Collectors.joining("、"));
        String oldRange = rangeText(old.getPlanStart(), old.getPlanEnd());
        String newRange = rangeText(planStart, planEnd);
        String oldNote = old.getNote() == null ? "" : old.getNote();
        String newNote = note == null ? "" : note;

        List<String> diffs = new java.util.ArrayList<>();
        if (!java.util.Objects.equals(oldLeadId, leadId)) diffs.add("负责人：" + userName(oldLeadId) + " → " + userName(leadId));
        if (!oldMemberNames.equals(newMemberNames)) diffs.add("组员：" + emptyDash(oldMemberNames) + " → " + emptyDash(newMemberNames));
        if (!oldVehicle.equals(newVehicle)) diffs.add("车辆：" + emptyDash(oldVehicle) + " → " + emptyDash(newVehicle));
        if (!oldInsNames.equals(newInsNames)) diffs.add("设备：" + emptyDash(oldInsNames) + " → " + emptyDash(newInsNames));
        if (!oldRange.equals(newRange)) diffs.add("计划区间：" + oldRange + " → " + newRange);
        if (!oldNote.equals(newNote)) diffs.add("备注：" + emptyDash(oldNote) + " → " + emptyDash(newNote));
        return diffs.isEmpty() ? "无字段变化" : String.join("；", diffs);
    }

    private String rangeText(LocalDateTime start, LocalDateTime end) {
        String s = start == null ? "—" : start.toLocalDate().toString();
        String e = end == null ? "—" : end.toLocalDate().toString();
        return s + " ~ " + e;
    }

    private String emptyDash(String s) {
        return s == null || s.isEmpty() ? "—" : s;
    }

    /** 当前请求操作人（会话 Token / API Token），取不到时返回 null（历史展示为系统） */
    private User currentOperator() {
        try {
            String userId = com.flow.engine.common.RequestContext.current().getUserId();
            return userId == null ? null : userService.getUser(Long.valueOf(userId));
        } catch (Exception e) {
            return null;
        }
    }

    /** 冲突预检接口：返回是否存在冲突 */
    public boolean hasConflict(LocalDateTime planStart, LocalDateTime planEnd, List<Long> resourceIds) {
        if (resourceIds == null || resourceIds.isEmpty()) {
            return false;
        }
        List<EmsDispatch> active = this.list(new LambdaQueryWrapper<EmsDispatch>()
                .in(EmsDispatch::getStatus, "已派单", "采样执行中"));
        for (EmsDispatch d : active) {
            if (d.getPlanStart() == null || d.getPlanEnd() == null) continue;
            boolean overlap = planStart.isBefore(d.getPlanEnd()) && planEnd.isAfter(d.getPlanStart());
            if (!overlap) continue;
            // 资源是否重叠
            if (d.getVehicleId() != null && resourceIds.contains(-d.getVehicleId())) return true;
            for (EmsDispatchMember m : dispatchMemberService.listByDispatch(d.getId())) {
                if (resourceIds.contains(m.getEmpId())) return true;
            }
            for (EmsDispatchDevice dev : dispatchDeviceService.listByDispatch(d.getId())) {
                if (resourceIds.contains(dev.getInstrumentId())) return true;
            }
        }
        return false;
    }

    /**
     * BR-023-04 资源冲突检测（详细版）：返回所有冲突资源及其占用时段与任务编号。
     * 资源 id 约定：车辆 = -vehicleId，人员 = empId，设备 = instrumentId。
     */
    public List<Map<String, Object>> findConflicts(LocalDateTime planStart, LocalDateTime planEnd, List<Long> resourceIds) {
        return findConflicts(planStart, planEnd, resourceIds, null);
    }

    /**
     * BR-023-04 资源冲突检测（详细版）：返回所有冲突资源及其占用时段与任务编号。
     * excludeOrderIds 内的派单（同一批次其它派单）不参与冲突判定。
     * 资源 id 约定：车辆 = -vehicleId，人员 = empId，设备 = instrumentId。
     */
    public List<Map<String, Object>> findConflicts(LocalDateTime planStart, LocalDateTime planEnd, List<Long> resourceIds, java.util.Set<Long> excludeOrderIds) {
        List<Map<String, Object>> conflicts = new java.util.ArrayList<>();
        if (resourceIds == null || resourceIds.isEmpty()) {
            return conflicts;
        }
        List<EmsDispatch> active = this.list(new LambdaQueryWrapper<EmsDispatch>()
                .in(EmsDispatch::getStatus, "已派单", "采样执行中"));
        for (EmsDispatch d : active) {
            if (d.getPlanStart() == null || d.getPlanEnd() == null) continue;
            // 同一批次内的派单互相排除，不做资源冲突判定
            if (excludeOrderIds != null && d.getOrderId() != null && excludeOrderIds.contains(d.getOrderId())) continue;
            boolean overlap = planStart.isBefore(d.getPlanEnd()) && planEnd.isAfter(d.getPlanStart());
            if (!overlap) continue;
            String start = d.getPlanStart().toLocalDate().toString();
            String end = d.getPlanEnd().toLocalDate().toString();
            String orderNo = "-";
            if (d.getOrderId() != null) {
                EmsSamplingOrder o = samplingOrderService.getById(d.getOrderId());
                if (o != null && o.getOrderNo() != null) orderNo = o.getOrderNo();
            }
            // 车辆冲突
            if (d.getVehicleId() != null && resourceIds.contains(-d.getVehicleId())) {
                conflicts.add(conflictItem("车", vehicleName(d.getVehicleId()), start, end, orderNo));
            }
            // 人员冲突（负责人 + 组员）
            for (EmsDispatchMember m : dispatchMemberService.listByDispatch(d.getId())) {
                if (resourceIds.contains(m.getEmpId())) {
                    conflicts.add(conflictItem("人", userName(m.getEmpId()), start, end, orderNo));
                }
            }
            // 设备冲突
            for (EmsDispatchDevice dev : dispatchDeviceService.listByDispatch(d.getId())) {
                if (resourceIds.contains(dev.getInstrumentId())) {
                    conflicts.add(conflictItem("设备", instrumentName(dev.getInstrumentId()), start, end, orderNo));
                }
            }
        }
        return conflicts;
    }

    private Map<String, Object> conflictItem(String type, String name, String start, String end, String orderNo) {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("type", type);
        m.put("name", name);
        m.put("start", start);
        m.put("end", end);
        m.put("orderNo", orderNo);
        return m;
    }

    private String vehicleName(Long vehicleId) {
        EmsVehicle v = vehicleService.getById(vehicleId);
        return v != null ? (v.getPlateNo() != null ? v.getPlateNo() : String.valueOf(vehicleId)) : String.valueOf(vehicleId);
    }

    private String userName(Long empId) {
        User u = userService.getUser(empId);
        return u != null ? (u.getRealName() != null ? u.getRealName() : u.getUsername()) : String.valueOf(empId);
    }

    private String instrumentName(Long instrumentId) {
        EmsInstrument ins = instrumentService.getById(instrumentId);
        return ins != null ? (ins.getName() != null ? ins.getName() : String.valueOf(instrumentId)) : String.valueOf(instrumentId);
    }

    /** 生成详细冲突提示文本 */
    private String buildConflictMessage(List<Map<String, Object>> conflicts) {
        StringBuilder sb = new StringBuilder("资源冲突(BR-023-04)：");
        for (Map<String, Object> c : conflicts) {
            sb.append("\n")
              .append(c.get("name")).append(c.get("type"))
              .append(" 在时间 ").append(c.get("start")).append("-").append(c.get("end"))
              .append(" 已存在任务 编号：").append(c.get("orderNo"));
        }
        return sb.toString();
    }

    /** 查询派单详情（TRD 5.2）：聚合订单、派单主表、负责人/组员、车辆、设备 */
    public EmsDispatchDetailVO getDispatchDetail(Long orderId) {
        EmsDispatchDetailVO vo = new EmsDispatchDetailVO();
        vo.setOrderId(orderId);
        EmsSamplingOrder order = samplingOrderService.getById(orderId);
        if (order != null) {
            vo.setOrderNo(order.getOrderNo());
            vo.setOrderStatus(order.getStatus());
            vo.setCreateBy(order.getCreateBy());
            vo.setCreateName(order.getCreateName());
            vo.setOrderCreateTime(order.getCreateTime());
            vo.setEntrustId(order.getEntrustId());
            if (order.getEntrustId() != null) {
                EmsEntrust entrust = entrustService.getById(order.getEntrustId());
                if (entrust != null) {
                    vo.setEntrustNo(entrust.getEntrustNo());
                    vo.setEntrustName(entrust.getEntrustName());
                    vo.setEntrustStatus(entrust.getStatus());
                }
                int pc = (int) monitorPointService.count(
                        new LambdaQueryWrapper<EmsMonitorPoint>().eq(EmsMonitorPoint::getEntrustId, order.getEntrustId()));
                vo.setPointCount(pc);
            }
        }
        // 任务关联样品（样品登记回写 t_sample.order_id，未派单时也可能存在手动收集样品）
        List<EmsDispatchDetailVO.SampleInfo> sampleList = new java.util.ArrayList<>();
        for (EmsSample s : sampleMapper.selectList(new LambdaQueryWrapper<EmsSample>()
                .eq(EmsSample::getOrderId, orderId).orderByAsc(EmsSample::getId))) {
            EmsDispatchDetailVO.SampleInfo si = new EmsDispatchDetailVO.SampleInfo();
            si.setId(s.getId());
            si.setBarcode(s.getBarcode());
            si.setSampleNo(s.getSampleNo());
            si.setName(s.getName());
            si.setCategory(s.getCategory());
            si.setItem(s.getItem());
            si.setType(s.getType());
            si.setStatus(s.getStatus());
            si.setSampler(s.getSampler());
            si.setSampleTime(s.getSampleTime());
            si.setReceiveBy(s.getReceiveBy());
            si.setReceiveTime(s.getReceiveTime());
            sampleList.add(si);
        }
        vo.setSamples(sampleList);
        EmsDispatch dispatch = this.getOne(new LambdaQueryWrapper<EmsDispatch>()
                .eq(EmsDispatch::getOrderId, orderId)
                .orderByDesc(EmsDispatch::getCreateTime)
                .last("LIMIT 1"));
        if (dispatch == null) {
            return vo;
        }
        vo.setDispatchId(dispatch.getId());
        vo.setDispatchTime(dispatch.getDispatchTime());
        vo.setPlanStart(dispatch.getPlanStart());
        vo.setPlanEnd(dispatch.getPlanEnd());
        vo.setNote(dispatch.getNote());

        if (dispatch.getVehicleId() != null) {
            EmsVehicle vehicle = vehicleService.getById(dispatch.getVehicleId());
            if (vehicle != null) {
                EmsDispatchDetailVO.VehicleInfo vi = new EmsDispatchDetailVO.VehicleInfo();
                vi.setId(vehicle.getId());
                vi.setPlateNo(vehicle.getPlateNo());
                vi.setModel(vehicle.getModel());
                vo.setVehicle(vi);
            }
        }

        List<EmsDispatchMember> members = dispatchMemberService.listByDispatch(dispatch.getId());
        List<EmsDispatchDetailVO.MemberInfo> memberList = new java.util.ArrayList<>();
        for (EmsDispatchMember m : members) {
            EmsDispatchDetailVO.MemberInfo mi = new EmsDispatchDetailVO.MemberInfo();
            mi.setUserId(m.getEmpId());
            mi.setRole(m.getRole());
            com.flow.engine.entity.User u = userService.getUser(m.getEmpId());
            if (u != null) {
                mi.setRealName(u.getRealName());
                mi.setUsername(u.getUsername());
            }
            // 人员资质名称列表（展示格式：姓名(账号)-[资质1、资质2]）
            List<String> quals = userService.listQualifications(m.getEmpId()).stream()
                    .map(com.flow.engine.entity.UserQualification::getQualName)
                    .filter(q -> q != null && !q.isBlank())
                    .collect(java.util.stream.Collectors.toList());
            mi.setQualNames(quals);
            if ("LEAD".equals(m.getRole())) {
                vo.setLead(mi);
            } else {
                memberList.add(mi);
            }
        }
        vo.setMembers(memberList);

        List<EmsDispatchDevice> devices = dispatchDeviceService.listByDispatch(dispatch.getId());
        List<EmsDispatchDetailVO.InstrumentInfo> insList = new java.util.ArrayList<>();
        for (EmsDispatchDevice dev : devices) {
            EmsInstrument ins = instrumentService.getById(dev.getInstrumentId());
            if (ins == null) continue;
            EmsDispatchDetailVO.InstrumentInfo ii = new EmsDispatchDetailVO.InstrumentInfo();
            ii.setId(ins.getId());
            ii.setCode(ins.getCode());
            ii.setName(ins.getName());
            ii.setModel(ins.getModel());
            ii.setCalibDue(ins.getCalibDue() == null ? null : ins.getCalibDue().toString());
            insList.add(ii);
        }
        vo.setInstruments(insList);

        return vo;
    }

    /**
     * 车辆使用日历（ISSUE-035）：查询每辆车在指定时间范围内的占用区间。
     * 占用来源为已派单/采样执行中的派单记录（vehicleId + planStart/planEnd）。
     * @param rangeStart 查询范围开始（可为空，默认从最早派单起）
     * @param rangeEnd   查询范围结束（可为空，默认一年窗口）
     * @return 每辆车的占用区间列表（含派单号、状态、起止时间）
     */
    public List<Map<String, Object>> getVehicleUsage(LocalDateTime rangeStart, LocalDateTime rangeEnd, Long onlyVehicleId) {
        // 1. 取时间范围内相关的派单（仅含车辆且有明确时间区间）
        LambdaQueryWrapper<EmsDispatch> q = new LambdaQueryWrapper<>();
        q.in(EmsDispatch::getStatus, "已派单", "采样执行中");
        q.isNotNull(EmsDispatch::getVehicleId);
        q.isNotNull(EmsDispatch::getPlanStart);
        q.isNotNull(EmsDispatch::getPlanEnd);
        if (rangeStart != null) {
            // 派单结束时间晚于范围开始，才可能重叠
            q.ge(EmsDispatch::getPlanEnd, rangeStart);
        }
        if (rangeEnd != null) {
            q.le(EmsDispatch::getPlanStart, rangeEnd);
        }
        if (onlyVehicleId != null) {
            q.eq(EmsDispatch::getVehicleId, onlyVehicleId);
        }
        q.orderByAsc(EmsDispatch::getPlanStart);
        List<EmsDispatch> dispatches = this.list(q);

        // 2. 聚合到车辆维度
        Map<Long, Map<String, Object>> vehicleMap = new java.util.LinkedHashMap<>();
        // 引入车辆台账，补充车辆名称
        List<EmsVehicle> vehicles = (onlyVehicleId != null)
                ? vehicleService.listByIds(java.util.Collections.singletonList(onlyVehicleId))
                : vehicleService.list();
        for (EmsVehicle v : vehicles) {
            Map<String, Object> vm = new java.util.LinkedHashMap<>();
            vm.put("vehicleId", v.getId());
            vm.put("plateNo", v.getPlateNo());
            vm.put("model", v.getModel());
            vm.put("status", v.getStatus());
            vm.put("ranges", new java.util.ArrayList<Map<String, Object>>());
            vm.put("maintenances", new java.util.ArrayList<Map<String, Object>>());
            vehicleMap.put(v.getId(), vm);
        }

        for (EmsDispatch d : dispatches) {
            Map<String, Object> vm = vehicleMap.get(d.getVehicleId());
            if (vm == null) {
                // 车辆可能已被删除，仍展示占用记录
                EmsVehicle v = vehicleService.getById(d.getVehicleId());
                vm = new java.util.LinkedHashMap<>();
                vm.put("vehicleId", d.getVehicleId());
                vm.put("plateNo", v != null ? v.getPlateNo() : ("车辆#" + d.getVehicleId()));
                vm.put("model", v != null ? v.getModel() : null);
                vm.put("status", v != null ? v.getStatus() : null);
                vm.put("ranges", new java.util.ArrayList<Map<String, Object>>());
                vm.put("maintenances", new java.util.ArrayList<Map<String, Object>>());
                vehicleMap.put(d.getVehicleId(), vm);
            }
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> ranges = (List<Map<String, Object>>) vm.get("ranges");
            Map<String, Object> r = new java.util.LinkedHashMap<>();
            r.put("dispatchId", d.getId());
            r.put("orderId", d.getOrderId());
            r.put("status", d.getStatus());
            r.put("start", d.getPlanStart());
            r.put("end", d.getPlanEnd());
            ranges.add(r);
        }

        // 3. 维修保养占用（ISSUE-036）：与派单同维度聚合
        List<EmsVehicleMaintenance> maints = vehicleService.listMaintenancesInRange(rangeStart, rangeEnd, onlyVehicleId);
        for (EmsVehicleMaintenance m : maints) {
            Map<String, Object> vm = vehicleMap.get(m.getVehicleId());
            if (vm == null) {
                EmsVehicle v = vehicleService.getById(m.getVehicleId());
                vm = new java.util.LinkedHashMap<>();
                vm.put("vehicleId", m.getVehicleId());
                vm.put("plateNo", v != null ? v.getPlateNo() : ("车辆#" + m.getVehicleId()));
                vm.put("model", v != null ? v.getModel() : null);
                vm.put("status", v != null ? v.getStatus() : null);
                vm.put("ranges", new java.util.ArrayList<Map<String, Object>>());
                vm.put("maintenances", new java.util.ArrayList<Map<String, Object>>());
                vehicleMap.put(m.getVehicleId(), vm);
            }
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> ms = (List<Map<String, Object>>) vm.get("maintenances");
            Map<String, Object> mm = new java.util.LinkedHashMap<>();
            mm.put("id", m.getId());
            mm.put("type", m.getMaintType());
            mm.put("start", m.getStartDate());
            mm.put("end", m.getEndDate());
            ms.add(mm);
        }

        return new java.util.ArrayList<>(vehicleMap.values());
    }

    /**
     * 设备使用日历：查询每台仪器在指定时间范围内的占用区间。
     * 占用来源为已派单/采样执行中的派单记录（经 t_dispatch_device 关联 instrumentId + planStart/planEnd），
     * 另聚合校准占用（t_instrument_calib 的 calib_date ~ calib_due 视为校准停用期）。
     * @param rangeStart 查询范围开始
     * @param rangeEnd   查询范围结束
     * @param onlyInstrumentId 仅查指定设备（可为 null 查全部）
     * @return 每台仪器的占用区间列表（含派单号、状态、起止时间）与校准占用
     */
    public List<Map<String, Object>> getInstrumentUsage(LocalDateTime rangeStart, LocalDateTime rangeEnd, Long onlyInstrumentId) {
        // 1. 取时间范围内相关的派单（仅含明确时间区间）
        LambdaQueryWrapper<EmsDispatch> q = new LambdaQueryWrapper<>();
        q.in(EmsDispatch::getStatus, "已派单", "采样执行中");
        q.isNotNull(EmsDispatch::getPlanStart);
        q.isNotNull(EmsDispatch::getPlanEnd);
        if (rangeStart != null) q.ge(EmsDispatch::getPlanEnd, rangeStart);
        if (rangeEnd != null) q.le(EmsDispatch::getPlanStart, rangeEnd);
        q.orderByAsc(EmsDispatch::getPlanStart);
        List<EmsDispatch> dispatches = this.list(q);

        // 2. 聚合到设备维度
        Map<Long, Map<String, Object>> instMap = new java.util.LinkedHashMap<>();
        List<EmsInstrument> instruments = (onlyInstrumentId != null)
                ? instrumentService.listByIds(java.util.Collections.singletonList(onlyInstrumentId))
                : instrumentService.list();
        for (EmsInstrument ins : instruments) {
            Map<String, Object> vm = new java.util.LinkedHashMap<>();
            vm.put("instrumentId", ins.getId());
            vm.put("code", ins.getCode());
            vm.put("name", ins.getName());
            vm.put("model", ins.getModel());
            vm.put("status", ins.getStatus());
            vm.put("ranges", new java.util.ArrayList<Map<String, Object>>());
            vm.put("maintenances", new java.util.ArrayList<Map<String, Object>>());
            instMap.put(ins.getId(), vm);
        }

        // 3. 经 t_dispatch_device 关联将派单占用挂到对应设备
        for (EmsDispatch d : dispatches) {
            List<EmsDispatchDevice> devs = dispatchDeviceService.listByDispatch(d.getId());
            for (EmsDispatchDevice dev : devs) {
                Map<String, Object> vm = instMap.get(dev.getInstrumentId());
                if (vm == null) {
                    EmsInstrument ins = instrumentService.getById(dev.getInstrumentId());
                    vm = new java.util.LinkedHashMap<>();
                    vm.put("instrumentId", dev.getInstrumentId());
                    vm.put("code", ins != null ? ins.getCode() : null);
                    vm.put("name", ins != null ? ins.getName() : ("设备#" + dev.getInstrumentId()));
                    vm.put("model", ins != null ? ins.getModel() : null);
                    vm.put("status", ins != null ? ins.getStatus() : null);
                    vm.put("ranges", new java.util.ArrayList<Map<String, Object>>());
                    vm.put("maintenances", new java.util.ArrayList<Map<String, Object>>());
                    instMap.put(dev.getInstrumentId(), vm);
                }
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> ranges = (List<Map<String, Object>>) vm.get("ranges");
                Map<String, Object> r = new java.util.LinkedHashMap<>();
                r.put("dispatchId", d.getId());
                r.put("orderId", d.getOrderId());
                r.put("status", d.getStatus());
                r.put("start", d.getPlanStart());
                r.put("end", d.getPlanEnd());
                ranges.add(r);
            }
        }

        // 4. 校准占用（t_instrument_calib：calib_date ~ calib_due 视为校准停用期）
        if (jdbcTemplate != null) {
            StringBuilder sql = new StringBuilder(
                    "SELECT instrument_id, calib_date, calib_due, cert_no FROM t_instrument_calib WHERE 1=1");
            List<Object> args = new java.util.ArrayList<>();
            if (rangeStart != null) { sql.append(" AND calib_due >= ?"); args.add(rangeStart.toLocalDate().toString()); }
            if (rangeEnd != null) { sql.append(" AND calib_date <= ?"); args.add(rangeEnd.toLocalDate().toString()); }
            if (onlyInstrumentId != null) { sql.append(" AND instrument_id = ?"); args.add(onlyInstrumentId); }
            try {
                List<Map<String, Object>> calibRows = jdbcTemplate.queryForList(sql.toString(), args.toArray());
                for (Map<String, Object> row : calibRows) {
                    Long iid = row.get("instrument_id") != null ? ((Number) row.get("instrument_id")).longValue() : null;
                    if (iid == null) continue;
                    Map<String, Object> vm = instMap.get(iid);
                    if (vm == null) {
                        EmsInstrument ins = instrumentService.getById(iid);
                        vm = new java.util.LinkedHashMap<>();
                        vm.put("instrumentId", iid);
                        vm.put("code", ins != null ? ins.getCode() : null);
                        vm.put("name", ins != null ? ins.getName() : ("设备#" + iid));
                        vm.put("model", ins != null ? ins.getModel() : null);
                        vm.put("status", ins != null ? ins.getStatus() : null);
                        vm.put("ranges", new java.util.ArrayList<Map<String, Object>>());
                        vm.put("maintenances", new java.util.ArrayList<Map<String, Object>>());
                        instMap.put(iid, vm);
                    }
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> ms = (List<Map<String, Object>>) vm.get("maintenances");
                    Map<String, Object> mm = new java.util.LinkedHashMap<>();
                    mm.put("id", iid + "_" + row.get("calib_date"));
                    mm.put("type", "校准");
                    mm.put("start", toLocalDateTime(row.get("calib_date")));
                    mm.put("end", toLocalDateTime(row.get("calib_due")));
                    mm.put("certNo", row.get("cert_no"));
                    ms.add(mm);
                }
            } catch (Exception ignored) { /* 校准表缺失时忽略 */ }
        }

        return new java.util.ArrayList<>(instMap.values());
    }

    private LocalDateTime toLocalDateTime(Object v) {
        if (v == null) return null;
        String s = String.valueOf(v);
        if (s.length() <= 10) return java.time.LocalDate.parse(s).atStartOfDay();
        return java.time.LocalDateTime.parse(s.replace(' ', 'T'));
    }

    /**
     * 在给定检车时间区间内，返回可用（未被派单占用、且不在维修保养期）的车辆 id 列表。
     * 若 planStart/planEnd 为空，则返回全部车辆。
     */
    public List<Long> getAvailableVehicles(LocalDateTime planStart, LocalDateTime planEnd) {
        if (planStart == null || planEnd == null) {
            return vehicleService.list().stream().map(EmsVehicle::getId).collect(java.util.stream.Collectors.toList());
        }
        List<Map<String, Object>> usage = getVehicleUsage(planStart, planEnd, null);
        List<Long> available = new java.util.ArrayList<>();
        for (Map<String, Object> vm : usage) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> ranges = (List<Map<String, Object>>) vm.get("ranges");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> maints = (List<Map<String, Object>>) vm.get("maintenances");
            boolean busy = false;
            for (Map<String, Object> r : ranges) {
                LocalDateTime rs = (LocalDateTime) r.get("start");
                LocalDateTime re = (LocalDateTime) r.get("end");
                // 时间区间重叠判定（与 BR-023-04 一致）
                if (planStart.isBefore(re) && planEnd.isAfter(rs)) {
                    busy = true;
                    break;
                }
            }
            if (!busy) {
                for (Map<String, Object> mt : maints) {
                    LocalDateTime ms = (LocalDateTime) mt.get("start");
                    LocalDateTime me = (LocalDateTime) mt.get("end");
                    if (planStart.isBefore(me) && planEnd.isAfter(ms)) {
                        busy = true;
                        break;
                    }
                }
            }
            if (!busy) available.add((Long) vm.get("vehicleId"));
        }
        return available;
    }

    /** 查询某辆车的派单记录（聚合订单号/状态/起止，ISSUE-036 车辆详情用） */
    public List<Map<String, Object>> listByVehicle(Long vehicleId) {
        LambdaQueryWrapper<EmsDispatch> q = new LambdaQueryWrapper<>();
        q.eq(EmsDispatch::getVehicleId, vehicleId);
        q.orderByDesc(EmsDispatch::getPlanStart);
        List<EmsDispatch> list = this.list(q);
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (EmsDispatch d : list) {
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("dispatchId", d.getId());
            m.put("orderId", d.getOrderId());
            m.put("status", d.getStatus());
            m.put("planStart", d.getPlanStart());
            m.put("planEnd", d.getPlanEnd());
            m.put("dispatchTime", d.getDispatchTime());
            // 派单编号（订单号）+ 委托单名称，避免仅展示 Id 数值
            String orderNo = "-";
            String entrustName = "-";
            if (d.getOrderId() != null) {
                EmsSamplingOrder order = samplingOrderService.getById(d.getOrderId());
                if (order != null) {
                    orderNo = order.getOrderNo() != null ? order.getOrderNo() : "-";
                    if (order.getEntrustId() != null) {
                        EmsEntrust entrust = entrustService.getById(order.getEntrustId());
                        if (entrust != null) {
                            entrustName = entrust.getEntrustName() != null ? entrust.getEntrustName() : "-";
                        }
                    }
                }
            }
            m.put("orderNo", orderNo);
            m.put("entrustName", entrustName);
            result.add(m);
        }
        return result;
    }

    /** 车辆详情（ISSUE-036）：基本信息 + 派单记录 + 维修保养记录 */
    public Map<String, Object> getVehicleDetail(Long vehicleId) {
        Map<String, Object> detail = vehicleService.getDetail(vehicleId);
        List<Map<String, Object>> dispatches = listByVehicle(vehicleId);
        detail.put("dispatches", dispatches);
        return detail;
    }

    /**
     * 清空全部派单记录：主表 + 设备关联表 + 人员关联表（物理删除，谨慎调用）。
     * 仅清除派单数据，不影响订单/车辆/人员/设备等基础数据。
     * @return 清除的派单主表记录数
     */
    @Transactional
    public int clearAll() {
        // 先删从表，再删主表，避免外键/关联悬挂
        dispatchDeviceService.remove(null);
        dispatchMemberService.remove(null);
        int count = (int) this.count();
        this.remove(null);
        return count;
    }

    /** BR-023-03 资质闸门：人员为有效后台用户 + 设备校准有效期未过期（物资闸门） */
    private void gateCheck(Long leadId, List<Long> empIds, List<Long> instrumentIds) {
        java.util.List<Long> allEmp = new java.util.ArrayList<>();
        if (leadId != null) allEmp.add(leadId);
        if (empIds != null) allEmp.addAll(empIds);
        for (Long id : allEmp) {
            com.flow.engine.entity.User u = userService.getUser(id);
            if (u == null) throw new BusinessException("人员不存在: " + id);
            if (u.getStatus() == null || u.getStatus() != 1) {
                throw new BusinessException("人员状态非正常，阻断派单(BR-023-03): " + (u.getRealName() != null ? u.getRealName() : u.getUsername()));
            }
        }
        if (instrumentIds != null) {
            for (Long id : instrumentIds) {
                EmsInstrument ins = instrumentService.getById(id);
                if (ins == null) throw new BusinessException("设备不存在: " + id);
                // 停用/维修/报废 不允许派单；校准过期强制拦截
                if ("停用".equals(ins.getStatus()) || "维修".equals(ins.getStatus()) || "报废".equals(ins.getStatus())) {
                    throw new BusinessException("设备状态不可用，阻断派单(BR-023-03): " + ins.getName());
                }
                if (ins.getCalibDue() != null && ins.getCalibDue().isBefore(java.time.LocalDate.now())) {
                    throw new BusinessException("设备校准已过期，阻断派单(BR-023-03): " + ins.getName());
                }
            }
        }
    }
}
