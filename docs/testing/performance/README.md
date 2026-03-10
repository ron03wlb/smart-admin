# SmartAdmin iGaming 性能測試套件

**測試工具**: k6 (現代化負載測試工具)
**版本**: 1.0.0
**最後更新**: 2026-03-10

---

## 📁 文件清單

| 文件名 | 描述 | 用途 |
|--------|------|------|
| **k6-wallet-concurrency-test.js** | k6 測試腳本 | 並發錢包扣款負載測試（1000 VUs） |
| **K6_WALLET_PERFORMANCE_TEST_GUIDE.md** | 執行指南 | 完整的測試執行、結果分析和故障排查指南 |
| **README.md** | 本文件 | 性能測試套件總覽 |

---

## 🎯 測試目標

### 功能驗證
- ✅ 三層防護機制（Redisson 鎖 + 樂觀鎖 + Request ID 去重）
- ✅ 錢包餘額一致性（無重複扣款）
- ✅ 冪等性保證（相同 requestId 返回原結果）

### 性能指標（iGaming 實施計劃驗收標準）
- ✅ **TPS**: > 500 requests/second
- ✅ **P95 響應時間**: < 100ms
- ✅ **錯誤率**: < 0.1%
- ✅ **重複扣款**: 0 次（🔴 關鍵）

---

## 🚀 快速開始

### 1. 安裝 k6

```bash
# macOS
brew install k6

# Windows
choco install k6

# Linux
sudo apt-get install k6
```

### 2. 準備測試環境

```bash
# 啟動 SmartAdmin 應用程序
cd smart-admin-api-java21-springboot3
./gradlew :smartadmin-app:bootRun

# 創建測試數據（100 個錢包，每個 10,000 CNY）
psql -h localhost -U smartadmin_user -d smartadmin_igaming -f test-data/create-test-wallets.sql

# 獲取 Access Token
export ACCESS_TOKEN=$(curl -X POST http://localhost:1024/api/system/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "123456"}' | jq -r '.data.accessToken')
```

### 3. 運行測試

```bash
cd docs/testing/performance

# 運行完整測試套件（推薦）
k6 run \
  --env BASE_URL=http://localhost:1024 \
  --env ACCESS_TOKEN="$ACCESS_TOKEN" \
  --out json=results/wallet-concurrency-test-result.json \
  k6-wallet-concurrency-test.js

# 查看 HTML 報告
open results/summary.html
```

**測試時長**: ~17 分鐘（漸進負載 + 壓力測試 + 尖峰測試）

---

## 📊 測試場景

### 場景 1: 漸進負載測試（預設）
- **目的**: 逐步增加負載，觀察系統行為
- **VUs**: 0 → 100 → 500 → 1000
- **時長**: 17 分鐘

### 場景 2: 壓力測試
- **目的**: 持續高負載測試穩定性
- **VUs**: 1000（固定）
- **時長**: 10 分鐘

### 場景 3: 尖峰測試
- **目的**: 測試突發流量應對能力
- **VUs**: 0 → 2000（10 秒內）
- **時長**: 1 分 20 秒

---

## ✅ 驗收標準檢查清單

測試完成後，驗證以下項目：

| # | 驗收項 | 標準 | 查看位置 |
|---|--------|------|---------|
| 1 | TPS | > 500 | `http_reqs.values.rate` |
| 2 | P95 響應時間 | < 100ms | `http_req_duration.values.p(95)` |
| 3 | 錯誤率 | < 0.1% | `http_req_failed.values.rate` |
| 4 | 重複扣款 | 0 次 | `duplicateDebits.values.count` |
| 5 | 樂觀鎖衝突率 | < 1% | `optimisticLockFailures / http_reqs` |
| 6 | 餘額一致性 | 100% | SQL 驗證腳本 |

---

## 📖 詳細文檔

**完整指南**: [K6_WALLET_PERFORMANCE_TEST_GUIDE.md](./K6_WALLET_PERFORMANCE_TEST_GUIDE.md)

內容包含：
- 詳細的安裝和配置步驟
- 測試數據準備 SQL 腳本
- 餘額一致性驗證方法
- 性能優化建議
- 故障排查指南

---

## 🔗 相關資源

- **k6 官方文檔**: [https://k6.io/docs/](https://k6.io/docs/)
- **iGaming 實施計劃**: [C:\Users\ron.chang\.claude\plans\memoized-beaming-pretzel.md](C:\Users\ron.chang\.claude\plans\memoized-beaming-pretzel.md)
- **Wallet API 源碼**: [WalletController.java](../../../smart-admin-api-java21-springboot3/smartadmin-igaming/smartadmin-igaming-wallet/src/main/java/net/lab1024/sa/igaming/wallet/controller/WalletController.java)

---

**維護者**: SmartAdmin iGaming Team
**最後更新**: 2026-03-10
