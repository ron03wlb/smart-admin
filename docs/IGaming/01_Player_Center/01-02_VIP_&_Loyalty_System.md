# 01-02 VIP 與忠誠度系統 (VIP & Loyalty System)

## 1. 系統概述 (Overview)
旨在透過獎勵機制提升玩家留存率 (Retention) 與終身價值 (LTV)。系統需具備高度靈活性，允許不同商戶自定義其 VIP 層級規則與權益。

## 2. 核心功能需求

### 2.1 層級與升降級 (Levels & Promotion)
- **層級設定**：
  - 支援無限制層級 (如 Bronze, Silver, Gold, Platinum, Diamond)
  - 每個層級需設定個別 Icon 與樣式
- **升級條件 (動態配置)**：
  - 累積存款 (Total Deposit)
  - 累積流水 (Total Turnover) —— 需排除無效投注
  - 積分 (Loyalty Points)
- **保級與降級 (Retention & Demotion)**：
  - 設定 "保級週期" (如每月) 與 "保級條件"
  - 若未達標，自動降級或扣除積分

### 2.2 積分商城 (Point System)
- **積分獲取**：每投注 $X 元獲得 1 點積分 (可依遊戲類型設定權重，如老虎機 100%，百家樂 20%)
- **積分兌換**：
  - 兌換現金 (Bonus / Cash)
  - 兌換實體獎品 (iPhone, 禮券)
  - 兌換遊戲道具 (Free Spins)

### 2.3 VIP 權益 (Privileges)
- **專屬客服**：分配一對一 VIP 經理
- **提款優惠**：
  - 更高的單日提款限額
  - 更快的提款處理 SLA (如 VIP 5分鐘出款)
  - 免除提款手續費
- **專屬紅利**：
  - 升級禮金 (Level Up Bonus)
  - 生日禮金
  - 月度/週度紅利

### 2.4 VIP等級權益詳細清單

| 等級 | 門檻條件 | 專屬紅利 | 返水比例 | 提款限額 | 提款速度 | 專屬客服 | 其他權益 |
|------|---------|---------|---------|---------|---------|---------|---------|
| **Bronze** | 累積存款 $1K<br>累積流水 $5K | 升級禮金 $10 | 0.3% | $5K/日 | 24小時 | 在線客服 | - |
| **Silver** | 累積存款 $5K<br>累積流水 $30K | 升級禮金 $50<br>生日禮金 $20 | 0.5% | $10K/日 | 12小時 | 在線客服 | 每週額外紅利 |
| **Gold** | 累積存款 $20K<br>累積流水 $150K | 升級禮金 $200<br>生日禮金 $100 | 0.8% | $30K/日 | 6小時 | VIP經理 | 每月現金回饋 |
| **Platinum** | 累積存款 $100K<br>累積流水 $1M | 升級禮金 $1000<br>生日禮金 $500 | 1.2% | $100K/日 | 2小時 | VIP經理 | 實體獎品、活動邀請 |
| **Diamond** | 累積存款 $500K<br>累積流水 $5M | 升級禮金 $5000<br>生日禮金 $2000 | 1.5% | 無限制 | 30分鐘 | 1對1 VIP經理 | 豪華旅遊、定制獎勵 |

### 2.5 積分計算規則詳解

**積分獲取公式**：
```
積分 = 有效投注額 × 遊戲權重 × VIP等級倍數
```

**遊戲權重表**：
| 遊戲類型 | 權重 | 說明 |
|---------|------|------|
| 老虎機 (Slots) | 100% | 每投注 $10 獲得 1 積分 |
| 真人娛樂場 (Live Casino) | 50% | 每投注 $20 獲得 1 積分 |
| 撲克 (Poker) | 80% | 每投注 $12.5 獲得 1 積分 |
| 體育投注 (Sports) | 30% | 每投注 $33 獲得 1 積分 |

**VIP等級倍數**：
- Bronze: 1.0x
- Silver: 1.2x
- Gold: 1.5x
- Platinum: 2.0x
- Diamond: 3.0x

**積分兌換比例**：
| 兌換項目 | 所需積分 | 實際價值 | 兌換比例 |
|---------|---------|---------|---------|
| 現金紅利 | 100 積分 | $1 | 100:1 |
| 免費旋轉 (10次) | 50 積分 | $2 | 25:1 |
| iPhone 15 Pro | 500,000 積分 | $1,200 | 416:1 |
| 豪華旅遊套餐 | 1,000,000 積分 | $5,000 | 200:1 |

