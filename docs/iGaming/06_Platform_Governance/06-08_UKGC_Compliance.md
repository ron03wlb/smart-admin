# 06-08 UKGC Compliance (UK Gambling Commission 合規)

**版本**: 1.0.0
**創建日期**: 2026-02-07
**狀態**: ✅ 完整

---

## 概述

UK Gambling Commission (UKGC) 是全球最嚴格的博彩監管機構之一。本文檔詳細說明 UK 牌照的合規要求。

### UKGC 監管框架

| 法規 | 說明 |
|------|------|
| **Gambling Act 2005** | 主要立法 |
| **LCCP** | License Conditions and Codes of Practice |
| **RTS** | Remote Technical Standards |
| **AML Guidance** | 反洗錢指南 |

---

## 2025 新規重點

### 即時 KYC

**生效日期**: 2025年1月

| 舊規 | 新規 |
|------|------|
| 72 小時寬限期 | **立即驗證** |
| 可先存款後驗證 | **驗證後才能存款** |

### 可負擔性評估

| 觸發條件 | 要求動作 |
|---------|---------|
| 淨虧損 £125-£500 | 顯示警告 |
| 淨虧損 £500-£2,000 | 玩家自我聲明 |
| 淨虧損 > £2,000 | 第三方數據驗證 |

### Gambling Levy

**生效日期**: 2025年4月6日

| GGY 範圍 | 稅率 |
|---------|------|
| £0 - £10M | 0.1% |
| £10M - £100M | 0.4% |
| £100M - £1B | 0.8% |
| > £1B | 1.1% |

---

## LCCP 社會責任條款

### SR 3.4 - 玩家保護

| 條款 | 要求 | 實現文檔 |
|------|------|---------|
| SR 3.4.1 | 存款限額選項 | [15-02](../15_Responsible_Gambling/15-02_Deposit_Limits.md) |
| SR 3.4.2 | 現實檢查 | [15-05](../15_Responsible_Gambling/15-05_Reality_Checks.md) |
| SR 3.4.3 | 活動聲明 | [15-07](../15_Responsible_Gambling/15-07_Player_Protection_API.md) |

### SR 3.5 - 自我排除

| 條款 | 要求 | 實現文檔 |
|------|------|---------|
| SR 3.5.1 | 自我排除機制 | [15-01](../15_Responsible_Gambling/15-01_Self_Exclusion.md) |
| SR 3.5.2 | Gamstop 整合 | 本文檔 |
| SR 3.5.3 | 排除期間服務 | [15-01](../15_Responsible_Gambling/15-01_Self_Exclusion.md) |

---

## Gamstop 整合

### 概述

Gamstop 是 UK 的全國性自我排除計劃，所有持有 UKGC 牌照的運營商必須參與。

