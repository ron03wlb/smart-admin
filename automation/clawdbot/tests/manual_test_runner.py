#!/usr/bin/env python3
"""
SmartAdmin Auto-Coding - Manual Test Runner
手動測試運行器（無需 pytest）

作者: SmartAdmin Auto-Coding System
版本: 1.0.0
日期: 2026-01-27
"""

import os
import sys
from pathlib import Path

# 添加路徑
project_root = os.path.join(os.path.dirname(__file__), '..', '..', '..')
sys.path.insert(0, project_root)
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', '..'))

def test_path_traversal_security():
    """測試路徑遍歷安全"""
    print("\n" + "=" * 70)
    print("測試 1: 路徑遍歷安全防護")
    print("=" * 70)

    from automation.clawdbot.tools.file_access_guard import normalize_path, SecurityError

    os.environ['PROJECT_ROOT'] = '/tmp/test-project'

    # 測試攻擊向量
    attack_vectors = [
        "/tmp/test-project/../../../etc/passwd",
        "../../secret/config.yml",
        "/tmp/test-project/../forbidden/file.txt",
        "../../../../../root/.ssh/id_rsa",
    ]

    blocked_count = 0
    for malicious_path in attack_vectors:
        try:
            normalize_path(malicious_path)
            print(f"❌ FAILED: {malicious_path} was NOT blocked!")
            return False
        except SecurityError:
            blocked_count += 1
            print(f"✅ Blocked: {malicious_path}")

    print(f"\n✅ SUCCESS: {blocked_count}/{len(attack_vectors)} 攻擊被成功阻止")

    # 測試允許的路徑
    print("\n測試允許的路徑...")
    os.environ['PROJECT_ROOT'] = os.getcwd()
    PROJECT_ROOT = Path(os.getcwd()).resolve()

    allowed_paths = [
        "automation/clawdbot/crews/analyzer_crew.py",
        str(PROJECT_ROOT / "README.md"),
    ]

    allowed_count = 0
    for allowed_path in allowed_paths:
        try:
            result = normalize_path(allowed_path)
            if str(PROJECT_ROOT) in result:
                allowed_count += 1
                print(f"✅ Allowed: {allowed_path}")
            else:
                print(f"❌ FAILED: {allowed_path} - result not in PROJECT_ROOT")
                return False
        except SecurityError as e:
            print(f"❌ FAILED: {allowed_path} was incorrectly blocked: {e}")
            return False

    print(f"\n✅ SUCCESS: {allowed_count}/{len(allowed_paths)} 允許的路徑正常通過")
    return True

def test_connection_pool_context_manager():
    """測試連接池 Context Manager"""
    print("\n" + "=" * 70)
    print("測試 2: 連接池 Context Manager")
    print("=" * 70)

    from unittest.mock import MagicMock, patch

    # Mock connection pool
    mock_pool = MagicMock()
    mock_conn = MagicMock()
    mock_pool.getconn.return_value = mock_conn

    # 測試 Context Manager 正常流程
    print("\n測試正常情況...")
    with patch.dict(os.environ, {
        'DB_CONNECTION_STRING': 'postgresql://test:test@localhost:5432/test_db',
        'PROJECT_ROOT': '/tmp/test-project'
    }):
        from automation.clawdbot.crews.common.base_crew import BaseCrew

        class TestCrew(BaseCrew):
            def create_crew(self):
                pass

            def run(self, **kwargs):
                return {"status": "success"}

        with patch('psycopg2.pool.ThreadedConnectionPool'):
            crew = TestCrew(crew_name="test-crew", crew_type="test")
            crew.connection_pool = mock_pool

            # 使用 context manager
            with crew.get_db_connection() as conn:
                assert conn == mock_conn
                print("✅ 連接通過 Context Manager 獲取成功")

            # 驗證連接被歸還
            mock_pool.putconn.assert_called_once_with(mock_conn)
            mock_conn.commit.assert_called_once()
            print("✅ 連接在使用後被正確歸還")

    # 測試異常情況
    print("\n測試異常情況...")
    mock_pool.reset_mock()
    mock_conn.reset_mock()

    try:
        with crew.get_db_connection() as conn:
            raise ValueError("測試異常")
    except ValueError:
        pass

    # 驗證連接回滾
    mock_conn.rollback.assert_called_once()
    mock_pool.putconn.assert_called_once_with(mock_conn)
    print("✅ 異常時連接被回滾並歸還")

    print("\n✅ SUCCESS: Context Manager 測試通過")
    return True

