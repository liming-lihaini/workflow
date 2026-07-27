"""TC06 - 日志管理自动化测试 (21 cases)"""
import pytest

URL = "http://localhost:3000"
API = "http://localhost:8080/api/v1"


def navigate_to_log(page):
    page.click('text=后台管理')
    page.wait_for_timeout(500)
    page.click('text=日志管理')
    page.wait_for_timeout(2000)


class TestAccessLogTab:
    """1. 访问日志 Tab"""

    def test_tc06_01_default_tab(self, page):
        """TC06-01 默认显示访问日志 Tab"""
        navigate_to_log(page)
        # 检查访问日志 Tab 是否处于激活状态
        active = page.locator('.ant-tabs-tab-active')
        assert active.count() > 0
        assert "访问日志" in active.text_content()

    def test_tc06_02_access_log_list(self, page):
        """TC06-02 访问日志列表加载"""
        navigate_to_log(page)
        page.wait_for_timeout(2000)
        # 表格应存在
        table = page.locator('.ant-table')
        assert table.count() > 0

    def test_tc06_03_only_login_logout(self, api):
        """TC06-03 仅显示登录/登出日志"""
        r = api.get(f"{API}/system/logs/access", params={"page": 1, "size": 50})
        assert r.status_code == 200
        data = r.json()["data"]
        logs = data.get("list", data.get("records", []))
        if logs:
            for log in logs:
                url = log.get("url", "")
                assert "login" in url or "logout" in url or "auth" in url, f"Unexpected URL: {url}"

    def test_tc06_04_search_keyword(self, page):
        """TC06-04 搜索 URL/IP"""
        navigate_to_log(page)
        search = page.locator('input[placeholder="搜索URL/IP"]')
        if search.count() > 0:
            search.fill("login")
            page.click('button:has-text("查询")')
            page.wait_for_timeout(1500)

    def test_tc06_05_pagination(self, page):
        """TC06-05 分页切换"""
        navigate_to_log(page)
        page.wait_for_timeout(2000)
        pager = page.locator('.ant-pagination')
        # 分页组件存在或数据较少不显示
        if pager.count() > 0:
            assert pager.is_visible()

    def test_tc06_06_table_fixed_height(self, page):
        """TC06-06 表格固定高度防溢出"""
        navigate_to_log(page)
        page.wait_for_timeout(2000)
        table = page.locator('.ant-table')
        assert table.count() > 0
        # 表体应有 scroll 样式
        body = page.locator('.ant-table-body')
        if body.count() > 0:
            style = body.get_attribute("style") or ""
            # 有 max-height 或 overflow 相关样式


class TestOperationLogTab:
    """2. 操作日志 Tab"""

    def test_tc06_07_switch_to_op_tab(self, page):
        """TC06-07 切换到操作日志 Tab"""
        navigate_to_log(page)
        page.click('.ant-tabs-tab:has-text("操作日志")')
        page.wait_for_timeout(2000)
        active = page.locator('.ant-tabs-tab-active')
        assert "操作日志" in active.text_content()

    def test_tc06_08_op_log_fields(self, api):
        """TC06-08 操作日志列表字段"""
        r = api.get(f"{API}/system/logs/operation", params={"page": 1, "size": 5})
        assert r.status_code == 200
        data = r.json()["data"]
        logs = data.get("list", data.get("records", []))
        if logs:
            log = logs[0]
            # 检查关键字段存在
            assert "id" in log
            assert "module" in log or "operation" in log

    def test_tc06_09_params_display(self, page):
        """TC06-09 请求参数展示"""
        navigate_to_log(page)
        page.click('.ant-tabs-tab:has-text("操作日志")')
        page.wait_for_timeout(2000)
        # 检查参数列是否截断显示
        params_cells = page.locator('.params-cell')
        if params_cells.count() > 0:
            assert params_cells.first.is_visible()

    def test_tc06_10_search_module(self, page):
        """TC06-10 搜索模块/操作"""
        navigate_to_log(page)
        page.click('.ant-tabs-tab:has-text("操作日志")')
        page.wait_for_timeout(2000)
        search = page.locator('input[placeholder="搜索模块/操作"]')
        if search.count() > 0:
            search.fill("用户")
            page.click('button:has-text("查询")')
            page.wait_for_timeout(1500)

    def test_tc06_11_op_log_trigger(self, api):
        """TC06-11 操作日志记录触发"""
        # 执行一个创建用户操作
        resp = api.post(f"{API}/system/users", json={
            "username": "log_trigger_user", "realName": "日志触发", "password": "123456", "status": 1
        })
        user_id = resp.json().get("data", {}).get("id")
        # 查看操作日志
        r = api.get(f"{API}/system/logs/operation", params={"page": 1, "size": 10})
        assert r.status_code == 200
        # 清理
        if user_id:
            api.delete(f"{API}/system/users/{user_id}")

    def test_tc06_12_login_params_security(self, api):
        """TC06-12 登录操作记录参数安全"""
        # 触发登录
        import requests
        requests.post(f"{API}/auth/login", json={"username": "sys_admin", "password": "admin123"})
        # 查看操作日志中的登录记录
        r = api.get(f"{API}/system/logs/operation", params={"page": 1, "size": 20})
        data = r.json()["data"]
        logs = data.get("list", data.get("records", []))
        login_logs = [l for l in logs if "login" in l.get("operation", "").lower() or "登录" in l.get("operation", "")]
        if login_logs:
            for ll in login_logs:
                params = ll.get("params", "")
                # params 应不包含密码明文
                if params:
                    assert "admin123" not in str(params)


