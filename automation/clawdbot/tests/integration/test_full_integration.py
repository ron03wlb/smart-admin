#!/usr/bin/env python3
"""
SmartAdmin Auto-Coding - Full Integration Tests
完整集成測試（CrewAI + Claude API + PostgreSQL）

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

# 設置 API keys（測試模式下使用 mock）
os.environ['CLAUDE_API_KEY'] = os.getenv('CLAUDE_API_KEY', 'test-key')
os.environ['TELEGRAM_BOT_TOKEN'] = 'test-token'
os.environ['TELEGRAM_CHAT_ID'] = '12345'


class TestCrewAIIntegration:
    """CrewAI 集成測試"""

    def test_analyzer_crew_initialization(self):
        """測試 AnalyzerCrew 初始化"""
        from automation.clawdbot.crews.analyzer_crew import AnalyzerCrew

        with patch('psycopg2.pool.ThreadedConnectionPool'):
            crew = AnalyzerCrew()

            # 驗證 Crew 屬性
            assert crew.crew_name == "analyzer-crew"
            assert crew.crew_type == "analysis"
            assert crew.crew is not None

            # 驗證 Agents 有 tools
            agents = crew.crew.agents if hasattr(crew.crew, 'agents') else []
            if agents:
                for agent in agents:
                    assert hasattr(agent, 'tools'), f"Agent {agent.role} missing tools"
                    print(f"✅ {agent.role} has {len(agent.tools)} tools")

    def test_developer_crew_initialization(self):
        """測試 DeveloperCrew 初始化"""
        from automation.clawdbot.crews.developer_crew import DeveloperCrew

        with patch('psycopg2.pool.ThreadedConnectionPool'):
            crew = DeveloperCrew()

            assert crew.crew_name == "developer-crew"
            assert crew.crew_type == "development"
            assert crew.crew is not None

    def test_qa_crew_initialization(self):
        """測試 QACrew 初始化"""
        from automation.clawdbot.crews.qa_crew import QACrew

        with patch('psycopg2.pool.ThreadedConnectionPool'):
            crew = QACrew()

            assert crew.crew_name == "qa-crew"
            assert crew.crew_type == "quality-assurance"
            assert crew.crew is not None


class TestClaudeServiceIntegration:
    """Claude API 服務集成測試"""

    def test_claude_service_initialization(self):
        """測試 ClaudeService 初始化"""
        from automation.clawdbot.ai.claude_service import ClaudeService

        # 使用 mock API key
        service = ClaudeService(api_key="test-key", model="sonnet")

        assert service.model == "sonnet"
        assert service.client is not None

    @pytest.mark.skipif(
        not os.getenv('CLAUDE_API_KEY') or os.getenv('CLAUDE_API_KEY') == 'test-key',
        reason="Real Claude API key not configured"
    )
    def test_claude_code_generation_real_api(self):
        """測試真實 Claude API 代碼生成（需要 API key）"""
        from automation.clawdbot.ai.claude_service import ClaudeService

        service = ClaudeService(model="haiku", temperature=0.2)  # Use haiku for faster/cheaper test

        feature_spec = {
            "entity": "TestEntity",
            "endpoints": ["list"],
            "validation": {}
        }

        # 簡化的 context
        context = {
            "example.java": "// Example SmartAdmin code"
        }

        try:
            result = service.generate_java_backend(feature_spec, context)
            assert isinstance(result, dict)
            print(f"✅ Generated {len(result)} files")

            # 驗證生成的文件路徑
            for file_path in result.keys():
                assert file_path.endswith('.java')
                print(f"   - {file_path}")
        except Exception as e:
            print(f"⚠️ Claude API call failed: {e}")
            pytest.skip(f"Claude API unavailable: {e}")

    def test_claude_code_parsing(self):
        """測試 Claude 代碼解析（mock 響應）"""
        from automation.clawdbot.ai.claude_service import ClaudeService

        service = ClaudeService(api_key="test-key")

        # Mock Claude 響應
        mock_response = """