def test_retry_mechanism():
    """測試重試機制"""
    print("\n" + "=" * 70)
    print("測試 3: 重試機制")
    print("=" * 70)

    from unittest.mock import patch, MagicMock
    import requests

    with patch.dict(os.environ, {
        'DB_CONNECTION_STRING': 'postgresql://test:test@localhost:5432/test_db',
        'PROJECT_ROOT': '/tmp/test-project'
    }):
        from automation.clawdbot.crews.common.base_crew import BaseCrew, RetryableError, FatalError

        # 測試可重試錯誤
        print("\n測試 RetryableError 自動重試...")
        class RetryTestCrew(BaseCrew):
            def __init__(self, *args, **kwargs):
                super().__init__(*args, **kwargs)
                self.attempt_count = 0

            def create_crew(self):
                pass

            def run(self, **kwargs):
                self.attempt_count += 1
                if self.attempt_count < 3:
                    raise requests.exceptions.RequestException("網絡錯誤")
                return {"status": "success", "attempts": self.attempt_count}

        with patch('psycopg2.pool.ThreadedConnectionPool'):
            crew = RetryTestCrew(crew_name="retry-test", crew_type="test")
            result = crew.run_with_retry()

            assert crew.attempt_count == 3
            assert result['status'] == 'success'
            print(f"✅ 重試了 {crew.attempt_count} 次後成功")

        # 測試不可重試錯誤
        print("\n測試 FatalError 不重試...")
        class FatalTestCrew(BaseCrew):
            def __init__(self, *args, **kwargs):
                super().__init__(*args, **kwargs)
                self.attempt_count = 0

            def create_crew(self):
                pass

            def run(self, **kwargs):
                self.attempt_count += 1
                raise ValueError("配置錯誤")

        with patch('psycopg2.pool.ThreadedConnectionPool'):
            crew = FatalTestCrew(crew_name="fatal-test", crew_type="test")

            try:
                crew.run_with_retry()
                print("❌ FAILED: FatalError 沒有被拋出")
                return False
            except FatalError:
                assert crew.attempt_count == 1
                print(f"✅ FatalError 立即失敗，只嘗試了 1 次")

    print("\n✅ SUCCESS: 重試機制測試通過")
    return True

def main():
    """運行所有測試"""
    print("\n" + "=" * 70)
    print("SmartAdmin Auto-Coding - 核心功能測試")
    print("Manual Test Runner (不依賴 pytest)")
    print("=" * 70)

    tests = [
        ("路徑遍歷安全", test_path_traversal_security),
        ("連接池 Context Manager", test_connection_pool_context_manager),
        ("重試機制", test_retry_mechanism),
    ]

    results = []
    for name, test_func in tests:
        try:
            success = test_func()
            results.append((name, success))
        except Exception as e:
            print(f"\n❌ 測試失敗: {name}")
            print(f"   錯誤: {e}")
            import traceback
            traceback.print_exc()
            results.append((name, False))

    # 打印總結
    print("\n" + "=" * 70)
    print("測試總結")
    print("=" * 70)

    passed = sum(1 for _, success in results if success)
    total = len(results)

    for name, success in results:
        status = "✅ PASS" if success else "❌ FAIL"
        print(f"{status}: {name}")

    print(f"\n總計: {passed}/{total} 測試通過 ({passed*100//total}%)")

    if passed == total:
        print("\n🎉 所有測試通過！")
        return 0
    else:
        print(f"\n⚠️ {total - passed} 個測試失敗")
        return 1

if __name__ == '__main__':
    sys.exit(main())
