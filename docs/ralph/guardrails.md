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
