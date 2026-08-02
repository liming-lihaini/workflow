package com.flow.engine.service;

import com.flow.engine.util.ExprEngine;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 公式计算引擎（ISSUE-029）。
 * 基于 SpEL 执行检测结果公式运算（联动 025 标准库 formula 字段），支持单位换算。
 * 操作审计复用 ISSUE-014 的 @OpLog 切面（LogService），不重复建设。
 */
@Service
public class EmsFormulaEngineService {

    /** 计算：formula 中变量通过 params 注入（#var）。返回计算结果。 */
    public Object calc(String formula, Map<String, Object> params) {
        Map<String, Object> ctx = params == null ? new HashMap<>() : new HashMap<>(params);
        return ExprEngine.eval(formula, ctx);
    }
}
