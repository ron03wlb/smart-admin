# MFA 需求 (Multi-Factor Authentication，多因素認證)

> **規範來源**: [06-06 MFA Implementation](../../source-archive/06_Platform_Governance/06-06_MFA_Implementation.md)
> **文件類型**: 業務需求
> **目標讀者**: 產品經理、合規官員
> **相關架構**: [MFA Technical Architecture](../../architecture/06_Platform_Core/MFA_Technical.md)
> **最後同步**: 2026-02-09

**Related Source Documents**:
- [06-06-01 MFA Architecture Design](../../source-archive/06_Platform_Governance/06-06-01_MFA_Architecture.md)
- [06-06-04 Compliance & Audit](../../source-archive/06_Platform_Governance/06-06-04_Compliance_Audit.md)

---

## Business Value

SmartAdmin 後台的 Multi-Factor Authentication (MFA) 為業務提供關鍵價值：
- **風險降低 (Risk Reduction)**：透過要求密碼以外的第二因素驗證，將帳號接管風險降低 47%（CVSS 分數從 8.1 High 降至 4.3 Medium）
- **財務損失預防 (Financial Loss Prevention)**：防止釣魚攻擊（如 2023 年 Finance Manager 事件，47 筆虛假提現導致 $237,000 USD 損失）和撞庫攻擊（2024 年 Super Admin 帳號洩露，暴露 120,000 名玩家記錄）
- **監管合規 (Regulatory Compliance)**：滿足 MGA 強制要求（避免 EUR 50K-500K 罰款和牌照吊銷）、PCI DSS 4.0 Requirement 8.3.1（管理員強制要求）、GDPR Art. 32（避免 EUR 20M 或全球營收 4% 罰款）、以及 UKGC LCCP 10.1.1（避免 GBP 100K-2M 罰款）
- **優化用戶體驗 (Optimized User Experience)**：差異化政策（Option B）達到 4.8/5.0 加權分數，透過對高風險角色（Super Admin、Finance、Risk Control、Database Admin、DevOps）強制執行 MFA，同時保持低風險角色（Customer Service、Marketing、Content Editor）為可選，平衡安全性（5/5）與用戶體驗（4/5）

---

## Acceptance Criteria

- [ ] 所有 5 個強制 MFA 角色類別無豁免地執行 MFA：Super Admin、Finance Manager、Risk Control、Database Admin、DevOps（高權限帳號無繞過）
- [ ] TOTP (Google Authenticator) 為主要 P0 方法，預期 90% 採用率；SMS OTP 為 P1 備用、Email OTP 為 P2 後備、Backup Codes 為 P3 緊急恢復
- [ ] 風險分數降低驗證：無 MFA 基準 CVSS 8.1 (High) 降至啟用 MFA 後的 4.3 (Medium)（47% 風險降低）
- [ ] 備份碼功能：10 個一次性使用碼、8 位數字格式 (XXXX-XXXX)、加密存儲、重新生成需要 TOTP 驗證、剩餘 ≤2 個時低碼警告
- [ ] 設備丟失恢復 SLA：多因素驗證（身份證件、Email OTP、SMS OTP、安全問題、人工審核）在 24-48 工作小時內完成
- [ ] 信任設備功能：30 天信任期、HttpOnly secure cookie、綁定 IP + User-Agent + Fingerprint、密碼變更時自動撤銷
- [ ] 失敗處理政策：連續 3 次失敗觸發 15 分鐘帳號鎖定並發送安全警報
- [ ] 審計日誌：記錄 10 種事件類型（MFA_SETUP_INIT、MFA_ENABLED、MFA_DISABLED、MFA_LOGIN_SUCCESS、MFA_LOGIN_FAILED、MFA_LOCKED、BACKUP_CODE_USED、BACKUP_CODE_REGENERATE、MFA_FORCE_RESET、DEVICE_TRUSTED），CRITICAL/WARNING 永久保留、INFO 保留 90 天
- [ ] 異常檢測：4 條規則啟動（多次失敗、地理異常、頻繁使用備份碼、高風險 MFA 禁用），高風險角色 MFA 禁用時立即警報 CTO/CISO
- [ ] 監管合規：通過所有 MGA 合規檢查清單項目（Secret 加密、審計日誌、滲透測試、無自助恢復）
- [ ] 實施路線圖：Phase 1（Week 1-2 TOTP 核心）、Phase 2（Week 3 備份與恢復）、Phase 3（Week 4 SMS OTP + 異常檢測）按計劃完成

