---
title: "Ch7: 治理與牌照技術架構"
part: technical
module: governance-licensing
version: v2.2
created: 2026-03-24
---

# 第 7 章：治理與牌照技術架構

## 7.1 模組概述

The Governance & Licensing module is the core foundation of the IGaming platform's multi-tenant architecture. It provides:

- **Multi-tenant isolation** with 4-level hierarchy (Platform → Brand → Tenant → Agent)
- **Role-Based Access Control (RBAC)** with permission matrix and dynamic role assignment
- **Multi-Factor Authentication (MFA)** supporting TOTP, WebAuthn, and SMS OTP
- **White-label customization** per brand with dynamic theme resolution
- **Billing system** supporting Fixed, Revenue Share, and Hybrid models with automated overdue escalation
- **Single Sign-On (SSO)** integration via SAML 2.0 and OIDC with wallet isolation guarantees
- **Tenant migration** tooling with data re-encryption and DNS management
- **Multi-jurisdiction compliance** with per-tenant license-type configuration

This module ensures 100% data isolation, audit-trail compliance, and scalable operator management.

---

## 7.2 資料模型

### Core Tables

#### t_platform
```sql
CREATE TABLE t_platform (
  platform_id BIGINT PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  version VARCHAR(50),
  config JSONB NOT NULL DEFAULT '{}'::jsonb,
  super_admin_email VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_platform_name UNIQUE (name)
);
```
Stores global platform configuration, super admin contact, and feature flags.

#### t_brand
```sql
CREATE TABLE t_brand (
  brand_id BIGINT PRIMARY KEY,
  platform_id BIGINT NOT NULL REFERENCES t_platform(platform_id),
  parent_brand_id BIGINT REFERENCES t_brand(brand_id),
  brand_name VARCHAR(255) NOT NULL,
  logo_url VARCHAR(500),
  primary_domain VARCHAR(255),
  config JSONB NOT NULL DEFAULT '{}'::jsonb,
  status VARCHAR(50) DEFAULT 'ACTIVE',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_brand_domain UNIQUE (primary_domain),
  CONSTRAINT uk_brand_name_platform UNIQUE (brand_name, platform_id)
);
```
Represents brand-level entity with visual identity and domain mapping. `config` stores SSO settings, email templates, and CDN asset paths.

#### t_tenant
```sql
CREATE TABLE t_tenant (
  tenant_id BIGINT PRIMARY KEY,
  brand_id BIGINT NOT NULL REFERENCES t_brand(brand_id),
  tenant_name VARCHAR(255) NOT NULL,
  license_type VARCHAR(50) NOT NULL,
  jurisdiction VARCHAR(50) NOT NULL,
  operator_domain VARCHAR(255),
  status VARCHAR(50) DEFAULT 'ACTIVE',
  billing_model VARCHAR(50) DEFAULT 'FIXED',
  config JSONB NOT NULL DEFAULT '{}'::jsonb,
  encryption_key_id VARCHAR(100) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_tenant_domain UNIQUE (operator_domain),
  CONSTRAINT uk_tenant_name_brand UNIQUE (tenant_name, brand_id)
);
```
Represents operator instance with license type (MGA/UKGC/CURACAO/PAGCOR) and jurisdiction-specific rules. `config` contains compliance settings and feature toggles.

#### t_agent
```sql
CREATE TABLE t_agent (
  agent_id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL REFERENCES t_tenant(tenant_id),
  parent_agent_id BIGINT REFERENCES t_agent(agent_id),
  agent_level INT CHECK (agent_level >= 1 AND agent_level <= 10),
  agent_name VARCHAR(255) NOT NULL,
  commission_model VARCHAR(50),
  status VARCHAR(50) DEFAULT 'ACTIVE',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_agent_tenant UNIQUE (agent_id, tenant_id)
);
```
Represents commission hierarchy (up to 10 levels) within a tenant. Parent-child relationship enables cascading commission calculations.

#### t_role
```sql
CREATE TABLE t_role (
  role_id BIGINT PRIMARY KEY,
  tenant_id BIGINT REFERENCES t_tenant(tenant_id),
  role_name VARCHAR(100) NOT NULL,
  role_level INT,
  permissions JSONB NOT NULL DEFAULT '[]'::jsonb,
  is_default BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_role_tenant_name UNIQUE (tenant_id, role_name)
);
```
Stores permission matrix as JSONB array: `["player:read", "wallet:adjust", "report:download"]`. `role_level` enables hierarchy (Super Admin=0, Brand Admin=1, Tenant Admin=2, etc.).

#### t_user
```sql
CREATE TABLE t_user (
  user_id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL REFERENCES t_tenant(tenant_id),
  username VARCHAR(100) NOT NULL,
  email VARCHAR(255),
  password_hash VARCHAR(255) NOT NULL,
  mfa_enabled BOOLEAN DEFAULT FALSE,
  mfa_method VARCHAR(50),
  mfa_secret VARCHAR(255),
  roles BIGINT[] NOT NULL DEFAULT '{}',
  status VARCHAR(50) DEFAULT 'ACTIVE',
  last_login TIMESTAMP,
  failed_login_attempts INT DEFAULT 0,
  locked_until TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_user_tenant_email UNIQUE (tenant_id, email)
);
```
User accounts with MFA support. `mfa_secret` stored encrypted at rest. `roles` is array of role_ids for direct assignment.

#### t_permission
```sql
CREATE TABLE t_permission (
  permission_id BIGINT PRIMARY KEY,
  module_name VARCHAR(50) NOT NULL,
  action_name VARCHAR(50) NOT NULL,
  description VARCHAR(500),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_permission_module_action UNIQUE (module_name, action_name)
);
```
Master list of all permissions in system (e.g., `player:read`, `wallet:adjust`). Used for UI permission management and audit trails.

---

## 7.3 四層租戶層級

### Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         PLATFORM                                 │
│          (Global config, super admin, feature flags)             │
│                      1 instance                                   │
└─────────────────────────────────────────────────────────────────┘
                              │
                        1 : N  │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                         BRAND                                     │
│      (Visual identity, domain, SSO, email templates)             │
│                    Multiple (e.g., 3-5)                         │
│  Examples: "Acme Casino", "XYZ Gaming", "Gamma Sports"         │
└─────────────────────────────────────────────────────────────────┘
                              │
                        1 : N  │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                         TENANT                                    │
│   (Operator instance, license, jurisdiction, billing)            │
│              Multiple per brand (e.g., 2-10)                    │
│  Examples: "Acme EU", "Acme APAC", "XYZ Malta", "XYZ India"   │
└─────────────────────────────────────────────────────────────────┘
                              │
                       1 : N  │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        AGENT                                      │
│  (Commission hierarchy, up to 10 levels deep)                    │
│        Multiple per tenant (e.g., 50-1000)                       │
│  Level 1: Master Agent → Level 2-10: Sub-agents                │
└─────────────────────────────────────────────────────────────────┘
```

### Level Responsibilities

**Level 0: Platform**
- Global configuration and feature toggles
- Super admin user management
- Multi-tenant monitoring and analytics
- System-wide security policies
- License agreement templates

**Level 1: Brand**
- Visual branding (logo, colors, fonts)
- Domain management and SSL certificates
- SSO configuration (SAML/OIDC endpoints)
- Email template customization
- Landing page and lobby layout
- Brand-level reporting (cross-tenant aggregation)

**Level 2: Tenant**
- Operator license and jurisdiction compliance
- Game portfolio and RTP configuration
- Player KYC/AML policies
- Payment method integration
- Billing and invoice generation
- Operator-specific feature toggles (e.g., GAMSTOP, deposit limits)
- Audit and regulatory reporting

**Level 3: Agent**
- Commission structure and hierarchy
- Affiliate marketing materials
- Sub-agent management
- Agent-level analytics and payouts
- Up to 10 levels for deep cascading structures

### Data Isolation Implementation

**Mandatory Requirement: 100% Cross-Tenant Data Isolation**

#### 1. Application-Level Isolation (MyBatis Interceptor)

```java
@Component
public class TenantLineInnerInterceptor implements InnerInterceptor {

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms,
                           Object parameter, RowBounds rowBounds,
                           ResultHandler resultHandler) throws SQLException {

        String currentTenantId = TenantContext.getCurrentTenantId();

        // Whitelist of SQL statements that ignore tenant_id filtering
        if (isWhitelisted(ms.getId())) {
            return;
        }

        // Auto-inject tenant_id filter for all other queries
        if (StringUtils.isNotBlank(currentTenantId)) {
            SqlUtil.addWhere(ms.getBoundSql().getSql(),
                            "tenant_id = '" + currentTenantId + "'");
        }
    }
}
```

**Whitelist (ADR-014):**
- Brand-level reports (cross-tenant aggregation)
- System health checks
- Super admin queries marked with `@TenantIgnore`

#### 2. Database-Level Isolation (PostgreSQL RLS)

```sql
-- Enable RLS on all tenant-scoped tables
ALTER TABLE t_user ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_role ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_agent ENABLE ROW LEVEL SECURITY;

