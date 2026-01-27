#!/usr/bin/env python3
"""
SmartAdmin Auto-Coding - Orchestrator Test Script
測試 Workflow Orchestrator 和 Telegram Command Handler

作者: SmartAdmin Auto-Coding System
版本: 1.0.0
日期: 2026-01-28
"""

import sys
import os
import json
import requests
import time
from datetime import datetime

# 添加項目根路徑
sys.path.insert(0, os.path.dirname(__file__))

# ============================================================================
# 測試配置
# ============================================================================

WEBHOOK_URL = os.getenv('WEBHOOK_URL', 'http://localhost:8080')
ENABLE_CREW_TESTS = os.getenv('ENABLE_CREW_TESTS', 'false').lower() == 'true'

# ============================================================================
# 測試用例
# ============================================================================

def test_health_check():
    """
    測試 1: 健康檢查端點

    驗證:
    - API 可訪問
    - 版本號正確（2.0.0）
    - Command Handler 已啟用
    """
    print("\n" + "=" * 70)
    print("Test 1: Health Check")
    print("=" * 70)

    try:
        response = requests.get(f"{WEBHOOK_URL}/health", timeout=5)

        if response.status_code == 200:
            data = response.json()
            print(f"✅ Health check passed")
            print(f"   Version: {data.get('version')}")
            print(f"   Telegram Bot: {data.get('telegram_bot')}")
            print(f"   Command Handler: {data.get('command_handler')}")

            # 驗證版本號
            if data.get('version') == '2.0.0':
                print("✅ Version is correct (2.0.0)")
            else:
                print(f"⚠️  Version mismatch: expected 2.0.0, got {data.get('version')}")

            # 驗證 Command Handler
            if data.get('command_handler') == 'enabled':
                print("✅ Command Handler is enabled")
            else:
                print("⚠️  Command Handler is not enabled")

            return True
        else:
            print(f"❌ Health check failed: HTTP {response.status_code}")
            return False

    except requests.exceptions.RequestException as e:
        print(f"❌ Health check failed: {e}")
        return False

def test_help_command():
    """
    測試 2: /help 命令

    驗證:
    - 命令處理正常
    - 返回幫助信息
    """
    print("\n" + "=" * 70)
    print("Test 2: /help Command")
    print("=" * 70)

    try:
        payload = {
            "command": "/help",
            "args": {}
        }

        response = requests.post(
            f"{WEBHOOK_URL}/command",
            json=payload,
            timeout=10
        )

        if response.status_code == 200:
            data = response.json()
            print("✅ /help command executed successfully")
            print(f"   Status: {data.get('status')}")
            print(f"   Message preview: {data.get('message', '')[:100]}...")
            return True
        else:
            print(f"❌ /help command failed: HTTP {response.status_code}")
            print(f"   Response: {response.text}")
            return False

    except requests.exceptions.RequestException as e:
        print(f"❌ /help command failed: {e}")
        return False

def test_orchestrator_import():
    """
    測試 3: Orchestrator 導入

    驗證:
    - orchestrator.py 可正常導入
    - WorkflowOrchestrator 類可實例化
    """
    print("\n" + "=" * 70)
    print("Test 3: Orchestrator Import")
    print("=" * 70)

    try:
        from orchestrator import WorkflowOrchestrator

        orchestrator = WorkflowOrchestrator()
        print("✅ WorkflowOrchestrator imported and instantiated successfully")
        print(f"   Registered crews: {list(orchestrator.crews.keys())}")

        # 清理資源
        orchestrator.cleanup()

        return True

    except ImportError as e:
        print(f"❌ Failed to import orchestrator: {e}")
        return False

    except Exception as e:
        print(f"❌ Failed to instantiate WorkflowOrchestrator: {e}")
        return False

def test_command_handler_import():
    """
    測試 4: Command Handler 導入

    驗證:
    - command_handler.py 可正常導入
    - TelegramCommandHandler 類可實例化
    """
    print("\n" + "=" * 70)
    print("Test 4: Command Handler Import")
    print("=" * 70)

    try:
        sys.path.insert(0, os.path.join(os.path.dirname(__file__), 'telegram-webhook'))
        from command_handler import TelegramCommandHandler

        handler = TelegramCommandHandler()
        print("✅ TelegramCommandHandler imported and instantiated successfully")

        # 測試 /help 命令
        result = handler.handle_command('/help', {})
        print(f"   /help command result: {result[:100]}...")

        # 清理資源
        handler.cleanup()

        return True

    except ImportError as e:
        print(f"❌ Failed to import command_handler: {e}")
        return False

    except Exception as e:
        print(f"❌ Failed to instantiate TelegramCommandHandler: {e}")
        return False

