# 05-02-04 運營工具 (Operations Tools)

## 文檔信息

| 屬性 | 值 |
|------|-----|
| **文檔版本** | 4.0.0 |
| **最後更新** | 2026-02-07 |
| **父文檔** | [05-02 欺詐檢測](./05-02_Fraud_Detection.md) |
| **維護團隊** | Risk Team & Backend Team |

---

## 1. SmartAdmin 架構映射

### 1.1 Entity 層 - 風控提案實體

```java
package net.lab1024.sa.business.risk.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import net.lab1024.sa.common.mybatis.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 風控提案實體
 *
 * @author Risk Team
 * @version 2.1.0
 * @since 2026-02-02
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_risk_proposal")
public class RiskProposalEntity extends BaseEntity {

    /**
     * 提案編號 (如 RP-20260202-123456)
     */
    @TableField("proposal_no")
    private String proposalNo;

    /**
     * 注單 ID
     */
    @TableField("bet_id")
    private String betId;

    /**
     * 玩家 ID
     */
    @TableField("player_id")
    private Long playerId;

    /**
     * 取款請求 ID (若關聯取款)
     */
    @TableField("withdrawal_request_id")
    private Long withdrawalRequestId;

    /**
     * 匹配的風控規則列表 (JSON 存儲)
     */
    @TableField("matched_rules")
    private List<String> matchedRules;

    /**
     * 標記原因列表 (JSON 存儲)
     */
    @TableField("flagged_reasons")
    private List<String> flaggedReasons;

    /**
     * 可疑金額
     */
    @TableField("suspicious_amount")
    private BigDecimal suspiciousAmount;

    /**
     * 提案狀態 (PENDING_REVIEW/APPROVED/REJECTED/PARTIAL_APPROVED)
     */
    @TableField("status")
    private String status;

    /**
     * 優先級 (LOW/MEDIUM/HIGH/URGENT)
     */
    @TableField("priority")
    private String priority;

    /**
     * 審核員 ID
     */
    @TableField("reviewer_id")
    private Long reviewerId;

    /**
     * 審核時間
     */
    @TableField("reviewed_at")
    private LocalDateTime reviewedAt;

    /**
     * 審核備註
     */
    @TableField("review_notes")
    private String reviewNotes;

    /**
     * 批准金額（部分批准場景）
     */
    @TableField("approved_amount")
    private BigDecimal approvedAmount;

    /**
     * 補償類型 (REFUND/ADJUSTMENT/DEDUCTION)
     */
    @TableField("compensation_type")
    private String compensationType;

    /**
     * 補償金額
     */
    @TableField("compensation_amount")
    private BigDecimal compensationAmount;

    /**
     * 補償執行時間
     */
    @TableField("compensation_executed_at")
    private LocalDateTime compensationExecutedAt;
}
```

### 1.2 Dao 層 - 數據訪問

```java
package net.lab1024.sa.business.risk.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import net.lab1024.sa.business.risk.domain.entity.RiskProposalEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 風控提案 Dao
 *
 * @author Risk Team
 * @version 2.1.0
 * @since 2026-02-02
 */
@Mapper
public interface RiskProposalDao extends BaseMapper<RiskProposalEntity> {

    /**
     * 查詢玩家的待審核提案 (MyBatis-Plus 動態查詢)
     *
     * @param playerId 玩家 ID
     * @param status 提案狀態
     * @param startDate 開始日期
     * @return List<RiskProposalEntity> 提案列表
     */
    default List<RiskProposalEntity> selectByPlayerIdAndStatus(
        Long playerId,
        String status,
        LocalDateTime startDate
    ) {
        return this.selectList(new LambdaQueryWrapper<RiskProposalEntity>()
            .eq(RiskProposalEntity::getPlayerId, playerId)
            .eq(RiskProposalEntity::getStatus, status)
            .ge(RiskProposalEntity::getCreatedAt, startDate)
            .eq(RiskProposalEntity::getDeleted, false)
            .orderByDesc(RiskProposalEntity::getCreatedAt)
        );
    }

    /**
     * 統計玩家的風控提案數量
     *
     * @param playerId 玩家 ID
     * @param startDate 開始日期
     * @return int 提案數量
     */
    default int countByPlayerId(Long playerId, LocalDateTime startDate) {
        return Math.toIntExact(this.selectCount(new LambdaQueryWrapper<RiskProposalEntity>()
            .eq(RiskProposalEntity::getPlayerId, playerId)
            .ge(RiskProposalEntity::getCreatedAt, startDate)
            .eq(RiskProposalEntity::getDeleted, false)
        ));
    }
}
```

