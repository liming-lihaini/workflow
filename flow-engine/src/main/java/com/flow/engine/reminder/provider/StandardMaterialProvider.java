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
 * 标准物质过期：有效期已过的标准物质（不论库存状态，提醒及时处置）。
 */
@Component
public class StandardMaterialProvider implements ReminderProvider {

    @Autowired(required = false)
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Override
    public String sourceType() {
        return "STANDARD_MATERIAL";
    }

    @Override
    public String sourceLabel() {
        return "标准物质";
    }

    @Override
    public List<ReminderItem> scan() {
        List<ReminderItem> items = new ArrayList<>();
        if (jdbcTemplate == null) return items;
        String sql = "SELECT id, name, lot_no, expire_date, stock, status FROM t_standard_material "
                + "WHERE expire_date IS NOT NULL AND expire_date < CURDATE()";
        try {
            for (Map<String, Object> row : jdbcTemplate.queryForList(sql)) {
                ReminderItem it = new ReminderItem();
                Long id = ((Number) row.get("id")).longValue();
                LocalDate due = LocalDate.parse(row.get("expire_date").toString().substring(0, 10));
                it.setBizId(id);
                it.setBizKey(String.valueOf(id));
                it.setTitle("标准物质【" + row.get("name") + "】已过期");
                it.setDueDate(due);
                it.setDetail("批号 " + (row.get("lot_no") != null ? row.get("lot_no") : "—")
                        + "，有效期至 " + due + "，库存 " + row.get("stock") + "，状态：" + row.get("status"));
                items.add(it);
            }
        } catch (Exception ignored) { /* 数据源异常不阻断其它来源 */ }
        return items;
    }
}
