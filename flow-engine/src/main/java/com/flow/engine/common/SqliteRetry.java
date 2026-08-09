package com.flow.engine.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * SQLite 写冲突重试工具。
 *
 * <p>SQLite 单写者模型下，并发写事务会抛出 SQLITE_BUSY(SQLITE_LOCKED 在某些场景也会表现为 BUSY)。
 * 即便 JDBC URL 已配置 busy_timeout，连接池复用的连接在长事务或自调用 HTTP 回环场景下仍可能瞬时冲突。
 * 此处对 SQLITE_BUSY 做有限次数的指数退避重试，使 webhook 回调等外部写入在瞬时锁竞争下自愈，
 * 避免一次性的「database is locked」导致流程节点回调丢失。
 */
public final class SqliteRetry {

    private static final Logger log = LoggerFactory.getLogger(SqliteRetry.class);

    private static final int MAX_ATTEMPTS = 5;
    private static final long BASE_DELAY_MS = 80;

    private SqliteRetry() {}

    public static <T> T execute(Supplier<T> action) {
        RuntimeException last = null;
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            try {
                return action.get();
            } catch (RuntimeException e) {
                if (isBusy(e) && attempt < MAX_ATTEMPTS - 1) {
                    long delay = BASE_DELAY_MS * (1L << attempt); // 80,160,320,640ms
                    log.warn("SQLite 写冲突(SQLITE_BUSY)，第{}次重试，{}ms 后重试: {}",
                            attempt + 1, delay, summary(e.getMessage()));
                    try {
                        TimeUnit.MILLISECONDS.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw e;
                    }
                    last = e;
                    continue;
                }
                throw e;
            }
        }
        throw last;
    }

    public static void execute(Runnable action) {
        execute(() -> {
            action.run();
            return null;
        });
    }

    private static boolean isBusy(Throwable e) {
        String msg = e.getMessage();
        if (msg == null) return false;
        String m = msg.toLowerCase();
        // SQLITE_BUSY(5) / SQLITE_LOCKED(6) 在连接复用时常被包装为 UncategorizedSQLException
        return m.contains("sqlite_busy")
                || m.contains("database is locked")
                || m.contains("database table is locked")
                || m.contains("[sqlite_busy]")
                || (m.contains("sql state [null]") && m.contains("error code [5]"));
    }

    private static String summary(String msg) {
        if (msg == null) return "";
        int len = Math.min(msg.length(), 200);
        return msg.substring(0, len).replace('\n', ' ');
    }
}
