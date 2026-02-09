# 06-07 Multi-Jurisdiction Framework (多牌照架構)

**版本**: 1.0.0
**創建日期**: 2026-02-07
**狀態**: ✅ 完整

---

## 概述

多司法管轄區框架 (Multi-Jurisdiction Framework) 支援 iGaming 平台在不同牌照下運營，根據玩家所在地區動態載入相應的合規規則。

### 支援的牌照

| 牌照 | 監管機構 | 地區 | 嚴格程度 |
|------|---------|------|---------|
| **UKGC** | UK Gambling Commission | 英國 | ⭐⭐⭐⭐⭐ |
| **MGA** | Malta Gaming Authority | 歐盟 | ⭐⭐⭐⭐ |
| **PAGCOR** | Philippine Amusement and Gaming | 菲律賓 | ⭐⭐⭐ |
| **Curacao** | Curacao eGaming | 離岸 | ⭐⭐ |
| **Brazil SPA** | Secretariat of Prizes and Bets | 巴西 | ⭐⭐⭐⭐ |

---

## 架構設計

### 核心組件

```
┌─────────────────────────────────────────────────────────────────┐
│                 Multi-Jurisdiction Gateway                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐         │
│  │ GeoIP       │    │ License     │    │ Rule        │         │
│  │ Detection   │───▶│ Router      │───▶│ Engine      │         │
│  └─────────────┘    └─────────────┘    └─────────────┘         │
│         │                  │                  │                 │
│         ▼                  ▼                  ▼                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                  Jurisdiction Config Store                │  │
│  │  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐      │  │
│  │  │ UKGC    │  │ MGA     │  │ PAGCOR  │  │ Brazil  │      │  │
│  │  │ Rules   │  │ Rules   │  │ Rules   │  │ Rules   │      │  │
│  │  └─────────┘  └─────────┘  └─────────┘  └─────────┘      │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 規則配置模型

```java
@Data
@Entity
@Table(name = "t_jurisdiction_config")
public class JurisdictionConfig {

    @Id
    private String jurisdictionCode;  // UKGC, MGA, PAGCOR, etc.

    private String displayName;
    private String regulatorName;
    private String website;

    // 地理圍欄
    @Type(type = "json")
    private List<String> allowedCountries;

    @Type(type = "json")
    private List<String> blockedCountries;

    // KYC 要求
    private Boolean immediateKycRequired;
    private Integer kycGracePeriodHours;
    private Boolean enhancedDueDiligenceRequired;

    // 負責任博彩
    private Boolean selfExclusionRequired;
    private Boolean depositLimitsRequired;
    private Boolean mandatoryCoolingOff;
    private Integer realityCheckMinutes;
    private Boolean affordabilityCheckRequired;

    // 遊戲限制
    @Type(type = "json")
    private List<String> allowedGameTypes;

    @Type(type = "json")
    private List<String> blockedGameTypes;

    private BigDecimal maxBetAmount;
    private BigDecimal maxWinAmount;

    // 支付限制
    private Boolean creditCardsAllowed;
    private Boolean cryptoAllowed;

    @Type(type = "json")
    private List<String> allowedPaymentMethods;

    // 稅務
    private BigDecimal ggrTaxRate;
    private Boolean withholdingTaxRequired;
    private BigDecimal withholdingTaxRate;

    // 報告要求
    private Integer reportingFrequencyDays;

    @Type(type = "json")
    private ReportingConfig reportingConfig;

    // 外部系統
    private String exclusionDatabaseUrl;  // Gamstop, etc.

