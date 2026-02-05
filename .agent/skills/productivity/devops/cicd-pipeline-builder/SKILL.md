---
name: cicd-pipeline-builder
description: "CI/CD 管線自動化建置"
priority: P2
category: devops
---

# CI/CD Pipeline Builder

為 SmartAdmin 專案自動化 CI/CD 管線配置，支援 GitHub Actions 或 GitLab CI，包含完整的建置、測試、品質分析和部署工作流程。

## Usage

```
User: "Set up GitHub Actions for SmartAdmin"
AI: [Generate workflow with build, test, quality gates, and deployment]
```

## When to Use

- 設置 CI/CD 管線
- 創建 GitHub Actions 工作流
- 配置 GitLab CI 管線
- 添加品質門檻自動化
- 設置 dev/staging/production 部署

## Generated Output

- GitHub Actions workflow YAML
- GitLab CI pipeline YAML
- Docker 建置配置
- 品質門檻整合 (SpotBugs, PMD, Checkstyle)
- 多環境部署腳本

## Workflow

1. **識別目標平台**
   - GitHub Actions 或 GitLab CI
   - 確認專案結構和建置工具

2. **生成工作流配置**
   - 建置階段 (compile, package)
   - 測試階段 (unit, integration, architecture)
   - 品質檢查階段

3. **配置服務容器**
   - PostgreSQL 測試資料庫
   - Redis 快取服務

4. **設置品質門檻**
   - SpotBugs 缺陷檢測
   - PMD 程式碼品質
   - Checkstyle 風格檢查
   - ArchUnit 架構測試

5. **配置部署管線**
   - Docker 映像建置
   - 環境變數管理
   - 多環境部署策略

## Related Rules

- [W01-sonarqube-rules.md](../../../rules/workflows/W01-sonarqube-rules.md)
- [Q01-checkstyle-rules.md](../../../rules/quality-tools/Q01-checkstyle-rules.md)

## Example Session

**User:** 為 SmartAdmin 設置 GitHub Actions，包含品質門檻和 Docker 部署

**AI Agent Actions:**
1. 創建 `.github/workflows/ci-cd.yml`
2. 配置 Java 21 + Gradle 建置環境
3. 添加 PostgreSQL 和 Redis 服務容器
4. 整合 SpotBugs、PMD、Checkstyle 品質檢查
5. 添加 Docker 映像建置和推送
6. 配置 dev/staging/prod 部署分支策略
