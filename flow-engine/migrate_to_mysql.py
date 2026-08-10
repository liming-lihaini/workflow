#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
将 flow_engine.db (SQLite) 迁移到 MySQL 8.0
- 生成 schema_mysql.sql (MySQL 8.0 全量建表脚本)
- 将表结构与数据迁移到 MySQL (root/123456 @ 127.0.0.1:3306, db=flow_engine)
"""
import os
import sqlite3
import json

SQLITE_DB = r"D:\git_hub\workflow\flow-engine\flow_engine.db"
OUT_SCHEMA = r"D:\git_hub\workflow\flow-engine\schema_mysql.sql"

MYSQL_HOST = "127.0.0.1"
MYSQL_PORT = 3306
MYSQL_USER = "root"
MYSQL_PASS = "123456"
MYSQL_DB = "flow_engine"

# ---------------- 类型映射 ----------------
def decl_to_type(decl):
    """仅根据 SQLite 列声明类型给出初始 MySQL 类型，不做数据采样。"""
    d = (decl or "").upper()
    if "INT" in d:
        return "BIGINT"
    if "REAL" in d or "FLOA" in d or "DOUB" in d:
        return "DOUBLE"
    if "BLOB" in d:
        return "LONGTEXT"
    if "BOOL" in d:
        return "TINYINT"
    return "VARCHAR(512)"


def refine_type(decl_type, col_name, sample_values):
    """根据声明类型和实际采样值，返回最终 MySQL 类型。"""
    name = (col_name or "").lower()
    # 时间类型列 -> DATETIME(6)（原 SQLite 以 TEXT 存储 LocalDateTime 的 ISO 字符串）
    # 注意：必须用"结尾/词边界"匹配，避免误伤 timeout(candidate/update 含 date 子串)
    if name.endswith(("time", "date", "datetime", "_at")) or name in (
            "valid_from", "valid_until", "begin_date", "end_date",
            "start_date", "finish_date", "due_date", "expire_date"):
        return "DATETIME(6)"
    # 长文本关键字 -> 直接用 LONGTEXT（避免超过 64KB 的 TEXT 上限）
    long_hint = ["json", "remark", "description", "content", "detail", "before_data",
                 "after_data", "params", "msg", "opinion", "result", "reason",
                 "address", "disposal_desc", "headers", "payload_template", "request_body",
                 "response_body", "error_msg", "error_message", "enum_text", "tip",
                 "variable_value", "value", "data_json", "model_json", "process_json",
                 "form_json", "attachment", "attachments", "review_opinion", "apply_reason",
                 "approve_opinion", "conclusion", "limit_val", "limit_value",
                 "standard_limit", "method"]
    if any(h in name for h in long_hint):
        return "LONGTEXT"
    base = decl_to_type(decl_type)
    # 数值类型：若采样到非数值字符串（非 None），降级为 LONGTEXT
    if base in ("BIGINT", "DOUBLE", "TINYINT"):
        for v in sample_values:
            if v is None:
                continue
            if isinstance(v, (int, float)):
                continue
            s = str(v).strip()
            if s == "":
                continue
            try:
                float(s)
            except ValueError:
                return "LONGTEXT"
        return base
    # VARCHAR：若采样到超长字符串，升级为 LONGTEXT
    if base == "VARCHAR(512)":
        for v in sample_values:
            if v is not None and len(str(v)) > 480:
                return "LONGTEXT"
    return base


def sqlite_quote(ident):
    return "`%s`" % ident


def mysql_value_literal(val):
    if val is None:
        return "NULL"
    if isinstance(val, (bytes, bytearray)):
        # 十六进制字面量
        return "X'" + val.hex() + "'"
    if isinstance(val, (int, float)):
        return str(val)
    s = str(val)
    s = s.replace("\\", "\\\\").replace("'", "''")
    return "'" + s + "'"


def main():
    if not os.path.exists(SQLITE_DB):
        raise SystemExit("找不到 SQLite 数据库: " + SQLITE_DB)

    con = sqlite3.connect(SQLITE_DB)
    con.text_factory = str
    cur = con.cursor()

    # 取出所有用户表（排除 sqlite 内部表）
    cur.execute("SELECT name, type FROM sqlite_master WHERE type IN ('table','view') AND name NOT LIKE 'sqlite_%' ORDER BY name")
    objs = cur.fetchall()

    tables = [n for n, t in objs if t == "table"]
    views = [n for n, t in objs if t == "view"]

    schema_parts = []
    schema_parts.append("-- =====================================================================")
    schema_parts.append("-- flow_engine 数据库 MySQL 8.0 全量建表脚本")
    schema_parts.append("-- 由 SQLite 库 flow_engine.db 自动迁移生成")
    schema_parts.append("-- 字符集: utf8mb4 / 排序: utf8mb4_general_ci")
    schema_parts.append("-- 共 %d 张表, %d 个视图" % (len(tables), len(views)))
    schema_parts.append("-- =====================================================================")
    schema_parts.append("")
    schema_parts.append("SET NAMES utf8mb4;")
    schema_parts.append("SET FOREIGN_KEY_CHECKS = 0;")
    schema_parts.append("")

    # 同时收集 CREATE INDEX 语句
    cur.execute("SELECT sql FROM sqlite_master WHERE type='index' AND sql IS NOT NULL AND name NOT LIKE 'sqlite_%'")
    index_sqls = [r[0] for r in cur.fetchall() if r[0]]

    # 收集视图定义
    view_defs = []
    cur.execute("SELECT name, sql FROM sqlite_master WHERE type='view'")
    for name, sql in cur.fetchall():
        if sql:
            view_defs.append(sql + ";")

    table_schemas = {}
    col_types = {}
    for t in tables:
        cur.execute("PRAGMA table_info('%s')" % t)
        cols = cur.fetchall()
        # cid, name, type, notnull, dflt_value, pk
        # 先采样该列实际数据用于类型精化
        sample_map = {}
        for cid, cname, ctype, notnull, dflt, pk in cols:
            try:
                cur.execute("SELECT `%s` FROM `%s` LIMIT 200" % (cname, t))
                sample_map[cname] = [r[0] for r in cur.fetchall()]
            except Exception:
                sample_map[cname] = []
        lines = []
        pk_cols = [c[1] for c in cols if c[5] >= 1]
        single_auto_pk = (len(pk_cols) == 1)
        for cid, cname, ctype, notnull, dflt, pk in cols:
            is_pk = (pk >= 1)
            final_type = refine_type(ctype, cname, sample_map.get(cname, []))
            col_def = "    " + sqlite_quote(cname) + " " + final_type
            if is_pk and single_auto_pk:
                col_def += " PRIMARY KEY AUTO_INCREMENT"
            else:
                is_longtext = final_type in ("LONGTEXT", "TEXT", "BLOB", "MEDIUMTEXT")
                if notnull and not is_longtext:
                    col_def += " NOT NULL"
                if dflt is not None and not is_longtext:
                    dv = str(dflt)
                    if dv.upper() in ("CURRENT_TIMESTAMP",):
                        col_def += " DEFAULT CURRENT_TIMESTAMP"
                    elif dv.isdigit() or (dv.startswith("-") and dv[1:].isdigit()) or dv.replace(".", "", 1).isdigit():
                        col_def += " DEFAULT " + dv
                    else:
                        col_def += " DEFAULT '%s'" % dv.replace("'", "''")
                else:
                    # LONGTEXT/TEXT 列不允许默认值；NOT NULL 数值/短文本补默认值
                    if notnull and not is_longtext:
                        if final_type in ("BIGINT", "DOUBLE", "TINYINT"):
                            col_def += " DEFAULT 0"
                        else:
                            col_def += " DEFAULT ''"
                    # LONGTEXT/TEXT 列：不写 DEFAULT（允许 NULL）
            lines.append(col_def)
        # 复合主键
        if len(pk_cols) > 1:
            lines.append("    PRIMARY KEY (" + ", ".join(sqlite_quote(c) for c in pk_cols) + ")")
        create_sql = "CREATE TABLE IF NOT EXISTS %s (\n%s\n) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='%s';" % (
            sqlite_quote(t), ",\n".join(lines), t)
        table_schemas[t] = create_sql
        col_types[t] = {c[1]: refine_type(c[2], c[1], sample_map.get(c[1], [])) for c in cols}
        schema_parts.append(create_sql)
        schema_parts.append("")

    # 索引：TEXT/LONGTEXT 或长 VARCHAR 列需加前缀长度，避免 "BLOB/TEXT in key" 与 key too long
    import re as _re
    for idx_sql in index_sqls:
        converted = idx_sql.replace('"', '`')
        # 解析 ON tbl (col1, col2, ...)
        m = _re.search(r"ON\s+`?(\w+)`?\s*\((.*)\)", converted, _re.IGNORECASE | _re.DOTALL)
        if m:
            tbl = m.group(1)
            cols_in_idx = [c.strip() for c in m.group(2).split(",")]
            new_cols = []
            for c in cols_in_idx:
                cm = _re.match(r"`?(\w+)`?(.*)$", c)
                cname = cm.group(1) if cm else c
                rest = cm.group(2) if cm else ""
                if rest.strip():
                    new_cols.append(c)  # 已有表达式/前缀，原样保留
                    continue
                ctype = col_types.get(tbl, {}).get(cname, "")
                if ctype in ("LONGTEXT", "TEXT", "MEDIUMTEXT", "BLOB") or ctype == "VARCHAR(512)":
                    new_cols.append("`%s`(255)" % cname)
                else:
                    new_cols.append("`%s`" % cname)
            converted = _re.sub(r"\(.*\)\s*$", "(" + ", ".join(new_cols) + ")", converted, count=1)
        schema_parts.append(converted.rstrip(";") + ";")
        schema_parts.append("")

    # 视图（MySQL 视图在导数据后再建更稳妥，这里先列出，末尾创建）
    for v in view_defs:
        schema_parts.append(v.rstrip(";") + ";")
        schema_parts.append("")

    schema_parts.append("SET FOREIGN_KEY_CHECKS = 1;")
    schema_parts.append("")

    with open(OUT_SCHEMA, "w", encoding="utf-8") as f:
        f.write("\n".join(schema_parts))
    print("已生成 schema_mysql.sql, 表数量=%d" % len(tables))

    # 导出表结构信息以便后续导入使用
    meta = {"tables": {t: [c[1] for c in cur.execute("PRAGMA table_info('%s')" % t).fetchall()] for t in tables}}
    con.close()
    return meta


if __name__ == "__main__":
    main()
