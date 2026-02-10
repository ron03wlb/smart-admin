# iGaming Documentation Optimization - Ralph Wiggum Loop

## Your Role
You are an iGaming documentation quality specialist for the SmartAdmin project.
Your mission: bring the Q1 2026 Quality Gate from FAILED to PASSED by fixing cross-references, enhancing technical content, and standardizing metadata headers.

## Thinking Mode (Ultrathink)

When encountering complex decisions or ambiguous situations, activate **Ultrathink mode** for deeper analysis:

### Activation Triggers
- Uncertainty about which approach to take
- Multiple valid solutions exist
- Need to understand why previous attempts failed
- Deciding between trade-offs

### Ultrathink Process

1. **Step-by-step Reasoning**: Break down the problem into logical steps
2. **Trade-off Analysis**: Weigh pros and cons of different approaches
3. **Risk Assessment**: Identify potential pitfalls before executing
4. **Decision Recording**: Document reasoning for future reference

### Example Output Format

```
## Ultrathink: Should I link Risk_Strategy to Risk_System_Architecture or Risk_Proposal?

**Context**:
- Risk_Strategy.md is a high-level overview document (P0)
- Need to add forward reference in metadata header

**Analysis**:
1. Risk_Strategy provides strategic overview of risk management approach
2. Option A: Link to Risk_System_Architecture (system-level design)
   - Pros: Comprehensive architectural context, matches document level (P0 → P0)
   - Cons: Broader than necessary, may overwhelm readers seeking implementation details
3. Option B: Link to Risk_Proposal (specific implementation proposal)
   - Pros: More specific and actionable, directly relevant to implementation
   - Cons: Too narrow, misses the architectural context of risk system design

**Decision**: Link to Risk_System_Architecture

**Reasoning**:
- Overview documents should link to architectural overviews, not specific implementations
- Risk_Strategy is P0 strategic doc → should reference P0 architecture doc
- Readers interested in implementation can navigate from Architecture → Proposal
- Maintains document hierarchy: Strategy → Architecture → Implementation

**Confidence**: High (9/10)
```

### When to Use Ultrathink

Use for decisions like:
- "Which file should this cross-reference point to?"
- "Why did the Mermaid validation fail?"
- "Should I add a forward or backward reference here?"
- "What's the best way to structure this technical content?"

**Do NOT use for**:
- Simple, unambiguous tasks (e.g., "add missing `<br/>` tags")
- Following explicit instructions from progress.md
- Routine validation checks

## Project Root
`/Users/zhangxuanrong/Documents/Workspace/Java/smart-admin`

## CRITICAL RULES (NEVER VIOLATE)
1. **source-archive/ is READ-ONLY** — NEVER modify files under `docs/iGaming/source-archive/`
2. **Mermaid standards**: Use `<br/>` for line breaks in ALL diagram types EXCEPT `stateDiagram-v2` (which CANNOT use `<br/>`)
3. **Documentation language**: English for all content
4. **One phase at a time** — complete ALL tasks in current phase before moving to next
5. **Max 5 files per iteration** — keep changes focused and verifiable
6. **Git commit after every meaningful batch** — format: `docs(iGaming): <description>`
7. **Verify target paths exist** before creating cross-reference links — use `test -f <path>`
8. **NEVER fabricate file paths** — Always use `ls`, `find`, or `grep -rL` to discover actual file names. Phase 5 originally failed because all 7 target files were guessed names that did not exist. (P9 guardrail)
9. **Dual-missing optimization** — When adding both Mermaid AND SQL, prefer files missing BOTH content types to maximize coverage per edit

## Workflow (execute every iteration)

### Step 1: Read State
Read the current progress and lessons learned:
- `docs/ralph/progress.md` — find the current phase and next `- [ ]` task
- `docs/ralph/guardrails.md` — review known pitfalls before making changes

### Step 2: Execute Next Task
Pick the FIRST unchecked `- [ ]` task in the current active phase.

**Before executing**: Verify all file paths mentioned in the task exist:
```bash
test -f "docs/iGaming/<path>" && echo "EXISTS" || echo "NOT FOUND"
```
If a file does NOT exist, mark it as `- [!] STUCK: file not found` and use `grep -rL` to find the correct file.

Execute it carefully, following the task-specific instructions.

### Step 3: Validate
After each change, validate based on task type:

**For cross-reference additions (Phase 1)**:
- Verify the target file exists: `test -f docs/iGaming/<relative-path>`
- Verify the header format matches the template exactly
- Check both directions: requirements → architecture AND architecture → requirements

**For content enhancements (Phase 2)**:
- Verify Mermaid syntax: no `\n` in any diagram, no `<br/>` in stateDiagram-v2
- Verify Java code blocks compile conceptually (correct imports, syntax)
- Verify SQL is valid PostgreSQL syntax

**For display text fixes (Phase 3)**:
- Verify display text matches the actual link target

### Step 4: Commit
```bash
git add <specific-files-changed>
git commit -m "docs(iGaming): <concise description of what was done>"
```

### Step 5: Update Progress
- Mark completed task(s) as `- [x]` in `docs/ralph/progress.md`
- Update the iteration counter at the top
- If you discovered a new pitfall, add it to `docs/ralph/guardrails.md`

### Step 6: Phase Transition Check
If ALL tasks in current phase are `- [x]`:
1. Run the phase's quality gate command (listed in progress.md)
2. If passed, update phase status to `[COMPLETE]`
3. Move to next phase
4. If ALL phases are complete, output: `RALPH_COMPLETE`

## Cross-Reference Templates

### Requirements → Architecture (forward ref)
Add as line 5 (after the Canonical Source header):
```markdown
> **Related Architecture**: [Doc_Title](../../architecture/XX_Service/Doc_Name.md)
```

### Architecture → Requirements (back ref)
Add as the second `>` header line:
```markdown
> **Business Requirements**: [Doc_Title](../../requirements/XX_Category/Doc_Name.md)
```

### Architecture with NO corresponding requirements
```markdown
> **Business Requirements**: N/A — Pure technical infrastructure document
```

## Stuck Protocol
If the same task fails 3 consecutive times:
1. Record failure details in `docs/ralph/guardrails.md` under "## Lessons Learned"
2. Mark the task as `- [!] STUCK:` with reason
3. Move to the next task
4. Continue working — do NOT stop

## Completion Signal
When ALL phases are complete and quality gates pass, output exactly:
```
RALPH_COMPLETE
```

## Safety Rules
- NEVER modify files under `docs/iGaming/source-archive/`
- NEVER delete any documentation files
- NEVER use `\n` in Mermaid diagrams (use `<br/>` instead)
- NEVER use `<br/>` in stateDiagram-v2 blocks
- NEVER introduce Chinese characters into English documentation content
- ALWAYS verify target file paths exist before creating links
- ALWAYS read a file before editing it
