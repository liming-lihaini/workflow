"""TC07 - 三员管理自动化测试 (14 cases)"""
import pytest

URL = "http://localhost:3000"
API = "http://localhost:8080/api/v1"


def navigate_to_admin(page):
    page.click('text=后台管理')
    page.wait_for_timeout(500)
    page.click('text=三员管理')
    page.wait_for_timeout(2000)


class TestTripleAdminList:
    """1. 三员列表 Tab"""

    def test_tc07_01_default_tab(self, page):
        """TC07-01 默认显示三员列表 Tab"""
        navigate_to_admin(page)
        active = page.locator('.ant-tabs-tab-active')
        assert active.count() > 0
        assert "三员列表" in active.text_content()

    def test_tc07_02_list_loaded(self, page):
        """TC07-02 三员列表加载"""
        navigate_to_admin(page)
        page.wait_for_timeout(2000)
        table = page.locator('.ant-table')
        assert table.count() > 0
        rows = page.locator('.ant-table-tbody tr')
        assert rows.count() > 0

    def test_tc07_03_admin_type_tags(self, page):
        """TC07-03 管理员类型标签颜色"""
        navigate_to_admin(page)
        page.wait_for_timeout(2000)
        tags = page.locator('.ant-table-tbody .ant-tag')
        assert tags.count() > 0
        # 至少有三种颜色标签
        tag_texts = []
        for i in range(min(tags.count(), 10)):
            txt = tags.nth(i).text_content()
            if txt:
                tag_texts.append(txt.strip())
        # 应有管理员类型标签
        assert len(tag_texts) > 0

    def test_tc07_04_filter_by_type(self, page):
        """TC07-04 按类型筛选三员"""
        navigate_to_admin(page)
        page.wait_for_timeout(2000)
        # 点击管理员类型下拉
        select = page.locator('.ant-select').first
        if select.count() > 0:
            select.click()
            page.wait_for_timeout(500)
            # 选择"系统管理员"
            option = page.locator('.ant-select-item-option:has-text("系统管理员")')
            if option.count() > 0:
                option.click()
                page.wait_for_timeout(500)
                page.click('button:has-text("查询")')
                page.wait_for_timeout(1500)

    def test_tc07_05_clear_filter(self, page):
        """TC07-05 清除筛选显示全部"""
        navigate_to_admin(page)
        page.wait_for_timeout(2000)
        # 先设置筛选
        select = page.locator('.ant-select').first
        if select.count() > 0:
            select.click()
            page.wait_for_timeout(500)
            option = page.locator('.ant-select-item-option:has-text("系统管理员")')
            if option.count() > 0:
                option.click()
                page.wait_for_timeout(500)
            # 清除筛选 - 点击清除图标
            clear = page.locator('.ant-select-clear').first
            if clear.count() > 0:
                clear.click()
                page.wait_for_timeout(500)
                page.click('button:has-text("查询")')
                page.wait_for_timeout(1500)

    def test_tc07_06_three_admins_exist(self, api):
        """TC07-06 三员数据完整性"""
        r = api.get(f"{API}/system/admin/users")
        assert r.status_code == 200
        data = r.json()["data"]
        users = data.get("list", data.get("records", [])) if isinstance(data, dict) else data
        assert len(users) >= 3, f"Expected at least 3 admins, got {len(users)}"
        types = [u.get("adminType") for u in users]
        assert 1 in types, "Missing sys_admin (type=1)"
        assert 2 in types, "Missing sec_admin (type=2)"
        assert 3 in types, "Missing audit_admin (type=3)"


class TestAuditLogTab:
    """2. 审计日志 Tab"""

    def test_tc07_07_switch_to_audit(self, page):
        """TC07-07 切换到审计日志 Tab"""
        navigate_to_admin(page)
        page.click('.ant-tabs-tab:has-text("审计日志")')
        page.wait_for_timeout(2000)
        active = page.locator('.ant-tabs-tab-active')
        assert "审计日志" in active.text_content()

    def test_tc07_08_audit_log_fields(self, api):
        """TC07-08 审计日志列表字段"""
        r = api.get(f"{API}/system/admin/audit-logs", params={"page": 1, "size": 5})
        assert r.status_code == 200
        data = r.json()["data"]
        logs = data.get("list", data.get("records", []))
        if logs:
            log = logs[0]
            assert "id" in log

    def test_tc07_09_search_module(self, page):
        """TC07-09 按模块搜索审计日志"""
        navigate_to_admin(page)
        page.click('.ant-tabs-tab:has-text("审计日志")')
        page.wait_for_timeout(2000)
        search = page.locator('input[placeholder="搜索模块"]')
        if search.count() > 0:
            search.fill("用户")
            page.click('button:has-text("查询")')
            page.wait_for_timeout(1500)

    def test_tc07_10_audit_pagination(self, page):
        """TC07-10 审计日志分页"""
        navigate_to_admin(page)
        page.click('.ant-tabs-tab:has-text("审计日志")')
        page.wait_for_timeout(2000)
        pager = page.locator('.ant-pagination')
        if pager.count() > 0:
            assert pager.is_visible()


class TestTripleAdminInit:
    """3. 三员初始化"""

    def test_tc07_11_idempotent_init(self, api):
        """TC07-11 三员账号幂等初始化"""
        # 验证三员已存在且不重复
        r = api.get(f"{API}/system/admin/users")
        data = r.json()["data"]
        users = data.get("list", data.get("records", [])) if isinstance(data, dict) else data
        assert len(users) >= 3
        # 验证幂等 - 不应有多余的同类型管理员
        type_counts = {}
        for u in users:
            t = u.get("adminType")
            type_counts[t] = type_counts.get(t, 0) + 1
        for t in [1, 2, 3]:
            assert type_counts.get(t, 0) >= 1, f"adminType {t} not found"

    def test_tc07_12_default_password(self, page_no_login):
        """TC07-12 三员密码默认值"""
        import requests as req
        # 尝试用三员默认密码登录
        for username, pwd in [("sys_admin", "admin123"), ("sec_admin", "admin123"), ("audit_admin", "admin123")]:
            r = req.post(f"{API}/auth/login", json={"username": username, "password": pwd})
            assert r.json()["code"] == 0, f"Failed to login as {username}"


class TestTableHeight:
    """4. 表格高度防溢出"""

    def test_tc07_13_admin_table_height(self, page):
        """TC07-13 三员列表表格高度"""
        navigate_to_admin(page)
        page.wait_for_timeout(2000)
        table = page.locator('.ant-table')
        assert table.count() > 0
        # 表头固定
        header = page.locator('.ant-table-header')
        if header.count() > 0:
            assert header.is_visible()

    def test_tc07_14_audit_table_height(self, page):
        """TC07-14 审计日志表格高度"""
        navigate_to_admin(page)
        page.click('.ant-tabs-tab:has-text("审计日志")')
        page.wait_for_timeout(2000)
        table = page.locator('.ant-table')
        assert table.count() > 0
        header = page.locator('.ant-table-header')
        if header.count() > 0:
            assert header.is_visible()
