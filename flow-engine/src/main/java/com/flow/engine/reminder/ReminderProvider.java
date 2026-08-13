package com.flow.engine.reminder;

import java.util.List;

/**
 * 到期提醒数据源提供者：每个实现对应一类业务数据（采样任务/仪器校准/标准物质/耗材/合同/收付款节点等）。
 * EmsReminderService 在查询提醒时调用 {@link #scan()} 全量扫描，按 bizKey 幂等写入 t_reminder；
 * 扫描结果中不再出现的业务对象，其历史提醒项会被标记为已消除（resolved）。
 * 新增提醒数据源只需增加一个 @Component 实现，无需改动服务与前端。
 */
public interface ReminderProvider {

    /** 数据源类型标识（t_reminder.source_type），全局唯一，如 SAMPLING_ORDER */
    String sourceType();

    /** 数据源中文名称（前端分类展示），如 采样任务 */
    String sourceLabel();

    /** 全量扫描当前需要提醒的业务对象（仅返回未消除的在办/逾期项），异常时返回空列表不阻断其它数据源 */
    List<ReminderItem> scan();
}
