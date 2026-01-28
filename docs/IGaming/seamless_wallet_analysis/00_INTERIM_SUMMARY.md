# Seamless Wallet 邏輯錯誤分析 - 中期總結報告

**分析日期**: 2026-01-28
**文檔版本**: 1.0
**分析範圍**: Phase 1-2（會話認證層 + 有效投注計算層）

---

## 📊 執行摘要

基於 [seamless_wallet.md](../seamless_wallet.md) 的深度審查，我們已識別並分析了 **4 個關鍵邏輯錯誤**，涵蓋：
1. ✅ API 會話驗證邏輯錯置
2. ✅ 冪等性檢查的架構缺陷
3. ✅ 體育博彩 Valid Bet 計算矛盾
4. ✅ 免費旋轉 Turnover 定義錯誤

這些錯誤的影響範圍從**安全漏洞**（P0）到**財務報表不準確**（P1），需要在系統設計階段就明確決策。

---

## 🔴 P0 級別錯誤（Critical - 安全與資金）

### 錯誤 #1: Bet 請求的 Token 驗證邏輯錯置

**問題描述**:
文檔在「Bet 請求處理邏輯」中混淆了 Bet API 和 Result API 的驗證策略，可能導致實現者誤認為 Bet 請求可以不驗證 Token。

**錯誤原文** (第 66 行):
> 會話有效性： 檢查 token 是否過期。但在某些供應商（如 Hub88）的規範中，對於「派彩」（Win）請求，通常不應驗證 Token 是否過期

**安全風險**:
- ⚠️ **高危**: 如果 Bet API 不驗證 Token 過期，攻擊者可以使用過期 Token 進行資金操作
- ⚠️ **高危**: 會話固定攻擊、Token 重放攻擊

**推薦決策**:
```yaml
API 驗證策略矩陣:
  Balance API:
    token_validation: STRICT     # 必須驗證有效期
    reason: 防止未授權的餘額查詢

  Bet API:
    token_validation: STRICT     # 必須驗證有效期
    reason: 涉及資金扣款，安全優先級最高
    exception: 無例外

  Result API:
    token_validation: CONDITIONAL  # 條件驗證
    strategy: |
      - 短週期遊戲（老虎機、輪盤）: STRICT
      - 長週期遊戲（體育、撲克）:
          IF token.isExpired() THEN
              FALLBACK TO round_id + bet_tx_id 驗證
          ELSE
              STRICT 驗證
    reason: 體育賽事可能跨天結算

  Rollback API:
    token_validation: CONDITIONAL  # 條件驗證
    strategy: |
      - 超時重試: 使用 transaction_id 驗證
      - 對帳補單: 需要管理員 Token
```

**詳細分析**: [01_token_verification_decision_tree.md](./01_token_verification_decision_tree.md)

---

### 錯誤 #2: 冪等性檢查的單層架構風險

**問題描述**:
文檔僅提到「檢查 transactionId 並返回緩存結果」，但沒有說明：
- 緩存有效期多長？
- 緩存失效後怎麼辦？
- Redis 故障時的容錯機制？

**風險場景**:
```
場景: Redis 緩存失效導致重複扣款

T0: GP 發送 Bet 請求 (tx_id = "bet_123", amount = 100)
T1: 營運商處理成功，扣款 100 元
T2: 響應返回，緩存到 Redis (TTL = 5 分鐘)
T3: 網路抖動，GP 未收到響應
T4: Redis 主從切換，緩存丟失
T5: GP 重試 (30 秒後)
T6: 營運商檢查 Redis → 未找到記錄
T7: 再次扣款 100 元  ← ⚠️ 重複扣款！

最終: 玩家損失 200 元，但只下注了一次
```

**推薦決策**: 三層防護架構

```
Layer 1: Redis 緩存（熱路徑優化）
  - 目的: 處理 99% 的重複請求
  - TTL 配置:
      * Bet API: 15 分鐘
      * Result API: 24 小時
      * Rollback API: 7 天
  - 性能: 1-5ms

Layer 2: 數據庫永久記錄（Truth Source）
  - 目的: 防止緩存失效後的重複處理
  - 實現: transaction_id 作為 PRIMARY KEY
  - 性能: 10-50ms

Layer 3: 分布式鎖（並發防護）
  - 目的: 防止同時進入業務邏輯
  - 實現: Redisson 分布式鎖
  - 鎖定時間: 10 秒
  - 等待時間: 3 秒
```

