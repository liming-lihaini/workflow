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
 * 收付款延期：执行中合同的收/付款节点已过计划日期且未收讫/付讫。
 * 收入合同 → 收款节点逾期；支出合同 → 付款节点逾期。
 */
@Component
public class TxnNodeOverdueProvider implements ReminderProvider {

    @Autowired(required = false)
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Override
    public String sourceType() {
        return "TXN_NODE";
    }

    @Override
    public String sourceLabel() {
        return "收付款";
    }

    @Override
    public List<ReminderItem> scan() {
        List<ReminderItem> items = new ArrayList<>();
        if (jdbcTemplate == null) return items;
        String sql = "SELECT n.id AS node_id, n.seq, n.node_name, n.plan_amount, n.plan_date, n.status, "
                + "c.id AS contract_id, c.contract_no, c.contract_type, c.lead_id, c.lead_name "
                + "FROM t_contract_node n "
                + "JOIN t_contract c ON c.id = n.contract_id "
                + "WHERE c.status = '执行中' AND n.plan_date IS NOT NULL AND n.plan_date <> '' "
                + "AND n.plan_date < DATE_FORMAT(CURDATE(), '%Y-%m-%d') "
                + "AND n.status NOT IN ('已收讫', '已付讫')";
        try {
            for (Map<String, Object> row : jdbcTemplate.queryForList(sql)) {
                ReminderItem it = new ReminderItem();
                Long nodeId = ((Number) row.get("node_id")).longValue();
                LocalDate due = LocalDate.parse(row.get("plan_date").toString().substring(0, 10));
                boolean income = "收入合同".equals(row.get("contract_type"));
                String action = income ? "收款" : "付款";
                it.setBizId(((Number) row.get("contract_id")).longValue());
                it.setBizKey(String.valueOf(nodeId));
                it.setTitle("合同【" + row.get("contract_no") + "】第 " + row.get("seq") + " 期" + action + "已逾期");
                it.setDueDate(due);
                it.setDetail("节点：" + (row.get("node_name") != null ? row.get("node_name") : "—")
                        + "，计划" + action + "日期 " + due + "，计划金额 " + row.get("plan_amount")
                        + " 元，当前状态：" + row.get("status"));
                it.setOwnerId(row.get("lead_id") != null ? ((Number) row.get("lead_id")).longValue() : null);
                it.setOwnerName(row.get("lead_name") != null ? row.get("lead_name").toString() : null);
                items.add(it);
            }
        } catch (Exception ignored) { /* 数据源异常不阻断其它来源 */ }
        return items;
    }
}
