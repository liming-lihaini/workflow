package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 环境监测 - 文件元信息（TRD 4.4 共享实体，WORM 受控）
 */
@Data
@TableName("t_file_meta")
public class EmsFileMeta {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String bizType;
    private Long bizId;
    private String fileName;
    private String filePath;
    private String hash;
    private Long size;
    private String uploadBy;
    private LocalDateTime createTime;
}
