package com.flow.engine.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 轻量业务编号生成器（ISSUE-023 复用，避免重复造轮子）。
 * 规则：前缀 + yyyyMMdd + 当日序号（4 位补零）。
 */
public final class CodeGenerator {

    private CodeGenerator() {
    }

    public static String generate(String prefix, int dailySeq) {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE); // yyyyMMdd
        return prefix + date + String.format("%04d", dailySeq);
    }
}