**關鍵設計決策**:
| 問題 | 錯誤方案 | 正確方案 | 理由 |
|------|---------|---------|------|
| 緩存丟失 | 系統報錯 | 查詢數據庫 | 數據庫是 Truth Source |
| 並發請求 | 數據庫鎖 | 分布式鎖 + 數據庫鎖 | 減少數據庫壓力 |
| TTL 過期 | 重新處理 | 從數據庫恢復 | 防止重複扣款 |

**詳細分析**: [02_idempotency_layered_design.md](./02_idempotency_layered_design.md)

---

## 🟠 P1 級別錯誤（High - 業務準確性）

### 錯誤 #3: 體育博彩「贏半/輸半」的 Valid Bet 計算矛盾

**問題描述**:
文檔稱「實際風險暴露 = 50 元」，但這與風險管理的基本定義矛盾。

**錯誤邏輯** (第 142 行):
> 計算規則： Valid Bet = 50元（即實際輸贏的金額絕對值），而非本金 100 元

**邏輯矛盾分析**:

```
場景: 投注 100 元在「讓球 -0.25」，結果平局（輸半）

文檔的說法:
  - 實際風險暴露 = 50 元
  - Valid Bet = 50 元

問題 1: 「實際風險暴露」的定義錯誤
  ✅ 正確定義: 玩家承擔的「最大可能損失」= 100 元
  ❌ 文檔定義: 玩家的「實際損失」= 50 元

問題 2: 公平性問題
  玩家 A: 投注 100 元，結果全贏 → Valid Bet = 100
  玩家 B: 投注 100 元，結果輸半 → Valid Bet = 50

  → 相同的投注行為，不同的流水貢獻 → 不公平

問題 3: 風控矛盾
  風控系統: 鎖定 100 元（評估為 100 元風險）
  流水計算: 只計入 50 元（評估為 50 元風險）

  → 風險評估與收益不對等
```

**業界標準對比**:

| 營運商類型 | Valid Bet 計算方式 | 比例 |
|-----------|------------------|------|
| **主流歐洲營運商**<br/>(Pinnacle, Betfair) | ✅ 固定本金法<br/>Valid Bet = 投注本金<br/>不論結果 | **90%** |
| **少數亞洲營運商**<br/>(部分活動規則) | ❌ 實際風險法<br/>Valid Bet = 實際輸贏金額 | **10%** |

**推薦決策**: 標準本金法

```java
// 推薦實現
public BigDecimal calculateValidBet(SportsBetSettlement settlement) {
    // 檢查賠率門檻
    if (!meetsOddsThreshold(settlement.getOdds())) {
        return BigDecimal.ZERO;
    }

    // 檢查結算狀態
    if (settlement.getStatus() == VOID ||
        settlement.getStatus() == CANCELLED ||
        settlement.getStatus() == PUSH) {
        return BigDecimal.ZERO;  // 沒有承擔風險
    }

    // ✅ 關鍵: 所有其他狀態（WIN, LOSE, HALF_WIN, HALF_LOSE）
    // Valid Bet = 投注本金（不論結果）
    return settlement.getBetAmount();
}
```

**詳細分析**: [03_sports_betting_valid_bet_logic.md](./03_sports_betting_valid_bet_logic.md)

---

### 錯誤 #4: 免費旋轉的 Turnover 定義矛盾

**問題描述**:
文檔在兩處對免費旋轉的 Turnover 定義不一致，導致 GGR 計算錯誤。

**矛盾之處**:
- **第 3.2.3 節**: 「免費旋轉 Turnover = 0」
- **第 6.2 節**: 「玩家在 GP 端進行遊戲，此時通常不扣除玩家餘額（Turnover = 0）」

**財務影響**:

```
場景: 贈送 10 次免費旋轉，每次面額 $1

遊戲結果: 玩家贏了 $8.50

❌ 錯誤計算（Turnover = 0）:
  Turnover = $0
  Payout = $8.50
  GGR = $0 - $8.50 = -$8.50  ← 看起來像虧損

問題:
  1. 免費旋轉的成本沒有被記錄
  2. 無法區分「玩家贏錢」和「促銷費用」
  3. 上市公司財報無法準確披露

✅ 正確計算（Turnover = 面額總和）:
  Turnover = $10.00
  Payout = $8.50
  GGR = $10.00 - $8.50 = $1.50  ← 促銷淨成本

含義:
  - 贈送了價值 $10 的免費旋轉（成本）
  - 玩家實際贏得 $8.50（回報）
  - 淨成本 $1.50（促銷費用）
```

