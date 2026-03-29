# Quick Start Guide - SmartAdmin .claude Configuration System

**Version**: 2.5.0
**Audience**: New contributors, developers unfamiliar with the agent system
**Estimated Reading Time**: 10-15 minutes

## What is the .claude System?

The `.claude` directory contains an intelligent agent configuration system that automates development tasks for the SmartAdmin project. Think of it as having **9 specialized AI experts** available 24/7 to help with:

- ✅ Backend development (Java/Spring Boot)
- ✅ Frontend development (Vue 3)
- ✅ Database optimization (PostgreSQL)
- ✅ Requirements analysis
- ✅ CI/CD and infrastructure
- ✅ Code quality review
- ✅ Architecture review
- ✅ Resilience testing
- ✅ Documentation creation

## Quick Decision: Which Agent Do I Need?

### I want to...

**...implement a Java feature**
→ Use **java-architect** (Backend expert)
Example: *"Implement employee CRUD API following SmartAdmin patterns"*

**...implement a Vue feature**
→ Use **vue-expert** (Frontend expert)
Example: *"Create employee list page with table and form modal"*

**...understand requirements**
→ Use **business-analyst** (Requirements expert)
Example: *"Analyze requirements for performance review feature"*

**...deploy or setup CI/CD**
→ Use **devops-engineer** (Infrastructure expert)
Example: *"Setup Docker deployment for SmartAdmin"*

**...optimize database**
→ Use **postgres-pro** (Database expert)
Example: *"Optimize slow employee query"*

**...test resilience**
→ Use **chaos-engineer** (Resilience expert)
Example: *"Test system behavior when database is slow"*

**...review code before merge**
→ Use **code-reviewer** (Quality gate)
Example: *"Review my PR for the new notification feature"*

**...review architecture**
→ Use **architect-reviewer** (Design expert)
Example: *"Review the new multi-tenant architecture design"*

**...create documentation**
→ Use **documentation-engineer** (Documentation expert)
Example: *"Document the new employee API endpoints"*

## How Agents Work Together

Agents often collaborate in sequences:

### Example 1: New Full-Stack Feature

```
1. business-analyst (gather requirements)
   ↓
2. java-architect (implement backend API)
   ↓
3. vue-expert (implement frontend)
   ↓
4. code-reviewer (validate quality)
   ↓
5. documentation-engineer (create docs)
   ↓
6. devops-engineer (deploy)
   ↓
7. chaos-engineer (validate resilience)
```

### Example 2: Performance Issue

```
java-architect + postgres-pro (investigate in parallel)
   ↓
Converge on solution
   ↓
devops-engineer (deploy fix & monitor)
```

### Example 3: Production Incident

```
devops-engineer (triage)
   ↓
Route to appropriate specialist:
- Application error → java-architect
- Database issue → postgres-pro
- Infrastructure → devops-engineer
   ↓
chaos-engineer (create test to prevent recurrence)
```

## SmartAdmin Patterns (Must Know)

All agents understand these core SmartAdmin patterns:

### 1. Layered Architecture
```
Controller → Service → Manager → Dao
```

**Rules:**
- Controller calls Service ONLY
- Service calls Manager OR Dao
- Manager calls Dao ONLY
- `@Transactional` ONLY in Manager layer

### 2. API Response Pattern
```java
// Success
return ResponseDTO.ok(data);

// Error
return ResponseDTO.error(ErrorCode.EMPLOYEE_NOT_EXIST);

// With message
return ResponseDTO.okMsg("Created successfully");
```

### 3. Pagination Pattern
```java
// Backend
Page<?> page = SmartPageUtil.convert2PageQuery(form);
List<EmployeeEntity> list = dao.selectList(page, wrapper);
return SmartPageUtil.convert2PageResult(page, list, EmployeeVO.class);
```

### 4. Bean Conversion
```java
// Single object
EmployeeEntity entity = SmartBeanUtil.copy(form, EmployeeEntity.class);

// List
List<EmployeeVO> voList = SmartBeanUtil.copyList(entities, EmployeeVO.class);
```

### 5. Dependency Injection
```java
// ✅ Correct: Constructor injection
@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeDao employeeDao;
}

// ❌ Wrong: Field injection
@Autowired
private EmployeeDao employeeDao;  // Will fail ArchitectureTest
```

### 6. Permission Management
```java
// Backend
@SaCheckPermission("system:employee:add")
@PostMapping("/employee/add")
public ResponseDTO<Long> add(@Valid EmployeeAddForm form) { }

// Frontend (must match backend permission string)
<a-button v-privilege="'system:employee:add'">新增</a-button>
```

## Common Workflows

### Workflow 1: "I want to add a new feature"

**Steps:**
1. Start with **business-analyst** to clarify requirements
2. Use **java-architect** to implement backend
3. Use **vue-expert** to implement frontend
4. Use **code-reviewer** before merging
5. Use **documentation-engineer** to create docs
6. Use **devops-engineer** to deploy
7. Use **chaos-engineer** to validate (for critical features)

