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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
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
    private final JdbcTemplate jdbcTemplate;
    private final ModelMenuPermissionService modelMenuPermissionService;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 模型来源：系统内置（只读） */
    public static final String SOURCE_BUILTIN = "builtin";
    /** 模型来源：用户自定义 */
    public static final String SOURCE_CUSTOM = "custom";

    /** 系统内置模型只读守卫：禁止更新/删除/发布/生成表 */
    private void assertNotBuiltin(DataModel entity) {
        if (SOURCE_BUILTIN.equals(entity.getSource())) {
            throw new BusinessException(ErrorCode.MODEL_BUILTIN_READONLY);
        }
    }

    /** 合法数据库标识符（防 SQL 注入，表名/字段名清洗后必须匹配） */
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");

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

        // 校验模型名称唯一性
        validateModelNameUnique(request.getModelName(), null);

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
        entity.setSource(SOURCE_CUSTOM); // API 创建的均为用户自定义，内置模型由系统种子化

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
                resp.setSource(entity.getSource());
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
        assertNotBuiltin(entity);

        if (entity.getStatus() != null && entity.getStatus() == 1) {
            throw new BusinessException(ErrorCode.MODEL_ALREADY_PUBLISHED);
        }

        // 校验模型名称唯一性（排除自身）
        validateModelNameUnique(request.getModelName(), entity.getId());

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
        assertNotBuiltin(entity);

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
        assertNotBuiltin(entity);

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
     * 校验模型名称唯一性（excludeId 非空时排除自身，用于更新场景）
     */
    private void validateModelNameUnique(String modelName, Long excludeId) {
        if (modelName == null || modelName.isBlank()) {
            return;
        }
        LambdaQueryWrapper<DataModel> nameWrapper = new LambdaQueryWrapper<>();
        nameWrapper.eq(DataModel::getModelName, modelName);
        if (excludeId != null) {
            nameWrapper.ne(DataModel::getId, excludeId);
        }
        if (dataModelMapper.selectCount(nameWrapper) > 0) {
            throw new BusinessException(ErrorCode.MODEL_NAME_DUPLICATE);
        }
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

    /**
     * 依据模型定义生成数据库表（主表 + 子表，ISSUE-010 扩展）
     * <p>
     * 表命名：dm_{modelKey}_{tableName}（非法字符替换为下划线）；
     * 采用 CREATE TABLE IF NOT EXISTS 幂等执行，子表附加 main_id 关联列与索引。
     *
     * @return 生成结果：每张表的物理表名、是否已存在、字段数及执行的 DDL
     */
    @Transactional
    public Map<String, Object> generateTables(String modelKey) {
        DataModel entity = getByModelKey(modelKey);
        assertNotBuiltin(entity);
        DataModelRequest model = dataModelParser.parse(entity.getModelJson());
        if (model.getMainTable() == null) {
            throw new BusinessException(ErrorCode.MODEL_VALIDATION_FAILED, "主表定义不能为空");
        }

        List<Map<String, Object>> tables = new ArrayList<>();
        List<String> ddlList = new ArrayList<>();

        // 主表：id 主键 + 模型字段 + 审计时间列
        String mainTable = physicalTableName(modelKey, model.getMainTable().getTableName());
        buildAndExecute(mainTable, model.getMainTable(), null, tables, ddlList);

        // 子表：附加 main_id 关联主表，并建索引
        if (model.getSubTables() != null) {
            for (DataModelRequest.TableDefinition sub : model.getSubTables()) {
                String subTable = physicalTableName(modelKey, sub.getTableName());
                buildAndExecute(subTable, sub, mainTable, tables, ddlList);
            }
        }

        log.info("[DataModelService] 生成数据库表: modelKey={}, tables={}", modelKey,
                tables.stream().map(t -> String.valueOf(t.get("tableName"))).collect(Collectors.joining(",")));

        // 同步生成菜单项与按钮权限（幂等）
        List<String> createdPerms = modelMenuPermissionService.syncMenuAndPermissions(modelKey, entity.getModelName());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("modelKey", modelKey);
        result.put("tables", tables);
        result.put("ddl", ddlList);
        result.put("createdPermissions", createdPerms);
        return result;
    }

    /** 生成单张表的 DDL 并执行，记录结果 */
    private void buildAndExecute(String tableName, DataModelRequest.TableDefinition table,
                                 String parentTable, List<Map<String, Object>> tables, List<String> ddlList) {
        boolean existed = tableExists(tableName);

        StringBuilder ddl = new StringBuilder("CREATE TABLE IF NOT EXISTS ").append(tableName).append(" (\n");
        ddl.append("    id BIGINT AUTO_INCREMENT PRIMARY KEY");
        if (parentTable != null) {
            ddl.append(",\n    main_id BIGINT");
        }
        int fieldCount = 0;
        if (table.getFields() != null) {
            for (DataModelRequest.FieldDefinition field : table.getFields()) {
                // id 为物理表内置自增主键，跳过避免与主键列冲突
                if ("id".equals(field.getFieldKey())) {
                    continue;
                }
                String column = sanitizeIdentifier(field.getFieldKey());
                ddl.append(",\n    ").append(column).append(" ").append(sqlType(field.getType()));
                if (Boolean.TRUE.equals(field.getRequired())) {
                    ddl.append(" NOT NULL");
                }
                fieldCount++;
            }
        }
        ddl.append(",\n    create_time DATETIME,\n    update_time DATETIME\n)");

        jdbcTemplate.execute(ddl.toString());
        ddlList.add(ddl.toString());

        if (parentTable != null) {
            // MySQL 不支持 CREATE INDEX IF NOT EXISTS，先探测索引是否存在
            String indexName = "idx_" + tableName + "_main_id";
            Integer indexCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?",
                    Integer.class, tableName, indexName);
            if (indexCount == null || indexCount == 0) {
                String indexDdl = "CREATE INDEX " + indexName + " ON " + tableName + "(main_id)";
                jdbcTemplate.execute(indexDdl);
                ddlList.add(indexDdl);
            }
        }

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("tableName", tableName);
        info.put("label", table.getLabel());
        info.put("existed", existed);
        info.put("fieldCount", fieldCount);
        tables.add(info);
    }

    /** 物理表名：dm_{modelKey}_{tableName}，非法字符替换为下划线 */
    public String physicalTableName(String modelKey, String tableName) {
        String name = "dm_" + sanitizeIdentifier(modelKey) + "_" + sanitizeIdentifier(tableName);
        if (!IDENTIFIER_PATTERN.matcher(name).matches()) {
            throw new BusinessException(ErrorCode.MODEL_VALIDATION_FAILED, "非法表名: " + name);
        }
        return name;
    }

    /** 标识符清洗：非字母数字下划线替换为下划线，并校验合法性 */
    public String sanitizeIdentifier(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BusinessException(ErrorCode.MODEL_VALIDATION_FAILED, "表名/字段名不能为空");
        }
        String cleaned = raw.trim().replaceAll("[^A-Za-z0-9_]", "_");
        if (!IDENTIFIER_PATTERN.matcher(cleaned).matches()) {
            throw new BusinessException(ErrorCode.MODEL_VALIDATION_FAILED, "非法标识符: " + raw);
        }
        return cleaned;
    }

    /** 模型字段类型 → MySQL 列类型 */
    private String sqlType(String type) {
        if (type == null) return "TEXT";
        return switch (type) {
            case "number", "amount", "computed" -> "DECIMAL(18,4)";
            case "datetime" -> "DATETIME";
            default -> "TEXT"; // text/date/file/person/department 均以 TEXT 存储
        };
    }

    /** 判断 MySQL 表是否已存在 */
    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
                Integer.class, tableName);
        return count != null && count > 0;
    }

    private DataModelResponse toResponse(DataModel entity, DataModelRequest parsed) {
        DataModelResponse resp = new DataModelResponse();
        resp.setId(entity.getId());
        resp.setModelKey(entity.getModelKey());
        resp.setModelName(entity.getModelName());
        resp.setModelJson(entity.getModelJson());
        resp.setVersion(entity.getVersion());
        resp.setStatus(entity.getStatus());
        resp.setSource(entity.getSource());
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
