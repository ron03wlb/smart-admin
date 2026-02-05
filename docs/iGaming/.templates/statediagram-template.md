# stateDiagram-v2 模板 (SmartAdmin iGaming Documentation)

> **用途**: 為 SmartAdmin iGaming 文檔提供標準的 stateDiagram-v2 範本
> **版本**: 1.0.0
> **最後更新**: 2026-02-04
> **維護者**: iGaming Documentation Team

---

## 📋 模板類型

### 1. 基礎狀態機（Simple State Machine）

**適用場景**: 簡單的狀態轉換流程（< 5 個狀態）

```mermaid
stateDiagram-v2
    [*] --> State1: Initial Event
    State1 --> State2: Transition Event
    State2 --> [*]: Final Event

    note right of State1
        狀態描述
        觸發條件
        執行動作
    end note
```

---

### 2. 複雜業務流程（Complex Business Workflow）

**適用場景**: 包含多個子狀態和條件分支的複雜流程

```mermaid
stateDiagram-v2
    [*] --> IDLE: 系統初始化

    IDLE --> PROCESSING: 接收請求
    PROCESSING --> COMPLETED: 處理成功
    PROCESSING --> FAILED: 處理失敗
    PROCESSING --> TIMEOUT: 超時

    state PROCESSING {
        [*] --> Validating: 開始處理
        Validating --> Executing: 驗證通過
        Validating --> ValidationFailed: 驗證失敗
        Executing --> [*]: 執行完成
    }

    FAILED --> RETRY: 可重試
    RETRY --> PROCESSING: 重新處理
    RETRY --> ABANDONED: 超過重試次數

    COMPLETED --> [*]
    ABANDONED --> [*]
    TIMEOUT --> [*]

    note right of PROCESSING
        處理階段
        ━━━━━━━━━━━━━━
        1. 驗證請求參數
        2. 執行業務邏輯
        3. 記錄處理結果
    end note

    note right of FAILED
        失敗處理
        ━━━━━━━━━━━━━━
        原因分類:
        - 業務異常
        - 系統錯誤
        - 超時異常

        處理策略:
        - 可重試錯誤 → RETRY
        - 不可重試錯誤 → ABANDONED
    end note
```

---

### 3. Round-Based 遊戲狀態機（Game Round State Machine）

**適用場景**: 遊戲商對接中的 Round-Based 交易模型

```mermaid
stateDiagram-v2
    [*] --> IDLE: 無活躍 Round

    IDLE --> OPEN: 下注請求
    OPEN --> CLOSED: 結算請求
    OPEN --> CANCELLED: 取消請求
    OPEN --> TIMEOUT: 超時未結算

    state OPEN {
        [*] --> BetPlaced: 扣款成功
        BetPlaced --> BetUpdated: 追加投注
    }

    TIMEOUT --> PENDING_REVIEW: GP 狀態未知
    PENDING_REVIEW --> CLOSED: 人工審核通過
    PENDING_REVIEW --> CANCELLED: 人工審核取消

    CLOSED --> ADJUSTED: 重新結算
    ADJUSTED --> [*]
    CLOSED --> [*]
    CANCELLED --> [*]

    note right of OPEN
        Round 開啟狀態
        ━━━━━━━━━━━━━━
        動作: 扣款
        記錄: 創建 Round
        狀態: OPEN

        追加投注:
        - 累加投注額
        - 更新 Round 記錄
    end note

    note right of TIMEOUT
        孤兒 Round 處理
        ━━━━━━━━━━━━━━
        檢測條件:
        - Round 狀態 = OPEN
        - 創建時間 > 2 小時

        處理流程:
        1. 查詢 GP API 狀態
        2. GP 已關閉 → CLOSED
        3. GP 狀態未知 → PENDING_REVIEW
    end note

    note right of PENDING_REVIEW
        人工審核
        ━━━━━━━━━━━━━━
        觸發條件:
        - GP API 查詢失敗
        - Round 金額 > $1000 (高優先級)

        SLA:
        - 高優先級: 4 小時
        - 中優先級: 24 小時
    end note
```

