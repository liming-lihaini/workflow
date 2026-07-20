package com.flow.engine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flow.engine.common.BusinessException;
import com.flow.engine.common.ErrorCode;
import com.flow.engine.common.utils.JsonUtils;
import com.flow.engine.dto.DataModelRequest;
import com.flow.engine.dto.ModelInstanceRequest;
import com.flow.engine.dto.ModelInstanceResponse;
import com.flow.engine.entity.DataModel;
import com.flow.engine.entity.ModelInstance;
import com.flow.engine.mapper.ModelInstanceMapper;
import com.flow.engine.parser.DataModelParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 模型实例管理器（ISSUE-010，TRD §4.3.2）
 * <p>
 * 负责：
 * 1. 创建/更新模型实例
 * 2. 计算字段自动求值
 * 3. 数据校验
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelInstanceManager {

    private final ModelInstanceMapper modelInstanceMapper;
    private final DataModelService dataModelService;
    private final DataModelParser dataModelParser;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 创建模型实例
     */
    @Transactional
    public ModelInstanceResponse createInstance(ModelInstanceRequest request) {
        DataModel model = dataModelService.getByModelKey(request.getModelKey());
        DataModelRequest modelDef = dataModelParser.parse(model.getModelJson());

        // 校验数据
        List<String> errors = validate(modelDef, request.getData());
        if (!errors.isEmpty()) {
            throw new BusinessException(ErrorCode.MODEL_VALIDATION_FAILED, String.join("; ", errors));
        }

        // 计算计算字段
        Map<String, Object> computedData = dataModelParser.computeAllFields(modelDef, request.getData());

        ModelInstance entity = new ModelInstance();
        entity.setModelKey(request.getModelKey());
        entity.setModelInstanceId(UUID.randomUUID().toString());
        entity.setProcessInstanceId(request.getProcessInstanceId());
        entity.setDataJson(JsonUtils.toJson(computedData));

        modelInstanceMapper.insert(entity);
        log.info("创建模型实例: modelKey={}, instanceId={}", entity.getModelKey(), entity.getModelInstanceId());

        return toResponse(entity, computedData);
    }

    /**
     * 更新模型实例
     */
    @Transactional
    public ModelInstanceResponse updateInstance(String instanceId, Map<String, Object> data) {
        ModelInstance entity = getByInstanceId(instanceId);
        DataModel model = dataModelService.getByModelKey(entity.getModelKey());
        DataModelRequest modelDef = dataModelParser.parse(model.getModelJson());

        // 合并数据：旧数据 + 新数据
        Map<String, Object> oldData = parseDataJson(entity.getDataJson());
        Map<String, Object> mergedData = new LinkedHashMap<>(oldData);
        if (data != null) {
            mergedData.putAll(data);
        }

        // 校验数据
        List<String> errors = validate(modelDef, mergedData);
        if (!errors.isEmpty()) {
            throw new BusinessException(ErrorCode.MODEL_VALIDATION_FAILED, String.join("; ", errors));
        }

        // 计算计算字段
        Map<String, Object> computedData = dataModelParser.computeAllFields(modelDef, mergedData);

        entity.setDataJson(JsonUtils.toJson(computedData));
        modelInstanceMapper.updateById(entity);

        log.info("更新模型实例: instanceId={}", instanceId);
        return toResponse(entity, computedData);
    }

    /**
     * 获取模型实例
     */
    public ModelInstanceResponse getInstance(String instanceId) {
        ModelInstance entity = getByInstanceId(instanceId);
        Map<String, Object> data = parseDataJson(entity.getDataJson());
        return toResponse(entity, data);
    }

    /**
     * 根据流程实例ID获取模型实例
     */
    public ModelInstanceResponse getInstanceByProcessInstanceId(Long processInstanceId) {
        LambdaQueryWrapper<ModelInstance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModelInstance::getProcessInstanceId, processInstanceId);
        wrapper.orderByDesc(ModelInstance::getCreateTime);
        wrapper.last("LIMIT 1");
        ModelInstance entity = modelInstanceMapper.selectOne(wrapper);
        if (entity == null) {
            throw new BusinessException(ErrorCode.MODEL_INSTANCE_NOT_FOUND);
        }
        Map<String, Object> data = parseDataJson(entity.getDataJson());
        return toResponse(entity, data);
    }

    /**
     * 归档模型实例（流程结束时调用）
     */
    @Transactional
    public void archiveInstance(String instanceId) {
        ModelInstance entity = getByInstanceId(instanceId);
        // 归档操作：标记实例为只读（这里简化处理，仅记录日志）
        log.info("归档模型实例: instanceId={}, modelKey={}", instanceId, entity.getModelKey());
    }

    /**
     * 校验实例数据
     */
    public List<String> validate(DataModelRequest modelDef, Map<String, Object> data) {
        List<String> errors = new ArrayList<>();

        if (modelDef == null || data == null) {
            return errors;
        }

        // 校验主表必填字段
        if (modelDef.getMainTable() != null && modelDef.getMainTable().getFields() != null) {
            for (DataModelRequest.FieldDefinition field : modelDef.getMainTable().getFields()) {
                if (Boolean.TRUE.equals(field.getRequired())) {
                    Object value = data.get(field.getFieldKey());
                    if (value == null || (value instanceof String && ((String) value).isBlank())) {
                        errors.add("主表字段 " + field.getFieldKey() + " 为必填项");
                    }
                }
            }
        }

        // 校验子表数据
        if (modelDef.getSubTables() != null) {
            for (DataModelRequest.TableDefinition subTable : modelDef.getSubTables()) {
                Object tableData = data.get(subTable.getTableName());
                if (tableData instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> rows = (List<Map<String, Object>>) tableData;
                    for (int i = 0; i < rows.size(); i++) {
                        Map<String, Object> row = rows.get(i);
                        if (subTable.getFields() != null) {
                            for (DataModelRequest.FieldDefinition field : subTable.getFields()) {
                                if (Boolean.TRUE.equals(field.getRequired()) && !"computed".equals(field.getType())) {
                                    Object value = row.get(field.getFieldKey());
                                    if (value == null || (value instanceof String && ((String) value).isBlank())) {
                                        errors.add("子表 " + subTable.getTableName() + " 第" + (i + 1) + "行字段 " + field.getFieldKey() + " 为必填项");
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return errors;
    }

    private ModelInstance getByInstanceId(String instanceId) {
        LambdaQueryWrapper<ModelInstance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModelInstance::getModelInstanceId, instanceId);
        ModelInstance entity = modelInstanceMapper.selectOne(wrapper);
        if (entity == null) {
            throw new BusinessException(ErrorCode.MODEL_INSTANCE_NOT_FOUND);
        }
        return entity;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseDataJson(String dataJson) {
        if (dataJson == null || dataJson.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return JsonUtils.getMapper().readValue(dataJson, LinkedHashMap.class);
        } catch (Exception e) {
            log.warn("解析实例数据JSON失败: {}", dataJson, e);
            return new LinkedHashMap<>();
        }
    }

    private ModelInstanceResponse toResponse(ModelInstance entity, Map<String, Object> data) {
        ModelInstanceResponse resp = new ModelInstanceResponse();
        resp.setId(entity.getId());
        resp.setModelKey(entity.getModelKey());
        resp.setModelInstanceId(entity.getModelInstanceId());
        resp.setProcessInstanceId(entity.getProcessInstanceId());
        resp.setData(data);
        resp.setCreateTime(entity.getCreateTime() != null ? entity.getCreateTime().format(FORMATTER) : null);
        resp.setUpdateTime(entity.getUpdateTime() != null ? entity.getUpdateTime().format(FORMATTER) : null);
        return resp;
    }
}