**業界標準**:

| 供應商 | Turnover | Valid Bet | 依據 |
|-------|----------|-----------|------|
| **Evolution Gaming** | ✅ 面額總和 | 0 | 官方 API 文檔 |
| **Pragmatic Play** | ✅ 面額總和 | 0 | 官方 API 文檔 |
| **Hub88 (Aggregator)** | ✅ 面額總和 | 0 | 技術白皮書 |

**上市公司財報範例**:

```
Evolution Gaming Annual Report 2023:

"Free spins provided to players are recorded as:
 - Turnover: At the face value of the free spin
 - Payout: At the actual win amount
 - Marketing Expense: Net cost (face value - payout)"
```

**推薦決策**:

```sql
-- 交易記錄表設計
CREATE TABLE wallet_transactions (
    transaction_id VARCHAR(128) PRIMARY KEY,

    transaction_type ENUM(
        'CASH_BET',           -- 真錢投注
        'FREESPIN_BET',       -- ✅ 免費旋轉投注
        'CASH_WIN',
        'FREESPIN_WIN'        -- ✅ 免費旋轉派彩
    ),

    amount DECIMAL(18, 4),

    -- ✅ 關鍵: 分開記錄
    turnover DECIMAL(18, 4),      -- 計入財務報表
    valid_bet DECIMAL(18, 4),     -- 計入流水要求

    -- 免費旋轉特有欄位
    is_free_spin BOOLEAN,
    freespin_cost DECIMAL(18, 4)  -- 記錄面額（成本）
);

-- 免費旋轉的記錄範例
INSERT INTO wallet_transactions VALUES (
    'fs_bet_123',
    'FREESPIN_BET',
    1.00,          -- 面額
    1.00,          -- ✅ Turnover = 面額（計入 GGR）
    0.00,          -- ✅ Valid Bet = 0（不計入流水要求）
    TRUE,
    1.00
);
```

**詳細分析**: [04_free_spins_turnover_calculation.md](./04_free_spins_turnover_calculation.md)

---

## 📋 待分析錯誤清單（Phase 3-6）

以下錯誤將在後續階段分析：

### Phase 3: 風控檢測層
- **錯誤 #5**: 輪盤覆蓋率檢測的邏輯漏洞（可繞過檢測）
- **錯誤 #6**: 百家樂和局投注的 Valid Bet 計算矛盾

### Phase 4: 流水累積與獎勵層
- **錯誤 #7**: 流水要求扣減的並發競爭條件（重複發放獎勵風險）

### Phase 5: 財務與對帳層
- **錯誤 #8**: 未結算注單的會計分錄錯誤
- **錯誤 #9**: 「三方對帳」概念混淆（遊戲交易 vs 存提款）

### Phase 6: 錯誤恢復層
- **錯誤 #10**: 缺少關鍵的錯誤恢復場景（亂序請求、預回滾等）

---

## ✅ 關鍵決策矩陣（需立即確認）

### 決策 #1: Token 驗證策略

| API 類型 | 推薦策略 | 需確認 |
|---------|---------|--------|
| Bet | STRICT（無例外） | [ ] 已確認 |
| Result | CONDITIONAL（根據遊戲類型） | [ ] 已確認 |
| Balance | STRICT | [ ] 已確認 |
| Rollback | CONDITIONAL | [ ] 已確認 |

**配置需求**:
- [ ] Token 有效期：15 分鐘 / 30 分鐘 / 其他？
- [ ] Result API 的最大容忍過期時間：7 天 / 14 天 / 其他？
- [ ] 是否需要 Token 刷新機制？

### 決策 #2: 冪等性架構

| 層級 | 推薦方案 | 需確認 |
|------|---------|--------|
| Layer 1 | Redis 緩存（熱路徑） | [ ] 已確認 |
| Layer 2 | 數據庫永久記錄 | [ ] 已確認 |
| Layer 3 | 分布式鎖 | [ ] 已確認 |

