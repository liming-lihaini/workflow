package com.flow.engine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.flow.engine.common.BusinessException;
import com.flow.engine.common.ErrorCode;
import com.flow.engine.common.RequestContext;
import com.flow.engine.dto.ContractSaveReq;
import com.flow.engine.dto.ContractTxnReq;
import com.flow.engine.dto.EmsContractVO;
import com.flow.engine.entity.*;
import com.flow.engine.mapper.*;
import com.flow.engine.util.CodeGenerator;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 合同管理台账服务（PRD-02）
 * 状态机：草稿 → 执行中 →(收付款全部核销) 已完结；执行中 ⇄ 已中止；草稿/执行中 → 已作废
 * 收付款登记与节点核销、合同状态在同一事务内更新，撤销登记级联回退。
 */
@Service
public class EmsContractService extends ServiceImpl<EmsContractMapper, EmsContract> {

    public static final String TYPE_INCOME = "收入合同";
    public static final String TYPE_EXPENSE = "支出合同";
    public static final String STATUS_DRAFT = "草稿";
    public static final String STATUS_RUNNING = "执行中";
    public static final String STATUS_FINISHED = "已完结";
    public static final String STATUS_SUSPENDED = "已中止";
    public static final String STATUS_VOID = "已作废";

    /** 合同模块数据权限 Key（{模块}:data-all 表示查看模块全部数据） */
    private static final String MODULE_PERM_KEY = "ems:contract";

    @Autowired
    private EmsContractNodeMapper nodeMapper;
    @Autowired
    private EmsContractTxnMapper txnMapper;
    @Autowired
    private EmsContractTxnNodeMapper txnNodeMapper;
    @Autowired
    private EmsContractEntrustMapper entrustLinkMapper;
    @Autowired
    private EmsContractHistoryMapper historyMapper;
    @Autowired
    private EmsEntrustMapper entrustMapper;
    @Autowired
    private EmsCustomerService customerService;
    @Autowired
    private PermissionEvaluator permissionEvaluator;

    // ==================== 列表与统计 ====================

    /** 台账分页列表（含已收付金额、进度、逾期节点数，受数据权限控制） */
    public Page<EmsContractVO> pageVO(String contractNo, String contractName, String contractType,
                                      String status, String counterparty, Long leadId,
                                      String signStart, String signEnd, int page, int size) {
        LambdaQueryWrapper<EmsContract> uw = new LambdaQueryWrapper<>();
        uw.like(StringUtils.hasText(contractNo), EmsContract::getContractNo, contractNo)
          .like(StringUtils.hasText(contractName), EmsContract::getContractName, contractName)
          .eq(StringUtils.hasText(contractType), EmsContract::getContractType, contractType)
          .eq(StringUtils.hasText(status), EmsContract::getStatus, status)
          .like(StringUtils.hasText(counterparty), EmsContract::getCounterpartyName, counterparty)
          .eq(leadId != null, EmsContract::getLeadId, leadId)
          .ge(StringUtils.hasText(signStart), EmsContract::getSignDate, signStart)
          .le(StringUtils.hasText(signEnd), EmsContract::getSignDate, signEnd)
          .orderByDesc(EmsContract::getId);
        applyDataScope(uw);
        Page<EmsContract> p = this.page(new Page<>(page, size), uw);
        Page<EmsContractVO> voPage = new Page<>(p.getCurrent(), p.getSize(), p.getTotal());
        List<EmsContract> list = p.getRecords();
        List<EmsContractVO> vos = new ArrayList<>();
        if (!list.isEmpty()) {
            List<Long> ids = list.stream().map(EmsContract::getId).collect(Collectors.toList());
            Map<Long, BigDecimal> settledMap = sumTxnByContract(ids);
            Map<Long, Integer> overdueMap = countOverdueNodes(ids);
            for (EmsContract c : list) {
                EmsContractVO vo = toVO(c);
                BigDecimal settled = settledMap.getOrDefault(c.getId(), BigDecimal.ZERO);
                vo.setSettledAmount(settled);
                vo.setProgress(percent(settled, c.getAmount()));
                vo.setOverdueNodeCount(overdueMap.getOrDefault(c.getId(), 0));
                vos.add(vo);
            }
        }
        voPage.setRecords(vos);
        return voPage;
    }

