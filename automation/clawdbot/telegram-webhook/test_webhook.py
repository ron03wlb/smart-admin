#!/usr/bin/env python3
"""
SmartAdmin Auto-Coding - Telegram Webhook 測試腳本
測試所有端點功能

作者: SmartAdmin Auto-Coding System
版本: 1.0.0
日期: 2026-01-27
"""

import requests
import json
import time

# ============================================================================
# 配置
# ============================================================================

BASE_URL = "http://localhost:8080"

# ============================================================================
# 測試案例
# ============================================================================

def test_health_check():
    """測試健康檢查端點"""
    print("\n" + "=" * 60)
    print("測試 1: /health - 健康檢查")
    print("=" * 60)

    try:
        response = requests.get(f"{BASE_URL}/health", timeout=5)
        print(f"Status Code: {response.status_code}")
        print(f"Response: {json.dumps(response.json(), indent=2)}")

        assert response.status_code == 200
        data = response.json()
        assert "status" in data
        assert "version" in data

        print("✓ 測試通過")
        return True

    except Exception as e:
        print(f"✗ 測試失敗: {e}")
        return False

def test_workflow_notification():
    """測試 Workflow 狀態通知"""
    print("\n" + "=" * 60)
    print("測試 2: /workflow - Workflow 狀態通知")
    print("=" * 60)

    payload = {
        "workflow_name": "test-analyzer-workflow",
        "status": "Succeeded",
        "message": "Test analysis completed successfully",
        "duration": "2m30s",
        "node_count": 3
    }

    try:
        response = requests.post(
            f"{BASE_URL}/workflow",
            json=payload,
            timeout=10
        )
        print(f"Status Code: {response.status_code}")
        print(f"Response: {json.dumps(response.json(), indent=2)}")

        assert response.status_code == 200
        data = response.json()
        assert data.get("status") == "sent"

        print("✓ 測試通過")
        return True

    except Exception as e:
        print(f"✗ 測試失敗: {e}")
        return False

def test_approval_request():
    """測試人工審批請求"""
    print("\n" + "=" * 60)
    print("測試 3: /approval - 人工審批請求")
    print("=" * 60)

    payload = {
        "type": "file_access",
        "file_path": "/project/foundation/Security.java",
        "agent": "java-architect",
        "reason": "Need to modify authentication mechanism",
        "operation": "WRITE"
    }

    try:
        response = requests.post(
            f"{BASE_URL}/approval",
            json=payload,
            timeout=10
        )
        print(f"Status Code: {response.status_code}")
        print(f"Response: {json.dumps(response.json(), indent=2)}")

        assert response.status_code == 200
        data = response.json()
        assert data.get("status") == "sent"
        assert "request_id" in data

        print("✓ 測試通過")
        return True

    except Exception as e:
        print(f"✗ 測試失敗: {e}")
        return False

def test_cost_alert():
    """測試成本告警"""
    print("\n" + "=" * 60)
    print("測試 4: /cost - 成本告警")
    print("=" * 60)

    payload = {
        "period": "monthly",
        "cost": 45.67,
        "threshold": 50.00,
        "breakdown": {
            "claude_api": 35.00,
            "infrastructure": 10.67
        }
    }

    try:
        response = requests.post(
            f"{BASE_URL}/cost",
            json=payload,
            timeout=10
        )
        print(f"Status Code: {response.status_code}")
        print(f"Response: {json.dumps(response.json(), indent=2)}")

        assert response.status_code == 200
        data = response.json()
        assert data.get("status") == "sent"

        print("✓ 測試通過")
        return True

    except Exception as e:
        print(f"✗ 測試失敗: {e}")
        return False

def test_prometheus_alert():
    """測試 Prometheus 告警"""
    print("\n" + "=" * 60)
    print("測試 5: /alert - Prometheus 告警")
    print("=" * 60)

    payload = {
        "status": "firing",
        "labels": {
            "alertname": "HighMemoryUsage",
            "severity": "warning"
        },
        "annotations": {
            "summary": "Memory usage is above 80%",
            "description": "Pod smartadmin-auto-coding is using 85% memory"
        }
    }

    try:
        response = requests.post(
            f"{BASE_URL}/alert",
            json=payload,
            timeout=10
        )
        print(f"Status Code: {response.status_code}")
        print(f"Response: {json.dumps(response.json(), indent=2)}")

        assert response.status_code == 200
        data = response.json()
        assert data.get("status") == "sent"

        print("✓ 測試通過")
        return True

    except Exception as e:
        print(f"✗ 測試失敗: {e}")
        return False

# ============================================================================
# 主測試函數
# ============================================================================

def main():
    """執行所有測試"""
    print("=" * 60)
    print("SmartAdmin Telegram Webhook 測試")
    print("Version: 1.0.0")
    print("=" * 60)

    print(f"\nBase URL: {BASE_URL}")
    print("請確保 Telegram Webhook 服務正在運行")
    print("並設置了正確的環境變量：")
    print("  - TELEGRAM_BOT_TOKEN")
    print("  - TELEGRAM_CHAT_ID")

    input("\n按 Enter 開始測試...")

    # 執行所有測試
    results = []

    results.append(("健康檢查", test_health_check()))
    time.sleep(1)

    results.append(("Workflow 通知", test_workflow_notification()))
    time.sleep(2)

    results.append(("審批請求", test_approval_request()))
    time.sleep(2)

    results.append(("成本告警", test_cost_alert()))
    time.sleep(2)

    results.append(("Prometheus 告警", test_prometheus_alert()))

    # 總結
    print("\n" + "=" * 60)
    print("測試總結")
    print("=" * 60)

    passed = sum(1 for _, result in results if result)
    failed = len(results) - passed

    for name, result in results:
        status = "✓ 通過" if result else "✗ 失敗"
        print(f"{name}: {status}")

    print(f"\n總計: {passed} 通過, {failed} 失敗")
    print("=" * 60)

    if failed > 0:
        exit(1)
    else:
        print("\n所有測試通過！")

if __name__ == '__main__':
    main()
