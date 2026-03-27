# Section 04 -- Seamless Wallet API (seamless-wallet-api)

> **Module**: `seamless-wallet-api`
> **Depends on**: section-01 (foundation), section-02 (wallet-core), section-03 (wallet-transaction)
> **Blocked by this section**: section-12 (integration-tests)
> **Parallelizable with**: section-05 (wallet-reconciliation)
> **Tech stack**: Java 17+ / Spring Boot 3.x / Redis Cluster / JUnit 5 / Mockito / Testcontainers

---

## Overview

The Seamless Wallet API is the GP-facing (external) HTTP layer that sits in front of the wallet transaction engine. It exposes five POST endpoints consumed by 10+ Game Providers. Every request passes through a shared middleware chain: HMAC-SHA256 token validation, idempotency check, GP-specific request normalization, and per-GP rate limiting. The module itself contains no business logic for balance mutations -- it delegates to `wallet-transaction` (section-03) for processing and `wallet-core` (section-02) for balance reads.

**Scale target**: 5,000--20,000 concurrent requests/sec across all GPs.

---

## 1. Tests (Write These First)

All test classes live under `seamless-wallet-api/src/test/java/`.

### 1.1 Endpoint Design Tests

**File**: `src/test/java/.../seamless/controller/SeamlessEndpointTest.java`

```java
/**
 * Tests for the five GP-facing endpoints and shared middleware behavior.
 * Uses @WebMvcTest with mocked TokenValidator, IdempotencyStore,
 * GPAdapterRegistry, and TransactionProcessor.
 */
class SeamlessEndpointTest {

    /**
     * All five endpoints (GetBalance, Debit, Credit, Rollback, Adjust)
     * return the updated balance alongside the operation result.
     * Verify each endpoint's response body contains a "balance" field
     * with the post-operation playable balance.
     */
    @Test void allFiveEndpoints_returnUpdatedBalanceAlongsideResult();

    /**
     * Verify the request flow order: Token validation runs first,
     * then idempotency check, then GP adapter normalization,
     * then transaction processor invocation, then response formatting.
     * Use InOrder verification on mocks to assert sequencing.
     */
    @Test void requestFlow_executesInCorrectOrder_token_idempotency_adapter_processor_response();

    /**
     * When a GP exceeds its configured rate limit, the endpoint returns
     * HTTP 429 Too Many Requests with a Retry-After header.
     */
    @Test void perGpRateLimiting_requestsBeyondLimit_returnHttp429();

    /**
     * The per-GP rate limit counter resets correctly per sliding window
     * (1-second window). After the window elapses, the GP can make
     * requests again up to its configured limit.
     */
    @Test void rateLimitCounter_resetsCorrectly_perSecondWindow();
}
```

### 1.2 Token Validation Tests

**File**: `src/test/java/.../seamless/security/HmacTokenValidatorTest.java`

```java
/**
 * Tests for HMAC-SHA256 token validation.
 * Unit tests with mocked Redis (for anti-replay token store).
 */
class HmacTokenValidatorTest {

    /**
     * A correctly signed HMAC-SHA256 token within the 5-minute TTL
     * is accepted. validate() returns TokenValidationResult.VALID.
     */
    @Test void validHmacSha256Token_accepted();

    /**
     * A token older than 5 minutes is rejected for mutation endpoints
     * (Debit, Credit, Rollback, Adjust) with STRICT strictness.
     * validate() returns TokenValidationResult.EXPIRED.
     */
    @Test void expiredToken_moreThan5Min_rejectedForMutationEndpoints();

    /**
     * GetBalance uses LENIENT mode: a token expired by up to 30 seconds
     * beyond the 5-minute TTL is still accepted.
     * Token aged 5m20s with LENIENT -> VALID.
     * Token aged 5m35s with LENIENT -> EXPIRED.
     */
    @Test void expiredToken_within30sGrace_acceptedForGetBalance_lenientMode();

    /**
     * One-time token enforcement: after a token is used once and recorded
     * in the Redis anti-replay set, a second request with the same token
     * is rejected with TokenValidationResult.REPLAYED.
     */
    @Test void oneTimeToken_secondUseOfSameToken_rejected_antiReplay();

    /**
     * A token with an invalid HMAC signature (wrong secret, tampered
     * payload, etc.) is rejected with TokenValidationResult.INVALID_SIGNATURE.
     */
    @Test void invalidSignature_rejected();

    /**
     * A request with a missing or blank token header is rejected
     * with TokenValidationResult.MISSING.
     */
    @Test void missingToken_rejected();
}
```