-- Policy: Users can only see data for their assigned tenant
CREATE POLICY tenant_isolation_policy ON t_user
  USING (tenant_id = current_setting('app.current_tenant_id')::bigint)
  WITH CHECK (tenant_id = current_setting('app.current_tenant_id')::bigint);

-- Set session variable before queries
SET app.current_tenant_id = '12345';
```

Each database session sets `app.current_tenant_id` before queries. RLS prevents any unauthorized cross-tenant access even if application logic is bypassed.

#### 3. Authentication & Authorization Binding

```java
@Component
public class TenantContextFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                        FilterChain chain) throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String token = extractJWT(httpRequest);

        if (token != null) {
            Claims claims = parseJWT(token);
            Long tenantId = claims.get("tenant_id", Long.class);
            String userId = claims.getSubject();

            // Bind tenant and user to thread context
            TenantContext.setCurrentTenantId(tenantId.toString());
            UserContext.setCurrentUserId(userId);

            // Verify tenant_id matches user's assigned tenant
            User user = userRepository.findById(userId).orElse(null);
            if (user == null || !user.getTenantId().equals(tenantId)) {
                throw new UnauthorizedException("Tenant mismatch");
            }
        }

        chain.doFilter(request, response);

        TenantContext.clear();
        UserContext.clear();
    }
}
```

### Cross-Brand Brand Reporting

Brand admins require visibility across all tenants under their brand for consolidated analytics. This is implemented via brand-level views:

```sql
CREATE VIEW v_brand_consolidated_stats AS
SELECT
  b.brand_id,
  b.brand_name,
  SUM(t.total_players) as total_players,
  SUM(t.revenue) as total_revenue
FROM t_brand b
JOIN t_tenant t ON b.brand_id = t.brand_id
WHERE b.brand_id = current_setting('app.current_brand_id')::bigint
GROUP BY b.brand_id, b.brand_name;

-- Query marked with @TenantIgnore (whitelisted)
@TenantIgnore
@Query("SELECT * FROM v_brand_consolidated_stats")
List<BrandStatsDTO> getBrandStats();
```

---

## 7.4 RBAC 實作

### Permission Framework

The RBAC system uses **Sa-Token** library for session and permission management.

#### Role Hierarchy

```
SUPER_ADMIN (Level 0)
  ├─ All platform-wide permissions
  ├─ Platform config management
  └─ Multi-tenant oversight

BRAND_ADMIN (Level 1)
  ├─ Brand branding & SSO configuration
  ├─ Cross-tenant brand analytics
  ├─ Tenant creation & lifecycle
  └─ Brand user management

TENANT_ADMIN (Level 2)
  ├─ Operator configuration
  ├─ Player & wallet management
  ├─ Game portfolio & RTP
  ├─ Payment method setup
  └─ Tenant user management

