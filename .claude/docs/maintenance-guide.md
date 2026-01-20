# Agent Configuration Maintenance Guide

**Purpose:** Guide for maintaining and updating the optimized agent configuration system.

## Configuration Architecture Overview

```
.claude/
├── shared/                        # Shared knowledge and templates
│   ├── knowledge/                 # SmartAdmin patterns (edit once, affects all)
│   │   ├── smartadmin-patterns.md
│   │   ├── project-architecture.md
│   │   └── quality-standards.md
│   ├── templates/                 # Agent inheritance system
│   │   ├── agent-base.md
│   │   ├── technical-agent-mixin.md
│   │   └── analysis-agent-mixin.md
│   └── orchestration/             # Agent coordination
│       ├── decision-matrix.md
│       ├── agent-dependencies.md
│       └── workflow-patterns.md
├── agents/                        # Individual agent definitions
│   ├── java-architect.md         # References shared knowledge
│   ├── business-analyst.md
│   ├── chaos-engineer.md
│   ├── devops-engineer.md
│   └── postgres-pro.md
├── settings.local.json            # Consolidated permissions (12 patterns)
└── docs/                          # Documentation
    ├── maintenance-guide.md      # This file
    ├── permission-guide.md
    └── changelog.md
```

## Common Maintenance Tasks

### Task 1: Update SmartAdmin Patterns

**Scenario:** SmartAdmin adds new pattern or changes convention (e.g., new utility class, changed naming convention)

**Impact:** All 5 agents automatically get the update

**Steps:**
1. **Edit ONE file:** `.claude/shared/knowledge/smartadmin-patterns.md`
2. Add or update the pattern with clear examples
3. Verify the change applies to all agents
4. Test with at least one agent to confirm
5. Update `.claude/docs/changelog.md`
6. Commit with clear message:
   ```bash
   git add .claude/shared/knowledge/smartadmin-patterns.md .claude/docs/changelog.md
   git commit -m "docs(agents): update SmartAdmin pattern - [description]"
   ```

**Time:** 5 minutes (vs 50 minutes editing 5 files)

**Example:**
```markdown
# In smartadmin-patterns.md, add:

## New Utility: DateUtil

**Purpose:** Standardized date formatting

**Usage:**
```java
String formatted = DateUtil.format(date, "yyyy-MM-dd HH:mm:ss");
LocalDateTime parsed = DateUtil.parse(dateString);
```
```

### Task 2: Update Project Architecture Information

**Scenario:** Technology stack upgraded (e.g., Spring Boot 3.5.4 → 3.6.0)

**Steps:**
1. **Edit:** `.claude/shared/knowledge/project-architecture.md`
2. Update the technology stack table
3. Update any affected build commands if changed
4. Update changelog
5. Commit:
   ```bash
   git commit -m "docs(agents): update Spring Boot version to 3.6.0"
   ```

**Time:** 3 minutes

### Task 3: Update Quality Standards

**Scenario:** New code quality rule or testing requirement

**Steps:**
1. **Edit:** `.claude/shared/knowledge/quality-standards.md`
2. Add the new standard to appropriate section
3. Update the quality checklist if needed
4. Update changelog
5. Commit:
   ```bash
   git commit -m "docs(agents): add new quality standard - [description]"
   ```

**Time:** 5 minutes

### Task 4: Modify Individual Agent

**Scenario:** Change agent-specific expertise (e.g., add new Java 21 feature to java-architect)

**Steps:**
1. **Determine if change is:**
   - ✅ Agent-specific → Edit `.claude/agents/[agent-name].md`
   - ❌ Applies to all → Edit shared knowledge instead

2. **Edit the specific agent file:**
   - Only modify the "Your Core Expertise" or specialized sections
   - Don't duplicate what's in shared knowledge

3. Test the agent
4. Update changelog
5. Commit:
   ```bash
   git commit -m "feat(java-architect): add Java 21 virtual threads expertise"
   ```

**Time:** 10-15 minutes

### Task 5: Add New Agent

**Scenario:** Need a new specialized agent (e.g., security-engineer)

**Steps:**
1. **Copy template:** Use `agent-base.md` as starting point
2. **Determine agent type:**
   - Technical agent → Reference `technical-agent-mixin.md`
   - Analysis agent → Reference `analysis-agent-mixin.md`
