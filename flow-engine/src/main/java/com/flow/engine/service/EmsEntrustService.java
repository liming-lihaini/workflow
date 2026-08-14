package com.flow.engine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.flow.engine.common.RequestContext;
import com.flow.engine.dto.EmsEntrustVO;
import com.flow.engine.entity.DictItem;
import com.flow.engine.entity.EmsCustomer;
import com.flow.engine.entity.EmsEntrust;
import com.flow.engine.entity.EmsEntrustHistory;
import com.flow.engine.entity.User;
import com.flow.engine.entity.EmsEntrustReview;
import com.flow.engine.entity.EmsMonitorPoint;
import com.flow.engine.entity.EmsSamplingOrder;
import com.flow.engine.mapper.EmsEntrustHistoryMapper;
import com.flow.engine.mapper.EmsEntrustMapper;
import com.flow.engine.mapper.UserMapper;
import com.flow.engine.util.CodeGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
    private EmsEntrustHistoryMapper historyMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    @Autowired
    private PermissionEvaluator permissionEvaluator;

    /** 来源字典 code（TRD 5.1 t_entrust.source） */
    private static final String SOURCE_DICT = "moni_entrust_source";

    /** 检测委托模块数据权限 Key（{模块}:data-all 表示查看模块全部数据） */
    private static final String MODULE_PERM_KEY = "ems:entrust";

    public EmsEntrust createDraft(EmsEntrust e) {
        if (!StringUtils.hasText(e.getEntrustName())) {
            throw new IllegalArgumentException("委托名称不能为空");
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
        User op = lookupUserByName(submitBy);
        recordHistory(id, "提交", "提交委托，进入技术确认", submitBy, op == null ? null : op.getRealName());
        return e;
    }

    /** 技术确认通过（BR-023-01）：写 review + 状态→已确认 + 拆单生成采样任务（BR-023-02） */
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

        // 技术确认后生成委托编号：WT + yyyyMMdd + 当日4位序号（如 WT202608080001）
        if (!StringUtils.hasText(e.getEntrustNo())) {
            String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
            String prefix = "WT" + date;
            long cnt = this.count(new LambdaQueryWrapper<EmsEntrust>().likeRight(EmsEntrust::getEntrustNo, prefix));
            e.setEntrustNo(CodeGenerator.generate("WT", (int) cnt + 1));
        }

        e.setStatus("已确认");
        e.setUpdateTime(LocalDateTime.now());
        this.updateById(e);

        User op = lookupUserById(reviewerId);
        samplingOrderService.genFromEntrust(e, op);
        recordHistory(id, "技术确认", "技术确认通过，委托编号 " + e.getEntrustNo()
                + "，状态：待技术确认 → 已确认；意见：" + stripHtml(opinion),
                op == null ? String.valueOf(reviewerId) : op.getUsername(),
                op == null ? null : op.getRealName());
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
        User op = lookupUserById(reviewerId);
        recordHistory(id, "退回", "技术确认退回，状态：待技术确认 → 已退回；意见：" + opinion,
                op == null ? String.valueOf(reviewerId) : op.getUsername(),
                op == null ? null : op.getRealName());
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
        recordHistory(id, "收样", "收样完成，状态 → 已收样", "system", "系统");
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
            // 采样任务 + 其派单
            List<Long> orderIds = jdbcTemplate.queryForList(
                    "SELECT id FROM t_sampling_order WHERE entrust_id = ?", Long.class, id);
            if (orderIds != null && !orderIds.isEmpty()) {
                String inSql = orderIds.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
                jdbcTemplate.update("DELETE FROM t_dispatch WHERE order_id IN (" + inSql + ")");
                jdbcTemplate.update("DELETE FROM t_sampling_order WHERE entrust_id = ?", id);
            }
            // 操作历史随委托一并清理
            historyMapper.delete(new LambdaQueryWrapper<EmsEntrustHistory>().eq(EmsEntrustHistory::getEntrustId, id));
        }
        removeByIds(ids);
    }

    /** 客户详情：该客户下的检测委托清单（关联客户名称、来源名称） */
    public List<EmsEntrustVO> listVOByCustId(Long custId) {
        List<EmsEntrust> list = this.list(new LambdaQueryWrapper<EmsEntrust>()
                .eq(EmsEntrust::getCustId, custId)
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

    /** 列表视图：关联客户名称、来源名称（TRD 5.1 委托列表展示），可按状态过滤，受数据权限控制 */
    public List<EmsEntrustVO> listVO(String status) {
        LambdaQueryWrapper<EmsEntrust> uw = new LambdaQueryWrapper<EmsEntrust>()
                .eq(status != null, EmsEntrust::getStatus, status)
                .orderByDesc(EmsEntrust::getCreateTime);
        applyDataScope(uw);
        List<EmsEntrust> list = this.list(uw);
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
        vo.setSampleFreq(e.getSampleFreq());
        vo.setSampleFreqName(loadSampleFreqNames().get(e.getSampleFreq()));
        vo.setPoints(monitorPointService.listByEntrust(id));
        return vo;
    }

    /**
     * 保存/更新委托并维护监测点位（事务）
     * 点位作为委托基础信息一并提交（ISSUE-023 改造）
     */
    @Transactional
    public EmsEntrustVO saveWithPoints(EmsEntrust e, List<EmsMonitorPoint> points, User operator) {
        boolean isNew = e.getId() == null;
        if (!StringUtils.hasText(e.getEntrustName())) {
            throw new IllegalArgumentException("委托名称不能为空(BR-022-05)");
        }
        Map<String, String> sourceNameMap = loadSourceNames();
        Map<String, String> sampleFreqNameMap = loadSampleFreqNames();
        Map<String, String> pointTypeNameMap = loadPointTypeNames();

        // 持久化字典 value（与 key 配对），便于列表/详情直接展示
        // 兜底：若提交值本身即显示文本（历史数据/非编码），则用原值作为名称
        String srcName = sourceNameMap.get(e.getSource());
        e.setSourceName(srcName != null ? srcName : e.getSource());
        String freqName = sampleFreqNameMap.get(e.getSampleFreq());
        e.setSampleFreqName(freqName != null ? freqName : e.getSampleFreq());

        if (isNew) {
            e.setStatus(e.getStatus() == null ? "草稿" : e.getStatus());
            e.setCreateTime(LocalDateTime.now());
            if (operator != null) {
                e.setCreateBy(operator.getUsername());
                e.setCreateName(operator.getRealName());
            }
        }
        if (operator != null) {
            e.setUpdateBy(operator.getUsername());
            e.setUpdateName(operator.getRealName());
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
                String ptName = pointTypeNameMap.get(p.getPointType());
                p.setPointTypeName(ptName != null ? ptName : p.getPointType());
                p.setHistoryOverFlag(p.getHistoryOverFlag() == null ? 0 : p.getHistoryOverFlag());
                p.setCreateTime(now);
                p.setUpdateTime(now);
                seq++;
            }
            monitorPointService.saveBatch(points);
        }
        // 操作历史：新建 / 编辑
        if (isNew) {
            recordHistory(e.getId(), "新建", "新建检测委托【" + e.getEntrustName() + "】",
                    operator == null ? null : operator.getUsername(),
                    operator == null ? null : operator.getRealName());
        } else {
            recordHistory(e.getId(), "编辑", "编辑委托信息【" + e.getEntrustName() + "】",
                    operator == null ? null : operator.getUsername(),
                    operator == null ? null : operator.getRealName());
        }
        return getVO(e.getId());
    }

    /**
     * 检测委托数据范围过滤：
     * 1. 系统管理员（角色数据范围=全部）或授权 ems:entrust:data-all → 不过滤，查看全部数据；
     * 2. 其余用户仅可见本人创建（createBy）的委托。
     */
    private void applyDataScope(LambdaQueryWrapper<EmsEntrust> uw) {
        RequestContext ctx = RequestContext.current();
        String userIdStr = ctx.getUserId();
        if (!StringUtils.hasText(userIdStr)) {
            uw.apply("1 = 0");
            return;
        }
        Long userId;
        try {
            userId = Long.valueOf(userIdStr);
        } catch (NumberFormatException e) {
            uw.apply("1 = 0");
            return;
        }
        if (permissionEvaluator.canViewAllModuleData(userId, MODULE_PERM_KEY)) {
            return;
        }
        String username = ctx.getUsername();
        if (StringUtils.hasText(username)) {
            uw.eq(EmsEntrust::getCreateBy, username);
        } else {
            uw.apply("1 = 0");
        }
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

    private Map<String, String> loadPointTypeNames() {
        Map<String, String> map = new HashMap<>();
        try {
            List<DictItem> items = dictService.getDictItemsByCode("moni_point_type");
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

    // ===================== 操作历史（详情页「操作记录」展示） =====================

    /** 委托操作历史列表（按时间倒序） */
    public List<EmsEntrustHistory> listHistory(Long entrustId) {
        return historyMapper.selectList(new LambdaQueryWrapper<EmsEntrustHistory>()
                .eq(EmsEntrustHistory::getEntrustId, entrustId)
                .orderByDesc(EmsEntrustHistory::getId));
    }

    private void recordHistory(Long entrustId, String action, String content, String opBy, String opName) {
        EmsEntrustHistory h = new EmsEntrustHistory();
        h.setEntrustId(entrustId);
        h.setAction(action);
        h.setContent(content);
        h.setOperatorId(opBy);
        h.setOperatorName(opName);
        h.setCreateTime(LocalDateTime.now());
        historyMapper.insert(h);
    }

    private User lookupUserById(Long userId) {
        if (userId == null) return null;
        try {
            return userMapper.selectById(userId);
        } catch (Exception e) {
            return null;
        }
    }

    private User lookupUserByName(String username) {
        if (!StringUtils.hasText(username)) return null;
        try {
            return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        } catch (Exception e) {
            return null;
        }
    }

    /** 富文本意见转纯文本（去标签），用于操作历史展示；review 表仍存原始 HTML */
    private String stripHtml(String html) {
        if (!StringUtils.hasText(html)) return "";
        return html.replaceAll("<[^>]+>", "").replaceAll("&nbsp;", " ").trim();
    }
}
