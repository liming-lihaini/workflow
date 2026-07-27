"""
系统管理测试 - 测试数据准备脚本（大规模版）
创建 5×3×4 = 80 个部门 + 每部门 10 用户 = 800 用户（水浒传人物）

用法: python prepare_data.py
前提: 后端服务已启动 (http://localhost:8080)
"""
import sys, os, json, sqlite3, hashlib, base64
from datetime import datetime

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import common as api

DB_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                       "..", "..", "..", "..", "flow-engine", "flow_engine.db")
DB_PATH = os.path.normpath(DB_PATH)

CREATED_IDS_FILE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "created_ids.json")
created_ids = {
    "dept_ids": [], "user_ids": [], "role_ids": [],
    "dict_type_ids": [], "dict_item_ids": [],
}

# ============================================================
# 水浒传人物名池 (160 人，800 用户循环使用)
# ============================================================
HEROES = [
    # 天罡星 36
    "宋江","卢俊义","吴用","公孙胜","关胜","林冲","秦明","呼延灼",
    "花荣","柴进","李应","朱仝","鲁智深","武松","董平","张清",
    "杨志","徐宁","索超","戴宗","刘唐","李逵","史进","穆弘",
    "雷横","李俊","阮小二","张横","阮小五","张顺","阮小七","杨雄",
    "石秀","解珍","解宝","燕青",
    # 地煞星 72
    "黄信","孙立","宣赞","郝思文","韩滔","彭玘","单廷珪","魏定国",
    "萧让","裴宣","欧鹏","邓飞","燕顺","杨林","凌振","蒋敬",
    "吕方","郭盛","安道全","皇甫端","王英","扈三娘","鲍旭","樊瑞",
    "孔明","孔亮","项充","李衮","金大坚","马麟","童威","童猛",
    "孟康","侯健","陈达","杨春","郑天寿","陶宗旺","薛永","施恩",
    "李忠","周通","汤隆","杜兴","宋清","乐和","龚旺","丁得胜",
    "穆春","曹正","宋万","杜迁","邹渊","邹润","朱贵","蔡福",
    "蔡庆","李立","李云","焦挺","石勇","张青","孙二娘","王定六",
    "郁保四","顾大嫂","张青嫂","段景住","时迁","白胜","朱武",
    # 其他人物
    "晁盖","王伦","高俅","蔡京","童贯","杨戬","方腊","田虎",
    "王庆","栾廷玉","史文恭","苏定","祝彪","祝虎","祝龙","扈太公",
    "西门庆","潘金莲","武大郎","阎婆惜","张文远","陆谦","富安","董超",
    "薛霸","蒋门神","张都监","黄文炳","刘高","慕容彦达","梁中书","蔡九知府",
    "牛二","郑屠","王婆","唐牛儿","何涛","黄安","闻达","李成",
    "贾氏","李固","林娘子","张教头","锦儿","迎儿","梅香","春香",
    "武大嫂","宋太公","李太公","孔太公","史太公","赵员外","金翠莲","金老",
]

