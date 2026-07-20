package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据字典项实体（ISSUE-015）
 */
@Data
@TableName("sys_dict_item")
public class DictItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 字典类型ID */
    private Long dictTypeId;

    /** 字典项文本 */
    private String itemText;

    /** 字典项值 */
    private String itemValue;

    /** 排序 */
    private Integer sortOrder;

    /** 状态：0-停用，1-正常 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
