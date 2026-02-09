# Mobile App Requirements

> **Canonical Source**: [source-archive/11_Frontend_CMS/11-04](../../source-archive/11_Frontend_CMS/11-04_Mobile_App_Architecture.md)
> **View Type**: Business Requirements
> **Target Audience**: Product Managers, Mobile Team Leads
> **Related Doc**: [Mobile App Architecture](../../architecture/11_Frontend/Mobile_App_Architecture.md)
> **Last Synced**: 2026-02-09

---

## 1. Platform Support Requirements

### 1.1 Framework Strategy

The mobile app must provide a native or hybrid experience. Cross-platform development (Flutter or React Native) is recommended.

**Selection Criteria**:
- High performance game lobby: Flutter preferred
- Fast iteration + hot update (iGaming norm): React Native + CodePush preferred

### 1.2 Distribution Strategy

Distribution is the core challenge due to app store restrictions on gambling apps.

**iOS Distribution**:

| Method | Description | Risk |
|--------|-------------|------|
| Enterprise Certificate | No App Store required; user trusts certificate | Frequent re-signing needed |
| TestFlight | Distributed as "test" app | 90-day validity |
| Super Signature | Uses developer account device ID quota | Limited capacity |
| WebClip (PWA) | Desktop bookmark opens Safari | Safest but slightly inferior UX |

**Android Distribution**:

| Method | Description | Risk |
|--------|-------------|------|
| APK Download | Direct download from official website | Standard approach |
| Disguised App | Upload as utility app with backend switch | High risk of takedown |

---

## 2. Core Feature Requirements

### 2.1 Infrastructure Features

- **HttpDNS**: Prevent DNS pollution, ensure stable API connectivity
- **Domain Fronting**: Hide real backend IP for anti-blocking

### 2.2 Native Features

| Feature | Purpose | Priority |
|---------|---------|----------|
| Biometric Login | FaceID/TouchID for quick login | P0 |
| Push Notifications | FCM/APNS for marketing and transaction alerts | P0 |
| Device Fingerprint | IDFA/GAID/IMEI/AndroidID for risk control | P0 |

### 2.3 Hot Update Mechanism

- App checks `version.json` on startup
- New patch downloads in background (JS Bundle / Assets)
- After download, prompt user "Restart to apply" or auto-apply on next launch
- **Rollback**: If new version crashes, automatically revert to previous version

**Staged Rollout**:

| Stage | User % | Purpose |
|-------|--------|---------|
| Stage 1 | 5% | Canary test, monitor crash rate |
| Stage 2 | 20% | Expand if no anomalies |
| Stage 3 | 100% | Full release |

### 2.4 Offline Support

**Available Offline**:
- Browse game lobby (cached game list)
- View recent bet history (last 100 records)
- View personal profile

**Requires Network**:
- Placing bets, deposits, withdrawals

---

## 3. Push Notification Requirements

### 3.1 Multi-Channel Integration

| Platform | Service | Purpose |
|----------|---------|---------|
| iOS | APNS | Official push channel |
| Android (Global) | FCM | Google official |
| Android (China) | Xiaomi/Huawei/OPPO Push | Improve domestic delivery rate |
| In-App | WebSocket/MQTT | Real-time notification when app is foreground |

### 3.2 Push Categories

| Category | Examples | Priority |
|----------|----------|----------|
| **Marketing** | New promotions, VIP offers, limited-time deals | Medium |
| **Transactional** | Deposit success, withdrawal status update, big win congratulations | High |
| **Risk Control** | Unusual login alert, account security warning | Critical |

### 3.3 Anti-Disturbance Policy

- Maximum 5 marketing pushes per day
- Quiet hours: 23:00 - 09:00 (no marketing pushes)
- Respect user subscription preferences

### 3.4 Deep Linking

Push notification clicks must navigate directly to relevant page (activity detail, withdrawal status), not just app homepage.

---

## 4. User Experience Requirements

### 4.1 Device Adaptation

- Screen sizes: 4.7" to 6.7" mainstream devices
- Safe area: Support iPhone notch, Android punch-hole cameras
- Orientation: Game pages support landscape (especially slots, live games)

### 4.2 Feedback and Animation

- Skeleton screen during loading (content placeholder)
- Haptic feedback for important operations
- Optimistic UI: Show updated balance immediately after deposit (sync with backend confirmation later)

### 4.3 Error Handling

- Global error boundary catches all crashes
- Errors logged to crash reporting service
- Friendly error pages shown to users (never raw stack traces)

---

## 5. Security Requirements

### 5.1 Code Protection

- APK must be packed/hardened to prevent decompilation
- Code obfuscation: ProGuard (Android) / Strip Symbols (iOS)
- Root/Jailbreak detection: Force exit or restrict high-risk operations

### 5.2 Communication Security

- SSL Pinning to prevent MITM attacks
- API signature verification on every request
- Sensitive fields encrypted with AES-256 in transit

### 5.3 Data Security

- Local data encryption (AsyncStorage encrypted with AES)
- Sensitive data (tokens) stored in Keychain/KeyStore
- Screenshot prevention on payment and personal data pages

---

## 6. Testing Requirements

### 6.1 Test Coverage

| Test Type | Tool | Coverage Target |
|-----------|------|----------------|
| E2E Tests | Detox/Flutter Driver | 100% critical flows |
| Integration Tests | Jest + Testing Library | 80% core components |
| Unit Tests | Jest | 90% utility functions |

### 6.2 Device Coverage

- iOS: iPhone SE (small), iPhone 14 Pro (notch), iPad
- Android: Samsung S22 (flagship), Xiaomi Redmi (mid-range), Huawei (HMS without GMS)

---

## 7. Acceptance Criteria

1. App supports iOS and Android with cross-platform framework
2. Hot update mechanism works with staged rollout and automatic rollback
3. Biometric login, push notifications, and device fingerprint functional
4. Offline mode supports browsing cached game list and bet history
5. Push notifications respect quiet hours and daily limits
6. Deep linking navigates to correct pages from push notifications
7. App passes security audit: SSL pinning, code obfuscation, root detection
