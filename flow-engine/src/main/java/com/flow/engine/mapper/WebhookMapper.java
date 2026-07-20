package com.flow.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flow.engine.entity.Webhook;
import org.apache.ibatis.annotations.Mapper;

/**
 * Webhook配置Mapper（ISSUE-012）
 */
@Mapper
public interface WebhookMapper extends BaseMapper<Webhook> {
}
