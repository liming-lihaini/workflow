package com.flow.engine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flow.engine.entity.EmsRuleDef;
import com.flow.engine.mapper.EmsRuleDefMapper;
import com.flow.engine.util.ExprEngine;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 规则引擎服务（ISSUE-029）。
 * 规则以表达式存于 t_rule_def，可配置热更；执行时用 SpEL 沙箱求值。
 * 操作审计复用 ISSUE-014 的 @OpLog 切面（LogService），不重复建设。
 */
@Service
public class EmsRuleEngineService {

    private final EmsRuleDefMapper ruleMapper;

    public EmsRuleEngineService(EmsRuleDefMapper ruleMapper) {
        this.ruleMapper = ruleMapper;
    }

    /** 按规则标识求值（布尔）。规则不存在/停用/异常时返回 false。 */
    public boolean eval(String ruleKey, Map<String, Object> context) {
        EmsRuleDef rule = ruleMapper.selectOne(
                new LambdaQueryWrapper<EmsRuleDef>().eq(EmsRuleDef::getRuleKey, ruleKey));
        boolean result = false;
        if (rule != null && (rule.getEnabled() == null || rule.getEnabled() == 1)) {
            result = ExprEngine.evalBoolean(rule.getExpr(), context);
        }
        return result;
    }

    /** 列表（管理员维护）。 */
    public List<EmsRuleDef> list() {
        return ruleMapper.selectList(new LambdaQueryWrapper<EmsRuleDef>().orderByAsc(EmsRuleDef::getId));
    }

    /** 保存（新增/更新）。 */
    public EmsRuleDef save(EmsRuleDef rule) {
        if (rule.getId() == null) {
            rule.setVersion(1);
            rule.setEnabled(rule.getEnabled() == null ? 1 : rule.getEnabled());
            ruleMapper.insert(rule);
        } else {
            EmsRuleDef old = ruleMapper.selectById(rule.getId());
            if (old != null) rule.setVersion((old.getVersion() == null ? 1 : old.getVersion()) + 1);
            ruleMapper.updateById(rule);
        }
        return rule;
    }

    /** 删除（配置维护，非审计日志）。 */
    public void remove(Long id) {
        ruleMapper.deleteById(id);
    }
}
