# MFA 登入與恢復要求

> **規範來源**: [06-06-03_Recovery_Flow.md](../../source-archive/06_Platform_Governance/06-06-03_Recovery_Flow.md)
> **目標讀者**: 高層管理、合規官員、安全運營
> **相關架構**: [MFA_Login_Recovery_Technical.md](../../architecture/06_Platform_Core/10_MFA_Login_Recovery_Technical.md)
> **最後同步**: 2026-02-09
>
> **補充說明**: 技術細節（Two-Phase Login 序列圖、Trusted Device Token 生成 Java 代碼、Cookie 配置 HttpOnly/Secure/SameSite、MFA Session 存儲 Redis + 5 分鐘 TTL、SHA256 哈希、QR Code 生成）已移至 Architecture 層。本文檔專注於業務政策。

---

## 業務價值（Business Value）

此功能為業務提供價值：
- **防止憑證洩露 (Preventing Credential Compromise)**：MFA 添加關鍵的第二認證因素，保護管理功能免受密碼被盜或暴力破解攻擊
- **滿足監管合規 (Meeting Regulatory Compliance)**：滿足 UKGC LCCP、MGA 技術標準和 ISO 27001 A.9.4.2 的強制認證要求
- **優化管理員生產力 (Optimizing Administrator Productivity)**：信任設備機制（30 天信任期）消除每天多次從同一工作站登入的管理員重複 MFA 提示
- **減少帳號鎖定摩擦 (Reducing Account Lockout Friction)**：備份碼政策提供緊急恢復路徑，防止可能延遲關鍵運營的管理員鎖定

## 成功指標（Success Metrics）

| 指標 | 目標 | 測量方式 |
|--------|--------|-------------|
| MFA 註冊率 | 強制角色 100% | 已註冊用戶 / MFA 必需角色的總用戶數 |
| 平均登入時間 | < 15 秒（包括 MFA 步驟） | 從密碼輸入到儀表板的總登入時長 |
| 信任設備採用率 | > 70% 的管理員 | 擁有活躍信任設備 Token 的用戶 / 總管理員數 |
| MFA 鎖定率 | < 2% 的登入嘗試 | 因 3 次 TOTP 嘗試失敗而鎖定的帳號 / 總 MFA 登入嘗試 |
| 備份碼使用率 | < 5% 的 MFA 驗證 | 備份碼使用次數 / 總 MFA 驗證次數 |
| 審計日誌完整性 | 100% 的 MFA 事件 | 已記錄事件 / 總 MFA 操作 |
| 監管合規通過率 | 100% 審計通過率 | 通過的監管審計 / 總審計數 |

---

## 1. 業務背景

Multi-Factor Authentication (MFA) 是後台管理平台的強制安全控制。本文檔定義兩階段登入流程、MFA 註冊過程、信任設備機制和錯誤處理政策的業務需求。MFA 保護管理功能免受憑證洩露，並滿足 UKGC、MGA 和 ISO 27001 框架的監管安全要求。

---

## 業務價值（Business Value）

此功能為業務提供價值：
- 通過強制兩階段認證（密碼 + TOTP）保護管理功能免受憑證洩露
- 滿足 UKGC LCCP、MGA 技術標準和 ISO 27001 A.9.4.2 框架的監管合規要求
- 通過 5 分鐘 MFA Session 生命週期和單次使用 Session Token 降低安全風險暴露
- 通過信任設備機制（30 天信任，三重綁定）改善用戶體驗的同時保持安全性
- 通過全面的審計日誌（設置、登入、鎖定、警報）實現快速事件響應
- 通過備份碼（10 個單次使用碼）提供緊急訪問而不影響安全態勢

---

## 2. 兩階段登入流程

### 2.1 階段概述

登入過程分為兩個連續階段：

| 階段 | 目的 | 玩家操作 | 系統操作 |
|-------|---------|---------------|---------------|
| **階段 1** | 密碼驗證 | 輸入用戶名和密碼 | 驗證憑證；判斷是否需要 MFA |
| **階段 2** | MFA 驗證 | 從 Authenticator App 輸入 6 位數 TOTP 代碼 | 驗證 TOTP；發放訪問和刷新 Token |

如果用戶角色未啟用 MFA，階段 1 完成完整登入並立即發放 Token。

### 2.2 MFA Session 規則

| 規則 | 值 | 原因 |
|------|-------|-----------|
| MFA Session 生命週期 | 5 分鐘 | 限制密碼驗證後的脆弱窗口 |
| Session 存儲 | 僅服務器端 | 防止客戶端篡改 |
| Session 為單次使用 | 是 | MFA 驗證成功後消耗並刪除 |

