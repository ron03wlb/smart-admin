---
trigger: always_on
description: AI 決策矩陣 - 場景到規則的映射
tags: [meta, ai-guide, decision-tree, orchestration]
positioning: current-standard
ai_role: orchestrator
auto_apply: true
ask_before_fix: false
related_rules:
  - rules/01-naming-conventions.md
  - rules/10-architecture-rules.md
  - rules/08-vavr-fundamentals.md
  - rules/11-checkstyle-rules.md
  - rules/12-pmd-rules.md
  - rules/13-spotbugs-rules.md
  - rules/14-spotless-rules.md
  - rules/15-error-prone-rules.md
  - rules/16-jacoco-coverage-rules.md
  - rules/17-commit-message-conventions.md
last_updated: 2025-01-17
---

# AI 決策矩陣

> **目的**: 幫助 AI 快速識別用戶請求場景，並自動應用正確的規則組合

## 🤖 AI 指令區塊

### 何時應用此規則
- ✅ **永遠**: 此規則在每次響應前都應該被查閱
- ✅ 用戶發起任何代碼生成請求時
- ✅ 用戶要求 Code Review 時
- ✅ 用戶報告錯誤需要診斷時

### 強制執行檢查清單
在響應用戶請求前，必須：
- [ ] 識別請求類型（生成代碼/審查/診斷/查詢）
- [ ] 確定涉及的代碼層次（Controller/Service/Repository/Entity）
- [ ] 列出需要應用的所有規則
- [ ] 按優先級順序應用規則

---

## AI 決策樹

```
用戶請求分類
├─ 1️⃣ 生成新代碼
│   ├─ Controller: [01-naming, 10-architecture, 04-exception-logging]
│   ├─ Service: [01-naming, 02-oop, 08-vavr, 10-architecture]
│   ├─ Repository/Mapper: [01-naming, 09-mybatis-plus, 05-postgresql]
│   └─ Entity: [01-naming, 05-postgresql, 09-mybatis-plus]
│
├─ 2️⃣ Code Review
│   ├─ 架構違規: [10-architecture-rules] → ArchUnit
│   ├─ 命名規範: [01-naming] → Checkstyle
│   ├─ Vavr 使用: [08-vavr-*] → Option/Try 檢查
│   ├─ OOP 原則: [02-oop-principles]
│   ├─ 並發安全: [03-concurrency-rules]
│   └─ 安全檢查: [07-owasp-top10-*]
│
├─ 3️⃣ 數據庫操作
│   ├─ 建表: [05-postgresql-basics]
│   ├─ JSONB/CTE/窗口函數: [05-postgresql-advanced]
│   ├─ MyBatis Mapper: [09-mybatis-plus-*]
│   └─ MySQL 遷移 PG: [05-postgresql-mybatis-integration]
│
├─ 4️⃣ 錯誤診斷
│   ├─ 編譯錯誤: Workflow [java-failure-recovery]
│   ├─ ArchUnit 失敗: [10-architecture] + 對應規則
│   ├─ Quality Gate 失敗: Workflow [quality-gates-local-ci]
│   └─ 運行時錯誤: [08-vavr (Try), 04-exception-logging]
│
├─ 5️⃣ 靜態分析工具
│   ├─ Checkstyle: [11-checkstyle-rules] → ./gradlew checkstyleMain
│   ├─ PMD: [12-pmd-rules] → ./gradlew pmdMain
│   ├─ SpotBugs: [13-spotbugs-rules] → ./gradlew spotbugsMain
│   ├─ Spotless: [14-spotless-rules] → ./gradlew spotlessApply
│   ├─ Error Prone: [15-error-prone-rules]
│   └─ JaCoCo: [16-jacoco-coverage-rules]
│
├─ 6️⃣ 知識查詢
│   ├─ PostgreSQL: [05-postgresql-*]
│   ├─ Vavr: [08-vavr-*]
│   ├─ MyBatis Plus: [09-mybatis-plus-*]
│   └─ 架構設計: [10-architecture-rules]
│
└─ 7️⃣ Git 操作
    └─ Commit Message: [17-commit-message-conventions]
```