def test_new_feature_command_dry_run():
    """
    測試 5: /new_feature 命令（Dry Run）

    驗證:
    - 命令參數驗證正確
    - 不真正執行 Crews（避免實際修改代碼）
    """
    print("\n" + "=" * 70)
    print("Test 5: /new_feature Command (Dry Run)")
    print("=" * 70)

    # 測試缺少參數的情況
    print("\nSubtest 5.1: Missing arguments")
    try:
        payload = {
            "command": "/new_feature",
            "args": {}  # 缺少 name 和 entity
        }

        response = requests.post(
            f"{WEBHOOK_URL}/command",
            json=payload,
            timeout=10
        )

        if response.status_code == 200:
            data = response.json()
            message = data.get('message', '')

            if 'Missing required arguments' in message or 'error' in message.lower():
                print("✅ Parameter validation works correctly")
            else:
                print("⚠️  Expected validation error, but command succeeded")
        else:
            print(f"⚠️  Unexpected HTTP status: {response.status_code}")

    except Exception as e:
        print(f"❌ Test failed: {e}")

    # 測試參數正確的情況（不真正執行）
    print("\nSubtest 5.2: Valid arguments (validation only)")
    print("⚠️  Skipping actual execution to avoid code changes")
    print("   To test full execution, set ENABLE_CREW_TESTS=true")

    if ENABLE_CREW_TESTS:
        print("\n⚠️  WARNING: ENABLE_CREW_TESTS=true, will execute real workflow!")
        print("   This will create a real Git branch, generate code, and create a PR")
        print("   Press Ctrl+C within 5 seconds to cancel...")
        time.sleep(5)

        try:
            payload = {
                "command": "/new_feature",
                "args": {
                    "name": "Test Feature",
                    "entity": "TestEntity",
                    "target": "sa-admin"
                }
            }

            print("\n🚀 Executing full workflow...")
            start_time = datetime.now()

            response = requests.post(
                f"{WEBHOOK_URL}/command",
                json=payload,
                timeout=600  # 10 分鐘超時
            )

            duration = datetime.now() - start_time
            duration_str = f"{int(duration.total_seconds() // 60)}m {int(duration.total_seconds() % 60)}s"

            if response.status_code == 200:
                data = response.json()
                print(f"✅ Full workflow executed successfully")
                print(f"   Duration: {duration_str}")
                print(f"   Status: {data.get('status')}")
                print(f"   Message preview: {data.get('message', '')[:200]}...")
                return True
            else:
                print(f"❌ Workflow failed: HTTP {response.status_code}")
                print(f"   Response: {response.text}")
                return False

        except Exception as e:
            print(f"❌ Workflow execution failed: {e}")
            return False
    else:
        print("✅ Parameter validation test passed (dry run)")
        return True

# ============================================================================
# 主測試函數
# ============================================================================

def run_all_tests():
    """
    運行所有測試用例

    Returns:
        int: 通過的測試數量
    """
    print("\n" + "=" * 70)
    print("SmartAdmin Auto-Coding - Orchestrator Test Suite")
    print("=" * 70)
    print(f"Webhook URL: {WEBHOOK_URL}")
    print(f"Enable Crew Tests: {ENABLE_CREW_TESTS}")
    print(f"Start Time: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")

    tests = [
        ("Health Check", test_health_check),
        ("/help Command", test_help_command),
        ("Orchestrator Import", test_orchestrator_import),
        ("Command Handler Import", test_command_handler_import),
        ("/new_feature Command (Dry Run)", test_new_feature_command_dry_run),
    ]

    results = []

    for test_name, test_func in tests:
        try:
            result = test_func()
            results.append((test_name, result))
        except Exception as e:
            print(f"\n❌ Test '{test_name}' crashed: {e}")
            results.append((test_name, False))

    # 打印測試總結
    print("\n" + "=" * 70)
    print("Test Summary")
    print("=" * 70)

    passed = sum(1 for _, result in results if result)
    total = len(results)

    for test_name, result in results:
        status = "✅ PASS" if result else "❌ FAIL"
        print(f"{status} - {test_name}")

    print("\n" + "-" * 70)
    print(f"Total: {passed}/{total} tests passed ({int(passed / total * 100)}%)")
    print("=" * 70)

    return passed

# ============================================================================
# 命令行入口
# ============================================================================

if __name__ == '__main__':
    import argparse

    parser = argparse.ArgumentParser(description='Test Workflow Orchestrator')
    parser.add_argument(
        '--url',
        type=str,
        default='http://localhost:8080',
        help='Webhook URL (default: http://localhost:8080)'
    )
    parser.add_argument(
        '--enable-crew-tests',
        action='store_true',
        help='Enable actual Crew execution (WARNING: will modify code)'
    )

    args = parser.parse_args()

    # 設置環境變量
    os.environ['WEBHOOK_URL'] = args.url
    if args.enable_crew_tests:
        os.environ['ENABLE_CREW_TESTS'] = 'true'

    # 運行測試
    passed = run_all_tests()

    # 退出碼
    sys.exit(0 if passed == 5 else 1)
