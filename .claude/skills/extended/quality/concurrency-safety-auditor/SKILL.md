---
name: concurrency-safety-auditor
description: [P1 - Extended] Concurrency safety audit with SpotBugs pattern detection and ⭐⭐⭐⭐⭐ risk rating system (8 patterns check-then-act, double-checked locking, etc.). Use when auditing thread safety, detecting race conditions, or analyzing concurrency issues.
---

# Concurrency Safety Auditor Skill

**Version**: 1.0.0
**Priority**: P1 (Extended/Quality)
**Category**: Quality
**Status**: Stable

---

## 概述

全面的並發安全審計工具，使用 SpotBugs 自定義檢測器和風險評級系統（⭐⭐⭐⭐⭐ 評級）。

---

## 核心功能

| 功能 | 描述 |
|------|------|
| **8 種模式檢測** | Check-then-act, Double-checked locking, 等 |
| **風險評級** | ⭐⭐⭐⭐⭐ 系統（Probability × Impact × Actual Harm） |
| **修復建議** | 自動生成修復代碼範例 |
| **SmartAdmin 基線** | 對比已知安全模式 |

---

## Related Skills & Boundaries

**互補 Skills**:
- `spring-pattern-checker` - 驗證 Spring 特定模式（@Transactional 位置、依賴注入）
- `archunit-test-generator` - 生成架構強制測試

**邊界澄清**:
- **本 skill 專注**: 並發原語（鎖、原子操作、執行緒安全）
- **spring-pattern-checker 專注**: Spring 框架模式（分層、註解）
- **重疊部分**: 兩者都檢查 @Transactional 使用，但角度不同：
  - concurrency-safety-auditor: 交易方法的執行緒安全性
  - spring-pattern-checker: 正確的層級放置（Manager vs Service）

---

## 觸發方式

```bash
# 完整審計
/concurrency-audit

# 指定包
/concurrency-audit --package net.lab1024.sa.base

# 僅高嚴重性
/concurrency-audit --severity HIGH
```

## Trigger Keywords

This skill is automatically activated when the user's request contains:

**Primary Keywords** (High confidence):
- "concurrency" - Concurrency safety audit and analysis
- "thread safety" - Thread-safe code validation
- "race condition" - Race condition detection
- "concurrency audit" - Explicit audit invocation
- "thread-safe audit" - Comprehensive thread safety audit

**Secondary Keywords** (Medium confidence):
- "deadlock detection" - Context: deadlock pattern detection
- "concurrent access" - Context: concurrent modification issues
- "synchronization issues" - Context: sync pattern validation
- "check-then-act" - Context: non-atomic operation detection
- "double-checked locking" - Context: DCL pattern validation
- "ConcurrentHashMap misuse" - Context: concurrent collection issues

**Phrase Patterns**:
- "Audit [component] for concurrency issues" - Example: "Audit Manager layer for concurrency issues"
- "Detect [concurrency pattern]" - Example: "Detect race conditions in wallet operations"
- "Check thread safety of [class]" - Example: "Check thread safety of UserManager"

**Example User Requests**:
```
User: "Audit the codebase for concurrency issues"
User: "Detect race conditions in financial operations"
User: "Check thread safety of Manager layer classes"
User: "Analyze deadlock risks in wallet transactions"
User: "Run concurrency safety audit with HIGH severity only"
```

**Note**: This skill can also be manually invoked via `/concurrency-safety-auditor` or `/concurrency-audit` command. Supports severity filtering: `--severity HIGH|MEDIUM|LOW`.

---

## 風險評級系統

### 評級標準

| 評級 | 含義 | 標準 |
|------|------|------|
| ⭐⭐⭐⭐⭐ | 生產級 | 無已知問題 |
| ⭐⭐⭐⭐ | 良好 | 小改進可能 |
| ⭐⭐⭐ | 可接受 | 應該改進 |
| ⭐⭐ | 有問題 | 必須盡快修復 |
| ⭐ | 危險 | 立即修復 |

### 計算公式

```
Risk Score = (Probability × 0.4) + (Impact × 0.35) + (Actual Harm × 0.25)
```

---

## 檢測的 8 種並發模式

### 1. Check-Then-Act（⚠️ HIGH）

```java
// ❌ 問題
if (map.containsKey(key)) {  // Check
    map.put(key, value);     // Act (non-atomic)
}

// ✅ 修復
map.putIfAbsent(key, value);  // Atomic
```

### 2. Double-Checked Locking（🚨 CRITICAL）

```java
// ❌ 問題
if (instance == null) {
    synchronized (this) {
        if (instance == null) {
            instance = new Singleton();  // Not volatile
        }
    }
}

// ✅ 修復
private static volatile Singleton instance;  // Add volatile
```

### 3. Inconsistent Synchronization（🚨 CRITICAL）

```java
// ❌ 問題
private int count = 0;
public synchronized void increment() { count++; }
public int getCount() { return count; }  // Unsynchronized read

// ✅ 修復
public synchronized int getCount() { return count; }
```

