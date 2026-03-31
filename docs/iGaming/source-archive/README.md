# iGaming Source Documentation (Archive)

> **狀態**: ⚠️ **唯讀歸檔（READ-ONLY ARCHIVE）** — 禁止直接修改此目錄下的任何文件
> **目的**: 保存原始 SSOT 文檔作為歷史參考
> **受眾**: 所有人（查閱，不可修改）
> **最後更新**: 2026-02-09（歸檔日期）

---

## ⚠️ 重要說明：禁止修改

**此目錄是唯讀歸檔。請勿修改任何文件。**

所有業務需求更新 → 前往 `../requirements/`
所有架構設計更新 → 前往 `../architecture/`

---

## 關於此目錄（About This Directory）

`source-archive/` 保存了 iGaming 平台文檔的**原始版本**，於 2026-02-09 歸檔時凍結。

### 命名差異說明（Naming Difference）

此目錄使用「**Center**（中心）」命名：
- `01_Player_Center/`（玩家中心）
- `02_Finance_Center/`（財務中心）
- `03_Game_Center/`（遊戲中心）

現行文件使用「**Service**（服務）」命名：
- `../architecture/01_Player_Service/`
- `../architecture/02_Finance_Service/`
- `../architecture/03_Game_Integration/`

**演進歷史**：原始設計採用「中心（Center）」概念，以業務域劃分。v4.1.0 重構後改用「服務（Service）」概念，更符合微服務架構語義。兩套命名指向相同業務領域，內容已拆分整理至 `requirements/` 和 `architecture/`。

### 內容遷移狀態

| 原始文件 | 已遷移至 |
|---------|---------|
| `01_Player_Center/` | `requirements/01_Player_Experience/` + `architecture/01_Player_Service/` |
| `02_Finance_Center/` | `requirements/02_Financial_Operations/` + `architecture/02_Finance_Service/` |
| `03_Game_Center/` | `requirements/03_Gaming_Operations/` + `architecture/03_Game_Integration/` |
| `04_Activity_Center/` | `requirements/04_Promotions_VIP/` + `architecture/04_Activity_Engine/` |
| `05_Risk_Control/` | `requirements/05_Risk_Compliance/` + `architecture/05_Risk_Engine/` |
| `06_Platform_Governance/` | `requirements/06_Governance_Licensing/` + `architecture/06_Platform_Core/` |
| `07_Agent_Center/` | `requirements/07_Agent_Operations/` + `architecture/07_Agent_Service/` |
| `08_Analytics_BI/` | `requirements/08_Analytics_Operations/` + `architecture/08_Analytics_Service/` |
| `09_Technical_Infrastructure/` | `requirements/09_Infrastructure_Requirements/` + `architecture/09_Infrastructure/` |
| `10_Platform_Management/` | `requirements/10_Platform_Operations/` + `architecture/10_Platform_Management/` |
| `11_Frontend_CMS/` | `requirements/11_Frontend_Experience/` + `architecture/11_Frontend/` |
| `12_System_Security/` | `requirements/12_Security_Compliance/` + `architecture/12_Security/` |
| `13_Customer_Service/` | `requirements/13_Customer_Service/` + `architecture/13_Customer_Service/` |
| `14_Third_Party_Integration/` | `requirements/14_Integration_Standards/` + `architecture/14_Third_Party/` |
| `15_Responsible_Gambling/` | `requirements/15_Responsible_Gambling/` + `architecture/15_Responsible_Gambling/` |

---

## 模組索引（Module Index）

| 模組（舊命名） | 對應新模組 |
|--------------|----------|
| [00_Foundation](00_Foundation/) | 原始概覽與快速入門 |
| [01_Player_Center](01_Player_Center/) | → `requirements/01` + `architecture/01` |
| [02_Finance_Center](02_Finance_Center/) | → `requirements/02` + `architecture/02` |
| [03_Game_Center](03_Game_Center/) | → `requirements/03` + `architecture/03` |
| [04_Activity_Center](04_Activity_Center/) | → `requirements/04` + `architecture/04` |
| [05_Risk_Control](05_Risk_Control/) | → `requirements/05` + `architecture/05` |
| [06_Platform_Governance](06_Platform_Governance/) | → `requirements/06` + `architecture/06` |
| [07_Agent_Center](07_Agent_Center/) | → `requirements/07` + `architecture/07` |
| [08_Analytics_BI](08_Analytics_BI/) | → `requirements/08` + `architecture/08` |
| [09_Technical_Infrastructure](09_Technical_Infrastructure/) | → `requirements/09` + `architecture/09` |
| [10_Platform_Management](10_Platform_Management/) | → `requirements/10` + `architecture/10` |
| [11_Frontend_CMS](11_Frontend_CMS/) | → `requirements/11` + `architecture/11` |
| [12_System_Security](12_System_Security/) | → `requirements/12` + `architecture/12` |
| [13_Customer_Service](13_Customer_Service/) | → `requirements/13` + `architecture/13` |
| [14_Third_Party_Integration](14_Third_Party_Integration/) | → `requirements/14` + `architecture/14` |
| [15_Responsible_Gambling](15_Responsible_Gambling/) | → `requirements/15` + `architecture/15` |
| [adr](adr/) | → `architecture/adr/` |

---

**狀態**: 已歸檔（2026-02-09）— 唯讀，不接受更新
