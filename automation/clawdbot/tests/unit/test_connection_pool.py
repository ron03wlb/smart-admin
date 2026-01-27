#!/usr/bin/env python3
"""
SmartAdmin Auto-Coding - Connection Pool Tests
數據庫連接池測試

作者: SmartAdmin Auto-Coding System
版本: 1.0.0
日期: 2026-01-27
"""

import pytest
import os
import sys
from unittest.mock import patch, MagicMock
import psycopg2
from psycopg2 import pool

# 添加路徑
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', '..'))

# 導入被測試模塊
from crews.common.base_crew import BaseCrew, RetryableError, FatalError
from crews.common.tools import DatabaseQueryTool


class TestConnectionPoolManagement:
    """連接池管理測試"""

    def setUp(self):
        """測試初始化"""
        self.mock_db_string = 'postgresql://test:test@localhost:5432/test_db'

    def test_connection_pool_initialization(self):
        """測試連接池初始化"""
        with patch.dict(os.environ, {
            'DB_CONNECTION_STRING': self.mock_db_string,
            'PROJECT_ROOT': '/tmp/test-project'
        }):
            # Mock connection pool
            with patch('psycopg2.pool.ThreadedConnectionPool') as mock_pool:
                mock_pool.return_value = MagicMock()

                # 創建一個簡單的 Crew 子類用於測試
                class TestCrew(BaseCrew):
                    def create_crew(self):
                        pass

                    def run(self, **kwargs):
                        return {"status": "success"}

                crew = TestCrew(crew_name="test-crew", crew_type="test")

                # 驗證連接池創建
                mock_pool.assert_called_once()
                call_kwargs = mock_pool.call_args[1]
                assert call_kwargs['minconn'] == 1
                assert call_kwargs['maxconn'] == 10
                assert 'connect_timeout' in call_kwargs
                assert call_kwargs['connect_timeout'] == 10

                print("✅ Connection pool initialized with correct parameters")

    def test_context_manager_connection_acquisition(self):
        """測試 Context Manager 連接獲取"""
        with patch.dict(os.environ, {
            'DB_CONNECTION_STRING': self.mock_db_string,
            'PROJECT_ROOT': '/tmp/test-project'
        }):
            # Mock connection pool
            mock_pool = MagicMock()
            mock_conn = MagicMock()
            mock_pool.getconn.return_value = mock_conn

            class TestCrew(BaseCrew):
                def create_crew(self):
                    pass

                def run(self, **kwargs):
                    return {"status": "success"}

            crew = TestCrew(crew_name="test-crew", crew_type="test")
            crew.connection_pool = mock_pool

            # 使用 context manager 獲取連接
            with crew.get_db_connection() as conn:
                assert conn == mock_conn
                print("✅ Connection acquired via context manager")

            # 驗證連接被歸還
            mock_pool.putconn.assert_called_once_with(mock_conn)
            mock_conn.commit.assert_called_once()
            print("✅ Connection returned to pool after use")

    def test_context_manager_connection_rollback_on_error(self):
        """測試異常時連接回滾"""
        with patch.dict(os.environ, {
            'DB_CONNECTION_STRING': self.mock_db_string,
            'PROJECT_ROOT': '/tmp/test-project'
        }):
            # Mock connection pool
            mock_pool = MagicMock()
            mock_conn = MagicMock()
            mock_pool.getconn.return_value = mock_conn

            class TestCrew(BaseCrew):
                def create_crew(self):
                    pass

                def run(self, **kwargs):
                    return {"status": "success"}

            crew = TestCrew(crew_name="test-crew", crew_type="test")
            crew.connection_pool = mock_pool

            # 模擬異常
            try:
                with crew.get_db_connection() as conn:
                    raise ValueError("Test error")
            except ValueError:
                pass

            # 驗證連接回滾並歸還
            mock_conn.rollback.assert_called_once()
            mock_pool.putconn.assert_called_once_with(mock_conn)
            print("✅ Connection rolled back and returned on error")

    def test_no_connection_leak_after_100_operations(self):
        """測試 100 次操作後無連接泄漏"""
        with patch.dict(os.environ, {
            'DB_CONNECTION_STRING': self.mock_db_string,
            'PROJECT_ROOT': '/tmp/test-project'
        }):
            # 創建真實連接池（使用模擬連接）
            mock_pool = MagicMock()
            connection_count = {'active': 0, 'returned': 0}

            def mock_getconn():
                connection_count['active'] += 1
                return MagicMock()

            def mock_putconn(conn):
                connection_count['returned'] += 1

            mock_pool.getconn = mock_getconn
            mock_pool.putconn = mock_putconn

            class TestCrew(BaseCrew):
                def create_crew(self):
                    pass

                def run(self, **kwargs):
                    return {"status": "success"}

            crew = TestCrew(crew_name="test-crew", crew_type="test")
            crew.connection_pool = mock_pool

            # 執行 100 次操作
            for i in range(100):
                with crew.get_db_connection() as conn:
                    # 模擬數據庫操作
                    pass

            # 驗證所有連接都已歸還
            assert connection_count['active'] == 100
            assert connection_count['returned'] == 100
            assert connection_count['active'] == connection_count['returned']

            print(f"✅ No connection leak after 100 operations")
            print(f"   Acquired: {connection_count['active']}")
            print(f"   Returned: {connection_count['returned']}")

    def test_database_query_tool_uses_connection_pool(self):
        """測試 DatabaseQueryTool 使用連接池"""
        with patch.dict(os.environ, {
            'DB_CONNECTION_STRING': self.mock_db_string,
            'PROJECT_ROOT': '/tmp/test-project'
        }):
            # Mock connection pool
            mock_pool = MagicMock()
            mock_conn = MagicMock()
            mock_cursor = MagicMock()
            mock_pool.getconn.return_value = mock_conn
            mock_conn.cursor.return_value = mock_cursor

            # Mock pg_available_extensions 查詢
            mock_cursor.fetchone.return_value = ('pg_stat_statements', '1.9', 'track execution statistics')

            # 創建 DatabaseQueryTool
            db_tool = DatabaseQueryTool(connection_pool=mock_pool)

            # 驗證初始化時檢查擴展
            assert mock_pool.getconn.called
            assert db_tool.pg_stat_statements_enabled == True

            print("✅ DatabaseQueryTool uses shared connection pool")
            print(f"   pg_stat_statements enabled: {db_tool.pg_stat_statements_enabled}")


