# SmartAdmin Translation Glossary

**Purpose**: Ensure consistent terminology in English translation of .agent/ documentation

**Last Updated**: 2026-01-24
**Version**: 1.0.0

---

## Architecture Terms

| 繁體中文 | English | Context / Notes |
|---------|---------|-----------------|
| 分層架構 | Layered Architecture | Controller → Service → Manager → Dao pattern |
| 控制層 / Controller 層 | Controller Layer | REST API endpoints, HTTP handling, validation |
| 服務層 / Service 層 | Service Layer | Business logic orchestration |
| 管理層 / Manager 層 | Manager Layer | Transaction management, caching |
| 持久層 / 數據訪問層 | Data Access Layer / Dao Layer | Database operations, MyBatis Mappers |
| 領域層 | Domain Layer | Entities, DTOs, VOs |
| 表現層 | Presentation Layer | Same as Controller Layer |
| 業務層 | Business Layer | Same as Service Layer |
| 依賴方向 | Dependency Direction | Direction of layer dependencies |
| 跨層訪問 | Cross-Layer Access | Violating layer boundaries |
| 反向依賴 | Reverse Dependency | Lower layer depending on upper layer (anti-pattern) |
| 橫向調用 | Lateral Invocation | Calling peers at same layer (anti-pattern) |
| 模塊化設計 | Modular Design | Separation of concerns, module boundaries |
| 架構邊界 | Architectural Boundaries | Defined interfaces between layers |
| 架構約束 | Architectural Constraints | Enforced rules via ArchUnit |
| 架構違規 | Architectural Violation | Breaking layered architecture rules |

## SmartAdmin Domain Objects

| 繁體中文 | English | Context / Notes |
|---------|---------|-----------------|
| 實體 / Entity | Entity | Database mapping (e.g., UserEntity) |
| 數據傳輸對象 / DTO | Data Transfer Object (DTO) | Internal data transfer |
| 值對象 / VO | Value Object (VO) | Response representation |
| 表單對象 / Form | Form Object | Request input (e.g., UserAddForm, UserQueryForm) |
| 領域物件 | Domain Object | Generic term for Entity/DTO/VO/Form |
| 查詢表單 | Query Form | Search/filter parameters (e.g., UserQueryForm) |
| 新增表單 | Add Form | Create operation input (e.g., UserAddForm) |
| 更新表單 | Update Form | Update operation input (e.g., UserUpdateForm) |
| 回應模式 | Response Pattern | ResponseDTO.ok() / ResponseDTO.error() |
| 分頁結果 | Page Result | Paginated query result (PageResult) |
| 分頁參數 | Page Parameter | Pagination input (PageParam) |
| 審計欄位 | Audit Fields | created_time, created_by, updated_time, updated_by, deleted_flag |

## Dependency Injection

| 繁體中文 | English | Context / Notes |
|---------|---------|-----------------|
| 依賴注入 | Dependency Injection | Constructor injection pattern |
| 構造函數注入 / 建構函數注入 | Constructor Injection | Preferred over field injection |
| 字段注入 | Field Injection | Anti-pattern using @Autowired on fields |
| 必需參數構造器 | Required Args Constructor | @RequiredArgsConstructor pattern |

## PostgreSQL Features

| 繁體中文 | English | Context / Notes |
|---------|---------|-----------------|
| 陣列類型 | Array Type | PostgreSQL native array support |
| 動態數據 | Dynamic Data | Flexible schema using JSONB |
| 審計日誌 | Audit Log | Change tracking records |
| 通用倒排索引 | Generalized Inverted Index (GIN) | JSONB index type |
| 公用表表達式 / CTE | Common Table Expression (CTE) | WITH clause for complex queries |
| 窗口函數 | Window Function | Ranking, running totals |
| 全文搜索 | Full-Text Search | PostgreSQL text search capabilities |
| 遷移 | Migration | MySQL → PostgreSQL migration |
| 類型處理器 / TypeHandler | Type Handler | MyBatis custom type conversion |
| 建表 | Table Creation | Schema definition |
| 索引 | Index | Database index (B-tree, GIN, etc.) |
| 主鍵 | Primary Key | Unique identifier |
| 自增主鍵 | Auto-Increment Primary Key | BIGSERIAL in PostgreSQL |

## Vavr Functional Programming

