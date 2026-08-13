package com.flow.engine.reminder;

import lombok.Data;

import java.time.LocalDate;

/**
 * 通用到期提醒项（由各数据源 Provider 扫描生成，落入 t_reminder）。
 * bizKey 用于幂等去重（source_type + biz_key 唯一）：同一业务对象重复扫描只更新不新增。
 */
@Data
public class ReminderItem {
    private String sourceType;     // 数据源类型（Provider 注入）
    private Long bizId;            // 业务对象 id
    private String bizKey;         // 去重键（默认取 bizId，含子对象时可拼接，如节点 id）
    private String title;          // 提醒标题（列表/弹窗主文案）
    private String detail;         // 明细说明
    private LocalDate dueDate;     // 到期日期（用于计算逾期天数）
    private Long ownerId;          // 负责人用户 id（可为空）
    private String ownerName;      // 负责人姓名（可为空）
}
