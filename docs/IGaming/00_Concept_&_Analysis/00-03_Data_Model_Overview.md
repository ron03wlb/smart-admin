# 數據模型總覽 (Data Model Overview)

> **版本**: 1.0.0
> **最後更新**: 2026-01-27
> **目的**: 提供 IGaming 平台所有核心數據實體的統一視圖

---

## 📋 目錄

- [核心實體關係圖](#核心實體關係圖)
- [實體分層架構](#實體分層架構)
- [跨模組外鍵關係](#跨模組外鍵關係)
- [核心數據表設計](#核心數據表設計)
- [數據一致性約束](#數據一致性約束)
- [索引策略](#索引策略)
- [數據安全與加密](#數據安全與加密)

---

## 🔗 核心實體關係圖

### 整體 ER 圖

```mermaid
erDiagram
    %% 多租戶層級
    BRAND ||--o{ TENANT : manages
    TENANT ||--o{ AGENT : belongs_to
    AGENT ||--o{ PLAYER : refers
    AGENT ||--o{ AGENT : has_sub_agents

    %% 玩家核心
    PLAYER ||--|| WALLET : has
    PLAYER ||--o{ TRANSACTION : makes
    PLAYER ||--o{ GAME_SESSION : plays
    PLAYER ||--o{ BONUS : receives
    PLAYER ||--o{ VIP_LEVEL : belongs_to
    PLAYER }o--|| AGENT : referred_by

    %% 錢包與交易
    WALLET ||--o{ TRANSACTION : records
    WALLET ||--o{ BONUS_WALLET : has_bonus
    TRANSACTION }o--|| GAME : relates_to
    TRANSACTION }o--|| PSP : processed_by

    %% 遊戲相關
    GAME_PROVIDER ||--o{ GAME : provides
    GAME ||--o{ GAME_SESSION : hosts
    GAME_SESSION ||--o{ BET_RECORD : contains
    BET_RECORD }o--|| PLAYER : placed_by

    %% 活動與紅利
    ACTIVITY ||--o{ BONUS : generates
    BONUS ||--o{ WAGERING_PROGRESS : tracks
    WAGERING_PROGRESS }o--|| BET_RECORD : calculated_from

    %% 支付系統
    PSP ||--o{ PAYMENT_CHANNEL : provides
    PAYMENT_CHANNEL ||--o{ TRANSACTION : processes
    TRANSACTION ||--o{ RECONCILIATION_RECORD : reconciles

    %% 風控系統
    PLAYER ||--o{ RISK_SCORE : assessed_by
    TRANSACTION ||--o{ RISK_EVENT : triggers
    WITHDRAWAL_REQUEST ||--o{ APPROVAL_RECORD : requires

    %% 代理系統
    AGENT ||--o{ COMMISSION_RECORD : earns
    COMMISSION_RECORD }o--|| PLAYER : from_player
```text

---

## 🏗️ 實體分層架構

### Layer 1: 多租戶與組織架構

```
Super Admin (系統級)
    └─ Brand (品牌)
        └─ Tenant (租戶/運營商)
            └─ Agent (代理)
                └─ Player (玩家)
```markdown

**核心實體**:
- `brands` - 品牌主體
- `tenants` - 租戶/運營商
- `agents` - 代理系統（無限層級）
- `players` - 終端玩家

---

### Layer 2: 財務核心

```
Player Wallet (玩家錢包)
    ├─ Cash Balance (現金餘額)
    ├─ Bonus Balance (紅利餘額)
    └─ Credit Balance (信用額度)
```markdown

**核心實體**:
- `wallets` - 統一錢包主表
- `transactions` - 所有資金流轉記錄
- `bonus_wallets` - 紅利子錢包
- `withdrawal_requests` - 提款申請
- `deposit_records` - 存款記錄

---

### Layer 3: 遊戲與投注

```
Game Provider
    └─ Game
        └─ Game Session
            └─ Bet Record (下注記錄)
```sql

**核心實體**:
- `game_providers` - 遊戲供應商 (GP)
- `games` - 遊戲目錄
- `game_sessions` - 遊戲會話
- `bet_records` - 投注明細
- `game_round_aggregation` - 遊戲局彙總

---

### Layer 4: 活動與風控

**活動系統**:
- `activities` - 活動配置
- `bonuses` - 紅利發放記錄
- `wagering_progress` - 流水進度追蹤

**風控系統**:
- `risk_rules` - 風控規則配置
- `risk_scores` - 玩家風險評分
- `risk_events` - 風險事件記錄
- `approval_workflows` - 審批工作流

---

## 🔑 跨模組外鍵關係

### 主鍵與外鍵約束表

| 子表 (From) | 外鍵字段 | 父表 (To) | 關係類型 | 級聯刪除 | 說明 |
|------------|---------|----------|---------|---------|------|
| **玩家相關** |
| `players` | `tenant_id` | `tenants` | N:1 | RESTRICT | 玩家屬於租戶 |
| `players` | `agent_id` | `agents` | N:1 | SET NULL | 推薦代理（可為空）|
| `players` | `vip_level_id` | `vip_levels` | N:1 | SET NULL | VIP等級 |
| `wallets` | `player_id` | `players` | 1:1 | CASCADE | 錢包與玩家綁定 |
| **交易相關** |
| `transactions` | `player_id` | `players` | N:1 | RESTRICT | 交易所有者 |
| `transactions` | `wallet_id` | `wallets` | N:1 | RESTRICT | 關聯錢包 |
| `transactions` | `game_id` | `games` | N:1 | SET NULL | 遊戲交易（可選）|
| `transactions` | `psp_id` | `psps` | N:1 | SET NULL | 支付服務商 |
| **遊戲相關** |
| `games` | `provider_id` | `game_providers` | N:1 | CASCADE | 遊戲屬於GP |
| `game_sessions` | `player_id` | `players` | N:1 | RESTRICT | 玩家會話 |
| `game_sessions` | `game_id` | `games` | N:1 | RESTRICT | 遊戲會話 |
| `bet_records` | `session_id` | `game_sessions` | N:1 | CASCADE | 屬於某會話 |
| `bet_records` | `player_id` | `players` | N:1 | RESTRICT | 下注玩家 |
| **活動相關** |
| `bonuses` | `player_id` | `players` | N:1 | CASCADE | 紅利所有者 |
| `bonuses` | `activity_id` | `activities` | N:1 | RESTRICT | 活動來源 |
| `wagering_progress` | `bonus_id` | `bonuses` | N:1 | CASCADE | 流水進度追蹤 |
| **代理相關** |
| `agents` | `parent_agent_id` | `agents` | N:1 | RESTRICT | 上級代理（自引用）|
| `agents` | `tenant_id` | `tenants` | N:1 | RESTRICT | 所屬租戶 |
| `commission_records` | `agent_id` | `agents` | N:1 | RESTRICT | 佣金所有者 |
| `commission_records` | `player_id` | `players` | N:1 | RESTRICT | 佣金來源玩家 |
| **風控相關** |
| `risk_scores` | `player_id` | `players` | N:1 | CASCADE | 玩家風險評分 |
| `risk_events` | `transaction_id` | `transactions` | N:1 | CASCADE | 風險事件觸發交易 |
| `withdrawal_requests` | `player_id` | `players` | N:1 | RESTRICT | 提款申請人 |
| `approval_records` | `request_id` | `withdrawal_requests` | N:1 | CASCADE | 審批記錄 |

---

## 📊 核心數據表設計

### 1. Player (玩家表)

**表名**: `players`
**功能**: 玩家賬戶核心信息


**關鍵欄位說明**:
- **email_blind_index**: 使用 HMAC-SHA256 的盲索引，用於加密郵箱的可檢索查詢
- **password_hash**: 使用 Argon2id 算法，參數為 m=65536, t=3, p=4
- **kyc_status**: 支持增強盡職調查 (EDD) 流程

---

### 2. Wallet (錢包表)

**表名**: `wallets`
**功能**: 玩家統一錢包（現金+紅利+信用）


**可下注餘額公式**:
```text
Playable Balance = Cash + Bonus + (Credit Limit - Credit Used) - Locked Balance
```

**並發控制**:
- 使用 `version` 欄位實現樂觀鎖
- 更新語句示例：

---

### 3. Transaction (交易表)

**表名**: `transactions`
**功能**: 所有資金流轉的統一記錄



---

### 4. Bonus (紅利表)

**表名**: `bonuses`
**功能**: 玩家紅利發放與流水追蹤



---

### 5. Game (遊戲表)

**表名**: `games`
**功能**: 遊戲目錄與元數據


---

### 6. Withdrawal_Request (提款申請表)

**表名**: `withdrawal_requests`
**功能**: 玩家提款申請與審核流程


**SAGA 分佈式事務流程**:
```text
1. 創建提款申請 (Pending)
2. 鎖定錢包餘額 (Locked)
3. 風控檢測 (Risk Check)
4. 多層審批 (L1 → L2 → L3)
5. PSP代付 (Processing)
6. 完成/回滾 (Completed/Failed)
```

---

## ✅ 數據一致性約束

### 1. 錢包餘額一致性



---

### 2. 流水計算一致性

**三層驗證架構**:
```text
Layer 1: 風控引擎驗證 (實時)
    ↓ 有效投注標記
Layer 2: 財務層驗證 (批次)
    ↓ 流水數據彙總
Layer 3: 活動層應用 (觸發)
    ↓ 活動進度更新
```


---

### 3. 多租戶數據隔離

**隔離策略**:

**Schema分離** (推薦用於大型租戶):
```text
database_tenant_1
database_tenant_2
database_tenant_3
```sql


**混合模式**:
- 大型租戶：獨立數據庫
- 中小租戶：共享數據庫（行級隔離）

---

## 🔍 索引策略

### 高頻查詢索引

| 表名 | 索引名稱 | 索引列 | 索引類型 | 說明 |
|------|---------|--------|---------|------|
| `players` | `idx_tenant_username` | `(tenant_id, username)` | UNIQUE | 租戶內唯一用戶名 |
| `players` | `idx_email_blind_index` | `email_blind_index` | BTREE | 郵箱盲索引查詢 |
| `wallets` | `idx_player_updated` | `(player_id, updated_at)` | BTREE | 玩家錢包歷史 |
| `transactions` | `idx_player_type_date` | `(player_id, type, created_at DESC)` | BTREE | 玩家交易記錄 |
| `transactions` | `idx_turnover` | `(is_valid_turnover, player_id, created_at)` | BTREE | 流水計算查詢 |
| `bonuses` | `idx_player_status_expires` | `(player_id, status, expires_at)` | BTREE | 活動紅利查詢 |
| `games` | `idx_provider_category` | `(provider_id, category, status)` | BTREE | 遊戲目錄分類 |
| `withdrawal_requests` | `idx_status_risk_created` | `(status, risk_level, created_at)` | BTREE | 提款審核隊列 |

### 覆蓋索引 (Covering Index)


---

## 🔐 數據安全與加密

### 加密字段策略

| 數據類型 | 加密方式 | 密鑰管理 | 查詢策略 |
|---------|---------|---------|---------|
| **PII (個人身份信息)** |
| 郵箱 | AES-256-GCM | KMS (每租戶獨立) | 盲索引查詢 |
| 手機號 | AES-256-GCM | KMS | 盲索引查詢 |
| 身份證號 | AES-256-GCM | KMS | 盲索引查詢 |
| 姓名 | AES-256-GCM | KMS | 無法直接查詢 |
| 地址 | AES-256-GCM | KMS | 無法直接查詢 |
| **金融信息** |
| 銀行卡號 | AES-256-GCM | HSM | Token化 + 盲索引 |
| CVV | 不存儲 | N/A | 交易時即時處理 |
| **憑證** |
| 密碼 | Argon2id | Salt per-record | Hash比對 |
| MFA密鑰 | AES-256-GCM | KMS | 解密後驗證 |

### 盲索引 (Blind Index) 實現

**原理**:
```text
Blind Index = HMAC-SHA256(index_key, plaintext_value)
```


**金鑰輪替策略**:
1. 保留舊金鑰（grace period）
2. 創建新金鑰
3. 後台批次重加密
4. 雙金鑰查詢過渡期
5. 停用舊金鑰

---

### GDPR Crypto-Shredding

**數據刪除策略**:
```markdown
Player Deletion Request
    ↓
1. 刪除 Data Encryption Key (DEK)
    ↓
2. 標記記錄為 "deleted"
    ↓
3. 保留交易記錄（監管要求，但無法解密PII）
    ↓
4. 生成合規報告
```


---

## 📚 相關文檔

### 數據模型參考
- [02-06 統一錢包模型](../02_Finance_Center/02-06_Unified_Wallet_Model.md) - 錢包詳細設計
- [02-04 流水計算與遊戲對賬](../02_Finance_Center/02-04_Turnover_and_Game_Reconciliation_Analysis.md) - 流水計算邏輯
- [05-01 風控系統](../05_Risk_Management/05-01_Risk_Control_System.md) - 風險評分模型
- [09-03 數據安全標準](../09_System_Security/09-03_Data_Security_Standard.md) - 加密與盲索引

### 架構參考
- [00-00 文檔導航地圖](./00-00_Document_Map.md) - 全局導航
- [00-01 方案概覽](./00-01_Solution_Overview.md) - 整體架構
- [07-01 層級架構](../07_Platform_Management/07-01_Hierarchy_Architecture.md) - 多租戶設計

---

**文檔版本**: 1.0.0
**維護團隊**: Architecture Team & Data Team
**下次審閱**: 2026-04-27（每季度審閱）
