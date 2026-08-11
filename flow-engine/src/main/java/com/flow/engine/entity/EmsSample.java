package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 环境监测 - 样品（TRD 5.3 样品全生命周期）
 * 状态机：待收样 → 已收样 → 留样中 → 实验室监测中 → 检测数据复核中 → 已完成 / 检测异常
 */
@Data
@TableName("t_sample")
public class EmsSample {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String barcode;      // 样品条码（YP前缀）
    private Long samplingId;     // 采样记录ID
    private Long orderId;        // 采样任务ID
    private Long pointId;        // 监测点位ID
    private String name;         // 样品名称
    private String type;         // 样品类型（水样/气样/土壤等）
    private String source;       // 样品来源（点位）
    private String amount;       // 数量/规格
    private String container;    // 容器
    private String preserve;     // 保存条件/固定剂
    private String weather;      // 采样天气状况
    private String status;       // 待收样/已收样/异常拒收/留样中/实验室监测中/检测数据复核中/已完成/检测异常/已处置
    private String disposalType;   // 异常处置类型（数据字典 moni_disposal_type）
    private String disposalMethod; // 异常处置方式（数据字典 moni_disposal_method）
    private String disposalDesc;   // 异常处置说明（富文本 HTML）
    private String disposalBy;     // 异常处置人
    private java.time.LocalDateTime disposalTime; // 异常处置时间
    private String receiveTime;  // 收样时间
    private String receiveBy;    // 收样人
    private Integer retainFlag;  // 是否留样（0/1）
    private Integer retainDays;  // 留样天数
    private String retainUntil;  // 留样到期日
    private String retainBy;     // 留样人
    private String retainDate;   // 留样日期
    private String retainLocation; // 存放位置
    private String dispatchTime; // 送检/检测下发时间
    /** 采样人（手动收集场景冗余存储，便于详情展示） */
    private String sampler;
    /** 采样时间（手动收集场景冗余存储） */
    private String sampleTime;
    private String remark;

    /** 采样派单ID（关联 t_sampling_order.id） */
    private Long dispatchId;
    /** 委托单ID（关联 t_entrust.id），采集录入时冗余存储便于检索 */
    private Long entrustId;
    /** 样品编号（如 SP-20260801-001），由采集表单录入 */
    private String sampleNo;
    /** 检测类别（如 水质/大气等业务分类） */
    private String category;
    /** 检测项目（监测因子，来自点位 factors） */
    private String item;
    /** 采样参数值：JSON 数组，元素 {code,name,value,unit} */
    private String sampleParams;
    /** 固定剂（多选，逗号分隔数据字典值），来源 sample_preservative */
    private String preservatives;
    /** 现场质控方式（多选，逗号分隔数据字典值），来源 moni_qc_type */
    private String qcTypes;
    /** 收样检查单（多选，逗号分隔数据字典值），来源 sample_receive_check */
    private String checkItems;
    /** 是否留样 0否 1是 */
    private Integer retainSample;
    /** 现场照片（逗号分隔的相对路径/URL） */
    private String samplePhoto;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
