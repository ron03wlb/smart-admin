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
- ✅ **異步提案生成**（高風險訂單生成提案，24小時內人工審核）
- ✅ **標記異常**（中風險訂單標記，不阻斷投注）
- ❌ 自動攔截 - **業界標準建議：不實時阻斷投注，避免誤判導致玩家流失**
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
    - 對正常玩家：零感知（投注不受影響，延遲忽略不計）
    - 對異常玩家：延遲摩擦（生成提案，取款時才觸發審核）
    - 對運營人員：人工審核（24小時SLA，確保準確性）
    - 對VIP玩家：無豁免（符合MGA合規要求，需EDD）
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
玩家下注 → ✅ 接受下注（不阻斷） → Kafka (bet.placed)
           ↓
     Flink消費者（流處理）@Async
           ↓
     並行處理4個維度:
     ├─ 設備指紋檢測（多帳號關聯）
     ├─ 行為模式分析（下注金額/頻率/時機）
     ├─ IP地理位置檢測（異常位置登錄）
     └─ 遊戲類型規則（體育博彩/老虎機/真人荷官/電競）
           ↓
     風險評分聚合（0-100分）
           ↓
     規則引擎判定（LiteFlow）
           ↓
     ├─ 評分 ≥ 80: 生成高風險提案 → Kafka (risk.proposals) → 人工審核（2h SLA）
     ├─ 評分 50-79: 生成中風險提案 → Kafka (risk.proposals) → 人工審核（24h SLA）
     └─ 評分 < 50: 標記正常 → 不生成提案
           ↓
     玩家繼續投注（體驗流暢）
           ↓
     玩家發起取款 → 關聯「自上次取款以來」的所有風控提案 → 人工決策 → 批准/拒絕取款
```

### 3.2 SmartAdmin分層設計（異步提案模式）

```java
// Controller層：投注接受，風控異步處理
@PostMapping("/bet/place")
public ResponseDTO<BetResultVO> placeBet(@RequestBody @Valid BetForm form) {
    // ✅ 立即接受投注，不等待風控結果
    BetResultVO result = betService.placeBet(form);

    // 異步觸發風控檢測
    applicationEventPublisher.publishEvent(new BetPlacedEvent(result.getBetId(), form));

    return ResponseDTO.ok(result);
}

// Service層：協調多個檢測維度（異步執行）
@Async("riskExecutor")
@EventListener
public void onBetPlaced(BetPlacedEvent event) {
    // 1. 設備指紋檢測
    DeviceFingerprintScore deviceScore = deviceManager.checkFingerprint(event.getForm());

    // 2. 行為模式分析
    BehaviorScore behaviorScore = behaviorManager.analyzeBehavior(event.getForm());

    // 3. IP地理位置檢測
    IpLocationScore ipScore = ipManager.checkLocation(event.getForm());

    // 4. 遊戲類型規則檢測
    GameTypeScore gameScore = gameTypeManager.checkGameRule(event.getForm());

    // 5. 聚合風險評分
    int totalScore = aggregateScore(deviceScore, behaviorScore, ipScore, gameScore);

    // 6. 規則引擎判定（LiteFlow）
    if (totalScore >= 50) {
        // 生成風控提案（不阻斷投注）
        RiskProposal proposal = createProposal(event, totalScore);
        riskProposalManager.saveAndNotify(proposal);
    }
}

// Manager層：提案持久化 + 人工審核通知
@Transactional(rollbackFor = Throwable.class)
public void saveAndNotify(RiskProposal proposal) {
    // 保存風控提案（審計）
    RiskProposalEntity entity = SmartBeanUtil.copy(proposal, RiskProposalEntity.class);
    entity.setStatus(ProposalStatus.PENDING_REVIEW);
    entity.setCreatedAt(LocalDateTime.now());
    riskProposalDao.insert(entity);

    // 發送人工審核通知（Kafka）
    kafkaTemplate.send("risk.proposals", entity);

    // 發送告警（高風險提案 2h SLA，中風險 24h SLA）
    if (proposal.getRiskScore() >= 80) {
        alertService.sendUrgentAlert(proposal, Duration.ofHours(2));
    } else {
        alertService.sendStandardAlert(proposal, Duration.ofHours(24));
    }
}