### 2.6 降級保護機制

**保級週期**: 每月1號 00:00 UTC 檢查上月活動

**保級條件** (以Gold等級為例)：
```
IF 上月存款 >= $1,000 OR 上月流水 >= $10,000
THEN 保持Gold等級
ELSE 降級至Silver等級
```

**降級保護（Grace Period）**：
- **首次未達標**: 發送警告郵件，暫不降級（保護期1個月）
- **連續2次未達標**: 降級1級
- **連續3次未達標**: 降級2級

**降級補償**：
- 降級後7天內，仍享有原等級50%的權益
- 提供"回歸紅利"以激勵重新升級

### 2.7 VIP專屬活動設計

**1. 升級禮金活動**
- 觸發條件：玩家達到新VIP等級
- 發放方式：自動發放到紅利錢包
- 流水要求：5x（例如$100禮金需完成$500流水）

**2. 生日禮金**
- 觸發條件：生日當月
- 發放方式：需玩家主動申請（客服確認身份）
- 流水要求：3x

**3. 月度/週度紅利**
- Gold及以上等級：每週一發放
- 金額：根據上週有效流水的0.5-1.5%
- 流水要求：1x（低流水要求以提升體驗）

**4. 專屬錦標賽**
- Platinum及以上專屬
- 獎池：$50,000+
- 頻率：每季度1次

## 3. 業務規則與審批

### 3.1 配置變更審批流程

**敏感操作清單**：
1. 修改VIP升級/保級條件
2. 調整積分兌換比例
3. 變更VIP權益（返水、提款限額）
4. 批量升降級操作

**審批流程**：
```
1. 運營人員提交變更申請
   ↓
2. 系統自動試算影響範圍
   - 預估受影響玩家數
   - 預估成本/收益變化
   ↓
3. VIP主管審核
   - 批准：進入排程
   - 拒絕：返回修訂
   ↓
4. 排程生效
   - 即時生效
   - 定時生效（次日 00:00 UTC）
   ↓
5. 自動通知受影響玩家
```

**變更影響試算範例**：
```sql
-- 試算：將Gold升級條件從$20K降至$10K
SELECT
    '當前Gold玩家' AS category,
    COUNT(*) AS player_count
FROM players
WHERE vip_level = 'Gold'

UNION ALL

SELECT
    '新達標玩家' AS category,
    COUNT(*) AS player_count
FROM players
WHERE vip_level = 'Silver'
  AND total_deposit >= 10000
  AND total_deposit < 20000;
```

### 3.2 風控規則整合

**黑名單排除**：
- 風控標記為 "Bonus Abuser" 的玩家：
  - ❌ 凍結VIP權益獲取
  - ❌ 凍結積分累積
  - ⚠️ 保留已獲得的VIP等級（避免法律糾紛）
  - ✅ 允許積分兌換（已獲得權益）

**異常行為偵測**：
```
IF 玩家在24小時內：
   - 存款 > $10K
   - 僅遊戲 < 10 分鐘
   - 立即申請VIP權益
THEN 標記為 "VIP Farming" → 人工審核
```

### 3.3 多租戶配置

**租戶級配置項**：
- VIP等級數量（3-10級）
- 升級/保級條件
- 積分獲取倍率
- 權益內容
- UI主題色與圖標

**共享資源池**（可選）：
- 多個小型租戶共享積分商城實體獎品庫存
- 降低運營成本

## 4. 數據表設計

### 4.1 VIP Level Config Table (vip_level_configs)

```sql
CREATE TABLE vip_level_configs (
    level_id INT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    level_name VARCHAR(50) NOT NULL,  -- Bronze, Silver, Gold...
    level_order INT NOT NULL,         -- 排序：1, 2, 3...
    level_icon_url VARCHAR(255),
    level_color VARCHAR(7),           -- HEX顏色碼

    -- 升級條件
    required_total_deposit BIGINT NOT NULL,
    required_total_turnover BIGINT NOT NULL,
    required_loyalty_points INT DEFAULT 0,

    -- 保級條件（每月）
    retention_deposit BIGINT NOT NULL,
    retention_turnover BIGINT NOT NULL,

    -- 權益
    cashback_percentage DECIMAL(5,4) NOT NULL,  -- 返水比例 (0.0050 = 0.5%)
    withdrawal_limit_daily BIGINT NOT NULL,
    withdrawal_speed_hours INT NOT NULL,       -- 處理時效（小時）
    loyalty_point_multiplier DECIMAL(5,2) DEFAULT 1.0,

    -- 紅利
    level_up_bonus BIGINT DEFAULT 0,
    birthday_bonus BIGINT DEFAULT 0,
    monthly_bonus BIGINT DEFAULT 0,

    -- 狀態
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_tenant (tenant_id),
    INDEX idx_order (level_order)
);
```

