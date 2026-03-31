# iGaming 文檔重整品質報告（2026 Q1）

> **報告類型**: 文檔重整完成報告（非例行品質閘門）
> **執行日期**: 2026-03-31
> **執行範圍**: `docs/iGaming/` 全目錄
> **執行依據**: [STANDARDS.md](../STANDARDS.md)
> **狀態**: ✅ 完成

---

## 執行摘要

本次重整依據 [STANDARDS.md](../STANDARDS.md) 的 SSOT 原則和受眾分層規範，對 `docs/iGaming/` 目錄進行了系統性整理。

### 重整前後對比

| 指標 | 整理前 | 整理後 |
|------|--------|--------|
| **總文件數（requirements + architecture）** | ~185 | ~163 |
| **命名衝突（07 重複）** | 1 | 0 |
| **未標記重複/分散** | 5+ 處 | 0（全改為 XREF） |
| **邏輯矛盾** | 至少 4 處 | 0 |
| **有 README 的模組** | 部分缺失 | 全部完整 |
| **跨模組整合指南** | 無 | 有 |
| **文檔規範 SSOT** | 無 | 有（STANDARDS.md） |

---

## 已解決問題清單

### 1. 命名衝突修復 ✅

**問題**: `requirements/07_Metrics_KPIs/` 與 `requirements/07_Agent_Operations/` 號碼重複

**解決方案**: 將 `07_Metrics_KPIs` 重命名為 `16_Metrics_KPIs`（置於 15 之後）

**影響文件**:
- `requirements/README.md` — 更新導航表
- `docs/iGaming/README.md` — 更新目錄結構

---

### 2. 缺失模組補完 ✅

**問題**: `16_Metrics_KPIs/` 只有空 README，無實際內容

**新建文件**:
- `requirements/16_Metrics_KPIs/01_KPI_Definitions.md` — GGR/NGR、DAU/MAU、Retention、Valid Turnover 等核心 KPI 定義
- `requirements/16_Metrics_KPIs/02_Business_Metrics.md` — 北極星指標、業務指標框架、監控閾值、報告節奏

---

### 3. MFA 文件合併（8→4）✅

**問題**: `architecture/06_Platform_Core/` 有 8 個 MFA 文件，內容碎片化

**合併結果**:

| 合併前（8 個文件） | 合併後（4 個文件） |
|-----------------|-----------------|
| 03_MFA_Technical_Architecture.md | **03_MFA_Architecture.md**（架構 + 方法選擇評估） |
| 05_MFA_Technical_Evaluation.md | ↑ |
| 04_MFA_Technical.md | **04_MFA_Implementation.md**（TOTP + WebAuthn 實作） |
| 06_TOTP_WebAuthn_Implementation.md | ↑ |
| 07_MFA_Compliance_Technical.md | **05_MFA_Compliance.md**（稽核日誌 + 備份碼加密） |
| 08_MFA_Compliance_Validation.md | ↑ |
| 09_MFA_Recovery_Implementation.md | **06_MFA_Recovery.md**（恢復流程 + 信任裝置） |
| 10_MFA_Login_Recovery_Technical.md | ↑ |

同時將 `11_Jurisdiction_Routing_Architecture.md` 重命名為 `07_Jurisdiction_Routing_Architecture.md`。

---

### 4. Token 文件合併（6→2）✅

**問題**: `architecture/09_Infrastructure/` 有 6 個 Token 相關文件，功能重疊

**合併結果**:

| 合併前（6 個文件） | 合併後（2 個文件） |
|-----------------|-----------------|
| 17_Multi_Actor_Token_Security.md | **17_Token_Security_Architecture.md** |
| 18_OAuth_Refresh_Token.md | ↑ |
| 19_Token_Validation_Architecture.md | ↑ |
| 20_Token_Validation_Service.md | **18_Token_Operations.md** |
| 21_Token_Cache_Performance.md | ↑ |
| 22_Token_Edge_Deployment.md | ↑ |

---

### 5. Turnover 文件合併（4→2）✅

**問題**: `architecture/02_Finance_Service/` 有 4 個有效投注額相關文件，重複度高

**合併結果**:

| 合併前（4 個文件） | 合併後（2 個文件） |
|-----------------|-----------------|
| 08_Turnover_Calculation_Architecture.md | **08_Turnover_Architecture.md**（架構 + 業務邏輯公式） |
| 09_Turnover_Calculation_Logic_Detail.md | ↑ |
| 10_Turnover_Implementation.md | **09_Turnover_Implementation.md**（實作 + 流程圖） |
| 11_Turnover_Flowcharts.md | ↑ |

---

### 6. 跨模組整合指南新建 ✅

**問題**: 無文件描述 Risk Engine ↔ Finance Service ↔ Activity Engine 之間的互動

**新建文件**: `architecture/00_Overview/06_Cross_Module_Integration.md`

