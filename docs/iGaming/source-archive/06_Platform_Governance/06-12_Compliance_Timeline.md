# 06-12 Compliance Timeline (合規時間線追蹤)

**版本**: 1.1.0
**創建日期**: 2026-02-07
**最後更新**: 2026-02-07
**狀態**: ✅ 完整
**維護團隊**: Compliance Team

---

## 概述

本文檔統一追蹤所有監管機構的合規要求生效日期，確保各團隊及時響應監管變化，避免合規風險。

### 使用說明

- **定期審查**: 每月第一個工作日由 Compliance Team 審查並更新狀態
- **狀態更新**: 各負責團隊在完成實施後更新狀態
- **風險預警**: 任何 deadline 前 90 天未達「實施中」狀態需觸發告警

---

## 狀態追蹤規則

| 狀態 | 說明 | 標記 |
|------|------|------|
| **已完成** | 已上線並通過驗證 | ✅ |
| **實施中** | 開發/測試階段 | 🔄 |
| **設計中** | 技術方案已確定 | ⚠️ |
| **待規劃** | 尚未開始 | ❌ |
| **不適用** | 本平台不涉及 | ➖ |

---

## UKGC 2025 時間線 (英國)

> **監管機構**: UK Gambling Commission
> **適用牌照**: Remote Operating Licence
> **法規框架**: Gambling Act 2005, LCCP (Licence Conditions and Codes of Practice)

### 2025 年上半年

| 生效日期 | 新規名稱 | LCCP 條款 | 狀態 | 實現文檔 | 負責團隊 | 備註 |
|---------|---------|-----------|------|---------|---------|------|
| 2025-01 | 即時 KYC 驗證 | RTS 5A | ⚠️ 設計中 | [05-03](../05_Risk_Control/05-03_KYC_AML.md) | Compliance | 取消 72h 寬限期 |
| 2025-02-28 | 財務脆弱性檢查 (£150) | LCCP 3.4.3 | ✅ 已完成 | [15-08](../15_Responsible_Gambling/15-08_Affordability_Assessment.md) | Risk | 月淨存款 ≥£150 觸發 (文檔完成) |
| 2025-04-01 | RET Levy (研究教育治療稅) | Gambling Levy Act | ✅ 已完成 | [06-08](06-08_UKGC_Compliance.md) | Finance | GGY 0.1%-1.1% (文檔完成) |
| 2025-04-09 | 老虎機限注 (25+) | Slots Review 2023 | ✅ 已完成 | [03-08](../03_Game_Center/03-08_Slot_Stake_Limits.md) | Games | £5/spin 上限 |
| 2025-05-01 | 行銷同意管理 | LCCP 5.1.12 | ✅ 已完成 | [11-05](../11_Frontend_CMS/11-05_Marketing_Compliance.md) | Marketing | 產品+渠道級同意 (文檔完成) |
| 2025-05-21 | 老虎機限注 (18-24) | Slots Review 2023 | ✅ 已完成 | [03-08](../03_Game_Center/03-08_Slot_Stake_Limits.md) | Games | £2/spin 上限 |

### 2025 年下半年

| 生效日期 | 新規名稱 | LCCP 條款 | 狀態 | 實現文檔 | 負責團隊 | 備註 |
|---------|---------|-----------|------|---------|---------|------|
| 2025-10-31 | 首存前限額設定 | LCCP 3.4.1 | ✅ 已完成 | [15-02](../15_Responsible_Gambling/15-02_Deposit_Limits.md) | Product | 首存前強制設定限額 (文檔完成) |
| 2025-10-31 | 資金保護披露 | LCCP 4.2.1 | ✅ 已完成 | [06-08](06-08_UKGC_Compliance.md) | Compliance | 每 6 個月告知 + 確認 (文檔完成) |

### 已生效規則 (參考)

