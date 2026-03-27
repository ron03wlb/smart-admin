<!-- PROJECT_CONFIG
runtime: java-gradle
test_command: ./gradlew test
END_PROJECT_CONFIG -->

<!-- SECTION_MANIFEST
section-01-foundation
section-02-wallet-core
section-03-wallet-transaction
section-04-seamless-api
section-05-wallet-reconciliation
section-06-currency-engine
section-07-payment-gateway
section-08-payment-deposit
section-09-payment-withdrawal
section-10-payment-dispute
section-11-shared-infrastructure
section-12-integration-tests
END_MANIFEST -->

# 02-funding Implementation Sections Index

## Dependency Graph

| Section | Depends On | Blocks | Parallelizable With |
|---------|------------|--------|---------------------|
| section-01-foundation | - | all | - |
| section-02-wallet-core | 01 | 03, 04, 05 | 06, 07 |
| section-03-wallet-transaction | 01, 02 | 04, 05 | - |
| section-04-seamless-api | 01, 02, 03 | 12 | 05 |
| section-05-wallet-reconciliation | 01, 02 | 12 | 04 |
| section-06-currency-engine | 01 | 07, 08, 09 | 02 |
| section-07-payment-gateway | 01, 06 | 08, 09 | - |
| section-08-payment-deposit | 01, 06, 07 | 12 | 09, 10 |
| section-09-payment-withdrawal | 01, 02, 06, 07 | 12 | 08, 10 |
| section-10-payment-dispute | 01 | 12 | 08, 09 |
| section-11-shared-infrastructure | 01 | 12 | any |
| section-12-integration-tests | all above | - | - |

## Execution Order (Batched for Parallel)

```
Batch 1: section-01-foundation (no dependencies)
Batch 2: section-02-wallet-core, section-06-currency-engine, section-11-shared-infrastructure (parallel after 01)
Batch 3: section-03-wallet-transaction, section-07-payment-gateway (parallel after batch 2)
Batch 4: section-04-seamless-api, section-05-wallet-reconciliation, section-08-payment-deposit, section-09-payment-withdrawal, section-10-payment-dispute (parallel after batch 3)
Batch 5: section-12-integration-tests (final, after all)
```

## Section Summaries

### section-01-foundation
Project structure, Gradle build config, Spring Boot application setup, database schema migrations, Redis/Kafka configuration, Money value object, base entity classes, test infrastructure (Testcontainers setup).

**Plan refs**: §2.1 Module Structure, §11.1 Money Value Object
**TDD refs**: §3.1 Data Model, §11.1 Money Value Object

### section-02-wallet-core
Three wallet types (CASH/BONUS/CREDIT), wallet entity and repository, playable balance calculation with versioned caching, deduction priority engine with 4-layer override, DB CHECK constraints.

**Plan refs**: §3 Wallet Core
**TDD refs**: §3 Wallet Core (all)

### section-03-wallet-transaction
Transaction processor with Redisson double-layer locking, nine transaction scenario handlers (Strategy pattern), round lifecycle state machine (Spring Statemachine), out-of-order parking with DB persistence, orphan round detector.

**Plan refs**: §4 Wallet Transaction Processing
**TDD refs**: §4 Wallet Transaction Processing (all)

### section-04-seamless-api
Five GP-facing endpoints (GetBalance/Debit/Credit/Rollback/Adjust), HMAC-SHA256 token validator, idempotency store (Redis 24h TTL), GP adapter layer (Strategy pattern), per-GP rate limiting.

**Plan refs**: §5 Seamless Wallet API
**TDD refs**: §5 Seamless Wallet API (all)

### section-05-wallet-reconciliation
Three-layer reconciliation model (real-time/hourly/daily), discrepancy resolver with GP-authoritative logic, reversal circuit breaker, orphan round scheduled detection, reconciliation scheduling.

**Plan refs**: §6 Wallet Reconciliation
**TDD refs**: §6 Wallet Reconciliation (all)

### section-06-currency-engine
CryptoAmount value object for native precision, FX rate manager with caching and freezing, FX risk sharing matrix (DB-configurable), blockchain monitor for BTC/ETH/USDT deposits, wallet address generator, AML screening integration point.

**Plan refs**: §11 Currency Engine
**TDD refs**: §11 Currency Engine (all)

### section-07-payment-gateway
PSP adapter interface and implementations, smart router with single-pass weighted scoring, circuit breaker per PSP (3-state), retry strategy with decline code classification, PSP health monitoring.

**Plan refs**: §7 Payment Gateway
**TDD refs**: §7 Payment Gateway (all)

### section-08-payment-deposit
Deposit flow orchestration, cashier friction evaluator (3 tiers), deposit limit checker (single/daily/monthly), PSP callback handler, 3D Secure / SCA integration, deposit notification.

**Plan refs**: §8 Payment Deposit (incl. §8.4 3DS)
**TDD refs**: §8 Payment Deposit (all)

### section-09-payment-withdrawal
Withdrawal flow with instant locking, wagering verification client (sync call to gaming domain), multi-tier approval workflow, holiday SLA adjuster, WAGERING_CHECK_PENDING queue with 2h escalation.

**Plan refs**: §9 Payment Withdrawal
**TDD refs**: §9 Payment Withdrawal (all)

### section-10-payment-dispute
Chargeback lifecycle state machine, hybrid evidence collector (auto-gather + human review), player penalty engine (escalating sanctions), chargeback monitoring and alerting.

**Plan refs**: §10 Payment Dispute
**TDD refs**: §10 Payment Dispute (all)

### section-11-shared-infrastructure
Distributed lock abstraction (Redisson), transactional outbox pattern for Kafka, governance domain stubs (contract + mock), DB-configurable parameter reader (3-layer override), Micrometer metrics setup.

**Plan refs**: §12 Shared Infrastructure, §14 Monitoring
**TDD refs**: §12 Shared Infrastructure, §15 Key Invariants

### section-12-integration-tests
End-to-end integration tests, cross-module scenario tests (deposit → wallet → GP callback flow), key invariant verification tests (INV-1 through INV-10), performance/concurrency stress tests.

**Plan refs**: §15 Key Invariants
**TDD refs**: §15 Key Invariants (Cross-Cutting Tests)
