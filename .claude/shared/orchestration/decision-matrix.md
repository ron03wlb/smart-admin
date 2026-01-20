# Agent Selection Decision Matrix

**Purpose:** Help users and the main assistant quickly identify which agent to use for any given task.

## Quick Decision Flow

```
[User Request]
    ↓
Is it about Java code implementation? ──YES──→ java-architect
    ↓ NO
Is it about requirements/process/stakeholders? ──YES──→ business-analyst
    ↓ NO
Is it about deployment/CI-CD/infrastructure? ──YES──→ devops-engineer
    ↓ NO
Is it PostgreSQL database specific? ──YES──→ postgres-pro
    ↓ NO
Is it about resilience/chaos testing? ──YES──→ chaos-engineer
    ↓ NO
Use general-purpose or ask user for clarification
```

## Keyword-Based Agent Mapping

| Keywords in Request | Agent | Confidence | Example Requests |
|---------------------|-------|------------|------------------|
| **Java Development** ||||
| Spring Boot, @Transactional, @Service, Controller, REST API, layering, MyBatis Plus, entity, service layer | **java-architect** | High | "implement employee API", "add REST endpoint", "optimize JPA queries", "fix N+1 problem", "review service code" |
| **Business Analysis** ||||
| requirements, stakeholders, ROI, business process, user story, workflow, acceptance criteria, KPI, metrics | **business-analyst** | High | "gather requirements", "analyze process", "improve workflow", "calculate ROI", "define success metrics" |
| **DevOps & Deployment** ||||
| deploy, CI/CD, Docker, Kubernetes, pipeline, container, infrastructure, monitoring, Prometheus, Grafana | **devops-engineer** | High | "setup deployment", "configure monitoring", "create pipeline", "containerize app", "deploy to production" |
| **Database** ||||
| PostgreSQL, query optimization, index, replication, pg_stat, slow query, database performance | **postgres-pro** | High | "optimize database", "setup replication", "analyze query performance", "create indexes", "backup strategy" |
| **Resilience** ||||
| resilience, chaos, failure injection, game day, circuit breaker, fallback, antifragility, disaster recovery | **chaos-engineer** | High | "test failover", "improve resilience", "design chaos experiment", "validate recovery", "test failure scenarios" |

## Context-Based Decision Logic

### Scenario 1: New Feature Implementation

**Request:** "Add employee performance review feature"

**Agent Sequence:**
1. **business-analyst** (first) - Gather requirements, define user stories, create process flows
2. **java-architect** (second) - Implement the feature following SmartAdmin patterns
3. **postgres-pro** (if complex queries) - Optimize database performance
4. **devops-engineer** (third) - Deploy to staging/production
5. **chaos-engineer** (fourth) - Validate resilience of critical path

### Scenario 2: Performance Problem

**Request:** "Employee search is slow"

**Parallel Investigation:**
- **java-architect** (lead) - Review code, check N+1 queries, caching strategy
- **postgres-pro** (parallel) - Analyze query execution plans, check indexes
- Converge on integrated solution

**Then:**
- **devops-engineer** - Deploy optimization, monitor improvements

### Scenario 3: Production Issue

**Request:** "Getting 503 errors in production"

**Sequential Response:**
1. **devops-engineer** (triage) - Check infrastructure, logs, monitoring
2. **Appropriate specialist** - Based on root cause:
   - Application error → **java-architect**
   - Database issue → **postgres-pro**
   - Infrastructure → **devops-engineer** (continues)
3. **chaos-engineer** (prevention) - Create test to prevent recurrence

### Scenario 4: Process Improvement

**Request:** "Employee onboarding takes too long"

**Agent Sequence:**
1. **business-analyst** (lead) - Analyze current process, identify bottlenecks
2. **java-architect** (if automation needed) - Implement workflow automation
3. **devops-engineer** (if infrastructure change) - Update deployment process
4. **chaos-engineer** (if critical) - Test resilience of new process

## Ambiguous Requests - How to Clarify

### "How do I..."

| User Says | Likely Means | Agent |
|-----------|--------------|-------|
| "How do I add a feature?" | Implementation guidance | **java-architect** |
| "How do I improve this process?" | Process optimization | **business-analyst** |
| "How do I deploy this?" | Deployment setup | **devops-engineer** |
| "How do I optimize queries?" | Database performance | **postgres-pro** |
| "How do I test resilience?" | Chaos testing | **chaos-engineer** |

