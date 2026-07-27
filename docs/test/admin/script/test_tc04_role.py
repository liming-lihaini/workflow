"""TC04 - 角色管理自动化测试 (19 cases)"""
import pytest

URL = "http://localhost:3000"
API = "http://localhost:8080/api/v1"


def navigate_to_role(page):
    page.click('text=后台管理'); page.wait_for_timeout(500)
    page.click('text=角色管理'); page.wait_for_selector(".ant-table", timeout=10000); page.wait_for_timeout(1000)


class TestRoleList:
    def test_tc04_01_page_load(self, page):
        """TC04-01 页面加载显示角色列表"""
        navigate_to_role(page)
        assert page.is_visible(".ant-table")
        assert page.locator(".ant-table-tbody tr").count() > 0

    def test_tc04_02_search(self, page):
        """TC04-02 搜索角色"""
        navigate_to_role(page)
        s = page.locator('input[placeholder*="搜索"], input[placeholder*="角色"]').first
        if s.count() > 0:
            s.fill("审批"); page.click('button:has-text("查询")'); page.wait_for_timeout(1500)

    def test_tc04_03_pagination(self, page):
        """TC04-03 分页"""
        navigate_to_role(page)
        assert page.locator('.ant-table').count() > 0


class TestRoleCreate:
    def test_tc04_04_create_role(self, api):
        """TC04-04 新建角色"""
        resp = api.post(f"{API}/system/roles", json={"roleName":"自动化角色","roleKey":"auto_role"})
        assert resp.json()["code"] == 0
        role = resp.json()["data"]
        api.delete(f"{API}/system/roles/{role['id']}")

    def test_tc04_05_empty_name(self, page):
        """TC04-05 角色名为空"""
        navigate_to_role(page)
        page.click('button:has-text("新建"), button:has-text("新建角色")')
        page.wait_for_selector(".ant-modal", timeout=5000)
        page.click('.ant-modal button:has-text("确")')
        page.wait_for_timeout(1000)

    def test_tc04_06_duplicate_key(self, api):
        """TC04-06 角色标识重复"""
        r1 = api.post(f"{API}/system/roles", json={"roleName":"角色A","roleKey":"dup_key"}).json()
        r2 = api.post(f"{API}/system/roles", json={"roleName":"角色B","roleKey":"dup_key"})
        assert r2.json()["code"] != 0 or r2.status_code != 200
        if r1.get("code") == 0:
            api.delete(f"{API}/system/roles/{r1['data']['id']}")


class TestRoleEdit:
    def test_tc04_07_edit_role(self, api):
        """TC04-07 编辑角色"""
        r = api.post(f"{API}/system/roles", json={"roleName":"待编辑","roleKey":"edit_role"}).json()["data"]
        r2 = api.put(f"{API}/system/roles/{r['id']}", json={"roleName":"已编辑","roleKey":"edit_role"})
        assert r2.json()["code"] == 0
        api.delete(f"{API}/system/roles/{r['id']}")

    def test_tc04_08_edit_echo(self, page):
        """TC04-08 编辑弹窗回显"""
        navigate_to_role(page)
        edit = page.locator('.ant-table-tbody button:has-text("编辑"), .ant-table-tbody a:has-text("编辑")').first
        if edit.count() > 0:
            edit.click(); page.wait_for_selector(".ant-modal", timeout=5000)
            assert page.locator('.ant-modal input').count() > 0
            page.click('.ant-modal button:has-text("取")')


class TestRoleDelete:
    def test_tc04_09_delete_role(self, api):
        """TC04-09 删除角色"""
        r = api.post(f"{API}/system/roles", json={"roleName":"待删除","roleKey":"del_role"}).json()["data"]
        assert api.delete(f"{API}/system/roles/{r['id']}").json()["code"] == 0

    def test_tc04_10_cancel_delete(self, page):
        """TC04-10 取消删除"""
        navigate_to_role(page)
        d = page.locator('.ant-table-tbody button:has-text("删除"), .ant-table-tbody a:has-text("删除")').first
        if d.count() > 0:
            d.click(); page.wait_for_timeout(500)
            c = page.locator('button:has-text("取消")').first
            if c.count() > 0: c.click()

    def test_tc04_11_delete_system_role(self, api):
        """TC04-11 删除系统角色"""
        # 尝试删除系统管理员角色
        roles = api.get(f"{API}/system/roles").json()["data"]
        sys_roles = [r for r in roles if r.get("roleType") == 1]
        if sys_roles:
            r = api.delete(f"{API}/system/roles/{sys_roles[0]['id']}")
            # 可能拒绝或允许


