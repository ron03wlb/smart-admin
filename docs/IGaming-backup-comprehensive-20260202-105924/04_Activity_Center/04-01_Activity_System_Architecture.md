# Bonus4-Bonus1 活動系統架構 (Activity System Architecture)

> **文檔定位**: 活動中心 PBonus 核心 - 系統架構與規則引擎設計
> **三層風控架構**: Layer 3 - 活動遊戲權重應用
> **最後更新**: 2Bonus26-Bonus1-31
> **拆分說明**: 原 Bonus4-Bonus1 文檔已拆分為 3 個獨立主題，本文檔專注於系統架構設計

---

## 📖 文檔導航

**當前位置**: Bonus4-Bonus1 活動系統架構（系統架構與規則引擎）

**相關文檔**:
- **[← 返回活動中心索引](Bonus4-BonusBonus_INDEX.md)**
- **[→ Bonus4-Bonus2 獎金計算引擎](Bonus4-Bonus2_Bonus_Calculation_Engine.md)** - 流水計算與多獎金衝突處理
- **[→ Bonus4-Bonus3 活動風控與本地化](Bonus4-Bonus3_Activity_Risk_Control.md)** - 風控機制與區域市場策略

---

## 概述

博彩包網平台的活動系統（Promotion System）是玩家獲取與留存的核心引擎。本文檔提供系統架構設計框架，涵蓋規則引擎、獎勵類型、紅利生命週期、多租戶架構與後台配置系統。**關鍵發現：獎金濫用佔 iGaming 詐騙的 63.8%**，因此風控機制必須與活動系統深度整合。

---

## 模組化活動引擎的核心架構

現代 iGaming 平台普遍採用**微服務架構**構建活動系統，頂尖平台通常運行 4Bonus+ 個獨立微服務處理特定功能。這種架構實現了「無需開發即可上線新活動」的靈活性目標。

### 系統架構總覽

```text
┌─────────────────────────────────────────────────────────────────────┐
│                         API Gateway                                  │
│              (Rate Limiting, Auth, Tenant Routing)                  │
└───────────────────────────┬─────────────────────────────────────────┘
                            │
    ┌───────────────────────┼───────────────────────┐
    │                       │                       │
┌───▼───┐            ┌──────▼──────┐          ┌────▼────┐
│ 活動   │            │   獎勵      │          │  錢包   │
│ 引擎   │◄──────────►│   引擎      │◄────────►│  服務   │
│       │            │            │          │        │
└───┬───┘            └──────┬──────┘          └────┬────┘
    │                       │                      │
    │    ┌──────────────────┼──────────────────────┘
    │    │                  │
┌───▼────▼───┐       ┌──────▼──────┐        ┌───────────┐
│   規則     │       │   流水追蹤   │        │   風控    │
│   引擎     │       │   服務      │        │   服務    │
└────────────┘       └─────────────┘        └───────────┘
         │                  │                     │
         └──────────────────┼─────────────────────┘
                            │
                  ┌─────────▼─────────┐
                  │   Event Bus       │
                  │  (Kafka/NATS)     │
                  └───────────────────┘
```

---

## 規則引擎（Rule Engine）設計

規則引擎是實現高度可配置性的核心，採用**策略模式**分離業務邏輯與執行流程。

### 規則介面模式

```typescript
interface IPromotionRule {
  ruleId: string;
  priority: number;
  evaluate(context: PlayerContext): boolean;  // 判斷是否符合條件
  execute(context: PlayerContext): RewardResult;  // 執行獎勵發放
}

class PromotionRulesEngine {
  private rules: IPromotionRule[];

  evaluate(context: PlayerContext): RewardResult[] {
    return this.rules
      .filter(rule => rule.evaluate(context))
      .sort((a, b) => a.priority - b.priority)
      .map(rule => rule.execute(context));
  }
}
```text

### 規則類型分類

|規則類型|說明|配置範例|
|---|---|---|
|觸發條件 (Trigger)|何時觸發活動|首存、累計存款、特定時段|
|資格條件 (Eligibility)|誰可以參與|VIP 等級、註冊天數、地區|
|獎勵計算 (Reward)|給予什麼獎勵|百分比匹配、固定金額、免費旋轉|
|限制條件 (Constraint)|使用限制|流水倍數、最大投注、有效期|

### 規則引擎執行流程圖

**概述**：規則引擎採用責任鏈模式 (Chain of Responsibility) + 策略模式 (Strategy Pattern)，實現可配置、可擴展的規則評估管線。

