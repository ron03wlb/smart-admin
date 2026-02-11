# 獎金計算引擎業務需求 (Bonus Calculation Engine Requirements)

> **Canonical Source**: [source-archive/04_Activity_Center/04-02_Bonus_Calculation_Engine.md](../../source-archive/04_Activity_Center/04-02_Bonus_Calculation_Engine.md)
> **Audience**: Executives, Product Managers
> **Related Architecture**: [Bonus_Calculation_Engine.md](../../architecture/04_Activity_Engine/Bonus_Calculation_Engine.md)
> **Last Synced**: 2026-02-08
> **Source Version**: 1.0.0

---

## Business Value

This requirements document delivers strategic value by:
- **Fair Play Assurance**: Defines game contribution rates (Slots 100%, Blackjack 5-10%, Poker 0%) ensuring wagering requirements reflect true game risk
- **Fraud Prevention**: Documents invalid bet types (hedge betting, arbitrage, low odds <1.5) to prevent bonus abuse and player exploitation
- **Cost Control**: Establishes multi-bonus conflict resolution strategies (MAX_REWARD, TYPE_EXCLUSIVE) with global limits (max 5 active bonuses, $10K balance cap)
- **Operational Visibility**: Specifies daily reconciliation with ≤0.01% deviation tolerance and A/B testing framework for rule optimization

---

## Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Wagering Completion Rate | ≥30% | Players completing wagering requirements / Total bonus claims |
| Conflict Complaint Rate | <2% | Player complaints about conflict rules / Total claims |
| Bonus Cost Ratio | <15% of revenue | Total bonus payout / Gross Gaming Revenue |
| Daily Reconciliation Deviation | ≤0.01% | Finance system vs Activity system turnover |
| Activity Switch Frequency | <5 per player/hour | Monitoring for suspected arbitrage testing |

---

## 概述

本文檔定義活動系統獎金計算引擎的業務需求，包括跨遊戲類型的流水計算規則、多獎金衝突處理策略，以及與風控系統整合的業務規則。這是確保活動系統公平性與財務一致性的核心業務文檔。

---

## 遊戲流水貢獻率規則 (Game Contribution Rules)

### 業務目標

不同遊戲類型的莊家優勢差異巨大（老虎機 ~5%、二十一點 ~0.5%），需透過貢獻率系統標準化流水計算，確保：
- 玩家無法通過低莊家優勢遊戲輕易完成流水要求
- 各遊戲類型對獎金釋放的貢獻度反映其真實風險

### 遊戲權重配置表

| 遊戲類型 | 貢獻率 (Contribution Rate) | 業務原因 | 玩家說明範例 |
|---------|---------------------------|---------|-------------|
| 老虎機/Slots | 100% | 高莊家優勢，標準基準 | $100 投注 = $100 流水 |
| 體育博彩 | 100% | 結果不可控，風險可接受 | $100 投注 = $100 流水 (需符合賠率要求) |
| 刮刮卡 | 100% | 單次結果型遊戲 | $100 投注 = $100 流水 |
| 輪盤 | 10-20% | 可對沖投注 | $100 投注 = $15 流水 |
| 百家樂 | 10-15% | 接近 50/50 賠率 | $100 投注 = $15 流水 |
| 二十一點 | 5-10% | 低莊家優勢，可計牌 | $100 投注 = $10 流水 |
| 視頻撲克 | 10-20% | 策略可降低莊家優勢 | $100 投注 = $15 流水 |
| 真人娛樂場 | 5-15% | 與桌遊類似 | $100 投注 = $10 流水 |
| 撲克（抽水池）| 0% | 玩家對玩家，通常排除 | $100 投注 = $0 流水 |

### 計算範例

**場景**: 玩家在二十一點投注 $100，貢獻率 10%

| 檢查層級 | 因子 | 說明 |
|---------|------|------|
| Layer 1 風控因子 | 1 | 通過風控驗證 |
| Layer 2 狀態因子 | 100% | 結果為 WIN/LOSS |
| Layer 3 遊戲權重 | 10% | 二十一點貢獻率 |
| **有效流水** | **$10** | $100 × 1 × 100% × 10% |

---

## 有效流水驗證規則 (Valid Turnover Rules)

### 驗證目的

流水計算必須過濾「無風險投注」與「對沖投注」，避免玩家濫用活動機制。

### 無效投注類型

| 投注類型 | 處理方式 | 業務原因 |
|---------|---------|---------|
| 對沖投注 (Hedge Betting) | 拒絕計入流水 | 玩家無風險套利 |
| 套利投注 (Arbitrage) | 拒絕計入流水 | 利用賠率差異套利 |
| 低賠率投注 (<1.5 歐洲盤) | 拒絕計入流水 | 風險極低的確定性投注 |
| 和局/作廢 (DRAW/VOID) | 不計流水 | 投注無風險承擔 |
| 半贏/半輸 | 50% 計入流水 | 部分風險承擔 |

### 狀態因子對照表

| 結算狀態 | 流水因子 | 說明 |
|---------|---------|------|
| WIN (贏) | 100% | 完整計入 |
| LOSS (輸) | 100% | 完整計入 |
| DRAW/TIE (和局) | 0% | 無風險不計 |
| VOID/CANCEL (作廢) | 0% | 注單無效 |
| HALF_WIN/HALF_LOSS | 50% | 部分計入 |

