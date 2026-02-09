# 03-07 RTP Monitoring (RTP 實時監控)

**版本**: 1.0.0
**創建日期**: 2026-02-07
**狀態**: ✅ 完整

---

## 概述

RTP (Return To Player) 監控系統用於實時追蹤遊戲的實際返還率，確保其符合理論值，並在異常時觸發告警。

### 監管要求

| 監管機構 | 要求 | 說明 |
|---------|------|------|
| **UKGC** | RTP 公開 | 必須在遊戲內顯示理論 RTP |
| **MGA** | 定期報告 | 月度 RTP 報告 |
| **GLI-19** | 偏差 ≤ 0.5% | 長期實際 RTP 與理論值偏差 |

---

## RTP 計算方法

### 基本公式

```
RTP = (Total Payout / Total Wagered) × 100%

其中：
- Total Payout: 所有獎金總額（含 Jackpot）
- Total Wagered: 所有投注總額
```

### 時間維度

| 維度 | 計算週期 | 用途 |
|------|---------|------|
| 即時 RTP | 最近 1000 回合 | 短期異常檢測 |
| 日 RTP | 過去 24 小時 | 日報表 |
| 週 RTP | 過去 7 天 | 週報表 |
| 月 RTP | 過去 30 天 | 月度合規報告 |
| 累計 RTP | 遊戲上線至今 | 長期公平性驗證 |

### 統計置信度

由於 RTP 具有隨機性，短期偏差是正常的。使用以下標準判斷是否異常：

| 樣本量 | 95% 置信區間 | 99% 置信區間 |
|--------|-------------|-------------|
| 1,000 回合 | ±3.0% | ±4.0% |
| 10,000 回合 | ±0.95% | ±1.25% |
| 100,000 回合 | ±0.30% | ±0.40% |
| 1,000,000 回合 | ±0.095% | ±0.125% |

---

## 技術實現

### 資料模型

```sql
-- 遊戲 RTP 配置
CREATE TABLE t_game_rtp_config (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    game_id             VARCHAR(50) NOT NULL UNIQUE,
    game_name           VARCHAR(100) NOT NULL,
    game_type           VARCHAR(30) NOT NULL,

    -- 理論 RTP
    theoretical_rtp     DECIMAL(6,3) NOT NULL,  -- 如 96.500
    rtp_variance        DECIMAL(6,3),           -- 波動性指標

    -- 組成部分
    base_game_rtp       DECIMAL(6,3),
    free_spins_rtp      DECIMAL(6,3),
    bonus_feature_rtp   DECIMAL(6,3),
    jackpot_rtp         DECIMAL(6,3),

    -- 告警閾值
    alert_threshold     DECIMAL(4,2) DEFAULT 1.00,  -- 偏差超過 1% 告警
    critical_threshold  DECIMAL(4,2) DEFAULT 2.00,  -- 偏差超過 2% 嚴重

    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- RTP 統計快照
CREATE TABLE t_rtp_statistics (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    game_id             VARCHAR(50) NOT NULL,
    period_type         VARCHAR(20) NOT NULL,  -- HOURLY, DAILY, WEEKLY, MONTHLY
    period_start        DATETIME NOT NULL,
    period_end          DATETIME NOT NULL,

    -- 統計
    total_rounds        BIGINT NOT NULL DEFAULT 0,
    total_wagered       DECIMAL(20,4) NOT NULL DEFAULT 0,
    total_payout        DECIMAL(20,4) NOT NULL DEFAULT 0,
    actual_rtp          DECIMAL(6,3),

    -- 與理論值比較
    theoretical_rtp     DECIMAL(6,3) NOT NULL,
    rtp_deviation       DECIMAL(6,3),  -- actual - theoretical

    -- 置信區間
    ci_lower            DECIMAL(6,3),
    ci_upper            DECIMAL(6,3),
    confidence_level    DECIMAL(4,2) DEFAULT 0.95,

    -- 狀態
    status              VARCHAR(20) DEFAULT 'NORMAL',  -- NORMAL, WARNING, CRITICAL

    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_game_period (game_id, period_type, period_start),
    INDEX idx_period_start (period_start)
);

-- RTP 異常告警
CREATE TABLE t_rtp_alert (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    game_id             VARCHAR(50) NOT NULL,
    alert_type          VARCHAR(20) NOT NULL,  -- HIGH_RTP, LOW_RTP, UNUSUAL_PATTERN
    severity            VARCHAR(20) NOT NULL,  -- WARNING, CRITICAL
    triggered_at        DATETIME NOT NULL,

    -- 詳情
    actual_rtp          DECIMAL(6,3),
    theoretical_rtp     DECIMAL(6,3),
    deviation           DECIMAL(6,3),
    sample_size         BIGINT,
    period_start        DATETIME,
    period_end          DATETIME,

    -- 處理
    acknowledged        BOOLEAN DEFAULT FALSE,
    acknowledged_by     BIGINT,
    acknowledged_at     DATETIME,
    resolution          VARCHAR(500),

    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_game_id (game_id),
    INDEX idx_triggered_at (triggered_at),
    INDEX idx_acknowledged (acknowledged)
);
```