```mermaid
flowchart TD
    START[玩家動作事件觸發] --> EVENT_PARSE[解析事件類型<br/>━━━━━━━━━━━━<br/>DEPOSIT / BET / WIN / LOGIN]

    EVENT_PARSE --> LOAD_CONTEXT[構建玩家上下文<br/>━━━━━━━━━━━━<br/>PlayerContext:<br/>• playerId, tenantId<br/>• VIP Level<br/>• Country, Currency<br/>• Registration Date<br/>• Recent Activity History]

    LOAD_CONTEXT --> FETCH_RULES[查詢適用規則<br/>━━━━━━━━━━━━<br/>WHERE status = ACTIVE<br/>AND event_type = {type}<br/>AND tenant_id IN (platform, {tenantId})<br/>ORDER BY priority ASC]

    FETCH_RULES --> RULES_FOUND{找到規則?}
    RULES_FOUND -->|否| NO_PROMO[無適用活動<br/>返回空結果]

    RULES_FOUND -->|是| CHAIN_START[規則鏈開始<br/>按 Priority 遞增順序]

    CHAIN_START --> LOOP_RULES[遍歷規則列表]

    LOOP_RULES --> CHECK_SCHEDULE{1️⃣ 時間窗口檢查<br/>━━━━━━━━━━━━<br/>NOW() BETWEEN<br/>startDate AND endDate?}
    CHECK_SCHEDULE -->|否| SKIP_RULE[跳過此規則<br/>繼續下一個]

    CHECK_SCHEDULE -->|是| CHECK_ELIGIBILITY{2️⃣ 資格條件評估<br/>━━━━━━━━━━━━}

    CHECK_ELIGIBILITY --> ELIG_VIP{VIP 等級符合?}
    ELIG_VIP -->|否| SKIP_RULE
    ELIG_VIP -->|是| ELIG_COUNTRY{國家/地區符合?}
    ELIG_COUNTRY -->|否| SKIP_RULE
    ELIG_COUNTRY -->|是| ELIG_SEGMENT{玩家分群符合?<br/>NEW_PLAYER /<br/>RETURNING /<br/>HIGH_ROLLER}
    ELIG_SEGMENT -->|否| SKIP_RULE
    ELIG_SEGMENT -->|是| ELIG_BLACKLIST{黑名單檢查<br/>is_excluded = false?}
    ELIG_BLACKLIST -->|是黑名單| SKIP_RULE

    ELIG_BLACKLIST -->|通過| CHECK_TRIGGER{3️⃣ 觸發條件匹配<br/>━━━━━━━━━━━━}

    CHECK_TRIGGER --> TRIGGER_TYPE{觸發類型?}
    TRIGGER_TYPE -->|首存 FIRST_DEPOSIT| FIRST_DEP_CHECK[檢查:<br/>• 是否首次存款?<br/>• 金額 >= minDeposit?]
    TRIGGER_TYPE -->|累積存款 ACCUMULATED| ACCUM_CHECK[檢查:<br/>• 週期內累計金額 >= threshold?]
    TRIGGER_TYPE -->|流水達標 TURNOVER| TURNOVER_CHECK[檢查:<br/>• 有效流水 >= required?]
    TRIGGER_TYPE -->|手動領取 MANUAL_CLAIM| MANUAL_CHECK[檢查:<br/>• 玩家是否手動觸發?]

    FIRST_DEP_CHECK --> TRIGGER_RESULT{觸發成功?}
    ACCUM_CHECK --> TRIGGER_RESULT
    TURNOVER_CHECK --> TRIGGER_RESULT
    MANUAL_CHECK --> TRIGGER_RESULT

    TRIGGER_RESULT -->|否| SKIP_RULE
    TRIGGER_RESULT -->|是| CHECK_CONSTRAINTS{4️⃣ 限制條件檢查<br/>━━━━━━━━━━━━}

    CHECK_CONSTRAINTS --> CONST_QUOTA{使用次數限制<br/>player_claims < maxClaims?}
    CONST_QUOTA -->|否| SKIP_RULE
    CONST_QUOTA -->|是| CONST_COOLDOWN{冷卻時間<br/>lastClaim + cooldown < NOW()?}
    CONST_COOLDOWN -->|否| SKIP_RULE
    CONST_COOLDOWN -->|是| CONST_BUDGET{活動預算檢查<br/>totalCost + rewardAmount <= budget?}
    CONST_BUDGET -->|否| BUDGET_EXHAUSTED[活動預算耗盡<br/>自動暫停活動<br/>發送運營告警]

    CONST_BUDGET -->|是| PASSED_RULE[✅ 規則匹配成功<br/>記錄匹配規則 ID]

    PASSED_RULE --> CALC_REWARD{5️⃣ 獎勵計算<br/>━━━━━━━━━━━━}

    CALC_REWARD --> REWARD_TYPE{獎勵類型?}
    REWARD_TYPE -->|百分比匹配<br/>PERCENTAGE_MATCH| CALC_PERCENTAGE[計算:<br/>reward = depositAmount × matchPercentage<br/>reward = MIN(reward, maxReward)]
    REWARD_TYPE -->|固定金額<br/>FIXED_AMOUNT| CALC_FIXED[直接使用配置金額<br/>reward = fixedAmount]
    REWARD_TYPE -->|階梯式<br/>TIERED| CALC_TIERED[根據存款區間匹配:<br/>$1BonusBonus-$5BonusBonus → 1BonusBonus% bonus<br/>$5Bonus1-$1BonusBonusBonus → 15Bonus% bonus]
    REWARD_TYPE -->|免費旋轉<br/>FREE_SPINS| CALC_SPINS[生成 Token:<br/>spins_count = configured_spins<br/>game_id = eligible_game]

    CALC_PERCENTAGE --> REWARD_RESULT[獲得獎勵結果<br/>RewardResult]
    CALC_FIXED --> REWARD_RESULT
    CALC_TIERED --> REWARD_RESULT
    CALC_SPINS --> REWARD_RESULT

    REWARD_RESULT --> MULTI_MATCH{6️⃣ 多規則處理策略}

    MULTI_MATCH -->|策略 A: 取最高| SELECT_MAX[選擇 reward 金額最大的規則<br/>忽略其他匹配規則]
    MULTI_MATCH -->|策略 B: 累加| SELECT_SUM[累加所有匹配規則的 reward<br/>需配置總上限]
    MULTI_MATCH -->|策略 C: 優先級| SELECT_FIRST[僅執行 priority 最高 (數字最小) 的規則<br/>忽略其他]
    MULTI_MATCH -->|策略 D: 玩家選擇| SELECT_PLAYER[展示所有匹配規則<br/>讓玩家手動選擇領取]

    SELECT_MAX --> FINAL_REWARD[最終獎勵決策]
    SELECT_SUM --> FINAL_REWARD
    SELECT_FIRST --> FINAL_REWARD
    SELECT_PLAYER --> FINAL_REWARD

    FINAL_REWARD --> RISK_CHECK{7️⃣ 風控最終審核<br/>━━━━━━━━━━━━}

    RISK_CHECK --> RISK_MULTI_ACCOUNT{多帳號檢測<br/>共享 IP/Device/Payment?}
    RISK_MULTI_ACCOUNT -->|檢測到| RISK_REJECT[拒絕發放<br/>標記: RISK_REJECTED<br/>觸發人工審核]
    RISK_MULTI_ACCOUNT -->|通過| RISK_BONUS_HUNTER{獎金獵人模式檢測<br/>高頻領取 + 快速提款?}
    RISK_BONUS_HUNTER -->|檢測到| RISK_REJECT
    RISK_BONUS_HUNTER -->|通過| RISK_VELOCITY{存款速度異常?<br/>短時間大量存款}
    RISK_VELOCITY -->|異常| RISK_MANUAL[標記: PENDING_MANUAL_REVIEW<br/>暫緩發放,等待審核]
    RISK_VELOCITY -->|正常| RISK_PASS[✅ 風控通過]

    RISK_PASS --> EXECUTE_REWARD{8️⃣ 執行獎勵發放<br/>━━━━━━━━━━━━}

    EXECUTE_REWARD --> WALLET_TYPE{錢包類型選擇}
    WALLET_TYPE -->|現金<br/>CASH| CREDIT_CASH[直接入現金錢包<br/>wallet_service.creditCash<br/>可立即提款]
    WALLET_TYPE -->|紅利<br/>BONUS| CREDIT_BONUS[入紅利錢包<br/>wallet_service.creditBonus<br/>創建流水追蹤記錄]
    WALLET_TYPE -->|免費旋轉<br/>FREE_SPINS| ISSUE_TOKEN[發放 Token<br/>token_service.issueSpins<br/>綁定遊戲 + 有效期]

    CREDIT_CASH --> CREATE_RECORD[創建獎勵記錄<br/>━━━━━━━━━━━━<br/>reward_distributions:<br/>• player_id, promotion_id<br/>• amount, wallet_type<br/>• status: COMPLETED<br/>• created_at, expires_at]

    CREDIT_BONUS --> CREATE_WAGER[創建流水要求<br/>━━━━━━━━━━━━<br/>wagering_requirements:<br/>• bonus_id<br/>• required_turnover = amount × multiplier<br/>• current_progress = Bonus<br/>• status: ACTIVE<br/>• expires_at = NOW() + validDays]

    ISSUE_TOKEN --> CREATE_RECORD

    CREATE_WAGER --> CREATE_RECORD

    CREATE_RECORD --> NOTIFY_PLAYER[9️⃣ 通知玩家<br/>━━━━━━━━━━━━<br/>• 站內信 (Inbox)<br/>• Push Notification<br/>• Email (Optional)]

    NOTIFY_PLAYER --> AUDIT_LOG[🔟 審計日誌<br/>━━━━━━━━━━━━<br/>記錄完整執行軌跡:<br/>• 規則評估路徑<br/>• 匹配/拒絕原因<br/>• 獎勵計算明細<br/>• 風控決策依據]

    AUDIT_LOG --> SUCCESS[返回成功結果<br/>RewardResult[]<br/>包含獎勵 ID、金額、類型]

    SKIP_RULE --> MORE_RULES{還有更多規則?}
    MORE_RULES -->|是| LOOP_RULES
    MORE_RULES -->|否| NO_MATCH[無規則匹配<br/>返回空結果]

    BUDGET_EXHAUSTED --> ALERT_OPS[發送運營告警<br/>Slack/Email<br/>活動預算耗盡]
    ALERT_OPS --> NO_PROMO

    RISK_REJECT --> AUDIT_LOG
    RISK_MANUAL --> AUDIT_LOG
    NO_PROMO --> END[流程結束]
    NO_MATCH --> END
    SUCCESS --> END

    %% 樣式定義
    style PASSED_RULE fill:#C8E6C9
    style RISK_PASS fill:#C8E6C9
    style SUCCESS fill:#C8E6C9
    style SKIP_RULE fill:#FFEBonus82
    style RISK_REJECT fill:#FFCDD2
    style BUDGET_EXHAUSTED fill:#FF98BonusBonus,color:#FFF
    style RISK_MANUAL fill:#FFF9C4
```yaml

