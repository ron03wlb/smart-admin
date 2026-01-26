# Agent Capability Matrix

**Purpose**: Visual comparison of agent capabilities for instant agent selection

**Version**: 1.0.0
**Last Updated**: 2026-01-21

---

## Quick Comparison Table

Legend:
- ✅ **Primary** - Agent's core expertise, handles independently
- 🤝 **Collaborate** - Contributes as part of team effort
- 🔍 **Review** - Reviews/validates work from other agents
- 📝 **Document** - Documents implementation/decisions
- ❌ **Not Involved** - Outside agent's scope

| Capability | java-architect | vue-expert | postgres-pro | devops-engineer | business-analyst | chaos-engineer | architect-reviewer | code-reviewer | documentation-engineer |
|------------|:--------------:|:----------:|:------------:|:---------------:|:----------------:|:--------------:|:------------------:|:-------------:|:----------------------:|
| **Backend Development** |
| Implement REST API | ✅ | ❌ | ❌ | ❌ | 📝 | ❌ | 🔍 | 🔍 | 📝 |
| Service Layer Logic | ✅ | ❌ | ❌ | ❌ | 🤝 | ❌ | 🔍 | 🔍 | 📝 |
| Manager Layer (Transactions) | ✅ | ❌ | ❌ | ❌ | ❌ | 🤝 | 🔍 | 🔍 | 📝 |
| Exception Handling | ✅ | ❌ | ❌ | 🤝 | ❌ | 🤝 | 🔍 | 🔍 | 📝 |
| Sa-Token Integration | ✅ | ❌ | ❌ | ❌ | ❌ | 🤝 | 🔍 | 🔍 | 📝 |
| **Frontend Development** |
| Vue 3 Components | ❌ | ✅ | ❌ | ❌ | 📝 | ❌ | 🔍 | 🔍 | 📝 |
| TypeScript Integration | ❌ | ✅ | ❌ | ❌ | ❌ | ❌ | 🔍 | 🔍 | 📝 |
| State Management (Pinia) | ❌ | ✅ | ❌ | ❌ | ❌ | ❌ | 🔍 | 🔍 | 📝 |
| API Integration | 🤝 | ✅ | ❌ | ❌ | ❌ | ❌ | 🔍 | 🔍 | 📝 |
| UI/UX Implementation | ❌ | ✅ | ❌ | ❌ | 🤝 | ❌ | 🔍 | 🔍 | 📝 |
| **Database Operations** |
| Write SQL Queries | ✅ | ❌ | ✅ | ❌ | ❌ | ❌ | 🔍 | 🔍 | 📝 |
| Optimize Query Performance | 🤝 | ❌ | ✅ | ❌ | ❌ | ❌ | 🔍 | 🔍 | 📝 |
| Database Schema Design | 🤝 | ❌ | ✅ | ❌ | 🤝 | ❌ | 🔍 | 🔍 | 📝 |
| Index Management | ❌ | ❌ | ✅ | ❌ | ❌ | ❌ | 🔍 | 🔍 | 📝 |
| Replication Setup | ❌ | ❌ | ✅ | 🤝 | ❌ | 🤝 | 🔍 | 🔍 | 📝 |
| **Infrastructure & Deployment** |
| CI/CD Pipeline | ❌ | ❌ | ❌ | ✅ | ❌ | 🤝 | 🔍 | 🔍 | 📝 |
| Docker Configuration | ❌ | ❌ | 🤝 | ✅ | ❌ | 🤝 | 🔍 | 🔍 | 📝 |
| Kubernetes Deployment | ❌ | ❌ | ❌ | ✅ | ❌ | 🤝 | 🔍 | 🔍 | 📝 |
| Monitoring Setup | ❌ | ❌ | 🤝 | ✅ | ❌ | 🤝 | 🔍 | 🔍 | 📝 |
| Log Aggregation | ❌ | ❌ | ❌ | ✅ | ❌ | 🤝 | 🔍 | 🔍 | 📝 |
| **Analysis & Planning** |
| Requirements Gathering | 🤝 | 🤝 | ❌ | ❌ | ✅ | ❌ | 🤝 | ❌ | 📝 |
| User Story Creation | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | 🤝 | ❌ | 📝 |
| API Contract Design | 🤝 | 🤝 | ❌ | ❌ | ✅ | ❌ | 🔍 | 🔍 | 📝 |
| Business Logic Analysis | 🤝 | ❌ | ❌ | ❌ | ✅ | ❌ | 🔍 | 🔍 | 📝 |
| Stakeholder Communication | 🤝 | 🤝 | ❌ | 🤝 | ✅ | ❌ | 🤝 | 🤝 | 🤝 |
| **Testing & Quality** |
| Unit Testing | ✅ | ✅ | ❌ | 🤝 | ❌ | 🤝 | 🔍 | ✅ | 📝 |
| Integration Testing | ✅ | ✅ | 🤝 | 🤝 | ❌ | 🤝 | 🔍 | ✅ | 📝 |
| Resilience Testing | 🤝 | 🤝 | 🤝 | 🤝 | ❌ | ✅ | 🔍 | 🔍 | 📝 |
| Performance Testing | 🤝 | 🤝 | 🤝 | 🤝 | ❌ | ✅ | 🔍 | 🔍 | 📝 |
| Code Quality Review | 🤝 | 🤝 | 🤝 | 🤝 | ❌ | ❌ | 🤝 | ✅ | 📝 |
| **Architecture & Design** |
| System Architecture | 🤝 | ❌ | 🤝 | 🤝 | 🤝 | ❌ | ✅ | 🤝 | 📝 |
| Design Patterns | ✅ | ✅ | 🤝 | 🤝 | ❌ | ❌ | ✅ | 🔍 | 📝 |
| Scalability Analysis | 🤝 | 🤝 | 🤝 | 🤝 | 🤝 | 🤝 | ✅ | 🔍 | 📝 |
| Technical Debt Assessment | 🤝 | 🤝 | 🤝 | 🤝 | 🤝 | ❌ | ✅ | 🤝 | 📝 |
| Architecture Documentation | 🤝 | 🤝 | 🤝 | 🤝 | 🤝 | ❌ | 🤝 | ❌ | ✅ |
| **Documentation** |
| API Documentation | 🤝 | 🤝 | ❌ | ❌ | 🤝 | ❌ | ❌ | ❌ | ✅ |
| Technical Guides | 🤝 | 🤝 | 🤝 | 🤝 | 🤝 | 🤝 | 🤝 | 🤝 | ✅ |
| Architecture Diagrams | 🤝 | 🤝 | 🤝 | 🤝 | 🤝 | 🤝 | 🤝 | 🤝 | ✅ |
| User Documentation | ❌ | 🤝 | ❌ | ❌ | 🤝 | ❌ | ❌ | ❌ | ✅ |
| Code Comments | ✅ | ✅ | 🤝 | 🤝 | ❌ | 🤝 | 🤝 | 🤝 | 📝 |

