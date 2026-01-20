# Multi-Agent Workflow Patterns

**Purpose:** Define common multi-agent workflow patterns for efficient collaboration.

## Pattern 1: New Feature Implementation (Sequential)

**When:** User requests a new business feature

**Agents:** business-analyst → java-architect → devops-engineer → chaos-engineer

**Timeline:** 1-5 days depending on complexity

### Phase 1: Requirements (business-analyst)

**Duration:** 0.5-1 day

**Activities:**
- Gather stakeholder requirements
- Define user stories with acceptance criteria
- Create process flows
- Define API contracts
- Document business rules
- Calculate ROI and prioritize

**Deliverables:**
- Requirements document
- User stories
- API contract specifications
- Success metrics

**Handoff to java-architect when:**
- Requirements approved by stakeholders
- Technical feasibility validated
- Acceptance criteria clear

### Phase 2: Implementation (java-architect)

**Duration:** 1-3 days

**Activities:**
- Design domain objects (Entity, Form, VO)
- Implement Dao layer
- Implement Manager layer (if transactions/caching needed)
- Implement Service layer (business logic)
- Implement Controller layer (API endpoints)
- Write unit and integration tests
- Run ArchitectureTest validation

**Deliverables:**
- Feature code following SmartAdmin patterns
- Unit tests (>85% coverage)
- Integration tests
- API documentation

**Handoff to devops-engineer when:**
- All tests passing
- ArchitectureTest passing
- Code reviewed
- Documentation complete

### Phase 3: Deployment (devops-engineer)

**Duration:** 0.5-1 day

**Activities:**
- Update CI/CD pipeline if needed
- Configure environment-specific settings
- Deploy to staging environment
- Run smoke tests
- Monitor deployment
- Deploy to production (manual approval)

**Deliverables:**
- Staging deployment complete
- Production deployment ready
- Monitoring dashboards updated
- Runbook updated

**Handoff to chaos-engineer when:**
- Feature deployed to staging
- Monitoring in place
- Rollback tested

### Phase 4: Resilience Validation (chaos-engineer)

**Duration:** 0.5-1 day

**Activities:**
- Design chaos experiments
- Test failure scenarios
- Validate graceful degradation
- Document findings
- Recommend improvements

**Deliverables:**
- Chaos experiment results
- Resilience report
- Improvement recommendations

**Complete when:**
- Critical paths validated
- Known failure modes tested
- Improvements documented

---

## Pattern 2: Performance Optimization (Parallel)

**When:** User reports slow performance

**Agents:** java-architect + postgres-pro (parallel) → devops-engineer

**Timeline:** 1-2 days

### Phase 1: Parallel Investigation

**Duration:** 0.5-1 day

**java-architect Activities:**
- Review code for N+1 queries
- Check caching strategy
- Profile application performance
- Identify algorithmic bottlenecks
- Review object creation patterns

**postgres-pro Activities:**
- Analyze slow queries log
- Review query execution plans
- Check index usage
- Assess database resource utilization
- Identify missing indexes

**Sync Point:**
- Share findings
- Identify root cause(s)
- Agree on optimization strategy

### Phase 2: Implementation (Coordinated)

**Duration:** 0.5-1 day

**java-architect:**
- Implement caching in Manager layer
- Fix N+1 queries (batch fetching or join queries)
- Optimize algorithms
- Add performance monitoring

**postgres-pro:**
- Create indexes
- Rewrite complex queries
- Update statistics
- Tune database parameters

**Deliverables:**
- Optimized code
- New/modified indexes
- Performance test results
- Before/after metrics

### Phase 3: Deployment & Validation (devops-engineer)

**Duration:** 0.5 day

**Activities:**
- Deploy optimizations to staging
- Monitor performance improvements
- Validate target metrics achieved
- Deploy to production
- Continue monitoring

**Complete when:**
- Performance targets met
- No regression in other areas
- Monitoring confirms improvement

---

## Pattern 3: Production Incident Response (Hub-and-Spoke)

**When:** Production issue detected

**Hub:** devops-engineer (triage)
**Spokes:** java-architect, postgres-pro, chaos-engineer (as needed)

**Timeline:** Minutes to hours

### Phase 1: Triage (devops-engineer - 5-15 min)

**Activities:**
- Check infrastructure health
- Review monitoring dashboards
- Analyze logs
- Assess customer impact
- Determine severity

**Decision Point:**
- **Application error** → java-architect
- **Database issue** → postgres-pro
- **Infrastructure problem** → devops-engineer (continues)
- **Unknown** → all hands investigation

### Phase 2: Diagnosis (Specialist - 15-60 min)

**java-architect (if application):**
- Review error logs
- Analyze stack traces
- Check recent deployments
- Identify code issue

**postgres-pro (if database):**
- Check connection pool
- Review slow queries
- Check replication lag
- Identify database bottleneck

**devops-engineer (if infrastructure):**
- Check resource utilization
- Review network issues
- Check auto-scaling
- Identify infrastructure issue

### Phase 3: Resolution (15-60 min)

**Immediate Actions:**
- Rollback deployment if recent change
- Scale resources if capacity issue
- Kill problematic queries if database
- Apply hotfix if code issue

**Communication:**
- Update status to team
- Notify stakeholders
- Document timeline

### Phase 4: Post-Mortem (chaos-engineer - 1-2 hours)

**Activities:**
- Conduct blameless post-mortem
- Document timeline and decisions
- Identify prevention measures
- Design chaos experiment to test fix

**Deliverables:**
- Incident report
- Action items with owners
- Chaos experiment plan

