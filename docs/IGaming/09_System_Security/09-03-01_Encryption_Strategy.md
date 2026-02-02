# 09-03-01 加密策略 (Encryption Strategy)

## 1. 系統概述

為符合 GDPR、PCI-DSS 與各國個資保護法規，本文檔定義 **敏感數據（Sensitive Data）** 在 **存儲（Data at Rest）** 與 **傳輸（Data in Transit）** 過程中的加密標準。

---

## 2. PII 定義 (Personally Identifiable Information)

以下欄位必須被視為 **高度敏感**，嚴禁明文存儲：

| PII 欄位 | 範例 | 風險等級 | 加密要求 |
|---------|------|---------|---------|
| **姓名（Real Name）** | "張三"、"John Doe" | 高 | AES-256-GCM |
| **手機號（Phone Number）** | "+886912345678" | 極高 | AES-256-GCM + Blind Index |
| **Email 地址** | "player@example.com" | 極高 | AES-256-GCM + Blind Index |
| **銀行帳號** | "1234567890" | 極高 | AES-256-GCM + Blind Index |
| **身分證字號/護照號** | "A123456789" | 極高 | AES-256-GCM + Blind Index |
| **密碼（Password）** | "P@ssw0rd123" | 極高 | Argon2id Hash（不可逆）|
| **家庭住址** | "台北市信義區..." | 中 | AES-256-GCM |
| **生日** | "1990-01-01" | 中 | AES-256-GCM |
| **IP 地址（可選）** | "1.2.3.4" | 中 | HMAC-SHA256（Blind Index）|

**分類原則**：
- **極高**：可直接識別個人身份，需同時加密 + 建立 Blind Index
- **高**：可間接識別個人，僅需加密
- **中**：敏感但不可識別，視業務需求決定

---

## 3. 存儲加密 (Column-Level Encryption)

### 3.1 應用層加密 (Application-Level Encryption)

**核心原則**：針對 PII 欄位，採用 **應用層加密（Application-Level Encryption）**，即數據在寫入資料庫 **之前** 就已被加密，資料庫管理員（DBA）無法查看明文。

**加密算法**：`AES-256-GCM` (Authenticated Encryption with Associated Data)

**選擇理由**：
- **AES-256**：業界標準，NIST 認證，抗量子運算攻擊（目前）
- **GCM 模式**：提供 **加密 + 完整性驗證**（AEAD），防止密文被篡改
- **效能優越**：硬體加速支援（AES-NI），加密/解密速度快

### 3.2 加密實現

**存儲格式**（Base64 Encoded）：
```text
[Version]:[IV]:[Ciphertext]:[AuthTag]

範例：
v1:a3f8d9e2c1b4:Y3J5cHRvZ3JhcGh5==:4a7b8c9d
```

**欄位說明**：
| 欄位 | 長度 | 說明 |
|------|------|------|
| Version | 2 bytes | 加密版本（用於金鑰輪替）|
| IV (Initialization Vector) | 12 bytes | 隨機生成，每次加密唯一 |
| Ciphertext | 可變 | AES-GCM 加密後的密文 |
| AuthTag | 16 bytes | GCM 模式的認證標籤 |

**Python 實現範例**：

**使用範例**：

### 3.3 資料庫 Schema 設計

**範例**（玩家表）：

### 3.4 傳輸加密 (Data in Transit)

**強制要求**：
1. **所有 API 通訊必須使用 HTTPS（TLS 1.3+）**：
   - 禁止 HTTP 明文傳輸
   - 自動重定向 HTTP → HTTPS
   - HSTS Header 強制瀏覽器使用 HTTPS

2. **TLS 配置標準**：
   ```nginx
   server {
       listen 443 ssl http2;
       ssl_protocols TLSv1.3 TLSv1.2;  # 禁用 TLSv1.0/1.1
       ssl_ciphers 'ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384';
       ssl_prefer_server_ciphers on;
       ssl_certificate /path/to/cert.pem;
       ssl_certificate_key /path/to/key.pem;

       # HSTS (強制 HTTPS)
       add_header Strict-Transport-Security "max-age=31536000; includeSubDomains; preload" always;

       # 其他安全標頭
       add_header X-Frame-Options "DENY" always;
       add_header X-Content-Type-Options "nosniff" always;
   }
   ```text

3. **內部服務通訊**（微服務間）：
   - 使用 mTLS（Mutual TLS）雙向認證
   - 或使用 Service Mesh（如 Istio）自動加密

---

## 4. 密碼雜湊 (Password Hashing)

### 4.1 算法選擇

**使用 `Argon2id`**（優於 bcrypt/PBKDF2）

**選擇理由**：
| 算法 | 發布年份 | 優勢 | 劣勢 |
|------|---------|------|------|
| **MD5** | 1992 | 快速 | ❌ 已被破解，禁止使用 |
| **bcrypt** | 1999 | 抗暴力破解 | 僅抗 CPU 攻擊，易受 GPU 攻擊 |
| **PBKDF2** | 2000 | NIST 認證 | 易受 GPU/ASIC 攻擊 |
| **Argon2id** | 2015 | **抗 CPU/GPU/ASIC 攻擊** | 計算成本高（但這正是優勢）|

**Argon2 版本選擇**：
- **Argon2d**：抗 GPU 攻擊，但易受側信道攻擊
- **Argon2i**：抗側信道攻擊，但稍弱於 GPU 防護
- **Argon2id**（推薦）：**混合模式**，兼具兩者優點

### 4.2 參數配置

**推薦參數**（基於 OWASP 建議）：

