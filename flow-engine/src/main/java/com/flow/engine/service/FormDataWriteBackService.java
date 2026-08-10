package com.flow.engine.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.flow.engine.common.utils.JsonUtils;
import com.flow.engine.dto.ProcessDefinitionResponse;
import com.flow.engine.entity.FormDefinition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 流程表单数据回填服务
 * <p>
 * 流程结束时，若流程绑定的表单关联了数据模型且模型已生成物理表（dm_ 前缀），
 * 依据表单字段的 modelField / 子表 subTableKey 绑定关系，
 * 将流程变量中的表单数据自动回填写入模型数据表（主表 + 子表）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FormDataWriteBackService {

    private final ProcessDefinitionService processDefinitionService;
    private final FormDefinitionService formDefinitionService;
    private final VariableService variableService;
    private final ModelDataService modelDataService;

    /**
     * 流程完成后执行回填（异常仅记录日志，不影响流程结束）
     */
    public void writeBackOnProcessCompleted(Long processInstanceId, String processKey) {
        try {
            Set<String> formKeys = resolveFormKeys(processKey);
            if (formKeys.isEmpty()) {
                return;
            }

            Map<String, Object> variables = variableService.getVariables(processInstanceId);
            if (variables == null || variables.isEmpty()) {
                return;
            }

            // 按模型聚合各表单的绑定数据（多个表单绑同一模型时合并为一条记录）
            Map<String, Map<String, Object>> modelDataMap = new LinkedHashMap<>();
            for (String formKey : formKeys) {
                collectFormBindings(formKey, variables, modelDataMap);
            }

            for (Map.Entry<String, Map<String, Object>> entry : modelDataMap.entrySet()) {
                String modelKey = entry.getKey();
                Map<String, Object> data = entry.getValue();
                if (data.isEmpty()) {
                    continue;
                }
                try {
                    Long id = modelDataService.writeBackInsert(modelKey, data);
                    if (id != null) {
                        log.info("[FormWriteBack] 流程 {} 表单数据已回填模型 {} 数据表, id={}",
                                processInstanceId, modelKey, id);
                    } else {
                        log.debug("[FormWriteBack] 模型 {} 尚未生成数据表，跳过回填", modelKey);
                    }
                } catch (Exception e) {
                    log.warn("[FormWriteBack] 模型 {} 回填失败: {}", modelKey, e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            log.warn("[FormWriteBack] 流程 {} 表单数据回填失败: {}", processInstanceId, e.getMessage(), e);
        }
    }

    /** 从流程定义 JSON 中提取绑定的表单 Key（userTask 节点级 + 流程顶层） */
    private Set<String> resolveFormKeys(String processKey) {
        Set<String> formKeys = new LinkedHashSet<>();
        try {
            ProcessDefinitionResponse def = processDefinitionService.getByKey(processKey);
            if (def == null || def.getProcessJson() == null) {
                return formKeys;
            }
            JsonNode pj = JsonUtils.getMapper().readTree(def.getProcessJson());
            for (JsonNode node : pj.path("nodes")) {
                if ("userTask".equals(node.path("type").asText())) {
                    String fk = textOrNull(node, "formKey");
                    if (fk == null) {
                        fk = textOrNull(node.path("properties"), "formKey");
                    }
                    if (fk != null) {
                        formKeys.add(fk);
                    }
                }
            }
            String topFk = textOrNull(pj, "formKey");
            if (topFk != null) {
                formKeys.add(topFk);
            }
        } catch (Exception e) {
            log.warn("[FormWriteBack] 解析流程定义表单绑定失败: processKey={}, {}", processKey, e.getMessage());
        }
        return formKeys;
    }

    /** 解析表单的模型绑定，将流程变量映射为模型数据（主表字段 modelField、子表 subTableKey） */
    private void collectFormBindings(String formKey, Map<String, Object> variables,
                                     Map<String, Map<String, Object>> modelDataMap) {
        FormDefinition form;
        try {
            form = formDefinitionService.getForm(formKey);
        } catch (Exception e) {
            log.debug("[FormWriteBack] 表单不存在，跳过: {}", formKey);
            return;
        }
        if (form == null || form.getModelKey() == null || form.getModelKey().isBlank()
                || form.getFormJson() == null) {
            return;
        }

        Map<String, Object> data = modelDataMap.computeIfAbsent(form.getModelKey(), k -> new LinkedHashMap<>());
        try {
            JsonNode root = JsonUtils.getMapper().readTree(form.getFormJson());
            for (JsonNode section : root.path("sections")) {
                for (JsonNode row : section.path("children")) {
                    for (JsonNode cell : row.path("cells")) {
                        for (JsonNode field : cell.path("fields")) {
                            mapField(field, variables, data);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[FormWriteBack] 解析表单 {} 绑定失败: {}", formKey, e.getMessage());
        }
    }

    /** 单个表单字段 → 模型数据映射 */
    private void mapField(JsonNode field, Map<String, Object> variables, Map<String, Object> data) {
        String formFieldKey = firstText(field, "field", "key", "id");
        if (formFieldKey == null) {
            return;
        }
        String type = field.path("type").asText();
        if ("subTable".equals(type)) {
            // 子表组件：绑定模型子表后列 fieldKey 与子表字段一致，行数据整体回填
            String subTableKey = textOrNull(field, "subTableKey");
            Object rows = variables.get(formFieldKey);
            if (subTableKey != null && rows instanceof List) {
                data.put(subTableKey, rows);
            }
        } else {
            String modelField = textOrNull(field, "modelField");
            Object value = variables.get(formFieldKey);
            if (modelField != null && value != null) {
                data.put(modelField, value);
            }
        }
    }

    private String textOrNull(JsonNode node, String name) {
        JsonNode v = node.path(name);
        if (v.isMissingNode() || v.isNull()) {
            return null;
        }
        String text = v.asText();
        return text.isBlank() ? null : text;
    }

    private String firstText(JsonNode node, String... names) {
        for (String name : names) {
            String v = textOrNull(node, name);
            if (v != null) {
                return v;
            }
        }
        return null;
    }
}
