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
 * 合同到期：执行中合同已到期，或 30 天内即将到期（临期预警）。
 */
@Component
public class ContractExpireProvider implements ReminderProvider {

    /** 临期预警窗口（天） */
    private static final int EXPIRING_SOON_DAYS = 30;

    @Autowired(required = false)
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Override
    public String sourceType() {
        return "CONTRACT";
    }

    @Override
    public String sourceLabel() {
        return "合同";
    }

    @Override
    public List<ReminderItem> scan() {
        List<ReminderItem> items = new ArrayList<>();
        if (jdbcTemplate == null) return items;
        String sql = "SELECT id, contract_no, contract_name, counterparty_name, expire_date, lead_id, lead_name "
                + "FROM t_contract WHERE status = '执行中' AND expire_date IS NOT NULL AND expire_date <> '' "
                + "AND expire_date <= DATE_FORMAT(DATE_ADD(CURDATE(), INTERVAL " + EXPIRING_SOON_DAYS + " DAY), '%Y-%m-%d')";
        try {
            LocalDate today = LocalDate.now();
            for (Map<String, Object> row : jdbcTemplate.queryForList(sql)) {
                ReminderItem it = new ReminderItem();
                Long id = ((Number) row.get("id")).longValue();
                LocalDate due = LocalDate.parse(row.get("expire_date").toString().substring(0, 10));
                boolean expired = due.isBefore(today);
                it.setBizId(id);
                it.setBizKey(String.valueOf(id));
                it.setTitle("合同【" + row.get("contract_no") + "】" + (expired ? "已到期" : EXPIRING_SOON_DAYS + " 天内即将到期"));
                it.setDueDate(due);
                it.setDetail((row.get("contract_name") != null ? row.get("contract_name") : "")
                        + "，相对方：" + (row.get("counterparty_name") != null ? row.get("counterparty_name") : "—")
                        + "，到期日期 " + due);
                it.setOwnerId(row.get("lead_id") != null ? ((Number) row.get("lead_id")).longValue() : null);
                it.setOwnerName(row.get("lead_name") != null ? row.get("lead_name").toString() : null);
                items.add(it);
            }
        } catch (Exception ignored) { /* 数据源异常不阻断其它来源 */ }
        return items;
    }
}