### 1.3 Manager 層 - 事務管理

```java
package net.lab1024.sa.business.risk.manager;

import lombok.RequiredArgsConstructor;
import net.lab1024.sa.business.risk.dao.RiskProposalDao;
import net.lab1024.sa.business.risk.domain.entity.RiskProposalEntity;
import net.lab1024.sa.business.risk.domain.event.RiskProposalEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 風控提案 Manager 層 (事務管理)
 *
 * @author Risk Team
 * @version 2.1.0
 * @since 2026-02-02
 */
@Service
@RequiredArgsConstructor
public class RiskProposalManager {

    private final RiskProposalDao riskProposalDao;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 創建風控提案（含事務）
     *
     * @param entity 提案實體
     * @return String 提案 ID
     */
    @Transactional(rollbackFor = Throwable.class)
    public String createProposalWithTransaction(RiskProposalEntity entity) {
        // 1. 插入提案記錄
        riskProposalDao.insert(entity);

        // 2. 發送 Kafka 事件（異步處理）
        kafkaTemplate.send("risk.proposal.created", RiskProposalEvent.builder()
            .proposalId(entity.getId())
            .playerId(entity.getPlayerId())
            .suspiciousAmount(entity.getSuspiciousAmount())
            .priority(entity.getPriority())
            .createdAt(entity.getCreatedAt())
            .build()
        );

        // 3. 返回提案 ID
        return entity.getId().toString();
    }

    /**
     * 批准風控提案（含事務）
     *
     * @param proposalId 提案 ID
     * @param reviewerId 審核員 ID
     * @param reviewNotes 審核備註
     * @return boolean 是否成功
     */
    @Transactional(rollbackFor = Throwable.class)
    public boolean approveProposalWithTransaction(
        Long proposalId,
        Long reviewerId,
        String reviewNotes
    ) {
        // 1. 更新提案狀態
        RiskProposalEntity entity = riskProposalDao.selectById(proposalId);
        if (entity == null) {
            return false;
        }

        entity.setStatus("APPROVED");
        entity.setReviewerId(reviewerId);
        entity.setReviewedAt(LocalDateTime.now());
        entity.setReviewNotes(reviewNotes);

        riskProposalDao.updateById(entity);

        // 2. 發送 Kafka 事件
        kafkaTemplate.send("risk.proposal.approved", RiskProposalEvent.builder()
            .proposalId(entity.getId())
            .playerId(entity.getPlayerId())
            .reviewerId(reviewerId)
            .build()
        );

        return true;
    }
}
```

### 1.4 Service 層 - 業務邏輯

