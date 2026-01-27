#!/usr/bin/env python3
"""
SmartAdmin Auto-Coding - Workflow Orchestrator
工作流編排器 - 統一管理所有 Crews 的執行

功能：
- 統一管理 3 個 Crews（Analyzer, Developer, QA）
- 實現工作流串接（Analyzer → Developer → QA）
- 提供單個 Crew 觸發接口
- 審計日誌和錯誤處理

作者: SmartAdmin Auto-Coding System
版本: 1.0.0
日期: 2026-01-28
"""

import sys
import os
import logging
from typing import Dict, Any, Optional
from datetime import datetime

# 添加項目根路徑到 Python Path
sys.path.insert(0, os.path.dirname(__file__))

# 導入 Crews
from crews.analyzer_crew import AnalyzerCrew
from crews.developer_crew import DeveloperCrew
from crews.qa_crew import QACrew

# ============================================================================
# 日誌配置
# ============================================================================

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# ============================================================================
# Workflow Orchestrator
# ============================================================================

class WorkflowOrchestrator:
    """
    工作流編排器

    職責：
    1. 管理所有 Crews 的生命週期
    2. 協調多個 Crews 的執行順序
    3. 實現完整工作流（Analyzer → Developer → QA）
    4. 提供統一的錯誤處理和日誌記錄
    """

    def __init__(self):
        """
        初始化 Orchestrator

        創建所有 Crews 實例並註冊到 Registry
        """
        logger.info("Initializing WorkflowOrchestrator...")

        try:
            # 創建 Crews（延遲初始化，避免啟動時開銷）
            self.crews = {
                'analyzer': None,
                'developer': None,
                'qa': None
            }

            logger.info("WorkflowOrchestrator initialized successfully")

        except Exception as e:
            logger.error(f"Failed to initialize WorkflowOrchestrator: {e}")
            raise

    def _get_crew(self, crew_name: str):
        """
        獲取或創建 Crew 實例（延遲初始化）

        Args:
            crew_name: Crew 名稱（analyzer, developer, qa）

        Returns:
            Crew 實例
        """
        if self.crews[crew_name] is None:
            logger.info(f"Lazy-loading crew: {crew_name}")

            if crew_name == 'analyzer':
                self.crews[crew_name] = AnalyzerCrew()
            elif crew_name == 'developer':
                self.crews[crew_name] = DeveloperCrew()
            elif crew_name == 'qa':
                self.crews[crew_name] = QACrew()

        return self.crews[crew_name]

    # ========================================================================
    # 工作流執行方法
    # ========================================================================

    def trigger_workflow(
        self,
        workflow_type: str,
        params: Dict[str, Any]
    ) -> Dict[str, Any]:
        """
        觸發完整工作流

        Args:
            workflow_type: 工作流類型
                - 'full_feature': 完整功能開發（Analyzer → Developer → QA）
                - 'code_review': 僅代碼審查（QA）
                - 'analysis_only': 僅代碼分析（Analyzer）

            params: 工作流參數
                {
                    "name": "Employee Management",
                    "entity": "Employee",
                    "target": "sa-admin",
                    "validation": {...}
                }

        Returns:
            Dict[str, Any]: 工作流執行結果
                {
                    "status": "SUCCESS",
                    "workflow_type": "full_feature",
                    "analysis": {...},
                    "development": {...},
                    "qa": {...},
                    "duration": "5m30s"
                }
        """
        start_time = datetime.now()
        logger.info(f"Starting workflow: {workflow_type}")
        logger.info(f"Parameters: {params}")

        try:
            if workflow_type == 'full_feature':
                return self._run_full_feature_workflow(params)

            elif workflow_type == 'code_review':
                return self._run_code_review_workflow(params)

            elif workflow_type == 'analysis_only':
                return self._run_analysis_workflow(params)

            else:
                raise ValueError(f"Unknown workflow type: {workflow_type}")

        except Exception as e:
            logger.error(f"Workflow execution failed: {e}", exc_info=True)

            duration = datetime.now() - start_time
            duration_str = f"{int(duration.total_seconds() // 60)}m {int(duration.total_seconds() % 60)}s"

            return {
                "status": "FAILED",
                "workflow_type": workflow_type,
                "error": str(e),
                "duration": duration_str,
                "timestamp": datetime.now().isoformat()
            }

    def trigger_crew(
        self,
        crew_name: str,
        params: Dict[str, Any]
    ) -> Dict[str, Any]:
        """
        觸發單個 Crew 執行

        Args:
            crew_name: Crew 名稱（analyzer, developer, qa）
            params: Crew 執行參數

        Returns:
            Dict[str, Any]: Crew 執行結果
        """
        logger.info(f"Triggering crew: {crew_name}")
        logger.info(f"Parameters: {params}")

        try:
            crew = self._get_crew(crew_name)

            if not crew:
                raise ValueError(f"Unknown crew: {crew_name}")

            # 調用 Crew 的 run 方法
            result = crew.run(**params)

            logger.info(f"Crew {crew_name} completed successfully")
            return result

        except Exception as e:
            logger.error(f"Crew {crew_name} execution failed: {e}", exc_info=True)

            return {
                "status": "FAILED",
                "crew_name": crew_name,
                "error": str(e),
                "timestamp": datetime.now().isoformat()
            }

    # ========================================================================
    # 內部工作流實現
    # ========================================================================

    def _run_full_feature_workflow(self, params: Dict[str, Any]) -> Dict[str, Any]:
        """
        執行完整功能開發工作流（Analyzer → Developer → QA）

        Args:
            params: {
                "name": "Employee Management",
                "entity": "Employee",
                "target": "sa-admin",
                "validation": {...}
            }

        Returns:
            Dict[str, Any]: 工作流結果
        """
        start_time = datetime.now()
        logger.info("=== Full Feature Workflow ===")

        # 步驟 1: Analyzer Crew - 分析現有代碼
        logger.info("Step 1/3: Running Analyzer Crew...")
        analyzer_crew = self._get_crew('analyzer')
        analysis_result = analyzer_crew.run(target=params.get('target', 'sa-admin'))

        logger.info(f"Analyzer Crew completed: {analysis_result.get('status')}")

        # 步驟 2: Developer Crew - 生成新代碼
        logger.info("Step 2/3: Running Developer Crew...")

        feature_spec = {
            "name": params['name'],
            "entity": params['entity'],
            "endpoints": params.get('endpoints', ['list', 'add', 'update', 'delete']),
            "views": params.get('views', ['list', 'form']),
            "components": params.get('components', ['table', 'form-modal']),
            "validation": params.get('validation', {}),
            "environment": params.get('environment', 'dev/staging/prod'),
            "dependencies": params.get('dependencies', [])
        }

        developer_crew = self._get_crew('developer')
        development_result = developer_crew.run(feature_spec=feature_spec)

        logger.info(f"Developer Crew completed: {development_result.get('status')}")

        # 步驟 3: QA Crew - 質量檢查
        logger.info("Step 3/3: Running QA Crew...")

        # 從 PR URL 提取 PR 編號
        pr_url = development_result.get('pr_url', '')
        pr_number = None

        if pr_url:
            try:
                pr_number = int(pr_url.split('/')[-1])
            except (ValueError, IndexError):
                logger.warning(f"Failed to extract PR number from URL: {pr_url}")
                pr_number = 999  # 使用占位符

        if pr_number:
            qa_crew = self._get_crew('qa')
            qa_result = qa_crew.run(
                pr_number=pr_number,
                target=params.get('target', 'sa-admin')
            )
            logger.info(f"QA Crew completed: {qa_result.get('status')}")
        else:
            logger.warning("Skipping QA Crew: No PR number available")
            qa_result = {
                "status": "SKIPPED",
                "reason": "No PR number available"
            }

        # 計算執行時長
        duration = datetime.now() - start_time
        duration_str = f"{int(duration.total_seconds() // 60)}m {int(duration.total_seconds() % 60)}s"

        # 返回完整結果
        return {
            "status": "SUCCESS",
            "workflow_type": "full_feature",
            "feature_name": params['name'],
            "steps": {
                "1_analysis": analysis_result,
                "2_development": development_result,
                "3_qa": qa_result
            },
            "pr_url": development_result.get('pr_url'),
            "branch_name": development_result.get('branch_name'),
            "duration": duration_str,
            "timestamp": datetime.now().isoformat()
        }

    def _run_code_review_workflow(self, params: Dict[str, Any]) -> Dict[str, Any]:
        """
        執行代碼審查工作流（僅 QA Crew）

        Args:
            params: {
                "pr_number": 123,
                "target": "sa-admin"
            }

        Returns:
            Dict[str, Any]: 審查結果
        """
        start_time = datetime.now()
        logger.info("=== Code Review Workflow ===")

        qa_crew = self._get_crew('qa')
        qa_result = qa_crew.run(
            pr_number=params['pr_number'],
            target=params.get('target', 'sa-admin')
        )

        duration = datetime.now() - start_time
        duration_str = f"{int(duration.total_seconds() // 60)}m {int(duration.total_seconds() % 60)}s"

        return {
            "status": "SUCCESS",
            "workflow_type": "code_review",
            "qa_result": qa_result,
            "duration": duration_str,
            "timestamp": datetime.now().isoformat()
        }

    def _run_analysis_workflow(self, params: Dict[str, Any]) -> Dict[str, Any]:
        """
        執行代碼分析工作流（僅 Analyzer Crew）

        Args:
            params: {
                "target": "sa-admin"
            }

        Returns:
            Dict[str, Any]: 分析結果
        """
        start_time = datetime.now()
        logger.info("=== Analysis Workflow ===")

        analyzer_crew = self._get_crew('analyzer')
        analysis_result = analyzer_crew.run(target=params['target'])

        duration = datetime.now() - start_time
        duration_str = f"{int(duration.total_seconds() // 60)}m {int(duration.total_seconds() % 60)}s"

        return {
            "status": "SUCCESS",
            "workflow_type": "analysis_only",
            "analysis_result": analysis_result,
            "duration": duration_str,
            "timestamp": datetime.now().isoformat()
        }

    # ========================================================================
    # 清理方法
    # ========================================================================

    def cleanup(self) -> None:
        """
        清理資源（關閉所有 Crews）
        """
        logger.info("Cleaning up WorkflowOrchestrator...")

        for crew_name, crew in self.crews.items():
            if crew:
                try:
                    crew.cleanup()
                    logger.info(f"Crew {crew_name} cleaned up")
                except Exception as e:
                    logger.error(f"Failed to cleanup crew {crew_name}: {e}")

        logger.info("WorkflowOrchestrator cleanup completed")

    def __del__(self):
        """析構函數，確保資源清理"""
        self.cleanup()

