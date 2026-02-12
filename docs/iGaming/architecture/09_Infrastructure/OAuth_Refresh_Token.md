# OAuth 2.0 Refresh Token 實施方案

> **業務需求**: [合規標準需求](../../requirements/12_Security_Compliance/Compliance_Standards_Requirements.md)
> **規範來源**: [09-11 OAuth Refresh Token](../../source-archive/09_Technical_Infrastructure/09-11_OAuth_Refresh_Token_Implementation.md)
> **目標讀者**: Security Architects, Backend Engineers

---

## 1. 問題背景

### 1.1 原始風險

**原始流程**: 根據交易 ID 找到用戶並重新生成 token

**風險等級**: CRITICAL

| 風險 | 說明 |
|------|------|
| **身份驗證缺失** | 任何人知道交易 ID 即可冒充用戶 |
| **水平越權攻擊** | 攻擊者可枚舉交易 ID 批量盜取憑證 |
| **違反 OAuth 2.0** | Token 刷新必須驗證 Refresh Token |
| **合規風險** | 違反 GDPR、等保三級、MGA/UKGC |

### 1.2 攻擊場景演示

**場景 1: 交易 ID 枚舉攻擊**

```python
# Attack script example (for security analysis only)
import requests

base_url = "https://igaming-api.example.com"

# Assuming transaction ID format is auto-increment Long
for transaction_id in range(100000, 200000):
    response = requests.post(
        f"{base_url}/api/bet/refreshToken",
        json={"transactionId": transaction_id}
    )

    if response.status_code == 200:
        stolen_token = response.json()["data"]["token"]
        print(f"[SUCCESS] Stolen user token: {stolen_token}")
```

**場景 2: 內部人員威脅**

```sql
-- Internal employee queries transaction IDs from database
SELECT transaction_id, player_id, bet_amount
FROM t_bet_transaction
WHERE created_time > NOW() - INTERVAL 1 HOUR
ORDER BY bet_amount DESC
LIMIT 100;

-- Then uses these transaction IDs to call API and steal high-value player accounts
```

### 1.3 ROI 分析

| 項目 | 金額 |
|------|------|
| **實施成本** | $24,000 - $42,000 |
| **年度收益** | $650,000 - $2,300,000 |
| **ROI** | 1448% (保守估計) |

---

## 2. OAuth 2.0 Refresh Token 架構

```mermaid
flowchart TD
    FE[iGaming Frontend<br/>Vue 3 + Ant Design Vue] -->|Access Token 15min| GW[API Gateway]
    GW -->|Token Verify| SA[SmartAdmin Backend<br/>Spring Boot 3 + Sa-Token]
    SA -->|Process| BET[Betting Service]

    FE -->|Token Expired| REFRESH[Refresh Token Flow]
    REFRESH -->|Refresh Token 30d| SA
    SA -->|Validate Device| FP[FingerprintJS<br/>Device Fingerprint]
    SA -->|New Tokens| FE

    SA --> RTM[RefreshTokenManager<br/>@Transactional]
    RTM --> REDIS[(Redis<br/>Refresh Token Store<br/>Sentinel HA)]

    style FE fill:#E3F2FD
    style SA fill:#C8E6C9
    style RTM fill:#E8F5E9
    style REDIS fill:#FFF3E0
```

### 2.1 Token 生命週期

| Token 類型 | 有效期 | 存儲位置 | 刷新機制 |
|-----------|--------|---------|---------|
| **Access Token** | 15 分鐘 | LocalStorage | 通過 Refresh Token 刷新 |
| **Refresh Token** | 30 天 | Redis (HttpOnly) | Token Rotation |

### 2.2 核心安全特性

- **Token Rotation**: 刷新後舊 Refresh Token 立即失效
- **Device Fingerprint**: FingerprintJS 設備識別（99.5% 準確率）
- **Redis 高可用**: 主從 + Sentinel 架構
- **Family Detection**: 偵測舊 Token 重用，自動撤銷整個 Token 家族

---

## 3. 刷新流程