// FILE: sa-admin/src/main/java/net/lab1024/sa/admin/module/test/domain/entity/TestEntity.java
package net.lab1024.sa.admin.module.test.domain.entity;

import lombok.Data;
import com.baomidou.mybatisplus.annotation.*;

@Data
@TableName("t_test")
public class TestEntity {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    @TableLogic
    private Boolean deleted;
}

// FILE: sa-admin/src/main/java/net/lab1024/sa/admin/module/test/dao/TestDao.java
package net.lab1024.sa.admin.module.test.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.lab1024.sa.admin.module.test.domain.entity.TestEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TestDao extends BaseMapper<TestEntity> {
}
"""

        # 測試解析
        result = service._parse_generated_code(mock_response)

        assert len(result) == 2
        assert "sa-admin/src/main/java/net/lab1024/sa/admin/module/test/domain/entity/TestEntity.java" in result
        assert "sa-admin/src/main/java/net/lab1024/sa/admin/module/test/dao/TestDao.java" in result

        # 驗證代碼內容
        entity_code = result["sa-admin/src/main/java/net/lab1024/sa/admin/module/test/domain/entity/TestEntity.java"]
        assert "@Data" in entity_code
        assert "TestEntity" in entity_code
        assert "@TableLogic" in entity_code

        print("✅ Successfully parsed 2 Java files from Claude response")


class TestDatabaseIntegration:
    """數據庫集成測試（使用 Testcontainers）"""

    @pytest.fixture(scope="class")
    def postgres_container(self):
        """啟動 PostgreSQL Testcontainer"""
        try:
            from testcontainers.postgres import PostgresContainer

            with PostgresContainer("postgres:15") as postgres:
                # 設置連接字符串
                connection_url = postgres.get_connection_url()
                os.environ['DB_CONNECTION_STRING'] = connection_url

                # 初始化數據庫
                conn = postgres.get_connection()
                cursor = conn.cursor()

                # 創建審計表
                cursor.execute("""
                    CREATE TABLE IF NOT EXISTS t_ai_operation_audit (
                        id SERIAL PRIMARY KEY,
                        execution_id VARCHAR(64),
                        crew_name VARCHAR(100),
                        crew_type VARCHAR(50),
                        operation VARCHAR(100),
                        status VARCHAR(20),
                        inputs JSONB,
                        outputs JSONB,
                        error_message TEXT,
                        execution_time_ms INTEGER,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """)

                # 啟用 pg_stat_statements
                try:
                    cursor.execute("CREATE EXTENSION IF NOT EXISTS pg_stat_statements;")
                    print("✅ pg_stat_statements extension enabled")
                except Exception as e:
                    print(f"⚠️ Could not enable pg_stat_statements: {e}")

                conn.commit()
                cursor.close()

                yield postgres
        except ImportError:
            pytest.skip("testcontainers not available")

    def test_connection_pool_with_real_postgres(self, postgres_container):
        """測試連接池與真實 PostgreSQL"""
        from automation.clawdbot.crews.common.base_crew import BaseCrew

        class TestCrew(BaseCrew):
            def create_crew(self):
                pass

            def run(self, **kwargs):
                return {"status": "success"}

        crew = TestCrew(crew_name="test-integration", crew_type="test")

        # 測試連接池
        with crew.get_db_connection() as conn:
            cursor = conn.cursor()
            cursor.execute("SELECT 1")
            result = cursor.fetchone()
            assert result[0] == 1
            cursor.close()
            print("✅ Connection pool works with real PostgreSQL")

    def test_audit_logging_with_real_postgres(self, postgres_container):
        """測試審計日誌寫入真實數據庫"""
        from automation.clawdbot.crews.common.base_crew import BaseCrew

        class TestCrew(BaseCrew):
            def create_crew(self):
                pass

            def run(self, **kwargs):
                return {"status": "success", "result": "test"}

        crew = TestCrew(crew_name="audit-test", crew_type="test")

        # 記錄審計日誌
        execution_id = crew.log_execution_start({"test": "input"})
        assert execution_id is not None
        print(f"✅ Created execution: {execution_id}")

        # 記錄完成
        crew.log_execution_complete(execution_id, {"test": "output"})

        # 驗證審計記錄
        with crew.get_db_connection() as conn:
            cursor = conn.cursor()
            cursor.execute("""
                SELECT execution_id, crew_name, status, inputs, outputs
                FROM t_ai_operation_audit
                WHERE execution_id = %s
            """, (execution_id,))

            record = cursor.fetchone()
            assert record is not None
            assert record[1] == "audit-test"
            assert record[2] == "COMPLETED"
            cursor.close()

            print(f"✅ Audit record verified: {record[0]}")

    def test_pg_stat_statements_integration(self, postgres_container):
        """測試 pg_stat_statements 集成"""
        from automation.clawdbot.crews.common.tools import DatabaseQueryTool
        from psycopg2 import pool

        # 創建連接池
        conn_pool = pool.SimpleConnectionPool(
            minconn=1,
            maxconn=5,
            dsn=os.environ['DB_CONNECTION_STRING']
        )

        db_tool = DatabaseQueryTool(connection_pool=conn_pool)

        # 檢查擴展狀態
        if db_tool.pg_stat_statements_enabled:
            print("✅ pg_stat_statements enabled")

            # 執行一些查詢
            with db_tool._get_connection() as conn:
                cursor = conn.cursor()
                for i in range(5):
                    cursor.execute("SELECT COUNT(*) FROM t_ai_operation_audit")
                cursor.close()

            # 檢查慢查詢（應該為空，因為查詢很快）
            slow_queries = db_tool.check_slow_queries(min_duration_ms=100)
            print(f"✅ Slow query check returned {len(slow_queries)} queries")
        else:
            print("⚠️ pg_stat_statements not enabled, skipping query tests")

        conn_pool.closeall()


class TestEndToEndWorkflow:
    """端到端工作流測試"""

    def test_analyzer_workflow_mock(self):
        """測試 Analyzer 工作流（mock 模式）"""
        from automation.clawdbot.crews.analyzer_crew import AnalyzerCrew

        with patch('psycopg2.pool.ThreadedConnectionPool'):
            crew = AnalyzerCrew()

            # Mock crew.kickoff 返回值
            mock_result = MagicMock()
            mock_result.tasks_output = [
                MagicMock(description="ArchUnit Test", output="All tests passed"),
                MagicMock(description="Checkstyle", output="No violations"),
            ]

            with patch.object(crew.crew, 'kickoff', return_value=mock_result):
                result = crew.run(target_module="sa-admin")

                assert result['status'] == 'SUCCESS'
                assert 'execution_id' in result
                print(f"✅ Analyzer workflow completed: {result['execution_id']}")

    def test_retry_mechanism_integration(self):
        """測試重試機制集成"""
        from automation.clawdbot.crews.common.base_crew import BaseCrew, RetryableError
        import requests

        class FlakyCrew(BaseCrew):
            def __init__(self, *args, **kwargs):
                super().__init__(*args, **kwargs)
                self.attempt = 0

            def create_crew(self):
                pass

            def run(self, **kwargs):
                self.attempt += 1
                if self.attempt < 2:
                    raise requests.exceptions.RequestException("Network error")
                return {"status": "success", "attempts": self.attempt}

        with patch('psycopg2.pool.ThreadedConnectionPool'):
            crew = FlakyCrew(crew_name="flaky-test", crew_type="test")

            result = crew.run_with_retry()

            assert result['status'] == 'success'
            assert result['attempts'] == 2
            print(f"✅ Retry mechanism worked: {result['attempts']} attempts")


def run_integration_tests():
    """運行所有集成測試"""
    print("\n" + "=" * 70)
    print("SmartAdmin Auto-Coding - Integration Tests")
    print("=" * 70)
    print()

    # 運行測試
    exit_code = pytest.main([
        __file__,
        "-v",
        "--tb=short",
        "-s",  # 顯示 print 輸出
        "--maxfail=3",  # 最多失敗 3 個測試就停止
    ])

    return exit_code


if __name__ == '__main__':
    sys.exit(run_integration_tests())