**涵蓋內容**:
- 模組依賴矩陣（5×6）
- 5 個核心整合流程（存款、投注、紅利、提款、封鎖玩家）
- Kafka 事件主題表（8 個事件）
- 跨模組共識規則（多租戶、冪等性、熔斷器）
- SAGA 分散式事務補償策略

---

### 7. source-archive README 更新 ✅

**問題**: source-archive 目錄缺乏清晰說明，讀者不清楚「Center」vs「Service」命名差異

**更新內容**:
- 明確標示「唯讀歸檔」政策
- 解釋 v4.1.0 Center→Service 命名演進歷史
- 15 個模組的遷移對照表

---

### 8. Affordability 需求矛盾解決 ✅

**問題**: `requirements/05_Risk_Compliance/09_Affordability_Requirements.md` 與 `requirements/15_Responsible_Gambling/04_Affordability_Requirements.md` 規則可能不一致

**解決方案**:
- `05_Risk_Compliance/09` 確立為 SSOT（風控主管業務）
- `15_Responsible_Gambling/04` 添加 XREF 指向 SSOT

---

### 9. Player Protection 重複解決 ✅

**問題**: Risk Engine 和 Responsible Gambling 兩處有 Player Protection API 文件

**解決方案**: 明確職責邊界
- Risk Engine（觸發機制、評分計算）
- RG（執行策略、玩家保護措施）
- 兩份文件互加 XREF 參照

---

### 10. 一致性驗證（[18][19][20]）✅

| 驗證項目 | 需求文件 | 架構文件 | 結果 |
|---------|---------|---------|------|
| **Turnover 業務規則** | 03_Gaming_Operations/01_Turnover_Business_Rules.md | 02_Finance_Service/08_Turnover_Architecture.md | ✅ 一致（3 層驗證、Standard Principal Method） |
| **KYC 等級定義** | 05_Risk_Compliance/03_KYC_AML_Requirements.md | 05_Risk_Engine/06_KYC_Verification_API.md | ✅ 一致（L0-L3、升級觸發條件、KPI 指標） |
| **玩家生命週期狀態** | 01_Player_Experience/04_Player_Lifecycle.md | 01_Player_Service/01_Player_Lifecycle_Implementation.md | ✅ 一致（5 狀態機、4 生命週期階段） |

---

## README 更新清單

| 文件 | 更新內容 |
|------|---------|
| `docs/iGaming/README.md` | 更新文件數量、加入 16_Metrics_KPIs、更新類別統計 |
| `docs/iGaming/requirements/README.md` | 加入 16_Metrics_KPIs 導航項目 |
| `docs/iGaming/architecture/README.md` | 已有完整導航（無需大幅修改） |
| `docs/iGaming/architecture/06_Platform_Core/README.md` | 重寫（反映 MFA 8→4 合併） |
| `docs/iGaming/architecture/09_Infrastructure/README.md` | 更新（Token 合併後） |
| `docs/iGaming/architecture/02_Finance_Service/README.md` | 重寫（反映 Turnover 合併） |
| `docs/iGaming/source-archive/README.md` | 重寫（唯讀政策 + 命名歷史） |

---

## 新建文件清單

| 文件 | 類型 | 說明 |
|------|------|------|
| `docs/iGaming/STANDARDS.md` | 規範 | 文檔規範 SSOT（SSOT 原則、命名、XREF 格式） |
| `docs/iGaming/AUDIT_ISSUES.md` | 臨時 | 整理過程工作文件（問題清單、進度追蹤） |
| `requirements/16_Metrics_KPIs/01_KPI_Definitions.md` | 需求 | 核心 KPI 定義 |
| `requirements/16_Metrics_KPIs/02_Business_Metrics.md` | 需求 | 業務指標框架 |
| `architecture/00_Overview/06_Cross_Module_Integration.md` | 架構 | 跨模組整合指南 |
| `architecture/06_Platform_Core/03_MFA_Architecture.md` | 架構 | MFA 架構（合併） |
| `architecture/06_Platform_Core/04_MFA_Implementation.md` | 架構 | MFA 實作（合併） |
| `architecture/06_Platform_Core/05_MFA_Compliance.md` | 架構 | MFA 合規（合併） |
| `architecture/06_Platform_Core/06_MFA_Recovery.md` | 架構 | MFA 恢復（合併） |
| `quality-reports/2026-Q1-docs-restructure-report.md` | 報告 | 本文件 |

---

## 剩餘待辦事項

| 項目 | 優先級 | 說明 |
|------|--------|------|
| `AUDIT_ISSUES.md` 可在下一版本清除 | P2 | 臨時工作文件，整理完成後可刪除 |
| 例行品質閘門（2026-Q2） | P1 | 按 quality-reports/README.md 流程執行 |
| `scripts/validate-file-numbering.sh` 例外清單持續維護 | P2 | 新增非數字命名文件時需更新 |

---

**報告版本**: 1.0.0
**執行者**: Claude Code（自動化重整）
**審核**: 待人工確認