    private Boolean active;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;
}
```

---

## 技術實現

### 牌照路由服務

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class JurisdictionRouterService {

    private final JurisdictionConfigDao configDao;
    private final GeoIPService geoIPService;
    private final CacheManager cacheManager;

    /**
     * 根據玩家 IP 確定適用的牌照
     */
    public JurisdictionConfig resolveJurisdiction(String clientIp) {
        // 1. GeoIP 解析
        GeoIPResult geoResult = geoIPService.lookup(clientIp);
        String countryCode = geoResult.getCountryCode();

        // 2. 查找適用的牌照
        List<JurisdictionConfig> configs = configDao.findActiveConfigs();

        for (JurisdictionConfig config : configs) {
            if (isCountryAllowed(countryCode, config)) {
                return config;
            }
        }

        // 3. 返回預設牌照或拒絕服務
        JurisdictionConfig defaultConfig = configDao.findDefault();
        if (defaultConfig != null && isCountryAllowed(countryCode, defaultConfig)) {
            return defaultConfig;
        }

        throw new JurisdictionBlockedException(countryCode);
    }

    /**
     * 根據玩家 ID 獲取牌照配置（已註冊玩家）
     */
    @Cacheable(value = "jurisdiction", key = "#playerId")
    public JurisdictionConfig getPlayerJurisdiction(Long playerId) {
        Player player = playerDao.selectById(playerId);
        return configDao.findByCode(player.getJurisdictionCode());
    }

    /**
     * 檢查功能是否在當前牌照下可用
     */
    public boolean isFeatureEnabled(Long playerId, String featureName) {
        JurisdictionConfig config = getPlayerJurisdiction(playerId);
        return switch (featureName) {
            case "CREDIT_CARD_DEPOSIT" -> config.getCreditCardsAllowed();
            case "CRYPTO_DEPOSIT" -> config.getCryptoAllowed();
            case "LIVE_CASINO" -> config.getAllowedGameTypes().contains("LIVE_CASINO");
            case "SPORTS_BETTING" -> config.getAllowedGameTypes().contains("SPORTS");
            default -> true;
        };
    }

    /**
     * 獲取玩家的限制配置
     */
    public PlayerLimitsConfig getPlayerLimits(Long playerId) {
        JurisdictionConfig config = getPlayerJurisdiction(playerId);
        return PlayerLimitsConfig.builder()
            .maxBetAmount(config.getMaxBetAmount())
            .maxWinAmount(config.getMaxWinAmount())
            .realityCheckMinutes(config.getRealityCheckMinutes())
            .depositLimitsRequired(config.getDepositLimitsRequired())
            .affordabilityCheckRequired(config.getAffordabilityCheckRequired())
            .build();
    }

    private boolean isCountryAllowed(String countryCode, JurisdictionConfig config) {
        if (config.getBlockedCountries().contains(countryCode)) {
            return false;
        }
        if (!config.getAllowedCountries().isEmpty()) {
            return config.getAllowedCountries().contains(countryCode);
        }
        return true;
    }
}
```

### 規則引擎整合

