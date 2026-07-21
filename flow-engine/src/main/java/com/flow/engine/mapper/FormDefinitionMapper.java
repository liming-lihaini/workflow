package com.flow.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flow.engine.entity.FormDefinition;
import org.apache.ibatis.annotations.Mapper;

/**
 * 表单定义 Mapper（ISSUE-008）
 */
@Mapper
public interface FormDefinitionMapper extends BaseMapper<FormDefinition> {
}
