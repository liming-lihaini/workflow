package com.flow.engine.controller;

import com.flow.engine.annotation.OpLog;
import com.flow.engine.common.Result;
import com.flow.engine.dto.CreateDelegationRequest;
import com.flow.engine.dto.DelegationResponse;
import com.flow.engine.service.DelegationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 全局委托控制器
 */
@RestController
@RequestMapping("/api/v1/delegations")
@RequiredArgsConstructor
@CrossOrigin
public class DelegationController {

    private final DelegationService delegationService;

    /**
     * 创建全局委托
     * POST /api/v1/delegations
     */
    @PostMapping
    @OpLog(module = "委托管理", operation = "创建委托")
    public Result<DelegationResponse> create(@RequestBody CreateDelegationRequest request) {
        return Result.ok(delegationService.createDelegation(request));
    }

    /**
     * 我的委托（当前用户作为委托人）
     * GET /api/v1/delegations/my?userId=xxx
     */
    @GetMapping("/my")
    public Result<List<DelegationResponse>> getMyDelegations(@RequestParam String userId) {
        return Result.ok(delegationService.getDelegationsByDelegator(userId));
    }

    /**
     * 代理记录（当前用户作为代理人）
     * GET /api/v1/delegations/proxy?userId=xxx
     */
    @GetMapping("/proxy")
    public Result<List<DelegationResponse>> getProxyDelegations(@RequestParam String userId) {
        return Result.ok(delegationService.getDelegationsByDelegate(userId));
    }

    /**
     * 取消委托
     * POST /api/v1/delegations/{id}/cancel
     */
    @PostMapping("/{id}/cancel")
    @OpLog(module = "委托管理", operation = "取消委托")
    public Result<Void> cancel(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String operatorId = body.get("operatorId");
        delegationService.cancelDelegation(id, operatorId);
        return Result.ok();
    }
}