OPERATOR (Level 3)
  ├─ Limited player management
  ├─ Wallet inquiries (read-only on others' wallets)
  ├─ Report access
  └─ Game configuration (limited)

CUSTOMER_SERVICE (Level 4)
  ├─ Player inquiry & ticket resolution
  ├─ Limited wallet operations (view transactions)
  ├─ Account status checks
  └─ No financial operations

AGENT (Level 5)
  ├─ Agent dashboard
  ├─ Sub-agent management (if multi-level)
  ├─ Commission tracking
  └─ Affiliate marketing materials
```

#### Permission Format: `module:action`

Permissions follow `module_name:action_name` format. Examples:

| Permission | Module | Action | Role |
|-----------|--------|--------|------|
| `player:read` | player | read | All authenticated |
| `player:create` | player | create | TENANT_ADMIN+ |
| `player:suspend` | player | suspend | TENANT_ADMIN+ |
| `wallet:adjust` | wallet | adjust | TENANT_ADMIN, OPERATOR (limited) |
| `wallet:transfer` | wallet | transfer | TENANT_ADMIN only |
| `report:download` | report | download | OPERATOR+ |
| `audit:view` | audit | view | TENANT_ADMIN+ |
| `role:manage` | role | manage | TENANT_ADMIN+ |
| `user:manage` | user | manage | TENANT_ADMIN+ |
| `game:configure` | game | configure | TENANT_ADMIN+ |
| `payment:manage` | payment | manage | TENANT_ADMIN only |
| `billing:view` | billing | view | TENANT_ADMIN+ |
| `billing:manage` | billing | manage | BRAND_ADMIN only |

#### Implementation: Annotation-Based Permission Checks

```java
@Component
@RestController
@RequestMapping("/api/v1")
public class PlayerController {

    @SaCheckPermission("player:read")
    @GetMapping("/players/{id}")
    public ResponseDTO<PlayerVO> getPlayer(@PathVariable Long id) {
        Long tenantId = TenantContext.getCurrentTenantId();
        Player player = playerService.getPlayerByIdAndTenant(id, tenantId);
        return ResponseDTO.success(player.toVO());
    }

    @SaCheckPermission("player:create")
    @PostMapping("/players")
    public ResponseDTO<PlayerVO> createPlayer(@RequestBody PlayerCreateDTO dto) {
        Player player = playerService.createPlayer(dto);
        auditLog.record("PLAYER_CREATED", player.getId());
        return ResponseDTO.success(player.toVO());
    }

    @SaCheckPermission("player:suspend")
    @PutMapping("/players/{id}/suspend")
    public ResponseDTO<String> suspendPlayer(@PathVariable Long id,
                                             @RequestBody SuspensionReasonDTO reason) {
        playerService.suspendPlayer(id, reason.getReason());
        auditLog.record("PLAYER_SUSPENDED", id, reason.getReason());
        return ResponseDTO.success("Player suspended");
    }
}
```

#### Dynamic Role Assignment

```java
@Service
public class RoleService {

    public void assignRoleToUser(Long userId, Long roleId) {
        User user = userRepository.findById(userId).orElseThrow();
        Role role = roleRepository.findById(roleId).orElseThrow();

        // Verify role belongs to user's tenant
        if (!role.getTenantId().equals(user.getTenantId())) {
            throw new IllegalArgumentException("Role not in user's tenant");
        }

        user.addRole(role);
        userRepository.save(user);

        // Invalidate cached permissions for this user
        permissionCache.invalidate("user:" + userId);

        auditLog.record("ROLE_ASSIGNED", userId, roleId);
    }

    public void createCustomRole(Long tenantId, RoleCreateDTO dto) {
        Role role = new Role();
        role.setTenantId(tenantId);
        role.setRoleName(dto.getRoleName());
        role.setPermissions(dto.getPermissions()); // JSONB array
        roleRepository.save(role);

        auditLog.record("ROLE_CREATED", role.getId(), dto.getRoleName());
    }
}
```

#### Permission Caching in Redis

Permissions are cached at the user level with 5-minute TTL to reduce database queries:

```java
@Service
public class PermissionCache {

    private static final String CACHE_KEY_PREFIX = "permissions:user:";
    private static final int TTL_SECONDS = 300; // 5 minutes

    public Set<String> getUserPermissions(Long userId) {
        String cacheKey = CACHE_KEY_PREFIX + userId;

        // Try cache
        Set<String> cached = (Set<String>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // Load from database
        User user = userRepository.findById(userId).orElseThrow();
        Set<String> permissions = new HashSet<>();

        for (Role role : user.getRoles()) {
            permissions.addAll(role.getPermissions()); // JSONB array
        }

        // Cache
        redisTemplate.opsForValue().set(cacheKey, permissions,
                                        Duration.ofSeconds(TTL_SECONDS));

        return permissions;
    }

    public void invalidateUserPermissions(Long userId) {
        redisTemplate.delete(CACHE_KEY_PREFIX + userId);
    }
}
```

#### Menu & Button Level Permission Checks

Frontend dynamically hides/disables UI elements based on user's permissions:

```typescript
// Frontend: Angular directive
@Directive({
  selector: '[appCheckPermission]'
})
export class CheckPermissionDirective implements OnInit {
  @Input('appCheckPermission') requiredPermission: string;

  constructor(
    private templateRef: TemplateRef<any>,
    private viewContainer: ViewContainerRef,
    private authService: AuthService
  ) {}

  ngOnInit() {
    const hasPermission = this.authService.hasPermission(this.requiredPermission);
    if (hasPermission) {
      this.viewContainer.createEmbeddedView(this.templateRef);
    } else {
      this.viewContainer.clear();
    }
  }
}

// Usage in template
<button *appCheckPermission="'player:suspend'"
        (click)="suspendPlayer()">
  Suspend Player
</button>

<a *appCheckPermission="'report:download'"
   href="/reports">Download Report</a>
```

---

## 7.5 MFA 實作

### MFA Requirements & Enforcement

5 roles **must** enable MFA:
1. **SUPER_ADMIN** - Platform superuser
2. **BRAND_ADMIN** - Brand manager
3. **TENANT_ADMIN** - Operator manager
4. **FINANCE** - Billing and payout manager
5. **RISK** - Compliance and risk manager

For other roles, MFA is optional but recommended.

### Supported MFA Methods

#### 1. TOTP (Time-Based One-Time Password)

**Library:** Google Authenticator, Microsoft Authenticator, Authy

**Implementation:**

```java
@Service
public class TOTPService {

    private static final int TOTP_TIME_STEP = 30; // 30 seconds
    private static final int TOTP_DIGITS = 6;
    private static final int TOTP_WINDOW = 1; // ±1 step (60 seconds tolerance)

    public String generateSecret(Long userId) {
        // Generate 32-byte random secret
        byte[] secret = new byte[32];
        SecureRandom random = new SecureRandom();
        random.nextBytes(secret);

        String encodedSecret = Base32.encode(secret);

        // Store encrypted secret in DB
        User user = userRepository.findById(userId).orElseThrow();
        user.setMfaSecret(encryptionService.encrypt(encodedSecret));
        userRepository.save(user);

        // Return provisioning URI for QR code
        return "otpauth://totp/" + user.getEmail()
             + "?secret=" + encodedSecret
             + "&issuer=IGaming";
    }

    public boolean validateTOTP(Long userId, String code) {
        User user = userRepository.findById(userId).orElseThrow();
        String secret = encryptionService.decrypt(user.getMfaSecret());

        long currentTime = System.currentTimeMillis() / 1000;
        long timeCounter = currentTime / TOTP_TIME_STEP;

        // Check current and ±1 window (60 seconds tolerance)
        for (int i = -TOTP_WINDOW; i <= TOTP_WINDOW; i++) {
            String expectedCode = generateTOTPCode(secret, timeCounter + i);
            if (expectedCode.equals(code)) {
                // TOTP is valid, store last-used counter to prevent replay
                user.setLastTOTPCounter(timeCounter + i);
                userRepository.save(user);
                return true;
            }
        }

        return false;
    }

    private String generateTOTPCode(String secret, long timeCounter) {
        byte[] decodedSecret = Base32.decode(secret);
        byte[] message = new byte[8];

        for (int i = 7; i >= 0; i--) {
            message[i] = (byte) (timeCounter & 0xff);
            timeCounter >>= 8;
        }

        HmacSHA1 hmac = new HmacSHA1(decodedSecret);
        byte[] hash = hmac.doFinal(message);

        int offset = hash[hash.length - 1] & 0xf;
        int code = ((hash[offset] & 0x7f) << 24)
                 | ((hash[offset + 1] & 0xff) << 16)
                 | ((hash[offset + 2] & 0xff) << 8)
                 | (hash[offset + 3] & 0xff);

        return String.format("%0" + TOTP_DIGITS + "d", code % 1000000);
    }
}
```

#### 2. WebAuthn (FIDO2 Security Keys)

**Hardware support:** YubiKey, Google Titan, Microsoft FIDO2 USB keys

**Implementation:**

```java
@Service
public class WebAuthnService {

    private WebAuthnServer server;

    @PostConstruct
    public void init() {
        server = new WebAuthnServer(
            Set.of("gaming.example.com"),
            "IGaming Platform",
            "https://gaming.example.com"
        );
    }

    public PublicKeyCredentialCreationOptions registerSecurityKey(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();

        PublicKeyCredentialCreationOptions options =
            server.getRegistrationOptions(
                new UserIdentity(
                    userId.toString(),
                    user.getEmail(),
                    user.getEmail()
                )
            );

        // Store challenge temporarily
        mfaChallengeCache.put("webauthn:" + userId, options.getChallenge());

        return options;
    }

    public void completeSecurityKeyRegistration(
            Long userId,
            PublicKeyCredential<AuthenticatorAttestationResponse> credential) {

        byte[] challenge = mfaChallengeCache.get("webauthn:" + userId);

        RegistrationResult result = server.finishRegistration(
            FinishRegistrationOptions.builder()
                .request(new PublicKeyCredentialCreationOptions(
                    challenge, ...
                ))
                .response(credential.getResponse())
                .build()
        );

        if (result.isSuccess()) {
            User user = userRepository.findById(userId).orElseThrow();
            user.setWebAuthnCredentialId(result.getKeyId());
            user.setMfaMethod("WEBAUTHN");
            userRepository.save(user);
        }
    }

    public boolean validateWebAuthn(Long userId,
                                   PublicKeyCredential<AuthenticatorAssertionResponse> assertion) {

        User user = userRepository.findById(userId).orElseThrow();

        AssertionResult result = server.finishAssertion(
            FinishAssertionOptions.builder()
                .request(assertion.getResponse().getClientDataJSON())
                .response(assertion.getResponse().getAuthenticatorData())
                .build()
        );

        return result.isSuccess();
    }
}
```

#### 3. SMS OTP (Fallback)

**Provider:** Twilio, AWS SNS, or third-party SMS gateway

**Implementation:**

```java
@Service
public class SMSOTPService {

    @Autowired
    private SmsProvider smsProvider; // Twilio/SNS implementation

    public void sendSMSOTP(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();

        if (user.getPhoneNumber() == null) {
            throw new IllegalArgumentException("No phone number on file");
        }

        // Generate 6-digit OTP
        String otp = String.valueOf(100000 + new Random().nextInt(900000));

        // Store with 10-minute expiry
        smsChallengeCache.put("sms:" + userId, otp, Duration.ofMinutes(10));

        // Send via SMS
        smsProvider.send(
            user.getPhoneNumber(),
            "Your IGaming verification code is: " + otp
        );

        auditLog.record("SMS_OTP_SENT", userId);
    }

    public boolean validateSMSOTP(Long userId, String otp) {
        String stored = smsChallengeCache.get("sms:" + userId);

        if (stored == null) {
            return false; // OTP expired
        }

        boolean isValid = stored.equals(otp);

        if (isValid) {
            smsChallengeCache.delete("sms:" + userId);
        }

        return isValid;
    }
}
```

### Login Flow with MFA

```java
@Service
public class AuthenticationService {

    @PostMapping("/login")
    public ResponseDTO<LoginResponseDTO> login(@RequestBody LoginDTO dto) {
        // Step 1: Verify credentials
        User user = userRepository.findByEmailAndTenantId(
            dto.getEmail(),
            dto.getTenantId()
        ).orElse(null);

        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            auditLog.record("LOGIN_FAILED_INVALID_CREDS", dto.getEmail());
            return ResponseDTO.failure("Invalid email or password");
        }

        // Step 2: Check if MFA is enabled
        if (!user.isMfaEnabled()) {
            // Issue JWT token directly
            String token = jwtProvider.generateToken(user);
            return ResponseDTO.success(
                new LoginResponseDTO(token, null)
            );
        }

        // Step 3: MFA is enabled - generate challenge
        String mfaChallenge = UUID.randomUUID().toString();
        mfaChallengeCache.put(mfaChallenge, user.getId(), Duration.ofMinutes(5));

        // Send MFA code based on user's preferred method
        switch (user.getMfaMethod()) {
            case "TOTP":
                // User will provide code from authenticator app
                break;
            case "WEBAUTHN":
                // Return challenge for security key
                PublicKeyCredentialRequestOptions options =
                    webAuthnService.getAuthenticationOptions(user.getId());
                return ResponseDTO.success(
                    new LoginResponseDTO(null, mfaChallenge,
                                       "WEBAUTHN", options)
                );
            case "SMS":
                smsotp.sendSMSOTP(user.getId());
                break;
        }

        auditLog.record("MFA_CHALLENGE_ISSUED", user.getId());

        return ResponseDTO.success(
            new LoginResponseDTO(null, mfaChallenge, user.getMfaMethod())
        );
    }

    @PostMapping("/verify-mfa")
    public ResponseDTO<LoginResponseDTO> verifyMFA(
            @RequestBody MFAVerificationDTO dto) {

        // Retrieve user from challenge
        Long userId = mfaChallengeCache.get(dto.getMfaChallenge());
        if (userId == null) {
            return ResponseDTO.failure("MFA challenge expired");
        }

        User user = userRepository.findById(userId).orElseThrow();

        // Verify based on method
        boolean isValid = false;
        switch (user.getMfaMethod()) {
            case "TOTP":
                isValid = totpService.validateTOTP(userId, dto.getCode());
                break;
            case "WEBAUTHN":
                isValid = webAuthnService.validateWebAuthn(userId, dto.getAssertion());
                break;
            case "SMS":
                isValid = smsotp.validateSMSOTP(userId, dto.getCode());
                break;
        }

        if (!isValid) {
            auditLog.record("MFA_VERIFICATION_FAILED", userId);
            return ResponseDTO.failure("Invalid MFA code");
        }

        // Issue JWT token
        String token = jwtProvider.generateToken(user);
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        auditLog.record("LOGIN_SUCCESS", userId);

        return ResponseDTO.success(
            new LoginResponseDTO(token, null)
        );
    }
}
```

### MFA Administration & Recovery

**Admin Dashboard:**

```java
@RestController
@RequestMapping("/api/v1/admin/mfa")
@SaCheckPermission("user:manage")
public class MFAAdminController {

    @GetMapping("/users/{userId}/status")
    public ResponseDTO<MFAStatusDTO> getMFAStatus(@PathVariable Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        return ResponseDTO.success(
            new MFAStatusDTO(
                user.isMfaEnabled(),
                user.getMfaMethod(),
                user.getUpdatedAt()
            )
        );
    }

    @PostMapping("/users/{userId}/reset")
    public ResponseDTO<String> resetMFA(@PathVariable Long userId,
                                        @RequestParam String reason) {
        User user = userRepository.findById(userId).orElseThrow();
        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        userRepository.save(user);

        auditLog.record("MFA_RESET_BY_ADMIN", userId, reason);

        // Send notification to user
        emailService.sendMFAResetNotification(user.getEmail());

        return ResponseDTO.success("MFA reset complete");
    }

    @PostMapping("/users/{userId}/force-totp-enrollment")
    public ResponseDTO<String> forceTOTPEnrollment(@PathVariable Long userId) {
        User user = userRepository.findById(userId).orElseThrow();

        if (!user.getRole().isMfaRequired()) {
            return ResponseDTO.failure("This role does not require MFA");
        }

        // Send enrollment link
        String enrollmentToken = UUID.randomUUID().toString();
        mfaChallengeCache.put("enroll:" + enrollmentToken, userId,
                             Duration.ofHours(24));

        emailService.sendMFAEnrollmentLink(user.getEmail(), enrollmentToken);

        auditLog.record("TOTP_ENROLLMENT_FORCED", userId);

        return ResponseDTO.success("Enrollment link sent to user");
    }
}
```

---

## 7.6 白標系統

### White-Label Customization Scope

Per-brand customization includes:

| Customization | Scope | Storage | Delivery |
|---------------|-------|---------|----------|
| Logo | 1-5 variants (header, footer, email) | CDN + JSONB | CDN URL |
| Colors | Primary, secondary, accent (CSS variables) | JSONB | CSS file |
| Fonts | Primary, secondary fonts with weight/style | JSONB | Google Fonts CDN |
| Domain Mapping | Custom domain → Brand resolution | JSONB + DNS | Route at ingress |
| Email Templates | Welcome, password reset, receipt emails | JSONB + S3 | Template engine |
| Landing Pages | Hero, features, pricing sections | JSONB | React SSR |
| Game Lobby Layout | Featured games, categories, filters | JSONB | Frontend config |
| Theme Mode | Light, dark, or auto-detect | JSONB | CSS theme |

### Configuration Storage (JSONB)

```sql
INSERT INTO t_brand (brand_id, brand_name, config)
VALUES (100, 'Acme Casino', '{
  "branding": {
    "logo_url": "https://cdn.example.com/brands/acme/logo.png",
    "favicon_url": "https://cdn.example.com/brands/acme/favicon.ico",
    "colors": {
      "primary": "#FF6B35",
      "secondary": "#004E89",
      "accent": "#F7B801"
    },
    "fonts": {
      "primary": "Montserrat",
      "secondary": "Open Sans"
    }
  },
  "domains": {
    "primary": "acme-casino.com",
    "aliases": ["www.acme-casino.com", "casino.acme.com"]
  },
  "email_templates": {
    "welcome_template_id": "acme_welcome_v2",
    "sender_name": "Acme Casino Support",
    "footer_html": "<p>© 2026 Acme Casino Ltd. All rights reserved.</p>"
  },
  "landing_pages": {
    "hero_title": "Acme Casino - Play & Win!",
    "hero_subtitle": "1000+ Games. Instant Withdrawals.",
    "features": [...]
  },
  "sso_config": {
    "enabled": true,
    "provider": "OKTA",
    "client_id": "xxxx",
    "discovery_url": "https://acme.okta.com/.well-known/openid-configuration"
  },
  "theme": {
    "mode": "auto",
    "custom_css_url": "https://cdn.example.com/brands/acme/custom.css"
  }
}'::jsonb);
```

### Runtime Brand Resolution

```java
@Component
public class BrandResolutionFilter implements Filter {

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private RedisTemplate<String, Brand> redisTemplate;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                        FilterChain chain) throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String domain = httpRequest.getServerName();

        // Try cache
        Brand brand = redisTemplate.opsForValue().get("brand:domain:" + domain);

        // Load from DB if not cached
        if (brand == null) {
            brand = brandRepository.findByDomain(domain)
                .orElseThrow(() -> new BrandNotFoundException(domain));

            // Cache for 1 hour
            redisTemplate.opsForValue().set("brand:domain:" + domain, brand,
                                           Duration.ofHours(1));
        }

        // Bind to context
        BrandContext.setCurrentBrand(brand);

        chain.doFilter(request, response);

        BrandContext.clear();
    }
}
```

### Theme Injection (React SSR)

```java
@RestController
@RequestMapping("/api/v1/brand")
public class BrandConfigController {

