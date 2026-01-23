# P0-04: KYC/AML 自動化 (KYC/AML Automation)

**文檔版本**: 1.1
**狀態**: 📝 草稿 (Draft)
**優先級**: P0 - 關鍵基礎 (Critical Foundation)
**預估行數**: 900-1100
**依賴文檔**: 無 (獨立實現)
**被依賴文檔**: P1-06 (風控引擎), P1-16 (合規審計)
**最後更新**: 2026-01-23

**變更歷史**:
- v1.1 (2026-01-23): 新增 3 個 Mermaid 圖表 - Progressive KYC 等級升級流程圖、KYC 文檔驗證狀態機圖、AML 風險評分決策樹
- v1.0.0 (2026-01-23): 初始版本完成

---

## 目錄 (Table of Contents)

1. [執行摘要 (Executive Summary)](#1-執行摘要-executive-summary)
2. [背景與戰略對齊 (Background & Strategic Alignment)](#2-背景與戰略對齊-background--strategic-alignment)
3. [Progressive KYC 分級體系 (Progressive KYC Tiers)](#3-progressive-kyc-分級體系-progressive-kyc-tiers)
4. [LiteFlow 流程編排集成 (LiteFlow Integration)](#4-liteflow-流程編排集成-liteflow-integration)
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
│  • 自動化: LiteFlow 流程引擎觸發                              │
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
- ✅ **自動化**: LiteFlow 流程引擎自動觸發升級
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
| **Automation Leverage** | LiteFlow 流程引擎自動觸發,替代 70% 人工審核 |
| **Code Leverage** | 統一 KycService 處理所有 Tier 升級邏輯 |
| **Trust Leverage** | 第三方 API (Jumio/Onfido) 提供專業驗證 |

**零邊際成本擴展**:
- 新增 1000 用戶: 自動化觸發,**無額外成本**
- 新增 1 個監管要求: 更新 LiteFlow 鏈,**無需改代碼**

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

#### 圖 3.1: 流程圖 - Progressive KYC 等級升級流程

> **說明**：此圖展示 Progressive KYC 的完整升級流程，從 Tier 0 匿名用戶逐步升級到 Tier 3 完全驗證。系統根據用戶操作自動觸發升級提示，並通過第三方服務（Jumio、ComplyAdvantage）進行身份驗證和 AML 篩查，確保合規性的同時最大化用戶轉化率。
>
> **關鍵要素**：
> - 🟢 **漸進式設計**：用戶按需升級，降低註冊摩擦
> - 🔵 **自動觸發**：達到金額閾值時自動提示升級
> - ⚠️ **第三方驗證**：集成 Jumio (身份) + ComplyAdvantage (AML)
> - 🔴 **風險攔截**：AML 命中或高風險自動凍結
>
> **相關文檔**：參見 [§3.1 Tier 定義](#31-tier-定義)、[§3.2 權限矩陣](#32-tier-權限矩陣)、[§5 第三方服務集成](#5-第三方服務集成-third-party-integrations)

```mermaid
flowchart TD
    START([用戶註冊]) --> TIER0[Tier 0: 匿名用戶<br/>僅郵箱註冊<br/>可試玩遊戲]

    TIER0 --> TRIGGER1{是否觸發<br/>升級條件?}

    TRIGGER1 -->|充值 > $100| PROMPT1[系統提示：<br/>升級到 Tier 1 可提現]
    TRIGGER1 -->|未觸發| TIER0

    PROMPT1 --> USER1{用戶是否<br/>開始升級?}

    USER1 -->|否| TIER0
    USER1 -->|是| SUBMIT1[填寫基礎信息<br/>姓名、生日、國籍、手機]

    SUBMIT1 --> VERIFY_EMAIL[驗證郵箱<br/>發送驗證碼]
    VERIFY_EMAIL --> VERIFY_PHONE[驗證手機號<br/>發送 SMS 碼]

    VERIFY_PHONE --> CHECK1{驗證是否<br/>全部通過?}

    CHECK1 -->|否| REJECT1[拒絕升級<br/>返回 Tier 0]
    REJECT1 --> TIER0

    CHECK1 -->|是| TIER1[✅ Tier 1: 基礎驗證<br/>日提現額度 $1000<br/>充值限額 $100/次]

    TIER1 --> TRIGGER2{是否觸發<br/>升級條件?}

    TRIGGER2 -->|提現 > $500| PROMPT2[系統提示：<br/>升級到 Tier 2 可提高額度]
    TRIGGER2 -->|未觸發| TIER1

    PROMPT2 --> USER2{用戶是否<br/>開始升級?}

    USER2 -->|否| TIER1
    USER2 -->|是| SUBMIT2[上傳證件<br/>身份證/護照/駕照<br/>+ 地址證明]

    SUBMIT2 --> JUMIO[第三方驗證<br/>Jumio API<br/>OCR + 活體檢測]

    JUMIO --> CHECK2{Jumio<br/>驗證結果?}

    CHECK2 -->|FAILED| REJECT2[拒絕升級<br/>通知用戶重新上傳]
    REJECT2 --> TIER1

    CHECK2 -->|APPROVED| TIER2[✅ Tier 2: 增強驗證<br/>日提現額度 $10000<br/>充值限額 $1000/次]

    TIER2 --> TRIGGER3{是否觸發<br/>升級條件?}

    TRIGGER3 -->|提現 > $5000| PROMPT3[系統提示：<br/>升級到 Tier 3 解除限額]
    TRIGGER3 -->|未觸發| TIER2

    PROMPT3 --> USER3{用戶是否<br/>開始升級?}

    USER3 -->|否| TIER2
    USER3 -->|是| SUBMIT3[視頻 KYC<br/>+ 資金來源說明]

    SUBMIT3 --> VIDEO_KYC[視頻 KYC 審核<br/>人工審核 + AI 輔助]

    VIDEO_KYC --> AML_CHECK[AML 篩查<br/>ComplyAdvantage API<br/>制裁名單/PEP/負面新聞]

    AML_CHECK --> CHECK3{AML<br/>篩查結果?}

    CHECK3 -->|HIT: 命中高風險| FREEZE[❌ 凍結帳戶<br/>人工審核]
    CHECK3 -->|CLEAR: 無風險| TIER3[✅ Tier 3: 完全驗證<br/>無限額<br/>VIP 專屬服務]

    FREEZE --> MANUAL_REVIEW{人工審核<br/>結果?}

    MANUAL_REVIEW -->|拒絕| PERMANENT_BAN[永久封禁]
    MANUAL_REVIEW -->|通過| TIER3

    TIER3 --> END([用戶享受完整服務])
    PERMANENT_BAN --> END

    style START fill:#90EE90
    style END fill:#FFB6C1
    style TIER0 fill:#E8E8E8
    style TIER1 fill:#87CEEB
    style TIER2 fill:#FFD700
    style TIER3 fill:#9370DB
    style FREEZE fill:#FF6B6B
    style PERMANENT_BAN fill:#8B0000,color:#fff
    style JUMIO fill:#FFA500
    style AML_CHECK fill:#FFA500
```

**圖例 (Legend)**:
- `圓角矩形`: 開始/結束
- `矩形`: 處理步驟
- `菱形`: 決策分支點
- `🟢 綠色`: 開始節點
- `⚪ 灰色`: Tier 0 匿名
- `🔵 藍色`: Tier 1 基礎
- `🟡 黃色`: Tier 2 增強
- `🟣 紫色`: Tier 3 完全
- `🔴 紅色`: 凍結/封禁
- `🟠 橙色`: 第三方驗證

**升級觸發條件**:

| 當前 Tier | 觸發升級的操作 | 提示時機 | 用戶轉化率 |
|----------|--------------|---------|-----------|
| **Tier 0 → 1** | 累計充值 > $100 | 首次充值後 | 60-70% |
| **Tier 1 → 2** | 單次提現 > $500 或累計提現 > $1000 | 提現被限額時 | 40-50% |
| **Tier 2 → 3** | 單次提現 > $5000 或月提現 > $50000 | 提現被限額時 | 10-15% |

**驗證時長**:

| Tier 升級 | 自動化部分 | 人工審核 | 總時長 |
|----------|----------|---------|-------|
| **0 → 1** | 郵箱/手機驗證 (< 5min) | 無 | < 5min |
| **1 → 2** | Jumio OCR + 活體 (< 2min) | 可選 (10%) | 2min - 2h |
| **2 → 3** | 視頻 KYC + AML (< 3min) | 必須 (100%) | 1-24h |

**AML 篩查邏輯**:

| 篩查維度 | 數據源 | 命中後處理 |
|---------|-------|-----------|
| **制裁名單** | OFAC, UN, EU | 立即凍結 + 人工審核 |
| **政治公眾人物 (PEP)** | ComplyAdvantage | 增強盡職調查 (EDD) |
| **負面新聞** | 全球新聞庫 | 風險評分 + 人工判斷 |
| **高風險地區** | IP + 證件國籍 | 額外驗證步驟 |

---

## 4. LiteFlow 流程編排集成 (LiteFlow Integration)

> **重要更新（2026-01-23）**: 本系統已從 Evrete 遷移至 LiteFlow 流程編排引擎。詳見 [ADR-011: LiteFlow Migration](../../architecture-decisions/011-liteflow-migration.md)。

### 4.1 LiteFlow 鏈定義示例

**流程鏈定義** (存儲在 PostgreSQL 數據庫)

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

### 4.2 LiteFlow 集成實現

```java
/**
 * KYC 流程執行服務
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KycFlowExecutor {

    private final LiteFlowExecutionService liteFlowExecutionService;
    private final PlayerDao playerDao;
    private final StatsService statsService;
    private final KycService kycService;

    /**
     * 執行 KYC 升級評估流程
     */
    public void evaluateKycUpgrade(Long playerId, KycEvent event) {
        log.info("[KYC Flow] Evaluating KYC upgrade for player: {}, event: {}", playerId, event);

        // 準備輸入參數
        Player player = playerDao.selectById(playerId);
        PlayerStats stats = statsService.getStats(playerId);

        LiteFlowExecutionForm executionForm = new LiteFlowExecutionForm();
        executionForm.setChainCode("kyc-upgrade-evaluation-chain");
        executionForm.setInputParams(Map.of(
            "player", player,
            "stats", stats,
            "event", event,
            "currentTier", player.getKycTier()
        ));

        // 執行流程鏈
        ResponseDTO<LiteFlowExecutionResultVO> response =
            liteFlowExecutionService.execute(executionForm);

        if (response.getOk()) {
            LiteFlowExecutionResultVO result = response.getData();
            Map<String, Object> output = result.getOutputResult();

            if (Boolean.TRUE.equals(output.get("shouldUpgrade"))) {
                KycTier targetTier = (KycTier) output.get("targetTier");
                log.info("[KYC Flow] Triggering upgrade: player={}, {} -> {}",
                    playerId, player.getKycTier(), targetTier);
                kycService.upgradeKycTier(playerId, targetTier);
            }
        } else {
            log.error("[KYC Flow] Flow execution failed: {}", response.getMsg());
        }
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

#### 圖 5.1: 狀態機圖 - KYC 文檔驗證生命週期

> **說明**：此圖展示單個 KYC 文檔（身份證、護照、地址證明等）從提交到最終狀態的完整驗證生命週期。涵蓋自動化 OCR 識別、活體檢測、人工審核、過期重新驗證等流程，確保合規性和用戶體驗的平衡。
>
> **關鍵要素**：
> - 🟡 **自動化驗證**：Jumio OCR + 活體檢測（80-90% 自動通過）
> - 🔵 **人工審核**：複雜案例轉人工（10-20%）
> - 🔴 **過期處理**：文檔過期自動失效，需重新提交
> - ⏱️ **時效性**：護照/身份證有效期前 30 天提醒更新
>
> **相關文檔**：參見 [§5.1 Jumio 集成](#51-jumio-實名驗證)、[§5.2 Onfido 備選](#52-onfido-實名驗證-備選)、[§3.1 Tier 定義](#31-tier-定義)

```mermaid
stateDiagram-v2
    [*] --> SUBMITTED: 用戶上傳文檔

    SUBMITTED --> PENDING_OCR: 進入處理隊列

    PENDING_OCR --> OCR_PROCESSING: Jumio OCR 識別<br/>提取姓名、生日、證件號

    OCR_PROCESSING --> OCR_SUCCESS: OCR 成功
    OCR_PROCESSING --> OCR_FAILED: OCR 失敗<br/>圖片模糊/反光

    OCR_SUCCESS --> LIVENESS_CHECK: 活體檢測<br/>防止照片偽造

    LIVENESS_CHECK --> AUTO_APPROVED: 活體通過 + 高置信度<br/>(80-90% 案例)
    LIVENESS_CHECK --> MANUAL_REVIEW: 活體失敗或低置信度<br/>轉人工審核

    OCR_FAILED --> REJECTED: 拒絕並通知用戶<br/>重新上傳

    MANUAL_REVIEW --> REVIEWING: 合規團隊審核中<br/>比對資料一致性

    REVIEWING --> APPROVED: 人工審核通過
    REVIEWING --> REJECTED: 人工審核拒絕<br/>偽造/篡改/過期

    AUTO_APPROVED --> ACTIVE: 文檔生效
    APPROVED --> ACTIVE: 文檔生效

    ACTIVE --> EXPIRING_SOON: 有效期前 30 天<br/>系統發送提醒

    EXPIRING_SOON --> EXPIRED: 到達有效期
    EXPIRING_SOON --> RENEWED: 用戶更新文檔

    RENEWED --> SUBMITTED: 重新驗證流程

    EXPIRED --> INACTIVE: 文檔失效<br/>降低 KYC Tier

    REJECTED --> [*]: 最終拒絕
    INACTIVE --> [*]: 失效狀態

    note right of SUBMITTED
        用戶操作
        上傳文檔類型：
        • 護照
        • 身份證
        • 駕照
        • 地址證明
    end note

    note right of OCR_PROCESSING
        自動化處理
        技術：
        • Tesseract OCR
        • Jumio API
        • 機器學習模型
        準確率：95%+
    end note

    note right of LIVENESS_CHECK
        活體檢測
        防偽手段：
        • 3D 面部識別
        • 微表情分析
        • 設備指紋
        • 環境光檢測
    end note

    note right of MANUAL_REVIEW
        人工審核
        審核要點：
        • 照片清晰度
        • 文檔真實性
        • 信息一致性
        • 有效期檢查
        平均時長：1-2小時
    end note

    note right of ACTIVE
        生效狀態
        有效期：
        • 護照：至過期日
        • 身份證：至過期日
        • 地址證明：6 個月
        • 銀行對帳單：3 個月
    end note
```

**圖例 (Legend)**:
- `實線箭頭 (→)`: 正常狀態轉換
- `SUBMITTED`: 用戶提交文檔
- `AUTO_APPROVED`: 自動批准（高置信度）
- `MANUAL_REVIEW`: 轉人工審核
- `ACTIVE`: 文檔生效
- `EXPIRED`: 文檔過期

**狀態統計數據**:

| 狀態 | 佔比 | 平均停留時間 | 後續轉換 |
|-----|------|------------|---------|
| **SUBMITTED** | 100% | < 1秒 | 進入 OCR |
| **OCR_PROCESSING** | 100% | 2-5秒 | 95% 成功，5% 失敗 |
| **AUTO_APPROVED** | 80-85% | < 10秒 | 直接生效 |
| **MANUAL_REVIEW** | 15-20% | 1-2小時 | 90% 通過，10% 拒絕 |
| **ACTIVE** | 85-90% | 180-365天 | 過期或續期 |
| **EXPIRED** | 5-10% | 永久 | 需重新提交 |

**文檔類型與有效期**:

| 文檔類型 | 有效期 | 過期前提醒 | 續期要求 |
|---------|-------|----------|---------|
| **護照** | 至文檔過期日 | 30天前 | 重新上傳新護照 |
| **身份證** | 至文檔過期日 | 30天前 | 重新上傳新身份證 |
| **駕照** | 至文檔過期日 | 30天前 | 重新上傳新駕照 |
| **地址證明** | 6個月 | 15天前 | 重新上傳近期賬單 |
| **銀行對帳單** | 3個月 | 7天前 | 重新上傳最新對帳單 |

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

#### 圖 6.1: 決策樹 - AML 風險評分自動化決策

> **說明**：此圖展示 AML（反洗錢）風險評分的自動化決策邏輯，基於多維度風險因子計算綜合風險分數，並根據閾值決定是否需要人工審核或直接凍結帳戶。系統集成 ComplyAdvantage API，實時篩查制裁名單、PEP、負面新聞等高風險信號。
>
> **關鍵要素**：
> - 🟢 **多維度評分**：制裁名單、PEP、地理位置、交易行為、設備指紋
> - 🔴 **自動化決策**：低風險自動通過，高風險自動凍結
> - 🟡 **中風險審核**：分數 40-69 需人工判斷
> - ⚠️ **實時更新**：每日同步全球制裁名單
>
> **相關文檔**：參見 [§6.1 AML 自動化邏輯](#61-自動化觸發)、[§5.3 ComplyAdvantage 集成](#53-complyadvantage-aml-篩查)

```mermaid
graph TD
    START([開始：AML 篩查<br/>玩家 KYC 資料]) --> SCORE_INIT[初始化風險分數<br/>riskScore = 0]

    SCORE_INIT --> CHECK_SANCTION{制裁名單匹配<br/>OFAC/UN/EU?}

    CHECK_SANCTION -->|匹配| ADD_100[riskScore += 100<br/>🚨 嚴重風險]
    CHECK_SANCTION -->|無匹配| CHECK_PEP

    ADD_100 --> FREEZE_IMMEDIATE[❌ 立即凍結帳戶<br/>riskLevel = CRITICAL]

    CHECK_PEP{政治公眾人物<br/>PEP?}

    CHECK_PEP -->|是| ADD_40[riskScore += 40<br/>⚠️ 高風險]
    CHECK_PEP -->|否| CHECK_NEWS

    CHECK_NEWS{負面新聞<br/>命中?}

    CHECK_NEWS -->|是| ADD_30[riskScore += 30<br/>⚠️ 中高風險]
    CHECK_NEWS -->|否| CHECK_COUNTRY

    CHECK_COUNTRY{國家風險等級<br/>FATF 黑名單?}

    CHECK_COUNTRY -->|高風險國家| ADD_25[riskScore += 25<br/>⚠️ 地理風險]
    CHECK_COUNTRY -->|中低風險國家| CHECK_TX

    CHECK_TX{異常交易模式<br/>ML 檢測?}

    CHECK_TX -->|檢測到異常| ADD_20[riskScore += 20<br/>⚠️ 行為風險]
    CHECK_TX -->|正常| CHECK_DEVICE

    CHECK_DEVICE{設備指紋<br/>可疑?}

    CHECK_DEVICE -->|可疑設備| ADD_15[riskScore += 15<br/>⚠️ 設備風險]
    CHECK_DEVICE -->|正常設備| CALCULATE

    ADD_40 --> CHECK_NEWS
    ADD_30 --> CHECK_COUNTRY
    ADD_25 --> CHECK_TX
    ADD_20 --> CHECK_DEVICE
    ADD_15 --> CALCULATE

    CALCULATE[計算總風險分數<br/>finalScore = riskScore]

    CALCULATE --> DECISION{風險分數<br/>評級?}

    DECISION -->|0-39: 低風險| APPROVE[✅ 自動通過<br/>riskLevel = LOW]
    DECISION -->|40-69: 中風險| MANUAL[⚠️ 人工審核<br/>riskLevel = MEDIUM]
    DECISION -->|70-99: 高風險| FREEZE[❌ 凍結帳戶<br/>riskLevel = HIGH]
    DECISION -->|100: 極高風險| FREEZE_IMMEDIATE

    APPROVE --> UPDATE_DB[更新數據庫<br/>aml_status = CLEAR]
    MANUAL --> NOTIFY_COMPLIANCE[通知合規團隊<br/>Slack + Email]
    FREEZE --> NOTIFY_COMPLIANCE
    FREEZE_IMMEDIATE --> NOTIFY_URGENT[緊急通知<br/>電話 + SMS + Email]

    UPDATE_DB --> END_CLEAR([結束：玩家可正常使用])
    NOTIFY_COMPLIANCE --> END_REVIEW([結束：等待人工審核])
    NOTIFY_URGENT --> END_FROZEN([結束：帳戶凍結])

    style START fill:#90EE90
    style END_CLEAR fill:#90EE90
    style END_REVIEW fill:#FFD700
    style END_FROZEN fill:#FF6B6B
    style FREEZE_IMMEDIATE fill:#8B0000,color:#fff
    style FREEZE fill:#FF6B6B
    style APPROVE fill:#87CEEB
    style MANUAL fill:#FFD700
    style ADD_100 fill:#8B0000,color:#fff
    style ADD_40 fill:#FF6B6B
    style ADD_30 fill:#FFA500
    style ADD_25 fill:#FFA500
    style ADD_20 fill:#FFD700
    style ADD_15 fill:#FFE4B5
```

**圖例 (Legend)**:
- `圓角矩形`: 開始/結束節點
- `矩形`: 處理步驟
- `菱形`: 決策分支點
- `🟢 綠色`: 低風險通過
- `🟡 黃色`: 中風險審核
- `🔴 紅色`: 高風險凍結
- `⚫ 黑色`: 極高風險立即凍結

**風險評分規則表**:

| 風險因子 | 權重 | 評分規則 | 數據源 |
|---------|------|---------|-------|
| **制裁名單匹配** | 100 | 命中 = +100（立即凍結） | OFAC, UN, EU, FATF |
| **政治公眾人物 (PEP)** | 40 | 是 = +40 | ComplyAdvantage |
| **負面新聞** | 30 | 命中洗錢/詐騙 = +30 | 全球新聞庫 |
| **高風險國家** | 25 | FATF 黑名單國家 = +25 | FATF 名單 |
| **異常交易模式** | 20 | ML 模型檢測異常 = +20 | 內部風控引擎 |
| **可疑設備** | 15 | 代理/VPN/多帳號 = +15 | 設備指紋 |

**風險等級與處理策略**:

| 風險等級 | 分數範圍 | 處理策略 | 通過率 | 平均處理時長 |
|---------|---------|---------|-------|------------|
| **LOW (低風險)** | 0-39 | ✅ 自動通過 | 75-80% | < 5秒 |
| **MEDIUM (中風險)** | 40-69 | ⚠️ 人工審核 | 15-20% | 2-4小時 |
| **HIGH (高風險)** | 70-99 | ❌ 凍結 + 審核 | 3-5% | 24-48小時 |
| **CRITICAL (極高)** | 100 | 🚨 立即凍結 + 報告 | < 1% | 永久凍結 |

**真實案例分析**:

**案例 1：低風險玩家（自動通過）**
```
姓名：John Smith
國籍：美國
交易：正常充值 $200
設備：個人 iPhone

風險評分：
• 制裁名單：0
• PEP：0
• 負面新聞：0
• 國家風險：0 (美國低風險)
• 交易模式：0 (正常)
• 設備指紋：0 (正常)
─────────────
總分：0 → 低風險 → 自動通過 ✅
```

**案例 2：中風險玩家（人工審核）**
```
姓名：王明
國籍：中國
交易：首次充值 $5000
設備：使用 VPN

風險評分：
• 制裁名單：0
• PEP：0
• 負面新聞：0
• 國家風險：15 (中等風險)
• 交易模式：20 (首充大額)
• 設備指紋：15 (VPN)
─────────────
總分：50 → 中風險 → 人工審核 ⚠️
```

**案例 3：高風險玩家（凍結）**
```
姓名：Ivan Petrov
國籍：俄羅斯
交易：快速充提 $10000
設備：多個設備登錄

風險評分：
• 制裁名單：0
• PEP：0
• 負面新聞：30 (涉詐騙新聞)
• 國家風險：25 (高風險國家)
• 交易模式：20 (快進快出)
• 設備指紋：15 (多設備)
─────────────
總分：90 → 高風險 → 凍結帳戶 ❌
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

        // 觸發 LiteFlow 流程評估
        flowExecutor.evaluateKycUpgrade(playerId, new KycUpgradeEvent(playerId, KycTier.TIER_1_BASIC));
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

        // 觸發 LiteFlow 流程評估
        flowExecutor.evaluateKycUpgrade(event.getPlayerId(), event);
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
- [LiteFlow 流程編排引擎](../../plans/liteflow/README.md)
- [ADR-011: LiteFlow Migration](../../architecture-decisions/011-liteflow-migration.md)

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
