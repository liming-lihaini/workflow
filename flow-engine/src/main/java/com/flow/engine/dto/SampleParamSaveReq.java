package com.flow.engine.dto;

import lombok.Data;

import java.util.List;

/**
 * 采样参数配置保存请求：主表字段 + 结构化参数明细列表。
 * 字段与前端表单（SampleParamConfig.vue 的 form）严格对齐。
 */
@Data
public class SampleParamSaveReq {
    private Long id;
    private String type;
    private String item;
    private String standard;
    private String limit;         // 对应实体 limitValue
    private String remark;
    private List<SampleParamItemReq> sampleParams;
}
