package com.flow.engine.dto;

import lombok.Data;

/**
 * 采样参数配置 - 结构化参数明细请求对象（用于保存/新建请求）。
 * required 用 Boolean 以匹配前端传入的 true/false，与实体 EmsSampleParamItem 的 Integer 解耦。
 */
@Data
public class SampleParamItemReq {
    private String code;
    private String name;
    private String paramType;
    private String unit;
    private Boolean required;
    private String enumText;
    private String tip;
}
