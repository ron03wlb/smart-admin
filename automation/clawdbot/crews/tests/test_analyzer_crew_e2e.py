#!/usr/bin/env python3
"""
SmartAdmin Auto-Coding - Analyzer Crew End-to-End Test
Analyzer Crew 端到端驗證測試

作者: SmartAdmin Auto-Coding System
版本: 1.0.0
日期: 2026-01-27
"""

import os
import sys
import unittest
from unittest.mock import patch, MagicMock
import json

# 添加路徑
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

# 導入 Analyzer Crew
from analyzer_crew import AnalyzerCrew


class TestAnalyzerCrewE2E(unittest.TestCase):
    """Analyzer Crew 端到端測試"""

    def setUp(self):
        """測試初始化"""
        # Mock 數據庫連接
        self.mock_db_string = 'postgresql://test:test@localhost:5432/test_db'

    def test_crew_initialization(self):
        """測試 Crew 初始化"""
        with patch.dict(os.environ, {
            'DB_CONNECTION_STRING': self.mock_db_string,
            'PROJECT_ROOT': '/tmp/test-project'
        }):
            crew = AnalyzerCrew()

            # 驗證 Agents 創建成功
            self.assertIsNotNone(crew.java_architect_agent)
            self.assertIsNotNone(crew.postgres_pro_agent)
            self.assertIsNotNone(crew.code_reviewer_agent)

            # 驗證 Agents 有工具
            self.assertGreater(len(crew.java_architect_agent.tools), 0)
            self.assertGreater(len(crew.postgres_pro_agent.tools), 0)
            self.assertGreater(len(crew.code_reviewer_agent.tools), 0)

            print(f"✅ Java Architect Agent tools: {len(crew.java_architect_agent.tools)}")
            print(f"✅ PostgreSQL Pro Agent tools: {len(crew.postgres_pro_agent.tools)}")
            print(f"✅ Code Reviewer Agent tools: {len(crew.code_reviewer_agent.tools)}")

    def test_crew_creation(self):
        """測試 Crew 創建"""
        with patch.dict(os.environ, {
            'DB_CONNECTION_STRING': self.mock_db_string,
            'PROJECT_ROOT': '/tmp/test-project'
        }):
            analyzer = AnalyzerCrew()
            crew = analyzer.create_crew(target_module="sa-admin")

            # 驗證 Crew 對象
            self.assertIsNotNone(crew)

            # 驗證 Agents
            self.assertEqual(len(crew.agents), 3)

            # 驗證 Tasks
            self.assertEqual(len(crew.tasks), 3)

            print(f"✅ Crew created with {len(crew.agents)} agents and {len(crew.tasks)} tasks")

    def test_tools_are_callable(self):
        """測試工具可調用"""
        with patch.dict(os.environ, {
            'DB_CONNECTION_STRING': self.mock_db_string,
            'PROJECT_ROOT': '/tmp/test-project'
        }):
            crew = AnalyzerCrew()

            # 驗證工具列表
            java_tools = crew.java_architect_agent.tools
            postgres_tools = crew.postgres_pro_agent.tools
            reviewer_tools = crew.code_reviewer_agent.tools

            # 所有工具都應該可調用
            for tool in java_tools:
                self.assertTrue(callable(tool) or hasattr(tool, 'run'))

            for tool in postgres_tools:
                self.assertTrue(callable(tool) or hasattr(tool, 'run'))

            for tool in reviewer_tools:
                self.assertTrue(callable(tool) or hasattr(tool, 'run'))

            print(f"✅ All tools are callable")

    @patch('analyzer_crew.BaseCrew.log_execution_start')
    @patch('analyzer_crew.BaseCrew.log_execution_complete')
    def test_run_method_structure(self, mock_log_complete, mock_log_start):
        """測試 run 方法結構（不實際執行 CrewAI）"""
        with patch.dict(os.environ, {
            'DB_CONNECTION_STRING': self.mock_db_string,
            'PROJECT_ROOT': '/tmp/test-project'
        }):
            mock_log_start.return_value = 'test-execution-id'

            analyzer = AnalyzerCrew()

            # Mock crew.kickoff
            with patch.object(analyzer, 'create_crew') as mock_create_crew:
                mock_crew = MagicMock()
                mock_crew.kickoff.return_value = MagicMock(
                    output="Analysis completed successfully"
                )
                mock_create_crew.return_value = mock_crew

                # 運行 Analyzer
                result = analyzer.run(target_module="sa-admin")

                # 驗證結果結構
                self.assertIn('status', result)
                self.assertIn('execution_id', result)
                self.assertIn('target_module', result)
                self.assertIn('results', result)

                # 驗證 kickoff 被調用
                mock_crew.kickoff.assert_called_once()

                print(f"✅ Run method structure correct")
                print(f"   Result keys: {list(result.keys())}")

    def test_parse_crew_output(self):
        """測試結果解析"""
        with patch.dict(os.environ, {
            'DB_CONNECTION_STRING': self.mock_db_string,
            'PROJECT_ROOT': '/tmp/test-project'
        }):
            analyzer = AnalyzerCrew()

            # 測試字符串輸出解析
            mock_output = """
            {
                "architecture": {
                    "violations_count": 0
                },
                "database": {
                    "slow_queries_count": 2
                }
            }
            """

            result = analyzer._parse_crew_output(mock_output)

            # 驗證解析結果
            self.assertIn('architecture', result)
            self.assertEqual(result['architecture']['violations_count'], 0)

            print(f"✅ Output parsing works correctly")

    def test_no_manual_run_method(self):
        """測試確認沒有手動繞過方法"""
        with patch.dict(os.environ, {
            'DB_CONNECTION_STRING': self.mock_db_string,
            'PROJECT_ROOT': '/tmp/test-project'
        }):
            analyzer = AnalyzerCrew()

            # 確認 _run_analysis_manually 方法不存在
            self.assertFalse(hasattr(analyzer, '_run_analysis_manually'))

            print(f"✅ No manual bypass method found")


