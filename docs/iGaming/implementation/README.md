# iGaming Implementation Design Documents

SmartAdmin v4.1.0 iGaming 基礎建設實作設計文檔索引。

**架構決策**：進化式模組化單體 (Evolutionary Modular Monolith)，不引入 Spring Cloud。

**計畫文件**：[Implementation Plan](../../../.claude/plans/stateless-hopping-noodle.md)

---

## Phase 0：Foundation Infrastructure（基礎設施準備）

| 文檔 | 行數 | 核心內容 |
|------|------|---------|
| [00-module-structure.md](00-module-structure.md) | 925 | 8 個新 iGaming 模組結構、依賴關係圖、build.gradle.kts 模板、ArchUnit 邊界規則 |
| [00-multi-tenant-design.md](00-multi-tenant-design.md) | 1,303 | TenantContext、MyBatis-Plus TenantLineInnerInterceptor、PostgreSQL RLS、@TenantIgnore 安全策略 |
| [00-event-driven-design.md](00-event-driven-design.md) | 1,144 | DomainEvent 基類、Kafka Topic 規劃、冪等消費三層防禦 (ADR-015)、事件流序列圖 |
| [00-database-schema.md](00-database-schema.md) | 1,108 | 完整 DDL (22+ 表)、命名規範 (ADR-001)、RLS 策略、索引策略、Flyway 遷移 |

## Phase 1：Core Financial Infrastructure（核心金融）

| 文檔 | 行數 | 核心內容 |
|------|------|---------|
| [01-wallet-design.md](01-wallet-design.md) | 1,722 | 無縫錢包六步原子操作、三層併發控制 (Redisson + SELECT FOR UPDATE + 樂觀鎖)、JetCache 二級快取、LiteFlow 有效投注額計算 |
| [01-payment-design.md](01-payment-design.md) | 1,213 | PSP 適配器策略模式、智能路由評分、提款 SAGA (LiteFlow Chain)、Resilience4j 熔斷器、三層對帳 |

## Phase 2：Player & Gaming Infrastructure（玩家與遊戲）

| 文檔 | 行數 | 核心內容 |
|------|------|---------|
| [02-player-design.md](02-player-design.md) | 866 | 玩家五狀態狀態機、KYC 三級漸進驗證、VIP 五級系統 (LiteFlow 規則)、PII 加密 + 盲索引 |
| [02-game-integration.md](02-game-integration.md) | 1,162 | GP 適配器介面、Seamless Wallet 回呼處理、三層對帳 (即時 + 輪詢 + 每日)、MockGameProvider |

## Phase 3：Risk & Compliance（風控與合規）

| 文檔 | 行數 | 核心內容 |
|------|------|---------|
| [03-risk-engine-design.md](03-risk-engine-design.md) | 1,639 | LiteFlow 五大風控組件、Kafka 事件消費、0-100 評分模型、風險提案工作流 (ADR-012)、服務抽取評估指標 |

---

## 統計摘要

| 指標 | 數值 |
|------|------|
| 文檔總數 | 9 份 |
| 總行數 | 11,082 行 |
| 涵蓋 Phase | 0-3 |
| SmartAdmin 模式遵循 | Vavr Option、Manager-only @Transactional、Constructor Injection |
| Mermaid 圖表 | flowchart、sequenceDiagram、stateDiagram-v2、gantt |
| 語言標準 | 繁體中文 + 英文技術術語保留 |

## SmartAdmin 架構規範檢查點

所有文檔中的程式碼範例均遵循以下規範：

- Service 層使用 `io.vavr.control.Option` / `Try` / `Either`（禁用 `java.util.Optional`）
- `@Transactional(rollbackFor = Throwable.class)` 僅在 Manager 層
- 建構子注入：`@RequiredArgsConstructor` + `private final`（禁用 `@Autowired`）
- Controller 不直接存取 Dao（Controller -> Service -> Manager -> Dao）
- `ResponseDTO.ok(data)` 統一 API 回應格式
- Boolean 欄位使用 `deleted` 非 `isDeleted`
- Mermaid 圖使用 `<br/>` 換行（stateDiagram-v2 除外，使用多行 `note` 區塊）

## 閱讀順序建議

1. **先讀計畫**: [Implementation Plan](../../../.claude/plans/stateless-hopping-noodle.md) - 理解整體架構決策與分階段策略
2. **Phase 0 按順序**: module-structure -> multi-tenant -> event-driven -> database-schema
3. **Phase 1-3 按需求**: 根據開發優先級閱讀對應 Phase 文檔

---

**文檔版本**: 1.0.0
**創建日期**: 2026-02-14
**維護團隊**: iGaming 架構組
