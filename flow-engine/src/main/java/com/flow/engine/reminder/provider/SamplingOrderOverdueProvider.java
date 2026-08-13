package com.flow.engine.reminder.provider;

import com.flow.engine.reminder.ReminderItem;
import com.flow.engine.reminder.ReminderProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 采样任务延期：负责人/组员参与的采样任务未完成且已过计划结束时间。
 */
@Component
public class SamplingOrderOverdueProvider implements ReminderProvider {

    @Autowired(required = false)
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Override
    public String sourceType() {
        return "SAMPLING_ORDER";
    }

    @Override
    public String sourceLabel() {
        return "采样任务";
    }

    @Override
    public List<ReminderItem> scan() {
        List<ReminderItem> items = new ArrayList<>();
        if (jdbcTemplate == null) return items;
        String sql = "SELECT o.id AS order_id, o.order_no, d.plan_end, "
                + "(SELECT COALESCE(u.real_name, u.username) FROM t_dispatch_member dm2 "
                + "  JOIN sys_user u ON u.id = dm2.emp_id "
                + "  WHERE dm2.dispatch_id = d.id AND dm2.role = 'LEAD' LIMIT 1) AS lead_name, "
                + "(SELECT dm3.emp_id FROM t_dispatch_member dm3 "
                + "  WHERE dm3.dispatch_id = d.id AND dm3.role = 'LEAD' LIMIT 1) AS lead_id "
                + "FROM t_dispatch d "
                + "JOIN t_sampling_order o ON o.id = d.order_id "
                + "WHERE d.plan_end < NOW() AND o.status NOT IN ('已完成', '归档完成') "
                + "ORDER BY d.plan_end ASC";
        try {
            for (Map<String, Object> row : jdbcTemplate.queryForList(sql)) {
                ReminderItem it = new ReminderItem();
                Long orderId = ((Number) row.get("order_id")).longValue();
                it.setBizId(orderId);
                it.setBizKey(String.valueOf(orderId));
                it.setTitle("采样任务【" + row.get("order_no") + "】延期未完成");
                LocalDate planEnd = row.get("plan_end") != null
                        ? java.time.LocalDateTime.parse(row.get("plan_end").toString().replace(' ', 'T')).toLocalDate()
                        : null;
                it.setDueDate(planEnd);
                it.setDetail("计划结束 " + (planEnd != null ? planEnd : "—") + "，负责人：" + (row.get("lead_name") != null ? row.get("lead_name") : "—"));
                it.setOwnerId(row.get("lead_id") != null ? ((Number) row.get("lead_id")).longValue() : null);
                it.setOwnerName(row.get("lead_name") != null ? row.get("lead_name").toString() : null);
                items.add(it);
            }
        } catch (Exception ignored) { /* 数据源异常不阻断其它来源 */ }
        return items;
    }
}