---

## SmartAdmin 審計結果範例

### 生產級模式（⭐⭐⭐⭐⭐）

| 類 | 並發機制 | 評級 | 風險 |
|---|---------|------|------|
| RepeatSubmitMemoryTicket | Interner + synchronized | ⭐⭐⭐⭐⭐ | 無 |
| SerialNumberInternService | Interner + synchronized | ⭐⭐⭐⭐⭐ | 無 |

### 低風險問題（⭐⭐）

**SmartReloadManager.register()**
- **模式**: Check-then-act
- **問題**: `containsKey()` then `put()` - 非原子
- **發生概率**: <0.01%（僅啟動時，Spring 初始化單線程）
- **影響**: 單個 reload tag 覆蓋
- **實際危害**: 未觀察到
- **修復**:
```java
// 修改前
if (reloadObjectMap.containsKey(tag)) {
    log.error("Duplicate tag");
}
reloadObjectMap.put(tag, obj);

// 修改後
SmartReloadObject existing = reloadObjectMap.putIfAbsent(tag, obj);
if (existing != null) log.error("Duplicate tag");
```

---

## 輸出報告範例

```markdown
# Concurrency Safety Audit Report

**Generated**: 2026-01-29 23:50:00
**Package**: net.lab1024.sa
**Classes Analyzed**: 127

---

## 📊 Executive Summary

**Risk Rating Summary**:
- ⭐⭐⭐⭐⭐ 85 classes (67%)
- ⭐⭐⭐⭐ 35 classes (27%)
- ⭐⭐⭐ 5 classes (4%)
- ⭐⭐ 2 classes (2%)
- ⭐ 0 classes (0%)

**Overall Rating**: ⭐⭐⭐⭐ (4.6/5.0)

---

## 🔍 Pattern Detection Results

### ⚠️ Check-Then-Act (2 instances)

1. SmartReloadManager.register()
   - Risk: ⭐⭐ (Low)
   - Probability: 0.01%
   - Impact: Single tag overwrite
   - Fix: Use putIfAbsent()

2. SmartJobScheduler.addJob()
   - Risk: ⭐ (Very Low)
   - Probability: 0.001%
   - Impact: Acceptable
   - Fix: Use ConcurrentHashMap.putIfAbsent()

---

## ✅ Action Items (Prioritized)

1. [MEDIUM] SmartReloadManager.register() - Use putIfAbsent()
   - Time: 10 minutes
   - Risk reduction: ⭐⭐ → ⭐⭐⭐⭐⭐

2. [LOW] SmartJobScheduler.addJob() - Consider refactoring
   - Time: 5 minutes
   - Risk reduction: Minimal
```

---

## 依賴配置

```kotlin
// build.gradle.kts
configure<com.github.spotbugs.snom.SpotBugsExtension> {
    effort.set(com.github.spotbugs.snom.Effort.MAX)
    reportLevel.set(com.github.spotbugs.snom.Confidence.LOW)
}

dependencies {
    spotbugsPlugins("com.github.spotbugs:spotbugs-annotations:4.8.3")
}
```

---

## 參考資料

- [SmartReloadManager 修復範例](examples/smartreloadmanager-fix.md)
- [並發模式參考](references/concurrency-patterns.md)
- [SpotBugs 規則](rules/check-then-act-detector.xml)

---

**維護者**: SmartAdmin Skills Team
**最後更新**: 2026-01-29

---

## 相關規則

本技能直接關聯以下並發安全規範：

### 強制要求

- **[Concurrency Safety Rules](./../../../.agent/rules/technology/patterns/05-concurrency-safety.md)**
  - Check-then-act 模式檢測（Map.containsKey() + put()）
  - Double-checked locking 驗證
  - ConcurrentHashMap 誤用檢測（size() 用於條件判斷）
  - 共享可變狀態的執行緒安全要求

- **[SpotBugs Rules](./../../../.agent/rules/quality-tools/13-spotbugs-rules.md)**
  - SpotBugs 並發相關檢測規則配置
  - 自定義 Detector 實現標準
  - 報告格式與嚴重性級別

### 參考指引

- **[Java Concurrency in Practice](https://jcip.net/)** - 經典並發模式
  - 有效不可變性（Effectively Immutable）
  - 安全發布（Safe Publication）
  - 並發集合使用模式

- **[Architecture Rules](./../../../.agent/rules/foundation/10-architecture-rules.md)**
  - Manager 層事務管理（與並發安全相關）
  - Service 層無狀態要求（避免共享狀態）

---

## 參考資料

- [SmartReloadManager 修復範例](examples/smartreloadmanager-fix.md) - Check-then-act 實際案例
- [並發模式參考](references/concurrency-patterns.md) - 完整模式目錄
- [SpotBugs Custom Detectors](https://spotbugs.readthedocs.io/en/latest/implement-plugin.html) - 自定義規則實現
- [Java Memory Model](https://docs.oracle.com/javase/specs/jls/se21/html/jls-17.html#jls-17.4) - JVM 記憶體模型規範