class TestAnalyzerCrewIntegration(unittest.TestCase):
    """Analyzer Crew 集成測試（需要真實環境）"""

    def setUp(self):
        """測試初始化"""
        self.db_string = os.getenv('DB_CONNECTION_STRING')
        self.project_root = os.getenv('PROJECT_ROOT', os.getcwd())

        if not self.db_string:
            self.skipTest("DB_CONNECTION_STRING not set")

    def test_real_crew_execution_dry_run(self):
        """測試真實 Crew 執行（Dry Run）"""
        # 這個測試需要真實的數據庫連接
        # 但我們只測試到 crew 創建階段，不真正執行

        with patch.dict(os.environ, {
            'DB_CONNECTION_STRING': self.db_string,
            'PROJECT_ROOT': self.project_root
        }):
            analyzer = AnalyzerCrew()
            crew = analyzer.create_crew(target_module="sa-admin")

            # 驗證 Crew 結構
            self.assertEqual(len(crew.agents), 3)
            self.assertEqual(len(crew.tasks), 3)

            # 驗證每個 Agent 的工具配置
            for agent in crew.agents:
                self.assertGreater(len(agent.tools), 0,
                    f"Agent {agent.role} should have tools")

            print(f"✅ Real crew structure validated")
            print(f"   Agents: {[a.role for a in crew.agents]}")
            print(f"   Tool counts: {[len(a.tools) for a in crew.agents]}")


def run_tests():
    """運行所有測試"""
    print("=" * 70)
    print("Analyzer Crew End-to-End Tests (Day 5)")
    print("=" * 70)
    print()

    # 創建測試套件
    loader = unittest.TestLoader()
    suite = unittest.TestSuite()

    # 添加測試
    suite.addTests(loader.loadTestsFromTestCase(TestAnalyzerCrewE2E))
    suite.addTests(loader.loadTestsFromTestCase(TestAnalyzerCrewIntegration))

    # 運行測試
    runner = unittest.TextTestRunner(verbosity=2)
    result = runner.run(suite)

    # 打印總結
    print()
    print("=" * 70)
    print(f"Tests run: {result.testsRun}")
    print(f"Successes: {result.testsRun - len(result.failures) - len(result.errors)}")
    print(f"Failures: {len(result.failures)}")
    print(f"Errors: {len(result.errors)}")
    print(f"Skipped: {len(result.skipped)}")
    print("=" * 70)

    # 驗收標準檢查
    print()
    print("=" * 70)
    print("驗收標準檢查 (Day 5)")
    print("=" * 70)

    if result.wasSuccessful():
        print("✅ Analyzer Crew 成功初始化")
        print("✅ 所有工具正確配置")
        print("✅ Crew 結構驗證通過")
        print("✅ 結果解析功能正常")
        print("✅ 無手動繞過方法")
        print()
        print("🎉 Day 5 驗收標準 100% 通過！")
    else:
        print("❌ 部分測試失敗，需要修復")

    print("=" * 70)

    return result.wasSuccessful()


if __name__ == '__main__':
    success = run_tests()
    sys.exit(0 if success else 1)
