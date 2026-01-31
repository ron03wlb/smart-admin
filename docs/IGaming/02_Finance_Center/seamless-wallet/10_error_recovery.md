# 錯誤恢復場景設計

## 問題來源
文檔第 2.3 節提到超時處理，但缺少具體的複雜場景處理邏輯。

## 缺失的場景

### 場景 1: 亂序請求（Out-of-Order Requests）

**問題描述**:
```yaml
正常順序:
1. Bet Request (tx_id = "bet_123")
2. Bet Response
3. Result Request (tx_id = "result_456", refer_id = "bet_123")
4. Result Response

異常順序（網路延遲導致）:
1. Result Request 先到達（referencing bet_123）
2. Bet Request 後到達

問題: 營運商如何處理？
❌ 直接拒絕 → 玩家贏錢丟失
✅ 暫存 Result，等待 Bet
```text


### 場景 2: 預回滾（Pre-Rollback）

**問題描述**:
```yaml
異常時序:
1. GP 發送 Bet Request
2. GP 超時（認為失敗）
3. GP 立即發送 Rollback Request
4. Rollback 先到達營運商
5. Bet Request 延遲 5 秒後才到達

問題: 如何處理這個 Rollback？
```text


### 場景 3: 部分失敗恢復（Two-Phase Commit）

**問題描述**:
```yaml
場景: Bet 扣款成功，但在記錄注單時數據庫崩潰

T1: Bet Request 到達
T2: 扣款成功（玩家餘額 -100）
T3: 開始插入 bet_details 表
T4: 數據庫崩潰（主從切換）
T5: API 超時，未返回響應
T6: GP 重試（相同 transaction_id）

問題:
- wallet_transactions 表有記錄（扣款成功）
- bet_details 表沒有記錄（注單丟失）
- 返回「已處理」→ GP 認為成功
- 但實際上數據不完整
```text


## 監控與告警

```yaml
metrics:
  # 亂序請求率
  - out_of_order_request_rate
    target: < 1%
    alert: > 5%

  # 預回滾發生率
  - pre_rollback_rate
    target: < 0.1%
    alert: > 1%

  # 部分失敗率
  - partial_failure_rate
    target: 0
    alert: > 0
```

## 決策總結

✅ **推薦方案**:
- 亂序請求: 暫存機制（TTL = 60 秒）
- 預回滾: 標記機制（TTL = 5 分鐘）
- 部分失敗: 兩階段提交（數據庫事務）

❌ **錯誤方案**:
- 直接拒絕亂序請求 → 玩家贏錢丟失
- 忽略預回滾 → 可能重複扣款
- 不使用事務 → 數據不一致

---

## 📚 相關文檔

### 上層導航
- [Seamless Wallet 索引](./00_INDEX.md) - 專題導航（P0/P1 分類）

### 架構文檔
- [02-06 統一錢包模型](../02-06_Unified_Wallet_Model.md) - 錢包整體架構
- [03-03 無縫錢包分析](../../03_Game_Center/03-03_Seamless_Wallet_Analysis.md) - GP API 規範