# ============================================================
# 部门层级定义 (5 × 3 × 4 = 80)
# ============================================================
DEPT_TREE = [
    {"name": "总经办", "children": [
        {"name": "战略规划部", "children": ["市场战略组","投资分析组","企业发展组","竞争情报组"]},
        {"name": "综合管理部", "children": ["行政办公组","后勤保障组","法务合规组","档案信息组"]},
        {"name": "审计监察部", "children": ["内部审计组","纪检监察组","风控管理组","合规检查  组"]},
    ]},
    {"name": "技术研发中心", "children": [
        {"name": "平台架构部", "children": ["基础架构组","数据平台组","中间件组","云原生组"]},
        {"name": "应用开发部", "children": ["前端开发组","后端开发组","移动端组","全栈工程组"]},
        {"name": "质量保障部", "children": ["测试工程组","安全审计组","性能测试组","自动化测试组"]},
    ]},
    {"name": "运营管理部", "children": [
        {"name": "市场营销部", "children": ["品牌推广组","渠道运营组","新媒体组","活动策划组"]},
        {"name": "客户服务部", "children": ["售前咨询组","售后支持组","客户成功组","投诉处理组"]},
        {"name": "供应链部", "children": ["采购管理组","仓储物流组","供应商管理组","库存控制组"]},
    ]},
    {"name": "财务管理中心", "children": [
        {"name": "会计核算部", "children": ["账务处理组","税务管理组","成本核算组","资产管理组"]},
        {"name": "资金管理部", "children": ["资金调度组","投融资组","外汇管理组","银行关系组"]},
        {"name": "预算控制部", "children": ["预算编制组","执行监控组","经营分析组","财务报告组"]},
    ]},
    {"name": "人力行政中心", "children": [
        {"name": "人力资源部", "children": ["招聘培训组","薪酬绩效组","员工关系组","组织发展组"]},
        {"name": "行政管理部", "children": ["办公管理组","车辆管理组","安保消防组","基建维修组"]},
        {"name": "企业文化部", "children": ["品牌宣传组","员工活动组","党建工  会组","社会责任组"]},
    ]},
]

NOW = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
PWD_HASH = base64.b64encode(hashlib.sha256(b"123456").digest()).decode()


def save_created_ids():
    with open(CREATED_IDS_FILE, "w", encoding="utf-8") as f:
        json.dump(created_ids, f, ensure_ascii=False, indent=2)
    print(f"\n[保存] 已创建资源 ID 写入 {CREATED_IDS_FILE}")


