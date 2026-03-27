# Research Findings — 03-player (Player Domain)

## Part 1: Codebase Research

### 1. SmartAdmin Architecture Patterns

#### 1.1 Layered Architecture
- **Controller**: `@RestController`, `@SaCheckPermission`, `@Tag` for OpenAPI
- **Service**: `@Service`, Vavr `Option` (NO `java.util.Optional`), NO `@Transactional`
- **Manager**: `@Service`, `@Transactional(rollbackFor = Throwable.class)` MANDATORY
- **DAO**: `@Mapper`, extends `BaseMapper<Entity>` from MyBatis Plus

**ArchitectureTest enforces**: Controller→Service→Manager→DAO layer access, constructor injection only, no `@Resource`, no `java.util.Optional` in Service.

#### 1.2 Entity Patterns
- Extends `SmartAdminBaseEntity` (provides `tenantId`, `createTime`, `updateTime`)
- `@TableName(value = "t_player", autoResultMap = true)`
- `@TableId(type = IdType.AUTO)` for BIGSERIAL
- `@Version` for optimistic locking
- Integer enums (SMALLINT), `deleted` boolean for soft delete
- PII: `@TableField(typeHandler = EncryptedFieldTypeHandler.class)` + blind index columns

#### 1.3 Form/VO Patterns
- **Form**: `@Data`, Jakarta validation (`@NotBlank`, `@Size`), `@Schema` for OpenAPI
- **VO**: `@Data`, `@Schema`, `@SchemaEnum` for enum display, PII masking (email/phone)

#### 1.4 Enum Pattern
```java
@AllArgsConstructor @Getter
public enum PlayerStatusEnum implements BaseEnum {
  ACTIVE(1, "活躍"), LOCKED(2, "鎖定"), ...
  private final Integer value;
  private final String desc;
}
```

### 2. Existing Player Module Structure

```
smartadmin-igaming-player/
├── controller/ (PlayerController, PlayerAuthController, KycAdminController)
├── service/ (PlayerService, PlayerAuthService, KycVerificationService)
├── manager/ (PlayerStateManager, PlayerRegistrationManager, VipLevelManager, KycApprovalManager)
├── dao/ (PlayerDao, KycDocumentDao, PlayerAuditLogDao, VipChangeLogDao)
├── domain/
│   ├── entity/ (PlayerEntity, KycDocumentEntity, PlayerAuditLogEntity, VipChangeLogEntity)
│   ├── form/ (PlayerLoginForm, PlayerRegisterForm, PlayerUpdateForm, PlayerQueryForm, KycL2SubmitForm, KycReviewForm)
│   └── vo/ (PlayerVO, PlayerAuthVO, KycDocumentVO)
├── selfexclusion/
│   ├── service/ (SelfExclusionRequestService, SelfExclusionEnforcementService, SelfExclusionReviewService)
│   └── domain/entity/ (SelfExclusionRequestEntity, SelfExclusionHistoryEntity)
├── vip/
│   ├── service/ (VipLevelConfigService, VipAutoUpgradeService, VipRewardDistributionService)
│   └── domain/entity/ (VipLevelConfigEntity, PlayerVipHistoryEntity, VipRewardRecordEntity)
└── typehandler/ (PostgresJsonbTypeHandler)
```

### 3. Service Pattern Example

```java
public Option<PlayerVO> getPlayer(Long playerId) {
  return Option.of(playerDao.selectById(playerId))
      .filter(entity -> !entity.getDeleted())
      .map(this::toPlayerVO);
}
```

### 4. Manager Pattern — State Transition

- `VALID_TRANSITIONS` map defines allowed transitions
- CAS update with `WHERE status = #{expectedCurrentStatus} AND version = #{expectedVersion}`
- Audit log INSERT in same transaction
- Domain event publish via `DomainEventPublisher` (Kafka)

### 5. Controller Pattern

```java
@GetMapping("/igaming/player/get/{playerId}")
@SaCheckPermission("player:info:query")
public ResponseDTO<PlayerVO> getPlayer(@PathVariable Long playerId) {
  return playerService.getPlayer(playerId)
      .map(ResponseDTO::ok)
      .getOrElse(() -> ResponseDTO.userErrorParam(PlayerErrorCode.PLAYER_NOT_FOUND.getMsg()));
}
```

### 6. Database Conventions

