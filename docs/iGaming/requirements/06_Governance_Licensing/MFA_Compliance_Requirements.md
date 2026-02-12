# MFA 合規與審計要求

> **Canonical Source**: [06-06-04_Compliance_Audit.md](../../source-archive/06_Platform_Governance/06-06-04_Compliance_Audit.md)
> **Audience**: 高層管理、合規官員
> **Related Doc**: [MFA_Compliance_Technical.md](../../architecture/06_Platform_Core/MFA_Compliance_Technical.md)
> **Last Synced**: 2026-02-09
>
> **Refinement Note**: 技術細節（Backup Code AES-256-GCM 加密、Identity Document S3 上傳實作、Audit Log JSONB 格式 + Kafka 整合、Anomaly Detection 規則實作、HTTP 狀態碼、Redis 快取配置）已移至 Architecture 層。本文檔專注於業務政策、合規要求和運營程序。

---

## Business Value

MFA 合規與審計為業務提供關鍵價值：
- **監管合規 (Regulatory Compliance)**：滿足 PCI DSS 強制要求（管理帳號 MFA）、GDPR Art. 30（審計日誌）、MGA 安全措施、NIST SP 800-63B（TOTP 主要、SMS 僅備用），避免牌照暫停和罰款
- **基於風險的訪問控制 (Risk-Based Access Control)**：差異化 MFA 策略（Option B）通過為高風險角色提供安全性（5/5）、為低風險角色提供用戶體驗（4/5）平衡，達到 4.8/5.0 加權分數，避免對 80%+ 員工造成干擾
- **欺詐防範 (Fraud Prevention)**：異常檢測規則（地理位置、多次失敗、頻繁使用備份碼）在可疑活動發生數分鐘內觸發自動帳號鎖定和安全警報
- **運營連續性 (Operational Continuity)**：多層備份策略（10 個備份碼、設備信任 30 天、SMS OTP 備用、緊急聯繫人驗證）確保授權用戶維持訪問的同時阻止攻擊者

---

## Success Metrics

| 指標 | 目標 | 測量方式 |
|--------|--------|-------------|
| 高風險角色 MFA 覆蓋率 | 100% 強制執行 | Super Admin、Finance Manager、Risk Control、Database Admin、DevOps 全部啟用（無豁免） |
| 監管合規率 | 100% | 通過所有 PCI DSS、GDPR Art. 30、MGA、NIST SP 800-63B 審計要求 |
| 異常檢測響應時間 | 5 分鐘內警報 | 從觸發條件到 Security Team 通知的時間 |
| 設備丟失恢復 SLA | ≤24 小時審批 | 從恢復請求提交到 Security Team 審查完成的時間 |
| 備份碼可用性 | 90% 用戶保持 ≥3 個未使用碼 | 擁有足夠剩餘備份碼的活躍 MFA 用戶百分比 |
| 審計日誌保留合規 | 100% | 所有 CRITICAL 和 WARNING 事件永久保留，INFO 事件 90 天 |
| 誤報率（地理異常） | ≤5% | 屬於合法用戶旅行而非欺詐的地理位置警報 |
| MFA 鎖定事件率 | 每月 ≤2% | 因 3 次連續 MFA 失敗而被鎖定的用戶百分比 |

---

## 1. 目的

本文檔定義 SmartAdmin iGaming 平台的業務合規要求、審計政策、監管要求和基於角色的 MFA 政策。涵蓋備份和恢復程序、角色級執行規則、審計事件追蹤和實施路線圖（從業務角度）。

---

## 2. Backup Code 政策

### 2.1 問題陳述

用戶可能因手機丟失、Google Authenticator 卸載或設備損壞而失去對基於 TOTP 的 MFA 的訪問。

### 2.2 Backup Code 規格

| 屬性 | 要求 |
|-----------|------------|
| **數量** | 每個用戶 10 個一次性備份碼 |
| **格式** | 8 位數字，顯示為 XXXX-XXXX（例如 1234-5678） |
| **使用** | 每個碼單次使用；一旦消耗，無法重複使用 |
| **存儲** | 碼安全存儲並追蹤已使用/未使用狀態 |
| **低碼警告** | 當剩餘未使用碼降至 2 個或更少時，提示用戶重新生成 |
| **重新生成** | 需要活躍 TOTP 驗證才能生成新碼 |

