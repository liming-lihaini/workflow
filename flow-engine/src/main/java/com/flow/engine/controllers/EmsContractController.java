package com.flow.engine.controllers;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.flow.engine.common.Result;
import com.flow.engine.dto.ContractSaveReq;
import com.flow.engine.dto.ContractTxnReq;
import com.flow.engine.dto.EmsContractVO;
import com.flow.engine.entity.EmsContractHistory;
import com.flow.engine.entity.EmsContractNode;
import com.flow.engine.entity.User;
import com.flow.engine.service.EmsContractService;
import com.flow.engine.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 合同管理台账 API（PRD-02）：合同信息、收付款节点、收款/支付登记、台账统计
 */
@RestController
@RequestMapping("/api/v1/ems/contract")
@RequiredArgsConstructor
public class EmsContractController {

    private final EmsContractService contractService;
    private final UserService userService;

    // ---------- 合同台账 ----------

    /** 台账分页列表（默认每页10条，支持编号/名称/类型/状态/相对方/负责人/签订日期区间筛选） */
    @GetMapping
    public Result<Page<EmsContractVO>> pageContracts(
            @RequestParam(required = false) String contractNo,
            @RequestParam(required = false) String contractName,
            @RequestParam(required = false) String contractType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String counterparty,
            @RequestParam(required = false) Long leadId,
            @RequestParam(required = false) String signStart,
            @RequestParam(required = false) String signEnd,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(contractService.pageVO(contractNo, contractName, contractType, status,
                counterparty, leadId, signStart, signEnd, page, size));
    }

    /** 台账统计卡片：应收应付/已收已付/逾期节点数 */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> statistics() {
        return Result.ok(contractService.statistics());
    }

    /** 新建/编辑合同（含收付款节点与关联委托） */
    @PostMapping
    public Result<EmsContractVO> saveContract(@RequestBody ContractSaveReq req) {
        return Result.ok(contractService.save(req, currentUser()));
    }

    /** 合同详情（节点核销进度 + 流水 + 关联委托 + 操作历史），受数据权限控制 */
    @GetMapping("/{id}")
    public Result<EmsContractVO> getContract(@PathVariable Long id) {
        contractService.checkContractVisible(id);
        return Result.ok(contractService.detail(id));
    }

    /** 删除草稿合同（级联删除节点/关联） */
    @DeleteMapping("/{id}")
    public Result<Void> deleteContract(@PathVariable Long id) {
        contractService.delete(id);
        return Result.ok();
    }

    // ---------- 状态流转 ----------

    @PostMapping("/{id}/submit")
    public Result<EmsContractVO> submitContract(@PathVariable Long id) {
        return Result.ok(contractService.submit(id, currentUser()));
    }

    @PostMapping("/{id}/suspend")
    public Result<EmsContractVO> suspendContract(@PathVariable Long id,
                                                 @RequestParam(required = false) String reason) {
        return Result.ok(contractService.suspend(id, reason, currentUser()));
    }

    @PostMapping("/{id}/resume")
    public Result<EmsContractVO> resumeContract(@PathVariable Long id) {
        return Result.ok(contractService.resume(id, currentUser()));
    }

    @PostMapping("/{id}/cancel")
    public Result<EmsContractVO> cancelContract(@PathVariable Long id) {
        return Result.ok(contractService.cancel(id, currentUser()));
    }

    // ---------- 收付款节点 ----------

    /** 节点整体替换（草稿/执行中） */
    @PostMapping("/{id}/nodes")
    public Result<EmsContractVO> updateNodes(@PathVariable Long id, @RequestBody List<EmsContractNode> nodes) {
        return Result.ok(contractService.updateNodes(id, nodes, currentUser()));
    }

    // ---------- 收付款登记 ----------

    /** 收款/支付登记（按合同类型自动判定流水类型） */
    @PostMapping("/{id}/txn")
    public Result<EmsContractVO> addTxn(@PathVariable Long id, @RequestBody ContractTxnReq req) {
        req.setContractId(id);
        return Result.ok(contractService.addTxn(req, currentUser()));
    }

    /** 撤销收付款登记（级联回退节点核销与合同状态） */
    @DeleteMapping("/txn/{txnId}")
    public Result<EmsContractVO> deleteTxn(@PathVariable Long txnId) {
        return Result.ok(contractService.deleteTxn(txnId, currentUser()));
    }

    // ---------- 操作历史 ----------

    @GetMapping("/{id}/history")
    public Result<List<EmsContractHistory>> getHistory(@PathVariable Long id) {
        contractService.checkContractVisible(id);
        return Result.ok(contractService.listHistory(id));
    }

    private User currentUser() {
        String userId = com.flow.engine.common.RequestContext.current().getUserId();
        if (userId == null) {
            return null;
        }
        try {
            return userService.getUser(Long.valueOf(userId));
        } catch (Exception e) {
            return null;
        }
    }
}
