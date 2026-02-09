# iGaming Documentation Quality Gate Process

> **Purpose**: Quarterly validation of documentation quality to ensure Requirements/Architecture layer separation
> **Frequency**: Quarterly (Q1, Q2, Q3, Q4)
> **Owner**: Documentation Team Lead
> **Version**: 1.0.0
> **Last Updated**: 2026-02-09

---

## Overview

The Quality Gate process ensures that iGaming documentation maintains strict layer separation:
- **Requirements Layer**: Pure business requirements (≥95% business purity)
- **Architecture Layer**: Complete technical implementation (100% coverage)
- **Bidirectional Cross-References**: Full traceability between layers

---

## Validation Scripts

### 1. Requirements Layer Purity Check

**Script**: [`scripts/validate-requirements-purity.sh`](../../../scripts/validate-requirements-purity.sh)

**Purpose**: Detect technical keywords in business requirements documents

**Usage**:
```bash
bash scripts/validate-requirements-purity.sh
```

**Exit Codes**:
- `0`: All documents are business-pure ✅
- `1`: Technical keywords detected ❌

**Technical Keywords Detected**:
- Infrastructure: Redis, PostgreSQL, Kafka, Elasticsearch
- Cryptography: HMAC-SHA256, AES-256-GCM, SHA-256
- Frameworks: @Transactional, @Service, RestTemplate
- Architecture Patterns: TCC, SAGA compensation
- Database: CREATE TABLE, SELECT * FROM, primary key
- HTTP: HTTP 200, HTTP 400, HTTP 503
- SmartAdmin: ResponseDTO, PageResult

**Legitimate Exclusions** (will NOT trigger violations):
- Cross-reference lines: `→ **[Technical Implementation](...)`
- Refinement Note headers: `> **Refinement Note**: Technical details (algorithms, patterns) moved to Architecture layer`
- Document metadata: `Related Doc:`, `Canonical Source:`
- Section titles: `## Implementation Timeline`, `### Implementation Process`
- Mermaid diagrams: `B{Select`, `A[Update order]`

---

### 2. Architecture Layer Completeness Check

**Script**: [`scripts/validate-architecture-completeness.sh`](../../../scripts/validate-architecture-completeness.sh)

**Purpose**: Verify Architecture documents contain complete technical implementation

**Usage**:
```bash
bash scripts/validate-architecture-completeness.sh
```

**Exit Codes**:
- `0`: All documents have complete technical content and back-references ✅
- `1`: Missing technical content or back-references ❌

**Coverage Metrics**:
| Metric | Target | Calculation |
|--------|--------|-------------|
| Java Code | ≥70% | Documents containing ` ```java ` code blocks |
| SQL Schema | ≥40% | Documents with `CREATE TABLE` or `CREATE INDEX` |
| YAML Config | ≥30% | Documents with ` ```yaml ` or ` ```yml ` |
| Mermaid Diagrams | ≥70% | Documents with ` ```mermaid ` |
| Business Requirements Backref | 100% | Documents with `> **Business Requirements**: [...]` header |

---

### 3. Cross-Reference Validation

**Script**: [`scripts/validate-cross-references.py`](../../../scripts/validate-cross-references.py)

**Purpose**: Verify bidirectional cross-references between Requirements and Architecture layers

**Usage**:
```bash
python3 scripts/validate-cross-references.py
```

**Exit Codes**:
- `0`: All cross-references are bidirectional ✅
- `1`: Missing back-references detected ❌

**Validation Logic**:
1. Parse Requirements → Architecture forward references (`→ **[Title](../../architecture/path/to/doc.md#anchor)**`)
2. Parse Architecture → Requirements back-references (`> **Business Requirements**: [Title](../../requirements/path/to/doc.md)`)
3. Verify every forward reference has a matching back-reference (by filename)
4. Report missing back-references with actionable recommendations

---

### 4. Quality Gate Report Generator

**Script**: [`scripts/generate-quality-report.sh`](../../../scripts/generate-quality-report.sh)

**Purpose**: Orchestrate all validation scripts and generate comprehensive quarterly report

**Usage**:
```bash
# Generate report for current quarter
bash scripts/generate-quality-report.sh

# Generate report for specific quarter
bash scripts/generate-quality-report.sh --quarter 2026-Q2

# Custom output path
bash scripts/generate-quality-report.sh --quarter 2026-Q2 --output custom-path/report.md
```

**Exit Codes**:
- `0`: All quality gates passed ✅
- `1`: One or more quality gates failed ❌

**Report Sections**:
1. Executive Summary (metrics table)
2. Requirements Layer Purity Check (violations, recommendations)
3. Architecture Layer Completeness Check (coverage, missing back-refs)
4. Cross-Reference Validation (broken links)
5. Manual Review Findings (10% sampling)
6. Action Items (P0/P1/P2 prioritization)
7. Next Quarter Targets

---

## Quarterly Execution Workflow

### Step 1: Pre-Check Preparation (5 minutes)

```bash
# Navigate to project root
cd /path/to/smart-admin

# Verify scripts are executable
chmod +x scripts/validate-*.sh scripts/generate-quality-report.sh

# Ensure Python 3 is available
python3 --version

# Create quality reports directory if not exists
mkdir -p docs/iGaming/quality-reports
```

### Step 2: Execute Quality Gate (2-3 minutes)

```bash
# Generate quarterly report (auto-detects current quarter)
bash scripts/generate-quality-report.sh

# Or specify quarter explicitly
bash scripts/generate-quality-report.sh --quarter 2026-Q1
```

