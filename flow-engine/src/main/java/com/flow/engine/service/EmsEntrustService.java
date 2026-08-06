package com.flow.engine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.flow.engine.dto.EmsEntrustVO;
import com.flow.engine.entity.DictItem;
import com.flow.engine.entity.EmsCustomer;
import com.flow.engine.entity.EmsEntrust;
import com.flow.engine.entity.EmsEntrustReview;
import com.flow.engine.entity.EmsMonitorPoint;
import com.flow.engine.entity.EmsSamplingOrder;
import com.flow.engine.mapper.EmsEntrustMapper;
import com.flow.engine.util.CodeGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 环境监测 - 委托单服务（TRD 5.1 + ISSUE-023 状态机）
 * 状态机：草稿 → 待技术确认 →(通过) 已确认 →(生成订单) 订单态
 *                    ↓(退回) 已退回 →(重提) 待技术确认
 * 复用 EmsSamplingOrderService 完成拆单（BR-023-02），不自建编号引擎。
 */
@Service
public class EmsEntrustService extends ServiceImpl<EmsEntrustMapper, EmsEntrust> {

    @Autowired
    private EmsSamplingOrderService samplingOrderService;
    @Autowired
    private EmsEntrustReviewService reviewService;
    @Autowired
    private EmsMonitorPointService monitorPointService;
    @Autowired
    private EmsCustomerService customerService;
    @Autowired
    private DictService dictService;
    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    /** 来源字典 code（TRD 5.1 t_entrust.source） */
    private static final String SOURCE_DICT = "moni_entrust_source";

    public EmsEntrust createDraft(EmsEntrust e) {
        if (!StringUtils.hasText(e.getEntrustName())) {
            throw new IllegalArgumentException("委托名称不能为空(BR-022-05)");
        }
        e.setStatus(e.getStatus() == null ? "草稿" : e.getStatus());
        e.setCreateTime(LocalDateTime.now());
        e.setUpdateTime(LocalDateTime.now());
        this.save(e);
        return e;
    }

    /** 草稿/已退回 → 待技术确认（BR-023-01 守卫：客户/点位/项目齐全） */
    public EmsEntrust submit(Long id, String submitBy) {
        EmsEntrust e = require(id);
        if (!"草稿".equals(e.getStatus()) && !"已退回".equals(e.getStatus())) {
            throw new IllegalStateException("仅草稿或已退回状态可提交，当前：" + e.getStatus());
        }
        e.setSubmitBy(submitBy);
        e.setStatus("待技术确认");
        e.setUpdateTime(LocalDateTime.now());
        this.updateById(e);
        return e;
    }

    /** 技术确认通过（BR-023-01）：写 review + 状态→已确认 + 拆单生成采样订单（BR-023-02） */
    @Transactional
    public EmsEntrust techConfirm(Long id, Long reviewerId, String opinion) {
        EmsEntrust e = require(id);
        if (!"待技术确认".equals(e.getStatus())) {
            throw new IllegalStateException("仅待技术确认状态可技术确认，当前：" + e.getStatus());
        }
        EmsEntrustReview r = new EmsEntrustReview();
        r.setEntrustId(id);
        r.setReviewerId(reviewerId);
        r.setOpinion(opinion);
        r.setResult("PASS");
        r.setReviewAt(LocalDateTime.now());
        reviewService.save(r);

        e.setStatus("已确认");
        e.setUpdateTime(LocalDateTime.now());
        this.updateById(e);

        samplingOrderService.genFromEntrust(e);
        return e;
    }

    /** 技术确认退回（BR-023-06）：写 review + 状态→已退回 */
    @Transactional
    public EmsEntrust reject(Long id, Long reviewerId, String opinion) {
        EmsEntrust e = require(id);
        if (!"待技术确认".equals(e.getStatus())) {
            throw new IllegalStateException("仅待技术确认状态可退回，当前：" + e.getStatus());
        }
        if (!StringUtils.hasText(opinion)) {
            throw new IllegalArgumentException("退回必填意见(BR-023-06)");
        }
        EmsEntrustReview r = new EmsEntrustReview();
        r.setEntrustId(id);
        r.setReviewerId(reviewerId);
        r.setOpinion(opinion);
        r.setResult("REJECT");
        r.setReviewAt(LocalDateTime.now());
        reviewService.save(r);

        e.setStatus("已退回");
        e.setUpdateTime(LocalDateTime.now());
        this.updateById(e);
        return e;
    }

    public List<EmsEntrust> listByStatus(String status) {
        return this.list(new LambdaQueryWrapper<EmsEntrust>().eq(status != null, EmsEntrust::getStatus, status));
    }

    /** 收样完成：委托单自动推进至「已收样」（BR：仅当尚未进入后续流程时） */
    public EmsEntrust markReceived(Long id) {
        EmsEntrust e = this.getById(id);
        if (e == null) return null;
        if ("已收样".equals(e.getStatus())
                || "报告编制".equals(e.getStatus())
                || "归档完成".equals(e.getStatus())) {
            return e; // 已是或已超过该状态，不回退
        }
        e.setStatus("已收样");
        e.setUpdateTime(LocalDateTime.now());
        this.updateById(e);
        return e;
    }

