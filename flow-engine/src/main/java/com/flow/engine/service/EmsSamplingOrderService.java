package com.flow.engine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.flow.engine.common.BusinessException;
import com.flow.engine.entity.EmsDispatch;
import com.flow.engine.entity.EmsCustomer;
import com.flow.engine.entity.EmsEntrust;
import com.flow.engine.entity.EmsMonitorPoint;
import com.flow.engine.entity.EmsSamplingOrder;
import com.flow.engine.entity.EmsSamplingOrderHistory;
import com.flow.engine.entity.User;
import com.flow.engine.mapper.EmsDispatchMapper;
import com.flow.engine.mapper.EmsSamplingOrderHistoryMapper;
import com.flow.engine.mapper.EmsSamplingOrderMapper;
import com.flow.engine.mapper.UserMapper;
import com.flow.engine.util.CodeGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 环境监测 - 采样任务服务（TRD 5.1+5.2，BR-023-02 拆单）
 * 以委托单为主体生成「待派单」订单，支持按采集频率再次派单（redispatch）。
 */
@Service
public class EmsSamplingOrderService extends ServiceImpl<EmsSamplingOrderMapper, EmsSamplingOrder> {

    // @Lazy：EmsEntrustService 亦依赖本服务（techConfirm → genFromEntrust），
    // 使用延迟代理打破启动期循环依赖
    @Autowired
    @Lazy
    private EmsEntrustService entrustService;
    @Autowired
    private EmsDispatchMapper dispatchMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private EmsMonitorPointService monitorPointService;
    @Autowired
    private EmsCustomerService customerService;
    @Autowired
    private EmsSamplingOrderHistoryMapper orderHistoryMapper;

    /**
     * 采样调度看板聚合数据：每个订单补充点位名称、派单计划区间、派单负责人姓名。
     * 避免与 EmsDispatchService 互相依赖，这里直接通过 Mapper 轻量查询。
     */
    public List<Map<String, Object>> listDispatchBoard(String orderNo, String leadName, String status) {
        return listDispatchBoard(orderNo, leadName, status, null);
    }