### 核心服務實現

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class RTPMonitoringService {

    private final RTPConfigDao rtpConfigDao;
    private final RTPStatisticsDao statisticsDao;
    private final RTPAlertDao alertDao;
    private final GameRoundAuditDao gameAuditDao;

    /**
     * 計算遊戲 RTP
     */
    public RTPCalculationResult calculateRTP(String gameId, LocalDateTime startTime, LocalDateTime endTime) {
        // 獲取理論 RTP
        GameRTPConfig config = rtpConfigDao.findByGameId(gameId);
        if (config == null) {
            throw new GameConfigNotFoundException(gameId);
        }

        // 聚合統計
        RTPAggregation agg = gameAuditDao.aggregateRTP(gameId, startTime, endTime);

        if (agg.getTotalRounds() == 0) {
            return RTPCalculationResult.noData(gameId);
        }

        // 計算實際 RTP
        BigDecimal actualRTP = agg.getTotalPayout()
            .divide(agg.getTotalWagered(), 6, RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"));

        // 計算偏差
        BigDecimal deviation = actualRTP.subtract(config.getTheoreticalRTP());

        // 計算置信區間
        ConfidenceInterval ci = calculateConfidenceInterval(
            agg.getTotalRounds(),
            actualRTP,
            0.95
        );

        // 判斷狀態
        RTPStatus status = determineStatus(deviation, config);

        return RTPCalculationResult.builder()
            .gameId(gameId)
            .periodStart(startTime)
            .periodEnd(endTime)
            .totalRounds(agg.getTotalRounds())
            .totalWagered(agg.getTotalWagered())
            .totalPayout(agg.getTotalPayout())
            .actualRTP(actualRTP)
            .theoreticalRTP(config.getTheoreticalRTP())
            .deviation(deviation)
            .confidenceInterval(ci)
            .status(status)
            .build();
    }

    /**
     * 定時更新 RTP 統計
     */
    @Scheduled(fixedRate = 300000) // 每 5 分鐘
    public void updateRTPStatistics() {
        List<GameRTPConfig> games = rtpConfigDao.findAllActive();

        for (GameRTPConfig config : games) {
            try {
                updateGameRTPStatistics(config);
            } catch (Exception e) {
                log.error("Failed to update RTP for game: {}", config.getGameId(), e);
            }
        }
    }

    private void updateGameRTPStatistics(GameRTPConfig config) {
        LocalDateTime now = LocalDateTime.now();

        // 更新各時間維度的統計
        updatePeriodStatistics(config, PeriodType.HOURLY,
            now.minusHours(1), now);
        updatePeriodStatistics(config, PeriodType.DAILY,
            now.minusDays(1), now);
        updatePeriodStatistics(config, PeriodType.WEEKLY,
            now.minusWeeks(1), now);
        updatePeriodStatistics(config, PeriodType.MONTHLY,
            now.minusMonths(1), now);

        // 即時 RTP（最近 1000 回合）
        updateRecentRTPStatistics(config, 1000);
    }

    private void updatePeriodStatistics(GameRTPConfig config, PeriodType periodType,
                                       LocalDateTime startTime, LocalDateTime endTime) {
        RTPCalculationResult result = calculateRTP(config.getGameId(), startTime, endTime);

        if (result.getTotalRounds() == 0) {
            return;
        }

        RTPStatistics stats = RTPStatistics.builder()
            .gameId(config.getGameId())
            .periodType(periodType)
            .periodStart(startTime)
            .periodEnd(endTime)
            .totalRounds(result.getTotalRounds())
            .totalWagered(result.getTotalWagered())
            .totalPayout(result.getTotalPayout())
            .actualRTP(result.getActualRTP())
            .theoreticalRTP(result.getTheoreticalRTP())
            .rtpDeviation(result.getDeviation())
            .ciLower(result.getConfidenceInterval().getLower())
            .ciUpper(result.getConfidenceInterval().getUpper())
            .confidenceLevel(new BigDecimal("0.95"))
            .status(result.getStatus())
            .build();

        statisticsDao.saveOrUpdate(stats);

        // 檢查是否需要告警
        if (result.getStatus() != RTPStatus.NORMAL) {
            triggerAlert(config, result);
        }
    }

    /**
     * 觸發 RTP 告警
     */
    private void triggerAlert(GameRTPConfig config, RTPCalculationResult result) {
        AlertType alertType;
        if (result.getDeviation().compareTo(BigDecimal.ZERO) > 0) {
            alertType = AlertType.HIGH_RTP;
        } else {
            alertType = AlertType.LOW_RTP;
        }

        Severity severity = result.getStatus() == RTPStatus.WARNING ?
            Severity.WARNING : Severity.CRITICAL;

        RTPAlert alert = RTPAlert.builder()
            .gameId(config.getGameId())
            .alertType(alertType)
            .severity(severity)
            .triggeredAt(LocalDateTime.now())
            .actualRTP(result.getActualRTP())
            .theoreticalRTP(result.getTheoreticalRTP())
            .deviation(result.getDeviation())
            .sampleSize(result.getTotalRounds())
            .periodStart(result.getPeriodStart())
            .periodEnd(result.getPeriodEnd())
            .build();

        alertDao.insert(alert);

        // 發送通知
        notificationService.sendRTPAlert(alert);

        log.warn("RTP Alert triggered: game={}, type={}, deviation={}%",
            config.getGameId(), alertType, result.getDeviation());
    }

    /**
     * 計算置信區間
     */
    private ConfidenceInterval calculateConfidenceInterval(
            long sampleSize,
            BigDecimal actualRTP,
            double confidenceLevel) {

        double p = actualRTP.doubleValue() / 100;
        double n = sampleSize;

        // 標準誤差
        double se = Math.sqrt(p * (1 - p) / n);

        // z 分數
        double z = confidenceLevel == 0.99 ? 2.576 : 1.96;

        double lower = (p - z * se) * 100;
        double upper = (p + z * se) * 100;

        return ConfidenceInterval.builder()
            .lower(new BigDecimal(lower).setScale(3, RoundingMode.HALF_UP))
            .upper(new BigDecimal(upper).setScale(3, RoundingMode.HALF_UP))
            .confidence(confidenceLevel)
            .build();
    }

    /**
     * 判斷 RTP 狀態
     */
    private RTPStatus determineStatus(BigDecimal deviation, GameRTPConfig config) {
        BigDecimal absDeviation = deviation.abs();

        if (absDeviation.compareTo(config.getCriticalThreshold()) >= 0) {
            return RTPStatus.CRITICAL;
        } else if (absDeviation.compareTo(config.getAlertThreshold()) >= 0) {
            return RTPStatus.WARNING;
        }
        return RTPStatus.NORMAL;
    }

    /**
     * 獲取 RTP 儀表板數據
     */
    public RTPDashboardVO getDashboard() {
        List<GameRTPConfig> games = rtpConfigDao.findAllActive();
        LocalDateTime now = LocalDateTime.now();

        List<GameRTPSummaryVO> summaries = new ArrayList<>();
        int normalCount = 0, warningCount = 0, criticalCount = 0;

        for (GameRTPConfig config : games) {
            RTPStatistics dailyStats = statisticsDao.findLatest(
                config.getGameId(), PeriodType.DAILY);

            if (dailyStats != null) {
                GameRTPSummaryVO summary = GameRTPSummaryVO.builder()
                    .gameId(config.getGameId())
                    .gameName(config.getGameName())
                    .theoreticalRTP(config.getTheoreticalRTP())
                    .actualRTP(dailyStats.getActualRTP())
                    .deviation(dailyStats.getRtpDeviation())
                    .status(dailyStats.getStatus())
                    .totalRounds(dailyStats.getTotalRounds())
                    .build();

                summaries.add(summary);

                switch (dailyStats.getStatus()) {
                    case NORMAL -> normalCount++;
                    case WARNING -> warningCount++;
                    case CRITICAL -> criticalCount++;
                }
            }
        }

        // 獲取最近告警
        List<RTPAlert> recentAlerts = alertDao.findRecent(10);

        return RTPDashboardVO.builder()
            .games(summaries)
            .normalCount(normalCount)
            .warningCount(warningCount)
            .criticalCount(criticalCount)
            .recentAlerts(recentAlerts.stream()
                .map(this::convertToAlertVO)
                .collect(Collectors.toList()))
            .lastUpdated(now)
            .build();
    }

    /**
     * 生成 RTP 合規報告
     */
    public RTPComplianceReport generateComplianceReport(LocalDate month) {
        LocalDateTime startTime = month.atStartOfDay();
        LocalDateTime endTime = month.plusMonths(1).atStartOfDay();

        List<GameRTPConfig> games = rtpConfigDao.findAllActive();
        List<GameRTPReportVO> gameReports = new ArrayList<>();

        for (GameRTPConfig config : games) {
            RTPCalculationResult result = calculateRTP(
                config.getGameId(), startTime, endTime);

            if (result.getTotalRounds() > 0) {
                GameRTPReportVO report = GameRTPReportVO.builder()
                    .gameId(config.getGameId())
                    .gameName(config.getGameName())
                    .theoreticalRTP(config.getTheoreticalRTP())
                    .actualRTP(result.getActualRTP())
                    .deviation(result.getDeviation())
                    .totalRounds(result.getTotalRounds())
                    .totalWagered(result.getTotalWagered())
                    .totalPayout(result.getTotalPayout())
                    .confidenceInterval(result.getConfidenceInterval())
                    .compliant(result.getDeviation().abs()
                        .compareTo(new BigDecimal("0.5")) <= 0)
                    .build();

                gameReports.add(report);
            }
        }

        return RTPComplianceReport.builder()
            .reportMonth(month)
            .generatedAt(LocalDateTime.now())
            .games(gameReports)
            .overallCompliant(gameReports.stream().allMatch(GameRTPReportVO::isCompliant))
            .build();
    }
}
```

---

## 監控儀表板

### 前端實現

```vue
<template>
  <div class="rtp-dashboard">
    <!-- 摘要卡片 -->
    <a-row :gutter="16" class="mb-4">
      <a-col :span="8">
        <a-statistic
          title="正常"
          :value="dashboard.normalCount"
          :value-style="{ color: '#52c41a' }"
        >
          <template #prefix><CheckCircleOutlined /></template>
        </a-statistic>
      </a-col>
      <a-col :span="8">
        <a-statistic
          title="警告"
          :value="dashboard.warningCount"
          :value-style="{ color: '#faad14' }"
        >
          <template #prefix><WarningOutlined /></template>
        </a-statistic>
      </a-col>
      <a-col :span="8">
        <a-statistic
          title="嚴重"
          :value="dashboard.criticalCount"
          :value-style="{ color: '#ff4d4f' }"
        >
          <template #prefix><CloseCircleOutlined /></template>
        </a-statistic>
      </a-col>
    </a-row>

    <!-- 遊戲 RTP 表格 -->
    <a-table :columns="columns" :data-source="dashboard.games">
      <template #deviation="{ record }">
        <span :class="getDeviationClass(record.deviation)">
          {{ record.deviation > 0 ? '+' : '' }}{{ record.deviation.toFixed(2) }}%
        </span>
      </template>

      <template #status="{ record }">
        <a-tag :color="getStatusColor(record.status)">
          {{ record.status }}
        </a-tag>
      </template>
    </a-table>

    <!-- 最近告警 -->
    <a-card title="最近告警" class="mt-4">
      <a-list
        :data-source="dashboard.recentAlerts"
        item-layout="horizontal"
      >
        <template #renderItem="{ item }">
          <a-list-item>
            <a-list-item-meta>
              <template #title>
                <a-tag :color="item.severity === 'CRITICAL' ? 'red' : 'orange'">
                  {{ item.alertType }}
                </a-tag>
                {{ item.gameName }}
              </template>
              <template #description>
                偏差: {{ item.deviation.toFixed(2) }}% |
                樣本: {{ item.sampleSize.toLocaleString() }} 回合 |
                時間: {{ formatTime(item.triggeredAt) }}
              </template>
            </a-list-item-meta>
          </a-list-item>
        </template>
      </a-list>
    </a-card>
  </div>
</template>
```

---

## 告警規則

| 條件 | 嚴重程度 | 動作 |
|------|---------|------|
| 日 RTP 偏差 > 1% | WARNING | 郵件通知 |
| 日 RTP 偏差 > 2% | CRITICAL | 郵件 + Slack + 短信 |
| 連續 3 天偏差同方向 | WARNING | 調查提示 |
| 即時 RTP 偏差 > 5% | CRITICAL | 自動暫停遊戲 |

---

## 相關文檔

- [03-05_GLI_Certification.md](03-05_GLI_Certification.md) - GLI 認證
- [03-06_Game_Audit_Trail.md](03-06_Game_Audit_Trail.md) - 遊戲審計
- [08-01_Reporting_BI.md](../08_Analytics_BI/08-01_Reporting_BI.md) - 報表系統

---

**返回**: [遊戲中心](README.md) | [iGaming 首頁](../README.md)