---

### 4. 玩家生命週期（Player Lifecycle）

**適用場景**: 玩家帳戶狀態管理

```mermaid
stateDiagram-v2
    [*] --> ACTIVE: 新玩家註冊

    state "ACTIVE\n活躍" as ACTIVE
    state "LOCKED\n鎖定" as LOCKED
    state "SUSPENDED\n暫停" as SUSPENDED
    state "PENDING_VERIFICATION\n待驗證" as PENDING_VERIFICATION
    state "CLOSED\n關閉 (永久)" as CLOSED

    ACTIVE --> ACTIVE: 正常活動
    ACTIVE --> LOCKED: 登入失敗 5 次
    ACTIVE --> SUSPENDED: 風險評分 >= 70
    ACTIVE --> PENDING_VERIFICATION: 提款觸發 KYC
    ACTIVE --> CLOSED: 自我排除 / AML 違規

    LOCKED --> ACTIVE: 30 分鐘後自動解鎖
    LOCKED --> SUSPENDED: 密碼重置失敗 3 次

    SUSPENDED --> ACTIVE: 人工審核通過
    SUSPENDED --> CLOSED: 確認欺詐

    PENDING_VERIFICATION --> ACTIVE: KYC 通過
    PENDING_VERIFICATION --> SUSPENDED: KYC 文件偽造
    PENDING_VERIFICATION --> CLOSED: 身份驗證失敗 3 次

    CLOSED --> [*]

    note right of ACTIVE
        預設狀態
        無任何限制
        可進行所有操作
    end note

    note right of LOCKED
        安全保護機制
        - 防暴力破解
        - 自動解鎖
        - 可重置密碼
    end note

    note right of SUSPENDED
        風控凍結狀態
        - 禁止資金操作
        - 需人工審核
        - 可提交申訴
    end note

    note right of CLOSED
        不可逆終止狀態
        - 自我排除 (賭博成癮)
        - AML 違規
        - 確認欺詐
        - 退還未使用餘額
    end note
```

---

### 5. VIP 等級狀態機（VIP Tier State Machine）

**適用場景**: VIP 等級升降級管理

```mermaid
stateDiagram-v2
    [*] --> Bronze: 新玩家註冊

    state "Bronze (基礎)" as Bronze
    state "Silver (Dep: $5K+)" as Silver
    state "Gold (Dep: $20K+)" as Gold
    state "Platinum (Dep: $100K+)" as Platinum
    state "Diamond (Dep: $500K+)" as Diamond

    Bronze --> Silver: 升級
    Silver --> Gold: 升級
    Gold --> Platinum: 升級
    Platinum --> Diamond: 升級

    Bronze --> Bronze: 永久保持
    Silver --> Silver: 保級達標
    Gold --> Gold: 保級達標
    Platinum --> Platinum: 保級達標
    Diamond --> Diamond: 保級達標

    state "Silver Warning" as S_Warn
    state "Gold Warning" as G_Warn
    state "Platinum Warning" as P_Warn
    state "Diamond Warning" as D_Warn

    Silver --> S_Warn: 未達保級條件
    S_Warn --> Silver: 達標
    S_Warn --> Bronze: 連續未達標 x2

    Gold --> G_Warn: 未達保級條件
    G_Warn --> Gold: 達標
    G_Warn --> Silver: 連續未達標 x2

    Platinum --> P_Warn: 未達保級條件
    P_Warn --> Platinum: 達標
    P_Warn --> Gold: 連續未達標 x2

    Diamond --> D_Warn: 未達保級條件
    D_Warn --> Diamond: 達標
    D_Warn --> Platinum: 連續未達標 x2

    note right of Diamond
        鑽石等級
        ━━━━━━━━━━━━━━
        保級條件最嚴格
        享有最高權益
        降級保留 50% 特權
    end note

    note right of D_Warn
        降級保護機制
        ━━━━━━━━━━━━━━
        1. 第一次: 發送警告
        2. 第二次: 最後通知
        3. 第三次: 正式降級 (附補償)
    end note
```

