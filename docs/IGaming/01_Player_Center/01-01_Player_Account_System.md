# 01-01 玩家帳戶系統設計 (Player Account System)

## 1. 系統概述 (System Overview)
玩家帳戶管理系統 (PAM) 是整個 iGaming 平台的基石，負責處理玩家從註冊、登入、身份驗證到個人資料管理的全生命週期。
本模組需支援 **多商戶 (Multi-Tenant)** 架構，確保不同商戶間的玩家數據完全隔離，同時支持集團級別 (Brand) 的統一視圖。

## 2. 核心功能需求 (Functional Requirements)

### 2.1 註冊流程 (Registration)
- **多種註冊方式**：
  - 帳號/密碼註冊 (需包含圖形驗證碼/滑塊驗證)
  - 手機號碼/OTP 註冊
  - 快速註冊 (一鍵生成帳號密碼)
  - Social Login (Google, Line, Telegram, Facebook)
- **商戶配置項**：
  - 是否強制手機驗證
  - 註冊欄位自定義 (生日、Email、性別是否必填)
  - 預設幣種與允許幣種

### 2.2 登入與安全 (Login & Security)
- **多因子認證 (MFA)**：
  - 支援 Google Authenticator
  - 簡訊 OTP / Email OTP
- **安全風控**：
  - **異地登入提醒**：偵測 IP 地理位置變化
  - **暴力破解防護**：連續失敗 5 次鎖定帳號 30 分鐘 (可由 Admin 配置)
  - **裝置指紋 (Device Fingerprint)**：記錄 Device ID，防止同一裝置多帳號套利
  - **重複帳號處置 (Duplicate Handling SOP)**：
    - **情境 A (誤操作)**：相同 IP/Device，但無獎金濫用行為。
      - **Action**: `Merge`。保留最早註冊帳號，將新帳號餘額轉移至舊帳號，並標記新帳號為 `Closed (Duplicate)`.
    - **情境 B (惡意套利)**：領取多次首存紅利 (Bonus Abuse)。
      - **Action**: `Ban All`. 凍結關聯的所有帳號，沒收紅利與盈利，僅退還本金 (視條款而定)。

### 2.3 實名認證 (KYC/AML)
- **分級認證體系**：
  - **L0 (註冊)**：僅需手機/Email，限制提款
  - **L1 (身份驗證)**：上傳證件 (Passport/ID)，AI 辨識 (OCR) + 人臉比對
  - **L2 (地址驗證)**：上傳水電單/銀行對帳單
- **審核流程**：
  - 支援 **自動審核 (Auto-Approval)** 與 **人工複審 (Manual Review)**
  - 整合第三方 KYC 服務 (如 Sumsub, Jumio)

### 2.4 玩家畫像 (Player Profile)
- **基本資料**：姓名、生日、性別、國家、語言、時區
- **標籤系統 (Tagging)**：
  - 系統自動標籤 (如：高價值、沈睡、套利疑慮)
  - 手動標籤 (客服備註)
- **風險等級**：Low, Medium, High, Critical

## 3. 數據結構設計 (Data Structure)

### 3.1 玩家主表 (Player Master)
| 欄位名稱 | 類型 | 說明 |
|---|---|---|
| player_id | UUID | 全局唯一標識 |
| tenant_id | INT | 所屬商戶 ID |
| username | VARCHAR | 登入帳號 |
| account_status | ENUM | Active, Locked, Suspended, Closed |
| risk_level | INT | 0-100 風險分數 |
| risk_level | INT | 0-100 風險分數 |
| kyc_level | INT | 目前 KYC 認證等級 |
| wallet_mode | ENUM | CASH_ONLY, CREDIT_ONLY, HYBRID |

## 4. 動態配置與審批 (Dynamic Config & Approval)
- **配置項**：註冊必填欄位、MFA 強制開啟條件、KYC 等級限制。
- **審批流**：若修改 "KYC 自動通過閾值" 或 "註冊驗證碼開關"，需觸發 **Maker-Checker** 流程，由商戶管理員審核通過後生效。