3. **Write agent-specific expertise:**
   - Focus on unique skills
   - Reference shared knowledge (don't duplicate)
4. **Update orchestration:**
   - Add to `decision-matrix.md` (keywords, decision flow)
   - Add to `agent-dependencies.md` (collaboration patterns)
   - Add to `workflow-patterns.md` if new workflows needed
5. **Assign color and model:**
   - Color: Choose unique color
   - Model: opus (default for all agents)
6. **Test the agent**
7. **Update changelog:**
   ```markdown
   ## [Version] - Date
   ### Added
   - security-engineer agent for security audits and compliance
   ```
8. Commit:
   ```bash
   git commit -m "feat(agents): add security-engineer agent"
   ```

**Time:** 30-45 minutes

**Checklist:**
- [ ] Inherits from agent-base.md
- [ ] References appropriate mixin
- [ ] References shared knowledge files
- [ ] Doesn't duplicate SmartAdmin patterns
- [ ] Added to decision-matrix.md
- [ ] Added to agent-dependencies.md
- [ ] Color assigned (unique)
- [ ] Model specified (opus)
- [ ] Description includes examples
- [ ] Tested successfully

### Task 6: Update Orchestration

**Scenario:** New workflow pattern discovered or agent collaboration needs clarification

**Steps:**
1. **Identify which file:**
   - New keywords → `decision-matrix.md`
   - New dependencies → `agent-dependencies.md`
   - New workflow → `workflow-patterns.md`

2. **Edit the appropriate file:**
   - Add clear examples
   - Include decision criteria
   - Document edge cases

3. Update changelog
4. Commit:
   ```bash
   git commit -m "docs(orchestration): add workflow pattern for [scenario]"
   ```

**Time:** 15-20 minutes

### Task 7: Add New Permission Pattern

**Scenario:** Agent needs new bash command permission

**Steps:**
1. **Determine if existing pattern covers it:**
   - `Bash(git *:*)` covers all git commands
   - `Bash(./gradlew *:*)` covers all gradle commands
   - Check if wildcard already exists

2. **If new pattern needed:**
   ```json
   {
     "permissions": {
       "allow": [
         // ... existing patterns ...
         "Bash(new-command *:*)"  // Add with wildcard if appropriate
       ]
     },
     "rationale": {
       // ... existing rationale ...
       "new_command": "Explanation of why this command is needed and safe"
     }
   }
   ```

3. Test the permission works
4. Update `.claude/docs/permission-guide.md` if new category
5. Update changelog
6. Commit:
   ```bash
   git commit -m "chore(config): add permission for [command] - [reason]"
   ```

**Time:** 5 minutes

**Guidelines:**
- Use wildcards when safe (`git *` vs `git add`, `git commit`, etc.)
- Document WHY in rationale section
- Avoid hardcoded absolute paths (use relative patterns)
- Check if already covered by existing wildcard

## Workflow Decision Tree

```
[Need to make a change]
    ↓
Applies to ALL agents? ──YES──→ Edit .claude/shared/knowledge/
    ↓ NO
Applies to technical agents only? ──YES──→ Edit technical-agent-mixin.md
    ↓ NO
Applies to analysis agents only? ──YES──→ Edit analysis-agent-mixin.md
    ↓ NO
Applies to single agent? ──YES──→ Edit .claude/agents/[agent-name].md
    ↓ NO
New agent coordination? ──YES──→ Edit .claude/shared/orchestration/
    ↓ NO
New permission? ──YES──→ Edit .claude/settings.local.json
```

## Testing Changes

### Test Single Agent

```bash
# Replace [agent-name] with: java-architect, business-analyst, etc.
# Test that agent loads and responds correctly
# Example: "Implement a simple REST endpoint" for java-architect
```

### Test Shared Knowledge Update

After updating shared knowledge:
1. Test with 2-3 different agents
2. Verify they all reference the updated content correctly
3. Ensure no broken references

### Test Permission Changes

After adding new permission:
1. Try the command that required the permission
2. Verify it executes successfully
3. Check logs for permission-related errors

### Test Orchestration Updates

After updating orchestration:
1. Review decision matrix logic with sample requests
2. Verify agent dependencies graph is accurate
3. Test workflow pattern with hypothetical scenario

## Troubleshooting

### Problem: Agent not referencing shared knowledge

**Symptoms:**
- Agent doesn't follow SmartAdmin patterns
- Agent seems to ignore updated shared knowledge

**Solution:**
1. Check agent frontmatter references shared files correctly
2. Verify file paths are correct (relative paths)
3. Check for typos in file names
4. Ensure shared knowledge file exists

### Problem: Permission denied error

**Symptoms:**
- Bash command fails with permission error
- Agent can't execute required command

**Solution:**
1. Check `.claude/settings.local.json` for pattern
2. Verify wildcard pattern covers the command
3. Add specific pattern if needed
4. Check for typos in permission patterns

### Problem: Unclear which agent to use

**Symptoms:**
- User confused about agent selection
- Multiple agents seem applicable

**Solution:**
1. Review `.claude/shared/orchestration/decision-matrix.md`
2. Add keywords to keyword mapping table
3. Add scenario to context-based decision logic
4. Update examples in decision matrix

### Problem: Agents duplicating work

**Symptoms:**
- Multiple agents doing similar tasks
- Unclear handoff points

**Solution:**
1. Review `.claude/shared/orchestration/agent-dependencies.md`
2. Clarify handoff protocols
3. Update workflow patterns
4. Define clear entry/exit criteria

## Version Control Best Practices

### Commit Message Format

Follow Conventional Commits:
```
<type>(<scope>): <subject>

Types:
- feat: New agent or major feature
- fix: Bug fix in agent configuration
- docs: Documentation updates
- refactor: Restructure without changing behavior
- chore: Maintenance tasks (permissions, cleanup)

Scopes:
- agents: Individual agent changes
- shared: Shared knowledge/templates
- orchestration: Coordination files
- config: settings.local.json changes
- docs: Documentation changes

Examples:
feat(agents): add security-engineer agent
docs(shared): update SmartAdmin pagination pattern
chore(config): consolidate git permissions to wildcard
fix(java-architect): correct MyBatis Plus example
refactor(orchestration): clarify agent handoff protocols
```

### Branching Strategy

**For major changes:**
1. Create feature branch: `git checkout -b feat/agent-optimization-v2`
2. Make changes incrementally
3. Test after each change
4. Commit with clear messages
5. Merge to main when stable

**For minor updates:**
1. Work directly on main
2. Commit immediately after testing
3. Keep commits atomic (one logical change per commit)

## Backup and Rollback

### Backup Current Configuration

Before major changes:
```bash
# Create backup branch
git checkout -b backup/pre-optimization-$(date +%Y%m%d)
git push origin backup/pre-optimization-$(date +%Y%m%d)

# Return to main
git checkout main
```

### Rollback Procedures

**Rollback last commit:**
```bash
git revert HEAD
git push
```

**Rollback to specific version:**
```bash
# Find commit hash
git log --oneline

# Rollback to that commit
git checkout <commit-hash> .claude/
git commit -m "revert(agents): rollback to previous configuration - [reason]"
git push
```

**Rollback specific file:**
```bash
git checkout HEAD~1 .claude/agents/java-architect.md
git commit -m "revert(java-architect): rollback to previous version - [reason]"
```

## Monitoring and Metrics

### Track Configuration Health

**Monthly review checklist:**
- [ ] All agent files load successfully
- [ ] No broken references to shared knowledge
- [ ] Orchestration docs reflect actual usage patterns
- [ ] Permission patterns cover all needed commands
- [ ] No duplicate content across agents
- [ ] Documentation up to date

**Key metrics to track:**
- Number of permission patterns (target: <15)
- Duplication % across agents (target: <10%)
- Time to update SmartAdmin pattern (target: <10 min)
- Agent selection accuracy (target: >90%)

### Gather User Feedback

**Questions to ask users:**
1. How clear is agent selection? (1-5 scale)
2. Did you get the right agent for your task?
3. Any confusion about which agent to use?
4. Suggestions for improvement?

**Act on feedback:**
- Update decision matrix with confusing scenarios
- Add examples to orchestration docs
- Clarify agent descriptions
- Improve keyword mapping

## Summary

**Key Principles:**
1. **Edit once, affect all** - Use shared knowledge for common patterns
2. **Test before committing** - Always verify changes work
3. **Document changes** - Update changelog and commit messages
4. **Keep it DRY** - Don't duplicate content
5. **Version control everything** - Commit frequently, rollback easily

**Most Common Tasks:**
- Update SmartAdmin patterns: Edit `smartadmin-patterns.md` (5 min)
- Add new agent: Copy template, customize, test (30-45 min)
- Add permission: Update `settings.local.json` (5 min)
- Update orchestration: Edit appropriate orchestration file (15 min)

**When in doubt:**
- Check if it affects all agents → shared knowledge
- Check if it's agent-specific → individual agent file
- Check if it's about coordination → orchestration files
- Ask clarifying questions rather than guessing

**Remember:** The goal is maintainability. Taking an extra 5 minutes to do it right saves 50 minutes of rework later!
