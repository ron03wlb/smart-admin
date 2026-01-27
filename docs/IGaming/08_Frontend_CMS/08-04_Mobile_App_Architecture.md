# 08-04 行動端 App 架構 (Mobile App Architecture)

## 1. 系統概述
為提供最佳的移動端體驗，平台需提供原生 (Native) 或 混合 (Hybrid) App。
核心挑戰在於 **"上架審核困難"** (AppStore/PlayStore 禁止賭博 App) 與 **"頻繁更新需求"**。

## 2. 技術選型

### 2.1 框架策略
推薦採用 **Flutter** 或 **React Native** 進行跨平台開發：
*   **優勢**：一套代碼同時生成 iOS 與 Android。
*   **熱更新**：支援 CodePush / HotUpdate 技術，繞過商店審核直接更新業務邏輯。

### 2.2 上架策略 (Distribution)
1.  **iOS**:
    *   **Enterprise Certificate (企業簽)**：無需上架 AppStore，用戶信任證書即可安裝 (容易掉簽，需頻繁補簽)。
    *   **TestFlight**: 透過測試名義分發 (有效期 90 天)。
    *   **Super Signature (超級簽)**：利用開發者帳號的 device id 額度分發。
    *   **WebClip (偽 App)**：在桌面生成一個 PWA 書籤，點擊開啟 Safari (最安全，但體驗稍差)。
2.  **Android**:
    *   **APK 下載**: 官網直接提供 APK 包。
    *   **Google Play (馬甲包)**：偽裝成 "記帳軟體" 或 "新聞閱讀器" 上架，通過後台開關切換為博彩介面 (風險高，易被下架)。

---

## 3. 核心功能模組

### 3.1 基礎設施
*   **HttpDNS**: 防止 DNS 污染，確保 API 連線穩定。
*   **Domain Fronting (域前置)**: 隱藏真實後端 IP，抗封鎖。

### 3.2 原生功能 (Native Features)
*   **Biometrics**: 支援 FaceID / TouchID 快速登入。
*   **Push Notification**: 集成 FCM / APNS，接收營銷推送。
*   **Device Fingerprint**: 採集 IDFA / GAID / IMEI / AndroidID 用於風控。

### 3.3 熱更新機制 (Hot Update)
1.  App 啟動時檢查 version.json。
2.  若有新版本 (Patch)，背景下載 JS Bundle / Assets。
3.  下載完成後，提示用戶 "重啟生效" 或 下次啟動自動套用。
4.  **回滾機制**：若新包崩潰 (Crash)，自動還原至上一版。

---

## 4. 安全性
*   **加殼 (Packing)**：防止 APK 被反編譯與篡改 (如 360加固 / 騰訊樂固)。
*   **Root/Jailbreak Detection**: 檢測到設備已越獄/Root，強制退出或限制高風險操作。
*   **SSL Pinning**: 防止中間人攻擊 (Man-in-the-Middle) 抓包。
