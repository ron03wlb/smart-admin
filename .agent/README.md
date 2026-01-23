# SmartAdmin 開發規範與導航總覽

> **目標架構**: Java 21 + Spring Boot 3.5.4 + PostgreSQL 16 + Vavr 0.10.4 + MyBatis Plus 3.5.12

---

## 1. 快速開始

### 主開發目錄

**後續主要開發目錄：**
- **後端**: `smart-admin-api-java21-springboot3/` - Java 21 + Spring Boot 3 版本
- **前端**: `smart-admin-web/` - TypeScript + Vue 3 版本

### 新人入職路徑

1. **環境設定**: 從 [workflows/init.md](workflows/init.md) 開始，完成 Java 21 + PostgreSQL + Redis 環境配置
2. **架構理解**: 閱讀 [rules/10-architecture-rules.md](rules/10-architecture-rules.md) 了解分層架構
3. **學習 Vavr**: 從 [rules/08-vavr-fundamentals.md](rules/08-vavr-fundamentals.md) 開始函數式編程
4. **PostgreSQL 特性**: 參考 [rules/05-postgresql-advanced.md](rules/05-postgresql-advanced.md) 學習 JSONB 和陣列
5. **開發實踐**: 閱讀 [開發規範總覽](docs/coding-standards-summary.md) 了解具體編碼標準

### 環境設定檢查

```bash
# 驗證前置條件
java -version      # 期望: Java 21+
mvn -version       # 期望: Maven 3.8+, Java 21
docker --version   # 期望: Docker installed

# 啟動開發環境
cd .agent/configs && docker-compose up -d postgres redis

# 驗證架構測試
cd smart-admin-api-java21-springboot3
mvn test -Dtest=ArchitectureTest
```

---

## 2. 規範導航

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
- [09-manager-layer.md](rules/09-manager-layer.md) - Manager 層規範

### 🔧 靜態分析工具規範
- [11-checkstyle-rules.md](rules/11-checkstyle-rules.md) - Checkstyle 代碼風格
- [12-pmd-rules.md](rules/12-pmd-rules.md) - PMD 代碼質量
- [13-spotbugs-rules.md](rules/13-spotbugs-rules.md) - SpotBugs 缺陷檢測
- [14-spotless-rules.md](rules/14-spotless-rules.md) - Spotless 格式化
- [15-error-prone-rules.md](rules/15-error-prone-rules.md) - Error Prone 編譯時檢查
- [16-jacoco-coverage-rules.md](rules/16-jacoco-coverage-rules.md) - JaCoCo 測試覆蓋率

### 📝 Git 規範
- [17-commit-message-conventions.md](rules/17-commit-message-conventions.md) - Commit Message 規範

### 🔄 開發工作流程
- [init.md](workflows/init.md) - 環境初始化設定
- [github-actions-pipeline.md](workflows/github-actions-pipeline.md) - GitHub Actions CI/CD
- [quality-gates-local-ci.md](workflows/quality-gates-local-ci.md) - 本地 Quality Gate、GitLab CI
- [tdd-workflow.md](workflows/tdd-workflow.md) - 測試驅動開發
- [java-failure-recovery.md](workflows/java-failure-recovery.md) - 錯誤恢復流程

### ⚙️ 配置參考
- [docker-compose.yml](configs/docker-compose.yml) - PostgreSQL + Redis 環境
- [ArchitectureTest.java](configs/ArchitectureTest.java) - ArchUnit 測試範本

---

## 3. 專案概覽

### 後端結構
```
smart-admin-api-java21-springboot3/
├── sa-base/                          # 基礎設施庫 (357 個 Java 檔案)
│   ├── common/                       # 核心框架程式碼
│   │   ├── domain/                  # 基礎 DTO (ResponseDTO, PageParam 等)
│   │   ├── util/                    # 工具類 (SmartBeanUtil, SmartPageUtil 等)
│   │   ├── constant/                # 系統常數
│   │   └── config/                  # 26+ Spring 配置類
│   └── module/support/              # 26 個支援模組
│
└── sa-admin/                         # 主應用程式 (202 個 Java 檔案)
    ├── module/
    │   ├── business/                # 業務模組（你的程式碼放這裡）
    │   └── system/                  # 系統模組（員工、角色等）
    └── config/                      # 應用專屬配置
```

### 前端結構
```
smart-admin-web/
├── src/
│   ├── api/                         # API 接口定義
│   ├── components/                  # 公共元件
│   ├── views/                       # 頁面視圖
│   └── store/                       # Pinia 狀態管理
└── package.json                     # 專案配置
```