### API 整合

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class GamstopIntegration {

    @Value("${gamstop.api.url}")
    private String apiUrl;

    @Value("${gamstop.api.key}")
    private String apiKey;

    @Value("${gamstop.operator.id}")
    private String operatorId;

    private final RestTemplate restTemplate;
    private final GamstopSyncLogDao syncLogDao;

    /**
     * 註冊時檢查玩家是否在 Gamstop 名單
     *
     * 必須在允許存款前調用
     */
    public GamstopCheckResult checkExclusion(GamstopCheckRequest request) {
        HttpHeaders headers = createHeaders();
        HttpEntity<GamstopCheckRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<GamstopCheckResponse> response = restTemplate.exchange(
                apiUrl + "/exclusion/check",
                HttpMethod.POST,
                entity,
                GamstopCheckResponse.class
            );

            GamstopCheckResponse body = response.getBody();

            // 記錄檢查
            GamstopSyncLog log = GamstopSyncLog.builder()
                .playerId(request.getPlayerId())
                .syncType("CHECK")
                .requestPayload(JsonUtil.toJson(request))
                .responsePayload(JsonUtil.toJson(body))
                .gamstopReference(body.getReference())
                .status(body.isExcluded() ? "EXCLUDED" : "CLEAR")
                .build();
            syncLogDao.insert(log);

            return GamstopCheckResult.builder()
                .excluded(body.isExcluded())
                .exclusionEndDate(body.getExclusionEndDate())
                .reference(body.getReference())
                .build();

        } catch (Exception e) {
            log.error("Gamstop check failed", e);

            // 記錄失敗
            GamstopSyncLog log = GamstopSyncLog.builder()
                .playerId(request.getPlayerId())
                .syncType("CHECK")
                .status("FAILED")
                .errorMessage(e.getMessage())
                .build();
            syncLogDao.insert(log);

            // Gamstop 不可用時的處理策略
            // 選項 1: 拒絕服務（最安全）
            // 選項 2: 允許繼續，記錄待後續驗證
            throw new GamstopUnavailableException("Gamstop 服務暫時不可用", e);
        }
    }

    /**
     * 定期同步 Gamstop 狀態
     */
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨 2 點
    public void syncGamstopStatus() {
        List<Player> ukPlayers = playerDao.findByJurisdiction("UKGC");

        for (Player player : ukPlayers) {
            try {
                GamstopCheckRequest request = buildCheckRequest(player);
                GamstopCheckResult result = checkExclusion(request);

                if (result.isExcluded() && !player.isExcluded()) {
                    // 玩家新被加入 Gamstop，需要排除
                    selfExclusionService.excludeFromGamstop(
                        player.getId(),
                        result.getExclusionEndDate()
                    );
                }
            } catch (Exception e) {
                log.error("Failed to sync Gamstop for player: {}",
                    player.getId(), e);
            }
        }
    }

    /**
     * 向 Gamstop 註冊排除
     */
    public GamstopRegistrationResult registerExclusion(
            Long playerId,
            GamstopRegistrationRequest request) {

        HttpHeaders headers = createHeaders();
        HttpEntity<GamstopRegistrationRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<GamstopRegistrationResponse> response = restTemplate.exchange(
            apiUrl + "/exclusion/register",
            HttpMethod.POST,
            entity,
            GamstopRegistrationResponse.class
        );

        GamstopRegistrationResponse body = response.getBody();

        // 記錄
        GamstopSyncLog log = GamstopSyncLog.builder()
            .playerId(playerId)
            .syncType("REGISTER")
            .gamstopReference(body.getReference())
            .status("SUCCESS")
            .build();
        syncLogDao.insert(log);

        return GamstopRegistrationResult.builder()
            .success(true)
            .reference(body.getReference())
            .build();
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Operator-ID", operatorId);
        headers.set("X-API-Key", apiKey);
        return headers;
    }
}
```

---

## RTS 技術標準

### RTS 4 - 安全要求

| 條款 | 要求 | SmartAdmin 實現 |
|------|------|----------------|
| 4.1 | ISO 27001 | [12-04](../12_System_Security/12-04_ISO27001_2022_Mapping.md) |
| 4.2 | 環境分離 | 開發/測試/生產分離 |
| 4.3 | 通訊安全 | TLS 1.3 |
| 4.4 | 訪問控制 | [06-02](06-02_RBAC_Permissions.md) |

### RTS 5 - 遊戲要求

| 條款 | 要求 | 實現文檔 |
|------|------|---------|
| 5.1 | RNG 認證 | [03-05](../03_Game_Center/03-05_GLI_Certification.md) |
| 5.2 | 遊戲規則顯示 | 遊戲內說明 |
| 5.3 | RTP 顯示 | [03-07](../03_Game_Center/03-07_RTP_Monitoring.md) |

---

## 報告要求

### 月度報告

```java
@Service
@RequiredArgsConstructor
public class UKGCReportingService {

