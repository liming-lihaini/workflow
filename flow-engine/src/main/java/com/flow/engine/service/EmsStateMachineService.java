package com.flow.engine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flow.engine.entity.EmsTransitionDef;
import com.flow.engine.mapper.EmsTransitionDefMapper;
import com.flow.engine.util.ExprEngine;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 固定状态机框架（ISSUE-029）。
 * 以配置（t_transition_def）+ 硬编码方式驱动各业务状态机：查迁移 → 执行 Guard → 返回结果。
 * 操作审计复用 ISSUE-014 的 @OpLog 切面（LogService），不重复建设。
 * 状态落库由业务模块负责（本框架专注迁移合法性与守卫校验，BR-029-02/06）。
 */
@Service
public class EmsStateMachineService {

    private final EmsTransitionDefMapper transitionMapper;

    public EmsStateMachineService(EmsTransitionDefMapper transitionMapper) {
        this.transitionMapper = transitionMapper;
    }

    /** 迁移执行结果。 */
    public static class FireResult {
        public boolean allowed;
        public String fromState;
        public String event;
        public String toState;
        public String guardMsg;
        public String message;
    }

    /**
     * 驱动状态机。
     * @param bizType   业务类型（如 entrust/common/order/sample/batch/report）
     * @param bizId     业务 ID
     * @param event     触发事件
     * @param fromState 当前状态（若提供则校验"不可跳变"，BR-029-06）
     * @param ctx       守卫上下文（#var 引用）
     * @param operator  操作人
     */
    public FireResult fire(String bizType, String bizId, String event,
                           String fromState, Map<String, Object> ctx, String operator) {
        FireResult res = new FireResult();
        res.event = event;

        LambdaQueryWrapper<EmsTransitionDef> q = new LambdaQueryWrapper<EmsTransitionDef>()
                .eq(EmsTransitionDef::getBizType, bizType)
                .eq(EmsTransitionDef::getEvent, event);
        if (fromState != null && !fromState.isEmpty()) {
            q.eq(EmsTransitionDef::getFromState, fromState);
        }
        List<EmsTransitionDef> transitions = transitionMapper.selectList(q);

        if (transitions.isEmpty()) {
            // 状态机未定义该迁移 → 拒绝（BR-029-06）
            res.allowed = false;
            res.fromState = fromState;
            res.message = "状态机未定义迁移: " + bizType + " / " + fromState + " / " + event;
            return res;
        }

        EmsTransitionDef def = transitions.get(0);
        res.fromState = fromState == null ? def.getFromState() : fromState;
        res.toState = def.getToState();

        // 执行 Guard（BR-029-02）
        boolean guardPass = true;
        String guardExpr = def.getGuardExpr();
        if (guardExpr != null && !guardExpr.trim().isEmpty()) {
            guardPass = ExprEngine.evalBoolean(guardExpr, ctx);
        }
        if (!guardPass) {
            res.allowed = false;
            res.guardMsg = def.getGuardFailMsg();
            res.message = "守卫校验未通过: " + (def.getGuardFailMsg() == null ? guardExpr : def.getGuardFailMsg());
            return res;
        }

        res.allowed = true;
        res.message = "迁移成功: " + res.fromState + " → " + res.toState;
        return res;
    }

    /** 查询某业务类型下某状态的所有可用事件（用于前端展示）。 */
    public List<EmsTransitionDef> transitionsOf(String bizType, String fromState) {
        LambdaQueryWrapper<EmsTransitionDef> q = new LambdaQueryWrapper<EmsTransitionDef>()
                .eq(EmsTransitionDef::getBizType, bizType);
        if (fromState != null && !fromState.isEmpty()) {
            q.eq(EmsTransitionDef::getFromState, fromState);
        }
        return transitionMapper.selectList(q);
    }
}
