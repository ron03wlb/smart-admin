# 06-03 第三方整合標準 (Third-Party Integration Standard)

> **MIGRATED FROM**: 14-01_Third_Party_Integration.md (Phase 4 Module Merge)
> **Version**: 2.0.0
> **Last Updated**: 2026-02-04

## 1. 系統概述

本文檔定義 iGaming 平台與外部服務整合的技術標準和最佳實踐，確保安全性、可維護性和可擴展性。

**核心原則**：
- **統一接口**: 所有第三方整合通過統一的適配器層訪問
- **容錯設計**: 第三方服務故障不應導致平台核心功能不可用
- **監控優先**: 每個整合點都必須有健康檢查和告警
- **安全第一**: API 金鑰加密存儲，Webhook 簽名驗證

---

## 2. 整合分類 (Integration Categories)

### 2.1 遊戲提供商 (Game Providers)

**詳細規範**: 參考 [02-01 遊戲整合標準](../03_Game_Center/03-01_Game_Integration_Standard.md)

**關鍵特性**：
- Seamless Wallet 整合
- 遊戲啟動 URL 生成
- 投注/派彩 Webhook 接收

---

### 2.2 支付服務商 (Payment Service Providers)

**詳細規範**: 參考 [01-03 支付網關整合](../02_Finance_Center/02-02_Payment_Gateway_Integration.md)

**關鍵特性**：
- 支付請求 API
- 支付回調 Webhook
- 對帳報表下載

---

### 2.3 KYC/AML 供應商

**常見供應商**：
| 供應商 | 服務類型 | 整合方式 | 成本 |
|--------|---------|---------|------|
| **Onfido** | 身份證件驗證、人臉識別 | REST API | ~$2/次 |
| **Jumio** | 文件驗證 | REST API + Webhook | ~$1.5/次 |
| **ComplyAdvantage** | AML 名單篩查 | REST API | ~$0.5/次 |
| **Sumsub** | 綜合 KYC | REST API + SDK | ~$3/次 |


**Webhook 回調處理**：

---

### 2.4 行銷工具

| 工具類型 | 供應商 | 用途 | 整合方式 |
|---------|--------|------|---------|
| **Email** | SendGrid, AWS SES | 交易郵件、行銷郵件 | REST API |
| **SMS** | Twilio, Vonage | 驗證碼、提款通知 | REST API |
| **推送通知** | OneSignal, Firebase | App 推送 | SDK + REST API |
| **行銷自動化** | Braze, Customer.io | 玩家生命週期管理 | REST API + Webhook |

**SendGrid 郵件發送範例**：

---

### 2.5 數據分析工具

| 工具 | 用途 | 整合方式 |
|------|------|---------|
| **Google Analytics 4** | 網站流量、用戶行為 | gtag.js SDK |
| **Mixpanel** | 產品分析、漏斗分析 | JavaScript SDK |
| **Amplitude** | 用戶留存、事件追蹤 | JavaScript SDK |
| **Segment** | 數據管道（統一接口） | JavaScript SDK + Server API |

**Segment 事件追蹤範例**：
```javascript
// 前端追蹤玩家存款事件
analytics.track('Deposit Completed', {
  amount: 100.00,
  currency: 'USD',
  payment_method: 'credit_card',
  player_vip_level: 'Gold'
});

// 後端追蹤提款事件
import requests

requests.post(
    "https://api.segment.io/v1/track",
    auth=(SEGMENT_WRITE_KEY, ''),
    json={
        "userId": player_id,
        "event": "Withdrawal Requested",
        "properties": {
            "amount": 500.00,
            "currency": "USD",
            "withdrawal_method": "bank_transfer"
        }
    }
)
```

---

## 3. 整合模式 (Integration Patterns)

### 3.1 REST API 整合

**使用場景**: 同步請求-響應模式（如 KYC 驗證、支付請求）

**標準實作**：

---

### 3.2 Webhook 模式

**使用場景**: 異步通知（如支付回調、KYC 結果回調）


---

### 3.3 SDK 整合

**使用場景**: 前端整合（如 Google Analytics、Firebase）