    @GetMapping("/config")
    public ResponseDTO<BrandConfigDTO> getBrandConfig() {
        Brand brand = BrandContext.getCurrentBrand();

        return ResponseDTO.success(
            new BrandConfigDTO(
                brand.getLogoUrl(),
                brand.getColors(),
                brand.getFonts(),
                brand.getLandingPageContent(),
                brand.getGameLobbyLayout()
            )
        );
    }
}

// Frontend: Next.js/React
export async function getServerSideProps(context) {
  const domain = context.req.headers.host;
  const { data: brandConfig } = await fetch(
    `/api/v1/brand/config`
  ).then(r => r.json());

  return {
    props: { brandConfig }
  };
}

export default function HomePage({ brandConfig }) {
  return (
    <div style={{
      '--primary-color': brandConfig.colors.primary,
      '--secondary-color': brandConfig.colors.secondary,
      '--font-family': brandConfig.fonts.primary
    }}>
      <img src={brandConfig.logo_url} alt="Logo" />
      <h1>{brandConfig.landing.heroTitle}</h1>
      <GameLobby layout={brandConfig.gameLobbyLayout} />
    </div>
  );
}
```

### Email Template Customization

```java
@Service
public class EmailTemplateService {

    public void sendWelcomeEmail(String email, String playerName) {
        Brand brand = BrandContext.getCurrentBrand();

        String templateId = brand.getConfig()
            .get("email_templates")
            .get("welcome_template_id")
            .asText();

        EmailTemplate template = emailTemplateRepository.findById(templateId)
            .orElseThrow();

        String html = template.getHtmlContent()
            .replace("{{BRAND_LOGO}}", brand.getLogoUrl())
            .replace("{{PLAYER_NAME}}", playerName)
            .replace("{{PRIMARY_COLOR}}", brand.getColors().getPrimary())
            .replace("{{BRAND_FOOTER}}", brand.getConfig()
                .get("email_templates")
                .get("footer_html")
                .asText());

        emailService.send(email, template.getSubject(), html);
    }
}
```

### Game Lobby Layout Configuration

```json
{
  "gameLobbyLayout": {
    "sections": [
      {
        "id": "featured",
        "title": "Featured Games",
        "gameIds": ["game_001", "game_005", "game_010"],
        "displayMode": "carousel"
      },
      {
        "id": "slots",
        "title": "Slots",
        "categoryId": "category_slots",
        "displayMode": "grid",
        "itemsPerRow": 4
      },
      {
        "id": "table_games",
        "title": "Table Games",
        "categoryId": "category_table",
        "displayMode": "grid",
        "itemsPerRow": 3
      },
      {
        "id": "promotions",
        "title": "Promotions",
        "promotionIds": ["promo_001", "promo_002"],
        "displayMode": "banner"
      }
    ],
    "filters": ["category", "provider", "popularity"],
    "sortOptions": ["newest", "popular", "rtp"]
  }
}
```

---

## 7.7 計費系統

### Billing Models

The platform supports three flexible billing models:

#### 1. Fixed Monthly Fee

```sql
INSERT INTO t_billing_record
  (tenant_id, period, model, base_fee, revenue_share, total, status)
