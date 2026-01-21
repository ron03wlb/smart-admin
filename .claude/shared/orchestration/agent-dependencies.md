# Agent Dependencies & Collaboration

**Purpose:** Define how agents depend on each other and collaborate effectively.

## Dependency Graph

### Agent Dependency Flow

```mermaid
graph TB
    %% Analysis & Requirements
    ArchRev[architect-reviewer<br/>Design Validation]
    BA[business-analyst<br/>Requirements & Process]

    %% Implementation Agents
    Java[java-architect<br/>Backend Implementation]
    Vue[vue-expert<br/>Frontend Implementation]
    PG[postgres-pro<br/>Database Optimization]
    Docs[documentation-engineer<br/>Documentation Creation]

    %% Infrastructure & Quality
    DevOps[devops-engineer<br/>CI/CD & Infrastructure]
    CodeRev[code-reviewer<br/>Quality Gate]

    %% Resilience
    Chaos[chaos-engineer<br/>Resilience Testing]

    %% Design validation flows into requirements
    ArchRev -->|validates design| BA
    ArchRev -->|reviews architecture| Java
    ArchRev -->|reviews architecture| Vue

    %% Requirements flow to implementation
    BA -->|requirements| Java
    BA -->|requirements| Vue
    BA -->|requirements| DevOps

    %% Backend to frontend API contract
    Java -->|API contract| Vue
    Java -->|needs optimization| PG
    Java -->|code complete| CodeRev
    Java -->|code complete| Docs

    %% Frontend flows
    Vue -->|code complete| CodeRev
    Vue -->|code complete| Docs

    %% Database flows
    PG -->|optimizations| DevOps
    PG -->|health metrics| Chaos

    %% Documentation flows
    Docs -->|docs complete| DevOps

    %% Quality gate to deployment
    CodeRev -->|approved| DevOps

    %% Deployment to resilience testing
    DevOps -->|deployed| Chaos

    %% Feedback loops
    Chaos -->|issues found| Java
    Chaos -->|issues found| PG
    Chaos -->|issues found| DevOps

    CodeRev -.->|needs arch review| ArchRev

    style ArchRev fill:#4ecdc4,stroke:#22a6b3,color:#fff
    style BA fill:#ffeaa7,stroke:#fdcb6e,color:#000
    style Java fill:#45b7d1,stroke:#3498db,color:#fff
    style Vue fill:#96ceb4,stroke:#6ab04c,color:#fff
    style PG fill:#fd79a8,stroke:#e84393,color:#fff
    style Docs fill:#a29bfe,stroke:#6c5ce7,color:#fff
    style DevOps fill:#dfe6e9,stroke:#b2bec3,color:#000
    style CodeRev fill:#ff6b6b,stroke:#c92a2a,color:#fff
    style Chaos fill:#ff6348,stroke:#e74c3c,color:#fff
```

### Collaboration Patterns

```mermaid
graph LR
    subgraph "Sequential Pattern"
        direction LR
        S1[Agent 1] --> S2[Agent 2]
        S2 --> S3[Agent 3]
        S3 --> S4[Agent 4]
    end

    subgraph "Parallel Pattern"
        direction TB
        P0[Trigger]
        P1[Agent A]
        P2[Agent B]
        P3[Agent C]
        PC[Converge Results]
        P0 --> P1
        P0 --> P2
        P0 --> P3
        P1 --> PC
        P2 --> PC
        P3 --> PC
    end

    subgraph "Hub-and-Spoke Pattern"
        direction TB
        HS1[Specialist 1] --> Hub[Hub Agent]
        HS2[Specialist 2] --> Hub
        HS3[Specialist 3] --> Hub
        Hub --> HSR[Consolidated Result]
    end

    subgraph "Iterative Pattern"
        direction TB
        I1[Assess] --> I2[Implement]
        I2 --> I3[Validate]
        I3 -.->|repeat if needed| I1
        I3 --> I4[Complete]
    end

    style S1 fill:#45b7d1,color:#fff
    style S2 fill:#96ceb4,color:#fff
    style S3 fill:#dfe6e9,color:#000
    style S4 fill:#a29bfe,color:#fff

    style P1 fill:#45b7d1,color:#fff
    style P2 fill:#fd79a8,color:#fff
    style P3 fill:#96ceb4,color:#fff
    style PC fill:#ffeaa7,color:#000

    style Hub fill:#ff6b6b,color:#fff
    style HS1 fill:#45b7d1,color:#fff
    style HS2 fill:#96ceb4,color:#fff
    style HS3 fill:#fd79a8,color:#fff

    style I1 fill:#ffeaa7,color:#000
    style I2 fill:#45b7d1,color:#fff
    style I3 fill:#ff6b6b,color:#fff
    style I4 fill:#00b894,color:#fff
```

## Agent-to-Agent Dependencies

### java-architect Dependencies