---

## Skill Level Matrix

Rating Scale: ⭐ (1 star) = Basic familiarity → ⭐⭐⭐⭐⭐ (5 stars) = Deep expertise

| Skill Dimension | java-architect | vue-expert | postgres-pro | devops-engineer | business-analyst | chaos-engineer | architect-reviewer | code-reviewer | documentation-engineer |
|-----------------|:--------------:|:----------:|:------------:|:---------------:|:----------------:|:--------------:|:------------------:|:-------------:|:----------------------:|
| **Backend Development** | ⭐⭐⭐⭐⭐ | ⭐ | ⭐⭐ | ⭐⭐ | ⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐ |
| **Frontend Development** | ⭐ | ⭐⭐⭐⭐⭐ | ⭐ | ⭐⭐ | ⭐⭐ | ⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐ |
| **Database Expertise** | ⭐⭐⭐ | ⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐ | ⭐ | ⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐ |
| **Infrastructure & DevOps** | ⭐⭐ | ⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐ |
| **Business Analysis** | ⭐⭐⭐ | ⭐⭐ | ⭐ | ⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐ |
| **Testing & Quality** | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐ |
| **System Architecture** | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ |

---

## Task Complexity & Duration

**Note**: Duration estimates are guidelines only. Actual time varies based on requirements.

### Common Tasks by Agent

#### java-architect

