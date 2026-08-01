package com.flow.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flow.engine.entity.EmsMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EmsMessageMapper extends BaseMapper<EmsMessage> {
}
