"""
系统管理测试 - 测试数据清理脚本
清理 prepare_data.py 创建的所有测试数据（直接 SQLite + API 混合模式）

用法: python cleanup_data.py
前提: 后端服务已启动 (http://localhost:8080)
"""
import sys, os, json, sqlite3

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import common as api

DB_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                       "..", "..", "..", "..", "flow-engine", "flow_engine.db")
DB_PATH = os.path.normpath(DB_PATH)

CREATED_IDS_FILE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "created_ids.json")


def load_created_ids():
    if not os.path.exists(CREATED_IDS_FILE):
        print(f"[警告] 未找到 {CREATED_IDS_FILE}，无数据需要清理")
        return None
    with open(CREATED_IDS_FILE, "r", encoding="utf-8") as f:
        return json.load(f)


def cleanup():
    print("=" * 60)
    print("  系统管理模块 - 测试数据清理")
    print("=" * 60)

    # 登录（验证后端可用，用于 API 清理部分）
    try:
        api.login()
    except Exception as e:
        print(f"\n[错误] 登录失败: {e}")
        sys.exit(1)

    ids = load_created_ids()
    if ids is None:
        return

    total_cleaned = 0

    # ========== 第一部分：API 清理字典/角色 ==========

    # 清理字典项
    api.log_section("清理字典项")
    for item_id in reversed(ids.get("dict_item_ids", [])):
        s, r = api.delete(f"/system/dict/items/{item_id}")
        if api.ok(s, r):
            api.log_success(f"删除字典项 id={item_id}")
            total_cleaned += 1
        else:
            api.log_fail(f"删除字典项 id={item_id}", s, r)

    # 清理字典类型
    api.log_section("清理字典类型")
    for type_id in reversed(ids.get("dict_type_ids", [])):
        s, r = api.delete(f"/system/dict/types/{type_id}")
        if api.ok(s, r):
            api.log_success(f"删除字典类型 id={type_id}")
            total_cleaned += 1
        else:
            api.log_fail(f"删除字典类型 id={type_id}", s, r)

    # 清理角色
    api.log_section("清理角色")
    for role_id in reversed(ids.get("role_ids", [])):
        s, r = api.delete(f"/system/roles/{role_id}")
        if api.ok(s, r):
            api.log_success(f"删除角色 id={role_id}")
            total_cleaned += 1
        else:
            api.log_fail(f"删除角色 id={role_id}", s, r)

    # ========== 第二部分：直接 SQLite 批量清理用户和部门 ==========
    user_ids = ids.get("user_ids", [])
    dept_ids = ids.get("dept_ids", [])

    if user_ids or dept_ids:
        api.log_section(f"SQLite 批量清理 ({len(user_ids)} 用户 + {len(dept_ids)} 部门)")

        if not os.path.exists(DB_PATH):
            print(f"[错误] 数据库不存在: {DB_PATH}")
            print("[提示] 将尝试通过 API 逐条删除（非常慢）...")
            # fallback 到 API
            for uid in reversed(user_ids):
                api.post(f"/system/users/{uid}/roles", [])
                s, r = api.delete(f"/system/users/{uid}")
                if api.ok(s, r):
                    total_cleaned += 1
            for did in reversed(dept_ids):
                s, r = api.delete(f"/system/depts/{did}")
                if api.ok(s, r):
                    total_cleaned += 1
        else:
            conn = sqlite3.connect(DB_PATH)
            c = conn.cursor()

            # 先清理用户关联表
            if user_ids:
                placeholders = ",".join("?" * len(user_ids))
                c.execute(f"DELETE FROM sys_user_role WHERE user_id IN ({placeholders})", user_ids)
                c.execute(f"DELETE FROM sys_user_post WHERE user_id IN ({placeholders})", user_ids)
                c.execute(f"DELETE FROM sys_user WHERE id IN ({placeholders})", user_ids)
                api.log_success(f"清理用户: {c.rowcount} 条 + 关联记录")
                total_cleaned += len(user_ids)

            # 再清理部门（先子后父：按 ID 逆序删除）
            if dept_ids:
                sorted_dept_ids = sorted(dept_ids, reverse=True)
                placeholders = ",".join("?" * len(sorted_dept_ids))
                c.execute(f"DELETE FROM sys_dept WHERE id IN ({placeholders})", sorted_dept_ids)
                api.log_success(f"清理部门: {c.rowcount} 条")
                total_cleaned += len(dept_ids)

            conn.commit()
            conn.close()
            api.log_success("SQLite 批量清理完成")

    # 删除 ID 记录文件
    if os.path.exists(CREATED_IDS_FILE):
        os.remove(CREATED_IDS_FILE)
        print(f"\n[清理] 已删除 {CREATED_IDS_FILE}")

    # 汇总
    print("\n" + "=" * 60)
    print(f"  清理完毕，共清理 {total_cleaned} 条测试数据")
    print("=" * 60)


if __name__ == "__main__":
    cleanup()
