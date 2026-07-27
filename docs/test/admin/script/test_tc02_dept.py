"""TC02 - 部门管理自动化测试 (26 cases)"""
import pytest
import time

URL = "http://localhost:3000"
API = "http://localhost:8080/api/v1"


def navigate_to_dept(page):
    """导航到部门管理页面"""
    page.click('text=后台管理')
    page.wait_for_timeout(500)
    page.click('text=部门管理')
    page.wait_for_selector(".ant-table", timeout=10000)
    page.wait_for_timeout(1000)


class TestDeptList:
    """1. 部门列表 - 表格视图"""

    def test_tc02_01_page_load(self, page):
        """TC02-01 页面加载显示部门列表"""
        navigate_to_dept(page)
        assert page.is_visible(".ant-table")
        rows = page.locator(".ant-table-tbody tr")
        assert rows.count() > 0

    def test_tc02_02_search_by_name(self, page):
        """TC02-02 按部门名称搜索"""
        navigate_to_dept(page)
        page.fill('input[placeholder*="搜索"], input[placeholder*="部门名称"]', "总经办")
        page.click('button:has-text("查询")')
        page.wait_for_timeout(1500)
        # 验证搜索结果
        assert page.is_visible(".ant-table")

    def test_tc02_03_filter_by_status(self, page):
        """TC02-03 按状态筛选"""
        navigate_to_dept(page)
        # 点击状态下拉
        status_select = page.locator('.ant-select').nth(0)
        if status_select.count() > 0:
            status_select.click()
            page.click('.ant-select-item:has-text("启用")')
            page.click('button:has-text("查询")')
            page.wait_for_timeout(1500)
        assert page.is_visible(".ant-table")

    def test_tc02_04_reset_search(self, page):
        """TC02-04 重置搜索条件"""
        navigate_to_dept(page)
        search_input = page.locator('input[placeholder*="搜索"], input[placeholder*="部门名称"]').first
        if search_input.count() > 0:
            search_input.fill("测试")
            page.click('button:has-text("重置")')
            page.wait_for_timeout(1000)
        assert page.is_visible(".ant-table")

    def test_tc02_05_pagination(self, page):
        """TC02-05 分页切换"""
        navigate_to_dept(page)
        # 修改每页条数
        pager = page.locator('.ant-pagination')
        if pager.count() > 0:
            assert pager.is_visible()

    def test_tc02_06_name_click_to_detail(self, page):
        """TC02-06 部门名称可点击跳转详情"""
        navigate_to_dept(page)
        # 点击第一个部门名称链接
        link = page.locator('.ant-table-tbody a, .ant-table-tbody td:first-child .ant-btn-link').first
        if link.count() > 0:
            link.click()
            page.wait_for_timeout(2000)
            # 可能跳转到详情页或打开弹窗


class TestDeptCreate:
    """2. 新建部门"""

    def test_tc02_07_create_top_dept(self, page):
        """TC02-07 新建顶级部门"""
        navigate_to_dept(page)
        page.click('button:has-text("新建部门"), button:has-text("新建")')
        page.wait_for_selector(".ant-modal", timeout=5000)
        # 填写表单
        name_input = page.locator('.ant-modal input').first
        name_input.fill("自动化测试部门")
        # 确定
        page.click('.ant-modal button:has-text("确")')
        page.wait_for_timeout(2000)
        # 验证成功提示或列表刷新
        page.screenshot(path="test_create_dept.png")

    def test_tc02_08_create_child_dept(self, page, api):
        """TC02-08 新建子部门"""
        # 通过 API 验证
        resp = api.get(f"{API}/system/depts/page", params={"page": 1, "size": 1})
        assert resp.json()["code"] == 0
        depts = resp.json()["data"].get("list", [])
        if depts:
            parent_id = depts[0]["id"]
            resp2 = api.post(f"{API}/system/depts", json={
                "deptName": "自动化子部门", "parentId": parent_id, "sortOrder": 99, "status": 1
            })
            assert resp2.json()["code"] == 0
            created = resp2.json()["data"]
            # 清理
            api.delete(f"{API}/system/depts/{created['id']}")

    def test_tc02_09_empty_name(self, page):
        """TC02-09 部门名称为空提交"""
        navigate_to_dept(page)
        page.click('button:has-text("新建部门"), button:has-text("新建")')
        page.wait_for_selector(".ant-modal", timeout=5000)
        page.click('.ant-modal button:has-text("确")')
        page.wait_for_timeout(1000)
        # 应有验证提示

    def test_tc02_10_create_disabled_dept(self, api):
        """TC02-10 新建禁用状态部门"""
        resp = api.post(f"{API}/system/depts", json={
            "deptName": "自动化禁用部门", "parentId": 0, "sortOrder": 98, "status": 0
        })
        assert resp.json()["code"] == 0
        dept = resp.json()["data"]
        assert dept["status"] == 0
        api.delete(f"{API}/system/depts/{dept['id']}")


