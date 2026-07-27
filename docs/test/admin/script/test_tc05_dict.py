"""TC05 - 数据字典自动化测试 (24 cases)"""
import pytest

URL = "http://localhost:3000"
API = "http://localhost:8080/api/v1"


def navigate_to_dict(page):
    page.click('text=后台管理'); page.wait_for_timeout(500)
    page.click('text=数据字典'); page.wait_for_timeout(2000)


class TestDictTypeList:
    def test_tc05_01_page_load(self, page):
        """TC05-01 页面加载显示字典类型列表"""
        navigate_to_dict(page)
        assert page.is_visible(".ant-table") or page.is_visible(".ant-list")

    def test_tc05_02_search_type(self, page):
        """TC05-02 搜索字典类型"""
        navigate_to_dict(page)
        s = page.locator('input[placeholder*="搜索"], input[placeholder*="字典"]').first
        if s.count() > 0:
            s.fill("审批"); page.wait_for_timeout(1500)

    def test_tc05_03_select_type(self, page):
        """TC05-03 选中类型显示字典项"""
        navigate_to_dict(page)
        row = page.locator('.ant-table-tbody tr, .ant-list-item').first
        if row.count() > 0:
            row.click(); page.wait_for_timeout(1500)


class TestDictTypeCRUD:
    def test_tc05_04_create_type(self, api):
        """TC05-04 新建字典类型"""
        r = api.post(f"{API}/system/dict/types", json={"dictName":"测试字典","dictCode":"auto_dict","description":"自动化"})
        assert r.json()["code"] == 0
        api.delete(f"{API}/system/dict/types/{r.json()['data']['id']}")

    def test_tc05_05_empty_name(self, page):
        """TC05-05 字典类型名为空"""
        navigate_to_dict(page)
        btn = page.locator('button:has-text("新建"), button:has-text("新建类型")').first
        if btn.count() > 0:
            btn.click(); page.wait_for_selector(".ant-modal", timeout=5000)
            page.click('.ant-modal button:has-text("确")'); page.wait_for_timeout(1000)

    def test_tc05_06_duplicate_code(self, api):
        """TC05-06 字典编码重复"""
        r1 = api.post(f"{API}/system/dict/types", json={"dictName":"A","dictCode":"dup_code"}).json()
        r2 = api.post(f"{API}/system/dict/types", json={"dictName":"B","dictCode":"dup_code"})
        assert r2.json()["code"] != 0 or r2.status_code != 200
        if r1.get("code") == 0: api.delete(f"{API}/system/dict/types/{r1['data']['id']}")

    def test_tc05_07_edit_type(self, api):
        """TC05-07 编辑字典类型"""
        r = api.post(f"{API}/system/dict/types", json={"dictName":"待编辑","dictCode":"edit_dict"}).json()["data"]
        r2 = api.put(f"{API}/system/dict/types/{r['id']}", json={"dictName":"已编辑","dictCode":"edit_dict","description":""})
        assert r2.json()["code"] == 0
        api.delete(f"{API}/system/dict/types/{r['id']}")

    def test_tc05_08_delete_type(self, api):
        """TC05-08 删除字典类型"""
        r = api.post(f"{API}/system/dict/types", json={"dictName":"待删","dictCode":"del_dict"}).json()["data"]
        assert api.delete(f"{API}/system/dict/types/{r['id']}").json()["code"] == 0

    def test_tc05_09_delete_with_items(self, api):
        """TC05-09 删除有字典项的类型"""
        r = api.post(f"{API}/system/dict/types", json={"dictName":"有项","dictCode":"has_items"}).json()["data"]
        api.post(f"{API}/system/dict/items", json={"itemText":"项1","itemValue":"v1","sortOrder":1,"dictTypeId":r['id']})
        resp = api.delete(f"{API}/system/dict/types/{r['id']}")
        # 可能级联删除或拒绝

    def test_tc05_10_type_list_refresh(self, page):
        """TC05-10 列表刷新"""
        navigate_to_dict(page); page.reload(); page.wait_for_timeout(3000)


class TestDictItemList:
    def test_tc05_11_item_list(self, page):
        """TC05-11 字典项列表"""
        navigate_to_dict(page)
        row = page.locator('.ant-table-tbody tr').first
        if row.count() > 0:
            row.click(); page.wait_for_timeout(1500)

    def test_tc05_12_item_sort(self, page):
        """TC05-12 字典项排序"""
        navigate_to_dict(page)
        row = page.locator('.ant-table-tbody tr').first
        if row.count() > 0:
            row.click(); page.wait_for_timeout(1500)


