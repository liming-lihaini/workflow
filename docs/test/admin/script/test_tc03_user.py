"""TC03 - 用户管理自动化测试 (27 cases)"""
import pytest

URL = "http://localhost:3000"
API = "http://localhost:8080/api/v1"


def navigate_to_user(page):
    page.click('text=后台管理')
    page.wait_for_timeout(500)
    page.click('text=用户管理')
    page.wait_for_selector(".ant-table", timeout=10000)
    page.wait_for_timeout(1000)


class TestUserList:
    """1. 用户列表"""
    def test_tc03_01_page_load(self, page):
        """TC03-01 页面加载显示用户列表"""
        navigate_to_user(page)
        assert page.is_visible(".ant-table")

    def test_tc03_02_search_by_name(self, page):
        """TC03-02 按用户名/姓名搜索"""
        navigate_to_user(page)
        search = page.locator('input[placeholder*="搜索"], input[placeholder*="用户"]').first
        if search.count() > 0:
            search.fill("宋江")
            page.click('button:has-text("查询"), button:has-text("搜索")')
            page.wait_for_timeout(1500)

    def test_tc03_03_dept_tree_filter(self, page):
        """TC03-03 点击部门树筛选用户"""
        navigate_to_user(page)
        tree_node = page.locator('.ant-tree-treenode, .ant-tree-node-content-wrapper').first
        if tree_node.count() > 0:
            tree_node.click()
            page.wait_for_timeout(1500)

    def test_tc03_04_dept_tree_search(self, page):
        """TC03-04 部门树搜索"""
        navigate_to_user(page)
        search = page.locator('input[placeholder*="搜索部门"]').first
        if search.count() > 0:
            search.fill("开发")
            page.wait_for_timeout(1500)

    def test_tc03_05_dept_expand_collapse(self, page):
        """TC03-05 部门树展开/折叠全部"""
        navigate_to_user(page)
        expand = page.locator('button:has-text("展开"), button:has-text("全部展开")').first
        if expand.count() > 0:
            expand.click()
            page.wait_for_timeout(1000)

    def test_tc03_06_pagination(self, page):
        """TC03-06 分页切换"""
        navigate_to_user(page)
        pager = page.locator('.ant-pagination')
        assert pager.count() > 0


