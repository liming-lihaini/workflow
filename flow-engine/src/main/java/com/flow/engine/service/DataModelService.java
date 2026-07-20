package com.flow.engine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flow.engine.common.BusinessException;
import com.flow.engine.common.ErrorCode;
import com.flow.engine.common.utils.JsonUtils;
import com.flow.engine.dto.DataModelRequest;
import com.flow.engine.dto.DataModelResponse;
import com.flow.engine.entity.DataModel;
import com.flow.engine.mapper.DataModelMapper;
import com.flow.engine.parser.DataModelParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据模型服务（ISSUE-010）
 * <p>
 * 提供数据模型的 CRUD、发布、表单绑定等功能。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataModelService {

    private final DataModelMapper dataModelMapper;
    private final DataModelParser dataModelParser;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 创建数据模型
     */
    @Transactional
    public DataModelResponse createModel(DataModelRequest request) {
        // 校验 modelKey 唯一性
        LambdaQueryWrapper<DataModel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DataModel::getModelKey, request.getModelKey());
        if (dataModelMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.MODEL_KEY_DUPLICATE);
        }

        // 校验模型结构
        List<String> errors = dataModelParser.validate(request);
        if (!errors.isEmpty()) {
            throw new BusinessException(ErrorCode.MODEL_VALIDATION_FAILED, String.join("; ", errors));
        }

        DataModel entity = new DataModel();
        entity.setModelKey(request.getModelKey());
        entity.setModelName(request.getModelName());
        entity.setModelJson(dataModelParser.toJson(request));
        entity.setVersion(1);
        entity.setStatus(0); // 草稿

        dataModelMapper.insert(entity);
        log.info("创建数据模型: modelKey={}", request.getModelKey());

        return toResponse(entity, request);
    }

    /**
     * 获取数据模型
     */
    public DataModelResponse getModel(String modelKey) {
        DataModel entity = getByModelKey(modelKey);
        DataModelRequest parsed = dataModelParser.parse(entity.getModelJson());
        return toResponse(entity, parsed);
    }

    /**
     * 获取数据模型列表
     */
    public List<DataModelResponse> listModels() {
        LambdaQueryWrapper<DataModel> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(DataModel::getUpdateTime);
        List<DataModel> models = dataModelMapper.selectList(wrapper);

        return models.stream().map(entity -> {
            try {
                DataModelRequest parsed = dataModelParser.parse(entity.getModelJson());
                return toResponse(entity, parsed);
            } catch (Exception e) {
                log.warn("解析模型JSON失败: modelKey={}", entity.getModelKey(), e);
                DataModelResponse resp = new DataModelResponse();
                resp.setId(entity.getId());
                resp.setModelKey(entity.getModelKey());
                resp.setModelName(entity.getModelName());
                resp.setModelJson(entity.getModelJson());
                resp.setVersion(entity.getVersion());
                resp.setStatus(entity.getStatus());
                resp.setCreateTime(entity.getCreateTime() != null ? entity.getCreateTime().format(FORMATTER) : null);
                resp.setUpdateTime(entity.getUpdateTime() != null ? entity.getUpdateTime().format(FORMATTER) : null);
                return resp;
            }
        }).collect(Collectors.toList());
    }

    /**
     * 更新数据模型（仅草稿状态可更新）
     */
    @Transactional
    public DataModelResponse updateModel(String modelKey, DataModelRequest request) {
        DataModel entity = getByModelKey(modelKey);

        if (entity.getStatus() != null && entity.getStatus() == 1) {
            throw new BusinessException(ErrorCode.MODEL_ALREADY_PUBLISHED);
        }

        // 校验模型结构
        List<String> errors = dataModelParser.validate(request);
        if (!errors.isEmpty()) {
            throw new BusinessException(ErrorCode.MODEL_VALIDATION_FAILED, String.join("; ", errors));
        }

        entity.setModelName(request.getModelName());
        entity.setModelJson(dataModelParser.toJson(request));
        dataModelMapper.updateById(entity);

        log.info("更新数据模型: modelKey={}", modelKey);
        return toResponse(entity, request);
    }

    /**
     * 删除数据模型（仅草稿状态可删除）
     */
    @Transactional
    public void deleteModel(String modelKey) {
        DataModel entity = getByModelKey(modelKey);

        if (entity.getStatus() != null && entity.getStatus() == 1) {
            throw new BusinessException(ErrorCode.MODEL_ALREADY_PUBLISHED);
        }

        dataModelMapper.deleteById(entity.getId());
        log.info("删除数据模型: modelKey={}", modelKey);
    }

    /**
     * 发布数据模型
     */
    @Transactional
    public DataModelResponse publishModel(String modelKey) {
        DataModel entity = getByModelKey(modelKey);

        if (entity.getStatus() != null && entity.getStatus() == 1) {
            throw new BusinessException(ErrorCode.MODEL_ALREADY_PUBLISHED);
        }

        entity.setStatus(1);
        entity.setVersion(entity.getVersion() == null ? 1 : entity.getVersion() + 1);
        dataModelMapper.updateById(entity);

        log.info("发布数据模型: modelKey={}, version={}", modelKey, entity.getVersion());

        DataModelRequest parsed = dataModelParser.parse(entity.getModelJson());
        return toResponse(entity, parsed);
    }

    /**
     * 根据 modelKey 获取模型实体
     */
    public DataModel getByModelKey(String modelKey) {
        LambdaQueryWrapper<DataModel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DataModel::getModelKey, modelKey);
        DataModel entity = dataModelMapper.selectOne(wrapper);
        if (entity == null) {
            throw new BusinessException(ErrorCode.MODEL_NOT_FOUND);
        }
        return entity;
    }

    /**
     * 根据模型定义生成表单字段（表单绑定数据模型）
     */
    public List<FieldMapping> generateFormFields(String modelKey) {
        DataModel entity = getByModelKey(modelKey);
        DataModelRequest model = dataModelParser.parse(entity.getModelJson());

        List<FieldMapping> mappings = new ArrayList<>();

        // 主表字段
        if (model.getMainTable() != null && model.getMainTable().getFields() != null) {
            for (DataModelRequest.FieldDefinition field : model.getMainTable().getFields()) {
                FieldMapping mapping = new FieldMapping();
                mapping.setFieldKey(field.getFieldKey());
                mapping.setLabel(field.getLabel());
                mapping.setType(field.getType());
                mapping.setRequired(field.getRequired() != null && field.getRequired());
                mapping.setSource("mainTable");
                mappings.add(mapping);
            }
        }

        // 子表字段
        if (model.getSubTables() != null) {
            for (DataModelRequest.TableDefinition subTable : model.getSubTables()) {
                if (subTable.getFields() == null) continue;
                for (DataModelRequest.FieldDefinition field : subTable.getFields()) {
                    FieldMapping mapping = new FieldMapping();
                    mapping.setFieldKey(subTable.getTableName() + "." + field.getFieldKey());
                    mapping.setLabel(field.getLabel());
                    mapping.setType(field.getType());
                    mapping.setRequired(field.getRequired() != null && field.getRequired());
                    mapping.setSource("subTable:" + subTable.getTableName());
                    mappings.add(mapping);
                }
            }
        }

        return mappings;
    }

    private DataModelResponse toResponse(DataModel entity, DataModelRequest parsed) {
        DataModelResponse resp = new DataModelResponse();
        resp.setId(entity.getId());
        resp.setModelKey(entity.getModelKey());
        resp.setModelName(entity.getModelName());
        resp.setModelJson(entity.getModelJson());
        resp.setVersion(entity.getVersion());
        resp.setStatus(entity.getStatus());
        resp.setMainTable(parsed.getMainTable());
        resp.setSubTables(parsed.getSubTables());
        resp.setCreateTime(entity.getCreateTime() != null ? entity.getCreateTime().format(FORMATTER) : null);
        resp.setUpdateTime(entity.getUpdateTime() != null ? entity.getUpdateTime().format(FORMATTER) : null);
        return resp;
    }

    /**
     * 字段映射信息
     */
    @lombok.Data
    public static class FieldMapping {
        private String fieldKey;
        private String label;
        private String type;
        private boolean required;
        private String source;
    }
}