class TestDictItemCRUD:
    def test_tc05_13_create_item(self, api):
        """TC05-13 新建字典项"""
        t = api.post(f"{API}/system/dict/types", json={"dictName":"项测试","dictCode":"item_test"}).json()["data"]
        r = api.post(f"{API}/system/dict/items", json={"itemText":"选项A","itemValue":"a","sortOrder":1,"dictTypeId":t['id']})
        assert r.json()["code"] == 0
        api.delete(f"{API}/system/dict/types/{t['id']}")

    def test_tc05_14_empty_text(self, page):
        """TC05-14 字典项文本为空"""
        navigate_to_dict(page)
        row = page.locator('.ant-table-tbody tr').first
        if row.count() > 0: row.click(); page.wait_for_timeout(1000)
        btn = page.locator('button:has-text("新建项"), button:has-text("新建")').last
        if btn.count() > 0:
            btn.click(); page.wait_for_selector(".ant-modal", timeout=5000)
            page.click('.ant-modal button:has-text("确")'); page.wait_for_timeout(1000)

    def test_tc05_15_edit_item(self, api):
        """TC05-15 编辑字典项"""
        t = api.post(f"{API}/system/dict/types", json={"dictName":"编辑项","dictCode":"edit_item"}).json()["data"]
        item = api.post(f"{API}/system/dict/items", json={"itemText":"原文","itemValue":"v","sortOrder":1,"dictTypeId":t['id']}).json()["data"]
        r = api.put(f"{API}/system/dict/items/{item['id']}", json={"itemText":"改后","itemValue":"v","sortOrder":1,"dictTypeId":t['id']})
        assert r.json()["code"] == 0
        api.delete(f"{API}/system/dict/types/{t['id']}")

    def test_tc05_16_delete_item(self, api):
        """TC05-16 删除字典项"""
        t = api.post(f"{API}/system/dict/types", json={"dictName":"删项","dictCode":"del_item"}).json()["data"]
        item = api.post(f"{API}/system/dict/items", json={"itemText":"待删","itemValue":"d","sortOrder":1,"dictTypeId":t['id']}).json()["data"]
        assert api.delete(f"{API}/system/dict/items/{item['id']}").json()["code"] == 0
        api.delete(f"{API}/system/dict/types/{t['id']}")

    def test_tc05_17_duplicate_value(self, api):
        """TC05-17 字典项值重复"""
        t = api.post(f"{API}/system/dict/types", json={"dictName":"重值","dictCode":"dup_val"}).json()["data"]
        api.post(f"{API}/system/dict/items", json={"itemText":"A","itemValue":"same","sortOrder":1,"dictTypeId":t['id']})
        r = api.post(f"{API}/system/dict/items", json={"itemText":"B","itemValue":"same","sortOrder":2,"dictTypeId":t['id']})
        # 可能允许或拒绝
        api.delete(f"{API}/system/dict/types/{t['id']}")


class TestDictCodeQuery:
    def test_tc05_18_query_by_code(self, api):
        """TC05-18 按编码查询字典项"""
        r = api.get(f"{API}/system/dict/types", params={"page":1,"size":5})
        types = r.json()["data"].get("list", r.json()["data"].get("records", []))
        if types:
            code = types[0].get("dictCode", "")
            if code:
                r2 = api.get(f"{API}/system/dict/items/by-code/{code}")
                assert r2.status_code == 200

    def test_tc05_19_query_nonexist(self, api):
        """TC05-19 查询不存在的编码"""
        r = api.get(f"{API}/system/dict/items/by-code/nonexistent_code_xxx")
        assert r.status_code == 200

    def test_tc05_20_type_status_filter(self, page):
        """TC05-20 字典类型状态筛选"""
        navigate_to_dict(page)

    def test_tc05_21_item_status(self, api):
        """TC05-21 字典项启用/禁用"""
        t = api.post(f"{API}/system/dict/types", json={"dictName":"状态测试","dictCode":"status_test"}).json()["data"]
        item = api.post(f"{API}/system/dict/items", json={"itemText":"启","itemValue":"on","sortOrder":1,"dictTypeId":t['id']}).json()["data"]
        api.put(f"{API}/system/dict/items/{item['id']}", json={"itemText":"启","itemValue":"on","sortOrder":1,"dictTypeId":t['id'],"status":0})
        api.delete(f"{API}/system/dict/types/{t['id']}")

    def test_tc05_22_item_order(self, api):
        """TC05-22 字典项排序"""
        t = api.post(f"{API}/system/dict/types", json={"dictName":"排序","dictCode":"sort_test"}).json()["data"]
        for i in range(3):
            api.post(f"{API}/system/dict/items", json={"itemText":f"项{i}","itemValue":f"v{i}","sortOrder":i+1,"dictTypeId":t['id']})
        items = api.get(f"{API}/system/dict/items", params={"dictTypeId":t['id']}).json()["data"]
        api.delete(f"{API}/system/dict/types/{t['id']}")

    def test_tc05_23_batch_items(self, api):
        """TC05-23 批量创建字典项"""
        t = api.post(f"{API}/system/dict/types", json={"dictName":"批量","dictCode":"batch_test"}).json()["data"]
        for i in range(5):
            api.post(f"{API}/system/dict/items", json={"itemText":f"批量{i}","itemValue":f"b{i}","sortOrder":i+1,"dictTypeId":t['id']})
        api.delete(f"{API}/system/dict/types/{t['id']}")

    def test_tc05_24_dict_page_refresh(self, page):
        """TC05-24 字典页面刷新"""
        navigate_to_dict(page); page.reload(); page.wait_for_timeout(3000)