// 取款時關聯風控提案
public List<RiskProposal> getRelatedProposals(Long playerId, WithdrawalRequest request) {
    LocalDateTime lastWithdrawalTime = withdrawalDao.getLastWithdrawalTime(playerId);

    // 首次取款：回溯 30 天或註冊以來
    if (lastWithdrawalTime == null) {
        LocalDateTime registrationTime = playerDao.getRegistrationTime(playerId);
        lastWithdrawalTime = registrationTime.isAfter(LocalDateTime.now().minusDays(30))
                ? registrationTime
                : LocalDateTime.now().minusDays(30);
    }

    return riskProposalDao.findPendingProposals(playerId, lastWithdrawalTime, LocalDateTime.now());
}
```

### 3.3 依賴的Foundation模組

- [x] **foundation.cache** (Redis緩存)
  - 緩存玩家30天歷史行為（設備ID、IP、下注模式）

- [x] **foundation.mq** (Kafka消息隊列)
  - Topic: `bet.placed`（下注事件）
  - Topic: `risk.proposals`（風控提案事件，替代 risk.blocked）

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
1. **多維度綜合評分**：不依賴單一指標（設備指紋、行為模式、IP、遊戲類型）
2. **閾值可調**：根據實際誤判率動態調整（LiteFlow熱部署）
3. ~~**白名單機制**：VIP玩家降低敏感度~~ ❌ **違反MGA合規要求**
4. **VIP增強盡職調查（EDD）**：累計存款 > €2,000 觸發EDD（MGA要求）
5. **申訴流程**：玩家可提交申訴，人工複核

---

### 4.2 性能風險 🔴 高

#### 風險描述
- 10,000 TPS高併發下，Flink計算延遲超標

#### 緩解措施
1. **異步處理**：風控檢測完全異步（@Async），投注立即響應，不阻塞下注流程
2. **並行計算**：Flink並行度設置為32，支持多維度並行檢測
3. **熱數據緩存**：設備指紋、IP黑名單、玩家歷史行為預加載到Redis
4. **降級策略**：Flink異常時，降級到簡單規則檢測（僅標記，不生成提案）
5. **取款時統一審核**：所有風控提案在取款時集中處理，避免投注流程性能損耗

---

### 4.3 合規風險 🟡 中

#### 風險描述
- 風控決策需要完整審計日誌（監管要求）

#### 緩解措施
1. **完整記錄**：每次風控決策記錄評分明細
2. **不可刪除**：審計日誌邏輯刪除，保留7年
3. **可追溯**：支持按玩家ID/時間查詢決策歷史
4. **VIP合規**：VIP玩家無豁免，符合MGA EDD要求

---

### 4.4 取款風控關聯機制 🟢 新增

#### 功能描述
- 玩家發起取款時，系統自動抓取「自上次取款以來」投注時段內的所有風控提案
- 所有待審核提案必須完成人工決策後，才能批准取款

#### 時間範圍邏輯
```java
// 正常用戶：自上次取款時間 → 當前取款請求時間
LocalDateTime lastWithdrawalTime = withdrawalDao.getLastWithdrawalTime(playerId);

// 首次取款用戶：回溯 30 天或註冊以來（取較短時間）
if (lastWithdrawalTime == null) {
    LocalDateTime registrationTime = playerDao.getRegistrationTime(playerId);
    lastWithdrawalTime = registrationTime.isAfter(LocalDateTime.now().minusDays(30))
            ? registrationTime
            : LocalDateTime.now().minusDays(30);
}

