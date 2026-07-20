package com.flow.engine.parser;

import com.flow.engine.common.BusinessException;
import com.flow.engine.common.ErrorCode;
import com.flow.engine.common.utils.ExpressionUtils;
import com.flow.engine.common.utils.JsonUtils;
import com.flow.engine.dto.DataModelRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据模型解析器（ISSUE-010，TRD §4.3.1）
 * <p>
 * 负责：
 * 1. 解析数据模型 JSON
 * 2. 校验模型结构（子表数 ≤ 10，字段数 ≤ 50，必填字段等）
 * 3. 计算字段求值
 */
@Slf4j
@Component
public class DataModelParser {

    /** 子表数量上限 */
    private static final int MAX_SUB_TABLES = 10;
    /** 单子表字段数量上限 */
    private static final int MAX_FIELDS_PER_TABLE = 50;

    /**
     * 解析数据模型 JSON
     */
    public DataModelRequest parse(String modelJson) {
        if (modelJson == null || modelJson.isBlank()) {
            throw new BusinessException(ErrorCode.MODEL_VALIDATION_FAILED, "模型JSON不能为空");
        }

        DataModelRequest model;
        try {
            model = JsonUtils.fromJson(modelJson, DataModelRequest.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.MODEL_VALIDATION_FAILED, "模型JSON解析失败: " + e.getMessage());
        }

        if (model == null) {
            throw new BusinessException(ErrorCode.MODEL_VALIDATION_FAILED, "模型JSON解析结果为空");
        }

        return model;
    }

    /**
     * 校验数据模型
     *
     * @return 校验错误列表，空列表表示通过
     */
    public List<String> validate(DataModelRequest model) {
        List<String> errors = new ArrayList<>();

        if (model.getModelKey() == null || model.getModelKey().isBlank()) {
            errors.add("modelKey不能为空");
        }
        if (model.getModelName() == null || model.getModelName().isBlank()) {
            errors.add("modelName不能为空");
        }

        // 校验主表
        if (model.getMainTable() == null) {
            errors.add("主表定义不能为空");
        } else {
            validateTable(model.getMainTable(), "主表", errors);
        }

        // 校验子表数量
        if (model.getSubTables() != null) {
            if (model.getSubTables().size() > MAX_SUB_TABLES) {
                errors.add("子表数量(" + model.getSubTables().size() + ")超过上限" + MAX_SUB_TABLES);
            }
            for (int i = 0; i < model.getSubTables().size(); i++) {
                validateTable(model.getSubTables().get(i), "子表[" + i + "]", errors);
            }
        }

        return errors;
    }

    /**
     * 校验表定义
     */
    private void validateTable(DataModelRequest.TableDefinition table, String tableName, List<String> errors) {
        if (table.getTableName() == null || table.getTableName().isBlank()) {
            errors.add(tableName + "的tableName不能为空");
        }
        if (table.getFields() == null || table.getFields().isEmpty()) {
            errors.add(tableName + "至少需要一个字段");
            return;
        }
        if (table.getFields().size() > MAX_FIELDS_PER_TABLE) {
            errors.add(tableName + "字段数量(" + table.getFields().size() + ")超过上限" + MAX_FIELDS_PER_TABLE);
        }

        Set<String> fieldKeys = new HashSet<>();
        for (DataModelRequest.FieldDefinition field : table.getFields()) {
            if (field.getFieldKey() == null || field.getFieldKey().isBlank()) {
                errors.add(tableName + "中存在空字段Key");
                continue;
            }
            if (!fieldKeys.add(field.getFieldKey())) {
                errors.add(tableName + "字段Key重复: " + field.getFieldKey());
            }
            if (field.getLabel() == null || field.getLabel().isBlank()) {
                errors.add(tableName + "字段 " + field.getFieldKey() + " 缺少label");
            }
            if (field.getType() == null || field.getType().isBlank()) {
                errors.add(tableName + "字段 " + field.getFieldKey() + " 缺少type");
            }
            // 计算字段必须有表达式
            if ("computed".equals(field.getType())) {
                if (field.getExpression() == null || field.getExpression().isBlank()) {
                    errors.add(tableName + "计算字段 " + field.getFieldKey() + " 缺少expression");
                }
            }
        }
    }

