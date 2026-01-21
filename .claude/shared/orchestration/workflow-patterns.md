# Multi-Agent Workflow Patterns

**Purpose:** Define common multi-agent workflow patterns for efficient collaboration.

## Pattern 1: New Full-Stack Feature Implementation (Sequential)

**When:** User requests a new business feature requiring both backend and frontend

**Agents:** business-analyst → java-architect → vue-expert → devops-engineer → chaos-engineer

**Timeline:** 2-7 days depending on complexity

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

### Phase 2: Backend Implementation (java-architect)

**Duration:** 1-3 days

**Activities:**
- Design domain objects (Entity, Form, VO)
- Implement Dao layer
- Implement Manager layer (if transactions/caching needed)
- Implement Service layer (business logic)
- Implement Controller layer (API endpoints)
- Write unit and integration tests
- Run ArchitectureTest validation
- Generate Swagger documentation

**Deliverables:**
- Backend API code following SmartAdmin patterns
- Unit tests (>85% coverage)
- Integration tests
- Swagger/OpenAPI documentation
- Sample test data

**Handoff to vue-expert when:**
- All tests passing
- ArchitectureTest passing
- API accessible in dev environment
- Swagger documentation complete
- Sample request/response data available

### Phase 3: Frontend Implementation (vue-expert)

**Duration:** 1-3 days

**Activities:**
- Define TypeScript interfaces matching backend DTOs
- Create API module (employee-api.ts)
- Implement list page component (employee-list.vue)
- Implement form modal component (employee-form-modal.vue)
- Integrate with backend API (ResponseModel handling)
- Add permission controls (v-privilege directives)
- Write component tests (>80% coverage)
- Run type checking and build validation

**Deliverables:**
- Frontend pages following SmartAdmin patterns
- Component tests (>80% coverage)
- TypeScript type definitions
- Build artifacts (dist/ folder)
- Frontend working in dev environment

**Handoff to devops-engineer when:**
- All tests passing (unit + integration)
- Type checking passing
- Frontend successfully integrated with backend API
- Build succeeds without errors
- Permissions working correctly

### Phase 4: Deployment (devops-engineer)

**Duration:** 0.5-1 day

**Activities:**
- Update CI/CD pipeline if needed (backend + frontend)
- Configure environment-specific settings (backend configs, frontend .env)
- Deploy backend to staging
- Deploy frontend to staging (static files to CDN/nginx)
- Run smoke tests (backend API + frontend UI)
- Monitor deployment
- Deploy to production (manual approval)

**Deliverables:**
- Backend and frontend deployed to staging
- Production deployment ready
- Monitoring dashboards updated
- Runbook updated

**Handoff to chaos-engineer when:**
- Full-stack feature deployed to staging
- Monitoring in place (backend + frontend)
- Rollback tested (both layers)

### Phase 5: Resilience Validation (chaos-engineer)

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

## Pattern 7: API Integration & Debugging (Parallel Convergence)

**When:** Frontend and backend integration issues (500 errors, data mismatch, contract issues)

**Agents:** java-architect + vue-expert (parallel investigation) → converge → devops-engineer

**Timeline:** 0.5-1 day

### Phase 1: Parallel Investigation (java-architect + vue-expert)

**Duration:** 15-30 minutes

**java-architect Activities:**
- Review backend logs for errors
- Check request validation logic
- Verify DTO structure matches documentation
- Test API endpoint with curl/Postman
- Check @SaCheckPermission annotations

**vue-expert Activities:**
- Review frontend request payload in Network tab
- Check TypeScript interface matches backend DTO
- Verify API request method (postRequest/getRequest)
- Check response.success handling
- Verify v-privilege permission strings

**Sync Point:**
- Share findings (logs, screenshots, payloads)
- Identify root cause (contract mismatch, validation error, permission issue)
- Agree on fix location (backend, frontend, or both)

### Phase 2: Aligned Fix (Coordinated)

**Duration:** 30-60 minutes

**If Contract Mismatch:**
- **java-architect:** Update DTO field names/types OR
- **vue-expert:** Update TypeScript interface OR
- **Both:** Align on new contract structure

