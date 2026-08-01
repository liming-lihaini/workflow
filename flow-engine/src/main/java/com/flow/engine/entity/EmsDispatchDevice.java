package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 环境监测 - 派单设备关联（TRD 5.2 设备校准有效期闸门）
 */
@Data
@TableName("t_dispatch_device")
public class EmsDispatchDevice {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long dispatchId;
    private Long instrumentId;  // 设备ID
}
