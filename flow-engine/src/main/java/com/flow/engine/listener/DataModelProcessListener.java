package com.flow.engine.listener;

import com.flow.engine.dto.ModelInstanceRequest;
import com.flow.engine.entity.ProcessInstance;
import com.flow.engine.event.ProcessCompletedEvent;
import com.flow.engine.event.ProcessStartedEvent;
import com.flow.engine.mapper.ProcessInstanceMapper;
import com.flow.engine.service.ModelInstanceManager;
import com.flow.engine.service.VariableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 数据模型流程集成监听器（ISSUE-010）
 * <p>
 * 监听流程启动/完成事件，自动创建/归档模型实例。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataModelProcessListener {

    private final ModelInstanceManager modelInstanceManager;
    private final VariableService variableService;
    private final ProcessInstanceMapper instanceMapper;

    /**
     * 流程启动时，检查流程变量中是否有 modelKey，如果有则自动创建模型实例
     */
    @EventListener
    public void onProcessStarted(ProcessStartedEvent event) {
        log.info("[DataModel] 流程启动事件: instanceId={}, processKey={}", 
                event.getProcessInstanceId(), event.getProcessKey());

        try {
            // 检查流程变量中是否有 modelKey
            Map<String, Object> variables = variableService.getVariables(event.getProcessInstanceId());
            String modelKey = (String) variables.get("_modelKey");
            
            if (modelKey != null && !modelKey.isBlank()) {
                // 自动创建模型实例
                ModelInstanceRequest request = new ModelInstanceRequest();
                request.setModelKey(modelKey);
                request.setProcessInstanceId(event.getProcessInstanceId());
                request.setData(variables);
                
                modelInstanceManager.createInstance(request);
                log.info("[DataModel] 自动创建模型实例: modelKey={}, processInstanceId={}", 
                        modelKey, event.getProcessInstanceId());
            }
        } catch (Exception e) {
            log.warn("[DataModel] 自动创建模型实例失败: {}", e.getMessage());
        }
    }

    /**
     * 流程完成时，自动归档模型实例
     */
    @EventListener
    public void onProcessCompleted(ProcessCompletedEvent event) {
        log.info("[DataModel] 流程完成事件: instanceId={}, processKey={}", 
                event.getProcessInstanceId(), event.getProcessKey());

        try {
            // 查找该流程实例关联的模型实例并归档
            ProcessInstance instance = instanceMapper.selectById(event.getProcessInstanceId());
            if (instance != null) {
                // 尝试归档（如果没有关联的模型实例，会抛出异常，这里忽略）
                try {
                    var modelInstance = modelInstanceManager.getInstanceByProcessInstanceId(event.getProcessInstanceId());
                    if (modelInstance != null) {
                        modelInstanceManager.archiveInstance(modelInstance.getModelInstanceId());
                        log.info("[DataModel] 自动归档模型实例: instanceId={}, processInstanceId={}", 
                                modelInstance.getModelInstanceId(), event.getProcessInstanceId());
                    }
                } catch (Exception e) {
                    // 没有关联的模型实例，忽略
                    log.debug("[DataModel] 流程实例 {} 没有关联的模型实例", event.getProcessInstanceId());
                }
            }
        } catch (Exception e) {
            log.warn("[DataModel] 自动归档模型实例失败: {}", e.getMessage());
        }
    }
}
