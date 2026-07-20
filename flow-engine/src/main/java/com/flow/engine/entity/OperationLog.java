package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志实体（ISSUE-014）
 */
@Data
@TableName("sys_operation_log")
public class OperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 模块 */
    private String module;

    /** 操作类型 */
    private String operation;

    /** 方法名 */
    private String method;

    /** 请求参数 */
    private String params;

    /** 返回结果 */
    private String result;

    /** 修改前数据 */
    private String beforeData;

    /** 修改后数据 */
    private String afterData;

    /** IP地址 */
    private String ip;

    /** 操作时间 */
    private LocalDateTime operationTime;
}
