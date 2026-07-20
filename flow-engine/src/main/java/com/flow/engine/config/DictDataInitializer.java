package com.flow.engine.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flow.engine.entity.DictItem;
import com.flow.engine.entity.DictType;
import com.flow.engine.mapper.DictItemMapper;
import com.flow.engine.mapper.DictTypeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 系统内置字典数据初始化（ISSUE-015）
 * <p>
 * 应用启动时自动初始化系统内置字典数据。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(10)
public class DictDataInitializer implements CommandLineRunner {

    private final DictTypeMapper dictTypeMapper;
    private final DictItemMapper dictItemMapper;

    @Override
    public void run(String... args) {
        log.info("[DictDataInitializer] 开始初始化系统内置字典数据...");
        
        initDictTypes();
        initDictItems();
        
        log.info("[DictDataInitializer] 系统内置字典数据初始化完成");
    }

    private void initDictTypes() {
        // 1. 流程状态
        createDictTypeIfNotExists("流程状态", "process_status", 1, "流程实例的运行状态");
        
        // 2. 节点类型
        createDictTypeIfNotExists("节点类型", "node_type", 1, "流程定义中的节点类型");
        
        // 3. 任务状态
        createDictTypeIfNotExists("任务状态", "task_status", 1, "任务的处理状态");
        
        // 4. 会签模式
        createDictTypeIfNotExists("会签模式", "counter_sign_mode", 1, "会签节点的投票模式");
        
        // 5. 加签类型
        createDictTypeIfNotExists("加签类型", "sign_type", 1, "加签操作的类型");
        
        // 6. 数据权限范围
        createDictTypeIfNotExists("数据权限范围", "data_scope", 1, "角色的数据权限范围");
        
        // 7. 密级
        createDictTypeIfNotExists("密级", "security_level", 1, "用户和数据的密级");
        
        // 8. 部门类型
        createDictTypeIfNotExists("部门类型", "dept_type", 1, "部门的分类");
        
        // 9. 字典类型分类
        createDictTypeIfNotExists("字典类型分类", "dict_type_category", 1, "字典类型的分类：1-系统内置，2-业务自定义");
    }