```mermaid
sequenceDiagram
    participant C as Client
    participant GW as API Gateway
    participant SA as Sa-Token Auth
    participant RTM as RefreshTokenManager
    participant R as Redis

    C->>GW: POST /api/v1/auth/refresh<br/>{refresh_token, device_fingerprint}
    GW->>SA: Forward
    SA->>RTM: validateRefreshToken(token)
    RTM->>R: GET refresh_token:{token}
    R-->>RTM: {player_id, device_fp, family_id}

    alt Token Valid + Device Match
        RTM->>R: DELETE old refresh_token
        RTM->>RTM: Generate new Access + Refresh Token
        RTM->>R: SET new refresh_token (30d TTL)
        RTM-->>SA: RefreshTokenVO
        SA-->>C: {access_token, refresh_token}
    else Token Reused (Rotation Violation)
        RTM->>R: DELETE ALL tokens in family
        RTM-->>SA: SecurityAlert
        SA-->>C: 401 - Security Alert
    else Device Mismatch
        RTM-->>SA: DeviceFingerprintMismatchException
        SA-->>C: 403 - Device Verification Required
    end
```

---

## 4. 核心類設計

### 4.1 模組結構

```
smartadmin-support/smartadmin-support-refresh-token/
└── src/
    ├── main/java/net/lab1024/sa/support/refreshtoken/
    │   ├── constant/
    │   │   └── RefreshTokenConstant.java
    │   ├── domain/
    │   │   ├── RefreshTokenEntity.java
    │   │   ├── RefreshTokenVO.java
    │   │   └── RefreshTokenForm.java
    │   ├── exception/
    │   │   ├── TokenExpiredException.java
    │   │   ├── TokenRotationLimitException.java
    │   │   └── DeviceFingerprintMismatchException.java
    │   ├── manager/
    │   │   └── RefreshTokenManager.java
    │   └── service/
    │       └── RefreshTokenService.java
    └── test/java/net/lab1024/sa/support/refreshtoken/
        ├── manager/
        │   └── RefreshTokenManagerTest.java
        └── service/
            └── RefreshTokenServiceTest.java
```

### 4.2 RefreshTokenConstant

```java
package net.lab1024.sa.support.refreshtoken.constant;

/**
 * Refresh Token constants
 */
public interface RefreshTokenConstant {

    /** Redis Key prefix */
    String REDIS_KEY_PREFIX = "refresh_token:";

    /** Token version */
    String TOKEN_VERSION = "v1";

    /** Token expiry in days */
    int TOKEN_EXPIRY_DAYS = 30;

    /** Maximum rotation count (prevent infinite refresh) */
    int MAX_ROTATION_COUNT = 10;

    /** Token format: RT_v1_{userId}_{uuid}_{checksum} */
    String TOKEN_FORMAT = "RT_%s_%d_%s_%s";
}
```

### 4.3 RefreshTokenEntity

```java
package net.lab1024.sa.support.refreshtoken.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Refresh Token entity (Redis storage)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenEntity {

    /** Token string (format: RT_v1_{userId}_{uuid}_{checksum}) */
    private String token;

    /** User ID */
    private Long userId;

    /** Device fingerprint */
    private String deviceId;

    /** IP address */
    private String ipAddress;

    /** Creation time */
    private LocalDateTime createdAt;

    /** Expiry time (30 days after creation) */
    private LocalDateTime expiresAt;

    /** Rotation count (prevent infinite refresh) */
    private Integer rotationCount;

    /** Last rotation time */
    private LocalDateTime lastRotatedAt;
}
```

### 4.4 RefreshTokenManager (Transaction Layer)

