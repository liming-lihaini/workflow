package com.flow.engine.dto;

import lombok.Data;

import java.util.List;

/**
 * 收样工作台-手动收集样品 入参。
 * 一次采集对应一个检测项目 + 一个采样点位，携带采样参数值、固定剂、现场质控方式、
 * 留样标记与现场照片。系统据此自动生成样品条码并写入已收样状态。
 */
@Data
public class SampleCollectReq {

    /** 采样派单ID（必填），关联 t_sampling_order.id */
    private Long dispatchId;
    /** 采样派单号（冗余展示用） */
    private String dispatchNo;
    /** 委托单ID（冗余存储便于检索） */
    private Long entrustId;
    /** 监测点位ID */
    private Long pointId;
    /** 单位/委托单名称 */
    private String custName;
    /** 检测类别（如 水质/大气） */
    private String category;
    /** 检测项目（监测因子） */
    private String item;
    /** 样品编号（可由表单录入，留空则系统生成） */
    private String sampleNo;
    /** 样品名称 */
    private String name;
    /** 样品类型（水样/气样/土壤等） */
    private String type;
    /** 样品来源（点位） */
    private String source;
    /** 数量/规格 */
    private String amount;
    /** 容器 */
    private String container;
    /** 保存条件/固定剂说明 */
    private String preserve;
    /** 采样天气状况 */
    private String weather;
    /** 采样人 */
    private String sampler;
    /** 采样时间（YYYY-MM-DD 或 YYYY-MM-DD HH:mm） */
    private String sampleTime;

    /**
     * 采样参数值列表：每个元素含 code/name/value/unit。
     * 来源：选择检测项目后，按 type+category、item 查询采样参数配置动态渲染。
     */
    private List<SampleParamValue> sampleParams;

    /** 固定剂（多选，数据字典 sample_preservative 项值） */
    private List<String> preservatives;
    /** 现场质控方式（多选，数据字典 moni_qc_type 项值） */
    private List<String> qcTypes;
    /** 是否留样 0否 1是 */
    private Integer retainSample;
    /** 留样保存天数 */
    private Integer retainDays;
    /** 留样人 */
    private String retainBy;
    /** 留样日期 */
    private String retainDate;
    /** 留样存放位置 */
    private String retainLocation;
    /** 现场照片（相对路径/URL 列表） */
    private List<String> photos;

    /** 收样人 */
    private String receiveBy;
    /** 收样备注 */
    private String remark;

    /** 采样参数值项 */
    @Data
    public static class SampleParamValue {
        /** 所属监测项目（多检测项目分组时标识归属，可选） */
        private String item;
        private String code;
        private String name;
        private String value;
        private String unit;
    }
}
