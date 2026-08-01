package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 环境监测 - 设备校准历史记录（TRD 5.5.5 校准登记台账）
 */
@Data
@TableName("t_instrument_calib")
public class EmsInstrumentCalib {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long instrumentId;   // 设备ID
    private LocalDate calibDate; // 校准日期
    private LocalDate calibDue;  // 下次校准到期日
    private String certNo;       // 校准证书编号
    private LocalDateTime createTime;
}