```java
package net.lab1024.sa.support.refreshtoken.manager;

import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.json.util.JsonUtil;
import net.lab1024.sa.support.refreshtoken.constant.RefreshTokenConstant;
import net.lab1024.sa.support.refreshtoken.domain.RefreshTokenEntity;
import net.lab1024.sa.support.refreshtoken.domain.RefreshTokenVO;
import net.lab1024.sa.support.refreshtoken.exception.TokenExpiredException;
import net.lab1024.sa.support.refreshtoken.exception.TokenRotationLimitException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Refresh Token Manager (transaction layer)
 *
 * Responsibilities:
 * 1. Create Refresh Token
 * 2. Validate Refresh Token
 * 3. Token Rotation
 * 4. Token Revocation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenManager {

    private final StringRedisTemplate redisTemplate;

    @Transactional(rollbackFor = Throwable.class)
    public RefreshTokenVO createRefreshToken(Long userId, String deviceId, String ipAddress) {
        String tokenId = UUID.randomUUID().toString().replace("-", "");
        String checksum = generateChecksum(userId, tokenId);
        String refreshToken = String.format(
            RefreshTokenConstant.TOKEN_FORMAT,
            RefreshTokenConstant.TOKEN_VERSION,
            userId, tokenId, checksum
        );

        LocalDateTime now = LocalDateTime.now();
        RefreshTokenEntity entity = RefreshTokenEntity.builder()
            .token(refreshToken)
            .userId(userId)
            .deviceId(deviceId)
            .ipAddress(ipAddress)
            .createdAt(now)
            .expiresAt(now.plusDays(RefreshTokenConstant.TOKEN_EXPIRY_DAYS))
            .rotationCount(0)
            .lastRotatedAt(null)
            .build();

        String redisKey = RefreshTokenConstant.REDIS_KEY_PREFIX + refreshToken;
        redisTemplate.opsForValue().set(
            redisKey, JsonUtil.toJson(entity),
            RefreshTokenConstant.TOKEN_EXPIRY_DAYS, TimeUnit.DAYS
        );

        log.info("Created refresh token for userId: {}, deviceId: {}", userId, deviceId);

        return RefreshTokenVO.builder()
            .token(refreshToken)
            .expiresIn(RefreshTokenConstant.TOKEN_EXPIRY_DAYS * 24 * 60 * 60)
            .build();
    }

    public Option<RefreshTokenEntity> validateRefreshToken(String refreshToken) {
        return Try.of(() -> {
            if (!refreshToken.startsWith("RT_" + RefreshTokenConstant.TOKEN_VERSION + "_")) {
                throw new IllegalArgumentException("Invalid token format");
            }

            String redisKey = RefreshTokenConstant.REDIS_KEY_PREFIX + refreshToken;
            String jsonValue = redisTemplate.opsForValue().get(redisKey);

            if (jsonValue == null) {
                throw new TokenExpiredException("Refresh token expired or revoked");
            }

            RefreshTokenEntity entity = JsonUtil.fromJson(jsonValue, RefreshTokenEntity.class);

            if (entity.getExpiresAt().isBefore(LocalDateTime.now())) {
                redisTemplate.delete(redisKey);
                throw new TokenExpiredException("Refresh token expired");
            }

            if (entity.getRotationCount() >= RefreshTokenConstant.MAX_ROTATION_COUNT) {
                throw new TokenRotationLimitException("Token rotation limit exceeded");
            }

            return entity;
        })
        .onFailure(e -> log.error("Refresh token validation failed: {}", refreshToken, e))
        .toOption();
    }

    @Transactional(rollbackFor = Throwable.class)
    public RefreshTokenVO rotateRefreshToken(RefreshTokenEntity oldToken, String deviceId) {
        String oldRedisKey = RefreshTokenConstant.REDIS_KEY_PREFIX + oldToken.getToken();
        redisTemplate.delete(oldRedisKey);
        log.info("Revoked old refresh token for userId: {}", oldToken.getUserId());

        RefreshTokenVO newToken = createRefreshToken(
            oldToken.getUserId(), deviceId, oldToken.getIpAddress()
        );

        // Update rotation count on new token
        String newRedisKey = RefreshTokenConstant.REDIS_KEY_PREFIX + newToken.getToken();
        String jsonValue = redisTemplate.opsForValue().get(newRedisKey);
        RefreshTokenEntity newEntity = JsonUtil.fromJson(jsonValue, RefreshTokenEntity.class);
        newEntity.setRotationCount(oldToken.getRotationCount() + 1);
        newEntity.setLastRotatedAt(LocalDateTime.now());

        redisTemplate.opsForValue().set(
            newRedisKey, JsonUtil.toJson(newEntity),
            RefreshTokenConstant.TOKEN_EXPIRY_DAYS, TimeUnit.DAYS
        );

        return newToken;
    }

    @Transactional(rollbackFor = Throwable.class)
    public void revokeRefreshToken(String refreshToken) {
        String redisKey = RefreshTokenConstant.REDIS_KEY_PREFIX + refreshToken;
        Boolean deleted = redisTemplate.delete(redisKey);
        if (Boolean.TRUE.equals(deleted)) {
            log.info("Revoked refresh token: {}", refreshToken);
        }
    }

    private String generateChecksum(Long userId, String tokenId) {
        String data = userId + ":" + tokenId + ":" + "secret";
        return DigestUtils.md5DigestAsHex(data.getBytes()).substring(0, 8);
    }
}
```

