---
name: scheduled-task-manager
description: "排程任務管理 (XXL-Job/Snail-Job)"
priority: P2
category: devops
---

# Scheduled Task Manager

為 SmartAdmin 應用程式生成 XXL-Job/Snail-Job 排程任務整合，包括任務處理器、Cron 表達式、監控告警和重試邏輯。

## Usage

```
User: "Create scheduled task for daily report generation"
AI: [Generate job handler, configure cron, add retry logic]
```

## When to Use

- 實現排程任務
- 批次處理作業
- 定時報表生成
- 資料同步任務
- 過期資料清理

## Generated Output

- XXL-Job/Snail-Job 任務處理器
- Cron 表達式配置
- 任務參數配置
- 重試和失敗處理邏輯
- 監控告警配置

## Workflow

1. **分析任務需求**
   - 任務類型 (simple, sharding, broadcast)
   - 執行頻率和時間

2. **生成任務處理器**
   - `@XxlJob("handlerName")` 註解
   - 任務參數解析
   - 業務邏輯實現

3. **配置 Cron 表達式**
   - 標準 Cron 語法
   - 人類可讀描述

4. **實現重試邏輯**
   - 失敗重試次數
   - 指數退避策略
   - 熔斷機制

5. **設置監控告警**
   - 失敗通知
   - SLA 追蹤
   - 執行日誌

## Related Rules

- [F04-architecture-rules.md](../../../rules/foundation/F04-architecture-rules.md)

## Example Session

**User:** 創建每日凌晨 2 點執行的用戶統計計算任務

**AI Agent Actions:**
1. 創建 `UserStatisticsJobHandler` 類
2. 添加 `@XxlJob("userStatisticsJob")` 註解
3. 配置 Cron: `0 0 2 * * ?` (每日 02:00)
4. 實現統計計算邏輯
5. 添加重試邏輯 (最多 3 次, 間隔 5 分鐘)
6. 配置失敗告警通知