**範例** (Firebase Cloud Messaging):
```html
<!-- Firebase SDK -->
<script src="https://www.gstatic.com/firebasejs/9.0.0/firebase-app.js"></script>
<script src="https://www.gstatic.com/firebasejs/9.0.0/firebase-messaging.js"></script>

<script>
// 初始化 Firebase
const firebaseConfig = {
  apiKey: "AIzaSyXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX",
  projectId: "igaming-platform",
  messagingSenderId: "123456789012"
};
firebase.initializeApp(firebaseConfig);

// 請求推送權限
const messaging = firebase.messaging();
messaging.requestPermission()
  .then(() => messaging.getToken())
  .then(token => {
    // 將 token 發送到後端存儲
    fetch('/api/v1/players/me/fcm-token', {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify({fcm_token: token})
    });
  });
</script>
```

---

## 4. Webhook 管理系統

### 4.1 出站 Webhook (Outbound Webhooks)

**使用場景**: 通知外部系統平台事件（如通知運營商新玩家註冊）



---

### 4.2 入站 Webhook (Inbound Webhooks)


---

### 4.3 Webhook 重試策略 (NEW)

**指數退避演算法 (Exponential Backoff)**：

| 重試次數 | 延遲時間 | 累計等待 |
|---------|---------|---------|
| 1st retry | 5 秒 | 5 秒 |
| 2nd retry | 10 秒 | 15 秒 |
| 3rd retry | 20 秒 | 35 秒 |
| 4th retry | 40 秒 | 1 分 15 秒 |
| 5th retry | 80 秒 | 2 分 35 秒 |
| 6th retry（最後） | 160 秒 | 5 分 15 秒 |

**死信隊列 (Dead Letter Queue)**：
- 重試 6 次後仍失敗的 Webhook 事件自動進入 DLQ
- DLQ 保留 7 天，可手動重放或分析失敗原因
- 告警觸發條件：DLQ 累積超過 100 條事件

**重試次數限制與告警**：
```yaml
# Webhook Retry Configuration
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

---

## 5. API 金鑰管理

### 5.1 HashiCorp Vault 整合

**金鑰存儲架構**:
```
Vault Secrets Engine:
├─ secret/psp/nuvei
│  ├─ merchant_id: "123456"
│  └─ secret_key: "abc123xyz"
│
├─ secret/kyc/onfido
│  ├─ api_key: "live_XXXXXXXXXXXXXXXX"
│  └─ webhook_secret: "webhook_secret_123"
│
└─ secret/email/sendgrid
   └─ api_key: "SG.XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
```


---

### 5.2 金鑰輪替政策

**輪替頻率**:
| 服務類型 | 輪替週期 | 自動化 | 觸發條件 |
|---------|---------|--------|---------|
| PSP API 金鑰 | 90 天 | ✅ 自動 | 定時任務 + 可疑活動 |
| 內部服務金鑰 | 30 天 | ✅ 自動 | 定時任務 |
| Webhook Secret | 按需 | ❌ 手動 | 懷疑洩露時 |
| 數據庫密碼 | 180 天 | ✅ 自動 | 定時任務 |

**自動輪替腳本** (Terraform + Vault):
```hcl
# terraform/vault_rotation.tf
resource "vault_generic_secret" "nuvei_api_key" {
  path = "secret/psp/nuvei"

  data_json = jsonencode({
    merchant_id = "123456"
    secret_key  = random_password.nuvei_secret.result
  })

  # 每 90 天輪替
  lifecycle {
    create_before_destroy = true
  }
}

resource "random_password" "nuvei_secret" {
  length  = 32
  special = true

  keepers = {
    # 每 90 天觸發輪替
    rotation_timestamp = timestamp()
  }
}
```

---

## 6. 速率限制 (Rate Limiting)

### 6.1 每整合限制配置

| 服務 | 限制 | 時間窗口 | 超限動作 |
|------|------|---------|---------|
| **Onfido KYC** | 100 req/min | 60 秒 | 排隊 + 429 錯誤 |
| **SendGrid Email** | 1000 req/hour | 3600 秒 | 排隊 + 延遲發送 |
| **GP 遊戲啟動** | 500 req/min | 60 秒 | 返回快取 URL |
| **Google Analytics** | 無限制 | - | - |


---

## 7. 監控與告警

### 7.1 健康檢查儀表板

**實時監控指標**:
```
┌──────────────────────────────────────────────────┐
│  第三方服務健康狀態 (最近 1 小時)                  │
├──────────────────────────────────────────────────┤
│  PSP: Nuvei          ✅ 99.8% 可用    P99: 1.2s  │
│  KYC: Onfido         ✅ 98.5% 可用    P99: 3.5s  │
│  Email: SendGrid     ✅ 100% 可用     P99: 0.5s  │
│  GP: Pragmatic Play  ⚠️ 95.2% 可用    P99: 2.8s  │
└──────────────────────────────────────────────────┘
```


---

### 7.2 告警規則

**Prometheus AlertManager 配置**:
```yaml
# alerts/third_party.yml
groups:
  - name: third_party_services
    interval: 1m
    rules:
      # PSP 可用性低於 95%
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

      # KYC 提供商響應時間過長
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