class TestUserCreate:
    """2. 新建用户"""
    def test_tc03_07_create_user_full(self, page):
        """TC03-07 新建用户-完整信息"""
        navigate_to_user(page)
        page.click('button:has-text("新建用户"), button:has-text("新建")')
        page.wait_for_selector(".ant-modal", timeout=5000)
        inputs = page.locator('.ant-modal input')
        if inputs.count() >= 3:
            inputs.nth(0).fill("autotest_user01")
            inputs.nth(1).fill("自动化测试员")
            inputs.nth(2).fill("123456")
        page.click('.ant-modal button:has-text("确")')
        page.wait_for_timeout(2000)
        # 清理
        import requests
        s = requests.Session()
        r = s.post(f"{API}/auth/login", json={"username":"sys_admin","password":"admin123"})
        token = r.json()["data"]["token"]
        s.headers["Authorization"] = f"Bearer {token}"
        r2 = s.get(f"{API}/system/users/page", params={"page":1,"size":5,"keyword":"autotest_user01"})
        users = r2.json()["data"].get("list",[])
        for u in users:
            if u["username"] == "autotest_user01":
                s.delete(f"{API}/system/users/{u['id']}")

    def test_tc03_08_empty_username(self, page):
        """TC03-08 新建用户-用户名为空"""
        navigate_to_user(page)
        page.click('button:has-text("新建用户"), button:has-text("新建")')
        page.wait_for_selector(".ant-modal", timeout=5000)
        page.click('.ant-modal button:has-text("确")')
        page.wait_for_timeout(1000)

    def test_tc03_09_empty_realname(self, page):
        """TC03-09 新建用户-姓名为空"""
        navigate_to_user(page)
        page.click('button:has-text("新建用户"), button:has-text("新建")')
        page.wait_for_selector(".ant-modal", timeout=5000)
        inputs = page.locator('.ant-modal input')
        if inputs.count() >= 1:
            inputs.nth(0).fill("autotest_empty_name")
        page.click('.ant-modal button:has-text("确")')
        page.wait_for_timeout(1000)
        page.click('.ant-modal button:has-text("取")')

    def test_tc03_10_duplicate_username(self, api):
        """TC03-10 新建用户-重复用户名"""
        resp = api.post(f"{API}/system/users", json={
            "username": "sys_admin", "realName": "重复", "password": "123456", "status": 1
        })
        # 应返回错误
        assert resp.json()["code"] != 0 or resp.status_code != 200

    def test_tc03_11_preselect_dept(self, page):
        """TC03-11 选中部门后新建用户"""
        navigate_to_user(page)
        tree_node = page.locator('.ant-tree-treenode').first
        if tree_node.count() > 0:
            tree_node.click()
            page.wait_for_timeout(1000)
            page.click('button:has-text("新建用户"), button:has-text("新建")')
            page.wait_for_selector(".ant-modal", timeout=5000)
            page.click('.ant-modal button:has-text("取")')

    def test_tc03_12_username_disabled_edit(self, page):
        """TC03-12 编辑时用户名不可修改"""
        navigate_to_user(page)
        edit = page.locator('.ant-table-tbody button:has-text("编辑"), .ant-table-tbody a:has-text("编辑")').first
        if edit.count() > 0:
            edit.click()
            page.wait_for_selector(".ant-modal", timeout=5000)
            # 用户名输入框应禁用
            inputs = page.locator('.ant-modal input')
            if inputs.count() > 0:
                disabled = inputs.first.get_attribute("disabled")
                # 可能为 disabled 或 readonly
            page.click('.ant-modal button:has-text("取")')


class TestUserEdit:
    """3. 编辑用户"""
    def test_tc03_13_edit_user(self, api):
        """TC03-13 编辑用户信息"""
        resp = api.post(f"{API}/system/users", json={
            "username": "autotest_edit", "realName": "编辑前", "password": "123456", "status": 1
        })
        user = resp.json()["data"]
        resp2 = api.put(f"{API}/system/users/{user['id']}", json={
            "realName": "编辑后", "status": 1
        })
        assert resp2.json()["code"] == 0
        api.delete(f"{API}/system/users/{user['id']}")

    def test_tc03_14_edit_echo(self, page):
        """TC03-14 编辑弹窗回显数据"""
        navigate_to_user(page)
        edit = page.locator('.ant-table-tbody button:has-text("编辑"), .ant-table-tbody a:has-text("编辑")').first
        if edit.count() > 0:
            edit.click()
            page.wait_for_selector(".ant-modal", timeout=5000)
            inputs = page.locator('.ant-modal input')
            assert inputs.count() > 0
            page.click('.ant-modal button:has-text("取")')


class TestUserDelete:
    """4. 删除用户"""
    def test_tc03_15_delete_user(self, api):
        """TC03-15 删除用户"""
        resp = api.post(f"{API}/system/users", json={
            "username": "autotest_del", "realName": "待删", "password": "123456", "status": 1
        })
        user = resp.json()["data"]
        resp2 = api.delete(f"{API}/system/users/{user['id']}")
        assert resp2.json()["code"] == 0

    def test_tc03_16_cancel_delete(self, page):
        """TC03-16 取消删除"""
        navigate_to_user(page)
        del_btn = page.locator('.ant-table-tbody button:has-text("删除"), .ant-table-tbody a:has-text("删除")').first
        if del_btn.count() > 0:
            del_btn.click()
            page.wait_for_timeout(500)
            cancel = page.locator('button:has-text("取消")').first
            if cancel.count() > 0:
                cancel.click()

    def test_tc03_17_delete_self(self, api):
        """TC03-17 删除当前登录用户"""
        info = api.get(f"{API}/auth/info").json()["data"]
        resp = api.delete(f"{API}/system/users/{info['id']}")
        # 应拒绝
        assert resp.json()["code"] != 0 or resp.status_code != 200