    /** 台账统计卡片（PRD-02 §5.1） */
    public Map<String, Object> statistics() {
        LambdaQueryWrapper<EmsContract> uw = new LambdaQueryWrapper<EmsContract>()
                .ne(EmsContract::getStatus, STATUS_VOID);
        applyDataScope(uw);
        List<EmsContract> all = this.list(uw);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalCount", all.size());
        data.put("runningCount", all.stream().filter(c -> STATUS_RUNNING.equals(c.getStatus())).count());

        BigDecimal receivable = BigDecimal.ZERO, payable = BigDecimal.ZERO;
        List<Long> incomeIds = new ArrayList<>(), expenseIds = new ArrayList<>();
        for (EmsContract c : all) {
            BigDecimal amt = c.getAmount() == null ? BigDecimal.ZERO : c.getAmount();
            if (TYPE_INCOME.equals(c.getContractType())) {
                receivable = receivable.add(amt);
                incomeIds.add(c.getId());
            } else if (TYPE_EXPENSE.equals(c.getContractType())) {
                payable = payable.add(amt);
                expenseIds.add(c.getId());
            }
        }
        BigDecimal received = sumTxn(incomeIds);
        BigDecimal paid = sumTxn(expenseIds);
        data.put("receivable", receivable);
        data.put("received", received);
        data.put("receivableUnsettled", receivable.subtract(received));
        data.put("payable", payable);
        data.put("paid", paid);
        data.put("payableUnsettled", payable.subtract(paid));

        List<Long> allIds = all.stream().map(EmsContract::getId).collect(Collectors.toList());
        data.put("overdueNodeCount", countOverdueNodes(allIds).values().stream().mapToInt(Integer::intValue).sum());
        return data;
    }

    // ==================== 合同保存与状态流转 ====================

    /** 新建/编辑合同（含节点整体替换与关联委托） */
    @Transactional
    public EmsContractVO save(ContractSaveReq req, User operator) {
        EmsContract c = req.getContract();
        if (c == null || !StringUtils.hasText(c.getContractName())) {
            throw new IllegalArgumentException("合同名称不能为空");
        }
        if (!StringUtils.hasText(c.getContractType())) {
            throw new IllegalArgumentException("合同类型不能为空");
        }
        if (!StringUtils.hasText(c.getCounterpartyName())) {
            throw new IllegalArgumentException("相对方不能为空");
        }
        if (c.getAmount() == null || c.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("合同金额不能为空且不能小于0");
        }
        if (!StringUtils.hasText(c.getSignDate())) {
            throw new IllegalArgumentException("签订日期不能为空");
        }
        if (!StringUtils.hasText(c.getEffectDate())) {
            c.setEffectDate(c.getSignDate());
        }

        boolean isNew = c.getId() == null;
        if (isNew) {
            if (!StringUtils.hasText(c.getContractNo())) {
                c.setContractNo(nextContractNo());
            }
            checkContractNoUnique(c.getContractNo(), null);
            c.setStatus(STATUS_DRAFT);
            c.setCreateBy(operator == null ? null : operator.getUsername());
            c.setCreateName(operator == null ? null : operator.getRealName());
            c.setUpdateBy(c.getCreateBy());
            c.setUpdateName(c.getCreateName());
            c.setCreateTime(LocalDateTime.now());
            c.setUpdateTime(LocalDateTime.now());
            this.save(c);
            recordHistory(c.getId(), "新建", "新建合同【" + c.getContractName() + "】，编号 " + c.getContractNo(), operator);
        } else {
            EmsContract old = require(c.getId());
            checkContractNoUnique(c.getContractNo(), c.getId());
            if (!STATUS_DRAFT.equals(old.getStatus())) {
                // 非草稿仅允许编辑非核心字段（备注/说明/到期日期）
                old.setRemark(c.getRemark());
                old.setDescription(c.getDescription());
                old.setExpireDate(c.getExpireDate());
                c = old;
            }
            c.setUpdateBy(operator == null ? null : operator.getUsername());
            c.setUpdateName(operator == null ? null : operator.getRealName());
            c.setUpdateTime(LocalDateTime.now());
            this.updateById(c);
            recordHistory(c.getId(), "编辑", "编辑合同【" + c.getContractName() + "】", operator);
        }

        // 收付款节点（整体替换）
        if (req.getNodes() != null) {
            replaceNodes(c, req.getNodes());
        }
        // 关联检测委托（仅收入合同）
        replaceEntrustLinks(c.getId(), TYPE_INCOME.equals(c.getContractType()) ? req.getEntrustIds() : null);
        return detail(c.getId());
    }