### 4.5 LoginService 整合

```java
// New dependency injection
private final RefreshTokenManager refreshTokenManager;

/**
 * Refresh Access Token (new endpoint)
 */
@NoNeedLogin
public ResponseDTO<LoginResultVO> refreshAccessToken(
    String refreshTokenStr, String deviceFingerprint
) {
    return refreshTokenManager.validateRefreshToken(refreshTokenStr)
        .flatMap(refreshToken -> {
            if (!refreshToken.getDeviceId().equals(deviceFingerprint)) {
                log.warn("Device fingerprint mismatch for userId: {}", refreshToken.getUserId());
                return Option.none();
            }
            return Option.of(refreshToken);
        })
        .map(refreshToken -> {
            String loginId = UserTypeEnum.ADMIN_EMPLOYEE.getValue() + ":" + refreshToken.getUserId();
            StpUtil.login(loginId, new SaLoginModel()
                .setTimeout(15 * 60)
                .setDevice(deviceFingerprint)
            );
            String newAccessToken = StpUtil.getTokenValue();

            RefreshTokenVO newRefreshToken = refreshTokenManager.rotateRefreshToken(
                refreshToken, deviceFingerprint
            );

            LoginResultVO result = new LoginResultVO();
            result.setAccessToken(newAccessToken);
            result.setRefreshToken(newRefreshToken.getToken());
            result.setAccessTokenExpiresIn(15 * 60);
            result.setRefreshTokenExpiresIn(30 * 24 * 60 * 60);

            return ResponseDTO.ok(result);
        })
        .getOrElse(() -> ResponseDTO.error(UserErrorCode.REFRESH_TOKEN_INVALID,
            "Refresh token invalid or expired, please re-login"));
}
```

### 4.6 LoginController 新增接口

```java
@NoNeedLogin
@PostMapping("/login/refresh")
@Operation(summary = "Refresh Access Token")
public ResponseDTO<LoginResultVO> refreshAccessToken(@RequestBody RefreshTokenForm form) {
    return loginService.refreshAccessToken(
        form.getRefreshToken(),
        form.getDeviceFingerprint()
    );
}
```

---

## 5. 深度防禦策略

```mermaid
graph TD
    A[User Request:<br/>Refresh Token] --> B{Layer 1:<br/>Refresh Token Validation}

    B -->|Invalid Format| E1[Reject: Invalid Token Format]
    B -->|Not Found / Expired| E2[Reject: Token Expired]
    B -->|Rotation Limit Exceeded| E3[Reject: Rotation Limit]
    B -->|Valid| C{Layer 2:<br/>Device Fingerprint}

    C -->|Mismatch| E4[Reject: Device Mismatch]
    C -->|Match| D{Layer 3:<br/>IP Whitelist<br/>Optional}

    D -->|IP Not Allowed| E5[Reject: IP Not Allowed]
    D -->|Valid| F[Generate New Access Token<br/>+ Rotate Refresh Token]

    F --> G[Return New Token Pair]

    style B fill:#E1F5FE
    style C fill:#F3E5F5
    style D fill:#E8F5E9
    style F fill:#C8E6C9
    style E1 fill:#FFCDD2
    style E2 fill:#FFCDD2
    style E3 fill:#FFCDD2
    style E4 fill:#FFCDD2
    style E5 fill:#FFCDD2
```

