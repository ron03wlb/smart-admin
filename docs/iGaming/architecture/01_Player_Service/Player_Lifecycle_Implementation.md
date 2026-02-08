# Player Lifecycle - Technical Implementation

> **Canonical Source**: [docs/iGaming/source/01_Player_Center/01-01_Player_Lifecycle.md](../../source/01_Player_Center/01-01_Player_Lifecycle.md)
> **View**: Technical Architecture (Development & DevOps)
> **Business Requirements**: [Player_Lifecycle.md](../../requirements/01_Player_Experience/Player_Lifecycle.md)

---

## Document Purpose

This document defines the **technical implementation** for Player Lifecycle Management in iGaming platforms. It covers:
- State machine implementation
- API specifications
- Database schema
- SmartAdmin layer mapping
- Code examples and architecture patterns

**For business requirements** (lifecycle stages, KYC rules, VIP progression), see [Player_Lifecycle.md](../../requirements/01_Player_Experience/Player_Lifecycle.md).

---

## 1. State Machine Implementation

### 1.1 Account Status State Machine

```mermaid
stateDiagram-v2
    [*] --> ACTIVE : New Player Registration

    state "ACTIVE" as ACTIVE
    state "LOCKED" as LOCKED
    state "SUSPENDED" as SUSPENDED
    state "PENDING_VERIFICATION" as PENDING_VERIFICATION
    state "CLOSED" as CLOSED

    ACTIVE --> ACTIVE : Normal Activity
    ACTIVE --> LOCKED : 5 Login Failures
    ACTIVE --> SUSPENDED : Risk Score >= 70
    ACTIVE --> PENDING_VERIFICATION : Withdrawal Triggers KYC
    ACTIVE --> CLOSED : Self-Exclusion / AML Violation

    LOCKED --> ACTIVE : Auto-unlock after 30 min
    LOCKED --> SUSPENDED : Password Reset Fails 3x

    SUSPENDED --> ACTIVE : Manual Review Approved
    SUSPENDED --> CLOSED : Fraud Confirmed

    PENDING_VERIFICATION --> ACTIVE : KYC Approved
    PENDING_VERIFICATION --> SUSPENDED : KYC Document Forgery
    PENDING_VERIFICATION --> CLOSED : ID Verification Fails 3x

    CLOSED --> [*]

    note right of ACTIVE
        Default State
        No Restrictions
        All Operations Allowed
    end note

    note right of LOCKED
        Security Protection
        - Anti Brute Force
        - Auto Unlock
        - Password Reset Available
    end note

    note right of SUSPENDED
        Risk Freeze State
        - No Fund Operations
        - Manual Review Required
        - Appeal Submission Allowed
    end note

    note right of PENDING_VERIFICATION
        Withdrawal Triggers KYC Upgrade
        - Limited Withdrawal Amount
        - L0 to L1: Upload Document
        - L1 to L2: Address Verification
    end note

    note right of CLOSED
        Irreversible Terminal State
        - Self Exclusion
        - AML Violation
        - Fraud Confirmed
        - Refund Unused Balance
    end note
```

### 1.2 KYC Upgrade Flow

```mermaid
flowchart TD
    START[New Player Registration] --> L0[L0 - Phone/Email Only]

    L0 --> DEPOSIT{First Deposit?}
    DEPOSIT -->|Yes| CHECK_AMOUNT{Amount > $100?}
    CHECK_AMOUNT -->|Yes| SUGGEST_L1[Suggest L1 Upgrade]
    CHECK_AMOUNT -->|No| NORMAL_L0[Stay L0 - Limit $500]

    NORMAL_L0 --> WITHDRAW1{First Withdrawal?}
    WITHDRAW1 -->|Amount > $500| FORCE_L1[Force L1 Upgrade]
    WITHDRAW1 -->|Amount <= $500| ALLOW_L0[Allow Withdrawal - L0 Limit]

    SUGGEST_L1 --> L1_UPLOAD[Upload Document]
    FORCE_L1 --> L1_UPLOAD

    L1_UPLOAD --> L1_OCR[AI OCR Recognition]
    L1_OCR --> L1_LIVENESS[Liveness Detection]
    L1_LIVENESS --> L1_REVIEW{Review Result?}

    L1_REVIEW -->|Approved| L1[L1 - Identity Verified]
    L1_REVIEW -->|Rejected| L1_RETRY[Rejected + Reason - Allow 3 Retries]

    L1 --> WITHDRAW2{Withdrawal Amount?}
    WITHDRAW2 -->|Amount > $5,000| FORCE_L2[Force L2 Upgrade]
    WITHDRAW2 -->|Amount <= $5,000| ALLOW_L1[Allow Withdrawal]

    FORCE_L2 --> L2_UPLOAD[Upload Address Proof]
    L2_UPLOAD --> L2_REVIEW{Manual Review?}

    L2_REVIEW -->|Approved| L2[L2 - Address Verified]
    L2_REVIEW -->|Rejected| L2_RETRY[Rejected + Reason]

    L2 --> UNLIMITED[Unlimited Withdrawal]

    style L0 fill:#FFE5B4
    style L1 fill:#B4D7FF
    style L2 fill:#B4FFB4
    style FORCE_L1 fill:#FFB4B4
    style FORCE_L2 fill:#FFB4B4
```