→ **[Backup Code 存儲與加密](../../architecture/06_Platform_Core/MFA_Compliance_Technical.md#backup-code-encryption)** - AES-256-GCM 加密算法、安全生成、數據庫架構、狀態追蹤實作

### 2.3 Backup Code 登入政策

1. 用戶在 MFA 驗證期間提交備份碼代替 TOTP
2. 系統根據存儲列表檢查碼
3. 如果碼有效且未使用：
   - 標記為已使用並記錄時間戳
   - 授予訪問權限
   - 如果剩餘碼為 2 個或更少，顯示警告建議重新生成
4. 如果碼無效或已使用：
   - 拒絕訪問
   - 記錄審計日誌條目（BACKUP_CODE_INVALID）

### 2.4 Backup Code 管理

**查看剩餘碼**：用戶可以查看剩餘多少備份碼（總數、已使用、剩餘、最後使用日期），但無法看到實際碼。

**重新生成碼**：用戶可以重新生成一整套新的備份碼，這將使所有先前的碼失效。此操作需要活躍 TOTP 驗證。

---

## 3. 設備丟失恢復政策

### 3.1 場景

用戶丟失手機、無法使用 TOTP、且沒有剩餘備份碼。

### 3.2 多步驟恢復流程

恢復流程需要通過以下所有驗證步驟：

| 步驟 | 驗證方法 | 描述 |
|------|-------------------|-------------|
| 1 | **身份證件 (Identity Document)** | 用戶提交護照、駕照或國民身份證照片 |
| 2 | **Email 驗證** | OTP 發送到註冊 Email 地址 |
| 3 | **Phone 驗證** | SMS OTP 發送到註冊電話號碼 |
| 4 | **安全問題 (Security Questions)** | 回答 3 個預配置的安全問題 |
| 5 | **人工審核 (Human Review)** | Security Team 審核並批准請求 |

### 3.3 恢復結果

- 如果所有驗證通過且 Security Team 批准：
  - 刪除舊的 MFA 配置
  - 生成新的 TOTP Secret 並發送給用戶
  - 用戶掃描新的 QR Code 重新激活 MFA
- 如果任何驗證失敗：
  - 恢復請求被拒絕
  - 引導用戶聯繫客戶服務

### 3.4 恢復請求追蹤

每個恢復請求必須追蹤：
- 用戶 ID 和恢復原因
- 身份證件上傳
- 審核狀態（PENDING / APPROVED / REJECTED）
- 審核者 ID 和審核時間戳
- 完整審計追蹤

→ **[Identity Document 上傳實作](../../architecture/06_Platform_Core/MFA_Compliance_Technical.md#identity-document-upload)** - S3 存儲配置、文件驗證、安全上傳 API、文檔驗證工作流

---

## 4. 緊急聯繫人驗證

### 4.1 增強安全措施

用戶可以在 MFA 設置期間指定 1-2 個緊急聯繫人（同事或經理）。

### 4.2 緊急聯繫人恢復流程

1. 用戶提交恢復請求
2. 系統向指定的緊急聯繫人發送驗證 Email
3. 緊急聯繫人點擊驗證鏈接確認請求合法
4. Security Team 收到通知並進行加速審核

### 4.3 聯繫人類型

| 類型 | 描述 |
|------|------------|
| **COLLEAGUE** | 同級團隊成員可以驗證身份 |
| **MANAGER** | 直屬主管有權確認 |

---

## 5. 基於角色的 MFA 策略

### 5.1 強制 MFA 角色（高風險）

以下角色**必須**啟用 MFA。不允許豁免。

| 角色 | 高風險操作 | MFA 要求 | 備註 |
|------|---------------------|-----------------|-------|
| **Super Admin** | 修改系統配置、刪除用戶、更改權限 | 強制 | 無豁免 |
| **Finance Manager** | 調整玩家餘額、批准提款 | 強制 | 無豁免 |
| **Risk Control** | 修改風控規則、白名單/黑名單管理 | 強制 | 無豁免 |
| **Database Admin** | 直接訪問生產數據庫 | 強制 | 需要額外 YubiKey |
| **DevOps** | 部署代碼、修改服務器配置 | 強制 | 需要額外 YubiKey |

**執行**：分配到強制 MFA 角色但尚未啟用 MFA 的用戶在登入時將被強制進入 MFA 設置頁面。他們無法繞過此步驟。

### 5.2 可選 MFA 角色（低風險）

以下角色可以選擇性啟用 MFA。

| 角色 | 主要職責 | MFA 要求 | 建議 |
|------|---------------|-----------------|---------------|
| **Customer Service** | 查詢玩家數據、響應工單 | 可選 | 推薦 |
| **Marketing** | 查看統計數據、編輯活動頁面 | 可選 | 建議 |
| **Content Editor** | 編輯公告、新聞、幫助文檔 | 可選 | 可跳過 |

**建議**：具有可選 MFA 的用戶在登入時將看到不阻塞的橫幅，建議 MFA 設置。橫幅可關閉。

### 5.3 決策依據

MFA 策略基於三個維度的加權決策矩陣選擇：

**維度 1：風險暴露**
- Super Admin / Finance：5/5（可以修改系統配置/調整餘額）
- Customer Service：3/5（僅查詢權限）

**維度 2：操作可逆性**
- Super Admin / Finance：不可逆
- Customer Service：可逆（可審計操作）

**維度 3：監管合規**
- Super Admin / Finance：PCI DSS 要求
- Customer Service：GDPR 推薦

**評估選項**：

| 選項 | 安全性 | 用戶體驗 | 合規性 | 加權分數（40%+30%+30%） |
|--------|---------|----------------|-----------|------------------------------|
| A - 所有角色強制 MFA | 5 | 2 | 5 | **4.1** |
| **B - 差異化（高風險強制、低風險可選）** | **5** | **4** | **5** | **4.8** |
| C - 所有角色可選 MFA | 2 | 5 | 2 | **2.8** |

**結論**：選擇 Option B。這在為高價值目標提供安全保護的同時，不會干擾低風險角色的日常運營。

### 5.4 實施指南

**強制 MFA（5 個角色類別）**：
- Super Admin
- Finance Manager
- Risk Control
- Database Admin
- DevOps

**可選 MFA（3 個角色類別）**：
- Customer Service（推薦）
- Marketing（建議）
- Content Editor（可跳過）

---

## 6. 審計事件要求

### 6.1 合規要求

所有 MFA 相關操作必須記錄在審計日誌中，符合 GDPR Article 30 要求。

### 6.2 事件類型目錄

| 事件類型 | 嚴重級別 | 觸發條件 | 保留期限 |
|-----------|----------|---------|-----------------|
| `MFA_SETUP_INIT` | INFO | 用戶開始 MFA 設置 | 永久 |
| `MFA_ENABLED` | INFO | MFA 激活成功 | 永久 |
| `MFA_DISABLED` | CRITICAL | 用戶或管理員禁用 MFA | 永久 |
| `MFA_LOGIN_SUCCESS` | INFO | 登入時 MFA 驗證成功 | 90 天 |
| `MFA_LOGIN_FAILED` | WARNING | TOTP 驗證失敗 | 永久 |
| `MFA_LOCKED` | CRITICAL | 3 次連續失敗後帳號鎖定 | 永久 |
| `BACKUP_CODE_USED` | WARNING | 使用備份碼登入 | 永久 |
| `BACKUP_CODE_REGENERATE` | INFO | 備份碼重新生成 | 永久 |
| `MFA_FORCE_RESET` | CRITICAL | Security Team 強制重置用戶 MFA | 永久 |
| `DEVICE_TRUSTED` | INFO | 用戶信任設備 | 永久 |

### 6.3 審計日誌內容要求

每個審計條目必須捕獲：
- 用戶 ID
- 事件類型和嚴重級別
- IP 地址
- User agent 字符串
- 設備指紋
- 結構化格式的附加詳情（例如原因、嘗試次數、剩餘嘗試次數）
- 時間戳

→ **[Audit Log 實作](../../architecture/06_Platform_Core/MFA_Compliance_Technical.md#audit-log-implementation)** - JSONB 格式規範、Kafka 事件流整合、PostgreSQL 存儲架構、保留策略自動化

---

## 7. 異常檢測規則

### 7.1 規則 1：短時間內多次 MFA 失敗

- **條件**：同一用戶在 5 分鐘內 3 次或更多 MFA 失敗
- **操作**：
  - 鎖定帳號 15 分鐘
  - 向 Security Team 發送警報
  - 向用戶發送 Email 通知異常登入嘗試

### 7.2 規則 2：異常地理位置登入

- **條件**：同一用戶在 1 小時內從不同國家成功 MFA 登入
- **操作**：
  - 向 Security Team 發送警報進行人工審核
  - 禁用信任設備狀態，下次登入需要完整 MFA

### 7.3 規則 3：頻繁使用備份碼

- **條件**：同一用戶在 7 天內 3 次或更多使用備份碼
- **操作**：
  - 警告用戶：「您頻繁使用備份碼；請重新配置 TOTP」
  - 提示：「您的設備丟失了嗎？點擊此處恢復 MFA」

### 7.4 規則 4：高風險角色 MFA 禁用

- **條件**：任何強制 MFA 角色的用戶（Super Admin、Finance Manager、Risk Control）禁用 MFA
- **操作**：
  - **立即警報 CTO / CISO**
  - 人工審核：「為什麼高風險帳號禁用 MFA？」
  - 如果不是帳號持有人發起，視為安全事件並立即鎖定帳號

→ **[Anomaly Detection 實作](../../architecture/06_Platform_Core/MFA_Compliance_Technical.md#anomaly-detection-rules)** - Java 規則引擎實作、閾值配置、警報觸發邏輯、地理位置檢測算法

---

## 8. 實施路線圖

### 8.1 階段 1：核心 TOTP 功能（第 1-2 週，10 個工作日）

**目標**：實施 TOTP 認證（Google Authenticator），對高風險角色強制 MFA。

**任務摘要**：

| 任務 | 負責人 | 時長 | 依賴項 |
|------|-------|----------|-------------|
| 設計數據庫表結構 | Backend Dev | 0.5 天 | - |
| 實施 TOTP 生成和驗證邏輯 | Backend Dev | 1 天 | - |
| 實施 QR Code 生成 | Backend Dev | 0.5 天 | TOTP 邏輯 |
| 實施 MFA 設置流程 | Backend Dev | 2 天 | TOTP + QR |
| 實施 MFA 登入流程（兩階段認證） | Backend Dev | 2 天 | MFA 設置 |
| 實施審計日誌記錄 | Backend Dev | 1 天 | 設置 + 登入流程 |
| Frontend MFA 設置頁面 | Frontend Dev | 2 天 | MFA 設置 API |
| Frontend MFA 登入頁面 | Frontend Dev | 1.5 天 | MFA 登入 API |
| 單元測試 + 整合測試 | QA | 2 天 | 以上所有 |
| 部署到測試環境 + 驗證 | DevOps | 0.5 天 | 以上所有 |

**交付成果**：
- TOTP 認證（Google Authenticator）
- 高風險角色強制 MFA 檢查
- 審計日誌記錄（INFO / WARNING / CRITICAL）
- 單元測試覆蓋率 >= 80%

### 8.2 階段 2：備份與恢復機制（第 3 週，5 個工作日）

**目標**：實施備份碼、設備信任和設備丟失恢復流程。

**任務摘要**：

| 任務 | 負責人 | 時長 | 依賴項 |
|------|-------|----------|-------------|
| 實施備份碼生成和驗證 | Backend Dev | 1 天 | 階段 1 |
| 實施設備信任機制（基於指紋） | Backend Dev | 1.5 天 | 階段 1 |
| 實施設備丟失恢復流程（人工審核） | Backend Dev | 1.5 天 | 階段 1 |
| Frontend 備份碼管理頁面 | Frontend Dev | 1 天 | 備份碼 API |
| 測試與驗證 | QA | 1 天 | 以上所有 |

**交付成果**：
- 10 個備份碼（8 位數字，單次使用）
- 設備信任 30 天（跳過 MFA）
- 設備丟失恢復流程（Security Team 人工審核）

### 8.3 階段 3：高級安全功能（第 4 週，5 個工作日）

**目標**：實施 SMS OTP 備用、異常檢測和合規審計報告。

**任務摘要**：

| 任務 | 負責人 | 時長 | 依賴項 |
|------|-------|----------|-------------|
| 整合 SMS OTP 網關（Twilio / AWS SNS） | Backend Dev | 1 天 | 階段 1 |
| 實施異常檢測規則（4 條規則） | Backend Dev | 1.5 天 | 階段 1 + 2 |
| 實施合規審計報告生成（GDPR / PCI DSS） | Backend Dev | 1 天 | 階段 1 + 2 |
| Frontend SMS OTP 登入流程 | Frontend Dev | 0.5 天 | SMS OTP API |
| 滲透測試 | Security Team | 2 天 | 以上所有 |

**交付成果**：
- SMS OTP 備用方法
- 異常檢測和自動警報
- 合規審計報告（可導出 PDF）
- 通過滲透測試（無 High / Critical 漏洞）

---

## 9. 常見問題

### Q1：用戶報告「TOTP 代碼總是不正確」

**根本原因**：99% 的情況是時間同步問題。

**解決方案**：
1. 驗證服務器 NTP 狀態
2. 讓用戶檢查設備時間設置（啟用自動時間）
3. 臨時擴大驗證窗口從 +/-1 到 +/-2（允許 +/-60 秒偏移）

### Q2：用戶丟失手機且沒有備份碼

**解決方案**：遵循設備丟失恢復流程（第 3 節）：
1. 提交恢復請求並附上身份證件
2. 驗證 Email + Phone + 安全問題
3. Security Team 人工審核
4. 強制重置 MFA（生成新 Secret）

### Q3：為什麼不使用 SMS OTP 作為主要方法？

**安全問題**：
- 易受 SIM Swap 攻擊
- 易受 SS7 劫持（電信協議漏洞）
- NIST 在 2016 年棄用 SMS OTP 作為主要因素
- 本系統中 SMS OTP 僅用作備用方法

### Q4：如果 TOTP 代碼在用戶輸入時過期怎麼辦？

**緩解措施**：系統允許 +/-1 窗口（90 秒有效期）：
- 前一窗口代碼（-30s）：有效
- 當前窗口代碼：有效
- 下一窗口代碼（+30s）：有效

### Q5：是否需要硬件令牌（YubiKey）支持？

**階段 3 可選功能**（僅 Super Admin）：
- 最高安全性（防釣魚、防中間人攻擊）
- 高成本（$50/設備）
- 物流複雜（遠程員工需郵寄）
- 建議：僅為 Super Admin、CTO、CFO 購買

---

## 10. 測試場景

以下測試場景必須通過驗證以進行合規驗收：

| 測試 ID | 場景 | 預期結果 |
|---------|---------|----------------|
| TC-MFA-001 | 用戶首次 TOTP 設置（掃描 QR Code） | Secret 安全存儲，狀態 = PENDING |
| TC-MFA-002 | 用戶輸入正確 TOTP 代碼激活 MFA | 狀態更改為 ACTIVE，記錄審計日誌 |
| TC-MFA-003 | 用戶輸入不正確 TOTP 代碼 | 訪問被拒絕，錯誤計數器遞增 |
| TC-MFA-004 | 用戶連續 3 次 TOTP 失敗 | 帳號鎖定 15 分鐘，觸發安全警報 |
| TC-MFA-005 | 用戶使用備份碼登入 | 登入成功，備份碼標記為已使用 |
| TC-MFA-006 | 用戶重複使用已消耗的備份碼 | 訪問被拒絕，記錄審計日誌 |
| TC-MFA-007 | 用戶選擇「信任此設備 30 天」 | 信任 Token 存儲（TTL 30 天） |
| TC-MFA-008 | 從信任設備登入（跳過 MFA） | 直接發放 Token，不提示 TOTP |
| TC-MFA-009 | 服務器時間偏移 +25 秒 | 驗證成功（+/-1 窗口） |
| TC-MFA-010 | 服務器時間偏移 +65 秒 | 驗證失敗（超過 +/-1 窗口） |
| TC-MFA-011 | Security Team 強制重置用戶 MFA | 刪除舊 Secret，生成新 Secret |
| TC-MFA-012 | 用戶在 1 小時內從不同國家登入 | 觸發異常檢測，發送警報 |

---

## 11. 監管合規映射

| 法規 | 要求 | 覆蓋 |
|-----------|------------|---------|
| **PCI DSS** | 管理帳號必須使用 MFA | 由高風險角色強制 MFA 覆蓋（第 5.1 節） |
| **GDPR Art. 30** | 所有處理活動必須記錄 | 由審計日誌要求覆蓋（第 6 節） |
| **MGA** | 管理訪問適當安全措施 | 由差異化 MFA 策略覆蓋（第 5 節） |
| **NIST SP 800-63B** | 不推薦 SMS OTP 作為主要因素 | 已覆蓋 - TOTP 作為主要，SMS 僅作為備用（第 9 節，Q3） |

---

## 12. 相關文檔

- MFA Architecture Design - 業務需求和方法選擇
- TOTP & WebAuthn Implementation - TOTP 算法詳情
- Login & Recovery Flow - 認證流程設計

### 技術實作

→ **[MFA Compliance Validation](../../architecture/06_Platform_Core/MFA_Compliance_Validation.md)** - Backup Code 存儲加密、身份證件上傳工作流、審計日誌實作（JSONB + Kafka）、異常檢測算法（失敗嘗試、地理位置、備份碼濫用）