### 1.3 Idempotency Store Tests

**File**: `src/test/java/.../seamless/idempotency/RedisIdempotencyStoreTest.java`

```java
/**
 * Tests for the Redis-backed idempotency store.
 * Integration tests using Testcontainers (Redis).
 */
class RedisIdempotencyStoreTest {

    /**
     * First request with a given transactionId: IdempotencyStore.get()
     * returns Optional.empty(). After processing, put() stores the
     * response. A subsequent get() returns the cached response.
     */
    @Test void firstRequest_withTransactionId_processAndCache();

    /**
     * Duplicate transactionId: get() returns the cached CachedResponse.
     * The transaction processor is NOT invoked a second time.
     */
    @Test void duplicateTransactionId_returnCachedResponse_withoutReprocessing();

    /**
     * The cached entry expires after 24 hours. After TTL elapses,
     * get() returns Optional.empty() and the request is reprocessed.
     * (Use Redis TIME manipulation or short TTL in test.)
     */
    @Test void cacheTtl24h_keyExpiresAfter24Hours();

    /**
     * When the original request resulted in a transient error (5xx,
     * timeout, lock contention), the response is NOT cached.
     * A subsequent request with the same transactionId is reprocessed.
     */
    @Test void transientError5xx_isNotCached_nextRequestReprocesses();

    /**
     * When the original request resulted in a deterministic error
     * (insufficient balance, invalid player), the error response IS
     * cached. A subsequent request returns the same error without
     * reprocessing.
     */
    @Test void deterministicError_insufficientBalance_isCached();

    /**
     * The idempotency key includes tenantId to prevent cross-tenant
     * collision. Two requests with the same gpId+transactionId but
     * different tenantIds are treated as distinct requests.
     */
    @Test void keyComposition_includesTenantId_preventsCrossTenantCollision();

    /**
     * On cache hit, the stored request body hash is compared with the
     * incoming request body hash. If they differ, return an error
     * response and log a warning (the GP is reusing a transactionId
     * with different parameters).
     */
    @Test void requestFingerprintMismatch_errorResponse_plusWarningLog();
}
```

### 1.4 GP Adapter Layer Tests

**File**: `src/test/java/.../seamless/adapter/GPAdapterLayerTest.java`

```java
/**
 * Tests for the GP adapter registry and individual adapters.
 * Unit tests with no external dependencies.
 */
class GPAdapterLayerTest {

    /**
     * Each registered GP adapter correctly normalizes a GP-specific
     * raw request into the internal canonical model (InternalDebitRequest,
     * InternalCreditRequest, etc.). Verify field mapping for at least
     * two different GP formats.
     */
    @Test void eachGpAdapter_normalizesRequest_toInternalModel();

    /**
     * Each registered GP adapter correctly formats an internal response
     * (InternalDebitResponse, etc.) back into the GP-specific response
     * format. Verify field mapping for at least two different GP formats.
     */
    @Test void eachGpAdapter_formatsResponse_backToGpSpecificFormat();

    /**
     * When a request arrives with an unknown/unregistered GP ID,
     * the adapter registry returns an appropriate error response
     * (e.g., HTTP 400 with GP_NOT_SUPPORTED error code).
     */
    @Test void unknownGpId_returnsAppropriateErrorResponse();
}
```

---

## 2. Endpoint Design

### 2.1 Five Endpoints

All endpoints are `POST` and share common middleware (token validation, idempotency, rate limiting, GP adapter normalization).

| Endpoint | Method | Purpose |
|---|---|---|
| `/api/v1/seamless/balance` | POST | GetBalance -- read current playable balance |
| `/api/v1/seamless/debit` | POST | Debit (Bet) -- deduct from player wallet |
| `/api/v1/seamless/credit` | POST | Credit (Win) -- add to player wallet |
| `/api/v1/seamless/rollback` | POST | Rollback -- reverse a previous debit |
| `/api/v1/seamless/adjust` | POST | Adjust (Resettlement) -- correct a settled round |

All mutation endpoints (Debit, Credit, Rollback, Adjust) return the updated playable balance alongside the transaction result. GetBalance returns the current playable balance.

### 2.2 Request Flow

Every incoming request passes through this pipeline in strict order:

```
HTTP Request
  |-> Token Validator (HMAC-SHA256)
  |-> Idempotency Check (Redis)
  |-> GP Adapter (normalize GP-specific format -> internal model)
  |-> Transaction Processor (delegates to wallet-transaction module)
  |-> GP Adapter (format internal response -> GP-specific format)
  |-> HTTP Response
```