**參數說明**：
| 參數 | 建議值 | 說明 |
|------|--------|------|
| `time_cost` | 3 | 迭代次數，值越大越慢（目標：0.5-1秒）|
| `memory_cost` | 65536（64 MB）| 記憶體消耗，防止 GPU 平行攻擊 |
| `parallelism` | 2 | CPU 核心數，值越大越快（但降低安全性）|
| `salt_len` | 16 bytes | 每個用戶獨立隨機 Salt |

**性能測試**（調整參數以達到目標時長）：

### 4.3 實現範例

**註冊時雜湊密碼**：

**登入時驗證密碼**：

**自動重雜湊（Rehash）機制**：

### 4.4 安全最佳實踐

**1. 不要在 Hash 前對密碼進行額外處理**：

**2. 限制密碼重試次數**（防止暴力破解）：

**3. 密碼複雜度要求**：

---

## 5. 數據脫敏 (Data Masking)

### 5.1 脫敏規範

**所有 API 回傳給前端（Frontend/Backoffice）時，必須依據角色權限進行脫敏**。

| PII 欄位 | 脫敏規則 | 範例 | 適用場景 |
|---------|---------|------|---------|
| **Name** | 保留首尾字符（中文則保留姓）| `David Beckham` → `D***m`<br>`王小明` → `王*明` | 客服查詢、VIP 管理 |
| **Phone** | 保留前3後3 | `0912345678` → `091****678` | 客服查詢、出金審核 |
| **Email** | 保留前2與域名 | `david@gmail.com` → `da***@gmail.com` | 客服查詢、帳戶設定 |
| **Bank Account** | 保留後4碼 | `1234567890` → `******7890` | 出金審核、財務報表 |
| **ID Number** | 保留前2後2 | `A123456789` → `A1*****89` | KYC 驗證、合規審查 |
| **IP Address** | 保留前2段 | `192.168.1.100` → `192.168.*.*` | 風控分析、日誌查詢 |

### 5.2 實現層級

**禁止**：在 Frontend 進行脫敏（掩耳盜鈴，API Response 仍是明文）
**必須**：在 Backend DTO Converter 層或 Serializer 層處理

**Python 實現範例**：

**API 層整合**：

### 5.3 權限分級脫敏

**不同角色看到不同級別的脫敏**：

| 角色 | Phone | Email | Bank Account |
|------|-------|-------|-------------|
| **玩家本人** | 明文（需二次驗證）| 明文 | `******7890`（後4碼）|
| **客服（Level 1）** | `091****678` | `da***@gmail.com` | 無權查看 |
| **客服主管（Level 2）** | `0912****78`（前4後2）| `dav***@gmail.com` | `******7890` |
| **財務人員** | `091****678` | 無權查看 | 明文（需審批）|
| **風控人員** | 明文（需審批）| 明文（需審批）| 明文（需審批）|
| **DBA（資料庫管理員）** | 密文（無法解密）| 密文（無法解密）| 密文（無法解密）|

**實現範例**：

---

## 6. 金鑰管理 (Key Management)

### 6.1 金鑰層級架構

**雙層金鑰系統**（推薦）：
```
┌────────────────────────────────────────────┐
│  Master Key (CMK - Customer Master Key)   │
│  - Stored in AWS KMS / HashiCorp Vault    │
│  - Never leaves HSM (Hardware Security    │
│    Module)                                 │
│  - Rotated every 365 days                 │
└──────────────────┬─────────────────────────┘
                   │ Encrypts
                   ↓
┌────────────────────────────────────────────┐
│  Data Encryption Keys (DEK)               │
│  - Generated per encryption operation      │
│  - Cached in application memory (ephemeral)│
│  - Encrypted by CMK before storage        │
└────────────────────────────────────────────┘
```

### 6.2 AWS KMS 整合

**範例**（Python + Boto3）：

**金鑰快取策略**：

### 6.3 金鑰輪替（Key Rotation）

**自動輪替策略**：
- **頻率**：每 365 天自動輪替 CMK
- **方式**：AWS KMS 自動管理，應用層無需修改
- **過渡期**：新舊金鑰共存 90 天（graceful migration）

**手動觸發輪替**（緊急情況，如金鑰洩露）：

### 6.4 金鑰存取控制

**IAM 政策範例**（最小權限原則）：
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "kms:Decrypt",
        "kms:GenerateDataKey"
      ],
      "Resource": "arn:aws:kms:us-east-1:123456789:key/abc-123",
      "Condition": {
        "StringEquals": {
          "kms:ViaService": "rds.us-east-1.amazonaws.com"
        }
      }
    }
  ]
}
```

**原則說明**：
- 僅允許 `Decrypt` 與 `GenerateDataKey`（不允許 `Encrypt`，避免濫用）
- 僅允許通過特定服務（RDS）調用（防止意外洩露）
- 禁止 `ScheduleKeyDeletion`（防止誤刪除）

---

## 📚 相關文檔

### 系列文檔
- [09-03-02 Blind Index 架構](./09-03-02_Blind_Index_Architecture.md) - 可檢索加密、HMAC 索引
- [09-03-03 GDPR 數據刪除](./09-03-03_GDPR_Data_Deletion.md) - Crypto-Shredding、數據遺忘權

### 技術架構參考
- [09-01 管理後台 RBAC](./09-01_Admin_RBAC.md) - 權限分級脫敏
- [09-02 審計日誌系統](./09-02_Audit_Log_System.md) - PII 存取審計
- [12-05 API 設計標準](../12_Technical_Operations/12-05_API_Design_Standard.md) - HTTPS 強制要求

### 業務邏輯參考
- [01-01 玩家賬戶系統](../01_Player_Center/01-01_Player_Account_System.md) - 密碼重置流程
- [02-01 出金風控](../02_Finance_Center/02-01_Withdrawal_Risk_Control.md) - 銀行帳號驗證

---

**文檔版本**: 1.0.0
**最後更新**: 2026-01-27
**維護團隊**: Security Team & Backend Team