```java
@Service
@RequiredArgsConstructor
public class JurisdictionRuleEngine {

    private final JurisdictionRouterService routerService;
    private final SelfExclusionService selfExclusionService;
    private final DepositLimitService depositLimitService;
    private final AffordabilityService affordabilityService;
    private final GamstopClient gamstopClient;

    /**
     * 玩家註冊時執行牌照特定規則
     */
    @Transactional(rollbackFor = Throwable.class)
    public RegistrationRuleResult executeRegistrationRules(
            PlayerRegistrationForm form,
            JurisdictionConfig config) {

        List<RuleViolation> violations = new ArrayList<>();

        // UKGC: 即時 KYC
        if (config.getImmediateKycRequired()) {
            if (!form.isKycVerified()) {
                violations.add(RuleViolation.of("UKGC_IMMEDIATE_KYC",
                    "UK 牌照要求在存款前完成身份驗證"));
            }
        }

        // UKGC: Gamstop 檢查
        if ("UKGC".equals(config.getJurisdictionCode())) {
            GamstopCheckResult gamstopResult = gamstopClient.checkExclusion(
                form.getFirstName(),
                form.getLastName(),
                form.getDateOfBirth(),
                form.getPostcode()
            );
            if (gamstopResult.isExcluded()) {
                violations.add(RuleViolation.of("GAMSTOP_EXCLUDED",
                    "玩家已在 Gamstop 排除名單中"));
            }
        }

        // 年齡驗證
        int age = Period.between(form.getDateOfBirth(), LocalDate.now()).getYears();
        int minAge = getMinimumAge(config);
        if (age < minAge) {
            violations.add(RuleViolation.of("UNDERAGE",
                "未滿 " + minAge + " 歲不能註冊"));
        }

        return RegistrationRuleResult.builder()
            .passed(violations.isEmpty())
            .violations(violations)
            .build();
    }

    /**
     * 存款時執行牌照特定規則
     */
    public DepositRuleResult executeDepositRules(
            Long playerId,
            DepositForm form,
            JurisdictionConfig config) {

        List<RuleViolation> violations = new ArrayList<>();

        // 信用卡檢查
        if ("CREDIT_CARD".equals(form.getPaymentMethod()) &&
            !config.getCreditCardsAllowed()) {
            violations.add(RuleViolation.of("CREDIT_CARD_NOT_ALLOWED",
                "此牌照不允許使用信用卡存款"));
        }

        // 可負擔性檢查 (UK)
        if (config.getAffordabilityCheckRequired()) {
            Option<AssessmentRequirement> assessmentOpt =
                affordabilityService.checkAssessmentRequired(playerId);

            if (assessmentOpt.isDefined()) {
                violations.add(RuleViolation.of("AFFORDABILITY_CHECK_REQUIRED",
                    "需要完成財務評估才能繼續存款"));
            }
        }

        return DepositRuleResult.builder()
            .passed(violations.isEmpty())
            .violations(violations)
            .build();
    }

    /**
     * 遊戲啟動時執行牌照特定規則
     */
    public GameLaunchRuleResult executeGameLaunchRules(
            Long playerId,
            String gameId,
            JurisdictionConfig config) {

        List<RuleViolation> violations = new ArrayList<>();

        // 遊戲類型檢查
        String gameType = gameService.getGameType(gameId);
        if (config.getBlockedGameTypes().contains(gameType)) {
            violations.add(RuleViolation.of("GAME_TYPE_BLOCKED",
                "此遊戲類型在您的地區不可用"));
        }

        // 自我排除檢查
        if (selfExclusionService.isExcluded(playerId)) {
            violations.add(RuleViolation.of("SELF_EXCLUDED",
                "您已設定自我排除，無法進行遊戲"));
        }

        // 冷靜期檢查
        if (coolingOffService.isInCoolingOff(playerId)) {
            violations.add(RuleViolation.of("COOLING_OFF_ACTIVE",
                "您正處於冷靜期，無法進行遊戲"));
        }

        return GameLaunchRuleResult.builder()
            .passed(violations.isEmpty())
            .violations(violations)
            .gameConfig(buildGameConfig(config))
            .build();
    }

    private int getMinimumAge(JurisdictionConfig config) {
        return switch (config.getJurisdictionCode()) {
            case "UKGC", "MGA" -> 18;
            case "PAGCOR" -> 21;
            default -> 18;
        };
    }
}
```