    /** 草稿 → 执行中（要求至少 1 个节点且金额合计等于合同金额） */
    @Transactional
    public EmsContractVO submit(Long id, User operator) {
        EmsContract c = require(id);
        if (!STATUS_DRAFT.equals(c.getStatus())) {
            throw new IllegalStateException("仅草稿状态可提交，当前：" + c.getStatus());
        }
        List<EmsContractNode> nodes = listNodes(id);
        if (nodes.isEmpty()) {
            throw new IllegalStateException("请先维护收付款节点后再提交");
        }
        checkNodeSum(c, nodes);
        c.setStatus(STATUS_RUNNING);
        c.setUpdateTime(LocalDateTime.now());
        this.updateById(c);
        recordHistory(id, "提交", "提交合同，状态：草稿 → 执行中", operator);
        return detail(id);
    }

    /** 执行中 → 已中止 */
    @Transactional
    public EmsContractVO suspend(Long id, String reason, User operator) {
        EmsContract c = require(id);
        if (!STATUS_RUNNING.equals(c.getStatus())) {
            throw new IllegalStateException("仅执行中状态可中止，当前：" + c.getStatus());
        }
        c.setStatus(STATUS_SUSPENDED);
        c.setUpdateTime(LocalDateTime.now());
        this.updateById(c);
        recordHistory(id, "中止", "中止合同" + (StringUtils.hasText(reason) ? "，原因：" + reason : ""), operator);
        return detail(id);
    }

    /** 已中止 → 执行中 */
    @Transactional
    public EmsContractVO resume(Long id, User operator) {
        EmsContract c = require(id);
        if (!STATUS_SUSPENDED.equals(c.getStatus())) {
            throw new IllegalStateException("仅已中止状态可恢复，当前：" + c.getStatus());
        }
        c.setStatus(STATUS_RUNNING);
        c.setUpdateTime(LocalDateTime.now());
        this.updateById(c);
        recordHistory(id, "恢复", "恢复合同执行", operator);
        return detail(id);
    }

    /** 草稿/执行中 → 已作废（已发生收付款登记的合同不允许作废） */
    @Transactional
    public EmsContractVO cancel(Long id, User operator) {
        EmsContract c = require(id);
        if (!STATUS_DRAFT.equals(c.getStatus()) && !STATUS_RUNNING.equals(c.getStatus())) {
            throw new IllegalStateException("仅草稿或执行中状态可作废，当前：" + c.getStatus());
        }
        Long txnCount = txnMapper.selectCount(new LambdaQueryWrapper<EmsContractTxn>()
                .eq(EmsContractTxn::getContractId, id));
        if (txnCount != null && txnCount > 0) {
            throw new IllegalStateException("已发生收付款登记的合同不允许作废，请改用中止");
        }
        c.setStatus(STATUS_VOID);
        c.setUpdateTime(LocalDateTime.now());
        this.updateById(c);
        recordHistory(id, "作废", "作废合同", operator);
        return detail(id);
    }

    /** 删除草稿合同（级联删除节点/关联/历史） */
    @Transactional
    public void delete(Long id) {
        EmsContract c = require(id);
        if (!STATUS_DRAFT.equals(c.getStatus())) {
            throw new IllegalStateException("仅草稿状态可删除，当前：" + c.getStatus());
        }
        nodeMapper.delete(new LambdaQueryWrapper<EmsContractNode>().eq(EmsContractNode::getContractId, id));
        entrustLinkMapper.delete(new LambdaQueryWrapper<EmsContractEntrust>().eq(EmsContractEntrust::getContractId, id));
        historyMapper.delete(new LambdaQueryWrapper<EmsContractHistory>().eq(EmsContractHistory::getContractId, id));
        this.removeById(id);
    }

    // ==================== 节点管理 ====================