| 繁體中文 | English | Context / Notes |
|---------|---------|-----------------|
| 函數式編程 | Functional Programming | Vavr library for Java |
| 模式匹配 | Pattern Matching | Vavr pattern matching capabilities |
| 鏈式調用 | Method Chaining | Fluent API style (e.g., map().flatMap()) |
| 空值處理 | Null Handling | Using Option instead of null checks |
| 異常處理 | Exception Handling | Using Try instead of try-catch |
| 函數組合 | Function Composition | Composing multiple operations |
| 不可變集合 | Immutable Collection | Vavr immutable data structures |
| 惰性求值 | Lazy Evaluation | Deferred computation |
| 副作用 | Side Effect | Operations that modify state |

## MyBatis Plus

| 繁體中文 | English | Context / Notes |
|---------|---------|-----------------|
| 分頁查詢 | Paginated Query | Page-based data retrieval |
| 條件構造器 | Query Wrapper | Building dynamic SQL conditions |
| Lambda 查詢包裝器 | Lambda Query Wrapper | Type-safe query builder |
| 批次操作 | Batch Operation | saveBatch, updateBatchById |
| 邏輯刪除 | Logical Delete | Soft delete using deleted_flag |
| 自動填充 | Auto Fill | Automatic population of audit fields |
| 類型枚舉 / IEnum | Type Enumeration (IEnum) | Enum mapping to database values |
| 持久層框架 | Persistence Framework | ORM framework (MyBatis Plus) |

## Quality & Testing

| 繁體中文 | English | Context / Notes |
|---------|---------|-----------------|
| 質量門禁 | Quality Gate | CI/CD validation thresholds |
| 測試覆蓋率 | Test Coverage | Code coverage metrics (line, branch) |
| 測試驅動開發 / TDD | Test-Driven Development (TDD) | Red-Green-Refactor cycle |
| 單元測試 | Unit Test | Isolated component testing |
| 集成測試 | Integration Test | Cross-component testing |
| 端到端測試 / E2E | End-to-End Testing (E2E) | Full workflow testing |
| 架構測試 | Architecture Test | ArchUnit validation |
| 靜態分析 | Static Analysis | Checkstyle, PMD, SpotBugs, etc. |
| 代碼質量 | Code Quality | Overall code health metrics |
| 代碼風格 | Code Style | Formatting and naming conventions |
| 缺陷檢測 | Bug Detection | SpotBugs analysis |
| 編譯時檢查 | Compile-Time Check | Error Prone validation |
| 格式化 | Formatting | Code formatting (Spotless) |
| 阻斷級別 | Blocking Level | CI/CD failure severity |
| 零容忍 | Zero Tolerance | No violations allowed |

## Development Workflow

| 繁體中文 | English | Context / Notes |
|---------|---------|-----------------|
| 工作流程 | Workflow | Development process |
| 環境設定 / 環境配置 | Environment Setup | Development environment initialization |
| 環境初始化 | Environment Initialization | Initial project setup |
| 前置條件 / 前置依賴 | Prerequisites | Required knowledge/setup before starting |
| 交叉引用 | Cross-Reference | Links between documentation files |
| 決策樹 / 決策矩陣 | Decision Tree / Decision Matrix | AI decision logic for code generation |
| 場景映射 | Scenario Mapping | Use case to rule mapping |
| 請求分類 | Request Classification | Categorizing user requests |
| 錯誤恢復 / 故障排除 | Error Recovery / Troubleshooting | Fixing build/runtime errors |
| 本地 CI | Local CI | Local continuous integration checks |
| 持續集成 / CI/CD | Continuous Integration / Deployment (CI/CD) | Automated build and deploy pipeline |
| 編譯錯誤 | Compilation Error | Build-time errors |
| 運行時錯誤 | Runtime Error | Execution-time errors |

## Security (OWASP)

| 繁體中文 | English | Context / Notes |
|---------|---------|-----------------|
| 安全檢查 | Security Check | Vulnerability scanning |
| 安全漏洞 | Security Vulnerability | Exploitable weakness |
| 注入攻擊 / SQL 注入 | Injection Attack / SQL Injection | OWASP A03 |
| 跨站腳本 / XSS | Cross-Site Scripting (XSS) | OWASP A07 |
| 跨站請求偽造 / CSRF | Cross-Site Request Forgery (CSRF) | Session hijacking attack |
| 密碼加密 | Password Encryption | BCrypt hashing |
| 傳輸加密 | Transport Encryption | HTTPS, TLS |
| 參數綁定 | Parameter Binding | Prepared statements |
| 輸出跳脫 | Output Escaping | XSS prevention |
| 權限控制 | Permission Control | Access control (@SaCheckPermission) |