**If Validation Error:**
- **java-architect:** Fix backend validation logic OR
- **vue-expert:** Fix frontend form validation

**If Permission Error:**
- **java-architect:** Update @SaCheckPermission OR
- **vue-expert:** Update v-privilege string OR
- **devops-engineer:** Update role configuration

**Deliverables:**
- Aligned API contract (Java DTO ↔ TypeScript interface)
- Fixed validation/permission issues
- Integration tests passing
- Documentation updated (if contract changed)

### Phase 3: Verification & Deployment (devops-engineer)

**Duration:** 15-30 minutes

**Activities:**
- Deploy fixes to dev environment
- Run integration tests (frontend + backend)
- Verify API contract alignment
- Deploy to staging if tests pass

**Complete when:**
- Frontend successfully calls backend API
- No 500 errors
- Data flows correctly
- Permissions working

---

## Pattern 8: Frontend Performance Optimization (Sequential)

**When:** Frontend UI is slow (laggy table, slow rendering, excessive re-renders)

**Agents:** vue-expert → java-architect (if API slow) → postgres-pro (if query slow) → devops-engineer

**Timeline:** 0.5-2 days

### Phase 1: Frontend Profiling (vue-expert)

**Duration:** 1-2 hours

**Activities:**
- Profile component with Vue DevTools
- Identify unnecessary re-renders
- Check for reactive data issues (large arrays, deep nesting)
- Review computed property dependencies
- Check v-for key usage
- Profile Vite build size

**Findings:**
- If frontend issue → Phase 2A
- If API slow → Phase 2B
- If both → Phase 2A then 2B

### Phase 2A: Frontend Optimization (vue-expert)

**Duration:** 2-4 hours

**Activities:**
- Implement shallow reactivity (shallowRef/shallowReactive) for large datasets
- Add virtual scrolling for large tables (if >1000 rows)
- Optimize computed dependencies
- Debounce user input
- Memoize expensive computations
- Lazy load components
- Optimize bundle size (code splitting)

**Deliverables:**
- Optimized Vue components
- Performance test results (before/after)
- Bundle size reduction metrics

### Phase 2B: Backend/Database Optimization (java-architect + postgres-pro)

**Duration:** 2-4 hours

**If API response time >500ms:**
- **java-architect:** Review service logic, add caching (Manager layer)
- **postgres-pro:** Optimize queries, add indexes

**Follow Pattern 2 (Performance Optimization - Parallel)**

### Phase 3: Deployment & Validation (devops-engineer)

**Duration:** 0.5-1 hour

**Activities:**
- Deploy optimizations to staging
- Run performance tests
- Monitor metrics (page load time, API response time, render time)
- Deploy to production
- Continue monitoring

**Complete when:**
- Page load time <2 seconds
- Table rendering smooth (<100ms per interaction)
- No user-reported lag
- Metrics show improvement

---

## Pattern 9: Architecture Review & Refactoring (Collaborative)

**When:** Periodic architecture assessment, pre-refactoring, technical debt reduction, scalability evaluation

**Agents:** architect-reviewer → (java-architect + vue-expert + postgres-pro + devops-engineer parallel) → business-analyst → architect-reviewer

**Timeline:** 1-3 days

### Phase 1: Architecture Assessment (architect-reviewer - 4-8 hours)

**Activities:**
- Comprehensive architecture review
- Validate layered architecture compliance (Controller → Service → Manager → Dao)
- Check layer boundary violations
- Assess module structure and cohesion
- Identify technical debt hotspots
- Evaluate scalability and maintainability
- Review architectural patterns adherence

**Deliverables:**
- Architecture review report
- Violations list with severity
- Technical debt assessment
- Scalability concerns
- Refactoring priorities

**Handoff when:**
- Architecture violations documented
- Technical debt quantified
- Refactoring priorities identified

### Phase 2: Impact Analysis (Parallel - 4-8 hours)

**java-architect:**
- Analyze code refactoring effort
- Estimate complexity and risk
- Identify affected modules
- Plan implementation approach

**vue-expert:**
- Assess frontend architectural impact
- Identify component restructuring needs
- Estimate frontend refactoring effort

