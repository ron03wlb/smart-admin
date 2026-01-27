#!/usr/bin/env python3
"""
SmartAdmin Auto-Coding - Claude Service Tests
測試 Claude API 服務集成

作者: SmartAdmin Auto-Coding System
版本: 1.0.0
日期: 2026-01-27
"""

import os
import sys
import unittest
import json
from unittest.mock import patch, MagicMock

# 添加路徑
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

# 導入 Claude 服務
try:
    from claude_service import ClaudeService
except ImportError as e:
    print(f"Warning: Could not import ClaudeService: {e}")
    ClaudeService = None


class TestClaudeService(unittest.TestCase):
    """測試 Claude 服務"""

    def setUp(self):
        """測試初始化"""
        self.test_api_key = "test-api-key-12345"

    @patch('claude_service.Anthropic')
    def test_initialization(self, mock_anthropic):
        """測試服務初始化"""
        if ClaudeService is None:
            self.skipTest("ClaudeService not available")

        service = ClaudeService(api_key=self.test_api_key, model="sonnet")

        self.assertEqual(service.api_key, self.test_api_key)
        self.assertEqual(service.model, "claude-sonnet-4.5")
        self.assertEqual(service.temperature, 0.2)

    def test_initialization_without_api_key(self):
        """測試沒有 API Key 時初始化失敗"""
        if ClaudeService is None:
            self.skipTest("ClaudeService not available")

        # 清除環境變量
        with patch.dict(os.environ, {}, clear=True):
            with self.assertRaises(ValueError):
                ClaudeService()

    def test_parse_generated_code(self):
        """測試代碼解析功能"""
        if ClaudeService is None:
            self.skipTest("ClaudeService not available")

        service = ClaudeService(api_key=self.test_api_key)

        # 模擬 Claude 返回的內容
        mock_content = """
Here is the generated code:

```java
// FILE: path/to/FileA.java
package com.example;

public class FileA {
    // Code here
}

// FILE: path/to/FileB.java
package com.example;

public class FileB {
    // More code
}
```

The code follows SmartAdmin patterns.
"""

        files = service._parse_generated_code(mock_content)

        self.assertEqual(len(files), 2)
        self.assertIn("path/to/FileA.java", files)
        self.assertIn("path/to/FileB.java", files)
        self.assertIn("public class FileA", files["path/to/FileA.java"])
        self.assertIn("public class FileB", files["path/to/FileB.java"])

    def test_format_fields_spec(self):
        """測試字段規格格式化"""
        if ClaudeService is None:
            self.skipTest("ClaudeService not available")

        service = ClaudeService(api_key=self.test_api_key)

        fields = {
            "name": {"type": "String", "required": True, "description": "Employee name"},
            "email": {"type": "String", "required": True},
            "age": {"type": "Integer", "required": False}
        }

        result = service._format_fields_spec(fields)

        self.assertIn("name: String (required)", result)
        self.assertIn("Employee name", result)
        self.assertIn("email: String (required)", result)
        self.assertIn("age: Integer", result)

    def test_estimate_tokens(self):
        """測試 token 數量估算"""
        if ClaudeService is None:
            self.skipTest("ClaudeService not available")

        service = ClaudeService(api_key=self.test_api_key)

        text = "This is a test" * 100  # 1400 characters
        tokens = service.estimate_tokens(text)

        # 1400 / 4 = 350
        self.assertAlmostEqual(tokens, 350, delta=10)

    def test_estimate_cost(self):
        """測試成本估算"""
        if ClaudeService is None:
            self.skipTest("ClaudeService not available")

        service = ClaudeService(api_key=self.test_api_key)

        # 1000 input tokens, 5000 output tokens
        cost = service.estimate_cost(1000, 5000)

        # Input: (1000 / 1_000_000) * 3 = 0.003
        # Output: (5000 / 1_000_000) * 15 = 0.075
        # Total: 0.078
        self.assertAlmostEqual(cost, 0.078, places=3)

    @patch('claude_service.Anthropic')
    def test_build_backend_prompt(self, mock_anthropic):
        """測試後端 Prompt 構建"""
        if ClaudeService is None:
            self.skipTest("ClaudeService not available")

        service = ClaudeService(api_key=self.test_api_key)

        feature_spec = {
            "name": "Employee Management",
            "entity": "Employee",
            "endpoints": ["list", "add", "update", "delete"],
            "fields": {
                "name": {"type": "String", "required": True},
                "email": {"type": "String", "required": True}
            }
        }

        prompt = service._build_backend_prompt(feature_spec, {})

        # 驗證 Prompt 包含關鍵信息
        self.assertIn("Employee Management", prompt)
        self.assertIn("Employee", prompt)
        self.assertIn("@RequiredArgsConstructor", prompt)
        self.assertIn("io.vavr.control.Option", prompt)
        self.assertIn("ResponseDTO.ok", prompt)
        self.assertIn("@Transactional", prompt)
        self.assertIn("SmartAdmin", prompt)


class TestClaudeServiceIntegration(unittest.TestCase):
    """測試 Claude 服務集成（需要真實 API Key）"""

    def setUp(self):
        """測試初始化"""
        self.api_key = os.getenv('CLAUDE_API_KEY')
        if not self.api_key:
            self.skipTest("CLAUDE_API_KEY not set in environment")

    def test_real_api_call_requirements_analysis(self):
        """測試真實 API 調用 - 需求分析"""
        if ClaudeService is None:
            self.skipTest("ClaudeService not available")

        service = ClaudeService(api_key=self.api_key, model="haiku")  # 使用 haiku 節省成本

        requirements = """
        Create a simple task management system.
        Each task has:
        - Title (required, max 100 chars)
        - Description (optional)
        - Status (todo, in_progress, done)
        - Created date

        CRUD operations: list, create, update, delete
        """

        try:
            feature_spec = service.analyze_requirements(requirements)

            # 驗證結果結構
            self.assertIn("name", feature_spec)
            self.assertIn("entity", feature_spec)
            self.assertIn("endpoints", feature_spec)
            self.assertIn("fields", feature_spec)

            print("\n=== Requirements Analysis Result ===")
            print(json.dumps(feature_spec, indent=2, ensure_ascii=False))

        except Exception as e:
            self.fail(f"Real API call failed: {e}")


def run_tests():
    """運行所有測試"""
    print("=" * 70)
    print("Claude Service Tests")
    print("=" * 70)
    print()

    # 檢查 API Key
    api_key = os.getenv('CLAUDE_API_KEY')
    if api_key:
        print("✅ CLAUDE_API_KEY found - will run integration tests")
    else:
        print("⚠️  CLAUDE_API_KEY not set - skipping integration tests")
    print()

    # 創建測試套件
    loader = unittest.TestLoader()
    suite = unittest.TestSuite()

    # 添加測試
    suite.addTests(loader.loadTestsFromTestCase(TestClaudeService))
    suite.addTests(loader.loadTestsFromTestCase(TestClaudeServiceIntegration))

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

    return result.wasSuccessful()


if __name__ == '__main__':
    success = run_tests()
    sys.exit(0 if success else 1)
