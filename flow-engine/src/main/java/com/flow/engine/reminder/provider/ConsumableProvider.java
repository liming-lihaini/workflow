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
 * 耗材过期：有效期已过的耗材（提醒及时处置）。
 */
@Component
public class ConsumableProvider implements ReminderProvider {

    @Autowired(required = false)
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Override
    public String sourceType() {
        return "CONSUMABLE";
    }

    @Override
    public String sourceLabel() {
        return "耗材";
    }

    @Override
    public List<ReminderItem> scan() {
        List<ReminderItem> items = new ArrayList<>();
        if (jdbcTemplate == null) return items;
        String sql = "SELECT id, name, spec, expire_date, qty, status FROM t_consumable "
                + "WHERE expire_date IS NOT NULL AND expire_date < CURDATE()";
        try {
            for (Map<String, Object> row : jdbcTemplate.queryForList(sql)) {
                ReminderItem it = new ReminderItem();
                Long id = ((Number) row.get("id")).longValue();
                LocalDate due = LocalDate.parse(row.get("expire_date").toString().substring(0, 10));
                it.setBizId(id);
                it.setBizKey(String.valueOf(id));
                it.setTitle("耗材【" + row.get("name") + "】已过期");
                it.setDueDate(due);
                it.setDetail("规格 " + (row.get("spec") != null ? row.get("spec") : "—")
                        + "，有效期至 " + due + "，库存 " + row.get("qty") + "，状态：" + row.get("status"));
                items.add(it);
            }
        } catch (Exception ignored) { /* 数据源异常不阻断其它来源 */ }
        return items;
    }
}
