package com.flow.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flow.engine.entity.ProcessInstance;
import org.apache.ibatis.annotations.Mapper;

/**
 * 流程实例 Mapper
 */
@Mapper
public interface ProcessInstanceMapper extends BaseMapper<ProcessInstance> {
}
