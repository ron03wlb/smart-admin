# Token 驗證邏輯決策樹

## 問題來源
文檔在第 2.2.2 節「Bet 請求處理邏輯」中混淆了不同 API 的 Token 驗證策略。

## 業務場景分析

### 場景 1: 短生命週期遊戲（老虎機、輪盤）
```
遊戲特徵:
- 回合時間: 數秒到數分鐘
- Token 有效期: 通常 15-30 分鐘
- 風險: Token 在遊戲過程中過期的機率極低

結論: 所有 API 都應該驗證 Token
```

### 場景 2: 長生命週期遊戲（體育賽事、撲克錦標賽）
```
遊戲特徵:
- 回合時間: 數小時到數天
- Token 有效期: 15-30 分鐘
- 風險: Token 必然會在遊戲過程中過期

結論: Result API 需要特殊處理
```

## 決策樹 (v2.0.0 增強版 - 完整錯誤處理)

```mermaid
graph TD
    A[API Request 到達] --> B{API 類型?}

    B -->|Balance| C[必須驗證 Token]
    B -->|Bet| D[必須驗證 Token]
    B -->|Result| E{遊戲類型?}
    B -->|Rollback| F{原因?}

    %% Balance API 驗證流程
    C --> C1{Token 解析}
    C1 -->|成功| C2{簽名驗證}
    C1 -->|失敗| ERR1[Error: INVALID_TOKEN_FORMAT]
    C2 -->|成功| C3{過期檢查}
    C2 -->|失敗| ERR2[Error: INVALID_SIGNATURE]
    C3 -->|有效| C4{玩家存在?}
    C3 -->|過期| ERR3[Error: TOKEN_EXPIRED]
    C4 -->|存在| O[返回餘額]
    C4 -->|不存在| ERR4[Error: PLAYER_NOT_FOUND]

    %% Bet API 驗證流程
    D --> D1{Token 解析}
    D1 -->|成功| D2{簽名驗證}
    D1 -->|失敗| ERR1
    D2 -->|成功| D3{過期檢查}
    D2 -->|失敗| ERR2
    D3 -->|有效| D4{玩家存在?}
    D3 -->|過期| ERR3
    D4 -->|存在| D5{GP 授權?}
    D4 -->|不存在| ERR4
    D5 -->|授權| D6{用戶匹配?}
    D5 -->|未授權| ERR5[Error: UNAUTHORIZED_GAME_PROVIDER]
    D6 -->|匹配| P[執行扣款]
    D6 -->|不匹配| ERR6[Error: USER_MISMATCH]

    %% Result API 驗證流程
    E -->|短週期<br/>老虎機/輪盤| G[必須驗證 Token]
    E -->|長週期<br/>體育/撲克| H{Token 狀態?}

    G --> G1{Token 解析}
    G1 -->|成功| G2{簽名驗證}
    G1 -->|失敗| ERR1
    G2 -->|成功| I[正常處理]
    G2 -->|失敗| ERR2

    H -->|有效| I
    H -->|過期| J{檢查 Bet 記錄}
    H -->|解析失敗| ERR1

    J -->|Bet 存在<br/>且未結算| K[放寬驗證<br/>使用 round_id 驗證]
    J -->|Bet 不存在| L[拒絕請求]
    J -->|Bet 已結算| ERR7[Error: BET_ALREADY_SETTLED]

    K --> Q[執行派彩]
    L --> R[Error: Invalid Session]

    %% Rollback API 驗證流程
    F -->|超時重試| M[放寬驗證<br/>使用 transaction_id 驗證]
    F -->|對帳補單| N[放寬驗證<br/>需要管理員權限]

    M --> S[執行回滾]
    N --> N1{管理員 Token?}
    N1 -->|有效| S
    N1 -->|無效| ERR8[Error: INSUFFICIENT_PRIVILEGES]

    %% 樣式定義
    classDef errorStyle fill:#ff6b6b,stroke:#c92a2a,color:#fff
    class ERR1,ERR2,ERR3,ERR4,ERR5,ERR6,ERR7,ERR8,R,L errorStyle
```

## 推薦方案

### API Token 驗證矩陣

| API 類型 | 預設策略 | 特殊情況處理 | 驗證欄位 |
|---------|---------|------------|---------|
| **Balance** | ✅ 嚴格驗證 | 無例外 | `token` |
| **Bet** | ✅ 嚴格驗證 | 無例外 | `token` + `user_id` |
| **Result** | ⚠️ 條件驗證 | Token 過期時檢查 `round_id` | `token` OR `round_id` + `bet_tx_id` |
| **Rollback** | ⚠️ 條件驗證 | 對帳場景允許管理員 Token | `token` OR `admin_token` |

