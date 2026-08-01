package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 环境监测 - 集成配置（TRD 5.10，cfg_value 为 AES 密文）
 */
@Data
@TableName("t_integration_cfg")
public class EmsIntegrationCfg {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String cfgKey;
    private String cfgValue;
    private Integer encryptFlag;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
