package com.flow.engine.parser;

import com.flow.engine.common.BusinessException;
import com.flow.engine.dto.DataModelRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DataModelParser 单元测试（ISSUE-010）
 */
@DisplayName("数据模型解析器测试")
class DataModelParserTest {

    private DataModelParser parser;

    @BeforeEach
    void setUp() {
        parser = new DataModelParser();
    }

    @Test
    @DisplayName("解析合法模型JSON")
    void testParseValidModel() {
        String json = buildValidModelJson("test_model", "测试模型");
        DataModelRequest model = parser.parse(json);

        assertNotNull(model);
        assertEquals("test_model", model.getModelKey());
        assertEquals("测试模型", model.getModelName());
        assertNotNull(model.getMainTable());
        assertEquals(3, model.getMainTable().getFields().size());
    }

    @Test
    @DisplayName("解析空JSON抛异常")
    void testParseEmptyJson() {
        assertThrows(BusinessException.class, () -> parser.parse(null));
        assertThrows(BusinessException.class, () -> parser.parse(""));
        assertThrows(BusinessException.class, () -> parser.parse("   "));
    }

    @Test
    @DisplayName("解析非法JSON抛异常")
    void testParseInvalidJson() {
        assertThrows(BusinessException.class, () -> parser.parse("{invalid json}"));
    }

    @Test
    @DisplayName("校验合法模型 - 无错误")
    void testValidateValidModel() {
        DataModelRequest model = buildValidModel("test_model", "测试模型");
        List<String> errors = parser.validate(model);
        assertTrue(errors.isEmpty(), "合法模型不应有校验错误: " + errors);
    }

    @Test
    @DisplayName("校验缺少modelKey")
    void testValidateMissingModelKey() {
        DataModelRequest model = buildValidModel(null, "测试模型");
        List<String> errors = parser.validate(model);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("modelKey")));
    }

    @Test
    @DisplayName("校验缺少modelName")
    void testValidateMissingModelName() {
        DataModelRequest model = buildValidModel("test_model", null);
        List<String> errors = parser.validate(model);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("modelName")));
    }

    @Test
    @DisplayName("校验缺少主表")
    void testValidateMissingMainTable() {
        DataModelRequest model = buildValidModel("test_model", "测试模型");
        model.setMainTable(null);
        List<String> errors = parser.validate(model);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("主表")));
    }

    @Test
    @DisplayName("校验子表数量超限")
    void testValidateSubTableCountExceed() {
        DataModelRequest model = buildValidModel("test_model", "测试模型");
        List<DataModelRequest.TableDefinition> subTables = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            subTables.add(buildSubTable("sub" + i, 2));
        }
        model.setSubTables(subTables);

        List<String> errors = parser.validate(model);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("子表数量")));
    }

    @Test
    @DisplayName("校验子表字段数超限")
    void testValidateSubTableFieldCountExceed() {
        DataModelRequest model = buildValidModel("test_model", "测试模型");
        DataModelRequest.TableDefinition subTable = buildSubTable("sub1", 51);
        model.setSubTables(Arrays.asList(subTable));

        List<String> errors = parser.validate(model);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("字段数量")));
    }

    @Test
    @DisplayName("校验主表字段Key重复")
    void testValidateDuplicateFieldKey() {
        DataModelRequest model = buildValidModel("test_model", "测试模型");
        // 添加重复字段
        DataModelRequest.FieldDefinition dupField = new DataModelRequest.FieldDefinition();
        dupField.setFieldKey("field1");
        dupField.setLabel("重复字段");
        dupField.setType("text");
        model.getMainTable().getFields().add(dupField);

        List<String> errors = parser.validate(model);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("重复")));
    }

    @Test
    @DisplayName("校验计算字段缺少表达式")
    void testValidateComputedFieldMissingExpression() {
        DataModelRequest model = buildValidModel("test_model", "测试模型");
        DataModelRequest.FieldDefinition computedField = new DataModelRequest.FieldDefinition();
        computedField.setFieldKey("total");
        computedField.setLabel("合计");
        computedField.setType("computed");
        // 不设置 expression
        model.getMainTable().getFields().add(computedField);

        List<String> errors = parser.validate(model);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("expression")));
    }

    @Test
    @DisplayName("校验合法子表数量边界 - 10个子表")
    void testValidateSubTableCountBoundary() {
        DataModelRequest model = buildValidModel("test_model", "测试模型");
        List<DataModelRequest.TableDefinition> subTables = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            subTables.add(buildSubTable("sub" + i, 2));
        }
        model.setSubTables(subTables);

        List<String> errors = parser.validate(model);
        // 10个子表是上限，不应报错
        assertTrue(errors.stream().noneMatch(e -> e.contains("子表数量")));
    }

    private DataModelRequest buildValidModel(String modelKey, String modelName) {
        DataModelRequest model = new DataModelRequest();
        model.setModelKey(modelKey);
        model.setModelName(modelName);

        DataModelRequest.TableDefinition mainTable = new DataModelRequest.TableDefinition();
        mainTable.setTableName("main");
        mainTable.setLabel("主表");

        List<DataModelRequest.FieldDefinition> fields = new ArrayList<>();
        fields.add(buildField("field1", "字段1", "text", true));
        fields.add(buildField("field2", "字段2", "number", false));
        fields.add(buildField("field3", "字段3", "date", false));
        mainTable.setFields(fields);

        model.setMainTable(mainTable);
        model.setSubTables(new ArrayList<>());
        return model;
    }

    private DataModelRequest.FieldDefinition buildField(String fieldKey, String label, String type, boolean required) {
        DataModelRequest.FieldDefinition field = new DataModelRequest.FieldDefinition();
        field.setFieldKey(fieldKey);
        field.setLabel(label);
        field.setType(type);
        field.setRequired(required);
        return field;
    }

    private DataModelRequest.TableDefinition buildSubTable(String tableName, int fieldCount) {
        DataModelRequest.TableDefinition table = new DataModelRequest.TableDefinition();
        table.setTableName(tableName);
        table.setLabel("子表" + tableName);

        List<DataModelRequest.FieldDefinition> fields = new ArrayList<>();
        for (int i = 0; i < fieldCount; i++) {
            fields.add(buildField("field" + i, "字段" + i, "text", false));
        }
        table.setFields(fields);
        return table;
    }

    private String buildValidModelJson(String modelKey, String modelName) {
        return "{"
                + "\"modelKey\":\"" + modelKey + "\","
                + "\"modelName\":\"" + modelName + "\","
                + "\"mainTable\":{"
                + "  \"tableName\":\"main\","
                + "  \"label\":\"主表\","
                + "  \"fields\":["
                + "    {\"fieldKey\":\"field1\",\"label\":\"字段1\",\"type\":\"text\",\"required\":true},"
                + "    {\"fieldKey\":\"field2\",\"label\":\"字段2\",\"type\":\"number\",\"required\":false},"
                + "    {\"fieldKey\":\"field3\",\"label\":\"字段3\",\"type\":\"date\",\"required\":false}"
                + "  ]"
                + "},"
                + "\"subTables\":[]"
                + "}";
    }
}
