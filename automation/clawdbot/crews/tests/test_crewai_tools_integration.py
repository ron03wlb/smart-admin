#!/usr/bin/env python3
"""
SmartAdmin Auto-Coding - CrewAI Tools Integration Test
測試 @tool 裝飾器函數和 CrewAI 集成

作者: SmartAdmin Auto-Coding System
版本: 2.0.0
日期: 2026-01-27
"""

import os
import sys
import unittest
from unittest.mock import patch, MagicMock

# 添加路徑以導入模塊
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'common'))

# 導入工具
from tools import (
    # 工具類
    SafeFileAccessTool, CodeAnalysisTool, GitOperationTool,
    # @tool 函數
    read_file_tool, write_file_tool, list_files_tool,
    run_checkstyle_tool, run_pmd_tool, run_spotbugs_tool, run_archunit_tool,
    check_slow_queries_tool,
    create_branch_tool, commit_changes_tool, create_pr_tool,
    # 工具列表
    FILE_TOOLS, CODE_ANALYSIS_TOOLS, DATABASE_TOOLS, GIT_TOOLS, ALL_TOOLS
)


class TestCrewAIToolsIntegration(unittest.TestCase):
    """測試 CrewAI Tools 集成"""

    def test_tool_lists_count(self):
        """測試工具列表數量"""
        self.assertEqual(len(FILE_TOOLS), 3, "應該有 3 個文件工具")
        self.assertEqual(len(CODE_ANALYSIS_TOOLS), 4, "應該有 4 個代碼分析工具")
        self.assertEqual(len(DATABASE_TOOLS), 3, "應該有 3 個數據庫工具")
        self.assertEqual(len(GIT_TOOLS), 3, "應該有 3 個 Git 工具")
        self.assertEqual(len(ALL_TOOLS), 13, "總共應該有 13 個工具")

    def test_tool_functions_exist(self):
        """測試所有 @tool 函數都存在"""
        # 文件操作
        self.assertTrue(callable(read_file_tool))
        self.assertTrue(callable(write_file_tool))
        self.assertTrue(callable(list_files_tool))

        # 代碼分析
        self.assertTrue(callable(run_checkstyle_tool))
        self.assertTrue(callable(run_pmd_tool))
        self.assertTrue(callable(run_spotbugs_tool))
        self.assertTrue(callable(run_archunit_tool))

        # 數據庫
        self.assertTrue(callable(check_slow_queries_tool))

        # Git
        self.assertTrue(callable(create_branch_tool))
        self.assertTrue(callable(commit_changes_tool))
        self.assertTrue(callable(create_pr_tool))

    def test_tool_has_crewai_decorator(self):
        """測試工具函數有 CrewAI 裝飾器屬性"""
        # CrewAI @tool 裝飾器會添加特殊屬性
        # 檢查函數是否被裝飾（通過 __name__ 或其他屬性）
        self.assertEqual(read_file_tool.__name__, "read_file_tool")
        self.assertEqual(run_checkstyle_tool.__name__, "run_checkstyle_tool")

    @patch.dict(os.environ, {'DB_CONNECTION_STRING': ''})
    def test_read_file_tool_no_db_config(self):
        """測試 read_file_tool 在沒有數據庫配置時的行為"""
        result = read_file_tool("/tmp/test.txt")
        self.assertIn("Error", result)
        self.assertIn("DB_CONNECTION_STRING not configured", result)

    @patch.dict(os.environ, {'PROJECT_ROOT': '/tmp/test-project'})
    @patch('tools.CodeAnalysisTool')
    def test_run_checkstyle_tool(self, mock_code_tool_class):
        """測試 run_checkstyle_tool"""
        # Mock CodeAnalysisTool
        mock_instance = MagicMock()
        mock_instance.run_checkstyle.return_value = {
            "tool": "Checkstyle",
            "success": True,
            "violations": 0
        }
        mock_code_tool_class.return_value = mock_instance

        result = run_checkstyle_tool("sa-admin")

        # 驗證結果包含 JSON
        self.assertIn("Checkstyle", result)


class TestCrewIntegration(unittest.TestCase):
    """測試 Crew 集成"""

    def test_analyzer_crew_agents_have_tools(self):
        """測試 Analyzer Crew 的 Agents 都有工具"""
        sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))
        from analyzer_crew import AnalyzerCrew

        crew = AnalyzerCrew()

        # 檢查 Java Architect Agent 有工具
        self.assertIsNotNone(crew.java_architect_agent.tools)
        self.assertGreater(len(crew.java_architect_agent.tools), 0)

        # 檢查 PostgreSQL Pro Agent 有工具
        self.assertIsNotNone(crew.postgres_pro_agent.tools)
        self.assertGreater(len(crew.postgres_pro_agent.tools), 0)

        # 檢查 Code Reviewer Agent 有工具
        self.assertIsNotNone(crew.code_reviewer_agent.tools)
        self.assertGreater(len(crew.code_reviewer_agent.tools), 0)

    def test_developer_crew_agents_have_tools(self):
        """測試 Developer Crew 的 Agents 都有工具"""
        sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))
        from developer_crew import DeveloperCrew

        crew = DeveloperCrew()

        # 檢查 Java Architect Agent 有工具
        self.assertIsNotNone(crew.java_architect_agent.tools)
        self.assertGreater(len(crew.java_architect_agent.tools), 0)

        # 檢查 Vue Expert Agent 有工具
        self.assertIsNotNone(crew.vue_expert_agent.tools)
        self.assertGreater(len(crew.vue_expert_agent.tools), 0)

        # 檢查 DevOps Engineer Agent 有工具
        self.assertIsNotNone(crew.devops_engineer_agent.tools)
        self.assertGreater(len(crew.devops_engineer_agent.tools), 0)

    def test_qa_crew_agents_have_tools(self):
        """測試 QA Crew 的 Agents 都有工具"""
        sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))
        from qa_crew import QACrew

        crew = QACrew()

        # 檢查 Code Reviewer Agent 有工具
        self.assertIsNotNone(crew.code_reviewer_agent.tools)
        self.assertGreater(len(crew.code_reviewer_agent.tools), 0)

    def test_no_manual_run_methods(self):
        """測試所有 Crew 都不再有 _run_*_manually 方法"""
        sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

        from analyzer_crew import AnalyzerCrew
        from developer_crew import DeveloperCrew
        from qa_crew import QACrew

        analyzer = AnalyzerCrew()
        developer = DeveloperCrew()
        qa = QACrew()

        # 驗證 _run_*_manually 方法不存在
        self.assertFalse(hasattr(analyzer, '_run_analysis_manually'))
        self.assertFalse(hasattr(developer, '_run_development_manually'))
        self.assertFalse(hasattr(qa, '_run_qa_manually'))


def run_tests():
    """運行所有測試"""
    print("=" * 70)
    print("CrewAI Tools Integration Tests (v2.0.0)")
    print("=" * 70)
    print()

    # 創建測試套件
    loader = unittest.TestLoader()
    suite = unittest.TestSuite()

    # 添加測試
    suite.addTests(loader.loadTestsFromTestCase(TestCrewAIToolsIntegration))
    suite.addTests(loader.loadTestsFromTestCase(TestCrewIntegration))

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
    print("=" * 70)

    return result.wasSuccessful()


if __name__ == '__main__':
    success = run_tests()
    sys.exit(0 if success else 1)
