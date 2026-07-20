package com.flow.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flow.engine.entity.DictItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 数据字典项Mapper（ISSUE-015）
 */
@Mapper
public interface DictItemMapper extends BaseMapper<DictItem> {
}
