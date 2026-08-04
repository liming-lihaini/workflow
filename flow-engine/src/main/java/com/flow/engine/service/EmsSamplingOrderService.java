package com.flow.engine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.flow.engine.entity.EmsDispatch;
import com.flow.engine.entity.EmsEntrust;
import com.flow.engine.entity.EmsMonitorPoint;
import com.flow.engine.entity.EmsSamplingOrder;
import com.flow.engine.entity.User;
import com.flow.engine.mapper.EmsDispatchMapper;
import com.flow.engine.mapper.EmsSamplingOrderMapper;
import com.flow.engine.mapper.UserMapper;
import com.flow.engine.util.CodeGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 环境监测 - 采样订单服务（TRD 5.1+5.2，BR-023-02 拆单）
 * 复用 EmsMonitorPointService 按客户的监测点位拆单：每个点位生成一张「待派单」订单。
 */
@Service
public class EmsSamplingOrderService extends ServiceImpl<EmsSamplingOrderMapper, EmsSamplingOrder> {

    @Autowired
    private EmsMonitorPointService monitorPointService;
    @Autowired
    private EmsDispatchMapper dispatchMapper;
    @Autowired
    private UserMapper userMapper;

    /**
     * 采样调度看板聚合数据：每个订单补充点位名称、派单计划区间、派单负责人姓名。
     * 避免与 EmsDispatchService 互相依赖，这里直接通过 Mapper 轻量查询。
     */
    public List<Map<String, Object>> listDispatchBoard(String orderNo, String leadName, String status) {
        List<EmsSamplingOrder> orders = this.list(new LambdaQueryWrapper<EmsSamplingOrder>().orderByDesc(EmsSamplingOrder::getId));
        List<Map<String, Object>> result = new ArrayList<>();
        boolean hasFilter = (orderNo != null && !orderNo.trim().isEmpty())
                || (leadName != null && !leadName.trim().isEmpty())
                || (status != null && !status.trim().isEmpty());
        for (EmsSamplingOrder o : orders) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", o.getId());
            m.put("orderNo", o.getOrderNo());
            m.put("status", o.getStatus());
            m.put("entrustId", o.getEntrustId());
            // 点位名称
            String pointName = "—";
            if (o.getPointId() != null) {
                EmsMonitorPoint p = monitorPointService.getById(o.getPointId());
                if (p != null && p.getPointName() != null) pointName = p.getPointName();
            }
            m.put("pointName", pointName);
            m.put("pointId", o.getPointId());
            // 派单计划区间 + 负责人（取该订单最新一条派单）
            String planStart = null;
            String planEnd = null;
            String leadNameVal = null;
            if (o.getId() != null) {
                EmsDispatch d = dispatchMapper.selectOne(new LambdaQueryWrapper<EmsDispatch>()
                        .eq(EmsDispatch::getOrderId, o.getId())
                        .orderByDesc(EmsDispatch::getId)
                        .last("LIMIT 1"));
                if (d != null) {
                    if (d.getPlanStart() != null) planStart = d.getPlanStart().toLocalDate().toString();
                    if (d.getPlanEnd() != null) planEnd = d.getPlanEnd().toLocalDate().toString();
                    // 负责人：派单成员表 role=LEAD 对应的后台人员姓名（sys_user）
                    User lead = userMapper.selectOne(new LambdaQueryWrapper<User>()
                            .inSql(User::getId, "SELECT emp_id FROM t_dispatch_member WHERE dispatch_id = " + d.getId() + " AND role = 'LEAD'")
                            .last("LIMIT 1"));
                    if (lead != null) leadNameVal = lead.getRealName() != null ? lead.getRealName() : lead.getUsername();
                }
            }
            m.put("planStart", planStart);
            m.put("planEnd", planEnd);
            m.put("planRange", (planStart != null ? planStart : "—") + " ~ " + (planEnd != null ? planEnd : "—"));
            m.put("leadName", leadNameVal != null ? leadNameVal : "—");
            // 条件过滤（订单号/负责人/状态）
            if (hasFilter) {
                if (orderNo != null && !orderNo.trim().isEmpty() && (o.getOrderNo() == null || !o.getOrderNo().contains(orderNo.trim()))) {
                    continue;
                }
                if (status != null && !status.trim().isEmpty() && !status.trim().equals(o.getStatus())) {
                    continue;
                }
                if (leadName != null && !leadName.trim().isEmpty()) {
                    String ln = leadNameVal != null ? leadNameVal : "";
                    if (!ln.contains(leadName.trim())) {
                        continue;
                    }
                }
            }
            result.add(m);
        }
        return result;
    }

    /**
     * 批量派单：对同一组派单信息（负责人/组员/车辆/设备/计划区间/备注）依次派发到多个订单。
     * 每个订单复用 dispatch 的资质闸门与冲突校验；收集成功与失败结果返回，便于前端逐条提示。
     * @return 含 successIds（成功订单 id）、failList（失败：订单 id + 原因）的明细
     */
    public Map<String, Object> batchDispatch(List<Long> orderIds, Long vehicleId, Long leadId,
                                             List<Long> empIds, List<Long> instrumentIds,
                                             LocalDateTime planStart, LocalDateTime planEnd, String note,
                                             EmsDispatchService dispatchService) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Long> successIds = new ArrayList<>();
        List<Map<String, Object>> failList = new ArrayList<>();
        // 同一批次内的派单互相排除资源冲突校验：本批次全部订单 id 作为排除集
        java.util.Set<Long> batchOrderIds = new java.util.LinkedHashSet<>(orderIds);
        for (Long orderId : orderIds) {
            try {
                dispatchService.dispatch(orderId, vehicleId, leadId, empIds, instrumentIds, planStart, planEnd, note, batchOrderIds);
                successIds.add(orderId);
            } catch (Exception e) {
                Map<String, Object> fail = new LinkedHashMap<>();
                fail.put("orderId", orderId);
                fail.put("reason", e.getMessage());
                failList.add(fail);
            }
        }
        result.put("successIds", successIds);
        result.put("failList", failList);
        result.put("total", orderIds.size());
        result.put("successCount", successIds.size());
        result.put("failCount", failList.size());
        return result;
    }


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
