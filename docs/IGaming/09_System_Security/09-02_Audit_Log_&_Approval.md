# 09-02 審計日誌系統 (Audit Log System)

## 1. 系統概述
為滿足監管 (License) 要求，系統內所有 "敏感操作" 必須留痕 (Audit Log)。本文檔專注於審計日誌的收集、存儲、檢索與合規報表功能。

> **📌 相關文檔**：關鍵變更的審批流程（Maker-Checker）請參考 [09-04 審批工作流系統](09-04_Approval_Workflow_System.md)。

## 2. 審計日誌 (Audit Log)

### 2.1 記錄範疇
- **登入日誌**：Who, When, Where (IP), Device, Status (Success/Fail)。
- **操作日誌**：
  - 格式：`[User A] 在 [Time T] 修改了 [Object O] 的 [Field F]，從 [Value Old] 變為 [Value New]`。
  - 範例：`Admin_David 修改了 Player_123 的 Status，從 Active 變為 Locked，原因：懷疑套利`。

### 2.2 查看與導出
- 支援依 User、時間、操作類型 (Update/Delete) 進行篩選。
- 不可刪除性：Audit Log 必須存儲於 "WORM (Write Once Read Many)" 介質，防止被駭客抹除。

---

## 5. 審計日誌檢索系統 (Audit Log Search & Export System)

### 5.1 系統架構

```
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

### 5.2 Elasticsearch 索引策略

#### 5.2.1 索引設計 (Index Template)

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

#### 5.2.2 自動 Rollover Policy (ILM)

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

### 5.3 搜尋 API 規格 (Search API Specification)

#### 5.3.1 基礎查詢 API

```
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

#### 5.3.2 高級搜尋 API (Full-Text Search)

```
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

#### 5.3.3 匯出 API (Export to CSV/JSON)

```
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

### 5.4 合規報告範本 (Compliance Report Templates)

#### 5.4.1 MGA (Malta Gaming Authority) 報告格式

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

#### 5.4.2 UKGC (UK Gambling Commission) 報告格式

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

### 5.5 防篡改機制 (Tamper-Proof Hash Chain)

#### 5.5.1 Hash Chain 原理

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

#### 5.5.2 實作程式碼

```python
import hashlib
import json
from datetime import datetime

class TamperProofAuditLog:
    def __init__(self, db_connection):
        self.db = db_connection
        self.previous_hash = self._get_latest_hash()

    def _get_latest_hash(self):
        """Get the hash of the most recent log entry"""
        result = self.db.query("""
            SELECT current_hash FROM audit_log_chain
            ORDER BY sequence_id DESC LIMIT 1
        """)
        return result[0]['current_hash'] if result else '0' * 64  # Genesis hash

    def append_log(self, log_entry):
        """Append a new log entry to the chain"""
        # Step 1: Serialize log data
        log_data = json.dumps(log_entry, sort_keys=True)

        # Step 2: Compute current hash (data + previous hash)
        hash_input = log_data + self.previous_hash
        current_hash = hashlib.sha256(hash_input.encode()).hexdigest()

        # Step 3: Store in database
        self.db.execute("""
            INSERT INTO audit_log_chain
            (log_data, previous_hash, current_hash, created_at)
            VALUES (%s, %s, %s, %s)
        """, (log_data, self.previous_hash, current_hash, datetime.now()))

        # Step 4: Update previous hash for next entry
        self.previous_hash = current_hash

        return current_hash

    def verify_integrity(self, from_sequence_id=None, to_sequence_id=None):
        """Verify hash chain integrity"""
        logs = self.db.query("""
            SELECT sequence_id, log_data, previous_hash, current_hash
            FROM audit_log_chain
            WHERE sequence_id BETWEEN %s AND %s
            ORDER BY sequence_id ASC
        """, (from_sequence_id or 1, to_sequence_id or 999999999))

        for i, log in enumerate(logs):
            # Recompute hash
            expected_hash = hashlib.sha256(
                (log['log_data'] + log['previous_hash']).encode()
            ).hexdigest()

            if expected_hash != log['current_hash']:
                return {
                    'integrity': 'BROKEN',
                    'tampered_log': log['sequence_id'],
                    'message': f'Hash mismatch at sequence {log["sequence_id"]}'
                }

            # Check chain linkage
            if i > 0 and logs[i-1]['current_hash'] != log['previous_hash']:
                return {
                    'integrity': 'BROKEN',
                    'message': f'Chain broken between {logs[i-1]["sequence_id"]} and {log["sequence_id"]}'
                }

        return {
            'integrity': 'INTACT',
            'verified_logs': len(logs),
            'message': 'All logs verified successfully'
        }
```

#### 5.5.3 定期完整性驗證

