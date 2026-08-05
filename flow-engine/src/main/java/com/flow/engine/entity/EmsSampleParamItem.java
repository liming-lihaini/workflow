package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 环境监测 - 采样参数配置明细（现场结构化必填采样参数，与主表一对多）
 */
@Data
@TableName("t_sample_param_item")
public class EmsSampleParamItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 关联主表 t_sample_param_config.id */
    private Long configId;
    /** 参数编码（如 flue_area） */
    private String code;
    /** 参数名称（如 烟道截面积） */
    private String name;
    /** 参数类型 number/text/select/bool/datetime */
    private String paramType;
    /** 单位（如 m²） */
    private String unit;
    /** 是否必填（1 必填 / 0 选填） */
    private Integer required;
    /** 下拉选项，逗号分隔（仅 select 类型使用） */
    private String enumText;
    /** 提示备注（采集要求说明） */
    private String tip;
    private Integer sortNo;
}