# ============================================================================
# 命令行接口
# ============================================================================

def main():
    """
    命令行主函數

    用法：
        python3 orchestrator.py full_feature --name "Employee Management" --entity "Employee"
        python3 orchestrator.py code_review --pr-number 123
        python3 orchestrator.py analysis --target sa-admin
    """
    import argparse
    import json

    parser = argparse.ArgumentParser(description='SmartAdmin Workflow Orchestrator')
    parser.add_argument(
        'workflow_type',
        type=str,
        choices=['full_feature', 'code_review', 'analysis_only'],
        help='Workflow type'
    )
    parser.add_argument('--name', type=str, help='Feature name')
    parser.add_argument('--entity', type=str, help='Entity name')
    parser.add_argument('--pr-number', type=int, help='PR number')
    parser.add_argument('--target', type=str, default='sa-admin', help='Target module')

    args = parser.parse_args()

    # 構建參數
    params = {
        'target': args.target
    }

    if args.workflow_type == 'full_feature':
        if not args.name or not args.entity:
            parser.error('--name and --entity are required for full_feature workflow')
        params['name'] = args.name
        params['entity'] = args.entity

    elif args.workflow_type == 'code_review':
        if not args.pr_number:
            parser.error('--pr-number is required for code_review workflow')
        params['pr_number'] = args.pr_number

    # 創建並運行 Orchestrator
    orchestrator = WorkflowOrchestrator()

    try:
        result = orchestrator.trigger_workflow(
            workflow_type=args.workflow_type,
            params=params
        )

        # 打印結果
        print("\n" + "=" * 70)
        print("Workflow Orchestrator Results")
        print("=" * 70)
        print(json.dumps(result, indent=2, ensure_ascii=False))
        print("=" * 70)

    finally:
        # 清理資源
        orchestrator.cleanup()

if __name__ == '__main__':
    main()
