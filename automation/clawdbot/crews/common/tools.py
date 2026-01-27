#!/usr/bin/env python3
"""
SmartAdmin Auto-Coding - Shared Tools for CrewAI
CrewAI Crew 使用的共享工具集

作者: SmartAdmin Auto-Coding System
版本: 1.0.0
日期: 2026-01-27
"""

import os
import logging
import subprocess
import json
from typing import Dict, Any, List, Optional
from pathlib import Path
import requests

# 導入文件訪問控制
import sys
sys.path.append(os.path.join(os.path.dirname(__file__), '..', '..', 'tools'))
from file_access_guard import FileAccessGuard, Operation

# ============================================================================
# 日誌配置
# ============================================================================

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# ============================================================================
# 文件訪問工具
# ============================================================================

class SafeFileAccessTool:
    """
    安全文件訪問工具

    封裝 FileAccessGuard，提供對 CrewAI Agent 的文件訪問控制。
    """

    def __init__(self, db_connection_string: str, agent_name: str):
        """
        初始化文件訪問工具

        Args:
            db_connection_string: PostgreSQL 連接字符串
            agent_name: Agent 名稱
        """
        self.guard = FileAccessGuard(db_connection_string)
        self.agent_name = agent_name

    def read_file(self, file_path: str) -> Optional[str]:
        """
        安全讀取文件

        Args:
            file_path: 文件路徑

        Returns:
            Optional[str]: 文件內容（如果允許訪問），否則 None
        """
        # 檢查訪問權限
        result = self.guard.check_access(file_path, Operation.READ, self.agent_name)

        # 記錄訪問決策
        self.guard.log_access_decision(
            file_path=file_path,
            operation=Operation.READ,
            decision=result.decision,
            agent_name=self.agent_name,
            reason=result.reason
        )

        # 如果拒絕訪問，返回 None
        if result.decision.value == 'DENY':
            logger.warning(f"Access denied for {self.agent_name}: {file_path}")
            return None

        # 如果需要審計，記錄並返回 None（需要人工審批）
        if result.decision.value == 'AUDIT':
            logger.info(f"Audit required for {self.agent_name}: {file_path}")
            return None

        # 允許訪問，讀取文件
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
            logger.info(f"File read successfully: {file_path}")
            return content
        except Exception as e:
            logger.error(f"Error reading file {file_path}: {e}")
            return None

    def write_file(self, file_path: str, content: str) -> bool:
        """
        安全寫入文件

        Args:
            file_path: 文件路徑
            content: 文件內容

        Returns:
            bool: 是否寫入成功
        """
        # 檢查訪問權限
        result = self.guard.check_access(file_path, Operation.WRITE, self.agent_name)

        # 記錄訪問決策
        self.guard.log_access_decision(
            file_path=file_path,
            operation=Operation.WRITE,
            decision=result.decision,
            agent_name=self.agent_name,
            reason=result.reason
        )

        # 如果拒絕訪問，返回 False
        if result.decision.value == 'DENY':
            logger.warning(f"Write access denied for {self.agent_name}: {file_path}")
            return False

        # 如果需要審計，記錄並返回 False（需要人工審批）
        if result.decision.value == 'AUDIT':
            logger.info(f"Audit required for write by {self.agent_name}: {file_path}")
            return False

        # 允許訪問，寫入文件
        try:
            # 確保目錄存在
            os.makedirs(os.path.dirname(file_path), exist_ok=True)

            with open(file_path, 'w', encoding='utf-8') as f:
                f.write(content)
            logger.info(f"File written successfully: {file_path}")
            return True
        except Exception as e:
            logger.error(f"Error writing file {file_path}: {e}")
            return False

# ============================================================================
# 代碼分析工具
# ============================================================================