    /** 节点整体替换（草稿/执行中可用；已有登记的节点不允许删除） */
    @Transactional
    public EmsContractVO updateNodes(Long id, List<EmsContractNode> nodes, User operator) {
        EmsContract c = require(id);
        if (!STATUS_DRAFT.equals(c.getStatus()) && !STATUS_RUNNING.equals(c.getStatus())) {
            throw new IllegalStateException("当前状态不允许维护节点：" + c.getStatus());
        }
        replaceNodes(c, nodes);
        recordHistory(id, "编辑", "调整收付款节点，共 " + (nodes == null ? 0 : nodes.size()) + " 期", operator);
        return detail(id);
    }

    // ==================== 收付款登记 ====================

    /** 收款/支付登记（PRD-02 §3.3）：写流水+分摊核销，重算节点状态，必要时自动完结 */
    @Transactional
    public EmsContractVO addTxn(ContractTxnReq req, User operator) {
        EmsContract c = require(req.getContractId());
        if (!STATUS_RUNNING.equals(c.getStatus())) {
            throw new IllegalStateException("仅执行中状态可登记收付款，当前：" + c.getStatus());
        }
        String txnType = TYPE_INCOME.equals(c.getContractType()) ? "收款" : "支付";
        if (req.getAmount() == null || req.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(txnType + "金额必须大于0");
        }
        if (!StringUtils.hasText(req.getTxnDate())) {
            req.setTxnDate(LocalDate.now().toString());
        }
        if (req.getAllocations() == null || req.getAllocations().isEmpty()) {
            throw new IllegalArgumentException("请选择核销节点");
        }
        // 分摊校验：节点归属 + 合计等于流水金额
        List<EmsContractNode> nodes = listNodes(c.getId());
        Map<Long, EmsContractNode> nodeMap = nodes.stream()
                .collect(Collectors.toMap(EmsContractNode::getId, Function.identity()));
        BigDecimal allocSum = BigDecimal.ZERO;
        for (ContractTxnReq.Alloc a : req.getAllocations()) {
            if (a.getNodeId() == null || !nodeMap.containsKey(a.getNodeId())) {
                throw new IllegalArgumentException("核销节点不存在或不属于本合同");
            }
            if (a.getAmount() == null || a.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("节点分摊金额必须大于0");
            }
            allocSum = allocSum.add(a.getAmount());
        }
        if (allocSum.compareTo(req.getAmount()) != 0) {
            throw new IllegalArgumentException("节点分摊合计（" + allocSum + "）必须等于" + txnType + "金额（" + req.getAmount() + "）");
        }
        // 累计金额不得超过合同金额
        BigDecimal settled = sumTxn(Collections.singletonList(c.getId()));
        if (settled.add(req.getAmount()).compareTo(c.getAmount()) > 0) {
            throw new IllegalArgumentException("累计" + txnType + "金额不得超过合同金额，剩余可登记："
                    + c.getAmount().subtract(settled));
        }

        EmsContractTxn txn = new EmsContractTxn();
        txn.setContractId(c.getId());
        txn.setTxnType(txnType);
        txn.setTxnDate(req.getTxnDate());
        txn.setAmount(req.getAmount());
        txn.setPayMethod(req.getPayMethod());
        txn.setTxnNo(req.getTxnNo());
        txn.setRemark(req.getRemark());
        txn.setOperatorId(operator == null ? null : operator.getUsername());
        txn.setOperatorName(operator == null ? null : operator.getRealName());
        txn.setCreateTime(LocalDateTime.now());
        txnMapper.insert(txn);
        for (ContractTxnReq.Alloc a : req.getAllocations()) {
            EmsContractTxnNode link = new EmsContractTxnNode();
            link.setTxnId(txn.getId());
            link.setNodeId(a.getNodeId());
            link.setAllocateAmount(a.getAmount());
            txnNodeMapper.insert(link);
        }

        recalcNodeStatus(c);
        recordHistory(c.getId(), txnType + "登记",
                txnType + "登记 " + req.getAmount() + " 元"
                        + (StringUtils.hasText(req.getTxnNo()) ? "，流水号 " + req.getTxnNo() : ""), operator);
        return detail(c.getId());
    }