| Task Type | Complexity | Typical Duration | Complexity Factors |
|-----------|------------|------------------|-------------------|
| Simple CRUD API | Low | 1-2 hours | Number of fields, validation rules |
| Complex Business Logic | Medium-High | 4-8 hours | Business rules complexity, integrations |
| N+1 Query Optimization | Medium | 2-4 hours | Number of queries, data relationships |
| Caching Implementation | Medium | 2-4 hours | Cache invalidation strategy, data size |
| Transaction Orchestration | High | 4-6 hours | Number of services, rollback complexity |

#### vue-expert

| Task Type | Complexity | Typical Duration | Complexity Factors |
|-----------|------------|------------------|-------------------|
| Simple Form Component | Low | 1-2 hours | Fields count, validation complexity |
| Complex Table with Filters | Medium | 3-5 hours | Filter types, pagination, sorting |
| State Management Setup | Medium | 2-4 hours | State complexity, async operations |
| API Integration | Low-Medium | 2-3 hours | API endpoints count, error handling |
| Complex Interactive UI | High | 6-10 hours | Interactivity, animations, responsiveness |

#### postgres-pro

| Task Type | Complexity | Typical Duration | Complexity Factors |
|-----------|------------|------------------|-------------------|
| Query Optimization | Medium | 2-4 hours | Query complexity, data volume |
| Index Strategy | Medium-High | 3-6 hours | Table size, query patterns |
| Schema Refactoring | High | 6-12 hours | Dependencies, migration complexity |
| Replication Setup | High | 8-16 hours | Architecture, failover requirements |
| Performance Audit | Medium-High | 4-8 hours | Database size, query volume |

#### devops-engineer

| Task Type | Complexity | Typical Duration | Complexity Factors |
|-----------|------------|------------------|-------------------|
| Basic CI/CD Pipeline | Medium | 4-6 hours | Build steps, test coverage |
| Docker Containerization | Low-Medium | 2-4 hours | Dependencies, multi-stage builds |
| Kubernetes Deployment | High | 8-12 hours | Services count, networking, volumes |
| Monitoring Setup | Medium | 4-6 hours | Metrics count, alerting rules |
| Production Incident | Variable | 1-8 hours | Incident severity, root cause complexity |

#### business-analyst

| Task Type | Complexity | Typical Duration | Complexity Factors |
|-----------|------------|------------------|-------------------|
| Requirements Gathering | Medium | 2-4 hours | Stakeholders count, scope clarity |
| User Story Creation | Low-Medium | 1-3 hours | Feature complexity, acceptance criteria |
| API Contract Design | Medium | 2-4 hours | Endpoints count, data model complexity |
| Impact Analysis | Medium-High | 3-6 hours | System touchpoints, dependencies |
| Business Process Modeling | High | 6-10 hours | Process complexity, edge cases |

#### chaos-engineer

| Task Type | Complexity | Typical Duration | Complexity Factors |
|-----------|------------|------------------|-------------------|
| Failure Scenario Design | Medium | 2-4 hours | System complexity, failure types |
| Resilience Testing | Medium-High | 4-8 hours | Test scenarios, validation depth |
| Game Day Execution | High | 6-12 hours | Scope, team coordination |
| Recovery Validation | Medium | 2-4 hours | Recovery procedures, validation steps |
| Chaos Report | Low-Medium | 1-2 hours | Findings count, recommendations |

#### architect-reviewer

| Task Type | Complexity | Typical Duration | Complexity Factors |
|-----------|------------|------------------|-------------------|
| Architecture Review | High | 4-8 hours | System size, architectural decisions |
| Design Pattern Validation | Medium | 2-4 hours | Patterns count, complexity |
| Scalability Assessment | High | 6-10 hours | Load requirements, bottlenecks |
| Technical Debt Analysis | Medium-High | 4-8 hours | Codebase size, debt severity |
| Architecture Documentation | Medium-High | 4-6 hours | System complexity, diagram count |

#### code-reviewer

| Task Type | Complexity | Typical Duration | Complexity Factors |
|-----------|------------|------------------|-------------------|
| Code Quality Review | Medium | 2-4 hours | Lines of code, issue density |
| Security Review | High | 4-6 hours | Security requirements, risk level |
| Performance Review | Medium-High | 3-5 hours | Performance targets, bottlenecks |
| Pre-Merge Gate | Medium | 2-3 hours | Change size, test coverage |
| Refactoring Validation | Medium-High | 3-5 hours | Refactoring scope, test coverage |

#### documentation-engineer

