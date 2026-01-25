# 實時風控引擎需求分析報告（示例）

**文檔元數據**
- 產品經理：igame-pm-analyst
- 創建日期：2026-01-23
- 優先級：P1重要
- 預估工作量：10 人天
- 風險等級：🔴 高

---

## 1. 需求澄清問題

> **原始需求**："需要做一個風控功能"

### 經過提問澄清後的完整需求

經過與用戶互動，確認了以下細節：

#### 1️⃣ 風控目標（已選擇）
- ✅ 套利行為（延遲套利、打水）
- ✅ 多帳號關聯（同一人註冊多個帳號）
- ✅ 異常提款（大額提款、頻繁提款）
- ❌ 洗錢行為（AML）- 由KYC模組負責
- ❌ 獎金濫用 - 由優惠引擎負責

#### 2️⃣ 觸發時機（已選擇）
- ✅ 下注時（即時風控，延遲<100ms）
- ✅ 提款時（最終審核）
- ❌ 註冊時 - 由KYC模組負責

#### 3️⃣ 處理動作（已選擇）
- ✅ 自動攔截（高風險訂單直接拒絕）
- ✅ 標記異常（中風險訂單人工審核）
- ❌ 限制額度 - 由額度管理模組負責

#### 4️⃣ 性能要求
- 預期併發量：10,000 TPS
- 可接受延遲：< 100ms（P95）
- 誤判容忍度：< 1%（false positive rate）

#### 5️⃣ 數據依賴
- ✅ 玩家歷史下注記錄（30天）
- ✅ 設備指紋（Device Fingerprint）
- ✅ IP地址與地理位置
- ❌ 第三方黑名單數據 - 暫不集成

---

## 2. Ultrathink深度分析

### 第一性原理拆解
```
第一層_表象層:
  需求描述: "實時風控引擎，攔截套利和異常提款"

第二層_交易層:
  風險流動: 套利者試圖利用系統漏洞獲取無風險利潤
  資金流動: 異常提款可能導致平台資金損失
  決策流動: 風控引擎需在毫秒級內做出攔截決策

第三層_第一性原理層:
  Trust（信任）:
    - 攔截準確性：誤判<1%，建立商戶信任
    - 可追溯性：完整記錄風控決策依據
    - 公平性：規則透明，玩家可申訴

  Velocity（速度）:
    - 即時決策：<100ms響應，不影響用戶體驗
    - 高吞吐：支持10,000 TPS併發下注檢測

  Friction（摩擦）:
    - 對正常玩家：零感知（延遲忽略不計）
    - 對異常玩家：自動攔截（無需人工介入）
    - 對運營人員：自動化審核（減少70%人工）
```

### 偽需求識別
```
❌ 偽需求: "所有風險都需要即時檢測"
   → 反向: 會導致系統複雜度爆炸，性能無法保證
   → 真實需求: 僅高頻場景（下注/提款）需即時，其餘可離線批處理

❌ 偽需求: "100%攔截所有套利"
   → 反向: 會導致大量誤判，正常玩家流失
   → 真實需求: 達到95%攔截率，誤判率<1%的平衡

✅ 真實需求確認: 實時檢測高頻風險，離線分析歷史模式
```

---

## 3. 技術方案摘要

### 3.1 架構設計

```
實時風控引擎 = Flink流處理 + 規則引擎 + 行為分析

數據流向:
玩家下注 → Kafka (bet.placed)
           ↓
     Flink消費者（流處理）
           ↓
     並行處理3個維度:
     ├─ 設備指紋檢測（多帳號關聯）
     ├─ 行為模式分析（下注金額/頻率/時機）
     └─ IP地理位置檢測（異常位置登錄）
           ↓
     風險評分聚合（0-100分）
           ↓
     規則引擎判定（LiteFlow）
           ↓
     ├─ 評分 > 80: 自動攔截 → 拒絕下注
     ├─ 評分 50-80: 人工審核 → 標記異常
     └─ 評分 < 50: 放行 → 正常處理
```

### 3.2 SmartAdmin分層設計