    /** 采样调度看板聚合：entrustId 非空时仅返回该委托关联的采样任务 */
    public List<Map<String, Object>> listDispatchBoard(String orderNo, String leadName, String status, Long entrustId) {
        List<EmsSamplingOrder> orders = this.list(new LambdaQueryWrapper<EmsSamplingOrder>().orderByDesc(EmsSamplingOrder::getId));
        List<Map<String, Object>> result = new ArrayList<>();
        boolean hasFilter = (orderNo != null && !orderNo.trim().isEmpty())
                || (leadName != null && !leadName.trim().isEmpty())
                || (status != null && !status.trim().isEmpty());
        for (EmsSamplingOrder o : orders) {
            if (entrustId != null && !entrustId.equals(o.getEntrustId())) {
                continue;
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", o.getId());
            m.put("orderNo", o.getOrderNo());
            m.put("status", o.getStatus());
            m.put("entrustId", o.getEntrustId());
            // 派单以委托单为主体：展示委托名称 + 委托单号（不再依赖点位）
            String entrustName = "—";
            String entrustNo = "—";
            String custName = "—";
            if (o.getEntrustId() != null) {
                EmsEntrust e = entrustService.getById(o.getEntrustId());
                if (e != null) {
                    if (e.getEntrustName() != null) entrustName = e.getEntrustName();
                    if (e.getEntrustNo() != null) entrustNo = e.getEntrustNo();
                    if (e.getCustId() != null) {
                        EmsCustomer c = customerService.getById(e.getCustId());
                        if (c != null && c.getCustName() != null) custName = c.getCustName();
                    }
                }
            }
            m.put("entrustName", entrustName);
            m.put("entrustNo", entrustNo);
            m.put("custName", custName);
            // 委托单定义的点位数量
            int pointCount = 0;
            if (o.getEntrustId() != null) {
                pointCount = (int) monitorPointService.count(
                        new LambdaQueryWrapper<EmsMonitorPoint>().eq(EmsMonitorPoint::getEntrustId, o.getEntrustId()));
            }
            m.put("pointCount", pointCount);
            // 派单计划区间 + 负责人（取该订单最新一条派单）
            String planStart = null;
            String planEnd = null;
            String leadNameVal = null;
            String samplerNamesVal = null;
            Long dispatchId = null;
            if (o.getId() != null) {
                EmsDispatch d = dispatchMapper.selectOne(new LambdaQueryWrapper<EmsDispatch>()
                        .eq(EmsDispatch::getOrderId, o.getId())
                        .orderByDesc(EmsDispatch::getId)
                        .last("LIMIT 1"));
                if (d != null) {
                    dispatchId = d.getId();
                    if (d.getPlanStart() != null) planStart = d.getPlanStart().toLocalDate().toString();
                    if (d.getPlanEnd() != null) planEnd = d.getPlanEnd().toLocalDate().toString();
                    // 负责人：派单成员表 role=LEAD 对应的后台人员姓名（sys_user）
                    User lead = userMapper.selectOne(new LambdaQueryWrapper<User>()
                            .inSql(User::getId, "SELECT emp_id FROM t_dispatch_member WHERE dispatch_id = " + d.getId() + " AND role = 'LEAD'")
                            .last("LIMIT 1"));
                    if (lead != null) leadNameVal = lead.getRealName() != null ? lead.getRealName() : lead.getUsername();
                    // 采样人：派单成员 role=MEMBER 姓名顿号拼接
                    List<User> samplers = userMapper.selectList(new LambdaQueryWrapper<User>()
                            .inSql(User::getId, "SELECT emp_id FROM t_dispatch_member WHERE dispatch_id = " + d.getId() + " AND role = 'MEMBER'"));
                    if (!samplers.isEmpty()) {
                        samplerNamesVal = samplers.stream()
                                .map(u -> u.getRealName() != null ? u.getRealName() : u.getUsername())
                                .collect(java.util.stream.Collectors.joining("、"));
                    }
                }
            }
            m.put("dispatchId", dispatchId);
            m.put("planStart", planStart);
            m.put("planEnd", planEnd);
            m.put("planRange", (planStart != null ? planStart : "—") + " ~ " + (planEnd != null ? planEnd : "—"));
            m.put("leadName", leadNameVal != null ? leadNameVal : "—");
            m.put("samplerNames", samplerNamesVal != null ? samplerNamesVal : "—");
            m.put("createTime", o.getCreateTime());
            m.put("createBy", o.getCreateBy());
            m.put("createName", o.getCreateName());
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


    /** 委托确认后拆单：以委托单为主体生成一张待派单订单（不再按点位拆单，点位为采样环节基础信息） */
    @Transactional
    public int genFromEntrust(EmsEntrust entrust) {
        return genFromEntrust(entrust, null);
    }

    /** 委托确认后拆单（带操作人）：生成订单并记录「新建」操作历史 */
    @Transactional
    public int genFromEntrust(EmsEntrust entrust, User operator) {
        // 同一委托已存在订单则不再重复生成（重复派单走 redispatch）
        long existing = this.count(new LambdaQueryWrapper<EmsSamplingOrder>().eq(EmsSamplingOrder::getEntrustId, entrust.getId()));
        if (existing > 0) {
            return 0;
        }
        long seq = this.count() + 1;
        EmsSamplingOrder o = new EmsSamplingOrder();
        o.setOrderNo(CodeGenerator.generate("SO", (int) (seq)));
        o.setEntrustId(entrust.getId());
        // pointId 不在此赋值，留待采样执行环节选择具体点位
        o.setStatus("待派单");
        if (operator != null) {
            o.setCreateBy(operator.getUsername());
            o.setCreateName(operator.getRealName());
        }
        o.setCreateTime(LocalDateTime.now());
        o.setUpdateTime(LocalDateTime.now());
        this.save(o);
        recordOrderHistory(o.getId(), "新建", "采样任务【" + o.getOrderNo() + "】创建（委托确认后自动拆单生成）", operator);
        return 1;
    }

    /** 按采集频率再次派单：同一委托生成下一张待派单订单（委托已确认且已有订单） */
    @Transactional
    public EmsSamplingOrder redispatch(Long entrustId) {
        return redispatch(entrustId, null);
    }

    /** 按采集频率再次派单（带操作人）：生成订单并记录「新建」操作历史 */
    @Transactional
    public EmsSamplingOrder redispatch(Long entrustId, User operator) {
        EmsEntrust entrust = entrustService.getById(entrustId);
        if (entrust == null) {
            throw new IllegalArgumentException("委托不存在");
        }
        if (!"已确认".equals(entrust.getStatus())) {
            throw new IllegalStateException("委托未确认，无法再次派单(BR-023-09)");
        }
        long existing = this.count(new LambdaQueryWrapper<EmsSamplingOrder>().eq(EmsSamplingOrder::getEntrustId, entrustId));
        long seq = this.count() + 1;
        EmsSamplingOrder o = new EmsSamplingOrder();
        o.setOrderNo(CodeGenerator.generate("SO", (int) (seq)));
        o.setEntrustId(entrustId);
        o.setStatus("待派单");
        if (operator != null) {
            o.setCreateBy(operator.getUsername());
            o.setCreateName(operator.getRealName());
        }
        o.setCreateTime(LocalDateTime.now());
        o.setUpdateTime(LocalDateTime.now());
        this.save(o);
        recordOrderHistory(o.getId(), "新建", "采样任务【" + o.getOrderNo() + "】创建（按采集频率再次派单）", operator);
        return o;
    }

    public List<EmsSamplingOrder> listByStatus(String status, Long entrustId) {
        return this.list(new LambdaQueryWrapper<EmsSamplingOrder>()
                .eq(status != null, EmsSamplingOrder::getStatus, status)
                .eq(entrustId != null, EmsSamplingOrder::getEntrustId, entrustId)
                .orderByDesc(EmsSamplingOrder::getId));
    }

    /**
     * 删除采样任务：级联删除关联派单（避免孤儿数据），再删除任务本身。
     */
    @Transactional
    public void deleteOrder(Long id) {
        dispatchMapper.delete(new LambdaQueryWrapper<EmsDispatch>().eq(EmsDispatch::getOrderId, id));
        this.removeById(id);
    }

    // ==================== 完成确认与操作历史 ====================

    /**
     * 完成确认：仅「已派单」订单可完成。负责人录入实际完成时间与完成描述（富文本），
     * 状态流转为「已完成」并记录操作历史。
     */
    @Transactional
    public EmsSamplingOrder complete(Long orderId, LocalDateTime actualFinishTime, String finishDesc, User operator) {
        EmsSamplingOrder order = this.getById(orderId);
        if (order == null) {
            throw new BusinessException("采样任务不存在");
        }
        if (!"已派单".equals(order.getStatus())) {
            throw new BusinessException("仅「已派单」状态的任务可确认完成，当前：" + order.getStatus());
        }
        order.setStatus("已完成");
        order.setActualFinishTime(actualFinishTime != null ? actualFinishTime : LocalDateTime.now());
        order.setFinishDesc(finishDesc);
        order.setUpdateTime(LocalDateTime.now());
        this.updateById(order);
        recordOrderHistory(orderId, "完成", "采样任务【" + order.getOrderNo() + "】确认完成", operator);
        return order;
    }

    /** 采样任务操作历史列表（按时间倒序） */
    public List<EmsSamplingOrderHistory> listHistory(Long orderId) {
        return orderHistoryMapper.selectList(new LambdaQueryWrapper<EmsSamplingOrderHistory>()
                .eq(EmsSamplingOrderHistory::getOrderId, orderId)
                .orderByDesc(EmsSamplingOrderHistory::getId));
    }

    /** 记录采样任务操作历史（新建/派单/编辑/完成等），operator 为空时前端展示为「系统」 */
    public void recordOrderHistory(Long orderId, String action, String content, User operator) {
        EmsSamplingOrderHistory h = new EmsSamplingOrderHistory();
        h.setOrderId(orderId);
        h.setAction(action);
        h.setContent(content);
        h.setOperatorId(operator == null ? null : operator.getUsername());
        h.setOperatorName(operator == null ? null : operator.getRealName());
        h.setCreateTime(LocalDateTime.now());
        orderHistoryMapper.insert(h);
    }
}
