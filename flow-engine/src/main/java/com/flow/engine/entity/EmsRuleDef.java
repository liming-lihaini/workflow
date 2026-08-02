package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 规则定义（ISSUE-029 规则引擎，表达式可配置热更）。 */
@Data
@TableName("t_rule_def")
public class EmsRuleDef {
    private Long id;
    private String ruleKey;
    private String ruleName;
    private String expr;          // SpEL 表达式
    private Integer enabled;      // 1 启用 0 停用
    private Integer version;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