**Output**:
- Report saved to: `docs/iGaming/quality-reports/YYYY-QN-quality-gate-report.md`
- Console output shows pass/fail for each check

### Step 3: Review Report (10-15 minutes)

Open the generated report and review:

1. **Executive Summary**:
   - Requirements Layer Business Purity: Target ≥95%
   - Architecture Layer Technical Coverage: Target 100%
   - Cross-Reference Completeness: Target 100%

2. **Validation Output**:
   - Review detailed violations for each check
   - Identify patterns in failures

3. **Action Items**:
   - **P0 (Critical)**: Fix immediately before next sprint
   - **P1 (High)**: Schedule for current sprint
   - **P2 (Medium)**: Add to backlog

### Step 4: Fix Violations (Variable Time)

**P0: Requirements Layer Technical Keywords** (Estimated 2-4 hours)

For each violated document:
1. Open the file in editor
2. Locate technical keywords (shown in report with line numbers)
3. Evaluate:
   - **If technical detail**: Move to Architecture layer, add cross-reference
   - **If Refinement Note**: Already documented, no action needed
   - **If false positive**: Update validation script exclusion rules
4. Update Refinement Note if technical content was moved
5. Re-run validation: `bash scripts/validate-requirements-purity.sh`

**P1: Architecture Layer Missing Back-References** (Estimated 1-2 hours)

For each Architecture document without back-reference:
1. Identify corresponding Requirements document
2. Add header to Architecture document:
   ```markdown
   > **Business Requirements**: [Title](../../requirements/path/to/doc.md)
   > **Audience**: Architects, Backend Developers
   > **Last Synced**: YYYY-MM-DD
   ```
3. Re-run validation: `bash scripts/validate-architecture-completeness.sh`

**P2: Cross-Reference Broken Links** (Estimated 30 minutes - 1 hour)

1. Review `validate-cross-references.py` output
2. Fix broken file paths or missing files
3. Ensure anchor links are valid
4. Re-run validation: `python3 scripts/validate-cross-references.py`

### Step 5: Re-Generate Report (2 minutes)

After fixing violations:
```bash
bash scripts/generate-quality-report.sh --quarter 2026-Q1
```

**Success Criteria**:
- All 3 checks show ✅ PASSED
- Report status: `> **Status**: ✅ PASSED`

### Step 6: Commit and Archive (5 minutes)

```bash
# Commit fixed documentation
git add docs/iGaming/requirements docs/iGaming/architecture docs/iGaming/quality-reports
git commit -m "docs(iGaming): Q1 2026 quality gate - 100% compliance"

# Tag the quality gate milestone
git tag -a "quality-gate-2026-Q1" -m "iGaming documentation quality gate Q1 2026 passed"
git push origin master --tags
```

---

## Metrics and Trends

### Historical Performance

| Quarter | Req Purity | Arch Coverage | Cross-Ref | Overall | Violations Fixed |
|---------|------------|---------------|-----------|---------|------------------|
| 2026-Q1 | <95% → TBD | 26% → TBD | 0% → TBD | ❌ → TBD | 7 P0, 34 P1, 0 P2 |

### Target Improvement Trajectory

| Quarter | Req Purity | Arch Coverage | Cross-Ref |
|---------|------------|---------------|-----------|
| 2026-Q1 | ≥95% | ≥50% | ≥80% |
| 2026-Q2 | ≥98% | ≥75% | 100% |
| 2026-Q3 | ≥99% | 100% | 100% |
| 2026-Q4 | 100% | 100% | 100% |

---

## Troubleshooting

### Issue: Too Many False Positives in Purity Check

**Symptom**: Business prose words like "select", "update" triggering violations

**Solution**:
1. Review violation context in report
2. Add exclusion pattern to `validate-requirements-purity.sh`:
   ```bash
   grep -v "B{Select"     # Exclude Mermaid diagrams
   grep -v "## .* Implementation"  # Exclude section titles
   ```
3. Re-run validation

### Issue: Cross-Reference Validation Empty Output

**Symptom**: `validate-cross-references.py` produces no output

**Solution**:
1. Verify Python 3 installed: `python3 --version`
2. Check directory paths exist:
   ```bash
   ls docs/iGaming/requirements
   ls docs/iGaming/architecture
   ```
3. Run script with verbose output:
   ```bash
   python3 -v scripts/validate-cross-references.py
   ```

### Issue: Report Generation Script Errors

**Symptom**: `generate-quality-report.sh` exits with heredoc parsing errors

**Solution**:
1. Ensure bash version ≥4.0: `bash --version`
2. Check for unicode/emoji in file names:
   ```bash
   find docs/iGaming -name "*[^a-zA-Z0-9_.-]*"
   ```
3. Run script with debug mode:
   ```bash
   bash -x scripts/generate-quality-report.sh
   ```

---

## Continuous Improvement

### Suggested Enhancements

1. **Automated Fixes**:
   - Script to auto-add missing Architecture back-references
   - Bot to suggest cross-reference links based on content similarity

2. **Dashboard**:
   - Web UI showing historical metrics trends
   - Real-time purity score visualization

3. **Pre-Commit Hooks**:
   - Block commits if purity check fails
   - Auto-run validation on modified .md files

4. **Integration**:
   - CI/CD pipeline integration (GitHub Actions)
   - Slack notifications for quality gate failures

---

## Contact and Support

**Documentation Team Lead**: TBD
**Slack Channel**: #igaming-docs
**Issues**: [GitHub Issues](https://github.com/your-org/smart-admin/issues)

---

**Process Version**: 1.0.0
**Last Review Date**: 2026-02-09
**Next Review Date**: 2026-05-09 (Quarterly)
