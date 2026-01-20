# Agent Base Template

**This template provides common foundations for all specialized agents. Each agent inherits these standards while adding their unique expertise.**

## Knowledge Base Integration

**MANDATORY:** All agents MUST read and follow patterns in:

1. **SmartAdmin Patterns**: `.claude/shared/knowledge/smartadmin-patterns.md`
   - Layered architecture (Controller → Service → Manager → Dao)
   - ResponseDTO pattern
   - Domain object patterns (Entity, Form, VO, QueryForm)
   - Bean conversion and pagination
   - MyBatis Plus patterns
   - Authentication and authorization
   - Dependency injection rules
   - Transaction management
   - Naming conventions
   - Anti-patterns to avoid

2. **Project Architecture**: `.claude/shared/knowledge/project-architecture.md`
   - Technology stack (Java 21, Spring Boot 3.5.4, MyBatis Plus, Sa-Token)
   - Module structure (sa-admin, sa-base, sa-common)
   - Build commands (Gradle)
   - Test commands
   - Application configuration
   - Development workflows

3. **Quality Standards**: `.claude/shared/knowledge/quality-standards.md`
   - Code quality checklist
   - Naming conventions (Alibaba guidelines)
   - Exception handling standards
   - Logging standards
   - Testing standards
   - Performance standards
   - Anti-patterns
   - Commit quality requirements

4. **Root CLAUDE.md**: Project-specific comprehensive guidelines

## Agent Coordination Protocol

Before starting work, follow this coordination sequence:

### 1. Verify Agent Selection
Check `.claude/shared/orchestration/decision-matrix.md` to confirm you're the right agent for this task.

### 2. Check Collaboration Needs
Review `.claude/shared/orchestration/agent-dependencies.md` to identify if:
- You need input from another agent first
- You should notify other agents of your work
- Multiple agents should work in parallel

### 3. Follow Workflow Patterns
Consult `.claude/shared/orchestration/workflow-patterns.md` for:
- Multi-agent coordination scenarios
- Sequential vs parallel execution guidance
- Handoff protocols between agents

## Deep Thinking & Reasoning Protocol

**CRITICAL:** Before and during all work, engage in ultrathink step-by-step analysis reasoning.

### Thinking Process Mandate

**Every agent MUST:**

1. **Think Before Acting**
   - Engage deep reasoning before every decision
   - Analyze multiple approaches systematically
   - Consider edge cases and implications
   - Question assumptions and validate reasoning

2. **Step-by-Step Analysis**
   - Break down complex problems into smaller components
   - Analyze each component individually
   - Identify dependencies and relationships
   - Build understanding incrementally

3. **Evidence-Based Reasoning**
   - Base decisions on code analysis, not assumptions
   - Verify patterns by reading actual implementations
   - Validate understanding with concrete examples
   - Test hypotheses before committing to approach

4. **Multi-Dimensional Evaluation**
   - **Technical Correctness**: Does this follow SmartAdmin patterns?
   - **Performance Impact**: What are the efficiency implications?
   - **Maintainability**: Can others understand and modify this?
   - **Security**: Are there vulnerability risks?
   - **Scalability**: How does this handle growth?
   - **Testability**: Can this be reliably tested?

5. **Continuous Reflection**
   - Pause at decision points to evaluate options
   - Reconsider approach if evidence contradicts assumptions
   - Learn from code patterns observed in the project
   - Adapt reasoning based on project-specific context

### Structured Reasoning Format

When approaching any task, explicitly structure your thinking:

```
1. Problem Understanding
   - What is the core requirement?
   - What are the constraints?
   - What are the success criteria?

2. Context Analysis
   - What existing patterns apply?
   - What related code exists?
   - What dependencies exist?
   - What are the project conventions?

3. Approach Evaluation
   - Option A: [describe] - Pros: [...] Cons: [...]
   - Option B: [describe] - Pros: [...] Cons: [...]
   - Recommended: [choice] because [reasoning]

4. Implementation Plan
   - Step 1: [action] - Why: [rationale]
   - Step 2: [action] - Why: [rationale]
   - Step 3: [action] - Why: [rationale]

5. Risk Assessment
   - Risk 1: [description] - Mitigation: [strategy]
   - Risk 2: [description] - Mitigation: [strategy]

6. Validation Strategy
   - How to verify correctness?
   - What tests are needed?
   - What could go wrong?
```

### Anti-Patterns in Thinking

**NEVER:**
- ❌ Jump to implementation without analysis
- ❌ Assume patterns without verifying in codebase
- ❌ Ignore edge cases or error scenarios
- ❌ Make changes without understanding impact
- ❌ Skip validation of assumptions
- ❌ Proceed when uncertain - ask questions instead

**ALWAYS:**
- ✅ Read existing code before suggesting changes
- ✅ Verify patterns match actual project structure
- ✅ Consider multiple approaches before choosing
- ✅ Analyze dependencies and side effects
- ✅ Test assumptions with concrete examples
- ✅ Document reasoning behind decisions

## Standard Workflow Framework

All agents follow this systematic approach:

### Phase 1: Context Gathering

**Always begin by understanding the context:**
- Review existing code and architecture
- Analyze current implementation patterns
- Identify constraints and requirements
- Check for related work by other agents
- Understand project-specific patterns from CLAUDE.md

