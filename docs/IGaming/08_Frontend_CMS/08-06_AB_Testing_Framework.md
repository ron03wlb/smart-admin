# 08-06 A/B 測試框架 (A/B Testing Framework)

> **版本**: 1.0.0
> **最後更新**: 2026-01-28
> **維護團隊**: Product Team & Frontend Team

---

## 📋 目錄

- [1. 系統概述](#1-系統概述-system-overview)
- [2. A/B 測試平台選型](#2-ab-測試平台選型-platform-selection)
- [3. 流量分配策略](#3-流量分配策略-traffic-allocation)
- [4. 指標跟蹤體系](#4-指標跟蹤體系-metrics-tracking)
- [5. 實驗管理流程](#5-實驗管理流程-experiment-management)
- [6. 統計顯著性檢驗](#6-統計顯著性檢驗-statistical-significance)
- [7. 相關文檔](#7-相關文檔)

---

## 1. 系統概述 (System Overview)

A/B 測試框架用於**數據驅動的產品優化**,通過對比不同版本(變體)的效果,科學地驗證產品假設,降低決策風險。

**核心功能**:
- **流量分配**: 基於 player_id hash 分桶,確保用戶體驗一致性
- **多變體測試**: 支持 A/B/C/D 多個變體同時測試
- **實時統計**: 實時計算轉化率、統計顯著性、置信區間
- **自動停止**: 達到統計顯著性或樣本量上限自動停止實驗
- **獲勝變體推全量**: 實驗結束後一鍵推送獲勝變體至全量用戶

---

## 2. A/B 測試平台選型 (Platform Selection)

### 2.1 平台對比

| 平台 | 類型 | 成本 | 易用性 | 功能完整度 | 推薦場景 |
|------|------|------|--------|-----------|---------|
| **自建系統** | 自研 | 開發成本高 | ⭐⭐ | ⭐⭐⭐⭐ | 完全控制、深度定制 |
| **Google Optimize** | 商業 (免費) | 免費 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | 小型團隊、快速驗證 |
| **Optimizely** | 商業 | $$$$ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | 企業級、功能全面 |
| **VWO** | 商業 | $$$ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | 易用性強、性價比高 |
| **GrowthBook** | 開源 | 免費 | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | 開源、自託管 ⭐ |

**推薦**: **GrowthBook** (開源、功能完整、可自託管)

---

### 2.2 自建系統架構

```
┌─────────────────────────────────────────────────────┐
│  Frontend (React/Vue)                                │
│  - 實驗配置 UI                                       │
│  - 實時數據看板                                      │
└────────────────┬────────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────────┐
│  AB Testing Service (Node.js / Go)                   │
│  - 流量分配算法                                      │
│  - 實驗配置管理                                      │
│  - 統計計算引擎                                      │
└────────────────┬────────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────────┐
│  Data Layer                                          │
│  - Redis (實驗配置緩存)                              │
│  - PostgreSQL (實驗定義、結果)                       │
│  - ClickHouse (事件流、指標聚合)                     │
└─────────────────────────────────────────────────────┘
```

---

## 3. 流量分配策略 (Traffic Allocation)

### 3.1 哈希分桶算法

**一致性哈希 (Consistent Hashing)**:
```javascript
function assignVariant(playerId, experimentId, variants) {
    // 1. 計算哈希值 (MurmurHash3)
    const hash = murmur3(`${experimentId}:${playerId}`);

    // 2. 取模映射到 [0, 100) 區間
    const bucket = hash % 100;

    // 3. 根據流量分配確定變體
    let cumulative = 0;
    for (const variant of variants) {
        cumulative += variant.trafficPercentage;
        if (bucket < cumulative) {
            return variant.name;
        }
    }

    return 'control';  // 默認對照組
}

// 範例
const variant = assignVariant(
    playerId = '12345',
    experimentId = 'homepage-redesign',
    variants = [
        { name: 'control', trafficPercentage: 50 },  // 對照組 50%
        { name: 'variant_a', trafficPercentage: 25 },  // 變體 A 25%
        { name: 'variant_b', trafficPercentage: 25 }   // 變體 B 25%
    ]
);
// Result: 'variant_a' (根據玩家 ID 穩定分配)
```

**優勢**:
- ✅ 同一玩家始終分配到同一變體 (體驗一致性)
- ✅ 流量分配均勻 (哈希函數分佈均勻)
- ✅ 無需存儲映射關係 (節省存儲)

---

### 3.2 流量分配策略

**策略 1: 漸進式灰度 (Progressive Rollout)**
```
Day 1: Control 95%, Variant A 5%
Day 3: Control 90%, Variant A 10%
Day 7: Control 80%, Variant A 20%
...
Day 30: Variant A 100% (全量推送)
```

**策略 2: 多變體均等分配**
```
Control: 25%
Variant A: 25%
Variant B: 25%
Variant C: 25%
```

**策略 3: 對照組 + 實驗組**
```
Control: 50%
Variant A: 50%
```

---

## 4. 指標跟蹤體系 (Metrics Tracking)

### 4.1 關鍵指標定義

**主要指標 (Primary Metrics)**:
- **轉化率 (Conversion Rate)**: 註冊轉首存、首存轉活躍
- **ARPU (Average Revenue Per User)**: 人均收入
- **留存率 (Retention Rate)**: D1/D7/D30 留存率

**次要指標 (Secondary Metrics)**:
- **點擊率 (CTR)**: CTA 按鈕點擊率
- **跳出率 (Bounce Rate)**: 單頁會話比例
- **平均會話時長 (Avg Session Duration)**

**防護指標 (Guardrail Metrics)**:
- **錯誤率 (Error Rate)**: 前端 JS 錯誤、API 錯誤
- **頁面加載時間 (Page Load Time)**: 性能退化檢測
- **用戶投訴率 (Complaint Rate)**

---

### 4.2 事件跟蹤設計

**事件埋點範例 (Google Analytics / Mixpanel)**:
```javascript
// 實驗曝光事件
analytics.track('Experiment Viewed', {
    experiment_id: 'homepage-redesign',
    variant: 'variant_a',
    player_id: '12345'
});

// 轉化事件
analytics.track('First Deposit Completed', {
    experiment_id: 'homepage-redesign',
    variant: 'variant_a',
    player_id: '12345',
    deposit_amount: 100
});
```

---

## 5. 實驗管理流程 (Experiment Management)

### 5.1 實驗生命週期

```
1. 假設提出 (Hypothesis)
   ↓
2. 實驗設計 (Design)
   - 定義變體
   - 選擇指標
   - 計算樣本量
   ↓
3. 開發實現 (Development)
   - 前端變體開發
   - 事件埋點
   ↓
4. 實驗啟動 (Launch)
   - 流量分配
   - 監控指標
   ↓
5. 數據收集 (Data Collection)
   - 等待統計顯著性
   - 防護指標監控
   ↓
6. 結果分析 (Analysis)
   - 統計檢驗
   - 商業決策
   ↓
7. 獲勝變體推全量 (Rollout)
   - 100% 流量
   - 下線對照組
```

---

### 5.2 實驗配置範例

```yaml
experiment:
  id: homepage-redesign
  name: "首頁重新設計 A/B 測試"
  status: running
  start_date: "2026-01-28T00:00:00Z"
  end_date: "2026-02-28T00:00:00Z"

  variants:
    - name: control
      description: "原始首頁"
      traffic: 50%

    - name: variant_a
      description: "新設計 (大圖 Banner)"
      traffic: 50%

  primary_metric:
    name: first_deposit_conversion_rate
    goal: maximize
    min_sample_size: 10000
    min_detectable_effect: 5%  # 最小可檢測效應 (MDE)

  secondary_metrics:
    - click_through_rate
    - bounce_rate

  guardrail_metrics:
    - error_rate
    - page_load_time
```

---

## 6. 統計顯著性檢驗 (Statistical Significance)

### 6.1 樣本量計算

**公式** (雙樣本 t 檢驗):
```
n = 2 × (Z_α/2 + Z_β)² × σ² / δ²

其中:
- Z_α/2: 顯著性水平 (α=0.05 → Z=1.96)
- Z_β: 統計功效 (β=0.8 → Z=0.84)
- σ: 標準差
- δ: 最小可檢測效應 (Minimum Detectable Effect)
```


---

### 6.2 統計檢驗方法


---

### 6.3 自動停止規則

**規則 1: 達到統計顯著性**
```
IF p_value < 0.05 AND sample_size >= min_sample_size
THEN stop_experiment()
```

**規則 2: 樣本量上限**
```
IF sample_size >= max_sample_size
THEN stop_experiment()
```

**規則 3: 防護指標觸發**
```
IF error_rate > baseline_error_rate * 1.5
THEN stop_experiment() AND rollback()
```

---

## 7. 相關文檔

### 前置知識
- [00-04 技術選型標準](../00_Concept_&_Analysis/00-04_Technology_Stack.md) - 前端技術棧、分析工具選型

### 核心依賴
- [01-03 玩家分群與標籤](../01_Player_Center/01-03_Player_Segmentation.md) - 基於分群的 A/B 測試
- [10-01 報表與 BI 架構](../10_Reporting_&_BI/10-01_Reporting_Architecture.md) - 實驗結果分析儀表板

### 延伸閱讀
- [08-01 前端佈局引擎](./08-01_Frontend_Layout_Engine.md) - 動態佈局與 A/B 測試集成
- [04-01 活動系統設計](../04_Activity_Center/04-01_Activity_System_Design.md) - 活動效果 A/B 測試

---

**文檔版本**: 1.0.0
**最後更新**: 2026-01-28
**維護團隊**: Product Team & Frontend Team