    /**
     * 生成 UKGC 月度報告
     */
    public UKGCMonthlyReport generateMonthlyReport(YearMonth month) {
        LocalDateTime startTime = month.atDay(1).atStartOfDay();
        LocalDateTime endTime = month.plusMonths(1).atDay(1).atStartOfDay();

        return UKGCMonthlyReport.builder()
            .reportMonth(month)
            .generatedAt(LocalDateTime.now())

            // 玩家保護統計
            .playerProtection(PlayerProtectionStats.builder()
                .selfExclusionsNew(countNewExclusions(startTime, endTime))
                .selfExclusionsActive(countActiveExclusions(endTime))
                .depositLimitsSet(countPlayersWithDepositLimits())
                .realityChecksTriggered(countRealityChecks(startTime, endTime))
                .affordabilityAssessments(countAffordabilityAssessments(startTime, endTime))
                .build())

            // 財務統計
            .financials(FinancialStats.builder()
                .grossGamingRevenue(calculateGGR(startTime, endTime))
                .gamblingLevy(calculateGamblingLevy(startTime, endTime))
                .build())

            // 遊戲統計
            .gaming(GamingStats.builder()
                .totalRounds(countTotalRounds(startTime, endTime))
                .uniquePlayers(countUniquePlayers(startTime, endTime))
                .averageRTP(calculateAverageRTP(startTime, endTime))
                .build())

            // AML 統計
            .aml(AMLStats.builder()
                .sarsSubmitted(countSARs(startTime, endTime))
                .enhancedDueDiligence(countEDD(startTime, endTime))
                .build())

            .build();
    }

    /**
     * 生成 Gambling Levy 報告
     */
    public GamblingLevyReport generateLevyReport(YearMonth month) {
        BigDecimal ggr = calculateGGR(
            month.atDay(1).atStartOfDay(),
            month.plusMonths(1).atDay(1).atStartOfDay()
        );

        BigDecimal levyRate = determineLevyRate(ggr);
        BigDecimal levyAmount = ggr.multiply(levyRate);

        return GamblingLevyReport.builder()
            .reportMonth(month)
            .grossGamingRevenue(ggr)
            .levyRate(levyRate)
            .levyAmount(levyAmount)
            .paymentDue(month.plusMonths(1).atDay(28).atStartOfDay())
            .build();
    }

    private BigDecimal determineLevyRate(BigDecimal annualGGR) {
        if (annualGGR.compareTo(new BigDecimal("10000000")) < 0) {
            return new BigDecimal("0.001"); // 0.1%
        } else if (annualGGR.compareTo(new BigDecimal("100000000")) < 0) {
            return new BigDecimal("0.004"); // 0.4%
        } else if (annualGGR.compareTo(new BigDecimal("1000000000")) < 0) {
            return new BigDecimal("0.008"); // 0.8%
        } else {
            return new BigDecimal("0.011"); // 1.1%
        }
    }
}
```

---

## 合規檢查清單

### 玩家保護

- [ ] 即時 KYC 驗證
- [ ] Gamstop 整合
- [ ] 自我排除系統
- [ ] 存款限額
- [ ] 現實檢查
- [ ] 可負擔性評估

### 遊戲

- [ ] RNG GLI 認證
- [ ] RTP 顯示
- [ ] 遊戲規則說明
- [ ] 遊戲歷史記錄

### 安全

- [ ] ISO 27001 合規
- [ ] TLS 1.3
- [ ] 環境分離
- [ ] 訪問控制審計

### 財務

- [ ] GGR 計算
- [ ] Gambling Levy 繳納
- [ ] 月度報告

---

## 相關文檔

- [06-07_Multi_Jurisdiction_Framework.md](06-07_Multi_Jurisdiction_Framework.md) - 多牌照框架
- [15-01_Self_Exclusion.md](../15_Responsible_Gambling/15-01_Self_Exclusion.md) - 自我排除
- [15-08_Affordability_Assessment.md](../15_Responsible_Gambling/15-08_Affordability_Assessment.md) - 可負擔性評估

---

**返回**: [平台治理](README.md) | [iGaming 首頁](../README.md)
