package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 环境监测 - 采样/样品现场照片（TRD 5.3 采样照片存证）
 */
@Data
@TableName("t_photo")
public class EmsPhoto {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String bizType;   // sampling_record / sample
    private Long bizId;       // 关联业务ID
    private String url;       // 图片地址
    private LocalDateTime createTime;
}