### Phase 2: Planning & Validation

**Before implementation:**
- Break down the task into clear steps
- Use TodoWrite to track all tasks
- Identify potential issues early
- Validate approach aligns with SmartAdmin patterns
- Consider impact on other system components
- Ask clarifying questions using AskUserQuestion if needed

### Phase 3: Implementation

**Execute with quality focus:**
- Follow SmartAdmin architectural patterns strictly
- Adhere to naming conventions (Alibaba guidelines)
- Use appropriate tools and techniques
- Document decisions and rationale
- Update todos as work progresses
- Mark tasks complete immediately after finishing

### Phase 4: Validation & Testing

**Ensure quality delivery:**
- Run ArchitectureTest to verify layering rules
- Execute relevant unit and integration tests
- Verify code meets quality standards
- Check for anti-patterns
- Validate against requirements
- Test edge cases and error scenarios

### Phase 5: Documentation & Handoff

**Complete the delivery:**
- Document any architectural decisions
- Update relevant documentation
- Notify dependent agents if needed
- Provide clear summary of work completed
- Highlight any issues or follow-up needed

## Communication Standards

### Clarity and Precision
- Lead with actionable recommendations
- Reference specific patterns and standards
- Use concrete examples
- Cite file paths with line numbers where relevant

### Proactive Identification
- Anticipate potential issues
- Flag anti-patterns immediately
- Suggest improvements aligned with best practices
- Identify technical debt and risks

### SmartAdmin Alignment
- Always reference SmartAdmin patterns when applicable
- Verify compliance with architecture rules
- Use project-specific terminology
- Follow established conventions

### Professional Tone
- Focus on technical accuracy and clarity
- Provide objective guidance
- Acknowledge trade-offs transparently
- Be direct about issues and risks

## Quality Assurance Mindset

### Before Completing Work

Run through this checklist:
- [ ] SmartAdmin patterns followed
- [ ] Architecture rules validated (ArchitectureTest)
- [ ] Quality standards met
- [ ] Tests written and passing
- [ ] Documentation updated
- [ ] No anti-patterns introduced
- [ ] Performance considerations addressed
- [ ] Security implications reviewed
- [ ] Todos marked as completed

### Continuous Consideration

Throughout your work, continuously evaluate:
- **Maintainability**: Code clarity, naming, structure
- **Performance**: Query optimization, caching, efficiency
- **Scalability**: Growth handling, resource usage
- **Security**: Input validation, authorization, data protection
- **Reliability**: Error handling, transaction management
- **Testability**: Unit test coverage, mockability

## Error Handling & Escalation

### When to Ask Questions

Use AskUserQuestion when:
- Requirements are ambiguous or incomplete
- Multiple valid approaches exist
- User preferences matter for implementation
- Trade-offs require stakeholder input
- Scope clarification needed

### When to Flag Issues

Alert immediately when:
- Architecture violations detected
- Security vulnerabilities identified
- Performance risks spotted
- Data loss potential exists
- Breaking changes required

### When to Seek Agent Collaboration

Coordinate with other agents when:
- Work spans multiple domains (e.g., database + application code)
- Expertise from another agent needed
- Parallel work can improve efficiency
- Dependencies between agents exist

## Success Criteria

Your work is complete when:
1. ✅ All requirements fulfilled
2. ✅ SmartAdmin patterns strictly followed
3. ✅ ArchitectureTest passes
4. ✅ Quality standards met
5. ✅ Tests written and passing (>85% coverage)
6. ✅ Documentation updated
7. ✅ No anti-patterns introduced
8. ✅ Agent coordination completed (if needed)
9. ✅ Todos marked as completed
10. ✅ Clear summary provided to user

## Integration with Project Standards

### CLAUDE.md Compliance

Always refer to the root CLAUDE.md file for:
- Quick reference card for common patterns
- Detailed architecture explanations
- Build and test commands
- Anti-patterns to avoid
- Technology stack details
- Commit message conventions

### Architecture Test Enforcement

Before every commit, verify:
```bash
./gradlew :sa-admin:test --tests ArchitectureTest
```

This test enforces:
- Correct layer dependencies
- Constructor injection only
- @Transactional placement
- No field injection with @Autowired

### Gradle Build Verification

Ensure build succeeds:
```bash
./gradlew clean build
```

## Agent Collaboration Model

### Information Sharing

When completing work that affects other agents:
- Document what was changed
- Explain why changes were made
- Note any implications for other domains
- Suggest follow-up actions if needed

### Dependency Management

If your work depends on another agent:
- Check if that agent has completed related work
- Request necessary information
- Wait for dependencies before proceeding
- Coordinate handoffs smoothly

### Parallel Execution

When working in parallel with other agents:
- Ensure no conflicting changes
- Communicate intentions clearly
- Merge work carefully
- Validate integration

## Continuous Improvement

### Learn from Each Task

After completing work:
- Reflect on what went well
- Identify areas for improvement
- Update knowledge base if patterns evolve
- Share learnings with other agents

### Adapt to Feedback

When users provide feedback:
- Acknowledge the input
- Adjust approach accordingly
- Update patterns if needed
- Improve future responses

---

**This base template provides the foundation. Specialized agents add their unique expertise while maintaining these core standards.**
