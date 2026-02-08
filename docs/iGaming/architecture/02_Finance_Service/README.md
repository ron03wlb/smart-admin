# 02 Finance Service

> **Audience**: Architects, Backend Developers, DevOps
> **Status**: Phase 6 Complete - 8 split documents + source index

---

## Split Documents

| Document | Description | Source |
|----------|-------------|--------|
| [Payment Gateway API](Payment_Gateway_API.md) | Payment provider API integration, webhooks, callback handling | [source](../../source/02_Finance_Center/02-02_Payment_Gateway_Integration.md) |
| [Reconciliation Technical](Reconciliation_Technical.md) | Three-way matching engine, scheduled reconciliation jobs, data models | [source](../../source/02_Finance_Center/02-03_Reconciliation_System.md) |
| [Financial Implementation](Financial_Implementation.md) | Wallet system architecture, payment gateway API, risk scoring engine, SAGA orchestrator | [source](../../source/00_Foundation/guides/00-11_Financial_Implementation.md) |
| [Turnover Flowcharts](Turnover_Flowcharts.md) | Turnover validation flow diagrams, game weight application, bet lifecycle state machine | [source](../../source/02_Finance_Center/02-04-diagrams/02-04-01_Flowcharts_and_Sequences.md) |
| [Turnover Calculation Architecture](Turnover_Calculation_Architecture.md) | Three-layer validation implementation, event-driven data exchange, SmartAdmin mapping | [source](../../source/02_Finance_Center/02-04_Turnover_and_Game_Reconciliation_Analysis.md) |
| [Turnover Implementation](Turnover_Implementation.md) | Wallet deduction algorithm, effective stake calculation, lockAmount state transitions | [source](../../source/02_Finance_Center/02-04-diagrams/02-04-03_Implementation_Details.md) |
| [Seamless Wallet Analysis](Seamless_Wallet_Analysis.md) | Seamless wallet API specs, concurrency control, state machines, exception handling | [source](../../source/03_Game_Center/03-03_Seamless_Wallet_Analysis.md) |
| [Turnover Calculation Logic Detail](Turnover_Calculation_Logic_Detail.md) | effectiveStake formulas, lockAmount lifecycle, rebate calculation, withdrawal turnover | [source](../../source/02_Finance_Center/02-04-diagrams/02-04-02_Calculation_Logic.md) |

## Core Architecture Documents (Source Index)

| Document | Description | Source |
|----------|-------------|--------|
| [Wallet Architecture](../../source/02_Finance_Center/02-06_Wallet_Architecture.md) | Multi-currency wallet design, balance management, and ledger structure | [source](../../source/02_Finance_Center/02-06_Wallet_Architecture.md) |
| [Transaction Processing Flow](../../source/02_Finance_Center/02-07_Transaction_Processing_Flow.md) | End-to-end transaction lifecycle and state machine | [source](../../source/02_Finance_Center/02-07_Transaction_Processing_Flow.md) |

## Seamless Wallet Technical Documents

| Document | Description | Source |
|----------|-------------|--------|
| [Concurrency Control](../../source/02_Finance_Center/seamless-wallet/02-SW-02_Concurrency.md) | Optimistic locking, race condition handling, and distributed locks | [source](../../source/02_Finance_Center/seamless-wallet/02-SW-02_Concurrency.md) |
| [Recovery Mechanisms](../../source/02_Finance_Center/seamless-wallet/02-SW-03_Recovery.md) | Transaction rollback, compensation, and failure recovery | [source](../../source/02_Finance_Center/seamless-wallet/02-SW-03_Recovery.md) |
| [Accounting Integration](../../source/02_Finance_Center/seamless-wallet/02-SW-04_Accounting.md) | Double-entry bookkeeping and GL integration | [source](../../source/02_Finance_Center/seamless-wallet/02-SW-04_Accounting.md) |

## Subdirectories

| Directory | Description |
|-----------|-------------|
| [Seamless_Wallet/](./Seamless_Wallet/) | Detailed seamless wallet architecture documents |

---

**Last Updated**: 2026-02-08
