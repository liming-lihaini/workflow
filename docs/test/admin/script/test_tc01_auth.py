"""TC01 - 认证管理自动化测试 (14 cases)"""
import pytest

URL = "http://localhost:3000"
API = "http://localhost:8080/api/v1"


class TestLogin:
    """1. 用户登录"""

    def test_tc01_01_normal_login(self, page_no_login):
        """TC01-01 正常登录"""
        p = page_no_login
        p.goto(f"{URL}/login")
        p.wait_for_selector(".login-card")
        p.fill('input[placeholder="请输入用户名"]', "sys_admin")
        p.fill('input[placeholder="请输入密码"]', "admin123")
        p.click('button:has-text("登录")')
        p.wait_for_url("**/dashboard**", timeout=15000)
        assert "/dashboard" in p.url

    def test_tc01_02_wrong_password(self, page_no_login):
        """TC01-02 密码错误"""
        p = page_no_login
        p.goto(f"{URL}/login")
        p.wait_for_selector(".login-card")
        p.fill('input[placeholder="请输入用户名"]', "sys_admin")
        p.fill('input[placeholder="请输入密码"]', "wrongpwd")
        p.click('button:has-text("登录")')
        p.wait_for_timeout(2000)
        assert "/login" in p.url  # 仍在登录页

    def test_tc01_03_unknown_user(self, page_no_login):
        """TC01-03 用户名不存在"""
        p = page_no_login
        p.goto(f"{URL}/login")
        p.wait_for_selector(".login-card")
        p.fill('input[placeholder="请输入用户名"]', "nobody_xxx")
        p.fill('input[placeholder="请输入密码"]', "admin123")
        p.click('button:has-text("登录")')
        p.wait_for_timeout(2000)
        assert "/login" in p.url

    def test_tc01_04_empty_username(self, page_no_login):
        """TC01-04 用户名为空"""
        p = page_no_login
        p.goto(f"{URL}/login")
        p.wait_for_selector(".login-card")
        p.fill('input[placeholder="请输入密码"]', "admin123")
        p.click('button:has-text("登录")')
        p.wait_for_timeout(1000)
        assert "/login" in p.url

    def test_tc01_05_empty_password(self, page_no_login):
        """TC01-05 密码为空"""
        p = page_no_login
        p.goto(f"{URL}/login")
        p.wait_for_selector(".login-card")
        p.fill('input[placeholder="请输入用户名"]', "sys_admin")
        p.click('button:has-text("登录")')
        p.wait_for_timeout(1000)
        assert "/login" in p.url

    def test_tc01_06_both_empty(self, page_no_login):
        """TC01-06 用户名密码均为空"""
        p = page_no_login
        p.goto(f"{URL}/login")
        p.wait_for_selector(".login-card")
        p.click('button:has-text("登录")')
        p.wait_for_timeout(1000)
        assert "/login" in p.url


class TestSession:
    """2. 会话管理"""

    def test_tc01_07_token_persist(self, page):
        """TC01-07 Token 持久化 - 刷新后保持登录"""
        p = page
        p.reload()
        p.wait_for_timeout(2000)
        assert "/login" not in p.url
        assert "/dashboard" in p.url or "/" in p.url

    def test_tc01_08_token_invalid(self, page_no_login):
        """TC01-08 Token 失效后访问受保护页面"""
        p = page_no_login
        p.goto(f"{URL}/login")
        p.wait_for_selector(".login-card")
        p.fill('input[placeholder="请输入用户名"]', "sys_admin")
        p.fill('input[placeholder="请输入密码"]', "admin123")
        p.click('button:has-text("登录")')
        p.wait_for_url("**/dashboard**", timeout=15000)
        # 清除 token
        p.evaluate("localStorage.removeItem('token')")
        p.goto(f"{URL}/system/user")
        p.wait_for_timeout(2000)
        assert "/login" in p.url

    def test_tc01_09_logged_in_visit_login(self, page):
        """TC01-09 已登录用户访问登录页"""
        p = page
        p.goto(f"{URL}/login")
        p.wait_for_timeout(2000)
        # 应自动跳转到工作台
        assert "/login" not in p.url

    def test_tc01_10_get_user_info(self, api):
        """TC01-10 获取用户信息"""
        resp = api.get(f"{API}/auth/info")
        assert resp.status_code == 200
        data = resp.json()
        assert data["code"] == 0
        assert "username" in data["data"]


class TestLogout:
    """3. 用户登出"""

    def test_tc01_11_normal_logout(self, page):
        """TC01-11 正常登出"""
        p = page
        p.click('button:has-text("退出")')
        p.wait_for_url("**/login**", timeout=10000)
        assert "/login" in p.url

    def test_tc01_12_logout_then_visit(self, page_no_login):
        """TC01-12 登出后访问受保护页面"""
        p = page_no_login
        p.goto(f"{URL}/login")
        p.wait_for_selector(".login-card")
        p.fill('input[placeholder="请输入用户名"]', "sys_admin")
        p.fill('input[placeholder="请输入密码"]', "admin123")
        p.click('button:has-text("登录")')
        p.wait_for_url("**/dashboard**", timeout=15000)
        # 登出
        p.click('button:has-text("退出")')
        p.wait_for_url("**/login**", timeout=10000)
        # 访问受保护页面
        p.goto(f"{URL}/system/user")
        p.wait_for_timeout(2000)
        assert "/login" in p.url


class TestLoginLog:
    """4. 登录日志记录"""

    def test_tc01_13_login_access_log(self, api):
        """TC01-13 登录产生访问日志"""
        # 先触发一次登录
        import requests
        resp = requests.post(f"{API}/auth/login", json={"username": "sys_admin", "password": "admin123"})
        assert resp.json()["code"] == 0
        # 查看访问日志
        resp = api.get(f"{API}/system/logs/access", params={"page": 1, "size": 5})
        assert resp.status_code == 200
        data = resp.json()["data"]
        logs = data.get("list", data.get("records", []))
        assert len(logs) > 0
        # 至少有 login 相关日志
        urls = [l.get("url", "") for l in logs]
        assert any("login" in u for u in urls)

    def test_tc01_14_logout_access_log(self, api):
        """TC01-14 登出产生访问日志"""
        # 先登录再登出
        import requests
        resp = requests.post(f"{API}/auth/login", json={"username": "sys_admin", "password": "admin123"})
        token = resp.json()["data"]["token"]
        requests.post(f"{API}/auth/logout", headers={"Authorization": f"Bearer {token}"})
        # 查看访问日志
        resp = api.get(f"{API}/system/logs/access", params={"page": 1, "size": 10})
        logs = resp.json()["data"].get("list", resp.json()["data"].get("records", []))
        urls = [l.get("url", "") for l in logs]
        assert any("logout" in u for u in urls)
