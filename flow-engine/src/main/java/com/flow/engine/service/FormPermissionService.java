package com.flow.engine.service;

import com.flow.engine.common.BusinessException;
import com.flow.engine.common.ErrorCode;
import com.flow.engine.dto.FormPermissionResponse;
import com.flow.engine.dto.FormPermissionResponse.FieldPermission;
import com.flow.engine.entity.ProcessInstance;
import com.flow.engine.entity.Task;
import com.flow.engine.mapper.ProcessInstanceMapper;
import com.flow.engine.mapper.TaskMapper;
import com.flow.engine.model.NodeModel;
import com.flow.engine.model.ProcessModel;
import com.flow.engine.parser.ProcessJsonParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 表单权限服务（ISSUE-009）
 * <p>
 * 根据任务关联的流程节点配置，计算表单权限。
 * 权限配置存储在流程定义JSON的节点 properties.formPermissions 中。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FormPermissionService {

    private final TaskMapper taskMapper;
    private final ProcessInstanceMapper processInstanceMapper;
    private final ProcessDefinitionService definitionService;
    private final PermissionCalculator permissionCalculator;
    private final VariableService variableService;
    private final ProcessJsonParser jsonParser;

    /**
     * 获取任务的表单权限
     *
     * @param taskId 任务ID
     * @return 表单权限响应
     */
    public FormPermissionResponse getFormPermissions(Long taskId) {
        // 1. 获取任务
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.TASK_NOT_FOUND);
        }

        // 2. 获取流程实例
        ProcessInstance instance = processInstanceMapper.selectById(task.getProcessInstanceId());
        if (instance == null) {
            throw new BusinessException(ErrorCode.PROCESS_INSTANCE_NOT_FOUND);
        }

        // 3. 获取流程定义
        var defResp = definitionService.getByKey(instance.getProcessKey());
        ProcessModel model = jsonParser.parse(defResp.getProcessJson());

        // 4. 找到当前节点
        NodeModel currentNode = findNode(model, task.getNodeId());
        if (currentNode == null) {
            // 节点未找到，返回默认权限
            return permissionCalculator.calculatePermissions(null, null);
        }

        // 5. 获取流程变量（用于条件化权限）
        Map<String, Object> variables = variableService.getVariables(instance.getId());

        // 6. 提取节点权限配置
        Map<String, Object> formPermissions = extractFormPermissions(currentNode);

        // 7. 处理继承
        String inheritFrom = extractInheritFrom(currentNode);
        if (inheritFrom != null && !inheritFrom.isBlank()) {
            NodeModel parentNode = findNode(model, inheritFrom);
            if (parentNode != null) {
                Map<String, Object> parentFormPermissions = extractFormPermissions(parentNode);
                return calculateWithInheritance(parentFormPermissions, formPermissions, variables);
            }
        }

        // 8. 计算权限
        FormPermissionResponse response = permissionCalculator.calculatePermissions(formPermissions, variables);
        response.setFormKey(extractFormKey(currentNode));
        return response;
    }

    /**
     * 带继承的权限计算
     */
    private FormPermissionResponse calculateWithInheritance(
            Map<String, Object> parentPermissions,
            Map<String, Object> childPermissions,
            Map<String, Object> variables) {

        // 分别计算父和子的权限
        FormPermissionResponse parentResp = permissionCalculator.calculatePermissions(parentPermissions, variables);
        FormPermissionResponse childResp = permissionCalculator.calculatePermissions(childPermissions, variables);

        // 合并字段权限：子覆盖父
        List<FieldPermission> mergedFields = permissionCalculator.mergePermissions(
                parentResp.getFieldPermissions(),
                childResp.getFieldPermissions());
        childResp.setFieldPermissions(mergedFields);

        // 节点级权限：子节点有配置则用子的，否则继承父的
        if (childResp.getNodePermission() == null || "edit".equals(childResp.getNodePermission())) {
            if (parentResp.getNodePermission() != null && !"edit".equals(parentResp.getNodePermission())) {
                childResp.setNodePermission(parentResp.getNodePermission());
            }
        }

        return childResp;
    }

    // ==================== 辅助方法 ====================

    private NodeModel findNode(ProcessModel model, String nodeId) {
        if (model == null || model.getNodes() == null || nodeId == null) {
            return null;
        }
        return model.getNodes().stream()
                .filter(n -> nodeId.equals(n.getId()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 从节点 properties 中提取 formPermissions 配置
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractFormPermissions(NodeModel node) {
        if (node == null || node.getProperties() == null) {
            return null;
        }
        Object fp = node.getProperties().get("formPermissions");
        if (fp instanceof Map) {
            return (Map<String, Object>) fp;
        }
        return null;
    }

    /**
     * 从节点 properties 中提取继承来源节点ID
     */
    private String extractInheritFrom(NodeModel node) {
        if (node == null || node.getProperties() == null) {
            return null;
        }
        Object inheritFrom = node.getProperties().get("inheritFrom");
        return inheritFrom instanceof String ? (String) inheritFrom : null;
    }

    /**
     * 从节点 properties 中提取 formKey
     */
    private String extractFormKey(NodeModel node) {
        if (node == null || node.getProperties() == null) {
            return null;
        }
        Object formKey = node.getProperties().get("formKey");
        return formKey instanceof String ? (String) formKey : null;
    }
}