| Task Type | Complexity | Typical Duration | Complexity Factors |
|-----------|------------|------------------|-------------------|
| API Documentation | Medium | 2-4 hours | Endpoints count, complexity |
| Architecture Guide | High | 6-10 hours | System complexity, diagram needs |
| User Guide | Medium | 3-5 hours | Feature complexity, audience |
| Technical Tutorial | Medium-High | 4-6 hours | Depth required, examples needed |
| Code Documentation | Low-Medium | 1-3 hours | Code complexity, existing docs |

---

## Common Agent Combinations

### Sequential Workflows

#### 1. Full-Stack Feature Implementation
**Flow**: business-analyst → java-architect → vue-expert → code-reviewer → devops-engineer → chaos-engineer → documentation-engineer

**When to Use**: New feature requires backend + frontend + deployment

**Handoff Points**:
- BA → Java: Requirements, user stories, API contract
- Java → Vue: Backend running, Swagger docs, sample JSON
- Vue → Code: Frontend complete, tests passing
- Code → DevOps: Review complete, quality gate passed
- DevOps → Chaos: Deployed to staging, smoke tests passing
- Chaos → Doc: Resilience validated, production ready

**Example**: "Add employee performance review feature"

---

#### 2. Database-First Implementation
**Flow**: postgres-pro → java-architect → vue-expert → code-reviewer

**When to Use**: Database changes drive application logic

**Handoff Points**:
- Postgres → Java: Schema ready, migrations tested, indexes optimized
- Java → Vue: API updated, entity models aligned
- Vue → Code: UI reflects new data model

**Example**: "Add multi-tenancy support with tenant isolation"

---

### Parallel Workflows

#### 3. Performance Optimization
**Flow**: java-architect + postgres-pro + vue-expert (parallel) → architect-reviewer (consolidate) → devops-engineer

**When to Use**: Performance issues require investigation across layers

**Coordination**: architect-reviewer consolidates findings and prioritizes optimizations

**Example**: "Application is slow, improve response time"

---

#### 4. Production Incident Response
**Flow**: devops-engineer (triage) → [java-architect | postgres-pro | vue-expert] (parallel investigation) → devops-engineer (coordinate fix) → chaos-engineer (prevent recurrence)

**When to Use**: Production incident requires rapid diagnosis

**Coordination**: devops-engineer acts as hub, routes to specialists

**Example**: "Service is returning 500 errors"

---

### Hub-and-Spoke Workflows

#### 5. Pre-Merge Quality Gate
**Flow**: code-reviewer (hub) → [java-architect | vue-expert | architect-reviewer] (spokes) → code-reviewer (consolidate)

**When to Use**: Large PR requires multiple specialist reviews

**Coordination**: code-reviewer orchestrates specialist reviews, makes final decision

**Example**: "Review PR: Major refactoring of authentication system"

---

#### 6. Architecture Evolution
**Flow**: architect-reviewer (hub) → [business-analyst | java-architect | postgres-pro | devops-engineer] (gather input) → architect-reviewer (design) → documentation-engineer (document)

**When to Use**: Significant architectural changes needed

**Coordination**: architect-reviewer gathers requirements and designs solution

**Example**: "Transition from monolith to microservices"

---

## Agent Selection Decision Tree

### Q1: What is your primary goal?

**A) Implement new functionality** → Go to Q2
**B) Review/validate existing work** → Go to Q3
**C) Deploy/operate system** → Go to Q4
**D) Analyze/plan** → Go to Q5
**E) Document** → **documentation-engineer**

---

### Q2: Implementation - What layer?

**A) Backend (Java/Spring Boot)** → **java-architect**
**B) Frontend (Vue.js)** → **vue-expert**
**C) Database (PostgreSQL)** → **postgres-pro**
**D) Full-stack (Backend + Frontend)** → Start with **business-analyst**, then sequential workflow
**E) Not sure** → **business-analyst** (will clarify requirements)

---

### Q3: Review - What aspect?

**A) Architecture/design** → **architect-reviewer**
**B) Code quality/security/performance** → **code-reviewer**
**C) Resilience/failure handling** → **chaos-engineer**
**D) All of the above (pre-merge)** → **code-reviewer** (orchestrates specialist reviews)

---

### Q4: Deploy/Operate - What operation?

