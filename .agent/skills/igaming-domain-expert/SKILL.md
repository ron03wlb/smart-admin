---
name: igaming-domain-expert
description: Specialized knowledge for iGaming Architecture, Risk Control, and Financial Reconciliation.
---

# iGaming Domain Expert Skill

## 🎯 Purpose
You are an expert in the **SmartAdmin iGaming Architecture**. You understand the intricacies of High-Concurrency Betting, Real-time Risk Control, Financial Reconciliation, and Regulatory Compliance.
Use this skill when the user asks about:
- **Risk Control** (Bonus Abuse, Arbitrage, Bot Detection)
- **Financial Reconciliation** (PSP, Game Provider, Affiliate, Internal Audit)
- **Unified Wallet** (Cash vs Credit, Multi-Currency, Seamless Wallet)
- **Turnover Validation** (Wagering Requirements, Snapshot Scheme)
- **Compliance** (Responsible Gaming, AML, KYC)

## 🧠 Core Mental Models (Ultrathink)

### 1. The "Snapshot" Turnover Validation (Option A)
- **Problem**: Calculating turnover for users with 10 years of history is slow (O(N)).
- **Solution**: **Incremental Snapshot**.
- **Logic**: `Current_Turnover - Last_Snapshot_Turnover = Delta_Turnover`.
- **Validation**: Performed at **Withdrawal Time**.
- **Consistency**: Risk Checks must match the Snapshot Time Range (Full Consistency Protocol).

### 2. Priority-Based Risk Decision
- **Old Way**: Score Accumulation (0-100). (Deprecated)
- **New Way**: **Priority Classification** (URGENT / HIGH / MEDIUM / LOW).
    - **URGENT**: Blacklist, Confirmed Fraud → **Auto-Block**.
    - **HIGH**: Arbitrage, Bot → **Auto-Block**.
    - **MEDIUM**: Suspicious IP/Device → **Manual Review**.
    - **LOW**: Normal → **Pass**.

### 3. Three-Layer Reconciliation (The Safety Net)
- **Layer 1 (Real-time)**: Defend against Fake Callbacks (30s).
- **Layer 2 (Batch)**: Fix dropped orders (1h).
- **Layer 3 (T+1)**: Financial Compliance & Reporting (24h).
- **Critical Gap**: Must perform **Internal Trial Balance** (`Total In - Total Out == User Balance`) daily.

### 4. Unified Wallet (Cash + Credit)
- **Formula**: `Playable = (Cash + Bonus) + (Credit Limit - Outstanding)`
- **Deduction Priority**: **4-Layer Config** (Player > Provider > Game > System Default).
    - Default: `Bonus -> Cash -> Credit`.
- **Risk**: **Exposure Ratio** (`Outstanding / Limit`).
    - > 100%: Tolerance Mode (Only allow bets that decrease exposure).
    - > 101%: Lock Mode.

## 🗺️ Documentation Map (Where to Look)

| Domain | Key Document | Critical Sections |
|--------|--------------|-------------------|
| **Risk Framework** | `docs/iGaming/04_Risk_Control/04-01_Risk_Framework.md` | Layer 1/2/3, Priority Matrix |
| **Withdrawal Risk** | `docs/iGaming/technical-specs/P1-important/07-withdrawal-risk-correlation.md` | Snapshot Logic, Full Consistency |
| **Turnover Logic** | `docs/iGaming/technical-specs/P1-important/08-turnover-validation-scheme.md` | Snapshot Implementation |
| **Financial Recon** | `docs/iGaming/02_Finance_Center/02-03_Reconciliation_System.md` | PSP 3-Layer Recon |
| **Game Recon** | `docs/iGaming/02_Finance_Center/02-04_Turnover_and_Game_Reconciliation_Analysis.md` | Status Factors, GP Recon |
| **Wallet Model** | `docs/iGaming/02_Finance_Center/02-06_Unified_Wallet_Model.md` | TCC, Deduction Layers |
| **Improvement Plan** | `docs/iGaming/000_improve/000-01_Risk_Control_Improvement.md` | **Master Roadmap** (Phases 1-4) |

## 🛠️ Common Workflows

### Workflow 1: Analyze Withdrawal Stoppage
1. **Check Risk Proposals**: Is there a PENDING proposal with HIGH/URGENT priority?
    - *Source*: `t_risk_proposal`
2. **Check Turnover**: Does `Total_Valid_Turnover - Last_Snapshot < Target`?
    - *Source*: `t_player_turnover_snapshot`
3. **Check Wallet Status**: Is `exposure_ratio > 100%` (Credit User)?

### Workflow 2: Investigate "Missing Money" (Reconciliation)
1. **PSP Side**: Check `02-03` logic. Is it a "Short Payment" (Platform has order, PSP has no money)? -> **Fraud Alert**.
2. **Game Side**: Check `02-04` logic. Is it a "Latency Arbitrage" event? (Bet Time vs Server Time).
3. **Internal Side**: Check `02-09` (Trial Balance). Is the Database integrity compromised?

### Workflow 3: Compliance Audit
1. **RG**: Did the user hit a **Self-Exclusion** or **Deposit Limit**? (Check `01-06`).
2. **Affiliate**: Was the traffic hijacked? (Check **CTIT** in `06-03`).

## 🚨 Critical Rules (Do's and Don'ts)
- **DO NOT** use "Score" for risk decisions. ALWAYS use **Priority**.
- **DO NOT** use fixed 30-day time ranges for old users. ALWAYS scan **Full History** if no Snapshot exists.
- **DO NOT** mix "Turnover" (Face Value) and "Valid Bet" (Effective) for Free Spins.
- **ALWAYS** validate `NGR = Bet - Win - Bonus - Tax` before paying Affiliate Commissions.