# 告警路由
receivers:
  - name: 'slack-finance-ops'
    slack_configs:
      - channel: '#finance-ops'
        title: '{{ .GroupLabels.alertname }}'
        text: '{{ range .Alerts }}{{ .Annotations.description }}{{ end }}'

  - name: 'pagerduty-oncall'
    pagerduty_configs:
      - service_key: '{{ .ServiceKey }}'
```

---

### 7.3 第三方服務降級策略 (NEW)

**降級優先級矩陣**：

| 服務類型 | 優先級 | 降級後影響 | Fallback 機制 |
|---------|--------|-----------|--------------|
| **支付網關 (PSP)** | Critical | 無法充值/提款 | 切換備用 PSP |
| **KYC 供應商** | Important | 無法完成身份驗證 | 手動審核流程 |
| **遊戲提供商 (GP)** | Important | 特定遊戲不可用 | 顯示維護通知 |
| **Email 服務** | Optional | 郵件延遲發送 | 排隊 + 稍後重試 |
| **數據分析工具** | Optional | 無法追蹤事件 | 本地日誌記錄 |

**降級時的 Fallback 機制**：
```java
// SmartAdmin Pattern: Manager Layer with Fallback
@Manager
@RequiredArgsConstructor
public class ThirdPartyServiceManager {
    private final OnfidoKycClient primaryKycClient;
    private final JumioKycClient fallbackKycClient;
    private final ThirdPartyHealthMonitor healthMonitor;

    @Transactional(rollbackFor = Throwable.class)
    public Option<KycResult> performKycVerification(PlayerId playerId, DocumentUpload document) {
        // Check primary service health
        if (healthMonitor.isHealthy("onfido")) {
            return Try.of(() -> primaryKycClient.verify(playerId, document))
                .onFailure(e -> log.warn("Onfido verification failed, switching to fallback", e))
                .toOption();
        }

        // Fallback to secondary provider
        log.info("Using fallback KYC provider: Jumio");
        return Try.of(() -> fallbackKycClient.verify(playerId, document))
            .onFailure(e -> log.error("Both KYC providers failed", e))
            .toOption();
    }
}
```

**服務恢復自動檢測**：
- Health Check 間隔：30 秒
- 恢復條件：連續 3 次 Health Check 通過（可用性 > 95%）
- 自動切回主服務，記錄恢復事件至告警頻道

---

## 📚 相關文檔

### 已整合模塊參考
- [01-03 支付網關整合](../02_Finance_Center/02-02_Payment_Gateway_Integration.md) - PSP 整合詳細規範
- [02-01 遊戲整合標準](../03_Game_Center/03-01_Game_Integration_Standard.md) - GP 整合協議
- [01-01 玩家生命週期](../01_Player_Center/01-01_Player_Lifecycle.md) - KYC 驗證流程

### 技術基礎設施參考
- [07-03 API 設計標準](../09_Technical_Infrastructure/09-03-01_Design_Principles.md) - RESTful 規範
- [05-05 數據安全標準](../06_Platform_Governance/06-05_Data_Security.md) - API 金鑰加密存儲
- [07-02 網關架構](../09_Technical_Infrastructure/09-02-01_Gateway_Core.md) - 速率限制實作

### Analytics & Operations
- [06-01 報表與 BI](../08_Analytics_BI/08-01_Reporting_BI.md) - 第三方數據整合至 BI 平台
- [06-02 客戶服務](../13_Customer_Service/13-01_CS_Platform_Design.md) - 第三方工單系統整合

---

**文檔版本**: 4.0.0
**最後更新**: 2026-02-04
**維護團隊**: Integration Team & Infrastructure Team
**遷移歷史**: 從 13-01 遷移至 06-03（Phase 4 Module Merge）