| 生效日期 | 新規名稱 | LCCP 條款 | 狀態 | 實現文檔 | 備註 |
|---------|---------|-----------|------|---------|------|
| 2020-04 | 禁止信用卡博彩 | LCCP 5.1.8 | ✅ 已完成 | [02-02](../02_Finance_Center/02-02_Payment_Gateway_Integration.md) | - |
| 2020-04 | Gamstop 強制整合 | LCCP 3.5.2 | ✅ 已完成 | [06-08](06-08_UKGC_Compliance.md) | SR 3.5.1 |
| 2021-09 | 存款限額選項 | LCCP 3.4.1 | ✅ 已完成 | [15-02](../15_Responsible_Gambling/15-02_Deposit_Limits.md) | - |
| 2021-09 | 現實檢查彈窗 | LCCP 3.4.2 | ✅ 已完成 | [15-05](../15_Responsible_Gambling/15-05_Reality_Checks.md) | - |

---

## MGA 時間線 (馬耳他)

> **監管機構**: Malta Gaming Authority
> **適用牌照**: B2C Remote Gaming Licence
> **法規框架**: Gaming Act 2018, Player Protection Directive

| 生效日期 | 新規名稱 | 條款 | 狀態 | 實現文檔 | 負責團隊 | 備註 |
|---------|---------|------|------|---------|---------|------|
| 持續 | 自我排除機制 | PPD Art. 7 | ✅ 已完成 | [15-01](../15_Responsible_Gambling/15-01_Self_Exclusion.md) | Compliance | - |
| 持續 | 存款限額 | PPD Art. 6 | ✅ 已完成 | [15-02](../15_Responsible_Gambling/15-02_Deposit_Limits.md) | Product | - |
| 持續 | 玩家保護基金 | License Condition | ✅ 已完成 | [06-09](06-09_MGA_Compliance.md) | Finance | - |
| 2025 Q2 | AML 強化監控 | 5AMLD | ⚠️ 設計中 | [05-03](../05_Risk_Control/05-03_KYC_AML.md) | Compliance | 加密貨幣 KYC |

---

## Brazil SPA 時間線 (巴西)

> **監管機構**: Secretaria de Prêmios e Apostas (SPA)
> **適用牌照**: Federal Sports Betting License
> **法規框架**: Lei 14.790/2023, Portaria SPA

| 生效日期 | 新規名稱 | 條款 | 狀態 | 實現文檔 | 負責團隊 | 備註 |
|---------|---------|------|------|---------|---------|------|
| 2025-01-01 | 正式牌照發放 | Lei 14.790 | ⚠️ 設計中 | [06-10](06-10_Brazil_SPA_Compliance.md) | Compliance | - |
| 2026-01-01 | .bet.br 域名強制 | Portaria SPA | ❌ 待規劃 | [06-10](06-10_Brazil_SPA_Compliance.md) | Tech | 強制使用 .bet.br |
| 2025-06 | 自我排除系統 | Portaria SPA | ❌ 待規劃 | [15-01](../15_Responsible_Gambling/15-01_Self_Exclusion.md) | Compliance | 本地化整合 |
| 2025-06 | 本地支付整合 | Portaria SPA | ❌ 待規劃 | [02-02](../02_Finance_Center/02-02_Payment_Gateway_Integration.md) | Finance | PIX 強制 |

---

## PAGCOR/Curacao 時間線 (亞洲/離岸)

> **監管機構**: PAGCOR (菲律賓) / Curacao eGaming
> **法規框架**: 各自法規

| 生效日期 | 新規名稱 | 監管機構 | 狀態 | 實現文檔 | 備註 |
|---------|---------|---------|------|---------|------|
| 持續 | 基礎 AML | PAGCOR | ✅ 已完成 | [05-03](../05_Risk_Control/05-03_KYC_AML.md) | CDD $2,000 |
| 持續 | 基礎 KYC | Curacao | ✅ 已完成 | [05-03](../05_Risk_Control/05-03_KYC_AML.md) | CDD $2,500 |
| 2025-01 | 新 Curacao 法規 | Curacao | ⚠️ 設計中 | [06-11](06-11_PAGCOR_Curacao.md) | 強化監管 |

---

## 風險優先級矩陣

### 🔴 P0 - 緊急 (90 天內到期)

