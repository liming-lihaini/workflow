package com.flow.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flow.engine.entity.DictType;
import org.apache.ibatis.annotations.Mapper;

/**
 * 数据字典类型Mapper（ISSUE-015）
 */
@Mapper
public interface DictTypeMapper extends BaseMapper<DictType> {
}
