# Third-Party Integration Architecture

> **Business Requirements**: [Third-Party Integration Requirements](../../requirements/14_Integration_Standards/Third_Party_Integration_Requirements.md)
> **Canonical Source**: [source-archive/14_Third_Party_Integration/14-01](../../source-archive/14_Third_Party_Integration/14-01_Third_Party_Integration.md)
> **View Type**: Technical Architecture
> **Target Audience**: Architects, Backend Developers, DevOps Engineers

---

## 1. Integration Categories

### 1.1 KYC/AML Providers

| Provider | Service | Integration | Cost |
|----------|---------|-------------|------|
| **Onfido** | ID verification, facial recognition | REST API | ~$2/check |
| **Jumio** | Document verification | REST API + Webhook | ~$1.5/check |
| **ComplyAdvantage** | AML screening | REST API | ~$0.5/check |
| **Sumsub** | Comprehensive KYC | REST API + SDK | ~$3/check |

### 1.2 Marketing Tools

| Tool Type | Provider | Purpose | Integration |
|-----------|----------|---------|-------------|
| **Email** | SendGrid, AWS SES | Transactional, marketing email | REST API |
| **SMS** | Twilio, Vonage | OTP, withdrawal notifications | REST API |
| **Push** | OneSignal, Firebase | App push notifications | SDK + REST API |
| **CRM** | Braze, Customer.io | Player lifecycle management | REST API + Webhook |

### 1.3 Analytics Tools

| Tool | Purpose | Integration |
|------|---------|-------------|
| **Google Analytics 4** | Traffic, user behavior | gtag.js SDK |
| **Mixpanel** | Product analytics, funnels | JavaScript SDK |
| **Amplitude** | Retention, event tracking | JavaScript SDK |
| **Segment** | Data pipeline (unified) | JS SDK + Server API |

## 2. Integration Patterns

### 2.1 Segment Event Tracking

```javascript
// Frontend: track player deposit event
analytics.track('Deposit Completed', {
  amount: 100.00,
  currency: 'USD',
  payment_method: 'credit_card',
  player_vip_level: 'Gold'
});
```

### 2.2 Firebase Cloud Messaging SDK

```html
<script src="https://www.gstatic.com/firebasejs/9.0.0/firebase-app.js"></script>
<script src="https://www.gstatic.com/firebasejs/9.0.0/firebase-messaging.js"></script>

<script>
const firebaseConfig = {
  apiKey: "AIzaSyXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX",
  projectId: "igaming-platform",
  messagingSenderId: "123456789012"
};
firebase.initializeApp(firebaseConfig);

const messaging = firebase.messaging();
messaging.requestPermission()
  .then(() => messaging.getToken())
  .then(token => {
    fetch('/api/v1/players/me/fcm-token', {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify({fcm_token: token})
    });
  });
</script>
```

## 3. Webhook Retry Strategy

**Exponential Backoff**:

| Retry | Delay | Cumulative |
|-------|-------|-----------|
| 1st | 5s | 5s |
| 2nd | 10s | 15s |
| 3rd | 20s | 35s |
| 4th | 40s | 1m 15s |
| 5th | 80s | 2m 35s |
| 6th (final) | 160s | 5m 15s |

**Configuration**:
```yaml
webhook:
  max_retries: 6
  initial_delay: 5s
  max_delay: 160s
  backoff_multiplier: 2.0

  dlq:
    retention_days: 7
    alert_threshold: 100

  alerting:
    slack_channel: '#integrations-ops'
    pagerduty_severity: high
```

## 4. API Key Management (HashiCorp Vault)

```text
Vault Secrets Engine:
+-- secret/psp/nuvei
|   +-- merchant_id: "123456"
|   +-- secret_key: "abc123xyz"
|
+-- secret/kyc/onfido
|   +-- api_key: "live_XXXXXXXXXXXXXXXX"
|   +-- webhook_secret: "webhook_secret_123"
|
+-- secret/email/sendgrid
    +-- api_key: "SG.XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
```

### Key Rotation Policy