**postgres-pro:**
- Review database schema implications
- Assess query pattern changes needed
- Estimate database refactoring effort

**devops-engineer:**
- Evaluate infrastructure impact
- Assess deployment complexity
- Estimate infrastructure changes needed

**Deliverables (from each):**
- Impact assessment report
- Effort estimates
- Risk assessment
- Dependencies identified

### Phase 3: Business Impact & ROI (business-analyst - 2-4 hours)

**Activities:**
- Calculate refactoring costs
- Estimate business value of improvements
- Assess risk of NOT refactoring (technical debt interest)
- Prioritize improvements by ROI
- Create phased refactoring roadmap

**Deliverables:**
- ROI analysis
- Cost-benefit summary
- Prioritized refactoring backlog
- Phased implementation plan

### Phase 4: Final Refactoring Roadmap (architect-reviewer - 2-4 hours)

**Activities:**
- Consolidate all findings
- Create comprehensive refactoring roadmap
- Define success metrics
- Document architectural principles to follow
- Identify quick wins vs long-term improvements

**Deliverables:**
- Comprehensive refactoring roadmap
- Phased implementation plan (with business priorities)
- Architectural principles document
- Success metrics and KPIs

### Phase 5: Implementation (Per Phase)

**Execute with code-reviewer validation:**
- java-architect implements backend refactoring
- vue-expert implements frontend refactoring
- code-reviewer validates each phase (quality gate)
- devops-engineer deploys incremental changes
- chaos-engineer tests resilience after critical changes

**Complete when:**
- All critical architectural violations resolved
- High-priority technical debt addressed
- ArchitectureTest passing
- Scalability concerns mitigated
- Team aligned on new patterns

---

## Pattern 10: Pre-Merge Quality Gate (Hub-and-Spoke)

**When:** Before merging feature branches, critical fixes, major refactoring

**Hub:** code-reviewer (orchestrates all specialist reviews)
**Spokes:** architect-reviewer, java-architect, vue-expert, postgres-pro (as needed)

**Timeline:** 30 minutes - 2 hours

### Phase 1: Initial Scan (code-reviewer - 10-20 min)

**Activities:**
- Review pull request changes (git diff)
- Identify change scope (backend, frontend, database, architecture)
- Check for obvious issues:
  - Security vulnerabilities (SQL injection, XSS, auth bypass)
  - Critical bugs (null pointer, logic errors)
  - Performance red flags (N+1 queries, missing indexes)
  - Code quality issues (complexity, duplication)

**Decision Point - Dispatch to specialists:**
- **Backend changes** → java-architect
- **Frontend changes** → vue-expert
- **Database changes** → postgres-pro
- **Architectural impact** → architect-reviewer
- **None needed** → Proceed to decision

### Phase 2: Specialist Reviews (Parallel - 15-45 min)

**java-architect (if backend):**
- Validate SmartAdmin patterns (layered architecture)
- Check ResponseDTO usage
- Verify dependency injection (constructor injection only)
- Review transaction management (@Transactional in Manager only)
- Check naming conventions
- Validate MyBatis Plus patterns (LambdaQueryWrapper)

**vue-expert (if frontend):**
- Validate Vue 3 Composition API patterns
- Check Ant Design Vue component usage
- Verify API integration (ResponseModel handling)
- Check permission controls (v-privilege vs @SaCheckPermission alignment)
- Review TypeScript interfaces (alignment with backend DTOs)
- Validate frontend performance patterns

**postgres-pro (if database):**
- Review query patterns and performance
- Check index usage
- Validate migration scripts (if any)
- Assess query optimization opportunities

**architect-reviewer (if architectural):**
- Validate layer boundaries (no Controller → Dao violations)
- Check module dependencies
- Assess architectural debt introduced
- Verify adherence to architectural principles

**Each delivers:**
- Domain-specific findings (Critical/Major/Minor/Suggestions)
- File paths and line numbers
- Specific recommendations
- Pass/Fail for their domain

### Phase 3: Consolidation (code-reviewer - 10-15 min)

