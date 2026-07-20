package com.flow.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flow.engine.entity.Variable;
import org.apache.ibatis.annotations.Mapper;

/**
 * 流程变量 Mapper
 */
@Mapper
public interface VariableMapper extends BaseMapper<Variable> {
}