If any step fails, the pipeline short-circuits and returns an appropriate error. The GP adapter operates in both directions: inbound normalization and outbound formatting.

### 2.3 Per-GP Rate Limiting

Each GP has a configurable request rate limit (default: 2,000 req/sec). This prevents a single misbehaving GP from consuming the entire platform capacity (5K--20K total req/sec).

Implementation details:
- **Algorithm**: Redis sliding window counter, keyed by GP ID.
- **Window**: 1 second.
- **Exceeded**: Return HTTP 429 Too Many Requests with `Retry-After` header indicating when the GP may retry.
- **Configuration**: Stored per-GP in the database; loaded into a local cache on startup and refreshed periodically.

**File**: `src/main/java/.../seamless/ratelimit/GpRateLimiter.java`

```java
/**
 * Per-GP rate limiter using Redis sliding window counter.
 * Returns true if the request is within the GP's rate limit,
 * false if the limit is exceeded.
 */
public interface GpRateLimiter {

    /**
     * @param gpId   the Game Provider identifier
     * @return true if the request is allowed, false if rate limit exceeded
     */
    boolean tryAcquire(String gpId);

    /**
     * @param gpId the Game Provider identifier
     * @return seconds until the current window resets
     */
    long retryAfterSeconds(String gpId);
}
```

---

## 3. Token Validation (HMAC-SHA256)

### 3.1 Token Structure

Every GP request includes an HMAC-SHA256 token in the request header. The token binds the request to a specific player and timestamp.

### 3.2 Validation Rules

| Rule | Behavior |
|---|---|
| **Algorithm** | HMAC-SHA256 with per-GP shared secret |
| **TTL** | 5 minutes from token creation |
| **Anti-replay** | One-time use; consumed tokens stored in a Redis SET with 5-minute TTL |
| **GetBalance mode** | `LENIENT` -- adds 30-second grace period beyond the 5-minute TTL |
| **Mutation mode** | `STRICT` -- expired tokens rejected immediately |

**Edge case -- token expired on Win/Credit**: The endpoint returns a specific error code. The GP should use the Adjust (Resettlement) endpoint or raise a CS ticket for credit reconciliation. Wagering progress still counts per BS-01 decision.

### 3.3 Interface and Enum Stubs

**File**: `src/main/java/.../seamless/security/TokenValidator.java`

```java
/**
 * Validates HMAC-SHA256 tokens attached to GP requests.
 */
public interface TokenValidator {

    /**
     * @param token      the HMAC-SHA256 token string from the request header
     * @param playerId   the player ID claimed in the request body
     * @param strictness STRICT for mutations, LENIENT for GetBalance
     * @return validation result indicating accept/reject reason
     */
    TokenValidationResult validate(String token, String playerId, TokenStrictness strictness);
}
```

**File**: `src/main/java/.../seamless/security/TokenStrictness.java`

```java
public enum TokenStrictness {
    /** Mutations (Debit, Credit, Rollback, Adjust) -- reject expired immediately. */
    STRICT,
    /** GetBalance -- allow 30-second grace period beyond 5-minute TTL. */
    LENIENT
}
```

**File**: `src/main/java/.../seamless/security/TokenValidationResult.java`

```java
public enum TokenValidationResult {
    VALID,
    EXPIRED,
    REPLAYED,
    INVALID_SIGNATURE,
    MISSING
}
```

---

## 4. Idempotency Store

### 4.1 Purpose

The idempotency store prevents duplicate processing of the same transaction. GPs may retry requests due to network timeouts, and the store guarantees at-most-once processing for each unique transaction.

### 4.2 Key Composition

The idempotency key is a composite string:

```
{tenantId}:{endpoint}:{gpId}:{transactionId}
```

All four segments are required. The `tenantId` prefix prevents cross-tenant collisions in the multi-tenant environment. The `endpoint` segment prevents collisions if a GP happens to reuse a transactionId across different operation types.

### 4.3 Caching Rules

| Scenario | Cached? | Reason |
|---|---|---|
| Successful response (2xx) | Yes | Return cached result on duplicate |
| Deterministic error (insufficient balance, invalid player) | Yes | Same input always produces same error |
| Transient error (5xx, timeout, lock contention) | No | Next attempt may succeed |

**TTL**: 24 hours (stakeholder decision, extended from original 1 hour).

### 4.4 Fingerprint Validation

