package com.flow.engine.controllers;

import com.flow.engine.annotation.OpLog;
import com.flow.engine.common.Result;
import com.flow.engine.entity.EmsRuleDef;
import com.flow.engine.service.EmsFormulaEngineService;
import com.flow.engine.service.EmsRuleEngineService;
import com.flow.engine.service.EmsSeqEngineService;
import com.flow.engine.service.EmsStateMachineService;
import com.flow.engine.service.EmsStateMachineService.FireResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 基础设施底座（ISSUE-029）：状态机 / 规则引擎 / 编号引擎 / 公式计算。
 * 操作审计复用 ISSUE-014 的 @OpLog 切面（LogService + sys_operation_log），不重复建设。
 * 路径前缀 /api/v1。
 */
@RestController
@RequestMapping("/api/v1")
public class EmsBaseController {

    private final EmsStateMachineService stateMachine;
    private final EmsRuleEngineService ruleEngine;
    private final EmsSeqEngineService seqEngine;
    private final EmsFormulaEngineService formulaEngine;

    public EmsBaseController(EmsStateMachineService stateMachine,
                             EmsRuleEngineService ruleEngine,
                             EmsSeqEngineService seqEngine,
                             EmsFormulaEngineService formulaEngine) {
        this.stateMachine = stateMachine;
        this.ruleEngine = ruleEngine;
        this.seqEngine = seqEngine;
        this.formulaEngine = formulaEngine;
    }

    // ===== 状态机 =====
    /** 驱动状态迁移：body { bizType, bizId, event, fromState?, ctx?, operator? } */
    @OpLog(module = "基础设施底座-状态机", operation = "驱动状态迁移")
    @PostMapping("/statemachine/fire")
    public Result<FireResult> fire(@RequestBody Map<String, Object> body) {
        String bizType = (String) body.get("bizType");
        String bizId = String.valueOf(body.getOrDefault("bizId", ""));
        String event = (String) body.get("event");
        String fromState = (String) body.getOrDefault("fromState", null);
        String operator = (String) body.getOrDefault("operator", "system");
        @SuppressWarnings("unchecked")
        Map<String, Object> ctx = (Map<String, Object>) body.getOrDefault("ctx", java.util.Collections.emptyMap());
        FireResult res = stateMachine.fire(bizType, bizId, event, fromState, ctx, operator);
        return res.allowed ? Result.ok(res) : Result.fail(409, res.message);
    }

    /** 查询某业务当前可用迁移（按 bizType + fromState）。 */
    @GetMapping("/statemachine/transitions")
    public Result<List<Object>> transitions(@RequestParam String bizType,
                                             @RequestParam(required = false) String fromState) {
        List<Object> list = new java.util.ArrayList<>(stateMachine.transitionsOf(bizType, fromState));
        return Result.ok(list);
    }

    // ===== 规则引擎 =====
    /** 规则求值：body { ruleKey, context? } */
    @OpLog(module = "基础设施底座-规则引擎", operation = "规则求值")
    @PostMapping("/rule/eval")
    public Result<Map<String, Object>> evalRule(@RequestBody Map<String, Object> body) {
        String ruleKey = (String) body.get("ruleKey");
        @SuppressWarnings("unchecked")
        Map<String, Object> ctx = (Map<String, Object>) body.getOrDefault("context", java.util.Collections.emptyMap());
        boolean r = ruleEngine.eval(ruleKey, ctx);
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("ruleKey", ruleKey);
        m.put("result", r);
        return Result.ok(m);
    }

    /** 规则列表（管理员维护）。 */
    @GetMapping("/rule")
    public Result<List<EmsRuleDef>> listRules() {
        return Result.ok(ruleEngine.list());
    }

    /** 保存规则（管理员）。 */
    @OpLog(module = "基础设施底座-规则引擎", operation = "保存规则")
    @PostMapping("/rule")
    public Result<EmsRuleDef> saveRule(@RequestBody EmsRuleDef rule) {
        return Result.ok(ruleEngine.save(rule));
    }

    /** 删除规则（管理员）。 */
    @OpLog(module = "基础设施底座-规则引擎", operation = "删除规则")
    @DeleteMapping("/rule/{id}")
    public Result<Object> removeRule(@PathVariable Long id) {
        ruleEngine.remove(id);
        return Result.ok();
    }

    // ===== 编号引擎 =====
    /** 取下一个唯一业务单号。 */
    @OpLog(module = "基础设施底座-编号引擎", operation = "生成业务单号")
    @GetMapping("/seq/next")
    public Result<Map<String, Object>> nextSeq(@RequestParam String biz) {
        String no = seqEngine.next(biz);
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("biz", biz);
        m.put("no", no);
        return Result.ok(m);
    }

    // ===== 公式计算引擎 =====
    /** 公式计算：body { formula, params? } */
    @OpLog(module = "基础设施底座-公式计算", operation = "公式计算")
    @PostMapping("/calc")
    public Result<Map<String, Object>> calc(@RequestBody Map<String, Object> body) {
        String formula = (String) body.get("formula");
        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) body.getOrDefault("params", java.util.Collections.emptyMap());
        Object result = formulaEngine.calc(formula, params);
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("formula", formula);
        m.put("result", result);
        return Result.ok(m);
    }
}