**Depends On:**
- **business-analyst** (requirements input)
  - Needs: User stories, business rules, acceptance criteria
  - Before: Starting implementation
  - Format: Written requirements, API contracts

**Collaborates With:**
- **postgres-pro** (database optimization)
  - When: Complex queries, performance issues
  - Exchange: Query patterns, index recommendations
  - Format: SQL queries, execution plans

- **devops-engineer** (deployment coordination)
  - When: Configuration changes, new features
  - Exchange: Application configs, resource requirements
  - Format: application.yml, environment variables

**Feeds Into:**
- **vue-expert** (backend API contracts)
  - Provides: API documentation, Request/Response DTOs, permission requirements
  - When: Backend API complete
  - Format: Swagger docs, API contracts, test data

- **chaos-engineer** (code for testing)
  - Provides: Critical code paths, error handling
  - When: Feature complete
  - Format: Code locations, failure scenarios

### business-analyst Dependencies

**Depends On:**
- **No direct dependencies** (starts the chain)

**Collaborates With:**
- **java-architect** (technical feasibility)
  - When: Defining requirements
  - Exchange: Feasibility assessment, complexity estimates
  - Format: Requirements ↔ technical constraints

- **devops-engineer** (operational requirements)
  - When: Defining NFRs (performance, availability)
  - Exchange: SLAs, monitoring needs
  - Format: Performance targets, uptime requirements

- **chaos-engineer** (resilience requirements)
  - When: Critical business processes
  - Exchange: Recovery objectives (RTO/RPO)
  - Format: Business impact analysis

**Feeds Into:**
- **java-architect** (requirements for implementation)
- **All agents** (context and priorities)

### devops-engineer Dependencies

**Depends On:**
- **java-architect** (application code)
  - Needs: Build artifacts, configuration requirements
  - Before: Deployment
  - Format: JAR files, Dockerfiles, configs

- **postgres-pro** (database requirements)
  - Needs: Database schema, migration scripts
  - Before: Infrastructure provisioning
  - Format: SQL scripts, connection requirements

**Collaborates With:**
- **java-architect** (application configuration)
  - When: Deployment setup, environment configs
  - Exchange: JVM parameters, connection pools
  - Format: application-{env}.yml

- **postgres-pro** (database infrastructure)
  - When: Database deployment, backups
  - Exchange: Infrastructure specs, backup schedules
  - Format: Terraform configs, backup procedures

- **chaos-engineer** (chaos infrastructure)
  - When: Setting up chaos testing
  - Exchange: Monitoring setup, rollback automation
  - Format: Kubernetes configs, alert rules

**Feeds Into:**
- **chaos-engineer** (deployed system for testing)
- **business-analyst** (deployment metrics, uptime data)

### vue-expert Dependencies

**Depends On:**
- **business-analyst** (UI/UX requirements)
  - Needs: User workflows, UI mockups, interaction patterns
  - Before: Starting frontend implementation
  - Format: User stories, wireframes, acceptance criteria

- **java-architect** (backend API contracts)
  - Needs: API endpoints, Request/Response models, permissions
  - Before: API integration
  - Format: Swagger/OpenAPI docs, sample requests, test data

**Collaborates With:**
- **java-architect** (API alignment)
  - When: Integrating frontend with backend
  - Exchange: Request/Response structure validation, permission alignment
  - Format: TypeScript interfaces ↔ Java DTOs, v-privilege ↔ @SaCheckPermission

- **devops-engineer** (frontend deployment)
  - When: Frontend build configuration
  - Exchange: Build artifacts, environment configs, CDN setup
  - Format: Vite configs, environment variables, deployment scripts

- **business-analyst** (UI feedback)
  - When: Reviewing implemented UI
  - Exchange: User workflow validation, UI improvement suggestions
  - Format: Screenshots, interactive prototypes

**Feeds Into:**
- **devops-engineer** (frontend build artifacts)
  - Provides: Built static files, deployment configs
  - When: Frontend complete
  - Format: dist/ folder, nginx configs, environment files

### postgres-pro Dependencies

**Depends On:**
- **java-architect** (application queries)
  - Needs: Query patterns, data access code
  - Before: Optimization
  - Format: MyBatis XML, LambdaQueryWrapper code

**Collaborates With:**
- **java-architect** (query optimization)
  - When: Performance issues, complex queries
  - Exchange: Optimized queries, index recommendations
  - Format: SQL, EXPLAIN ANALYZE output

- **devops-engineer** (database infrastructure)
  - When: Database deployment, monitoring
  - Exchange: Infrastructure requirements, metrics
  - Format: Resource specs, monitoring queries

- **chaos-engineer** (database resilience)
  - When: Testing failover, replication lag
  - Exchange: Database health metrics, failure scenarios
  - Format: SQL scripts, monitoring queries

**Feeds Into:**
- **java-architect** (optimized query patterns)
- **devops-engineer** (database health metrics)
- **chaos-engineer** (database failure scenarios)

