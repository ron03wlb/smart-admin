#!/usr/bin/env python3
"""
SmartAdmin Auto-Coding - Core Integration Tests (No CrewAI dependency)
核心集成測試（不依賴 CrewAI）

作者: SmartAdmin Auto-Coding System
版本: 1.0.0
日期: 2026-01-27
"""

import os
import sys
import pytest
from unittest.mock import patch, MagicMock
from pathlib import Path

# 添加路徑
project_root = os.path.join(os.path.dirname(__file__), '..', '..', '..')
sys.path.insert(0, project_root)

# 設置測試環境變量
os.environ['PROJECT_ROOT'] = project_root
os.environ['DB_CONNECTION_STRING'] = os.getenv(
    'TEST_DB_CONNECTION_STRING',
    'postgresql://postgres:postgres@localhost:5432/test_db'
)
os.environ['CLAUDE_API_KEY'] = os.getenv('CLAUDE_API_KEY', 'test-key')
os.environ['TELEGRAM_BOT_TOKEN'] = 'test-token'
os.environ['TELEGRAM_CHAT_ID'] = '12345'


class TestPathTraversalIntegration:
    """路徑遍歷安全集成測試"""

    def test_security_error_raised(self):
        """測試 SecurityError 被正確拋出"""
        from automation.clawdbot.tools.file_access_guard import normalize_path, SecurityError

        os.environ['PROJECT_ROOT'] = '/tmp/test-project'

        with pytest.raises(SecurityError) as exc_info:
            normalize_path("../../etc/passwd")

        assert "Path traversal detected" in str(exc_info.value)
        print("✅ SecurityError correctly raised for path traversal")

    def test_allowed_path_normalization(self):
        """測試允許路徑的規範化"""
        from automation.clawdbot.tools.file_access_guard import normalize_path

        os.environ['PROJECT_ROOT'] = os.getcwd()
        PROJECT_ROOT = Path(os.getcwd()).resolve()

        result = normalize_path("README.md")

        assert str(PROJECT_ROOT) in result
        assert result.endswith("README.md")
        print(f"✅ Allowed path normalized: {result}")


class TestConnectionPoolIntegration:
    """連接池集成測試"""

    def test_base_crew_connection_pool(self):
        """測試 BaseCrew 連接池"""
        from automation.clawdbot.crews.common.base_crew import BaseCrew

        class TestCrew(BaseCrew):
            def create_crew(self):
                pass

            def run(self, **kwargs):
                return {"status": "success"}

        with patch('psycopg2.pool.ThreadedConnectionPool') as mock_pool:
            mock_pool_instance = MagicMock()
            mock_pool.return_value = mock_pool_instance

            crew = TestCrew(crew_name="pool-test", crew_type="test")

            # 驗證連接池創建
            assert crew.connection_pool is not None
            mock_pool.assert_called_once()

            # 檢查連接池參數
            call_kwargs = mock_pool.call_args[1]
            assert call_kwargs['minconn'] == 1
            assert call_kwargs['maxconn'] == 10
            print("✅ Connection pool initialized with correct parameters")

    def test_context_manager_integration(self):
        """測試 Context Manager 集成"""
        from automation.clawdbot.crews.common.base_crew import BaseCrew

        class TestCrew(BaseCrew):
            def create_crew(self):
                pass

            def run(self, **kwargs):
                return {"status": "success"}

        with patch('psycopg2.pool.ThreadedConnectionPool'):
            crew = TestCrew(crew_name="cm-test", crew_type="test")

            mock_pool = MagicMock()
            mock_conn = MagicMock()
            mock_pool.getconn.return_value = mock_conn

            crew.connection_pool = mock_pool

            # 測試正常流程
            with crew.get_db_connection() as conn:
                assert conn == mock_conn

            mock_pool.putconn.assert_called_once_with(mock_conn)
            mock_conn.commit.assert_called_once()
            print("✅ Context manager correctly manages connection lifecycle")

            # 測試異常流程
            mock_pool.reset_mock()
            mock_conn.reset_mock()

            try:
                with crew.get_db_connection() as conn:
                    raise ValueError("Test exception")
            except ValueError:
                pass

            mock_conn.rollback.assert_called_once()
            mock_pool.putconn.assert_called_once_with(mock_conn)
            print("✅ Context manager correctly handles exceptions")


class TestRetryMechanismIntegration:
    """重試機制集成測試"""

    def test_retryable_error_integration(self):
        """測試 RetryableError 集成"""
        from automation.clawdbot.crews.common.base_crew import BaseCrew, RetryableError
        import requests

        class RetryTestCrew(BaseCrew):
            def __init__(self, *args, **kwargs):
                super().__init__(*args, **kwargs)
                self.attempts = 0

            def create_crew(self):
                pass

            def run(self, **kwargs):
                self.attempts += 1
                if self.attempts < 2:
                    raise requests.exceptions.RequestException("Network error")
                return {"status": "success", "attempts": self.attempts}

        with patch('psycopg2.pool.ThreadedConnectionPool'):
            crew = RetryTestCrew(crew_name="retry-integration", crew_type="test")

            result = crew.run_with_retry()

            assert result['status'] == 'success'
            assert result['attempts'] == 2
            print(f"✅ Retry mechanism: succeeded after {result['attempts']} attempts")

    def test_fatal_error_integration(self):
        """測試 FatalError 集成"""
        from automation.clawdbot.crews.common.base_crew import BaseCrew, FatalError

        class FatalTestCrew(BaseCrew):
            def __init__(self, *args, **kwargs):
                super().__init__(*args, **kwargs)
                self.attempts = 0

            def create_crew(self):
                pass

            def run(self, **kwargs):
                self.attempts += 1
                raise ValueError("Config error")

        with patch('psycopg2.pool.ThreadedConnectionPool'):
            crew = FatalTestCrew(crew_name="fatal-integration", crew_type="test")

            with pytest.raises(FatalError) as exc_info:
                crew.run_with_retry()

            assert "Config error" in str(exc_info.value)
            assert crew.attempts == 1
            print("✅ Fatal error: failed immediately without retry")


