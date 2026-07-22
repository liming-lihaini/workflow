package com.flow.engine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.flow.engine.common.BusinessException;
import com.flow.engine.common.ErrorCode;
import com.flow.engine.dto.*;
import com.flow.engine.entity.ProcessDefinition;
import com.flow.engine.mapper.ProcessDefinitionMapper;
import com.flow.engine.model.ProcessModel;
import com.flow.engine.parser.ProcessJsonParser;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 流程定义服务（ISSUE-003）
 */
@Service
@RequiredArgsConstructor
public class ProcessDefinitionService {

    private final ProcessDefinitionMapper definitionMapper;
    private final ProcessJsonParser jsonParser;

    /**
     * 创建流程定义
     */
    @Transactional
    public ProcessDefinitionResponse create(ProcessDefinitionCreateRequest request) {
        // 参数校验
        if (!StringUtils.hasText(request.getProcessKey())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "processKey不能为空");
        }
        if (!StringUtils.hasText(request.getProcessName())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "processName不能为空");
        }

        // processJson 可选，允许先创建基本信息再设计流程图
        if (StringUtils.hasText(request.getProcessJson())) {
            jsonParser.parse(request.getProcessJson());
        }

        // 检查 processKey 是否已存在同版本
        Long count = definitionMapper.selectCount(
                new LambdaQueryWrapper<ProcessDefinition>()
                        .eq(ProcessDefinition::getProcessKey, request.getProcessKey())
        );
        if (count > 0) {
            throw new BusinessException(ErrorCode.PROCESS_KEY_DUPLICATE,
                    "processKey '" + request.getProcessKey() + "' 已存在");
        }

        ProcessDefinition entity = new ProcessDefinition();
        entity.setProcessKey(request.getProcessKey());
        entity.setProcessName(request.getProcessName());
        entity.setCategory(request.getCategory());
        entity.setProcessType(request.getProcessType());
        entity.setDescription(request.getDescription());
        entity.setProcessJson(request.getProcessJson());
        entity.setVersion(1);
        entity.setStatus(0); // 草稿
        entity.setCreateBy(request.getCreateBy());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());

        definitionMapper.insert(entity);
        return toResponse(entity);
    }

    /**
     * 更新流程定义（同名 process_key 二次保存生成 version+1）
     */
    @Transactional
    public ProcessDefinitionResponse update(Long id, ProcessDefinitionUpdateRequest request) {
        ProcessDefinition entity = definitionMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.PROCESS_DEF_NOT_FOUND);
        }

        // 已部署的不允许直接修改
        if (entity.getStatus() == 1) {
            throw new BusinessException(ErrorCode.PROCESS_DEF_ALREADY_DEPLOYED, "已部署的流程定义不允许直接修改，请创建新版本");
        }

        if (StringUtils.hasText(request.getProcessJson())) {
            jsonParser.parse(request.getProcessJson());
            entity.setProcessJson(request.getProcessJson());
        }
        if (StringUtils.hasText(request.getProcessName())) {
            entity.setProcessName(request.getProcessName());
        }
        if (request.getCategory() != null) {
            entity.setCategory(request.getCategory());
        }
        if (request.getProcessType() != null) {
            entity.setProcessType(request.getProcessType());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
        entity.setUpdateTime(LocalDateTime.now());

        definitionMapper.updateById(entity);
        return toResponse(entity);
    }

    /**
     * 获取流程定义详情
     */
    public ProcessDefinitionResponse getById(Long id) {
        ProcessDefinition entity = definitionMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.PROCESS_DEF_NOT_FOUND);
        }
        return toResponse(entity);
    }

    /**
     * 根据 processKey 获取最新版本的流程定义
     */
    @Cacheable(value = "processDef", key = "#processKey")
    public ProcessDefinitionResponse getByKey(String processKey) {
        ProcessDefinition entity = definitionMapper.selectOne(
                new LambdaQueryWrapper<ProcessDefinition>()
                        .eq(ProcessDefinition::getProcessKey, processKey)
                        .orderByDesc(ProcessDefinition::getVersion)
                        .last("LIMIT 1")
        );
        if (entity == null) {
            throw new BusinessException(ErrorCode.PROCESS_DEF_NOT_FOUND);
        }
        return toResponse(entity);
    }

    /**
     * 获取流程定义列表
     */
    public List<ProcessDefinitionResponse> list(String processKey, String category, Integer status) {
        LambdaQueryWrapper<ProcessDefinition> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(processKey)) {
            wrapper.like(ProcessDefinition::getProcessKey, processKey);
        }
        if (StringUtils.hasText(category)) {
            wrapper.eq(ProcessDefinition::getCategory, category);
        }
        if (status != null) {
            wrapper.eq(ProcessDefinition::getStatus, status);
        }
        wrapper.orderByDesc(ProcessDefinition::getUpdateTime);

        List<ProcessDefinition> list = definitionMapper.selectList(wrapper);
        return list.stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * 分页查询
     */
    public Page<ProcessDefinitionResponse> page(int pageNum, int pageSize, String processKey, Integer status) {
        Page<ProcessDefinition> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ProcessDefinition> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(processKey)) {
            wrapper.like(ProcessDefinition::getProcessKey, processKey);
        }
        if (status != null) {
            wrapper.eq(ProcessDefinition::getStatus, status);
        }
        wrapper.orderByDesc(ProcessDefinition::getUpdateTime);

        Page<ProcessDefinition> result = definitionMapper.selectPage(page, wrapper);
        Page<ProcessDefinitionResponse> responsePage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        responsePage.setRecords(result.getRecords().stream().map(this::toResponse).collect(Collectors.toList()));
        return responsePage;
    }

    /**
     * 删除流程定义
     */
    @Transactional
    public void delete(Long id) {
        ProcessDefinition entity = definitionMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.PROCESS_DEF_NOT_FOUND);
        }
        definitionMapper.deleteById(id);
    }

    /**
     * 部署流程定义
     */
    @Transactional
    public ProcessDefinitionResponse deploy(Long id) {
        ProcessDefinition entity = definitionMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.PROCESS_DEF_NOT_FOUND);
        }

        // 部署前必须校验 processJson 存在且合法（含节点结构严格校验）
        if (!StringUtils.hasText(entity.getProcessJson())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "流程定义JSON为空，请先设计流程图再部署");
        }
        jsonParser.parseForDeploy(entity.getProcessJson());

        entity.setStatus(1); // 已部署
        entity.setDeploymentId(UUID.randomUUID().toString().replace("-", ""));
        entity.setUpdateTime(LocalDateTime.now());

        definitionMapper.updateById(entity);
        return toResponse(entity);
    }

    /**
     * 导出流程定义
     */
    public ProcessDefinitionResponse export(Long id) {
        return getById(id);
    }

    /**
     * 导入流程定义
     */
    @Transactional
    public ProcessDefinitionResponse importDefinition(ProcessDefinitionImportRequest request) {
        if (!StringUtils.hasText(request.getProcessJson())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "processJson不能为空");
        }

        // 校验JSON
        ProcessModel model = jsonParser.parse(request.getProcessJson());

        // 使用JSON中的processKey（如果请求中没有指定）
        String processKey = StringUtils.hasText(request.getProcessKey())
                ? request.getProcessKey()
                : (model.getProcessKey() != null ? model.getProcessKey() : null);

        if (!StringUtils.hasText(processKey)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "processKey不能为空");
        }

        // 检查是否已存在
        Long count = definitionMapper.selectCount(
                new LambdaQueryWrapper<ProcessDefinition>()
                        .eq(ProcessDefinition::getProcessKey, processKey)
        );
        if (count > 0) {
            throw new BusinessException(ErrorCode.PROCESS_KEY_DUPLICATE,
                    "processKey '" + processKey + "' 已存在，导入失败");
        }

        ProcessDefinition entity = new ProcessDefinition();
        entity.setProcessKey(processKey);
        entity.setProcessName(StringUtils.hasText(request.getProcessName())
                ? request.getProcessName()
                : (model.getProcessName() != null ? model.getProcessName() : processKey));
        entity.setCategory(request.getCategory() != null ? request.getCategory() : model.getCategory());
        entity.setProcessJson(request.getProcessJson());
        entity.setVersion(1);
        entity.setStatus(0); // 草稿
        entity.setCreateBy(request.getCreateBy());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());

        definitionMapper.insert(entity);
        return toResponse(entity);
    }

    private ProcessDefinitionResponse toResponse(ProcessDefinition entity) {
        ProcessDefinitionResponse response = new ProcessDefinitionResponse();
        response.setId(entity.getId());
        response.setProcessKey(entity.getProcessKey());
        response.setProcessName(entity.getProcessName());
        response.setVersion(entity.getVersion());
        response.setProcessJson(entity.getProcessJson());
        response.setCategory(entity.getCategory());
        response.setProcessType(entity.getProcessType());
        response.setDescription(entity.getDescription());
        response.setStatus(entity.getStatus());
        response.setDeploymentId(entity.getDeploymentId());
        response.setCreateTime(entity.getCreateTime());
        response.setUpdateTime(entity.getUpdateTime());
        response.setCreateBy(entity.getCreateBy());
        return response;
    }
}