# ============================================================
# 第一部分：直接 SQLite 批量创建部门 + 用户
# ============================================================
def bulk_create_depts_and_users():
    """直接操作 SQLite 批量创建 80 部门 + 800 用户"""
    print(f"\n[数据库] {DB_PATH}")
    if not os.path.exists(DB_PATH):
        print("[错误] 数据库文件不存在，请确认路径")
        sys.exit(1)

    conn = sqlite3.connect(DB_PATH)
    c = conn.cursor()

    # ---------- 创建部门，同时记录每个部门的 (id, name) ----------
    api.log_section("批量创建部门 (5×3×4 = 80)")
    all_dept_ids = []
    all_depts = []  # [(dept_id, dept_name), ...]  按创建顺序

    for l1_sort, l1 in enumerate(DEPT_TREE):
        l1_name = l1["name"]
        c.execute(
            "INSERT INTO sys_dept (parent_id,dept_name,dept_code,dept_type,sort_order,status,create_time,update_time) "
            "VALUES (0,?,?,?,1,1,?,?)",
            (l1_name, f"DEPT-{l1_name[:2]}", "company", NOW, NOW))
        l1_id = c.lastrowid
        all_dept_ids.append(l1_id)
        all_depts.append((l1_id, l1_name))
        api.log_success(f"L1: {l1_name} (id={l1_id})")

        for l2_sort, l2 in enumerate(l1["children"]):
            l2_name = l2["name"]
            c.execute(
                "INSERT INTO sys_dept (parent_id,dept_name,dept_code,dept_type,sort_order,status,create_time,update_time) "
                "VALUES (?,?,?,?,?,1,?,?)",
                (l1_id, l2_name, f"DEPT-{l2_name[:2]}", "dept", l2_sort + 1, NOW, NOW))
            l2_id = c.lastrowid
            all_dept_ids.append(l2_id)
            all_depts.append((l2_id, l2_name))

            for l3_sort, l3_name_raw in enumerate(l2["children"]):
                l3_name = l3_name_raw.strip()
                c.execute(
                    "INSERT INTO sys_dept (parent_id,dept_name,dept_code,dept_type,sort_order,status,create_time,update_time) "
                    "VALUES (?,?,?,?,?,1,?,?)",
                    (l2_id, l3_name, f"GRP-{l3_name[:2]}", "group", l3_sort + 1, NOW, NOW))
                l3_id = c.lastrowid
                all_dept_ids.append(l3_id)
                all_depts.append((l3_id, l3_name))

    created_ids["dept_ids"] = all_dept_ids
    print(f"  部门创建完成: {len(all_dept_ids)} 个")

    # ---------- 为每个部门创建 10 个用户 ----------
    api.log_section(f"批量创建用户 ({len(all_depts)} 部门 × 10 人 = {len(all_depts)*10})")
    hero_idx = 0
    hero_count = {}  # real_name -> count，用于重名后缀
    all_user_ids = []
    all_post_rows = []
    first_user_per_dept = {}  # dept_id -> (user_id, real_name)  用于设置领导

    for dept_id, dept_name in all_depts:
        for u_idx in range(10):
            base_name = HEROES[hero_idx % len(HEROES)]
            hero_count[base_name] = hero_count.get(base_name, 0) + 1
            real_name = base_name if hero_count[base_name] == 1 else f"{base_name}{hero_count[base_name]}"
            username = f"d{dept_id:03d}u{u_idx+1:02d}"

            c.execute(
                "INSERT INTO sys_user (username,password,real_name,dept_id,security_level,status,create_time,update_time) "
                "VALUES (?,?,?,?,1,1,?,?)",
                (username, PWD_HASH, real_name, dept_id, NOW, NOW))
            uid = c.lastrowid
            all_user_ids.append(uid)
            all_post_rows.append((uid, dept_id, 0, 1))

            if dept_id not in first_user_per_dept:
                first_user_per_dept[dept_id] = (uid, real_name)

            hero_idx += 1

    # 批量插入 user_post
    c.executemany(
        "INSERT INTO sys_user_post (user_id,dept_id,post_id,is_main) VALUES (?,?,?,?)",
        all_post_rows)

    created_ids["user_ids"] = all_user_ids
    print(f"  用户创建完成: {len(all_user_ids)} 人")

    # ---------- 设置部门领导（每个部门第一个用户） ----------
    leader_count = 0
    for dept_id, _ in all_depts:
        if dept_id in first_user_per_dept:
            uid, rname = first_user_per_dept[dept_id]
            c.execute(
                "UPDATE sys_dept SET leader_id=?, leader_name=?, update_time=? WHERE id=?",
                (uid, rname, NOW, dept_id))
            leader_count += 1
    print(f"  部门领导设置完成: {leader_count} 个")

    conn.commit()
    conn.close()
    api.log_success(f"SQLite 批量操作完成: {len(all_dept_ids)} 部门 + {len(all_user_ids)} 用户")


# ============================================================
# 第二部分：API 创建测试角色/字典 (TC03-TC07)
# ============================================================
def prepare_test_roles():
    api.log_section("TC04 - 创建测试角色")
    test_roles = [
        {"roleName": "审批专员", "roleKey": "tc_approver"},
        {"roleName": "观察员", "roleKey": "tc_observer"},
    ]
    for rd in test_roles:
        s, r = api.post("/system/roles", rd)
        if api.ok(s, r):
            role = api.data(r)
            created_ids["role_ids"].append(role["id"])
            api.log_success(f"创建角色: {role['roleName']} (id={role['id']})")
        else:
            api.log_fail(f"创建角色: {rd['roleName']}", s, r)

    # 为第一个角色分配权限
    if created_ids["role_ids"]:
        s, r = api.get("/system/permissions")
        if api.ok(s, r):
            perms = api.data(r)
            perm_ids = [p["id"] for p in (perms or [])[:5]]
            if perm_ids:
                s2, r2 = api.put(f"/system/roles/{created_ids['role_ids'][0]}/permissions", perm_ids)
                if api.ok(s2, r2):
                    api.log_success(f"分配权限: 审批专员 -> {len(perm_ids)} 个权限")