    /**
     * 计算计算字段
     * <p>
     * 支持两种表达式：
     * 1. 行内计算：如 "price * quantity"，在当前行数据上下文中求值
     * 2. 聚合计算：如 "sum(items.subtotal)"，对子表数据求和
     *
     * @param expression 计算表达式
     * @param rowData    当前行数据
     * @param allData    完整实例数据（含子表）
     * @return 计算结果
     */
    public Object computeField(String expression, Map<String, Object> rowData, Map<String, Object> allData) {
        if (expression == null || expression.isBlank()) {
            return null;
        }

        try {
            // 检查是否是聚合表达式：sum(xxx.field)
            if (expression.matches("(?i)sum\\(\\w+\\.\\w+\\)")) {
                return computeAggregate(expression, allData);
            }

            // 行内计算：在行数据上下文中求值
            Map<String, Object> context = new HashMap<>();
            if (rowData != null) {
                context.putAll(rowData);
            }
            // 也加入全局数据
            if (allData != null) {
                for (Map.Entry<String, Object> entry : allData.entrySet()) {
                    if (!(entry.getValue() instanceof List)) {
                        context.putIfAbsent(entry.getKey(), entry.getValue());
                    }
                }
            }

            return ExpressionUtils.eval(expression, context);
        } catch (Exception e) {
            log.warn("计算字段表达式求值失败: {}, error: {}", expression, e.getMessage());
            return null;
        }
    }

    /**
     * 计算聚合表达式，如 sum(items.subtotal)
     */
    private Object computeAggregate(String expression, Map<String, Object> allData) {
        // 解析 sum(tableName.fieldName)
        String inner = expression.replaceAll("(?i)sum\\((.+)\\)", "$1");
        String[] parts = inner.split("\\.");
        if (parts.length != 2) {
            log.warn("聚合表达式格式错误: {}", expression);
            return null;
        }

        String tableName = parts[0];
        String fieldName = parts[1];

        Object tableData = allData != null ? allData.get(tableName) : null;
        if (!(tableData instanceof List)) {
            return BigDecimal.ZERO;
        }

        BigDecimal sum = BigDecimal.ZERO;
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) tableData;
        for (Map<String, Object> row : rows) {
            Object val = row.get(fieldName);
            if (val != null) {
                try {
                    sum = sum.add(new BigDecimal(String.valueOf(val)));
                } catch (NumberFormatException e) {
                    log.warn("聚合计算: 字段 {} 值 {} 无法转为数字", fieldName, val);
                }
            }
        }
        return sum;
    }

    /**
     * 对实例数据执行所有计算字段更新
     *
     * @param model   数据模型定义
     * @param allData 完整实例数据
     * @return 更新后的数据（含计算字段结果）
     */
    public Map<String, Object> computeAllFields(DataModelRequest model, Map<String, Object> allData) {
        if (model == null || allData == null) return allData;

        Map<String, Object> result = new LinkedHashMap<>(allData);

        // 1. 计算子表行内计算字段
        if (model.getSubTables() != null) {
            for (DataModelRequest.TableDefinition subTable : model.getSubTables()) {
                String tableName = subTable.getTableName();
                Object tableData = allData.get(tableName);
                if (!(tableData instanceof List)) continue;

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> rows = new ArrayList<>((List<Map<String, Object>>) tableData);

                // 计算每行的计算字段
                for (Map<String, Object> row : rows) {
                    for (DataModelRequest.FieldDefinition field : subTable.getFields()) {
                        if ("computed".equals(field.getType()) && field.getExpression() != null) {
                            Object computed = computeField(field.getExpression(), row, allData);
                            row.put(field.getFieldKey(), computed);
                        }
                    }
                }
                result.put(tableName, rows);
            }
        }

        // 2. 计算主表计算字段
        if (model.getMainTable() != null) {
            for (DataModelRequest.FieldDefinition field : model.getMainTable().getFields()) {
                if ("computed".equals(field.getType()) && field.getExpression() != null) {
                    Object computed = computeField(field.getExpression(), null, result);
                    result.put(field.getFieldKey(), computed);
                }
            }
        }

        return result;
    }

    /**
     * 将模型定义序列化为 JSON
     */
    public String toJson(DataModelRequest model) {
        return JsonUtils.toJson(model);
    }

    /**
     * 从 JSON 解析出主表和子表定义
     */
    public DataModelRequest fromJson(String modelJson) {
        return parse(modelJson);
    }
}
