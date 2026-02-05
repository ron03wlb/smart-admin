# Troubleshooting Guide

**Purpose**: Self-service problem resolution for common Claude Code agent issues

**Version**: 1.0.0
**Last Updated**: 2026-01-21

---

## How to Use This Guide

1. **Identify your issue category** from the table of contents
2. **Find the matching symptom** within that category
3. **Follow the diagnosis steps** to confirm root cause
4. **Apply the solution** with step-by-step instructions
5. **Prevent future occurrences** using prevention guidance

---

## Table of Contents

1. [Agent Selection Issues](#1-agent-selection-issues)
2. [Multi-Agent Collaboration Issues](#2-multi-agent-collaboration-issues)
3. [Permission and Configuration Issues](#3-permission-and-configuration-issues)
4. [Hook System Issues](#4-hook-system-issues)
5. [Performance Issues](#5-performance-issues)
6. [Knowledge Base Issues](#6-knowledge-base-issues)

---

## 1. Agent Selection Issues

### Issue 1.1: "I don't know which agent to use"

**Symptoms**:
- Unclear which agent is appropriate for your task
- Multiple agents seem relevant
- Task spans multiple domains (backend + frontend + database)

**Diagnosis**:
Check these resources in order:
```bash
# 1. Check agent capability matrix
cat .claude/docs/agent-capability-matrix.md | grep -A 5 "Quick Reference"

# 2. Check decision matrix keywords
cat .claude/shared/orchestration/decision-matrix.md | grep -A 10 "Keyword Mapping"

# 3. Check workflow patterns
cat .claude/shared/orchestration/workflow-patterns.md | head -50
```

**Solutions**:

**Solution A: Use Decision Tree** (for single agent selection)
1. Open `.claude/docs/agent-capability-matrix.md`
2. Navigate to "Agent Selection Decision Tree"
3. Answer Q1: What is your primary goal?
4. Follow the decision path to recommended agent

**Solution B: Use Keyword Matching**
1. Identify keywords in your task (e.g., "backend", "API", "Vue", "deploy")
2. Open `.claude/shared/orchestration/decision-matrix.md`
3. Find keyword in mapping table
4. Use recommended agent

**Solution C: Start with business-analyst** (when truly unclear)
- business-analyst clarifies requirements and recommends appropriate agent(s)
- Useful for complex, multi-faceted tasks
- Example: "Improve system performance" → BA analyzes → Routes to appropriate specialists

**Prevention**:
- Bookmark the agent capability matrix for quick reference
- Familiarize yourself with the 9 agent specializations
- When in doubt, start with business-analyst

---

### Issue 1.2: "Agent says task is outside their expertise"

**Symptoms**:
- Agent declines task or suggests different agent
- Agent provides suboptimal solution
- Agent explicitly states "this is outside my expertise"

**Diagnosis**:
Agent was incorrectly selected for the task.

**Solutions**:

**Solution**: Follow Agent's Recommendation
1. Agent will suggest the appropriate specialist
2. Switch to recommended agent
3. Provide context from previous conversation if needed

**Example**:
```
User: (to vue-expert) "Optimize database queries"
vue-expert: "Database optimization is outside my expertise.
            Please use postgres-pro agent for query optimization."

User: (switches to postgres-pro) "Optimize database queries for employee module"
```

**Prevention**:
- Verify agent selection using decision matrix before starting
- Use capability matrix to understand agent expertise
- Don't force-fit tasks to wrong agents

---

### Issue 1.3: "Task requires multiple agents, unclear which to start with"

**Symptoms**:
- Task spans backend + frontend + database + deployment
- Unclear execution order
- Risk of agents working in wrong sequence

**Diagnosis**:
Multi-agent workflow needed. Check workflow patterns.

**Solutions**:

**Solution A: Use Workflow Patterns**
1. Open `.claude/shared/orchestration/workflow-patterns.md`
2. Find matching workflow pattern:
   - **New Full-Stack Feature** → Sequential (BA → Java → Vue → Code → DevOps → Chaos)
   - **Performance Optimization** → Parallel (Java + Postgres + Vue → Architect)
   - **Production Incident** → Hub-and-Spoke (DevOps coordinates specialists)
   - **Database Migration** → Sequential with Checkpoints (Postgres → Java → DevOps)
3. Follow pattern's sequence

**Solution B: Start with business-analyst** (when no pattern matches)
- BA analyzes requirements
- BA recommends agent sequence
- BA creates handoff plan

**Example**:
```
Task: "Add employee performance review feature with approval workflow"

Pattern Match: New Full-Stack Feature (Sequential)

Sequence:
1. business-analyst → Requirements, API contract, approval workflow design
2. java-architect → Backend implementation (REST API, service logic, workflows)
3. vue-expert → Frontend implementation (forms, approval UI)
4. quality-reviewer → Pre-merge quality gate
5. devops-engineer → Deployment
6. chaos-engineer → Resilience validation
```

**Prevention**:
- Review workflow patterns at project start
- Create project-specific workflow for common tasks
- Document custom workflows in project docs

---

## 2. Multi-Agent Collaboration Issues

### Issue 2.1: "Frontend can't connect to backend API"

**Symptoms**:
- Frontend gets network errors (ERR_CONNECTION_REFUSED)
- CORS errors in browser console
- 404 Not Found for API endpoints
- Timeout errors

**Diagnosis**:

Run these checks:
```bash
# Check if backend is running
curl http://localhost:1024/actuator/health
# Expected: {"status":"UP"}

# Check backend logs for startup errors
tail -f smart-admin-api-java21-springboot3/smartadmin-app/logs/smart-admin.log

# Check CORS configuration
grep -r "CorsConfiguration" smart-admin-api-java21-springboot3/
```

**Root Causes & Solutions**:

**Cause 1: Backend Not Running**
```bash
# Solution: Start backend
cd smart-admin-api-java21-springboot3/
./gradlew :smartadmin-app:bootRun

# Verify backend started
curl http://localhost:1024/actuator/health
```

**Cause 2: Wrong URL in Frontend**
```javascript
// Frontend API configuration
// Check: smart-admin-web/src/api/config.js or similar

// WRONG:
const API_URL = 'http://localhost:8080/api'  // Wrong port

// CORRECT (SmartAdmin default):
const API_URL = 'http://localhost:1024'
```

**Cause 3: CORS Not Configured**
```java
// Backend CORS configuration
// File: smartadmin-app/src/main/java/net/lab1024/sa/admin/config/WebMvcConfig.java

@Override
public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/**")
        .allowedOrigins("http://localhost:5173") // Vite dev server
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
        .allowCredentials(true);
}
```

**Prevention**:
- Document API base URL in handoff package
- Verify backend running before starting frontend work
- Test CORS with simple API call first

---

### Issue 2.2: "Permission mismatch between backend and frontend"

**Symptoms**:
- User has role but gets 403 Forbidden
- `v-privilege` directive doesn't work
- Permission check passes in backend but fails in frontend (or vice versa)

**Diagnosis**:

```bash
# Extract backend permission strings
grep -r "@SaCheckPermission" smart-admin-api-java21-springboot3/smartadmin-app/ | grep -o '"[^"]*"'

# Search frontend permission strings
grep -r "v-privilege" smart-admin-web/src/ | grep -o '"[^"]*"'

# Compare: Are they identical?
```

**Root Cause**:
Permission strings don't match exactly.

**Common Mismatches**:
| Backend | Frontend (WRONG) | Frontend (CORRECT) |
|---------|------------------|-------------------|
| `system:employee:add` | `employee:add` | `system:employee:add` |
| `system:employee:edit` | `system:employee:update` | `system:employee:edit` |
| `system:employee:delete` | `employee:delete` | `system:employee:delete` |

**Solutions**:

**Solution**: Get Exact Strings from Backend
```bash
# Step 1: java-architect extracts exact permission strings
cd smart-admin-api-java21-springboot3/
grep -r "@SaCheckPermission" smartadmin-app/src/main/java/net/lab1024/sa/admin/module/employee/ \
  | grep -o '"[^"]*"' \
  | sort -u

# Output example:
# "system:employee:add"
# "system:employee:delete"
# "system:employee:edit"
# "system:employee:query"

# Step 2: vue-expert uses EXACT strings (copy-paste)
# File: smart-admin-web/src/views/employee/index.vue

<el-button v-privilege="'system:employee:add'">Add</el-button>
<el-button v-privilege="'system:employee:edit'">Edit</el-button>
<el-button v-privilege="'system:employee:delete'">Delete</el-button>

# Step 3: Test end-to-end
# - Login with role that has these permissions
# - Verify buttons visible and functional
# - Verify API calls succeed
```

**Prevention**:
- java-architect includes exact permission strings in handoff package
- vue-expert never modifies permission strings (copy verbatim)
- Create constants file in frontend for reusability:
  ```typescript
  // smart-admin-web/src/constants/permissions.ts
  export const EMPLOYEE_PERMISSIONS = {
    ADD: 'system:employee:add',
    EDIT: 'system:employee:edit',
    DELETE: 'system:employee:delete',
    QUERY: 'system:employee:query'
  }
  ```

---

### Issue 2.3: "Agent waiting for another agent, work blocked"

**Symptoms**:
- Agent reports waiting for input from another agent
- Work stalled with no progress
- Unclear when blocking agent will complete work

**Diagnosis**:
Handoff dependency blocking progress.

**Solutions**:

**Solution A: Partial Handoff** (Best for API contracts)
```markdown
### Example: Backend not fully implemented but frontend can start

java-architect provides partial handoff:
- ✅ API contract defined (request/response DTOs)
- ✅ Swagger documentation with example JSON
- ✅ Backend stub running (returns mock data)
- ⏳ Full business logic pending

vue-expert can start:
- Implement UI components
- Integrate with mock API
- Build frontend logic
- When backend ready, swap mock for real API
```

**Solution B: Async Communication** (Leave message and move on)
```markdown
Blocked agent: "Waiting for X from agent Y.
               Moving to independent task Z while waiting."

Example:
vue-expert: "Waiting for employee API from java-architect.
             Working on department module frontend in parallel."
```

**Solution C: Escalate** (If blocked >4 hours)
```markdown
### Escalation Template

🚨 BLOCKER: Waiting for [agent] to complete [task]

Impact: [Your agent] blocked on [your task]
Duration: Blocked for [N hours]
Criticality: [Critical/High/Medium/Low]

Request:
- Option A: Partial handoff (if possible)
- Option B: ETA for completion
- Option C: Alternative approach

Tag: @business-analyst (for prioritization decision)
```

**Prevention**:
- Agree on handoff timeline upfront
- Communicate delays proactively
- Consider parallel work on independent tasks
- Use partial handoffs when possible

---

### Issue 2.4: "Multiple agents gave conflicting advice"

**Symptoms**:
- Agent A recommends approach X
- Agent B recommends approach Y (contradicts A)
- Unclear which recommendation to follow

**Diagnosis**:
Agents have different perspectives or incomplete context.

**Solutions**:

**Solution A: Understand Perspectives**
```markdown
Each agent optimizes for their specialty:
- java-architect: Code maintainability, performance
- devops-engineer: Operability, deployment simplicity
- postgres-pro: Database efficiency, query performance
- quality-reviewer: Code quality, long-term architecture, scalability

All perspectives are valid, need to balance trade-offs.
```

**Solution B: Hierarchy of Authority**
```markdown
For conflicting architectural advice:
1. quality-reviewer --mode=architecture (final authority on architecture)
2. Specialist agent (authority in their domain)
3. business-analyst (for business requirements)

Example:
- postgres-pro recommends denormalization for performance
- java-architect recommends normalization for maintainability
- quality-reviewer (--mode=architecture) makes final call based on system requirements
```

**Solution C: Escalate for Decision**
```markdown
### Escalation Template

🚨 CONFLICTING RECOMMENDATIONS

Agent A (@java-architect) recommends: [Approach X]
Rationale: [Reason]

Agent B (@postgres-pro) recommends: [Approach Y]
Rationale: [Reason]

Context: [What we're trying to accomplish]

Request: @quality-reviewer (--mode=architecture) to make final decision

Timeline: Decision needed by [date] to avoid blocking [task]
```

**Prevention**:
- Provide complete context to all agents
- Consult quality-reviewer (--mode=architecture) for major architectural decisions upfront
- Document decisions and rationale
- Create decision log for future reference

---

### Issue 2.5: "Handoff package incomplete, missing information"

**Symptoms**:
- Receiving agent asks many follow-up questions
- Missing documentation, test data, or configuration
- Unable to start work due to missing inputs

**Diagnosis**:
Handoff package doesn't meet quality checklist.

**Solutions**:

**Solution**: Use Handoff Checklists
```markdown
See .claude/shared/orchestration/agent-dependencies.md
Section: "Handoff Quality Checklists"

Before handing off, verify EVERY checkbox:

### java-architect → vue-expert Checklist:
- [ ] Backend tests passing
- [ ] Swagger docs accessible
- [ ] Sample JSON for each endpoint
- [ ] Permission strings documented
- [ ] Error codes documented
- [ ] Test data in database
- [ ] Backend running and accessible

If ANY checkbox unchecked, handoff is incomplete.
```

**How to Request Missing Information**:
```markdown
### Incomplete Handoff Template

Hi @[agent], received your handoff package for [task].

Missing information:
- ❌ [Missing item 1]
- ❌ [Missing item 2]

I need these to proceed. Could you provide:
1. [Specific request 1]
2. [Specific request 2]

Timeline: I'm blocked until I receive these. ETA?
```

**Prevention**:
- Always use handoff checklists before handing off
- Include links to all documentation
- Provide examples (don't just describe)
- Test that receiving agent can access all resources

---

## 3. Permission and Configuration Issues

### Issue 3.1: "Permission denied when running bash command"

**Symptoms**:
```
Error: Permission denied - bash command "gradle build" requires approval
```

**Diagnosis**:

Check if permission exists:
```bash
# View current permissions
cat .claude/settings.local.json | jq '.allowedPrompts'

# Look for pattern matching your command
# Example: "gradle build" should match "Bash(./gradlew *:*)"
```

**Root Cause**:
Command pattern not in `allowedPrompts` configuration.

**Solutions**:

**Solution**: Add Permission Pattern

Common missing permissions:

**1. Gradle Commands**
```json
// Add to .claude/settings.local.json

{
  "allowedPrompts": [
    {
      "tool": "Bash",
      "prompt": "./gradlew *:*"
    }
  ]
}
```

**2. NPM Commands**
```json
{
  "allowedPrompts": [
    {
      "tool": "Bash",
      "prompt": "npm *:*"
    }
  ]
}
```

**3. Docker Commands**
```json
{
  "allowedPrompts": [
    {
      "tool": "Bash",
      "prompt": "docker *:*"
    },
    {
      "tool": "Bash",
      "prompt": "docker compose *:*"
    }
  ]
}
```

**4. Git Commands**
```json
{
  "allowedPrompts": [
    {
      "tool": "Bash",
      "prompt": "git *:*"
    }
  ]
}
```

**5. Project Scripts**
```json
{
  "allowedPrompts": [
    {
      "tool": "Bash",
      "prompt": "*/scripts/*.sh:*"
    }
  ]
}
```

**After adding permission**:
```bash
# Restart Claude Code session
# OR reload configuration (if supported)
```

**Prevention**:
- Pre-configure common permissions at project start
- Use wildcard patterns for flexibility
- Document custom permissions in project README
- See [Permission Guide](permission-guide.md) for patterns

---

### Issue 3.2: "Agent not referencing shared knowledge"

**Symptoms**:
- Agent suggests patterns inconsistent with SmartAdmin
- Agent doesn't follow layered architecture (Controller → Service → Manager → Dao)
- Agent recommends anti-patterns (e.g., `@Autowired` field injection)

**Diagnosis**:

Check agent configuration:
```bash
# Check if agent file references shared knowledge
head -20 .claude/agents/java-architect.md

# Should see frontmatter like:
# ---
# foundation_knowledge:
#   - ../shared/knowledge/smartadmin-patterns.md
#   - ../shared/knowledge/project-architecture.md
# ---
```

**Root Causes**:

**Cause 1: Agent File Missing Foundation Knowledge Reference**

**Solution**: Verify agent configuration
```bash
# All agents should reference shared knowledge
# Example from java-architect.md:

---
role: Backend Development Expert
foundation_knowledge:
  - ../shared/knowledge/smartadmin-patterns.md
  - ../shared/knowledge/project-architecture.md
  - ../shared/knowledge/quality-standards.md
---
```

**Cause 2: Shared Knowledge Files Deleted or Moved**

**Solution**: Restore shared knowledge
```bash
# Check if files exist
ls -la .claude/shared/knowledge/

# Should show:
# smartadmin-patterns.md
# smartadmin-frontend-patterns.md
# project-architecture.md
# quality-standards.md

# If missing, restore from git
git checkout HEAD -- .claude/shared/knowledge/
```

**Cause 3: Agent Configuration Cached**

**Solution**: Restart session
```
Exit current Claude Code session
Start new session
Agent will reload configuration with shared knowledge
```

**Prevention**:
- Never delete shared knowledge files
- Verify agent configuration after updates
- Test agent behavior with SmartAdmin-specific requests
- Review agent outputs for pattern compliance

---

### Issue 3.3: "Configuration changes not taking effect"

**Symptoms**:
- Updated `.claude/settings.local.json` but changes not applied
- Modified agent file but agent behavior unchanged
- Added new shared knowledge but agents don't use it

**Diagnosis**:
Configuration cached in current session.

**Solutions**:

**Solution A: Restart Claude Code Session** (Most reliable)
```bash
# Exit current session
exit

# Start new session
claude-code

# Configuration reloaded fresh
```

**Solution B: Verify File Syntax** (If restart doesn't help)
```bash
# Check JSON syntax
cat .claude/settings.local.json | jq .

# If error, fix JSON syntax:
# - Missing commas
# - Trailing commas (not allowed in JSON)
# - Mismatched brackets
```

**Solution C: Check File Permissions**
```bash
# Verify files are readable
ls -la .claude/settings.local.json
ls -la .claude/agents/*.md
ls -la .claude/shared/knowledge/*.md

# Should show readable permissions (-rw-r--r-- or similar)
```

**Prevention**:
- Restart session after configuration changes
- Validate JSON syntax before saving
- Use editor with JSON validation (VS Code, etc.)
- Test configuration changes with simple requests first

---

## 4. Hook System Issues

### Issue 4.1: "Hooks take too long (>15 minutes)"

**Symptoms**:
- Hook execution exceeds 10-15 minutes
- Slow CI/CD pipeline
- Delayed feedback on commits/PRs

**Diagnosis**:

Check hook execution time:
```bash
# View hook configuration
cat .claude/settings.local.json | jq '.hooks'

# Check which steps are slow:
# - architecture-review (can be slow on large changes)
# - Full test suite (can be slow with many tests)
# - Code coverage (can be slow with large codebase)
```

**Solutions**:

**Solution A: Disable Non-Critical Hooks for Local Development**
```json
// .claude/settings.local.json

{
  "hooks": {
    "pre-commit": {
      "enabled": true,
      "steps": [
        // DISABLE architecture-review for local commits
        // {
        //   "name": "architecture-review",
        //   "agent": "quality-reviewer",
        //   "autoFix": true
        // },

        // Keep essential hooks only
        {
          "name": "format-check",
          "command": "./gradlew spotlessCheck"
        }
      ]
    }
  }
}
```

**Solution B: Reduce Auto-Fix Retry Attempts**
```json
{
  "hooks": {
    "autoFix": {
      "enabled": true,
      "maxAttempts": 1,  // Reduced from 3 to 1
      "timeout": 300000  // 5 minutes
    }
  }
}
```

**Solution C: Fix Only Major+ Issues**
```json
{
  "hooks": {
    "autoFix": {
      "enabled": true,
      "minSeverity": "major"  // Skip minor issues
    }
  }
}
```

**Solution D: Run Slow Hooks in CI Only**
```json
// Local: .claude/settings.local.json (fast hooks)
{
  "hooks": {
    "pre-commit": {
      "steps": [
        { "name": "format-check", "command": "./gradlew spotlessCheck" },
        { "name": "quick-tests", "command": "./gradlew test -x integrationTest" }
      ]
    }
  }
}

// CI: .github/workflows/ci.yml (all hooks including slow ones)
- name: Architecture Review
  run: # Full architecture validation
```

**Prevention**:
- Profile hook execution times
- Optimize slow steps (parallel execution, caching)
- Reserve comprehensive checks for CI/CD
- Keep local hooks fast (<5 minutes)

---

### Issue 4.2: "Auto-fix fails repeatedly (3 attempts)"

**Symptoms**:
```
Auto-fix attempt 1 failed
Auto-fix attempt 2 failed
Auto-fix attempt 3 failed
Hook failed after max retry attempts
```

**Diagnosis**:

```bash
# Check what issue auto-fix is trying to resolve
# Look at hook output (last N lines)
tail -50 .claude/hooks/hook-output.log

# Common failure patterns:
# - Complex refactoring beyond auto-fix capability
# - Conflicting requirements (can't satisfy both)
# - Test failures (not just formatting)
```

**Root Causes & Solutions**:

**Cause 1: Issue Too Complex for Auto-Fix**

Example: "Extract complex method into multiple methods"
- Auto-fix can format code but can't refactor complex logic

**Solution**: Manual fix
```bash
# Step 1: Disable auto-fix temporarily
# Edit .claude/settings.local.json:
{
  "hooks": {
    "autoFix": {
      "enabled": false
    }
  }
}

# Step 2: Fix manually with quality-reviewer guidance
# quality-reviewer will provide specific steps

# Step 3: Re-enable auto-fix after fixing
```

**Cause 2: Conflicting Requirements**

Example: Auto-fix tries to satisfy:
- Requirement A: Method must be <20 lines
- Requirement B: Method must handle 5 complex cases inline (requires >30 lines)

**Solution**: Document pattern for future
```markdown
### Known Pattern Exception

**Pattern**: Complex validation method exceeds line limit
**Rationale**: Business requirements necessitate comprehensive validation
**Approved By**: quality-reviewer
**Date**: 2026-01-21

Auto-fix will fail on this pattern. Suppress warnings:
@SuppressWarnings("method-length")
```

**Prevention**:
- Set realistic auto-fix expectations
- Document patterns that can't be auto-fixed
- Manual review for complex refactorings
- Improve auto-fix rules over time based on failures

---

### Issue 4.3: "Hook system not executing"

**Symptoms**:
- Commit succeeds without hook execution
- No hook output in logs
- Configuration seems correct but hooks don't run

**Diagnosis**:

```bash
# Step 1: Verify hook system enabled
cat .claude/settings.local.json | jq '.hooks.enabled'
# Expected: true

# Step 2: Verify hook configuration exists
cat .claude/settings.local.json | jq '.hooks.["pre-commit"]'
# Should show hook steps

# Step 3: Check hook script exists and is executable
ls -la .git/hooks/pre-commit
# Should show: -rwxr-xr-x (executable)
```

**Solutions**:

**Solution A: Enable Hook System**
```json
// .claude/settings.local.json

{
  "hooks": {
    "enabled": true,  // Must be true
    "pre-commit": {
      "enabled": true,  // Must be true
      "steps": [ /* ... */ ]
    }
  }
}
```

**Solution B: Install Git Hooks**
```bash
# Navigate to project root
cd /path/to/smart-admin

# Install hooks
# (If .claude provides install script)
bash .claude/scripts/install-hooks.sh

# OR manually:
chmod +x .git/hooks/pre-commit
```

**Solution C: Verify Git Hooks Not Bypassed**
```bash
# Check if commits bypass hooks
git log --oneline -5 | xargs -I {} git show {} --format="%h %s" | grep "\-\-no-verify"

# If you see --no-verify:
# Someone is bypassing hooks. Don't do this unless absolutely necessary.
```

**Prevention**:
- Never commit with `--no-verify` unless emergency
- Document why hooks were bypassed (in commit message)
- Install hooks as part of onboarding
- Verify hooks working after git operations (rebase, etc.)

---

## 5. Performance Issues

### Issue 5.1: "Agent responses are slow"

**Symptoms**:
- Agent takes >30 seconds to respond
- Timeouts on tool calls
- Laggy interaction

**Diagnosis**:

Check these factors:
```bash
# 1. Check codebase size
du -sh smart-admin-api-java21-springboot3/
# If >500MB, may slow down file operations

# 2. Check number of files
find smart-admin-api-java21-springboot3/ -type f | wc -l
# If >5000 files, may slow down searches

# 3. Check network latency (if Claude Code uses network)
ping api.anthropic.com
# If >200ms, may cause delays
```

**Solutions**:

**Solution A: Be Specific in Requests**
```markdown
# SLOW (vague, requires extensive search):
"Fix the bug in the codebase"

# FAST (specific, targeted):
"Fix the bug in EmployeeService.java:45 where @Autowired field injection
is used instead of constructor injection"
```

**Solution B: Provide File Paths**
```markdown
# SLOW:
"Update the employee API"

# FAST:
"Update the employee API in
smart-admin-api-java21-springboot3/smartadmin-app/src/main/java/net/lab1024/sa/admin/module/employee/controller/EmployeeController.java"
```

**Solution C: Use Appropriate Agent for Task**
```markdown
# SLOW:
Ask java-architect to "find all files related to employee management"
(Searches through entire codebase)

# FAST:
Ask Explore agent (specialized for codebase exploration)
"Find all employee-related files"
```

**Solution D: Limit Scope**
```markdown
# SLOW:
"Review all code in the project"

# FAST:
"Review code in the employee module:
smart-admin-api-java21-springboot3/smartadmin-app/src/main/java/net/lab1024/sa/admin/module/employee/"
```

**Prevention**:
- Provide specific file paths when known
- Use appropriate agents (Explore for searches)
- Break large tasks into smaller, focused tasks
- Maintain stable network connection

---

### Issue 5.2: "Hooks timeout after 10 minutes"

**Symptoms**:
```
Hook execution timeout after 600000ms
Hook failed: timeout exceeded
```

**Diagnosis**:

Check hook step duration:
```bash
# Review hook configuration
cat .claude/settings.local.json | jq '.hooks.["pre-commit"].steps'

# Identify slow steps:
# - Full test suite: Can take 10+ minutes
# - Architecture review: Can take 5-10 minutes on large changes
# - Code coverage: Can take 5-10 minutes
```

**Solutions**:

**Solution A: Increase Timeout** (if hooks legitimately need more time)
```json
// .claude/settings.local.json

{
  "hooks": {
    "pre-commit": {
      "enabled": true,
      "timeout": 1200000,  // Increased to 20 minutes
      "steps": [ /* ... */ ]
    }
  }
}
```

**Solution B: Optimize Hook Steps** (preferred)
```bash
# Run only fast tests in hooks
./gradlew test -x integrationTest  # Skip slow integration tests

# Run only changed files analysis
# Instead of analyzing entire codebase
```

**Solution C: Split Large PRs** (prevent large changes)
```markdown
# Instead of 1 PR with 50 files changed:
# Split into 3 PRs with 15-20 files each

# Benefits:
# - Faster hook execution (smaller changeset)
# - Easier code review
# - Faster feedback
```

**Prevention**:
- Keep PRs small (<300 lines changed)
- Optimize test suite (parallel execution, selective tests)
- Use fast linters (prefer static analysis over runtime)
- Run comprehensive checks in CI, not pre-commit

---

### Issue 5.3: "Gradle build is slow"

**Symptoms**:
- `./gradlew build` takes >10 minutes
- Slows down development iteration
- Hooks timeout due to slow build

**Diagnosis**:

```bash
# Profile gradle build
./gradlew build --profile

# Check report: build/reports/profile/profile-*.html
# Identifies slow tasks
```

**Solutions**:

**Solution A: Use Gradle Daemon** (should be default)
```bash
# Verify daemon running
./gradlew --status

# If not running, daemon will start automatically on next build
# Daemon caches JVM and build data for faster subsequent builds
```

**Solution B: Enable Parallel Builds**
```groovy
// gradle.properties

org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configureondemand=true
```

**Solution C: Skip Tests During Development**
```bash
# Build without tests (development only)
./gradlew build -x test

# Run tests separately when needed
./gradlew test
```

**Solution D: Use Build Scans**
```bash
# Enable build scan
./gradlew build --scan

# Analyzes build performance
# Identifies bottlenecks
```

**Prevention**:
- Configure gradle.properties for optimal performance
- Use gradle daemon
- Keep dependencies up-to-date
- Use incremental compilation

---

## 6. Knowledge Base Issues

### Issue 6.1: "Pattern updated but agents not following"

**Symptoms**:
- Updated shared knowledge file (e.g., `smartadmin-patterns.md`)
- Agent still suggests old pattern
- Agent behavior inconsistent with updated pattern

**Diagnosis**:

Agent using cached configuration from old session.

**Solutions**:

**Solution A: Restart Claude Code Session** (most reliable)
```bash
# Exit current session
exit

# Start new session
claude-code

# Agents reload shared knowledge from files
```

**Solution B: Verify File Updated Correctly**
```bash
# Check file content
cat .claude/shared/knowledge/smartadmin-patterns.md | grep -A 10 "pattern-name"

# Verify changes saved
git diff .claude/shared/knowledge/smartadmin-patterns.md
```

**Solution C: Explicitly Reference Updated Pattern**
```markdown
# In your request to agent:
"Please use the updated pattern from smartadmin-patterns.md section
'ResponseDTO Pattern' (updated 2026-01-21) which now recommends..."
```

**Prevention**:
- Restart session after updating shared knowledge
- Document pattern updates in changelog
- Communicate pattern changes to team
- Verify agent behavior after updates

---

### Issue 6.2: "Agent suggests pattern not in shared knowledge"

**Symptoms**:
- Agent recommends approach not documented in SmartAdmin patterns
- Pattern conflicts with project standards
- Agent references external best practices instead of project patterns

**Diagnosis**:

Agent improvising or using general knowledge instead of project-specific patterns.

**Solutions**:

**Solution A: Verify Against Shared Knowledge**
```bash
# Check if pattern exists in shared knowledge
grep -r "pattern-name" .claude/shared/knowledge/

# If not found:
# Pattern is NOT part of SmartAdmin standards
```

**Solution B: Ask Agent for Justification**
```markdown
"This pattern doesn't appear in our SmartAdmin shared knowledge
(.claude/shared/knowledge/smartadmin-patterns.md).

Can you either:
A) Point to where this pattern is documented in our shared knowledge
B) Explain why you're recommending a pattern outside our standards
C) Suggest an alternative that follows SmartAdmin patterns"
```

**Solution C: Add Pattern to Shared Knowledge** (if valid)
```markdown
If agent's recommendation is valuable:
1. Discuss with team
2. Document pattern in appropriate shared knowledge file
3. Create PR with pattern addition
4. After merge, all agents will use new pattern
```

**Solution D: Reject and Request SmartAdmin Pattern**
```markdown
"Please use SmartAdmin patterns from shared knowledge only.
Do not suggest external patterns unless explicitly requested.

Refer to: .claude/shared/knowledge/smartadmin-patterns.md
Section: [relevant section]"
```

**Prevention**:
- Keep shared knowledge comprehensive and up-to-date
- Document all standard patterns
- Regularly review agent suggestions for pattern compliance
- Add new patterns to shared knowledge as needed

---

### Issue 6.3: "Shared knowledge file is too large/hard to navigate"

**Symptoms**:
- `smartadmin-patterns.md` exceeds 1000 lines
- Hard to find specific patterns
- Agents reference wrong sections

**Diagnosis**:

Shared knowledge file needs refactoring or splitting.

**Solutions**:

**Solution A: Add Table of Contents**
```markdown
# smartadmin-patterns.md

## Table of Contents
1. [Layered Architecture](#layered-architecture)
2. [ResponseDTO Pattern](#responsedto-pattern)
3. [Domain Objects](#domain-objects)
...

<!-- Makes navigation easier -->
```

**Solution B: Split into Focused Files** (if >1500 lines)
```markdown
Current: smartadmin-patterns.md (1800 lines)

Split into:
- smartadmin-architecture.md (Layered architecture, modules)
- smartadmin-api-patterns.md (ResponseDTO, REST conventions)
- smartadmin-domain-patterns.md (Entity, Form, VO, QueryForm)
- smartadmin-data-patterns.md (MyBatis, pagination, transactions)
- smartadmin-security-patterns.md (Sa-Token, permissions)

Update agent frontmatter to reference multiple files:
---
foundation_knowledge:
  - ../shared/knowledge/smartadmin-architecture.md
  - ../shared/knowledge/smartadmin-api-patterns.md
  - ../shared/knowledge/smartadmin-domain-patterns.md
---
```

**Solution C: Add Section Markers**
```markdown
<!-- Use clear section markers -->

## =============================================================================
## SECTION: ResponseDTO Pattern
## =============================================================================

<!-- Pattern details here -->

## =============================================================================
## SECTION: Domain Objects
## =============================================================================

<!-- Makes grep/search easier -->
```

**Prevention**:
- Keep files <1000 lines when possible
- Split logically by domain (architecture, API, data, security)
- Maintain table of contents
- Use consistent formatting

---

## Quick Reference: Troubleshooting Checklist

When encountering issues, check these in order:

### 1. Agent Selection
- [ ] Used decision matrix or capability matrix?
- [ ] Agent appropriate for task?
- [ ] Multi-agent workflow pattern identified?

### 2. Collaboration
- [ ] Handoff package complete?
- [ ] API contracts aligned (backend ↔ frontend)?
- [ ] Permission strings match exactly?
- [ ] Communication templates used?

### 3. Configuration
- [ ] Permissions configured in settings.local.json?
- [ ] Shared knowledge referenced in agent frontmatter?
- [ ] Session restarted after config changes?

### 4. Hooks
- [ ] Hooks enabled?
- [ ] Timeout sufficient?
- [ ] Auto-fix attempts reasonable?
- [ ] Hook steps optimized?

### 5. Performance
- [ ] Requests specific (not vague)?
- [ ] File paths provided when known?
- [ ] Appropriate agent for task?
- [ ] Network connection stable?

### 6. Knowledge Base
- [ ] Shared knowledge files exist?
- [ ] Patterns up-to-date?
- [ ] Session restarted after updates?
- [ ] Agents following documented patterns?

---

## Getting Additional Help

If issue not resolved:

1. **Check related documentation**:
   - [Maintenance Guide](maintenance-guide.md) - Configuration management
   - [Permission Guide](permission-guide.md) - Permission patterns
   - [Agent Capability Matrix](agent-capability-matrix.md) - Agent expertise
   - [Agent Dependencies](../shared/orchestration/agent-dependencies.md) - Collaboration protocols

2. **Review recent changes**:
   ```bash
   # Check recent configuration changes
   git log --oneline -10 .claude/

   # Check recent shared knowledge updates
   git diff HEAD~5 .claude/shared/knowledge/
   ```

3. **Create minimal reproduction**:
   - Simplify to smallest failing case
   - Document exact steps
   - Capture error messages
   - Note environment details

4. **Escalate**:
   - Document issue thoroughly using templates above
   - Include diagnosis results
   - Tag appropriate agent or maintainer
   - Propose potential solutions if possible

---

## Summary

This troubleshooting guide covers:
- **6 major categories** of issues
- **20+ common problems** with solutions
- **Step-by-step diagnosis** procedures
- **Prevention strategies** for each issue
- **Quick reference checklist** for systematic troubleshooting

**When to use this guide**:
- ✅ Before asking for help (self-service first)
- ✅ When agent behavior is unexpected
- ✅ When collaboration is blocked
- ✅ When configuration doesn't work
- ✅ When performance is poor

## Related Documentation

- [.claude/README.md](../README.md) - Directory overview and navigation hub
- [CLAUDE.md (root)](../../CLAUDE.md) - Developer quick reference card
- [Agent Capability Matrix](agent-capability-matrix.md) - Agent comparison
- [Maintenance Guide](maintenance-guide.md) - Configuration updates
- [Permission Guide (archived)](_archive/permission-guide.md) - Permission management
- [Quick Start Guide](quick-start-guide.md) - Getting started

---

**Last Updated**: 2026-01-21 | **Version**: 1.0.0
