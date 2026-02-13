# 代理系統技術架構（Agent System Technical Architecture）

> **業務需求**: [Agent System Requirements](../../requirements/07_Agent_Operations/02_Agent_System_Requirements.md)
> **規範來源**: [source-archive/07_Agent_Center/07-03_Agent_System.md](../../source-archive/07_Agent_Center/07-03_Agent_System.md)
> **視角**: Technical Architecture
> **目標讀者**: Architects, Backend Developers

---

## 1. 架構概述（Architecture Overview）

代理（Agent）系統（又稱聯盟夥伴系統，Affiliate）使用 **Closure Table** 模式進行階層儲存，採用事件驅動的佣金（Commission）計算引擎，以及不可變的調整分類帳用於跨週期修正。

---

## 2. 階層儲存設計（Hierarchy Storage Design）

### 2.1 儲存策略比較（Storage Strategy Comparison）

| 策略 | 優點 | 缺點 | 最適用場景 |
|----------|------|------|----------|
| **Nested Set** | 快速子樹查詢（單次查詢） | 插入/移動操作昂貴 | 讀取密集型工作負載 |
| **Closure Table** | 靈活的插入/移動操作 | 較高的儲存成本 | 寫入密集型工作負載 |

**選定策略**：**Closure Table + 路徑冗余欄位**

**理由**：
- 代理（Agent）階層變動頻繁（新代理加入、上級轉移）
- Closure Table 支援快速插入和路徑查詢
- 路徑欄位（`/1/5/12/`）提供快速階層級別判斷

---

## 3. 佣金計算引擎（Commission Calculation Engine）

```mermaid
graph TD
    A[遊戲下注完成] --> B[Event Bus]
    B --> C[佣金計算引擎]
    C --> D{計算模式}
    D -->|淨營收分成| E[淨贏額計算]
    D -->|有效投注額返佣| F[有效投注額計算]
    D -->|CPA| G[首次存款檢測]
    E --> H[多層級佣金分配]
    F --> H
    G --> H
    H --> I[調整錢包處理]
    I --> J[佣金記錄表]
    J --> K[審批隊列]
```

**架構特性**：
- **事件驅動**：下注事件觸發佣金計算
- **批次處理**：每日凌晨 02:00 透過排程任務進行結算（Settlement）
- **調整分類帳**：調整項目獨立記錄，歷史報表不可變

---

## 4. 資料庫架構（Database Schema）

### 4.1 t_affiliate_agent Table（代理主表）