### 1.3 KYC Review Decision Flow

```mermaid
flowchart TD
    START[KYC Document Upload Complete] --> OCR[Sumsub OCR Recognition]

    OCR --> CHECK_CONF{OCR Confidence Score?}
    CHECK_CONF -->|>= 95%| FACE[Face Matching]
    CHECK_CONF -->|< 95%| MANUAL1[Manual Review - OCR Uncertain]

    FACE --> CHECK_FACE{Match Score?}
    CHECK_FACE -->|>= 98%| BLACKLIST[Blacklist Check]
    CHECK_FACE -->|< 98%| MANUAL2[Manual Review - Face Mismatch]

    BLACKLIST --> CHECK_BL{Blacklist Match?}
    CHECK_BL -->|Yes| REJECT[Auto Reject - Blacklisted]
    CHECK_BL -->|No| AGE_CHECK{Age Check?}

    AGE_CHECK -->|< 18| REJECT_AGE[Auto Reject - Underage]
    AGE_CHECK -->|18-20| MANUAL3[Manual Review - Risk Age]
    AGE_CHECK -->|>= 21| AUTO_APPROVE[Auto Approve - 1-5 min]

    MANUAL1 & MANUAL2 & MANUAL3 --> QUEUE[Add to Review Queue]
    QUEUE --> REVIEWER[Reviewer Inspection]

    REVIEWER --> DECISION{Review Decision?}
    DECISION -->|Approve| APPROVE[Manual Approve - 1-4 hours]
    DECISION -->|Reject| REJECT_MANUAL[Manual Reject + Reason]

    AUTO_APPROVE & APPROVE --> UPDATE[Update KYC Level]
    REJECT & REJECT_AGE & REJECT_MANUAL --> NOTIFY[Notify Player]

    style AUTO_APPROVE fill:#B4FFB4
    style APPROVE fill:#B4FFB4
    style REJECT fill:#FFB4B4
    style REJECT_AGE fill:#FFB4B4
    style REJECT_MANUAL fill:#FFB4B4
    style MANUAL1 fill:#FFE5B4
    style MANUAL2 fill:#FFE5B4
    style MANUAL3 fill:#FFE5B4
```

---

## 2. Database Schema

### 2.1 Players Table

```sql
CREATE TABLE t_players (
    player_id BIGSERIAL PRIMARY KEY,
    tenant_id INT NOT NULL,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    phone_number VARCHAR(32),

    -- Account Status (ACTIVE, LOCKED, SUSPENDED, PENDING_VERIFICATION, CLOSED)
    account_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',

    -- Lifecycle Stage (NEW_USER, ACTIVE_USER, DORMANT_USER, CHURNED_USER)
    lifecycle_stage VARCHAR(32),

    -- KYC
    current_kyc_level VARCHAR(8) DEFAULT 'L0',
    pending_kyc_level VARCHAR(8),
    kyc_verified_at TIMESTAMP,
    kyc_reject_reason TEXT,
    kyc_retry_count INT DEFAULT 0,
    sumsub_applicant_id VARCHAR(64),

    -- Risk
    risk_score INT DEFAULT 0,
    risk_level VARCHAR(16) DEFAULT 'LOW',
    risk_flags JSONB,

    -- VIP
    vip_tier VARCHAR(16) DEFAULT 'BRONZE',

    -- Device
    device_fingerprint JSONB,

    -- Login Security
    failed_login_attempts INT DEFAULT 0,
    last_failed_login_at TIMESTAMP,
    locked_until TIMESTAMP,
    locked_reason VARCHAR(64),

    -- Suspension
    suspended_at TIMESTAMP,
    suspended_reason VARCHAR(255),

    -- Closure
    closed_at TIMESTAMP,
    closed_reason VARCHAR(64),
    closed_by_user_id BIGINT,

    -- Activity
    last_bet_date TIMESTAMP,

    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    version INT DEFAULT 0,
    deleted INT DEFAULT 0,

    CONSTRAINT uk_players_tenant_username UNIQUE (tenant_id, username),
    CONSTRAINT uk_players_tenant_email UNIQUE (tenant_id, email),
    CONSTRAINT uk_players_tenant_phone UNIQUE (tenant_id, phone_number)
);

CREATE INDEX idx_players_tenant_id ON t_players(tenant_id);
CREATE INDEX idx_players_account_status ON t_players(account_status);
CREATE INDEX idx_players_lifecycle_stage ON t_players(lifecycle_stage);
CREATE INDEX idx_players_risk_level ON t_players(risk_level);
CREATE INDEX idx_players_vip_tier ON t_players(vip_tier);
CREATE INDEX idx_players_last_bet_date ON t_players(last_bet_date);
```

### 2.2 Player Devices Table