    /** 撤销收付款登记：删除流水与分摊，重算节点状态，必要时回退合同状态 */
    @Transactional
    public EmsContractVO deleteTxn(Long txnId, User operator) {
        EmsContractTxn txn = txnMapper.selectById(txnId);
        if (txn == null) {
            throw new IllegalArgumentException("收付款登记不存在");
        }
        EmsContract c = require(txn.getContractId());
        txnNodeMapper.delete(new LambdaQueryWrapper<EmsContractTxnNode>().eq(EmsContractTxnNode::getTxnId, txnId));
        txnMapper.deleteById(txnId);
        recalcNodeStatus(c);
        recordHistory(c.getId(), "撤销登记", "撤销" + txn.getTxnType() + "登记 " + txn.getAmount() + " 元"
                + (StringUtils.hasText(txn.getTxnNo()) ? "（流水号 " + txn.getTxnNo() + "）" : ""), operator);
        return detail(c.getId());
    }

    // ==================== 详情与历史 ====================

    /** 合同详情：基础信息 + 节点（核销进度/逾期）+ 流水（分摊）+ 关联委托 + 操作历史 */
    public EmsContractVO detail(Long id) {
        EmsContract c = require(id);
        EmsContractVO vo = toVO(c);

        Map<Long, BigDecimal> allocByNode = sumAllocByContract(id);
        String today = LocalDate.now().toString();
        boolean income = TYPE_INCOME.equals(c.getContractType());
        List<EmsContractVO.NodeVO> nodeVOs = new ArrayList<>();
        for (EmsContractNode n : listNodes(id)) {
            EmsContractVO.NodeVO nv = new EmsContractVO.NodeVO();
            nv.setId(n.getId());
            nv.setSeq(n.getSeq());
            nv.setNodeName(n.getNodeName());
            nv.setPlanAmount(n.getPlanAmount());
            nv.setPlanDate(n.getPlanDate());
            nv.setNodeDesc(n.getNodeDesc());
            nv.setStatus(n.getStatus());
            BigDecimal allocated = allocByNode.getOrDefault(n.getId(), BigDecimal.ZERO);
            nv.setAllocatedAmount(allocated);
            nv.setOverdue(n.getPlanDate() != null && n.getPlanDate().compareTo(today) < 0
                    && !settledStatus(income).equals(n.getStatus()));
            nodeVOs.add(nv);
        }
        vo.setNodes(nodeVOs);

        BigDecimal settled = BigDecimal.ZERO;
        List<EmsContractTxn> txns = txnMapper.selectList(new LambdaQueryWrapper<EmsContractTxn>()
                .eq(EmsContractTxn::getContractId, id).orderByDesc(EmsContractTxn::getId));
        Map<Long, EmsContractNode> nodeMap = listNodes(id).stream()
                .collect(Collectors.toMap(EmsContractNode::getId, Function.identity()));
        List<EmsContractVO.TxnVO> txnVOs = new ArrayList<>();
        for (EmsContractTxn t : txns) {
            settled = settled.add(t.getAmount() == null ? BigDecimal.ZERO : t.getAmount());
            EmsContractVO.TxnVO tv = new EmsContractVO.TxnVO();
            BeanUtils.copyProperties(t, tv);
            List<EmsContractVO.AllocVO> allocs = new ArrayList<>();
            List<EmsContractTxnNode> links = txnNodeMapper.selectList(
                    new LambdaQueryWrapper<EmsContractTxnNode>().eq(EmsContractTxnNode::getTxnId, t.getId()));
            for (EmsContractTxnNode link : links) {
                EmsContractVO.AllocVO av = new EmsContractVO.AllocVO();
                av.setNodeId(link.getNodeId());
                av.setAllocateAmount(link.getAllocateAmount());
                EmsContractNode n = nodeMap.get(link.getNodeId());
                if (n != null) {
                    av.setNodeSeq(n.getSeq());
                    av.setNodeName(n.getNodeName());
                }
                allocs.add(av);
            }
            tv.setAllocations(allocs);
            txnVOs.add(tv);
        }
        vo.setTxns(txnVOs);
        vo.setSettledAmount(settled);
        vo.setProgress(percent(settled, c.getAmount()));

        // 关联委托（与检测委托列表展示对齐：编号/名称/客户/来源/状态/创建信息，供穿透跳转）
        List<EmsContractEntrust> links = entrustLinkMapper.selectList(
                new LambdaQueryWrapper<EmsContractEntrust>().eq(EmsContractEntrust::getContractId, id));
        List<EmsContractVO.EntrustRefVO> entrustRefs = new ArrayList<>();
        Map<Long, String> custNameCache = new HashMap<>();
        for (EmsContractEntrust link : links) {
            EmsEntrust e = entrustMapper.selectById(link.getEntrustId());
            if (e == null) continue;
            EmsContractVO.EntrustRefVO ev = new EmsContractVO.EntrustRefVO();
            ev.setEntrustId(e.getId());
            ev.setEntrustNo(e.getEntrustNo());
            ev.setEntrustName(e.getEntrustName());
            ev.setStatus(e.getStatus());
            ev.setUrgent(e.getUrgent());
            ev.setStartDate(e.getStartDate());
            ev.setSourceName(e.getSourceName());
            ev.setCreateName(e.getCreateName());
            ev.setCreateBy(e.getCreateBy());
            ev.setCreateTime(e.getCreateTime());
            if (e.getCustId() != null) {
                ev.setCustName(custNameCache.computeIfAbsent(e.getCustId(), cid -> {
                    EmsCustomer cust = customerService.getById(cid);
                    return cust == null ? null : cust.getCustName();
                }));
            }
            entrustRefs.add(ev);
        }
        vo.setEntrusts(entrustRefs);

        vo.setHistories(listHistory(id));
        return vo;
    }

