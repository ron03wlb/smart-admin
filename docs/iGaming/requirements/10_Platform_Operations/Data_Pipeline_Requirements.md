# Data Pipeline Business Requirements

> **Canonical Source**: [source-archive/10_Platform_Management/10-04_Data_Pipeline_Architecture.md](../../source-archive/10_Platform_Management/10-04_Data_Pipeline_Architecture.md)
> **View Type**: Business Requirements
> **Target Audience**: Product Managers, Compliance Officers
> **Related Architecture**: [Data Pipeline Architecture](../../architecture/10_Platform_Management/Data_Pipeline_Architecture.md)
> **Last Synced**: 2026-02-09

---

## 1. Business Overview

The Data Pipeline system solves the performance bottleneck of traditional transactional databases when handling analytical queries. It enables **T+1 business reports** and **near real-time risk control dashboards**, supporting operational decision-making across the iGaming platform.

---

## 2. Data Processing Requirements

### 2.1 Processing Modes

| Mode | Maximum Delay | Data Completeness | Business Use Case |
|------|--------------|-------------------|-------------------|
| **Real-time** | < 5 seconds | May have minor gaps | Operations monitoring, risk alerts |
| **Near Real-time** | 5-60 seconds | High (99%+) | Live dashboards, anomaly detection |
| **Batch (T+1)** | Next day | 100% complete | Business reports, settlement reconciliation |

### 2.2 Processing Mode Selection Guide

Product managers should use this decision matrix when evaluating new report requirements:

| Decision Factor | Choose Real-time | Choose Batch (T+1) |
|----------------|-----------------|---------------------|
| **Revenue impact of delay** | Business loses money with 1-day delay | No immediate financial impact |
| **Operational urgency** | Used for immediate decisions | Used for strategic planning |
| **Regulatory requirement** | Compliance mandates real-time monitoring | Periodic submission acceptable |
| **Calculation complexity** | Simple aggregations (counts, sums) | Complex multi-table joins |
| **Accuracy requirement** | 95%+ acceptable | Must be 100% accurate |
| **Cost sensitivity** | Budget allows 5-10x premium | Cost optimization priority |

---

## 3. Report Type Requirements

### 3.1 Real-time Dashboard Reports

| Report | Update Frequency | Key Metrics | Decision Type |
|--------|-----------------|-------------|---------------|
| **Online Player Count** | Every 10 seconds | Current connected users | Capacity management |
| **Today's Deposit Total** | Every 5 minutes | Cumulative deposits | Revenue tracking |
| **Risk Alert Monitor** | Real-time | Active fraud/risk events | Incident response |
| **Game Performance Live** | Every hour | GGR by game, player count | Game operations |

### 3.2 T+1 Business Reports

| Report | Schedule | Key Metrics | Stakeholder |
|--------|----------|-------------|-------------|
| **Monthly P&L Statement** | 1st of month | Revenue, costs, net profit | CFO, Finance |
| **Agent Settlement** | Weekly (Monday) | Commission, position, payables | Agent operations |
| **Game Reconciliation** | Daily (02:00 AM) | Provider bets vs platform records | Finance |
| **Player LTV Analysis** | Weekly | Lifetime value by segment | Marketing |
| **Compliance Report** | Monthly | Regulatory metrics | Compliance officer |

---

## 4. Data Quality Business Rules

### 4.1 Quality Standards

| Standard | Requirement | Consequence of Failure |
|----------|-------------|----------------------|
| **Completeness** | All records from previous day must be present | Report delayed, not published |
| **Accuracy** | Financial figures must match source to the cent | Manual reconciliation required |
| **Timeliness** | Daily reports available by 03:00 AM | SLA breach notification |
| **Consistency** | Same metric shows same value across all views | Investigation triggered |
| **Anomaly Detection** | If error rate > 5%, pipeline halts | Downstream reports blocked |

### 4.2 Data Correction Policy