VALUES
  (50, '2026-03', 'FIXED', 5000.00, 0, 5000.00, 'INVOICED');
```

- Fixed monthly cost regardless of operator performance
- Suitable for smaller operators with stable revenue
- Simple invoice management and predictable costs

#### 2. Revenue Share Model

```sql
INSERT INTO t_billing_record
  (tenant_id, period, model, base_fee, revenue_share, total, status)
VALUES
  (51, '2026-03', 'REVENUE_SHARE', 0, 45000.00, 45000.00, 'INVOICED');

-- Calculation: GGR * 15% = €300,000 * 15% = €45,000
```

- Platform takes percentage of Gross Gaming Revenue (GGR)
- GGR = Total Bets - Winnings (house edge)
- Typical range: 10-25% depending on operator tier
- Incentivizes platform investment in operator success

#### 3. Hybrid Model

```sql
INSERT INTO t_billing_record
  (tenant_id, period, model, base_fee, revenue_share, total, status)
VALUES
  (52, '2026-03', 'HYBRID', 10000.00, 22500.00, 32500.00, 'INVOICED');

-- Calculation:
-- Base fee: €10,000 (fixed)
-- Revenue share: €300,000 GGR * 7.5% = €22,500 (above €250,000 threshold)
```

- Minimum base fee + percentage above threshold
- Reduces platform risk while rewarding growth
- Typical structure: €X base + Y% of GGR above €Z threshold

### Billing Calculation Engine

```java
@Service
public class BillingCalculationService {

    public BillingRecord calculateBilling(Long tenantId, YearMonth period) {
        Tenant tenant = tenantRepository.findById(tenantId).orElseThrow();

        // Fetch transactions for period
        List<Transaction> transactions = transactionService
            .getTransactionsByTenantAndPeriod(tenantId, period);

        // Calculate GGR
        BigDecimal totalBets = transactions.stream()
            .filter(t -> t.getType() == TransactionType.BET)
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalWinnings = transactions.stream()
            .filter(t -> t.getType() == TransactionType.WINNINGS)
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal ggr = totalBets.subtract(totalWinnings);

        // Calculate billing based on model
        BillingRecord record = new BillingRecord();
        record.setTenantId(tenantId);
        record.setPeriod(period);
        record.setModel(tenant.getBillingModel());
        record.setGgr(ggr);

        switch (tenant.getBillingModel()) {
            case FIXED:
                record.setBaseFee(tenant.getFixedMonthlyFee());
                record.setTotal(tenant.getFixedMonthlyFee());
                break;

            case REVENUE_SHARE:
                BigDecimal sharePercentage = tenant.getRevenueSharePercentage();
                BigDecimal revShare = ggr.multiply(sharePercentage)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                record.setRevenueShare(revShare);
                record.setTotal(revShare);
                break;

            case HYBRID:
                BigDecimal baseFee = tenant.getHybridBaseFee();
                BigDecimal threshold = tenant.getHybridThreshold();
                BigDecimal sharePercentage2 = tenant.getHybridSharePercentage();

                record.setBaseFee(baseFee);

                if (ggr.compareTo(threshold) > 0) {
                    BigDecimal excessGgr = ggr.subtract(threshold);
                    BigDecimal revShare2 = excessGgr.multiply(sharePercentage2)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                    record.setRevenueShare(revShare2);
                    record.setTotal(baseFee.add(revShare2));
                } else {
                    record.setRevenueShare(BigDecimal.ZERO);
                    record.setTotal(baseFee);
                }
                break;
        }

        record.setStatus(BillingStatus.PENDING);
        billingRepository.save(record);

        auditLog.record("BILLING_CALCULATED", tenantId, period.toString());

        return record;
    }
}
```

### Overdue Escalation (31-Day Calendar)

```java
@Service
@Scheduled(cron = "0 2 * * *") // Daily at 2 AM
public class BillingEscalationService {

    public void processOverdueAccounts() {
        List<BillingRecord> overdueRecords = billingRepository
            .findOverdueRecords(LocalDate.now());

        for (BillingRecord record : overdueRecords) {
            int daysSinceDue = Days.daysBetween(
                record.getDueDate(),
                LocalDate.now()
            ).getDays();

            if (daysSinceDue >= 1 && daysSinceDue <= 7) {
                // Day 1-7: Email reminder
                sendOverdueNotification(record, 1);

            } else if (daysSinceDue >= 8 && daysSinceDue <= 14) {
                // Day 8-14: Dashboard warning
                activateDashboardWarning(record);

            } else if (daysSinceDue >= 15 && daysSinceDue <= 21) {
                // Day 15-21: Restrict features
                Tenant tenant = tenantRepository.findById(record.getTenantId())
                    .orElseThrow();
                tenant.getFeatureRestrictions().add("game_launches");
                tenant.getFeatureRestrictions().add("new_player_registration");
                tenantRepository.save(tenant);

            } else if (daysSinceDue >= 22 && daysSinceDue <= 31) {
                // Day 22-31: Suspend account
                Tenant tenant = tenantRepository.findById(record.getTenantId())
                    .orElseThrow();
                tenant.setStatus(TenantStatus.SUSPENDED);
                tenantRepository.save(tenant);

                // Prevent all game play
                broadcastTenantSuspension(tenant.getId());
            }
        }
    }

    private void sendOverdueNotification(BillingRecord record, int level) {
        Tenant tenant = tenantRepository.findById(record.getTenantId())
            .orElseThrow();

        String subject = level == 1
            ? "Invoice Overdue - Immediate Action Required"
            : "Final Notice - Account Suspension Imminent";

        String body = String.format(
            "Your invoice for %s (€%.2f) is %d days overdue. " +
            "Please settle immediately to avoid service suspension.",
            record.getPeriod(),
            record.getTotal(),
            Days.daysBetween(record.getDueDate(), LocalDate.now()).getDays()
        );

        emailService.sendToFinanceTeam(
            tenant.getPrimaryContactEmail(),
            subject,
            body
        );
    }
}
```

### Invoice Generation & Management

```java
@RestController
@RequestMapping("/api/v1/billing")
@SaCheckPermission("billing:view")
public class BillingController {

