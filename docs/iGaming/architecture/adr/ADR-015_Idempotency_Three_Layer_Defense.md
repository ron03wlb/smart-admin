# ADR 015: 冪等策略三層防禦標準

**狀態**: ✅ 已採納（2026-02-13）

**決策者**: iGaming 技術團隊

**相關文檔**:
- [Seamless_Wallet_Technical.md](../02_Finance_Service/Seamless_Wallet_Technical.md) — Section 4 冪等三層防禦 + Section 5.3 鎖順序協議
- [Financial_Implementation.md](../02_Finance_Service/Financial_Implementation.md) — Section 2.3 WalletManager Redis 快取層
- [Seamless_Wallet_Analysis.md](../02_Finance_Service/Seamless_Wallet_Analysis.md) — Section 3 逾時重試時序圖

---

## 背景

iGaming 平台的無縫錢包系統需要處理遊戲供應商的重複請求（網路超時重試、回調重複送達）。原始設計在三個不同文件中描述了冪等策略，但缺乏：

1. 三層之間的統一說明
2. 冪等檢查與分佈式鎖的交互順序保證
3. 文件間的交叉引用

---

## 決策

**所有金融交易必須通過三層冪等防禦，且冪等檢查在分佈式鎖外執行。**

### 三層架構

| 層 | 機制 | 延遲 | 位置 | TTL |
|---|------|-----|------|-----|
| 第 1 層 | Redis 快取查詢 | < 5ms | Service 層（IdempotencyGuard） | 1 小時 |
| 第 2 層 | DB 唯一約束（`t_transaction.tx_id UNIQUE`） | < 50ms | Manager 層（@Transactional 範圍內） | 永久 |
| 第 3 層 | DB 回退查詢（`SELECT WHERE tx_id`） | < 100ms | Manager 層 | 永久 |

### 鎖順序保證

```
冪等檢查（鎖外）→ 玩家級鎖（外層）→ 狀態轉換 + DB 持久化（鎖內）→ 釋放鎖
```

- 重複請求在第 1 層即被攔截，不消耗鎖資源
- 狀態機轉換（OPEN → CLOSED）必須在鎖保護範圍內完成
- 所有 DB 寫入在 Manager 層的 `@Transactional` 範圍內

### TTL 標準化

統一冪等 TTL 為 **3600 秒（1 小時）**，與有效投注額計算模組一致。

---

## 後果

**正面**:
- 零重複扣款：三層防禦確保即使 Redis 失效，DB 唯一約束仍能攔截
- 高吞吐量：重複請求在第 1 層攔截（< 5ms），不影響主流程效能
- 架構清晰：三個文件已建立交叉引用，維護者可快速定位完整設計

**負面**:
- Redis 故障時延遲增加：第 1 層失效後，所有請求需走第 2 層（< 50ms）
- TTL 窗口風險：1 小時 TTL 過後，如果 DB 也沒記錄，理論上可能重複處理（但概率極低）