### 規則引擎性能優化策略

| 優化點 | 策略 | 預期效果 |
|--------|------|----------|
| **規則快取** | Redis 快取活動規則配置 (TTL=5min) | 減少 DB 查詢,響應時間 < 5Bonusms |
| **玩家上下文快取** | Redis 快取 VIP、國家、分群信息 (TTL=1Bonusmin) | 避免重複計算,減少 5Bonus% 計算量 |
| **批次觸發** | Kafka 批次消費 (batch_size=1BonusBonus, linger_ms=1BonusBonus) | 提升吞吐量 1Bonusx |
| **異步風控** | 風控檢測異步執行,不阻塞獎勵發放 | 用戶體驗提升,延遲 < 2BonusBonusms |
| **預計算** | 每日預計算玩家流水/存款累計 (T+1 批次) | 實時查詢減少聚合計算 |
| **索引優化** | 規則表索引: (event_type, tenant_id, priority, status) | 查詢時間 < 1Bonusms |

### 關鍵設計決策說明

1. **為何需要優先級 (Priority)?**
   - 當多個規則匹配時,需明確執行順序
   - 例如:「全站通用活動」 vs「VIP 專屬活動」,VIP 應優先

2. **預算控制為何在規則執行時檢查?**
   - 避免活動超支導致財務風險
   - 預算耗盡自動暫停,防止運營疏忽