### 實現邏輯


## 錯誤代碼完整列表 (v2.0.0 新增)

### 錯誤代碼分類與處理策略

| 錯誤代碼 | 嚴重性 | 觸發場景 | HTTP 狀態 | 建議處理 | 是否可重試 |
|---------|--------|---------|----------|---------|----------|
| **INVALID_TOKEN_FORMAT** | HIGH | JWT 格式錯誤、解析失敗 | 400 | 重新登入獲取新 Token | ❌ |
| **INVALID_SIGNATURE** | CRITICAL | JWT 簽名驗證失敗 | 401 | 重新登入 + 安全審計 | ❌ |
| **TOKEN_EXPIRED** | MEDIUM | Token 過期時間已過 | 401 | 刷新 Token 或重新登入 | ✅ (刷新後) |
| **PLAYER_NOT_FOUND** | HIGH | 玩家賬戶不存在/已刪除 | 404 | 通知用戶聯繫客服 | ❌ |
| **UNAUTHORIZED_GAME_PROVIDER** | HIGH | 玩家未授權該遊戲供應商 | 403 | 檢查玩家權限配置 | ❌ |
| **USER_MISMATCH** | CRITICAL | Token user_id 與請求不符 | 403 | 安全審計 + 凍結賬戶 | ❌ |
| **TOKEN_REVOKED** | MEDIUM | Token 已被加入黑名單 | 401 | 重新登入獲取新 Token | ❌ |
| **PLAYER_BLOCKED** | HIGH | 玩家賬戶被凍結/封禁 | 403 | 顯示封禁原因 + 客服聯繫方式 | ❌ |
| **BET_NOT_FOUND** | MEDIUM | Bet 記錄不存在 | 404 | 檢查交易 ID 是否正確 | ✅ (延遲重試) |
| **ROUND_MISMATCH** | HIGH | Round ID 不匹配 | 400 | 檢查遊戲供應商數據 | ❌ |
| **BET_ALREADY_SETTLED** | MEDIUM | Bet 已經結算過 | 409 | 檢查是否重複結算 | ❌ |
| **BET_CANCELLED** | LOW | Bet 已被取消 | 400 | 通知 GP 該 Bet 已取消 | ❌ |
| **BET_EXPIRED** | MEDIUM | Bet 超過最大時效性 | 410 | 人工介入處理 | ❌ |

### 錯誤響應格式標準

```json
{
  "success": false,
  "error_code": "INVALID_TOKEN_FORMAT",
  "error_message": "Token format is malformed or corrupted",
  "details": {
    "timestamp": "2026-01-28T10:30:00Z",
    "request_id": "req_abc123",
    "suggestions": [
      "Please re-authenticate to obtain a new token"
    ]
  },
  "metadata": {
    "retryable": false,
    "contact_support": false
  }
}
```

### 客戶端錯誤處理流程圖

```mermaid
graph TD
    A[收到錯誤響應] --> B{檢查 error_code}

    B -->|TOKEN_EXPIRED| C[嘗試刷新 Token]
    B -->|INVALID_SIGNATURE| D[清除本地 Token<br/>強制重新登入]
    B -->|PLAYER_BLOCKED| E[顯示封禁提示<br/>提供客服聯繫方式]
    B -->|BET_NOT_FOUND| F{retryable?}
    B -->|其他錯誤| G[顯示錯誤信息]

    C --> H{刷新成功?}
    H -->|是| I[重試原始請求]
    H -->|否| D

    F -->|true| J[延遲 3 秒後重試]
    F -->|false| G

    J --> K{重試次數?}
    K -->|< 3 次| I
    K -->|≥ 3 次| L[顯示錯誤<br/>建議聯繫客服]

    G --> M[記錄錯誤日誌]
    L --> M
    D --> M
    E --> M
```

### 監控告警規則

**Critical 級別告警** (立即通知)：
- `INVALID_SIGNATURE` 頻率 > 10/分鐘 → 可能遭受攻擊
- `USER_MISMATCH` 單個 IP > 5 次/小時 → Session fixation 攻擊
- `UNAUTHORIZED_GAME_PROVIDER` 特定 GP > 50 次/小時 → 配置錯誤或攻擊

**High 級別告警** (5 分鐘內通知)：
- `PLAYER_NOT_FOUND` 頻率 > 100/分鐘 → 數據同步問題
- `BET_EXPIRED` 頻率 > 20/分鐘 → GP 延遲結算問題