```java
package net.lab1024.sa.business.risk.service;

import io.vavr.control.Option;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.business.risk.dao.RiskProposalDao;
import net.lab1024.sa.business.risk.domain.entity.RiskProposalEntity;
import net.lab1024.sa.business.risk.domain.form.RiskProposalCreateDTO;
import net.lab1024.sa.business.risk.domain.vo.RiskProposalVO;
import net.lab1024.sa.business.risk.manager.RiskProposalManager;
import net.lab1024.sa.business.player.dao.PlayerDao;
import net.lab1024.sa.common.core.util.SmartBeanUtil;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 風控提案 Service 層 (業務邏輯)
 * 使用 Vavr Option 處理空值
 *
 * @author Risk Team
 * @version 2.1.0
 * @since 2026-02-02
 */
@Service
@RequiredArgsConstructor
public class RiskProposalService {

    private final RiskProposalManager riskProposalManager;
    private final RiskProposalDao riskProposalDao;
    private final PlayerDao playerDao;
    private final RedissonClient redissonClient;

    /**
     * 創建風控提案 (FLAG 規則觸發時調用)
     *
     * @param createDTO 提案創建 DTO
     * @return Option<String> 提案 ID
     */
    public Option<String> createProposal(RiskProposalCreateDTO createDTO) {
        // 驗證玩家存在性
        return Option.of(playerDao.selectById(createDTO.getPlayerId()))
            .flatMap(player -> {
                // 委託給 Manager 層執行事務操作
                String proposalId = riskProposalManager.createProposalWithTransaction(
                    RiskProposalEntity.builder()
                        .proposalNo(generateProposalNo())
                        .betId(createDTO.getBetId())
                        .playerId(createDTO.getPlayerId())
                        .matchedRules(createDTO.getFlaggedRules())
                        .flaggedReasons(createDTO.getFlaggedReasons())
                        .suspiciousAmount(createDTO.getSuspiciousAmount())
                        .status("PENDING_REVIEW")
                        .priority(calculatePriority(createDTO))
                        .build()
                );

                return Option.of(proposalId);
            });
    }

    /**
     * 查詢玩家的待審核提案 (近 N 天)
     * 使用 Redis 緩存提升性能
     *
     * @param playerId 玩家 ID
     * @param days 查詢天數
     * @return Option<List<RiskProposalVO>> 提案列表
     */
    public Option<List<RiskProposalVO>> findPendingProposals(Long playerId, int days) {
        String cacheKey = String.format("risk:proposal:player:%d:days:%d", playerId, days);

        // 1. 嘗試從 Redis 緩存讀取 (TTL 5 分鐘)
        RMap<String, List<RiskProposalVO>> cache = redissonClient.getMap(cacheKey);
        if (cache.isExists()) {
            return Option.of(cache.get("proposals"));
        }

        // 2. 緩存未命中,查詢數據庫
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        List<RiskProposalEntity> entities = riskProposalDao.selectByPlayerIdAndStatus(
            playerId,
            "PENDING_REVIEW",
            startDate
        );

        // 3. Entity → VO 轉換
        List<RiskProposalVO> vos = entities.stream()
            .map(entity -> SmartBeanUtil.copy(entity, RiskProposalVO.class))
            .collect(Collectors.toList());

        // 4. 寫入 Redis 緩存 (TTL 5 分鐘)
        cache.put("proposals", vos);
        cache.expire(Duration.ofMinutes(5));

        return Option.of(vos);
    }

    /**
     * 計算玩家的總可疑金額
     *
     * @param proposals 風控提案列表
     * @return BigDecimal 總可疑金額
     */
    public BigDecimal calculateTotalSuspiciousAmount(List<RiskProposalVO> proposals) {
        return proposals.stream()
            .filter(proposal -> "PENDING_REVIEW".equals(proposal.getStatus()))
            .map(RiskProposalVO::getSuspiciousAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 生成提案編號 (格式: RP-YYYYMMDD-HHMMSS-RANDOM)
     */
    private String generateProposalNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String random = RandomStringUtils.randomNumeric(6);
        return String.format("RP-%s-%s", timestamp, random);
    }

    /**
     * 計算提案優先級
     */
    private String calculatePriority(RiskProposalCreateDTO createDTO) {
        BigDecimal amount = createDTO.getSuspiciousAmount();

        if (amount.compareTo(new BigDecimal("10000")) > 0) {
            return "URGENT";
        } else if (amount.compareTo(new BigDecimal("5000")) > 0) {
            return "HIGH";
        } else if (amount.compareTo(new BigDecimal("1000")) > 0) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }
}
```

### 1.5 Controller 層 - API 接口

