# Concurrency Safety Auditor - Quick Reference

**快速開始**: `/concurrency-audit`

---

## 🔍 核心功能

- 8 種並發模式檢測
- ⭐⭐⭐⭐⭐ 風險評級系統
- 自動修復建議

---

## 📊 風險評級

| 評級 | 含義 |
|------|------|
| ⭐⭐⭐⭐⭐ | 生產級（無問題） |
| ⭐⭐⭐⭐ | 良好（小改進） |
| ⭐⭐⭐ | 可接受（應改進） |
| ⭐⭐ | 有問題（必須修復） |
| ⭐ | 危險（立即修復） |

---

## 🚀 快速命令

```bash
# 完整審計
/concurrency-audit

# 指定包
/concurrency-audit --package net.lab1024.sa.base

# 高嚴重性only
/concurrency-audit --severity HIGH
```

---

## 📖 完整文檔

詳見 [SKILL.md](SKILL.md)

---

**版本**: 1.0.0 | **狀態**: Stable | **優先級**: P1