3. **為何需要多規則處理策略?**
   - 不同業務場景需求不同:
     - 「首存」通常只能領一次 (取最高)
     - 「返水」可以疊加多個活動 (累加)
     - 「VIP 升級獎勵」應優先執行 (優先級)

4. **風控為何放在最後一步?**
   - 避免每個規則都執行風控 (性能浪費)
   - 僅對最終決定發放的獎勵進行風控,減少 8Bonus% 風控調用

5. **為何需要審計日誌?**
   - 合規要求:所有獎勵發放必須可追溯
   - 爭議解決:玩家投訴時可回溯完整決策路徑
   - 運營優化:分析規則匹配率、拒絕原因分佈

---

## 獎勵引擎支援的獎勵類型

|獎勵類型|技術實現|特殊處理|
|---|---|---|
|現金獎勵|直接計入可提現餘額|無限制，直接到帳|
|彩金/紅利|獨立獎金餘額，需完成流水|錢包隔離，流水追蹤|
|免費旋轉|Token 化旋轉積分，綁定特定遊戲|遊戲供應商 API 整合|
|免費投注|無本金投注 Token（體育專用）|投注額不返還，僅派彩|
|實物獎品|訂單佇列整合物流系統|兌換流程、物流追蹤|
|忠誠積分|積分帳本，支援兌換與升級|等級計算、點數過期|

