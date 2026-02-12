# Ralph Guardrails - iGaming Documentation Optimization

> This file is read at the start of every iteration to avoid known pitfalls.
> Update this file whenever you discover a new trap.

---

## Known Pitfalls

### P1: source-archive/ is READ-ONLY
- Files under `docs/iGaming/source-archive/` are the Single Source of Truth (SSOT)
- NEVER modify them — only `requirements/` and `architecture/` files should be edited

### P2: Mermaid diagram rules differ by type
- ALL diagram types (graph, flowchart, sequenceDiagram, classDiagram, etc.): USE `<br/>` for line breaks
- EXCEPTION: `stateDiagram-v2` CANNOT use `<br/>` — use multi-line note blocks instead
- Detection script: `./scripts/detect-statediagram-br.sh docs/iGaming/`
- Validation script: `./scripts/validate-mermaid.sh docs/iGaming/`

### P3: 09_Infrastructure architecture docs are special
- 22 files in `architecture/09_Infrastructure/` have NO corresponding requirements docs (most are pure technical)
- Only 2 have clear requirements counterparts:
  - `Cost_Optimization_Architecture.md` → `requirements/09_Infrastructure_Requirements/Cost_Optimization_Requirements.md`
  - `QA_Standards.md` → `requirements/09_Infrastructure_Requirements/QA_Standards_Requirements.md`
- The other 20 should use: `> **Business Requirements**: N/A — Pure technical infrastructure document`

### P4: Cross-reference header placement
- `> **Canonical Source**:` goes on line 3 (after title)
- `> **Related Architecture**:` or `> **Business Requirements**:` goes on line 5 (after Canonical Source)
- Always check existing header format before adding — some files use slightly different patterns
- NEVER duplicate an existing cross-reference — check first with grep

### P5: Canonical Source display text inconsistency
- 24 files have display text `[source/...]` but href correctly points to `../../source-archive/...`
- Fix: change display text to match the href (replace `source/` with `source-archive/` in the label)
- The actual link (href) is already correct — do NOT change the link target

### P6: Glossary and Standards docs
- `Industry_Glossary.md` and `Terminology_Standards.md` are reference documents
- They may not have direct architecture counterparts
- Use: `> **Related Architecture**: N/A — Reference/glossary document`

### P7: File path verification
- Before creating a cross-reference, ALWAYS verify the target file exists
- Use `test -f docs/iGaming/<path>` or `ls docs/iGaming/<path>`
- Architecture docs in 09_Infrastructure have many specialized subdocs

### P8: Read before edit
- ALWAYS read a file before editing it
- The Edit tool requires prior Read — will fail otherwise
- Check existing headers to avoid duplicating cross-references

