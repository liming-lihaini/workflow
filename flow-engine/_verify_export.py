#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""校验导出数据行数与 INSERT 语句数一致。"""
import pymysql

conn = pymysql.connect(host="127.0.0.1", port=3306, user="root", password="123456",
                       database="flow_engine", charset="utf8mb4")
try:
    with conn.cursor() as cur:
        cur.execute("SELECT table_name FROM information_schema.tables "
                    "WHERE table_schema='flow_engine'")
        tables = [r[0] for r in cur.fetchall()]
        total = 0
        per_table = {}
        for t in tables:
            cur.execute("SELECT COUNT(*) FROM `{}`".format(t))
            n = cur.fetchone()[0]
            per_table[t] = n
            total += n
finally:
    conn.close()

with open(r"d:\git_hub\workflow\flow-engine\flow_engine_data_inserts.sql",
          encoding="utf-8") as f:
    insert_cnt = sum(1 for line in f if line.startswith("INSERT INTO"))

print("tables:", len(tables))
print("rows total:", total)
print("insert statements:", insert_cnt)
print("MATCH" if total == insert_cnt else "MISMATCH")
top = sorted(per_table.items(), key=lambda kv: -kv[1])[:8]
for t, n in top:
    print("  {:32s} {}".format(t, n))