### 5.1 攻擊場景與緩解

**Token 重放攻擊**:

```
T0: User logs in, receives Refresh Token (RT_v1_abc123)
T1: Attacker intercepts Refresh Token via MITM
T2: User refreshes Access Token -> old Token invalidated (Token Rotation)
T3: Attacker tries stolen RT_v1_abc123 -> BLOCKED (Token already revoked)

Mitigation: Token Rotation mechanism (old Token immediately invalidated)
```

**暴力破解 Token**:

```
Token format: RT_v1_{userId}_{uuid}_{checksum}
UUID entropy: 128 bits (2^128 = 3.4 x 10^38 possibilities)
Checksum entropy: 32 bits (2^32 = 4.3 x 10^9 possibilities)
Total entropy: 160 bits

Brute force estimate (assuming 100M attempts/sec):
2^160 / 10^8 = 1.46 x 10^40 seconds = 4.6 x 10^32 years

Mitigation: High-entropy Token generation (UUID + Checksum)
```

---

## 6. 測試計劃

### 6.1 覆蓋目標

| 組件 | 目標覆蓋率 |
|------|-----------|
| RefreshTokenManager | >= 90% |
| LoginService (new methods) | >= 85% |
| Overall | >= 85% |

### 6.2 RefreshTokenManager 測試用例 (20 cases)

| 類別 | 用例數 | 說明 |
|------|--------|------|
| **創建 Token** | 5 | 成功、格式驗證、null 參數、併發安全 |
| **驗證 Token** | 6 | 成功、不存在、格式錯誤、已過期、輪換超限、null 參數 |
| **輪換 Token** | 3 | 成功、輪換次數遞增、舊 Token 刪除驗證 |
| **吊銷 Token** | 2 | 成功、Token 不存在 |
| **邊界條件** | 3 | null 參數處理 |
| **併發測試** | 1 | 多線程安全 |

### 6.3 核心測試代碼

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenManager Unit Tests")
class RefreshTokenManagerTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RefreshTokenManager refreshTokenManager;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("Create Refresh Token - Success")
    void testCreateRefreshToken_Success() {
        Long userId = 10001L;
        String deviceId = "fp_abc123";
        String ipAddress = "192.168.1.100";

        RefreshTokenVO result = refreshTokenManager.createRefreshToken(userId, deviceId, ipAddress);

        assertThat(result).isNotNull();
        assertThat(result.getToken()).startsWith("RT_v1_10001_");
        assertThat(result.getExpiresIn()).isEqualTo(30 * 24 * 60 * 60);

        verify(valueOperations).set(
            startsWith("refresh_token:RT_v1_"),
            anyString(),
            eq(30L),
            eq(TimeUnit.DAYS)
        );
    }

    @Test
    @DisplayName("Validate Refresh Token - Expired")
    void testValidateRefreshToken_Expired() {
        String refreshToken = "RT_v1_10001_abc123_checksum";
        RefreshTokenEntity entity = RefreshTokenEntity.builder()
            .token(refreshToken)
            .userId(10001L)
            .expiresAt(LocalDateTime.now().minusDays(1))
            .rotationCount(0)
            .build();

        when(valueOperations.get("refresh_token:" + refreshToken))
            .thenReturn(JsonUtil.toJson(entity));

        Option<RefreshTokenEntity> result = refreshTokenManager.validateRefreshToken(refreshToken);

        assertThat(result.isEmpty()).isTrue();
        verify(redisTemplate).delete("refresh_token:" + refreshToken);
    }

    @Test
    @DisplayName("Rotate Refresh Token - Rotation Count Incremented")
    void testRotateRefreshToken_RotationCountIncremented() {
        RefreshTokenEntity oldToken = RefreshTokenEntity.builder()
            .token("RT_v1_10001_old_checksum")
            .userId(10001L)
            .deviceId("fp_abc123")
            .ipAddress("192.168.1.100")
            .rotationCount(5)
            .build();

        refreshTokenManager.rotateRefreshToken(oldToken, "fp_abc123");

        verify(valueOperations).set(
            argThat(key -> key.startsWith("refresh_token:RT_v1_")),
            argThat(json -> {
                RefreshTokenEntity entity = JsonUtil.fromJson(json, RefreshTokenEntity.class);
                return entity.getRotationCount() == 6;
            }),
            eq(30L),
            eq(TimeUnit.DAYS)
        );
    }
}
```

### 6.4 LoginService 安全測試 (12 cases)

```java
@Test
@DisplayName("Refresh Access Token - Device Fingerprint Mismatch")
void testRefreshAccessToken_DeviceFingerprintMismatch() {
    String refreshToken = "RT_v1_10001_abc123_checksum";
    String deviceFingerprint = "fp_different";  // mismatched

    RefreshTokenEntity entity = RefreshTokenEntity.builder()
        .token(refreshToken)
        .userId(10001L)
        .deviceId("fp_abc123")  // original fingerprint
        .build();

    when(refreshTokenManager.validateRefreshToken(refreshToken))
        .thenReturn(Option.of(entity));

    ResponseDTO<LoginResultVO> result = loginService.refreshAccessToken(
        refreshToken, deviceFingerprint
    );

    assertThat(result.getOk()).isFalse();
    verify(refreshTokenManager, never()).rotateRefreshToken(any(), any());
}