### chaos-engineer Dependencies

**Depends On:**
- **java-architect** (application code)
  - Needs: Critical paths, error handling logic
  - Before: Designing experiments
  - Format: Code walkthrough, architecture diagram

- **postgres-pro** (database architecture)
  - Needs: Replication setup, backup procedures
  - Before: Database chaos experiments
  - Format: Database topology, health checks

- **devops-engineer** (infrastructure)
  - Needs: Deployment architecture, monitoring
  - Before: Infrastructure chaos
  - Format: Infrastructure diagram, rollback procedures

**Collaborates With:**
- **All technical agents** (experiment design)
  - When: Designing chaos scenarios
  - Exchange: Failure modes, expected behavior
  - Format: Experiment plans, success criteria

**Feeds Into:**
- **java-architect** (resilience improvements)
- **postgres-pro** (database resilience patterns)
- **devops-engineer** (infrastructure improvements)
- **business-analyst** (resilience reports, risk assessments)

### architect-reviewer Dependencies

**Depends On:**
- **java-architect** (code for architecture review)
  - Needs: Current architecture, module structure, layer implementations
  - Before: Starting architecture review
  - Format: Code walkthrough, architecture diagrams, dependency analysis

- **vue-expert** (frontend architecture)
  - Needs: Frontend architecture patterns, component structure
  - Before: Full-stack architecture review
  - Format: Component hierarchy, state management patterns

**Collaborates With:**
- **java-architect** (architecture validation)
  - When: Reviewing layered architecture, module boundaries
  - Exchange: Architecture compliance findings, refactoring recommendations
  - Format: Architecture review report, violation list

- **postgres-pro** (database architecture)
  - When: Reviewing database schema design, query patterns
  - Exchange: Database architecture assessment, optimization recommendations
  - Format: Schema review, index strategy

- **devops-engineer** (infrastructure architecture)
  - When: Reviewing deployment architecture, scalability
  - Exchange: Infrastructure recommendations, scaling strategies
  - Format: Architecture assessment, capacity planning

**Feeds Into:**
- **java-architect** (refactoring priorities)
  - Provides: Architecture violations, recommended patterns, technical debt priorities
  - When: Architecture review complete
  - Format: Architecture review report, refactoring roadmap

- **business-analyst** (technical debt assessment)
  - Provides: Technical debt impact, ROI for refactoring
  - When: Prioritizing improvements
  - Format: Risk assessment, cost-benefit analysis

### code-reviewer Dependencies

**Depends On:**
- **java-architect** (code to review)
  - Needs: Recently written code, pull request changes
  - Before: Starting code review
  - Format: Git diff, pull request link, implementation context

- **vue-expert** (frontend code to review)
  - Needs: Vue component changes, frontend code
  - Before: Frontend code review
  - Format: Git diff, component files, style changes

**Collaborates With:**
- **architect-reviewer** (architectural violations)
  - When: Code changes impact architecture
  - Exchange: Layer boundary violations, architectural concerns
  - Format: Architecture compliance check

- **java-architect** (SmartAdmin pattern validation)
  - When: Reviewing backend code
  - Exchange: Pattern violations, best practice recommendations
  - Format: Code review comments, refactoring suggestions

- **vue-expert** (Vue best practices)
  - When: Reviewing frontend code
  - Exchange: Vue 3 pattern violations, performance issues
  - Format: Code review comments, optimization suggestions

- **postgres-pro** (query validation)
  - When: Database queries in code changes
  - Exchange: Query optimization recommendations, index suggestions
  - Format: Query review, performance analysis

**Feeds Into:**
- **java-architect** (fixes needed)
  - Provides: Critical/major issues to fix, line-specific feedback
  - When: Code review complete
  - Format: Code review report with severity levels

- **vue-expert** (frontend fixes needed)
  - Provides: Frontend code issues, component improvements
  - When: Frontend review complete
  - Format: Code review report, component feedback

## Handoff Protocols

### business-analyst → java-architect

**Handoff Trigger:** Requirements approved and signed off

**Handoff Package:**
- User stories with acceptance criteria
- Business rules and validation logic
- API contracts (endpoints, request/response)
- Performance requirements (response time, throughput)
- Security requirements (permissions, authentication)

**Acceptance Criteria:**
- Requirements clear and unambiguous
- Technical feasibility validated
- All stakeholders aligned

### java-architect → devops-engineer

**Handoff Trigger:** Feature implemented and tests passing

**Handoff Package:**
- Build artifacts (JAR files)
- Configuration requirements (application.yml changes)
- Environment variables needed
- Database migration scripts (if any)
- Resource requirements (CPU, memory)
- Monitoring endpoints (/actuator/health)

**Acceptance Criteria:**
- All tests pass including ArchitectureTest
- Test coverage >85%
- No security vulnerabilities
- Documentation updated

### java-architect → vue-expert