    public List<EmsContractHistory> listHistory(Long contractId) {
        return historyMapper.selectList(new LambdaQueryWrapper<EmsContractHistory>()
                .eq(EmsContractHistory::getContractId, contractId)
                .orderByDesc(EmsContractHistory::getId));
    }

    // ==================== 内部工具 ====================

    private EmsContract require(Long id) {
        EmsContract c = this.getById(id);
        if (c == null) {
            throw new IllegalArgumentException("合同不存在");
        }
        return c;
    }

    /**
     * 合同数据权限校验（详情/操作历史等按 ID 访问的接口）：
     * 1. 系统管理员（角色数据范围=全部）或持有 ems:contract:data-all 权限 → 放行；
     * 2. 否则仅创建人或合同负责人可查看。
     */
    public void checkContractVisible(Long id) {
        RequestContext ctx = RequestContext.current();
        String userIdStr = ctx.getUserId();
        if (!StringUtils.hasText(userIdStr)) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        }
        Long userId = Long.valueOf(userIdStr);
        if (permissionEvaluator.canViewAllModuleData(userId, MODULE_PERM_KEY)) {
            return;
        }
        EmsContract c = require(id);
        String username = ctx.getUsername();
        boolean creator = StringUtils.hasText(username) && username.equals(c.getCreateBy());
        boolean lead = userId.equals(c.getLeadId());
        if (!creator && !lead) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        }
    }

    /**
     * 应用合同模块数据范围过滤：
     * 1. 系统管理员（角色数据范围=全部）或授权 ems:contract:data-all → 不过滤，查看全部数据；
     * 2. 否则仅可见「创建人 = 本人」或「负责人 = 本人」的合同。
     */
    private void applyDataScope(LambdaQueryWrapper<EmsContract> uw) {
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
        boolean hasUser = StringUtils.hasText(username);
        uw.and(w -> w.eq(hasUser, EmsContract::getCreateBy, username)
                .or(hasUser).eq(EmsContract::getLeadId, userId));
    }

    private String nextContractNo() {
        String prefix = "HT" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        long cnt = this.count(new LambdaQueryWrapper<EmsContract>().likeRight(EmsContract::getContractNo, prefix));
        return CodeGenerator.generate("HT", (int) cnt + 1);
    }

    private void checkContractNoUnique(String contractNo, Long excludeId) {
        if (!StringUtils.hasText(contractNo)) return;
        LambdaQueryWrapper<EmsContract> uw = new LambdaQueryWrapper<EmsContract>()
                .eq(EmsContract::getContractNo, contractNo)
                .ne(excludeId != null, EmsContract::getId, excludeId);
        if (this.count(uw) > 0) {
            throw new IllegalArgumentException("合同编号已存在：" + contractNo);
        }
    }

    /** 节点整体替换：金额合计校验 + 已登记节点保护 + 重算状态 */
    private void replaceNodes(EmsContract c, List<EmsContractNode> incoming) {
        Long contractId = c.getId();
        List<EmsContractNode> existing = listNodes(contractId);
        Map<Long, BigDecimal> allocByNode = sumAllocByContract(contractId);
        boolean income = TYPE_INCOME.equals(c.getContractType());

        if (incoming == null) incoming = new ArrayList<>();
        if (!incoming.isEmpty()) {
            checkNodeSum(c, incoming);
        }
        // 已有核销登记的节点不允许被移除
        Set<Long> keepIds = incoming.stream().map(EmsContractNode::getId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        for (EmsContractNode old : existing) {
            if (!keepIds.contains(old.getId())) {
                BigDecimal allocated = allocByNode.getOrDefault(old.getId(), BigDecimal.ZERO);
                if (allocated.compareTo(BigDecimal.ZERO) > 0) {
                    throw new IllegalStateException("第 " + old.getSeq() + " 期节点已产生收付款登记，不允许删除，请先撤销对应登记");
                }
            }
        }
        // 删除不再保留的节点
        for (EmsContractNode old : existing) {
            if (!keepIds.contains(old.getId())) {
                nodeMapper.deleteById(old.getId());
            }
        }
        int seq = 1;
        LocalDateTime now = LocalDateTime.now();
        for (EmsContractNode n : incoming) {
            n.setContractId(contractId);
            n.setSeq(seq++);
            n.setUpdateTime(now);
            if (n.getId() == null) {
                n.setStatus(waitStatus(income));
                n.setCreateTime(now);
                nodeMapper.insert(n);
            } else {
                // 保留已有状态，稍后按核销金额重算
                nodeMapper.updateById(n);
            }
        }
        recalcNodeStatus(c);
    }

    private void checkNodeSum(EmsContract c, List<EmsContractNode> nodes) {
        BigDecimal sum = nodes.stream()
                .map(n -> n.getPlanAmount() == null ? BigDecimal.ZERO : n.getPlanAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sum.compareTo(c.getAmount()) != 0) {
            throw new IllegalArgumentException("节点计划金额合计（" + sum + "）必须等于合同金额（" + c.getAmount() + "）");
        }
        if ("分期".equals(c.getPayMode()) && nodes.size() < 2) {
            throw new IllegalArgumentException("付款方式为分期时，收付款节点至少 2 期");
        }
    }

    /** 重算节点核销状态，并在全部核销完成时自动完结合同（或回退执行中） */
    private void recalcNodeStatus(EmsContract c) {
        boolean income = TYPE_INCOME.equals(c.getContractType());
        Map<Long, BigDecimal> allocByNode = sumAllocByContract(c.getId());
        List<EmsContractNode> nodes = listNodes(c.getId());
        boolean allSettled = !nodes.isEmpty();
        LocalDateTime now = LocalDateTime.now();
        for (EmsContractNode n : nodes) {
            BigDecimal allocated = allocByNode.getOrDefault(n.getId(), BigDecimal.ZERO);
            String status;
            if (allocated.compareTo(BigDecimal.ZERO) <= 0) {
                status = waitStatus(income);
                allSettled = false;
            } else if (allocated.compareTo(n.getPlanAmount()) >= 0) {
                status = settledStatus(income);
            } else {
                status = partialStatus(income);
                allSettled = false;
            }
            if (!status.equals(n.getStatus())) {
                n.setStatus(status);
                n.setUpdateTime(now);
                nodeMapper.updateById(n);
            }
        }
        if (allSettled && STATUS_RUNNING.equals(c.getStatus())) {
            c.setStatus(STATUS_FINISHED);
            c.setUpdateTime(now);
            this.updateById(c);
        } else if (!allSettled && STATUS_FINISHED.equals(c.getStatus())) {
            c.setStatus(STATUS_RUNNING);
            c.setUpdateTime(now);
            this.updateById(c);
        }
    }

    private String waitStatus(boolean income) { return income ? "待收" : "待付"; }
    private String partialStatus(boolean income) { return income ? "部分收" : "部分付"; }
    private String settledStatus(boolean income) { return income ? "已收讫" : "已付讫"; }

    private void replaceEntrustLinks(Long contractId, List<Long> entrustIds) {
        entrustLinkMapper.delete(new LambdaQueryWrapper<EmsContractEntrust>()
                .eq(EmsContractEntrust::getContractId, contractId));
        if (entrustIds == null) return;
        for (Long entrustId : entrustIds) {
            if (entrustId == null) continue;
            EmsContractEntrust link = new EmsContractEntrust();
            link.setContractId(contractId);
            link.setEntrustId(entrustId);
            entrustLinkMapper.insert(link);
        }
    }

    private List<EmsContractNode> listNodes(Long contractId) {
        return nodeMapper.selectList(new LambdaQueryWrapper<EmsContractNode>()
                .eq(EmsContractNode::getContractId, contractId)
                .orderByAsc(EmsContractNode::getSeq).orderByAsc(EmsContractNode::getId));
    }

    /** 合同维度已登记金额合计 */
    private Map<Long, BigDecimal> sumTxnByContract(List<Long> contractIds) {
        Map<Long, BigDecimal> map = new HashMap<>();
        if (contractIds.isEmpty()) return map;
        List<EmsContractTxn> txns = txnMapper.selectList(new LambdaQueryWrapper<EmsContractTxn>()
                .in(EmsContractTxn::getContractId, contractIds));
        for (EmsContractTxn t : txns) {
            map.merge(t.getContractId(), t.getAmount() == null ? BigDecimal.ZERO : t.getAmount(), BigDecimal::add);
        }
        return map;
    }

    private BigDecimal sumTxn(List<Long> contractIds) {
        return sumTxnByContract(contractIds).values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** 合同维度每节点已核销金额（分摊合计） */
    private Map<Long, BigDecimal> sumAllocByContract(Long contractId) {
        Map<Long, BigDecimal> map = new HashMap<>();
        List<EmsContractTxn> txns = txnMapper.selectList(new LambdaQueryWrapper<EmsContractTxn>()
                .eq(EmsContractTxn::getContractId, contractId));
        if (txns.isEmpty()) return map;
        List<Long> txnIds = txns.stream().map(EmsContractTxn::getId).collect(Collectors.toList());
        List<EmsContractTxnNode> links = txnNodeMapper.selectList(new LambdaQueryWrapper<EmsContractTxnNode>()
                .in(EmsContractTxnNode::getTxnId, txnIds));
        for (EmsContractTxnNode link : links) {
            map.merge(link.getNodeId(),
                    link.getAllocateAmount() == null ? BigDecimal.ZERO : link.getAllocateAmount(), BigDecimal::add);
        }
        return map;
    }

    /** 逾期未核销完成节点数（计划日期 < 今天） */
    private Map<Long, Integer> countOverdueNodes(List<Long> contractIds) {
        Map<Long, Integer> map = new HashMap<>();
        if (contractIds.isEmpty()) return map;
        String today = LocalDate.now().toString();
        List<EmsContractNode> nodes = nodeMapper.selectList(new LambdaQueryWrapper<EmsContractNode>()
                .in(EmsContractNode::getContractId, contractIds)
                .lt(EmsContractNode::getPlanDate, today));
        Map<Long, EmsContract> contractMap = this.listByIds(contractIds).stream()
                .collect(Collectors.toMap(EmsContract::getId, Function.identity()));
        for (EmsContractNode n : nodes) {
            EmsContract c = contractMap.get(n.getContractId());
            boolean income = c != null && TYPE_INCOME.equals(c.getContractType());
            if (!settledStatus(income).equals(n.getStatus())) {
                map.merge(n.getContractId(), 1, Integer::sum);
            }
        }
        return map;
    }

    private BigDecimal percent(BigDecimal settled, BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        return settled.multiply(BigDecimal.valueOf(100)).divide(total, 1, RoundingMode.HALF_UP);
    }

    private EmsContractVO toVO(EmsContract c) {
        EmsContractVO vo = new EmsContractVO();
        BeanUtils.copyProperties(c, vo);
        return vo;
    }

    private void recordHistory(Long contractId, String action, String content, User operator) {
        EmsContractHistory h = new EmsContractHistory();
        h.setContractId(contractId);
        h.setAction(action);
        h.setContent(content);
        h.setOperatorId(operator == null ? null : operator.getUsername());
        h.setOperatorName(operator == null ? null : operator.getRealName());
        h.setCreateTime(LocalDateTime.now());
        historyMapper.insert(h);
    }
}
