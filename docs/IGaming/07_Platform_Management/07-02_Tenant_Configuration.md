# 07-02 商戶配置管理 (Tenant Configuration)

## 1. 系統概述
提供 "開箱即用" 的站點配置能力，讓商戶可自助化管理站點基礎屬性。
所有配置需支援動態生效，無需重啟服務。

## 2. 核心配置項

### 2.1 基礎資訊 (Basic Info)
- **站點名稱**：顯示於瀏覽器 Title。
- **域名綁定 (Domains)**：
  - 主域名 (www.abc.com)
  - API 域名 (api.abc.com)
  - CDN 域名 (img.abc.com)
- **幣種 (Currency)**：
  - 主幣種 (Base Currency)
  - 支援的錢包幣種 (如 USD, VND, THB)。

### 2.2 限額與開關 (Limits & Switches)
- **維護開關**：一鍵開啟 "全站維護" 或 "前台維護"。
- **註冊開關**：暫停新用戶註冊。
- **IP 限制**：
  - 允許訪問的國家 (GeoIP Allowlist)。
  - 禁止訪問的國家 (Blacklist)。
  - 辦公室 IP 白名單 (後台訪問用)。

### 2.3 遊戲額度控制 (Credit Quota)
- **買分模式 (Transfer Mode)**：
  - 商戶需先向平台 "購買額度" (Pre-paid)，玩家才能遊戲。
  - 當商戶餘額不足時，系統自動阻擋玩家轉入遊戲，並發送警報 (Low Balance Alert)。

### 2.4 遊戲管理 (Game Management)
允許商戶針對特定遊戲廠商或遊戲進行屏蔽 (例如：因當地法規或商業考量)。
- **黑名單配置 (Blocklist)**：
  - `Provider Level`: 屏蔽整個廠商 (e.g. Block 'PGSoft')。
  - `Game Level`: 屏蔽特定遊戲 (e.g. Block 'mahjong-ways-2')。
- **維護模式 (Maintenance)**：
  - 商戶可自行設定顯示 "維護中" 頁面，而不依賴平台的全局維護。


## 3. 動態配置與審批 (Dynamic Config & Approval)

### 3.1 動態配置原則
- **實時生效**：IP 白名單、域名綁定、維護開關等配置修改後，必須在 < 1分鐘內同步至所有 Gateway 節點。
- **無停機變更**：配置變更絕不可導致服務重啟。

### 3.2 審批工作流
- **高危操作 (雙人覆核)**：
  - **修改 IP 限制**：防止惡意封鎖正常流量或誤開後門。
  - **修改域名綁定**：防止域名劫持風險。
  - **流程**：商戶 Admin 提交 -> 平台運營覆核 -> 生效。
- **一般操作 (記錄留痕)**：
  - **修改站點名稱/Logo**：無需審批，但必須記錄 Audit Log。
  - **Audit Log 格式**：`[User] [Action] [Target] [OldValue] -> [NewValue]`。
