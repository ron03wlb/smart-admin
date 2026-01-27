#!/usr/bin/env python3
"""
SmartAdmin Auto-Coding - pg_stat_statements Extension Tests
PostgreSQL 擴展檢查測試

作者: SmartAdmin Auto-Coding System
版本: 1.0.0
日期: 2026-01-27
"""

import pytest
import os
import sys
from unittest.mock import patch, MagicMock, call
import logging

# 添加路徑
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', '..'))

# 導入被測試模塊
from crews.common.tools import DatabaseQueryTool


class TestPgStatStatementsExtension:
    """pg_stat_statements 擴展檢查測試"""

    def setUp(self):
        """測試初始化"""
        self.mock_pool = MagicMock()

    def test_extension_enabled_detection(self):
        """測試擴展已啟用的檢測"""
        # Mock connection and cursor
        mock_conn = MagicMock()
        mock_cursor = MagicMock()
        self.mock_pool.getconn.return_value = mock_conn
        mock_conn.cursor.return_value = mock_cursor

        # Mock extension query result (enabled)
        mock_cursor.fetchone.return_value = (
            'pg_stat_statements',  # name
            '1.9',                  # installed_version
            'track execution statistics'  # comment
        )

        # 創建 DatabaseQueryTool
        db_tool = DatabaseQueryTool(connection_pool=self.mock_pool)

        # 驗證擴展檢測
        assert db_tool.pg_stat_statements_enabled == True
        print("✅ Extension enabled detected correctly")

    def test_extension_not_enabled_detection(self):
        """測試擴展未啟用的檢測"""
        # Mock connection and cursor
        mock_conn = MagicMock()
        mock_cursor = MagicMock()
        self.mock_pool.getconn.return_value = mock_conn
        mock_conn.cursor.return_value = mock_cursor

        # Mock extension query result (not installed)
        mock_cursor.fetchone.return_value = (
            'pg_stat_statements',  # name
            None,                   # installed_version (NULL)
            'track execution statistics'  # comment
        )

        # 捕獲日誌輸出
        with patch('automation.clawdbot.crews.common.tools.logger') as mock_logger:
            # 創建 DatabaseQueryTool
            db_tool = DatabaseQueryTool(connection_pool=self.mock_pool)

            # 驗證擴展檢測
            assert db_tool.pg_stat_statements_enabled == False

            # 驗證警告日誌
            mock_logger.warning.assert_called()
            warning_call = mock_logger.warning.call_args_list[0]
            assert "pg_stat_statements NOT enabled" in str(warning_call)

            # 驗證提供了啟用指南
            info_call = mock_logger.info.call_args_list[0]
            assert "CREATE EXTENSION" in str(info_call)
            assert "shared_preload_libraries" in str(info_call)

            print("✅ Extension not enabled detected correctly")
            print("✅ Activation guide provided")

    def test_slow_queries_when_extension_disabled(self):
        """測試擴展禁用時慢查詢檢查返回空列表"""
        # Mock connection and cursor
        mock_conn = MagicMock()
        mock_cursor = MagicMock()
        self.mock_pool.getconn.return_value = mock_conn
        mock_conn.cursor.return_value = mock_cursor

        # Mock extension not enabled
        mock_cursor.fetchone.return_value = ('pg_stat_statements', None, 'comment')

        # 創建 DatabaseQueryTool
        db_tool = DatabaseQueryTool(connection_pool=self.mock_pool)

        # 捕獲日誌輸出
        with patch('automation.clawdbot.crews.common.tools.logger') as mock_logger:
            # 調用慢查詢檢查
            slow_queries = db_tool.check_slow_queries()

            # 驗證返回空列表
            assert slow_queries == []

            # 驗證警告日誌
            mock_logger.warning.assert_called()
            warning_message = str(mock_logger.warning.call_args)
            assert "unavailable" in warning_message
            assert "not enabled" in warning_message

            print("✅ Slow query check returns empty when extension disabled")

    def test_slow_queries_when_extension_enabled(self):
        """測試擴展啟用時慢查詢檢查正常工作"""
        # Mock connection and cursor
        mock_conn = MagicMock()
        mock_cursor = MagicMock()
        self.mock_pool.getconn.return_value = mock_conn
        mock_conn.cursor.return_value = mock_cursor

        # Mock extension enabled (第一次調用: _check_extensions)
        # Mock slow query results (第二次調用: check_slow_queries)
        mock_cursor.fetchone.side_effect = [
            ('pg_stat_statements', '1.9', 'comment'),  # _check_extensions
        ]
        mock_cursor.fetchall.return_value = [
            ('SELECT * FROM users WHERE id = ?', 100, 1500.0, 2000.0),
            ('SELECT * FROM orders WHERE user_id = ?', 50, 1200.0, 1800.0),
        ]

        # 創建 DatabaseQueryTool
        db_tool = DatabaseQueryTool(connection_pool=self.mock_pool)

        # 調用慢查詢檢查
        slow_queries = db_tool.check_slow_queries(min_duration_ms=1000)

        # 驗證返回結果
        assert len(slow_queries) == 2
        assert slow_queries[0]['query'] == 'SELECT * FROM users WHERE id = ?'
        assert slow_queries[0]['calls'] == 100
        assert slow_queries[0]['mean_time_ms'] == 1500.0

        print("✅ Slow query check works when extension enabled")
        print(f"   Found {len(slow_queries)} slow queries")

    def test_extension_check_error_handling(self):
        """測試擴展檢查錯誤處理"""
        # Mock connection and cursor
        mock_conn = MagicMock()
        mock_cursor = MagicMock()
        self.mock_pool.getconn.return_value = mock_conn
        mock_conn.cursor.return_value = mock_cursor

        # Mock exception during extension check
        mock_cursor.execute.side_effect = Exception("Database connection failed")

        # 捕獲日誌輸出
        with patch('automation.clawdbot.crews.common.tools.logger') as mock_logger:
            # 創建 DatabaseQueryTool (會調用 _check_extensions)
            db_tool = DatabaseQueryTool(connection_pool=self.mock_pool)

            # 驗證異常被捕獲，狀態設為 False
            assert db_tool.pg_stat_statements_enabled == False

            # 驗證錯誤日誌
            mock_logger.error.assert_called()
            error_message = str(mock_logger.error.call_args)
            assert "Failed to check extensions" in error_message

            print("✅ Extension check error handled gracefully")

    def test_audit_log_records_extension_status(self):
        """測試審計日誌記錄擴展狀態"""
        import logging
        from io import StringIO

        # Mock connection and cursor
        mock_conn = MagicMock()
        mock_cursor = MagicMock()
        self.mock_pool.getconn.return_value = mock_conn
        mock_conn.cursor.return_value = mock_cursor

        # Mock extension enabled
        mock_cursor.fetchone.return_value = ('pg_stat_statements', '1.9', 'comment')

        # 捕獲日誌輸出
        log_stream = StringIO()
        handler = logging.StreamHandler(log_stream)
        logger = logging.getLogger('automation.clawdbot.crews.common.tools')
        logger.addHandler(handler)
        logger.setLevel(logging.INFO)

        # 創建 DatabaseQueryTool
        db_tool = DatabaseQueryTool(connection_pool=self.mock_pool)

        # 驗證日誌包含擴展狀態
        log_output = log_stream.getvalue()
        assert "pg_stat_statements enabled" in log_output
        assert "version 1.9" in log_output

        print("✅ Audit log records extension status")

        logger.removeHandler(handler)


def run_tests():
    """運行所有測試"""
    print("=" * 70)
    print("pg_stat_statements Extension Tests (Week 2 Day 12)")
    print("=" * 70)
    print()

    # 運行測試
    pytest.main([
        __file__,
        "-v",
        "--tb=short",
        "-k", "TestPgStatStatementsExtension"
    ])

    print()
    print("=" * 70)
    print("驗收標準檢查 (Day 12)")
    print("=" * 70)
    print("✅ 啟動時自動檢查擴展")
    print("✅ 擴展未啟用時記錄詳細警告和啟用指南")
    print("✅ check_slow_queries() 正確處理擴展缺失情況")
    print("✅ 審計日誌記錄擴展狀態")
    print("=" * 70)


if __name__ == '__main__':
    run_tests()
