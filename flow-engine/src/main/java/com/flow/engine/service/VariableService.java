package com.flow.engine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flow.engine.entity.Variable;
import com.flow.engine.mapper.VariableMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 流程变量服务（ISSUE-004）
 */
@Service
@RequiredArgsConstructor
public class VariableService {

    private final VariableMapper variableMapper;

    /**
     * 批量保存流程变量
     */
    public void saveVariables(Long processInstanceId, Map<String, Object> variables) {
        if (variables == null || variables.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            // 变量名不以 $ 开头（PRD §流程变量）
            if (entry.getKey().startsWith("$")) {
                continue;
            }
            Variable var = new Variable();
            var.setProcessInstanceId(processInstanceId);
            var.setVariableKey(entry.getKey());
            var.setVariableValue(entry.getValue() == null ? null : String.valueOf(entry.getValue()));
            var.setVariableType(inferType(entry.getValue()));
            var.setCreateTime(LocalDateTime.now());
            variableMapper.insert(var);
        }
    }

    /**
     * 获取流程实例的所有变量
     */
    public Map<String, Object> getVariables(Long processInstanceId) {
        List<Variable> vars = variableMapper.selectList(
                new LambdaQueryWrapper<Variable>()
                        .eq(Variable::getProcessInstanceId, processInstanceId)
                        .isNull(Variable::getTaskId)
        );
        Map<String, Object> result = new HashMap<>();
        for (Variable v : vars) {
            result.put(v.getVariableKey(), convertValue(v.getVariableValue(), v.getVariableType()));
        }
        return result;
    }

    /**
     * 更新或新增变量
     */
    public void updateVariables(Long processInstanceId, Map<String, Object> variables) {
        if (variables == null || variables.isEmpty()) {
            return;
        }
        Map<String, Object> existing = getVariables(processInstanceId);
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("$")) {
                continue;
            }
            if (existing.containsKey(key)) {
                // 更新：删除旧值后重新插入
                variableMapper.delete(
                        new LambdaQueryWrapper<Variable>()
                                .eq(Variable::getProcessInstanceId, processInstanceId)
                                .eq(Variable::getVariableKey, key)
                                .isNull(Variable::getTaskId)
                );
            }
            Variable var = new Variable();
            var.setProcessInstanceId(processInstanceId);
            var.setVariableKey(key);
            var.setVariableValue(entry.getValue() == null ? null : String.valueOf(entry.getValue()));
            var.setVariableType(inferType(entry.getValue()));
            var.setCreateTime(LocalDateTime.now());
            variableMapper.insert(var);
        }
    }

    private String inferType(Object value) {
        if (value == null) return "null";
        if (value instanceof Integer) return "integer";
        if (value instanceof Long) return "long";
        if (value instanceof Double || value instanceof Float) return "double";
        if (value instanceof Boolean) return "boolean";
        return "string";
    }

    private Object convertValue(String value, String type) {
        if (value == null) return null;
        return switch (type) {
            case "integer" -> Integer.parseInt(value);
            case "long" -> Long.parseLong(value);
            case "double" -> Double.parseDouble(value);
            case "boolean" -> Boolean.parseBoolean(value);
            default -> value;
        };
    }
}