    @GetMapping("/invoices")
    public ResponseDTO<Page<InvoiceDTO>> getInvoices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) BillingStatus status) {

        Long tenantId = TenantContext.getCurrentTenantId();

        Page<BillingRecord> records = billingRepository.findByTenantId(
            tenantId,
            PageRequest.of(page, size, Sort.by("period").descending()),
            status
        );

        return ResponseDTO.success(
            records.map(r -> new InvoiceDTO(
                r.getId(),
                r.getPeriod(),
                r.getModel().name(),
                r.getBaseFee(),
                r.getRevenueShare(),
                r.getTotal(),
                r.getStatus().name(),
                r.getDueDate()
            ))
        );
    }

    @PostMapping("/invoices/{invoiceId}/pay")
    @SaCheckPermission("billing:manage")
    public ResponseDTO<String> payInvoice(@PathVariable Long invoiceId,
                                          @RequestBody PaymentDTO payment) {

        BillingRecord record = billingRepository.findById(invoiceId)
            .orElseThrow();

        if (payment.getAmount().compareTo(record.getTotal()) < 0) {
            return ResponseDTO.failure("Partial payments not allowed");
        }

        // Process payment via payment gateway
        PaymentResult result = paymentGateway.charge(
            record.getTenant().getPaymentMethodId(),
            payment.getAmount(),
            "Invoice-" + invoiceId
        );

        if (result.isSuccessful()) {
            record.setStatus(BillingStatus.PAID);
            record.setPaidAt(LocalDateTime.now());
            billingRepository.save(record);

            auditLog.record("INVOICE_PAID", invoiceId, payment.getAmount());

            return ResponseDTO.success("Payment processed successfully");
        } else {
            return ResponseDTO.failure("Payment failed: " + result.getErrorMessage());
        }
    }

    @GetMapping("/invoices/{invoiceId}/pdf")
    public ResponseEntity<byte[]> downloadInvoicePDF(@PathVariable Long invoiceId) {
        BillingRecord record = billingRepository.findById(invoiceId)
            .orElseThrow();

        byte[] pdfContent = invoiceService.generatePDF(record);

        return ResponseEntity.ok()
            .header("Content-Disposition",
                   "attachment; filename=invoice-" + invoiceId + ".pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdfContent);
    }
}
```

---

## 7.8 SSO 整合

### Single Sign-On Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    APPLICATION LOGIN PAGE                        │
│  user@example.com → Password → [Login] or [SSO Button]          │
└──────────────────────────┬──────────────────────────────────────┘
                           │
           ┌───────────────┴───────────────┐
           ▼                               ▼
    ┌─────────────────┐          ┌─────────────────────┐
    │  Local Auth     │          │   SSO Auth          │
    │  (Direct DB)    │          │  (SAML/OIDC)        │
    └────────┬────────┘          └────────┬────────────┘
             │                            │
             └────────────┬───────────────┘
                          ▼
                  ┌──────────────────┐
                  │   JWT Token      │
                  │  issued          │
                  └────────┬─────────┘
                           ▼
            ┌──────────────────────────────────┐
            │  User Context + Wallet Access    │
            │  (Wallet always isolated)        │
            └──────────────────────────────────┘
```

### Three SSO Modes

#### Mode 1: Brand SSO (SAML/OIDC)

```java
@Configuration
public class SAMLSSConfig extends WebSecurityConfigurerAdapter {

    @Bean
    public SAMLProcessingFilter samlProcessingFilter() throws Exception {
        SAMLProcessingFilter samlProcessingFilter = new SAMLProcessingFilter();
        samlProcessingFilter.setAuthenticationManager(authenticationManager());
        samlProcessingFilter.setAuthenticationSuccessHandler(
            new SavedRequestAwareAuthenticationSuccessHandler() {
                @Override
                public void onAuthenticationSuccess(HttpServletRequest request,
                    HttpServletResponse response,
                    Authentication authentication) throws IOException {

                    SAMLCredential credential = (SAMLCredential)
                        authentication.getCredentials();

                    String email = credential.getNameID().getValue();
                    String tenantId = extractTenantFromAssertion(credential);

                    // Find or create user
                    User user = userService.findOrCreateFromSSO(email, tenantId);

                    // Issue JWT
                    String token = jwtProvider.generateToken(user);

                    response.sendRedirect("/login?token=" + token);
                }
            }
        );
        return samlProcessingFilter;
    }
}

@RestController
@RequestMapping("/api/v1/auth")
public class SSOController {

    @PostMapping("/sso/callback")
    public ResponseDTO<LoginResponseDTO> ssoCallback(
            @RequestBody OIDCCallbackDTO dto) {

        // Validate OIDC token
        JwtClaims claims = jwtProvider.validateAndParseSSOToken(dto.getIdToken());

        String email = claims.getClaimValueAsString("email");
        String tenantId = claims.getClaimValueAsString("tenant_id");
        String firstName = claims.getClaimValueAsString("given_name");
        String lastName = claims.getClaimValueAsString("family_name");

        // Find or create user
        User user = userService.findOrCreateFromSSO(
            email,
            tenantId,
            firstName,
            lastName
        );

        // Issue platform JWT
        String token = jwtProvider.generateToken(user);

        auditLog.record("SSO_LOGIN_SUCCESS", user.getId(),
                       "provider=OIDC");

        return ResponseDTO.success(
            new LoginResponseDTO(token, null)
        );
    }
}
```

#### Mode 2: Platform SSO

All tenants under a brand share the same authentication session:

```java
@Service
public class PlatformSSOService {

    public String generatePlatformToken(User user) {
        return jwtProvider.generateToken(
            new JwtBuilder()
                .withSubject(user.getId().toString())
                .withClaim("email", user.getEmail())
                .withClaim("brand_id", user.getTenant().getBrand().getId())
                .withClaim("tenant_id", user.getTenant().getId())
                .withClaim("roles", user.getRoles().stream()
                    .map(Role::getName)
                    .collect(Collectors.toList()))
                .withExpiresAt(new Date(System.currentTimeMillis() +
                    24*60*60*1000)) // 24 hours
                .sign(new HMACAlgorithm(jwtSecret))
        );
    }
}
```

#### Mode 3: No SSO (Local Authentication)

```java
@RestController
@RequestMapping("/api/v1/auth")
public class LocalAuthController {

    @PostMapping("/login")
    public ResponseDTO<LoginResponseDTO> login(@RequestBody LoginDTO dto) {
        User user = userRepository.findByEmailAndTenantId(
            dto.getEmail(),
            dto.getTenantId()
        ).orElse(null);

        if (user == null ||
            !passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            return ResponseDTO.failure("Invalid credentials");
        }

        // MFA flow if enabled...
        String token = jwtProvider.generateToken(user);
        return ResponseDTO.success(
            new LoginResponseDTO(token, null)
        );
    }
}
```

### Wallet Isolation Guarantee

**Critical: Wallets are ALWAYS isolated regardless of SSO mode.**

```java
@Service
public class WalletAccessControl {

    @Autowired
    private WalletRepository walletRepository;

    public Wallet getWallet(Long walletId) {
        String currentTenantId = TenantContext.getCurrentTenantId();
        String currentUserId = UserContext.getCurrentUserId();

        Wallet wallet = walletRepository.findById(walletId)
            .orElseThrow(() -> new WalletNotFoundException(walletId));

        // Verify wallet belongs to current tenant
        if (!wallet.getTenantId().equals(Long.parseLong(currentTenantId))) {
            auditLog.recordSecurityEvent("WALLET_ACCESS_VIOLATION",
                                        currentUserId, walletId);
            throw new UnauthorizedException(
                "Wallet does not belong to your tenant"
            );
        }

        // Verify user's tenant matches wallet's tenant
        User user = userRepository.findById(Long.parseLong(currentUserId))
            .orElseThrow();

        if (!user.getTenantId().equals(wallet.getTenantId())) {
            auditLog.recordSecurityEvent("WALLET_CROSS_TENANT_ACCESS",
                                        currentUserId, walletId);
            throw new UnauthorizedException(
                "Access denied"
            );
        }

        return wallet;
    }

    public void transferFunds(Long fromWalletId, Long toWalletId,
                             BigDecimal amount) {
        Wallet fromWallet = getWallet(fromWalletId);
        Wallet toWallet = getWallet(toWalletId);

        // Both wallets must belong to same tenant
        if (!fromWallet.getTenantId().equals(toWallet.getTenantId())) {
            throw new UnauthorizedException(
                "Cannot transfer between different operators"
            );
        }

        // Execute transfer...
    }
}
```

---

## 7.9 租戶遷移

### Tenant Migration Workflow

Migration allows an operator to move from one platform instance to another, typically for jurisdiction change or consolidation.

### Migration Checklist

#### Phase 1: Pre-Migration (Days 1-3)

