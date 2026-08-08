package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 环境监测 - 采样参数配置主表（检测类别 + 检测项目 + 执行标准 + 限值 + 备注）
 */
@Data
@TableName("t_sample_param_config")
public class EmsSampleParamConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 检测类别（废气/废水/土壤...，取自字典 moni_sample_param_type） */
    private String type;
    /** 检测项目（颗粒物/CODcr...，取自字典 moni_sample_param_item） */
    private String item;
    /** 执行标准编号 */
    private String standard;
    /** 标准限值 / 管控要求 */
    private String limitValue;
    /** 检测结果单位（如 mg/L、μg/m³） */
    private String unit;
    /** 企业内控限制（严于国标的企业内部管控要求） */
    private String innerLimit;
    /** 采样备注说明（容器材质、固定剂、保存条件等） */
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 结构化采样参数明细（非持久化，用于承载/返回一对多数据） */
    @TableField(exist = false)
    private List<EmsSampleParamItem> items;
}
