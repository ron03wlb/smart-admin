#!/usr/bin/env python3
"""
SmartAdmin Auto-Coding - Telegram Command Handler
Telegram 命令處理器 - 將 Telegram 命令轉換為 Workflow 觸發

功能：
- 解析 Telegram 命令和參數
- 調用 WorkflowOrchestrator 觸發工作流
- 格式化結果消息返回給用戶

作者: SmartAdmin Auto-Coding System
版本: 1.0.0
日期: 2026-01-28
"""

import sys
import os
import logging
from typing import Dict, Any, Optional

# 添加父目錄到 Python Path
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

# 導入 Orchestrator
from orchestrator import WorkflowOrchestrator

# ============================================================================
# 日誌配置
# ============================================================================

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# ============================================================================
# Telegram Command Handler
# ============================================================================

class TelegramCommandHandler:
    """
    Telegram 命令處理器

    職責：
    1. 解析 Telegram 命令和參數
    2. 驗證參數有效性
    3. 調用 WorkflowOrchestrator 觸發工作流
    4. 格式化執行結果為 Markdown 消息

    支持的命令：
    - /new_feature <name> <entity> - 創建新功能（完整工作流）
    - /code_review <pr_number> - 代碼審查
    - /analyze <target> - 代碼分析
    - /help - 顯示幫助信息
    """

    def __init__(self):
        """
        初始化 Command Handler
        """
        logger.info("Initializing TelegramCommandHandler...")
        self.orchestrator = WorkflowOrchestrator()
        logger.info("TelegramCommandHandler initialized successfully")

    # ========================================================================
    # 命令處理方法
    # ========================================================================

    def handle_command(self, command: str, args: Dict[str, Any]) -> str:
        """
        處理 Telegram 命令

        Args:
            command: 命令名稱（例如：/new_feature, /code_review）
            args: 命令參數
                {
                    "name": "Employee Management",
                    "entity": "Employee",
                    "pr_number": 123,
                    "target": "sa-admin"
                }

        Returns:
            str: 格式化的 Markdown 消息
        """
        logger.info(f"Handling command: {command}")
        logger.info(f"Arguments: {args}")

        try:
            # 根據命令類型分發處理
            if command == '/new_feature':
                return self._handle_new_feature(args)

            elif command == '/code_review':
                return self._handle_code_review(args)

            elif command == '/analyze':
                return self._handle_analyze(args)

            elif command == '/help':
                return self._handle_help()

            else:
                return self._format_error_message(
                    command=command,
                    error=f"Unknown command: {command}"
                )

        except Exception as e:
            logger.error(f"Error handling command {command}: {e}", exc_info=True)
            return self._format_error_message(
                command=command,
                error=str(e)
            )

    # ========================================================================
    # 命令處理實現
    # ========================================================================

    def _handle_new_feature(self, args: Dict[str, Any]) -> str:
        """
        處理 /new_feature 命令

        Args:
            args: {
                "name": "Employee Management",
                "entity": "Employee",
                "target": "sa-admin" (可選)
            }

        Returns:
            str: Markdown 格式的結果消息
        """
        # 驗證參數
        if 'name' not in args or 'entity' not in args:
            return self._format_error_message(
                command='/new_feature',
                error="Missing required arguments: 'name' and 'entity'"
            )

        logger.info(f"Creating new feature: {args['name']}")

        # 構建工作流參數
        params = {
            'name': args['name'],
            'entity': args['entity'],
            'target': args.get('target', 'sa-admin'),
            'endpoints': args.get('endpoints', ['list', 'add', 'update', 'delete']),
            'views': args.get('views', ['list', 'form']),
            'validation': args.get('validation', {})
        }

        # 觸發完整工作流
        result = self.orchestrator.trigger_workflow(
            workflow_type='full_feature',
            params=params
        )

        # 格式化結果消息
        if result['status'] == 'SUCCESS':
            return self._format_success_message(
                command='/new_feature',
                feature_name=args['name'],
                result=result
            )
        else:
            return self._format_error_message(
                command='/new_feature',
                error=result.get('error', 'Unknown error')
            )

    def _handle_code_review(self, args: Dict[str, Any]) -> str:
        """
        處理 /code_review 命令

        Args:
            args: {
                "pr_number": 123,
                "target": "sa-admin" (可選)
            }

        Returns:
            str: Markdown 格式的結果消息
        """
        # 驗證參數
        if 'pr_number' not in args:
            return self._format_error_message(
                command='/code_review',
                error="Missing required argument: 'pr_number'"
            )

        logger.info(f"Running code review for PR #{args['pr_number']}")

        # 構建工作流參數
        params = {
            'pr_number': args['pr_number'],
            'target': args.get('target', 'sa-admin')
        }

        # 觸發代碼審查工作流
        result = self.orchestrator.trigger_workflow(
            workflow_type='code_review',
            params=params
        )

        # 格式化結果消息
        if result['status'] == 'SUCCESS':
            qa_result = result.get('qa_result', {})
            decision = qa_result.get('decision', 'UNKNOWN')
            emoji = "✅" if decision == "APPROVED" else "❌"

            return f"""
{emoji} *Code Review Completed*

*PR*: #{args['pr_number']}
*Decision*: {decision}
*Duration*: {result.get('duration', 'N/A')}

*QA Report*:
{qa_result.get('summary', 'No summary available')}

*Timestamp*: {result.get('timestamp', 'N/A')}
"""
        else:
            return self._format_error_message(
                command='/code_review',
                error=result.get('error', 'Unknown error')
            )

    def _handle_analyze(self, args: Dict[str, Any]) -> str:
        """
        處理 /analyze 命令

        Args:
            args: {
                "target": "sa-admin"
            }

        Returns:
            str: Markdown 格式的結果消息
        """
        target = args.get('target', 'sa-admin')
        logger.info(f"Running code analysis for: {target}")

        # 構建工作流參數
        params = {
            'target': target
        }

        # 觸發分析工作流
        result = self.orchestrator.trigger_workflow(
            workflow_type='analysis_only',
            params=params
        )

        # 格式化結果消息
        if result['status'] == 'SUCCESS':
            analysis_result = result.get('analysis_result', {})

            return f"""
✅ *Code Analysis Completed*

*Target*: {target}
*Duration*: {result.get('duration', 'N/A')}

*Analysis Summary*:
{analysis_result.get('summary', 'No summary available')}

*Timestamp*: {result.get('timestamp', 'N/A')}
"""
        else:
            return self._format_error_message(
                command='/analyze',
                error=result.get('error', 'Unknown error')
            )

    def _handle_help(self) -> str:
        """
        處理 /help 命令

        Returns:
            str: 幫助信息（Markdown 格式）
        """
        return """
🤖 *SmartAdmin Auto-Coding Bot*

*Available Commands*:

1. `/new_feature` - Create new feature (full workflow)
   Usage: `/new_feature <name> <entity>`
   Example: `/new_feature "Employee Management" Employee`

2. `/code_review` - Review pull request
   Usage: `/code_review <pr_number>`
   Example: `/code_review 123`

3. `/analyze` - Analyze codebase
   Usage: `/analyze <target>`
   Example: `/analyze sa-admin`

4. `/help` - Show this help message

*Full Workflow*:
Analyzer → Developer (Claude AI) → QA → Create PR → Notify

*Support*:
- GitHub: https://github.com/1024-lab/smart-admin
- Documentation: See project README.md
"""

    # ========================================================================
    # 消息格式化方法
    # ========================================================================

    def _format_success_message(
        self,
        command: str,
        feature_name: str,
        result: Dict[str, Any]
    ) -> str:
        """
        格式化成功消息

        Args:
            command: 命令名稱
            feature_name: 功能名稱
            result: 工作流執行結果

        Returns:
            str: Markdown 格式的消息
        """
        pr_url = result.get('pr_url', 'N/A')
        branch_name = result.get('branch_name', 'N/A')
        duration = result.get('duration', 'N/A')

        # 獲取各步驟狀態
        steps = result.get('steps', {})
        analysis_status = steps.get('1_analysis', {}).get('status', 'UNKNOWN')
        development_status = steps.get('2_development', {}).get('status', 'UNKNOWN')
        qa_status = steps.get('3_qa', {}).get('status', 'UNKNOWN')

        message = f"""
✅ *Feature Created Successfully*

*Command*: `{command}`
*Feature*: {feature_name}
*Duration*: {duration}

*Workflow Steps*:
• Step 1: Analyzer - {analysis_status}
• Step 2: Developer - {development_status}
• Step 3: QA - {qa_status}

*Output*:
• Branch: `{branch_name}`
• PR: {pr_url}

*Next Steps*:
1. Review the generated code
2. Run tests locally
3. Approve and merge the PR

*Timestamp*: {result.get('timestamp', 'N/A')}

🤖 Generated with SmartAdmin Auto-Coding System
"""
        return message.strip()

    def _format_error_message(self, command: str, error: str) -> str:
        """
        格式化錯誤消息

        Args:
            command: 命令名稱
            error: 錯誤描述

        Returns:
            str: Markdown 格式的錯誤消息
        """
        message = f"""
❌ *Command Failed*

*Command*: `{command}`
*Error*: {error}

*Troubleshooting*:
- Check if all required arguments are provided
- Verify the command syntax
- Use `/help` to see available commands

*Support*:
If this issue persists, please check the logs or contact support.
"""
        return message.strip()

    # ========================================================================
    # 清理方法
    # ========================================================================

    def cleanup(self) -> None:
        """
        清理資源
        """
        logger.info("Cleaning up TelegramCommandHandler...")
        self.orchestrator.cleanup()
        logger.info("TelegramCommandHandler cleanup completed")

    def __del__(self):
        """析構函數，確保資源清理"""
        self.cleanup()

# ============================================================================
# 測試主函數
# ============================================================================

if __name__ == '__main__':
    """
    測試 Command Handler

    用法：
        python3 command_handler.py
    """
    import json

    # 創建 Handler
    handler = TelegramCommandHandler()

    # 測試 /help 命令
    print("\n" + "=" * 70)
    print("Test 1: /help command")
    print("=" * 70)
    result = handler.handle_command('/help', {})
    print(result)

    # 測試 /new_feature 命令（模擬，不真正執行）
    print("\n" + "=" * 70)
    print("Test 2: /new_feature command (validation only)")
    print("=" * 70)
    result = handler.handle_command('/new_feature', {
        'name': 'Test Feature',
        'entity': 'TestEntity',
        'target': 'sa-admin'
    })
    print(result)

    # 清理資源
    handler.cleanup()

    print("\n" + "=" * 70)
    print("Tests completed")
    print("=" * 70)
