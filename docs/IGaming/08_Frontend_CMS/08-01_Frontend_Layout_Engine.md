# 08-01 前台版面配置引擎 (Frontend Layout Engine)

## 1. 系統概述
無需工程師介入，運營人員即可透過後台 "拖拉拽" (Drag & Drop) 方式調整前台首頁佈局。
支援多終端 (Web, H5, App) 的自適應配置。

## 2. 核心功能需求

### 2.1 模板管理 (Template Management)
- **主題切換**：
  - 平台預置多套主題 (深色、亮色、節日限定)。
  - 商戶可一鍵切換整站配色 (CSS Variables)。
- **組件庫 (Component Library)**：
  - Banner 輪播模組
  - 跑馬燈 (Marquee)
  - 遊戲入口網格 (Game Grid) - 可設定 3列/4列/5列
  - 存款引導按鈕

### 2.2 頁面編輯器 (Page Editor)
- **首頁裝修**：
  - 上下拖動模組排序。
  - 設置模組標題 (如 "熱門遊戲" 改為 "TOP 10")。
- **導航欄配置 (Menu Config)**：
  - 自定義 Header/Footer 菜單順序。
  - 支援跳轉內部頁面或外部連結。

## 3. 審批與發布
- **預覽模式 (Preview)**：編輯完成後，生成一個 "預覽連結" (Preview URL) 供內部測試。
- **發布流程**：
  - 點擊 "發布 (Publish)" -> 生成版本號 (v1.0.1)。
  - 系統將配置推送至 CDN。
  - 前段透過 API 拉取最新 JSON Config 渲染頁面。

## 4. 實驗與優化 (Experimentation)
為提升轉換率 (CTR)，佈局引擎需支援 A/B 測試：
- **實驗配置**：
  - 在 Layout JSON 中添加 `experiment_id: "EXP_HOME_V2"`.
  - **Variant A**: Control Group (原版).
  - **Variant B**: Test Group (如：將 "熱門遊戲" 移至最頂部).
- **分流邏輯 (Traffic Splitting)**：
  - 基於 `DeviceID` 或 `PlayerID` 進行 Hash 取模：`hash(id) % 100 < 50 ? A : B`.
  - 確保同一用戶始終看到相同版本 (Consistency).
- **數據追蹤**：
  - 前端渲染時自動上報 `Exposure` 事件 (包含 `experiment_id`, `variant`).

## 5. 動態規則
- **客群定向**：
  - 可設定 "僅 VIP 可見" 的專屬 Banner。
  - 可設定 "僅新註冊用戶可見" 的首存優惠廣告。

## 5. 錢包模式 UI 適配 (Wallet Mode UI Adaptation)
前端需根據 `player.wallet_mode` 自動切換 Header 資訊顯示：

### 5.1 Cash Mode (現金模式)
*   **顯示內容**：`Balance` (Total Cash + Bonus)
*   **特徵**：強調 "充值 (Deposit)" 按鈕。

### 5.2 Credit Mode (信用模式)
*   **顯示內容**：
    *   `Credit`: 信用額度 (Limit)。
    *   `Used`: 已用額度。
    *   `Available`: 可用額度 (重點顯示)。
*   **特徵**：
    *   隱藏 "充值" 按鈕，改為 "額度 (Quota)" 詳情頁。
    *   顯示 **"週結倒數 (Settlement Countdown)"**，提醒玩家結算日。

### 5.3 Hybrid Mode (混合模式)
*   **顯示內容**：同時顯示 Cash Balance 與 Available Credit。
*   **支付選擇器**：在下注或購買道具時，允許玩家選擇 "優先扣除 Cash" 或 "使用 Credit"。