---

## Pattern 4: Database Migration (Sequential with Checkpoints)

**When:** Schema changes or data migration needed

**Agents:** postgres-pro → java-architect → devops-engineer

**Timeline:** 0.5-2 days

### Phase 1: Migration Design (postgres-pro)

**Activities:**
- Design schema changes
- Write migration scripts (up/down)
- Plan data transformation
- Estimate downtime
- Create rollback procedure

**Checkpoints:**
- Review with java-architect (code impact)
- Review with devops-engineer (deployment strategy)

### Phase 2: Code Updates (java-architect)

**Activities:**
- Update Entity classes
- Modify Dao queries if needed
- Update Service logic for new schema
- Add/update tests
- Validate backward compatibility if needed

**Checkpoint:**
- Review with postgres-pro (query correctness)

### Phase 3: Deployment (devops-engineer)

**Activities:**
- Schedule maintenance window
- Deploy to staging and test migration
- Backup production database
- Execute migration in production
- Validate data integrity
- Monitor application

**Complete when:**
- Migration successful
- Application functioning correctly
- Rollback procedure validated

---

## Pattern 5: Architecture Review (Collaborative)

**When:** Reviewing significant architectural change

**Agents:** All technical agents (java-architect leads)

**Timeline:** 2-4 hours

### Preparation (java-architect)

- Document proposed architecture
- Create diagrams
- List trade-offs
- Identify risks

### Review Session (All)

**Each agent reviews from their perspective:**

**java-architect:**
- Code maintainability
- Design patterns alignment
- SmartAdmin compliance

**postgres-pro:**
- Database implications
- Query patterns
- Data model impact

**devops-engineer:**
- Deployment complexity
- Infrastructure requirements
- Monitoring needs

**chaos-engineer:**
- Failure modes
- Resilience implications
- Testing strategy

### Output

- Consensus on approach
- List of concerns addressed
- Action items for each agent
- Documented decision and rationale

---

## Pattern 6: Technical Debt Reduction (Iterative)

**When:** Addressing accumulated technical debt

**Agents:** java-architect (lead) + devops-engineer + chaos-engineer

**Timeline:** Ongoing, 1-2 days per iteration

### Iteration Cycle

1. **Identify** (java-architect): List debt items, prioritize by impact
2. **Plan** (java-architect): Break into small, safe changes
3. **Implement** (java-architect): Refactor with comprehensive tests
4. **Deploy** (devops-engineer): Safe, incremental deployments
5. **Validate** (chaos-engineer): Ensure no regression in resilience
6. **Repeat**: Next debt item

### Key Principles

- Small, incremental changes
- Comprehensive test coverage
- No new features during refactoring
- Monitor for regressions
- Document improvements

---

## Pattern Selection Guide

### Use Pattern 1 (Sequential) When:
- Clear requirements needed upfront
- Phases have hard dependencies
- New feature development
- User-facing changes

### Use Pattern 2 (Parallel) When:
- Multiple independent investigations
- Time is critical
- Performance optimization
- Root cause unclear

### Use Pattern 3 (Hub-and-Spoke) When:
- Urgent response needed
- Triage required first
- Production incidents
- Unknown root cause

### Use Pattern 4 (Sequential with Checkpoints) When:
- High-risk changes
- Multiple dependencies
- Database migrations
- Breaking changes

### Use Pattern 5 (Collaborative) When:
- Significant architectural decision
- Multiple perspectives needed
- Trade-offs to evaluate
- Need team alignment

### Use Pattern 6 (Iterative) When:
- Technical debt reduction
- Continuous improvement
- Risk must be minimized
- No hard deadlines

## Workflow Best Practices

### 1. Clear Entry and Exit Criteria

**Each phase should have:**
- Clear trigger to start
- Defined deliverables
- Acceptance criteria
- Handoff protocol

### 2. Communication Cadence

**Status updates:**
- Start: Notify dependent agents
- Progress: Daily standups or async updates
- Blockers: Immediate escalation
- Complete: Handoff notification

### 3. Documentation

**Always document:**
- Decisions and rationale
- Trade-offs considered
- Risks identified
- Lessons learned

### 4. Parallel When Possible

**Maximize efficiency:**
- Identify independent workstreams
- Coordinate sync points
- Avoid blocking dependencies

### 5. Quality Gates

**Don't skip:**
- ArchitectureTest validation
- Test coverage checks
- Security scanning
- Performance validation
- Monitoring verification

## Anti-Patterns to Avoid

❌ **Skipping business-analyst** for "quick features"
- Result: Unclear requirements, rework

❌ **Deploying without devops-engineer** review
- Result: Configuration issues, downtime

❌ **Ignoring chaos-engineer** for critical paths
- Result: Production incidents

❌ **Working in complete isolation**
- Result: Misalignment, integration issues

❌ **Skipping handoff protocols**
- Result: Missing information, delays

## Summary

**Choose the right pattern for the task:**
- New feature → Sequential (Pattern 1)
- Performance → Parallel (Pattern 2)
- Incident → Hub-and-Spoke (Pattern 3)
- Migration → Sequential with Checkpoints (Pattern 4)
- Architecture → Collaborative (Pattern 5)
- Tech debt → Iterative (Pattern 6)

**Key Success Factors:**
1. Clear entry/exit criteria
2. Comprehensive handoffs
3. Regular communication
4. Parallel when possible
5. Don't skip quality gates

**Remember:** Workflows should be efficient but not rushed. Quality and collaboration lead to better outcomes than speed alone.
