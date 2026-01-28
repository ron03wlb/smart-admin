# 06-01 代理系統設計 (Affiliate System Design)

## 1. 系統概述
代理系統 (Affiliate/Agent System) 是 iGaming 平台獲客的核心。
本系統支援 **無限層級 (Unlimited Levels)** 的代理模式，以及基於 **商戶 (Tenant)** 的獨立代理體系。

## 2. 核心功能需求

### 2.1 代理層級與關係 (Hierarchy)
- **綁定關係**：
  - 透過推廣連結 (Referral Link) 或邀請碼 (Code) 綁定玩家。
  - 綁定關係永久有效，或設定 "保護期" (如 30 天)。
- **層級結構**：
  - 總代 (Master Agent) -> 一級代理 -> 二級代理 -> ... -> 會員。
  - 支援 **佔成 (Credit/Position Taking)** 與 **單純佣金 (Commission)** 兩種模式。

### 2.2 佣金計算 (Commission Calculation)
- **佣金計畫 (Commission Plan)**：
  - **輸贏分潤 (Revenue Share)**：基於淨輸贏 (Net Win = Bet - Win - Bonus - Fee)。
    - 例：淨輸贏 < 10萬 (30%)、10萬~50萬 (40%)、> 50萬 (50%)。
  - **流水佣金 (Turnover Rebate)**：基於有效投注額 (Valid Bet)。
    - 例：百家樂流水 0.8%、老虎機流水 1.0%。
  - **CPA (Cost Per Acquisition)**：每帶來一個首存 ($50+) 的活躍會員，獎勵 $100。
- **結算週期**：
  - 日結 (每日 02:00)
  - 週結 (每週一)
  - 月結 (每月 1 號)

### 2.3 調整錢包與遲結算 (Adjustment Wallet)
解決 "跨週期注單" 或 "歷史帳務修正" 的問題。
- **原則**：已結算 (Settled) 的歷史報表 **不可修改** (Immutable)。
- **機制**：
  - 若 1月 的注單在 2/5 發生 Rollback。
  - 系統 **不修改** 1月報表。
  - 系統在 2月 報表中新增一筆 `Type=ADJUSTMENT` 的負數金額。
- **公式**：
  `本期應付 = 本期產生佣金 + 上期結轉餘額 (Carried Over) + 人工/系統調整項`


### 2.3 代理後台 (Agent Portal)
- **獨立登入入口**，與玩家前台分離。
- **儀表板 (Dashboard)**：顯示今日新增會員、今日佣金、活躍人數。
- **推廣工具**：生成推廣連結、下載 Banner 素材。
- **報表**：下級會員報表、輸贏報表。

## 3. 業務規則與審批
- **佣金發放**：
  - 系統自動計算佣金報表 (Pending)。
  - **審批流程**：財務人員複核佣金數據 (排除套利與異常) -> 主管批准 -> 發放至代理錢包。
- **負盈利結轉 (Negative Carryover)**：
  - **定義**：當月 `Net Win` 為負 (玩家大贏)，導致代理佣金為負數時，該負值需結轉至下月扣除。
  - **公式**：
    `Carryover_Next = Min(0, Current_Commission + Carryover_Prev)`
  - **歸零機制 (Reset Rules)**：
    - **Threshold Reset**: 若負值 < -$1,000,000 (視配置)，平台吸收 50% 以避免代理流失。
    - **Time Reset**: 每年 1/1 自動歸零 (可選，通常用於激勵新年度推廣)。
    - **Active Activity**: 若代理連續 3 個月無新增活躍玩家，停止歸零福利。

## 4. 風控
- **同 IP 偵測**：代理與其下線玩家使用相同 IP，標記為異常 (可能是代理自己刷佣金)。

---

**文檔版本**: 1.0.0
**最後更新**: 2026-01-28
**維護團隊**: Product Team & Backend Team