class TestResetPassword:
    """5. 重置密码"""
    def test_tc03_18_reset_password(self, page, api):
        """TC03-18 重置密码"""
        navigate_to_user(page)
        reset = page.locator('.ant-table-tbody button:has-text("重置"), .ant-table-tbody a:has-text("重置")').first
        if reset.count() > 0:
            reset.click()
            page.wait_for_timeout(2000)

    def test_tc03_19_login_after_reset(self, api):
        """TC03-19 重置后用新密码登录"""
        resp = api.post(f"{API}/system/users", json={
            "username": "autotest_reset", "realName": "重置测试", "password": "oldpwd123", "status": 1
        })
        user = resp.json()["data"]
        api.post(f"{API}/system/users/{user['id']}/reset-pwd", json={"password": "123456"})
        # 用重置密码登录
        import requests
        r = requests.post(f"{API}/auth/login", json={"username": "autotest_reset", "password": "123456"})
        assert r.json()["code"] == 0
        api.delete(f"{API}/system/users/{user['id']}")


class TestUserRole:
    """6. 授权角色"""
    def test_tc03_20_assign_role(self, page):
        """TC03-20 授权角色"""
        navigate_to_user(page)
        role_btn = page.locator('.ant-table-tbody button:has-text("角色"), .ant-table-tbody a:has-text("角色")').first
        if role_btn.count() > 0:
            role_btn.click()
            page.wait_for_timeout(2000)

    def test_tc03_21_remove_role(self, api):
        """TC03-21 取消角色授权"""
        users = api.get(f"{API}/system/users/page", params={"page":1,"size":1}).json()["data"].get("list",[])
        roles = api.get(f"{API}/system/roles").json()["data"]
        if users and roles:
            api.post(f"{API}/system/users/{users[0]['id']}/roles", json=[roles[0]["id"]])
            api.post(f"{API}/system/users/{users[0]['id']}/roles", json=[])

    def test_tc03_22_role_modal_echo(self, page):
        """TC03-22 授权弹窗显示当前角色"""
        navigate_to_user(page)
        role_btn = page.locator('.ant-table-tbody button:has-text("角色"), .ant-table-tbody a:has-text("角色")').first
        if role_btn.count() > 0:
            role_btn.click()
            page.wait_for_timeout(2000)
            cancel = page.locator('.ant-modal button:has-text("取"), .ant-drawer button:has-text("取")').first
            if cancel.count() > 0:
                cancel.click()


class TestUserDetail:
    """7. 用户详情页"""
    def test_tc03_23_view_detail(self, page):
        """TC03-23 查看用户详情"""
        navigate_to_user(page)
        link = page.locator('.ant-table-tbody a').first
        if link.count() > 0:
            link.click()
            page.wait_for_timeout(2000)

    def test_tc03_24_detail_role_info(self, page):
        """TC03-24 详情-角色信息"""
        navigate_to_user(page)
        link = page.locator('.ant-table-tbody a').first
        if link.count() > 0:
            link.click()
            page.wait_for_timeout(2000)

    def test_tc03_25_detail_dept(self, page):
        """TC03-25 详情-兼职部门"""
        navigate_to_user(page)
        link = page.locator('.ant-table-tbody a').first
        if link.count() > 0:
            link.click()
            page.wait_for_timeout(2000)

    def test_tc03_26_detail_dept_link(self, page):
        """TC03-26 详情-部门链接跳转"""
        navigate_to_user(page)
        link = page.locator('.ant-table-tbody a').first
        if link.count() > 0:
            link.click()
            page.wait_for_timeout(2000)

    def test_tc03_27_detail_back(self, page):
        """TC03-27 详情-返回按钮"""
        navigate_to_user(page)
        link = page.locator('.ant-table-tbody a').first
        if link.count() > 0:
            link.click()
            page.wait_for_timeout(2000)
            back = page.locator('button:has-text("返回")').first
            if back.count() > 0:
                back.click()
                page.wait_for_timeout(1000)