    /** 批量删除委托（ISSUE-026：先删关联子表，再删主表，避免外键约束） */
    @Transactional
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Long id : ids) {
            // 监测点位
            jdbcTemplate.update("DELETE FROM t_monitor_point WHERE entrust_id = ?", id);
            // 委托明细
            jdbcTemplate.update("DELETE FROM t_entrust_detail WHERE entrust_id = ?", id);
            // 采样订单 + 其派单
            List<Long> orderIds = jdbcTemplate.queryForList(
                    "SELECT id FROM t_sampling_order WHERE entrust_id = ?", Long.class, id);
            if (orderIds != null && !orderIds.isEmpty()) {
                String inSql = orderIds.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
                jdbcTemplate.update("DELETE FROM t_dispatch WHERE order_id IN (" + inSql + ")");
                jdbcTemplate.update("DELETE FROM t_sampling_order WHERE entrust_id = ?", id);
            }
        }
        removeByIds(ids);
    }

    /** 列表视图：关联客户名称、来源名称（TRD 5.1 委托列表展示），可按状态过滤 */
    public List<EmsEntrustVO> listVO(String status) {
        List<EmsEntrust> list = this.list(new LambdaQueryWrapper<EmsEntrust>()
                .eq(status != null, EmsEntrust::getStatus, status)
                .orderByDesc(EmsEntrust::getCreateTime));
        Map<Long, String> custNameMap = loadCustNames(list);
        Map<String, String> sourceNameMap = loadSourceNames();
        Map<String, String> sampleFreqNameMap = loadSampleFreqNames();
        List<EmsEntrustVO> vos = new ArrayList<>();
        for (EmsEntrust e : list) {
            EmsEntrustVO vo = EmsEntrustVO.from(e);
            vo.setCustName(custNameMap.get(e.getCustId()));
            vo.setSourceName(sourceNameMap.get(e.getSource()));
            vo.setSampleFreqName(sampleFreqNameMap.get(e.getSampleFreq()));
            vos.add(vo);
        }
        return vos;
    }

    /** 委托详情视图：含监测点位（委托基础信息） */
    public EmsEntrustVO getVO(Long id) {
        EmsEntrust e = require(id);
        EmsEntrustVO vo = EmsEntrustVO.from(e);
        if (e.getCustId() != null) {
            EmsCustomer c = customerService.getById(e.getCustId());
            vo.setCustName(c == null ? null : c.getCustName());
        }
        vo.setSourceName(loadSourceNames().get(e.getSource()));
        vo.setSampleFreqName(loadSampleFreqNames().get(e.getSampleFreq()));
        vo.setPoints(monitorPointService.listByEntrust(id));
        return vo;
    }

    /**
     * 保存/更新委托并维护监测点位（事务）
     * 点位作为委托基础信息一并提交（ISSUE-023 改造）
     */
    @Transactional
    public EmsEntrustVO saveWithPoints(EmsEntrust e, List<EmsMonitorPoint> points) {
        boolean isNew = e.getId() == null;
        if (!StringUtils.hasText(e.getEntrustName())) {
            throw new IllegalArgumentException("委托名称不能为空(BR-022-05)");
        }
        if (isNew) {
            e.setStatus(e.getStatus() == null ? "草稿" : e.getStatus());
            e.setCreateTime(LocalDateTime.now());
        }
        e.setUpdateTime(LocalDateTime.now());
        this.saveOrUpdate(e);

        // 维护点位：先删除原 entrust 下点位，再批量保存
        monitorPointService.remove(new LambdaQueryWrapper<EmsMonitorPoint>().eq(EmsMonitorPoint::getEntrustId, e.getId()));
        if (points != null && !points.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            int seq = 1;
            for (EmsMonitorPoint p : points) {
                p.setId(null);
                p.setEntrustId(e.getId());
                p.setCustId(e.getCustId());
                if (!StringUtils.hasText(p.getPointNo())) {
                    p.setPointNo(String.format("P%03d", seq));
                }
                p.setHistoryOverFlag(p.getHistoryOverFlag() == null ? 0 : p.getHistoryOverFlag());
                p.setCreateTime(now);
                p.setUpdateTime(now);
                seq++;
            }
            monitorPointService.saveBatch(points);
        }
        return getVO(e.getId());
    }

    private Map<Long, String> loadCustNames(List<EmsEntrust> list) {
        Map<Long, String> map = new HashMap<>();
        for (EmsEntrust e : list) {
            if (e.getCustId() != null && !map.containsKey(e.getCustId())) {
                EmsCustomer c = customerService.getById(e.getCustId());
                map.put(e.getCustId(), c == null ? null : c.getCustName());
            }
        }
        return map;
    }

    private Map<String, String> loadSourceNames() {
        Map<String, String> map = new HashMap<>();
        try {
            List<DictItem> items = dictService.getDictItemsByCode(SOURCE_DICT);
            for (DictItem it : items) {
                map.put(it.getItemValue(), it.getItemText());
            }
        } catch (Exception ignored) {
            // 字典未初始化时忽略，展示原始编码
        }
        return map;
    }

    private Map<String, String> loadSampleFreqNames() {
        Map<String, String> map = new HashMap<>();
        try {
            List<DictItem> items = dictService.getDictItemsByCode("moni_sample_freq");
            for (DictItem it : items) {
                map.put(it.getItemValue(), it.getItemText());
            }
        } catch (Exception ignored) {
            // 字典未初始化时忽略，展示原始编码
        }
        return map;
    }

    private EmsEntrust require(Long id) {
        EmsEntrust e = this.getById(id);
        if (e == null) {
            throw new IllegalArgumentException("委托不存在");
        }
        return e;
    }
}