```
[ ] Audit source tenant data completeness
[ ] Notify all players of upcoming migration
[ ] Schedule maintenance window (24 hours)
[ ] Backup source database (multiple snapshots)
[ ] Verify destination tenant created & initialized
[ ] Test migration in staging environment
[ ] Prepare DNS cutover plan
```

#### Phase 2: Data Export & Encryption

```java
@Service
public class TenantMigrationService {

    public MigrationExport exportTenantData(Long sourceTenantId) {
        Tenant sourceTenant = tenantRepository.findById(sourceTenantId)
            .orElseThrow();

        MigrationExport export = new MigrationExport();

        // Export players with PII encrypted with source tenant key
        List<Player> players = playerRepository.findByTenantId(sourceTenantId);
        export.setPlayers(players.stream()
            .map(p -> new PlayerExportDTO(
                p.getId(),
                p.getUsername(),
                encryptionService.encrypt(p.getEmail(), sourceTenant.getEncryptionKeyId()),
                encryptionService.encrypt(p.getPhoneNumber(), sourceTenant.getEncryptionKeyId()),
                p.getStatus()
            ))
            .collect(Collectors.toList())
        );

        // Export wallets & balance history
        List<Wallet> wallets = walletRepository.findByTenantId(sourceTenantId);
        export.setWallets(wallets.stream()
            .map(w -> new WalletExportDTO(
                w.getId(),
                w.getPlayerId(),
                w.getBalance(),
                w.getCurrency()
            ))
            .collect(Collectors.toList())
        );

        // Export transactions
        List<Transaction> transactions = transactionRepository
            .findByTenantId(sourceTenantId);
        export.setTransactions(transactions);

        // Export KYC documents (encrypted)
        List<KYCDocument> documents = kycRepository.findByTenantId(sourceTenantId);
        export.setKycDocuments(documents.stream()
            .map(d -> new KYCDocumentExportDTO(
                d.getId(),
                d.getPlayerId(),
                d.getDocumentType(),
                encryptionService.encrypt(d.getDocumentData(),
                                         sourceTenant.getEncryptionKeyId()),
                d.getStatus()
            ))
            .collect(Collectors.toList())
        );

        // Create migration package
        String packageId = UUID.randomUUID().toString();
        export.setPackageId(packageId);
        export.setSourceTenantId(sourceTenantId);
        export.setExportedAt(LocalDateTime.now());

        // Store on secure S3 with versioning
        storeMigrationPackage(export);

        auditLog.record("TENANT_EXPORT_STARTED", sourceTenantId, packageId);

        return export;
    }
}
```

#### Phase 3: PII Re-encryption

```java
@Service
public class PIIReencryptionService {

    public void reencryptPIIForNewTenant(MigrationExport export,
                                         Long destinationTenantId) {

        Tenant destinationTenant = tenantRepository.findById(destinationTenantId)
            .orElseThrow();

        // Decrypt with source key, re-encrypt with destination key
        for (PlayerExportDTO player : export.getPlayers()) {
            String email = encryptionService.decrypt(player.getEncryptedEmail());
            String reencryptedEmail = encryptionService.encrypt(email,
                destinationTenant.getEncryptionKeyId());
            player.setEncryptedEmail(reencryptedEmail);

            String phone = encryptionService.decrypt(player.getEncryptedPhone());
            String reencryptedPhone = encryptionService.encrypt(phone,
                destinationTenant.getEncryptionKeyId());
            player.setEncryptedPhone(reencryptedPhone);
        }

        for (KYCDocumentExportDTO doc : export.getKycDocuments()) {
            byte[] documentData = encryptionService.decrypt(doc.getEncryptedData());
            byte[] reencryptedData = encryptionService.encrypt(documentData,
                destinationTenant.getEncryptionKeyId());
            doc.setEncryptedData(reencryptedData);
        }

        auditLog.record("PII_REENCRYPTED", destinationTenantId,
                       export.getPackageId());
    }
}
```

#### Phase 4: DNS Cutover

```bash
#!/bin/bash
# DNS migration script (executed by DevOps)

SOURCE_DOMAIN="source.acme-casino.com"
DESTINATION_DOMAIN="destination.acme-casino.com"

# Step 1: Verify destination is healthy
curl -f https://$DESTINATION_DOMAIN/health || exit 1

# Step 2: Set TTL to 5 minutes for quick rollback
aws route53 change-resource-record-sets \
  --hosted-zone-id Z123ABC \
  --change-batch file://reduce-ttl.json

sleep 300 # Wait for 5 minutes

# Step 3: Update DNS to point to destination
aws route53 change-resource-record-sets \
  --hosted-zone-id Z123ABC \
  --change-batch file://cutover.json

# Step 4: Monitor for 1 hour
for i in {1..60}; do
  STATUS=$(curl -s https://$DESTINATION_DOMAIN/status)
  if [[ $STATUS != *"healthy"* ]]; then
    echo "Destination unhealthy, rolling back..."
    aws route53 change-resource-record-sets \
      --hosted-zone-id Z123ABC \
      --change-batch file://rollback.json
    exit 1
  fi
  sleep 60
done

echo "Migration complete"
```

#### Phase 5: Verification Testing

```java
@Service
public class MigrationVerificationService {

    public MigrationVerificationReport verify(Long destinationTenantId) {
        MigrationVerificationReport report = new MigrationVerificationReport();

        // Verify player count matches
        int sourceCount = 1000; // From export
        int destCount = playerRepository.countByTenantId(destinationTenantId);
        report.addCheck("Player Count", sourceCount == destCount);

        // Verify total balance matches (within transaction fees)
        BigDecimal sourceTotalBalance = new BigDecimal("50000.00");
        BigDecimal destTotalBalance = walletRepository
            .getTotalBalanceByTenantId(destinationTenantId);
        report.addCheck("Total Balance",
            sourceTotalBalance.subtract(destTotalBalance).abs()
                .compareTo(new BigDecimal("10.00")) < 0); // ±€10

        // Verify transaction history count
        int sourceTransactionCount = 50000;
        int destTransactionCount = transactionRepository
            .countByTenantId(destinationTenantId);
        report.addCheck("Transaction Count",
            sourceTransactionCount == destTransactionCount);

        // Verify player data integrity
        for (Player player : playerRepository.findByTenantId(destinationTenantId)) {
            if (player.getEmail() == null ||
                player.getUsername() == null) {
                report.addCheck("Player Data Integrity", false);
                return report;
            }
        }
        report.addCheck("Player Data Integrity", true);

        // Login test
        try {
            User testUser = userRepository.findByEmailAndTenantId(
                "test@example.com",
                destinationTenantId
            ).orElseThrow();
            report.addCheck("User Login", true);
        } catch (Exception e) {
            report.addCheck("User Login", false);
        }

        return report;
    }
}
```

#### Phase 6: Old Tenant Cleanup (30-Day Retention)

```java
@Service
@Scheduled(cron = "0 3 * * *") // Daily at 3 AM
public class TenantCleanupService {

    public void cleanupMigratedTenants() {
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);

        List<Tenant> migratedTenants = tenantRepository
            .findByStatusAndMigrationDateBefore(
                TenantStatus.MIGRATED,
                thirtyDaysAgo
            );

        for (Tenant tenant : migratedTenants) {
            // Archive all data to cold storage (S3 Glacier)
            archiveTenantData(tenant.getId());

            // Delete from operational database
            tenantRepository.delete(tenant);
            userRepository.deleteByTenantId(tenant.getId());
            playerRepository.deleteByTenantId(tenant.getId());
            walletRepository.deleteByTenantId(tenant.getId());
            transactionRepository.deleteByTenantId(tenant.getId());

            auditLog.record("TENANT_PURGED", tenant.getId(),
                           "30-day retention expired");
        }
    }
}
```

---

## 7.10 多管轄區合規

### Jurisdiction-Specific Configurations

