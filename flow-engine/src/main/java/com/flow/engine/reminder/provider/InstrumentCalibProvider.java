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
 * 仪器设备校准到期：校准到期日已过且未报废的仪器。
 */
@Component
public class InstrumentCalibProvider implements ReminderProvider {

    @Autowired(required = false)
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Override
    public String sourceType() {
        return "INSTRUMENT";
    }

    @Override
    public String sourceLabel() {
        return "仪器设备";
    }

    @Override
    public List<ReminderItem> scan() {
        List<ReminderItem> items = new ArrayList<>();
        if (jdbcTemplate == null) return items;
        String sql = "SELECT id, code, name, calib_due, status FROM t_instrument "
                + "WHERE calib_due IS NOT NULL AND calib_due < CURDATE() AND IFNULL(status, '') <> '报废'";
        try {
            for (Map<String, Object> row : jdbcTemplate.queryForList(sql)) {
                ReminderItem it = new ReminderItem();
                Long id = ((Number) row.get("id")).longValue();
                LocalDate due = LocalDate.parse(row.get("calib_due").toString().substring(0, 10));
                it.setBizId(id);
                it.setBizKey(String.valueOf(id));
                it.setTitle("仪器【" + row.get("name") + "】校准已到期");
                it.setDueDate(due);
                it.setDetail("编号 " + row.get("code") + "，校准到期日 " + due + "，当前状态：" + row.get("status"));
                items.add(it);
            }
        } catch (Exception ignored) { /* 数据源异常不阻断其它来源 */ }
        return items;
    }
}
