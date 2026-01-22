# Agent Architecture Diagrams

Visual representations of the .claude configuration system using Mermaid diagrams.

**Version:** 2.3.0
**Last Updated:** 2026-01-21

## Table of Contents

1. [Agent Dependency Graph](#agent-dependency-graph)
2. [Agent Collaboration Patterns](#agent-collaboration-patterns)
3. [Configuration Architecture](#configuration-architecture)
4. [Evolution Timeline](#evolution-timeline)
5. [How to Read These Diagrams](#how-to-read-these-diagrams)

---

## Agent Dependency Graph

This diagram shows the relationships between agents, templates, and knowledge base files.

```mermaid
graph TB
    %% Base & Mixins
    base[agent-base.md<br/>Foundation for all]
    tech[technical-agent-mixin.md<br/>Technical agents]
    analysis[analysis-agent-mixin.md<br/>Analysis agents]

    %% Knowledge Base
    kb_sa[smartadmin-patterns.md<br/>Backend patterns]
    kb_fe[smartadmin-frontend-patterns.md<br/>Frontend patterns]
    kb_arch[project-architecture.md<br/>Tech stack]
    kb_qual[quality-standards.md<br/>Code quality]

    %% Agents - Technical
    java[java-architect<br/>Backend Expert]
    vue[vue-expert<br/>Frontend Expert]
    devops[devops-engineer<br/>CI/CD Expert]
    postgres[postgres-pro<br/>Database Expert]
    arch_rev[architect-reviewer<br/>Design Review]
    code_rev[code-reviewer<br/>Quality Review]

    %% Agents - Analysis
    ba[business-analyst<br/>Requirements]
    chaos[chaos-engineer<br/>Resilience]

    %% Inheritance
    base -.->|inherits| java
    base -.->|inherits| vue
    base -.->|inherits| ba
    base -.->|inherits| chaos
    base -.->|inherits| devops
    base -.->|inherits| postgres
    base -.->|inherits| arch_rev
    base -.->|inherits| code_rev

    tech -.->|mixin| java
    tech -.->|mixin| vue
    tech -.->|mixin| devops
    tech -.->|mixin| postgres
    tech -.->|mixin| arch_rev
    tech -.->|mixin| code_rev

    analysis -.->|mixin| ba
    analysis -.->|mixin| chaos

    %% Knowledge Base Dependencies
    kb_sa -->|references| java
    kb_sa -->|references| arch_rev
    kb_sa -->|references| code_rev
    kb_fe -->|references| vue
    kb_arch -->|references| java
    kb_arch -->|references| vue
    kb_arch -->|references| devops
    kb_qual -->|references| code_rev
    kb_qual -->|references| arch_rev

    style base fill:#e1f5ff
    style tech fill:#fff4e1
    style analysis fill:#ffe1f5
    style java fill:#b3e5fc
    style vue fill:#b3e5fc
    style devops fill:#b3e5fc
    style postgres fill:#b3e5fc
    style arch_rev fill:#b3e5fc
    style code_rev fill:#b3e5fc
    style ba fill:#f8bbd0
    style chaos fill:#f8bbd0
```

### Legend

- **Blue boxes** (light): Base template
- **Orange boxes**: Technical agent mixin
- **Pink boxes**: Analysis agent mixin
- **Cyan boxes**: Technical agents (6)
- **Pink boxes** (darker): Analysis agents (2)
- **Dotted arrows**: Inheritance/mixin relationship
- **Solid arrows**: Knowledge base references

---

## Agent Collaboration Patterns

### Pattern 1: Full-Stack Feature Development

Sequential workflow from requirements to deployment.

```mermaid
sequenceDiagram
    participant BA as business-analyst
    participant Arch as architect-reviewer
    participant Java as java-architect
    participant Vue as vue-expert
    participant Code as code-reviewer
    participant Dev as devops-engineer
    participant Chaos as chaos-engineer

    BA->>BA: Gather requirements<br/>Create user stories
    BA->>Arch: Request architecture design

    Arch->>Arch: Validate design<br/>Check layer boundaries
    Arch->>Java: Backend architecture plan
    Arch->>Vue: Frontend architecture plan

    par Backend & Frontend Development
        Java->>Java: Implement backend<br/>(Controller→Service→Manager→Dao)
        Vue->>Vue: Implement frontend<br/>(Vue 3 + Ant Design Vue)
    end

    Java->>Vue: API contracts<br/>(Swagger, ResponseDTO)
    Vue->>Java: Integration testing

    Code->>Code: Pre-merge review<br/>(Security, quality, performance)
    Code->>Java: Fix critical issues
    Code->>Vue: Fix critical issues

    Java->>Code: Fixes applied
    Vue->>Code: Fixes applied

    Code->>Dev: Approve for deployment

    Dev->>Dev: Deploy to staging
    Dev->>Chaos: Ready for resilience testing

    Chaos->>Chaos: Validate failure scenarios
    Chaos->>Dev: Resilience validated

    Dev->>Dev: Deploy to production
```

**Key Points:**
- BA gathers requirements first
- Architect reviews design before implementation
- Backend and frontend work in parallel
- Code reviewer acts as quality gate
- DevOps handles deployment
- Chaos engineer validates resilience

---

### Pattern 2: Pre-Merge Quality Gate (Hub-and-Spoke)

Code reviewer coordinates with specialists for comprehensive review.

```mermaid
graph TB
    PR[Pull Request<br/>Ready for Merge]
    Code[code-reviewer<br/>HUB]

    PR --> Code

    Code -->|Backend changes?| Java[java-architect<br/>SmartAdmin patterns]
    Code -->|Frontend changes?| Vue[vue-expert<br/>Vue 3 best practices]
    Code -->|DB changes?| PG[postgres-pro<br/>Query optimization]
    Code -->|Architecture impact?| Arch[architect-reviewer<br/>Layer boundaries]

    Java -.->|Feedback:<br/>- Issues found<br/>- Severity levels<br/>- Suggested fixes| Code
    Vue -.->|Feedback| Code
    PG -.->|Feedback| Code
    Arch -.->|Feedback| Code

    Code --> Decision{All checks<br/>passed?}
    Decision -->|Yes<br/>No critical issues| Merge[✅ Approve Merge]
    Decision -->|No<br/>Critical/Major issues| Fix[❌ Request Changes]
    Fix --> PR

    style Code fill:#ffcccc,stroke:#ff0000,stroke-width:3px
    style Decision fill:#ffffcc
    style Merge fill:#ccffcc
    style Fix fill:#ffcccc
```

**Key Points:**
- Code reviewer is the hub, coordinates all reviews
- Dispatches to specialists based on change type
- Aggregates feedback from all specialists
- Makes final pass/fail decision
- Iterates until all critical issues resolved

---

### Pattern 3: Architecture Review & Refactoring

Collaborative architecture assessment with impact analysis.

```mermaid
graph LR
    Start[Architecture<br/>Review Needed] --> Arch[architect-reviewer<br/>Assessment]

    Arch --> |Findings:<br/>- Layer violations<br/>- Technical debt<br/>- Scalability concerns| Par1[Parallel Impact<br/>Analysis]

    Par1 --> Java[java-architect<br/>Code Impact]
    Par1 --> Vue[vue-expert<br/>Frontend Impact]
    Par1 --> PG[postgres-pro<br/>DB Impact]
    Par1 --> Dev[devops-engineer<br/>Infrastructure Impact]
    Par1 --> BA[business-analyst<br/>ROI Analysis]

    Java --> |Impact report| Consolidate[architect-reviewer<br/>Consolidate Findings]
    Vue --> |Impact report| Consolidate
    PG --> |Impact report| Consolidate
    Dev --> |Impact report| Consolidate
    BA --> |ROI report| Consolidate

    Consolidate --> Plan[Refactoring<br/>Roadmap<br/>- Priorities<br/>- Timeline<br/>- Resources]
    Plan --> Impl[Implementation<br/>Phase]
    Impl --> CodeRev[code-reviewer<br/>Validation]
    CodeRev --> |Approved| Done[✅ Refactoring<br/>Complete]

    style Arch fill:#ffcccc,stroke:#ff0000,stroke-width:2px
    style Consolidate fill:#ffcccc,stroke:#ff0000,stroke-width:2px
    style CodeRev fill:#ffcccc,stroke:#ff0000,stroke-width:2px
    style Done fill:#ccffcc
```

**Key Points:**
- Architect reviewer leads assessment
- All specialists analyze impact in parallel
- Business analyst evaluates ROI
- Consolidated roadmap with priorities
- Implementation followed by quality validation

---

## Configuration Architecture

Visualizes the .claude directory structure and relationships.

```mermaid
graph TB
    subgraph "Shared Infrastructure"
        subgraph "Knowledge Base (4 files)"
            KB1[smartadmin-patterns.md<br/>313 lines<br/>Backend patterns]
            KB2[smartadmin-frontend-patterns.md<br/>823 lines<br/>Frontend patterns]
            KB3[project-architecture.md<br/>263 lines<br/>Tech stack & build]
            KB4[quality-standards.md<br/>390 lines<br/>Code quality]
        end

        subgraph "Templates (3 files)"
            T1[agent-base.md<br/>378 lines<br/>Foundation]
            T2[technical-agent-mixin.md<br/>318 lines<br/>Technical agents]
            T3[analysis-agent-mixin.md<br/>220 lines<br/>Analysis agents]
        end

        subgraph "Orchestration (3 files)"
            O1[decision-matrix.md<br/>~290 lines<br/>Agent selection]
            O2[agent-dependencies.md<br/>~310 lines<br/>Collaboration]
            O3[workflow-patterns.md<br/>~450 lines<br/>10 patterns]
        end
    end

    subgraph "Specialized Agents (8 files)"
        A1[java-architect.md<br/>548 lines]
        A2[vue-expert.md<br/>662 lines]
        A3[business-analyst.md<br/>602 lines]
        A4[chaos-engineer.md<br/>456 lines]
        A5[devops-engineer.md<br/>614 lines]
        A6[postgres-pro.md<br/>768 lines]
        A7[architect-reviewer.md<br/>~135 lines]
        A8[code-reviewer.md<br/>~155 lines]
    end

    KB1 -.->|Backend patterns| A1
    KB1 -.->|Backend patterns| A7
    KB1 -.->|Backend patterns| A8
    KB2 -.->|Frontend patterns| A2
    KB3 -.->|Tech stack| A1
    KB3 -.->|Tech stack| A2
    KB3 -.->|Tech stack| A5
    KB4 -.->|Quality standards| A7
    KB4 -.->|Quality standards| A8

    T1 -.->|Inherits| A1
    T1 -.->|Inherits| A2
    T1 -.->|Inherits| A3
    T1 -.->|Inherits| A4
    T1 -.->|Inherits| A5
    T1 -.->|Inherits| A6
    T1 -.->|Inherits| A7
    T1 -.->|Inherits| A8

    T2 -.->|Mixin| A1
    T2 -.->|Mixin| A2
    T2 -.->|Mixin| A5
    T2 -.->|Mixin| A6
    T2 -.->|Mixin| A7
    T2 -.->|Mixin| A8

    T3 -.->|Mixin| A3
    T3 -.->|Mixin| A4

    O1 -.->|Guides selection| User[User Request]
    O2 -.->|Coordinates| A1
    O2 -.->|Coordinates| A2
    O3 -.->|Orchestrates| A1
    O3 -.->|Orchestrates| A2

    style KB1 fill:#e3f2fd
    style KB2 fill:#e3f2fd
    style KB3 fill:#e3f2fd
    style KB4 fill:#e3f2fd
    style T1 fill:#fff3e0
    style T2 fill:#fff3e0
    style T3 fill:#fff3e0
    style O1 fill:#f3e5f5
    style O2 fill:#f3e5f5
    style O3 fill:#f3e5f5
```

### Architecture Principles

1. **Single Source of Truth**: SmartAdmin patterns in one place
2. **Template Inheritance**: All agents inherit from agent-base.md
3. **Mixin Pattern**: Technical vs Analysis agent behaviors
4. **Orchestration Framework**: Clear agent selection and collaboration

---

## Evolution Timeline

Shows the progression of the .claude configuration system.

```mermaid
timeline
    title .claude Configuration Evolution (2026)

    v1.0.0 (Jan 20) : 5 agents
                     : 55% duplication
                     : 58 permission patterns
                     : No orchestration
                     : Manual agent selection

    v2.0.0 (Jan 21) : Shared knowledge base
                     : Template inheritance system
                     : Orchestration framework
                     : 82% duplication reduction
                     : 79% permission consolidation
                     : Decision matrix added

    v2.1.0 (Jan 21) : Deep thinking protocol
                     : Enhanced reasoning
                     : Ultrathink methodology
                     : All agents upgraded

    v2.2.0 (Jan 21) : Frontend integration
                     : vue-expert optimization
                     : Full-stack orchestration
                     : 6 agents complete
                     : Frontend patterns added

    v2.3.0 (Jan 21) : 🎉 100% completion
                     : 8 agents integrated
                     : architect-reviewer added
                     : code-reviewer added
                     : 10 workflow patterns
                     : Visual documentation
```

### Version Highlights

| Version | Key Achievement | Impact |
|---------|----------------|--------|
| v1.0.0 | Initial configuration | 5 agents, high duplication |
| v2.0.0 | Infrastructure optimization | 82% duplication reduction |
| v2.1.0 | Deep thinking protocol | Enhanced agent intelligence |
| v2.2.0 | Frontend integration | Full-stack coverage |
| v2.3.0 | 100% completion | All agents v2.x compliant |

---

## Permission Consolidation

Visualizes the dramatic reduction in permission patterns.

```mermaid
graph LR
    subgraph "v1.0.0 (58 patterns)"
        P1[git add<br/>git commit<br/>git push<br/>git status<br/>git log<br/>git diff<br/>git branch<br/>git checkout<br/>...50 more]
    end

    subgraph "v2.3.0 (12 patterns)"
        P2[git *<br/>./gradlew *<br/>gradlew.bat *<br/>docker *<br/>docker-compose *<br/>netstat *<br/>curl *<br/>findstr *<br/>grep *<br/>dir * / ls *<br/>find *<br/>del * / rm *]
    end

    P1 -->|79% reduction<br/>Consolidate with wildcards| P2

    style P1 fill:#ffcccc
    style P2 fill:#ccffcc
```

**Benefits:**
- **Maintainability**: Update once per category
- **Clarity**: Clear permission categories
- **Cross-platform**: Support both Windows and Unix
- **Security**: Well-documented rationale

---

## Agent Selection Flow

Decision tree for selecting the right agent.

```mermaid
graph TD
    Start[User Request] --> Q1{Code quality/<br/>pre-merge review?}
    Q1 -->|Yes| CodeReviewer[code-reviewer]
    Q1 -->|No| Q2{Architecture/<br/>design review?}

    Q2 -->|Yes| ArchReviewer[architect-reviewer]
    Q2 -->|No| Q3{Java/Spring Boot<br/>development?}

    Q3 -->|Yes| Java[java-architect]
    Q3 -->|No| Q4{Vue/Frontend<br/>development?}

    Q4 -->|Yes| Vue[vue-expert]
    Q4 -->|No| Q5{Requirements/<br/>stakeholders?}

    Q5 -->|Yes| BA[business-analyst]
    Q5 -->|No| Q6{CI/CD/<br/>deployment?}

    Q6 -->|Yes| DevOps[devops-engineer]
    Q6 -->|No| Q7{Database/<br/>PostgreSQL?}

    Q7 -->|Yes| Postgres[postgres-pro]
    Q7 -->|No| Q8{Resilience/<br/>failure testing?}

    Q8 -->|Yes| Chaos[chaos-engineer]
    Q8 -->|No| General[Use general-purpose<br/>or ask for clarification]

    style CodeReviewer fill:#ffcccc
    style ArchReviewer fill:#ffcccc
    style Java fill:#b3e5fc
    style Vue fill:#b3e5fc
    style BA fill:#f8bbd0
    style DevOps fill:#b3e5fc
    style Postgres fill:#b3e5fc
    style Chaos fill:#f8bbd0
    style General fill:#ffffcc
```

**Usage:** Follow this decision tree when unsure which agent to invoke.

---

## How to Read These Diagrams

### Symbol Guide

| Symbol | Meaning |
|--------|---------|
| Solid arrow (`→`) | Direct dependency or flow |
| Dotted arrow (`-.->`) | Inheritance or reference |
| Box with rounded corners | Agent or component |
| Diamond (`◇`) | Decision point |
| Parallel bars (`║`) | Concurrent operations |

### Color Coding

| Color | Meaning |
|-------|---------|
| 🔵 Light blue | Base templates |
| 🟠 Orange | Technical agent mixin |
| 🌸 Pink (light) | Analysis agent mixin |
| 🔷 Cyan | Technical agents |
| 🌺 Pink (dark) | Analysis agents |
| 🟢 Green | Success/approval |
| 🔴 Red | Action/review required |
| 🟡 Yellow | Decision/conditional |

### Diagram Types

**Sequence Diagrams:**
- Show temporal order of agent interactions
- Read top to bottom for chronological flow
- `par` blocks indicate parallel operations

**Flow Diagrams:**
- Show decision points and branching
- Follow arrows for logical flow
- Diamonds indicate decision nodes

**Graph Diagrams:**
- Show relationships between components
- Dotted lines: Inheritance/reference
- Solid lines: Direct dependencies

**Timeline:**
- Shows chronological evolution
- Read left to right for progression

---

## Using These Diagrams

### For Developers

1. **Agent Selection**: Use the Agent Selection Flow diagram
2. **Understanding Workflows**: Review collaboration pattern diagrams
3. **Architecture Changes**: Check Configuration Architecture diagram
4. **Version History**: Consult Evolution Timeline

### For Architects

1. **System Design**: Study the Agent Dependency Graph
2. **Collaboration Patterns**: Review all 3 collaboration diagrams
3. **Evolution Planning**: Use Evolution Timeline for roadmap
4. **Permission Management**: Review Permission Consolidation

### For Maintenance

1. **Adding Agents**: Follow Configuration Architecture structure
2. **Updating Patterns**: Check Agent Dependency Graph for impacts
3. **Workflow Changes**: Update relevant collaboration pattern diagrams
4. **Documentation**: Keep diagrams in sync with code

---

## Mermaid Syntax Reference

These diagrams use Mermaid syntax. To render them:

**In GitHub:**
- Diagrams render automatically in markdown files

**In IDEs:**
- Install Mermaid preview extension
- VS Code: "Markdown Preview Mermaid Support"
- IntelliJ: Built-in Mermaid support

**Online:**
- https://mermaid.live/ - Live editor
- Copy diagram code, paste, and preview

---

## Maintaining These Diagrams

### When to Update

**Add/Remove Agent:**
1. Update Agent Dependency Graph
2. Update Configuration Architecture
3. Update Agent Selection Flow
4. Update Evolution Timeline (if version bump)

**Change Workflow:**
1. Update relevant collaboration pattern diagram
2. Add notes explaining the change

**New Version:**
1. Add entry to Evolution Timeline
2. Update version numbers in diagrams

### Validation

Before committing diagram changes:
1. Render in Mermaid Live editor to verify syntax
2. Check all relationships are accurate
3. Ensure colors/styling are consistent
4. Update diagram legends if needed

---

**Generated:** 2026-01-21
**Version:** 2.3.0
**Status:** ✅ Complete (8 agents)

For questions about these diagrams, see [Maintenance Guide](maintenance-guide.md) or [README](../README.md).