def prepare_test_dict():
    api.log_section("TC05 - 创建测试字典")
    test_types = [
        {"dictName": "审批结果", "dictCode": "tc_approval_result", "description": "测试-审批结果枚举"},
        {"dictName": "优先级", "dictCode": "tc_priority", "description": "测试-优先级枚举"},
    ]
    type_id_for_items = None
    for td in test_types:
        s, r = api.post("/system/dict/types", td)
        if api.ok(s, r):
            dt = api.data(r)
            created_ids["dict_type_ids"].append(dt["id"])
            api.log_success(f"创建字典类型: {dt['dictName']} (id={dt['id']})")
            if td["dictCode"] == "tc_approval_result":
                type_id_for_items = dt["id"]
        else:
            api.log_fail(f"创建字典类型: {td['dictName']}", s, r)

    if type_id_for_items:
        for item in [
            {"itemText": "同意", "itemValue": "approve", "sortOrder": 1, "dictTypeId": type_id_for_items},
            {"itemText": "拒绝", "itemValue": "reject", "sortOrder": 2, "dictTypeId": type_id_for_items},
            {"itemText": "退回", "itemValue": "return", "sortOrder": 3, "dictTypeId": type_id_for_items},
        ]:
            s, r = api.post("/system/dict/items", item)
            if api.ok(s, r):
                di = api.data(r)
                created_ids["dict_item_ids"].append(di["id"])
                api.log_success(f"创建字典项: {di['itemText']}={di['itemValue']}")
            else:
                api.log_fail(f"创建字典项: {item['itemText']}", s, r)


def prepare_test_users_api():
    """通过 API 创建少量测试用户 (TC03)"""
    api.log_section("TC03 - 创建测试用户")
    # 取第一个部门
    s, r = api.get("/system/depts/page", {"page": 1, "size": 1})
    dept_id = None
    if api.ok(s, r):
        depts = api.data(r).get("list", [])
        if depts:
            dept_id = depts[0]["id"]

    test_users = [
        {"username": "testuser01", "realName": "测试员甲", "password": "123456", "deptId": dept_id, "status": 1},
        {"username": "testuser02", "realName": "测试员乙", "password": "123456", "deptId": dept_id, "status": 1},
        {"username": "testuser_disabled", "realName": "测试禁用用户", "password": "123456", "deptId": dept_id, "status": 0},
    ]
    test_user_ids = []
    for ud in test_users:
        s, r = api.post("/system/users", ud)
        if api.ok(s, r):
            u = api.data(r)
            created_ids["user_ids"].append(u["id"])
            test_user_ids.append(u["id"])
            api.log_success(f"创建用户: {u['username']} (id={u['id']})")
        else:
            api.log_fail(f"创建用户: {ud['username']}", s, r)

    # 给第一个测试用户分配角色
    if created_ids["role_ids"] and test_user_ids:
        s, r = api.post(f"/system/users/{test_user_ids[0]}/roles", [created_ids["role_ids"][0]])
        if api.ok(s, r):
            api.log_success("授权角色: testuser01 -> 审批专员")