**Handoff Trigger:** Backend API implemented and tested

**Handoff Package:**
- Swagger/OpenAPI documentation (accessible at /swagger-ui.html)
- API endpoint URLs and HTTP methods
- Request DTO structures (Form objects)
- Response DTO structures (VO objects)
- Permission requirements (@SaCheckPermission annotations)
- Sample request/response JSON
- Error codes and messages (ErrorCode enum)
- Test data (employee IDs, valid payloads)
- Mock server URL (if available)

**Acceptance Criteria:**
- All tests pass (unit + integration)
- API accessible in dev environment
- Swagger documentation complete
- Sample data available for testing
- Permissions configured

**API Contract Alignment Checklist:**
- [ ] Request DTO fields match frontend Form interfaces
- [ ] Response DTO fields match frontend VO interfaces
- [ ] Permission strings documented (for v-privilege)
- [ ] Error handling patterns documented
- [ ] Pagination parameters consistent (pageNum, pageSize)

### java-architect → postgres-pro

**Handoff Trigger:** Complex query or performance issue identified

**Handoff Package:**
- Slow query examples
- Current execution plans (EXPLAIN ANALYZE)
- Expected query volume
- Current performance metrics
- Business requirements for query

**Acceptance Criteria:**
- Query patterns documented
- Performance baseline established
- Optimization goals defined

### vue-expert → devops-engineer

**Handoff Trigger:** Frontend implementation complete and tested

**Handoff Package:**
- Build artifacts (dist/ folder with static files)
- Environment configuration (.env files for dev/test/prod)
- Build commands (npm run build, npm run type-check)
- Vite configuration (vite.config.ts)
- Nginx configuration (if custom routing needed)
- Asset optimization settings
- API proxy configuration
- Frontend resource requirements (CDN, bandwidth)

**Acceptance Criteria:**
- All tests pass (unit + integration)
- Build succeeds without errors
- Type checking passes (TypeScript)
- No linting errors
- Test coverage >80%
- Frontend accessible in dev environment

### devops-engineer → chaos-engineer

**Handoff Trigger:** Critical feature deployed to staging

**Handoff Package:**
- Deployment architecture diagram
- Monitoring dashboards
- Alert rules configured
- Rollback procedures documented
- Steady state metrics defined

**Acceptance Criteria:**
- Monitoring comprehensive
- Rollback tested
- Team trained on procedures

### architect-reviewer → java-architect

**Handoff Trigger:** Architecture review complete with recommendations

**Handoff Package:**
- Architecture review report
- Identified architectural violations
- Recommended patterns and refactoring priorities
- Layer boundary issues
- Module structure improvements
- Technical debt assessment with ROI estimates

**Acceptance Criteria:**
- All architectural violations documented with severity
- Refactoring recommendations are specific and actionable
- Technical debt prioritized by business impact
- Architecture compliance checklist provided

### code-reviewer → java-architect / vue-expert

**Handoff Trigger:** Code review complete with findings

**Handoff Package:**
- Code review report (categorized: Critical/Major/Minor/Suggestions)
- File-specific feedback with line numbers
- Security vulnerabilities identified
- Performance issues flagged
- SmartAdmin pattern violations
- Test coverage gaps
- Pass/Fail decision with rationale

**Acceptance Criteria:**
- All issues categorized by severity
- Specific line numbers and file paths provided
- Suggested fixes included for critical issues
- Clear pass/fail criteria defined
- Re-review requested if needed

## Collaboration Patterns

### Pattern 1: Sequential Handoff

**When:** Dependencies between agents

**Example:** New Full-Stack Feature
```
business-analyst (complete requirements)
        ↓
java-architect (implement backend API)
        ↓
vue-expert (implement frontend)
        ↓
devops-engineer (deploy to staging)
        ↓
chaos-engineer (validate resilience)
```

**Communication:** Each agent waits for previous to complete

### Pattern 2: Parallel Collaboration

**When:** Independent workstreams that converge

**Example:** Performance Optimization
```
        ┌──→ java-architect (code optimization)
problem ┤
        └──→ postgres-pro (query optimization)
                    ↓
            (converge on solution)
                    ↓
            devops-engineer (deploy)
```

**Communication:** Regular sync points to align

### Pattern 3: Frontend-Backend Integration

**When:** API integration issues or contract misalignment

**Example:** API Debugging
```
        ┌──→ java-architect (check backend logs, validation)
problem ┤
        └──→ vue-expert (check frontend payload, error handling)
                    ↓
            (sync on data structures)
                    ↓
        ┌──→ java-architect (fix DTO if needed)
aligned ┤
        └──→ vue-expert (fix TypeScript interface if needed)
                    ↓
            (integration testing)
                    ↓
            devops-engineer (deploy)
```

**Communication:** Real-time collaboration to align API contracts

### Pattern 4: Iterative Refinement

**When:** Solution requires multiple rounds

