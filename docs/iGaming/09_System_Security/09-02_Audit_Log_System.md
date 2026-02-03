# 09-02 審計日誌系統 (Audit Log System)

> **相關文檔**: [09-04 審批工作流系統](./09-04_Approval_Workflow_System.md) - 高風險操作的 Maker-Checker 審批流程

## 1. 系統概述

為滿足監管 (License) 要求，系統內所有 "敏感操作" 必須留痕 (Audit Log)。本系統提供完整的審計日誌記錄、檢索、導出和合規報告功能。

**核心目標**：
- **合規性**: 滿足 MGA、UKGC、Curacao 等監管機構要求
- **不可篡改**: 使用 Hash Chain 防止日誌被修改或刪除
- **高可用性**: 99.9% 可用性，支持高併發日誌寫入（10000+ ops/s）
- **成本優化**: 分層存儲策略，降低長期存儲成本

---

## 2. 審計日誌基礎 (Audit Log Fundamentals)

### 2.1 記錄範疇

**登入日誌**：
| 字段 | 說明 | 範例 |
|------|------|------|
| Who | 用戶身份 | admin_david (user_id: 12345) |
| When | 時間戳 | 2026-01-27T10:30:15.234Z |
| Where | IP地址 + 地理位置 | 192.168.1.100 (TW, Taipei) |
| Device | 設備信息 | Desktop, Chrome 120, Windows 11 |
| Status | 登入結果 | SUCCESS / FAILURE |
| Reason | 失敗原因（如有）| INVALID_PASSWORD, ACCOUNT_LOCKED |

**操作日誌**：
```text
格式：[User A] 在 [Time T] 對 [Resource R] 執行了 [Action A]，
      將 [Field F] 從 [Old Value] 變更為 [New Value]

範例：Admin_David 於 2026-01-27T10:30:15Z 修改了 Player_123 的 Status，
      從 ACTIVE 變更為 LOCKED，原因：懷疑套利行為
```

**必須記錄的敏感操作**：
| 類別 | 操作 | 記錄內容 |
|------|------|---------|
| **玩家管理** | 鎖定/解鎖帳號 | 玩家ID、狀態變更、原因 |
| **財務操作** | 手動加減款 | 玩家ID、金額、原因、審批人 |
| **提款審核** | 批准/拒絕提款 | 提款ID、決策、理由 |
| **配置變更** | 修改VIP等級門檻 | 配置項、舊值、新值 |
| **權限管理** | 修改管理員權限 | 用戶ID、權限變更 |
| **數據刪除** | 刪除玩家數據 | 玩家ID、刪除原因（GDPR等）|

---

### 2.2 查看與導出

**查詢功能**：
- 按用戶篩選（username, user_id）
- 按時間範圍篩選（支援最多 90 天單次查詢）
- 按操作類型篩選（LOGIN, CREATE, UPDATE, DELETE）
- 按資源類型篩選（PLAYER, WITHDRAWAL, BONUS等）
- 按結果篩選（SUCCESS, FAILURE）
- 全文檢索（如搜尋 "fraud", "manual credit"）

**導出格式**：
- CSV（適合 Excel 分析）
- JSON（適合程式化處理）
- Excel（帶格式的報表）
- PDF（合規報告用途）

---

### 2.3 不可刪除性 (WORM - Write Once Read Many)

**技術實作**：
1. **雙寫機制**: 同時寫入 Elasticsearch（可查詢）和 S3（不可變）
2. **S3 Object Lock**: 啟用 S3 Governance Mode，防止刪除
3. **Hash Chain**: 每條日誌包含前一條的哈希值，形成不可篡改鏈
4. **定期驗證**: 每日檢查 Hash Chain 完整性


---

## 3. 審計日誌檢索系統 (Audit Log Search & Export System)

### 3.1 系統架構

