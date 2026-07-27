"""
系统管理测试 - 公共工具模块
提供 HTTP 请求封装、登录认证、日志输出等基础设施
"""
import json
import urllib.request
import urllib.error
import urllib.parse

BASE_URL = "http://localhost:8080/api/v1"
TOKEN = None


def login(username="sys_admin", password="admin123"):
    """登录并缓存 Token"""
    global TOKEN
    data = json.dumps({"username": username, "password": password}).encode("utf-8")
    req = urllib.request.Request(
        f"{BASE_URL}/auth/login",
        data=data,
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=10) as resp:
        body = json.loads(resp.read().decode("utf-8"))
    if body.get("code") != 0:
        raise RuntimeError(f"登录失败: {body.get('message', body)}")
    TOKEN = body["data"]["token"]
    print(f"[登录成功] user={username}, token={TOKEN[:16]}...")
    return TOKEN


def _headers(content_type="application/json"):
    """构造请求头"""
    h = {"Content-Type": content_type}
    if TOKEN:
        h["Authorization"] = f"Bearer {TOKEN}"
    return h


def _do_request(method, path, data=None, params=None):
    """通用请求方法，返回 (status_code, response_body_dict)"""
    url = f"{BASE_URL}{path}"
    if params:
        qs = urllib.parse.urlencode(
            {k: v for k, v in params.items() if v is not None}
        )
        url = f"{url}?{qs}"

    body = None
    if data is not None:
        body = json.dumps(data, ensure_ascii=False).encode("utf-8")

    req = urllib.request.Request(url, data=body, headers=_headers(), method=method)
    try:
        with urllib.request.urlopen(req, timeout=15) as resp:
            result = json.loads(resp.read().decode("utf-8"))
            return resp.status, result
    except urllib.error.HTTPError as e:
        err_body = e.read().decode("utf-8", errors="replace")
        try:
            err_json = json.loads(err_body)
        except Exception:
            err_json = {"raw": err_body}
        return e.code, err_json


def get(path, params=None):
    """GET 请求"""
    return _do_request("GET", path, params=params)


def post(path, data=None, params=None):
    """POST 请求"""
    return _do_request("POST", path, data=data, params=params)


def put(path, data=None, params=None):
    """PUT 请求"""
    return _do_request("PUT", path, data=data, params=params)


def delete(path, params=None):
    """DELETE 请求"""
    return _do_request("DELETE", path, params=params)


def ok(status, result):
    """判断请求是否成功 (code==0)"""
    if status == 200 and result.get("code") == 0:
        return True
    return False


def data(result):
    """提取响应数据"""
    return result.get("data")


def log_success(msg):
    print(f"  [OK] {msg}")


def log_fail(msg, status=None, result=None):
    detail = ""
    if result:
        detail = f" -> {result.get('message', result)}"
    print(f"  [FAIL] {msg} (status={status}){detail}")


def log_section(title):
    print(f"\n{'='*60}")
    print(f"  {title}")
    print(f"{'='*60}")


def log_subsection(title):
    print(f"\n--- {title} ---")
