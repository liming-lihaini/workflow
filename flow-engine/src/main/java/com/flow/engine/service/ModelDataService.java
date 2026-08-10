package com.flow.engine.service;

import com.flow.engine.common.BusinessException;
import com.flow.engine.common.ErrorCode;
import com.flow.engine.common.utils.JsonUtils;
import com.flow.engine.dto.DataModelRequest;
import com.flow.engine.dto.DataModelResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 模型业务数据服务
 * <p>
 * 面向数据模型生成的物理表（dm_{modelKey}_{tableName}）提供通用 CRUD：
 * 关键字检索、分页查询、详情（含子表数据）、新增/修改/删除（主表 + 子表级联）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelDataService {

    private final DataModelService dataModelService;
    private final JdbcTemplate jdbcTemplate;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 分页查询主表数据（keyword 对所有模型字段模糊匹配）
     */
    public Map<String, Object> page(String modelKey, String keyword, int page, int size) {
        DataModelResponse model = requireModel(modelKey);
        String table = mainTableName(modelKey, model);
        List<String> columns = columnNames(model.getMainTable());

        StringBuilder where = new StringBuilder();
        List<Object> args = new ArrayList<>();
        if (keyword != null && !keyword.isBlank() && !columns.isEmpty()) {
            List<String> likes = new ArrayList<>();
            for (String col : columns) {
                likes.add("CAST(" + col + " AS CHAR) LIKE ?");
                args.add("%" + keyword.trim() + "%");
            }
            where.append(" WHERE (").append(String.join(" OR ", likes)).append(")");
        }

        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table + where, Long.class, args.toArray());
        int offset = Math.max(page - 1, 0) * size;
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(size);
        pageArgs.add(offset);
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
                "SELECT * FROM " + table + where + " ORDER BY id DESC LIMIT ? OFFSET ?", pageArgs.toArray());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", list);
        result.put("total", total == null ? 0 : total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    /**
     * 详情：主表行 + 各子表数据（以子表 tableName 为键）
     */
    public Map<String, Object> detail(String modelKey, Long id) {
        DataModelResponse model = requireModel(modelKey);
        String table = mainTableName(modelKey, model);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM " + table + " WHERE id = ?", id);
        if (rows.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "数据不存在: id=" + id);
        }
        Map<String, Object> detail = new LinkedHashMap<>(rows.get(0));

        if (model.getSubTables() != null) {
            Map<String, Object> subData = new LinkedHashMap<>();
            for (DataModelRequest.TableDefinition sub : model.getSubTables()) {
                String subTable = dataModelService.physicalTableName(modelKey, sub.getTableName());
                if (tableExists(subTable)) {
                    subData.put(sub.getTableName(),
                            jdbcTemplate.queryForList("SELECT * FROM " + subTable + " WHERE main_id = ? ORDER BY id", id));
                }
            }
            detail.put("subTables", subData);
        }
        return detail;
    }

    /**
     * 新增主表记录（可携带子表行：data 中以子表 tableName 为键的 List）
     */
    @Transactional
    public Map<String, Object> create(String modelKey, Map<String, Object> data) {
        DataModelResponse model = requireModel(modelKey);
        String table = mainTableName(modelKey, model);
        validateRequired(model.getMainTable(), data);

        Long id = insertRow(table, model.getMainTable(), data, null, null);
        saveSubRows(modelKey, model, id, data, false);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id);
        return result;
    }

    /**
     * 修改主表记录；data 中包含的子表键会整体替换该子表数据
     */
    @Transactional
    public void update(String modelKey, Long id, Map<String, Object> data) {
        DataModelResponse model = requireModel(modelKey);
        String table = mainTableName(modelKey, model);
        requireRow(table, id);
        validateRequired(model.getMainTable(), data);

        List<String> sets = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        for (DataModelRequest.FieldDefinition field : safeFields(model.getMainTable())) {
            if (data.containsKey(field.getFieldKey())) {
                sets.add(dataModelService.sanitizeIdentifier(field.getFieldKey()) + " = ?");
                args.add(toSqlValue(data.get(field.getFieldKey())));
            }
        }
        sets.add("update_time = ?");
        args.add(LocalDateTime.now().format(FORMATTER));
        args.add(id);
        jdbcTemplate.update("UPDATE " + table + " SET " + String.join(", ", sets) + " WHERE id = ?", args.toArray());

        saveSubRows(modelKey, model, id, data, true);
    }

    /**
     * 删除主表记录及其子表行
     */
    @Transactional
    public void delete(String modelKey, Long id) {
        DataModelResponse model = requireModel(modelKey);
        String table = mainTableName(modelKey, model);
        requireRow(table, id);

        if (model.getSubTables() != null) {
            for (DataModelRequest.TableDefinition sub : model.getSubTables()) {
                String subTable = dataModelService.physicalTableName(modelKey, sub.getTableName());
                if (tableExists(subTable)) {
                    jdbcTemplate.update("DELETE FROM " + subTable + " WHERE main_id = ?", id);
                }
            }
        }
        jdbcTemplate.update("DELETE FROM " + table + " WHERE id = ?", id);
    }

    /**
     * 流程结束回填写入：跳过必填校验，表未生成时静默跳过（返回 null）
     * <p>
     * REQUIRES_NEW：调用方（FormDataWriteBackService）以 try-catch 容忍回填失败，
     * 必须独立事务，否则失败时会把流程主事务标记为 rollback-only，
     * 导致提交时抛出 UnexpectedRollbackException。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long writeBackInsert(String modelKey, Map<String, Object> data) {
        DataModelResponse model = requireModel(modelKey);
        if (model.getMainTable() == null) {
            return null;
        }
        String table = dataModelService.physicalTableName(modelKey, model.getMainTable().getTableName());
        if (!tableExists(table)) {
            return null;
        }
        Long id = insertRow(table, model.getMainTable(), data, null, null);
        saveSubRows(modelKey, model, id, data, false);
        return id;
    }

    // ========== 内部方法 ==========

    /** 校验模型存在并返回定义 */
    private DataModelResponse requireModel(String modelKey) {
        return dataModelService.getModel(modelKey);
    }

    /** 主表物理表名，未生成表时给出明确提示 */
    private String mainTableName(String modelKey, DataModelResponse model) {
        if (model.getMainTable() == null) {
            throw new BusinessException(ErrorCode.MODEL_VALIDATION_FAILED, "主表定义不能为空");
        }
        String table = dataModelService.physicalTableName(modelKey, model.getMainTable().getTableName());
        if (!tableExists(table)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "数据表尚未生成，请先在数据模型中执行「生成表」操作");
        }
        return table;
    }

    /** 保存子表行：replace=true 时仅处理 data 中出现的子表键（先删后插） */
    private void saveSubRows(String modelKey, DataModelResponse model, Long mainId,
                             Map<String, Object> data, boolean replace) {
        if (model.getSubTables() == null) {
            return;
        }
        for (DataModelRequest.TableDefinition sub : model.getSubTables()) {
            Object subValue = data.get(sub.getTableName());
            if (replace && !data.containsKey(sub.getTableName())) {
                continue;
            }
            String subTable = dataModelService.physicalTableName(modelKey, sub.getTableName());
            if (!tableExists(subTable)) {
                continue;
            }
            if (replace) {
                jdbcTemplate.update("DELETE FROM " + subTable + " WHERE main_id = ?", mainId);
            }
            if (subValue instanceof List<?> rows) {
                for (Object row : rows) {
                    if (row instanceof Map<?, ?> rowMap) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> casted = (Map<String, Object>) rowMap;
                        insertRow(subTable, sub, casted, "main_id", mainId);
                    }
                }
            }
        }
    }

    /** 插入一行（白名单列 + 审计时间），返回自增 id */
    private Long insertRow(String table, DataModelRequest.TableDefinition def,
                           Map<String, Object> data, String extraColumn, Object extraValue) {
        List<String> cols = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        if (extraColumn != null) {
            cols.add(extraColumn);
            args.add(extraValue);
        }
        for (DataModelRequest.FieldDefinition field : safeFields(def)) {
            // id 为物理表内置自增主键，不参与 INSERT
            if ("id".equals(field.getFieldKey())) {
                continue;
            }
            if (data.containsKey(field.getFieldKey())) {
                cols.add(dataModelService.sanitizeIdentifier(field.getFieldKey()));
                args.add(toSqlValue(data.get(field.getFieldKey())));
            }
        }
        String now = LocalDateTime.now().format(FORMATTER);
        cols.add("create_time");
        args.add(now);
        cols.add("update_time");
        args.add(now);

        String placeholders = String.join(", ", java.util.Collections.nCopies(cols.size(), "?"));
        String sql = "INSERT INTO " + table + " (" + String.join(", ", cols) + ") VALUES (" + placeholders + ")";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        Object[] argArray = args.toArray();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < argArray.length; i++) {
                ps.setObject(i + 1, argArray[i]);
            }
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key != null ? key.longValue() : null;
    }

    /** 必填字段校验 */
    private void validateRequired(DataModelRequest.TableDefinition def, Map<String, Object> data) {
        for (DataModelRequest.FieldDefinition field : safeFields(def)) {
            if (Boolean.TRUE.equals(field.getRequired())) {
                Object value = data.get(field.getFieldKey());
                if (value == null || (value instanceof String s && s.isBlank())) {
                    throw new BusinessException(ErrorCode.PARAM_INVALID,
                            "必填字段不能为空: " + (field.getLabel() != null ? field.getLabel() : field.getFieldKey()));
                }
            }
        }
    }

    /** 复杂值（Map/List）序列化为 JSON 存储 */
    private Object toSqlValue(Object value) {
        if (value instanceof Map || value instanceof List) {
            return JsonUtils.toJson(value);
        }
        return value;
    }

    private List<DataModelRequest.FieldDefinition> safeFields(DataModelRequest.TableDefinition def) {
        return def.getFields() != null ? def.getFields() : List.of();
    }

    /** 清洗后的主表模型列名列表 */
    private List<String> columnNames(DataModelRequest.TableDefinition def) {
        List<String> columns = new ArrayList<>();
        for (DataModelRequest.FieldDefinition field : safeFields(def)) {
            columns.add(dataModelService.sanitizeIdentifier(field.getFieldKey()));
        }
        return columns;
    }

    private void requireRow(String table, Long id) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE id = ?", Integer.class, id);
        if (count == null || count == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "数据不存在: id=" + id);
        }
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
                Integer.class, tableName);
        return count != null && count > 0;
    }
}