**Example:** Complex Feature
```
business-analyst → java-architect → business-analyst (clarification)
                        ↓
                java-architect (revised implementation)
                        ↓
                   vue-expert
                        ↓
                devops-engineer
```

**Communication:** Continuous feedback loops

### Pattern 5: Hub-and-Spoke

**When:** Central coordinator with multiple specialists

**Example:** Production Incident
```
            devops-engineer (hub - triage)
                /        |        \        \
               /         |         \        \
    java-architect  vue-expert  postgres-pro  chaos-engineer
     (as needed)    (as needed)  (as needed)   (as needed)
```

**Communication:** Central agent orchestrates

### Pattern 6: Design-First Development (Sequential)

**When:** New feature requiring architectural validation before implementation

**Example:** Complex Feature with Architecture Review
```
architect-reviewer (validate design)
        ↓
business-analyst (refine requirements based on architecture)
        ↓
java-architect (implement backend following architectural guidance)
        ↓
vue-expert (implement frontend)
        ↓
code-reviewer (pre-merge quality gate)
        ↓
devops-engineer (deploy)
```

**Communication:** Each phase validates before next begins

**Benefits:**
- Prevents architectural debt
- Ensures scalable design from start
- Reduces rework from architectural issues

### Pattern 7: Quality Gate (Hub-and-Spoke)

**When:** Pre-merge code review requiring multiple specialist validations

**Example:** Pre-Merge Quality Gate
```
            code-reviewer (hub - initial scan)
                /       |        |        \
               /        |        |         \
    architect-reviewer  |   postgres-pro   |
    (if architecture)   |   (if DB changes)|
                        |                  |
                 java-architect      vue-expert
                 (backend code)    (frontend code)
                        \                /
                         \              /
                    code-reviewer (consolidate)
                            ↓
                    (pass/fail decision)
                            ↓
                    java-architect/vue-expert (fix)
                            ↓
                    code-reviewer (re-validate)
```

**Communication:** Hub coordinates specialist reviews, consolidates findings

**Benefits:**
- Comprehensive quality validation
- Domain-specific expertise applied
- Consolidated feedback for developers
- Clear pass/fail decision

## Communication Standards

### Information Sharing Format

**When sharing context:**
```markdown
## Context
- **From:** [Agent Name]
- **To:** [Agent Name]
- **Task:** [Brief description]
- **Background:** [Relevant context]
- **Artifacts:** [Links to files, data, logs]
- **Expected Output:** [What you need from them]
- **Deadline:** [If time-sensitive]
```

### Status Updates

**Progress updates:**
- Started work on X
- Completed analysis of Y
- Blocked on Z (need input from [Agent])
- Ready for handoff to [Agent]

### Escalation

**When to escalate:**
- Conflicting requirements from multiple agents
- Technical constraint blocks requirement
- Timeline at risk
- Resource constraint identified

**How to escalate:**
- Document the blocker clearly
- Identify who needs to make decision
- Propose options with trade-offs
- Request timely decision

## Coordination Checklist

### Before Starting Work

- [ ] Read shared knowledge documents
- [ ] Check decision matrix - am I the right agent?
- [ ] Review dependencies - do I have required inputs?
- [ ] Check if other agents working on related tasks
- [ ] Notify dependent agents that I'm starting

### During Work

