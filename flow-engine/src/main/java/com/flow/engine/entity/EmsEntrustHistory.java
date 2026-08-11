package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 检测委托操作历史：记录委托的新建、编辑、提交、技术确认、退回、收样等处置轨迹，
 * 供委托详情页「操作记录」展示。
 */
@Data
@TableName("t_entrust_history")
public class EmsEntrustHistory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long entrustId;     // 委托ID
    private String action;      // 操作动作：新建/编辑/提交/技术确认/退回/收样/删除
    private String content;     // 操作内容说明
    private String operatorId;   // 操作人账号
    private String operatorName; // 操作人姓名
    private LocalDateTime createTime;
}