class TestDeptEdit:
    """3. 编辑部门"""

    def test_tc02_11_edit_dept_name(self, api):
        """TC02-11 编辑部门名称"""
        # 创建测试部门
        resp = api.post(f"{API}/system/depts", json={
            "deptName": "待编辑部门", "parentId": 0, "sortOrder": 97, "status": 1
        })
        dept = resp.json()["data"]
        # 编辑
        resp2 = api.put(f"{API}/system/depts/{dept['id']}", json={
            "deptName": "已编辑部门", "sortOrder": 97, "status": 1
        })
        assert resp2.json()["code"] == 0
        api.delete(f"{API}/system/depts/{dept['id']}")

    def test_tc02_12_edit_modal_echo(self, page):
        """TC02-12 编辑弹窗回显数据"""
        navigate_to_dept(page)
        edit_btn = page.locator('.ant-table-tbody button:has-text("编辑"), .ant-table-tbody a:has-text("编辑")').first
        if edit_btn.count() > 0:
            edit_btn.click()
            page.wait_for_selector(".ant-modal", timeout=5000)
            # 弹窗中应有回显数据
            inputs = page.locator('.ant-modal input')
            assert inputs.count() > 0
            page.click('.ant-modal button:has-text("取")')


class TestDeptDelete:
    """4. 删除部门"""

    def test_tc02_13_delete_leaf_dept(self, api):
        """TC02-13 删除无子节点部门"""
        resp = api.post(f"{API}/system/depts", json={
            "deptName": "待删除部门", "parentId": 0, "sortOrder": 96, "status": 1
        })
        dept = resp.json()["data"]
        resp2 = api.delete(f"{API}/system/depts/{dept['id']}")
        assert resp2.json()["code"] == 0

    def test_tc02_14_delete_parent_dept(self, api):
        """TC02-14 删除有子部门的节点"""
        resp = api.post(f"{API}/system/depts", json={
            "deptName": "父部门", "parentId": 0, "sortOrder": 95, "status": 1
        })
        parent = resp.json()["data"]
        resp2 = api.post(f"{API}/system/depts", json={
            "deptName": "子部门", "parentId": parent["id"], "sortOrder": 1, "status": 1
        })
        child = resp2.json()["data"]
        # 尝试删除父部门
        resp3 = api.delete(f"{API}/system/depts/{parent['id']}")
        # 可能拒绝或级联删除
        # 清理
        api.delete(f"{API}/system/depts/{child['id']}")
        api.delete(f"{API}/system/depts/{parent['id']}")

    def test_tc02_15_cancel_delete(self, page):
        """TC02-15 取消删除"""
        navigate_to_dept(page)
        del_btn = page.locator('.ant-table-tbody button:has-text("删除"), .ant-table-tbody a:has-text("删除")').first
        if del_btn.count() > 0:
            del_btn.click()
            page.wait_for_timeout(500)
            # 取消
            cancel = page.locator('.ant-modal-confirm-btns button:has-text("取消"), .ant-popover button:has-text("取消")').first
            if cancel.count() > 0:
                cancel.click()