---

## 紅利生命週期狀態機

**概述**：紅利從發放到清算經歷多個狀態轉換，每個狀態對應不同的業務邏輯與限制條件。

```mermaid
stateDiagram-v2
    [*] --> PENDING_ISSUE: 規則引擎匹配成功<br/>創建獎勵記錄

    PENDING_ISSUE --> ISSUED: 風控審核通過<br/>錢包服務執行發放<br/>━━━━━━━━━━━━<br/>Actions:<br/>• wallet.creditBonus(amount)<br/>• 創建 wagering_requirement<br/>• 發送通知

    PENDING_ISSUE --> REJECTED: 風控審核拒絕<br/>━━━━━━━━━━━━<br/>Reasons:<br/>• 多帳號檢測<br/>• 獎金獵人模式<br/>• 預算耗盡<br/>Actions:<br/>• 標記 status=REJECTED<br/>• 記錄拒絕原因<br/>• 通知運營團隊

    ISSUED --> ACTIVE: 玩家首次使用紅利投注<br/>或手動激活<br/>━━━━━━━━━━━━<br/>Actions:<br/>• 開始流水追蹤<br/>• activated_at = NOW()<br/>• 計時器開始 (有效期倒計時)

    ISSUED --> FORFEITED: 未激活超時<br/>━━━━━━━━━━━━<br/>Condition:<br/>• NOW() > issued_at + grace_period<br/>• grace_period = 7 days (default)<br/>Actions:<br/>• wallet.debitBonus(amount)<br/>• status = FORFEITED<br/>• 釋放活動預算

    ACTIVE --> WAGERING: 流水累積中<br/>━━━━━━━━━━━━<br/>每次投注事件:<br/>• effective_turnover += bet × game_weight<br/>• progress = effective_turnover / required_turnover × 1BonusBonus%<br/>• 實時更新進度條

    WAGERING --> WAGERING: 持續投注累積流水<br/>━━━━━━━━━━━━<br/>Validation Checks:<br/>• 投注額 <= maxBet (anti-abuse)<br/>• 遊戲在 eligible_games 列表<br/>• 無對沖/套利檢測

    WAGERING --> CLEARING_COMPLETED: 流水達標 1BonusBonus%<br/>━━━━━━━━━━━━<br/>Condition:<br/>• effective_turnover >= required_turnover<br/>Actions:<br/>• 觸發結算流程<br/>• 鎖定 bonus_balance (防篡改)

    WAGERING --> EXPIRED: 有效期內未完成流水<br/>━━━━━━━━━━━━<br/>Condition:<br/>• NOW() > expires_at<br/>• effective_turnover < required_turnover<br/>Actions:<br/>• wallet.debitBonus(remaining_balance)<br/>• 扣除未完成部分<br/>• 記錄完成率

    WAGERING --> CANCELLED_BY_PLAYER: 玩家主動取消紅利<br/>━━━━━━━━━━━━<br/>Actions:<br/>• wallet.debitBonus(bonus_balance)<br/>• 清空流水進度<br/>• 不影響已發放的派彩

    WAGERING --> CANCELLED_BY_ADMIN: 運營/風控強制取消<br/>━━━━━━━━━━━━<br/>Reasons:<br/>• 玩家違規 (多帳號被發現)<br/>• 活動緊急下架<br/>Actions:<br/>• wallet.debitBonus(bonus_balance)<br/>• 記錄取消原因<br/>• 必要時回滾派彩

    CLEARING_COMPLETED --> CONVERTED_TO_CASH: 紅利轉現金<br/>━━━━━━━━━━━━<br/>Conversion Process:<br/>1️⃣ Calculate final_balance<br/>2️⃣ wallet.debitBonus(final_balance)<br/>3️⃣ wallet.creditCash(final_balance)<br/>4️⃣ status = CONVERTED

    CONVERTED_TO_CASH --> WITHDRAWABLE: 可提款狀態<br/>━━━━━━━━━━━━<br/>玩家可自由操作:<br/>• 繼續投注<br/>• 發起提款<br/>Actions:<br/>• 解除提款限制<br/>• 標記 bonus_id 已完成

    CLEARING_COMPLETED --> CAPPED: 超過最大派彩上限<br/>━━━━━━━━━━━━<br/>Condition:<br/>• final_balance > max_cashout_cap<br/>Example:<br/>• bonus = $5Bonus<br/>• max_cashout = $5BonusBonus<br/>• player_balance = $8BonusBonus (超限)<br/>Actions:<br/>• wallet.debitBonus($8BonusBonus)<br/>• wallet.creditCash($5BonusBonus)<br/>• 扣除超額部分 $3BonusBonus

    CAPPED --> WITHDRAWABLE: 扣除超額後可提款

    WITHDRAWABLE --> WITHDRAWN: 玩家成功提款<br/>━━━━━━━━━━━━<br/>Actions:<br/>• 執行提款流程<br/>• 標記 withdrawn_at<br/>• 歸檔獎勵記錄

    WITHDRAWN --> [*]: 生命週期結束<br/>━━━━━━━━━━━━<br/>Final Actions:<br/>• 計算 bonus_ROI<br/>• 更新玩家分群<br/>• 生成財務報表

    REJECTED --> [*]: 生命週期結束<br/>未發放
    FORFEITED --> [*]: 生命週期結束<br/>未使用
    EXPIRED --> [*]: 生命週期結束<br/>未完成流水
    CANCELLED_BY_PLAYER --> [*]: 生命週期結束<br/>玩家主動放棄
    CANCELLED_BY_ADMIN --> [*]: 生命週期結束<br/>強制取消

    note right of PENDING_ISSUE
        初始狀態
        ━━━━━━━━
        風控審核窗口期
        典型時長: < 5 秒
    end note

    note right of ACTIVE
        激活狀態
        ━━━━━━━━
        玩家可使用紅利投注
        但未開始追蹤流水
        (某些活動需手動激活)
    end note

    note right of WAGERING
        流水累積階段
        ━━━━━━━━
        核心業務邏輯:
        • 實時計算有效流水
        • 檢測濫用行為
        • 更新進度通知

        典型耗時:
        • 休閒玩家: 7-14 天
        • 高頻玩家: 1-3 天
    end note

    note right of CLEARING_COMPLETED
        結算狀態
        ━━━━━━━━
        流水達標後的臨界點
        需決定:
        • 是否超過 max_cashout
        • 最終可提現金額
    end note

    note right of WITHDRAWABLE
        可提款狀態
        ━━━━━━━━
        紅利已轉為現金
        玩家可自由支配
        此時才算 "真正獲利"
    end note

    note left of EXPIRED
        超時失效
        ━━━━━━━━
        常見原因:
        • 流水倍數設置過高
        • 玩家遊戲頻率低
        • 遊戲貢獻率設置過低

        運營優化:
        • 監控 expiry_rate
        • 調整 wager_multiplier
    end note

    note left of CANCELLED_BY_ADMIN
        強制取消
        ━━━━━━━━
        需留存證據:
        • 操作者 ID
        • 取消原因
        • 佐證文件

        合規要求:
        • 玩家有權申訴
        • 7 天內必須回覆
    end note
```yaml