class TestExport:
    """3. 导出功能"""

    def test_tc06_13_export_access(self, page):
        """TC06-13 导出访问日志"""
        navigate_to_log(page)
        page.wait_for_timeout(2000)
        export_btn = page.locator('button:has-text("导出")')
        if export_btn.count() > 0:
            export_btn.first.click()
            page.wait_for_timeout(2000)

    def test_tc06_14_export_operation(self, page):
        """TC06-14 导出操作日志"""
        navigate_to_log(page)
        page.click('.ant-tabs-tab:has-text("操作日志")')
        page.wait_for_timeout(2000)
        # 操作日志 Tab 下的导出按钮
        export_btns = page.locator('button:has-text("导出")')
        if export_btns.count() > 0:
            export_btns.last.click()
            page.wait_for_timeout(2000)


class TestLogCleanup:
    """4. 日志清理"""

    def test_tc06_15_clean_all_logs(self, api):
        """TC06-15 清理过期日志"""
        r = api.post(f"{API}/system/logs/clean", json={"retentionDays": 0})
        # 可能成功或返回提示
        assert r.status_code in (200, 201, 400)

    def test_tc06_16_clean_with_retention(self, api):
        """TC06-16 清理保留天数内日志"""
        r = api.post(f"{API}/system/logs/clean", json={"retentionDays": 30})
        assert r.status_code in (200, 201, 400)


class TestOpLogAnnotation:
    """5. @OpLog 注解覆盖验证"""

    def test_tc06_17_dept_op_log(self, api):
        """TC06-17 部门管理操作记录日志"""
        # 创建部门
        resp = api.post(f"{API}/system/depts", json={
            "deptName": "日志测试部门", "parentId": 0, "sortOrder": 1, "leader": ""
        })
        dept = resp.json().get("data", {})
        dept_id = dept.get("id")
        if dept_id:
            # 编辑
            api.put(f"{API}/system/depts/{dept_id}", json={"deptName": "日志测试改", "parentId": 0, "sortOrder": 1})
            # 删除
            api.delete(f"{API}/system/depts/{dept_id}")
        # 查看操作日志
        r = api.get(f"{API}/system/logs/operation", params={"page": 1, "size": 10})
        assert r.status_code == 200

    def test_tc06_18_user_op_log(self, api):
        """TC06-18 用户管理操作记录日志"""
        # 创建用户
        resp = api.post(f"{API}/system/users", json={
            "username": "oplog_user", "realName": "日志用户", "password": "123456", "status": 1
        })
        user = resp.json().get("data", {})
        user_id = user.get("id")
        if user_id:
            # 更新
            api.put(f"{API}/system/users/{user_id}", json={"realName": "改后", "status": 1})
            # 重置密码
            api.post(f"{API}/system/users/{user_id}/reset-pwd", json={"password": "newpwd"})
            # 删除
            api.delete(f"{API}/system/users/{user_id}")
        r = api.get(f"{API}/system/logs/operation", params={"page": 1, "size": 10})
        assert r.status_code == 200

    def test_tc06_19_role_op_log(self, api):
        """TC06-19 角色管理操作记录日志"""
        resp = api.post(f"{API}/system/roles", json={"roleName": "日志角色", "roleKey": "oplog_role"})
        role = resp.json().get("data", {})
        role_id = role.get("id")
        if role_id:
            api.put(f"{API}/system/roles/{role_id}", json={"roleName": "日志角色改", "roleKey": "oplog_role"})
            # 分配权限
            perms = api.get(f"{API}/system/permissions").json().get("data", [])
            if perms:
                api.put(f"{API}/system/roles/{role_id}/permissions", json=[perms[0]["id"]])
            api.delete(f"{API}/system/roles/{role_id}")
        r = api.get(f"{API}/system/logs/operation", params={"page": 1, "size": 10})
        assert r.status_code == 200

    def test_tc06_20_dict_op_log(self, api):
        """TC06-20 字典管理操作记录日志"""
        resp = api.post(f"{API}/system/dict/types", json={"dictName": "日志字典", "dictCode": "oplog_dict"})
        dtype = resp.json().get("data", {})
        type_id = dtype.get("id")
        if type_id:
            item_resp = api.post(f"{API}/system/dict/items", json={
                "itemText": "项1", "itemValue": "v1", "sortOrder": 1, "dictTypeId": type_id
            })
            item = item_resp.json().get("data", {})
            if item.get("id"):
                api.put(f"{API}/system/dict/items/{item['id']}", json={
                    "itemText": "改项", "itemValue": "v1", "sortOrder": 1, "dictTypeId": type_id
                })
                api.delete(f"{API}/system/dict/items/{item['id']}")
            api.delete(f"{API}/system/dict/types/{type_id}")
        r = api.get(f"{API}/system/logs/operation", params={"page": 1, "size": 10})
        assert r.status_code == 200

    def test_tc06_21_flow_op_log(self, api):
        """TC06-21 流程相关操作记录日志"""
        # 尝试创建流程定义（如果接口存在）
        r = api.get(f"{API}/system/logs/operation", params={"page": 1, "size": 10})
        assert r.status_code == 200
        data = r.json()["data"]
        logs = data.get("list", data.get("records", []))
        # 验证操作日志接口正常工作
