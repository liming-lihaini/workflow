# -*- coding: utf-8 -*-
"""SQLite 与 MySQL flow_engine 库逐表行数一致性校验（只读）"""
import sqlite3
import pymysql

sc = sqlite3.connect(r"D:\git_hub\workflow\flow-engine\flow_engine.db")
mc = pymysql.connect(host="127.0.0.1", port=3306, user="root", password="123456",
                     database="flow_engine", charset="utf8mb4")

sc_t = [r[0] for r in sc.execute(
    "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name")]
mismatch = 0
total = 0
for t in sc_t:
    s = sc.execute('SELECT COUNT(*) FROM "%s"' % t).fetchone()[0]
    total += s
    cur = mc.cursor()
    cur.execute('SELECT COUNT(*) FROM `%s`' % t)
    m = cur.fetchone()[0]
    if s != m:
        mismatch += 1
        print("MISMATCH %-30s sqlite=%d mysql=%d" % (t, s, m))
print("tables=%d sqlite_total_rows=%d mismatched=%d" % (len(sc_t), total, mismatch))
sc.close()
mc.close()
