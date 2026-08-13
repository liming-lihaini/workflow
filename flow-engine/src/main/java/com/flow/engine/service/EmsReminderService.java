package com.flow.engine.service;

import com.flow.engine.reminder.ReminderItem;
import com.flow.engine.reminder.ReminderProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 通用到期提醒服务：多数据源（采样任务/仪器校准/标准物质/耗材/合同/收付款节点等）。
 * 查询时先调用各 {@link ReminderProvider#scan()} 同步 t_reminder（bizKey 幂等 upsert），
 * 扫描结果中消失的业务对象其提醒项自动标记已消除（resolved）。
 * 免打扰状态（用户 × 提醒项维度，t_reminder_mute）：
 * - 关闭提醒（muted）：不再弹窗
 * - 今日不再提醒（snooze_date）：当天不弹窗
 * - 其余情况：每隔 1 小时弹窗一次（last_popup_time 记录上次弹窗时间）
 */
@Service
public class EmsReminderService {

    /** 弹窗提醒间隔：1 小时 */
    private static final long POPUP_INTERVAL_HOURS = 1;

    @Autowired(required = false)
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Autowired
    private List<ReminderProvider> providers;

    /**
     * 同步全部数据源的提醒项到 t_reminder（幂等）。
     */
    public synchronized void sync() {
        if (jdbcTemplate == null) return;
        for (ReminderProvider p : providers) {
            List<ReminderItem> items;
            try {
                items = p.scan();
            } catch (Exception e) {
                continue;
            }
            Set<String> keys = new LinkedHashSet<>();
            for (ReminderItem it : items) {
                if (it.getBizKey() == null) it.setBizKey(String.valueOf(it.getBizId()));
                keys.add(it.getBizKey());
                upsertReminder(p.sourceType(), it);
            }
            // 扫描结果中不再出现的业务对象 → 历史提醒项标记已消除（任务完成/已处置/节点核销等）
            try {
                List<Map<String, Object>> existing = jdbcTemplate.queryForList(
                        "SELECT biz_key FROM t_reminder WHERE source_type = ? AND resolved = 0", p.sourceType());
                for (Map<String, Object> row : existing) {
                    String k = String.valueOf(row.get("biz_key"));
                    if (!keys.contains(k)) {
                        jdbcTemplate.update("UPDATE t_reminder SET resolved = 1, update_time = NOW() "
                                + "WHERE source_type = ? AND biz_key = ?", p.sourceType(), k);
                    }
                }
            } catch (Exception ignored) { /* 单源失败不阻断 */ }
        }
    }

    /**
     * 未消除的提醒列表（含当前用户的免打扰状态与是否需要弹窗）。
     */
    public List<Map<String, Object>> listForUser(Long userId) {
        if (jdbcTemplate == null) return new ArrayList<>();
        sync();
        String sql = "SELECT r.id, r.source_type, r.biz_id, r.title, r.detail, r.due_date, r.owner_id, r.owner_name, "
                + "m.muted, m.snooze_date, m.last_popup_time "
                + "FROM t_reminder r "
                + "LEFT JOIN t_reminder_mute m ON m.reminder_id = r.id AND m.user_id = ? "
                + "WHERE r.resolved = 0 "
                + "ORDER BY r.due_date ASC, r.id ASC";
        List<Map<String, Object>> rows;
        try {
            rows = jdbcTemplate.queryForList(sql, userId);
        } catch (Exception e) {
            return new ArrayList<>();
        }
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        Map<String, String> labels = sourceLabels();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            boolean muted = row.get("muted") != null && ((Number) row.get("muted")).longValue() == 1L;
            LocalDate snoozeDate = toLocalDate(row.get("snooze_date"));
            LocalDateTime lastPopup = toLocalDateTime(row.get("last_popup_time"));
            LocalDate due = toLocalDate(row.get("due_date"));
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", row.get("id"));
            m.put("sourceType", row.get("source_type"));
            m.put("sourceLabel", labels.getOrDefault(String.valueOf(row.get("source_type")), String.valueOf(row.get("source_type"))));
            m.put("bizId", row.get("biz_id"));
            m.put("title", row.get("title"));
            m.put("detail", row.get("detail"));
            m.put("dueDate", due != null ? due.toString() : null);
            m.put("overdueDays", due != null ? ChronoUnit.DAYS.between(due, today) : 0);
            m.put("ownerId", row.get("owner_id"));
            m.put("ownerName", row.get("owner_name"));
            m.put("muted", muted);
            m.put("snoozeToday", snoozeDate != null && !snoozeDate.isBefore(today));
            // 弹窗判定：未永久关闭 且 今日未免打扰 且 距上次弹窗超过 1 小时
            boolean needPopup = !muted
                    && !(snoozeDate != null && !snoozeDate.isBefore(today))
                    && (lastPopup == null || Duration.between(lastPopup, now).toHours() >= POPUP_INTERVAL_HOURS);
            m.put("needPopup", needPopup);
            result.add(m);
        }
        return result;
    }

    /** 数据源类型 → 中文名称（供前端分类展示） */
    public Map<String, String> sourceLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        for (ReminderProvider p : providers) {
            labels.put(p.sourceType(), p.sourceLabel());
        }
        return labels;
    }

    /** 关闭提醒：该提醒项不再弹窗（永久，直到业务对象消除）。 */
    public void dismiss(Long userId, Long reminderId) {
        upsertMute(userId, reminderId, "muted = 1", "1");
    }

    /** 今日不再提醒：当天不弹窗，次日起恢复（受 1 小时间隔控制）。 */
    public void snoozeToday(Long userId, Long reminderId) {
        upsertMute(userId, reminderId, "snooze_date = CURDATE()", "CURDATE()");
    }

    /** 记录一次弹窗时间（用于 1 小时间隔控制）。 */
    public void markPopped(Long userId, Long reminderId) {
        upsertMute(userId, reminderId, "last_popup_time = NOW()", "NOW()");
    }

    private void upsertReminder(String sourceType, ReminderItem it) {
        try {
            jdbcTemplate.update("INSERT INTO t_reminder (source_type, biz_id, biz_key, title, detail, due_date, "
                            + "owner_id, owner_name, resolved, create_time, update_time) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0, NOW(), NOW()) "
                            + "ON DUPLICATE KEY UPDATE title = VALUES(title), detail = VALUES(detail), "
                            + "due_date = VALUES(due_date), owner_id = VALUES(owner_id), "
                            + "owner_name = VALUES(owner_name), resolved = 0, update_time = NOW()",
                    sourceType, it.getBizId(), it.getBizKey(), it.getTitle(), it.getDetail(),
                    it.getDueDate(), it.getOwnerId(), it.getOwnerName());
        } catch (Exception ignored) { /* 单项写入失败不阻断 */ }
    }

    private void upsertMute(Long userId, Long reminderId, String updateClause, String insertValue) {
        if (jdbcTemplate == null || userId == null || reminderId == null) return;
        String col = updateClause.split(" ")[0];
        String sql = "INSERT INTO t_reminder_mute (user_id, reminder_id, create_time, update_time, " + col + ") "
                + "VALUES (?, ?, NOW(), NOW(), " + insertValue + ") "
                + "ON DUPLICATE KEY UPDATE " + updateClause + ", update_time = NOW()";
        jdbcTemplate.update(sql, userId, reminderId);
    }

    private LocalDate toLocalDate(Object v) {
        if (v == null) return null;
        if (v instanceof java.sql.Date d) return d.toLocalDate();
        if (v instanceof LocalDate d) return d;
        if (v instanceof LocalDateTime dt) return dt.toLocalDate();
        try {
            return LocalDate.parse(v.toString().substring(0, 10));
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDateTime toLocalDateTime(Object v) {
        if (v == null) return null;
        if (v instanceof LocalDateTime dt) return dt;
        if (v instanceof java.sql.Timestamp ts) return ts.toLocalDateTime();
        try {
            return LocalDateTime.parse(v.toString().replace(' ', 'T'));
        } catch (Exception e) {
            return null;
        }
    }
}
