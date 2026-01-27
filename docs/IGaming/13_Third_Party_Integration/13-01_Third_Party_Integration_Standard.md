# 13-01 第三方整合標準 (Third-Party Integration Standard)

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

**詳細規範**: 參考 [03-01 遊戲整合標準](../03_Game_Center/03-01_Game_Integration_Standard.md)

**關鍵特性**：
- Seamless Wallet 整合
- 遊戲啟動 URL 生成
- 投注/派彩 Webhook 接收

---

### 2.2 支付服務商 (Payment Service Providers)

**詳細規範**: 參考 [02-02 支付網關整合](../02_Finance_Center/02-02_Payment_Gateway_Integration.md)

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

**整合流程範例** (Onfido):
```python
import requests

def verify_player_kyc(player_id, id_document_photo, selfie_photo):
    # Step 1: 創建 Onfido Applicant
    applicant_response = requests.post(
        "https://api.onfido.com/v3/applicants",
        headers={"Authorization": f"Token token={ONFIDO_API_KEY}"},
        json={
            "first_name": player.first_name,
            "last_name": player.last_name,
            "email": player.email
        }
    )
    applicant_id = applicant_response.json()["id"]

    # Step 2: 上傳文件
    document_response = requests.post(
        "https://api.onfido.com/v3/documents",
        headers={"Authorization": f"Token token={ONFIDO_API_KEY}"},
        files={"file": id_document_photo},
        data={"applicant_id": applicant_id, "type": "passport"}
    )

    # Step 3: 創建檢查（異步）
    check_response = requests.post(
        "https://api.onfido.com/v3/checks",
        headers={"Authorization": f"Token token={ONFIDO_API_KEY}"},
        json={
            "applicant_id": applicant_id,
            "report_names": ["document", "facial_similarity_photo"]
        }
    )
    check_id = check_response.json()["id"]

    # Step 4: 等待 Webhook 回調（異步處理）
    # Webhook URL: https://platform.com/api/v1/webhooks/onfido
    return {"check_id": check_id, "status": "pending"}
```

**Webhook 回調處理**：
```python
from fastapi import Request, HTTPException
import hmac
import hashlib

@app.post("/api/v1/webhooks/onfido")
async def onfido_webhook(request: Request):
    # Step 1: 驗證 Webhook 簽名
    signature = request.headers.get("X-Signature")
    body = await request.body()

    expected_signature = hmac.new(
        ONFIDO_WEBHOOK_SECRET.encode(),
        body,
        hashlib.sha256
    ).hexdigest()

    if signature != expected_signature:
        raise HTTPException(status_code=403, detail="Invalid signature")

    # Step 2: 處理回調
    payload = await request.json()
    check_id = payload["object"]["id"]
    result = payload["object"]["result"]  # "clear", "consider"

    if result == "clear":
        # 更新玩家 KYC 狀態為已驗證
        update_player_kyc_status(check_id, status="approved")
    else:
        # 需要人工審核
        create_manual_review_task(check_id, reason=payload["object"]["sub_result"])

    return {"status": "ok"}
```

---

### 2.4 行銷工具

| 工具類型 | 供應商 | 用途 | 整合方式 |
|---------|--------|------|---------|
| **Email** | SendGrid, AWS SES | 交易郵件、行銷郵件 | REST API |
| **SMS** | Twilio, Vonage | 驗證碼、提款通知 | REST API |
| **推送通知** | OneSignal, Firebase | App 推送 | SDK + REST API |
| **行銷自動化** | Braze, Customer.io | 玩家生命週期管理 | REST API + Webhook |

**SendGrid 郵件發送範例**：
```python
import requests

def send_withdrawal_approval_email(player_email, withdrawal_amount):
    response = requests.post(
        "https://api.sendgrid.com/v3/mail/send",
        headers={
            "Authorization": f"Bearer {SENDGRID_API_KEY}",
            "Content-Type": "application/json"
        },
        json={
            "personalizations": [{
                "to": [{"email": player_email}],
                "dynamic_template_data": {
                    "amount": withdrawal_amount,
                    "currency": "USD"
                }
            }],
            "from": {"email": "noreply@platform.com", "name": "Platform Support"},
            "template_id": "d-12345abc"  # SendGrid 模板 ID
        }
    )
    return response.json()
```

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
```python
import requests
from requests.adapters import HTTPAdapter
from requests.packages.urllib3.util.retry import Retry

def create_http_client():
    """創建帶重試機制的 HTTP 客戶端"""
    session = requests.Session()

    # 配置重試策略
    retry = Retry(
        total=3,                          # 最多重試 3 次
        backoff_factor=1,                 # 指數退避：1s, 2s, 4s
        status_forcelist=[500, 502, 503, 504],  # 這些狀態碼才重試
        method_whitelist=["GET", "POST"]  # 只重試冪等方法
    )

    adapter = HTTPAdapter(max_retries=retry)
    session.mount("https://", adapter)
    session.mount("http://", adapter)

    return session

# 使用範例
http_client = create_http_client()
response = http_client.post(
    "https://api.kyc-provider.com/v1/verify",
    headers={"Authorization": f"Bearer {API_KEY}"},
    json={"player_id": "12345"},
    timeout=10  # 10 秒超時
)
```

