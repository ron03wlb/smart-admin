#!/usr/bin/env python3
"""
SmartAdmin Auto-Coding - Analyzer Crew
代碼分析工作流 Crew

功能：
- Java 架構分析（Java Architect Agent）
- 數據庫性能分析（PostgreSQL Pro Agent）
- 代碼質量檢查（Code Reviewer Agent）

作者: SmartAdmin Auto-Coding System
版本: 1.0.0
日期: 2026-01-27
"""

import os
import json
import time
from typing import Dict, Any, List, Optional
from datetime import datetime

# 導入 CrewAI
from crewai import Crew, Agent, Task, Process

# 導入基礎類和工具
import sys
sys.path.append(os.path.join(os.path.dirname(__file__), 'common'))
from base_crew import BaseCrew
from tools import SafeFileAccessTool, CodeAnalysisTool, DatabaseQueryTool

# ============================================================================
# Analyzer Crew 實現
# ============================================================================

class AnalyzerCrew(BaseCrew):
    """
    Analyzer Crew

    執行代碼分析工作流：
    1. Java 架構分析
    2. 數據庫性能分析
    3. 代碼質量檢查
    """

    def __init__(self):
        """初始化 Analyzer Crew"""
        super().__init__(crew_name="analyzer-crew", crew_type="analyzer")

        # 初始化工具
        self.file_tool = SafeFileAccessTool(
            db_connection_string=os.getenv('DB_CONNECTION_STRING', ''),
            agent_name="analyzer-crew"
        )
        self.code_tool = CodeAnalysisTool(
            project_root=os.getenv('PROJECT_ROOT', os.getcwd())
        )
        self.db_tool = DatabaseQueryTool(
            db_connection_string=os.getenv('DB_CONNECTION_STRING', '')
        )

        # 創建 Agents
        self.java_architect_agent = self._create_java_architect_agent()
        self.postgres_pro_agent = self._create_postgres_pro_agent()
        self.code_reviewer_agent = self._create_code_reviewer_agent()

    # ========================================================================
    # Agent 創建方法
    # ========================================================================

    def _create_java_architect_agent(self) -> Agent:
        """
        創建 Java Architect Agent

        職責：分析 Java 代碼架構，檢測架構違規
        """
        return Agent(
            role="Java Architect",
            goal="Analyze Java code architecture and detect architectural violations",
            backstory="""You are an expert Java architect specializing in SmartAdmin's
            layered architecture (Controller → Service → Manager → Dao). You enforce
            architectural rules using ArchUnit and ensure code follows SmartAdmin patterns.""",
            verbose=True,
            allow_delegation=False,
            tools=[]  # 工具將在 Task 中提供
        )

    def _create_postgres_pro_agent(self) -> Agent:
        """
        創建 PostgreSQL Pro Agent

        職責：分析數據庫查詢性能，檢測 N+1 查詢問題
        """
        return Agent(
            role="PostgreSQL Expert",
            goal="Analyze database queries and identify performance bottlenecks",
            backstory="""You are a PostgreSQL performance expert. You analyze query
            execution plans, detect N+1 queries, suggest indexes, and optimize slow queries.""",
            verbose=True,
            allow_delegation=False,
            tools=[]
        )

    def _create_code_reviewer_agent(self) -> Agent:
        """
        創建 Code Reviewer Agent

        職責：執行靜態代碼分析（Checkstyle, PMD, SpotBugs）
        """
        return Agent(
            role="Code Reviewer",
            goal="Perform static code analysis and ensure code quality standards",
            backstory="""You are a meticulous code reviewer. You run Checkstyle, PMD,
            and SpotBugs to detect code smells, potential bugs, and style violations.""",
            verbose=True,
            allow_delegation=False,
            tools=[]
        )

    # ========================================================================
    # Task 創建方法
    # ========================================================================

    def _create_architecture_analysis_task(self, target_module: str) -> Task:
        """
        創建架構分析 Task

        Args:
            target_module: 目標模塊（例如：employee）

        Returns:
            Task: 架構分析任務
        """
        description = f"""
        Analyze the architecture of the '{target_module}' module:

        1. Run ArchUnit tests to check for architectural violations
        2. Verify layered architecture (Controller → Service → Manager → Dao)
        3. Check for forbidden patterns (@Autowired field injection, etc.)
        4. Validate transaction annotations (@Transactional in Manager only)
        5. Check Vavr Option usage in Service layer

        Use the CodeAnalysisTool to run ArchUnit tests.

        Target module: {target_module}
        """

        expected_output = """
        Architecture analysis report including:
        - ArchUnit test results (pass/fail)
        - List of architectural violations (if any)
        - Recommendations for fixing violations
        """

        return Task(
            description=description,
            expected_output=expected_output,
            agent=self.java_architect_agent
        )

    def _create_database_analysis_task(self, target_module: str) -> Task:
        """
        創建數據庫分析 Task

        Args:
            target_module: 目標模塊

        Returns:
            Task: 數據庫分析任務
        """
        description = f"""
        Analyze database queries in the '{target_module}' module:

        1. Identify slow queries (execution time > 1000ms)
        2. Detect N+1 query problems
        3. Analyze query execution plans
        4. Suggest missing indexes
        5. Check for inefficient joins

        Use the DatabaseQueryTool to analyze queries.

        Target module: {target_module}
        """

        expected_output = """
        Database analysis report including:
        - List of slow queries with execution times
        - N+1 query detections
        - Index recommendations
        - Query optimization suggestions
        """

        return Task(
            description=description,
            expected_output=expected_output,
            agent=self.postgres_pro_agent
        )

    def _create_code_quality_task(self, target_module: str) -> Task:
        """
        創建代碼質量檢查 Task

        Args:
            target_module: 目標模塊

        Returns:
            Task: 代碼質量檢查任務
        """
        description = f"""
        Run static code analysis on the '{target_module}' module:

        1. Run Checkstyle to check code style violations
        2. Run PMD to detect code smells
        3. Run SpotBugs to find potential bugs
        4. Summarize all quality issues by severity
        5. Provide recommendations for high-priority issues

        Use the CodeAnalysisTool to run all checks.

        Target module: {target_module}
        """

        expected_output = """
        Code quality report including:
        - Checkstyle violations count and details
        - PMD issues count and details
        - SpotBugs warnings count and details
        - Summary by severity (Critical, High, Medium, Low)
        - Top 10 issues to fix first
        """

        return Task(
            description=description,
            expected_output=expected_output,
            agent=self.code_reviewer_agent
        )

    # ========================================================================
    # Crew 創建方法（實現抽象方法）
    # ========================================================================

    def create_crew(self, target_module: str) -> Crew:
        """
        創建 Analyzer Crew 實例

        Args:
            target_module: 目標模塊

        Returns:
            Crew: CrewAI Crew 實例
        """
        # 創建 Tasks
        architecture_task = self._create_architecture_analysis_task(target_module)
        database_task = self._create_database_analysis_task(target_module)
        quality_task = self._create_code_quality_task(target_module)

        # 創建 Crew（順序執行）
        crew = Crew(
            agents=[
                self.java_architect_agent,
                self.postgres_pro_agent,
                self.code_reviewer_agent
            ],
            tasks=[
                architecture_task,
                database_task,
                quality_task
            ],
            process=Process.sequential,  # 順序執行
            verbose=True
        )

        return crew

    # ========================================================================
    # 運行方法（實現抽象方法）
    # ========================================================================

    def run(self, target_module: str = "sa-admin") -> Dict[str, Any]:
        """
        運行 Analyzer Crew

        Args:
            target_module: 目標模塊（默認：sa-admin）

        Returns:
            Dict[str, Any]: 分析結果
        """
        start_time = time.time()

        # 記錄執行開始
        execution_id = self.log_execution_start({
            "target_module": target_module,
            "crew_type": "analyzer"
        })

        try:
            self.logger.info(f"Starting analyzer crew for module: {target_module}")

            # 手動執行分析（不使用 CrewAI，因為工具集成複雜）
            results = self._run_analysis_manually(target_module)

            # 計算執行時長
            duration = time.time() - start_time
            duration_str = f"{int(duration // 60)}m {int(duration % 60)}s"

            # 記錄執行完成
            self.log_execution_complete(
                execution_id=execution_id,
                status='SUCCESS',
                output_result=results
            )

            # 發送 Telegram 通知
            notification = self.format_execution_notification(
                status='SUCCESS',
                duration=duration_str,
                summary=f"Analysis completed for module: {target_module}",
                details=[
                    f"Architecture violations: {results['architecture']['violations_count']}",
                    f"Slow queries: {results['database']['slow_queries_count']}",
                    f"Code quality issues: {results['code_quality']['total_issues']}"
                ]
            )
            self.send_telegram_notification(notification)

            return {
                "status": "SUCCESS",
                "execution_id": execution_id,
                "target_module": target_module,
                "duration": duration_str,
                "results": results,
                "timestamp": datetime.now().isoformat()
            }

        except Exception as e:
            return self.handle_error(execution_id, e, "Analyzer crew execution failed")

    # ========================================================================
    # 內部執行方法
    # ========================================================================

    def _run_analysis_manually(self, target_module: str) -> Dict[str, Any]:
        """
        手動運行分析（不使用 CrewAI）

        Args:
            target_module: 目標模塊

        Returns:
            Dict[str, Any]: 分析結果
        """
        results = {}

        # 1. 架構分析
        self.logger.info("Running architecture analysis...")
        archunit_result = self.code_tool.run_archunit_tests(target_module)
        results['architecture'] = {
            "success": archunit_result['success'],
            "violations_count": 0 if archunit_result['success'] else 1,
            "output": archunit_result.get('output', ''),
            "errors": archunit_result.get('errors', '')
        }

        # 2. 數據庫分析
        self.logger.info("Running database analysis...")
        slow_queries = self.db_tool.check_slow_queries(min_duration_ms=1000)
        results['database'] = {
            "slow_queries_count": len(slow_queries),
            "slow_queries": slow_queries[:5]  # 只返回前 5 個
        }

        # 3. 代碼質量檢查
        self.logger.info("Running code quality checks...")
        quality_results = self.code_tool.run_all_checks(target_module)

        total_issues = 0
        for result in quality_results:
            if not result['success']:
                total_issues += 1

        results['code_quality'] = {
            "total_issues": total_issues,
            "checkstyle": quality_results[0],
            "pmd": quality_results[1],
            "spotbugs": quality_results[2],
            "archunit": quality_results[3]
        }

        return results

# ============================================================================
# 命令行接口
# ============================================================================

def main():
    """
    命令行主函數

    用法：
        python3 analyzer_crew.py --target sa-admin
    """
    import argparse

    parser = argparse.ArgumentParser(description='SmartAdmin Analyzer Crew')
    parser.add_argument(
        '--target',
        type=str,
        default='sa-admin',
        help='Target module to analyze (default: sa-admin)'
    )
    args = parser.parse_args()

    # 創建並運行 Crew
    crew = AnalyzerCrew()
    result = crew.run(target_module=args.target)

    # 打印結果
    print("\n" + "=" * 70)
    print("Analyzer Crew Results")
    print("=" * 70)
    print(json.dumps(result, indent=2, ensure_ascii=False))
    print("=" * 70)

    # 清理資源
    crew.cleanup()

if __name__ == '__main__':
    main()