```java
package net.lab1024.sa.business.risk.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.business.risk.domain.form.RiskProposalQueryForm;
import net.lab1024.sa.business.risk.domain.vo.RiskProposalVO;
import net.lab1024.sa.business.risk.service.RiskProposalService;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.core.domain.response.PageResult;
import cn.dev33.satoken.annotation.SaCheckPermission;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 風控提案 Controller
 *
 * @author Risk Team
 * @version 2.1.0
 * @since 2026-02-02
 */
@Tag(name = "風控提案管理")
@RestController
@RequestMapping("/risk/proposal")
@RequiredArgsConstructor
public class RiskProposalController {

    private final RiskProposalService riskProposalService;

    /**
     * 查詢玩家的待審核提案
     *
     * @param playerId 玩家 ID
     * @return ResponseDTO<List<RiskProposalVO>> 提案列表
     */
    @Operation(summary = "查詢玩家待審核提案")
    @GetMapping("/player/{playerId}/pending")
    @SaCheckPermission("risk:proposal:query")
    public ResponseDTO<List<RiskProposalVO>> queryPendingProposals(
        @PathVariable Long playerId,
        @RequestParam(defaultValue = "30") int days
    ) {
        return riskProposalService.findPendingProposals(playerId, days)
            .map(ResponseDTO::ok)
            .getOrElse(ResponseDTO.ok(Collections.emptyList()));
    }

    /**
     * 分頁查詢風控提案
     *
     * @param queryForm 查詢表單
     * @return ResponseDTO<PageResult<RiskProposalVO>> 分頁結果
     */
    @Operation(summary = "分頁查詢風控提案")
    @PostMapping("/query")
    @SaCheckPermission("risk:proposal:query")
    public ResponseDTO<PageResult<RiskProposalVO>> queryProposals(
        @Valid @RequestBody RiskProposalQueryForm queryForm
    ) {
        return riskProposalService.queryProposals(queryForm)
            .map(ResponseDTO::ok)
            .getOrElse(ResponseDTO.error("查詢失敗"));
    }

    /**
     * 批准風控提案
     *
     * @param proposalId 提案 ID
     * @param reviewNotes 審核備註
     * @return ResponseDTO<Void> 操作結果
     */
    @Operation(summary = "批准風控提案")
    @PostMapping("/{proposalId}/approve")
    @SaCheckPermission("risk:proposal:approve")
    public ResponseDTO<Void> approveProposal(
        @PathVariable Long proposalId,
        @RequestParam(required = false) String reviewNotes
    ) {
        // TODO: 獲取當前審核員 ID
        Long reviewerId = 1L;  // StpUtil.getLoginIdAsLong();

        boolean success = riskProposalService.approveProposal(proposalId, reviewerId, reviewNotes)
            .getOrElse(false);

        return success ? ResponseDTO.ok() : ResponseDTO.error("批准失敗");
    }
}
```

---

## 2. 性能優化

### 2.1 Redis 緩存策略

**緩存層級設計**:

| 緩存層級 | Key 格式 | TTL | 用途 |
|---------|---------|-----|------|
| **L1 - 提案列表** | `risk:proposal:player:{playerId}:days:{days}` | 5 分鐘 | 玩家待審核提案列表 |
| **L2 - 提案詳情** | `risk:proposal:detail:{proposalId}` | 10 分鐘 | 單個提案完整資訊 |
| **L3 - 風險檔案** | `risk:profile:player:{playerId}` | 30 分鐘 | 玩家風險檔案 |
| **L4 - 規則配置** | `risk:rule:config:game:{gameType}` | 1 小時 | 遊戲類型規則配置 |

**RedissonClient 配置**:

```java
/**
 * Redis 緩存配置
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
            .setAddress("redis://localhost:6379")
            .setPassword("your-password")
            .setConnectionPoolSize(50)
            .setConnectionMinimumIdleSize(10);

        return Redisson.create(config);
    }
}
```

**緩存更新策略**:

```java
/**
 * Manager 層 - 緩存更新
 */
@Service
@RequiredArgsConstructor
public class RiskProposalManager {

    private final RedissonClient redissonClient;

    /**
     * 創建提案後清除相關緩存
     */
    @Transactional(rollbackFor = Throwable.class)
    public String createProposalWithTransaction(RiskProposalEntity entity) {
        // 1. 插入提案記錄
        riskProposalDao.insert(entity);

        // 2. 清除玩家提案列表緩存（所有 days 組合）
        String cacheKeyPattern = String.format("risk:proposal:player:%d:days:*", entity.getPlayerId());
        redissonClient.getKeys().deleteByPattern(cacheKeyPattern);

        // 3. 清除玩家風險檔案緩存
        String profileCacheKey = String.format("risk:profile:player:%d", entity.getPlayerId());
        redissonClient.getMap(profileCacheKey).delete();

        return entity.getId().toString();
    }
}
```