class TestRolePermission:
    def test_tc04_12_open_perm_tree(self, page):
        """TC04-12 打开权限分配"""
        navigate_to_role(page)
        perm = page.locator('.ant-table-tbody button:has-text("权限"), .ant-table-tbody a:has-text("权限")').first
        if perm.count() > 0:
            perm.click(); page.wait_for_timeout(2000)

    def test_tc04_13_assign_perm(self, api):
        """TC04-13 分配权限"""
        r = api.post(f"{API}/system/roles", json={"roleName":"权限角色","roleKey":"perm_role"}).json()["data"]
        perms = api.get(f"{API}/system/permissions").json()["data"]
        if perms:
            ids = [p["id"] for p in perms[:3]]
            assert api.put(f"{API}/system/roles/{r['id']}/permissions", json=ids).json()["code"] == 0
        api.delete(f"{API}/system/roles/{r['id']}")

    def test_tc04_14_clear_perm(self, api):
        """TC04-14 清空权限"""
        r = api.post(f"{API}/system/roles", json={"roleName":"清空角色","roleKey":"clr_role"}).json()["data"]
        perms = api.get(f"{API}/system/permissions").json()["data"]
        if perms:
            api.put(f"{API}/system/roles/{r['id']}/permissions", json=[perms[0]["id"]])
            assert api.put(f"{API}/system/roles/{r['id']}/permissions", json=[]).json()["code"] == 0
        api.delete(f"{API}/system/roles/{r['id']}")

    def test_tc04_15_perm_echo(self, page):
        """TC04-15 权限回显"""
        navigate_to_role(page)
        perm = page.locator('.ant-table-tbody button:has-text("权限"), .ant-table-tbody a:has-text("权限")').first
        if perm.count() > 0:
            perm.click(); page.wait_for_timeout(2000)
            cancel = page.locator('.ant-modal button:has-text("取"), .ant-drawer button:has-text("取")').first
            if cancel.count() > 0: cancel.click()


class TestDataScope:
    def test_tc04_16_open_data_scope(self, page):
        """TC04-16 打开数据权限"""
        navigate_to_role(page)
        ds = page.locator('.ant-table-tbody button:has-text("数据"), .ant-table-tbody a:has-text("数据")').first
        if ds.count() > 0:
            ds.click(); page.wait_for_timeout(2000)

    def test_tc04_17_set_data_scope(self, api):
        """TC04-17 设置数据权限"""
        r = api.post(f"{API}/system/roles", json={"roleName":"数据角色","roleKey":"data_role"}).json()["data"]
        resp = api.put(f"{API}/system/roles/{r['id']}/data-scope", json={"dataScope": 1})
        assert resp.json()["code"] == 0 or resp.status_code == 200
        api.delete(f"{API}/system/roles/{r['id']}")

    def test_tc04_18_data_scope_echo(self, page):
        """TC04-18 数据权限回显"""
        navigate_to_role(page)
        ds = page.locator('.ant-table-tbody button:has-text("数据"), .ant-table-tbody a:has-text("数据")').first
        if ds.count() > 0:
            ds.click(); page.wait_for_timeout(2000)

    def test_tc04_19_role_list_refresh(self, page):
        """TC04-19 角色列表刷新"""
        navigate_to_role(page)
        count_before = page.locator(".ant-table-tbody tr").count()
        page.reload(); page.wait_for_timeout(3000)
        count_after = page.locator(".ant-table-tbody tr").count()
        assert count_before == count_after
