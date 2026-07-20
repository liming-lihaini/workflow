package com.flow.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flow.engine.entity.WebhookLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * Webhook回调日志Mapper（ISSUE-012）
 */
@Mapper
public interface WebhookLogMapper extends BaseMapper<WebhookLog> {
}