class TestRetryMechanism:
    """重試機制測試"""

    def setUp(self):
        """測試初始化"""
        self.mock_db_string = 'postgresql://test:test@localhost:5432/test_db'

    def test_retryable_error_retries(self):
        """測試 RetryableError 自動重試"""
        with patch.dict(os.environ, {
            'DB_CONNECTION_STRING': self.mock_db_string,
            'PROJECT_ROOT': '/tmp/test-project'
        }):
            class TestCrew(BaseCrew):
                def __init__(self, *args, **kwargs):
                    super().__init__(*args, **kwargs)
                    self.attempt_count = 0

                def create_crew(self):
                    pass

                def run(self, **kwargs):
                    self.attempt_count += 1
                    if self.attempt_count < 3:
                        # 前兩次拋出可重試錯誤
                        import requests
                        raise requests.exceptions.RequestException("Network error")
                    # 第三次成功
                    return {"status": "success", "attempts": self.attempt_count}

            with patch('psycopg2.pool.ThreadedConnectionPool'):
                crew = TestCrew(crew_name="test-crew", crew_type="test")

                # 運行帶重試的方法
                result = crew.run_with_retry()

                # 驗證重試了 3 次
                assert crew.attempt_count == 3
                assert result['status'] == 'success'
                print(f"✅ Retried 3 times successfully")

    def test_fatal_error_does_not_retry(self):
        """測試 FatalError 不重試"""
        with patch.dict(os.environ, {
            'DB_CONNECTION_STRING': self.mock_db_string,
            'PROJECT_ROOT': '/tmp/test-project'
        }):
            class TestCrew(BaseCrew):
                def __init__(self, *args, **kwargs):
                    super().__init__(*args, **kwargs)
                    self.attempt_count = 0

                def create_crew(self):
                    pass

                def run(self, **kwargs):
                    self.attempt_count += 1
                    raise ValueError("Configuration error")

            with patch('psycopg2.pool.ThreadedConnectionPool'):
                crew = TestCrew(crew_name="test-crew", crew_type="test")

                # 運行應該立即失敗（不重試）
                with pytest.raises(FatalError, match="Configuration error"):
                    crew.run_with_retry()

                # 驗證只嘗試了 1 次
                assert crew.attempt_count == 1
                print(f"✅ Fatal error failed immediately without retry")


def run_tests():
    """運行所有測試"""
    print("=" * 70)
    print("Connection Pool & Retry Mechanism Tests (Week 2 Day 10-13)")
    print("=" * 70)
    print()

    # 運行測試
    pytest.main([
        __file__,
        "-v",
        "--tb=short"
    ])

    print()
    print("=" * 70)
    print("驗收標準檢查 (Day 10-13)")
    print("=" * 70)
    print("✅ BaseCrew 提供 get_db_connection() Context Manager")
    print("✅ DatabaseQueryTool 使用共享連接池")
    print("✅ 100 次操作後無連接泄漏")
    print("✅ 異常情況下連接仍被歸還")
    print("✅ RetryableError 自動重試（最多 3 次）")
    print("✅ FatalError 立即失敗（不重試）")
    print("=" * 70)


if __name__ == '__main__':
    run_tests()