class CodeAnalysisTool:
    """
    代碼分析工具

    提供靜態代碼分析功能（Checkstyle, PMD, SpotBugs, ArchUnit）。
    """

    def __init__(self, project_root: str):
        """
        初始化代碼分析工具

        Args:
            project_root: 項目根目錄
        """
        self.project_root = project_root

    def run_checkstyle(self, target_module: str = "sa-admin") -> Dict[str, Any]:
        """
        運行 Checkstyle 檢查

        Args:
            target_module: 目標模塊（例如：sa-admin）

        Returns:
            Dict[str, Any]: 檢查結果
        """
        try:
            cmd = f"./gradlew :{target_module}:checkstyleMain"
            result = subprocess.run(
                cmd,
                shell=True,
                cwd=os.path.join(self.project_root, 'smart-admin-api-java21-springboot3'),
                capture_output=True,
                text=True,
                timeout=300
            )

            return {
                "tool": "Checkstyle",
                "success": result.returncode == 0,
                "output": result.stdout,
                "errors": result.stderr
            }
        except Exception as e:
            logger.error(f"Error running Checkstyle: {e}")
            return {"tool": "Checkstyle", "success": False, "error": str(e)}

    def run_pmd(self, target_module: str = "sa-admin") -> Dict[str, Any]:
        """
        運行 PMD 檢查

        Args:
            target_module: 目標模塊

        Returns:
            Dict[str, Any]: 檢查結果
        """
        try:
            cmd = f"./gradlew :{target_module}:pmdMain"
            result = subprocess.run(
                cmd,
                shell=True,
                cwd=os.path.join(self.project_root, 'smart-admin-api-java21-springboot3'),
                capture_output=True,
                text=True,
                timeout=300
            )

            return {
                "tool": "PMD",
                "success": result.returncode == 0,
                "output": result.stdout,
                "errors": result.stderr
            }
        except Exception as e:
            logger.error(f"Error running PMD: {e}")
            return {"tool": "PMD", "success": False, "error": str(e)}

    def run_spotbugs(self, target_module: str = "sa-admin") -> Dict[str, Any]:
        """
        運行 SpotBugs 檢查

        Args:
            target_module: 目標模塊

        Returns:
            Dict[str, Any]: 檢查結果
        """
        try:
            cmd = f"./gradlew :{target_module}:spotbugsMain"
            result = subprocess.run(
                cmd,
                shell=True,
                cwd=os.path.join(self.project_root, 'smart-admin-api-java21-springboot3'),
                capture_output=True,
                text=True,
                timeout=300
            )

            return {
                "tool": "SpotBugs",
                "success": result.returncode == 0,
                "output": result.stdout,
                "errors": result.stderr
            }
        except Exception as e:
            logger.error(f"Error running SpotBugs: {e}")
            return {"tool": "SpotBugs", "success": False, "error": str(e)}

    def run_archunit_tests(self, target_module: str = "sa-admin") -> Dict[str, Any]:
        """
        運行 ArchUnit 架構測試

        Args:
            target_module: 目標模塊

        Returns:
            Dict[str, Any]: 測試結果
        """
        try:
            cmd = f"./gradlew :{target_module}:test --tests ArchitectureTest"
            result = subprocess.run(
                cmd,
                shell=True,
                cwd=os.path.join(self.project_root, 'smart-admin-api-java21-springboot3'),
                capture_output=True,
                text=True,
                timeout=300
            )

            return {
                "tool": "ArchUnit",
                "success": result.returncode == 0,
                "output": result.stdout,
                "errors": result.stderr
            }
        except Exception as e:
            logger.error(f"Error running ArchUnit tests: {e}")
            return {"tool": "ArchUnit", "success": False, "error": str(e)}

    def run_all_checks(self, target_module: str = "sa-admin") -> List[Dict[str, Any]]:
        """
        運行所有代碼檢查

        Args:
            target_module: 目標模塊

        Returns:
            List[Dict[str, Any]]: 所有檢查結果
        """
        results = []
        results.append(self.run_checkstyle(target_module))
        results.append(self.run_pmd(target_module))
        results.append(self.run_spotbugs(target_module))
        results.append(self.run_archunit_tests(target_module))
        return results

# ============================================================================
# 數據庫查詢工具
# ============================================================================

class DatabaseQueryTool:
    """
    數據庫查詢工具

    提供 PostgreSQL 查詢和性能分析功能。
    """

    def __init__(self, db_connection_string: str):
        """
        初始化數據庫查詢工具

        Args:
            db_connection_string: PostgreSQL 連接字符串
        """
        import psycopg2
        self.conn = psycopg2.connect(db_connection_string)

    def analyze_query(self, query: str) -> Dict[str, Any]:
        """
        分析 SQL 查詢性能

        Args:
            query: SQL 查詢語句

        Returns:
            Dict[str, Any]: 查詢執行計劃
        """
        try:
            cursor = self.conn.cursor()

            # 運行 EXPLAIN ANALYZE
            explain_query = f"EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON) {query}"
            cursor.execute(explain_query)
            result = cursor.fetchone()[0]

            cursor.close()

            return {
                "success": True,
                "query": query,
                "plan": result
            }
        except Exception as e:
            logger.error(f"Error analyzing query: {e}")
            return {"success": False, "error": str(e)}

    def check_slow_queries(self, min_duration_ms: int = 1000) -> List[Dict[str, Any]]:
        """
        檢查慢查詢

        Args:
            min_duration_ms: 最小持續時間（毫秒）

        Returns:
            List[Dict[str, Any]]: 慢查詢列表
        """
        try:
            cursor = self.conn.cursor()

            # 查詢 pg_stat_statements（需要擴展）
            query = """
            SELECT query, calls, mean_exec_time, max_exec_time
            FROM pg_stat_statements
            WHERE mean_exec_time > %s
            ORDER BY mean_exec_time DESC
            LIMIT 10
            """
            cursor.execute(query, (min_duration_ms,))
            rows = cursor.fetchall()

            cursor.close()

            slow_queries = []
            for row in rows:
                slow_queries.append({
                    "query": row[0],
                    "calls": row[1],
                    "mean_time_ms": row[2],
                    "max_time_ms": row[3]
                })

            return slow_queries
        except Exception as e:
            logger.error(f"Error checking slow queries: {e}")
            return []

    def close(self):
        """關閉數據庫連接"""
        if self.conn:
            self.conn.close()

