# Deep-Plan Interview Transcript — 01-governance-agent

**Date**: 2026-03-26
**Interviewer**: Claude (Claudian)
**Interviewee**: Ron
**Subject**: Governance & Agent Domain Implementation Decisions

---

## Q1: Multi-Tenant Data Isolation Strategy

**Question**: For multi-tenant data isolation, research shows three approaches: (A) Shared DB + PostgreSQL RLS, (B) Hybrid — RLS for small tenants + dedicated DB for premium/regulated brands, (C) Schema-per-tenant. Which approach fits?

**Answer**: **Hybrid RLS + dedicated DB**

Small agents share DB with RLS; UKGC/large brands get their own dedicated database. Best balance of cost and compliance.

---

## Q2: Commission Model Concurrency

**Question**: Can a single agent have MULTIPLE active commission models simultaneously (e.g., CPA for new players + RevShare for existing), or strictly one model at a time?

**Answer**: **Configurable per agent agreement**

Platform supports both modes; each agent contract specifies whether they use a single model or multiple concurrent models.

---

## Q3: Credit Cascade on Exhaustion

**Question**: When a mid-level agent's credit is fully used and a player tries to bet, what happens?

**Answer**: **Reject bet immediately**

Hard stop. Player sees 'insufficient credit' error. Safest approach.

---

## Q4: Technology Stack

**Question**: Confirmed tech stack?

**Answer**: **Spring Boot 3.x + Java 21 + PostgreSQL**

Modern stack aligned with SmartAdmin V3.0 pattern.

---

## Q5: Maker-Checker Scope

**Question**: Which operations should REQUIRE dual approval beyond what's in the spec?

**Answer**: **All of the above (broadest scope)**

Dual approval for: credit adjustments, settlement approval, agent creation/deactivation, commission changes, hierarchy changes, PLUS tenant creation, billing model changes, AND all 3-layer configuration overrides.

---

## Q6: Settlement Failure Handling

**Question**: How should settlement payment failures be handled?

**Answer**: **Auto-retry with escalation**

Auto-retry up to 3 times over 3 business days; if still failing, escalate to manual queue with alert.

---

## Q7: Feature Flag Strategy

**Question**: Should we implement tenant-level feature flags here (governance split) or defer to infrastructure?

**Answer**: **Implement basic tenant-level flags here**

Define the flag schema + API in this split. Other splits consume the API. Governance as the flag authority.

---

## Q8: Audit Trail Lifecycle

**Question**: Should audit data compression and archival be part of THIS split's scope?

**Answer**: **Full audit lifecycle here**

This split owns creation, verification, tiering, and archival. Complete self-contained audit module.

---

## Q9: Expected Scale

**Question**: Expected scale at launch and 1-year maturity?

**Answer**: **Large: 10+ brands, 50+ tenants, 1000+ agents**

Mature platform operating across multiple jurisdictions.

---

## Q10: Credit Check Performance

**Question**: With 1000+ agents and 10-level hierarchies, how should credit checks perform?

**Answer**: **Redis credit cache + event-driven sync**

Pre-computed credit summaries in Redis. Updated via events on credit changes. O(1) lookup per bet.

---

## Q11: Identity & Authentication

**Question**: Should the platform use an external IdP or build auth in-house?

**Answer**: **Keycloak self-hosted**

Open-source, supports SAML/OIDC, RBAC, impersonation out of the box. Self-hosted for compliance.

---

## Decision Summary

| # | Decision | Choice |
|---|----------|--------|
| Q1 | Data isolation | Hybrid RLS + dedicated DB |
| Q2 | Commission models | Configurable per agent (single or concurrent) |
| Q3 | Credit exhaustion | Reject bet immediately |
| Q4 | Tech stack | Spring Boot 3.x + Java 21 + PostgreSQL |
| Q5 | Maker-Checker | Broadest scope (all sensitive operations) |
| Q6 | Settlement failures | Auto-retry 3x/3 days + escalate |
| Q7 | Feature flags | Basic tenant-level flags in this split |
| Q8 | Audit lifecycle | Full lifecycle in this split |
| Q9 | Scale target | Large (10+ brands, 50+ tenants, 1000+ agents) |
| Q10 | Credit performance | Redis cache + event-driven sync |
| Q11 | Authentication | Keycloak self-hosted |
