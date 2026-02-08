# MFA 架構規格 - 業務需求與方法選擇

> **Canonical Source**: [06-06-01_MFA_Architecture.md](../../source/06_Platform_Governance/06-06-01_MFA_Architecture.md)
> **Audience**: Executives, Compliance Officers
> **Related Doc**: [MFA_Technical_Architecture.md](../../architecture/06_Platform_Core/MFA_Technical_Architecture.md)
> **Last Synced**: 2026-02-08

---

## 1. 業務需求

### 1.1 為什麼後台用戶需要 MFA

後台用戶（管理員）擁有高權限操作能力，一旦帳號被盜用，可能造成嚴重損失。

**高風險操作清單**：

| 角色 | 高風險操作 | 潛在損失 |
|------|----------|---------|
| **Super Admin** | 修改系統配置、刪除用戶、修改權限 | 系統癱瘓、數據洩露 |
| **Finance Manager** | 調整玩家餘額、批准提現、修改交易記錄 | 直接金錢損失（$10K-$1M+） |
| **Risk Control** | 修改風控規則、白名單黑名單 | 欺詐損失、合規風險 |
| **Customer Service** | 查看玩家隱私資料、修改玩家資訊 | 隱私洩露、GDPR 罰款 |

**行業案例**：

| 年份 | 事件摘要 | 損失 |
|------|---------|------|
| 2023 | 某平台 Finance Manager 帳號被釣魚攻擊盜用，攻擊者批准 47 筆虛假提現申請 | $237,000 USD |
| 2024 | 某平台 Super Admin 帳號通過密碼撞庫攻擊，攻擊者獲取完整數據庫訪問權限 | 120,000 玩家個資洩露 |

**結論**：僅靠密碼認證不足以保護高權限帳號，需要雙因素認證（MFA）作為第二道防線。

---

### 1.2 風險分析

**威脅建模**（STRIDE 框架）：

| 威脅類型 | 攻擊場景 | 僅密碼防護 | 密碼 + MFA |
|---------|---------|----------|-----------|
| **Spoofing（身份欺騙）** | 釣魚網站竊取密碼 | 直接失效 | 攻擊者無法獲取 TOTP |
| **Tampering（數據篡改）** | Session Hijacking | 攻擊者可劫持會話 | MFA 綁定設備指紋 |
| **Repudiation（否認）** | 內部人員惡意操作後否認 | 難以證明 | MFA 審計日誌 |
| **Information Disclosure** | 密碼洩露（數據庫被拖庫） | 密碼 Hash 可暴力破解 | TOTP Secret 單獨加密存儲 |
| **Denial of Service** | 暴力破解登入 | 可通過 IP 限流緩解 | MFA 增加破解難度 |
| **Elevation of Privilege** | 橫向移動攻擊（從低權限帳號升級） | 密碼可能被猜測 | 高權限角色強制 MFA |

**風險評分**（CVSS 3.1）：

| 配置 | Base Score | 等級 | 關鍵因素 |
|------|-----------|------|---------|
| 無 MFA 的後台登入系統 | 8.1 | High | Attack Complexity = Low |
| 有 MFA 的後台登入系統 | 4.3 | Medium | Attack Complexity = High（MFA 增加攻擊難度） |

**結論**：MFA 可將安全風險從 High（8.1）降低至 Medium（4.3），降低約 47% 風險。

---

### 1.3 合規要求

iGaming 行業需遵守以下監管要求，大部分明確要求或強烈建議 MFA：

| 監管機構 | 合規標準 | MFA 要求 | 罰款/後果 |
|---------|---------|---------|---------|
| **MGA（Malta）** | MGA/B2C/183/2010 | 強制要求高權限帳號使用 MFA | 吊銷牌照、EUR 50K-500K 罰款 |
| **UKGC（UK）** | LCCP 10.1.1 | 推薦 MFA（Risk-based Authentication） | 牌照暫停、GBP 100K-2M 罰款 |
| **Curacao eGaming** | Gaming Control Board | 未明確要求但審計時檢查 | 審計失敗、牌照續期問題 |
| **GDPR（EU）** | Art. 32 | 要求「適當的技術措施」保護個人數據 | EUR 20M 或全球營收 4% |
| **PCI DSS 4.0** | Requirement 8.3.1 | 強制要求所有管理員使用 MFA | 無法處理支付卡交易 |

**合規檢查清單**（以 MGA 為例）：

| 序號 | 要求 |
|------|------|
| 1 | 所有 Super Admin、Finance Manager、Risk Control 必須啟用 MFA |
| 2 | MFA Secret 必須加密存儲（AES-256-GCM） |
| 3 | 審計日誌必須記錄所有 MFA 事件（註冊、驗證、失敗） |
| 4 | 備份恢復機制必須有二次驗證（不能自助恢復） |
| 5 | MFA 實施後需通過滲透測試（Penetration Test） |

---

## 2. MFA 方法選擇

### 2.1 方法對比

**四種常見 MFA 方法**：

| 方法 | 原理 | 安全性 | 用戶體驗 | 實施成本 | 依賴性 |
|------|------|-------|---------|---------|-------|
| **TOTP（Time-based OTP）** | 基於時間的一次性密碼（Google Authenticator） | 5/5 | 4/5 | 5/5 | 無（離線可用） |
| **SMS OTP** | 短信發送驗證碼 | 3/5 | 5/5 | 3/5 | 依賴 SMS 網關 |
| **Email OTP** | 郵件發送驗證碼 | 3/5 | 3/5 | 4/5 | 依賴郵件服務 |
| **Hardware Token（YubiKey）** | 硬件設備生成密鑰 | 5/5 | 2/5 | 2/5 | 需購買硬件 |

#### TOTP（Google Authenticator / Authy）