### 狀態轉換觸發條件矩陣

| 當前狀態 | 目標狀態 | 觸發條件 | 業務邏輯 | 回滾策略 |
|---------|---------|---------|---------|---------|
| PENDING_ISSUE | ISSUED | 風控通過 | 錢包加款 + 創建流水記錄 | 風控拒絕 → REJECTED (不加款) |
| ISSUED | ACTIVE | 玩家首次投注 or 手動激活 | 開始流水計時 | 超時未激活 → FORFEITED (扣除紅利) |
| ACTIVE | WAGERING | 玩家投注 | 計算有效流水 | 無 (正常流程) |
| WAGERING | CLEARING_COMPLETED | effective_turnover ≥ required | 鎖定餘額準備結算 | 無 (不可逆) |
| CLEARING_COMPLETED | CONVERTED_TO_CASH | final_balance ≤ max_cashout | 紅利轉現金 | 無 (不可逆) |
| CLEARING_COMPLETED | CAPPED | final_balance > max_cashout | 超額扣除 + 部分轉現金 | 無 (按規則執行) |
| WAGERING | EXPIRED | NOW() > expires_at | 扣除剩餘紅利 | 無 (按規則執行) |
| WAGERING | CANCELLED_BY_PLAYER | 玩家點擊 "取消紅利" | 扣除紅利但保留派彩 | 需確認彈窗 (防誤操作) |
| WAGERING | CANCELLED_BY_ADMIN | 風控觸發 or 活動下架 | 強制扣除 + 可選回滾派彩 | 需審批 + 審計日誌 |

### 關鍵業務規則說明