# ============================================================
# 第三部分：触发操作日志 + 登录日志 (TC01, TC06)
# ============================================================
def prepare_log_data():
    api.log_section("TC06 - 触发操作日志")

    api.log_subsection("触发部门操作日志")
    s, r = api.post("/system/depts", {"deptName": "日志测试部门", "sortOrder": 99, "status": 1, "parentId": 0})
    if api.ok(s, r):
        d = api.data(r)
        api.log_success(f"创建: {d['deptName']}")
        api.put(f"/system/depts/{d['id']}", {"deptName": "日志测试部门(改)", "sortOrder": 99, "status": 1})
        api.log_success("编辑")
        api.delete(f"/system/depts/{d['id']}")
        api.log_success("删除")

    api.log_subsection("触发用户操作日志")
    s, r = api.post("/system/users", {"username": "logtest_user", "realName": "日志测试", "password": "123456", "status": 1})
    if api.ok(s, r):
        u = api.data(r)
        api.log_success(f"创建: {u['username']}")
        api.put(f"/system/users/{u['id']}", {"realName": "日志测试(改)", "status": 1})
        api.log_success("编辑")
        api.post(f"/system/users/{u['id']}/reset-pwd", {"password": "123456"})
        api.log_success("重置密码")
        api.delete(f"/system/users/{u['id']}")
        api.log_success("删除")

    api.log_subsection("触发角色操作日志")
    s, r = api.post("/system/roles", {"roleName": "日志测试角色", "roleKey": "tc_log_role"})
    if api.ok(s, r):
        rl = api.data(r)
        api.log_success(f"创建: {rl['roleName']}")
        api.put(f"/system/roles/{rl['id']}", {"roleName": "日志测试角色(改)"})
        api.log_success("编辑")
        api.put(f"/system/roles/{rl['id']}/permissions", [])
        api.log_success("分配权限")
        api.delete(f"/system/roles/{rl['id']}")
        api.log_success("删除")

    api.log_subsection("触发字典操作日志")
    s, r = api.post("/system/dict/types", {"dictName": "日志字典", "dictCode": "tc_log_dict", "description": "日志测试"})
    if api.ok(s, r):
        dt = api.data(r)
        api.log_success(f"创建: {dt['dictName']}")
        api.post("/system/dict/items", {"itemText": "测试", "itemValue": "v", "sortOrder": 1, "dictTypeId": dt["id"]})
        api.log_success("创建字典项")
        api.delete(f"/system/dict/types/{dt['id']}")
        api.log_success("删除")


def prepare_auth_log_data():
    api.log_section("TC01 - 触发登录/登出日志")
    import urllib.request
    for username, password in [("sys_admin", "admin123"), ("sec_admin", "admin123")]:
        try:
            data = json.dumps({"username": username, "password": password}).encode()
            req = urllib.request.Request(f"{api.BASE_URL}/auth/login", data=data,
                                         headers={"Content-Type": "application/json"}, method="POST")
            with urllib.request.urlopen(req, timeout=10) as resp:
                body = json.loads(resp.read().decode())
            if body.get("code") == 0:
                api.log_success(f"{username} 登录")
                token = body["data"]["token"]
                req2 = urllib.request.Request(f"{api.BASE_URL}/auth/logout",
                                              headers={"Authorization": f"Bearer {token}", "Content-Type": "application/json"},
                                              method="POST")
                urllib.request.urlopen(req2, timeout=10)
                api.log_success(f"{username} 登出")
        except Exception as e:
            api.log_fail(f"{username} 登录/登出异常: {e}")


# ============================================================
# 主入口
# ============================================================
def main():
    print("=" * 60)
    print("  系统管理模块 - 测试数据准备 (大规模版)")
    print("  部门: 5×3×4=80 | 用户: 80×10=800")
    print("=" * 60)

    # 1. 登录 (验证后端可用)
    try:
        api.login()
    except Exception as e:
        print(f"\n[错误] 登录失败: {e}")
        print("请确保后端服务已启动: http://localhost:8080")
        sys.exit(1)

    # 2. 直接 SQLite 批量创建部门+用户 (高效)
    bulk_create_depts_and_users()

    # 3. API 创建测试数据 (TC03-TC07)
    prepare_test_roles()
    prepare_test_dict()
    prepare_test_users_api()

    # 4. 触发日志
    prepare_log_data()
    prepare_auth_log_data()

    # 5. 保存 + 统计
    save_created_ids()
    print("\n" + "=" * 60)
    print("  准备完成汇总")
    print("=" * 60)
    for key, ids in created_ids.items():
        print(f"  {key}: {len(ids)} 条")
    print(f"\n测试数据准备完毕！共 {len(created_ids['dept_ids'])} 部门 + {len(created_ids['user_ids'])} 用户")
    print(f"清理数据请运行: python cleanup_data.py")


if __name__ == "__main__":
    main()