```sql
CREATE TABLE t_player_devices (
    device_record_id BIGSERIAL PRIMARY KEY,
    player_id BIGINT NOT NULL REFERENCES t_players(player_id),
    tenant_id INT NOT NULL,
    device_id VARCHAR(64) NOT NULL,
    device_fingerprint JSONB,
    first_seen_at TIMESTAMP NOT NULL DEFAULT NOW(),
    last_seen_at TIMESTAMP NOT NULL DEFAULT NOW(),
    login_count INT DEFAULT 1,
    is_trusted BOOLEAN DEFAULT TRUE,
    risk_flags JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_player_devices_player_id ON t_player_devices(player_id);
CREATE INDEX idx_player_devices_device_id ON t_player_devices(device_id);
CREATE UNIQUE INDEX idx_player_devices_unique ON t_player_devices(player_id, device_id);
```

### 2.3 Player Login Logs Table

```sql
CREATE TABLE t_player_login_logs (
    log_id BIGSERIAL PRIMARY KEY,
    player_id BIGINT NOT NULL,
    tenant_id INT NOT NULL,
    action_type VARCHAR(32) NOT NULL,
    ip_address VARCHAR(45),
    country_code VARCHAR(8),
    device_id VARCHAR(64),
    from_status VARCHAR(32),
    to_status VARCHAR(32),
    reason VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_player_login_logs_player_id ON t_player_login_logs(player_id);
CREATE INDEX idx_player_login_logs_created_at ON t_player_login_logs(created_at);
```

### 2.4 KYC Verification Tasks Table

```sql
CREATE TABLE t_kyc_verification_tasks (
    task_id BIGSERIAL PRIMARY KEY,
    player_id BIGINT NOT NULL REFERENCES t_players(player_id),
    tenant_id INT NOT NULL,
    current_level VARCHAR(8) NOT NULL,
    target_level VARCHAR(8) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    reviewer_user_id BIGINT,
    review_note TEXT,
    reviewed_at TIMESTAMP,
    expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_kyc_tasks_player_id ON t_kyc_verification_tasks(player_id);
CREATE INDEX idx_kyc_tasks_status ON t_kyc_verification_tasks(status);
```

---

## 3. API Specifications

### 3.1 Player Lifecycle API

#### Get Player Lifecycle Stage

```
GET /api/player/lifecycle/{playerId}/stage
```

**Response**:
```json
{
  "code": 0,
  "data": {
    "playerId": 12345,
    "username": "player123",
    "lifecycleStage": "ACTIVE_USER",
    "accountStatus": "ACTIVE",
    "kycLevel": "L1",
    "vipTier": "SILVER",
    "riskScore": 25,
    "riskLevel": "LOW",
    "lastBetDate": "2026-02-07T15:30:00",
    "createdAt": "2026-01-15T10:00:00"
  },
  "msg": "success"
}
```

#### Lock Player Account

```
POST /api/player/lifecycle/{playerId}/lock?reason=MANUAL_LOCK
```

**Response**:
```json
{
  "code": 0,
  "data": null,
  "msg": "success"
}
```

#### Suspend Player Account

```
POST /api/player/lifecycle/{playerId}/suspend?reason=HIGH_RISK_SCORE
```

**Response**:
```json
{
  "code": 0,
  "data": null,
  "msg": "success"
}
```

### 3.2 KYC API

#### Create KYC Session

```
POST /api/player/kyc/session
```

**Request**:
```json
{
  "playerId": 12345,
  "targetLevel": "L1"
}
```

**Response**:
```json
{
  "code": 0,
  "data": {
    "sessionUrl": "https://kyc.sumsub.com/idensic/l/#/abc123..."
  },
  "msg": "success"
}
```

#### KYC Webhook (Sumsub Callback)

```
POST /api/webhook/kyc/sumsub
```

**Request** (from Sumsub):
```json
{
  "applicantId": "abc123",
  "reviewStatus": "approved",
  "rejectLabels": []
}
```

---

## 4. SmartAdmin Architecture Mapping

### 4.1 Layer Architecture Overview

SmartAdmin strictly follows a four-layer architecture:

```
Controller Layer
    | (calls)
Service Layer (Business Logic)
    | (single-table queries) OR (calls Manager for complex transactions)
Manager Layer (Transaction Coordination)
    | (calls)
Dao Layer (Data Access - MyBatis Plus)
    | (operates)
Entity Layer (Database Table Mapping)
```

**Key Constraints (ArchUnit Enforced)**:
- Controller can only call Service, cannot directly call Dao/Manager
- Service can directly call Dao (single-table CRUD), requires Manager for transactions
- `@Transactional` only in Manager layer, never in Service/Controller
- Service layer uses `io.vavr.control.Option`, not `java.util.Optional`
- Dependency injection: `@RequiredArgsConstructor` + `private final`, no `@Autowired` field injection

### 4.2 Entity - PlayerEntity

