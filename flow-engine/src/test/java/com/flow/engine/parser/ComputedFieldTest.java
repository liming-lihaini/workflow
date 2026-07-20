package com.flow.engine.parser;

import com.flow.engine.dto.DataModelRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 计算字段单元测试（ISSUE-010）
 */
@DisplayName("计算字段测试")
class ComputedFieldTest {

    private DataModelParser parser;

    @BeforeEach
    void setUp() {
        parser = new DataModelParser();
    }

    @Test
    @DisplayName("行内计算字段：price * quantity")
    void testRowComputedField() {
        String expression = "price * quantity";
        Map<String, Object> rowData = new HashMap<>();
        rowData.put("price", 10);
        rowData.put("quantity", 5);

        Object result = parser.computeField(expression, rowData, null);
        assertNotNull(result);
        assertEquals(50, ((Number) result).intValue());
    }

    @Test
    @DisplayName("聚合计算字段：sum(items.subtotal)")
    void testAggregateComputedField() {
        String expression = "sum(items.subtotal)";
        Map<String, Object> allData = new HashMap<>();

        List<Map<String, Object>> items = new ArrayList<>();
        items.add(createRow("subtotal", 100));
        items.add(createRow("subtotal", 200));
        items.add(createRow("subtotal", 300));
        allData.put("items", items);

        Object result = parser.computeField(expression, null, allData);
        assertNotNull(result);
        assertEquals(600, ((BigDecimal) result).intValue());
    }

    @Test
    @DisplayName("computeAllFields - 主表计算字段")
    void testComputeAllFieldsMainTable() {
        DataModelRequest model = buildModelWithComputedField();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("price", 10);
        data.put("quantity", 5);

        Map<String, Object> result = parser.computeAllFields(model, data);
        assertNotNull(result);
        assertEquals(50, ((Number) result.get("total")).intValue());
    }

    @Test
    @DisplayName("computeAllFields - 子表行内计算字段")
    void testComputeAllFieldsSubTableRow() {
        DataModelRequest model = buildModelWithSubTableComputedField();
        Map<String, Object> data = new LinkedHashMap<>();

        List<Map<String, Object>> items = new ArrayList<>();
        items.add(createRowWithValues("price", 10, "quantity", 3));
        items.add(createRowWithValues("price", 20, "quantity", 2));
        data.put("items", items);

        Map<String, Object> result = parser.computeAllFields(model, data);
        assertNotNull(result);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> resultItems = (List<Map<String, Object>>) result.get("items");
        assertEquals(30, ((Number) resultItems.get(0).get("subtotal")).intValue());
        assertEquals(40, ((Number) resultItems.get(1).get("subtotal")).intValue());
    }

    @Test
    @DisplayName("computeAllFields - 聚合计算字段")
    void testComputeAllFieldsAggregate() {
        DataModelRequest model = buildModelWithAggregateField();
        Map<String, Object> data = new LinkedHashMap<>();

        List<Map<String, Object>> items = new ArrayList<>();
        items.add(createRow("subtotal", 100));
        items.add(createRow("subtotal", 200));
        data.put("items", items);

        Map<String, Object> result = parser.computeAllFields(model, data);
        assertNotNull(result);
        assertEquals(300, ((BigDecimal) result.get("totalAmount")).intValue());
    }

    @Test
    @DisplayName("空表达式返回null")
    void testEmptyExpression() {
        Object result = parser.computeField(null, new HashMap<>(), null);
        assertNull(result);

        result = parser.computeField("", new HashMap<>(), null);
        assertNull(result);
    }

    @Test
    @DisplayName("无效表达式返回null")
    void testInvalidExpression() {
        Object result = parser.computeField("invalid expression !!!", new HashMap<>(), null);
        assertNull(result);
    }

    private Map<String, Object> createRow(String key, Object value) {
        Map<String, Object> row = new HashMap<>();
        row.put(key, value);
        return row;
    }

    private Map<String, Object> createRowWithValues(String key1, Object val1, String key2, Object val2) {
        Map<String, Object> row = new HashMap<>();
        row.put(key1, val1);
        row.put(key2, val2);
        return row;
    }

    private DataModelRequest buildModelWithComputedField() {
        DataModelRequest model = new DataModelRequest();
        model.setModelKey("test");
        model.setModelName("测试");

        DataModelRequest.TableDefinition mainTable = new DataModelRequest.TableDefinition();
        mainTable.setTableName("main");
        mainTable.setLabel("主表");

        List<DataModelRequest.FieldDefinition> fields = new ArrayList<>();
        fields.add(buildField("price", "单价", "number"));
        fields.add(buildField("quantity", "数量", "number"));
        fields.add(buildComputedField("total", "合计", "price * quantity"));
        mainTable.setFields(fields);

        model.setMainTable(mainTable);
        return model;
    }

    private DataModelRequest buildModelWithSubTableComputedField() {
        DataModelRequest model = new DataModelRequest();
        model.setModelKey("test");
        model.setModelName("测试");

        DataModelRequest.TableDefinition mainTable = new DataModelRequest.TableDefinition();
        mainTable.setTableName("main");
        mainTable.setLabel("主表");
        mainTable.setFields(new ArrayList<>());
        model.setMainTable(mainTable);

        DataModelRequest.TableDefinition subTable = new DataModelRequest.TableDefinition();
        subTable.setTableName("items");
        subTable.setLabel("明细");

        List<DataModelRequest.FieldDefinition> fields = new ArrayList<>();
        fields.add(buildField("price", "单价", "number"));
        fields.add(buildField("quantity", "数量", "number"));
        fields.add(buildComputedField("subtotal", "小计", "price * quantity"));
        subTable.setFields(fields);

        model.setSubTables(Arrays.asList(subTable));
        return model;
    }

    private DataModelRequest buildModelWithAggregateField() {
        DataModelRequest model = new DataModelRequest();
        model.setModelKey("test");
        model.setModelName("测试");

        DataModelRequest.TableDefinition mainTable = new DataModelRequest.TableDefinition();
        mainTable.setTableName("main");
        mainTable.setLabel("主表");

        List<DataModelRequest.FieldDefinition> fields = new ArrayList<>();
        fields.add(buildComputedField("totalAmount", "总金额", "sum(items.subtotal)"));
        mainTable.setFields(fields);
        model.setMainTable(mainTable);

        DataModelRequest.TableDefinition subTable = new DataModelRequest.TableDefinition();
        subTable.setTableName("items");
        subTable.setLabel("明细");
        List<DataModelRequest.FieldDefinition> subFields = new ArrayList<>();
        subFields.add(buildField("subtotal", "小计", "number"));
        subTable.setFields(subFields);
        model.setSubTables(Arrays.asList(subTable));

        return model;
    }

    private DataModelRequest.FieldDefinition buildField(String fieldKey, String label, String type) {
        DataModelRequest.FieldDefinition field = new DataModelRequest.FieldDefinition();
        field.setFieldKey(fieldKey);
        field.setLabel(label);
        field.setType(type);
        return field;
    }

    private DataModelRequest.FieldDefinition buildComputedField(String fieldKey, String label, String expression) {
        DataModelRequest.FieldDefinition field = new DataModelRequest.FieldDefinition();
        field.setFieldKey(fieldKey);
        field.setLabel(label);
        field.setType("computed");
        field.setExpression(expression);
        return field;
    }
}