class TestClaudeServiceIntegration:
    """Claude 服務集成測試（無需真實 API）"""

    def test_claude_service_initialization(self):
        """測試 Claude 服務初始化"""
        from automation.clawdbot.ai.claude_service import ClaudeService

        service = ClaudeService(api_key="test-key", model="sonnet")

        assert service.model == "claude-sonnet-4.5"  # Model name is mapped internally
        assert service.client is not None
        assert service.temperature == 0.2
        print(f"✅ Claude service initialized: model={service.model}, temp={service.temperature}")

    def test_code_parsing_integration(self):
        """測試代碼解析集成"""
        from automation.clawdbot.ai.claude_service import ClaudeService

        service = ClaudeService(api_key="test-key")

        mock_response = """
// FILE: sa-admin/src/main/java/net/lab1024/sa/admin/test/TestEntity.java
package net.lab1024.sa.admin.test;

import lombok.Data;

@Data
public class TestEntity {
    private Long id;
    private String name;
}

// FILE: sa-admin/src/main/java/net/lab1024/sa/admin/test/TestDao.java
package net.lab1024.sa.admin.test;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TestDao {
}
"""

        result = service._parse_generated_code(mock_response)

        assert len(result) == 2
        assert all(path.endswith('.java') for path in result.keys())

        entity_code = result["sa-admin/src/main/java/net/lab1024/sa/admin/test/TestEntity.java"]
        assert "@Data" in entity_code
        assert "TestEntity" in entity_code

        print(f"✅ Successfully parsed {len(result)} Java files")
        for path in result.keys():
            print(f"   - {path}")

    def test_prompt_building_integration(self):
        """測試 Prompt 構建集成"""
        from automation.clawdbot.ai.claude_service import ClaudeService

        service = ClaudeService(api_key="test-key")

        feature_spec = {
            "name": "Employee Management",  # Required field
            "entity": "Employee",
            "endpoints": ["list", "add", "update", "delete"],
            "validation": {"name": "required", "email": "email"}
        }

        context = {
            "example.java": "public class Example {}"
        }

        prompt = service._build_backend_prompt(feature_spec, context)

        # 驗證 prompt 包含關鍵信息
        assert "Employee" in prompt
        assert "list" in prompt
        assert "SmartAdmin" in prompt
        assert "@Transactional" in prompt
        assert "ResponseDTO" in prompt

        print("✅ Prompt correctly built with SmartAdmin patterns")
        print(f"   Prompt length: {len(prompt)} characters")


class TestToolsIntegration:
    """工具集成測試"""

    def test_database_query_tool_initialization(self):
        """測試 DatabaseQueryTool 初始化"""
        from automation.clawdbot.crews.common.tools import DatabaseQueryTool

        mock_pool = MagicMock()
        mock_conn = MagicMock()
        mock_cursor = MagicMock()

        mock_pool.getconn.return_value = mock_conn
        mock_conn.cursor.return_value = mock_cursor

        # Mock extension query
        mock_cursor.fetchone.return_value = ('pg_stat_statements', '1.9', 'comment')

        db_tool = DatabaseQueryTool(connection_pool=mock_pool)

        assert db_tool.connection_pool is mock_pool
        assert db_tool.pg_stat_statements_enabled == True
        print("✅ DatabaseQueryTool initialized with connection pool")

    def test_slow_query_check_integration(self):
        """測試慢查詢檢查集成"""
        from automation.clawdbot.crews.common.tools import DatabaseQueryTool

        mock_pool = MagicMock()
        mock_conn = MagicMock()
        mock_cursor = MagicMock()

        mock_pool.getconn.return_value = mock_conn
        mock_conn.cursor.return_value = mock_cursor

        # Mock extension enabled
        mock_cursor.fetchone.return_value = ('pg_stat_statements', '1.9', 'comment')

        db_tool = DatabaseQueryTool(connection_pool=mock_pool)

        # Mock slow query results
        mock_cursor.fetchall.return_value = [
            ('SELECT * FROM users', 100, 1500.0, 2000.0),
        ]

        slow_queries = db_tool.check_slow_queries(min_duration_ms=1000)

        assert len(slow_queries) == 1
        assert slow_queries[0]['query'] == 'SELECT * FROM users'
        assert slow_queries[0]['mean_time_ms'] == 1500.0
        print(f"✅ Slow query check returned {len(slow_queries)} queries")


def run_core_integration_tests():
    """運行核心集成測試"""
    print("\n" + "=" * 70)
    print("SmartAdmin Auto-Coding - Core Integration Tests")
    print("(No CrewAI dependency)")
    print("=" * 70)
    print()

    exit_code = pytest.main([
        __file__,
        "-v",
        "--tb=short",
        "-s",
        "--maxfail=5",
    ])

    return exit_code


if __name__ == '__main__':
    sys.exit(run_core_integration_tests())