**Activities:**
- Aggregate all specialist findings
- Categorize by severity:
  - **Critical** (must fix, blocks merge)
  - **Major** (should fix, may block merge)
  - **Minor** (nice to fix, doesn't block)
  - **Suggestions** (improvements)
- Eliminate duplicates
- Create consolidated review report
- Make pass/fail decision

**Pass Criteria:**
- Zero critical issues
- Zero major security issues
- ArchitectureTest passing (if backend changes)
- Test coverage >85% for backend, >80% for frontend
- No architectural violations

**Fail Criteria:**
- Any critical issue present
- Security vulnerabilities
- ArchitectureTest failures
- Test coverage below threshold

### Phase 4: Fix Issues (java-architect or vue-expert - 30-60 min)

**If FAIL:**
- Developer (java-architect/vue-expert) fixes critical/major issues
- Updates tests if needed
- Commits fixes to same PR

**Communication:**
- code-reviewer provides clear, actionable feedback
- Developers acknowledge and commit to timeline
- code-reviewer tracks fix progress

### Phase 5: Re-Validation (code-reviewer - 10-15 min)

**Activities:**
- Review fixes
- Verify critical/major issues resolved
- Check no new issues introduced
- Confirm tests passing
- Make final pass/fail decision

**If PASS:**
- Approve merge ✅
- Notify devops-engineer for deployment

**If FAIL:**
- Document remaining issues
- Iterate back to Phase 4

**Complete when:**
- All critical and major issues resolved
- Tests passing
- No architectural violations
- Ready for merge and deployment

---

## Pattern Selection Guide

### Use Pattern 1 (Full-Stack Sequential) When:
- New feature requires both backend and frontend
- Clear requirements needed upfront
- Phases have hard dependencies (backend API → frontend UI)
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

### Use Pattern 7 (API Integration) When:
- Frontend getting 500 errors from backend
- Data structure mismatch between frontend and backend
- Permission issues (frontend vs backend mismatch)
- Integration testing failures
- Contract alignment needed

### Use Pattern 8 (Frontend Performance) When:
- UI is laggy or slow to render
- Table with many rows performing poorly
- User reporting slow page load
- Excessive re-renders detected
- Frontend profiling shows bottlenecks

### Use Pattern 9 (Architecture Review & Refactoring) When:
- Module restructuring needed
- Pre-refactoring evaluation required
- Technical debt assessment needed
- Scalability concerns identified
- Architecture audit requested
- Post-major-release architecture review
- Planning significant refactoring effort

### Use Pattern 10 (Pre-Merge Quality Gate) When:
- Feature branch ready for merge
- Critical fix before deployment
- Major refactoring complete
- Pull request submitted
- Code review required before merge
- Quality validation needed
- Pre-deployment checkpoint

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

❌ **Starting frontend before backend API ready**
- Result: Mocked APIs, rework when real API differs

❌ **Not aligning API contracts between java-architect and vue-expert**
- Result: 500 errors, data mismatch, integration failures

❌ **Deploying frontend without testing API integration**
- Result: Production errors, broken user flows

❌ **Skipping architecture review before major refactoring**
- Result: Architectural debt, incorrect patterns, wasted effort

❌ **Merging code without quality gate review**
- Result: Bugs in production, technical debt accumulation, security vulnerabilities

## Summary

**Choose the right pattern for the task:**
- New full-stack feature → Full-Stack Sequential (Pattern 1)
- Backend performance → Parallel (Pattern 2)
- Production incident → Hub-and-Spoke (Pattern 3)
- Database migration → Sequential with Checkpoints (Pattern 4)
- Architecture review → Collaborative (Pattern 5)
- Technical debt → Iterative (Pattern 6)
- API integration issue → API Integration (Pattern 7)
- Frontend performance → Frontend Performance (Pattern 8)
- Architecture & refactoring → Architecture Review & Refactoring (Pattern 9)
- Pre-merge code review → Pre-Merge Quality Gate (Pattern 10)

**Key Success Factors:**
1. Clear entry/exit criteria
2. Comprehensive handoffs
3. Regular communication
4. Parallel when possible
5. Don't skip quality gates

**Remember:** Workflows should be efficient but not rushed. Quality and collaboration lead to better outcomes than speed alone.