---

## 1. 執行摘要

本文檔概述 SmartAdmin iGaming 平台後台管理系統的 **Multi-Factor Authentication (MFA，多因素認證)** 需求。MFA 對於保護高權限帳號免受未授權訪問、防止金融欺詐以及滿足監管合規要求至關重要。

**關鍵統計數據**：
- MFA 將帳號接管風險降低 **47%**（CVSS 8.1 → 4.3）
- 防止釣魚攻擊（iGaming 行業主要攻擊向量）
- 監管合規所需（MGA、PCI DSS、GDPR）

---

## 2. 業務需求

### 2.1 為什麼後台用戶需要 MFA

後台管理員擁有高權限訪問權限，一旦被入侵，可能導致嚴重損失：

| 角色 | 高風險操作 | 潛在損失 |
|------|---------------------|----------------|
| **Super Admin** | 修改系統配置、刪除用戶、變更權限 | 系統癱瘓、數據洩露 |
| **Finance Manager** | 調整玩家餘額、批准提現、修改交易 | 直接金錢損失（$10K-$1M+） |
| **Risk Control** | 修改風控規則、白名單/黑名單管理 | 欺詐損失、合規違規 |
| **Customer Service** | 查看玩家 PII、修改玩家資訊 | 隱私洩露、GDPR 罰款 |

**行業真實案例 (iGaming Industry)**：
- **案例 A (2023)**：Finance Manager 帳號被釣魚，攻擊者批准 47 筆虛假提現，**$237,000 USD** 損失
- **案例 B (2024)**：Super Admin 帳號透過撞庫攻擊被入侵，導致 **120,000 名玩家記錄暴露**

### 2.2 風險分析 (STRIDE Framework，STRIDE 框架)

| 威脅類型 | 攻擊場景 | 僅密碼 | 密碼 + MFA |
|------------|-----------------|---------------|----------------|
| **Spoofing（身份欺騙）** | 釣魚網站竊取密碼 | 被入侵 | 攻擊者無法獲取 TOTP |
| **Tampering（數據篡改）** | Session Hijacking（會話劫持） | 會話可被劫持 | MFA 綁定設備指紋 |
| **Repudiation（否認）** | 內部人員惡意操作後否認 | 難以證明 | MFA 審計追蹤 |
| **Information Disclosure（資訊洩露）** | 密碼洩露（數據庫洩露） | Hash 可被暴力破解 | TOTP Secret 單獨加密 |
| **Denial of Service（拒絕服務）** | 暴力破解登入 | 可透過 IP 限流緩解 | MFA 增加攻擊難度 |
| **Elevation of Privilege（權限提升）** | 橫向移動攻擊 | 密碼可能被猜測 | 高權限角色要求 MFA |

**風險評分 (CVSS 3.1)**：
- 無 MFA：**8.1 (High)**
- 有 MFA：**4.3 (Medium)**
- 風險降低：**47%**

---

## 3. 支援的認證方法

### 3.1 方法比較

| 方法 | 原理 | 安全性 | 用戶體驗 | 成本 | 依賴性 |
|--------|-----------|----------|-----|------|------------|
| **TOTP** | 基於時間的一次性密碼 (Google Authenticator) | 5/5 | 4/5 | 5/5 | 無（離線） |
| **SMS OTP** | SMS 驗證碼 | 3/5 | 5/5 | 3/5 | SMS 網關 |
| **Email OTP** | Email 驗證碼 | 3/5 | 3/5 | 4/5 | Email 服務 |
| **Hardware Token (YubiKey)** | 物理設備金鑰生成 | 5/5 | 2/5 | 2/5 | 需購買硬件 |

### 3.2 推薦的三層架構

| 優先級 | 方法 | 使用場景 | 安全級別 |
|----------|--------|----------|----------------|
| **P0** | TOTP | 日常登入（90% 用戶） | 5/5 |
| **P1** | SMS OTP | 設備丟失 / TOTP 不可用 | 3/5 |
| **P2** | Email OTP | SMS 不可用（國際漫遊） | 3/5 |
| **P3** | Backup Codes | 所有方法不可用（離線恢復） | 4/5 |

### 3.3 方法選擇理由