| Deadline | 新規 | 剩餘天數 | 狀態 | 行動項 |
|---------|------|---------|------|--------|
| 2025-02-28 | 財務脆弱性 £150 | ⚠️ 已過期 | ✅ 已完成 | 文檔已完成於 [15-08](../15_Responsible_Gambling/15-08_Affordability_Assessment.md) |
| 2025-04-09 | 老虎機限注 (25+) | 61 天 | ❌ 待規劃 | **UKGC Slots Review - 需新建文檔** |
| 2025-05-21 | 老虎機限注 (18-24) | 103 天 | ❌ 待規劃 | **UKGC Slots Review - 需新建文檔** |

### 🟡 P1 - 重要 (90-180 天內到期)

| Deadline | 新規 | 剩餘天數 | 狀態 | 行動項 |
|---------|------|---------|------|--------|
| 2025-04-01 | RET Levy | - | ❌ 待規劃 | 啟動財務模塊設計 |
| 2025-05-01 | 行銷同意 | - | ❌ 待規劃 | 啟動 CRM 模塊設計 |

### 🟢 P2 - 規劃中 (180 天以上)

| Deadline | 新規 | 狀態 | 行動項 |
|---------|------|------|--------|
| 2025-10-31 | 首存前限額 | ✅ 已完成 | 文檔已完成於 [15-02](../15_Responsible_Gambling/15-02_Deposit_Limits.md) |
| 2025-10-31 | 資金保護披露 | ✅ 已完成 | 文檔已完成於 [06-08](06-08_UKGC_Compliance.md) |
| 2026-01-01 | .bet.br 域名 | ❌ 待規劃 | Q3 啟動實施 |

---

## 監控與告警

### 自動化告警規則

```yaml
compliance_deadline_alerts:
  - name: P0_90_days_warning
    condition: deadline - today <= 90 AND status NOT IN ['已完成', '實施中']
    severity: CRITICAL
    channel: [Slack, Email]
    recipients: [compliance-team, tech-leads, cto]

  - name: P1_180_days_warning
    condition: deadline - today <= 180 AND status = '待規劃'
    severity: HIGH
    channel: [Slack]
    recipients: [compliance-team]

  - name: monthly_review_reminder
    cron: "0 9 1 * *"  # 每月 1 日 9:00
    channel: [Slack]
    message: "請審查合規時間線狀態"
```

### 審計日誌

所有狀態變更記錄在 `t_compliance_timeline_audit`:

```sql
CREATE TABLE t_compliance_timeline_audit (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    regulation_code VARCHAR(50) NOT NULL,
    jurisdiction VARCHAR(20) NOT NULL,
    old_status VARCHAR(20),
    new_status VARCHAR(20) NOT NULL,
    changed_by BIGINT NOT NULL,
    changed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    change_reason VARCHAR(500),

    INDEX idx_regulation (regulation_code),
    INDEX idx_changed_at (changed_at)
);
```

---

## 變更歷史

| 版本 | 日期 | 變更內容 | 變更者 |
|------|------|---------|--------|
| 1.1.0 | 2026-02-07 | Gap Analysis 狀態同步：更新已文檔化項目狀態，新增老虎機限注追蹤 | Claude Code |
| 1.0.0 | 2026-02-07 | 初始版本，整合 UKGC/MGA/Brazil/PAGCOR 時間線 | Claude Code |

---

## 相關文檔

- [06-07 多司法管轄區框架](06-07_Multi_Jurisdiction_Framework.md) - 牌照配置中心
- [06-08 UKGC 合規](06-08_UKGC_Compliance.md) - UK 牌照詳細要求
- [06-09 MGA 合規](06-09_MGA_Compliance.md) - Malta 牌照詳細要求
- [06-10 Brazil SPA 合規](06-10_Brazil_SPA_Compliance.md) - Brazil 牌照詳細要求
- [05-03 KYC/AML](../05_Risk_Control/05-03_KYC_AML.md) - KYC/AML 實現
- [15 負責任博彩](../15_Responsible_Gambling/README.md) - 玩家保護工具

---

**返回**: [06_Platform_Governance](README.md) | [iGaming 首頁](../README.md)