    private void initDictItems() {
        // 流程状态
        DictType processStatusType = getDictTypeByCode("process_status");
        if (processStatusType != null) {
            createDictItemIfNotExists(processStatusType.getId(), "运行中", "running", 1);
            createDictItemIfNotExists(processStatusType.getId(), "已完成", "completed", 2);
            createDictItemIfNotExists(processStatusType.getId(), "已终止", "terminated", 3);
            createDictItemIfNotExists(processStatusType.getId(), "已挂起", "suspended", 4);
        }
        
        // 节点类型
        DictType nodeTypeType = getDictTypeByCode("node_type");
        if (nodeTypeType != null) {
            createDictItemIfNotExists(nodeTypeType.getId(), "开始节点", "start", 1);
            createDictItemIfNotExists(nodeTypeType.getId(), "结束节点", "end", 2);
            createDictItemIfNotExists(nodeTypeType.getId(), "用户任务", "userTask", 3);
            createDictItemIfNotExists(nodeTypeType.getId(), "服务任务", "serviceTask", 4);
            createDictItemIfNotExists(nodeTypeType.getId(), "脚本任务", "scriptTask", 5);
            createDictItemIfNotExists(nodeTypeType.getId(), "排他网关", "exclusiveGateway", 6);
            createDictItemIfNotExists(nodeTypeType.getId(), "并行网关", "parallelGateway", 7);
            createDictItemIfNotExists(nodeTypeType.getId(), "包容网关", "inclusiveGateway", 8);
            createDictItemIfNotExists(nodeTypeType.getId(), "子流程", "subProcess", 9);
            createDictItemIfNotExists(nodeTypeType.getId(), "会签节点", "counterSign", 10);
        }
        
        // 任务状态
        DictType taskStatusType = getDictTypeByCode("task_status");
        if (taskStatusType != null) {
            createDictItemIfNotExists(taskStatusType.getId(), "待处理", "pending", 1);
            createDictItemIfNotExists(taskStatusType.getId(), "已完成", "completed", 2);
            createDictItemIfNotExists(taskStatusType.getId(), "已驳回", "rejected", 3);
            createDictItemIfNotExists(taskStatusType.getId(), "已撤回", "withdrawn", 4);
            createDictItemIfNotExists(taskStatusType.getId(), "已转办", "transferred", 5);
            createDictItemIfNotExists(taskStatusType.getId(), "已委托", "delegated", 6);
        }
        
        // 会签模式
        DictType counterSignModeType = getDictTypeByCode("counter_sign_mode");
        if (counterSignModeType != null) {
            createDictItemIfNotExists(counterSignModeType.getId(), "一票通过", "any", 1);
            createDictItemIfNotExists(counterSignModeType.getId(), "全票通过", "all", 2);
            createDictItemIfNotExists(counterSignModeType.getId(), "比例通过", "ratio", 3);
            createDictItemIfNotExists(counterSignModeType.getId(), "票数通过", "count", 4);
        }
        
        // 加签类型
        DictType signTypeType = getDictTypeByCode("sign_type");
        if (signTypeType != null) {
            createDictItemIfNotExists(signTypeType.getId(), "前加签", "before", 1);
            createDictItemIfNotExists(signTypeType.getId(), "后加签", "after", 2);
            createDictItemIfNotExists(signTypeType.getId(), "并行加签", "parallel", 3);
        }
        
        // 数据权限范围
        DictType dataScopeType = getDictTypeByCode("data_scope");
        if (dataScopeType != null) {
            createDictItemIfNotExists(dataScopeType.getId(), "全部数据", "1", 1);
            createDictItemIfNotExists(dataScopeType.getId(), "本部门数据", "2", 2);
            createDictItemIfNotExists(dataScopeType.getId(), "本部门及子部门数据", "3", 3);
            createDictItemIfNotExists(dataScopeType.getId(), "仅本人数据", "4", 4);
        }
        
        // 密级
        DictType securityLevelType = getDictTypeByCode("security_level");
        if (securityLevelType != null) {
            createDictItemIfNotExists(securityLevelType.getId(), "公开", "1", 1);
            createDictItemIfNotExists(securityLevelType.getId(), "内部", "2", 2);
            createDictItemIfNotExists(securityLevelType.getId(), "秘密", "3", 3);
            createDictItemIfNotExists(securityLevelType.getId(), "机密", "4", 4);
        }
        
        // 部门类型
        DictType deptTypeType = getDictTypeByCode("dept_type");
        if (deptTypeType != null) {
            createDictItemIfNotExists(deptTypeType.getId(), "公司", "company", 1);
            createDictItemIfNotExists(deptTypeType.getId(), "部门", "dept", 2);
            createDictItemIfNotExists(deptTypeType.getId(), "小组", "group", 3);
        }
        
        // 字典类型分类
        DictType dictTypeCategoryType = getDictTypeByCode("dict_type_category");
        if (dictTypeCategoryType != null) {
            createDictItemIfNotExists(dictTypeCategoryType.getId(), "系统内置", "1", 1);
            createDictItemIfNotExists(dictTypeCategoryType.getId(), "业务自定义", "2", 2);
        }
    }

    private void createDictTypeIfNotExists(String dictName, String dictCode, int dictType, String description) {
        DictType existing = dictTypeMapper.selectOne(
                new LambdaQueryWrapper<DictType>().eq(DictType::getDictCode, dictCode)
        );
        if (existing == null) {
            DictType dictTypeEntity = new DictType();
            dictTypeEntity.setDictName(dictName);
            dictTypeEntity.setDictCode(dictCode);
            dictTypeEntity.setDictType(dictType);
            dictTypeEntity.setDescription(description);
            dictTypeEntity.setStatus(1);
            dictTypeEntity.setCreateTime(LocalDateTime.now());
            dictTypeEntity.setUpdateTime(LocalDateTime.now());
            dictTypeMapper.insert(dictTypeEntity);
            log.debug("[DictDataInitializer] 创建字典类型: code={}", dictCode);
        }
    }

    private void createDictItemIfNotExists(Long dictTypeId, String itemText, String itemValue, int sortOrder) {
        DictItem existing = dictItemMapper.selectOne(
                new LambdaQueryWrapper<DictItem>()
                        .eq(DictItem::getDictTypeId, dictTypeId)
                        .eq(DictItem::getItemValue, itemValue)
        );
        if (existing == null) {
            DictItem dictItem = new DictItem();
            dictItem.setDictTypeId(dictTypeId);
            dictItem.setItemText(itemText);
            dictItem.setItemValue(itemValue);
            dictItem.setSortOrder(sortOrder);
            dictItem.setStatus(1);
            dictItem.setCreateTime(LocalDateTime.now());
            dictItem.setUpdateTime(LocalDateTime.now());
            dictItemMapper.insert(dictItem);
            log.debug("[DictDataInitializer] 创建字典项: text={}, value={}", itemText, itemValue);
        }
    }

    private DictType getDictTypeByCode(String dictCode) {
        return dictTypeMapper.selectOne(
                new LambdaQueryWrapper<DictType>().eq(DictType::getDictCode, dictCode)
        );
    }
}