**為什麼選擇 TOTP 作為主要方法**：
- **最高安全性**：RFC 6238 標準、HMAC-SHA1 演算法
- **零依賴性**：離線工作，無需第三方服務
- **最低成本**：免費應用程式（Google Authenticator、Authy、1Password）
- **廣泛支援**：後台系統的行業標準

**為什麼 SMS OTP 僅作為備用**：
- 安全性疑慮：易受 SIM Swap 攻擊、SS7 劫持
- NIST SP 800-63B 於 2016 年棄用 SMS OTP
- 成本：每則訊息 $0.05-$0.10
- 僅在 TOTP 不可用時使用

---

## 4. 按司法管轄區的 MFA 要求

### 4.1 監管合規矩陣

| 監管機構 | 標準 | MFA 要求 | 罰款 |
|-----------|----------|-----------------|---------|
| **MGA (Malta)** | MGA/B2C/183/2010 | 高權限帳號**強制要求** | 牌照吊銷、EUR 50K-500K 罰款 |
| **UKGC (UK)** | LCCP 10.1.1 | **推薦**（基於風險的認證） | 牌照暫停、GBP 100K-2M 罰款 |
| **Curacao eGaming** | Gaming Control Board | 未明確要求但審計時檢查 | 審計失敗、牌照續期問題 |
| **GDPR (EU)** | Art. 32 | 要求「適當的技術措施」 | EUR 20M 或全球營收 4% |
| **PCI DSS 4.0** | Requirement 8.3.1 | 所有管理員**強制要求** | 無法處理卡交易 |

### 4.2 MGA 合規檢查清單

1. 所有 Super Admin、Finance Manager、Risk Control 必須啟用 MFA
2. MFA Secret 必須使用 NIST 批准的 256 位元加密標準加密

