package com.flow.engine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.flow.engine.entity.EmsVehicle;
import com.flow.engine.entity.EmsVehicleMaintenance;
import com.flow.engine.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 环境监测 - 采样车辆服务（TRD 4.4 共享实体 / 车辆台账）
 * 车辆状态：1-可用 2-占用 3-维修保养中（字典 moni_vehicle_status）
 */
@Service
public class EmsVehicleService extends ServiceImpl<EmsVehicleMapper, EmsVehicle> {

    @Autowired
    private EmsVehicleMaintenanceMapper maintenanceMapper;

    public EmsVehicle create(EmsVehicle v) {
        if (!StringUtils.hasText(v.getPlateNo())) {
            throw new IllegalArgumentException("车牌号不能为空");
        }
        v.setStatus(v.getStatus() == null ? 1 : v.getStatus());
        v.setCreateTime(LocalDateTime.now());
        v.setUpdateTime(LocalDateTime.now());
        this.save(v);
        return v;
    }

    public EmsVehicle updateVehicle(Long id, EmsVehicle v) {
        EmsVehicle exist = getById(id);
        if (exist == null) throw new IllegalArgumentException("车辆不存在: " + id);
        v.setId(id);
        v.setUpdateTime(LocalDateTime.now());
        this.updateById(v);
        return v;
    }

    /** 分页检索（支持车牌/型号关键字） */
    public Page<EmsVehicle> pageSearch(String keyword, int page, int size) {
        LambdaQueryWrapper<EmsVehicle> q = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            q.like(EmsVehicle::getPlateNo, keyword).or().like(EmsVehicle::getModel, keyword);
        }
        q.orderByDesc(EmsVehicle::getCreateTime);
        return this.page(new Page<>(page, size), q);
    }

    // ============ 维修保养（ISSUE-036）============

    /** 新增维修保养记录，并同步车辆状态（在途→维修保养中） */
    @org.springframework.transaction.annotation.Transactional
    public EmsVehicleMaintenance createMaintenance(EmsVehicleMaintenance m) {
        if (m.getVehicleId() == null) throw new IllegalArgumentException("车辆不能为空");
        if (m.getStartDate() == null || m.getEndDate() == null) {
            throw new IllegalArgumentException("请填写开始与结束日期");
        }
        if (!m.getEndDate().isAfter(m.getStartDate())) {
            throw new IllegalArgumentException("结束日期须晚于开始日期");
        }
        m.setCreateTime(LocalDateTime.now());
        m.setUpdateTime(LocalDateTime.now());
        maintenanceMapper.insert(m);
        syncStatus(m.getVehicleId());
        return m;
    }

    public List<EmsVehicleMaintenance> listMaintenances(Long vehicleId) {
        LambdaQueryWrapper<EmsVehicleMaintenance> q = new LambdaQueryWrapper<>();
        q.eq(EmsVehicleMaintenance::getVehicleId, vehicleId);
        q.orderByDesc(EmsVehicleMaintenance::getStartDate);
        return maintenanceMapper.selectList(q);
    }

    /** 查询时间范围内（[start,end] 与保养区间重叠）的维修保养记录（ISSUE-035 日历用） */
    public List<EmsVehicleMaintenance> listMaintenancesInRange(LocalDateTime start, LocalDateTime end, Long onlyVehicleId) {
        LambdaQueryWrapper<EmsVehicleMaintenance> q = new LambdaQueryWrapper<>();
        if (start != null) q.ge(EmsVehicleMaintenance::getEndDate, start);
        if (end != null) q.le(EmsVehicleMaintenance::getStartDate, end);
        if (onlyVehicleId != null) q.eq(EmsVehicleMaintenance::getVehicleId, onlyVehicleId);
        q.orderByAsc(EmsVehicleMaintenance::getStartDate);
        return maintenanceMapper.selectList(q);
    }

    /** 删除维修保养记录并恢复车辆状态 */
    @org.springframework.transaction.annotation.Transactional
    public void deleteMaintenance(Long id) {
        EmsVehicleMaintenance m = maintenanceMapper.selectById(id);
        if (m == null) return;
        maintenanceMapper.deleteById(id);
        syncStatus(m.getVehicleId());
    }

    /** 判断某车在 [start,end] 区间是否处于维修保养中（与派单区间重叠即不可派单） */
    public boolean isUnderMaintenance(Long vehicleId, LocalDateTime start, LocalDateTime end) {
        if (vehicleId == null || start == null || end == null) return false;
        LambdaQueryWrapper<EmsVehicleMaintenance> q = new LambdaQueryWrapper<>();
        q.eq(EmsVehicleMaintenance::getVehicleId, vehicleId);
        q.ge(EmsVehicleMaintenance::getEndDate, start);
        q.le(EmsVehicleMaintenance::getStartDate, end);
        return maintenanceMapper.selectCount(q) > 0;
    }

    /**
     * 同步车辆状态：
     * - 存在进行中的维修保养（start<=now<=end）→ 3 维修保养中
     * - 否则 → 1 可用（占用状态由派单占用另行维护，这里仅清理保养造成的维修态）
     */
    public void syncStatus(Long vehicleId) {
        EmsVehicle v = getById(vehicleId);
        if (v == null) return;
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<EmsVehicleMaintenance> q = new LambdaQueryWrapper<>();
        q.eq(EmsVehicleMaintenance::getVehicleId, vehicleId);
        q.le(EmsVehicleMaintenance::getStartDate, now);
        q.ge(EmsVehicleMaintenance::getEndDate, now);
        boolean inMaint = maintenanceMapper.selectCount(q) > 0;
        Integer target = inMaint ? 3 : 1;
        if (!target.equals(v.getStatus())) {
            v.setStatus(target);
            v.setUpdateTime(now);
            this.updateById(v);
        }
    }

    /**
     * 车辆详情（ISSUE-036）：基本信息 + 派单记录 + 维修保养记录。
     * 列表查询后对 status=3 的车辆惰性校验（保养到期则恢复可用）。
     * 注：详情聚合由 EmsDispatchService.getVehicleDetail 提供，避免与派单服务形成循环依赖。
     */
    public Map<String, Object> getDetail(Long vehicleId) {
        EmsVehicle v = getById(vehicleId);
        if (v == null) throw new IllegalArgumentException("车辆不存在: " + vehicleId);
        // 惰性恢复：保养已到期但状态仍为 3
        if (Integer.valueOf(3).equals(v.getStatus())) syncStatus(vehicleId);
        v = getById(vehicleId);
        List<EmsVehicleMaintenance> maints = listMaintenances(vehicleId);
        Map<String, Object> detail = new java.util.LinkedHashMap<>();
        detail.put("vehicle", v);
        detail.put("maintenances", maints);
        return detail;
    }
}