| Service Type | Rotation Cycle | Automated | Trigger |
|-------------|---------------|-----------|---------|
| PSP API Key | 90 days | Yes | Scheduled + suspicious activity |
| Internal Service Key | 30 days | Yes | Scheduled |
| Webhook Secret | On demand | No | Suspected compromise |
| Database Password | 180 days | Yes | Scheduled |

### Terraform + Vault Auto-Rotation

```hcl
resource "vault_generic_secret" "nuvei_api_key" {
  path = "secret/psp/nuvei"

  data_json = jsonencode({
    merchant_id = "123456"
    secret_key  = random_password.nuvei_secret.result
  })

  lifecycle {
    create_before_destroy = true
  }
}

resource "random_password" "nuvei_secret" {
  length  = 32
  special = true

  keepers = {
    rotation_timestamp = timestamp()
  }
}
```

## 5. Rate Limiting Configuration

| Service | Limit | Window | Over-Limit Action |
|---------|-------|--------|-------------------|
| **Onfido KYC** | 100 req/min | 60s | Queue + 429 error |
| **SendGrid Email** | 1000 req/hour | 3600s | Queue + delayed send |
| **GP Game Launch** | 500 req/min | 60s | Return cached URL |
| **Google Analytics** | Unlimited | - | - |

## 6. Service Degradation Strategy

| Service Type | Priority | Impact When Down | Fallback |
|-------------|----------|------------------|----------|
| **PSP** | Critical | No deposits/withdrawals | Switch to backup PSP |
| **KYC Provider** | Important | Cannot complete verification | Manual review process |
| **Game Provider** | Important | Specific games unavailable | Show maintenance notice |
| **Email Service** | Optional | Delayed email delivery | Queue + retry later |
| **Analytics** | Optional | Cannot track events | Local log recording |

### Fallback Implementation

```java
@Manager
@RequiredArgsConstructor
public class ThirdPartyServiceManager {
    private final OnfidoKycClient primaryKycClient;
    private final JumioKycClient fallbackKycClient;
    private final ThirdPartyHealthMonitor healthMonitor;

    @Transactional(rollbackFor = Throwable.class)
    public Option<KycResult> performKycVerification(PlayerId playerId, DocumentUpload document) {
        if (healthMonitor.isHealthy("onfido")) {
            return Try.of(() -> primaryKycClient.verify(playerId, document))
                .onFailure(e -> log.warn("Onfido failed, switching to fallback", e))
                .toOption();
        }

        log.info("Using fallback KYC provider: Jumio");
        return Try.of(() -> fallbackKycClient.verify(playerId, document))
            .onFailure(e -> log.error("Both KYC providers failed", e))
            .toOption();
    }
}
```

## 7. Monitoring & Alerting

### Health Check Dashboard

```text
Third-Party Service Health (Last 1 Hour)
+--------------------------------------------------+
|  PSP: Nuvei          OK  99.8% Available P99: 1.2s|
|  KYC: Onfido         OK  98.5% Available P99: 3.5s|
|  Email: SendGrid     OK  100% Available  P99: 0.5s|
|  GP: Pragmatic Play  WARN 95.2% Available P99: 2.8s|
+--------------------------------------------------+
```

### Prometheus AlertManager

```yaml
groups:
  - name: third_party_services
    interval: 1m
    rules:
      - alert: PSP_HighFailureRate
        expr: |
          (
            sum(rate(third_party_requests_total{service="nuvei", status="error"}[5m]))
            /
            sum(rate(third_party_requests_total{service="nuvei"}[5m]))
          ) > 0.05
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "Nuvei PSP failure rate > 5%"
          description: "Current failure rate: {{ $value | humanizePercentage }}"

      - alert: KYC_SlowResponse
        expr: |
          histogram_quantile(0.99,
            sum(rate(third_party_request_duration_bucket{service="onfido"}[5m])) by (le)
          ) > 5.0
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "Onfido KYC P99 latency > 5s"
```

### Service Recovery Detection

- Health check interval: 30 seconds
- Recovery condition: 3 consecutive passes (availability > 95%)
- Auto-switch back to primary service with recovery event logged
