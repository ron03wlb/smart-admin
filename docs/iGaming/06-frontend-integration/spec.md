---
title: "Split 06: 前端與整合域 — Frontend & Integration Domain Spec"
part: spec
module: 06-frontend-integration
version: v1.0
created: 2026-03-26
sources:
  - requirements/11_Frontend_Experience_前端體驗.md (Ch11)
  - requirements/14_Third_Party_Integration_第三方整合.md (Ch14)
depends_on:
  - 01-governance-agent
  - 02-funding
  - 03-player
  - 04-gaming
  - 05-risk-compliance
priority: 6
---

# Split 06 — Frontend & Integration Domain Spec

> **涵蓋章節**: Ch11 前端體驗 + Ch14 第三方整合
> **核心職責**: 多語言、行動 App、SEO、CMS、統一適配層、Webhook 重試、API 金鑰管理、服務降級
> **上游依賴**: 所有後端域 (01-05) — 前端整合所有後端功能；第三方整合是平台對外的標準化介面層
> **複雜度**: 中高 — 6+ 語言 x 多終端 x 第三方容錯 x App Store 合規

---

## Table of Contents

1. [i18n — Multi-Language Support](#1-i18n--multi-language-support)
2. [Mobile App](#2-mobile-app)
3. [SEO & Core Web Vitals](#3-seo--core-web-vitals)
4. [Responsive Design & Multi-Device](#4-responsive-design--multi-device)
5. [CMS Page Configuration](#5-cms-page-configuration)
6. [Banner System](#6-banner-system)
7. [Accessibility (a11y)](#7-accessibility-a11y)
8. [App Store Compliance](#8-app-store-compliance)
9. [Publishing & Approval Flow](#9-publishing--approval-flow)
10. [Third-Party Integration Principles](#10-third-party-integration-principles)
11. [Vendor SLA Requirements](#11-vendor-sla-requirements)
12. [PSP SLA vs Platform RTO Compatibility](#12-psp-sla-vs-platform-rto-compatibility)
13. [Third-Party Data Residency](#13-third-party-data-residency)
14. [Webhook Retry Strategy](#14-webhook-retry-strategy)
15. [API Key Management](#15-api-key-management)
16. [Webhook Signature Upgrade Path](#16-webhook-signature-upgrade-path)
17. [Service Degradation & Fault Tolerance](#17-service-degradation--fault-tolerance)
18. [Gaps & TBD Items](#18-gaps--tbd-items)
19. [SSOT Registry](#19-ssot-registry)
20. [Success Metrics](#20-success-metrics)

---

## 1. i18n — Multi-Language Support

### 1.1 Language Coverage

| 優先級 | 語言 | 代碼 | 說明 |
|--------|------|------|------|
| **P0** | 簡體中文 | zh-CN | 核心市場 |
| **P0** | 英文 | en-US | 全球通用 |
| **P1** | 越南文 | vi-VN | 東南亞市場 |
| **P1** | 泰文 | th-TH | 東南亞市場 |
| **P2** | 巴西葡萄牙文 | pt-BR | 拉美市場 |
| **P2** | 日文 | ja-JP | 亞太市場 |

### 1.2 Fallback Chain

```
用戶語言 → en-US → zh-CN → 預設圖片
```

### 1.3 Translation Management

- CMS 管理翻譯，支援版本控制
- 命名空間結構: `module.component.key`
- Banner 素材須支援 6+ 語言版本
- 支援 **RTL 語言** (自動偵測 DOM 方向)

---

## 2. Mobile App

### 2.1 Key Requirements

| 項目 | 要求 |
|------|------|
| 深度連結 (Deep Links) | 支援所有導航目標 |
| 推送通知 | 最多 **5 則/天**，靜音時段 **23:00-09:00** |
| 離線支援 | 本地快取 + 增量同步 (非全量重新載入) |
| 安全 | SSL Pinning, API 簽名驗證, 本地加密 (AES-256), TLS 1.2+ |
| 熱更新 (Hot Update) | 支援即時更新 (無需重新提交商店) |
| APK 體積 | 東南亞市場 **< 5MB** |

### 2.2 Wallet Mode UI Adaptation

| 模式 | 顯示內容 | 重點 |
|------|---------|------|
| 現金模式 (Cash) | 總餘額 (現金 + 紅利) | 強調「儲值」按鈕 |
| 信用模式 (Credit) | 信用額度/已用/可用 | 顯示結算倒數 |
| 混合模式 (Hybrid) | 現金餘額 + 可用信用 | 支付選擇器 |

> **SSOT REF**: 錢包三類型完整定義 -> Ch2 (02-funding split)。

---

## 3. SEO & Core Web Vitals

### 3.1 Performance Targets

| 指標 | 目標 | 說明 |
|------|------|------|
| **LCP** | < 2.5 秒 | 最大內容繪製 — Banner 須壓縮至 <= 200KB + lazy load + WebP/AVIF |
| **FCP** | < 2 秒 | 首次內容繪製 |
| **CLS** | < 0.1 | 累計布局偏移 |
| **Lighthouse** | >= 0.9 | 效能評分 |

### 3.2 SEO Technical Requirements

- 支援 **ISR** (增量靜態再生) + **SSR** Fallback
- **Canonical URL** + **hreflang** 多語言標記
- **Schema.org** 結構化數據 (遊戲頁面)
- Open Graph 標記 (社交分享)
- 多層快取: CDN (60s) -> Origin (3600s) -> API (300s)
- 圖片 WebP/AVIF 格式 + srcset 響應式
- 4G 場景採用低解析度版本

---

## 4. Responsive Design & Multi-Device

### 4.1 Multi-Device Support

| 終端 | 要求 |
|------|------|
| **Desktop** | 全功能 |
| **Mobile Web (H5)** | 適配主流機型 |
| **Native App** | iOS + Android |

---

## 5. CMS Page Configuration

### 5.1 Editor Capabilities

- **無代碼拖拽式編輯器** (No-code drag-drop)
  - Banner 輪播
  - 遊戲網格 3/4/5 欄
  - 按鈕組件
- **Header 導航**自訂連結
- **主題切換** (明亮/暗黑/季節性) 一鍵全局套用
- **組件級設備可見性控制** (僅 Desktop / 僅 Mobile / 全部)

---

## 6. Banner System

### 6.1 Asset Specifications

| 版位 | 尺寸 | 大小上限 |
|------|------|---------|
| Desktop Banner | 1920 x 600 | <= 200KB |
| Mobile Banner | 750 x 400 | <= 150KB |
| Popup | 600 x 600 | <= 100KB |

### 6.2 Targeting Rules

- 依用戶分群投放 (新/活躍/沉睡/高價值)
- 依 **VIP 等級、設備、地理位置、時間**定向
- 分析追蹤: 曝光、點擊、CTR、轉化、CVR
- 支援 **A/B 測試**

---

## 7. Accessibility (a11y)

| 要求 | 標準 |
|------|------|
| 標準 | **WCAG 2.1 AA** |
| HTML | 語義化 HTML + ARIA 標記 |
| 鍵盤 | 完整鍵盤導航支援 |
| 對比度 | **4.5:1** (正常文字) / 3:1 (大文字) |
| 測試 | 螢幕閱讀器測試 |

---

## 8. App Store Compliance

### 8.1 Apple App Store (Guideline 5.3.4)

| 要求 | 說明 |
|------|------|
| 地區限制 | 僅在已取得牌照的管轄區發布 (透過 App Store Connect 地區設定) |
| 年齡門控 | Age Rating **17+**，啟動時須年齡確認閘門 |
| 真錢博彩標記 | 須在 App Store Connect 中勾選「Gambling and Contests」 |
| 無第三方支付 | Apple 不抽成博彩交易，但須使用平台自有支付閘道 (**非 IAP**) |
| 帳戶管理 | 須提供**帳戶刪除功能** (GDPR + Apple 要求) |
| 審核資料 | 須提供牌照副本、測試帳號、合規文件 |

### 8.2 Google Play (Gambling Policy)

| 要求 | 說明 |
|------|------|
| 申請資格 | 須通過 Google 博彩營運商申請流程 (提供牌照) |
| 地區限制 | 僅 Google 核准的國家/地區 (目前: 英國、法國、巴西、菲律賓等) |
| 年齡驗證 | 須整合年齡驗證機制 |
| 負責任博彩 (RG) | 須在 App 內提供自我排除、限額設定入口 |
| APK 分發 | 非 Google Play 核准地區須透過官網 APK 側載 (須 SSL + 簽名驗證) |

### 8.3 Compliance Checklist

- [ ] Apple 審核資料包準備 (牌照、測試帳號、合規說明)
- [ ] Google 博彩營運商申請
- [ ] 年齡門控閘門實作
- [ ] 帳戶刪除功能 (符合 GDPR + App Store 要求)
- [ ] 負責任博彩功能可從 App 內直接存取
- [ ] 非核准地區的 APK 側載方案

---

## 9. Publishing & Approval Flow

| 角色 | 權限 |
|------|------|
| 營運專員 (Operator) | 建立 + 提交 |
| 營運主管 (Supervisor) | 審批 + 發布 |
| CTO | **緊急下架** |

**流程規範**:
- 發布前生成預覽 URL
- 版本號管理 + CDN 推送
- 本地版本過期檢查後才下載更新

---

## 10. Third-Party Integration Principles

| 原則 | 說明 |
|------|------|
| **統一適配層** (Unified Adapter) | 所有第三方透過統一 Adapter 接入 |
| **容錯優先** | 第三方故障 ≠ 平台故障 |
| **監控優先** | 每個整合點均有健康檢查與警報 |
| **安全優先** | 加密 API 金鑰、Webhook 簽名驗證 |

---

## 11. Vendor SLA Requirements

| 服務 | 最低可用率 | P99 回應時間 | 速率限制 |
|------|----------|------------|---------|
| **PSP** (支付) | 99.5% | < 3 秒 | — |
| **KYC** 供應商 | 98.0% | < 5 秒 | 100 次/分鐘 |
| **遊戲供應商** (GP) | 99.0% | < 3 秒 | 500 次/分鐘 |
| **Email** 服務 | 99.0% | < 1 秒 | 1,000 次/小時 |
| **分析** 服務 | 95.0% | 盡力 | 無限制 |

**SLA 管理**:
- 30 天滾動窗口計算
- 違約時供應商提供月度服務信用
- 低於目標自動升級至供應商經理

---

## 12. PSP SLA vs Platform RTO Compatibility

**問題**: PSP SLA 99.5% 允許每月約 22 分鐘停機，但 Ch10 定義支付 RTO < 15 分鐘。

**解決方案 — PSP Degradation Strategy**:

| 階段 | 條件 | 動作 |
|------|------|------|
| **偵測** | 主 PSP 連續 3 次請求失敗或 P99 > 5 秒 | 觸發 Circuit Breaker |
| **切換** | Circuit Breaker OPEN | 自動路由至備用 PSP (**< 30 秒**) |
| **恢復** | 主 PSP 連續 3 次健康檢查通過 | 逐步切回 (25% -> 50% -> 100%) |

**RTO 計算規則**: 平台 RTO 計算**排除**外部 PSP 不可控停機時間，但平台須在 PSP 停機 **30 秒內**完成備援切換。

---

## 13. Third-Party Data Residency

> v2.2 新增 — GAP-5 (第三方資料駐留合規)
> **Status**: TBD — 待合規團隊 + 法務確認

**問題**: EU 平台使用 US-based PSP/KYC 供應商時，可能違反 GDPR 資料傳輸規則。

| 要求 | 說明 | 狀態 |
|------|------|------|
| 資料駐留 | PII 必須留在資料駐留區域內處理 | TBD — 待法務確認各區域要求 |
| SCC (標準合約條款) | 跨境資料傳輸須簽署 SCC | TBD — 待法務準備模板 |
| DPA (資料處理協議) | 所有第三方整合前須簽署 DPA | TBD — 待合規流程定義 |
| 年度稽核 | 每年審計第三方資料處理實務 | TBD — 待稽核排程 |
| Schrems II 合規 | 確認 EU->US 資料傳輸符合 Schrems II 判決 | TBD — 待法務評估 |

---

## 14. Webhook Retry Strategy

### 14.1 Exponential Backoff (6 Retries)

| 重試次數 | 延遲 | 累計 |
|---------|------|------|
| 1 | 5 秒 | 5 秒 |
| 2 | 10 秒 | 15 秒 |
| 3 | 20 秒 | 35 秒 |
| 4 | 40 秒 | 75 秒 |
| 5 | 80 秒 | 155 秒 |
| 6 | 160 秒 | 315 秒 |

### 14.2 Webhook Idempotency (Receiver-Side)

| 項目 | 規則 |
|------|------|
| Idempotency Key | 每個 webhook 須攜帶唯一 `idempotency_key` (由發送方生成) |
| 去重窗口 | 24 小時 (Redis SET + TTL 24h) |
| 重複處理 | 收到重複 key -> 回傳 200 OK + 原始處理結果 (不重複執行) |
| 缺少 Key | 回傳 400 Bad Request (PSP/GP 整合強制要求) |
| 簽名驗證 | **HMAC-SHA256** 簽名驗證 -> 失敗則回傳 401 + 記錄告警 |

### 14.3 Dead Letter Queue (DLQ)

| 項目 | 規則 |
|------|------|
| 進入條件 | 6 次重試全部失敗 |
| 保留期限 | **7 天** |
| 手動重放 | 支援 |
| 警報 | DLQ 事件 > 100 則觸發 |

---

## 15. API Key Management

| 服務類型 | 輪換週期 | 自動/手動 | 觸發條件 |
|---------|---------|----------|---------|
| PSP API | **90 天** | 自動 | 週期性 + 可疑活動 |
| 內部服務 | **30 天** | 自動 | 週期性 |
| Webhook Secret | **按需** | 手動 | 疑似洩漏 |
| 資料庫密碼 | **180 天** | 自動 | 週期性 |

**安全要求**:
- 所有金鑰存放於加密秘密管理器
- 禁止出現在原始碼、日誌或配置檔中
- RBAC 存取控制
- 所有存取留審計記錄

---

## 16. Webhook Signature Upgrade Path

| 項目 | 規則 |
|------|------|
| 當前演算法 | HMAC-SHA256 |
| 版本協商 | Webhook Header 包含 `X-Signature-Version: v1` |
| 過渡期策略 | 新版發布後 **90 天內**同時接受 v1 和 v2 簽名 (共存期) |
| 升級通知 | 提前 **60 天**通知所有 webhook 消費者 |
| 降級回退 | 若 v2 驗證失敗率 > 1%，自動回退至 v1 |

---

## 17. Service Degradation & Fault Tolerance

### 17.1 Degradation Priority

| 服務 | 優先級 | 影響 | 備援方案 |
|------|--------|------|---------|
| **PSP** | Critical (嚴重) | 無法存提款 | 切換備用 PSP |
| **KYC** | Important (重要) | 無法驗證身份 | 人工審核佇列 |
| **遊戲供應商** | Important (重要) | 特定遊戲不可用 | 維護中通知 |
| **Email** | Optional (可選) | 通知延遲 | 重試佇列 |
| **分析** | Optional (可選) | 無事件追蹤 | 本地日誌 |

### 17.2 Recovery Detection

| 項目 | 規則 |
|------|------|
| 健康檢查間隔 | **30 秒** |
| 恢復條件 | 連續 **3 次**健康檢查通過 (> 95%) |
| 恢復動作 | 自動切回主要服務 |
| 記錄 | 恢復事件記入警報管道 |

### 17.3 Monitoring & Alerts

**即時健康儀表板**:
- 可用率 % (過去 1 小時)
- P99 回應時間
- 錯誤率
- 狀態指示器 (健康/降級/不可用)

| 警報 | 條件 | 嚴重度 |
|------|------|--------|
| PSP 失敗率 | > 5% / 5 分鐘窗口 | 嚴重 |
| KYC P99 延遲 | > 5 秒 / 10 分鐘 | 警告 |
| Email 發送失敗 | 任何失敗 | 警告 |
| DLQ 累積 | > 100 事件 | 高 |

---

## 18. Gaps & TBD Items

以下為已知缺漏，待後續補充完善:

### 18.1 Notification Hub

> **Status**: TBD

統一 push/SMS/email/in-app 通知策略，跨所有模組。當前各模組 (風控告警、活動推送、客服回覆、系統通知) 的通知機制分散定義，缺乏統一的通知中心規範。

**待定義項目**:
- 統一通知管道優先級 (push > in-app > email > SMS)
- 跨模組通知合併/去重策略
- 通知偏好設定 (玩家可控)
- 各管道的 SLA 與重試策略

### 18.2 Admin UX

> **Status**: TBD

統一後台管理體驗 — CMS/活動建立/客服工具/報表操作的統一後台界面。當前各模組後台設計分散，缺乏統一的管理端體驗規範。

**待定義項目**:
- 統一導航結構與權限體驗
- 跨模組搜尋與快速操作
- 批量操作標準
- 響應式後台 (平板支援)

### 18.3 Feature Flag / Dark Launch Strategy

> **Status**: TBD

漸進式上線機制，支援新功能的安全發布與快速回滾。

**待定義項目**:
- Feature Flag 管理平台選型
- 灰度發布策略 (百分比/用戶分群/地區)
- 與 Canary 部署的整合方式
- Flag 生命週期管理 (清理過期 Flag)

---

## 19. SSOT Registry

### This Split HOLDS

本 split 不持有跨域 SSOT。所有業務規則的權威定義在各後端域 split 中。

### This Split REFERENCES

| 引用項目 | SSOT 來源 | 所屬 Split |
|---------|----------|-----------|
| 所有後端域 API | 各域 spec.md | 01-05 |
| 平台配置三層覆蓋 | Ch0 S0.10 | Cross-cutting |
| 多管轄區合規矩陣 | Ch7 S7.9 | 01-governance-agent |
| 錢包三類型 | Ch2 | 02-funding |
| 風險分數邊界 | Ch6 S6.6 | 05-risk-compliance |
| GDPR 刪除/帳戶管理 | Ch13 S13.2 | 05-risk-compliance |
| 自我排除機制 | Ch15 S15.1 | 03-player |
| SLA 三級體系 | Ch10 S10.3 | 07-data-infrastructure |

---

## 20. Success Metrics

### 20.1 Frontend KPIs

| 指標 | 目標 |
|------|------|
| 頁面載入時間 | < 3 秒 |
| App 啟動時間 | < 2 秒 |
| LCP | < 2.5 秒 |
| 翻譯覆蓋率 | >= 95% |
| Banner CTR | >= 8% |

### 20.2 Integration KPIs

| 指標 | 目標 |
|------|------|
| 平台可用率 (含第三方) | >= 99.9% |
| PSP 切換時間 | < 30 秒 |
| DLQ 清零率 (7 天) | > 95% |
| API 金鑰零洩漏 | 100% |