→ **[MFA Secret Encryption](../../architecture/06_Platform_Core/MFA_Technical.md#secret-encryption)** — 查看 architecture 層了解批准的演算法詳情
3. 審計日誌必須記錄所有 MFA 事件（設置、驗證、失敗）
4. 恢復機制必須要求二次驗證（無自助恢復）
5. MFA 實施必須通過滲透測試

---

## 5. 基於角色的 MFA 政策

### 5.1 強制 MFA 角色（5 個類別）

| 角色 | 高風險操作 | MFA 要求 | 備註 |
|------|---------------------|-----------------|-------|
| **Super Admin** | 修改系統配置、刪除用戶、變更權限 | **強制** | 無豁免 |
| **Finance Manager** | 調整玩家餘額、批准提現 | **強制** | 無豁免 |
| **Risk Control** | 修改風控規則、白名單/黑名單 | **強制** | 無豁免 |
| **Database Admin** | 直接生產數據庫訪問 | **強制** | 額外推薦 YubiKey |
| **DevOps** | 部署代碼、修改伺服器配置 | **強制** | 額外推薦 YubiKey |

### 5.2 可選 MFA 角色（3 個類別）

| 角色 | 主要職責 | MFA 要求 | 建議 |
|------|-------------------------|-----------------|----------------|
| **Customer Service** | 查詢玩家數據、回應工單 | 可選 | **推薦** |
| **Marketing** | 查看統計、編輯活動頁面 | 可選 | **建議** |
| **Content Editor** | 編輯公告、新聞、幫助文檔 | 可選 | 可跳過 |

### 5.3 政策決策理由

**為什麼採用差異化政策（強制 vs 可選）**：

| 標準 | Super Admin | Finance | Customer Service |
|----------|-------------|---------|------------------|
| 風險暴露 | 5/5（系統配置） | 5/5（玩家餘額） | 3/5（僅查看） |
| 操作可逆性 | 否 | 否 | 是（審計追蹤） |
| 合規要求 | PCI DSS 要求 | PCI DSS 要求 | GDPR 推薦 |

**決策矩陣**：

| 選項 | 安全性 | 用戶體驗 | 合規 | 評分 (40%+30%+30%) |
|--------|----------|-----|------------|---------------------|
| A (全部強制) | 5 | 2 | 5 | 4.1 |
| **B (差異化)** | 5 | 4 | 5 | **4.8** |
| C (全部可選) | 2 | 5 | 2 | 2.8 |

**結論**：選擇 Option B（高風險強制 + 低風險可選）。

---

## 6. 恢復程序

### 6.1 備份碼 (Backup Codes)

**目的**：允許在 TOTP 設備不可用時登入（手機丟失、應用程式刪除、設備損壞）。

**規格**：
- **數量**：10 個一次性使用碼
- **格式**：8 位數字（例如 1234-5678）
- **生成**：SecureRandom，加密安全
- **存儲**：NIST 批准的 256 位元加密，與用戶 MFA 記錄一起存儲

→ **[Encryption Implementation Details](../../architecture/06_Platform_Core/MFA_Technical.md#secret-encryption)** — 查看 architecture 層了解批准的演算法詳情
- **使用**：每個碼僅能使用一次

**用戶指南**：
1. MFA 設置後立即下載備份碼
2. 存儲在安全位置（密碼管理器、列印放保險箱）
3. 當剩餘碼 <= 2 時，重新生成完整套碼
4. 重新生成需要當前 TOTP 驗證

### 6.2 設備丟失恢復流程

**場景**：用戶丟失手機、無法使用 TOTP、沒有備份碼。

**恢復流程（多因素驗證）**：

1. **提交恢復請求**
   - 上傳身份證件（護照/駕照/身份證）
   - 提供請求原因

2. **身份驗證**
   - Email OTP 驗證（發送至註冊 Email）
   - SMS OTP 驗證（發送至註冊電話）
   - 安全問題驗證（3 個預設問題）

3. **人工審核**
   - Security Team 審核請求
   - 根據用戶記錄驗證身份證件
   - 24 小時內批准或拒絕

4. **MFA 重置**
   - Security Team 強制重置 MFA
   - 生成新的 TOTP Secret
   - 透過安全 Email 發送給用戶
   - 用戶掃描新的 QR Code 重新啟動

**SLA**：24-48 工作小時內完成恢復。

### 6.3 緊急聯繫人驗證

**增強安全性選項**：用戶可在 MFA 設置期間指定 1-2 個緊急聯繫人（同事/經理）。

**流程**：
1. 用戶提交恢復請求
2. 系統發送驗證 Email 給緊急聯繫人
3. 緊急聯繫人點擊驗證連結（確認合法請求）
4. Security Team 收到通知，加速審核

---

## 7. 用戶體驗需求

### 7.1 信任設備功能

**目的**：減少從同一設備頻繁登入的 MFA 摩擦。

**規格**：
- **信任期限**：30 天
- **機制**：設備指紋 + HttpOnly secure cookie
- **選擇加入**：用戶在 MFA 驗證期間選擇「信任此設備」
- **撤銷**：用戶可從設置中撤銷信任設備

**安全措施**：
- HttpOnly cookie（無法被 JavaScript 讀取）
- SameSite=Strict（防止 CSRF）
- 綁定 IP + User-Agent + Fingerprint 三元組
- 密碼變更時自動撤銷

### 7.2 失敗處理

| 場景 | 行為 | 用戶訊息 |
|----------|----------|--------------|
| TOTP 碼錯誤 | 失敗計數遞增 | "驗證碼錯誤。剩餘 X 次嘗試機會。" |
| 連續 3 次失敗 | 鎖定帳號 15 分鐘 | "嘗試次數過多。帳號已鎖定 15 分鐘。" |
| MFA 會話過期 | 重定向至密碼登入 | "會話已過期。請重新登入。" |
| 伺服器時間同步問題 | 內部錯誤 | "系統錯誤。請 30 秒後再試。" |

### 7.3 首次設置流程

**觸發條件**：
- 強制 MFA 角色首次登入
- 用戶從個人設置啟用 MFA
- 管理員強制啟用 MFA（安全政策）

**設置步驟**：
1. 顯示 Google Authenticator 的 QR Code
2. 顯示明文 Secret 供手動輸入
3. 生成並顯示 10 個備份碼
4. 要求用戶輸入當前 TOTP 以驗證設置
5. 成功驗證後啟動 MFA

---

## 8. 審計要求

### 8.1 要記錄的事件類型

| 事件類型 | 等級 | 觸發條件 | 保留期限 |
|------------|-------|---------|-----------|
| `MFA_SETUP_INIT` | INFO | 用戶開始 MFA 設置 | 永久 |
| `MFA_ENABLED` | INFO | MFA 成功啟動 | 永久 |
| `MFA_DISABLED` | CRITICAL | 用戶/管理員禁用 MFA | 永久 |
| `MFA_LOGIN_SUCCESS` | INFO | MFA 驗證成功 | 90 天 |
| `MFA_LOGIN_FAILED` | WARNING | TOTP 驗證失敗 | 永久 |
| `MFA_LOCKED` | CRITICAL | 3 次失敗後帳號鎖定 | 永久 |
| `BACKUP_CODE_USED` | WARNING | 使用備份碼登入 | 永久 |
| `BACKUP_CODE_REGENERATE` | INFO | 重新生成備份碼 | 永久 |
| `MFA_FORCE_RESET` | CRITICAL | Security Team 強制重置 MFA | 永久 |
| `DEVICE_TRUSTED` | INFO | 用戶信任設備 | 永久 |

### 8.2 審計日誌欄位

每個審計日誌條目必須包含：
- 用戶 ID
- 事件類型和等級
- IP 地址
- User agent
- 設備指紋
- 時間戳（UTC）
- 額外詳情（JSON 格式）

### 8.3 異常檢測規則

| 規則 | 檢測邏輯 | 動作 |
|------|-----------------|--------|
| 多次 MFA 失敗 | 5 分鐘內 >= 3 次失敗 | 鎖定 15 分鐘，警報 Security Team |
| 地理異常 | 1 小時內從不同國家登入 | 警報 Security Team，要求重新驗證 |
| 頻繁使用備份碼 | 7 天內 >= 3 次使用備份碼 | 警告用戶，建議 TOTP 重置 |
| 高風險 MFA 禁用 | Super Admin/Finance/Risk 的 MFA 被禁用 | **立即警報 CTO/CISO** |

---

## 9. 合規報告要求

### 9.1 必需報告

| 報告 | 頻率 | 受眾 | 內容 |
|--------|-----------|----------|---------|
| MFA 採用率報告 | 每月 | Security Team | 按角色的採用率、待設置 |
| MFA 事件摘要 | 每週 | Security Team | 登入成功/失敗比率、異常 |
| 高風險操作審計 | 按需 | Compliance Team | 所有 MFA_DISABLED 和 MFA_FORCE_RESET 事件 |
| 滲透測試結果 | 每年 | 監管機構 | MFA 繞過嘗試結果 |

### 9.2 監管提交

對於 MGA/UKGC 審計，必須證明以下內容：
1. 所有強制 MFA 角色已啟用 MFA
2. MFA Secret 在靜態時加密
3. 按政策保留審計日誌
4. 恢復程序需要人工驗證
5. 滲透測試通過，無 High/Critical 發現

---

## 10. 實施路線圖

| 階段 | 時間線 | 目標 | 交付成果 |
|-------|----------|-------|--------------|
| **Phase 1** | Week 1-2 | 核心 TOTP 功能 | TOTP 登入、強制角色檢查、審計日誌 |
| **Phase 2** | Week 3 | 備份與恢復 | 備份碼、設備信任、恢復流程 |
| **Phase 3** | Week 4 | 進階安全 | SMS OTP 備用、異常檢測、合規報告 |

---

## 11. 常見問題 (FAQ, Frequently Asked Questions)

**Q1: 用戶報告「TOTP 碼總是錯誤」？**
- 99% 由時間同步問題引起
- 檢查伺服器 NTP 狀態
- 讓用戶驗證設備自動時間設置
- 暫時增加驗證窗口（+-2 而非 +-1）

**Q2: 用戶丟失手機且沒有備份碼？**
- 執行設備丟失恢復流程（第 6.2 節）
- 需要身份驗證 + Security Team 批准
- SLA：24-48 工作小時

**Q3: 為什麼不使用 SMS OTP 作為主要方法？**
- 易受 SIM Swap 攻擊
- 易受 SS7 協議劫持
- NIST 於 2016 年棄用
- 僅在 TOTP 不可用時作為備用

**Q4: TOTP 碼在輸入期間過期（30 秒窗口）？**
- 系統允許 +-1 窗口（90 秒有效期）
- 前一個窗口碼有效
- 下一個窗口碼有效
- 無需用戶操作

**Q5: 我們是否應該支援硬件令牌 (YubiKey)？**
- Phase 3 可選功能
- 僅推薦給 Super Admin、CTO、CFO
- 成本：每個設備 $50
- 提供最高安全性（防釣魚、防中間人攻擊）

---

## 相關文檔

→ **[TOTP & WebAuthn Implementation](../../architecture/06_Platform_Core/TOTP_WebAuthn_Implementation.md)** - TOTP 演算法實作 (RFC 6238)、Secret 生成、QR Code 渲染、備份碼加密 (AES-256-GCM)、信任設備指紋、審計日誌 Schema

---

**End of Document**
