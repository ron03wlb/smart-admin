# MFA Login and Recovery Requirements (MFA 登入與恢復業務需求)

> **Canonical Source**: [06-06-03_Recovery_Flow.md](../../source-archive/06_Platform_Governance/06-06-03_Recovery_Flow.md)
> **Audience**: Executives, Compliance Officers, Security Operations
> **Related Doc**: [MFA_Login_Recovery_Technical.md](../../architecture/06_Platform_Core/MFA_Login_Recovery_Technical.md)
> **Last Synced**: 2026-02-09
>
> **Refinement Note**: Technical details (Two-Phase Login Sequence Diagram, Trusted Device Token generation Java code, Cookie configuration HttpOnly/Secure/SameSite, MFA Session Storage Redis + 5-minute TTL, SHA256 hashing, QR code generation) moved to Architecture layer. This document focuses on business policies only.

---

## 1. Business Context

Multi-Factor Authentication (MFA) is a mandatory security control for the back-office administration platform. This document defines the business requirements for the two-phase login flow, the MFA registration process, the trusted device mechanism, and error handling policies. MFA protects administrative functions against credential compromise and satisfies regulatory security requirements across UKGC, MGA, and ISO 27001 frameworks.

---

## 2. Two-Phase Login Flow

### 2.1 Phase Overview

The login process is divided into two sequential phases:

| Phase | Purpose | Player Action | System Action |
|-------|---------|---------------|---------------|
| **Phase 1** | Password verification | Enter username and password | Validate credentials; determine whether MFA is required |
| **Phase 2** | MFA verification | Enter 6-digit TOTP code from authenticator app | Validate TOTP; issue access and refresh tokens |

If MFA is not enabled for the user's role, Phase 1 completes the full login and tokens are issued immediately.

### 2.2 MFA Session Rules

| Rule | Value | Rationale |
|------|-------|-----------|
| MFA session lifetime | 5 minutes | Limits window of vulnerability after password verification |
| Session storage | Server-side only | Prevents client-side tampering |
| Session is single-use | Yes | Consumed and deleted upon successful MFA verification |

