package com.flow.engine.service;

import com.flow.engine.common.BusinessException;
import com.flow.engine.dto.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 模型实例集成测试（ISSUE-010）
 */
@SpringBootTest
@DisplayName("数据模型集成测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ModelInstanceIntegrationTest {

    @Autowired
    private DataModelService dataModelService;

    @Autowired
    private ModelInstanceManager modelInstanceManager;

    @Autowired
    private com.flow.engine.mapper.DataModelMapper dataModelMapper;

    @Autowired
    private com.flow.engine.mapper.ModelInstanceMapper modelInstanceMapper;

    private String modelKey;

    @BeforeEach
    void setUp() {
        // 使用唯一 modelKey 避免测试间冲突
        modelKey = "order_model_" + System.currentTimeMillis();
    }

    @Test
    @Order(1)
    @DisplayName("创建含主表+2子表的数据模型")
    void testCreateModelWithSubTables() {
        DataModelRequest request = buildOrderModel();
        DataModelResponse response = dataModelService.createModel(request);

        assertNotNull(response);
        assertEquals(modelKey, response.getModelKey());
        assertEquals("订单模型", response.getModelName());
        assertNotNull(response.getMainTable());
        assertEquals(3, response.getMainTable().getFields().size());
        assertNotNull(response.getSubTables());
        assertEquals(2, response.getSubTables().size());
    }

    @Test
    @Order(2)
    @DisplayName("创建模型实例 - 主表+子表数据")
    void testCreateInstance() {
        // 先创建并发布模型
        dataModelService.createModel(buildOrderModel());
        dataModelService.publishModel(modelKey);

        ModelInstanceRequest request = new ModelInstanceRequest();
        request.setModelKey(modelKey);
        request.setData(buildOrderData());

        ModelInstanceResponse response = modelInstanceManager.createInstance(request);

        assertNotNull(response);
        assertNotNull(response.getModelInstanceId());
        assertEquals(modelKey, response.getModelKey());
        assertNotNull(response.getData());

        // 验证计算字段
        assertNotNull(response.getData().get("totalAmount"));
    }

    @Test
    @Order(3)
    @DisplayName("获取模型实例")
    void testGetInstance() {
        // 创建并发布模型
        dataModelService.createModel(buildOrderModel());
        dataModelService.publishModel(modelKey);

        // 创建实例
        ModelInstanceRequest request = new ModelInstanceRequest();
        request.setModelKey(modelKey);
        request.setData(buildOrderData());
        ModelInstanceResponse created = modelInstanceManager.createInstance(request);

        // 获取实例
        ModelInstanceResponse response = modelInstanceManager.getInstance(created.getModelInstanceId());
        assertNotNull(response);
        assertEquals(created.getModelInstanceId(), response.getModelInstanceId());
    }

    @Test
    @Order(4)
    @DisplayName("更新模型实例 - 计算字段自动重算")
    void testUpdateInstance() {
        // 创建并发布模型
        dataModelService.createModel(buildOrderModel());
        dataModelService.publishModel(modelKey);

        // 创建实例
        ModelInstanceRequest request = new ModelInstanceRequest();
        request.setModelKey(modelKey);
        request.setData(buildOrderData());
        ModelInstanceResponse created = modelInstanceManager.createInstance(request);

        // 更新数据
        Map<String, Object> newData = new LinkedHashMap<>();
        List<Map<String, Object>> items = new ArrayList<>();
        items.add(createItem("产品A", 20, 5)); // 小计 100
        items.add(createItem("产品B", 30, 3)); // 小计 90
        newData.put("items", items);

        ModelInstanceResponse updated = modelInstanceManager.updateInstance(created.getModelInstanceId(), newData);
        assertNotNull(updated);

        // 验证计算字段重算
        assertNotNull(updated.getData().get("totalAmount"));
    }

    @Test
    @Order(5)
    @DisplayName("子表动态增删行")
    void testSubTableDynamicRows() {
        // 创建并发布模型
        dataModelService.createModel(buildOrderModel());
        dataModelService.publishModel(modelKey);

        ModelInstanceRequest request = new ModelInstanceRequest();
        request.setModelKey(modelKey);

        Map<String, Object> data = buildOrderData();
        request.setData(data);
        ModelInstanceResponse created = modelInstanceManager.createInstance(request);

        // 添加新行
        Map<String, Object> updateData = new LinkedHashMap<>();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) data.get("items");
        items.add(createItem("产品C", 50, 2)); // 新增行
        updateData.put("items", items);

        ModelInstanceResponse updated = modelInstanceManager.updateInstance(created.getModelInstanceId(), updateData);
        assertNotNull(updated);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> updatedItems = (List<Map<String, Object>>) updated.getData().get("items");
        assertEquals(3, updatedItems.size());
    }

    @Test
    @Order(6)
    @DisplayName("必填字段校验")
    void testRequiredFieldValidation() {
        // 创建并发布模型
        dataModelService.createModel(buildOrderModel());
        dataModelService.publishModel(modelKey);

        ModelInstanceRequest request = new ModelInstanceRequest();
        request.setModelKey(modelKey);

        // 完全空的 data，所有必填字段都缺失
        Map<String, Object> data = new LinkedHashMap<>();
        request.setData(data);

        BusinessException ex = assertThrows(BusinessException.class, () -> modelInstanceManager.createInstance(request));
        assertTrue(ex.getMessage().contains("必填") || ex.getMessage().contains("orderNo"));
    }

    @Test
    @Order(7)
    @DisplayName("归档模型实例")
    void testArchiveInstance() {
        // 创建并发布模型
        dataModelService.createModel(buildOrderModel());
        dataModelService.publishModel(modelKey);

        ModelInstanceRequest request = new ModelInstanceRequest();
        request.setModelKey(modelKey);
        request.setData(buildOrderData());
        ModelInstanceResponse created = modelInstanceManager.createInstance(request);

        // 归档不应抛异常
        assertDoesNotThrow(() -> modelInstanceManager.archiveInstance(created.getModelInstanceId()));
    }

    @Test
    @Order(8)
    @DisplayName("表单绑定模型 - 生成表单字段")
    void testGenerateFormFields() {
        // 创建模型
        dataModelService.createModel(buildOrderModel());

        List<DataModelService.FieldMapping> fields = dataModelService.generateFormFields(modelKey);

        assertNotNull(fields);
        assertFalse(fields.isEmpty());
        // 主表3个字段 + items子表4字段 + remarks子表1字段 = 8个
        assertEquals(8, fields.size());
    }

    @Test
    @Order(9)
    @DisplayName("modelKey唯一校验")
    void testModelKeyUnique() {
        // 先创建一个模型
        dataModelService.createModel(buildOrderModel());

        // 再次创建相同 modelKey 的模型应失败
        DataModelRequest request = buildOrderModel();
        assertThrows(BusinessException.class, () -> dataModelService.createModel(request));
    }

    @Test
    @Order(10)
    @DisplayName("子表数 >10 校验失败")
    void testSubTableExceedLimit() {
        DataModelRequest request = buildValidModel("exceed_model", "超限模型");
        List<DataModelRequest.TableDefinition> subTables = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            DataModelRequest.TableDefinition subTable = new DataModelRequest.TableDefinition();
            subTable.setTableName("sub" + i);
            subTable.setLabel("子表" + i);
            subTable.setFields(Arrays.asList(buildField("f1", "字段1", "text")));
            subTables.add(subTable);
        }
        request.setSubTables(subTables);

        assertThrows(BusinessException.class, () -> dataModelService.createModel(request));
    }

    private DataModelRequest buildOrderModel() {
        DataModelRequest model = new DataModelRequest();
        model.setModelKey(modelKey);
        model.setModelName("订单模型");

        // 主表
        DataModelRequest.TableDefinition mainTable = new DataModelRequest.TableDefinition();
        mainTable.setTableName("order");
        mainTable.setLabel("订单");

        List<DataModelRequest.FieldDefinition> mainFields = new ArrayList<>();
        mainFields.add(buildField("orderNo", "订单号", "text", true));
        mainFields.add(buildField("orderDate", "订单日期", "date", true));
        mainFields.add(buildComputedField("totalAmount", "总金额", "sum(items.subtotal)"));
        mainTable.setFields(mainFields);
        model.setMainTable(mainTable);

        // 子表1: 订单明细
        DataModelRequest.TableDefinition itemsTable = new DataModelRequest.TableDefinition();
        itemsTable.setTableName("items");
        itemsTable.setLabel("订单明细");

        List<DataModelRequest.FieldDefinition> itemFields = new ArrayList<>();
        itemFields.add(buildField("productName", "产品名称", "text", true));
        itemFields.add(buildField("price", "单价", "number", true));
        itemFields.add(buildField("quantity", "数量", "number", true));
        itemFields.add(buildComputedField("subtotal", "小计", "price * quantity"));
        itemsTable.setFields(itemFields);

        // 子表2: 备注
        DataModelRequest.TableDefinition remarksTable = new DataModelRequest.TableDefinition();
        remarksTable.setTableName("remarks");
        remarksTable.setLabel("备注");

        List<DataModelRequest.FieldDefinition> remarkFields = new ArrayList<>();
        remarkFields.add(buildField("content", "备注内容", "text", true));
        remarksTable.setFields(remarkFields);

        model.setSubTables(Arrays.asList(itemsTable, remarksTable));
        return model;
    }

    private Map<String, Object> buildOrderData() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderNo", "ORD-2024-001");
        data.put("orderDate", "2024-01-15");

        List<Map<String, Object>> items = new ArrayList<>();
        items.add(createItem("产品A", 10, 5));
        items.add(createItem("产品B", 20, 3));
        data.put("items", items);

        List<Map<String, Object>> remarks = new ArrayList<>();
        Map<String, Object> remark = new LinkedHashMap<>();
        remark.put("content", "请尽快发货");
        remarks.add(remark);
        data.put("remarks", remarks);

        return data;
    }

    private Map<String, Object> createItem(String productName, int price, int quantity) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("productName", productName);
        item.put("price", price);
        item.put("quantity", quantity);
        return item;
    }

    private DataModelRequest.FieldDefinition buildField(String fieldKey, String label, String type) {
        return buildField(fieldKey, label, type, false);
    }

    private DataModelRequest.FieldDefinition buildField(String fieldKey, String label, String type, boolean required) {
        DataModelRequest.FieldDefinition field = new DataModelRequest.FieldDefinition();
        field.setFieldKey(fieldKey);
        field.setLabel(label);
        field.setType(type);
        field.setRequired(required);
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

    private DataModelRequest buildValidModel(String modelKey, String modelName) {
        DataModelRequest model = new DataModelRequest();
        model.setModelKey(modelKey);
        model.setModelName(modelName);

        DataModelRequest.TableDefinition mainTable = new DataModelRequest.TableDefinition();
        mainTable.setTableName("main");
        mainTable.setLabel("主表");
        mainTable.setFields(Arrays.asList(buildField("f1", "字段1", "text")));
        model.setMainTable(mainTable);
        model.setSubTables(new ArrayList<>());

        return model;
    }
}