## Code Review & Standards

| 繁體中文 | English | Context / Notes |
|---------|---------|-----------------|
| 代碼審查 / Code Review | Code Review | Peer review process |
| 命名規範 | Naming Convention | Variable, class, method naming rules |
| OOP 原則 | OOP Principles | Object-Oriented Programming principles |
| 並發規則 / 並發安全 | Concurrency Rules / Thread Safety | Multi-threading safety |
| 異常與日誌 | Exception and Logging | Error handling standards |
| 提交訊息 / Commit Message | Commit Message | Git commit message format |
| 約定式提交 | Conventional Commits | Structured commit message format |
| 分支管理 | Branch Management | Git branching strategy |
| 功能分支 | Feature Branch | feature/* branches |
| 緊急修復分支 | Hotfix Branch | hotfix/* branches |
| 魔法數字 | Magic Number | Hardcoded literals (anti-pattern) |
| 見名知意 | Self-Explanatory Names | Descriptive naming |
| 布林值 | Boolean Value | true/false fields |

## Configuration & Build

| 繁體中文 | English | Context / Notes |
|---------|---------|-----------------|
| 配置管理 | Configuration Management | Application settings |
| 基礎配置 | Base Configuration | sa-base.yaml settings |
| 應用配置 | Application Configuration | application.yaml settings |
| 環境 | Environment | dev, test, pre, prod |
| 建置命令 / 構建命令 | Build Command | mvn/gradle commands |
| 套件 / 包 | Package | Java package namespace |
| 模組 | Module | Gradle/Maven module |
| 依賴 | Dependency | Library dependencies |

## General Terms

| 繁體中文 | English | Context / Notes |
|---------|---------|-----------------|
| 理想架構 | Ideal Architecture | Target state (positioning: ideal) |
| 當前實作 / 當前實現 | Current Implementation | Existing codebase state |
| 遷移路徑 | Migration Path | Transition strategy (positioning: migration) |
| 強制 | Mandatory / Required | Must be enforced |
| 推薦 / 建議 | Recommended | Best practice but not enforced |
| 禁止 | Forbidden / Prohibited | Must not be done |
| 允許 | Allowed / Permitted | Acceptable approach |
| 新人入職 | New Developer Onboarding | Getting started guide |
| 快速開始 | Quick Start | Initial setup instructions |
| 詳細文檔 / 完整規範 | Detailed Documentation | Full specification |
| 範例 / 示例 | Example | Code sample |
| 檢查清單 / 檢查列表 | Checklist | Validation items |
| 速查卡 / 快速參考 | Quick Reference Card | Cheat sheet |
| 導航 / 總覽 | Navigation / Overview | Table of contents |
| 用戶請求 | User Request | User's ask to AI |
| 生成代碼 | Code Generation | AI creating code |
| 自動修正 | Auto-Fix | Automatic code correction |
| 詢問用戶 | Ask User | Require user confirmation |
| 優先級 | Priority | P0 (highest) to P3 (lowest) |
| 衝突解決 | Conflict Resolution | Handling rule conflicts |
| 關鍵詞 | Keyword | Trigger word for rule application |
| 檢查點 | Checkpoint | Validation point |
| 字元限制 | Character Limit | 11000 char limit per file |
| 平鋪結構 | Flat Structure | No nested subdirectories |
| 定位標籤 | Positioning Tag | ideal/migration/current-standard |
| 技術棧 | Technology Stack | Tech stack components |
| 專案概覽 | Project Overview | High-level structure |
| 返回 | Return to | Navigation link back |
| 最後更新 | Last Updated | Document modification date |
| 版本 | Version | Document version number |

---

**Total Terms**: 175 technical terms across 12 categories

**Translation Principles**:
1. **Consistency**: Same Chinese term always maps to same English translation
2. **Technical Accuracy**: Use industry-standard English terminology
3. **Context Preservation**: Maintain technical meaning in context
4. **Abbreviations**: Spell out on first use, then use common abbreviations (DTO, VO, CTE, etc.)
5. **Code Terms**: Keep Java/SQL keywords unchanged (e.g., @Transactional, JSONB)
