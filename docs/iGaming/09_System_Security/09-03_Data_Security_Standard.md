# 09-03 數據安全標準 (Data Security Standard)

## 📌 文檔導航

本文檔已拆分為 **3 個專項子文檔**，請根據需求選擇閱讀：

| 子文檔 | 主要內容 | 適用角色 |
|-------|---------|---------|
| **[09-03-01 加密策略](./09-03-01_Encryption_Strategy.md)** | AES-256-GCM 加密、密碼雜湊、數據脫敏、金鑰管理 | 後端開發、安全工程師 |
| **[09-03-02 Blind Index 架構](./09-03-02_Blind_Index_Architecture.md)** | 可檢索加密、HMAC 索引、金鑰輪替、效能優化 | 後端開發、數據庫工程師 |
| **[09-03-03 GDPR 數據刪除](./09-03-03_GDPR_Data_Deletion.md)** | Crypto-Shredding、數據遺忘權、合規證明、狀態機 | 安全工程師、法務團隊 |

---

## 1. 系統概述

為符合 **GDPR**、**PCI-DSS** 與各國個資保護法規（CCPA, PDPA, LGPD），本模組定義了 **敏感數據（Sensitive Data）** 在 **存儲（Data at Rest）** 與 **傳輸（Data in Transit）** 過程中的安全標準。

**核心原則**：
- **零信任架構**：假設資料庫、備份、日誌都可能洩露，因此 PII 必須加密
- **應用層加密**：數據在寫入資料庫前就已加密，DBA 無法查看明文
- **可檢索加密**：通過 Blind Index 實現加密數據的精確查詢
- **Crypto-Shredding**：通過銷毀金鑰實現數據永久不可恢復（符合 GDPR）

---

## 2. PII 定義 (Personally Identifiable Information)

以下欄位必須被視為 **高度敏感**，嚴禁明文存儲：

| PII 欄位 | 範例 | 風險等級 | 加密要求 |
|---------|------|---------|---------|
| **姓名** | "張三"、"John Doe" | 高 | AES-256-GCM |
| **手機號** | "+886912345678" | 極高 | AES-256-GCM + Blind Index |
| **Email** | "player@example.com" | 極高 | AES-256-GCM + Blind Index |
| **銀行帳號** | "1234567890" | 極高 | AES-256-GCM + Blind Index |
| **身分證字號** | "A123456789" | 極高 | AES-256-GCM + Blind Index |
| **密碼** | "P@ssw0rd123" | 極高 | **Argon2id Hash**（不可逆）|
| **家庭住址** | "台北市信義區..." | 中 | AES-256-GCM |
| **IP 地址** | "1.2.3.4" | 中 | HMAC-SHA256（Blind Index）|

**詳細加密策略**：參見 [09-03-01 加密策略](./09-03-01_Encryption_Strategy.md)

---

## 3. 核心技術架構

### 3.1 存儲加密（Column-Level Encryption）

**採用應用層加密**（Application-Level Encryption）：
- **算法**：AES-256-GCM（Authenticated Encryption）
- **IV**：每次加密隨機生成（12 bytes）
- **金鑰管理**：AWS KMS / HashiCorp Vault
- **存儲格式**：`[Version]:[IV]:[Ciphertext]:[AuthTag]`

**資料庫 Schema 範例**：

