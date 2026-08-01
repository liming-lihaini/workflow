package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 环境监测 - 样品质控类型绑定（TRD 5.3 质控样）
 */
@Data
@TableName("t_sample_qc_binding")
public class EmsSampleQcBinding {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sampleId;   // 样品ID
    private String sampleNo; // 样品编号
    private String qcType;   // 全程序空白/现场空白/平行样/加标回收/密码样
    private String remark;
}