```java
package net.lab1024.sa.business.module.player.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import net.lab1024.sa.business.module.player.domain.vo.DeviceFingerprintVO;
import net.lab1024.sa.business.module.player.domain.vo.RiskFlagsVO;

import java.time.LocalDateTime;

/**
 * Player Entity
 *
 * Database Table: t_players
 * SSOT: Player account status authoritative definition
 */
@Data
@TableName(value = "t_players", autoResultMap = true)
public class PlayerEntity {

    @TableId(type = IdType.AUTO)
    private Long playerId;

    private Integer tenantId;
    private String username;
    private String passwordHash;
    private String email;
    private String phoneNumber;

    /**
     * Account Status (ACTIVE, LOCKED, SUSPENDED, PENDING_VERIFICATION, CLOSED)
     */
    private String accountStatus;

    /**
     * Lifecycle Stage (NEW_USER, ACTIVE_USER, DORMANT_USER, CHURNED_USER)
     */
    private String lifecycleStage;

    /**
     * KYC Level (L0, L1, L2)
     */
    private String currentKycLevel;
    private String pendingKycLevel;
    private LocalDateTime kycVerifiedAt;
    private String kycRejectReason;
    private Integer kycRetryCount;
    private String sumsubApplicantId;

    /**
     * Risk Score (0-100)
     */
    private Integer riskScore;
    private String riskLevel;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private RiskFlagsVO riskFlags;

    /**
     * VIP Tier (BRONZE, SILVER, GOLD, PLATINUM, DIAMOND)
     */
    private String vipTier;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private DeviceFingerprintVO deviceFingerprint;

    private Integer failedLoginAttempts;
    private LocalDateTime lastFailedLoginAt;
    private LocalDateTime lockedUntil;
    private String lockedReason;

    private LocalDateTime suspendedAt;
    private String suspendedReason;

    private LocalDateTime closedAt;
    private String closedReason;
    private Long closedByUserId;

    private LocalDateTime lastBetDate;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Version
    private Integer version;

    @TableLogic
    private Integer deleted;
}
```

### 4.3 Manager - PlayerLifecycleManager

