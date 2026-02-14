# Mermaid Issues Catalog - Phase 12A Discovery

**Generated**: 2026-02-12
**Total Files Scanned**: 102
**Total Mermaid Blocks**: 254
**Blocks with Issues**: 61

---

## Issue Categories

### Category A: Unmatched subgraph/end (45 instances)
**Root Cause**: Missing `subgraph X` declaration before `end` statement, or missing `end` after subgraph.

**High-Impact Files** (≥3 instances):
1. `architecture/02_Finance_Service/Payment_Gateway_API.md` - 8 ends (line 28)
2. `architecture/02_Finance_Service/Turnover_Flowcharts.md` - 7 ends (line 145), 4 ends (line 717)
3. `architecture/02_Finance_Service/Seamless_Wallet_Analysis.md` - 6 ends (line 20), 3 ends (line 116)
4. `architecture/03_Game_Integration/Turnover_Calculation_Logic.md` - 6 ends (line 910)
5. `architecture/06_Platform_Core/MFA_Compliance_Technical.md` - 5 ends (line 858)
6. `architecture/06_Platform_Core/MFA_Login_Recovery_Technical.md` - 5 ends (line 13)
7. `architecture/07_Agent_Service/Credit_Network_Architecture.md` - 5 ends (line 115)
8. `architecture/09_Infrastructure/Multi_Actor_Token_Security.md` - 5 ends (line 293)
9. `architecture/01_Player_Service/Player_Lifecycle_Implementation.md` - 5 ends (line 26)

**Medium-Impact Files** (2-4 instances):
- `architecture/00_Overview/Platform_Architecture.md` - 4 ends
- `architecture/02_Finance_Service/Reconciliation_Technical.md` - 4 ends + 3 ends + 2 ends + 1 end + 2 ends
- `architecture/06_Platform_Core/MFA_Technical.md` - 3 ends + 2 ends + 1 end
- `architecture/06_Platform_Core/MFA_Recovery_Implementation.md` - 3 ends + 1 end
- `architecture/09_Infrastructure/Multi_Actor_Token_Security.md` - 2 ends + 1 end + 3 ends (multiple blocks)
- Plus 13 more files with 1-2 instances each

### Category B: Invalid Arrow Syntax (16 instances)
**Root Cause**: Arrows like `--<`, `-- ` (space), `--|` used instead of standard Mermaid arrows (`-->`, `==>`, `-.->`)

**Files**:
1. `architecture/02_Finance_Service/Payment_Gateway_API.md` - 6 blocks with `--<` arrows (lines 449, 483, 533, 650, 703, 754)
2. `architecture/02_Finance_Service/Seamless_Wallet_Analysis.md` - `-- ` arrows (line 466)
3. `architecture/02_Finance_Service/Turnover_Calculation_Architecture.md` - `-- ` arrows (lines 476, 506)
4. `architecture/03_Game_Integration/Game_Integration_Implementation.md` - `-- ` arrows (lines 105, 225, 409)
5. `architecture/03_Game_Integration/Turnover_Calculation_Logic.md` - `-- ` arrows (line 1010)
6. `architecture/00_Overview/Data_Model.md` - `--o`, `--|` arrows (line 14) - **NOTE**: ERD syntax may be intentional
7. `architecture/05_Risk_Engine/Fraud_Detection_System.md` - `--|` arrows (line 804)
8. `architecture/09_Infrastructure/Deployment_Architecture.md` - `--|` arrows (line 22)

**Note**: Some `--|` and `--o` patterns in ERD diagrams (Entity-Relationship Diagrams) are valid Mermaid syntax for cardinality. Need manual review.

### Category C: Special Cases (0 instances)
**stateDiagram-v2 with `<br/>` tags**: None found (PASS)

---

## Batch Repair Plan

### Phase 12B Execution Strategy
- **Constraint**: Max 3 files per iteration
- **Priority**: High-impact files first (Category A: ≥3 instances)
- **Approach**: Fix Category A (unmatched subgraph/end) first, then Category B (invalid arrows)

### Batch 1 — High-Impact Finance (3 files)
- [ ] `architecture/02_Finance_Service/Turnover_Flowcharts.md` (11 end statements without subgraph)
- [ ] `architecture/02_Finance_Service/Seamless_Wallet_Analysis.md` (9 end statements)
- [ ] `architecture/02_Finance_Service/Payment_Gateway_API.md` (8 end statements + 6 blocks with `--<` arrows)

### Batch 2 — High-Impact MFA/Game (3 files)
- [ ] `architecture/03_Game_Integration/Turnover_Calculation_Logic.md` (6 ends + `-- ` arrows)
- [ ] `architecture/06_Platform_Core/MFA_Compliance_Technical.md` (5 ends)
- [ ] `architecture/06_Platform_Core/MFA_Login_Recovery_Technical.md` (5 ends)

### Batch 3 — High-Impact Platform/Agent (3 files)
- [ ] `architecture/07_Agent_Service/Credit_Network_Architecture.md` (5 ends)
- [ ] `architecture/09_Infrastructure/Multi_Actor_Token_Security.md` (11 ends total across 4 blocks)
- [ ] `architecture/01_Player_Service/Player_Lifecycle_Implementation.md` (5 ends)

### Batch 4 — Medium-Impact Finance/Reconciliation (3 files)
- [ ] `architecture/02_Finance_Service/Reconciliation_Technical.md` (12 ends across 5 blocks)
- [ ] `architecture/00_Overview/Platform_Architecture.md` (4 ends)
- [ ] `architecture/06_Platform_Core/MFA_Technical.md` (6 ends across 3 blocks)

### Batch 5 — Remaining High/Medium Impact (3 files)
- [ ] `architecture/06_Platform_Core/MFA_Recovery_Implementation.md` (4 ends)
- [ ] `architecture/02_Finance_Service/Turnover_Calculation_Logic_Detail.md` (2 ends)
- [ ] `architecture/05_Risk_Engine/Risk_System_Architecture.md` (3 ends)

### Batch 6-10 — Remaining files (1-2 issues each)
- [ ] Remaining 25 files with 1-2 instances each (grouped by directory)

---

## Validation Checklist
- [ ] Run Python script: `python3 scripts/extract-mermaid-blocks.py`
- [ ] Run validate-mermaid.sh: `bash scripts/validate-mermaid.sh docs/iGaming/`
- [ ] Run stateDiagram check: `bash scripts/detect-statediagram-br.sh docs/iGaming/`
- [ ] Git commit after each batch

---

**Last Updated**: 2026-02-12
**Status**: Discovery complete, ready for batch repairs