---

### 3.2 Webhook 模式

**使用場景**: 異步通知（如支付回調、KYC 結果回調）

**安全驗證**:
```python
import hmac
import hashlib
from fastapi import Header, HTTPException

def verify_webhook_signature(
    body: bytes,
    signature: str,
    secret: str
) -> bool:
    """驗證 Webhook 簽名"""
    expected_signature = hmac.new(
        secret.encode(),
        body,
        hashlib.sha256
    ).hexdigest()

    return hmac.compare_digest(signature, expected_signature)

@app.post("/webhooks/payment")
async def payment_webhook(
    request: Request,
    x_signature: str = Header(...)
):
    body = await request.body()

    if not verify_webhook_signature(body, x_signature, PAYMENT_WEBHOOK_SECRET):
        raise HTTPException(status_code=403, detail="Invalid signature")

    payload = await request.json()
    # 處理支付回調...
    return {"status": "ok"}
```

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

**配置數據模型**:
```sql
CREATE TABLE webhook_subscriptions (
    subscription_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,  -- 哪個租戶訂閱
    webhook_url VARCHAR(500) NOT NULL,
    events TEXT NOT NULL,  -- JSON array: ["player.registered", "player.deposited"]
    secret_key VARCHAR(100) NOT NULL,  -- 用於簽名

    -- 重試策略
    max_retries TINYINT DEFAULT 3,
    retry_backoff ENUM('linear', 'exponential') DEFAULT 'exponential',

    -- 狀態
    is_active BOOLEAN DEFAULT TRUE,
    failed_count INT DEFAULT 0,

    INDEX idx_tenant (tenant_id),
    INDEX idx_active (is_active)
);
```

**發送邏輯** (帶重試):
```python
import requests
import time
import hmac
import hashlib

def send_webhook(subscription_id, event_type, payload):
    subscription = db.query(
        "SELECT * FROM webhook_subscriptions WHERE subscription_id = %s",
        (subscription_id,)
    ).fetchone()

    if not subscription['is_active']:
        return

    # 生成簽名
    payload_json = json.dumps(payload)
    signature = hmac.new(
        subscription['secret_key'].encode(),
        payload_json.encode(),
        hashlib.sha256
    ).hexdigest()

    headers = {
        'Content-Type': 'application/json',
        'X-Webhook-Signature': signature,
        'X-Event-Type': event_type
    }

    # 重試邏輯
    for attempt in range(subscription['max_retries']):
        try:
            response = requests.post(
                subscription['webhook_url'],
                data=payload_json,
                headers=headers,
                timeout=10
            )

            if response.status_code == 200:
                # 成功，重置失敗計數
                db.execute(
                    "UPDATE webhook_subscriptions SET failed_count = 0 WHERE subscription_id = %s",
                    (subscription_id,)
                )
                return True

        except requests.RequestException as e:
            logger.error(f"Webhook delivery failed (attempt {attempt + 1}): {e}")

        # 指數退避
        if subscription['retry_backoff'] == 'exponential':
            time.sleep(2 ** attempt)  # 1s, 2s, 4s
        else:
            time.sleep(5)  # 固定 5 秒

    # 所有重試失敗
    db.execute(
        "UPDATE webhook_subscriptions SET failed_count = failed_count + 1 WHERE subscription_id = %s",
        (subscription_id,)
    )

    # 失敗次數 > 10 次，自動禁用
    if subscription['failed_count'] + 1 > 10:
        db.execute(
            "UPDATE webhook_subscriptions SET is_active = FALSE WHERE subscription_id = %s",
            (subscription_id,)
        )
        alert_to_ops_team(f"Webhook {subscription_id} disabled after 10 failures")

    return False
```

---

### 4.2 入站 Webhook (Inbound Webhooks)

