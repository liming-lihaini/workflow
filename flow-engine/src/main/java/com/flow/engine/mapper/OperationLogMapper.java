package com.flow.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flow.engine.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作日志Mapper（ISSUE-014）
 */
@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {
}