| Policy | Description |
|--------|-------------|
| **Historical Immutability** | Published reports are never retroactively modified |
| **Correction by Overwrite** | For corrections, the affected date's data is fully recalculated |
| **Audit Trail** | All corrections logged with operator, timestamp, and reason |
| **Notification** | Stakeholders notified when a correction affects their reports |

---

## 5. Data Privacy Requirements

### 5.1 PII Masking Rules

All personally identifiable information must be masked before entering the analytics layer.

| Data Type | Masking Rule | Example |
|-----------|-------------|---------|
| **Name** | First initial + asterisks | R** C** |
| **Phone** | Middle digits masked | 0912***789 |
| **ID Number** | Middle digits masked | A12****89 |
| **Email** | Username partially masked | r***@gmail.com |

### 5.2 Access Control

| Data Layer | Authorized Access |
|-----------|------------------|
| **Raw Data (Original)** | Database administrators only |
| **Cleaned Data** | Data engineers |
| **Summary Data** | BI analysts, data scientists |
| **Application Data** | All authorized applications |

---

## 6. Data Retention Policy

| Data Tier | Storage Duration | Access Pattern | Cost Category |
|-----------|-----------------|----------------|---------------|
| **Hot Data** | Most recent 3 months | High-frequency queries | Premium (SSD) |
| **Warm Data** | 3 months - 1 year | Occasional queries | Standard (HDD) |
| **Cold Data** | 1+ years | Audit and compliance only | Archive (object storage) |
| **Regulatory Archive** | 7+ years | Regulator requests only | Deep archive |

---

## 7. Multi-Tenant Data Isolation

### 7.1 Isolation Model

| Rule | Description |
|------|-------------|
| **Logical Isolation** | All tenants share the same infrastructure, separated by tenant_id |
| **Mandatory Filtering** | Every query must include tenant_id; queries without it are rejected |
| **Cross-Tenant Reporting** | Platform-level reports aggregate across tenants (platform admin only) |
| **Tenant-Level SLA** | Each tenant has independent data freshness guarantees |

### 7.2 Business Benefits

| Benefit | Description |
|---------|-------------|
| No per-tenant database management | Single infrastructure reduces operational overhead |
| Platform-wide analytics | Cross-tenant insights (e.g., total platform GGR) |
| Consistent quality | Uniform data quality standards across all tenants |
| Cost efficiency | Shared compute and storage resources |

---

## 8. Scheduling & Delivery Requirements

### 8.1 Daily Report Schedule

| Time | Activity | Output |
|------|----------|--------|
| 02:00 - 02:15 | Data cleaning and masking | Cleaned data layer |
| 02:15 - 02:45 | Aggregation calculations | Summary data layer |
| 02:45 - 03:00 | Application table building | Application data layer |
| 03:00 - 03:05 | Data quality validation | Quality report |
| 03:05 | Completion notification | Reports available |

### 8.2 Report Delivery Channels

| Channel | Use Case |
|---------|----------|
| **Dashboard** | Real-time and T+1 metrics visualization |
| **Email** | Scheduled report delivery to stakeholders |
| **Export** | On-demand Excel/PDF/CSV download |
| **Notification** | Alert when report is ready or anomaly detected |

---

## 9. Business SLAs

| SLA | Target | Measurement |
|-----|--------|-------------|
| **Daily Report Availability** | By 03:00 AM | Time when ADS layer data is ready |
| **Report Accuracy** | 100% for financial data | Post-reconciliation error rate |
| **Pipeline Uptime** | 99.5% | Successful pipeline runs / total scheduled runs |
| **Query Response Time** | < 3 seconds (P95) | Application layer query performance |
| **Data Freshness** | < 5 minutes (real-time), T+1 (batch) | Delay from source to analytics |

---

**Document Version**: 4.0.0
**Last Updated**: 2026-02-09
**Maintenance Team**: Data Team & Platform Team