---

## 4. 技術棧與開發規範

### 📚 詳細文檔
- **技術棧詳情**: [../.claude/shared/knowledge/project-architecture.md](../.claude/shared/knowledge/project-architecture.md#technology-stack)
- **開發規範總覽**: [docs/coding-standards-summary.md](docs/coding-standards-summary.md)
- **快速參考**: [../CLAUDE.md](../CLAUDE.md)
- **常見問題**: [docs/faq-troubleshooting.md](docs/faq-troubleshooting.md)

### 核心技術要點

**分層架構（強制）**:
```
Controller → Service → Manager → Dao → Database
```

**Vavr 函數式編程**:
```java
// Option 替代 null
Option<User> user = Option.of(userMapper.selectById(id));

// Try 替代 try-catch
Try<User> result = Try.of(() -> userService.create(dto));
```

**ResponseDTO 模式**:
```java
return ResponseDTO.ok(data);
return ResponseDTO.error(ErrorCode.XXX);
```

---

## 5. AI 規則系統

### 定位說明

`.agent` 文檔定義的是 **理想目標架構**，用於引導專案技術演進：

- **理想架構** (`positioning: ideal`): PostgreSQL + Vavr + LambdaQueryWrapper
- **當前實作**: MySQL + 傳統 Java + XML Mapper（見實際程式碼）
- **遷移路徑** (`positioning: migration`): 提供從當前實作遷移至理想架構的指南

### 檔案組織原則

1. **字元限制**: 所有 rules/*.md 和 workflows/*.md ≤ 11000 字元（特殊索引檔案除外）
2. **平鋪結構**: rules/ 和 workflows/ 不使用子目錄
3. **前置依賴**: 透過 front matter 的 `prerequisites` 欄位標註
4. **交叉引用**: 透過 `related_rules` 和 `related_workflows` 欄位連結
5. **定位標籤**: `ideal`（理想架構）/ `migration`（遷移支援）/ `current-standard`（當前必須遵守）

### AI 決策矩陣

所有 AI 規則的入口：[rules/00-ai-decision-matrix.md](rules/00-ai-decision-matrix.md)

**核心檢查清單**:
- ✅ Service 層必須使用 `io.vavr.control.Option` 而非 `java.util.Optional`
- ✅ Controller 不能直接存取 Repository
- ✅ 禁止欄位注入（使用建構函數注入）
- ✅ @Transactional/@Cacheable 只在 Manager 層
- ✅ 命名規範（Controller/Service/Dao 後綴）
- ✅ 測試覆蓋率 ≥ 80%

---

## 6. 質量保證

### ArchUnit 測試

```bash
# 執行完整架構測試
mvn test -Dtest=ArchitectureTest

# 執行特定測試
mvn test -Dtest=ArchitectureTest#serviceUsesVavrOption
mvn test -Dtest=ArchitectureTest#layerDependencies
```

**強制規則（阻斷 PR）**:
- ✅ Service 層使用 Vavr Option
- ✅ Controller 不直接訪問 Repository
- ✅ 構造函數注入
- ✅ @Transactional 只在 Manager 層

### 質量門禁標準

```yaml
Quality Gate 通過條件:
  ✅ ArchUnit:        100% 通過 (零容忍)
  ✅ Checkstyle:      0 errors
  ✅ PMD:             0 violations
  ✅ SpotBugs:        0 bugs
  ✅ 測試覆蓋率:       ≥ 80% (Line), ≥ 70% (Branch)
```

**詳細規範**: [workflows/quality-gates-local-ci.md](workflows/quality-gates-local-ci.md)

---

## 7. 快速導航

### 常用資源
- **快速參考**: [../CLAUDE.md](../CLAUDE.md) - 常用命令、模式速查
- **常見問題**: [docs/faq-troubleshooting.md](docs/faq-troubleshooting.md) - 故障排除
- **官方文件**: https://smartadmin.vip
- **線上預覽**: https://preview.smartadmin.vip

### Workflow 索引
- **Workflow 總覽**: [workflows/00-workflow-index.md](workflows/00-workflow-index.md)
- **AI 決策矩陣**: [rules/00-ai-decision-matrix.md](rules/00-ai-decision-matrix.md)

---

**最後更新**: 2025-01-21
**版本**: v3.0 (拆分重構版)

**記住**: 我們推崇高品質的程式碼。身為開發者，程式碼即利劍！ ⚔️
