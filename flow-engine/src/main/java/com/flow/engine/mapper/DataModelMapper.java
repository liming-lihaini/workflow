package com.flow.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flow.engine.entity.DataModel;
import org.apache.ibatis.annotations.Mapper;

/**
 * 数据模型Mapper（ISSUE-010）
 */
@Mapper
public interface DataModelMapper extends BaseMapper<DataModel> {
}
