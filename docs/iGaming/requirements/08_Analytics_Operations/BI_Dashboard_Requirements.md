# BI Dashboard & Data Pipeline Business Requirements

> **Canonical Source**: [source-archive/08_Analytics_BI/08-04_Reporting_Architecture.md](../../source-archive/08_Analytics_BI/08-04_Reporting_Architecture.md)
> **View Type**: Business Requirements
> **Target Audience**: Product Managers, Compliance Officers
> **Related Doc**: [BI Dashboard Architecture](../../architecture/08_Analytics_Service/BI_Dashboard_Architecture.md)
> **Last Synced**: 2026-02-09

---

## 1. Business Overview

The BI Dashboard and Data Pipeline system addresses the operational analytics needs of the iGaming platform. It provides real-time monitoring for operations teams, historical analysis for executives, and compliance reporting for regulators. The system supports both immediate operational decisions (within seconds) and strategic analysis (next-day reports).

---

## 2. Dashboard Requirements by Role

### 2.1 Operations Dashboard (Real-time)

| Widget | Metric | Refresh Rate | Purpose |
|--------|--------|-------------|---------|
| Online Players | Current connected users | Every 10 seconds | Capacity monitoring |
| Today's Deposits | Cumulative deposit total | Every 5 minutes | Revenue tracking |
| Today's GGR | Gross Gaming Revenue | Every hour | Revenue monitoring |
| Risk Alerts | Active risk event count | Real-time | Incident response |
| PSP Status | Payment provider availability | Every minute | Service monitoring |

### 2.2 Executive Dashboard (T+1)

| Widget | Metric | Refresh Rate | Purpose |
|--------|--------|-------------|---------|
| Monthly P&L | Profit & Loss statement | Daily | Financial health |
| Player LTV Trends | Lifetime value by segment | Weekly | Strategic planning |
| Market Performance | Revenue by jurisdiction | Daily | Market expansion decisions |
| Agent Network Health | Top agents, commission ratios | Weekly | Partner management |
| Compliance Status | Regulatory report readiness | Monthly | Risk mitigation |

### 2.3 Self-Service Analytics

| Capability | Description | Target Users |
|-----------|-------------|-------------|
| Ad-hoc Queries | Free-form query builder | BI Analysts |
| Pre-built Templates | Standard analysis templates | Operations team |
| Custom Dashboards | Drag-and-drop dashboard builder | All authorized users |
| Scheduled Reports | Automated periodic report delivery | Managers |

---

## 3. Data Timeliness Requirements

### 3.1 Data Path Categories

| Path Type | Maximum Delay | Data Completeness | Use Case |
|-----------|--------------|-------------------|----------|
| **Hot Path** | < 5 seconds | May have gaps | Operations monitoring, risk alerts |
| **Warm Path** | 5-60 seconds | High (99%+) | Real-time dashboards, anomaly detection |
| **Cold Path** | Next day (T+1) | 100% complete | Business reports, settlement reconciliation |

### 3.2 Report Freshness Requirements

| Report Type | Delivery Time | Stakeholder |
|-------------|--------------|-------------|
| Real-time KPI Dashboard | Continuous (< 5 min delay) | Operations team |
| Daily Financial Report | By 03:00 AM next day | Finance team |
| Weekly Agent Settlement | By Monday 06:00 AM | Agent operations |
| Monthly Compliance Report | By 2nd of following month | Compliance team |
| Annual Audit Report | By January 15th | External auditors |

---

## 4. Data Quality Requirements

### 4.1 Quality Dimensions

| Dimension | Definition | Target |
|-----------|------------|--------|
| **Completeness** | All expected records are present | > 99.9% |
| **Accuracy** | Values match source of truth | 100% (financial), 99%+ (operational) |
| **Timeliness** | Data available within SLA window | 99.5% |
| **Consistency** | Same metric yields same result across views | 100% |
| **Uniqueness** | No duplicate records | 100% |

### 4.2 Data Correction Policy

When historical transactions are voided or adjusted:

| Policy | Description |
|--------|-------------|
| **Immutability** | Historical reports are never modified retroactively |
| **Idempotent Overwrite** | Recalculate affected date partition from raw data |
| **Audit Trail** | All corrections logged with timestamp, reason, and operator |

---

## 5. Multi-Tenant Data Requirements

### 5.1 Isolation Rules

| Rule | Description |
|------|-------------|
| **Logical Isolation** | All tenants share infrastructure, separated by tenant_id |
| **Mandatory Filtering** | Every query must include tenant_id filter |
| **Cross-Tenant View** | Only platform-level admins can view across tenants |
| **Platform Reports** | Aggregated cross-tenant reports (e.g., total platform GGR) available to platform admins only |

### 5.2 Tenant-Level Customization

| Customization | Description |
|---------------|-------------|
| Custom KPIs | Tenants can define additional tracking metrics |
| Report Branding | Tenant logo and colors on exported reports |
| Dashboard Layout | Customizable widget arrangement |
| Scheduled Delivery | Tenant-specific report delivery schedule |

---

## 6. Data Retention Business Rules

| Data Category | Retention Period | Storage Tier | Access Pattern |
|--------------|-----------------|-------------|----------------|
| **Real-time metrics** | 5 minutes | Hot (in-memory) | Very high frequency |
| **Recent operational data** | 3 months | Fast (SSD) | High frequency |
| **Historical reports** | 3 months - 1 year | Standard (HDD) | Moderate frequency |
| **Archived data** | 1+ years | Cold (object storage) | Audit/compliance only |
| **Compliance archives** | 7+ years | Cold (object storage) | Regulatory requests only |

---

## 7. Data Privacy & Masking Requirements

All personally identifiable information (PII) must be masked before entering the analytics layer.

| Data Type | Masking Rule | Example |
|-----------|-------------|---------|
| **Name** | First and last initial + asterisks | R** C** |
| **Phone** | Partial digits visible | 0912***789 |
| **ID Number** | Partial digits visible | A12****89 |
| **Email** | Partial address visible | r***@gmail.com |

---

## 8. Processing Mode Decision Guide

For product managers evaluating new report requests:

| Question | Answer: Real-time | Answer: Batch (T+1) |
|----------|-------------------|---------------------|
| Does the business lose money with a 1-day delay? | Yes | No |
| Is the metric used for immediate operational decisions? | Yes | No |
| Does regulatory compliance require real-time monitoring? | Yes | No |
| Is the calculation simple (counts, sums)? | Suitable for real-time | Either |
| Does the calculation require complex joins across tables? | Consider batch instead | Yes |
| Must the data be 100% accurate? | Consider hybrid approach | Yes |

---

## 9. BI Tool Requirements

### 9.1 Tool Selection Criteria

| Criterion | Requirement |
|-----------|-------------|
| **Self-service** | Business users can build queries without engineering support |
| **Real-time** | Support for live dashboards with sub-minute refresh |
| **Export** | Excel, PDF, CSV export capabilities |
| **Embedding** | Dashboards embeddable in admin portal |
| **Access Control** | Role-based data access restrictions |
| **Cost** | Preference for open-source solutions |

### 9.2 Recommended Tool Allocation

| Tool Category | Use Case | Users |
|--------------|----------|-------|
| **Real-time Monitoring** | Live KPIs, system health, risk alerts | Operations team |
| **Self-service Analytics** | Business team queries, player segmentation, campaign analysis | Business analysts |
| **Advanced Analytics** | Data scientist deep analysis, custom SQL, custom visualizations | Data team |

---

**Document Version**: 4.0.0
**Last Updated**: 2026-02-09
**Maintenance Team**: Data Team & BI Team
