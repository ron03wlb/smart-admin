#!/usr/bin/env python3
"""
SmartAdmin Auto-Coding - Developer Crew
開發工作流 Crew

功能：
- 後端實現（Java Architect Agent）
- 前端實現（Vue Expert Agent）
- 部署配置（DevOps Engineer Agent）
- 自動創建 PR

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
from tools import (
    SafeFileAccessTool, GitOperationTool,
    # 導入 @tool 函數
    read_file_tool, write_file_tool, list_files_tool,
    create_branch_tool, commit_changes_tool, create_pr_tool,
    generate_java_code_tool,  # Week 1 Day 3-4: Claude AI 集成
    FILE_TOOLS, GIT_TOOLS, AI_TOOLS
)

# ============================================================================
# Developer Crew 實現
# ============================================================================

class DeveloperCrew(BaseCrew):
    """
    Developer Crew

    執行開發工作流：
    1. 後端實現（Java）
    2. 前端實現（Vue）
    3. 部署配置
    4. PR 創建
    """

    def __init__(self):
        """初始化 Developer Crew"""
        super().__init__(crew_name="developer-crew", crew_type="developer")

        # 初始化工具
        self.file_tool = SafeFileAccessTool(
            db_connection_string=os.getenv('DB_CONNECTION_STRING', ''),
            agent_name="developer-crew"
        )
        self.git_tool = GitOperationTool(
            repo_path=os.getenv('PROJECT_ROOT', os.getcwd())
        )

        # 創建 Agents
        self.java_architect_agent = self._create_java_architect_agent()
        self.vue_expert_agent = self._create_vue_expert_agent()
        self.devops_engineer_agent = self._create_devops_engineer_agent()

    # ========================================================================
    # Agent 創建方法
    # ========================================================================

    def _create_java_architect_agent(self) -> Agent:
        """
        創建 Java Architect Agent (v2.1.0 - 集成 Claude AI)

        職責：實現後端 API（Controller, Service, Manager, Dao）
        """
        return Agent(
            role="Java Architect (Backend Developer)",
            goal="Implement backend APIs following SmartAdmin patterns using Claude AI",
            backstory="""You are an expert Java backend developer specializing in
            SmartAdmin's layered architecture. You use Claude AI to generate production-ready
            code with proper ResponseDTO, pagination, validation, and transaction management.
            You ensure all generated code follows SmartAdmin conventions and passes ArchUnit tests.""",
            verbose=True,
            allow_delegation=False,
            tools=[
                generate_java_code_tool,  # ✅ Claude AI 代碼生成（新增）
                read_file_tool,
                write_file_tool,
                list_files_tool
            ]
        )

    def _create_vue_expert_agent(self) -> Agent:
        """
        創建 Vue Expert Agent

        職責：實現前端 UI（Vue 3 + Ant Design）
        """
        return Agent(
            role="Vue Expert (Frontend Developer)",
            goal="Implement frontend UI components with Vue 3 and Ant Design",
            backstory="""You are a Vue 3 expert specializing in Ant Design Vue.
            You implement CRUD components (list views, form modals) with proper
            API integration, state management (Pinia), and responsive design.""",
            verbose=True,
            allow_delegation=False,
            tools=[
                read_file_tool,
                write_file_tool,
                list_files_tool
            ]
        )

    def _create_devops_engineer_agent(self) -> Agent:
        """
        創建 DevOps Engineer Agent

        職責：配置部署（Docker, Kubernetes, CI/CD）
        """
        return Agent(
            role="DevOps Engineer",
            goal="Configure deployment and CI/CD pipelines",
            backstory="""You are a DevOps engineer expert in Docker, Kubernetes,
            and CI/CD automation. You create Dockerfiles, K8s manifests, and
            GitHub Actions workflows.""",
            verbose=True,
            allow_delegation=False,
            tools=[
                read_file_tool,
                write_file_tool
            ]
        )

    # ========================================================================
    # Task 創建方法
    # ========================================================================

    def _create_backend_implementation_task(self, feature_spec: Dict[str, Any]) -> Task:
        """
        創建後端實現 Task

        Args:
            feature_spec: 功能規格

        Returns:
            Task: 後端實現任務
        """
        description = f"""
        Implement backend API for feature: {feature_spec['name']}

        Requirements:
        - Entity: {feature_spec.get('entity', 'N/A')}
        - Endpoints: {', '.join(feature_spec.get('endpoints', []))}
        - Validation: {feature_spec.get('validation', 'Standard')}

        Implementation steps:
        1. Create Entity class (with Lombok annotations)
        2. Create Dao interface (extends BaseMapper)
        3. Create Manager class (with @Transactional)
        4. Create Service class (with Vavr Option)
        5. Create Controller (with @RestController, @SaCheckPermission)
        6. Create Form/VO classes
        7. Create XML mapper (if complex queries)
        8. Write unit tests

        Follow SmartAdmin patterns:
        - ResponseDTO.ok(data) for success responses
        - SmartPageUtil for pagination
        - SmartBeanUtil for bean conversion
        - Constructor injection with @RequiredArgsConstructor
        """

        expected_output = """
        Backend implementation including:
        - Java source files (Entity, Dao, Manager, Service, Controller, Form, VO)
        - XML mapper (if applicable)
        - Unit tests
        - File paths list
        """

        return Task(
            description=description,
            expected_output=expected_output,
            agent=self.java_architect_agent
        )

    def _create_frontend_implementation_task(self, feature_spec: Dict[str, Any]) -> Task:
        """
        創建前端實現 Task

        Args:
            feature_spec: 功能規格

        Returns:
            Task: 前端實現任務
        """
        description = f"""
        Implement frontend UI for feature: {feature_spec['name']}

        Requirements:
        - Views: {', '.join(feature_spec.get('views', []))}
        - Components: {', '.join(feature_spec.get('components', []))}

        Implementation steps:
        1. Create list view component (with Ant Design Table)
        2. Create form modal/drawer component (with Ant Design Form)
        3. Create API client (axios)
        4. Create TypeScript types
        5. Create Pinia store (if state management needed)
        6. Add route configuration
        7. Add i18n translations (zh-CN, en-US)

        Follow SmartAdmin Vue patterns:
        - Composition API with <script setup>
        - Ant Design Vue components
        - Smart-admin-api wrapper for axios
        - v-privilege directive for permissions
        """

        expected_output = """
        Frontend implementation including:
        - Vue components (.vue files)
        - API client files (.ts)
        - TypeScript types
        - Route configuration
        - i18n translations
        - File paths list
        """

        return Task(
            description=description,
            expected_output=expected_output,
            agent=self.vue_expert_agent
        )

    def _create_deployment_config_task(self, feature_spec: Dict[str, Any]) -> Task:
        """
        創建部署配置 Task

        Args:
            feature_spec: 功能規格

        Returns:
            Task: 部署配置任務
        """
        description = f"""
        Configure deployment for feature: {feature_spec['name']}

        Requirements:
        - Environment: {feature_spec.get('environment', 'dev/staging/prod')}
        - Dependencies: {', '.join(feature_spec.get('dependencies', []))}

        Implementation steps:
        1. Update Dockerfile (if needed)
        2. Update docker-compose.yml (if needed)
        3. Create K8s ConfigMap (if config needed)
        4. Update K8s Deployment (if resources changed)
        5. Create GitHub Actions workflow (for CI/CD)
        6. Update environment variables documentation

        Follow DevOps best practices:
        - Multi-stage Docker builds
        - Health checks
        - Resource limits
        - Secrets management
        """

        expected_output = """
        Deployment configuration including:
        - Dockerfile updates
        - K8s manifests
        - CI/CD workflow
        - Documentation updates
        - File paths list
        """

        return Task(
            description=description,
            expected_output=expected_output,
            agent=self.devops_engineer_agent
        )

    # ========================================================================
    # Crew 創建方法（實現抽象方法）
    # ========================================================================

    def create_crew(self, feature_spec: Dict[str, Any]) -> Crew:
        """
        創建 Developer Crew 實例

        Args:
            feature_spec: 功能規格

        Returns:
            Crew: CrewAI Crew 實例
        """
        # 創建 Tasks
        backend_task = self._create_backend_implementation_task(feature_spec)
        frontend_task = self._create_frontend_implementation_task(feature_spec)
        deployment_task = self._create_deployment_config_task(feature_spec)

        # 創建 Crew（順序執行：Backend → Frontend → Deployment）
        crew = Crew(
            agents=[
                self.java_architect_agent,
                self.vue_expert_agent,
                self.devops_engineer_agent
            ],
            tasks=[
                backend_task,
                frontend_task,
                deployment_task
            ],
            process=Process.sequential,
            verbose=True
        )

        return crew

    # ========================================================================
    # 運行方法（實現抽象方法）
    # ========================================================================

    def run(self, feature_spec: Dict[str, Any]) -> Dict[str, Any]:
        """
        運行 Developer Crew (v2.0.0 - 使用 CrewAI)

        Args:
            feature_spec: 功能規格
                {
                    "name": "Employee Management",
                    "entity": "Employee",
                    "endpoints": ["list", "add", "update", "delete"],
                    "views": ["list", "form"],
                    "validation": "Standard"
                }

        Returns:
            Dict[str, Any]: 開發結果（包含 PR URL）
        """
        start_time = time.time()

        # 記錄執行開始
        execution_id = self.log_execution_start({
            "feature_spec": feature_spec,
            "crew_type": "developer"
        })

        try:
            self.logger.info(f"Starting developer crew for feature: {feature_spec['name']}")

            # 創建功能分支
            branch_name = f"feature/{feature_spec['name'].lower().replace(' ', '-')}"
            self.git_tool.create_branch(branch_name)

            # ✅ 讓 CrewAI 真正執行（不再手動繞過）
            crew = self.create_crew(feature_spec)
            crew_result = crew.kickoff(inputs={
                "feature_spec": feature_spec
            })

            # 解析結果
            results = self._parse_crew_output(crew_result)

            # 提交更改
            commit_message = f"feat: implement {feature_spec['name']}"
            self.git_tool.commit_changes(commit_message)

            # 創建 PR
            pr_title = f"feat: {feature_spec['name']}"
            pr_body = f"""
