@echo off
REM ====================================================================
REM Windows Batch Root Layer Cleanup v1.0.0
REM ====================================================================
REM Purpose: Move root-layer skills to _deprecated using Windows commands
REM Date: 2026-01-29
REM Usage: Double-click this file or run from Command Prompt
REM ====================================================================

echo ====================================================================
echo Root Layer Cleanup - Windows Batch Version
echo ====================================================================
echo.

cd /d "%~dp0..\skills"

echo Current directory: %CD%
echo.

REM Create backup directories
echo Creating backup directories...
if not exist "_deprecated\root-layer-v3" mkdir "_deprecated\root-layer-v3"
if not exist "_deprecated\non-skill-items-v3" mkdir "_deprecated\non-skill-items-v3"
if not exist "_deprecated\skill-files-v3" mkdir "_deprecated\skill-files-v3"
echo Done.
echo.

REM Move skill directories (28 items)
echo ========================================
echo Moving Skill Directories (28 items)
echo ========================================

set SKILL_COUNT=0

call :MoveSkill apm-integration-skill
call :MoveSkill batch-plan-executor
call :MoveSkill cache-strategy-generator
call :MoveSkill cicd-pipeline-builder
call :MoveSkill db-migration-manager
call :MoveSkill fraud-detection-pattern-generator
call :MoveSkill full-text-search-integration
call :MoveSkill i18n-generator
call :MoveSkill igame-feature-builder
call :MoveSkill igame-pm-analyst
call :MoveSkill igaming-multi-tenant-wallet-pm
call :MoveSkill java-performance-pro
call :MoveSkill liteflow-rule-builder
call :MoveSkill message-queue-pattern-generator
call :MoveSkill quality-gate-orchestrator
call :MoveSkill report-generator-skill
call :MoveSkill scheduled-task-manager
call :MoveSkill security-hardening-pro
call :MoveSkill smartadmin-api-docs
call :MoveSkill smartadmin-crud-generator
call :MoveSkill smartadmin-integration-test
call :MoveSkill smartadmin-mybatis
call :MoveSkill smartadmin-performance-suite
call :MoveSkill smartadmin-testing-suite
call :MoveSkill smartadmin-vue-crud
call :MoveSkill test-fixture-generator
call :MoveSkill vavr-refactoring-assistant
call :MoveSkill websocket-sse-realtime-generator

echo Skills moved: %SKILL_COUNT%/28
echo.

REM Move .skill files (9 items)
echo ========================================
echo Moving .skill Files (9 items)
echo ========================================

set FILE_COUNT=0

call :MoveFile cicd-pipeline-builder.skill skill-files-v3
call :MoveFile db-migration-manager.skill skill-files-v3
call :MoveFile igame-feature-builder.skill skill-files-v3
call :MoveFile java-performance-pro.skill skill-files-v3
call :MoveFile security-hardening-pro.skill skill-files-v3
call :MoveFile smartadmin-api-docs.skill skill-files-v3
call :MoveFile smartadmin-crud-generator.skill skill-files-v3
call :MoveFile smartadmin-integration-test.skill skill-files-v3
call :MoveFile smartadmin-vue-crud.skill skill-files-v3

echo .skill files moved: %FILE_COUNT%/9
echo.

REM Move non-skill text items (27 items)
echo ========================================
echo Moving Non-Skill Items (27 items)
echo ========================================

set TEXT_COUNT=0

call :MoveText better-auth-best-practices
call :MoveText brainstorming
call :MoveText claude-settings-audit
call :MoveText cloudflare
call :MoveText design-md
call :MoveText dispatching-parallel-agents
call :MoveText executing-plans
call :MoveText file-organizer
call :MoveText find-bugs
call :MoveText finishing-a-development-branch
call :MoveText frontend-design
call :MoveText git-pushing
call :MoveText iterate-pr
call :MoveText prompt-engineering
call :MoveText receiving-code-review
call :MoveText requesting-code-review
call :MoveText review-implementing
call :MoveText semgrep-rule-creator
call :MoveText skill-creator
call :MoveText subagent-driven-development
call :MoveText supabase-postgres-best-practices
call :MoveText test-driven-development
call :MoveText tinybird
call :MoveText verification-before-completion
call :MoveText webapp-testing
call :MoveText writing-plans
call :MoveText writing-skills

echo Non-skill items moved: %TEXT_COUNT%/27
echo.

REM Move documentation files (6 items)
echo ========================================
echo Moving Documentation Files (6 items)
echo ========================================

set DOC_COUNT=0