| 維度 | 說明 |
|------|------|
| **優點** | 高安全性（RFC 6238 標準）；離線可用（不依賴網絡）；成本低（免費應用）；廣泛支持 |
| **缺點** | 設備丟失風險（需備份恢復機制）；時間同步問題 |
| **適用場景** | Super Admin、Finance Manager（高權限角色）；開發者、DevOps |

#### SMS OTP

| 維度 | 說明 |
|------|------|
| **優點** | 用戶體驗好（無需安裝應用）；覆蓋率高（99% 用戶有手機號） |
| **缺點** | 安全性較低（SIM Swap 攻擊、SS7 劫持）；依賴 SMS 網關（成本 $0.05-$0.10/條）；NIST SP 800-63B 已不推薦 |
| **適用場景** | 作為 TOTP 的備用方案；低敏感度操作 |

**NIST 官方立場**：SMS OTP 已被棄用（deprecated），未來版本將禁止使用。

#### Email OTP

| 維度 | 說明 |
|------|------|
| **優點** | 實施簡單；成本幾乎為零 |
| **缺點** | 安全性最低（郵箱被盜即 MFA 失效）；延遲問題（可能 5-10 分鐘） |
| **適用場景** | 最後的備用方案；僅用於帳號恢復流程 |

#### Hardware Token（YubiKey）

| 維度 | 說明 |
|------|------|
| **優點** | 最高安全性（物理設備防釣魚、防中間人攻擊）；符合 FIDO2 標準 |
| **缺點** | 成本高（每個設備 $50-$70 USD）；物流問題（需郵寄給遠程員工）；丟失風險 |
| **適用場景** | 超高權限角色（Super Admin、CTO、CFO）；有預算的大型企業 |

---

### 2.2 決策分析：為什麼選擇 TOTP 作為主要方法

**決策框架**（3 個維度：安全性 40%、用戶體驗 30%、實施成本 30%）

**方案選項**：

#### Option A：僅使用 TOTP

| 維度 | 評估 |
|------|------|
| **優點** | 安全性 5/5（符合 PCI DSS、GDPR）；零依賴（離線可用）；成本最低 |
| **缺點** | 設備丟失時無法登入；用戶學習成本 |
| **適用場景** | 中小型 iGaming 平台（50-200 名員工）；預算有限但需合規 |

#### Option B：TOTP（主要）+ SMS OTP（備用） -- 推薦

| 維度 | 評估 |
|------|------|
| **優點** | 結合 TOTP 安全性 + SMS 便利性；設備丟失時有備用方案；符合「多層防禦」原則 |
| **缺點** | SMS 成本（約 $300/月，以 200 員工計）；SMS 安全性較低（但作為備用可接受） |
| **適用場景** | 大中型 iGaming 平台（200+ 員工）；有一定預算且重視用戶體驗 |

#### Option C：Hardware Token（YubiKey）

| 維度 | 評估 |
|------|------|
| **優點** | 最高安全性（物理設備防釣魚）；符合金融級安全標準（PSD2、FIDO2） |
| **缺點** | 成本極高（$10,000 以 200 員工計）；物流複雜；需每人 2 個設備 |
| **適用場景** | 超高安全要求（銀行、支付平台）；僅用於 Super Admin、Finance Manager（5-10 人） |

**決策矩陣**：

| 方案 | 安全性評分 | 用戶體驗評分 | 實施成本評分 | 加權總分（40%/30%/30%） |
|------|----------|-----------|-----------|----------------------|
| Option A（僅 TOTP） | 5 | 4 | 5 | **4.7** |
| Option B（TOTP + SMS） | 5 | 5 | 4 | **4.8** |
| Option C（YubiKey） | 5 | 3 | 2 | **3.5** |

**決策結論**：選擇 Option B（TOTP 主要 + SMS 備用）作為 SmartAdmin iGaming 平台的 MFA 方案。

---

### 2.3 最終選擇

**SmartAdmin iGaming MFA 三層架構**：

| 優先級 | 方法 | 使用場景 | 安全級別 |
|-------|------|---------|---------|
| **P0** | TOTP | 日常登入（推薦 90% 用戶使用） | 5/5 |
| **P1** | SMS OTP | 設備丟失 / TOTP 不可用 | 3/5 |
| **P2** | Email OTP | SMS 不可用（國際漫遊） | 3/5 |
| **P3** | Backup Codes | 所有方法都不可用（離線恢復） | 4/5 |

**驗證失敗政策**：

| 規則 | 內容 |
|------|------|
| 失敗計數 | 每次驗證失敗，錯誤計數 + 1 |
| 帳號鎖定 | 連續 3 次失敗，帳號暫時鎖定 15 分鐘 |
| 安全警報 | 鎖定觸發時，發送安全警報通知 |

**實施策略**：

| 階段 | 時間範圍 | 內容 |
|------|---------|------|
| Phase 1 | Week 1-2 | 實施 TOTP（Google Authenticator） |
| Phase 2 | Week 3 | 實施 SMS OTP 備用方案 |
| Phase 3 | Week 4 | 實施 Backup Codes（10 個一次性恢復碼） |

---

## 相關文檔

- [MFA_Technical_Architecture.md](../../architecture/06_Platform_Core/MFA_Technical_Architecture.md) - MFA 技術架構設計
- [TOTP_WebAuthn_Implementation.md](../../architecture/06_Platform_Core/TOTP_WebAuthn_Implementation.md) - TOTP 與 WebAuthn 技術實作
- [MFA_Compliance_Requirements.md](./MFA_Compliance_Requirements.md) - 合規與審計要求
- [MFA_Recovery_Requirements.md](./MFA_Recovery_Requirements.md) - 登入與恢復流程需求

---

**End of Document**