### 4.2 Player VIP Status Table (player_vip_status)

```sql
CREATE TABLE player_vip_status (
    status_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    player_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,

    -- 當前VIP等級
    current_level_id INT NOT NULL,
    current_level_name VARCHAR(50),

    -- 升級進度
    total_deposit BIGINT DEFAULT 0,
    total_turnover BIGINT DEFAULT 0,
    loyalty_points INT DEFAULT 0,

    -- 保級狀態
    last_retention_check DATE,
    retention_warning_count INT DEFAULT 0,
    grace_period_end DATE,

    -- 歷史
    promoted_at TIMESTAMP,
    last_demoted_at TIMESTAMP,

    -- 狀態
    is_frozen BOOLEAN DEFAULT FALSE,     -- 風控凍結
    freeze_reason VARCHAR(255),

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE INDEX idx_player (player_id),
    INDEX idx_tenant_level (tenant_id, current_level_id),
    FOREIGN KEY (player_id) REFERENCES players(player_id) ON DELETE CASCADE,
    FOREIGN KEY (current_level_id) REFERENCES vip_level_configs(level_id)
);
```

### 4.3 Loyalty Points Transactions Table (loyalty_point_transactions)

```sql
CREATE TABLE loyalty_point_transactions (
    transaction_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    player_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,

    -- 交易類型
    type ENUM('earn', 'redeem', 'expire', 'manual_adjust') NOT NULL,

    -- 金額
    points INT NOT NULL,              -- 正數為獲得，負數為消耗
    balance_before INT NOT NULL,
    balance_after INT NOT NULL,

    -- 來源
    source_type ENUM('game', 'deposit', 'promotion', 'birthday', 'manual'),
    source_id BIGINT,                 -- 關聯的遊戲ID/交易ID

    -- 兌換詳情（僅redeem類型）
    redeem_item_id BIGINT,
    redeem_item_name VARCHAR(255),
    redeem_item_value BIGINT,

    -- 審計
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),

    INDEX idx_player (player_id, created_at DESC),
    INDEX idx_tenant (tenant_id),
    INDEX idx_type (type),
    FOREIGN KEY (player_id) REFERENCES players(player_id) ON DELETE CASCADE
);
```

### 4.4 VIP Level History Table (vip_level_history)

```sql
CREATE TABLE vip_level_history (
    history_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    player_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,

    -- 變更
    from_level_id INT,
    from_level_name VARCHAR(50),
    to_level_id INT NOT NULL,
    to_level_name VARCHAR(50) NOT NULL,

    -- 原因
    change_reason ENUM('promoted', 'demoted', 'manual_adjust') NOT NULL,
    change_note TEXT,

    -- 條件達成情況
    deposit_at_change BIGINT,
    turnover_at_change BIGINT,
    points_at_change INT,

    -- 審計
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),

    INDEX idx_player (player_id, created_at DESC),
    INDEX idx_tenant (tenant_id),
    FOREIGN KEY (player_id) REFERENCES players(player_id) ON DELETE CASCADE
);
```

## 5. 相關文檔

### 業務邏輯參考
- [02-06 統一錢包模型](../02_Finance_Center/02-06_Unified_Wallet_Model.md) - 紅利錢包集成
- [04-01 活動系統設計](../04_Activity_Center/04-01_Activity_System_Design.md) - VIP專屬活動
- [02-04 流水計算與對賬](../02_Finance_Center/02-04_Turnover_and_Game_Reconciliation_Analysis.md) - 有效流水定義

### 技術架構參考
- [00-03 數據模型總覽](../00_Concept_&_Analysis/00-03_Data_Model_Overview.md) - 數據庫設計
- [09-02 審計日誌與審批](../09_System_Security/09-02_Audit_Log_&_Approval.md) - 配置變更審批

---

**文檔版本**: 1.1.0
**最後更新**: 2026-01-27
**維護團隊**: Product Team & Backend Team
