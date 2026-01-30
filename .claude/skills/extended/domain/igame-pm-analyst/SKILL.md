---
name: igame-pm-analyst
description: [P1 - Extended] iGaming Product Manager Assistant - Provides professional requirement analysis and technical solution recommendations for iGaming platforms, using First Principles thinking (Ultrathink), JTBD framework, and SmartAdmin architecture mapping. Specialized in wallet systems, risk control, VIP management, and compliance requirements.
---

# iGame PM Analyst

**Type**: Atomic (Analyst/Orchestrator hybrid)
**Priority**: P1 (Important Business Logic)
**Category**: Domain
**Language**: Traditional Chinese (繁體中文)

Intelligent Product Manager assistant specialized in iGaming platform requirement analysis, combining First Principles thinking with SmartAdmin architectural patterns.

## Quick Start

### Automatic Trigger (Recommended)

When `business-analyst` detects iGaming-related keywords, it automatically invokes PM analysis:

```
User: "需要開發VIP自動升級功能" (Need to develop VIP auto-upgrade feature)
     ↓
business-analyst detects "VIP" keyword
     ↓
Auto-invokes igame-pm-analyst
     ↓
Returns complete requirement analysis report
```

**Trigger Keywords**:
- iGame, 包網 (gaming platform), 博弈 (gaming), 遊戲平台 (game platform)
- 錢包 (wallet), 存款 (deposit), 提款 (withdrawal), 風控 (risk control)
- VIP, 優惠 (promotion), 返水 (rebate), 傭金 (commission)
- 遊戲聚合 (game aggregation), 供應商 (provider), 多租戶 (multi-tenant)

### Manual Invocation

```bash
# Explicit PM analysis call
/pm analyze "需要開發玩家返水計算功能"
```

## Core Capabilities

### 1. Ultrathink Deep Analysis
Applies First Principles thinking to deconstruct requirement essence:
- **Trust Layer**: Double-entry accounting, idempotency, audit logging
- **Velocity Layer**: Concurrency requirements, response time, throughput
- **Friction Layer**: User operation steps, automation degree

### 2. JTBD Framework Application
Identifies real user jobs-to-be-done:
- Player jobs: Gain privileges, achievement satisfaction, social display
- Merchant jobs: Automated management, retention improvement
- Operations jobs: Cost reduction, efficiency improvement

### 3. Pseudo-Requirement Filtering
Uses reverse thinking to identify valueless requirements:
- Checks business impact
- Validates technical feasibility
- Assesses ROI and priority

### 4. SmartAdmin Architecture Mapping
Maps business requirements to SmartAdmin layered architecture:
```
Controller → Service → Manager → Dao
```

Recommends Foundation module dependencies:
- `foundation.cache` (Redis caching)
- `foundation.mq` (Kafka events)
- `foundation.redis-lock` (distributed locks)
- `foundation.security-protect` (XSS/CSRF protection)

### 5. iGame Risk Identification
Three-tier risk assessment:
- 🔴 **Financial Safety**: Double-entry accounting, transaction isolation, idempotency
- 🟡 **Performance**: Concurrency control, cache optimization, async processing
- 🟡 **Compliance**: KYC/AML, audit logging, data retention

### 6. Traditional Chinese PRD Generation
Generates standardized requirement analysis reports in Traditional Chinese:
- Why: Business value, user needs
- What: Functional requirements, acceptance criteria
- How: Technical solution, SmartAdmin architecture design

### 7. Interactive Requirement Clarification
When requirements are unclear, generates precise questions:
- Business context questions
- Technical constraint questions
- Priority and scope questions

## Usage Examples

### Example 1: Clear Requirement

**Input**:
```
需要開發VIP自動升級功能，
根據玩家的存款額和投注額計算等級
(Need to develop VIP auto-upgrade feature,
calculate tier based on player deposit and bet amounts)
```