### "I need help with..."

| User Says | Likely Means | Agent |
|-----------|--------------|-------|
| "...my code" | Code implementation/review | **java-architect** |
| "...requirements" | Requirements analysis | **business-analyst** |
| "...deployment" | CI/CD and infrastructure | **devops-engineer** |
| "...database" | Database operations | **postgres-pro** |
| "...testing failures" | Resilience testing | **chaos-engineer** |

### "Can you review..."

| User Says | What They Want Reviewed | Agent |
|-----------|-------------------------|-------|
| "...this Java class" | Code quality, patterns | **java-architect** |
| "...this business process" | Process efficiency | **business-analyst** |
| "...this pipeline" | CI/CD configuration | **devops-engineer** |
| "...this query" | Query performance | **postgres-pro** |
| "...our resilience" | System robustness | **chaos-engineer** |

## Multi-Agent Coordination Scenarios

### When to Use Multiple Agents Sequentially

**Indicator:** Task spans multiple domains
**Pattern:** Complete one domain before moving to next
**Example:** New feature (BA → Java → Deploy → Test)

### When to Use Multiple Agents in Parallel

**Indicator:** Independent investigations needed
**Pattern:** Agents work simultaneously, converge on solution
**Example:** Performance issue (Java + DB investigate in parallel)

### When to Loop Back to Previous Agent

**Indicator:** New information changes requirements
**Pattern:** Agent B discovers issue, route back to Agent A
**Example:** DevOps finds deployment blocker → back to Java architect

## Special Cases

### "Just deployed and something broke"

**Priority Agent:** devops-engineer (rollback first, diagnose second)

### "Customer complaining about X"

**Priority Agent:** business-analyst (understand impact, gather context) → technical agent

### "Code review needed before merge"

**Agent:** java-architect (architecture compliance critical)

### "Production database slow"

**Urgent Path:** postgres-pro (immediate) + devops-engineer (monitoring)

### "Want to start chaos engineering"

**Agent:** chaos-engineer (establish practice, create roadmap)

## Decision Confidence Levels

| Confidence | Meaning | Action |
|------------|---------|--------|
| **High (>90%)** | Clear keywords match | Route directly to agent |
| **Medium (60-90%)** | Context suggests agent | Route with clarifying question |
| **Low (<60%)** | Ambiguous request | Ask user for clarification |

## Agent Selection Anti-Patterns

❌ **Don't:**
- Route deployment questions to java-architect (use devops-engineer)
- Route business questions to postgres-pro (use business-analyst)
- Route Java implementation to chaos-engineer (use java-architect)
- Skip business-analyst for "quick features" (requirements matter)
- Skip chaos-engineer for critical features (resilience matters)

✅ **Do:**
- Use business-analyst before java-architect for new features
- Coordinate java-architect + postgres-pro for complex queries
- Always involve devops-engineer for production changes
- Include chaos-engineer for critical business paths
- Ask clarifying questions when uncertain

## Edge Cases

### User provides code snippet

**If Java code:** java-architect (review, improve)
**If SQL:** postgres-pro (optimize query)
**If YAML/config:** devops-engineer (review infrastructure config)

### User mentions specific technology

**Spring Boot, MyBatis Plus:** java-architect
**PostgreSQL, pg_stat_statements:** postgres-pro
**Docker, Kubernetes, Terraform:** devops-engineer
**Resilience4j, circuit breaker:** chaos-engineer (or java-architect for implementation)

### User mentions stakeholders

**Almost always:** business-analyst (stakeholder management is their expertise)

## Summary

**Primary Rule:** Match keywords to agent expertise
**Secondary Rule:** Consider task lifecycle (requirements → implementation → deployment → testing)
**Tertiary Rule:** When uncertain, ask clarifying questions

**Most Common Patterns:**
1. New feature: BA → Java → DevOps → Chaos
2. Bug fix: Java → (DB if needed) → DevOps
3. Performance: Java + DB parallel → DevOps
4. Production issue: DevOps → Specialist → Chaos
5. Process improvement: BA → (Java if automation) → DevOps
