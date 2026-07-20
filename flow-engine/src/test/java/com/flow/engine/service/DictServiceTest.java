package com.flow.engine.service;

import com.flow.engine.common.BusinessException;
import com.flow.engine.entity.DictItem;
import com.flow.engine.entity.DictType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 数据字典服务测试（ISSUE-015）
 */
@SpringBootTest
@DisplayName("数据字典服务测试")
public class DictServiceTest {

    @Autowired
    private DictService dictService;

    @Test
    @DisplayName("创建字典类型")
    void testCreateDictType() {
        DictType dictType = new DictType();
        dictType.setDictName("测试字典_" + System.currentTimeMillis());
        dictType.setDictCode("test_dict_" + System.currentTimeMillis());
        dictType.setDictType(2); // 业务自定义
        dictType.setDescription("测试字典描述");
        
        DictType created = dictService.createDictType(dictType);
        assertNotNull(created.getId());
        assertEquals(1, created.getStatus());
    }

    @Test
    @DisplayName("字典编码重复校验")
    void testDictCodeDuplicate() {
        String code = "dup_code_" + System.currentTimeMillis();
        
        DictType dictType1 = new DictType();
        dictType1.setDictName("字典1");
        dictType1.setDictCode(code);
        dictService.createDictType(dictType1);
        
        DictType dictType2 = new DictType();
        dictType2.setDictName("字典2");
        dictType2.setDictCode(code);
        
        assertThrows(BusinessException.class, () -> dictService.createDictType(dictType2));
    }

    @Test
    @DisplayName("查询字典类型列表")
    void testListDictTypes() {
        List<DictType> types = dictService.getDictTypes(null, null, null, null);
        assertFalse(types.isEmpty());
    }

    @Test
    @DisplayName("按名称模糊查询字典类型")
    void testQueryDictTypesByName() {
        String name = "流程状态";
        List<DictType> types = dictService.getDictTypes(name, null, null, null);
        assertFalse(types.isEmpty());
    }

    @Test
    @DisplayName("创建字典项")
    void testCreateDictItem() {
        // 先创建字典类型
        DictType dictType = new DictType();
        dictType.setDictName("测试字典项_" + System.currentTimeMillis());
        dictType.setDictCode("test_item_" + System.currentTimeMillis());
        dictType = dictService.createDictType(dictType);
        
        // 创建字典项
        DictItem dictItem = new DictItem();
        dictItem.setDictTypeId(dictType.getId());
        dictItem.setItemText("测试项");
        dictItem.setItemValue("test_value");
        dictItem.setSortOrder(1);
        
        DictItem created = dictService.createDictItem(dictItem);
        assertNotNull(created.getId());
        assertEquals(1, created.getStatus());
    }

    @Test
    @DisplayName("根据字典类型ID获取字典项")
    void testGetDictItemsByTypeId() {
        // 创建字典类型
        DictType dictType = new DictType();
        dictType.setDictName("按类型查询_" + System.currentTimeMillis());
        dictType.setDictCode("by_type_" + System.currentTimeMillis());
        dictType = dictService.createDictType(dictType);
        
        // 创建字典项
        DictItem item1 = new DictItem();
        item1.setDictTypeId(dictType.getId());
        item1.setItemText("项1");
        item1.setItemValue("value1");
        dictService.createDictItem(item1);
        
        DictItem item2 = new DictItem();
        item2.setDictTypeId(dictType.getId());
        item2.setItemText("项2");
        item2.setItemValue("value2");
        dictService.createDictItem(item2);
        
        // 查询
        List<DictItem> items = dictService.getDictItemsByTypeId(dictType.getId());
        assertEquals(2, items.size());
    }

    @Test
    @DisplayName("根据字典编码获取字典项")
    void testGetDictItemsByCode() {
        // 使用系统内置字典测试
        List<DictItem> items = dictService.getDictItemsByCode("process_status");
        assertFalse(items.isEmpty());
    }

    @Test
    @DisplayName("删除业务自定义字典类型成功")
    void testDeleteCustomDictType() {
        DictType dictType = new DictType();
        dictType.setDictName("可删除字典_" + System.currentTimeMillis());
        dictType.setDictCode("deletable_" + System.currentTimeMillis());
        dictType.setDictType(2); // 业务自定义
        final DictType created = dictService.createDictType(dictType);
        
        // 删除应该成功
        assertDoesNotThrow(() -> dictService.deleteDictType(created.getId()));
    }

    @Test
    @DisplayName("删除系统内置字典类型被拒")
    void testDeleteBuiltinDictType() {
        // 查找系统内置字典
        List<DictType> builtinTypes = dictService.getDictTypes(null, null, 1, null);
        assertFalse(builtinTypes.isEmpty());
        
        DictType builtinType = builtinTypes.get(0);
        
        // 删除应该失败
        assertThrows(BusinessException.class, () -> dictService.deleteDictType(builtinType.getId()));
    }

    @Test
    @DisplayName("更新字典项")
    void testUpdateDictItem() {
        // 创建字典类型
        DictType dictType = new DictType();
        dictType.setDictName("更新测试_" + System.currentTimeMillis());
        dictType.setDictCode("update_test_" + System.currentTimeMillis());
        dictType = dictService.createDictType(dictType);
        
        // 创建字典项
        DictItem dictItem = new DictItem();
        dictItem.setDictTypeId(dictType.getId());
        dictItem.setItemText("原始文本");
        dictItem.setItemValue("original");
        dictItem = dictService.createDictItem(dictItem);
        
        // 更新
        DictItem updateItem = new DictItem();
        updateItem.setItemText("更新后文本");
        
        DictItem updated = dictService.updateDictItem(dictItem.getId(), updateItem);
        assertEquals("更新后文本", updated.getItemText());
    }

    @Test
    @DisplayName("删除字典项")
    void testDeleteDictItem() {
        // 创建字典类型
        DictType dictType = new DictType();
        dictType.setDictName("删除项测试_" + System.currentTimeMillis());
        dictType.setDictCode("delete_item_" + System.currentTimeMillis());
        final DictType createdType = dictService.createDictType(dictType);
        
        // 创建字典项
        DictItem dictItem = new DictItem();
        dictItem.setDictTypeId(createdType.getId());
        dictItem.setItemText("待删除项");
        dictItem.setItemValue("to_delete");
        final DictItem createdItem = dictService.createDictItem(dictItem);
        
        // 删除应该成功
        assertDoesNotThrow(() -> dictService.deleteDictItem(createdItem.getId()));
    }

    @Test
    @DisplayName("系统内置字典数据初始化验证")
    void testBuiltinDictDataInitialized() {
        // 验证系统内置字典类型已初始化
        List<DictType> builtinTypes = dictService.getDictTypes(null, null, 1, null);
        assertTrue(builtinTypes.size() >= 9, "系统内置字典类型应至少有9个");
        
        // 验证流程状态字典项已初始化
        List<DictItem> processStatusItems = dictService.getDictItemsByCode("process_status");
        assertTrue(processStatusItems.size() >= 4, "流程状态字典项应至少有4个");
        
        // 验证节点类型字典项已初始化
        List<DictItem> nodeTypeItems = dictService.getDictItemsByCode("node_type");
        assertTrue(nodeTypeItems.size() >= 8, "节点类型字典项应至少有8个");
    }
}
