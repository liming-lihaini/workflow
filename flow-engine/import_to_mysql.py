#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""在 MySQL 中建库、执行 schema_mysql.sql，并把 SQLite 数据导入 MySQL。"""
import os
import sqlite3

import pymysql

SQLITE_DB = r"D:\source_code\workflow\flow-engine\flow_engine.db"
SCHEMA_SQL = r"D:\source_code\workflow\flow-engine\schema_mysql.sql"

MYSQL_HOST = "127.0.0.1"
MYSQL_PORT = 3306
MYSQL_USER = "root"
MYSQL_PASS = "123456"
MYSQL_DB = "flow_engine"

BATCH = 500


def mysql_literal(val):
    if val is None:
        return "NULL"
    if isinstance(val, (bytes, bytearray)):
        return "X'" + val.hex() + "'"
    if isinstance(val, (int, float)):
        return str(val)
    s = str(val)
    s = s.replace("\\", "\\\\").replace("'", "''")
    return "'" + s + "'"


def split_statements(sql):
    out = []
    buf = []
    for line in sql.split("\n"):
        if line.strip().startswith("--"):
            continue
        buf.append(line)
        if line.strip().endswith(";"):
            stmt = "\n".join(buf).strip().rstrip(";").strip()
            if stmt:
                out.append(stmt)
            buf = []
    if buf:
        stmt = "\n".join(buf).strip().rstrip(";").strip()
        if stmt:
            out.append(stmt)
    return out


def main():
    # 1) 连接 MySQL（先不指定库），建库
    conn = pymysql.connect(host=MYSQL_HOST, port=MYSQL_PORT, user=MYSQL_USER,
                           password=MYSQL_PASS, charset="utf8mb4")
    conn.autocommit(True)
    cur = conn.cursor()
    cur.execute("DROP DATABASE IF EXISTS `%s`" % MYSQL_DB)
    cur.execute("CREATE DATABASE IF NOT EXISTS `%s` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci" % MYSQL_DB)
    cur.execute("USE `%s`" % MYSQL_DB)
    cur.execute("SET FOREIGN_KEY_CHECKS=0")
    print("已建库并选中 %s" % MYSQL_DB)

    # 2) 执行 schema
    with open(SCHEMA_SQL, "r", encoding="utf-8") as f:
        schema = f.read()
    stmts = split_statements(schema)
    ok = 0
    for st in stmts:
        try:
            cur.execute(st)
            ok += 1
        except Exception as e:
            print("  [SCHEMA 警告] %s ... -> %s" % (st[:60].replace("\n", " "), e))
    print("schema 执行完成, 语句数=%d, 成功=%d" % (len(stmts), ok))

    # 3) 从 SQLite 导入数据
    scon = sqlite3.connect(SQLITE_DB)
    scon.text_factory = str
    scur = scon.cursor()
    scur.execute("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name")
    tables = [r[0] for r in scur.fetchall()]

    total_rows = 0
    for t in tables:
        scur.execute("PRAGMA table_info('%s')" % t)
        cols = [c[1] for c in scur.fetchall()]
        scur.execute("SELECT COUNT(*) FROM `%s`" % t)
        cnt = scur.fetchone()[0]
        if cnt == 0:
            print("  %s: 0 行 (跳过)" % t)
            continue
        col_sql = ", ".join("`%s`" % c for c in cols)
        scur.execute("SELECT * FROM `%s`" % t)
        rows = scur.fetchall()
        # 批量插入
        inserted = 0
        for i in range(0, len(rows), BATCH):
            chunk = rows[i:i + BATCH]
            values = []
            for row in chunk:
                values.append("(" + ", ".join(mysql_literal(v) for v in row) + ")")
            sql = "INSERT INTO `%s` (%s) VALUES %s" % (t, col_sql, ", ".join(values))
            try:
                cur.execute(sql)
                inserted += len(chunk)
            except Exception as e:
                print("  [DATA 错误] %s (批 %d) -> %s" % (t, i, e))
                break
        total_rows += inserted
        print("  %s: 导入 %d/%d 行" % (t, inserted, cnt))

    cur.execute("SET FOREIGN_KEY_CHECKS=1")
    scon.close()
    conn.close()
    print("数据导入完成, 总导入行数=%d" % total_rows)


if __name__ == "__main__":
    main()
