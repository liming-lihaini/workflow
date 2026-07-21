package com.flow.engine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.flow.engine.common.BusinessException;
import com.flow.engine.common.ErrorCode;
import com.flow.engine.entity.FormDefinition;
import com.flow.engine.mapper.FormDefinitionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 表单定义服务（ISSUE-008，TRD §3.4）
 * <p>
 * 提供表单的 CRUD 功能。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FormDefinitionService {

    private final FormDefinitionMapper formDefinitionMapper;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 创建表单定义
     */
    @Transactional
    public FormDefinition createForm(FormDefinition form) {
        // 校验 formKey 唯一性
        if (!StringUtils.hasText(form.getFormKey())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "表单Key不能为空");
        }
        if (!StringUtils.hasText(form.getFormName())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "表单名称不能为空");
        }

        LambdaQueryWrapper<FormDefinition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FormDefinition::getFormKey, form.getFormKey());
        if (formDefinitionMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.FORM_KEY_DUPLICATE, "表单Key已存在: " + form.getFormKey());
        }

        form.setCreateTime(LocalDateTime.now());
        form.setUpdateTime(LocalDateTime.now());
        formDefinitionMapper.insert(form);

        log.info("创建表单定义: formKey={}, formName={}", form.getFormKey(), form.getFormName());
        return form;
    }

    /**
     * 更新表单定义
     */
    @Transactional
    public FormDefinition updateForm(String formKey, FormDefinition form) {
        FormDefinition existing = getByFormKey(formKey);

        existing.setFormName(form.getFormName());
        existing.setFormJson(form.getFormJson());
        existing.setCategory(form.getCategory());
        existing.setUpdateTime(LocalDateTime.now());

        formDefinitionMapper.updateById(existing);
        log.info("更新表单定义: formKey={}", formKey);

        return existing;
    }

    /**
     * 删除表单定义
     */
    @Transactional
    public void deleteForm(String formKey) {
        FormDefinition existing = getByFormKey(formKey);
        formDefinitionMapper.deleteById(existing.getId());
        log.info("删除表单定义: formKey={}", formKey);
    }

    /**
     * 获取表单定义
     */
    public FormDefinition getForm(String formKey) {
        return getByFormKey(formKey);
    }

    /**
     * 获取表单定义列表（分页）
     */
    public IPage<FormDefinition> getFormList(int page, int size, String keyword) {
        Page<FormDefinition> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<FormDefinition> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(FormDefinition::getFormKey, keyword)
                    .or()
                    .like(FormDefinition::getFormName, keyword)
            );
        }

        wrapper.orderByDesc(FormDefinition::getCreateTime);
        return formDefinitionMapper.selectPage(pageParam, wrapper);
    }

    /**
     * 获取表单定义列表（不分页）
     */
    public List<FormDefinition> getFormList() {
        LambdaQueryWrapper<FormDefinition> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(FormDefinition::getCreateTime);
        return formDefinitionMapper.selectList(wrapper);
    }

    /**
     * 根据 formKey 查询
     */
    private FormDefinition getByFormKey(String formKey) {
        LambdaQueryWrapper<FormDefinition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FormDefinition::getFormKey, formKey);
        FormDefinition form = formDefinitionMapper.selectOne(wrapper);
        if (form == null) {
            throw new BusinessException(ErrorCode.FORM_NOT_FOUND, "表单不存在: " + formKey);
        }
        return form;
    }
}