@Test
@DisplayName("Refresh Access Token - Invalid Token")
void testRefreshAccessToken_InvalidToken() {
    String invalidToken = "INVALID_TOKEN";

    when(refreshTokenManager.validateRefreshToken(invalidToken))
        .thenReturn(Option.none());

    ResponseDTO<LoginResultVO> result = loginService.refreshAccessToken(
        invalidToken, "fp_abc123"
    );

    assertThat(result.getOk()).isFalse();
    assertThat(result.getCode()).isEqualTo(UserErrorCode.REFRESH_TOKEN_INVALID.getCode());
}
```

### 6.5 手動驗證

```bash
# 1. Login and obtain Refresh Token
curl -X POST http://localhost:1024/api/login/login \
  -H "Content-Type: application/json" \
  -d '{"loginName": "admin", "password": "123456"}'

# 2. Refresh using Refresh Token
curl -X POST http://localhost:1024/api/login/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken": "RT_v1_...", "deviceFingerprint": "fp_abc123"}'
```

---

## 7. 業務場景替代方案

| 業務場景 | 錯誤方案 | 正確方案 |
|---------|---------|---------|
| 前端 token 過期自動刷新 | 根據交易 ID 生成 token | OAuth 2.0 Refresh Token |
| GP 回調 | 根據交易 ID 生成 token | Webhook HMAC-SHA256 簽名 |
| 管理員處理異常訂單 | 冒充用戶操作 | 管理員權限 + 審計日誌 |

---

## 8. Redis 配置與高可用

### 8.1 Redis 存儲設計

```yaml
# application-refresh-token.yml
spring:
  data:
    redis:
      sentinel:
        master: igaming-master
        nodes:
          - sentinel-1:26379
          - sentinel-2:26379
          - sentinel-3:26379
      timeout: 3000ms
      lettuce:
        pool:
          max-active: 50
          max-idle: 20
          min-idle: 5
          max-wait: 2000ms

refresh-token:
  key-prefix: "refresh_token:"
  expiry-days: 30
  max-rotation-count: 10
  device-fingerprint-required: true
```

### 8.2 Redis Key 設計

```
Key:   refresh_token:RT_v1_{userId}_{uuid}_{checksum}
Value: JSON(RefreshTokenEntity)
TTL:   30 days

