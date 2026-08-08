package com.flow.engine.dto;

import lombok.Data;

@Data
public class SampleDisposeReq {

    /** 异常处置方式（数据字典 moni_disposal_method） */
    private String disposalMethod;

    /** 异常处置类型（数据字典 moni_disposal_type） */
    private String disposalType;

    /** 异常处置说明（富文本 HTML） */
    private String disposalDesc;
}