**A) CI/CD setup** → **devops-engineer**
**B) Deployment to production** → **devops-engineer**
**C) Monitoring/alerting** → **devops-engineer**
**D) Incident response** → **devops-engineer** (hub, coordinates specialists)
**E) Resilience testing** → **chaos-engineer**

---

### Q5: Analyze/Plan - What focus?

**A) Business requirements** → **business-analyst**
**B) Architecture planning** → **architect-reviewer**
**C) Performance analysis** → **java-architect** + **postgres-pro** (parallel)
**D) Failure analysis** → **chaos-engineer**
**E) Stakeholder needs** → **business-analyst**

---

## Agent Collaboration Patterns

### Pattern 1: Backend → Frontend Handoff

**Critical Success Factor**: API Contract Alignment

**Handoff Package from java-architect**:
- ✅ Backend running locally or in dev environment
- ✅ Swagger documentation accessible
- ✅ Sample JSON request/response for each endpoint
- ✅ Permission strings documented (exact format for v-privilege)
- ✅ Error codes and messages documented
- ✅ Test data created in database

**What vue-expert validates**:
- DTO field names match TypeScript interfaces
- Enum values match exactly
- Date formats consistent (ISO 8601)
- Pagination parameters match (pageNum, pageSize)
- Permission strings copied verbatim

**Common Issues**:
- ❌ Field name mismatch: `employeeName` (backend) vs `employee_name` (frontend)
- ❌ Permission string mismatch: Backend has `system:employee:add`, frontend uses `employee:add`
- ❌ Pagination: Backend expects `pageNum`, frontend sends `page`

**Prevention**: Use exact strings, test integration early

---

### Pattern 2: Review → Fix → Re-review Cycle

**Flow**: Developer → code-reviewer → Developer (fix) → code-reviewer (verify)

**Critical Success Factor**: Clear, actionable feedback

**code-reviewer provides**:
- File path and line numbers: `EmployeeService.java:45`
- Issue severity: Critical, Major, Minor
- Specific violation: "Uses @Autowired field injection"
- Correct pattern: "Use @RequiredArgsConstructor with private final"
- Why it matters: "Prevents field injection anti-pattern"

**Developer fixes and confirms**:
- "Fixed @Autowired in EmployeeService.java:45, EmployeeController.java:23"
- "All ArchitectureTest tests now passing"

**code-reviewer verifies**:
- Issue resolved correctly
- No new issues introduced
- Tests still passing

---

### Pattern 3: Parallel Investigation → Convergence

**Scenario**: Performance issue, unclear root cause

**Phase 1: Parallel Investigation (2-4 hours)**
- java-architect: Profile backend, check N+1 queries, analyze service logic
- postgres-pro: Check slow queries, index usage, connection pool
- vue-expert: Check frontend performance, network waterfalls, rendering

**Phase 2: Sync and Share Findings (30 min)**
- Each agent shares findings
- Identify bottleneck(s)
- Agree on optimization priority

**Phase 3: Coordinated Fix**
- Implement fixes in priority order
- Test end-to-end performance
- Validate improvement

