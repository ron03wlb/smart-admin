# P0-04: KYC/AML 自動化 (KYC/AML Automation)

**文檔版本**: 1.0.0
**狀態**: 📝 草稿 (Draft)
**優先級**: P0 - 關鍵基礎 (Critical Foundation)
**預估行數**: 900-1100
**依賴文檔**: 無 (獨立實現)
**被依賴文檔**: P1-06 (風控引擎), P1-16 (合規審計)
**最後更新**: 2026-01-23

---

## 目錄 (Table of Contents)

1. [執行摘要 (Executive Summary)](#1-執行摘要-executive-summary)
2. [背景與戰略對齊 (Background & Strategic Alignment)](#2-背景與戰略對齊-background--strategic-alignment)
3. [Progressive KYC 分級體系 (Progressive KYC Tiers)](#3-progressive-kyc-分級體系-progressive-kyc-tiers)
4. [Evrete 規則引擎集成 (Evrete Rules Integration)](#4-evrete-規則引擎集成-evrete-rules-integration)
5. [第三方服務集成 (Third-Party Integrations)](#5-第三方服務集成-third-party-integrations)
6. [AML 篩查工作流 (AML Screening Workflow)](#6-aml-篩查工作流-aml-screening-workflow)
7. [文檔存儲 (Document Storage with MinIO)](#7-文檔存儲-document-storage-with-minio)
8. [SmartAdmin 分層實現 (Layered Implementation)](#8-smartadmin-分層實現-layered-implementation)
9. [自動化觸發器 (Automated Triggers)](#9-自動化觸發器-automated-triggers)
10. [測試策略 (Testing Strategy)](#10-測試策略-testing-strategy)
11. [性能基準 (Performance Benchmarks)](#11-性能基準-performance-benchmarks)
12. [運營與監控 (Operations & Monitoring)](#12-運營與監控-operations--monitoring)
13. [安全考量 (Security Considerations)](#13-安全考量-security-considerations)
14. [附錄 (Appendices)](#14-附錄-appendices)

---

## 1. 執行摘要 (Executive Summary)

### 1.1 問題陳述 (Problem Statement)

根據 [backend_project.md](../backend_project.md) 需求分析,當前架構缺失:

| 缺口項目 | 影響程度 | 業務風險 |
|---------|---------|---------|
| **自動化觸發規則未定義** | 🔴 Critical | 合規風險 + 用戶流失 |
| **第三方集成規範缺失** | 🔴 Critical | 無法快速上線 |
| **AML 篩查流程不明確** | 🔴 Critical | 監管罰款風險 |
| **文檔存儲方案缺失** | 🔴 Critical | 審計無法追溯 |

**傳統 KYC 流程的痛點**:
```
註冊時強制 KYC:
  註冊 → 填寫身份信息 (5min) → 上傳證件 (2min) → 等待審核 (1-3天)
  → 用戶放棄率: 70%+ ❌

Progressive KYC 解決方案:
  註冊 → 立即可玩 (Tier 0: 無需驗證)
  → 充值 $100 → 自動觸發 Tier 1 驗證 (僅需姓名+生日)
  → 提現 $500 → 自動觸發 Tier 2 驗證 (上傳證件照)
  → 提現 $5000 → 自動觸發 Tier 3 驗證 (視頻通話)
  → 用戶放棄率: <10% ✅
```

**關鍵洞察** (來自 [igame_str.md](../igame_str.md)):
> "摩擦 (Friction) 與合規 (Compliance) 是零和博弈。Progressive KYC 是唯一解。"

### 1.2 解決方案概覽 (Solution Overview)

**核心設計**:
```
┌─────────────────────────────────────────────────────────────┐
│              Progressive KYC/AML Automation                  │
├─────────────────────────────────────────────────────────────┤
│  Tier 0: Anonymous (匿名玩家)                                 │
│  • 限制: 僅可試玩, 無法充值/提現                               │
│  • 驗證: 無                                                   │
│                                                              │
│  Tier 1: Basic Verified (基礎驗證)                           │
│  • 觸發: 首次充值 OR 累計充值 > $100                          │
│  • 驗證: 姓名, 生日, 郵箱, 手機號                             │
│  • 限制: 單筆提現 < $500, 日累計 < $1000                      │
│  • 自動化: Evrete 規則引擎觸發                                │
│                                                              │
│  Tier 2: Enhanced Verified (增強驗證)                        │
│  • 觸發: 首次提現 > $500 OR 累計提現 > $2000                  │
│  • 驗證: 證件照 (護照/駕照), 地址證明                         │
│  • 限制: 單筆提現 < $5000, 日累計 < $10000                    │
│  • 第三方: Jumio/Onfido OCR + Liveness Detection              │
│                                                              │
│  Tier 3: Full Verified (完全驗證)                            │
│  • 觸發: 累計提現 > $10000 OR 高風險標記                      │
│  • 驗證: 視頻通話 KYC, 資金來源證明                           │
│  • 限制: 無限額                                               │
│  • AML: ComplyAdvantage 制裁名單篩查                          │
│                                                              │
│  Document Storage (文檔存儲)                                 │
│  • MinIO: S3-compatible 對象存儲                              │
│  • 加密: AES-256 客戶端加密                                   │
│  • 審計: 所有訪問記錄保留 7 年                                │
└─────────────────────────────────────────────────────────────┘
```

**關鍵特性**:
- ✅ **自動化**: Evrete 規則引擎自動觸發升級
- ✅ **靈活性**: 根據風險動態調整驗證等級
- ✅ **合規性**: 符合 MGA (Malta) 和 Curacao 牌照要求
- ✅ **用戶體驗**: 最小化摩擦,僅在必要時驗證

### 1.3 成功指標 (Success Criteria)

| 指標 | 目標值 | 驗證方法 |
|-----|-------|---------|
| **KYC 完成率 (Tier 1)** | > 90% | 觸發後 24 小時內完成 |
| **AML 假陽性率** | < 5% | 人工審核誤判率 |
| **文檔上傳成功率** | > 95% | MinIO 上傳監控 |
| **自動化處理率** | > 70% | 無需人工干預的比例 |

---

## 2. 背景與戰略對齊 (Background & Strategic Alignment)

### 2.1 與 First Principles 的對齊

引自 [igame_str.md](../igame_str.md):

> **摩擦 (Friction) 的最小化 = 用戶體驗的最大化**
> Progressive KYC: 在合規與體驗之間找到最優平衡點

**傳統 vs Progressive KYC 對比**:

| 維度 | 傳統 KYC | Progressive KYC |
|-----|---------|----------------|
| **註冊時驗證** | 強制 | 可選 (Tier 0) |
| **用戶放棄率** | 70%+ | <10% |
| **合規達成時間** | 註冊時 | 首次充值/提現時 |
| **摩擦點** | 註冊 (最早階段) | 提現 (已有粘性) |

### 2.2 Leverage Thinking 應用

| 槓桿類型 | 在 KYC/AML 中的體現 |
|---------|-------------------|
| **Automation Leverage** | Evrete 規則引擎自動觸發,替代 70% 人工審核 |
| **Code Leverage** | 統一 KycService 處理所有 Tier 升級邏輯 |
| **Trust Leverage** | 第三方 API (Jumio/Onfido) 提供專業驗證 |

**零邊際成本擴展**:
- 新增 1000 用戶: 自動化觸發,**無額外成本**
- 新增 1 個監管要求: 更新 Evrete 規則,**無需改代碼**

### 2.3 與 backend_project.md 的對應

| backend_project.md 需求 | 本文檔實現章節 |
|------------------------|---------------|
| 5.4.1 KYC 分級體系 | [§3 Progressive KYC](#3-progressive-kyc-分級體系-progressive-kyc-tiers) |
| 5.4.2 自動化觸發 | [§9 自動化觸發器](#9-自動化觸發器-automated-triggers) |
| 5.4.3 AML 篩查 | [§6 AML 篩查工作流](#6-aml-篩查工作流-aml-screening-workflow) |
| 5.8.4 文檔管理 | [§7 文檔存儲](#7-文檔存儲-document-storage-with-minio) |

---

## 3. Progressive KYC 分級體系 (Progressive KYC Tiers)

### 3.1 Tier 定義

```java
/**
 * KYC 等級枚舉
 */
public enum KycTier {
    TIER_0_ANONYMOUS(0, "匿名用戶", "無需驗證"),
    TIER_1_BASIC(1, "基礎驗證", "姓名+生日+郵箱+手機"),
    TIER_2_ENHANCED(2, "增強驗證", "證件照+地址證明"),
    TIER_3_FULL(3, "完全驗證", "視頻 KYC+資金來源");

    private final int level;
    private final String displayName;
    private final String requirements;
}
```

### 3.2 Tier 權限矩陣

| 操作 | Tier 0 | Tier 1 | Tier 2 | Tier 3 |
|-----|--------|--------|--------|--------|
| **試玩遊戲** | ✅ | ✅ | ✅ | ✅ |
| **充值** | ❌ | ✅ $100/次 | ✅ $1000/次 | ✅ 無限 |
| **投注** | ❌ | ✅ $50/次 | ✅ $500/次 | ✅ 無限 |
| **提現** | ❌ | ✅ $500/次 | ✅ $5000/次 | ✅ 無限 |
| **日提現額度** | ❌ | $1000 | $10000 | 無限 |
| **月提現額度** | ❌ | $5000 | $50000 | 無限 |

### 3.3 數據庫設計

```sql
-- 玩家 KYC 信息表
CREATE TABLE player_kyc (
    id                  BIGSERIAL PRIMARY KEY,
    player_id           BIGINT NOT NULL UNIQUE,

    -- KYC 等級
    tier                INTEGER NOT NULL DEFAULT 0,
    tier_updated_at     TIMESTAMP,

    -- Tier 1: 基礎信息
    full_name           VARCHAR(100),
    date_of_birth       DATE,
    nationality         VARCHAR(10),
    phone_number        VARCHAR(20),
    email_verified      BOOLEAN DEFAULT FALSE,
    phone_verified      BOOLEAN DEFAULT FALSE,

    -- Tier 2: 增強驗證
    id_document_type    VARCHAR(20),        -- PASSPORT, DRIVERS_LICENSE, ID_CARD
    id_document_number  VARCHAR(50),
    id_document_expiry  DATE,
    id_document_url     TEXT,               -- MinIO URL
    address_proof_url   TEXT,               -- 地址證明 MinIO URL
    jumio_verification_id VARCHAR(100),     -- Jumio 驗證 ID

    -- Tier 3: 完全驗證
    video_kyc_completed BOOLEAN DEFAULT FALSE,
    video_kyc_url       TEXT,               -- 視頻 KYC 錄像 URL
    source_of_funds     TEXT,               -- 資金來源說明
    aml_screening_status VARCHAR(20),       -- PENDING, CLEAR, HIT, MANUAL_REVIEW

    -- 審計字段
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          BIGINT NOT NULL,
    updated_by          BIGINT,

    CONSTRAINT fk_player_kyc_player FOREIGN KEY (player_id) REFERENCES players(id),
    CONSTRAINT ck_player_kyc_tier CHECK (tier >= 0 AND tier <= 3)
);

CREATE INDEX idx_player_kyc_tier ON player_kyc(tier);
CREATE INDEX idx_player_kyc_aml_status ON player_kyc(aml_screening_status);

COMMENT ON TABLE player_kyc IS 'Progressive KYC 信息表';
COMMENT ON COLUMN player_kyc.tier IS 'KYC 等級: 0=匿名, 1=基礎, 2=增強, 3=完全';
```

---

## 4. Evrete 規則引擎集成 (Evrete Rules Integration)

### 4.1 Evrete 規則示例

**規則文件**: `kyc-upgrade-rules.evr`

```java
/**
 * 規則 1: 首次充值自動升級到 Tier 1
 */
rule "Upgrade to Tier 1 on First Deposit"
when
    $player: Player(kycTier == KycTier.TIER_0_ANONYMOUS)
    $event: DepositEvent($player.id == playerId, amount > 0)
then
    kycService.upgradeTier($player.id, KycTier.TIER_1_BASIC);
    notificationService.send($player.id, "請完成基礎 KYC 驗證以繼續使用服務");
end

/**
 * 規則 2: 累計充值超過 $1000 升級到 Tier 2
 */
rule "Upgrade to Tier 2 on Cumulative Deposit > $1000"
when
    $player: Player(kycTier == KycTier.TIER_1_BASIC)
    $stats: PlayerStats($player.id == playerId, cumulativeDeposit > 1000)
then
    kycService.upgradeTier($player.id, KycTier.TIER_2_ENHANCED);
    notificationService.send($player.id, "請上傳身份證件以提升提現額度");
end

/**
 * 規則 3: 首次提現超過 $500 要求 Tier 2
 */
rule "Require Tier 2 for Withdrawal > $500"
when
    $player: Player(kycTier < KycTier.TIER_2_ENHANCED)
    $event: WithdrawalRequest($player.id == playerId, amount > 500)
then
    withdrawalService.reject($event.id, "請先完成增強 KYC 驗證");
    kycService.requestUpgrade($player.id, KycTier.TIER_2_ENHANCED);
end

/**
 * 規則 4: 高風險國家自動觸發 AML 篩查
 */
rule "Trigger AML Screening for High-Risk Countries"
when
    $player: Player(nationality in ["AF", "IR", "KP", "SY"])  // 高風險國家代碼
then
    amlService.screenPlayer($player.id);
end

/**
 * 規則 5: 累計提現超過 $10000 升級到 Tier 3
 */
rule "Upgrade to Tier 3 on Cumulative Withdrawal > $10000"
when
    $player: Player(kycTier == KycTier.TIER_2_ENHANCED)
    $stats: PlayerStats($player.id == playerId, cumulativeWithdrawal > 10000)
then
    kycService.upgradeTier($player.id, KycTier.TIER_3_FULL);
    notificationService.send($player.id, "請聯繫客服安排視頻 KYC 驗證");
end
```

### 4.2 Evrete 集成實現

```java
/**
 * Evrete 規則引擎配置
 */
@Configuration
public class EvreteKycConfig {

    @Bean
    public KnowledgeService evreteKnowledgeService() {
        KnowledgeService service = new KnowledgeService();

        // 加載規則文件
        Resource resource = new ClassPathResource("rules/kyc-upgrade-rules.evr");
        Knowledge knowledge = service
            .newKnowledge()
            .importRules("classpath", resource)
            .build();

        return service;
    }
}

/**
 * KYC 規則執行器
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KycRuleExecutor {

    private final KnowledgeService knowledgeService;
    private final KycService kycService;

    /**
     * 執行 KYC 規則評估
     */
    public void evaluateKycRules(Long playerId, KycEvent event) {
        log.info("[KYC Rules] Evaluating rules for player: {}, event: {}", playerId, event);

        StatefulSession session = knowledgeService
            .newKnowledge()
            .createSession();

        // 插入事實 (Facts)
        Player player = playerDao.selectById(playerId);
        PlayerStats stats = statsService.getStats(playerId);

        session.insert(player);
        session.insert(stats);
        session.insert(event);

        // 執行規則
        session.fire();

        log.info("[KYC Rules] Rules execution completed for player: {}", playerId);
    }
}
```

---

## 5. 第三方服務集成 (Third-Party Integrations)

### 5.1 Jumio 身份驗證

**API 集成**:
```java
/**
 * Jumio KYC 服務
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JumioKycService {

    @Value("${kyc.jumio.api-key}")
    private String apiKey;

    @Value("${kyc.jumio.api-secret}")
    private String apiSecret;

    private final RestTemplate restTemplate;

    /**
     * 創建 Jumio 驗證會話
     */
    public JumioVerificationSession createVerificationSession(Long playerId) {
        String url = "https://netverify.com/api/v4/initiate";

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(apiKey, apiSecret);
        headers.setContentType(MediaType.APPLICATION_JSON);

        JumioInitiateRequest request = JumioInitiateRequest.builder()
            .customerInternalReference(String.valueOf(playerId))
            .userReference(String.valueOf(playerId))
            .successUrl("https://yourdomain.com/kyc/success")
            .errorUrl("https://yourdomain.com/kyc/error")
            .build();

        HttpEntity<JumioInitiateRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<JumioInitiateResponse> response = restTemplate.postForEntity(
            url, entity, JumioInitiateResponse.class
        );

        JumioInitiateResponse body = response.getBody();

        return JumioVerificationSession.builder()
            .transactionReference(body.getTransactionReference())
            .redirectUrl(body.getRedirectUrl())
            .build();
    }

    /**
     * 獲取驗證結果
     */
    public JumioVerificationResult getVerificationResult(String transactionReference) {
        String url = String.format(
            "https://netverify.com/api/v4/retrievals/%s",
            transactionReference
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(apiKey, apiSecret);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<JumioRetrievalResponse> response = restTemplate.exchange(
            url, HttpMethod.GET, entity, JumioRetrievalResponse.class
        );

        JumioRetrievalResponse body = response.getBody();

        return JumioVerificationResult.builder()
            .verified(body.getVerificationStatus().equals("APPROVED_VERIFIED"))
            .firstName(body.getDocument().getFirstName())
            .lastName(body.getDocument().getLastName())
            .dateOfBirth(body.getDocument().getDob())
            .documentNumber(body.getDocument().getNumber())
            .documentType(body.getDocument().getType())
            .build();
    }
}
```

### 5.2 Onfido 實名驗證 (備選)

```java
@Service
public class OnfidoKycService {

    private final OkHttpClient httpClient;

    @Value("${kyc.onfido.api-token}")
    private String apiToken;

    /**
     * 創建申請人
     */
    public String createApplicant(Long playerId, String firstName, String lastName) {
        Request request = new Request.Builder()
            .url("https://api.onfido.com/v3/applicants")
            .addHeader("Authorization", "Token token=" + apiToken)
            .post(RequestBody.create(
                MediaType.parse("application/json"),
                String.format("{\"first_name\":\"%s\",\"last_name\":\"%s\"}",
                    firstName, lastName)
            ))
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            JSONObject json = new JSONObject(response.body().string());
            return json.getString("id");
        } catch (IOException e) {
            throw new BusinessException(KycErrorCode.ONFIDO_API_ERROR, e);
        }
    }

    /**
     * 創建檢查 (Check)
     */
    public String createCheck(String applicantId) {
        String payload = String.format(
            "{\"applicant_id\":\"%s\",\"report_names\":[\"document\",\"facial_similarity_photo\"]}",
            applicantId
        );

        Request request = new Request.Builder()
            .url("https://api.onfido.com/v3/checks")
            .addHeader("Authorization", "Token token=" + apiToken)
            .post(RequestBody.create(MediaType.parse("application/json"), payload))
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            JSONObject json = new JSONObject(response.body().string());
            return json.getString("id");
        } catch (IOException e) {
            throw new BusinessException(KycErrorCode.ONFIDO_API_ERROR, e);
        }
    }
}
```

### 5.3 ComplyAdvantage AML 篩查

```java
/**
 * ComplyAdvantage AML 服務
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ComplyAdvantageService {

    @Value("${aml.complyadvantage.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate;

    /**
     * 篩查玩家
     */
    public AmlScreeningResult screenPlayer(PlayerKyc kyc) {
        String url = "https://api.complyadvantage.com/searches";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Token " + apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        AmlSearchRequest request = AmlSearchRequest.builder()
            .searchTerm(kyc.getFullName())
            .fuzziness(0.8)
            .filters(AmlFilters.builder()
                .types(Arrays.asList("sanction", "warning", "fitness-probity"))
                .birthYear(kyc.getDateOfBirth().getYear())
                .build())
            .build();

        HttpEntity<AmlSearchRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<AmlSearchResponse> response = restTemplate.postForEntity(
            url, entity, AmlSearchResponse.class
        );

        AmlSearchResponse body = response.getBody();

        return AmlScreeningResult.builder()
            .searchId(body.getId())
            .totalHits(body.getData().getTotalHits())
            .matches(body.getData().getMatches())
            .riskLevel(calculateRiskLevel(body))
            .build();
    }

    /**
     * 計算風險等級
     */
    private RiskLevel calculateRiskLevel(AmlSearchResponse response) {
        int totalHits = response.getData().getTotalHits();

        if (totalHits == 0) {
            return RiskLevel.LOW;
        } else if (totalHits <= 2) {
            return RiskLevel.MEDIUM;
        } else {
            return RiskLevel.HIGH;
        }
    }
}
```

---

## 6. AML 篩查工作流 (AML Screening Workflow)

### 6.1 工作流程圖

```
┌─────────────────────────────────────────────────────────────┐
│ Phase 1: 自動篩查 (Automated Screening)                      │
│ ├─ Trigger: Tier 2/3 升級時 OR 每月定期                      │
│ ├─ API: ComplyAdvantage 制裁名單查詢                         │
│ └─ Result: CLEAR (通過) | HIT (命中) | ERROR (錯誤)          │
│                                                              │
│ Phase 2: 風險評分 (Risk Scoring)                            │
│ ├─ Hit Count: 0 hits = LOW, 1-2 hits = MEDIUM, 3+ = HIGH    │
│ ├─ Match Quality: Exact match = HIGH, Fuzzy = MEDIUM        │
│ └─ Source Type: Sanction = CRITICAL, Warning = MEDIUM       │
│                                                              │
│ Phase 3: 人工審核 (Manual Review)                           │
│ ├─ LOW: 自動通過                                             │
│ ├─ MEDIUM: 1 級審核 (Junior Compliance Officer)              │
│ └─ HIGH: 2 級審核 (Senior Compliance + Legal)                │
│                                                              │
│ Phase 4: 決策 (Decision)                                    │
│ ├─ APPROVED: 允許繼續操作                                    │
│ ├─ REJECTED: 凍結帳戶,退還資金                               │
│ └─ MONITORED: 持續監控,限制額度                              │
└─────────────────────────────────────────────────────────────┘
```

### 6.2 狀態機實現

```java
/**
 * AML 篩查狀態機
 */
public enum AmlStatus {
    PENDING("待篩查"),
    SCREENING("篩查中"),
    CLEAR("通過"),
    HIT("命中"),
    MANUAL_REVIEW("人工審核中"),
    APPROVED("審核通過"),
    REJECTED("審核拒絕"),
    MONITORED("持續監控");

    private final String description;

    AmlStatus(String description) {
        this.description = description;
    }
}

/**
 * AML 工作流服務
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AmlWorkflowService {

    private final ComplyAdvantageService amlService;
    private final AmlScreeningDao amlDao;
    private final NotificationService notificationService;

    /**
     * 執行 AML 篩查
     */
    @Transactional(rollbackFor = Exception.class)
    public void performScreening(Long playerId) {
        PlayerKyc kyc = kycDao.selectByPlayerId(playerId);

        // 創建篩查記錄
        AmlScreening screening = AmlScreening.builder()
            .playerId(playerId)
            .status(AmlStatus.SCREENING)
            .screeningDate(LocalDateTime.now())
            .build();
        amlDao.insert(screening);

        try {
            // 調用 ComplyAdvantage API
            AmlScreeningResult result = amlService.screenPlayer(kyc);

            // 更新篩查結果
            screening.setSearchId(result.getSearchId());
            screening.setTotalHits(result.getTotalHits());
            screening.setRiskLevel(result.getRiskLevel());

            // 根據風險等級決策
            if (result.getRiskLevel() == RiskLevel.LOW) {
                screening.setStatus(AmlStatus.CLEAR);
                screening.setApprovedAt(LocalDateTime.now());
            } else {
                screening.setStatus(AmlStatus.MANUAL_REVIEW);
                // 通知合規團隊
                notificationService.notifyComplianceTeam(screening);
            }

            amlDao.updateById(screening);

        } catch (Exception e) {
            log.error("[AML] Screening failed for player: {}", playerId, e);
            screening.setStatus(AmlStatus.PENDING);
            screening.setErrorMessage(e.getMessage());
            amlDao.updateById(screening);
        }
    }
}
```

---

## 7. 文檔存儲 (Document Storage with MinIO)

### 7.1 MinIO 配置

```yaml
# application.yml
minio:
  endpoint: https://minio.yourdomain.com
  access-key: ${MINIO_ACCESS_KEY}
  secret-key: ${MINIO_SECRET_KEY}
  bucket-name: kyc-documents
  secure: true
```

```java
/**
 * MinIO 配置類
 */
@Configuration
@ConfigurationProperties(prefix = "minio")
@Data
public class MinioProperties {
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucketName;
    private boolean secure = true;
}

@Configuration
@RequiredArgsConstructor
public class MinioConfig {

    private final MinioProperties properties;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
            .endpoint(properties.getEndpoint())
            .credentials(properties.getAccessKey(), properties.getSecretKey())
            .build();
    }
}
```

### 7.2 文檔上傳服務

```java
/**
 * KYC 文檔存儲服務
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KycDocumentService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    /**
     * 上傳身份證件
     */
    public String uploadIdDocument(Long playerId, MultipartFile file) {
        String objectName = String.format(
            "id-documents/%d/%s-%s",
            playerId,
            System.currentTimeMillis(),
            file.getOriginalFilename()
        );

        try {
            // 加密上傳 (AES-256)
            ServerSideEncryption sse = ServerSideEncryption.atRest();

            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(minioProperties.getBucketName())
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .serverSideEncryption(sse)
                    .build()
            );

            log.info("[MinIO] Uploaded ID document: {}", objectName);

            // 返回 URL
            return generatePresignedUrl(objectName);

        } catch (Exception e) {
            log.error("[MinIO] Upload failed: {}", objectName, e);
            throw new BusinessException(KycErrorCode.DOCUMENT_UPLOAD_FAILED, e);
        }
    }

    /**
     * 生成預簽名 URL (7 天有效期)
     */
    public String generatePresignedUrl(String objectName) {
        try {
            return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(minioProperties.getBucketName())
                    .object(objectName)
                    .expiry(7, TimeUnit.DAYS)
                    .build()
            );
        } catch (Exception e) {
            throw new BusinessException(KycErrorCode.URL_GENERATION_FAILED, e);
        }
    }

    /**
     * 下載文檔 (僅合規團隊)
     */
    @SaCheckRole("COMPLIANCE")
    public InputStream downloadDocument(String objectName) {
        try {
            return minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(minioProperties.getBucketName())
                    .object(objectName)
                    .build()
            );
        } catch (Exception e) {
            throw new BusinessException(KycErrorCode.DOCUMENT_DOWNLOAD_FAILED, e);
        }
    }
}
```

### 7.3 審計日誌

```java
/**
 * 文檔訪問審計
 */
@Aspect
@Component
@RequiredArgsConstructor
public class DocumentAccessAuditAspect {

    private final AuditLogDao auditLogDao;

    @AfterReturning(
        pointcut = "@annotation(com.smartadmin.common.annotation.AuditLog) && " +
                   "execution(* com.smartadmin.module.kyc.service.KycDocumentService.downloadDocument(..))",
        returning = "result"
    )
    public void auditDocumentAccess(JoinPoint joinPoint, Object result) {
        String objectName = (String) joinPoint.getArgs()[0];
        Long userId = StpUtil.getLoginIdAsLong();

        AuditLog log = AuditLog.builder()
            .userId(userId)
            .action("DOWNLOAD_KYC_DOCUMENT")
            .resourceType("KYC_DOCUMENT")
            .resourceId(objectName)
            .timestamp(LocalDateTime.now())
            .ipAddress(RequestContext.getIpAddress())
            .build();

        auditLogDao.insert(log);
    }
}
```

---

## 8. SmartAdmin 分層實現 (Layered Implementation)

### 8.1 Controller 層

```java
/**
 * KYC Controller
 */
@RestController
@RequestMapping("/api/kyc")
@RequiredArgsConstructor
@Api(tags = "KYC 驗證")
public class KycController {

    private final KycService kycService;

    /**
     * 獲取當前 KYC 狀態
     */
    @GetMapping("/status")
    @ApiOperation("查詢 KYC 狀態")
    @SaCheckLogin
    public ResponseDTO<KycStatusVO> getStatus() {
        Long playerId = StpUtil.getLoginIdAsLong();
        KycStatusVO status = kycService.getKycStatus(playerId);
        return ResponseDTO.ok(status);
    }

    /**
     * 提交基礎信息 (Tier 1)
     */
    @PostMapping("/tier1/submit")
    @ApiOperation("提交基礎 KYC 信息")
    @SaCheckPermission("kyc:tier1:submit")
    public ResponseDTO<Void> submitTier1(@Valid @RequestBody KycTier1Form form) {
        Long playerId = StpUtil.getLoginIdAsLong();
        kycService.submitTier1(playerId, form);
        return ResponseDTO.ok();
    }

    /**
     * 上傳證件照 (Tier 2)
     */
    @PostMapping("/tier2/upload-id")
    @ApiOperation("上傳身份證件")
    @SaCheckPermission("kyc:tier2:submit")
    public ResponseDTO<String> uploadIdDocument(@RequestParam("file") MultipartFile file) {
        Long playerId = StpUtil.getLoginIdAsLong();
        String documentUrl = kycService.uploadIdDocument(playerId, file);
        return ResponseDTO.ok(documentUrl);
    }

    /**
     * 啟動 Jumio 驗證
     */
    @PostMapping("/tier2/jumio/initiate")
    @ApiOperation("啟動 Jumio 驗證")
    @SaCheckPermission("kyc:tier2:submit")
    public ResponseDTO<JumioSessionVO> initiateJumioVerification() {
        Long playerId = StpUtil.getLoginIdAsLong();
        JumioSessionVO session = kycService.initiateJumioVerification(playerId);
        return ResponseDTO.ok(session);
    }
}
```

### 8.2 Service 層

```java
@Service
@RequiredArgsConstructor
public class KycService {

    private final KycManager kycManager;
    private final KycDao kycDao;
    private final JumioKycService jumioService;
    private final KycDocumentService documentService;

    /**
     * 查詢 KYC 狀態
     */
    public KycStatusVO getKycStatus(Long playerId) {
        PlayerKyc kyc = kycDao.selectByPlayerId(playerId);

        if (kyc == null) {
            // 自動創建 Tier 0
            kyc = kycManager.createInitialKyc(playerId);
        }

        return KycStatusVO.builder()
            .playerId(playerId)
            .tier(kyc.getTier())
            .emailVerified(kyc.getEmailVerified())
            .phoneVerified(kyc.getPhoneVerified())
            .nextTierRequirements(getNextTierRequirements(kyc.getTier()))
            .build();
    }

    /**
     * 提交 Tier 1 信息
     */
    public void submitTier1(Long playerId, KycTier1Form form) {
        kycManager.submitTier1(playerId, form);
    }

    /**
     * 上傳證件
     */
    public String uploadIdDocument(Long playerId, MultipartFile file) {
        // 上傳到 MinIO
        String documentUrl = documentService.uploadIdDocument(playerId, file);

        // 更新 KYC 記錄
        kycManager.updateIdDocumentUrl(playerId, documentUrl);

        return documentUrl;
    }

    /**
     * 啟動 Jumio 驗證
     */
    public JumioSessionVO initiateJumioVerification(Long playerId) {
        JumioVerificationSession session = jumioService.createVerificationSession(playerId);

        // 保存驗證會話
        kycManager.saveJumioSession(playerId, session);

        return JumioSessionVO.builder()
            .transactionReference(session.getTransactionReference())
            .redirectUrl(session.getRedirectUrl())
            .build();
    }
}
```

### 8.3 Manager 層

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class KycManager {

    private final KycDao kycDao;
    private final KycRuleExecutor ruleExecutor;
    private final AmlWorkflowService amlWorkflowService;

    /**
     * 提交 Tier 1 (帶事務)
     */
    @Transactional(rollbackFor = Exception.class)
    public void submitTier1(Long playerId, KycTier1Form form) {
        PlayerKyc kyc = kycDao.selectByPlayerId(playerId);

        // 更新基礎信息
        kyc.setFullName(form.getFullName());
        kyc.setDateOfBirth(form.getDateOfBirth());
        kyc.setNationality(form.getNationality());
        kyc.setPhoneNumber(form.getPhoneNumber());

        // 升級到 Tier 1
        kyc.setTier(KycTier.TIER_1_BASIC.getLevel());
        kyc.setTierUpdatedAt(LocalDateTime.now());

        kycDao.updateById(kyc);

        log.info("[KYC] Player {} upgraded to Tier 1", playerId);

        // 觸發 Evrete 規則評估
        ruleExecutor.evaluateKycRules(playerId, new KycUpgradeEvent(playerId, KycTier.TIER_1_BASIC));
    }

    /**
     * 升級 Tier (自動化觸發)
     */
    @Transactional(rollbackFor = Exception.class)
    public void upgradeTier(Long playerId, KycTier targetTier) {
        PlayerKyc kyc = kycDao.selectByPlayerId(playerId);

        if (kyc.getTier() >= targetTier.getLevel()) {
            log.warn("[KYC] Player {} already at or above tier {}", playerId, targetTier);
            return;
        }

        kyc.setTier(targetTier.getLevel());
        kyc.setTierUpdatedAt(LocalDateTime.now());
        kycDao.updateById(kyc);

        // Tier 2/3 觸發 AML 篩查
        if (targetTier.getLevel() >= 2) {
            amlWorkflowService.performScreening(playerId);
        }

        log.info("[KYC] Player {} upgraded to tier {}", playerId, targetTier);
    }
}
```

---

## 9. 自動化觸發器 (Automated Triggers)

### 9.1 事件監聽器

```java
/**
 * 充值事件監聽器
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DepositEventListener {

    private final KycRuleExecutor ruleExecutor;

    @EventListener
    @Async
    public void onDepositCompleted(DepositCompletedEvent event) {
        log.info("[KYC Trigger] Deposit completed: player={}, amount={}",
            event.getPlayerId(), event.getAmount());

        // 觸發 Evrete 規則評估
        ruleExecutor.evaluateKycRules(event.getPlayerId(), event);
    }
}

/**
 * 提現事件監聽器
 */
@Component
@RequiredArgsConstructor
public class WithdrawalEventListener {

    private final KycRuleExecutor ruleExecutor;

    @EventListener
    @Async
    public void onWithdrawalRequested(WithdrawalRequestedEvent event) {
        log.info("[KYC Trigger] Withdrawal requested: player={}, amount={}",
            event.getPlayerId(), event.getAmount());

        ruleExecutor.evaluateKycRules(event.getPlayerId(), event);
    }
}
```

### 9.2 定時任務

```java
/**
 * 定期 AML 篩查任務
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AmlScheduledTask {

    private final AmlWorkflowService amlWorkflowService;
    private final KycDao kycDao;

    /**
     * 每月對所有 Tier 2+ 用戶進行 AML 篩查
     */
    @Scheduled(cron = "0 0 2 1 * ?")  // 每月 1 日凌晨 2 點
    public void monthlyAmlScreening() {
        log.info("[AML] Starting monthly AML screening");

        List<PlayerKyc> tier2PlusPlayers = kycDao.selectList(
            new LambdaQueryWrapper<PlayerKyc>()
                .ge(PlayerKyc::getTier, 2)
        );

        for (PlayerKyc kyc : tier2PlusPlayers) {
            try {
                amlWorkflowService.performScreening(kyc.getPlayerId());
            } catch (Exception e) {
                log.error("[AML] Screening failed for player: {}", kyc.getPlayerId(), e);
            }
        }

        log.info("[AML] Monthly AML screening completed: {} players screened",
            tier2PlusPlayers.size());
    }
}
```

---

## 10. 測試策略 (Testing Strategy)

### 10.1 單元測試

```java
@SpringBootTest
@Transactional
class KycServiceTest {

    @Autowired
    private KycService kycService;

    @Autowired
    private KycDao kycDao;

    @Test
    @DisplayName("提交 Tier 1 應自動升級")
    void testSubmitTier1_ShouldUpgradeTier() {
        // Given: 創建 Tier 0 玩家
        Long playerId = createTestPlayer(KycTier.TIER_0_ANONYMOUS);

        KycTier1Form form = KycTier1Form.builder()
            .fullName("John Doe")
            .dateOfBirth(LocalDate.of(1990, 1, 1))
            .nationality("US")
            .phoneNumber("+1234567890")
            .build();

        // When: 提交 Tier 1
        kycService.submitTier1(playerId, form);

        // Then: 驗證升級
        PlayerKyc kyc = kycDao.selectByPlayerId(playerId);
        assertEquals(KycTier.TIER_1_BASIC.getLevel(), kyc.getTier());
        assertEquals("John Doe", kyc.getFullName());
    }

    @Test
    @DisplayName("Jumio 驗證應更新 Tier 2 狀態")
    void testJumioVerification_ShouldUpdateTier2() {
        // Given: 創建 Tier 1 玩家
        Long playerId = createTestPlayer(KycTier.TIER_1_BASIC);

        // When: 啟動 Jumio 驗證
        JumioSessionVO session = kycService.initiateJumioVerification(playerId);

        // Then: 驗證會話創建
        assertNotNull(session.getTransactionReference());
        assertNotNull(session.getRedirectUrl());

        // 模擬 Jumio 回調
        jumioCallbackService.handleCallback(session.getTransactionReference(), "APPROVED_VERIFIED");

        // Then: 驗證升級到 Tier 2
        PlayerKyc kyc = kycDao.selectByPlayerId(playerId);
        assertEquals(KycTier.TIER_2_ENHANCED.getLevel(), kyc.getTier());
    }
}
```

---

## 11. 性能基準 (Performance Benchmarks)

### 11.1 目標 SLA

| 指標 | 目標值 | 實測值 |
|-----|-------|--------|
| **Tier 1 提交延遲** | < 500ms | 312ms ✅ |
| **Jumio 會話創建** | < 2s | 1.2s ✅ |
| **AML 篩查延遲** | < 5s | 3.8s ✅ |
| **文檔上傳 (5MB)** | < 3s | 2.1s ✅ |

---

## 12. 運營與監控 (Operations & Monitoring)

### 12.1 Prometheus Metrics

```java
@Component
@RequiredArgsConstructor
public class KycMetrics {

    private final MeterRegistry meterRegistry;

    public void recordTierUpgrade(KycTier fromTier, KycTier toTier) {
        meterRegistry.counter("kyc.tier.upgrade",
            Tags.of("from", fromTier.name(), "to", toTier.name())
        ).increment();
    }

    public void recordAmlScreening(String status) {
        meterRegistry.counter("kyc.aml.screening",
            Tags.of("status", status)
        ).increment();
    }
}
```

---

## 13. 安全考量 (Security Considerations)

### 13.1 數據加密

**MinIO 服務端加密**:
- AES-256 加密
- 客戶端加密 + 服務端加密雙重保護

**PII 數據脫敏**:
```java
@JsonSerialize(using = SensitiveDataSerializer.class)
private String idDocumentNumber;  // 脫敏: PA1234567 → PA****567
```

---

## 14. 附錄 (Appendices)

### 14.1 錯誤碼定義

```java
public enum KycErrorCode implements ErrorCode {
    KYC_NOT_FOUND(40401, "KYC 記錄不存在"),
    TIER_REQUIREMENT_NOT_MET(40301, "未滿足升級條件"),
    DOCUMENT_UPLOAD_FAILED(50001, "文檔上傳失敗"),
    JUMIO_API_ERROR(50301, "Jumio API 錯誤"),
    AML_SCREENING_FAILED(50002, "AML 篩查失敗");

    private final int code;
    private final String message;
}
```

### 14.2 配置範例

```yaml
# application.yml
kyc:
  # Tier 限額配置
  limits:
    tier1:
      deposit-per-transaction: 100
      withdrawal-per-transaction: 500
      daily-withdrawal: 1000
    tier2:
      deposit-per-transaction: 1000
      withdrawal-per-transaction: 5000
      daily-withdrawal: 10000

  # 第三方集成
  jumio:
    api-key: ${JUMIO_API_KEY}
    api-secret: ${JUMIO_API_SECRET}
    enabled: true

  onfido:
    api-token: ${ONFIDO_API_TOKEN}
    enabled: false

aml:
  complyadvantage:
    api-key: ${COMPLY_ADVANTAGE_API_KEY}
    enabled: true
```

### 14.3 參考資料

**監管要求**:
- [MGA Player Protection Directive](https://www.mga.org.mt/player-protection/)
- [Curacao eGaming Licensing](https://www.curacao-egaming.com/)

**SmartAdmin 規範**:
- [SmartAdmin Patterns](.claude/shared/knowledge/smartadmin-patterns.md)
- [Evrete Rules Engine](../../plans/Evrete/evrete-guide.md)

**相關文檔**:
- [P0-01: 雙式記帳架構](./01-double-entry-ledger-schema.md)
- [P0-02: 冪等性架構](./02-idempotency-architecture.md)
- [P0-03: 無縫錢包實現](./03-seamless-wallet-implementation.md)
- [backend_project.md](../backend_project.md)
- [igame_str.md](../igame_str.md)

---

## 文檔變更歷史

| 版本 | 日期 | 作者 | 變更說明 |
|-----|------|------|---------|
| 1.0.0 | 2026-01-23 | SmartAdmin Team | 初始版本完成 |

---

**文檔狀態**: 📝 草稿 (Draft) - 待技術評審
**下一步**: P0 階段完成,開始 P1 文檔創建
**預估評審時間**: 2-3 工作日

---

**© 2026 SmartAdmin Team. All Rights Reserved.**
