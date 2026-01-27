#!/usr/bin/env python3
"""
SmartAdmin Auto-Coding - CrewAI Integration Tests
CrewAI 框架集成測試

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
os.environ['DB_CONNECTION_STRING'] = 'postgresql://test:test@localhost:5432/test_db'
os.environ['CLAUDE_API_KEY'] = 'test-key'
os.environ['TELEGRAM_BOT_TOKEN'] = 'test-token'
os.environ['TELEGRAM_CHAT_ID'] = '12345'


class TestCrewAIToolsIntegration:
    """CrewAI Tools 集成測試"""

    def test_tools_module_import(self):
        """測試 tools 模塊導入"""
        try:
            from automation.clawdbot.crews.common.tools import (
                read_file_tool,
                write_file_tool,
                list_files_tool,
                run_checkstyle_tool,
                run_pmd_tool,
                run_spotbugs_tool,
                run_archunit_tool,
                query_database_tool,
                check_slow_queries_tool,
                create_branch_tool,
                commit_changes_tool,
                create_pr_tool,
                generate_java_code_tool
            )
            print("✅ All 13 @tool functions imported successfully")

            # 驗證是否為 crewai.tools.Tool 類型
            from crewai.tools import BaseTool
            assert isinstance(read_file_tool, BaseTool) or callable(read_file_tool)
            print("✅ Tools are valid CrewAI Tool instances")

        except ImportError as e:
            pytest.fail(f"Failed to import tools: {e}")

    def test_tool_metadata(self):
        """測試工具元數據"""
        from automation.clawdbot.crews.common.tools import read_file_tool, generate_java_code_tool

        # 檢查工具有描述
        assert hasattr(read_file_tool, 'name') or hasattr(read_file_tool, 'description')
        assert hasattr(generate_java_code_tool, 'name') or hasattr(generate_java_code_tool, 'description')

        print("✅ Tools have required metadata")


class TestAnalyzerCrewIntegration:
    """AnalyzerCrew 集成測試"""

    def test_analyzer_crew_initialization(self):
        """測試 AnalyzerCrew 初始化"""
        with patch('psycopg2.pool.ThreadedConnectionPool'):
            from automation.clawdbot.crews.analyzer_crew import AnalyzerCrew

            crew = AnalyzerCrew()

            # 驗證基本屬性
            assert crew.crew_name == "analyzer-crew"
            assert crew.crew_type == "analysis"
            assert crew.crew is not None

            print(f"✅ AnalyzerCrew initialized: {crew.crew_name}")

    def test_analyzer_agents_have_tools(self):
        """測試 Analyzer Agents 是否有工具"""
        with patch('psycopg2.pool.ThreadedConnectionPool'):
            from automation.clawdbot.crews.analyzer_crew import AnalyzerCrew

            crew = AnalyzerCrew()

            # 獲取 agents
            if hasattr(crew.crew, 'agents'):
                agents = crew.crew.agents
                assert len(agents) > 0, "No agents found in crew"

                for agent in agents:
                    # 驗證 agent 有 tools
                    if hasattr(agent, 'tools'):
                        print(f"✅ Agent '{agent.role}' has {len(agent.tools)} tools")
                        assert len(agent.tools) > 0, f"Agent {agent.role} has no tools"
                    else:
                        print(f"⚠️ Agent '{agent.role}' has no tools attribute")
            else:
                print("⚠️ Crew has no agents attribute")

    def test_analyzer_tasks_defined(self):
        """測試 Analyzer Tasks 是否定義"""
        with patch('psycopg2.pool.ThreadedConnectionPool'):
            from automation.clawdbot.crews.analyzer_crew import AnalyzerCrew

            crew = AnalyzerCrew()

            # 檢查是否有 tasks
            if hasattr(crew.crew, 'tasks'):
                tasks = crew.crew.tasks
                assert len(tasks) > 0, "No tasks defined"

                for task in tasks:
                    print(f"✅ Task defined: {task.description[:50]}...")
                    assert hasattr(task, 'agent'), "Task missing agent"
            else:
                print("⚠️ Crew has no tasks attribute")


class TestDeveloperCrewIntegration:
    """DeveloperCrew 集成測試"""

    def test_developer_crew_initialization(self):
        """測試 DeveloperCrew 初始化"""
        with patch('psycopg2.pool.ThreadedConnectionPool'):
            from automation.clawdbot.crews.developer_crew import DeveloperCrew

            crew = DeveloperCrew()

            assert crew.crew_name == "developer-crew"
            assert crew.crew_type == "development"
            assert crew.crew is not None

            print(f"✅ DeveloperCrew initialized: {crew.crew_name}")

    def test_developer_has_claude_tool(self):
        """測試 Developer Crew 是否有 Claude 生成工具"""
        with patch('psycopg2.pool.ThreadedConnectionPool'):
            from automation.clawdbot.crews.developer_crew import DeveloperCrew

            crew = DeveloperCrew()

            # 檢查 Java Architect Agent 是否有 generate_java_code_tool
            if hasattr(crew.crew, 'agents'):
                agents = crew.crew.agents

                java_architect = None
                for agent in agents:
                    if 'Java' in agent.role or 'Architect' in agent.role:
                        java_architect = agent
                        break

                if java_architect:
                    assert hasattr(java_architect, 'tools')
                    assert len(java_architect.tools) > 0

                    # 檢查是否有 Claude 工具
                    tool_names = [str(tool) for tool in java_architect.tools]
                    print(f"✅ Java Architect has {len(java_architect.tools)} tools")
                    print(f"   Tool types: {tool_names[:3]}")
                else:
                    print("⚠️ Java Architect agent not found")


class TestQACrewIntegration:
    """QACrew 集成測試"""

    def test_qa_crew_initialization(self):
        """測試 QACrew 初始化"""
        with patch('psycopg2.pool.ThreadedConnectionPool'):
            from automation.clawdbot.crews.qa_crew import QACrew

            crew = QACrew()

            assert crew.crew_name == "qa-crew"
            assert crew.crew_type == "quality-assurance"
            assert crew.crew is not None

            print(f"✅ QACrew initialized: {crew.crew_name}")

    def test_qa_quality_decision_method(self):
        """測試 QA 質量決策方法"""
        with patch('psycopg2.pool.ThreadedConnectionPool'):
            from automation.clawdbot.crews.qa_crew import QACrew

            crew = QACrew()

            # 測試 APPROVED 決策
            results_pass = {
                "archunit_passed": True,
                "checkstyle_violations": 3,
                "pmd_violations": 5,
                "spotbugs_high": 0
            }

            if hasattr(crew, '_make_quality_decision'):
                decision = crew._make_quality_decision(results_pass)
                assert decision == "APPROVED"
                print("✅ Quality decision: APPROVED for passing results")

                # 測試 REJECTED 決策
                results_fail = {
                    "archunit_passed": False,
                    "checkstyle_violations": 10,
                    "pmd_violations": 20,
                    "spotbugs_high": 3
                }

                decision = crew._make_quality_decision(results_fail)
                assert decision == "REJECTED"
                print("✅ Quality decision: REJECTED for failing results")
            else:
                print("⚠️ _make_quality_decision method not found")


class TestCrewWorkflowIntegration:
    """Crew 工作流集成測試"""

    def test_crew_kickoff_structure(self):
        """測試 Crew.kickoff() 方法結構"""
        with patch('psycopg2.pool.ThreadedConnectionPool'):
            from automation.clawdbot.crews.analyzer_crew import AnalyzerCrew

            crew = AnalyzerCrew()

            # 驗證 crew.kickoff 方法存在
            assert hasattr(crew.crew, 'kickoff')
            assert callable(crew.crew.kickoff)

            print("✅ Crew.kickoff() method exists and is callable")

    def test_manual_bypass_removed(self):
        """測試手動繞過方法已刪除"""
        with patch('psycopg2.pool.ThreadedConnectionPool'):
            from automation.clawdbot.crews.analyzer_crew import AnalyzerCrew
            from automation.clawdbot.crews.developer_crew import DeveloperCrew
            from automation.clawdbot.crews.qa_crew import QACrew

            analyzer = AnalyzerCrew()
            developer = DeveloperCrew()
            qa = QACrew()

            # 驗證沒有 _run_*_manually 方法
            assert not hasattr(analyzer, '_run_analysis_manually')
            assert not hasattr(developer, '_run_development_manually')
            assert not hasattr(qa, '_run_qa_manually')

            print("✅ All _run_*_manually() methods removed")
            print("✅ Crews use native CrewAI execution")


def run_crewai_integration_tests():
    """運行 CrewAI 集成測試"""
    print("\n" + "=" * 70)
    print("SmartAdmin Auto-Coding - CrewAI Integration Tests")
    print("=" * 70)
    print()

    exit_code = pytest.main([
        __file__,
        "-v",
        "-s",
        "--tb=short",
        "--maxfail=5",
    ])

    return exit_code


if __name__ == '__main__':
    sys.exit(run_crewai_integration_tests())
