package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 访问日志实体（ISSUE-014）
 */
@Data
@TableName("sys_access_log")
public class AccessLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** IP地址 */
    private String ip;

    /** 用户代理 */
    private String userAgent;

    /** 请求URL */
    private String url;

    /** 请求方法 */
    private String method;

    /** 请求参数 */
    private String params;

    /** 结果：0-失败，1-成功 */
    private Integer result;

    /** 错误信息 */
    private String errorMsg;

    /** 访问时间 */
    private LocalDateTime accessTime;
}