- Table: `t_` prefix, singular (e.g., `t_player`, `t_kyc_document`)
- PK: `BIGSERIAL`
- Enums: `SMALLINT` with CHECK constraints
- Money: `DECIMAL(19,4)`
- Time: `TIMESTAMPTZ`
- Unique index: `WHERE deleted = FALSE`
- Blind index: conditional index on `_blind_idx` columns
- Comments: Traditional Chinese

### 7. Error Code Ranges

- `303xx` for Player module (00-09: not found, 10-19: duplicates, 20-29: auth/PII, 30-39: KYC, 40-49: validation)
- Other modules: 301xx (Wallet), 302xx (Activity), 304xx (Game), 305xx (Risk), 306xx (Agent/Payment)

### 8. Build & Module Dependencies

- Player module depends on: `smartadmin-igaming-common`, `smartadmin-igaming-wallet`, `smartadmin-api-igaming`
- Common modules: `smartadmin-common-core`, `-mybatis`, `-security`, `-tenant`, `-token`, `-mq`, `-redis`, `-json`, `-cache`
- Support: `smartadmin-support-operatelog`, `-datatracer`
- Testing: JUnit 5, ArchUnit, `@Tag("integration")` for integration tests (excluded by default)

### 9. Migration Files

- V1: Core infrastructure
- V2: Domain tables (all 30 igaming tables)
- V3: Feature tables
- V4: Business rules engine
- V5: Seed data
- V6: Governance tables

---

## Part 2: Web Research

### Topic 1: GAMSTOP / Self-Exclusion API Integration

#### GAMSTOP API Technical Architecture
- **Protocol**: REST API over HTTPS (POST only)
- **Authentication**: API key + IP whitelist
- **Request**: `first_name`, `last_name`, `date_of_birth`, `email`, `postcode`, `X-Trace-ID` header
- **Response**: Binary `isAllowed()` / `isBlocked()`
- **Errors**: `MissingParametersException`, `ApiKeyInvalidException`, `NonPostCallException`, `RateLimitedException`, `NetworkingErrorException`

#### Mandatory Check Points
1. Registration — check before account creation
2. Login — check on every authentication
3. Periodic re-check — poll for newly self-excluded players

#### Timeout Handling
- Circuit breaker (Resilience4j) wrapping GAMSTOP calls
- On timeout: queue check, apply "pending verification" state (NEVER default to allowing play)
- Retry with exponential backoff on `NetworkingErrorException` / `RateLimitedException`

#### Other Self-Exclusion Databases

| System | Country | ID Mechanism | Notes |
|--------|---------|-------------|-------|
| GAMSTOP | UK | Name+DOB+Email+Postcode | REST API, mandatory for UKGC |
| CRUKS | Netherlands | DigiD + BSN | KSA-managed, database must reside in NL |
| Spelpaus | Sweden | BankID | Real-time, immediate propagation |
| ROFUS | Denmark | NemID/MitID | Covers online + land-based |

**Architecture**: Need a `SelfExclusionGateway` abstraction with jurisdiction-specific adapters.

#### Token/Session Revocation
1. `StpUtil.logout(playerId)` to revoke all Sa-Token sessions
2. Force-settle in-progress bets
3. Publish `PlayerSelfExcluded` domain event → downstream services enforce
4. Redis token blacklist with TTL
5. Marketing suppression (DND flag)

