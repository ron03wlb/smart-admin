# Legacy Kafka v1 文檔

**狀態**：🗄️ 已歸檔
**歸檔日期**：2026-01-27
**歸檔原因**：Kafka 整合已升級至 v2

---

## 文檔結構

此歸檔包含 SmartAdmin Kafka v1 整合的完整文檔（46 個文件）：

### 快速開始
- `kafka/getting-started/quick-start.md` - 5 分鐘快速開始
- `kafka/getting-started/hello-world.md` - Hello World 範例
- `kafka/getting-started/quick-reference.md` - 快速參考卡

### 架構設計
- `kafka/architecture/overview.md` - 架構總覽
- `kafka/architecture/module-structure.md` - 模組結構
- `kafka/architecture/message-flow.md` - 消息流程
- `kafka/architecture/batch-processing.md` - 批次處理
- `kafka/architecture/dead-letter-queue.md` - 死信隊列

### 使用指南
- `kafka/guides/consumer-guide.md` - 消費者指南
- `kafka/guides/producer-guide.md` - 生產者指南
- `kafka/guides/configuration.md` - 配置指南
- `kafka/guides/batch-operations.md` - 批次操作
- `kafka/guides/best-practices.md` - 最佳實踐
- `kafka/guides/error-handling.md` - 錯誤處理

### 範例
- `kafka/examples/basic-example.md` - 基礎範例
- `kafka/examples/batch-example.md` - 批次範例
- `kafka/examples/dlq-example.md` - DLQ 範例
- `kafka/examples/aggregator-example.md` - 聚合器範例
- `kafka/examples/docker-compose-example.md` - Docker Compose 範例

### 進階主題
- `kafka/advanced/custom-listeners.md` - 自定義監聽器
- `kafka/advanced/idempotency.md` - 冪等性
- `kafka/advanced/transactions.md` - 事務
- `kafka/advanced/schema-registry.md` - Schema Registry
- `kafka/advanced/extending-framework.md` - 擴展框架

### 運維
- `kafka/operations/monitoring.md` - 監控
- `kafka/operations/deployment.md` - 部署
- `kafka/operations/performance-tuning.md` - 性能調優
- `kafka/operations/backup-recovery.md` - 備份恢復
- `kafka/operations/health-checks.md` - 健康檢查

### 測試
- `kafka/testing/unit-testing.md` - 單元測試
- `kafka/testing/integration-testing.md` - 整合測試
- `kafka/testing/testing-strategy.md` - 測試策略
- `kafka/testing/verification-framework.md` - 驗證框架

### 故障排除
- `kafka/troubleshooting/common-issues.md` - 常見問題
- `kafka/troubleshooting/debugging-tips.md` - 調試提示
- `kafka/troubleshooting/diagnostic-guide.md` - 診斷指南
- `kafka/troubleshooting/faq.md` - FAQ

### 參考
- `kafka/reference/configuration-reference.md` - 配置參考
- `kafka/reference/api-reference.md` - API 參考
- `kafka/reference/error-codes.md` - 錯誤代碼
- `kafka/reference/migration-guide.md` - 遷移指南

### 附錄
- `kafka/appendix/changelog.md` - 變更日誌
- `kafka/appendix/glossary.md` - 術語表
- `kafka/appendix/resources.md` - 資源列表
- `kafka/appendix/contributing.md` - 貢獻指南

### 其他
- `kafka/index.md` - 文檔主入口

---

## 遷移至 Kafka v2

**文檔組織變更**: Kafka 文檔已從獨立的 VitePress 網站遷移至內嵌模組文檔

**新文檔位置**:
```
smart-admin-api-java21-springboot3/sa-base/foundation/mq/docs/
├── kafka-batch-quickstart.md      - 快速開始指南
├── kafka-batch-api-reference.md   - API 參考
├── kafka-batch-examples.md        - 使用範例
├── kafka-batch-testing-guide.md   - 測試指南
└── VERIFICATION-REPORT.md         - 驗證報告
```

**主要變更**:
- ✅ **文檔簡化**: 從 46 個文件 → 5 個核心文件
- ✅ **位置變更**: 從獨立 VitePress 網站 → 內嵌在 foundation/mq 模組
- ✅ **聚焦實用**: 保留核心 API、範例和測試指南,移除冗餘內容
- ✅ **代碼同步**: 文檔與代碼在同一目錄,更易維護

**為什麼歸檔 Kafka v1**:
- 📚 舊文檔網站過於龐大 (46 個文件),維護成本高
- 🎯 新文檔更聚焦實際使用場景 (批次處理、API、測試)
- 🔄 文檔與代碼同步,避免不一致

**代碼實現狀態**:
- ✅ Kafka 集成仍在使用中
- ✅ Foundation MQ 模組: `sa-base/foundation/mq/`
- ✅ 核心類: `KafkaProducerServiceImpl`, `AbstractKafkaListener`
- ✅ 範例代碼: `sa-admin/.../sample/kafka/`

---

## 何時參考此文檔

- 理解 Kafka v1 整合的歷史設計決策
- 比較 v1 和 v2 的架構差異
- 遷移舊代碼時的參考

**注意**：此文檔僅作歷史參考，新專案請使用 Kafka v2 文檔。

---

**維護責任**：SmartAdmin Documentation Team
**版本**：1.0.0