```python
# Scheduled task (runs daily at 02:00)
def daily_integrity_check():
    verifier = TamperProofAuditLog(db)
    result = verifier.verify_integrity()

    if result['integrity'] == 'BROKEN':
        # Alert security team immediately
        send_alert(
            severity='CRITICAL',
            title='Audit Log Tampering Detected',
            message=result['message'],
            recipients=['security@company.com', 'cto@company.com']
        )
        # Log to separate immutable system
        blockchain_logger.log_security_event(result)
    else:
        logger.info(f"Integrity check passed: {result['verified_logs']} logs verified")
```

### 5.6 效能優化策略

#### 5.6.1 查詢優化技巧

| 場景 | 優化策略 | 效能提升 |
|---|---|---|
| 高頻查詢 (近 7 天) | ES Read Replica (3 副本) | Latency: 100ms → 30ms |
| 大範圍查詢 (30-90 天) | Async Job + S3 Athena | 避免 ES 節點過載 |
| 導出大檔案 (>1M rows) | Streaming Export (Chunked) | 避免 Memory OOM |
| 複雜聚合查詢 | Pre-computed Materialized View | Query Time: 10s → 500ms |

#### 5.6.2 成本優化

**預估成本 (每月 1 億條日誌)**:

| Storage Tier | Data Volume | Storage Cost | Query Cost | Total |
|---|---|---|---|---|
| Hot (ES) | 30M logs (~100GB) | $300/月 | $50/月 | $350/月 |
| Warm (S3) | 300M logs (~500GB) | $12/月 | $20/月 | $32/月 |
| Cold (Glacier) | 7B logs (~10TB) | $10/月 | $5/月 | $15/月 |
| **Total** | **7.33B logs** | **$322/月** | **$75/月** | **$397/月** |

**對比**: 全部存 ES = **$30,000/月** (75x more expensive)

### 5.7 監控與告警

#### 5.7.1 關鍵指標

| 指標 | 正常範圍 | Alert Threshold |
|---|---|---|
| Log Ingestion Rate | 1000-5000/s | < 100/s (資料遺失) 或 > 20000/s (攻擊) |
| ES Cluster Health | GREEN | YELLOW > 5min, RED > 1min |
| Query Latency (P99) | < 100ms | > 500ms |
| Hash Chain Integrity | 100% | < 100% (立即告警) |
| Kafka Lag | < 1000 messages | > 10000 messages |

#### 5.7.2 異常模式檢測

```python
def detect_anomalies(tenant_id, time_window='1h'):
    """Detect suspicious audit log patterns"""

    # Pattern 1: Excessive failed login attempts
    failed_logins = count_logs(
        tenant_id=tenant_id,
        action='LOGIN',
        result='FAILURE',
        time_window=time_window
    )
    if failed_logins > 100:
        alert('Potential brute force attack')

    # Pattern 2: Mass deletion
    deletions = count_logs(
        tenant_id=tenant_id,
        action='DELETE',
        time_window=time_window
    )
    if deletions > 1000:
        alert('Mass deletion detected (potential data breach)')

    # Pattern 3: Unusual time access
    night_access = count_logs(
        tenant_id=tenant_id,
        time_range=('22:00', '06:00')
    )
    if night_access > baseline_night_access * 5:
        alert('Unusual night-time activity')

    # Pattern 4: Geographic anomaly
    countries = get_unique_countries(tenant_id, time_window)
    if len(countries) > 5:
        alert('Admin accessed from multiple countries in short time')
```

---

## 📚 相關文檔

### 前置知識（必讀）
- [09-03 認證與授權](09-03_Authentication_Authorization.md) - 審計日誌中的用戶身份驗證

### 核心依賴
- [12-01 部署架構](../12_DevOps_Observability/12-01_Deployment_Architecture.md) - Elasticsearch 集群部署
- [09-03 數據安全](09-03_Authentication_Authorization.md) - 審計日誌加密存儲

### 相關實作
- [09-04 審批工作流系統](09-04_Approval_Workflow_System.md) - **審批操作的實現（本文檔的延伸）**
- [02-01 提款風控](../02_Finance_Center/02-01_Withdrawal_Risk_Control.md) - 提款審批的審計追蹤
- [11-01 客服平台設計](../11_Customer_Service/11-01_CS_Platform_Design.md) - 客服操作審計

### 延伸閱讀
- [12-03 監控與告警](../12_DevOps_Observability/12-03_Monitoring_Alerting.md) - 日誌監控告警
- [07-03 通知架構](../07_Platform_Management/07-03_Notification_Architecture.md) - 審計告警通知

---

**文件版本**: V2.0 (Enhanced)
**最後更新**: 2026-01-27
**狀態**: Architecture-Level Complete
**維護團隊**: Security & Compliance Team

## CHANGELOG

### v2.0.0 (2026-01-27)
- **重大變更**：拆分審批工作流內容至 [09-04 審批工作流系統](09-04_Approval_Workflow_System.md)
- 優化：專注於審計日誌的收集、存儲、檢索功能
- 新增：完整的相關文檔交叉引用
- 優化：Elasticsearch 架構與效能優化詳解
