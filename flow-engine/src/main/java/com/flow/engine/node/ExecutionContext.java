package com.flow.engine.node;

import com.flow.engine.common.utils.ExpressionUtils;
import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 流程执行上下文（PRD §运行引擎 变量 / TRD §4.1.2）。
 *
 * <p>持有流程实例、流程定义、当前节点、操作人信息，以及流程变量。
 * 变量分两级作用域：
 * <ul>
 *   <li><b>全局作用域</b>：整个流程实例共享</li>
 *   <li><b>本地作用域</b>：仅当前节点可见，优先级高于全局（同名本地覆盖全局）</li>
 * </ul>
 */
@Data
public class ExecutionContext implements Serializable {

    private String processInstanceId;
    private String processDefinitionId;
    private String currentNodeId;
    private String operator;

    /** 全局流程变量 */
    private final Map<String, Object> variables = new HashMap<>();

    /** 当前节点本地变量（随节点切换不应残留，由引擎在进入节点时清空/重建） */
    private Map<String, Object> localVariables = new HashMap<>();

    public ExecutionContext() {
    }

    public ExecutionContext(String processInstanceId, String processDefinitionId) {
        this.processInstanceId = processInstanceId;
        this.processDefinitionId = processDefinitionId;
    }

    // ==================== 变量读写（全局 + 本地合并视图） ====================

    /**
     * 取变量：本地优先，本地无则取全局。
     */
    public Object getVariable(String key) {
        if (localVariables.containsKey(key)) {
            return localVariables.get(key);
        }
        return variables.get(key);
    }

    public Object getVariable(String key, Object defaultValue) {
        Object v = getVariable(key);
        return v == null ? defaultValue : v;
    }

    /**
     * 设置全局变量。
     */
    public void setVariable(String key, Object value) {
        variables.put(key, value);
    }

    /**
     * 设置本地变量（仅当前节点可见）。
     */
    public void setLocalVariable(String key, Object value) {
        localVariables.put(key, value);
    }

    public Object getLocalVariable(String key) {
        return localVariables.get(key);
    }

    /**
     * 进入新节点时调用，重置本地变量作用域。
     */
    public void resetLocalScope() {
        this.localVariables = new HashMap<>();
    }

    /**
     * 将全部变量（全局+本地）组装为统一 Map，供 EL 表达式求值使用。
     */
    public Map<String, Object> getAllVariables() {
        Map<String, Object> merged = new HashMap<>(variables);
        merged.putAll(localVariables);
        return merged;
    }

    /**
     * 便捷 EL 求值：基于当前上下文变量求值表达式。
     */
    public Object eval(String expression) {
        return ExpressionUtils.eval(expression, getAllVariables());
    }

    public boolean evalBoolean(String expression) {
        return ExpressionUtils.evalBoolean(expression, getAllVariables());
    }
}