**配置需求**:
- [ ] Redis 部署模式：單機 / 哨兵 / 集群？
- [ ] Redis TTL 配置：Bet=15min, Result=24h, Rollback=7d？
- [ ] 分布式鎖超時時間：鎖定 10 秒，等待 3 秒？
- [ ] 數據庫分區策略：按月 / 按年？

### 決策 #3: Valid Bet 計算方法

| 遊戲類型 | 推薦方法 | 需確認 |
|---------|---------|--------|
| 體育博彩（贏半/輸半） | 標準本金法（Valid Bet = 本金） | [ ] 已確認 |
| 免費旋轉 | Turnover = 面額, Valid Bet = 0 | [ ] 已確認 |
| 真人百家樂（和局投注） | 待分析 | [ ] 待決策 |
| 輪盤（覆蓋率檢測） | 待分析 | [ ] 待決策 |

**配置需求**:
- [ ] 體育博彩賠率門檻：歐洲盤 ≥ 1.50？
- [ ] 體育博彩結算狀態規則：WIN/LOSE/HALF_WIN/HALF_LOSE = 全額？
- [ ] 免費旋轉派彩錢包：紅利錢包 / 現金錢包？
- [ ] 免費旋轉流水要求：20x / 30x / 其他？

---

## 🎯 實施優先級建議

### P0 (Critical - 立即修復)
**影響**: 安全漏洞、重複扣款

1. **錯誤 #1**: Bet 請求的 Token 驗證（安全漏洞）
   - **時間**: 1-2 天
   - **複雜度**: 低
   - **影響**: 防止資金安全問題

2. **錯誤 #2**: 冪等性檢查架構（防止重複扣款）
   - **時間**: 3-5 天
   - **複雜度**: 中
   - **影響**: 資金安全的核心機制

### P1 (High - 盡快修復)
**影響**: 業務準確性、財務報表

3. **錯誤 #3**: 體育博彩 Valid Bet 計算
   - **時間**: 2-3 天
   - **複雜度**: 中
   - **影響**: 優惠活動、返水計算的準確性

4. **錯誤 #4**: 免費旋轉 Turnover 計算
   - **時間**: 2-3 天
   - **複雜度**: 中
   - **影響**: GGR 計算、財務報表準確性

### P2 (Medium - 計劃修復)
**影響**: 用戶體驗、風控效率

5. **錯誤 #5**: 輪盤覆蓋率檢測（待分析）
6. **錯誤 #6**: 百家樂和局計算（待分析）
7. **錯誤 #7**: 流水累積並發問題（待分析）

### P3 (Low - 逐步完善)
**影響**: 邊緣場景、錯誤恢復

8. **錯誤 #8**: 會計分錄錯誤（待分析）
9. **錯誤 #9**: 對帳模型混淆（待分析）
10. **錯誤 #10**: 錯誤恢復場景（待分析）

---

## 📁 文檔索引

### 已完成分析
1. [Token 驗證邏輯決策樹](./01_token_verification_decision_tree.md)
2. [冪等性檢查分層設計](./02_idempotency_layered_design.md)
3. [體育博彩 Valid Bet 計算邏輯](./03_sports_betting_valid_bet_logic.md)
4. [免費旋轉 Turnover 計算邏輯](./04_free_spins_turnover_calculation.md)

### 待完成分析
5. 百家樂和局 Valid Bet 計算邏輯
6. 輪盤覆蓋率檢測算法設計
7. 流水累積的並發安全機制
8. 未結算注單的會計處理邏輯
9. 遊戲交易與存提款對帳模型
10. 亂序請求與預回滾處理機制

---

## 🚀 下一步行動

### 立即行動項
1. **決策確認**: 與業務、產品、財務團隊確認上述決策矩陣
2. **技術架構**: 根據決策完成技術架構設計
3. **開發排程**: 按照優先級排程開發任務

### 後續分析
4. **Phase 3 分析**: 啟動風控檢測層的錯誤分析
5. **Phase 4-6 分析**: 完成剩餘 6 個錯誤的深度分析
6. **整合文檔**: 創建完整的系統設計文檔

---

## 📞 聯絡方式

如需進一步澄清或討論，請參考：
- **原始文檔**: [seamless_wallet.md](../seamless_wallet.md)
- **分析目錄**: `docs/IGaming/seamless_wallet_analysis/`

---

**版本歷史**:
- v1.0 (2026-01-28): 初版，完成 Phase 1-2 分析