- [ ] Update todos to track progress
- [ ] Document key decisions and rationale
- [ ] Flag issues that affect other agents
- [ ] Seek input when needed (don't guess)

### After Completing Work

- [ ] Mark todos as completed
- [ ] Notify dependent agents work is ready
- [ ] Handoff package complete per protocol
- [ ] Document learnings for future work

## Dependency Matrix

| Agent | Depends On | Feeds Into | Parallel With |
|-------|------------|------------|---------------|
| **architect-reviewer** | java-architect, vue-expert | java-architect, business-analyst | - |
| **business-analyst** | architect-reviewer (optional) | java-architect, vue-expert, All | - |
| **java-architect** | business-analyst | vue-expert, devops, chaos, code-reviewer | postgres-pro |
| **vue-expert** | business-analyst, java-architect | devops, code-reviewer | java-architect (for API debugging) |
| **devops-engineer** | java-architect, vue-expert, postgres-pro | chaos, business-analyst | - |
| **postgres-pro** | java-architect | java-architect, devops | java-architect |
| **code-reviewer** | java-architect, vue-expert | java-architect, vue-expert (for fixes) | architect-reviewer, postgres-pro |
| **chaos-engineer** | All technical | All technical | - |

## Collaboration Troubleshooting Guide

### Common Handoff Issues and Solutions

#### Issue 1: API Contract Mismatch (Backend ↔ Frontend)

**Symptoms**:
- Frontend receives 400 Bad Request errors
- Field name mismatches (e.g., `employeeName` vs `employee_name`)
- Type mismatches (string vs number)
- Date format inconsistencies

**Root Causes**:
- DTO field names differ from TypeScript interface properties
- Enum values don't match exactly
- Pagination parameter names inconsistent
- Date serialization format differs

**Prevention**:
- java-architect provides exact JSON samples (copy-paste ready)
- vue-expert validates against Swagger docs before implementation
- Use SmartAdmin patterns: `pageNum`, `pageSize` (not `page`, `size`)
- Date format: Always ISO 8601 (`yyyy-MM-dd'T'HH:mm:ss.SSS'Z'`)

**Resolution** (4-step fix):
1. **java-architect**: Provide sample JSON for request/response
   ```json
   {
     "employeeName": "John Doe",
     "departmentId": 123,
     "hireDate": "2024-01-15T00:00:00.000Z"
   }
   ```
2. **vue-expert**: Compare TypeScript interface with JSON
3. **Identify mismatch**: Document exact differences
4. **Align**: Update either backend DTO or frontend interface (prefer backend as source of truth)

---

#### Issue 2: Permission String Mismatch

**Symptoms**:
- User gets 403 Forbidden despite having correct role
- `v-privilege` directive doesn't hide UI elements
- Sa-Token throws permission denied

**Root Cause**:
- Frontend uses `v-privilege="employee:add"`
- Backend requires `@SaCheckPermission("system:employee:add")`
- Missing `system:` prefix in frontend

**Prevention**:
- java-architect documents **exact** permission strings in handoff package
- vue-expert copies permission strings **verbatim** (no modifications)
- Backend provides constants: `public static final String PERMISSION = "system:employee:add";`

**Resolution**:
1. **java-architect**: Extract all `@SaCheckPermission` strings from controllers
2. **java-architect**: Provide list in handoff package:
   ```markdown
   ### Permission Strings
   - Add Employee: `system:employee:add`
   - Edit Employee: `system:employee:edit`
   - Delete Employee: `system:employee:delete`
   - Query Employees: `system:employee:query`
   ```
3. **vue-expert**: Use exact strings in `v-privilege` directives
4. **Test**: Verify permissions work end-to-end

---

#### Issue 3: Pagination Parameter Inconsistency

**Symptoms**:
- Frontend sends `page` but backend expects `pageNum`
- Backend returns wrong page
- Pagination controls don't work

**Root Cause**:
- Not following SmartAdmin pagination standard

**SmartAdmin Standard**:
- **Always** use `pageNum` and `pageSize` (never `page`, `size`, `offset`, `limit`)
- Backend: `PageParam` base class with `pageNum`, `pageSize`
- Frontend: Send `pageNum`, `pageSize` in query params

**Prevention**:
- Use `SmartPageUtil.convert2PageQuery(form)` in backend
- Use consistent naming in frontend API calls

**Resolution**:
1. **java-architect**: Verify `QueryForm` extends `PageParam`
2. **vue-expert**: Update API call to use `pageNum`, `pageSize`
3. **Test**: Verify pagination works across pages

---

#### Issue 4: Blocked Waiting for Handoff

**Symptoms**:
- Agent waiting for another agent to complete work
- Work stalled, no progress
- Uncertainty about when work will be ready

**Root Causes**:
- Previous agent taking longer than expected
- Unclear handoff timeline
- No communication about delays

**Prevention**:
- Agree on handoff timeline upfront
- Communicate delays immediately
- Consider partial handoffs (e.g., API contract before full implementation)

**Resolution**:
1. **Waiting agent**: Ask for status update
2. **Blocking agent**: Provide realistic ETA or partial handoff
3. **Options**:
   - **Partial handoff**: Share API contract, let frontend mock API
   - **Parallel work**: Work on independent parts while waiting
   - **Escalate**: If blocked >4 hours, escalate to business-analyst

---

#### Issue 5: Reviewers Overwhelmed with Context

**Symptoms**:
- code-reviewer asks many clarification questions
- Review takes much longer than expected
- Reviewer misses important context

**Root Causes**:
- PR description lacks context
- No design docs provided
- Test results not shared
- Changes too large (>500 lines)

**Prevention**:
- Provide complete handoff package to code-reviewer:
  - PR description with context
  - Link to requirements/design docs
  - Test results (all passing)
  - Screenshots/demo for UI changes
  - Performance metrics (if applicable)

**Resolution**:
1. **Developer**: Create comprehensive PR description using template (see below)
2. **code-reviewer**: Has all context needed, reviews efficiently
3. **If PR too large**: Break into smaller PRs (recommended: <300 lines per PR)

---

### Collaboration Communication Templates

#### Template 1: Handoff Notification

```markdown
### Handoff from [Agent A] to [Agent B]

**Task**: [Description of completed work]

**What's Complete**:
- ✅ [Deliverable 1]
- ✅ [Deliverable 2]
- ✅ [Deliverable 3]

**Handoff Package**:
- [Link to code/branch/commit]
- [Link to documentation]
- [Link to Swagger/API docs if applicable]

**What You Need to Do**:
1. [Step 1]
2. [Step 2]
3. [Step 3]

**Expected Output**:
- [Deliverable 1]
- [Deliverable 2]

**Blocked On**: None | [Blocker description if applicable]

**Questions**:
- [Question 1 if clarification needed]
- [Question 2 if clarification needed]

**Estimated Effort**: [e.g., 3-5 hours] (optional)
```

**Example Usage**:
```markdown
### Handoff from java-architect to vue-expert

**Task**: Employee management backend API implementation

**What's Complete**:
- ✅ REST API endpoints (CRUD operations)
- ✅ Service + Manager + Dao layers
- ✅ Unit tests (92% coverage)
- ✅ Integration tests passing
- ✅ Swagger documentation

**Handoff Package**:
- Branch: `feature/employee-management-backend`
- Swagger: http://localhost:1024/swagger-ui.html#/employee-controller
- Sample JSON: See attached employee-api-samples.json
- Permission strings:
  - Add: `system:employee:add`
  - Edit: `system:employee:edit`
  - Delete: `system:employee:delete`
  - Query: `system:employee:query`

**What You Need to Do**:
1. Create employee list page with table, filters, pagination
2. Create employee add/edit form with validation
3. Integrate with backend API
4. Add permission directives using exact permission strings above

**Expected Output**:
- Employee management UI in `smart-admin-web/src/views/employee/`
- Components: EmployeeList.vue, EmployeeForm.vue
- API integration using provided endpoints
- Frontend tests passing

**Blocked On**: None

**Questions**:
- Should we support bulk operations (multi-select + bulk delete)?
```

---

#### Template 2: Blocking Issue Escalation

```markdown
### 🚨 BLOCKER: [Issue Title]

**Impact**: [Who is blocked / what work is blocked]

**Root Cause**: [Why we're blocked]

**Options**:
- **Option A**: [Description]
  - Pros: [Pros]
  - Cons: [Cons]
  - Effort: [Estimate]

- **Option B**: [Description]
  - Pros: [Pros]
  - Cons: [Cons]
  - Effort: [Estimate]

- **Option C**: [Description]
  - Pros: [Pros]
  - Cons: [Cons]
  - Effort: [Estimate]

**Recommendation**: [Preferred option with rationale]

**Decision Needed From**: [Stakeholder]

**Urgency**: Critical / High / Medium / Low

**Timeline**: [When decision is needed]
```

**Example Usage**:
```markdown
### 🚨 BLOCKER: API Contract Incompatible with Frontend Requirements

**Impact**: vue-expert blocked on employee management implementation

**Root Cause**: Backend pagination returns `total` as number, but frontend table component requires `{ total, pages, current }` object

**Options**:
- **Option A**: Change backend DTO to return pagination object
  - Pros: Frontend gets all needed info
  - Cons: Breaking change for existing frontend code
  - Effort: 2 hours (backend change + update existing frontend)

- **Option B**: Frontend transforms backend response
  - Pros: No backend changes
  - Cons: Frontend duplication (every API call needs transformation)
  - Effort: 1 hour (create utility function)

- **Option C**: Adopt SmartAdmin standard `PageResult<T>` format
  - Pros: Consistent with SmartAdmin patterns, reusable
  - Cons: Requires backend change
  - Effort: 1.5 hours (backend change + utility function)

**Recommendation**: Option C - Adopt SmartAdmin `PageResult<T>` format
- Rationale: Follows SmartAdmin conventions, most maintainable long-term

**Decision Needed From**: java-architect

**Urgency**: High (vue-expert blocked)

**Timeline**: Decision needed within 2 hours to avoid delaying frontend work
```

---

#### Template 3: Multi-Agent Sync Request

```markdown
### Sync Request: [Topic]

**Participants**: @[agent-1] @[agent-2] @[agent-3]

**Purpose**: [Why we need to sync]

**Discussion Points**:
1. [Point 1]
2. [Point 2]
3. [Point 3]

**Desired Outcome**:
- [Goal 1]
- [Goal 2]

**Duration**: [Expected sync time, e.g., 30 min]

**Proposed Time**: [When to sync, if applicable]
```

**Example Usage**:
```markdown
### Sync Request: Employee Performance Review API Design

**Participants**: @business-analyst @java-architect @vue-expert

**Purpose**: Align on API contract before implementation to avoid rework

**Discussion Points**:
1. What fields should be in the review form?
2. How do we handle multi-step approval workflow?
3. What permission granularity (view vs edit vs approve)?
4. Do we need file attachments?

**Desired Outcome**:
- Agreed API contract (endpoints, DTOs, permissions)
- Documented approval workflow
- Clear handoff packages defined

**Duration**: 30-45 minutes

**Proposed Time**: Next available window for all three agents
```

---

### Handoff Quality Checklists

#### java-architect → vue-expert

**Checklist for java-architect**:
- [ ] Backend tests passing (run `./gradlew :sa-admin:test`)
- [ ] Swagger documentation accessible (http://localhost:1024/swagger-ui.html)
- [ ] Sample JSON provided for each endpoint (request + response)
- [ ] Permission strings documented (exact format for `v-privilege`)
- [ ] Error codes and messages documented
- [ ] Test data created in database (or SQL script provided)
- [ ] Backend running and accessible (dev environment or localhost)

**Checklist for vue-expert** (before starting):
- [ ] Swagger docs reviewed and understood
- [ ] Sample JSON compared with TypeScript interfaces
- [ ] Permission strings copied to frontend constants
- [ ] Test data accessible (can call API endpoints successfully)
- [ ] API contract questions resolved with java-architect

---

#### vue-expert → devops-engineer

**Checklist for vue-expert**:
- [ ] Frontend tests passing (run `npm run test`)
- [ ] TypeScript type checking clean (run `npm run type-check`)
- [ ] Build succeeds (run `npm run build`)
- [ ] API integration tested (all endpoints work end-to-end)
- [ ] Browser console clean (no errors or warnings)
- [ ] Responsive design validated (mobile, tablet, desktop)
- [ ] Accessibility validated (keyboard navigation, screen readers)

**Checklist for devops-engineer** (before deploying):
- [ ] Build artifacts generated successfully
- [ ] Environment variables documented
- [ ] Dependencies compatible with production
- [ ] Smoke tests defined for deployment validation

---

#### devops-engineer → chaos-engineer

**Checklist for devops-engineer**:
- [ ] Deployed to staging and healthy (health check passing)
- [ ] Smoke tests passing (basic functionality works)
- [ ] Monitoring dashboards configured (metrics, logs, traces)
- [ ] Rollback procedure tested and documented
- [ ] Production deployment plan ready

**Checklist for chaos-engineer** (before starting):
- [ ] Staging environment accessible
- [ ] Monitoring accessible (to observe failures)
- [ ] Rollback procedure understood
- [ ] Failure scenarios defined and prioritized

---

### Escalation Paths

#### When to Escalate to business-analyst

Escalate when:
- ❗ Requirements unclear or ambiguous
- ❗ Business logic questions arise during implementation
- ❗ Conflicting requirements discovered
- ❗ Scope creep detected
- ❗ Stakeholder decision needed

**How to escalate**:
1. Document the ambiguity/conflict clearly
2. Provide context (what you're trying to implement)
3. List questions or options
4. Tag business-analyst with Template 2 (Blocking Issue Escalation)

---

#### When to Escalate to architect-reviewer

Escalate when:
- ❗ Significant architectural decision required
- ❗ Design patterns unclear or conflicting
- ❗ Scalability concerns
- ❗ Technical debt tradeoffs needed
- ❗ Cross-cutting concerns (security, performance) require design

**How to escalate**:
1. Describe the architectural challenge
2. Provide context (current design, constraints)
3. List architectural options with pros/cons
4. Tag architect-reviewer with Template 2 (Blocking Issue Escalation)

---

#### When to Escalate to code-reviewer

Escalate when:
- ❗ Code quality concerns during implementation
- ❗ Security vulnerabilities discovered
- ❗ Performance issues detected
- ❗ Architectural violations in existing code
- ❗ Need pre-merge review guidance

**How to escalate**:
1. Describe the code quality/security/performance issue
2. Provide file paths and line numbers
3. Ask for recommended approach
4. Tag code-reviewer with clear question

---

## Summary

**Key Principles:**
1. **Clear handoffs** - Know when to pass work to next agent
2. **Complete packages** - Provide all information needed
3. **Continuous communication** - Don't work in silos
4. **Respect dependencies** - Don't skip required inputs
5. **Parallel when possible** - Work simultaneously when independent

**Most Important:**
- architect-reviewer validates design before implementation (optional but recommended for complex features)
- business-analyst typically starts new features
- java-architect implements backend APIs
- vue-expert implements frontend UI
- java-architect ↔ vue-expert must align on API contracts
- devops-engineer enables deployment (both backend + frontend)
- postgres-pro optimizes database
- code-reviewer performs pre-merge quality gate
- chaos-engineer validates resilience

**Key Handoffs:**
- Architect-Reviewer → Java: Architecture validation + refactoring priorities
- BA → Java: Requirements with API contracts
- Java → Vue: Swagger docs + test data + permissions
- Vue → DevOps: Build artifacts + configs
- Java/Vue → Code-Reviewer: Pull request for pre-merge review
- Code-Reviewer → Java/Vue: Review findings + fixes needed
- Java/Vue → DevOps: Complete feature for deployment

**New Patterns:**
- Pattern 6: Design-First Development (validate architecture before coding)
- Pattern 7: Quality Gate (hub-and-spoke pre-merge review)

**Collaboration beats isolation!**
