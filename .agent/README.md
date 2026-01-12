# .agent 開發規範導航

> **目標架構**: Java 21 + Spring Boot 3.5.4 + PostgreSQL 16 + Vavr 0.10.4 + MyBatis Plus 3.5.12

## 快速導航

### 🏗️ 核心編碼規範
基礎編碼標準（always apply）:
- [01-naming-conventions.md](rules/01-naming-conventions.md) - 命名規範
- [02-oop-principles.md](rules/02-oop-principles.md) - OOP 原則
- [03-concurrency-rules.md](rules/03-concurrency-rules.md) - 並發規則
- [04-exception-logging.md](rules/04-exception-logging.md) - 異常與日誌
- [06-sonarqube-rules.md](rules/06-sonarqube-rules.md) - SonarQube 規則
- [10-architecture-rules.md](rules/10-architecture-rules.md) - 分層架構

### 🗄️ PostgreSQL 資料庫規範（理想架構）
- [05-postgresql-basics.md](rules/05-postgresql-basics.md) - 建表與索引
- [05-postgresql-advanced.md](rules/05-postgresql-advanced.md) - JSONB、陣列、CTE、窗口函數
- [05-postgresql-mybatis-integration.md](rules/05-postgresql-mybatis-integration.md) - SQL 優化、MySQL 遷移

### 🔒 安全規範（OWASP Top 10）
- [07-owasp-top10-part1.md](rules/07-owasp-top10-part1.md) - A01-A04
- [07-owasp-top10-part2.md](rules/07-owasp-top10-part2.md) - A05-A10

### 🎯 Vavr 函數式編程（理想架構）
- [08-vavr-fundamentals.md](rules/08-vavr-fundamentals.md) - Option、Try 基礎
- [08-vavr-advanced.md](rules/08-vavr-advanced.md) - Either、集合、模式匹配
- [08-vavr-mybatis-integration.md](rules/08-vavr-mybatis-integration.md) - Vavr + MyBatis Plus

### 💾 MyBatis Plus 持久層（理想架構：LambdaQueryWrapper 為主）
- [09-mybatis-plus-core.md](rules/09-mybatis-plus-core.md) - LambdaQueryWrapper、分頁、IEnum
- [09-mybatis-plus-postgresql.md](rules/09-mybatis-plus-postgresql.md) - PostgreSQL 整合、JSONB/陣列 TypeHandler

### 🔄 開發工作流程
- [init.md](workflows/init.md) - 環境初始化設定
- [github-actions-pipeline.md](workflows/github-actions-pipeline.md) - GitHub Actions CI/CD
- [quality-gates-local-ci.md](workflows/quality-gates-local-ci.md) - 本地 Quality Gate、GitLab CI
- [tdd-workflow.md](workflows/tdd-workflow.md) - 測試驅動開發
- [java-failure-recovery.md](workflows/java-failure-recovery.md) - 錯誤恢復流程

### ⚙️ 配置參考
- [maven-dependencies.md](configs/maven-dependencies.md) - 完整依賴清單
- [docker-compose.yml](configs/docker-compose.yml) - PostgreSQL + Redis 環境
- [ArchitectureTest.java](configs/ArchitectureTest.java) - ArchUnit 測試範本

---

## 檔案命名規則

- **編號前綴**: 使用原始編號（01-10）維持連貫性
- **拆分檔案**: 同編號 + 後綴說明主題
  - 例: `05-postgresql-basics.md`, `05-postgresql-advanced.md`
  - 例: `08-vavr-fundamentals.md`, `08-vavr-advanced.md`
- **便於識別**: 同編號檔案屬於相同主題系列

---

## 檔案組織原則

1. **字元限制**: 所有 rules/*.md 和 workflows/*.md ≤ 11000 字元
2. **平鋪結構**: rules/ 和 workflows/ 不使用子目錄
3. **前置依賴**: 透過 front matter 的 `prerequisites` 欄位標註
4. **交叉引用**: 透過 `related_rules` 和 `related_workflows` 欄位連結
5. **定位標籤**: `ideal`（理想架構）/ `migration`（遷移支援）/ `current-standard`（當前必須遵守）
6. **Workflow 依賴**: 所有 workflow 必須在 `required_rules` 中指定依賴的 rules

---

## 快速入門

1. **新人入職**: 從 [workflows/init.md](workflows/init.md) 開始
2. **設定 CI/CD**: 閱讀 [workflows/github-actions-pipeline.md](workflows/github-actions-pipeline.md)
3. **學習 Vavr**: 從 [rules/08-vavr-fundamentals.md](rules/08-vavr-fundamentals.md) 開始
4. **PostgreSQL 遷移**: 參考 [rules/05-postgresql-mybatis-integration.md](rules/05-postgresql-mybatis-integration.md)

---

## 定位說明

`.agent` 文檔定義的是 **理想目標架構**，用於引導專案技術演進：

- **理想架構** (`positioning: ideal`): PostgreSQL + Vavr + LambdaQueryWrapper
- **當前實作**: MySQL + 傳統 Java + XML Mapper（見實際程式碼）
- **遷移路徑** (`positioning: migration`): 提供從當前實作遷移至理想架構的指南

---

**最後更新**: 2025-01-12
