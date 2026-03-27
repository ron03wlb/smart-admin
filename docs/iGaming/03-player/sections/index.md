<!-- PROJECT_CONFIG
runtime: java-gradle
test_command: ./gradlew :smartadmin-app:test
END_PROJECT_CONFIG -->

<!-- SECTION_MANIFEST
section-01-migration
section-02-lifecycle-statemachine
section-03-registration
section-04-kyc
section-05-vip
section-06-segmentation
section-07-self-exclusion
section-08-limits-session
section-09-problem-gambling-affordability
section-10-cs-ticketing
section-11-cs-chatbot-360view
section-12-events-consumers
section-13-security-infrastructure
END_MANIFEST -->

# Implementation Sections Index

## Dependency Graph

| Section | Depends On | Blocks | Parallelizable |
|---------|------------|--------|----------------|
| section-01-migration | - | all | No |
| section-02-lifecycle-statemachine | 01 | 03, 04, 05, 06, 07, 08, 09 | No |
| section-03-registration | 02 | 12 | Yes |
| section-04-kyc | 02 | 12 | Yes |
| section-05-vip | 02 | 12 | Yes |
| section-06-segmentation | 02 | 12 | Yes |
| section-07-self-exclusion | 02 | 08, 09, 12 | No |
| section-08-limits-session | 07 | 09, 12 | No |
| section-09-problem-gambling-affordability | 08 | 12 | No |
| section-10-cs-ticketing | 01 | 11 | Yes |
| section-11-cs-chatbot-360view | 10 | 12 | No |
| section-12-events-consumers | 03, 04, 05, 06, 09, 11 | 13 | No |
| section-13-security-infrastructure | 12 | - | No |

## Execution Order

1. **Batch 1**: section-01-migration (foundation, no dependencies)
2. **Batch 2**: section-02-lifecycle-statemachine (depends on 01)
3. **Batch 3**: section-03-registration, section-04-kyc, section-05-vip, section-06-segmentation, section-10-cs-ticketing (parallel after 02; 10 only needs 01)
4. **Batch 4**: section-07-self-exclusion, section-11-cs-chatbot-360view (07 depends on 02; 11 depends on 10)
5. **Batch 5**: section-08-limits-session (depends on 07)
6. **Batch 6**: section-09-problem-gambling-affordability (depends on 08)
7. **Batch 7**: section-12-events-consumers (depends on all feature sections)
8. **Batch 8**: section-13-security-infrastructure (final, depends on 12)

## Section Summaries

### section-01-migration
V7 database migration: new tables, expanded enums, data transformation for existing player status values, CHECK constraints, indexes, partitioning for append-only tables.

### section-02-lifecycle-statemachine
Replace PlayerStateManager's VALID_TRANSITIONS with full 7-state model. Expand PlayerStatusEnum. CAS-based transitions with audit logging and Kafka events. Permission matrix service.

### section-03-registration
Extend PlayerRegistrationManager for 4 registration methods (email, phone, OAuth, one-click). Anti-duplicate logic, age verification, registration saga with compensation for wallet creation failure.

### section-04-kyc
KycVerificationAdapter interface with StubAdapter. KYC workflow (submit → webhook → approve/reject). Multi-jurisdiction config (UKGC, MGA, PAGCOR, Curacao). KycTriggerEvaluator for auto-triggered upgrades.

### section-05-vip
VipPointsService with Redis balance strategy. VipMonthlyEvaluationJob with instant upgrade. Points earning/expiry/redemption. VipPointsBalanceFlushJob.

### section-06-segmentation
RfmCalculationService with daily batch job. PlayerTagService (system auto-tags + manual tags). Tag priority resolution. RFM anomaly detection.

### section-07-self-exclusion
SelfExclusionGateway interface with GamstopGateway (mock/real mode). CompositeGateway for jurisdiction routing. Token/Session revocation flow (v2.2). GAMSTOP timeout handling (GAP-7). Anti-circumvention detection. Enforcement task persistence for restart recovery.

### section-08-limits-session
DepositLimitService/Manager, LossLimitService/Manager with atomic enforcement. Session protection (time-out, forced rest, reality check, idle logout). Jurisdiction-specific rules (UKGC, Germany, Sweden).

### section-09-problem-gambling-affordability
ProblemGamblingDetectionService with hybrid architecture (real-time Kafka + batch). 6 indicators with alert thresholds. AffordabilityAssessmentService (3-tier UKGC framework). AffordabilityCheckAdapter interface. Marketing restriction service.

### section-10-cs-ticketing
TicketService with lifecycle management. SlaEnforcementService with type+VIP-based deadlines. SlaEscalationJob. TicketRoutingService (VIP → skill → language → load balance). KnowledgeBaseService.

### section-11-cs-chatbot-360view
PlayerViewAggregationService (360° view with parallel calls + circuit breaker). ChatbotService with IntentClassificationAdapter. Intent routing and auto-escalation. Degradation strategy. ChannelMessageService with multi-channel adapters.

### section-12-events-consumers
Kafka consumers: BetSettledConsumer, DepositCompletedConsumer, RiskScoreUpdatedConsumer, DeviceFingerprintMatchConsumer. Domain event publishing for all new state changes. Consumer idempotency with event_id deduplication.

### section-13-security-infrastructure
Webhook security (signature, timestamp, nonce, rate limiting). PII masking in 360° view. Chatbot input sanitization. Distributed locking (Redisson) for all scheduled jobs. ArchitectureTest updates for new classes.
