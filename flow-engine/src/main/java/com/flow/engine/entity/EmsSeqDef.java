package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 编号序列定义（ISSUE-029 编号引擎，分段全局唯一）。 */
@Data
@TableName("t_seq_def")
public class EmsSeqDef {
    private Long id;
    private String bizKey;        // 业务键：entrust/order/sample/batch/report
    private String prefix;        // 前缀
    private String seqDate;       // 日期段 yyyyMMdd
    private Long currentVal;      // 当前序列值
    private Integer step;         // 步长
}