call :MoveFile MIGRATION-REPORT-v3.0.0.md non-skill-items-v3
call :MoveFile MONITORING-SYSTEM-DESIGN.md non-skill-items-v3
call :MoveFile NEW-SKILLS-OVERVIEW.md non-skill-items-v3
call :MoveFile P1-BASELINE-TEST-SUMMARY.md non-skill-items-v3
call :MoveFile SPRINT1-PROGRESS.md non-skill-items-v3
call :MoveFile skill-aliases.json non-skill-items-v3

echo Documentation files moved: %DOC_COUNT%/6
echo.

REM Create README files
echo ========================================
echo Creating README files
echo ========================================

call :CreateReadme

echo.
echo ====================================================================
echo Cleanup Completed!
echo ====================================================================
echo.
echo Summary:
echo   - Skill directories: %SKILL_COUNT%/28
echo   - .skill files: %FILE_COUNT%/9
echo   - Non-skill items: %TEXT_COUNT%/27
echo   - Documentation files: %DOC_COUNT%/6
echo.
echo Backup location: .claude\skills\_deprecated\
echo.
echo Next: Run verification and create Git commit
echo See: .claude\scripts\WEEK1-COMPLETION-REPORT.md
echo.
pause
exit /b 0

REM ====================================================================
REM Helper Functions
REM ====================================================================

:MoveSkill
if exist "%~1" (
    move "%~1" "_deprecated\root-layer-v3\%~1" >nul 2>&1
    if errorlevel 1 (
        echo   X Failed: %~1 ^(Permission denied or in use^)
    ) else (
        echo   √ Moved: %~1
        set /a SKILL_COUNT+=1
    )
) else (
    echo   - Not found: %~1
)
exit /b 0

:MoveText
if exist "%~1" (
    move "%~1" "_deprecated\non-skill-items-v3\%~1" >nul 2>&1
    if errorlevel 1 (
        echo   X Failed: %~1 ^(Permission denied or in use^)
    ) else (
        echo   √ Moved: %~1
        set /a TEXT_COUNT+=1
    )
) else (
    echo   - Not found: %~1
)
exit /b 0

:MoveFile
if exist "%~1" (
    move "%~1" "_deprecated\%~2\%~1" >nul 2>&1
    if errorlevel 1 (
        echo   X Failed: %~1 ^(Permission denied or in use^)
    ) else (
        echo   √ Moved: %~1
        set /a FILE_COUNT+=1
        set /a DOC_COUNT+=1
    )
) else (
    echo   - Not found: %~1
)
exit /b 0

:CreateReadme
(
echo # Root Layer Archive ^(v3.0.0^)
echo.
echo 已於 v4.0.0 遷移至分層結構。
echo.
echo ## 遷移信息
echo.
echo - **遷移映射表**: `../../scripts/root-to-hierarchical-mapping.json`
echo - **遷移日期**: 2026-01-29
echo - **保留期限**: 2026-07-29 ^(6 個月後刪除^)
echo - **備份原因**: v4.0.0 根層清理，所有技能已遷移至分層結構
echo.
echo ## 還原方法
echo.
echo 如需還原任何技能：
echo.
echo ```bash
echo cp -r .claude/skills/_deprecated/root-layer-v3/{skill-name} .claude/skills/
echo ```
echo.
echo ## 警告
echo.
echo ⚠️ 這些是舊版本的重複副本。正式版本已在分層結構中：
echo - foundation/
echo - extended/
echo - productivity/
echo - lifecycle/
echo.
echo 請勿直接使用此備份中的文件。
) > "_deprecated\root-layer-v3\README.md"

(
echo # Non-Skill Items Archive ^(v3.0.0^)
echo.
echo 這些項目不應在 .claude/skills/ 中，已於 v4.0.0 遷移時歸檔。
echo.
echo ## 內容類別
echo.
echo - **開發工作流**: dispatching-parallel-agents, git-pushing, etc.
echo - **測試/設計**: test-driven-development, design-md, etc.
echo - **技能開發**: skill-creator, writing-skills, etc.
echo - **特定技術**: better-auth-best-practices, cloudflare, etc.
echo.
echo ## 建議歸檔位置
echo.
echo - 工作流相關: `.claude/docs/workflows/`
echo - 技能開發: `.claude/docs/skill-development/`
echo - 技術文檔: `docs/technical/`
echo.
echo ## 保留期限
echo.
echo 2026-07-29 ^(6 個月後評估是否永久刪除或遷移到正確位置^)
) > "_deprecated\non-skill-items-v3\README.md"

echo   √ Created: _deprecated\root-layer-v3\README.md
echo   √ Created: _deprecated\non-skill-items-v3\README.md
exit /b 0
