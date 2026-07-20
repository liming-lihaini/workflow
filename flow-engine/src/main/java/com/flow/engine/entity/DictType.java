package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据字典类型实体（ISSUE-015）
 */
@Data
@TableName("sys_dict_type")
public class DictType {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 字典名称 */
    private String dictName;

    /** 字典编码 */
    private String dictCode;

    /** 字典类型：1-系统内置，2-业务自定义 */
    private Integer dictType;

    /** 描述 */
    private String description;

    /** 状态：0-停用，1-正常 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