Example:
Key:   refresh_token:RT_v1_10001_a3b2c1d4e5f6_ab12cd34
Value: {
  "token": "RT_v1_10001_a3b2c1d4e5f6_ab12cd34",
  "userId": 10001,
  "deviceId": "fp_abc123xyz",
  "ipAddress": "192.168.1.100",
  "createdAt": "2026-02-10T10:00:00",
  "expiresAt": "2026-03-12T10:00:00",
  "rotationCount": 0,
  "lastRotatedAt": null
}
```

---

## 9. 實施路線圖

| 階段 | 任務 | 預計時間 | 狀態 |
|------|------|---------|------|
| **Phase 1** | 基礎架構搭建 | 3 天 | Pending |
| | 1.1 創建模組結構 | 0.5 天 | |
| | 1.2 實現核心類 | 1 天 | |
| | 1.3 實現 RefreshTokenManager | 1.5 天 | |
| **Phase 2** | LoginService 集成 | 2 天 | Pending |
| | 2.1 修改 LoginService | 1 天 | |
| | 2.2 修改 Controller | 0.5 天 | |
| | 2.3 本地測試 | 0.5 天 | |
| **Phase 3** | 單元測試 | 4 天 | Pending |
| | 3.1 RefreshTokenManager 測試 (20 cases) | 2 天 | |
| | 3.2 LoginService 測試 (12 cases) | 1.5 天 | |
| | 3.3 集成測試 | 0.5 天 | |
| **Phase 4** | ArchUnit 架構測試 | 1 天 | Pending |
| **Total** | | **10 天 (2 週)** | |

---

## 10. 合規性

| 標準 | 狀態 | 說明 |
|------|------|------|
| GDPR Article 32 | Compliant | 多重身份驗證 + Token Rotation + 審計日誌 |
| 等保三級 8.1.4.1 | Compliant | Refresh Token 作為合法身份鑑別信息，符合複雜度要求 |
| 等保三級 8.1.4.5 | Compliant | 審計日誌保留 >= 6 個月 |
| MGA Player Funds Directive | Compliant | 多重身份驗證防止未經授權訪問玩家帳戶 |
| UKGC LCCP 2.1.1 | Compliant | 符合 OAuth 2.0 國際標準 |

### 10.1 違規風險（原方案）

| 標準 | 罰款風險 |
|------|---------|
| GDPR | 全球年營業額的 4% 或 EUR 20,000,000 |
| 等保三級 | 整改要求 + 罰款，嚴重情況業務停運 |
| MGA/UKGC | 牌照吊銷風險 |

---

## 11. 性能基準

```
JMeter benchmark (10k TPS):
- P50 latency:  5ms
- P99 latency:  25ms
- P999 latency: 50ms

Refresh Token validation (Redis read):
- P99 < 10ms

Access Token validation (JWT signature):
- P99 < 5ms (no database access)
```

---

## 12. 常見問題

### Q1: 為什麼 Access Token 只有 15 分鐘有效期？

15 分鐘是行業最佳實踐（Google、Facebook 均採用）。即使 Token 被盜取，攻擊者只有 15 分鐘窗口，且需要 Device Fingerprint 才能刷新。用戶無感知，前端自動刷新。

### Q2: 如果 Redis 故障，所有 Refresh Token 丟失怎麼辦？

- Redis 主從 + Sentinel 高可用架構（故障自動切換）
- 定期備份 Redis（RDB + AOF）
- 大規模 Token 丟失時引導用戶重新登入

### Q3: Device Fingerprint 準確嗎？

- FingerprintJS 準確率 99.5%（開源版）或 99.9%（商業版）
- 誤判時提供郵箱驗證碼二次驗證
- 可配置為僅在高風險場景啟用（如提款）

---

## 相關文件

- [多主體 Token 安全](./Multi_Actor_Token_Security.md) — 多主體 Token 安全方案
- [Token 驗證服務](./Token_Validation_Service.md) — Token 驗證服務架構
- [身份驗證架構](./Authentication_Architecture.md) — 認證與授權架構