Sources: [Cherry-Pie/Gamstop GitHub](https://github.com/Cherry-Pie/Gamstop), [PIE Gaming Blog](https://piegaming.com/blog/how-self-exclusion-tools-affect-igaming-platforms/)

---

### Topic 2: UKGC Affordability Assessment 2025

#### Two-Tier System (Not Three)

**Tier 1 — Financial Vulnerability Checks (LIVE)**:
- Threshold: Net deposits >= GBP 150 in rolling 30 days (effective 28 Feb 2025)
- Previous threshold: GBP 500 (30 Aug 2024 – 27 Feb 2025)
- Data: Publicly available records ONLY (bankruptcy, CCJs, IVAs, Debt Relief Orders)
- Frictionless rate: 97%
- No repeat needed within 12 months

**Tier 2 — Enhanced Financial Risk Assessments (PILOT, not yet live)**:
- Thresholds: Net losses > GBP 1,000/24h OR > GBP 2,000/90d
- Data: Credit Reference Agency data
- Status: Still in pilot

**Open Banking**: Exploratory only, NOT mandatory. Players who decline are NOT blocked.

#### Calculation
`Net Deposits = Total Deposits - Total Withdrawals` (rolling 30-day window)

#### Recommended State Model
```
UNCHECKED -> THRESHOLD_BREACHED -> CHECK_IN_PROGRESS -> CLEAR | VULNERABILITY_FLAGGED -> ACTION_REQUIRED -> ACTION_APPLIED -> MONITORING
```

**Note**: Spec describes a 3-tier framework (Basic/Enhanced/Full). Web research shows UKGC actually uses 2 tiers with a pilot 3rd tier. The spec's framework is a **superset** design that accommodates both current and anticipated regulations — this is appropriate for a multi-jurisdiction system.

Sources: [UKGC Official Position](https://www.gamblingcommission.gov.uk/guidance/regulatory-decisions-procedures-and-guidance-for-regulatory-hearings/our-position-light-touch-financial-vulnerability-checks), [SBC News](https://sbcnews.co.uk/featurednews/2025/02/10/ukgc-pilot-update-2025/)

---

### Topic 3: iGaming KYC OCR Verification Patterns

#### Leading Providers

| Provider | Coverage | Speed | Key Feature |
|----------|----------|-------|-------------|
| Sumsub | 220+ countries, 14K+ doc types | <30s | Hybrid AI+human, 90%+ pass rate |
| Onfido (Entrust) | 195 countries, 2.5K+ doc types | Instant majority | AI-first, facial biometrics |
| Jumio | 200+ countries, 5K+ doc types | Variable | ML trained on billions of data points |
| Veriff | 230+ countries, 12K+ doc types | 6s average | Passive liveness |

#### OCR Capabilities
- Extracts: document number, name, nationality, DOB, expiry, MRZ
- 50+ languages/scripts
- Common escalation: blurred/cropped images, sophisticated counterfeiting, unrecognized document type

#### Webhook-Based Async Verification (Sumsub Example)
```
1. POST /resources/applicants → Create applicant
2. POST /resources/applicants/{id}/info/idDoc → Upload document
3. POST /resources/applicants/{id}/status/pending → Trigger verification
4. WEBHOOK applicantReviewed → Result (GREEN=approved, RED=rejected)
   - rejectType: FINAL (block) | RETRY (resubmit)
```

**Webhook security**: HMAC-SHA256 signature verification

Sources: [Sumsub API](https://docs.sumsub.com/reference/get-started-with-api), [Onfido API v3](https://documentation.identity.entrust.com/api/3.0.0)

---

### Topic 4: Player State Machine Concurrency in Spring Boot

#### Recommended: Custom Enum-Based State Machine (NOT Spring Statemachine)

**Why**: SmartAdmin uses MyBatis Plus (not JPA). Spring Statemachine assumes JPA persistence. Custom approach aligns with Manager-layer `@Transactional` pattern.

#### Optimistic Locking (CAS Pattern)
```sql
UPDATE t_player
SET status = #{newStatus}, version = version + 1, update_time = NOW()
WHERE player_id = #{playerId}
  AND status = #{expectedCurrentStatus}
  AND version = #{expectedVersion}
```
- Check `affectedRows == 0` → concurrent modification → reload and re-evaluate

#### Handling Multiple Simultaneous Triggers

**Scenario**: Risk flag + self-exclusion fire at same time.

**Solution**:
1. First-write-wins via CAS `WHERE version = #{expected}`
2. Loser retries by reloading current state and re-evaluating
3. Priority ordering: Self-exclusion (regulatory) > AML flag > Risk flag > KYC expired
4. Idempotent transitions: if already `SUSPENDED`, risk flag records audit but doesn't change state

**Anti-pattern**: Do NOT use `synchronized` blocks or pessimistic locks — optimistic locking with retry is more scalable.

Sources: [Spring Statemachine Docs](https://docs.spring.io/spring-statemachine/docs/current/reference/), [ByteByteGo Optimistic Locking](https://blog.bytebytego.com/p/optimistic-locking)

---

## Cross-Topic Synthesis

1. **Self-Exclusion Gateway**: Abstraction layer with jurisdiction-specific adapters (GAMSTOP, CRUKS, Spelpaus, ROFUS)
2. **Orthogonal State Tracks**: KYC track, Affordability track, Self-Exclusion track — separate concerns with a unified player lifecycle state machine
3. **Optimistic Locking Everywhere**: Version columns with CAS for all state transitions
4. **Audit-First Design**: Every transition writes immutable audit record in same transaction (regulatory requirement)
5. **Webhook-Driven Architecture**: KYC providers and affordability checks deliver async results via webhooks → trigger state transitions through event-driven state machine
