#!/usr/bin/env python3
"""
SmartAdmin Auto-Coding - Developer Crew End-to-End Test
Developer Crew 端到端驗證測試（含 Claude API 集成驗證）

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
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', '..', 'ai'))

# 導入 Developer Crew 和 Claude Service
from developer_crew import DeveloperCrew
try:
    from claude_service import ClaudeService
except ImportError:
    ClaudeService = None


class TestDeveloperCrewE2E(unittest.TestCase):
    """Developer Crew 端到端測試"""

    def setUp(self):
        """測試初始化"""
        self.mock_db_string = 'postgresql://test:test@localhost:5432/test_db'
        self.test_feature_spec = {
            "name": "Test Employee Management",
            "entity": "TestEmployee",
            "endpoints": ["list", "add", "update", "delete"],
            "fields": {
                "name": {"type": "String", "required": True},
                "email": {"type": "String", "required": True}
            }
        }

    def test_crew_initialization(self):
        """測試 Crew 初始化"""
        with patch.dict(os.environ, {
            'DB_CONNECTION_STRING': self.mock_db_string,
            'PROJECT_ROOT': '/tmp/test-project'
        }):
            crew = DeveloperCrew()

            # 驗證 Agents 創建成功
            self.assertIsNotNone(crew.java_architect_agent)
            self.assertIsNotNone(crew.vue_expert_agent)
            self.assertIsNotNone(crew.devops_engineer_agent)

            # 驗證 Agents 有工具
            self.assertGreater(len(crew.java_architect_agent.tools), 0)
            self.assertGreater(len(crew.vue_expert_agent.tools), 0)
            self.assertGreater(len(crew.devops_engineer_agent.tools), 0)

            print(f"✅ Java Architect Agent tools: {len(crew.java_architect_agent.tools)}")
            print(f"✅ Vue Expert Agent tools: {len(crew.vue_expert_agent.tools)}")
            print(f"✅ DevOps Engineer Agent tools: {len(crew.devops_engineer_agent.tools)}")

    def test_java_architect_has_claude_tool(self):
        """測試 Java Architect Agent 有 Claude 代碼生成工具"""
        with patch.dict(os.environ, {
            'DB_CONNECTION_STRING': self.mock_db_string,
            'PROJECT_ROOT': '/tmp/test-project'
        }):
            crew = DeveloperCrew()

            # 獲取工具列表
            tools = crew.java_architect_agent.tools

            # 查找 generate_java_code_tool
            tool_names = [getattr(t, '__name__', str(t)) for t in tools]

            # 驗證包含 Claude 生成工具
            has_claude_tool = any('generate' in str(name).lower() and 'java' in str(name).lower()
                                 for name in tool_names)

            self.assertTrue(has_claude_tool,
                f"Java Architect should have Claude code generation tool. Tools: {tool_names}")

            print(f"✅ Java Architect has Claude code generation tool")
            print(f"   Tools: {tool_names}")

    def test_crew_creation(self):
        """測試 Crew 創建"""
        with patch.dict(os.environ, {
            'DB_CONNECTION_STRING': self.mock_db_string,
            'PROJECT_ROOT': '/tmp/test-project'
        }):
            developer = DeveloperCrew()
            crew = developer.create_crew(self.test_feature_spec)

            # 驗證 Crew 對象
            self.assertIsNotNone(crew)

            # 驗證 Agents
            self.assertEqual(len(crew.agents), 3)

            # 驗證 Tasks
            self.assertEqual(len(crew.tasks), 3)

            print(f"✅ Crew created with {len(crew.agents)} agents and {len(crew.tasks)} tasks")

    @patch('developer_crew.BaseCrew.log_execution_start')
    @patch('developer_crew.BaseCrew.log_execution_complete')
    @patch('developer_crew.GitOperationTool')
    def test_run_method_structure(self, mock_git, mock_log_complete, mock_log_start):
        """測試 run 方法結構（不實際執行 CrewAI）"""
        with patch.dict(os.environ, {
            'DB_CONNECTION_STRING': self.mock_db_string,
            'PROJECT_ROOT': '/tmp/test-project'
        }):
            mock_log_start.return_value = 'test-execution-id'

            # Mock Git 操作
            mock_git_instance = MagicMock()
            mock_git.return_value = mock_git_instance
            mock_git_instance.create_branch.return_value = True
            mock_git_instance.commit_changes.return_value = True
            mock_git_instance.create_pull_request.return_value = "https://github.com/test/pull/123"

            developer = DeveloperCrew()

            # Mock crew.kickoff
            with patch.object(developer, 'create_crew') as mock_create_crew:
                mock_crew = MagicMock()
                mock_crew.kickoff.return_value = MagicMock(
                    output=json.dumps({
                        "files_generated": 9,
                        "files": {}
                    })
                )
                mock_create_crew.return_value = mock_crew

                # 運行 Developer
                result = developer.run(feature_spec=self.test_feature_spec)

                # 驗證結果結構
                self.assertIn('status', result)
                self.assertIn('execution_id', result)
                self.assertIn('feature_name', result)
                self.assertIn('branch_name', result)
                self.assertIn('pr_url', result)

                # 驗證 kickoff 被調用
                mock_crew.kickoff.assert_called_once()

                print(f"✅ Run method structure correct")
                print(f"   Result keys: {list(result.keys())}")

    def test_no_manual_run_method(self):
        """測試確認沒有手動繞過方法"""
        with patch.dict(os.environ, {
            'DB_CONNECTION_STRING': self.mock_db_string,
            'PROJECT_ROOT': '/tmp/test-project'
        }):
            developer = DeveloperCrew()

            # 確認 _run_development_manually 方法不存在
            self.assertFalse(hasattr(developer, '_run_development_manually'))

            print(f"✅ No manual bypass method found")


class TestClaudeIntegration(unittest.TestCase):
    """Claude API 集成測試"""

    def setUp(self):
        """測試初始化"""
        self.api_key = os.getenv('CLAUDE_API_KEY')
        self.test_feature_spec = {
            "name": "Simple Task",
            "entity": "Task",
            "endpoints": ["list", "add"],
            "fields": {
                "title": {"type": "String", "required": True}
            }
        }

    def test_claude_service_available(self):
        """測試 Claude Service 可用"""
        if ClaudeService is None:
            self.skipTest("ClaudeService not available")

        self.assertIsNotNone(ClaudeService)
        print(f"✅ ClaudeService imported successfully")

    def test_generate_java_code_tool_callable(self):
        """測試 generate_java_code_tool 可調用"""
        sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'common'))
        from tools import generate_java_code_tool

        self.assertTrue(callable(generate_java_code_tool))
        print(f"✅ generate_java_code_tool is callable")

    @unittest.skipIf(not os.getenv('CLAUDE_API_KEY'), "CLAUDE_API_KEY not set")
    def test_real_claude_code_generation(self):
        """測試真實 Claude 代碼生成（需要 API Key）"""
        if ClaudeService is None:
            self.skipTest("ClaudeService not available")

        service = ClaudeService(api_key=self.api_key, model="haiku")  # 使用 haiku 節省成本

        try:
            # 生成代碼
            files = service.generate_java_backend(self.test_feature_spec, {})

            # 驗證結果
            self.assertIsInstance(files, dict)
            self.assertGreater(len(files), 0, "Should generate at least one file")

            # 驗證文件類型
            for file_path, content in files.items():
                self.assertIn('.java', file_path, f"File should be Java: {file_path}")
                self.assertGreater(len(content), 0, f"File content should not be empty: {file_path}")

            print(f"\n✅ Claude generated {len(files)} files:")
            for file_path in files.keys():
                print(f"   - {file_path}")

            # 驗證生成的代碼包含 SmartAdmin 關鍵詞
            all_content = '\n'.join(files.values())
            self.assertIn('package net.lab1024.sa', all_content, "Should use SmartAdmin package")

            # 檢查是否包含關鍵類
            file_types = [os.path.basename(f).replace('.java', '') for f in files.keys()]
            print(f"\n   File types: {file_types}")

        except Exception as e:
            self.fail(f"Real Claude code generation failed: {e}")


def run_tests():
    """運行所有測試"""
    print("=" * 70)
    print("Developer Crew End-to-End Tests (Day 6)")
    print("=" * 70)
    print()

    # 檢查 API Key
    api_key = os.getenv('CLAUDE_API_KEY')
    if api_key:
        print("✅ CLAUDE_API_KEY found - will run Claude integration tests")
    else:
        print("⚠️  CLAUDE_API_KEY not set - skipping Claude integration tests")
    print()

    # 創建測試套件
    loader = unittest.TestLoader()
    suite = unittest.TestSuite()

    # 添加測試
    suite.addTests(loader.loadTestsFromTestCase(TestDeveloperCrewE2E))
    suite.addTests(loader.loadTestsFromTestCase(TestClaudeIntegration))

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
    print("驗收標準檢查 (Day 6)")
    print("=" * 70)

    if result.wasSuccessful():
        print("✅ Developer Crew 成功初始化")
        print("✅ Java Architect Agent 包含 Claude 工具")
        print("✅ Crew 結構驗證通過")
        print("✅ run() 方法結構正確")
        print("✅ 無手動繞過方法")
        if api_key:
            print("✅ Claude API 代碼生成測試通過")
        else:
            print("⏳ Claude API 測試跳過（無 API Key）")
        print()
        print("🎉 Day 6 驗收標準通過！")
    else:
        print("❌ 部分測試失敗，需要修復")

    print("=" * 70)

    return result.wasSuccessful()


if __name__ == '__main__':
    success = run_tests()
    sys.exit(0 if success else 1)