On a cache hit, the store compares a hash of the incoming request body with the hash stored alongside the cached response. If the hashes differ, the GP is reusing a `transactionId` with different parameters. In this case:
- Return an error response (do NOT return the cached result).
- Log a WARNING with both hashes and the GP ID for investigation.

### 4.5 Interface Stubs

**File**: `src/main/java/.../seamless/idempotency/IdempotencyStore.java`

```java
/**
 * Redis-backed idempotency store. Prevents duplicate transaction processing.
 */
public interface IdempotencyStore {

    /**
     * Look up a previously cached response for the given idempotency key.
     *
     * @param key the composite idempotency key
     * @return cached response if present, empty if this is a new request
     */
    Optional<CachedResponse> get(IdempotencyKey key);

    /**
     * Store a response for future idempotency lookups.
     * Only call this for successful responses and deterministic errors.
     * NEVER call this for transient errors (5xx, timeout, lock contention).
     *
     * @param key      the composite idempotency key
     * @param response the response to cache (includes request body hash)
     * @param ttl      cache duration (24 hours)
     */
    void put(IdempotencyKey key, CachedResponse response, Duration ttl);
}
```

**File**: `src/main/java/.../seamless/idempotency/IdempotencyKey.java`

```java
/**
 * Composite idempotency key: {tenantId}:{endpoint}:{gpId}:{transactionId}.
 * Implements equals/hashCode based on the composite string.
 */
public record IdempotencyKey(
    String tenantId,
    String endpoint,
    String gpId,
    String transactionId
) {
    /** @return the Redis key string "{tenantId}:{endpoint}:{gpId}:{transactionId}" */
    public String toRedisKey() {
        return String.join(":", tenantId, endpoint, gpId, transactionId);
    }
}
```

**File**: `src/main/java/.../seamless/idempotency/CachedResponse.java`

```java
/**
 * Stored alongside the idempotency key in Redis.
 * Contains the serialized response body and the request fingerprint hash
 * for mismatch detection.
 */
public record CachedResponse(
    int httpStatus,
    String responseBody,
    String requestBodyHash
) {}
```

---

## 5. GP Adapter Layer (Strategy Pattern)

### 5.1 Purpose

Each Game Provider has a slightly different request/response format (field names, nesting, enum values, error codes). The adapter layer normalizes inbound GP-specific requests into a canonical internal model and formats outbound internal responses back into the GP's expected format.

New GP integrations require only implementing a new adapter -- no changes to core transaction logic. Target: GP technical integration under 5 business days.

### 5.2 Interface Stub

**File**: `src/main/java/.../seamless/adapter/GPAdapter.java`

```java
/**
 * Strategy interface for GP-specific request/response normalization.
 * One implementation per integrated Game Provider.
 */
public interface GPAdapter {

    /** @return the unique GP identifier this adapter handles */
    String getGPId();

    /** Normalize a GP-specific raw request into the internal debit model. */
    InternalDebitRequest normalizeDebit(RawGPRequest request);

    /** Format an internal debit response into the GP-specific format. */
    RawGPResponse formatDebitResponse(InternalDebitResponse response);

    /** Normalize a GP-specific raw request into the internal credit model. */
    InternalCreditRequest normalizeCredit(RawGPRequest request);

    /** Format an internal credit response into the GP-specific format. */
    RawGPResponse formatCreditResponse(InternalCreditResponse response);

    /** Normalize a GP-specific raw request into the internal rollback model. */
    InternalRollbackRequest normalizeRollback(RawGPRequest request);

    /** Format an internal rollback response into the GP-specific format. */
    RawGPResponse formatRollbackResponse(InternalRollbackResponse response);

    /** Normalize a GP-specific raw request into the internal adjust model. */
    InternalAdjustRequest normalizeAdjust(RawGPRequest request);

    /** Format an internal adjust response into the GP-specific format. */
    RawGPResponse formatAdjustResponse(InternalAdjustResponse response);

    /** Normalize a GP-specific raw request into the internal balance query model. */
    InternalBalanceRequest normalizeBalance(RawGPRequest request);

    /** Format an internal balance response into the GP-specific format. */
    RawGPResponse formatBalanceResponse(InternalBalanceResponse response);
}
```

### 5.3 Adapter Registry

**File**: `src/main/java/.../seamless/adapter/GPAdapterRegistry.java`