**Example request:**
```
"I want to add an employee performance review feature with rating system"
```

**Agent Response:**
- business-analyst gathers requirements, defines user stories
- java-architect implements backend with layered architecture
- vue-expert creates list page and rating form
- code-reviewer validates SmartAdmin patterns
- documentation-engineer creates API docs
- devops-engineer deploys to staging
- chaos-engineer tests failure scenarios

### Workflow 2: "My code is ready for review"

**Steps:**
1. Use **code-reviewer** (quality gate)
   - Automatically coordinates with specialists as needed
2. Fix any issues found
3. Re-run code-reviewer
4. Merge when approved

**Example request:**
```
"Please review my PR for the notification feature"
```

**What happens:**
- code-reviewer scans for security, correctness, performance
- Routes to java-architect for SmartAdmin pattern validation
- Checks permission alignment
- Validates layered architecture compliance
- Returns consolidated findings

### Workflow 3: "The app is slow"

**Steps:**
1. Use **java-architect** + **postgres-pro** in parallel
2. Identify bottlenecks (code vs database)
3. Implement fixes with appropriate agent
4. Use **devops-engineer** to deploy
5. Monitor improvement

**Example request:**
```
"The employee search endpoint is slow (5+ seconds response time)"
```

**Agent Response:**
- java-architect checks for N+1 queries, caching issues
- postgres-pro analyzes query plans, missing indexes
- Agents converge on solution (e.g., add index + implement caching)
- devops-engineer deploys and monitors performance

### Workflow 4: "I want to refactor the architecture"

**Steps:**
1. Use **architect-reviewer** to assess current architecture
2. Get specialist input (java-architect, vue-expert, postgres-pro, devops-engineer)
3. Use **business-analyst** for ROI analysis
4. Use **architect-reviewer** to create refactoring roadmap
5. Implement in phases with **code-reviewer** validation

**Example request:**
```
"Review the employee module architecture for technical debt and suggest improvements"
```

## Automated Quality Assurance (Hooks)

After **java-architect** completes implementation, the hooks system automatically:

1. ✅ Formats code (`spotlessApply`)
2. ✅ Runs ArchitectureTest
3. ✅ Reviews code quality (**code-reviewer**)
4. ✅ Reviews architecture (**architect-reviewer**)
5. ✅ Auto-fixes issues (up to 3 iterations)
6. ✅ Records new rules to knowledge base

**No manual intervention needed!** The system ensures SmartAdmin patterns are followed automatically.

## Key Files to Know

### For Understanding Patterns
- **`.claude/shared/knowledge/smartadmin-patterns.md`** - Backend patterns
- **`.claude/shared/knowledge/smartadmin-frontend-patterns.md`** - Frontend patterns
- **`CLAUDE.md`** (root) - Quick reference card

### For Choosing Agents
- - **`CLAUDE.md`** - Constraints and navigation hub
- **`.claude/shared/orchestration/orchestration-playbook.md`** - Complete workflows, dependencies, handoffs

### For Maintenance
- **`.claude/docs/maintenance-guide.md`** - How to update configuration
- **`.claude/docs/changelog.md`** - Version history

## Common Mistakes to Avoid

### ❌ Mistake 1: Skipping business-analyst for "quick features"
**Why wrong:** Unclear requirements lead to rework
**Correct:** Always start with requirements analysis, even for small features

### ❌ Mistake 2: Using wrong agent
**Why wrong:** Inefficient, may miss best practices
**Correct:** Check [CLAUDE.md](../../CLAUDE.md) for rules

### ❌ Mistake 3: Merging without code-reviewer
**Why wrong:** Bugs and pattern violations slip into production
**Correct:** Always use code-reviewer before merge (enforced by hooks)

### ❌ Mistake 4: Implementing frontend before backend API ready
**Why wrong:** Mocked APIs differ from real APIs, leading to rework
**Correct:** Backend API complete → Frontend implementation

### ❌ Mistake 5: Not testing resilience for critical features
**Why wrong:** Production incidents and downtime
**Correct:** Use chaos-engineer for critical business paths (payment, auth, etc.)

### ❌ Mistake 6: Field injection instead of constructor injection
**Why wrong:** Violates SmartAdmin patterns, fails ArchitectureTest
**Correct:** Use `@RequiredArgsConstructor` with `private final` fields

### ❌ Mistake 7: @Transactional in Service layer
**Why wrong:** Violates SmartAdmin architecture, fails ArchitectureTest
**Correct:** `@Transactional` ONLY in Manager layer

## Testing Your Understanding

### Quiz 1: Which agent?
**Scenario:** "I need to add pagination to the employee list endpoint"

<details>
<summary>Click for answer</summary>

**Answer:** **java-architect** (backend) + **vue-expert** (frontend)

**Explanation:** Pagination requires both backend (SmartPageUtil) and frontend (table pagination) changes.

- java-architect implements: `SmartPageUtil.convert2PageQuery(form)` and `SmartPageUtil.convert2PageResult()`
- vue-expert implements: `a-table` with pagination component

Both must align on pageNum/pageSize/total contract.
</details>