**Critical Success Factor**: Regular sync points (don't wait until end)

---

## Anti-Patterns: What NOT to Do

### ❌ Anti-Pattern 1: Using Wrong Agent for Task

**Mistake**: Asking vue-expert to implement backend REST API

**Impact**: Agent outside expertise, low-quality implementation, wasted time

**Correct**: Use java-architect for backend, vue-expert for frontend integration

---

### ❌ Anti-Pattern 2: Skipping business-analyst for "Quick" Features

**Mistake**: "Just add a delete button" → Directly to java-architect

**Impact**: Missed requirements (confirmation dialog? soft delete? audit log?), rework later

**Correct**: Quick BA session (30 min) → Clarify complete requirements → Implement once

---

### ❌ Anti-Pattern 3: Merging Without code-reviewer

**Mistake**: "Tests pass, looks good" → Merge

**Impact**: Quality issues in production, security vulnerabilities, performance problems

**Correct**: Always gate merges with code-reviewer, especially for:
- Security-sensitive code (authentication, authorization)
- Performance-critical paths
- Architecture changes
- Public APIs

---

### ❌ Anti-Pattern 4: Frontend Before Backend Ready

**Mistake**: vue-expert starts before java-architect finishes backend

**Impact**: API contract changes, frontend rework, integration issues

**Correct**: Backend complete + documented → Handoff package → Frontend starts

**Exception**: Can start with mocked API if contract is stable

---

### ❌ Anti-Pattern 5: Deploying Without Resilience Testing

**Mistake**: devops-engineer deploys → Skip chaos-engineer

**Impact**: Production failures reveal untested failure modes

**Correct**: chaos-engineer validates resilience before production deployment

---

### ❌ Anti-Pattern 6: No Documentation for Complex Features

**Mistake**: Feature complete → Skip documentation-engineer

**Impact**: Knowledge loss, maintenance difficulty, poor onboarding

**Correct**: documentation-engineer creates architecture guides, API docs for complex features

---

### ❌ Anti-Pattern 7: Ignoring architect-reviewer for Major Changes

**Mistake**: "Let's just refactor to microservices" → Skip architect-reviewer

**Impact**: Poor architectural decisions, technical debt, scalability issues

**Correct**: architect-reviewer designs solution → Team implements → architect-reviewer validates

---

## Quick Reference: When to Use Each Agent

### Development Phase

| Task | Primary Agent | Supporting Agents |
|------|--------------|-------------------|
| Gather requirements | business-analyst | - |
| Design API contract | business-analyst | java-architect, vue-expert |
| Implement backend | java-architect | postgres-pro (if complex queries) |
| Implement frontend | vue-expert | - |
| Optimize database | postgres-pro | java-architect (query review) |
| Write tests | [Implementation agent] | chaos-engineer (resilience tests) |

### Review Phase

| Task | Primary Agent | Supporting Agents |
|------|--------------|-------------------|
| Architecture review | architect-reviewer | - |
| Code quality review | code-reviewer | - |
| Security review | code-reviewer | java-architect, vue-expert (context) |
| Performance review | code-reviewer | java-architect, postgres-pro |
| Pre-merge quality gate | code-reviewer | architect-reviewer (if architecture changes) |

### Deployment Phase

| Task | Primary Agent | Supporting Agents |
|------|--------------|-------------------|
| Setup CI/CD | devops-engineer | - |
| Deploy to staging | devops-engineer | - |
| Deploy to production | devops-engineer | chaos-engineer (validate resilience) |
| Monitor production | devops-engineer | - |
| Incident response | devops-engineer | [Specialist based on root cause] |

### Analysis Phase

| Task | Primary Agent | Supporting Agents |
|------|--------------|-------------------|
| Business analysis | business-analyst | - |
| Performance analysis | java-architect, postgres-pro | - |
| Failure analysis | chaos-engineer | devops-engineer (logs, metrics) |
| Architecture planning | architect-reviewer | All agents (gather requirements) |
| Impact analysis | business-analyst | Affected agents |

### Documentation Phase

| Task | Primary Agent | Supporting Agents |
|------|--------------|-------------------|
| API documentation | documentation-engineer | java-architect (technical details) |
| Architecture guide | documentation-engineer | architect-reviewer (design rationale) |
| User guide | documentation-engineer | business-analyst (user perspective) |
| Runbook | documentation-engineer | devops-engineer (operational details) |
| Code comments | [Implementation agent] | - |

---

## Summary

This capability matrix provides:

1. **Visual Comparison**: Instant understanding of agent capabilities across 40+ tasks
2. **Skill Ratings**: Clear expertise levels across 7 dimensions
3. **Duration Guidance**: Typical task durations to set expectations
4. **Collaboration Patterns**: 6 proven multi-agent workflows
5. **Decision Tree**: Step-by-step agent selection process
6. **Anti-Patterns**: What NOT to do (7 common mistakes)
7. **Quick Reference**: At-a-glance agent selection guide

**When to Use This Matrix**:
- ✅ Selecting the right agent for a task
- ✅ Understanding agent collaboration needs
- ✅ Planning multi-agent workflows
- ✅ Estimating task complexity
- ✅ Troubleshooting agent selection issues

**Related Documentation**:
- [Unified Decision Center](../../.agent/rules/00-INDEX.md) - Rule routing, skill selection, agent orchestration
- [Orchestration Playbook](../shared/orchestration/orchestration-playbook.md) - Complete workflows, dependencies, collaboration protocols, handoffs

---

**Last Updated**: 2026-01-21 | **Version**: 1.0.0
