package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 状态迁移定义（ISSUE-029 固定状态机）。 */
@Data
@TableName("t_transition_def")
public class EmsTransitionDef {
    private Long id;
    private String bizType;
    private String fromState;
    private String event;
    private String toState;
    private String guardExpr;     // 守卫表达式（SpEL）
    private String guardFailMsg;  // 守卫失败提示
}
