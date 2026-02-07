# 12_System_Security - 系統安全

> **模塊定位**: 安全架構與數據保護
> **最後更新**: 2026-02-07

---

## 📋 模塊職責

數據安全標準、加密策略、盲索引、GDPR 合規。

**核心功能**：
- 數據安全標準
- 加密策略（傳輸加密、存儲加密）
- 盲索引架構（可搜索加密）
- GDPR 數據刪除

**職責邊界**：
- ✅ 包含：數據加密、隱私保護、合規
- ❌ 不包含：應用安全（09_Technical_Infrastructure）、權限管理（06_Platform_Governance）

---

## 📂 文檔清單

| 編號 | 文檔名稱 | 主題 | 優先級 |
|------|---------|------|--------|
| 12-03 | [Data_Security_Standard.md](12-03_Data_Security_Standard.md) | 數據安全標準總覽 | P0 |
| 12-03-01 | [Encryption_Strategy.md](12-03-01_Encryption_Strategy.md) | 加密策略 | P0 |
| 12-03-02 | [Blind_Index_Architecture.md](12-03-02_Blind_Index_Architecture.md) | 盲索引架構 | P1 |
| 12-03-03 | [GDPR_Data_Deletion.md](12-03-03_GDPR_Data_Deletion.md) | GDPR 數據刪除 | P0 |

---

## 🔗 核心依賴

```text
12_System_Security
    ↓
    ├─► 06_Platform_Governance (06-05) - 數據安全治理
    ├─► 01_Player_Center - 玩家 PII 保護
    ├─► 02_Finance_Center - 財務數據保護
    └─► 09_Technical_Infrastructure - 基礎設施安全
```

---

## 🔐 安全分類

| 數據類型 | 加密要求 | 存儲要求 |
|---------|---------|---------|
| PII（個人身份信息）| AES-256-GCM | 加密存儲 + 盲索引 |
| 財務數據 | AES-256-GCM | 加密存儲 |
| 密碼 | Argon2id | Hash + Salt |
| Session | JWT + Redis | 短期存儲 |

---

## 🔑 SSOT 定義

| 概念 | 文檔 | 章節 |
|------|------|------|
| 加密算法標準 | 12-03-01 Encryption_Strategy | §2.1 |
| 盲索引設計 | 12-03-02 Blind_Index | §3.1 |
| GDPR 刪除流程 | 12-03-03 GDPR_Data_Deletion | §2.1 |

---

**索引版本**: 1.0.0
**創建日期**: 2026-02-07
**維護團隊**: Security Team