---

## ✅ 最佳實踐

### 1. Transition Labels（狀態轉換標籤）

**✅ 正確做法**:
- 簡潔明確的單行描述
- 避免使用 `<br/>` 標籤（stateDiagram-v2 不支持）
- 複雜信息移至 note 區塊

```mermaid
# ✅ 正確
stateDiagram-v2
    A --> B: Event Triggered
```

**❌ 錯誤做法**:
```mermaid
# ❌ 錯誤 - 使用 <br/> 標籤
stateDiagram-v2
    A --> B: Event<br/>Action<br/>Result
```

### 2. Note Blocks（註釋區塊）

**✅ 正確做法**:
- 使用多行 note 格式（`note ... end note`）
- 每行一個信息點
- 使用分隔線 `━━━━━` 進行視覺分組

```mermaid
# ✅ 正確
stateDiagram-v2
    note right of STATE
        狀態描述
        ━━━━━━━━━━━━━━
        觸發條件: xxx
        執行動作: yyy
        後續狀態: zzz
    end note
```

**❌ 錯誤做法**:
```mermaid
# ❌ 錯誤 - 單行 note + <br/> 標籤
stateDiagram-v2
    note right of STATE : Text<br/>More<br/>Info
```

### 3. State Naming（狀態命名）

**✅ 推薦命名規則**:
- 使用全大寫或 PascalCase: `ACTIVE`, `ProcessingPayment`
- 狀態名稱簡潔明確: `OPEN`, `CLOSED`, `TIMEOUT`
- 使用狀態別名顯示中文: `state "活躍狀態" as ACTIVE`

### 4. 複雜狀態（Nested States）

**✅ 適度使用**:
- 僅在必要時使用嵌套狀態（`state { }`）
- 嵌套深度不超過 2 層
- 超過 3 個子狀態考慮拆分為獨立圖表

---

## 🚫 常見錯誤

### 錯誤 1: 使用 HTML 標籤

**❌ 錯誤**:
```mermaid
stateDiagram-v2
    A --> B: Event<br/>Action
```

**✅ 修復**:
```mermaid
stateDiagram-v2
    A --> B: Event

    note right of B
        Event Triggered
        Action Executed
    end note
```

### 錯誤 2: 過度複雜的 Transition Labels

**❌ 錯誤**:
```
A --> B: Request Received | Validate Parameters | Check Balance | Debit Wallet | Update Status | Send Notification
```

**✅ 修復**:
```mermaid
stateDiagram-v2
    A --> B: Process Request

    note right of B
        處理流程
        ━━━━━━━━━━━━━━
        1. Validate Parameters
        2. Check Balance
        3. Debit Wallet
        4. Update Status
        5. Send Notification
    end note
```

---

## 🔧 驗證工具

**檢測錯誤**:
```bash
./scripts/detect-statediagram-br.sh docs/iGaming/
```

**自動修復**:
```bash
./scripts/batch-fix-statediagram-br.sh --verify
```

**驗證語法**:
```bash
./scripts/validate-mermaid.sh docs/iGaming/
```

**線上驗證**:
- [Mermaid Live Editor](https://mermaid.live/)

---

## 📚 參考資源

- **SmartAdmin Mermaid 最佳實踐**: [.claude/skills/extended/domain/igaming-multi-tenant-wallet-pm/knowledge/mermaid-best-practices.md](.claude/skills/extended/domain/igaming-multi-tenant-wallet-pm/knowledge/mermaid-best-practices.md)
- **Mermaid 官方文檔**: [State Diagram](https://mermaid.js.org/syntax/stateDiagram.html)
- **CLAUDE.md Mermaid Standards**: [CLAUDE.md#mermaid-diagram-standards](../../../CLAUDE.md#mermaid-diagram-standards)

---

**版本**: 1.0.0
**最後更新**: 2026-02-04
**維護者**: iGaming Documentation Team
**狀態**: Production Ready
