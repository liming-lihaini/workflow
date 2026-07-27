"""
一键执行全部测试脚本
功能：
1. 准备测试数据（调用 prepare_data.py）
2. 逐模块执行 pytest
3. 每个模块独立生成 HTML 报告 + 截图目录
4. 汇总输出测试结果
"""
import os
import sys
import subprocess
import time
import re
from datetime import datetime

# 常量
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
ADMIN_DIR = os.path.dirname(SCRIPT_DIR)  # docs/test/admin/

MODULES = [
    ("test_tc01_auth.py", "TC01-认证管理"),
    ("test_tc02_dept.py", "TC02-部门管理"),
    ("test_tc03_user.py", "TC03-用户管理"),
    ("test_tc04_role.py", "TC04-角色管理"),
    ("test_tc05_dict.py", "TC05-数据字典"),
    ("test_tc06_log.py", "TC06-日志管理"),
    ("test_tc07_admin.py", "TC07-三员管理"),
]


def run_command(cmd, cwd=None):
    """执行命令并返回结果"""
    print(f"  > {' '.join(cmd)}")
    result = subprocess.run(
        cmd,
        cwd=cwd or SCRIPT_DIR,
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace"
    )
    return result


def prepare_data():
    """准备测试数据"""
    print("=" * 60)
    print("Step 1: 准备测试数据")
    print("=" * 60)
    prepare_script = os.path.join(SCRIPT_DIR, "prepare_data.py")
    if os.path.exists(prepare_script):
        result = run_command([sys.executable, prepare_script])
        if result.returncode == 0:
            print("  [OK] 测试数据准备完成")
        else:
            print(f"  [WARN] 测试数据准备返回非零: {result.returncode}")
            if result.stderr:
                print(f"  stderr: {result.stderr[:500]}")
    else:
        print("  [SKIP] prepare_data.py 不存在，跳过数据准备")
    print()


def parse_pytest_result(output):
    """从 pytest 输出中解析测试结果"""
    # 匹配 "X passed, Y failed, Z error, W skipped" 等
    passed = failed = errors = skipped = 0
    for line in output.split("\n"):
        # 匹配 summary 行
        if "passed" in line or "failed" in line or "error" in line:
            m = re.search(r"(\d+) passed", line)
            if m:
                passed = int(m.group(1))
            m = re.search(r"(\d+) failed", line)
            if m:
                failed = int(m.group(1))
            m = re.search(r"(\d+) error", line)
            if m:
                errors = int(m.group(1))
            m = re.search(r"(\d+) skipped", line)
            if m:
                skipped = int(m.group(1))
    return passed, failed, errors, skipped


def run_module(test_file, module_dir):
    """执行单个模块的测试"""
    module_path = os.path.join(SCRIPT_DIR, test_file)
    report_path = os.path.join(module_dir, "report.html")
    screenshot_dir = os.path.join(module_dir, "screenshots")
    os.makedirs(screenshot_dir, exist_ok=True)

    cmd = [
        sys.executable, "-m", "pytest",
        module_path,
        f"--html={report_path}",
        "--self-contained-html",
        "-v",
        "--tb=short",
        "--no-header",
    ]

    result = run_command(cmd, cwd=SCRIPT_DIR)
    output = result.stdout + result.stderr
    passed, failed, errors, skipped = parse_pytest_result(output)

    return {
        "passed": passed,
        "failed": failed,
        "errors": errors,
        "skipped": skipped,
        "returncode": result.returncode,
        "report": report_path,
    }


def main():
    start_time = time.time()
    timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")

    print(f"\n{'=' * 60}")
    print(f"系统管理模块自动化测试 - {timestamp}")
    print(f"{'=' * 60}\n")

    # Step 1: 准备数据
    prepare_data()

    # Step 2: 逐模块执行
    print("=" * 60)
    print("Step 2: 逐模块执行测试")
    print("=" * 60)

    results = {}
    total_passed = 0
    total_failed = 0
    total_errors = 0
    total_skipped = 0

    for test_file, module_name in MODULES:
        module_dir = os.path.join(ADMIN_DIR, module_name)
        print(f"\n--- [{module_name}] ---")

        result = run_module(test_file, module_dir)
        results[module_name] = result

        total_passed += result["passed"]
        total_failed += result["failed"]
        total_errors += result["errors"]
        total_skipped += result["skipped"]

        status = "PASS" if result["returncode"] == 0 else "FAIL"
        print(f"  [{status}] passed={result['passed']} failed={result['failed']} "
              f"errors={result['errors']} skipped={result['skipped']}")
        print(f"  报告: {result['report']}")

    # Step 3: 汇总
    elapsed = time.time() - start_time
    total = total_passed + total_failed + total_errors + total_skipped

    print(f"\n{'=' * 60}")
    print("测试汇总")
    print(f"{'=' * 60}")
    print(f"  总用例数: {total}")
    print(f"  通过:     {total_passed}")
    print(f"  失败:     {total_failed}")
    print(f"  错误:     {total_errors}")
    print(f"  跳过:     {total_skipped}")
    print(f"  耗时:     {elapsed:.1f}s")
    if total > 0:
        rate = total_passed / total * 100
        print(f"  通过率:   {rate:.1f}%")
    print()

    print("各模块结果:")
    for module_name, result in results.items():
        status_icon = "✓" if result["returncode"] == 0 else "✗"
        print(f"  {status_icon} {module_name}: "
              f"passed={result['passed']} failed={result['failed']} errors={result['errors']}")

    print(f"\n截图目录:")
    for _, module_name in MODULES:
        ss_dir = os.path.join(ADMIN_DIR, module_name, "screenshots")
        if os.path.exists(ss_dir):
            count = len([f for f in os.listdir(ss_dir) if f.endswith(".png")])
            print(f"  {module_name}: {count} 张截图 -> {ss_dir}")

    print(f"\nHTML 报告:")
    for _, module_name in MODULES:
        report = os.path.join(ADMIN_DIR, module_name, "report.html")
        if os.path.exists(report):
            print(f"  {module_name}: {report}")

    print(f"\n{'=' * 60}")
    print("执行完毕!")
    print(f"{'=' * 60}")

    # 返回退出码
    if total_failed > 0 or total_errors > 0:
        sys.exit(1)
    sys.exit(0)


if __name__ == "__main__":
    main()