```java
// Controller層
@PostMapping("/risk/check")
public ResponseDTO<RiskCheckResultVO> checkRisk(@RequestBody @Valid RiskCheckForm form) {
    RiskCheckResultVO result = riskService.checkRisk(form);
    return ResponseDTO.ok(result);
}

// Service層：協調多個檢測維度
public RiskCheckResultVO checkRisk(RiskCheckForm form) {
    // 1. 設備指紋檢測
    DeviceFingerprintScore deviceScore = deviceManager.checkFingerprint(form);

    // 2. 行為模式分析
    BehaviorScore behaviorScore = behaviorManager.analyzeBehavior(form);

    // 3. IP地理位置檢測
    IpLocationScore ipScore = ipManager.checkLocation(form);

    // 4. 聚合風險評分
    int totalScore = aggregateScore(deviceScore, behaviorScore, ipScore);

    // 5. 規則引擎判定
    RiskDecision decision = ruleEngine.decide(totalScore);

    return buildResult(totalScore, decision);
}

// Manager層：具體檢測邏輯 + 數據持久化
@Transactional(rollbackFor = Exception.class)
public RiskDecision saveRiskRecord(RiskCheckForm form, int score) {
    // 保存風控記錄（審計）
    RiskRecordEntity record = new RiskRecordEntity();
    record.setPlayerId(form.getPlayerId());
    record.setRiskScore(score);
    record.setDecision(decision.getAction());
    riskRecordDao.insert(record);

    return decision;
}
```

### 3.3 依賴的Foundation模組

- [x] **foundation.cache** (Redis緩存)
  - 緩存玩家30天歷史行為（設備ID、IP、下注模式）

- [x] **foundation.mq** (Kafka消息隊列)
  - Topic: `bet.placed`（下注事件）
  - Topic: `risk.blocked`（攔截事件）

- [x] **Flink 1.20.0** (流處理)
  - 實時計算風險評分

- [x] **LiteFlow 2.12.5** (規則引擎)
  - 可視化配置風控規則

---

## 4. 風險評估

### 4.1 誤判風險 🔴 高

#### 風險描述
- 正常玩家被誤判為套利者，導致客訴和流失

#### 緩解措施
1. **多維度綜合評分**：不依賴單一指標
2. **閾值可調**：根據實際誤判率動態調整
3. **白名單機制**：VIP玩家降低敏感度
4. **申訴流程**：玩家可提交申訴，人工複核

---

### 4.2 性能風險 🔴 高

#### 風險描述
- 10,000 TPS高併發下，Flink計算延遲超標

#### 緩解措施
1. **異步處理**：風控結果異步返回（不阻塞下注）
2. **並行計算**：Flink並行度設置為32
3. **熱數據緩存**：設備指紋、IP黑名單預加載
4. **降級策略**：Flink異常時，降級到簡單規則檢測

---

### 4.3 合規風險 🟡 中

#### 風險描述
- 風控決策需要完整審計日誌（監管要求）

#### 緩解措施
1. **完整記錄**：每次風控決策記錄評分明細
2. **不可刪除**：審計日誌邏輯刪除，保留7年
3. **可追溯**：支持按玩家ID/時間查詢決策歷史

---

## 5. 實施計劃（預估10人天）

### 階段1：Flink流處理開發（4天）
- [ ] T1: Kafka消費者開發
- [ ] T2: 設備指紋檢測邏輯
- [ ] T3: 行為模式分析邏輯
- [ ] T4: 風險評分聚合

### 階段2：規則引擎集成（2天）
- [ ] T5: LiteFlow規則配置
- [ ] T6: 規則熱加載機制

### 階段3：SmartAdmin接口開發（2天）
- [ ] T7: Manager/Service/Controller層實現
- [ ] T8: 審計日誌持久化

### 階段4：測試與優化（2天）
- [ ] T9: 性能壓測（10,000 TPS）
- [ ] T10: 誤判率測試（<1%）

---

## 6. 驗收標準

#### 功能驗收
- [x] 套利檢測準確率 > 95%
- [x] 誤判率 < 1%
- [x] 人工審核減少 70%

#### 性能驗收
- [x] 風控延遲 < 100ms（P95）
- [x] 吞吐量 > 10,000 TPS
- [x] Flink消費延遲 < 500ms

#### 質量驗收
- [x] 完整審計日誌
- [x] ArchitectureTest通過
- [x] SonarQube無critical問題

---

## 參考文檔

- [P1-06: 實時風控引擎](../../docs/iGame/technical-specs/P1-important/06-real-time-risk-engine.md)
- [Flink流處理最佳實踐](../../docs/iGame/architecture-decisions/007-flink-real-time-stream-processing.md)
- [LiteFlow規則引擎](../../docs/iGame/architecture-decisions/011-liteflow-migration.md)

---

**下一步行動**：
將此需求分析報告傳遞給 java-architect，開始Flink流處理開發。