```java
package net.lab1024.sa.business.module.player.manager;

import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.business.module.player.dao.PlayerDao;
import net.lab1024.sa.business.module.player.dao.PlayerLoginLogDao;
import net.lab1024.sa.business.module.player.domain.entity.PlayerEntity;
import net.lab1024.sa.business.module.player.domain.entity.PlayerLoginLogEntity;
import net.lab1024.sa.business.module.player.domain.event.PlayerStatusChangedEvent;
import net.lab1024.sa.support.redislock.DistributedLock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * Player Lifecycle Manager
 *
 * Responsibility: Complex transaction coordination (status transition + logging + event publishing)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerLifecycleManager {

    private final PlayerDao playerDao;
    private final PlayerLoginLogDao playerLoginLogDao;
    private final DistributedLock redisLock;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Transition account status (transaction guaranteed)
     *
     * @param playerId Player ID
     * @param targetStatus Target status (ACTIVE, LOCKED, SUSPENDED, PENDING_VERIFICATION, CLOSED)
     * @param reason Transition reason
     * @return Try<PlayerEntity> Success returns updated entity, failure returns exception
     */
    @Transactional(rollbackFor = Throwable.class)
    public Try<PlayerEntity> transitionAccountStatus(
        Long playerId,
        String targetStatus,
        String reason
    ) {
        return Try.of(() -> {
            String lockKey = "player:status:transition:" + playerId;

            return redisLock.executeWithLock(lockKey, 5, TimeUnit.SECONDS, () -> {
                // Step 1: Query current status
                PlayerEntity player = playerDao.selectById(playerId);
                if (player == null) {
                    throw new IllegalArgumentException("Player not found: " + playerId);
                }

                String currentStatus = player.getAccountStatus();

                // Step 2: Validate transition legality
                validateStatusTransition(currentStatus, targetStatus);

                // Step 3: Update player status
                player.setAccountStatus(targetStatus);

                switch (targetStatus) {
                    case "LOCKED":
                        player.setLockedUntil(LocalDateTime.now().plusMinutes(30));
                        player.setLockedReason(reason);
                        break;

                    case "SUSPENDED":
                        player.setSuspendedAt(LocalDateTime.now());
                        player.setSuspendedReason(reason);
                        break;

                    case "CLOSED":
                        player.setClosedAt(LocalDateTime.now());
                        player.setClosedReason(reason);
                        break;

                    case "ACTIVE":
                        player.setLockedUntil(null);
                        player.setLockedReason(null);
                        player.setSuspendedAt(null);
                        player.setSuspendedReason(null);
                        player.setFailedLoginAttempts(0);
                        break;
                }

                player.setUpdatedAt(LocalDateTime.now());
                playerDao.updateById(player);

                // Step 4: Record status transition log
                PlayerLoginLogEntity logEntity = new PlayerLoginLogEntity();
                logEntity.setPlayerId(playerId);
                logEntity.setActionType("STATUS_TRANSITION");
                logEntity.setFromStatus(currentStatus);
                logEntity.setToStatus(targetStatus);
                logEntity.setReason(reason);
                logEntity.setCreatedAt(LocalDateTime.now());
                playerLoginLogDao.insert(logEntity);

                // Step 5: Publish event
                eventPublisher.publishEvent(new PlayerStatusChangedEvent(
                    playerId,
                    currentStatus,
                    targetStatus,
                    reason,
                    LocalDateTime.now()
                ));

                log.info("Player status transition success - playerId: {}, {} -> {}, reason: {}",
                    playerId, currentStatus, targetStatus, reason);

                return player;
            });
        });
    }

    /**
     * Validate status transition legality
     */
    private void validateStatusTransition(String currentStatus, String targetStatus) {
        // ACTIVE can transition to any status
        if ("ACTIVE".equals(currentStatus)) {
            return;
        }

        // LOCKED can only transition to ACTIVE or SUSPENDED
        if ("LOCKED".equals(currentStatus)) {
            if (!"ACTIVE".equals(targetStatus) && !"SUSPENDED".equals(targetStatus)) {
                throw new IllegalStateException(
                    String.format("LOCKED status cannot directly transition to %s", targetStatus)
                );
            }
            return;
        }

        // SUSPENDED can only transition to ACTIVE or CLOSED
        if ("SUSPENDED".equals(currentStatus)) {
            if (!"ACTIVE".equals(targetStatus) && !"CLOSED".equals(targetStatus)) {
                throw new IllegalStateException(
                    String.format("SUSPENDED status cannot directly transition to %s", targetStatus)
                );
            }
            return;
        }

        // PENDING_VERIFICATION can transition to ACTIVE, SUSPENDED, CLOSED
        if ("PENDING_VERIFICATION".equals(currentStatus)) {
            if (!"ACTIVE".equals(targetStatus)
                && !"SUSPENDED".equals(targetStatus)
                && !"CLOSED".equals(targetStatus)) {
                throw new IllegalStateException(
                    String.format("PENDING_VERIFICATION cannot directly transition to %s", targetStatus)
                );
            }
            return;
        }

        // CLOSED is terminal state
        if ("CLOSED".equals(currentStatus)) {
            throw new IllegalStateException("CLOSED status cannot transition (terminal state)");
        }
    }

    /**
     * Handle login failure (increment failure count, lock account if necessary)
     *
     * @param playerId Player ID
     * @return Try<Boolean> Whether account was locked
     */
    @Transactional(rollbackFor = Throwable.class)
    public Try<Boolean> handleLoginFailure(Long playerId) {
        return Try.of(() -> {
            String lockKey = "player:login:failure:" + playerId;

            return redisLock.executeWithLock(lockKey, 3, TimeUnit.SECONDS, () -> {
                PlayerEntity player = playerDao.selectById(playerId);
                if (player == null) {
                    throw new IllegalArgumentException("Player not found: " + playerId);
                }

                player.setFailedLoginAttempts(player.getFailedLoginAttempts() + 1);
                player.setLastFailedLoginAt(LocalDateTime.now());
                playerDao.updateById(player);

                if (player.getFailedLoginAttempts() >= 5
                    && player.getLastFailedLoginAt().isAfter(LocalDateTime.now().minusMinutes(5))) {

                    transitionAccountStatus(playerId, "LOCKED", "CONSECUTIVE_LOGIN_FAILURES");

                    log.warn("Player account locked - playerId: {}, failedAttempts: {}",
                        playerId, player.getFailedLoginAttempts());

                    return true;
                }

                return false;
            });
        });
    }
}
```

### 4.4 Service - PlayerLifecycleService

