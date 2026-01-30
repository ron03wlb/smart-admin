# PostgreSQL Best Practices - Quick Reference

**快速開始**: 在對話中說 "analyze database performance" 或使用 `/postgres-analyze`

---

## 🚀 快速觸發

```
"database performance"
"PostgreSQL optimization"
"HikariCP tuning"
"N+1 query detection"
```

---

## 📊 核心功能（4 種）

| 功能 | 觸發命令 | 輸出 |
|------|---------|------|
| **HikariCP 分析** | `--mode hikaricp-only` | 連接池調優建議 |
| **N+1 檢測** | `--mode n1-detection` | N+1 模式清單 |
| **EXPLAIN ANALYZE** | `--mode explain-analyze` | 查詢執行計劃 |
| **索引建議** | `--mode index-recommendations` | CREATE INDEX 語句 |

---

## 💡 常見場景

### 場景 1: 應用響應緩慢
```
使用者：應用最近變慢了
AI：[自動觸發完整分析]
結果：HikariCP + N+1 + 索引建議
```

### 場景 2: 連接池超時
```
使用者：經常出現 "Connection timeout"
AI：[觸發 HikariCP 分析]
結果：max-pool-size: 10 → 20
```

### 場景 3: 特定頁面慢
```
使用者：員工列表頁面要 5 秒
AI：[觸發 N+1 檢測]
結果：發現 EmployeeService N+1 查詢
```

---

## 📖 完整文檔

詳見 [SKILL.md](SKILL.md)

---

## 🔧 依賴配置

```kotlin
// build.gradle.kts
dependencies {
    runtimeOnly("com.p6spy:p6spy:3.9.1")
    testImplementation("org.testcontainers:postgresql:1.19.3")
}
```

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:p6spy:postgresql://localhost:5432/smartadmin
    driver-class-name: com.p6spy.engine.spy.P6SpyDriver
```

---

**版本**: 1.0.0 | **狀態**: Stable | **優先級**: P2