**詳細內容**：[09-03-01 §3 存儲加密](./09-03-01_Encryption_Strategy.md#3-存儲加密-column-level-encryption)

### 3.2 可檢索加密（Blind Index）

**問題**：AES 密文無法被索引 → 無法進行 `WHERE encrypted_phone = '+886912345678'`

**解決方案**：Blind Index（盲索引）
- 使用 HMAC-SHA256 生成可索引的雜湊值
- 單向性：無法從 Index 反推明文
- 確定性：相同輸入永遠生成相同 Index

**工作流程**：
```sql
寫入路徑：
1. 輸入：+886912345678
2. AES-256-GCM(phone) → encrypted_phone
3. HMAC-SHA256(phone, blind_key) → phone_index
4. 存儲：encrypted_phone + phone_index

查詢路徑：
1. 輸入：+886912345678
2. HMAC-SHA256(phone, blind_key) → computed_index
3. WHERE phone_index = computed_index
4. 解密 encrypted_phone 顯示給客服
```

**詳細內容**：[09-03-02 Blind Index 架構](./09-03-02_Blind_Index_Architecture.md)

### 3.3 Crypto-Shredding（加密粉碎）

**GDPR 數據遺忘權要求**：玩家有權要求刪除所有個人數據

**傳統刪除的問題**：
- 資料庫備份中仍存在明文/密文
- 軟刪除（`deleted_at`）資料仍可查詢

**Crypto-Shredding 原理**：
```sql
雙層加密架構：
- Master Key (KEK) 存於 AWS KMS
  ↓ Encrypts
- Per-Player DEK (Data Encryption Key)
  ↓ Encrypts
- Player PII

刪除流程：
DELETE FROM user_keys WHERE player_id = ?
→ DEK 銷毀
→ 所有 PII 永久不可恢復（即使有備份）
```

**完整流程**：[09-03-03 §5 詳細執行流程](./09-03-03_GDPR_Data_Deletion.md#5-詳細執行流程-step-by-step-workflow)

---

## 4. 密碼安全

### 4.1 密碼雜湊算法

**使用 Argon2id**（優於 bcrypt/PBKDF2）：

**為何選擇 Argon2id？**
| 算法 | 優勢 | 劣勢 |
|------|------|------|
| **MD5** | 快速 | ❌ 已被破解 |
| **bcrypt** | 抗暴力破解 | 易受 GPU 攻擊 |
| **Argon2id** | **抗 CPU/GPU/ASIC 攻擊** | 計算成本高（這正是優勢）|

**詳細內容**：[09-03-01 §4 密碼雜湊](./09-03-01_Encryption_Strategy.md#4-密碼雜湊-password-hashing)

### 4.2 登入保護

**限制重試次數**（防止暴力破解）：

**密碼複雜度要求**：
- 最少 8 個字符
- 至少 1 個大寫字母、1 個小寫字母、1 個數字、1 個特殊字符

---

## 5. 數據脫敏 (Data Masking)

### 5.1 脫敏規範

**所有 API 回傳給前端時，必須依據角色權限進行脫敏**：

| PII 欄位 | 脫敏規則 | 範例 |
|---------|---------|------|
| **Name** | 保留首尾字符 | `David Beckham` → `D***m` |
| **Phone** | 保留前3後3 | `0912345678` → `091****678` |
| **Email** | 保留前2與域名 | `david@gmail.com` → `da***@gmail.com` |
| **Bank Account** | 保留後4碼 | `1234567890` → `******7890` |

### 5.2 權限分級脫敏

| 角色 | Phone | Email | Bank Account |
|------|-------|-------|-------------|
| **玩家本人** | 明文（需二次驗證）| 明文 | `******7890`（後4碼）|
| **客服（L1）** | `091****678` | `da***@gmail.com` | 無權查看 |
| **風控人員** | 明文（需審批）| 明文（需審批）| 明文（需審批）|
| **DBA** | 密文（無法解密）| 密文（無法解密）| 密文（無法解密）|

**詳細內容**：[09-03-01 §5 數據脫敏](./09-03-01_Encryption_Strategy.md#5-數據脫敏-data-masking)

---

## 6. GDPR 合規

### 6.1 數據遺忘權（Right to be Forgotten）

**玩家可要求刪除所有個人數據**（GDPR Art. 17）

**刪除流程**：
```
1. 玩家提交刪除申請
   ↓
2. 發送確認 Email（7 天有效）
   ↓
3. 玩家確認 → 進入冷靜期（30 天）
   ↓
4. 冷靜期結束 → 執行 Crypto-Shredding
   ↓
5. 發送刪除證明書
```

**例外情況**（暫停刪除）：
- ❌ 帳號調查中（AML/Fraud）
- ❌ 未結訴訟（Pending Legal Case）
- ❌ 未完成流水（Pending Wagering）
- ❌ 未提領餘額（Outstanding Balance > $0）

**詳細內容**：[09-03-03 GDPR 數據刪除](./09-03-03_GDPR_Data_Deletion.md)

### 6.2 資料保留矩陣

**並非所有資料都可刪除**：

| 資料類別 | GDPR 刪除義務 | 保留期限 | 處理方式 |
|---------|--------------|---------|---------|
| 玩家姓名、地址 | ✅ Yes | 無 | Physical Delete |
| 遊戲歷史 | ✅ Yes (anonymize) | 無 | 匿名化（Replace player_id）|
| 交易記錄（AML）| ❌ No (legal obligation) | **5-7 years** | Retain with anonymized player_id |
| 獎金派彩（Tax）| ❌ No (legal obligation) | **7 years** | Retain with anonymized player_id |
| 違規記錄（Ban）| ❌ No (legitimate interest) | **Permanent** | Retain for fraud prevention |

---

## 7. 傳輸安全

### 7.1 強制 HTTPS (TLS 1.3+)

**要求**：
- ❌ 禁止 HTTP 明文傳輸
- ✅ 自動重定向 HTTP → HTTPS
- ✅ HSTS Header 強制瀏覽器使用 HTTPS

**Nginx 配置範例**：
```nginx
server {
    listen 443 ssl http2;
    ssl_protocols TLSv1.3 TLSv1.2;
    ssl_ciphers 'ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384';

    # HSTS（強制 HTTPS）
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains; preload" always;
}
```

### 7.2 微服務通訊安全

**內部服務間通訊**：
- 使用 mTLS（Mutual TLS）雙向認證
- 或使用 Service Mesh（如 Istio）自動加密

---

## 8. 合規性檢查清單

### 8.1 GDPR 合規

| 要求 | 實施狀態 | 實施方式 |
|------|---------|---------|
| **數據最小化** | ✅ 合規 | 僅收集必要 PII |
| **存儲加密** | ✅ 合規 | AES-256-GCM |
| **傳輸加密** | ✅ 合規 | TLS 1.3 |
| **數據遺忘權** | ✅ 合規 | Crypto-Shredding |
| **數據可攜權** | ✅ 合規 | 提供 JSON 導出 |
| **審計日誌** | ✅ 合規 | Elasticsearch 記錄所有 PII 存取 |

### 8.2 PCI-DSS 合規

| 要求 | 實施狀態 | 實施方式 |
|------|---------|---------|
| **禁止儲存完整卡號** | ✅ 合規 | 使用 PSP Token |
| **銀行帳號加密** | ✅ 合規 | AES-256-GCM + Blind Index |
| **密碼雜湊** | ✅ 合規 | Argon2id |
| **存取控制** | ✅ 合規 | RBAC + IP 白名單 |

---

## 9. 實施建議

### 9.1 開發階段

**優先順序**：
1. **P0（必須）**：密碼雜湊（Argon2id）、HTTPS 強制
2. **P1（高優先級）**：PII 加密（AES-256-GCM）、Blind Index
3. **P2（中優先級）**：Crypto-Shredding、數據脫敏
4. **P3（低優先級）**：金鑰輪替自動化、審計日誌

### 9.2 預估工時

| 階段 | 任務 | 預估工時 |
|------|------|---------|
| **Phase 1** | 密碼雜湊 + HTTPS | 1 週 |
| **Phase 2** | PII 加密 + Blind Index | 2-3 週 |
| **Phase 3** | Crypto-Shredding + GDPR 刪除流程 | 3-4 週 |
| **Phase 4** | 數據脫敏 + 權限分級 | 1-2 週 |
| **總計** | | **7-10 週** |

---

## 📚 相關文檔

### 系列文檔（子文檔）
- [09-03-01 加密策略](./09-03-01_Encryption_Strategy.md) - AES-256-GCM、密碼雜湊、金鑰管理
- [09-03-02 Blind Index 架構](./09-03-02_Blind_Index_Architecture.md) - 可檢索加密、HMAC 索引、效能優化
- [09-03-03 GDPR 數據刪除](./09-03-03_GDPR_Data_Deletion.md) - Crypto-Shredding、數據遺忘權、合規證明

### 技術架構參考
- [09-01 管理後台 RBAC](./09-01_Admin_RBAC.md) - 權限分級脫敏
- [09-02 審計日誌系統](./09-02_Audit_Log_System.md) - PII 存取審計
- [12-05 API 設計標準](../12_Technical_Operations/12-05_API_Design_Standard.md) - HTTPS 強制要求

### 業務邏輯參考
- [01-01 玩家賬戶系統](../01_Player_Center/01-01_Player_Account_System.md) - 密碼重置流程
- [02-01 出金風控](../01_Player_Center/01-05_Withdrawal_Risk.md) - 銀行帳號驗證
- [11-01 客服中台設計](../06_Analytics_Operations_NEW/06-02_Customer_Service.md) - 客服 PII 查詢場景

---

**文檔版本**: 2.0.0（拆分版）
**最後更新**: 2026-01-27
**維護團隊**: Security Team & Backend Team
