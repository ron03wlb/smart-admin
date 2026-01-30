# Fraud Detection Pattern Generator

> iGaming 反欺詐系統：KYC/AML 合規 + 風控規則引擎 + 黑名單管理

## 🚀 快速開始

```bash
# 場景：需要實現註冊風控
User: "Implement KYC verification for user registration"

# 自動生成：
# 1. KYC 驗證流程（身份證、手機號、銀行卡）
# 2. AML 反洗錢規則（交易異常檢測）
# 3. 風控規則引擎（LiteFlow 集成）
# 4. 黑名單管理（IP、設備指紋、用戶ID）
```

## 核心功能

### 1. KYC（Know Your Customer）

**身份驗證**：
- 身份證實名認證（OCR + 活體檢測）
- 手機號實名認證（運營商三要素）
- 銀行卡實名認證（銀行四要素）

### 2. AML（Anti-Money Laundering）

**反洗錢規則**：
- 大額交易監控（單筆 > 10萬）
- 異常頻繁交易（24小時內 > 10筆）
- 分散轉賬檢測（拆分大額避監管）

### 3. 風控規則引擎（LiteFlow）

**規則配置**：
```java
// 註冊風控規則
THEN(
    checkBlacklist,      // 檢查黑名單
    checkDeviceRisk,     // 設備指紋風險評分
    checkIPRisk,         // IP 風險評分
    checkKYC             // KYC 驗證
);
```

### 4. 黑名單管理

**黑名單類型**：
- IP 黑名單（惡意 IP、代理 IP）
- 設備指紋黑名單（多賬戶關聯）
- 用戶 ID 黑名單（作弊用戶）

## 使用場景

### ✅ When to Use

1. **註冊風控** - 防止機器人、多賬戶
2. **交易風控** - 異常存提款、洗錢檢測
3. **遊戲風控** - 套利、作弊檢測

### ❌ When NOT to Use

- 低風險業務（內部系統）
- 非金融平台

## 常見問題

### Q1: 如何設計風控規則？

**A**: 使用 LiteFlow DSL：
```java
THEN(
    IF(isBlacklisted, THEN(reject)),
    IF(riskScoreHigh, THEN(manualReview)),
    ELSE(approve)
);
```

### Q2: 如何處理誤報和漏報？

**A**:
- 誤報：調整風險閾值、白名單機制
- 漏報：增強規則、機器學習模型

---

**Version**: 1.0.0
**Last Updated**: 2026-01-30