→ **[MFA Session Storage Implementation](../../architecture/06_Platform_Core/MFA_Login_Recovery_Technical.md#mfa-session-storage)** - Redis storage configuration, 5-minute TTL, session cleanup mechanism

---

## 3. Trusted Device Mechanism

### 3.1 Business Objective

Frequent MFA prompts degrade user experience for administrators who log in multiple times per day from the same workstation. The trusted device mechanism allows users to skip MFA verification on recognised devices for a defined period.

### 3.2 Policy Rules

| Policy | Value | Rationale |
|--------|-------|-----------|
| Trust duration | 30 days | Industry standard; balances security and usability |
| Opt-in required | Yes | User explicitly checks "Trust this device" during MFA step |
| Trust binding | Device fingerprint + IP address + User-Agent | Three-factor binding prevents token reuse on different devices |
| Cookie attributes | Secure cookie configuration | Prevents JavaScript access, requires HTTPS, blocks CSRF |

### 3.3 Security Risk Mitigations

| Risk | Mitigation |
|------|-----------|
| Trust token theft | Secure cookie configuration prevents JavaScript access |
| Cross-site request forgery | Cookie policy blocks cross-origin requests |
| Device fingerprint collision | Triple binding (fingerprint + IP + User-Agent) reduces false matches |
| Trust period too long | 30-day TTL with automatic expiry; administrator can revoke remotely |

→ **[Trusted Device Token Implementation](../../architecture/06_Platform_Core/MFA_Login_Recovery_Technical.md#trusted-device-token)** - Cookie configuration (HttpOnly, Secure, SameSite=Strict), SHA256 token generation, triple binding implementation

---

## 4. Failed Attempt and Lockout Policy

### 4.1 Lockout Rules

| Rule | Value |
|------|-------|
| Maximum TOTP attempts | 3 consecutive failures |
| Lockout duration | 15 minutes |
| Lockout scope | Per user account |
| Lockout counter reset | Automatic after 15-minute window or successful login |

### 4.2 Error Code Definitions

| Error Condition | Error Code | Recommended User Guidance |
|----------------|------------|--------------------------|
| MFA session token expired or missing | INVALID_MFA_SESSION | Redirect user to restart login from Phase 1 |
| TOTP code incorrect | INVALID_TOTP | Display remaining attempt count; prompt re-entry |
| Account locked after 3 failures | MFA_LOCKED | Show lockout countdown timer (15 minutes) |
| User calls MFA verify but MFA is not enabled | MFA_NOT_ENABLED | Guide user to enable MFA in personal settings |
| Server time synchronisation failure | TIME_SYNC_ERROR | Trigger operations alert; display generic error to user |

---

## 5. MFA Registration Requirements

### 5.1 Registration Triggers

MFA registration is initiated under the following conditions:

| Trigger | Description | User Action Required |
|---------|------------|---------------------|
| First login with mandatory MFA role | User's role requires MFA; user has not yet enrolled | Must complete registration before accessing the system |
| Voluntary enablement | User navigates to Personal Settings and enables MFA | Follows guided registration flow |
| Administrative enforcement | Security administrator enables MFA for the user | User is prompted at next login |

### 5.2 Registration Steps

The registration process follows three sequential steps:

| Step | Name | Description |
|------|------|-------------|
| 1 | Generate secret | System generates TOTP secret key and presents it to user |
| 2 | Scan QR code | User scans QR code with Google Authenticator (or compatible TOTP app) |
| 3 | Verify and activate | User enters first 6-digit code to prove successful registration |

→ **[TOTP Secret and QR Code Generation](../../architecture/06_Platform_Core/MFA_Login_Recovery_Technical.md#totp-registration)** - Secret generation algorithm, QR code encoding, secure display implementation

### 5.3 Backup Code Policy

| Policy | Value |
|--------|-------|
| Number of backup codes | 10 |
| Code format | 8-digit numeric |
| Delivery method | Displayed on screen once; downloadable as text file |
| Storage | Securely stored server-side; plaintext shown only at generation time |
| Usage | Single-use; each code may be used once for account recovery |

→ **[Backup Code Storage Implementation](../../architecture/06_Platform_Core/MFA_Login_Recovery_Technical.md#backup-codes)** - Encryption algorithm (AES-256-GCM), secure generation, storage format

---

## 6. Supported MFA Methods

| Method | Status | Description |
|--------|--------|-------------|
| TOTP (Google Authenticator) | Primary, mandatory | Time-based 6-digit code with 30-second period |
| SMS | Secondary, optional | 6-digit code sent via SMS (fallback) |
| Backup codes | Recovery | Pre-generated single-use codes for emergency access |

---

## 7. Compliance and Audit Requirements

### 7.1 Audit Events

Every MFA-related action must produce an immutable audit log entry:

| Event | Audit Log Code | Details Captured |
|-------|---------------|-----------------|
| MFA setup initiated | MFA_SETUP_INIT | User ID, timestamp |
| MFA successfully enabled | MFA_ENABLED | User ID, activation timestamp |
| MFA login succeeded | MFA_LOGIN_SUCCESS | User ID, device fingerprint, IP address |
| MFA login failed | MFA_LOGIN_FAILED | User ID, failure reason, remaining attempts |
| Account locked | MFA_ACCOUNT_LOCKED | User ID, lockout timestamp, unlock time |
| Security alert sent | MFA_SECURITY_ALERT | User ID, alert type, recipient |

### 7.2 Regulatory Alignment

| Regulation | MFA Requirement | Status |
|-----------|----------------|--------|
| UKGC LCCP | Strong authentication for administrative access | Covered by TOTP + trusted device |
| MGA Technical Standards | Two-factor authentication for back-office | Covered by two-phase login |
| ISO 27001 A.9.4.2 | Secure log-on procedures | Covered by lockout + audit trail |

---

## 8. User Experience Requirements

### 8.1 UI Flow Summary

| Screen | Content | Actions Available |
|--------|---------|-------------------|
| Login form | Username and password fields | Submit credentials |
| MFA prompt | 6-digit TOTP input field; "Trust this device" checkbox | Submit code; go back to login |
| MFA setup wizard | Step indicator; QR code display; backup code download; verification input | Scan QR; download codes; verify code |
| Lockout screen | Countdown timer; support contact information | Wait for unlock; contact admin |
| Error display | Context-appropriate error message with remaining attempts | Retry; restart login |

### 8.2 Accessibility

| Requirement | Implementation |
|-------------|---------------|
| Manual secret entry | Displayed alongside QR code for users who cannot scan |
| Backup code download | Available as plain text file for offline storage |
| Error messages | Clear guidance with specific next steps (not generic errors) |

---

## 9. Related Business Requirements

| Document | Relationship |
|----------|-------------|
| MFA Architecture Design (06-06-01) | High-level MFA strategy and method selection |
| TOTP and WebAuthn Implementation (06-06-02) | Detailed TOTP algorithm specification |
| Compliance and Audit (06-06-04) | Audit logging standards and role-based MFA policies |
| RBAC Permissions (06-02) | Role definitions that determine MFA enforcement |

### Technical Implementation

→ **[MFA Recovery Implementation](../../architecture/06_Platform_Core/MFA_Recovery_Implementation.md)** - Two-phase login sequence diagrams, MFA session storage (Redis + TTL), trusted device token generation (SHA256), cookie configuration (HttpOnly/Secure/SameSite), and TOTP QR code generation

---

**Navigation**: [Governance and Licensing Requirements](../06_Governance_Licensing/) | [iGaming Home](../../README.md)