→ **[MFA Session 存儲實作](../../architecture/06_Platform_Core/10_MFA_Login_Recovery_Technical.md#mfa-session-storage)** - Redis 存儲配置、5 分鐘 TTL、Session 清理機制

---

## 3. 信任設備機制

### 3.1 業務目標

每天從同一工作站多次登入的管理員因頻繁 MFA 提示而降低用戶體驗。信任設備機制允許用戶在已識別設備上在定義期限內跳過 MFA 驗證。

### 3.2 政策規則

| 政策 | 值 | 原因 |
|--------|-------|-----------|
| 信任期限 | 30 天 | 行業標準；平衡安全性和可用性 |
| 需要選擇加入 | 是 | 用戶在 MFA 步驟期間明確勾選「信任此設備」 |
| 信任綁定 | 設備指紋 + IP 地址 + User-Agent | 三因素綁定防止 Token 在不同設備上重複使用 |
| Cookie 屬性 | 安全 Cookie 配置 | 防止 JavaScript 訪問、需要 HTTPS、阻止 CSRF |

### 3.3 安全風險緩解

| 風險 | 緩解措施 |
|------|-----------|
| 信任 Token 被盜 | 安全 Cookie 配置防止 JavaScript 訪問 |
| 跨站請求偽造 | Cookie 政策阻止跨源請求 |
| 設備指紋碰撞 | 三重綁定（指紋 + IP + User-Agent）減少誤匹配 |
| 信任期過長 | 30 天 TTL 自動過期；管理員可遠程撤銷 |

→ **[Trusted Device Token 實作](../../architecture/06_Platform_Core/10_MFA_Login_Recovery_Technical.md#trusted-device-token)** - Cookie 配置（HttpOnly、Secure、SameSite=Strict）、SHA256 Token 生成、三重綁定實作

---

## 4. 失敗嘗試和鎖定政策

### 4.1 鎖定規則

| 規則 | 值 |
|------|-------|
| 最大 TOTP 嘗試次數 | 3 次連續失敗 |
| 鎖定時長 | 15 分鐘 |
| 鎖定範圍 | 每個用戶帳號 |
| 鎖定計數器重置 | 15 分鐘窗口後或成功登入後自動重置 |

### 4.2 錯誤代碼定義

| 錯誤條件 | 錯誤代碼 | 推薦用戶指導 |
|----------------|------------|--------------------------|
| MFA Session Token 過期或缺失 | INVALID_MFA_SESSION | 重定向用戶從階段 1 重新開始登入 |
| TOTP 代碼不正確 | INVALID_TOTP | 顯示剩餘嘗試次數；提示重新輸入 |
| 3 次失敗後帳號鎖定 | MFA_LOCKED | 顯示鎖定倒計時定時器（15 分鐘） |
| 用戶調用 MFA 驗證但 MFA 未啟用 | MFA_NOT_ENABLED | 引導用戶在個人設置中啟用 MFA |
| 服務器時間同步失敗 | TIME_SYNC_ERROR | 觸發運營警報；向用戶顯示通用錯誤 |

---

## 5. MFA 註冊要求

### 5.1 註冊觸發條件

MFA 註冊在以下條件下啟動：

| 觸發條件 | 描述 | 需要的用戶操作 |
|---------|------------|---------------------|
| 首次使用強制 MFA 角色登入 | 用戶角色需要 MFA；用戶尚未註冊 | 訪問系統前必須完成註冊 |
| 自願啟用 | 用戶導航到個人設置並啟用 MFA | 遵循引導註冊流程 |
| 管理強制執行 | 安全管理員為用戶啟用 MFA | 下次登入時提示用戶 |

### 5.2 註冊步驟

註冊過程遵循三個連續步驟：

| 步驟 | 名稱 | 描述 |
|------|------|-------------|
| 1 | 生成 Secret | 系統生成 TOTP Secret Key 並呈現給用戶 |
| 2 | 掃描 QR Code | 用戶使用 Google Authenticator（或兼容 TOTP App）掃描 QR Code |
| 3 | 驗證和激活 | 用戶輸入第一個 6 位數代碼證明註冊成功 |

→ **[TOTP Secret 和 QR Code 生成](../../architecture/06_Platform_Core/10_MFA_Login_Recovery_Technical.md#totp-registration)** - Secret 生成算法、QR Code 編碼、安全顯示實作

### 5.3 Backup Code 政策

| 政策 | 值 |
|--------|-------|
| 備份碼數量 | 10 個 |
| 碼格式 | 8 位數字 |
| 交付方式 | 在屏幕上顯示一次；可下載為文本文件 |
| 存儲 | 服務器端安全存儲；明文僅在生成時顯示一次 |
| 使用 | 單次使用；每個碼可用於一次帳號恢復 |

→ **[Backup Code 存儲實作](../../architecture/06_Platform_Core/10_MFA_Login_Recovery_Technical.md#backup-codes)** - 加密算法（AES-256-GCM）、安全生成、存儲格式

---

## 6. 支持的 MFA 方法

| 方法 | 狀態 | 描述 |
|--------|--------|-------------|
| TOTP (Google Authenticator) | 主要，強制 | 基於時間的 6 位數代碼，30 秒週期 |
| SMS | 次要，可選 | 通過 SMS 發送 6 位數代碼（備用） |
| Backup Codes | 恢復 | 預生成的單次使用碼用於緊急訪問 |

---

## 7. 合規和審計要求

### 7.1 審計事件

每個 MFA 相關操作必須產生不可變的審計日誌條目：

| 事件 | 審計日誌代碼 | 捕獲的詳情 |
|-------|---------------|-----------------|
| MFA 設置啟動 | MFA_SETUP_INIT | 用戶 ID、時間戳 |
| MFA 成功啟用 | MFA_ENABLED | 用戶 ID、激活時間戳 |
| MFA 登入成功 | MFA_LOGIN_SUCCESS | 用戶 ID、設備指紋、IP 地址 |
| MFA 登入失敗 | MFA_LOGIN_FAILED | 用戶 ID、失敗原因、剩餘嘗試次數 |
| 帳號鎖定 | MFA_ACCOUNT_LOCKED | 用戶 ID、鎖定時間戳、解鎖時間 |
| 發送安全警報 | MFA_SECURITY_ALERT | 用戶 ID、警報類型、收件人 |

### 7.2 監管對齊

| 法規 | MFA 要求 | 狀態 |
|-----------|----------------|--------|
| UKGC LCCP | 管理訪問強認證 | 由 TOTP + 信任設備覆蓋 |
| MGA 技術標準 | 後台兩因素認證 | 由兩階段登入覆蓋 |
| ISO 27001 A.9.4.2 | 安全登入程序 | 由鎖定 + 審計追蹤覆蓋 |

---

## 8. 用戶體驗要求

### 8.1 UI 流程摘要

| 屏幕 | 內容 | 可用操作 |
|--------|---------|-------------------|
| 登入表單 | 用戶名和密碼字段 | 提交憑證 |
| MFA 提示 | 6 位數 TOTP 輸入字段；「信任此設備」複選框 | 提交代碼；返回登入 |
| MFA 設置嚮導 | 步驟指示器；QR Code 顯示；備份碼下載；驗證輸入 | 掃描 QR；下載碼；驗證代碼 |
| 鎖定屏幕 | 倒計時定時器；支持聯繫信息 | 等待解鎖；聯繫管理員 |
| 錯誤顯示 | 上下文適當的錯誤消息及剩餘嘗試次數 | 重試；重新開始登入 |

### 8.2 可訪問性

| 要求 | 實施 |
|-------------|---------------|
| 手動 Secret 輸入 | 與 QR Code 一起顯示，供無法掃描的用戶使用 |
| 備份碼下載 | 可作為純文本文件用於離線存儲 |
| 錯誤消息 | 清晰指導具體的下一步（不是通用錯誤） |

---

## 9. 相關業務需求

| 文檔 | 關係 |
|----------|-------------|
| MFA Architecture Design (06-06-01) | 高級 MFA 策略和方法選擇 |
| TOTP and WebAuthn Implementation (06-06-02) | 詳細 TOTP 算法規範 |
| Compliance and Audit (06-06-04) | 審計日誌標準和基於角色的 MFA 政策 |
| RBAC Permissions (06-02) | 決定 MFA 執行的角色定義 |

### 技術實作

→ **[MFA Recovery Implementation](../../architecture/06_Platform_Core/09_MFA_Recovery_Implementation.md)** - 兩階段登入序列圖、MFA Session 存儲（Redis + TTL）、信任設備 Token 生成（SHA256）、Cookie 配置（HttpOnly/Secure/SameSite）和 TOTP QR Code 生成

---

**Navigation**: [Governance and Licensing Requirements](../06_Governance_Licensing/) | [iGaming Home](../../README.md)
