package com.flow.engine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.flow.engine.common.BusinessException;
import com.flow.engine.dto.EmsDispatchDetailVO;
import com.flow.engine.entity.*;
import com.flow.engine.mapper.EmsDispatchMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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

    /**
     * 派单：资质闸门 + 冲突检测通过后落库，订单转「已派单」。
     * 人员来源为后台用户(sys_user)，负责人 leadId 标记为 LEAD。
     * @return 派单记录
     */
    @Transactional
    public EmsDispatch dispatch(Long orderId, Long vehicleId, Long leadId, List<Long> empIds, List<Long> instrumentIds,
                                LocalDateTime planStart, LocalDateTime planEnd, String note) {
        EmsSamplingOrder order = samplingOrderService.getById(orderId);
        if (order == null) {
            throw new BusinessException("采样订单不存在");
        }
        if (!"待派单".equals(order.getStatus())) {
            throw new BusinessException("仅待派单状态可派单，当前：" + order.getStatus());
        }
        // BR-023-07 最小集校验
        if (order.getPointId() == null) {
            throw new BusinessException("订单缺少点位，不可派单(BR-023-07)");
        }
        // BR-023-03 资质闸门：人员状态有效 + 设备校准有效期
        gateCheck(leadId, empIds, instrumentIds);
        // BR-023-04 资源冲突检测
        List<Long> resources = new java.util.ArrayList<>();
        if (vehicleId != null) resources.add(-vehicleId); // 车辆以负 id 区分
        if (leadId != null) resources.add(leadId);
        if (empIds != null) resources.addAll(empIds);
        if (instrumentIds != null) resources.addAll(instrumentIds);
        if (hasConflict(planStart, planEnd, resources)) {
            throw new BusinessException("资源冲突：同一人/车/设备在该时间段已被占用(BR-023-04)");
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
        return d;
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

    /** 查询派单详情（TRD 5.2）：聚合订单、派单主表、负责人/组员、车辆、设备 */
    public EmsDispatchDetailVO getDispatchDetail(Long orderId) {
        EmsDispatchDetailVO vo = new EmsDispatchDetailVO();
        vo.setOrderId(orderId);
        EmsSamplingOrder order = samplingOrderService.getById(orderId);
        if (order != null) {
            vo.setOrderNo(order.getOrderNo());
            vo.setOrderStatus(order.getStatus());
        }
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