---

## 📋 強制檢查矩陣

| 代碼類型          | 必須應用的規則                   | 自動化工具 | 阻斷級別 |
| ----------------- | -------------------------------- | ---------- | -------- |
| Service 新方法    | 08-vavr (Option/Try)             | ArchUnit   | 🚫 阻斷PR |
| Service 新方法    | 10-architecture (構造函數注入)   | ArchUnit   | 🚫 阻斷PR |
| Controller 新方法 | 10-architecture (不直接訪問Repo) | ArchUnit   | 🚫 阻斷PR |
| 任何新代碼        | 01-naming (命名規範)             | Checkstyle | 🚫 阻斷PR |
| 任何新代碼        | 測試覆蓋率 ≥ 80%                 | JaCoCo     | 🚫 阻斷PR |

### 質量門禁標準

```yaml
Quality Gate 通過條件:
  ✅ ArchUnit:        100% 通過 (零容忍)
  ✅ Checkstyle:      0 errors
  ✅ PMD:             0 violations
  ✅ SpotBugs:        0 bugs
  ✅ 測試覆蓋率:       ≥ 80% (Line), ≥ 70% (Branch)
  ✅ SonarQube:       0 Blocker/Critical issues
```

---

## 🔧 AI 自動修正策略

### 可以自動修正 (auto_apply: true)
- Optional → Option（Vavr）
- try-catch → Try.of()
- @Autowired 字段注入 → @RequiredArgsConstructor
- 命名不規範（類名/方法名/常量）

### 需要詢問用戶 (ask_before_fix: true)
- Service 方法缺少 @Transactional
- Controller 直接訪問 Repository
- Entity 缺少 JSONB TypeHandler
- 複雜業務邏輯在 Controller

### 禁止自動修正
- 🚫 業務邏輯錯誤
- 🚫 安全漏洞
- 🚫 數據庫遷移
- 🚫 刪除代碼

---

## 📊 規則優先級

| 優先級  | 規則類型                     | 說明     |
| ------- | ---------------------------- | -------- |
| P0 最高 | 10-architecture, 07-owasp-*  | 架構安全 |
| P1 高   | 08-vavr, 01-naming, 02-oop   | 代碼質量 |
| P2 中   | 09-mybatis, 05-postgresql    | 最佳實踐 |
| P3 低   | 03-concurrency, 06-sonarqube | 優化建議 |

### 衝突解決原則
1. **安全 > 性能 > 可讀性**
2. **架構約束 > 代碼風格**
3. **新代碼高標準 > 舊代碼兼容**

---

## 📚 快速參考卡

| 關鍵詞            | 立即應用的規則             | 檢查點                    |
| ----------------- | -------------------------- | ------------------------- |
| "創建 Controller" | 01, 10, 04                 | RESTful, 不直接訪問 Repo  |
| "創建 Service"    | 01, 02, 08, 10             | Option/Try, 構造注入      |
| "創建 Mapper"     | 01, 09, 05                 | LambdaQueryWrapper        |
| "創建 Entity"     | 01, 05                     | @TableName, JSONB/Array   |
| "Code Review"     | 10, 08, 01                 | ArchUnit, Vavr, 命名      |
| "JSONB"           | 05-advanced, 09-postgresql | TypeHandler               |
| "異常處理"        | 08-vavr, 04                | Try.of(), 日誌記錄        |
| "事務"            | 09-manager-layer           | @Transactional 在 Manager |
| "commit"          | 17-commit-message          | Conventional Commits 格式 |

---

## ✅ 核心檢查清單

### 基礎檢查
- [ ] 類名 UpperCamelCase，方法名 lowerCamelCase
- [ ] 構造函數注入（無 @Autowired 字段）

### 架構檢查
- [ ] Controller 不直接訪問 Repository
- [ ] @Transactional/@Cacheable 只在 Manager 層

### 驗證命令
```bash
./gradlew check                    # 完整檢查
mvn test -Dtest=ArchitectureTest   # 架構測試
```

---

**此決策矩陣是所有規則的總入口，AI 在處理任何請求前都應該先查閱此文檔。**
