package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 状态定义（ISSUE-029 状态机）。 */
@Data
@TableName("t_state_def")
public class EmsStateDef {
    private Long id;
    private String bizType;
    private String stateKey;
    private String stateName;
    private Integer sort;
}