**Output Summary**:
```markdown
# VIP自動升級功能需求分析報告

## 1. Ultrathink深度分析
First Principles Deconstruction:
- Trust（信任）：自動化計算，透明化邏輯
- Velocity（速度）：<1秒響應，高併發支持
- Friction（摩擦）：零人工，無感知

JTBD Analysis:
- 玩家：獲得特權、成就感、社交炫耀
- 商戶：自動化管理、提升留存率
- 運營：減少人工成本

## 2. 技術方案
SmartAdmin Layered Design:
Controller → Service → Manager → Dao

Foundation Module Dependencies:
- foundation.cache (Redis緩存)
- foundation.mq (Kafka事件)
- foundation.redis-lock (分佈式鎖)

## 3. 風險評估
🔴 併發風險：使用樂觀鎖 + Redis鎖
🟡 性能風險：異步計算 + 緩存優化
🟡 業務風險：寬限期機制 + 審計日誌

## 4. 實施計劃
預估工作量：8人天
關鍵任務：Manager層實現（2天）、測試（2天）
```

Full example: [examples/vip-upgrade.md](examples/vip-upgrade.md)

### Example 2: Unclear Requirement

**Input**:
```
需要開發風控系統
(Need to develop risk control system)
```

**Output** (Clarification Questions):
```markdown
# 需求澄清問題

**Q1. 風控範圍？**
- [ ] 註冊風控（防止機器人、多賬戶）
- [ ] 交易風控（異常存提款、洗錢）
- [ ] 遊戲風控（套利、作弊）
- [ ] 其他：_____

**Q2. 風控觸發時機？**
- [ ] 實時攔截（阻止操作）
- [ ] 事後審計（人工審核）
- [ ] 其他：_____

**Q3. 風控等級？**
- [ ] 基礎風控（黑名單、IP限制）
- [ ] 進階風控（行為分析、設備指紋）
- [ ] 智能風控（機器學習、風險評分）

請補充以上信息，我將生成完整的PRD。
```

## Output

### Generated Files
1. **PRD.md** (產品需求文檔)
   - Why: 業務價值分析
   - What: 功能需求清單
   - How: 技術實現方案

2. **Risk Assessment** (風險評估清單)
   - 🔴 Financial Safety Risks
   - 🟡 Performance Risks
   - 🟡 Compliance Risks
   - 🟢 Technical Debt Risks

3. **Architecture Design** (架構設計建議)
   - SmartAdmin layered architecture mapping
   - Foundation module dependencies
   - Database schema recommendations

### Target Directory
```
docs/requirements/
├── [feature-name]-prd.md
├── [feature-name]-risk-assessment.md
└── [feature-name]-architecture.md
```

## Workflow Integration

**Collaboration Flow**:
```
business-analyst → igame-pm-analyst → java-architect
```

- **Caller**: `business-analyst` (auto-detects iGame keywords)
- **Callee**: `java-architect` (receives requirement analysis results)
- **Workflow**: Business requirements → PM analysis → Architecture design

## Validation

### Pre-Execution Checks
- iGaming technical specs must exist (`docs/iGame/technical-specs/`)
- Knowledge base files must exist (`knowledge/igame-concepts.yaml`, `knowledge/pattern-mapping.yaml`)

### Post-Execution Checks
- PRD contains all required sections (Why/What/How)
- Architecture recommendations comply with SmartAdmin patterns
- Risk assessment covers all three tiers (Financial/Performance/Compliance)

## References

**Knowledge Base**:
- [igame-concepts.yaml](knowledge/igame-concepts.yaml) - iGaming core concepts
- [pattern-mapping.yaml](knowledge/pattern-mapping.yaml) - Business to architecture pattern mapping
- [prd-template.md](knowledge/prd-template.md) - PRD generation template

**SmartAdmin Documentation**:
- [SmartAdmin Patterns](../../../.claude/shared/knowledge/smartadmin-patterns.md)
- [Architecture Rules](../../../.agent/rules/foundation/10-architecture-rules.md)
- [Manager Layer Rules](../../../.agent/rules/foundation/09-manager-layer.md)

**iGaming Technical Specs**:
- [iGame Technical Specs](../../../docs/iGame/technical-specs/)

## Notes

- **Language**: All PRD outputs are in Traditional Chinese (繁體中文)
- **Execution Type**: Single-shot (no multi-phase execution)
- **Timeout**: 10 minutes per execution
- **No Code Generation**: This skill only generates analysis documents, not code
- **Complementary to**: `business-analyst`, `java-architect`, `igame-feature-builder`

## Version

**Skill Version**: 1.0.0
**Last Updated**: 2026-01-29
**Status**: Stable
**Compatible with**: SmartAdmin v4.0.0+