### Quiz 2: What's wrong with this code?
```java
@Service
@Transactional
public class EmployeeService {
    @Autowired
    private EmployeeDao employeeDao;

    public ResponseDTO<Long> add(EmployeeAddForm form) {
        EmployeeEntity entity = new EmployeeEntity();
        entity.setName(form.getName());
        employeeDao.insert(entity);
        return ResponseDTO.ok(entity.getEmployeeId());
    }
}
```

<details>
<summary>Click for answer</summary>

**4 Problems:**
1. ❌ `@Transactional` in Service layer (should be Manager only)
2. ❌ `@Autowired` field injection (should be constructor injection)
3. ❌ Missing `@RequiredArgsConstructor`
4. ❌ Manual bean mapping (should use `SmartBeanUtil.copy()`)

**Correct version:**
```java
@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeManager employeeManager;

    public ResponseDTO<Long> add(EmployeeAddForm form) {
        EmployeeEntity entity = SmartBeanUtil.copy(form, EmployeeEntity.class);
        employeeManager.save(entity);
        return ResponseDTO.ok(entity.getEmployeeId());
    }
}

@Service
@RequiredArgsConstructor
public class EmployeeManager {
    private final EmployeeDao employeeDao;

    @Transactional(rollbackFor = Throwable.class)
    public void save(EmployeeEntity entity) {
        employeeDao.insert(entity);
    }
}
```
</details>

### Quiz 3: Workflow sequence?
**Scenario:** "Production is down with 500 errors from the employee API"

<details>
<summary>Click for answer</summary>

**Answer:**
1. **devops-engineer** (triage and stabilize FIRST)
   - Check infrastructure health
   - Review monitoring dashboards
   - Analyze logs
   - Assess customer impact
2. Route to specialist based on root cause:
   - Application error → **java-architect**
   - Database issue → **postgres-pro**
   - Infrastructure → **devops-engineer** (continues)
3. **chaos-engineer** (create test to prevent recurrence)

**Pattern:** Hub-and-Spoke (devops-engineer is hub, specialists are spokes)

**Why this order:** Stabilize FIRST (stop the bleeding), then fix root cause, then prevent future occurrences.
</details>

## Next Steps

### I want to learn more
1. Read [README.md](../.claude/README.md) for system overview
2. Read [CLAUDE.md](../../CLAUDE.md) for constraints and patterns
3. Read [orchestration-playbook.md](../shared/orchestration/orchestration-playbook.md) for all 10 workflow patterns + collaboration protocols

### I want to contribute
1. Read [maintenance-guide.md](.claude/docs/maintenance-guide.md) for update procedures
2. Read [changelog.md](.claude/docs/changelog.md) for version history
3. Run `.claude/scripts/verify-config.sh` to validate your changes

### I want to customize
1. Update shared knowledge in `.claude/shared/knowledge/` (affects all agents)
2. Update individual agent in `.claude/agents/` (agent-specific expertise)
3. Update orchestration in `.claude/shared/orchestration/` (collaboration patterns)
4. Test with `verify-config.sh`
5. Commit with conventional commit format: `docs(.claude): your change description`

## Getting Help

### Configuration Issues
- Check [maintenance-guide.md - Troubleshooting](.claude/docs/maintenance-guide.md#troubleshooting)
- Run `.claude/scripts/verify-config.sh` to diagnose issues

### Agent Selection Unclear
- Check [CLAUDE.md](../../CLAUDE.md) for rules and constraints
- Look at example scenarios in [workflow-patterns.md](.claude/shared/orchestration/workflow-patterns.md)

### SmartAdmin Patterns Questions
- Check `CLAUDE.md` (root) for quick reference card
- Check `.claude/shared/knowledge/smartadmin-patterns.md` for detailed backend patterns
- Check `.claude/shared/knowledge/smartadmin-frontend-patterns.md` for Vue 3 patterns

## Summary

**Remember these key points:**
1. **Follow SmartAdmin patterns** - `CLAUDE.md` defines all rules
2. **Follow SmartAdmin patterns** - `CLAUDE.md` is your friend
3. **Use multi-agent workflows** - agents collaborate for complex tasks
4. **Always review before merge** - code-reviewer is mandatory
5. **Test resilience for critical features** - chaos-engineer prevents incidents
6. **Document as you go** - documentation-engineer keeps docs current

**The system is designed to help you succeed!** 🎉

Start with simple tasks, learn the patterns, and gradually take on more complex workflows. The agents are here to guide you through SmartAdmin development best practices.

---

**Questions?** Check the [maintenance-guide.md](./maintenance-guide.md) or ask the development team.

---

## Related Documentation

- [.claude/README.md](../README.md) - Directory overview and navigation hub
- [CLAUDE.md (root)](../../CLAUDE.md) - Developer quick reference card
- [Agent Capability Matrix](agent-capability-matrix.md) - Agent comparison
- [Troubleshooting Guide](troubleshooting-guide.md) - Problem resolution
- [Maintenance Guide](maintenance-guide.md) - Configuration updates