```sql
[Audit Log Architecture - Complete Data Pipeline]
┌──────────────────────────────────────────────────────────────────┐
│  Write Path (Real-time Ingestion)                               │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │  Application (Java/Node.js)                               │  │
│  │    ↓ Async write (non-blocking)                           │  │
│  │  Kafka Topic: audit-logs                                  │  │
│  │    - Partitions: 12 (by tenant_id hash)                   │  │
│  │    - Retention: 7 days (buffer)                           │  │
│  │    ↓ Consumer Group: audit-indexer                        │  │
│  │  Logstash / Vector                                        │  │
│  │    - Transform: Add @timestamp, geoip, user_agent parse   │  │
│  │    - Filter: Sensitive field redaction (PII masking)      │  │
│  │    - Output: Elasticsearch + S3 (dual-write)              │  │
│  └────────────────────────────────────────────────────────────┘  │
├──────────────────────────────────────────────────────────────────┤
│  Storage Tiering (Cost Optimization)                            │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │  Hot Storage (Elasticsearch)                              │  │
│  │  - Data: Last 30 days                                     │  │
│  │  - Query Latency: < 100ms                                 │  │
│  │  - Index: audit-logs-2026-01 (monthly rollover)          │  │
│  │  - Replicas: 2 (HA)                                       │  │
│  ├────────────────────────────────────────────────────────────┤  │
│  │  Warm Storage (S3 Standard)                              │  │
│  │  - Data: 31-365 days                                      │  │
│  │  - Query Latency: 2-5 seconds (Athena)                   │  │
│  │  - Format: Parquet (compressed, columnar)                │  │
│  ├────────────────────────────────────────────────────────────┤  │
│  │  Cold Storage (S3 Glacier)                               │  │
│  │  - Data: 1-7 years (compliance retention)                │  │
│  │  - Query Latency: 1-5 minutes (restore first)            │  │
│  │  - Format: Parquet (compressed)                           │  │
│  └────────────────────────────────────────────────────────────┘  │
├──────────────────────────────────────────────────────────────────┤
│  Query Layer (API Gateway)                                       │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │  GET /api/v1/audit-logs                                   │  │
│  │  - Hot: Direct ES query                                   │  │
│  │  - Warm: Athena query (async job)                        │  │
│  │  - Cold: Glacier restore → Athena query                  │  │
│  └────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────┘
```

---

### 3.2 Elasticsearch 索引策略

#### 3.2.1 索引設計 (Index Template)

```json
{
  "index_patterns": ["audit-logs-*"],
  "settings": {
    "number_of_shards": 3,
    "number_of_replicas": 2,
    "index.lifecycle.name": "audit-log-policy",
    "index.lifecycle.rollover_alias": "audit-logs",
    "refresh_interval": "5s"
  },
  "mappings": {
    "properties": {
      "@timestamp": { "type": "date" },
      "tenant_id": { "type": "keyword" },
      "user_id": { "type": "long" },
      "username": { "type": "keyword" },
      "action": { "type": "keyword" },
      "resource": { "type": "keyword" },
      "resource_id": { "type": "keyword" },
      "result": { "type": "keyword" },
      "ip_address": { "type": "ip" },
      "user_agent": { "type": "text", "fields": {"keyword": {"type": "keyword"}}},
      "geolocation": {
        "properties": {
          "country": { "type": "keyword" },
          "city": { "type": "keyword" }
        }
      },
      "changes": {
        "properties": {
          "field": { "type": "keyword" },
          "old_value": { "type": "text" },
          "new_value": { "type": "text" }
        }
      },
      "request_id": { "type": "keyword" },
      "session_id": { "type": "keyword" },
      "hash": { "type": "keyword" }
    }
  }
}
```

---

#### 3.2.2 自動 Rollover Policy (ILM)

```json
{
  "policy": "audit-log-policy",
  "phases": {
    "hot": {
      "min_age": "0ms",
      "actions": {
        "rollover": {
          "max_age": "30d",
          "max_size": "50GB",
          "max_docs": 100000000
        },
        "set_priority": {
          "priority": 100
        }
      }
    },
    "warm": {
      "min_age": "30d",
      "actions": {
        "forcemerge": {
          "max_num_segments": 1
        },
        "shrink": {
          "number_of_shards": 1
        },
        "allocate": {
          "require": {
            "box_type": "warm"
          }
        },
        "set_priority": {
          "priority": 50
        }
      }
    },
    "cold": {
      "min_age": "90d",
      "actions": {
        "allocate": {
          "require": {
            "box_type": "cold"
          }
        },
        "freeze": {},
        "set_priority": {
          "priority": 0
        }
      }
    },
    "delete": {
      "min_age": "2555d",
      "actions": {
        "delete": {}
      }
    }
  }
}
```

**說明**:
- **Hot Phase (0-30天)**: 高性能 SSD 節點，支援高頻查詢
- **Warm Phase (30-90天)**: 合併 segment 優化儲存，移至較慢節點
- **Cold Phase (90天-7年)**: 凍結索引，極低成本存儲
- **Delete Phase (7年後)**: 自動刪除過期數據