```java
package net.lab1024.sa.business.module.player.service;

import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.business.module.player.dao.PlayerDao;
import net.lab1024.sa.business.module.player.domain.entity.PlayerEntity;
import net.lab1024.sa.business.module.player.domain.vo.PlayerLifecycleStageVO;
import net.lab1024.sa.business.module.player.manager.PlayerLifecycleManager;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Player Lifecycle Service
 *
 * Responsibility: Business logic processing (no transactions)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerLifecycleService {

    private final PlayerDao playerDao;
    private final PlayerLifecycleManager playerLifecycleManager;

    /**
     * Query player lifecycle stage
     *
     * @param playerId Player ID
     * @return Option<PlayerLifecycleStageVO> Player lifecycle info (using Vavr Option)
     */
    public Option<PlayerLifecycleStageVO> getPlayerLifecycleStage(Long playerId) {
        return playerDao.findById(playerId)
            .map(player -> {
                String stage = calculateLifecycleStage(player);

                PlayerLifecycleStageVO vo = new PlayerLifecycleStageVO();
                vo.setPlayerId(player.getPlayerId());
                vo.setUsername(player.getUsername());
                vo.setLifecycleStage(stage);
                vo.setAccountStatus(player.getAccountStatus());
                vo.setKycLevel(player.getCurrentKycLevel());
                vo.setVipTier(player.getVipTier());
                vo.setRiskScore(player.getRiskScore());
                vo.setRiskLevel(player.getRiskLevel());
                vo.setLastBetDate(player.getLastBetDate());
                vo.setCreatedAt(player.getCreatedAt());

                return vo;
            });
    }

    /**
     * Calculate lifecycle stage
     */
    private String calculateLifecycleStage(PlayerEntity player) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime createdAt = player.getCreatedAt();
        LocalDateTime lastBetDate = player.getLastBetDate();

        // Rule 1: Within 7 days of registration -> NEW_USER
        long daysSinceRegistration = ChronoUnit.DAYS.between(createdAt, now);
        if (daysSinceRegistration <= 7) {
            return "NEW_USER";
        }

        // Rule 2: Bet within last 30 days -> ACTIVE_USER
        if (lastBetDate != null) {
            long daysSinceLastBet = ChronoUnit.DAYS.between(lastBetDate, now);

            if (daysSinceLastBet <= 30) {
                return "ACTIVE_USER";
            }

            // Rule 3: No bet for 30-90 days -> DORMANT_USER
            if (daysSinceLastBet <= 90) {
                return "DORMANT_USER";
            }

            // Rule 4: No bet for 90+ days -> CHURNED_USER
            return "CHURNED_USER";
        }

        // Never bet and registered > 7 days -> CHURNED_USER
        return "CHURNED_USER";
    }

    /**
     * Check if account can login
     *
     * @param playerId Player ID
     * @return Try<Boolean> True if can login, false + reason if not
     */
    public Try<Boolean> canLogin(Long playerId) {
        return Try.of(() ->
            playerDao.findById(playerId)
                .map(player -> {
                    String status = player.getAccountStatus();

                    if ("CLOSED".equals(status)) {
                        throw new IllegalStateException("Account is closed");
                    }

                    if ("SUSPENDED".equals(status)) {
                        throw new IllegalStateException("Account is suspended, please contact support");
                    }

                    if ("LOCKED".equals(status)) {
                        LocalDateTime lockedUntil = player.getLockedUntil();
                        if (lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now())) {
                            long minutesLeft = ChronoUnit.MINUTES.between(LocalDateTime.now(), lockedUntil);
                            throw new IllegalStateException(
                                String.format("Account is locked, please try again in %d minutes or reset password", minutesLeft)
                            );
                        }

                        // Lock time passed -> auto unlock (delegate to Manager)
                        playerLifecycleManager.transitionAccountStatus(playerId, "ACTIVE", "AUTO_UNLOCK");
                    }

                    // PENDING_VERIFICATION -> can login but limited features
                    // ACTIVE -> normal login
                    return true;
                })
                .getOrElseThrow(() -> new IllegalArgumentException("Player not found"))
        );
    }

    /**
     * Lock player account (for risk control)
     */
    public Try<Void> lockPlayer(Long playerId, String reason) {
        return playerLifecycleManager.transitionAccountStatus(playerId, "LOCKED", reason)
            .map(player -> null);
    }

    /**
     * Suspend player account (for risk control)
     */
    public Try<Void> suspendPlayer(Long playerId, String reason) {
        return playerLifecycleManager.transitionAccountStatus(playerId, "SUSPENDED", reason)
            .map(player -> null);
    }
}
```

### 4.5 Controller - PlayerLifecycleController

```java
package net.lab1024.sa.business.module.player.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.business.module.player.service.PlayerLifecycleService;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import org.springframework.web.bind.annotation.*;

/**
 * Player Lifecycle Controller
 */
@RestController
@RequestMapping("/api/player/lifecycle")
@RequiredArgsConstructor
@Tag(name = "Player Lifecycle Management")
public class PlayerLifecycleController {

    private final PlayerLifecycleService playerLifecycleService;

    /**
     * Query player lifecycle stage
     */
    @GetMapping("/{playerId}/stage")
    @Operation(summary = "Query player lifecycle stage")
    public ResponseDTO<?> getLifecycleStage(@PathVariable Long playerId) {
        return playerLifecycleService.getPlayerLifecycleStage(playerId)
            .map(ResponseDTO::ok)
            .getOrElse(ResponseDTO.error("Player not found"));
    }

    /**
     * Lock player account (admin portal)
     */
    @PostMapping("/{playerId}/lock")
    @Operation(summary = "Lock player account")
    public ResponseDTO<?> lockPlayer(
        @PathVariable Long playerId,
        @RequestParam String reason
    ) {
        return playerLifecycleService.lockPlayer(playerId, reason)
            .map(v -> ResponseDTO.ok())
            .getOrElse(ResponseDTO.error("Lock failed"));
    }

    /**
     * Suspend player account (risk control)
     */
    @PostMapping("/{playerId}/suspend")
    @Operation(summary = "Suspend player account")
    public ResponseDTO<?> suspendPlayer(
        @PathVariable Long playerId,
        @RequestParam String reason
    ) {
        return playerLifecycleService.suspendPlayer(playerId, reason)
            .map(v -> ResponseDTO.ok())
            .getOrElse(ResponseDTO.error("Suspend failed"));
    }
}
```

### 4.6 Foundation Module Dependencies

