"""
系统管理模块 - pytest 测试夹具
提供浏览器、页面、API 会话、截图等基础设施
"""
import os
import json
import pytest
import requests
from playwright.sync_api import sync_playwright

# 常量
FRONTEND_URL = "http://localhost:3000"
API_BASE_URL = "http://localhost:8080/api/v1"
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
ADMIN_DIR = os.path.dirname(BASE_DIR)  # docs/test/admin/

# 模块映射: 测试文件前缀 -> (模块目录名, 中文名)
MODULE_MAP = {
    "test_tc01": ("TC01-认证管理", "认证管理"),
    "test_tc02": ("TC02-部门管理", "部门管理"),
    "test_tc03": ("TC03-用户管理", "用户管理"),
    "test_tc04": ("TC04-角色管理", "角色管理"),
    "test_tc05": ("TC05-数据字典", "数据字典"),
    "test_tc06": ("TC06-日志管理", "日志管理"),
    "test_tc07": ("TC07-三员管理", "三员管理"),
}


def get_module_dir(test_file):
    """根据测试文件路径获取模块输出目录"""
    filename = os.path.basename(test_file)
    for prefix, (dir_name, _) in MODULE_MAP.items():
        if filename.startswith(prefix):
            module_dir = os.path.join(ADMIN_DIR, dir_name)
            screenshot_dir = os.path.join(module_dir, "screenshots")
            os.makedirs(screenshot_dir, exist_ok=True)
            return module_dir, screenshot_dir, dir_name
    return ADMIN_DIR, os.path.join(ADMIN_DIR, "screenshots"), "unknown"


# ============================================================
# pytest 夹具
# ============================================================

@pytest.fixture(scope="session")
def browser_context():
    """会话级浏览器上下文"""
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        context = browser.new_context(viewport={"width": 1400, "height": 900})
        yield context
        browser.close()


@pytest.fixture
def page(browser_context):
    """每个测试独立的页面（已登录）"""
    page = browser_context.new_page()
    # 登录
    page.goto(f"{FRONTEND_URL}/login")
    page.wait_for_selector(".login-card", timeout=10000)
    page.fill('input[placeholder="请输入用户名"]', "sys_admin")
    page.fill('input[placeholder="请输入密码"]', "admin123")
    page.click('button:has-text("登录")')
    page.wait_for_url("**/dashboard**", timeout=15000)
    yield page
    page.close()


@pytest.fixture
def page_no_login(browser_context):
    """未登录的页面（用于登录测试）"""
    page = browser_context.new_page()
    yield page
    page.close()


@pytest.fixture
def api():
    """API 会话（已登录）"""
    session = requests.Session()
    resp = session.post(f"{API_BASE_URL}/auth/login", json={
        "username": "sys_admin", "password": "admin123"
    })
    data = resp.json()
    token = data["data"]["token"]
    session.headers.update({
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json"
    })
    yield session
    session.close()


@pytest.fixture(autouse=True)
def _auto_screenshot(request, browser_context):
    """自动截图钩子: 测试结束后截图"""
    yield
    # 获取当前测试的 page fixture
    page = request.node.funcargs.get("page") or request.node.funcargs.get("page_no_login")
    if page and not page.is_closed():
        _, screenshot_dir, _ = get_module_dir(request.fspath)
        tc_id = request.node.name
        screenshot_path = os.path.join(screenshot_dir, f"{tc_id}.png")
        try:
            page.screenshot(path=screenshot_path, full_page=False)
        except Exception:
            pass


@pytest.hookimpl(hookwrapper=True)
def pytest_runtest_makereport(item, call):
    """\u6d4b\u8bd5\u62a5\u544a\u94a9\u5b50: \u9644\u52a0\u622a\u56fe\u5230\u62a5\u544a"""
    outcome = yield
    report = outcome.get_result()
    if report.when == "call":
        item._report = report
        _, screenshot_dir, _ = get_module_dir(item.fspath)
        tc_id = item.name
        screenshot_path = os.path.join(screenshot_dir, f"{tc_id}.png")
        if os.path.exists(screenshot_path):
            from pytest_html import extras as html_extras
            report.extras = getattr(report, "extras", [])
            report.extras.append(html_extras.image(screenshot_path))
