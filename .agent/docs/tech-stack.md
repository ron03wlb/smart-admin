# 技術棧

> **目標架構**: Java 21 + Spring Boot 3.5.4 + PostgreSQL 16 + Vavr 0.10.4 + MyBatis Plus 3.5.12

---

## 後端技術棧 (smart-admin-api-java21-springboot3)

| 技術 | 版本 | 用途 | 規則檔案 |
|------|------|------|----------|
| Java | 21 | 核心語言 | [01-naming-conventions.md](../rules/01-naming-conventions.md) |
| Spring Boot | 3.5.4 | 應用框架 | [10-architecture-rules.md](../rules/10-architecture-rules.md) |
| Sa-Token | 1.44.0 | 認證授權 | - |
| MyBatis Plus | 3.5.12 | ORM 框架 | [09-mybatis-plus-core.md](../rules/09-mybatis-plus-core.md) |
| PostgreSQL Driver | 42.7.5 | 資料庫驅動 | [05-postgresql-basics.md](../rules/05-postgresql-basics.md) |
| Vavr | 0.10.4 | 函數式編程 | [08-vavr-fundamentals.md](../rules/08-vavr-fundamentals.md) |
| Knife4j | 4.6.0 | API 文檔 | - |
| Druid | 1.2.25 | 資料庫連接池 | - |
| Redisson | 3.50.0 | Redis 快取和分散式 | - |
| P6Spy | 3.9.1 | SQL 監控 | - |

---

## 前端技術棧 (smart-admin-web)

| 技術 | 版本 | 用途 | 規則檔案 |
|------|------|------|----------|
| Vue | 3.4.27 | 前端框架 | - |
| TypeScript | 5.6.3 | 型別系統 | - |
| Vite | 5.2.12 | 建置工具 | - |
| Ant Design Vue | 4.2.5 | UI 元件庫 | - |
| Pinia | 2.1.7 | 狀態管理 | - |
| Vue Router | 4.3.2 | 路由管理 | - |
| Node | >= 18 | 執行環境 | - |

---

## 技術選型說明

### 後端核心技術

**Java 21**
- 使用 Virtual Threads、Pattern Matching 等新特性
- 詳見: [01-naming-conventions.md](../rules/01-naming-conventions.md)

**Vavr 0.10.4**
- 函數式編程庫，提供 Option、Try、Either 等容器
- 替代傳統的 null 檢查和 try-catch
- 詳見: [08-vavr-fundamentals.md](../rules/08-vavr-fundamentals.md)

**PostgreSQL 16**
- 支持 JSONB、陣列、CTE、窗口函數等高級特性
- 理想架構目標資料庫 (當前實作可能使用 MySQL)
- 詳見: [05-postgresql-advanced.md](../rules/05-postgresql-advanced.md)

**MyBatis Plus 3.5.12**
- 推薦使用 LambdaQueryWrapper 進行類型安全的查詢
- 詳見: [09-mybatis-plus-core.md](../rules/09-mybatis-plus-core.md)

### 前端核心技術

**Vue 3 + TypeScript**
- Composition API
- 完整的類型檢查支持

**Ant Design Vue 4**
- 企業級 UI 元件庫
- 豐富的表單、表格、布局元件

---

**最後更新**: 2025-01-21
**返回**: [README.md](../README.md)
