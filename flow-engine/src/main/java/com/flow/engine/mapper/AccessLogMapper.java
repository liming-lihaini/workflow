package com.flow.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flow.engine.entity.AccessLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 访问日志Mapper（ISSUE-014）
 */
@Mapper
public interface AccessLogMapper extends BaseMapper<AccessLog> {
}