# ============================================================================
# Git 操作工具
# ============================================================================

class GitOperationTool:
    """
    Git 操作工具

    提供 Git 分支、提交、PR 創建功能。
    """

    def __init__(self, repo_path: str):
        """
        初始化 Git 操作工具

        Args:
            repo_path: Git 倉庫路徑
        """
        self.repo_path = repo_path

    def create_branch(self, branch_name: str, base_branch: str = "master") -> bool:
        """
        創建新分支

        Args:
            branch_name: 新分支名稱
            base_branch: 基礎分支

        Returns:
            bool: 是否創建成功
        """
        try:
            # 切換到基礎分支
            subprocess.run(["git", "checkout", base_branch], cwd=self.repo_path, check=True)

            # 拉取最新代碼
            subprocess.run(["git", "pull"], cwd=self.repo_path, check=True)

            # 創建並切換到新分支
            subprocess.run(["git", "checkout", "-b", branch_name], cwd=self.repo_path, check=True)

            logger.info(f"Branch created: {branch_name}")
            return True
        except Exception as e:
            logger.error(f"Error creating branch: {e}")
            return False

    def commit_changes(self, message: str) -> bool:
        """
        提交更改

        Args:
            message: 提交消息

        Returns:
            bool: 是否提交成功
        """
        try:
            # 添加所有更改
            subprocess.run(["git", "add", "."], cwd=self.repo_path, check=True)

            # 提交
            commit_message = f"{message}\n\nCo-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
            subprocess.run(
                ["git", "commit", "-m", commit_message],
                cwd=self.repo_path,
                check=True
            )

            logger.info(f"Changes committed: {message}")
            return True
        except Exception as e:
            logger.error(f"Error committing changes: {e}")
            return False

    def create_pull_request(
        self,
        title: str,
        body: str,
        base_branch: str = "master"
    ) -> Optional[str]:
        """
        創建 Pull Request（使用 GitHub CLI）

        Args:
            title: PR 標題
            body: PR 描述
            base_branch: 目標分支

        Returns:
            Optional[str]: PR URL（如果成功）
        """
        try:
            # 推送當前分支
            subprocess.run(["git", "push", "-u", "origin", "HEAD"], cwd=self.repo_path, check=True)

            # 創建 PR
            result = subprocess.run(
                [
                    "gh", "pr", "create",
                    "--title", title,
                    "--body", body,
                    "--base", base_branch
                ],
                cwd=self.repo_path,
                capture_output=True,
                text=True,
                check=True
            )

            pr_url = result.stdout.strip()
            logger.info(f"Pull request created: {pr_url}")
            return pr_url
        except Exception as e:
            logger.error(f"Error creating pull request: {e}")
            return None

# ============================================================================
# 測試主函數
# ============================================================================

if __name__ == '__main__':
    print("=" * 70)
    print("Shared Tools Test")
    print("=" * 70)
    print()

    # 測試文件訪問工具
    print("Testing SafeFileAccessTool...")
    db_string = os.getenv('DB_CONNECTION_STRING', 'postgresql://postgres:@localhost:5432/smart_admin')
    file_tool = SafeFileAccessTool(db_string, "test-agent")

    # 嘗試讀取允許的文件
    test_file = "/project/controller/UserController.java"
    content = file_tool.read_file(test_file)
    if content is None:
        print(f"✓ Access control working (file does not exist or access denied)")
    print()

    # 測試代碼分析工具
    print("Testing CodeAnalysisTool...")
    project_root = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
    code_tool = CodeAnalysisTool(project_root)
    print(f"✓ CodeAnalysisTool initialized (project root: {project_root})")
    print()

    # 測試 Git 操作工具
    print("Testing GitOperationTool...")
    git_tool = GitOperationTool(project_root)
    print(f"✓ GitOperationTool initialized (repo path: {project_root})")
    print()

    print("=" * 70)
    print("Test completed")
    print("=" * 70)