List<RiskProposal> proposals = riskProposalDao.findPendingProposals(
    playerId,
    lastWithdrawalTime,
    LocalDateTime.now()
);
```

#### 業界標準對比

| 維度 | 舊設計（實時阻斷） | 新設計（異步提案） | 業界標準 |
|------|------------------|------------------|---------|
| 投注處理 | 高風險直接拒絕 | ✅ 接受投注，異步檢測 | Flag模式（標記不阻斷） |
| 玩家體驗 | 誤判導致客訴流失 | ✅ 流暢，零感知 | 零摩擦體驗 |
| VIP處理 | 白名單降低敏感度 | ✅ 無豁免，需EDD | MGA合規要求 |
| 取款審核 | 僅審核當次取款 | ✅ 關聯投注時段所有提案 | 業界最佳實踐 |
| 人工審核SLA | 無明確SLA | ✅ 24小時標準，2小時高風險 | 業界標準 |

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
- [ ] 套利檢測準確率 > 95%
- [ ] 誤判率 < 1%（人工審核確保準確性）
- [ ] **投注流程不受影響**（異步風控，投注延遲 < 50ms）
- [ ] **取款時成功關聯所有相關風控單**（自上次取款以來）
- [ ] **VIP 玩家觸發 EDD**（累計存款 > €2,000）

#### 性能驗收
- [ ] 投注響應延遲 < 50ms（P95，風控異步處理）
- [ ] 風控檢測延遲 < 200ms（P95，異步執行）
- [ ] 吞吐量 > 10,000 TPS
- [ ] Flink消費延遲 < 500ms
- [ ] 取款時風控單查詢延遲 < 200ms（P95）

#### 質量驗收
- [ ] 完整審計日誌（保留7年）
- [ ] ArchitectureTest通過
- [ ] SonarQube無critical問題
- [ ] **所有遊戲類型規則可獨立配置**（體育博彩/老虎機/真人荷官/電競）

#### 合規驗收
- [ ] **MGA合規**：VIP玩家需要EDD，無白名單豁免
- [ ] **人工審核SLA**：24小時標準，2小時高風險
- [ ] **風控提案狀態機完整**：PENDING → FLAGGED → PENDING_REVIEW → APPROVED/REJECTED → EXECUTED/CLOSED

---

## 參考文檔

### 內部文檔
- [ADR 012: 異步風控提案系統](../../../docs/iGame/architecture-decisions/012-async-risk-proposal-system.md)
- [P1-07: 取款風控關聯技術規格](../../../docs/iGame/technical-specs/P1-important/07-withdrawal-risk-correlation.md)
- [Flink流處理最佳實踐](../../../docs/iGame/architecture-decisions/007-flink-real-time-stream-processing.md)
- [LiteFlow規則引擎](../../../docs/iGame/architecture-decisions/011-liteflow-migration.md)

### 業界標準來源
- [CrossClassify – AI‑Powered iGaming Fraud Detection](https://www.crossclassify.com/solutions/iGaming/)
- [Transaction Monitoring in iGaming](https://seon.io/resources/transaction-monitoring-in-igaming/)
- [Malta Gaming Authority Player Protection](https://www.mga.org.mt/licensee-hub/compliance/player-protection/)
- [Enhanced Due Diligence For High Risk Customers](https://financialcrimeacademy.org/enhanced-due-diligence-for-high-risk-customers/)
- [Analysis of Casino Online Gambling Data](https://www.researchgate.net/publication/228225597)

---

**文檔版本**: 2.0.0 (異步提案模式)
**最後更新**: 2026-02-03
**變更摘要**:
- ✅ 移除實時阻斷投注邏輯
- ✅ 新增異步提案生成機制
- ✅ 移除VIP白名單，新增VIP EDD要求
- ✅ 新增取款時風控關聯機制
- ✅ 新增多維度規則配置（遊戲類型）

**下一步行動**：
將此需求分析報告傳遞給 java-architect，開始實施異步風控提案系統。