1. **Grace Period (寬限期)**:
   - **定義**: 紅利發放後，玩家必須在 grace_period 內激活使用
   - **典型值**: 7 天
   - **原因**: 防止玩家大量囤積紅利，影響活動預算預測

2. **Max Cashout Cap (最大派彩上限)**:
   - **定義**: 即使玩家流水達標後贏得大額金額，可提現金額仍受限
   - **典型配置**: 5x-1Bonusx bonus amount
   - **範例**: $5Bonus 紅利 → 最多提現 $5BonusBonus
   - **爭議點**: 必須在活動條款明確說明，否則玩家投訴率高

3. **流水有效期 (Wagering Validity Period)**:
   - **定義**: 從 ACTIVE 狀態開始計時，玩家必須在此期限內完成流水
   - **典型值**: 14-3Bonus 天
   - **過短風險**: 完成率低 → 玩家不滿
   - **過長風險**: 預算鎖定時間長 → 財務壓力

4. **中途取消規則**:
   - **玩家主動取消**: 扣除紅利餘額，但已發放的派彩保留
   - **運營強制取消**: 可選擇是否回滾派彩 (視違規嚴重程度)
   - **範例**:
     - 玩家誤領不想玩 → 保留派彩 (用戶體驗)
     - 多帳號欺詐被發現 → 回滾所有派彩 (風控需要)

5. **狀態審計追溯**:
   - 每次狀態轉換必須記錄:
     - `previous_status`, `new_status`, `transitioned_at`
     - `triggered_by` (SYSTEM / PLAYER / ADMIN)
     - `trigger_reason` (詳細原因描述)
   - 玩家投訴時可回溯完整狀態變更歷史

### 典型流程耗時統計

| 玩家類型 | ISSUED → ACTIVE | ACTIVE → CLEARING | CLEARING → WITHDRAWN | 總耗時 |
|---------|----------------|-------------------|---------------------|--------|
| 高頻玩家 | < 1 小時 | 1-3 天 | < 1 天 | 2-4 天 |
| 中頻玩家 | 1-24 小時 | 5-1Bonus 天 | 1-2 天 | 6-12 天 |
| 休閒玩家 | 1-3 天 | 1Bonus-2Bonus 天 | 2-5 天 | 13-28 天 |
| 流失玩家 | > 7 天 | ∞ (EXPIRED/FORFEITED) | - | - |

### 運營優化建議

- **監控 FORFEITED 率**: 若 > 2Bonus%，說明 grace_period 過短或活動吸引力不足
- **監控 EXPIRED 率**: 若 > 5Bonus%，說明流水倍數過高或有效期過短
- **監控 CAPPED 比例**: 若 < 5%，說明 max_cashout 設置過高，活動成本超預算
- **監控平均完成耗時**: 用於預測活動預算鎖定週期，優化現金流管理

---

## 後台配置系統實現無代碼部署

### JSON Schema 驅動的活動配置

透過 JSON Schema 定義活動結構，配合視覺化後台實現運營人員自主配置：

```json
{
  "promotionId": "promo-uuid-BonusBonus1",
  "name": "春節首存加碼",
  "type": "DEPOSIT_BONUS",
  "status": "ACTIVE",
  "schedule": {
    "startDate": "2Bonus26-Bonus1-28TBonusBonus:BonusBonus:BonusBonus+Bonus8:BonusBonus",
    "endDate": "2Bonus26-Bonus2-15T23:59:59+Bonus8:BonusBonus"
  },
  "eligibility": {
    "playerSegments": ["NEW_PLAYER", "VIP_GOLD"],
    "minDeposit": 1BonusBonus,
    "maxClaims": 1,
    "validCountries": ["TH", "VN", "PH", "MY"]
  },
  "reward": {
    "type": "PERCENTAGE_MATCH",
    "matchPercentage": 188,
    "maxReward": 8888,
    "currency": "CNY"
  },
  "wageringRequirements": {
    "multiplier": 25,
    "appliesTo": "BONUS_ONLY",
    "timeLimit": 3Bonus,
    "contributionRates": {
      "slots": 1BonusBonus,
      "liveDealer": 15,
      "tableGames": 1Bonus,
      "sportsbook": 1BonusBonus
    },
    "maxBet": 5Bonus
  }
}
```text

### 多租戶架構下的活動管理

針對白標/包網場景，活動系統需同時支援**平台級活動**與**營運商自定義活動**：