### 地理圍欄服務

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class GeoFenceService {

    private final GeoIPService geoIPService;
    private final JurisdictionConfigDao configDao;

    /**
     * 驗證 IP 是否在允許的地理範圍內
     */
    public GeoFenceResult validateAccess(String clientIp, String jurisdictionCode) {
        GeoIPResult geoResult = geoIPService.lookup(clientIp);
        JurisdictionConfig config = configDao.findByCode(jurisdictionCode);

        // 檢查 VPN/Proxy
        if (geoResult.isVpn() || geoResult.isProxy()) {
            return GeoFenceResult.blocked("PROXY_DETECTED",
                "檢測到 VPN/Proxy，請關閉後重試");
        }

        // 檢查國家
        String countryCode = geoResult.getCountryCode();
        if (config.getBlockedCountries().contains(countryCode)) {
            return GeoFenceResult.blocked("COUNTRY_BLOCKED",
                "您所在的地區無法使用此服務");
        }

        // 檢查州/省（美國等地區）
        if ("US".equals(countryCode)) {
            String stateCode = geoResult.getStateCode();
            if (!isStateAllowed(stateCode, config)) {
                return GeoFenceResult.blocked("STATE_BLOCKED",
                    "您所在的州無法使用此服務");
            }
        }

        return GeoFenceResult.allowed(geoResult);
    }

    /**
     * 定期驗證玩家位置
     */
    @Scheduled(fixedRate = 300000) // 每 5 分鐘
    public void validateActivePlayerLocations() {
        List<ActiveSession> sessions = sessionService.getActiveSessions();

        for (ActiveSession session : sessions) {
            String currentIp = session.getLastKnownIp();
            String registeredJurisdiction = session.getJurisdictionCode();

            GeoFenceResult result = validateAccess(currentIp, registeredJurisdiction);

            if (!result.isAllowed()) {
                log.warn("Player location violation: playerId={}, ip={}, reason={}",
                    session.getPlayerId(), currentIp, result.getReason());

                // 終止會話
                sessionService.terminateSession(session.getPlayerId(),
                    "GEO_FENCE_VIOLATION: " + result.getReason());

                // 通知玩家
                notificationService.sendGeoFenceAlert(session.getPlayerId());
            }
        }
    }
}
```

---

## 合規 Dashboard

### 前端實現

```vue
<template>
  <div class="compliance-dashboard">
    <a-row :gutter="16">
      <!-- 牌照狀態卡片 -->
      <a-col :span="6" v-for="license in licenses" :key="license.code">
        <a-card :class="getCardClass(license)">
          <template #title>
            <div class="license-header">
              <img :src="license.logo" :alt="license.displayName" />
              <span>{{ license.displayName }}</span>
            </div>
          </template>

          <a-descriptions :column="1" size="small">
            <a-descriptions-item label="活躍玩家">
              {{ license.activePlayers.toLocaleString() }}
            </a-descriptions-item>
            <a-descriptions-item label="合規狀態">
              <a-tag :color="getStatusColor(license.complianceStatus)">
                {{ license.complianceStatus }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="下次報告">
              {{ formatDate(license.nextReportDue) }}
            </a-descriptions-item>
          </a-descriptions>

          <template #actions>
            <a-button type="link" @click="viewDetails(license)">
              查看詳情
            </a-button>
          </template>
        </a-card>
      </a-col>
    </a-row>

    <!-- 合規告警 -->
    <a-card title="合規告警" class="mt-4">
      <a-list :data-source="alerts" :loading="loading">
        <template #renderItem="{ item }">
          <a-list-item>
            <a-list-item-meta>
              <template #title>
                <a-tag :color="item.severity === 'CRITICAL' ? 'red' : 'orange'">
                  {{ item.jurisdiction }}
                </a-tag>
                {{ item.title }}
              </template>
              <template #description>
                {{ item.description }} | {{ formatTime(item.createdAt) }}
              </template>
            </a-list-item-meta>
            <template #actions>
              <a-button type="link" @click="handleAlert(item)">
                處理
              </a-button>
            </template>
          </a-list-item>
        </template>
      </a-list>
    </a-card>
  </div>
</template>
```

---

## 相關文檔

- [06-08_UKGC_Compliance.md](06-08_UKGC_Compliance.md) - UK 合規
- [06-09_MGA_Compliance.md](06-09_MGA_Compliance.md) - Malta 合規
- [06-10_Brazil_SPA_Compliance.md](06-10_Brazil_SPA_Compliance.md) - Brazil 合規
- [15-01_Self_Exclusion.md](../15_Responsible_Gambling/15-01_Self_Exclusion.md) - 自我排除

---

**返回**: [平台治理](README.md) | [iGaming 首頁](../README.md)
