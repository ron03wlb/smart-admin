# SmartAdmin Manager Extractor - Quick Reference

**快速開始**: `/manager-extract EmployeeService`

---

## 🚀 核心價值

**時間節省**: 30 分鐘 → 5 分鐘 (**83% 改善**)

---

## 📝 快速命令

```bash
# 提取所有 @Transactional 方法
/manager-extract EmployeeService

# 提取特定方法
/manager-extract EmployeeService --method saveEmployee

# 預覽變更
/manager-extract EmployeeService --dry-run
```

---

## ✅ 自動處理

- ✅ JavaParser AST 解析
- ✅ Manager 類自動生成
- ✅ 依賴注入自動更新
- ✅ 三重驗證（Compile + ArchUnit + Tests）
- ✅ 失敗自動回滾

---

## 🔧 修復的 ArchUnit 違規

- `transactionalMustUseRollbackForThrowable`
- `@Transactional must be in Manager layer`
- `serviceMustNotUseTransactional`

---

## 📖 完整文檔

詳見 [SKILL.md](SKILL.md)

---

**版本**: 1.0.0 | **狀態**: Stable | **優先級**: P2