```sql
CREATE TABLE t_affiliate_agent (
    agent_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id VARCHAR(32) NOT NULL,
    username VARCHAR(50) NOT NULL,
    parent_agent_id BIGINT,
    hierarchy_path VARCHAR(500),
    agent_level INT NOT NULL DEFAULT 1,
    commission_plan_id BIGINT,
    total_players INT DEFAULT 0,
    active_players INT DEFAULT 0,
    total_commission DECIMAL(15,2) DEFAULT 0.00,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_tenant_username (tenant_id, username),
    INDEX idx_parent_agent (parent_agent_id),
    INDEX idx_hierarchy_path (hierarchy_path(200)),
    UNIQUE KEY uk_tenant_username (tenant_id, username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 4.2 t_affiliate_hierarchy Table（Closure Table）

```sql
CREATE TABLE t_affiliate_hierarchy (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id VARCHAR(32) NOT NULL,
    ancestor_id BIGINT NOT NULL,
    descendant_id BIGINT NOT NULL,
    depth INT NOT NULL,

    INDEX idx_ancestor (ancestor_id, depth),
    INDEX idx_descendant (descendant_id),
    UNIQUE KEY uk_ancestor_descendant (ancestor_id, descendant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 4.3 t_affiliate_commission_plan Table（佣金方案表）

```sql
CREATE TABLE t_affiliate_commission_plan (
    plan_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id VARCHAR(32) NOT NULL,
    plan_name VARCHAR(100) NOT NULL,
    plan_type VARCHAR(20) NOT NULL,
    tiers JSON,
    settlement_period VARCHAR(20),
    negative_carryover BOOLEAN DEFAULT TRUE,
    reset_threshold DECIMAL(15,2),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- tiers JSON example:
-- [
--   {"min": 0, "max": 100000, "rate": 0.30},
--   {"min": 100000, "max": 500000, "rate": 0.40},
--   {"min": 500000, "max": null, "rate": 0.50}
-- ]
```

### 4.4 t_affiliate_commission_record Table（佣金記錄表）

```sql
CREATE TABLE t_affiliate_commission_record (
    record_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id VARCHAR(32) NOT NULL,
    agent_id BIGINT NOT NULL,
    settlement_date DATE NOT NULL,
    plan_type VARCHAR(20) NOT NULL,
    gross_amount DECIMAL(15,2) NOT NULL,
    adjustment_amount DECIMAL(15,2) DEFAULT 0.00,
    carryover_amount DECIMAL(15,2) DEFAULT 0.00,
    net_amount DECIMAL(15,2) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    approved_by BIGINT,
    approved_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_agent_settlement (agent_id, settlement_date),
    INDEX idx_status (status),
    UNIQUE KEY uk_agent_settlement_type (agent_id, settlement_date, plan_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 4.5 t_affiliate_adjustment Table（調整記錄表）

```sql
CREATE TABLE t_affiliate_adjustment (
    adjustment_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id VARCHAR(32) NOT NULL,
    agent_id BIGINT NOT NULL,
    adjustment_type VARCHAR(20) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    original_settlement_date DATE,
    target_settlement_date DATE NOT NULL,
    reason TEXT,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_agent_target_date (agent_id, target_settlement_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## 5. SQL 查詢範例（SQL Query Examples）

### 5.1 查詢代理下線樹（Query Agent Downline Tree）（Closure Table）

```sql
-- Query all downline agents for agent_id=10 (including self)
SELECT a.*
FROM t_affiliate_agent a
INNER JOIN t_affiliate_hierarchy h
    ON h.descendant_id = a.agent_id
WHERE h.ancestor_id = 10
    AND h.tenant_id = 'tenant_001'
ORDER BY h.depth, a.agent_id;
```

### 5.2 計算月度佣金（Calculate Monthly Commission）（淨營收分成）

```sql
SELECT
    agent_id,
    SUM(CASE
        WHEN net_win BETWEEN 0 AND 100000 THEN net_win * 0.30
        WHEN net_win BETWEEN 100000 AND 500000 THEN 100000 * 0.30 + (net_win - 100000) * 0.40
        WHEN net_win > 500000 THEN 100000 * 0.30 + 400000 * 0.40 + (net_win - 500000) * 0.50
        ELSE 0
    END) AS total_commission
FROM (
    SELECT
        ag.agent_id,
        SUM(b.bet_amount - b.win_amount) AS net_win
    FROM t_affiliate_agent ag
    INNER JOIN player p ON p.referrer_agent_id = ag.agent_id
    INNER JOIN bet_record b ON b.player_id = p.player_id
    WHERE ag.agent_id = 10
        AND b.settlement_date BETWEEN '2026-02-01' AND '2026-02-28'
        AND b.status = 'SETTLED'
    GROUP BY ag.agent_id
) sub
GROUP BY agent_id;
```

### 5.3 同 IP 檢測（Same IP Detection）

```sql
-- Detect agent and downline players sharing same IP
SELECT
    ag.agent_id,
    ag.username AS agent_username,
    p.player_id,
    p.username AS player_username,
    p.last_login_ip
FROM t_affiliate_agent ag
INNER JOIN player p ON p.referrer_agent_id = ag.agent_id
WHERE p.last_login_ip = (
    SELECT last_login_ip
    FROM admin_user
    WHERE user_id = ag.agent_id
)
  AND ag.status = 'ACTIVE';
```

---

## 6. SmartAdmin 架構實作（SmartAdmin Architecture Implementation）

### 6.1 AffiliateEntity（Entity 層）

```java
package net.lab1024.sa.affiliate.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Affiliate agent entity
 */
@Data
@TableName("t_affiliate_agent")
public class AffiliateEntity {

    @TableId(type = IdType.AUTO)
    private Long agentId;

    private String tenantId;
    private String username;
    private Long parentAgentId;
    private String hierarchyPath;
    private Integer agentLevel;
    private Long commissionPlanId;
    private Integer totalPlayers;
    private Integer activePlayers;
    private BigDecimal totalCommission;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### 6.2 AffiliateCommissionManager（Manager 層）

```java
package net.lab1024.sa.affiliate.manager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.affiliate.domain.entity.AffiliateCommissionRecord;
import net.lab1024.sa.affiliate.dao.AffiliateCommissionRecordDao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Affiliate commission Manager layer
 *
 * Responsibilities:
 * 1. Commission calculation and issuance (requires transaction)
 * 2. Multi-table operations (commission_record + adjustment)
 * 3. Negative carryover logic
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AffiliateCommissionManager {

    private final AffiliateCommissionRecordDao commissionRecordDao;

    @Transactional(rollbackFor = Throwable.class)
    public Long calculateAndIssueCommission(Long agentId, LocalDate settlementDate) {
        log.info("Starting commission calculation for agent {} on {}", agentId, settlementDate);

        // 1. Query previous carryover
        BigDecimal carryoverAmount = commissionRecordDao.getCarryoverAmount(
            agentId, settlementDate.minusMonths(1));

        // 2. Calculate gross commission
        BigDecimal grossAmount = commissionRecordDao.calculateGrossCommission(
            agentId, settlementDate);

        // 3. Query adjustments
        BigDecimal adjustmentAmount = commissionRecordDao.getAdjustmentAmount(
            agentId, settlementDate);

        // 4. Calculate net commission
        BigDecimal netAmount = grossAmount.add(carryoverAmount).add(adjustmentAmount);

        // 5. Negative carryover logic
        BigDecimal nextCarryover = BigDecimal.ZERO;
        if (netAmount.compareTo(BigDecimal.ZERO) < 0) {
            nextCarryover = netAmount;
            netAmount = BigDecimal.ZERO;
            log.warn("Agent {} commission is negative {}, carrying over", agentId, nextCarryover);
        }

        // 6. Insert commission record
        AffiliateCommissionRecord record = new AffiliateCommissionRecord();
        record.setAgentId(agentId);
        record.setSettlementDate(settlementDate);
        record.setPlanType("REVENUE_SHARE");
        record.setGrossAmount(grossAmount);
        record.setAdjustmentAmount(adjustmentAmount);
        record.setCarryoverAmount(carryoverAmount);
        record.setNetAmount(netAmount);
        record.setStatus("PENDING");

        commissionRecordDao.insert(record);
        return record.getRecordId();
    }

    @Transactional(rollbackFor = Throwable.class)
    public void approveCommission(Long recordId, Long approvedBy) {
        AffiliateCommissionRecord record = commissionRecordDao.selectById(recordId);

        if (record == null) {
            throw new IllegalArgumentException("Commission record not found: " + recordId);
        }
        if (!"PENDING".equals(record.getStatus())) {
            throw new IllegalStateException("Invalid commission status: " + record.getStatus());
        }

        record.setStatus("APPROVED");
        record.setApprovedBy(approvedBy);
        record.setApprovedAt(LocalDateTime.now());
        commissionRecordDao.updateById(record);

        log.info("Commission record {} approved by {}", recordId, approvedBy);
    }
}
```

### 6.3 AffiliateService（Service 層）

```java
package net.lab1024.sa.affiliate.service;

import io.vavr.control.Option;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.affiliate.domain.entity.AffiliateEntity;
import net.lab1024.sa.affiliate.dao.AffiliateDao;
import net.lab1024.sa.affiliate.manager.AffiliateCommissionManager;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Affiliate Service layer
 *
 * Responsibilities:
 * 1. Single-table queries (direct Dao calls)
 * 2. Business logic without transaction requirements
 * 3. Returns Vavr Option (SmartAdmin convention)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AffiliateService {

    private final AffiliateDao affiliateDao;
    private final AffiliateCommissionManager commissionManager;

    public Option<AffiliateEntity> getAgentById(Long agentId) {
        AffiliateEntity agent = affiliateDao.selectById(agentId);
        return Option.of(agent);
    }

    public List<AffiliateEntity> getDownlineTree(Long agentId) {
        return affiliateDao.selectDownlineTree(agentId);
    }

    public Long calculateCommission(Long agentId, LocalDate settlementDate) {
        return commissionManager.calculateAndIssueCommission(agentId, settlementDate);
    }
}
```

### 6.4 AffiliateDao（Dao 層）

```java
package net.lab1024.sa.affiliate.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.lab1024.sa.affiliate.domain.entity.AffiliateEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Affiliate Dao - Closure Table downline tree query
 */
@Mapper
public interface AffiliateDao extends BaseMapper<AffiliateEntity> {

    @Select("""
        SELECT a.*
        FROM t_affiliate_agent a
        INNER JOIN t_affiliate_hierarchy h
            ON h.descendant_id = a.agent_id
        WHERE h.ancestor_id = #{ancestorId}
        ORDER BY h.depth, a.agent_id
        """)
    List<AffiliateEntity> selectDownlineTree(Long ancestorId);
}
```

### 6.5 負數結轉公式實作（Negative Carryover Formula Implementation）

```java
/**
 * Negative carryover formula implementation
 *
 * Formula: Carryover_Next = Min(0, Current_Commission + Carryover_Prev)
 *
 * Example:
 * - Previous carryover: -$10,000
 * - Current commission: +$8,000
 * - Net amount: -$2,000 -> carried over to next period
 */
public BigDecimal calculateCarryover(BigDecimal currentCommission, BigDecimal previousCarryover) {
    BigDecimal netAmount = currentCommission.add(previousCarryover);
    return netAmount.compareTo(BigDecimal.ZERO) < 0 ? netAmount : BigDecimal.ZERO;
}
```

---

## 7. API 規格（API Specifications）

### 7.1 推薦連結生成（Referral Link Generation）

**Endpoint**: `POST /api/affiliate/referral-link`

```json
// Request
{
  "agentId": 12345,
  "campaign": "spring_promotion",
  "expiresIn": 7776000
}

// Response
{
  "code": "00000",
  "message": "success",
  "data": {
    "referralLink": "https://example.com/register?ref=ABC123XYZ",
    "referralCode": "ABC123XYZ",
    "qrCodeUrl": "https://cdn.example.com/qr/ABC123XYZ.png",
    "expiresAt": "2026-05-05T00:00:00Z"
  }
}
```

### 7.2 玩家綁定（Player Binding）

**Endpoint**: `POST /api/affiliate/bind-player`

```json
// Request
{ "playerId": 56789, "referralCode": "ABC123XYZ" }

// Response
{
  "code": "00000",
  "data": { "agentId": 12345, "bindingStatus": "SUCCESS", "protectionPeriod": 30 }
}
```

### 7.3 佣金歷史查詢（Commission History Query）

**Endpoint**: `GET /api/affiliate/commission/history?agentId=12345&startDate=2026-02-01&endDate=2026-02-28`

```json
{
  "code": "00000",
  "data": {
    "list": [
      {
        "recordId": 100001,
        "settlementDate": "2026-02-01",
        "planType": "REVENUE_SHARE",
        "grossAmount": 5000.00,
        "adjustmentAmount": -200.00,
        "carryoverAmount": -500.00,
        "netAmount": 4300.00,
        "status": "APPROVED"
      }
    ],
    "total": 50, "page": 1, "pageSize": 20
  }
}
```

---

## 8. 監控與告警（Monitoring & Alerting）

### 8.1 Prometheus 告警規則（Prometheus Alert Rules）

```yaml
groups:
  - name: affiliate_commission
    rules:
      - alert: CommissionCalculationSlow
        expr: affiliate_commission_calculation_duration_seconds > 300
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Commission calculation timeout"

      - alert: CommissionAmountAnomaly
        expr: affiliate_commission_amount > 100000
        labels:
          severity: critical
        annotations:
          summary: "Commission amount anomaly detected"
```

### 8.2 Grafana 儀表板面板（Grafana Dashboard Panels）

| 面板 | 類型 | 查詢 |
|-------|------|-------|
| 今日佣金總額 | 折線圖 | `sum(affiliate_commission_amount{status="APPROVED"}) by (tenant_id)` |
| 待審批數量 | 數字面板 | `count(t_affiliate_commission_record{status="PENDING"})` |
| 計算耗時 | 折線圖 | `avg(affiliate_commission_calculation_duration_seconds) by (agent_id)` |
| 佣金前 10 名代理 | 表格 | 按佣金金額排序 |
| 同 IP 檢測告警 | 表格 | 標記的代理-玩家 IP 匹配 |

---

**文件版本**: 4.0.0
**最後更新**: 2026-02-09
**維護團隊**: Product Team & Backend Team