---

### 3.3 搜尋 API 規格 (Search API Specification)

#### 3.3.1 基礎查詢 API

```markdown
GET /api/v1/audit-logs
Authorization: Bearer {admin_jwt}
X-Tenant-ID: {tenant_id}

Query Parameters:
- tenant_id: string (optional for Super Admin, required for Tenant Admin)
- user_id: integer (optional) - Filter by specific user
- username: string (optional) - Filter by username
- action: string (optional) - Values: LOGIN, LOGOUT, CREATE, UPDATE, DELETE, APPROVE, REJECT
- resource: string (optional) - Values: PLAYER, WITHDRAWAL, BONUS, GAME, CONFIG
- resource_id: string (optional) - Specific resource ID
- result: string (optional) - Values: SUCCESS, FAILURE
- ip_address: string (optional) - Filter by IP
- from: ISO8601 datetime (required) - Start date
- to: ISO8601 datetime (required) - End date (max 90 days range)
- page: integer (default: 1)
- page_size: integer (default: 100, max: 1000)
- sort: string (default: "@timestamp:desc")

Response 200 OK:
{
  "total": 125678,
  "page": 1,
  "page_size": 100,
  "total_pages": 1257,
  "data": [
    {
      "log_id": "log-uuid-1234",
      "timestamp": "2026-01-27T10:30:15.234Z",
      "tenant_id": "tenant-001",
      "user_id": 12345,
      "username": "admin_david",
      "action": "UPDATE",
      "resource": "PLAYER",
      "resource_id": "player-789",
      "result": "SUCCESS",
      "ip_address": "192.168.1.100",
      "geolocation": {
        "country": "TW",
        "city": "Taipei"
      },
      "changes": [
        {
          "field": "status",
          "old_value": "ACTIVE",
          "new_value": "LOCKED",
          "reason": "Suspected arbitrage betting"
        }
      ],
      "user_agent": "Mozilla/5.0...",
      "request_id": "req-uuid-5678",
      "session_id": "session-uuid-9012"
    }
  ],
  "query_execution_time_ms": 45,
  "storage_tier": "hot"
}

Response 400 Bad Request:
{
  "error": "INVALID_DATE_RANGE",
  "message": "Date range cannot exceed 90 days for single query"
}

Response 503 Service Unavailable (Warm/Cold storage):
{
  "error": "ASYNC_QUERY_REQUIRED",
  "message": "Requested date range is in warm/cold storage",
  "job_id": "job-uuid-1234",
  "estimated_completion_seconds": 120,
  "poll_url": "/api/v1/audit-logs/jobs/job-uuid-1234"
}
```

---

#### 3.3.2 高級搜尋 API (Full-Text Search)

```text
POST /api/v1/audit-logs/search
Authorization: Bearer {admin_jwt}
Content-Type: application/json

Request Body:
{
  "query": {
    "bool": {
      "must": [
        {
          "range": {
            "@timestamp": {
              "gte": "2026-01-01T00:00:00Z",
              "lte": "2026-01-31T23:59:59Z"
            }
          }
        },
        {
          "match": {
            "changes.reason": "fraud"
          }
        }
      ],
      "should": [
        {
          "term": {
            "action": "DELETE"
          }
        },
        {
          "term": {
            "action": "UPDATE"
          }
        }
      ],
      "minimum_should_match": 1
    }
  },
  "aggs": {
    "actions_by_user": {
      "terms": {
        "field": "username",
        "size": 10
      }
    },
    "actions_over_time": {
      "date_histogram": {
        "field": "@timestamp",
        "calendar_interval": "day"
      }
    }
  },
  "size": 100,
  "from": 0,
  "sort": [{"@timestamp": "desc"}]
}

Response 200 OK:
{
  "hits": {
    "total": {"value": 2456, "relation": "eq"},
    "hits": [...]
  },
  "aggregations": {
    "actions_by_user": {
      "buckets": [
        {"key": "admin_david", "doc_count": 345},
        {"key": "admin_sarah", "doc_count": 289}
      ]
    },
    "actions_over_time": {
      "buckets": [
        {"key_as_string": "2026-01-01", "doc_count": 120},
        {"key_as_string": "2026-01-02", "doc_count": 135}
      ]
    }
  },
  "took": 78
}
```

---

#### 3.3.3 匯出 API (Export to CSV/JSON)