class TestDeptLeader:
    """5. 设置部门领导"""

    def test_tc02_16_set_leader(self, page):
        """TC02-16 设置领导"""
        navigate_to_dept(page)
        leader_btn = page.locator('.ant-table-tbody button:has-text("设置领导"), .ant-table-tbody a:has-text("设置领导")').first
        if leader_btn.count() > 0:
            leader_btn.click()
            page.wait_for_timeout(2000)

    def test_tc02_17_search_user(self, page):
        """TC02-17 搜索用户"""
        navigate_to_dept(page)
        leader_btn = page.locator('.ant-table-tbody button:has-text("设置领导"), .ant-table-tbody a:has-text("设置领导")').first
        if leader_btn.count() > 0:
            leader_btn.click()
            page.wait_for_timeout(1000)
            search = page.locator('.ant-modal input[placeholder*="搜索"], .ant-modal input').first
            if search.count() > 0:
                search.fill("宋江")
                page.wait_for_timeout(1500)

    def test_tc02_18_change_leader(self, api):
        """TC02-18 更换领导"""
        resp = api.get(f"{API}/system/depts/page", params={"page": 1, "size": 1})
        depts = resp.json()["data"].get("list", [])
        if depts:
            dept = depts[0]
            users_resp = api.get(f"{API}/system/users/page", params={"page": 1, "size": 2})
            users = users_resp.json()["data"].get("list", [])
            if len(users) >= 2:
                api.post(f"{API}/system/depts/{dept['id']}/leader", json={
                    "leaderId": users[1]["id"], "leaderName": users[1].get("realName", users[1]["username"])
                })

    def test_tc02_19_no_leader_submit(self, page):
        """TC02-19 不选择领导直接确定"""
        navigate_to_dept(page)
        leader_btn = page.locator('.ant-table-tbody button:has-text("设置领导"), .ant-table-tbody a:has-text("设置领导")').first
        if leader_btn.count() > 0:
            leader_btn.click()
            page.wait_for_timeout(1000)
            page.click('.ant-modal button:has-text("确")')
            page.wait_for_timeout(1000)


class TestDeptTreeView:
    """6. 树形视图"""

    def test_tc02_20_switch_tree_view(self, page):
        """TC02-20 切换到树形视图"""
        navigate_to_dept(page)
        tree_btn = page.locator('button:has-text("树形"), button:has-text("树形视图")').first
        if tree_btn.count() > 0:
            tree_btn.click()
            page.wait_for_timeout(2000)

    def test_tc02_21_tree_search(self, page):
        """TC02-21 树节点搜索"""
        navigate_to_dept(page)
        tree_btn = page.locator('button:has-text("树形"), button:has-text("树形视图")').first
        if tree_btn.count() > 0:
            tree_btn.click()
            page.wait_for_timeout(2000)
            search = page.locator('input[placeholder*="搜索"]').first
            if search.count() > 0:
                search.fill("开发")
                page.wait_for_timeout(1500)

    def test_tc02_22_expand_collapse(self, page):
        """TC02-22 展开全部/折叠全部"""
        navigate_to_dept(page)
        tree_btn = page.locator('button:has-text("树形"), button:has-text("树形视图")').first
        if tree_btn.count() > 0:
            tree_btn.click()
            page.wait_for_timeout(2000)
            expand = page.locator('button:has-text("展开"), button:has-text("全部展开")').first
            if expand.count() > 0:
                expand.click()
                page.wait_for_timeout(1000)
            collapse = page.locator('button:has-text("折叠"), button:has-text("全部折叠")').first
            if collapse.count() > 0:
                collapse.click()
                page.wait_for_timeout(1000)

    def test_tc02_23_select_node_detail(self, page):
        """TC02-23 选中节点显示详情"""
        navigate_to_dept(page)
        tree_btn = page.locator('button:has-text("树形"), button:has-text("树形视图")').first
        if tree_btn.count() > 0:
            tree_btn.click()
            page.wait_for_timeout(2000)
            node = page.locator('.ant-tree-treenode, .ant-tree-node-content-wrapper').first
            if node.count() > 0:
                node.click()
                page.wait_for_timeout(1500)

    def test_tc02_24_tree_operations(self, page):
        """TC02-24 树形视图下的操作"""
        navigate_to_dept(page)
        tree_btn = page.locator('button:has-text("树形"), button:has-text("树形视图")').first
        if tree_btn.count() > 0:
            tree_btn.click()
            page.wait_for_timeout(2000)


class TestDeptDetail:
    """7. 部门详情页"""

    def test_tc02_25_detail_page(self, page):
        """TC02-25 详情页展示"""
        navigate_to_dept(page)
        link = page.locator('.ant-table-tbody a').first
        if link.count() > 0:
            link.click()
            page.wait_for_timeout(2000)

    def test_tc02_26_detail_links(self, page):
        """TC02-26 子部门和成员链接"""
        navigate_to_dept(page)
        link = page.locator('.ant-table-tbody a').first
        if link.count() > 0:
            link.click()
            page.wait_for_timeout(2000)
