# ADR 013: 分佈式錢包交易的 Manager 層邊界

**狀態**: ✅ 已採納（2026-02-13）

**決策者**: iGaming 技術團隊

**相關文檔**:
- [Financial_Implementation.md](../02_Finance_Service/04_Financial_Implementation.md) — Section 2.3 WalletManager
- [Seamless_Wallet_Technical.md](../02_Finance_Service/03_Seamless_Wallet_Technical.md) — Section 5 ConcurrencyManager
- [F04-architecture-rules.md](../../../.agent/rules/foundation/F04-architecture-rules.md) — SmartAdmin 架構規則

---

## 背景

iGaming 平台的錢包系統涉及分佈式操作（Redis Lua script + DB 持久化），原始文件未明確標注這些操作應放置在 SmartAdmin 分層架構的哪一層。這導致兩個問題：

1. `debitWallet()` 方法缺少類別宣告，可能被誤放在 Service 層
2. Service 層程式碼使用 `java.util.Optional` 而非 SmartAdmin 規定的 `io.vavr.control.Option`

---

## 決策

**所有涉及分佈式鎖協調和 @Transactional 的錢包操作，必須放置在 Manager 層。**

### 具體規則

| 操作類型 | 層級 | 類別範例 | 備註 |
|---------|------|---------|------|
| Redis Lua 原子扣款 + DB 持久化 | **Manager** | `WalletManager` | `@Transactional(rollbackFor = Throwable.class)` |
| Redisson 分佈式鎖協調 | **Manager** | `ConcurrencyManager` | `@Component` + `@RequiredArgsConstructor` |
| 餘額查詢（單表唯讀） | **Service** 或 **Dao** | `WalletService` | Service 可直接呼叫 Dao |
| 冪等性檢查（Redis 快取） | **Service** | `IdempotencyGuard` | 使用 `io.vavr.control.Option` |

### 合規標準

- PCI-DSS v4 Req 6.2.1: 安全開發生命週期 — 金融交易邏輯必須遵循分層設計
- ISO 27001 A.14.2: 安全設計原則

---

## 後果

**正面**:
- 架構一致性：所有 @Transactional 操作統一在 Manager 層，ArchUnit 可自動強制
- 可測試性：Manager 可獨立測試（不依賴 Controller/Service 上下文）
- 審計合規：分層清晰便於 MGA/UKGC 審計時追溯交易邏輯

**負面**:
- 增加一層間接呼叫（Service → Manager → Dao），但效能影響可忽略