### 2.2 數據庫索引設計

**核心索引設計**:

```sql
-- 1. 玩家提案查詢優化（覆蓋索引）
CREATE INDEX idx_proposal_query ON t_risk_proposal (
    player_id,
    status,
    created_at DESC,
    deleted
) INCLUDE (id, suspicious_amount, matched_rules, priority);

-- 2. 取款關聯查詢優化
CREATE INDEX idx_withdrawal_request ON t_risk_proposal (
    withdrawal_request_id,
    status,
    deleted
);

-- 3. 審核隊列查詢優化
CREATE INDEX idx_review_queue ON t_risk_proposal (
    status,
    priority DESC,
    created_at ASC,
    deleted
);

-- 4. 黑名單玩家查詢優化
CREATE INDEX idx_blacklist ON t_player_risk_profile (
    is_blacklisted,
    deleted
);
```

**索引使用率監控**:

```sql
-- 查詢索引使用率（MySQL）
SELECT
    TABLE_NAME,
    INDEX_NAME,
    SEQ_IN_INDEX,
    COLUMN_NAME,
    CARDINALITY,
    INDEX_TYPE
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'smart_admin_igaming'
  AND TABLE_NAME IN ('t_risk_proposal', 't_player_risk_profile')
ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX;
```

### 2.3 查詢性能監控

**P6Spy 慢查詢監控**:

```properties
# application.properties
spring.datasource.driver-class-name=com.p6spy.engine.spy.P6SpyDriver
spring.datasource.url=jdbc:p6spy:mysql://localhost:3306/smart_admin_igaming

# spy.properties
logMessageFormat=com.p6spy.engine.spy.appender.CustomLineFormat
customLogMessageFormat=%(currentTime) | took %(executionTime)ms | %(category) | connection%(connectionId) | %(sql)

# 慢查詢閾值 200ms
executionThreshold=200
```

**Grafana 監控指標**:

```yaml
# Prometheus metrics
management:
  metrics:
    export:
      prometheus:
        enabled: true
  endpoints:
    web:
      exposure:
        include: prometheus,health,metrics

# 關鍵指標
- risk_proposal_query_duration_ms (P50/P95/P99)
- risk_proposal_cache_hit_rate (%)
- risk_proposal_creation_rate (per minute)
- risk_proposal_pending_count (gauge)
```

---

## 3. 監控與告警

### 3.1 關鍵指標 (Metrics)

**業務指標**:

| 指標名稱 | 類型 | 說明 | 告警閾值 |
|---------|------|------|---------|
| `risk_proposal_created_total` | Counter | 風控提案創建總數 | - |
| `risk_proposal_pending_count` | Gauge | 待審核提案數量 | >100 |
| `risk_proposal_approval_rate` | Gauge | 提案批准率 (%) | <70% |
| `risk_proposal_avg_review_time_seconds` | Histogram | 平均審核時間 (秒) | P95 >3600s |
| `risk_proposal_flagged_amount_total` | Counter | 累計可疑金額 ($) | - |

**技術指標**:

| 指標名稱 | 類型 | 說明 | 告警閾值 |
|---------|------|------|---------|
| `risk_query_duration_ms` | Histogram | 查詢響應時間 (ms) | P95 >200ms |
| `risk_cache_hit_rate` | Gauge | Redis 緩存命中率 (%) | <90% |
| `risk_db_connection_pool_utilization` | Gauge | 數據庫連接池使用率 (%) | >80% |
| `risk_kafka_event_publish_errors` | Counter | Kafka 事件發布失敗數 | >10/min |

### 3.2 Grafana 儀表板

**Dashboard JSON 範例**:

```json
{
  "dashboard": {
    "title": "配置驅動風控系統監控",
    "panels": [
      {
        "id": 1,
        "title": "風控提案創建率",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(risk_proposal_created_total[5m])",
            "legendFormat": "提案創建率 (per second)"
          }
        ]
      },
      {
        "id": 2,
        "title": "待審核提案數量",
        "type": "graph",
        "targets": [
          {
            "expr": "risk_proposal_pending_count",
            "legendFormat": "待審核提案"
          }
        ],
        "alert": {
          "conditions": [
            {
              "evaluator": {
                "params": [100],
                "type": "gt"
              },
              "query": {
                "params": ["A", "5m", "now"]
              },
              "reducer": {
                "type": "avg"
              },
              "type": "query"
            }
          ]
        }
      },
      {
        "id": 3,
        "title": "查詢響應時間 (P50/P95/P99)",
        "type": "graph",
        "targets": [
          {
            "expr": "histogram_quantile(0.50, rate(risk_query_duration_ms_bucket[5m]))",
            "legendFormat": "P50"
          },
          {
            "expr": "histogram_quantile(0.95, rate(risk_query_duration_ms_bucket[5m]))",
            "legendFormat": "P95"
          },
          {
            "expr": "histogram_quantile(0.99, rate(risk_query_duration_ms_bucket[5m]))",
            "legendFormat": "P99"
          }
        ]
      },
      {
        "id": 4,
        "title": "Redis 緩存命中率",
        "type": "gauge",
        "targets": [
          {
            "expr": "risk_cache_hit_rate * 100",
            "legendFormat": "緩存命中率 (%)"
          }
        ]
      }
    ]
  }
}
```

### 3.3 PagerDuty 告警規則

**告警規則配置**:

```yaml
# Prometheus AlertManager 配置
groups:
  - name: risk_control_alerts
    interval: 30s
    rules:
      # 1. 待審核提案積壓告警
      - alert: RiskProposalBacklogHigh
        expr: risk_proposal_pending_count > 100
        for: 10m
        labels:
          severity: warning
          team: risk
        annotations:
          summary: "風控提案積壓過多"
          description: "待審核提案數量: {{ $value }}, 超過閾值 100"

      # 2. 查詢性能下降告警
      - alert: RiskQueryLatencyHigh
        expr: histogram_quantile(0.95, rate(risk_query_duration_ms_bucket[5m])) > 200
        for: 5m
        labels:
          severity: warning
          team: backend
        annotations:
          summary: "風控查詢 P95 延遲過高"
          description: "P95 延遲: {{ $value }}ms, 超過閾值 200ms"

      # 3. 緩存命中率下降告警
      - alert: RiskCacheHitRateLow
        expr: risk_cache_hit_rate < 0.90
        for: 10m
        labels:
          severity: info
          team: backend
        annotations:
          summary: "風控緩存命中率下降"
          description: "緩存命中率: {{ $value | humanizePercentage }}, 低於 90%"

      # 4. Kafka 事件發布失敗告警
      - alert: RiskKafkaPublishErrors
        expr: rate(risk_kafka_event_publish_errors[5m]) > 10
        for: 5m
        labels:
          severity: critical
          team: backend
        annotations:
          summary: "風控 Kafka 事件發布失敗率過高"
          description: "失敗率: {{ $value }} per second"
```

**PagerDuty Integration**:

```yaml
# AlertManager PagerDuty 路由配置
route:
  group_by: ['alertname', 'team']
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 4h
  receiver: 'pagerduty-risk'

  routes:
    - match:
        severity: critical
      receiver: 'pagerduty-oncall'
      continue: true

    - match:
        severity: warning
      receiver: 'pagerduty-risk'

    - match:
        severity: info
      receiver: 'slack-notifications'

receivers:
  - name: 'pagerduty-oncall'
    pagerduty_configs:
      - service_key: '<PAGERDUTY_SERVICE_KEY>'
        description: '{{ .GroupLabels.alertname }}'
        severity: 'critical'

  - name: 'pagerduty-risk'
    pagerduty_configs:
      - service_key: '<PAGERDUTY_RISK_TEAM_KEY>'
        description: '{{ .GroupLabels.alertname }}'
        severity: 'warning'

  - name: 'slack-notifications'
    slack_configs:
      - api_url: '<SLACK_WEBHOOK_URL>'
        channel: '#risk-alerts'
        title: '{{ .GroupLabels.alertname }}'
        text: '{{ range .Alerts }}{{ .Annotations.description }}{{ end }}'
```

---

## 4. 測試策略

### 4.1 單元測試

**RiskProposalService 單元測試**:

```java
/**
 * 風控提案服務單元測試
 */
@SpringBootTest
class RiskProposalServiceTest {

    @Autowired
    private RiskProposalService riskProposalService;

    @MockBean
    private RiskProposalDao riskProposalDao;

    @Test
    void testCreateProposal_Success() {
        // Given
        RiskProposalCreateDTO createDTO = RiskProposalCreateDTO.builder()
            .betId("BET-20260202-123456")
            .playerId(1001L)
            .flaggedRules(List.of("LOW_ODDS_WAGERING", "CROSS_MATCH_HEDGE"))
            .flaggedReasons(List.of("賠率過低 (<1.5)", "跨局反向投注檢測"))
            .suspiciousAmount(new BigDecimal("1500.00"))
            .build();

        when(playerDao.selectById(1001L)).thenReturn(new PlayerEntity());

        // When
        Option<String> proposalId = riskProposalService.createProposal(createDTO);

        // Then
        assertTrue(proposalId.isDefined());
        verify(riskProposalDao, times(1)).insert(any(RiskProposalEntity.class));
    }

    @Test
    void testFindPendingProposals_CacheHit() {
        // Given
        Long playerId = 1001L;
        String cacheKey = "risk:proposal:player:1001:days:30";

        // 模擬緩存命中
        RMap<String, List<RiskProposalVO>> mockCache = mock(RMap.class);
        when(redissonClient.getMap(cacheKey)).thenReturn(mockCache);
        when(mockCache.isExists()).thenReturn(true);
        when(mockCache.get("proposals")).thenReturn(Collections.emptyList());

        // When
        Option<List<RiskProposalVO>> result = riskProposalService.findPendingProposals(playerId, 30);

        // Then
        assertTrue(result.isDefined());
        verify(riskProposalDao, never()).selectByPlayerIdAndStatus(any(), any(), any());
    }
}
```

### 4.2 集成測試

**SAGA Step 2.5 集成測試**:

```java
/**
 * SAGA Step 2.5 延遲風控檢查集成測試
 */
@SpringBootTest
@Testcontainers
class WithdrawalDeferredRiskCheckIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("test_db")
        .withUsername("test")
        .withPassword("test");

    @Autowired
    private WithdrawalSagaService withdrawalSagaService;

    @Autowired
    private RiskProposalDao riskProposalDao;

    @Test
    void testDeferredRiskCheck_NoSuspiciousAmount_ShouldProceed() {
        // Given
        WithdrawalRequest request = WithdrawalRequest.builder()
            .playerId(1001L)
            .amount(new BigDecimal("5000.00"))
            .build();

        // 無待審核提案（可疑金額 = 0）

        // When
        Option<StepResult> result = withdrawalSagaService.performDeferredRiskCheck(request);

        // Then
        assertTrue(result.isDefined());
        assertEquals(StepResult.Action.PROCEED, result.get().getAction());
    }

    @Test
    void testDeferredRiskCheck_WithSuspiciousAmount_ShouldFreeze() {
        // Given
        WithdrawalRequest request = WithdrawalRequest.builder()
            .playerId(1001L)
            .amount(new BigDecimal("5000.00"))
            .build();

        // 插入待審核提案（可疑金額 = $1500）
        riskProposalDao.insert(RiskProposalEntity.builder()
            .betId("BET-20260202-123456")
            .playerId(1001L)
            .suspiciousAmount(new BigDecimal("1500.00"))
            .status("PENDING_REVIEW")
            .build());

        // When
        Option<StepResult> result = withdrawalSagaService.performDeferredRiskCheck(request);

        // Then
        assertTrue(result.isDefined());
        assertEquals(StepResult.Action.FREEZE, result.get().getAction());
        assertEquals(new BigDecimal("1500.00"), result.get().getFrozenAmount());
    }
}
```

---

## 相關文檔

- [05-02-01 檢測模型](./05-02-01_Detection_Model.md) - 系統概述與架構
- [05-02-02 規則配置](./05-02-02_Rule_Configuration.md) - 風控提案服務與延遲檢查
- [05-02-03 ML 整合](./05-02-03_ML_Integration.md) - 多維度風控規則