**集中式 Webhook 接收器**:
```python
from fastapi import FastAPI, Request, HTTPException

app = FastAPI()

# 支持的 Provider 及其驗證邏輯
WEBHOOK_PROVIDERS = {
    "nuvei": {
        "secret_key": os.getenv("NUVEI_WEBHOOK_SECRET"),
        "signature_header": "X-Nuvei-Signature"
    },
    "pragmatic": {
        "secret_key": os.getenv("PRAGMATIC_WEBHOOK_SECRET"),
        "signature_header": "X-GP-Signature"
    },
    "onfido": {
        "secret_key": os.getenv("ONFIDO_WEBHOOK_SECRET"),
        "signature_header": "X-Signature"
    }
}

@app.post("/api/v1/webhooks/{provider}")
async def unified_webhook_receiver(provider: str, request: Request):
    if provider not in WEBHOOK_PROVIDERS:
        raise HTTPException(status_code=404, detail="Unknown provider")

    config = WEBHOOK_PROVIDERS[provider]
    body = await request.body()
    signature = request.headers.get(config["signature_header"])

    # 驗證簽名
    expected_signature = hmac.new(
        config["secret_key"].encode(),
        body,
        hashlib.sha256
    ).hexdigest()

    if not hmac.compare_digest(signature or "", expected_signature):
        raise HTTPException(status_code=403, detail="Invalid signature")

    # 解析並路由到對應處理器
    payload = await request.json()

    if provider == "nuvei":
        handle_payment_callback(payload)
    elif provider == "pragmatic":
        handle_game_callback(payload)
    elif provider == "onfido":
        handle_kyc_callback(payload)

    return {"status": "ok"}
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

**應用層讀取金鑰**:
```python
import hvac

# 初始化 Vault 客戶端
vault_client = hvac.Client(url='https://vault.internal:8200')
vault_client.auth.approle.login(
    role_id=os.getenv('VAULT_ROLE_ID'),
    secret_id=os.getenv('VAULT_SECRET_ID')
)

# 讀取金鑰
nuvei_secrets = vault_client.secrets.kv.v2.read_secret_version(
    path='psp/nuvei'
)
NUVEI_SECRET_KEY = nuvei_secrets['data']['data']['secret_key']
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

**Redis 實作** (Token Bucket 算法):
```python
import redis
import time

redis_client = redis.Redis(host='localhost', port=6379)

def rate_limit(service_name, limit, window_seconds):
    """
    Token Bucket 限流算法
    :param service_name: 服務名稱 (e.g. "onfido_kyc")
    :param limit: 允許的最大請求數
    :param window_seconds: 時間窗口（秒）
    :return: True 允許請求，False 超限
    """
    key = f"rate_limit:{service_name}"
    current_time = int(time.time())

    # 使用 Redis Sorted Set 存儲請求時間戳
    pipe = redis_client.pipeline()
    pipe.zadd(key, {str(current_time): current_time})
    pipe.zremrangebyscore(key, 0, current_time - window_seconds)  # 移除過期記錄
    pipe.zcard(key)  # 計算當前請求數
    pipe.expire(key, window_seconds)  # 設置過期時間
    results = pipe.execute()

    request_count = results[2]

    if request_count <= limit:
        return True
    else:
        return False

# 使用範例
if rate_limit("onfido_kyc", limit=100, window_seconds=60):
    # 允許請求
    make_onfido_api_call()
else:
    # 超限，返回 429
    raise HTTPException(status_code=429, detail="Rate limit exceeded")
```

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

**Prometheus 指標採集**:
```python
from prometheus_client import Counter, Histogram

# 定義指標
third_party_requests_total = Counter(
    'third_party_requests_total',
    'Total requests to third-party services',
    ['service', 'status']
)

third_party_request_duration = Histogram(
    'third_party_request_duration_seconds',
    'Request duration to third-party services',
    ['service']
)

# 使用範例
def make_onfido_request():
    with third_party_request_duration.labels(service='onfido').time():
        try:
            response = requests.post("https://api.onfido.com/...")
            third_party_requests_total.labels(service='onfido', status='success').inc()
            return response
        except Exception as e:
            third_party_requests_total.labels(service='onfido', status='error').inc()
            raise
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

## 📚 相關文檔

### 已整合模塊參考
- [02-02 支付網關整合](../02_Finance_Center/02-02_Payment_Gateway_Integration.md) - PSP 整合詳細規範
- [03-01 遊戲整合標準](../03_Game_Center/03-01_Game_Integration_Standard.md) - GP 整合協議
- [01-01 玩家賬戶系統](../01_Player_Center/01-01_Player_Account_System.md) - KYC 驗證流程

### 技術基礎設施參考
- [12-05 API 設計標準](../12_Technical_Operations/12-05_API_Design_Standard.md) - RESTful 規範
- [09-03 數據安全標準](../09_System_Security/09-03_Data_Security_Standard.md) - API 金鑰加密存儲
- [12-03 網關架構](../12_Technical_Operations/12-03_Gateway_Architecture.md) - 速率限制實作

---

**文檔版本**: 1.0.0
**最後更新**: 2026-01-27
**維護團隊**: Integration Team & Infrastructure Team
