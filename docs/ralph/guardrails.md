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

---

## Lessons Learned
(Auto-populated during execution — add entries here when you discover new pitfalls)
