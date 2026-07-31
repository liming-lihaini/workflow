package com.flow.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flow.engine.entity.ApiToken;
import org.apache.ibatis.annotations.Mapper;

/**
 * 个人API Token Mapper
 */
@Mapper
public interface ApiTokenMapper extends BaseMapper<ApiToken> {
}