```text
POST /api/v1/audit-logs/export
Authorization: Bearer {admin_jwt}
Content-Type: application/json

Request Body:
{
  "filters": {
    "from": "2026-01-01T00:00:00Z",
    "to": "2026-01-31T23:59:59Z",
    "action": "WITHDRAWAL_APPROVE",
    "result": "SUCCESS"
  },
  "format": "CSV",  // or "JSON", "EXCEL"
  "fields": [
    "timestamp", "username", "resource_id", "old_value", "new_value", "ip_address"
  ],
  "notify_email": "admin@company.com"
}

Response 202 Accepted:
{
  "export_job_id": "export-uuid-1234",
  "status": "PROCESSING",
  "estimated_completion_seconds": 300,
  "poll_url": "/api/v1/audit-logs/exports/export-uuid-1234"
}

// Poll endpoint
GET /api/v1/audit-logs/exports/{export_job_id}

Response 200 OK (Completed):
{
  "export_job_id": "export-uuid-1234",
  "status": "COMPLETED",
  "download_url": "https://cdn.example.com/exports/audit-logs-2026-01.csv",
  "file_size_bytes": 15728640,
  "row_count": 125678,
  "expires_at": "2026-02-03T10:00:00Z",
  "completed_at": "2026-01-27T10:35:22Z"
}
```

---

### 3.4 合規報告範本 (Compliance Report Templates)

#### 3.4.1 MGA (Malta Gaming Authority) 報告格式

```json
{
  "report_type": "MGA_AUDIT_LOG_REPORT",
  "report_period": {
    "start": "2026-01-01T00:00:00Z",
    "end": "2026-01-31T23:59:59Z"
  },
  "operator": {
    "license_number": "MGA/B2C/123/2023",
    "company_name": "Casino Operator Ltd",
    "contact_email": "compliance@operator.com"
  },
  "summary": {
    "total_events": 1500000,
    "high_risk_events": 2456,
    "failed_login_attempts": 12345,
    "unauthorized_access_attempts": 45,
    "data_modifications": 5678,
    "administrative_actions": 890
  },
  "high_risk_events": [
    {
      "event_id": "log-uuid-1234",
      "timestamp": "2026-01-15T14:30:00Z",
      "user": "admin_david",
      "action": "MANUAL_CREDIT",
      "resource": "PLAYER_WALLET",
      "resource_id": "player-789",
      "amount": 5000.00,
      "currency": "EUR",
      "reason": "Compensation for technical issue",
      "approver": "finance_manager_sarah",
      "risk_level": "HIGH"
    }
  ],
  "security_incidents": [
    {
      "incident_id": "incident-001",
      "timestamp": "2026-01-20T03:15:00Z",
      "type": "BRUTE_FORCE_ATTACK",
      "source_ip": "203.0.113.45",
      "target_user": "admin_john",
      "attempts": 50,
      "blocked": true,
      "actions_taken": "IP blocked, user notified, security team alerted"
    }
  ],
  "data_protection_activities": [
    {
      "activity_type": "GDPR_DELETION_REQUEST",
      "player_id": "player-456",
      "request_date": "2026-01-10T10:00:00Z",
      "completion_date": "2026-02-09T02:15:00Z",
      "status": "COMPLETED",
      "certificate_id": "GDPR-DEL-2026-001234"
    }
  ],
  "generated_at": "2026-02-01T09:00:00Z",
  "generated_by": "compliance_officer_jane"
}
```

---

#### 3.4.2 UKGC (UK Gambling Commission) 報告格式

```json
{
  "report_type": "UKGC_REGULATORY_RETURN",
  "licence_number": "000-039XXX-R-319XXXX-XXX",
  "reporting_period": "Q1 2026",
  "audit_trail_summary": {
    "player_account_changes": {
      "total": 45678,
      "by_type": {
        "self_exclusion": 234,
        "account_closure": 567,
        "deposit_limit_changes": 3456,
        "time_limit_changes": 1234
      }
    },
    "responsible_gambling_interactions": {
      "total": 8901,
      "by_type": {
        "reality_check_displayed": 7890,
        "take_a_break_initiated": 890,
        "self_exclusion_initiated": 121
      }
    },
    "source_of_funds_checks": {
      "total_checks": 2345,
      "passed": 2100,
      "failed": 245,
      "average_completion_time_hours": 18.5
    },
    "unusual_betting_patterns": {
      "total_flagged": 567,
      "investigated": 567,
      "false_positives": 500,
      "confirmed_issues": 67
    }
  },
  "key_events": [
    {
      "event_type": "LICENCE_CONDITION_11_BREACH",
      "date": "2026-01-15",
      "description": "Player exceeded £1000 loss in 24h without SOF check",
      "player_id": "ANONYMIZED-123",
      "actions_taken": "SOF check initiated, account suspended pending verification",
      "resolution": "SOF verified, account reinstated",
      "resolution_date": "2026-01-17"
    }
  ]
}
```

