package com.flow.engine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.flow.engine.dto.EmsInstrumentDetailVO;
import com.flow.engine.entity.EmsDispatch;
import com.flow.engine.entity.EmsDispatchDevice;
import com.flow.engine.entity.EmsInstrument;
import com.flow.engine.entity.EmsInstrumentCalib;
import com.flow.engine.entity.EmsSamplingOrder;
import com.flow.engine.mapper.EmsInstrumentMapper;
import com.flow.engine.service.EmsDispatchService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 环境监测 - 采样设备/仪器服务（TRD 5.5 仪器设备全生命周期台账）
 * 状态机：在用 → 校准临期 →（到期）强制停用 → 校准/维修 → 恢复在用 / 报废
 */
@Service
public class EmsInstrumentService extends ServiceImpl<EmsInstrumentMapper, EmsInstrument> {

    /** 校准临期阈值（天），TRD 5.5.2 三级预警起点 */
    private static final int CALIB_WARN_DAYS = 30;

    @Autowired
    private EmsInstrumentCalibService calibService;
    @Autowired
    private EmsDispatchDeviceService dispatchDeviceService;
    @Lazy
    @Autowired
    private EmsDispatchService dispatchService;
    @Autowired
    private EmsSamplingOrderService samplingOrderService;

    /**
     * 根据校准到期日重算状态（不覆盖手动置的维修/报废）：
     * 校准过期 → 停用（强制停用 BR-023-03 物资闸门）；临期<30天 → 临期；否则 在用。
     */
    private String recalcStatus(EmsInstrument ins) {
        if ("维修".equals(ins.getStatus()) || "报废".equals(ins.getStatus())) {
            return ins.getStatus(); // 人工态保持
        }
        if (ins.getCalibDue() == null) {
            return "在用";
        }
        LocalDate now = LocalDate.now();
        if (ins.getCalibDue().isBefore(now)) {
            return "停用"; // 过期强制停用
        }
        if (ins.getCalibDue().isBefore(now.plusDays(CALIB_WARN_DAYS))) {
            return "临期"; // 校准临期
        }
        return "在用";
    }

    public EmsInstrument create(EmsInstrument ins) {
        if (!StringUtils.hasText(ins.getName())) {
            throw new IllegalArgumentException("仪器名称不能为空");
        }
        ins.setStatus(recalcStatus(ins));
        ins.setCreateTime(LocalDateTime.now());
        ins.setUpdateTime(LocalDateTime.now());
        this.save(ins);
        return ins;
    }

    public EmsInstrument updateInstrument(Long id, EmsInstrument ins) {
        EmsInstrument exist = getById(id);
        if (exist == null) throw new IllegalArgumentException("设备不存在: " + id);
        ins.setId(id);
        ins.setStatus(recalcStatus(ins));
        ins.setUpdateTime(LocalDateTime.now());
        this.updateById(ins);
        return ins;
    }

    /** 校准登记（TRD 5.5.5 POST /instrument/{id}/calibrate） */
    public EmsInstrument calibrate(Long id, LocalDate calibDate, LocalDate calibDue, String certNo) {
        EmsInstrument exist = getById(id);
        if (exist == null) throw new IllegalArgumentException("设备不存在: " + id);
        exist.setLastCalibDate(calibDate == null ? LocalDate.now() : calibDate);
        if (calibDue != null) exist.setCalibDue(calibDue);
        if (StringUtils.hasText(certNo)) exist.setCertNo(certNo);
        exist.setStatus(recalcStatus(exist)); // 登记后按新到期日重算
        exist.setUpdateTime(LocalDateTime.now());
        this.updateById(exist);
        // 写入校准历史台账
        calibService.record(id, exist.getLastCalibDate(), exist.getCalibDue(), exist.getCertNo());
        return exist;
    }

    /** 设备详情（TRD 5.5）：基本信息 + 校准记录 + 关联采样任务 */
    public EmsInstrumentDetailVO getDetail(Long id) {
        EmsInstrument exist = getById(id);
        if (exist == null) return null;
        EmsInstrumentDetailVO vo = new EmsInstrumentDetailVO();
        BeanUtils.copyProperties(exist, vo);

        // 校准记录
        List<EmsInstrumentCalib> calibs = calibService.listByInstrument(id);
        List<EmsInstrumentDetailVO.CalibRecord> calibList = new ArrayList<>();
        for (EmsInstrumentCalib c : calibs) {
            EmsInstrumentDetailVO.CalibRecord r = new EmsInstrumentDetailVO.CalibRecord();
            r.setCalibDate(c.getCalibDate() == null ? null : c.getCalibDate().toString());
            r.setCalibDue(c.getCalibDue() == null ? null : c.getCalibDue().toString());
            r.setCertNo(c.getCertNo());
            r.setCreateTime(c.getCreateTime() == null ? null : c.getCreateTime().toString());
            calibList.add(r);
        }
        vo.setCalibRecords(calibList);

        // 关联采样任务：设备 -> 派单设备关联 -> 派单 -> 采样订单
        List<EmsDispatchDevice> links = dispatchDeviceService.list(
                new LambdaQueryWrapper<EmsDispatchDevice>().eq(EmsDispatchDevice::getInstrumentId, id));
        Map<Long, EmsInstrumentDetailVO.SamplingTask> taskMap = new LinkedHashMap<>();
        for (EmsDispatchDevice link : links) {
            if (link.getDispatchId() == null) continue;
            EmsDispatch dispatch = dispatchService.getById(link.getDispatchId());
            if (dispatch == null || dispatch.getOrderId() == null) continue;
            EmsSamplingOrder order = samplingOrderService.getById(dispatch.getOrderId());
            if (order == null) continue;
            if (taskMap.containsKey(order.getId())) continue;
            EmsInstrumentDetailVO.SamplingTask t = new EmsInstrumentDetailVO.SamplingTask();
            t.setOrderId(order.getId());
            t.setOrderNo(order.getOrderNo());
            t.setOrderStatus(order.getStatus());
            t.setPlanStart(dispatch.getPlanStart() == null ? null : dispatch.getPlanStart().toString());
            t.setPlanEnd(dispatch.getPlanEnd() == null ? null : dispatch.getPlanEnd().toString());
            taskMap.put(order.getId(), t);
        }
        vo.setSamplingTasks(new ArrayList<>(taskMap.values()));
        return vo;
    }

    /** 分页检索（关键字=编号/名称/型号） */
    public Page<EmsInstrument> pageSearch(String keyword, String status, int page, int size) {
        LambdaQueryWrapper<EmsInstrument> q = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            q.like(EmsInstrument::getCode, keyword)
             .or().like(EmsInstrument::getName, keyword)
             .or().like(EmsInstrument::getModel, keyword);
        }
        if (StringUtils.hasText(status)) {
            q.eq(EmsInstrument::getStatus, status);
        }
        q.orderByDesc(EmsInstrument::getCreateTime);
        return this.page(new Page<>(page, size), q);
    }

    /** 即将到期/已停用的预警列表（TRD 5.5.2 三级预警） */
    public java.util.List<EmsInstrument> expiringSoon() {
        LocalDate warnLine = LocalDate.now().plusDays(CALIB_WARN_DAYS);
        return this.list(new LambdaQueryWrapper<EmsInstrument>()
                .and(w -> w.le(EmsInstrument::getCalibDue, warnLine)
                           .or().eq(EmsInstrument::getStatus, "停用")));
    }
}