```java
/**
 * Registry of all GP adapters. Resolves the correct adapter by GP ID.
 * Populated at startup via Spring component scanning (all GPAdapter beans
 * are auto-registered by their getGPId() value).
 *
 * If no adapter is registered for a given GP ID, throws
 * GPNotSupportedException (maps to HTTP 400).
 */
public interface GPAdapterRegistry {

    /**
     * @param gpId the Game Provider identifier
     * @return the adapter for the given GP
     * @throws GPNotSupportedException if no adapter is registered for the GP ID
     */
    GPAdapter getAdapter(String gpId);
}
```

---

## 6. File Paths Summary

All paths are relative to the `seamless-wallet-api` module root.

### Source Files

| Path | Description |
|---|---|
| `src/main/java/.../seamless/controller/SeamlessWalletController.java` | Five endpoint definitions; delegates to middleware and processor |
| `src/main/java/.../seamless/security/TokenValidator.java` | HMAC-SHA256 token validation interface |
| `src/main/java/.../seamless/security/HmacTokenValidator.java` | TokenValidator implementation using HMAC-SHA256 + Redis anti-replay |
| `src/main/java/.../seamless/security/TokenStrictness.java` | Enum: STRICT / LENIENT |
| `src/main/java/.../seamless/security/TokenValidationResult.java` | Enum: VALID / EXPIRED / REPLAYED / INVALID_SIGNATURE / MISSING |
| `src/main/java/.../seamless/idempotency/IdempotencyStore.java` | Idempotency store interface |
| `src/main/java/.../seamless/idempotency/RedisIdempotencyStore.java` | Redis-backed implementation with 24h TTL |
| `src/main/java/.../seamless/idempotency/IdempotencyKey.java` | Composite key record |
| `src/main/java/.../seamless/idempotency/CachedResponse.java` | Cached response record with request fingerprint hash |
| `src/main/java/.../seamless/adapter/GPAdapter.java` | Strategy interface for GP normalization |
| `src/main/java/.../seamless/adapter/GPAdapterRegistry.java` | Adapter lookup by GP ID |
| `src/main/java/.../seamless/ratelimit/GpRateLimiter.java` | Per-GP rate limiter interface (Redis sliding window) |

### Test Files

| Path | Description |
|---|---|
| `src/test/java/.../seamless/controller/SeamlessEndpointTest.java` | Endpoint + middleware + rate limiting tests (4 tests) |
| `src/test/java/.../seamless/security/HmacTokenValidatorTest.java` | Token validation tests (6 tests) |
| `src/test/java/.../seamless/idempotency/RedisIdempotencyStoreTest.java` | Idempotency store tests (7 tests, integration w/ Testcontainers Redis) |
| `src/test/java/.../seamless/adapter/GPAdapterLayerTest.java` | GP adapter normalization + error tests (3 tests) |

---

## 7. Dependencies on Other Sections

This section depends on three prior sections. Do NOT re-implement their contents; consume them as provided APIs.

| Dependency | What This Section Uses |
|---|---|
| **section-01 (foundation)** | Gradle module setup, Spring Boot application config, Redis/Kafka configuration, Money value object, base entity classes, Testcontainers test infrastructure |
| **section-02 (wallet-core)** | `WalletService` for balance reads, `BalanceCalculator` for playable balance computation, wallet entity model |
| **section-03 (wallet-transaction)** | `TransactionProcessor` for executing Debit/Credit/Rollback/Adjust operations, round lifecycle management, scenario handlers |

The `SeamlessWalletController` calls `TransactionProcessor` (from section-03) for all mutation endpoints and `BalanceCalculator` (from section-02) for GetBalance. The GP adapter layer translates between GP-specific formats and the internal request/response models defined in sections 02 and 03.

---

## 8. Implementation Checklist

The following order respects internal dependencies within this section:

1. Create the `seamless-wallet-api` module structure and Gradle build file (depends on section-01 foundation being complete).
2. Define enums: `TokenStrictness`, `TokenValidationResult`.
3. Define records: `IdempotencyKey`, `CachedResponse`.
4. Define interfaces: `TokenValidator`, `IdempotencyStore`, `GPAdapter`, `GPAdapterRegistry`, `GpRateLimiter`.
5. Write all 20 tests (they will fail -- red phase).
6. Implement `HmacTokenValidator` (makes 6 token tests pass).
7. Implement `RedisIdempotencyStore` (makes 7 idempotency tests pass).
8. Implement `GPAdapterRegistry` and at least two concrete GP adapters (makes 3 adapter tests pass).
9. Implement `GpRateLimiter` with Redis sliding window (makes 2 rate limit tests pass).
10. Implement `SeamlessWalletController` wiring the full middleware pipeline (makes 2 endpoint tests pass).
11. Verify all 20 tests green. Refactor as needed.
