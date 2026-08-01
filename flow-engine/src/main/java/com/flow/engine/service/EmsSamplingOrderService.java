package com.flow.engine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.flow.engine.entity.EmsEntrust;
import com.flow.engine.entity.EmsMonitorPoint;
import com.flow.engine.entity.EmsSamplingOrder;
import com.flow.engine.mapper.EmsSamplingOrderMapper;
import com.flow.engine.util.CodeGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 环境监测 - 采样订单服务（TRD 5.1+5.2，BR-023-02 拆单）
 * 复用 EmsMonitorPointService 按客户的监测点位拆单：每个点位生成一张「待派单」订单。
 */
@Service
public class EmsSamplingOrderService extends ServiceImpl<EmsSamplingOrderMapper, EmsSamplingOrder> {

    @Autowired
    private EmsMonitorPointService monitorPointService;

    /** 委托确认后拆单：该委托下每个监测点位生成一张待派单订单 */
    @Transactional
    public int genFromEntrust(EmsEntrust entrust) {
        List<EmsMonitorPoint> points = monitorPointService.listByEntrust(entrust.getId());
        if (points.isEmpty()) {
            // 无点位也允许生成一张仅含委托的订单（BR-023-07 在派单时校验点位）
            points = List.of(new EmsMonitorPoint());
        }
        long seq = this.count() + 1;
        int created = 0;
        for (EmsMonitorPoint p : points) {
            EmsSamplingOrder o = new EmsSamplingOrder();
            o.setOrderNo(CodeGenerator.generate("SO", (int) (seq++)));
            o.setEntrustId(entrust.getId());
            o.setPointId(p.getId());
            o.setStatus("待派单");
            o.setCreateTime(LocalDateTime.now());
            o.setUpdateTime(LocalDateTime.now());
            this.save(o);
            created++;
        }
        return created;
    }

    public List<EmsSamplingOrder> listByStatus(String status) {
        return this.list(new LambdaQueryWrapper<EmsSamplingOrder>().eq(status != null, EmsSamplingOrder::getStatus, status));
    }
}