| Foundation Module | Usage | Call Location |
|------------------|-------|---------------|
| **support.redis-lock** | Prevent concurrent status transitions (distributed lock) | Manager `transitionAccountStatus()` |
| **support.mq** | Publish status change events (Kafka) | Manager `PlayerStatusChangedEvent` |
| **support.cache** | Cache player lifecycle stage (Redis) | Service `getPlayerLifecycleStage()` |
| **support.audit-log** | Record status transition audit logs | Manager `PlayerLoginLogEntity` |

---

## 5. KYC Integration

### 5.1 Sumsub Integration

```java
@Service
@RequiredArgsConstructor
public class KycVerificationService {
    private final SumsubApiClient sumsubClient;
    private final PlayerDao playerDao;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Create KYC verification session
     *
     * @param playerId Player ID
     * @param targetLevel Target KYC level (L1 or L2)
     * @return Sumsub session URL (player redirects to upload documents)
     */
    public Option<String> createKycSession(Long playerId, String targetLevel) {
        return playerDao.findById(playerId)
            .flatMap(player -> {
                ApplicantRequest request = ApplicantRequest.builder()
                    .externalUserId(player.getPlayerId().toString())
                    .email(player.getEmail())
                    .phone(player.getPhoneNumber())
                    .build();

                return sumsubClient.createApplicant(request)
                    .map(applicant -> {
                        String sessionUrl = sumsubClient.generateAccessToken(
                            applicant.getId(),
                            targetLevel.equals("L1") ? "basic-kyc" : "advanced-kyc"
                        );

                        player.setPendingKycLevel(targetLevel);
                        player.setSumsubApplicantId(applicant.getId());
                        playerDao.updateById(player);

                        return sessionUrl;
                    });
            });
    }

    /**
     * Webhook: Receive Sumsub review result
     */
    @Transactional(rollbackFor = Throwable.class)
    public void handleKycWebhook(SumsubWebhookPayload webhookPayload) {
        String applicantId = webhookPayload.getApplicantId();
        String reviewStatus = webhookPayload.getReviewStatus();

        playerDao.findBySumsubApplicantId(applicantId)
            .forEach(player -> {
                if ("approved".equals(reviewStatus)) {
                    String newLevel = player.getPendingKycLevel();
                    player.setCurrentKycLevel(newLevel);
                    player.setAccountStatus("ACTIVE");
                    player.setKycVerifiedAt(LocalDateTime.now());

                    playerDao.updateById(player);

                    eventPublisher.publishEvent(new PlayerKycCompletedEvent(
                        player.getPlayerId(),
                        newLevel,
                        LocalDateTime.now()
                    ));

                } else if ("rejected".equals(reviewStatus)) {
                    String rejectReason = webhookPayload.getRejectLabels().stream()
                        .map(RejectLabel::getLabel)
                        .collect(Collectors.joining(", "));

                    player.setKycRejectReason(rejectReason);
                    player.setKycRetryCount(player.getKycRetryCount() + 1);

                    playerDao.updateById(player);

                    if (player.getKycRetryCount() >= 3) {
                        player.setAccountStatus("SUSPENDED");
                        player.setSuspendedReason("KYC_FAILED_3_TIMES");
                        playerDao.updateById(player);
                    }
                }
            });
    }
}
```

### 5.2 Device Fingerprint Collection

**Frontend JavaScript SDK** (FingerprintJS Pro):

```javascript
import FingerprintJS from '@fingerprintjs/fingerprintjs-pro'

const fpPromise = FingerprintJS.load({
  apiKey: 'YOUR_PUBLIC_API_KEY',
  region: 'ap',
  endpoint: 'https://fp.yourdomain.com'
})

fpPromise
  .then(fp => fp.get())
  .then(result => {
    const deviceId = result.visitorId;
    const confidence = result.confidence.score;

    fetch('/api/player/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username: 'player123',
        password: '...',
        deviceId: deviceId,
        deviceFingerprint: {
          confidence: confidence,
          ip: result.ip,
          ipLocation: result.ipLocation,
          browserName: result.browserName,
          browserVersion: result.browserVersion,
          os: result.os,
          osVersion: result.osVersion,
          device: result.device,
          incognito: result.incognito,
          vpn: result.vpnDetection.result,
          proxy: result.proxyDetection.result,
          emulator: result.emulatorDetection.result,
          tampered: result.tamperDetection.result
        }
      })
    });
  });
```

---

## 6. ArchUnit Architecture Tests