**Medium 級別告警** (15 分鐘內通知)：
- `TOKEN_EXPIRED` 刷新失敗率 > 10% → Token 服務異常
- `BET_ALREADY_SETTLED` 頻率 > 50/分鐘 → GP 重複發送 Result

## 安全性分析

### 嚴格驗證的必要性（Bet API）

**攻擊場景 1: Token 重放攻擊**
```
攻擊者獲取合法用戶的 Token（例如通過中間人攻擊）

T0: 用戶正常登入，獲得 Token (token_abc123)
T1: 攻擊者攔截 Token
T2: 攻擊者使用 Token 發起 Bet 請求
    - 如果不驗證 Token 過期 → 攻擊成功
    - 如果驗證 Token 過期 → 攻擊被阻止

結論: Bet API 必須嚴格驗證 Token 有效期
```

**攻擊場景 2: 會話固定攻擊**
```
攻擊者誘導用戶使用攻擊者控制的 Token

T0: 攻擊者生成 Token (token_evil)
T1: 誘導用戶使用此 Token 登入
T2: 攻擊者使用相同 Token 進行操作
    - 如果不驗證 Token 所有者 → 攻擊成功
    - 如果驗證 user_id 匹配 → 攻擊被阻止

結論: 必須驗證 Token 中的 user_id 與請求中的 user_id 一致
```

### 寬鬆驗證的合理性（Result API）

**合理場景: 體育賽事延遲結算**
```
正常流程:
T0 (週一 10:00): 玩家投注足球賽事，Token 有效期 30 分鐘
T1 (週一 10:01): Bet 成功，創建注單
T2 (週一 10:30): Token 過期
T3 (週三 22:00): 比賽結束，GP 發送 Result 請求

問題: Token 已經過期 60 小時

選項 A（嚴格驗證）:
→ 拒絕 Result 請求
→ 玩家贏錢無法入帳
→ 需要人工介入
→ 用戶體驗極差

選項 B（寬鬆驗證）:
→ 使用 round_id + bet_tx_id 驗證
→ 確認 Bet 存在且未結算
→ 自動完成派彩
→ ✅ 推薦方案
```

## 實現建議

### 配置管理

```yaml
# application.yml
token:
  verification:
    # 預設策略
    default_mode: STRICT

    # 各 API 的策略
    api_modes:
      balance: STRICT
      bet: STRICT
      result: CONDITIONAL
      rollback: CONDITIONAL

    # 長週期遊戲配置
    long_lived_games:
      - SPORTS_BETTING
      - POKER_TOURNAMENT
      - LIVE_DEALER_MULTI_TABLE

    # Result API 的 Token 過期容忍度
    result:
      allow_expired_token: true
      fallback_verification:
        - round_id
        - bet_transaction_id
      max_age_hours: 168  # 7 天（體育賽事最長結算時間）
```

### 監控指標


## 決策總結

✅ **推薦決策**:

1. **Bet API**: 必須嚴格驗證 Token（無例外）
   - 原因: 涉及資金扣款，安全優先級最高

2. **Result API**: 條件驗證（根據遊戲類型）
   - 短週期遊戲: 嚴格驗證
   - 長週期遊戲: Token 過期時使用 round_id 備用驗證

3. **Balance API**: 嚴格驗證 Token
   - 原因: 防止未授權的餘額查詢

4. **Rollback API**: 條件驗證（根據原因）
   - 超時重試: 使用 transaction_id 驗證
   - 對帳補單: 需要管理員 Token

## 需要確認的需求

- [ ] Token 的有效期應該設置為多長？（建議: 15-30 分鐘）
- [ ] 是否需要支持 Token 刷新機制？
- [ ] Result API 的最大容忍過期時間？（建議: 7 天）
- [ ] 是否需要在 Token 中包含遊戲類型信息？
- [ ] Rollback API 是否需要支持管理員手動補單？

---

## 📚 相關文檔

### 上層導航
- [Seamless Wallet 索引](./00_INDEX.md) - 專題導航（P0/P1 分類）

### 相關專題
- [02 冪等性設計](./02_idempotency_design.md) - 防重複扣款
- [10 錯誤恢復](./10_error_recovery.md) - 異常處理

### 架構文檔
- [02-06 統一錢包模型](../02-06_Unified_Wallet_Model.md) - 錢包整體架構
- [03-03 無縫錢包分析](../../03_Game_Center/03-03_Seamless_Wallet_Analysis.md) - GP API 規範