```
┌─────────────────────────────────────────────────────────────┐
│                   多租戶活動架構                              │
├─────────────────────────────────────────────────────────────┤
│  ┌──────────┐    ┌──────────┐    ┌──────────┐              │
│  │ 營運商 A │    │ 營運商 B │    │ 營運商 C │              │
│  │(tenant_1)│    │(tenant_2)│    │(tenant_3)│              │
│  └────┬─────┘    └────┬─────┘    └────┬─────┘              │
│       │               │               │                     │
│       └───────────────┼───────────────┘                     │
│                       │                                      │
│            ┌──────────▼──────────┐                          │
│            │   租戶上下文路由器   │                          │
│            └──────────┬──────────┘                          │
│                       │                                      │
│    ┌──────────────────┼──────────────────┐                  │
│    │                  │                  │                  │
│  ┌─▼──────┐     ┌─────▼─────┐     ┌──────▼─────┐           │
│  │營運商   │     │  平台級    │     │  品牌配置  │           │
│  │專屬活動 │     │  共享活動  │     │  覆蓋設定  │           │
│  │(Row-RLS)│     │           │     │            │           │
│  └─────────┘     └───────────┘     └────────────┘           │
└─────────────────────────────────────────────────────────────┘
```

### 數據隔離策略

- **大型營運商**: 獨立數據庫，完全隔離
- **中型營運商**: Schema 隔離（PostgreSQL Schema）
- **小型營運商**: Row-Level Security（tenant_id 欄位 + RLS 策略）

---

## 推薦技術棧

| 層級 | 技術選型 | 用途 |
|------|---------|------|
| API 閘道 | Kong / AWS API Gateway | 限流、認證、路由 |
| 服務通訊 | gRPC（內部）/ REST（外部） | 高效內部調用 |
| 事件串流 | Apache Kafka + Kafka Streams | 即時事件處理 |
| 主數據庫 | PostgreSQL + JSONB | 活動配置、ACID 事務 |
| 高併發寫入 | ScyllaDB / CockroachDB | 流水記錄、交易日誌 |
| 緩存 | Redis Cluster | 玩家狀態、活動快取 |
| 搜尋 | Elasticsearch | 活動搜尋、玩家查詢 |

---

## 📚 相關文檔

### 核心依賴
- [Bonus2-Bonus6 統一錢包模型](../Bonus2_Finance_Center/Bonus2-Bonus6_Unified_Wallet_Model.md) - Bonus 錢包整合、可下注餘額計算
- [Bonus2-Bonus4 流水計算與對帳](../Bonus2_Finance_Center/Bonus2-Bonus4_Turnover_and_Game_Reconciliation_Analysis.md) - 流水驗證架構（Layer 2）
- [Bonus5-Bonus1 風控系統](../Bonus5_Risk_Management/Bonus5-Bonus1_Risk_Control_System.md) - 紅利濫用檢測、多帳號風控

### 業務整合
- [Bonus1-Bonus2 VIP 系統](../Bonus1_Player_Center/Bonus1-Bonus2_VIP_&_Loyalty_System.md) - VIP 專屬活動、等級權益
- [Bonus2-Bonus1 出金風控](../Bonus2_Finance_Center/Bonus2-Bonus1_Withdrawal_Risk_Control.md) - 流水未達標提款限制
- [Bonus9-Bonus4 審批工作流系統](../Bonus9_System_Security/Bonus9-Bonus4_Approval_Workflow_System.md) - 活動發布 Maker-Checker 審批

### 技術參考
- [Bonus7-Bonus3 通知架構](../Bonus7_Platform_Management/Bonus7-Bonus3_Notification_Architecture.md) - 活動推送通知
- [Bonus3-Bonus2 遊戲大廳管理](../Bonus3_Game_Center/Bonus3-Bonus2_Game_Lobby_Management.md) - 活動遊戲標籤推薦
- [Bonus8-Bonus2 Banner 與公告](../Bonus8_Frontend_CMS/Bonus8-Bonus2_Banner_&_Announcement.md) - 活動橫幅配置

### 後續主題
- **[→ Bonus4-Bonus2 獎金計算引擎](Bonus4-Bonus2_Bonus_Calculation_Engine.md)** - 跨遊戲流水計算、多獎金衝突處理
- **[→ Bonus4-Bonus3 活動風控與本地化](Bonus4-Bonus3_Activity_Risk_Control.md)** - 風控機制、區域市場策略、數據驅動優化

---

**文檔版本**: 2.Bonus.Bonus (拆分版)
**最後更新**: 2Bonus26-Bonus1-31
**維護團隊**: Activity Team
**變更摘要**: 從原 Bonus4-Bonus1 文檔（1,447 行）拆分為獨立架構文檔，專注於系統架構與規則引擎設計