### P9: Always verify target file paths exist BEFORE adding to task list
- Phase 5 originally listed 7 target files — ALL were fabricated (did not exist on filesystem)
- Root cause: previous session generated file names from assumptions, not from actual `ls` or `find`
- **ALWAYS run** `test -f <path>` or `ls <directory>` before including a file in the task list
- Prefer `grep -rL` to find files MISSING specific content (e.g., `grep -rL '```mermaid'` to find files without Mermaid)
- This guardrail applies to both Ralph loop AND manual task planning

### P10: Phase 6 dual-missing strategy
- 8 files missing BOTH Mermaid AND SQL → each enhancement gives +1 to both counters
- Complete ALL 8 dual-missing files before moving to single-missing files
- Expected: After 8 dual-missing files, Mermaid 91.8%, SQL 72.6%

### P11: Frontend/thin document content guidelines
- Files in 11_Frontend/ and 14_Third_Party/ are often under 300 lines
- Keep additions proportional: 1 diagram + 1-2 SQL tables is sufficient
- Do NOT over-engineer content for documents marked as PLANNED
- SQL tables should be frontend-relevant (configs, experiments, localization)

### P12: Business completeness — derive, don't fabricate
- When adding Business Value / Success Metrics / Acceptance Criteria sections to requirements files
- Content MUST be derived from the existing document text, NOT invented
- Read the document first, understand what it describes, then summarize as business value
- If the document doesn't have enough context to derive a section, add a minimal placeholder and mark as TODO
- Check with: `bash scripts/measure-business-completeness.sh`

### P13: SmartAdmin Java code pattern rules
- Architecture docs containing Java code examples must follow SmartAdmin conventions:
  - ❌ `@Autowired` field injection → ✅ `@RequiredArgsConstructor` + `private final`
  - ❌ `@Transactional` in Service class → ✅ `@Transactional(rollbackFor = Throwable.class)` in Manager class
  - ❌ `java.util.Optional` in Service → ✅ `io.vavr.control.Option`
- When fixing: extract the @Transactional method to a new Manager class, keep Service calling Manager
- Check with: `bash scripts/check-smartadmin-patterns.sh`

### P14: Terminology standardization (Updated 2026-02-11)
- **Language**: 繁體中文為主，技術術語保留英文 (Traditional Chinese primary, technical terms remain in English)
- **Technical Terms (保留英文)**:
  - SmartAdmin: Controller, Service, Manager, Dao, Repository, Entity, VO, DTO, Form
  - Annotations: @Transactional, @RequiredArgsConstructor, @SaCheckPermission, @Cacheable
  - Infrastructure: API, REST, HTTP, JSON, YAML, Database, PostgreSQL, Redis, Kafka
  - Architecture: Multi-Tenant, Row-Level Security (RLS), Session, Token, JWT, OAuth
  - Data Types: Option (Vavr), Try, Either, ResponseDTO, PageResult
  - Code: All class names, method names, variable names, table names, column names
- **Business Terms (繁體中文 with English notation on first use)**:
  - "有效投注額 (Valid Turnover)" - subsequent uses: "有效投注額"
  - "可下注餘額 (Playable Balance)" - subsequent uses: "可下注餘額"
  - "自我排除 (Self-Exclusion)" - subsequent uses: "自我排除"
  - "身份驗證 (KYC, Know Your Customer)" - subsequent uses: "KYC" or "身份驗證"
  - "反洗錢 (AML, Anti-Money Laundering)" - subsequent uses: "AML" or "反洗錢"
- **Translation Rules**:
  1. Technical terms NEVER translate (PlayerService stays as PlayerService)
  2. Business terms use Traditional Chinese + English notation on first appearance
  3. Code snippets (Java/SQL/YAML) remain entirely in English
  4. Mermaid diagram labels use Traditional Chinese, but class/method names stay in English
  5. SQL table/column names remain in English, but COMMENT uses Traditional Chinese
- **Exception**: Chinese terms may appear in code comments within source-archive/ (READ-ONLY)
- **Reference**: [docs/iGaming/TRANSLATION_GLOSSARY.md](../iGaming/TRANSLATION_GLOSSARY.md) - 500+ term mappings
- **Check with**:
  - `bash scripts/check-technical-terms.sh` - Verify technical terms remain in English
  - `bash scripts/validate-zh-tw-encoding.sh` - Check Traditional Chinese encoding
  - `bash scripts/check-terminology-consistency-zh-tw.sh` - Verify term consistency

### P15: Never decrease existing coverage
- Mermaid must stay at 100% (73/73 core files)
- SQL must stay at ≥80% (≥59 files)
- Back-references must stay at 100%
- Requirements purity must stay at 100%
- SSOT violations must stay at 0
- Always run full validation after changes to ensure no regression

<!--
### P16: PR-triggered validation mode rules (Added 2026-02-12) - TEMPORARILY DISABLED
User decided to restore continuous execution mode for 20-iteration test.
This rule is commented out but preserved for future reference.

- Ralph now operates in **PR-triggered mode** (NOT continuous execution)
- **Validation scope**: ONLY check files changed in the PR (not entire codebase)
- **Quality gates**: 5 checks (Technical Terms, Encoding, Terminology, Mermaid, Links)
- **Severity classification**:
  - **CRITICAL**: Technical Terms (100%) → Immediate PR failure
  - **HIGH**: Encoding (< 2 errors), Mermaid Syntax (100%) → PR failure
  - **MEDIUM**: Terminology (>= 95%), Links (< 5%) → Warning (allow merge with review)
- **Report format**: Markdown with grouped errors by severity
- **No auto-translation**: Ralph validates only, does not translate
- **Metrics tracking**: Record validation results to `docs/ralph/metrics/pr-validation-*.json`
- **Validation scripts**:
  - `bash scripts/check-technical-terms.sh` - CRITICAL
  - `bash scripts/validate-zh-tw-encoding.sh` - HIGH
  - `bash scripts/check-terminology-consistency-zh-tw.sh` - MEDIUM
  - `bash scripts/validate-mermaid.sh` - HIGH
- **Integration points**:
  - Pre-commit Hook: `.git/hooks/pre-commit` (integrated with Spotless)
  - GitHub Actions: `.github/workflows/igaming-translation-quality.yml`
-->

---

## Lessons Learned
(Auto-populated during execution — add entries here when you discover new pitfalls)

### 2026-02-10: Phantom file references in Phase 5
- All 7 original Phase 5 targets were non-existent files
- Fixed by using `grep -rL` to find actual files missing content
- Strategy: select files missing BOTH Mermaid AND SQL for maximum efficiency

### 2026-02-11: --print flag prevents tool usage
- `claude --print` = text-only output mode, Claude CANNOT use Read/Write/Edit/Bash tools
- Root cause of 30 failed iterations (0 file changes)
- Fix: remove `--print` from ralph-igaming-docs.sh, keep `--dangerously-skip-permissions`

### 2026-02-11: macOS realpath -m incompatibility
- GNU `realpath -m` (canonicalize missing paths) not supported on macOS BSD
- Causes scan-broken-links.sh to report ALL links as broken
- Fix: use relative path resolution `"$file_dir/$link_path"` instead of `realpath -m`

### 2026-02-11: validate_links.sh code block false positives
- Regex patterns inside code blocks (e.g., `[a-zA-Z0-9]`) were parsed as markdown links
- Fix: Python extraction now strips fenced code blocks and inline code before extracting links

### 2026-02-12: Rate limit regex missed "You've hit your limit" message
- Actual error: `You've hit your limit · resets 2am (Asia/Taipei)`
- Old regex only matched: `rate.?limit|usage.?limit|capacity|overloaded|429`
- Root cause: 91 iterations (#30-#120) burned through with 0 progress (no rate limit sleep)
- Fix: Added `hit.+limit|your.+limit|resets [0-9]+am` to grep pattern in ralph-igaming-docs.sh line 169
- Impact: Wasted all 120 iteration quota; only 29 effective iterations completed 65 tasks

### 2026-02-12: Ralph mode switch - Continuous to PR-triggered
- **Stopped at**: Phase 9 Batch 3 (13/184 files completed, 5%)
- **Reason**: Transitioning to PR-triggered validation mode for better quality control and cost efficiency
- **Previous mode**: 24h continuous loop with token monitoring (Mode B - Fresh Context)
- **New mode**: PR-triggered quality validation (no auto-translation)
- **Translation execution**: Moved to independent tool (`scripts/translate-igaming-docs.py`)
- **Quality infrastructure**: Implemented 4 validation scripts + Pre-commit Hook + GitHub Actions
- **Benefits**:
  - Better separation of concerns (translation vs validation)
  - Cost-effective (only run on PR events, not 24/7)
  - Faster feedback (< 2 min validation vs hours of continuous loop)
  - Integration with existing Spotless hook
- **Ralph's new role**: Quality gatekeeper for iGaming documentation translation
- **Reference**: Phase 1-2 completed in commit `5892b1e3`

### 2026-02-12: Ralph mode rollback - PR-triggered to Continuous
- **Reason**: User requested 20-iteration continuous execution test
- **Rolled back**: commit `708644a4` (Phase 2 PR-triggered mode)
- **Kept**: Phase 1 validation scripts (still useful for post-iteration checks)
- **Impact**: Ralph resumes translation duties (not just validation)
- **Duration**: Temporary (20 iterations test, then re-evaluate)

### P17: Hybrid intelligent wait strategy for rate limits (Added 2026-02-12)
- **Problem**: Ralph script misclassified "resets Xpm" daily limit as "regular rate limit", causing ineffective 5-minute retries
- **Root Cause**: Pattern `resets [0-9]+am` only matched AM times, not PM times like "resets 4pm"
- **Solution**: Implemented 3-layer hybrid intelligent wait strategy:
  - **Layer 1**: Daily reset detection (`resets [0-9]+(am|pm)`) - Calculates precise wait time until reset, capped at 4 hours
  - **Layer 2**: 5-hour limit detection (`5.?hour|five.?hour`) - Fixed 60-minute wait + quota reset
  - **Layer 3**: Exponential backoff for unknown limits - 5min → 10min → 20min → 40min → 80min → 120min (max)
- **Safety Mechanism**: Exit after 5 consecutive retry failures (prevents infinite loops)
- **Cross-Platform**: Fallback logic for macOS/Linux/Windows Git Bash date command differences
- **Reset Logic**: `RATE_LIMIT_RETRY_COUNT` resets to 0 after successful validation
- **Logging**: Enhanced with limit type, wait duration, and retry count for transparency
- **Implementation**: Functions `calculate_wait_until()` and `handle_rate_limit()` in `ralph-igaming-docs.sh`
- **Expected Impact**: Zero human intervention for rate limit handling, 48-hour continuous execution capability
- **Test Coverage**: Verified daily reset (4pm/2am/12pm), 5-hour limit, and exponential backoff patterns
- **Reference**: Plan v1.2.0 in `C:\Users\ron.chang\.claude\plans\elegant-pondering-sparrow.md`

### P18: Validation scripts must exclude source-archive/ (Added 2026-02-12)
- **Problem**: `check-terminology-consistency-zh-tw.sh` v1.0 counted all 380 files (including 191 READ-ONLY source-archive files), reporting 90% consistency when editable files were actually at 94%
- **Root Cause**: Script used `find "$target" -name "*.md"` without excluding source-archive directory
- **Fix**: Added source-archive/ skip rule in check_file() + smart compound term exclusion for "流水"
- **Impact**: Consistency score jumped from 90% → 100% after script enhancement + 2 terminology fixes
- **Lesson**: Any validation script scanning docs/iGaming/ must skip source-archive/ (per P1 guardrail)
- **Compound Terms**: "流水" in compound forms (流水要求/進度/計算/對帳/操縱) is legitimate and should NOT be flagged

### P19: Mermaid rendering requires mmdc CLI validation (Added 2026-02-12)
- **Problem**: `validate-mermaid.sh` only checks basic patterns (stateDiagram `<br/>`, `\n` in labels)
- **Root Cause**: 72 pre-existing Mermaid rendering issues are NOT caught by pattern-based checks
- **Fix**: Use `npx -p @mermaid-js/mermaid-cli mmdc -i block.mmd -o /dev/null` for actual rendering validation
- **Impact**: Enables Phase 12 to identify and fix all rendering issues systematically
- **Installation**: `npx -p @mermaid-js/mermaid-cli mmdc --version` (no global install needed)
- **Workflow**: Extract each `\`\`\`mermaid ... \`\`\`` block to temp file → run mmdc → check exit code

### P20: Mermaid `<br/>` convention conflict resolution (Added 2026-02-12)
- **Problem**: `mermaid-syntax-check.yml` says NO `<br/>` in Mermaid; SmartAdmin convention says USE `<br/>`
- **Root Cause**: Two conflicting CI/CD workflows created at different times with different assumptions
- **Resolution**: SmartAdmin convention wins (defined in CLAUDE.md, enforced project-wide)
  - ✅ Use `<br/>` in ALL Mermaid types (graph, flowchart, sequenceDiagram, classDiagram, etc.)
  - ❌ EXCEPTION: `stateDiagram-v2` CANNOT use `<br/>` (P2 guardrail)
- **Action**: Phase 14 will update `mermaid-syntax-check.yml` to only check stateDiagram-v2
- **Impact**: Resolves CI false positives on legitimate line-break tag usage

### P21: Coverage enhancement — dual-missing files first (Added 2026-02-12)
- **Strategy**: Fix files missing BOTH Java AND SQL before fixing single-missing files
- **Rationale**: Each dual-fix gives +1 Java AND +1 SQL (2x efficiency per iteration)
- **Current gaps**: 7 dual-missing, 15 Java-only, 1 SQL-only
- **Priority order**: (1) Dual-missing → (2) Java-only → (3) SQL-only
- **Constraint**: All Java code MUST follow SmartAdmin patterns (P13 guardrail)

### P22: Ralph immortal loop — never exit on quota issues (Added 2026-02-13)
- **Problem**: `set -euo pipefail` + `threshold-checker.sh` exit 1 = script silently crashes
- **Root Cause**: `set -e` terminates entire script when any command returns non-zero; `threshold-checker.sh` returning exit 1 (REST_RECOMMENDED) triggers this before `THRESHOLD_STATUS=$?` executes
- **Fix**: Removed `set -e`, replaced all `exit 1/2/3` with sleep-and-retry, added `.rate-limit-until` file
- **Design Principles**:
  1. Script only exits on RALPH_COMPLETE or TODO=0
  2. Rate limit → calculate recovery time → write `.rate-limit-until` → sleep → auto-resume
  3. 24-hour cycle → 2h rest + reset timer (not exit)
  4. 5 consecutive rate limits → 4h long rest then reset counter (not exit)
  5. `.rate-limit-until` file survives script restarts (pre-iteration probe checks it)
- **Disabled**: usage-tracker.js (produces all-zero token data, inflates failure_rate, triggers false REST)
- **Simplified**: Quota monitoring uses outcome counters instead of broken Node.js pipeline

### 2026-02-12: Optimized Phase 9C from 6 batches to 2 commits
- Original plan: 6 batches (Batch 38-43) for 91% → 100% terminology consistency
- Actual execution: 2 commits in ~30 minutes
- Key insight: source-archive/ exclusion + smart compound term detection resolved most "issues"
- Only 2 actual terminology fixes needed: 充值→存款, 存取款→存提款

### P23: Java/SQL coverage calculation - exclude navigation files (Added 2026-02-13)
- **Problem**: Phase 13 progress.md showed Java 85.6% (101/118), but actual content coverage was 100%
- **Root Cause**: 118 total includes 14 README/INDEX/quality-report files (navigation, not content)
- **Correct Calculation**: 104 content files are the coverage denominator
- **Result**: Java = 104/104 (100%), SQL = 104/104 (100%)
- **Coverage Files to Exclude**: README.md, INDEX.md, quality-reports/*.md
- **Lesson**: Coverage metrics should only count actual content files, not navigation/index files

### P24: CI/CD workflow design - SmartAdmin convention alignment (Added 2026-02-13)
- **Problem**: Phase 14 identified conflict between mermaid-syntax-check.yml (blanket `<br/>` ban) and SmartAdmin convention (USE `<br/>` in all Mermaid types except stateDiagram-v2)
- **Root Cause**: Two workflows created at different times with different assumptions
- **Resolution**: SmartAdmin convention wins (defined in CLAUDE.md, project-wide standard)
- **Implementation**:
  - ✅ `mermaid-syntax-check.yml`: Replaced blanket check with `detect-statediagram-br.sh` (stateDiagram-v2 only)
  - ✅ `igaming-translation-quality.yml`: Added 6 quality gates (Technical Terms, Encoding, Terminology, Mermaid, stateDiagram, Coverage)
  - ✅ Artifact upload: Quality reports saved for 30 days
  - ✅ source-archive/ exclusion: P1 guardrail enforced in all checks
- **Quality Gate Structure**:
  - CRITICAL: Technical Terms (100%) - blocking
  - HIGH: Encoding, Mermaid Syntax, stateDiagram HTML tag check - blocking
  - MEDIUM: Terminology (≥95%), Coverage Thresholds (Java ≥90%, SQL ≥95%) - warning
- **Coverage Calculation**: Uses P23 approach (content files only, excludes README/INDEX/reports)
- **Lesson**: CI/CD workflows must align with project-wide conventions, not create conflicting standards