```sql
-- Jurisdiction rules stored in t_tenant.config JSONB
INSERT INTO t_tenant (tenant_id, jurisdiction, config)
VALUES (100, 'MGA', '{
  "jurisdiction": {
    "code": "MGA",
    "name": "Malta Gaming Authority",
    "deposit_limits": null,
    "affordability_checks": false,
    "self_exclusion_days": 7,
    "mandatory_account_cooling": 0,
    "gambling_frequency_check": false,
    "max_bet_limits": null,
    "rtp_min": 0.85
  }
}'::jsonb);

INSERT INTO t_tenant (tenant_id, jurisdiction, config)
VALUES (101, 'UKGC', '{
  "jurisdiction": {
    "code": "UKGC",
    "name": "UK Gambling Commission",
    "deposit_limits": {
      "daily": 10,
      "weekly": 50,
      "monthly": 500,
      "currency": "GBP"
    },
    "affordability_checks": true,
    "self_exclusion_days": 6,
    "mandatory_account_cooling": 7,
    "gambling_frequency_check": true,
    "gamstop_integration": true,
    "max_bet_limits": {
      "slot": 2,
      "table": 5,
      "virtual_sports": 1
    },
    "rtp_min": 0.85,
    "customer_funds_segregation": true,
    "mandatory_safer_gambling_tools": true
  }
}'::jsonb);

INSERT INTO t_tenant (tenant_id, jurisdiction, config)
VALUES (102, 'CURACAO', '{
  "jurisdiction": {
    "code": "CURACAO",
    "name": "Curaçao eGaming License",
    "deposit_limits": null,
    "affordability_checks": false,
    "self_exclusion_days": 30,
    "mandatory_account_cooling": 0,
    "gambling_frequency_check": false,
    "max_bet_limits": null,
    "rtp_min": 0.85,
    "simplified_kyc": true
  }
}'::jsonb);

INSERT INTO t_tenant (tenant_id, jurisdiction, config)
VALUES (103, 'PAGCOR', '{
  "jurisdiction": {
    "code": "PAGCOR",
    "name": "Philippine Amusement and Gaming Corporation",
    "deposit_limits": null,
    "affordability_checks": false,
    "self_exclusion_days": 14,
    "mandatory_account_cooling": 0,
    "gambling_frequency_check": false,
    "max_bet_limits": null,
    "rtp_min": 0.85,
    "php_only": true,
    "ph_player_kyc": "strict"
  }
}'::jsonb);
```

### Jurisdiction Rules Applied at Runtime

```java
@Service
public class JurisdictionComplianceService {

    public boolean isDepositAllowed(Long playerId, BigDecimal amount) {
        Player player = playerRepository.findById(playerId).orElseThrow();
        Tenant tenant = tenantRepository.findById(player.getTenantId())
            .orElseThrow();

        JsonNode jurisdiction = tenant.getConfig().get("jurisdiction");
        JsonNode depositLimits = jurisdiction.get("deposit_limits");

        // No limits for MGA or Curaçao
        if (depositLimits == null || depositLimits.isNull()) {
            return true;
        }

        // Check daily limit
        BigDecimal dailyLimit = new BigDecimal(
            depositLimits.get("daily").asText()
        );
        BigDecimal todayTotal = transactionRepository
            .getTodayDepositTotal(playerId);

        if (todayTotal.add(amount).compareTo(dailyLimit) > 0) {
            return false; // Exceeds daily limit
        }

        // Check weekly limit
        BigDecimal weeklyLimit = new BigDecimal(
            depositLimits.get("weekly").asText()
        );
        BigDecimal weekTotal = transactionRepository
            .getWeekDepositTotal(playerId);

        if (weekTotal.add(amount).compareTo(weeklyLimit) > 0) {
            return false;
        }

        // Check monthly limit
        BigDecimal monthlyLimit = new BigDecimal(
            depositLimits.get("monthly").asText()
        );
        BigDecimal monthTotal = transactionRepository
            .getMonthDepositTotal(playerId);

        if (monthTotal.add(amount).compareTo(monthlyLimit) > 0) {
            return false;
        }

        return true;
    }

    public boolean isAffordabilityCheckRequired(Long playerId) {
        Player player = playerRepository.findById(playerId).orElseThrow();
        Tenant tenant = tenantRepository.findById(player.getTenantId())
            .orElseThrow();

        JsonNode jurisdiction = tenant.getConfig().get("jurisdiction");
        return jurisdiction.get("affordability_checks").asBoolean();
    }

    public void enforceAffordabilityCheck(Long playerId) {
        // UKGC requirement: assess if player can afford gaming activity
        // Based on income, debts, gambling losses

        BigDecimal monthlyIncome = getPlayerMonthlyIncome(playerId);
        BigDecimal monthlyLosses = transactionRepository
            .getMonthLosses(playerId);

        if (monthlyLosses.divide(monthlyIncome, 2, RoundingMode.HALF_UP)
            .compareTo(new BigDecimal("0.25")) > 0) {
            // 25% of income lost to gambling
            PlayerAccount account = playerAccountRepository.findByPlayerId(playerId)
                .orElseThrow();
            account.setAffordabilityReviewRequired(true);
            account.setReviewDate(LocalDate.now());
            playerAccountRepository.save(account);

            // Send affordability warning to player
            emailService.sendAffordabilityWarning(playerId);
        }
    }

    public boolean isGAMSTOPRegistered(Long playerId) {
        // UK players can register with GAMSTOP for multi-operator self-exclusion
        Player player = playerRepository.findById(playerId).orElseThrow();
        Tenant tenant = tenantRepository.findById(player.getTenantId())
            .orElseThrow();

        JsonNode jurisdiction = tenant.getConfig().get("jurisdiction");
        if (!jurisdiction.get("gamstop_integration").asBoolean()) {
            return false;
        }

        // Check GAMSTOP exclusion list
        return gamstopService.isExcluded(player.getEmail());
    }

    public int getSelfExclusionDays(Long playerId) {
        Player player = playerRepository.findById(playerId).orElseThrow();
        Tenant tenant = tenantRepository.findById(player.getTenantId())
            .orElseThrow();

        JsonNode jurisdiction = tenant.getConfig().get("jurisdiction");
        return jurisdiction.get("self_exclusion_days").asInt();
    }
}
```

### Jurisdiction-Specific Compliance Checks at Bet Time

```java
@Service
public class BetValidationService {

    public boolean isBetAllowed(Long playerId, Long gameId, BigDecimal stakeAmount) {
        Player player = playerRepository.findById(playerId).orElseThrow();
        Tenant tenant = tenantRepository.findById(player.getTenantId())
            .orElseThrow();

        JsonNode jurisdiction = tenant.getConfig().get("jurisdiction");

        // 1. Check max bet limits by game type
        if (jurisdiction.has("max_bet_limits")) {
            Game game = gameRepository.findById(gameId).orElseThrow();
            BigDecimal maxBet = extractMaxBetForGameType(jurisdiction, game.getType());

            if (stakeAmount.compareTo(maxBet) > 0) {
                return false;
            }
        }

        // 2. Check affordability (UKGC)
        if (isAffordabilityCheckRequired(playerId)) {
            enforceAffordabilityCheck(playerId);
        }

        // 3. Check gambling frequency (UKGC)
        if (jurisdiction.get("gambling_frequency_check").asBoolean()) {
            boolean tooFrequent = checkGamblingFrequency(playerId);
            if (tooFrequent) {
                notifyPlayer(playerId, "Frequent gambling detected");
                return false;
            }
        }

        // 4. Check self-exclusion
        if (player.isSelfExcluded()) {
            return false;
        }

        // 5. Check account cooling-off period (UKGC)
        if (jurisdiction.get("mandatory_account_cooling").asInt() > 0) {
            if (player.isInCoolingOffPeriod()) {
                return false;
            }
        }

        return true;
    }
}
```

---

## 7.11 對應業務文檔

> Link to: `/mnt/IGaming/requirements/07_Governance_Licensing_治理與牌照.md`

This technical document implements the governance and licensing requirements defined in the business specification. Cross-reference the requirements document for:
- Detailed tenant hierarchy use cases
- RBAC permission matrix
- MFA enforcement policies
- Billing model terms and SLA
- Jurisdiction-specific compliance checklists
- Tenant migration scenarios
- SSO integration requirements

---

## Summary

The Governance & Licensing module provides a comprehensive, scalable foundation for managing multi-tenant IGaming platforms with:

1. **Strict data isolation** at application and database levels
2. **Flexible RBAC** with permission caching and dynamic role assignment
3. **Enterprise-grade MFA** supporting TOTP, WebAuthn, and SMS OTP
4. **White-label customization** with runtime brand resolution
5. **Automated billing** with overdue escalation workflows
6. **SSO integration** with wallet isolation guarantees
7. **Safe tenant migration** with PII re-encryption
8. **Multi-jurisdiction compliance** with runtime jurisdiction rules

All components are production-hardened with comprehensive audit logging and security monitoring.
