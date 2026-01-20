# Agent Dependencies & Collaboration

**Purpose:** Define how agents depend on each other and collaborate effectively.

## Dependency Graph

```
                    business-analyst
                           |
                    (requirements)
                           ↓
                    java-architect ←─────┐
                      /         \        |
                     /           \       |
          (database)              (code) |
                   /               \     |
                  ↓                 ↓    |
            postgres-pro      devops-engineer
                  |                 |
             (db health)    (infrastructure)
                  |                 |
                  └────→ chaos-engineer ←┘
                      (resilience testing)
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

## Collaboration Patterns

### Pattern 1: Sequential Handoff

**When:** Dependencies between agents

**Example:** New Feature
```
business-analyst (complete requirements)
        ↓
java-architect (implement feature)
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

### Pattern 3: Iterative Refinement

**When:** Solution requires multiple rounds

**Example:** Complex Feature
```
business-analyst → java-architect → business-analyst (clarification)
                        ↓
                java-architect (revised implementation)
                        ↓
                devops-engineer
```

**Communication:** Continuous feedback loops

### Pattern 4: Hub-and-Spoke

**When:** Central coordinator with multiple specialists

**Example:** Production Incident
```
            devops-engineer (hub - triage)
                /        |        \
               /         |         \
    java-architect  postgres-pro  chaos-engineer
     (as needed)    (as needed)   (as needed)
```

**Communication:** Central agent orchestrates

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
| **business-analyst** | None (starts chain) | java-architect, All | - |
| **java-architect** | business-analyst | devops, chaos | postgres-pro |
| **devops-engineer** | java-architect, postgres-pro | chaos, business-analyst | - |
| **postgres-pro** | java-architect | java-architect, devops | java-architect |
| **chaos-engineer** | All technical | All technical | - |

## Summary

**Key Principles:**
1. **Clear handoffs** - Know when to pass work to next agent
2. **Complete packages** - Provide all information needed
3. **Continuous communication** - Don't work in silos
4. **Respect dependencies** - Don't skip required inputs
5. **Parallel when possible** - Work simultaneously when independent

**Most Important:**
- business-analyst typically starts new features
- java-architect is central to implementation
- devops-engineer enables deployment
- postgres-pro optimizes database
- chaos-engineer validates resilience

**Collaboration beats isolation!**