---

### 3.5 防篡改機制 (Tamper-Proof Hash Chain)

#### 3.5.1 Hash Chain 原理

```
[Hash Chain Architecture]
┌──────────────────────────────────────────────────────────────┐
│  Log Entry N                                                 │
│  ┌────────────────────────────────────────────────────────┐  │
│  │  Data: {user_id, action, timestamp, ...}              │  │
│  │  Previous Hash: SHA256(Log N-1)                       │  │
│  │  Current Hash: SHA256(Data + Previous Hash)          │  │
│  └────────────────────────────────────────────────────────┘  │
│                         ↓                                    │
│  Log Entry N+1                                               │
│  ┌────────────────────────────────────────────────────────┐  │
│  │  Data: {...}                                          │  │
│  │  Previous Hash: SHA256(Log N)  ← Links to above      │  │
│  │  Current Hash: SHA256(Data + Previous Hash)          │  │
│  └────────────────────────────────────────────────────────┘  │
│                                                              │
│  Tampering Detection:                                        │
│  - Modify Log N → Current Hash changes                      │
│  - Log N+1's Previous Hash no longer matches                │
│  - Chain integrity broken → ALERT                           │
└──────────────────────────────────────────────────────────────┘
```

---

#### 3.5.2 實作程式碼


---

#### 3.5.3 定期完整性驗證


---

### 3.6 效能優化策略

#### 3.6.1 查詢優化技巧

| 場景 | 優化策略 | 效能提升 |
|---|---|---|
| 高頻查詢 (近 7 天) | ES Read Replica (3 副本) | Latency: 100ms → 30ms |
| 大範圍查詢 (30-90 天) | Async Job + S3 Athena | 避免 ES 節點過載 |
| 導出大檔案 (>1M rows) | Streaming Export (Chunked) | 避免 Memory OOM |
| 複雜聚合查詢 | Pre-computed Materialized View | Query Time: 10s → 500ms |

---

#### 3.6.2 成本優化

**預估成本 (每月 1 億條日誌)**:

| Storage Tier | Data Volume | Storage Cost | Query Cost | Total |
|---|---|---|---|---|
| Hot (ES) | 30M logs (~100GB) | $300/月 | $50/月 | $350/月 |
| Warm (S3) | 300M logs (~500GB) | $12/月 | $20/月 | $32/月 |
| Cold (Glacier) | 7B logs (~10TB) | $10/月 | $5/月 | $15/月 |
| **Total** | **7.33B logs** | **$322/月** | **$75/月** | **$397/月** |

**對比**: 全部存 ES = **$30,000/月** (75x more expensive)

---

### 3.7 監控與告警

#### 3.7.1 關鍵指標

| 指標 | 正常範圍 | Alert Threshold |
|---|---|---|
| Log Ingestion Rate | 1000-5000/s | < 100/s (資料遺失) 或 > 20000/s (攻擊) |
| ES Cluster Health | GREEN | YELLOW > 5min, RED > 1min |
| Query Latency (P99) | < 100ms | > 500ms |
| Hash Chain Integrity | 100% | < 100% (立即告警) |
| Kafka Lag | < 1000 messages | > 10000 messages |

---

#### 3.7.2 異常模式檢測


---

## 📚 相關文檔

### 配套系統參考
- [09-04 審批工作流系統](./09-04_Approval_Workflow_System.md) - 審計日誌的審批操作記錄
- [09-01 管理後台RBAC](./09-01_Admin_RBAC.md) - 審計日誌的權限控制
- [09-03 數據安全標準](./09-03_Data_Security_Standard.md) - 敏感數據脫敏要求

### 技術架構參考
- [12-01 部署架構](../12_Technical_Operations/12-01_Deployment_Architecture.md) - Elasticsearch 集群部署
- [07-04 數據管道架構](../07_Platform_Management/07-04_Data_Pipeline_Architecture.md) - Kafka 數據流配置

---

**文檔版本**: 1.0.0 (拆分自 09-02 V2.0)
**最後更新**: 2026-01-27
**維護團隊**: Security Team & Infrastructure Team
