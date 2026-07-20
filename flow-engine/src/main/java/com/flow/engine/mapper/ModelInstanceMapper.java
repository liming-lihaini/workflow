package com.flow.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flow.engine.entity.ModelInstance;
import org.apache.ibatis.annotations.Mapper;

/**
 * 模型实例Mapper（ISSUE-010）
 */
@Mapper
public interface ModelInstanceMapper extends BaseMapper<ModelInstance> {
}