```java
package net.lab1024.sa.app.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * Player Lifecycle Module Architecture Tests
 *
 * Validates SmartAdmin architecture rules
 */
class PlayerLifecycleArchitectureTest {

    private final JavaClasses importedClasses = new ClassFileImporter()
        .importPackages("net.lab1024.sa.business.module.player");

    @Test
    void controllersShouldNotAccessDaoDirectly() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..controller..")
            .should().dependOnClassesThat().resideInAPackage("..dao..");

        rule.check(importedClasses);
    }

    @Test
    void transactionalOnlyInManager() {
        ArchRule rule = methods()
            .that().areAnnotatedWith(Transactional.class)
            .should().beDeclaredInClassesThat().resideInAPackage("..manager..");

        rule.check(importedClasses);
    }

    @Test
    void serviceShouldUseVavrOption() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..service..")
            .should().dependOnClassesThat().haveFullyQualifiedName("java.util.Optional");

        rule.check(importedClasses);
    }

    @Test
    void layeredArchitecture() {
        layeredArchitecture()
            .consideringAllDependencies()
            .layer("Controller").definedBy("..controller..")
            .layer("Service").definedBy("..service..")
            .layer("Manager").definedBy("..manager..")
            .layer("Dao").definedBy("..dao..")

            .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
            .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller", "Manager")
            .whereLayer("Manager").mayOnlyBeAccessedByLayers("Service")
            .whereLayer("Dao").mayOnlyBeAccessedByLayers("Service", "Manager")

            .check(importedClasses);
    }
}
```

---

## 7. Recovery Path Implementations

### 7.1 LOCKED -> ACTIVE (Auto-Unlock)

**Recovery Path 1: Time-based Auto-Unlock**

```sql
-- Cron Job: Execute every 5 minutes
UPDATE t_players
SET account_status = 'ACTIVE',
    locked_until = NULL,
    failed_login_attempts = 0,
    updated_at = NOW()
WHERE account_status = 'LOCKED'
  AND locked_until <= NOW();
```

**Recovery Path 2: Password Reset Flow**

```java
@Service
@RequiredArgsConstructor
public class PasswordResetService {
    private final PlayerDao playerDao;
    private final OtpService otpService;

    @Transactional(rollbackFor = Throwable.class)
    public Try<Void> resetPasswordAndUnlock(String email, String otp, String newPassword) {
        return otpService.verifyOtp(email, otp)
            .flatMap(isValid -> {
                if (!isValid) {
                    return Try.failure(new InvalidOtpException("OTP verification failed"));
                }

                return playerDao.findByEmail(email)
                    .map(player -> {
                        player.setPasswordHash(BCrypt.hashpw(newPassword, BCrypt.gensalt()));

                        if ("LOCKED".equals(player.getAccountStatus())) {
                            player.setAccountStatus("ACTIVE");
                            player.setLockedUntil(null);
                            player.setFailedLoginAttempts(0);
                        }

                        playerDao.updateById(player);
                        return null;
                    })
                    .toTry(() -> new PlayerNotFoundException("Player not found"));
            });
    }
}
```

### 7.2 SUSPENDED -> ACTIVE (Manual Review)

```java
@Service
@RequiredArgsConstructor
public class AppealManager {
    private final PlayerDao playerDao;
    private final AppealDao appealDao;

    @Transactional(rollbackFor = Throwable.class)
    public Try<Void> approveAppeal(Long appealId, Long reviewerUserId, String reviewNote) {
        return appealDao.findById(appealId)
            .flatMap(appeal -> {
                appeal.setStatus("APPROVED");
                appeal.setReviewerUserId(reviewerUserId);
                appeal.setReviewNote(reviewNote);
                appeal.setReviewedAt(LocalDateTime.now());
                appealDao.updateById(appeal);

                return playerDao.findById(appeal.getPlayerId())
                    .map(player -> {
                        player.setAccountStatus("ACTIVE");
                        player.setSuspendedAt(null);
                        player.setSuspendedReason(null);
                        playerDao.updateById(player);
                        return null;
                    })
                    .toTry(() -> new PlayerNotFoundException("Player not found"));
            })
            .toTry(() -> new AppealNotFoundException("Appeal not found"));
    }
}
```

### 7.3 CLOSED -> No Recovery

**Irreversible State**: CLOSED status cannot be recovered. Players must re-register (with different Email/Phone).

**Exceptions**:
- **Mistaken Operation**: System error or CS mistake - direct DB modification (requires executive approval)
- **Self-Exclusion Expiry**: If player set a cooling-off period (e.g., 180 days), can apply to reopen after expiry

```sql
-- Reopen account (requires executive approval)
UPDATE t_players
SET account_status = 'ACTIVE',
    closed_at = NULL,
    closed_reason = NULL,
    reopened_at = NOW(),
    reopened_by_user_id = ?
WHERE player_id = ?
  AND account_status = 'CLOSED'
  AND closed_reason = 'SELF_EXCLUSION'
  AND closed_at <= NOW() - INTERVAL '180 DAY';
```

---

## 8. Related Documents

### Technical Documents
- Risk Control Framework
- Unified Wallet Model
- Multi-Tenant Architecture

### Business Documents
- [Player_Lifecycle.md](../../requirements/01_Player_Experience/Player_Lifecycle.md) - Business requirements view

---

**Document Version**: 1.0.0
**Created Date**: 2026-02-08
**Last Updated**: 2026-02-08
**Maintainers**: Player Center Team & Backend Team

**Change History**:
- 1.0.0 (2026-02-08): Initial version - Split from source document (technical architecture view)