---

## 多獎金衝突處理策略 (Multi-Bonus Conflict Resolution)

### 業務目標

當玩家同時符合多個活動時，系統需明確的衝突處理策略：
- 避免活動疊加濫用
- 維護用戶體驗一致性
- 控制獎金成本

### 策略類型對比

| 策略 | 適用場景 | 用戶體驗 | 成本控制 |
|------|---------|---------|---------|
| **取最高 (MAX_REWARD)** | 互斥首存活動 | ⭐⭐⭐⭐⭐ 最佳 | 🟢 可控 |
| **按優先級 (PRIORITY)** | VIP 等級活動 | ⭐⭐⭐ 中等 | 🟢 可控 |
| **玩家選擇 (PLAYER_CHOICE)** | 多樣化活動池 | ⭐⭐⭐⭐ 良好 | 🟡 中等 |
| **全部疊加 (STACK_ALL)** | 返水 + 簽到 | ⭐⭐⭐⭐⭐ 最佳 | 🔴 高風險 |
| **同類型互斥 (TYPE_EXCLUSIVE)** | 混合活動組 | ⭐⭐⭐⭐ 良好 | 🟢 可控 |
| **順序模式 (SEQUENTIAL)** | 新手任務鏈 | ⭐⭐⭐ 中等 | 🟢 可控 |

### 流水追蹤模式

| 模式 | 說明 | 適用場景 |
|------|------|---------|
| **隔離流水 (ISOLATED)** | 每個獎金獨立追蹤流水進度 | 多紅利疊加場景 |
| **共用流水 (SHARED)** | 所有獎金共用同一流水池 | 簡化用戶體驗場景 |

### 典型場景決策

| 場景 | 匹配活動 | 決策策略 | 結果 |
|------|---------|---------|------|
| 新玩家首存 $100 | 首存 100%、首存 50%、VIP 銅牌 20% | MAX_REWARD | $100 獎金 (選最高) |
| VIP 金牌週末存款 | 週末 50%、VIP 金牌 30%、全站返水 1% | TYPE_EXCLUSIVE + STACK | 存款獎勵 $200 + 返水 $5 |
| 同時領取免費旋轉 | 每日簽到 10、新遊戲 50、補償 20 | STACK_ALL | 80 spins 分別追蹤 |
| 高風險玩家存款 | 首存 100%、週末 50% | RISK_REJECTION | 全部拒絕，人工審核 |

---

## 全局限制規則 (Global Limits)

| 限制項目 | 建議值 | 業務原因 |
|---------|-------|---------|
| 單玩家最大活躍獎金數 | 5 | 避免複雜度過高 |
| 獎金餘額上限 | $10,000 | 控制風險敞口 |
| 每日領取次數上限 | 3 | 防止套利行為 |
| 返水疊加上限 | 5% | 控制成本 |

---

## 運營優化建議

### 規則設計原則

1. **簡單透明**: 衝突規則不超過 3 層，用戶應在 5 秒內理解獎勵原因
2. **明確說明**: 被拒絕的活動需告知原因（如「您已選擇更高獎勵的活動」）
3. **活動條款**: 在活動頁面明確列出互斥規則

### 監控指標

| 指標 | 警報閾值 | 可能原因 |
|------|---------|---------|
| 活動切換頻率 | 單玩家 >5 次/小時 | 疑似套利測試 |
| 衝突規則投訴率 | >2% | 規則說明不清晰 |
| 流水完成率 | <30% | 門檻過高或權重不合理 |
| 獎金成本比 | >15% 營收 | 疊加規則過於寬鬆 |

### A/B 測試建議

| 測試項目 | 測試組 | 觀察指標 |
|---------|-------|---------|
| 衝突策略效果 | MAX_REWARD vs PRIORITY | 玩家滿意度、投訴率 |
| 流水追蹤模式 | ISOLATED vs SHARED | 流水完成率、用戶理解度 |
| 遊戲權重調整 | 百家樂 10% vs 15% | 遊戲分佈、獎金成本 |

---

## 每日對帳與偏差監控

### 對帳要求

| 項目 | 說明 |
|------|------|
| 執行時間 | 每日凌晨 03:00 (結算完成後) |
| 比對對象 | 財務系統流水 vs 活動系統流水 |
| 偏差容忍 | ≤0.01% |
| 警報通知 | Slack/Email 至財務與風控團隊 |

### 常見偏差原因

| 原因 | 解決方案 |
|------|---------|
| 時區差異 | 統一使用 UTC 時間 |
| 重複計算 | 注單去重檢查 |
| 權重配置不一致 | 使用集中配置服務 |

---

## 相關文檔

### 業務需求
- [活動中心業務需求索引](README.md)
- [風控策略總覽](../05_Risk_Compliance/Risk_Strategy_Overview.md)

### 技術實現

→ **[獎金計算引擎 - 技術架構](../../architecture/04_Activity_Engine/Bonus_Calculation_Engine.md)** - 獎金計算規則引擎、流水要求追蹤演算法、多幣種處理、遊戲權重矩陣、即時計算優化策略

---

**文檔版本**: 1.0.0
**創建日期**: 2026-02-08
**維護團隊**: Product Team