## Summary

Implemented {feature_spec['name']} feature.

**Backend**:
- Entity, Dao, Manager, Service, Controller
- Form/VO classes
- Unit tests

**Frontend**:
- List view component
- Form modal component
- API client
- TypeScript types

**Deployment**:
- Configuration updates
- CI/CD workflow

## Test Plan

- [ ] Backend API tests pass
- [ ] Frontend components render correctly
- [ ] CRUD operations work as expected
- [ ] Permissions are properly configured
- [ ] Deployment succeeds in dev environment

🤖 Generated with SmartAdmin Auto-Coding System (v2.0.0)
"""
            pr_url = self.git_tool.create_pull_request(pr_title, pr_body)

            # 計算執行時長
            duration = time.time() - start_time
            duration_str = f"{int(duration // 60)}m {int(duration % 60)}s"

            # 記錄執行完成
            self.log_execution_complete(
                execution_id=execution_id,
                status='SUCCESS',
                output_result={
                    "pr_url": pr_url,
                    "results": results
                }
            )

            # 發送 Telegram 通知
            notification = self.format_execution_notification(
                status='SUCCESS',
                duration=duration_str,
                summary=f"Feature '{feature_spec['name']}' implemented successfully",
                details=[
                    f"Branch: {branch_name}",
                    f"PR URL: {pr_url}"
                ]
            )
            self.send_telegram_notification(notification)

            return {
                "status": "SUCCESS",
                "execution_id": execution_id,
                "feature_name": feature_spec['name'],
                "branch_name": branch_name,
                "pr_url": pr_url,
                "duration": duration_str,
                "results": results,
                "timestamp": datetime.now().isoformat()
            }

        except Exception as e:
            return self.handle_error(execution_id, e, "Developer crew execution failed")

    def _parse_crew_output(self, crew_result: Any) -> Dict[str, Any]:
        """
        解析 CrewAI 輸出結果

        Args:
            crew_result: CrewAI kickoff 返回的結果

        Returns:
            Dict[str, Any]: 結構化的開發結果
        """
        try:
            # 如果結果是字符串,嘗試解析為 JSON
            if isinstance(crew_result, str):
                import re
                json_match = re.search(r'\{.*\}', crew_result, re.DOTALL)
                if json_match:
                    return json.loads(json_match.group())

            # 如果有 output 屬性
            if hasattr(crew_result, 'output'):
                return {"raw_output": str(crew_result.output)}

            # 默認返回原始結果
            return {"raw_output": str(crew_result)}

        except Exception as e:
            self.logger.warning(f"Failed to parse crew output: {e}")
            return {"raw_output": str(crew_result), "parse_error": str(e)}

    # ========================================================================
    # 內部執行方法
    # ========================================================================
    #
    # _run_development_manually() 方法已刪除 (v2.0.0)
    # 現在使用 CrewAI 真正執行 Tasks,不再手動繞過
    #

# ============================================================================
# 命令行接口
# ============================================================================

def main():
    """
    命令行主函數

    用法：
        python3 developer_crew.py --feature-name "Employee Management" --entity "Employee"
    """
    import argparse

    parser = argparse.ArgumentParser(description='SmartAdmin Developer Crew')
    parser.add_argument(
        '--feature-name',
        type=str,
        required=True,
        help='Feature name (e.g., "Employee Management")'
    )
    parser.add_argument(
        '--entity',
        type=str,
        required=True,
        help='Entity name (e.g., "Employee")'
    )
    args = parser.parse_args()

    # 構建功能規格
    feature_spec = {
        "name": args.feature_name,
        "entity": args.entity,
        "endpoints": ["list", "add", "update", "delete"],
        "views": ["list", "form"],
        "components": ["table", "form-modal"],
        "validation": "Standard",
        "environment": "dev/staging/prod",
        "dependencies": []
    }

    # 創建並運行 Crew
    crew = DeveloperCrew()
    result = crew.run(feature_spec=feature_spec)

    # 打印結果
    print("\n" + "=" * 70)
    print("Developer Crew Results")
    print("=" * 70)
    print(json.dumps(result, indent=2, ensure_ascii=False))
    print("=" * 70)

    # 清理資源
    crew.cleanup()

if __name__ == '__main__':
    main()
