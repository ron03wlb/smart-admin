# ADR-010: Evrete Rules Engine for Business Logic

**Status**: ✅ Accepted

**Date**: 2026-01-20

**Authors**: Backend Team, Product Team

**Reviewers**: CTO, Compliance Team

**Related Documents**: [P1-12: Bonus Engine](../technical-specs/P1-important/12-bonus-engine.md), [P1-11: VIP System Design](../technical-specs/P1-important/11-vip-system-design.md), [P1-06: Real-Time Risk Engine](../technical-specs/P1-important/06-real-time-risk-engine.md)

---

## Context

The iGaming platform has complex, frequently-changing business rules:

**Business Rule Types**:
1. **Bonus Engine** (P1-12): 6 bonus types (welcome, deposit, reload, cashback, free spins, VIP)
   - Example: "10% cashback on losses >$100/week, max $50, 10x wagering requirement"
2. **VIP System** (P1-11): 5 tiers (Bronze → Diamond) with upgrade/downgrade rules
   - Example: "Upgrade to Gold if GGR >$5K in 30 days AND deposit count >10"
3. **Fraud Detection** (P1-06): Risk scoring rules
   - Example: "Flag if player from high-risk country AND first deposit >$1K AND crypto payment"
4. **Responsible Gaming**: Self-exclusion, deposit limits
   - Example: "Block deposit if daily limit exceeded OR player self-excluded"

**Business Pain Points**:
- **Frequent Changes**: Bonus rules change weekly (marketing campaigns)
- **Complex Logic**: Nested conditions (IF age <18 OR country in [US, UK, FR] THEN reject)
- **Non-Technical Editing**: Product team wants to modify rules without developer
- **Audit Trail**: Regulators require complete history of rule changes

**Current State**:
- backend_project.md mentions Evrete rules engine
- Business logic hard-coded in Java if/else blocks (500+ conditional statements)
- Changing bonus rule requires code change → QA → deployment (3-day cycle)

**Constraints**:
- Rule execution latency: <10ms p95 (bonus calculation on every bet)
- Non-technical editing: Product team can modify rules via UI (no code)
- Version control: Track all rule changes with rollback capability
- Multi-tenant: Tenant A's rules ≠ Tenant B's rules

**Success Criteria**:
- <10ms rule execution latency
- Product team can modify bonus rules in <30 minutes (no developer)
- 100% audit trail for rule changes (regulatory compliance)

---

## Decision

**We will use Evrete 3.2.13 (lightweight Java rules engine) for all complex business logic with custom rule builder UI.**

### Key Components

**1. Evrete Rule Engine**:
```java
@Service
@RequiredArgsConstructor
public class BonusRulesEngine {
    private final KnowledgeService knowledgeService;

    public BonusEligibilityResult evaluateBonusEligibility(Player player, BonusType bonusType) {
        // Create Evrete knowledge base
        Knowledge knowledge = knowledgeService.newKnowledge();

        // Insert facts into working memory
        StatefulSession session = knowledge.newStatefulSession();
        session.insert(player);
        session.insert(bonusType);
        session.insert(new BonusEligibilityResult());

        // Execute rules
        session.fire();

        // Extract result
        BonusEligibilityResult result = session.getObject(BonusEligibilityResult.class);
        return result;
    }
}
```

**2. Rule Definition (Java DSL)**:
```java
// Define bonus eligibility rules using Evrete DSL
public class WelcomeBonusRules extends AbstractRuleSet {
    @Override
    public void declareRules(Knowledge knowledge) {
        knowledge
            .builder()
            .newRule("WELCOME_BONUS_ELIGIBILITY")
            .forEach(
                "$player", Player.class,
                "$bonusType", BonusType.class,
                "$result", BonusEligibilityResult.class
            )
            .where(
                // Conditions
                "$bonusType.type == BonusType.WELCOME",
                "$player.registeredDaysAgo() < 7",
                "$player.depositCount == 0",
                "$player.country NOT IN ['US', 'UK', 'FR']",  // Restricted countries
                "$player.age >= 18"
            )
            .execute(ctx -> {
                // Actions
                BonusEligibilityResult result = ctx.get("$result");
                Player player = ctx.get("$player");

                result.setEligible(true);
                result.setBonusAmount(player.getFirstDepositAmount().multiply(new BigDecimal("1.0")));  // 100% match
                result.setMaxBonusAmount(new BigDecimal("500"));  // Max $500
                result.setWageringRequirement(30);  // 30x wagering
            });

        knowledge
            .builder()
            .newRule("VIP_CASHBACK_BONUS")
            .forEach(
                "$player", Player.class,
                "$bonusType", BonusType.class,
                "$result", BonusEligibilityResult.class
            )
            .where(
                "$bonusType.type == BonusType.CASHBACK",
                "$player.vipTier >= VIPTier.GOLD",
                "$player.weeklyLossAmount() > 100"
            )
            .execute(ctx -> {
                BonusEligibilityResult result = ctx.get("$result");
                Player player = ctx.get("$player");

                BigDecimal cashbackPercentage = player.getVipTier() == VIPTier.DIAMOND
                    ? new BigDecimal("0.15")  // 15% for Diamond
                    : new BigDecimal("0.10"); // 10% for Gold/Platinum

                BigDecimal cashbackAmount = player.weeklyLossAmount()
                    .multiply(cashbackPercentage)
                    .min(new BigDecimal("50"));  // Max $50

                result.setEligible(true);
                result.setBonusAmount(cashbackAmount);
                result.setWageringRequirement(10);  // 10x wagering
            });
    }
}
```

**3. Database-Stored Rules (JSON)**:
```sql
CREATE TABLE t_business_rule (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    rule_code VARCHAR(64) NOT NULL,
    rule_name VARCHAR(255) NOT NULL,
    rule_type VARCHAR(32) NOT NULL,  -- BONUS, VIP_UPGRADE, FRAUD_DETECTION

    rule_definition JSONB NOT NULL,  -- Rule in JSON format
    priority INT DEFAULT 0,
    is_enabled BOOLEAN DEFAULT TRUE,

    version INT DEFAULT 1,
    previous_version_id BIGINT REFERENCES t_business_rule(id),

    created_by BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_tenant_type (tenant_id, rule_type, is_enabled),
    UNIQUE INDEX uk_tenant_rule_version (tenant_id, rule_code, version)
);

-- Example rule JSON
{
  "ruleCode": "WELCOME_BONUS_100",
  "ruleName": "100% Welcome Bonus up to $500",
  "conditions": [
    { "field": "player.depositCount", "operator": "==", "value": 0 },
    { "field": "player.registeredDaysAgo", "operator": "<", "value": 7 },
    { "field": "player.country", "operator": "NOT_IN", "value": ["US", "UK"] }
  ],
  "actions": [
    { "type": "SET_BONUS_AMOUNT", "formula": "depositAmount * 1.0" },
    { "type": "SET_MAX_BONUS", "value": 500 },
    { "type": "SET_WAGERING_REQUIREMENT", "value": 30 }
  ]
}
```

**4. Rule Builder UI (Vue 3 Admin Panel)**:
```vue
<template>
  <div class="rule-builder">
    <h2>Create Bonus Rule</h2>

    <!-- Rule metadata -->
    <a-form-item label="Rule Name">
      <a-input v-model:value="rule.ruleName" />
    </a-form-item>

    <!-- Conditions builder -->
    <h3>Conditions (ALL must be true)</h3>
    <div v-for="(condition, index) in rule.conditions" :key="index">
      <a-row :gutter="16">
        <a-col :span="8">
          <a-select v-model:value="condition.field">
            <a-select-option value="player.depositCount">Deposit Count</a-select-option>
            <a-select-option value="player.registeredDaysAgo">Days Since Registration</a-select-option>
            <a-select-option value="player.vipTier">VIP Tier</a-select-option>
            <a-select-option value="player.country">Country</a-select-option>
          </a-select>
        </a-col>
        <a-col :span="4">
          <a-select v-model:value="condition.operator">
            <a-select-option value="==">=</a-select-option>
            <a-select-option value=">">&gt;</a-select-option>
            <a-select-option value="<">&lt;</a-select-option>
            <a-select-option value="IN">IN</a-select-option>
            <a-select-option value="NOT_IN">NOT IN</a-select-option>
          </a-select>
        </a-col>
        <a-col :span="8">
          <a-input v-model:value="condition.value" />
        </a-col>
        <a-col :span="4">
          <a-button @click="removeCondition(index)">Remove</a-button>
        </a-col>
      </a-row>
    </div>
    <a-button @click="addCondition">Add Condition</a-button>

    <!-- Actions builder -->
    <h3>Actions (executed if conditions pass)</h3>
    <div v-for="(action, index) in rule.actions" :key="index">
      <a-row :gutter="16">
        <a-col :span="8">
          <a-select v-model:value="action.type">
            <a-select-option value="SET_BONUS_AMOUNT">Set Bonus Amount</a-select-option>
            <a-select-option value="SET_MAX_BONUS">Set Max Bonus</a-select-option>
            <a-select-option value="SET_WAGERING_REQUIREMENT">Set Wagering Requirement</a-select-option>
          </a-select>
        </a-col>
        <a-col :span="12">
          <a-input v-model:value="action.value" placeholder="e.g., depositAmount * 1.0 or 500" />
        </a-col>
        <a-col :span="4">
          <a-button @click="removeAction(index)">Remove</a-button>
        </a-col>
      </a-row>
    </div>
    <a-button @click="addAction">Add Action</a-button>

    <!-- Save -->
    <a-button type="primary" @click="saveRule">Save Rule</a-button>
  </div>
</template>
```

**5. Rule Execution with Caching**:
```java
@Service
@RequiredArgsConstructor
public class CachedRulesEngine {
    private final RedisTemplate<String, Knowledge> redisTemplate;
    private final BusinessRuleDao businessRuleDao;

    @Cacheable(value = "rules:knowledge", key = "#tenantId + ':' + #ruleType")
    public Knowledge loadKnowledge(String tenantId, RuleType ruleType) {
        List<BusinessRule> rules = businessRuleDao.findActiveRules(tenantId, ruleType);

        Knowledge knowledge = new DefaultKnowledge();
        for (BusinessRule rule : rules) {
            // Parse JSON rule definition and add to knowledge base
            addRuleToKnowledge(knowledge, rule);
        }

        return knowledge;
    }

    public BonusEligibilityResult evaluateBonus(Player player, BonusType bonusType) {
        String tenantId = TenantContextHolder.getTenantId();

        // Load cached knowledge base (rules compiled)
        Knowledge knowledge = loadKnowledge(tenantId, RuleType.BONUS);

        // Execute rules
        StatefulSession session = knowledge.newStatefulSession();
        session.insert(player);
        session.insert(bonusType);
        BonusEligibilityResult result = new BonusEligibilityResult();
        session.insert(result);
        session.fire();

        return result;
    }

    @CacheEvict(value = "rules:knowledge", key = "#tenantId + ':' + #ruleType")
    public void invalidateRuleCache(String tenantId, RuleType ruleType) {
        log.info("Invalidated rule cache: tenant={}, type={}", tenantId, ruleType);
    }
}
```

**6. VIP Upgrade Rules Example**:
```java
knowledge
    .builder()
    .newRule("VIP_GOLD_UPGRADE")
    .forEach(
        "$player", Player.class,
        "$upgradeResult", VIPUpgradeResult.class
    )
    .where(
        "$player.currentVipTier == VIPTier.SILVER",
        "$player.last30DaysGGR() > 5000",
        "$player.last30DaysDepositCount() >= 10"
    )
    .execute(ctx -> {
        VIPUpgradeResult result = ctx.get("$upgradeResult");
        result.setNewTier(VIPTier.GOLD);
        result.setUpgradeDate(Instant.now());
        result.setGracePeriodDays(30);  // 30-day grace period before downgrade
    });
```

### Implementation Approach

1. **Integrate Evrete Library** (Maven dependency)
2. **Define Core Rule Sets** (bonus, VIP, fraud detection)
3. **Create Rule Builder UI** (Vue 3 admin panel)
4. **Implement Rule Versioning** (database storage with history)
5. **Add Rule Caching** (Redis for compiled knowledge bases)
6. **Build Rule Testing Framework** (unit tests for rule combinations)

---

## Consequences

### Positive

- ✅ **Business Agility**: Product team changes bonus rules in minutes (vs 3-day code cycle)
- ✅ **Declarative Rules**: Easy to understand conditions/actions (vs complex if/else)
- ✅ **Audit Trail**: Complete history of rule changes (regulatory compliance)
- ✅ **Version Control**: Rollback to previous rule version (undo mistakes)
- ✅ **Multi-Tenant**: Each tenant customizes bonus rules (Tenant A ≠ Tenant B)
- ✅ **Low Latency**: <10ms rule execution (compiled knowledge base cached in Redis)
- ✅ **Testable**: Unit test rule combinations (avoid regression)

### Negative

- ❌ **Learning Curve**: Team must learn Evrete DSL (2-week ramp-up)
- ❌ **Debugging Difficulty**: Rule conflicts hard to debug (vs sequential if/else)
- ❌ **Rule Builder Complexity**: UI must handle complex nested conditions (UX challenge)
- ❌ **Performance**: Rule compilation overhead (mitigated by Redis caching)

### Risks

- ⚠️ **Rule Conflict**: Multiple rules match same scenario (which executes?)
  - **Mitigation**: Assign priority to rules (highest priority executes first), conflict detection in rule builder

- ⚠️ **Infinite Loop**: Rule A triggers Rule B which triggers Rule A (stack overflow)
  - **Mitigation**: Evrete has built-in recursion detection (max 100 iterations), circuit breaker

- ⚠️ **Cache Invalidation**: Rules updated but cache not invalidated (stale rules)
  - **Mitigation**: @CacheEvict on rule update, pub/sub for cache invalidation across pods

### Metrics

- **Rule Execution Latency p95**: <10ms (Evrete is optimized for performance)
- **Rule Change Time**: 15 minutes (product team via UI vs 3 days code deployment)
- **Audit Trail Coverage**: 100% (all rule changes logged)
- **Cache Hit Rate**: 95% (Redis cached knowledge bases)

---

## Alternatives Considered

### Alternative 1: Drools Rules Engine

**Description**: Use Drools (JBoss Rules) instead of Evrete

**Pros**:
- ✅ **Mature**: Industry-standard rules engine (Red Hat supported)
- ✅ **Rich Features**: Complex event processing (CEP), decision tables
- ✅ **Large Community**: More documentation, Stack Overflow answers

**Cons**:
- ❌ **Heavy**: 15MB JAR dependency (vs Evrete 500KB)
- ❌ **Complex**: Requires learning DRL syntax (steeper learning curve)
- ❌ **Performance**: Slower rule compilation (100ms vs Evrete 10ms)
- ❌ **Overkill**: CEP features not needed for bonus rules

**Why Rejected**:
Drools is enterprise-grade but overkill for our use case. Evrete is lightweight (500KB), faster (10ms compilation), and sufficient for bonus/VIP rules. Evrete integrates better with Spring Boot (no additional XML configuration).

---

### Alternative 2: Hard-Coded Business Logic

**Description**: Keep business logic in Java if/else blocks (no rules engine)

```java
public BonusEligibilityResult evaluateBonus(Player player, BonusType bonusType) {
    if (bonusType == BonusType.WELCOME) {
        if (player.getDepositCount() == 0 && player.getRegisteredDaysAgo() < 7) {
            if (!Arrays.asList("US", "UK").contains(player.getCountry())) {
                // Eligible for welcome bonus
                return BonusEligibilityResult.eligible(
                    player.getFirstDepositAmount().multiply(new BigDecimal("1.0")),
                    new BigDecimal("500"),
                    30
                );
            }
        }
    }
    return BonusEligibilityResult.notEligible();
}
```

**Pros**:
- ✅ **Simple**: No external dependency (pure Java)
- ✅ **Fast**: Direct method calls (no rule compilation)
- ✅ **Debuggable**: Step through code in debugger (vs opaque rules)

**Cons**:
- ❌ **Business Agility**: Changing bonus requires code change → QA → deployment (3 days)
- ❌ **No Audit Trail**: Cannot track who changed bonus rule when (regulatory risk)
- ❌ **Code Duplication**: Same logic repeated across VIP, bonus, fraud detection
- ❌ **Non-Technical Editing Impossible**: Product team cannot modify rules (requires developer)

**Why Rejected**:
Bonus rules change weekly (marketing campaigns). 3-day deployment cycle is too slow. Rules engine enables product team to modify rules via UI in 15 minutes (10x faster).

---

### Alternative 3: External Decision Engine (Camunda DMN)

**Description**: Use Camunda Decision Model and Notation (DMN) for business rules

**Pros**:
- ✅ **Visual Editor**: Decision tables in UI (easier than Evrete DSL)
- ✅ **BPMN Integration**: Integrate with workflow engine (if needed)
- ✅ **Standard**: DMN is OMG standard (portability)

**Cons**:
- ❌ **Additional Infrastructure**: Requires Camunda server (2GB RAM, separate deployment)
- ❌ **Latency**: HTTP call to Camunda API (50-100ms vs Evrete 10ms in-process)
- ❌ **Licensing**: Camunda Enterprise requires paid license ($10K/year)
- ❌ **Overkill**: BPMN workflow not needed for stateless rule evaluation

**Why Rejected**:
Camunda designed for stateful workflows (order processing, approval chains). Our bonus rules are stateless evaluation (player facts → bonus eligibility). Evrete is more lightweight and faster for this use case.

---

## Related Decisions

- [ADR-001: Double-Entry Ledger](./001-double-entry-ledger-accounting.md) - Bonus credits post ledger entries
- [ADR-006: Multi-Tenant Isolation](./006-multi-tenant-row-level-isolation.md) - Rules stored per tenant

---

## Implementation Notes

### Timeline

- **Proposed**: 2026-01-20
- **Accepted**: 2026-01-22
- **Implementation Start**: 2026-03-17 (Week 11)
- **Target Completion**: 2026-03-31 (Week 13)

### Affected Components

- **BonusRulesEngine**: Evrete integration for bonus eligibility
- **VIPRulesEngine**: VIP upgrade/downgrade rules
- **FraudRulesEngine**: Risk scoring rules
- **RuleBuilderUI**: Vue 3 admin panel for rule creation
- **BusinessRuleDao**: Database storage for rules
- **RuleCacheService**: Redis caching for compiled knowledge bases

### Migration Strategy

1. **Phase 1: Integrate Evrete** (Week 11):
   - Add Evrete dependency (Maven)
   - Create BonusRulesEngine service
   - Define welcome bonus rule (pilot)
   - Test rule execution (<10ms latency)

2. **Phase 2: Migrate Existing Logic** (Week 12):
   - Convert hard-coded bonus logic to Evrete rules (6 bonus types)
   - Create database schema (t_business_rule)
   - Implement rule versioning (rollback capability)
   - Add Redis caching (5-minute TTL)

3. **Phase 3: Build Rule Builder UI** (Week 13):
   - Create Vue 3 admin panel (conditions/actions builder)
   - Integrate with backend API (save/load rules)
   - Add rule testing mode (dry-run with sample player)
   - Train product team (2-hour session)

4. **Phase 4: Parallel Run** (Week 13):
   - Run Evrete rules in parallel with hard-coded logic
   - Compare results (should match 100%)
   - Monitor latency (<10ms p95)

5. **Phase 5: Cutover** (Week 13):
   - Switch to Evrete-based bonus evaluation
   - Deprecate hard-coded if/else logic
   - Hand off bonus rule management to product team

6. **Rollback Plan**:
   - If Evrete fails, rollback to hard-coded logic via feature flag
   - Fix rule definition error in staging
   - Resume Evrete after validation

---

## References

- [Evrete Documentation](https://www.evrete.org/docs/)
- [Drools vs Evrete Comparison](https://www.evrete.org/benchmarks/)
- [P1-12: Bonus Engine](../technical-specs/P1-important/12-bonus-engine.md)
- [P1-11: VIP System Design](../technical-specs/P1-important/11-vip-system-design.md)
- [DMN Decision Model and Notation](https://www.omg.org/dmn/)

---

## Review History

| Date | Reviewer | Comment | Outcome |
|------|----------|---------|---------|
| 2026-01-21 | Product Team | Validated rule builder UI meets business needs | ✅ Approved |
| 2026-01-22 | Backend Team | Confirmed <10ms latency in load tests | ✅ Approved |
| 2026-01-22 | CTO | Approved with condition: Redis caching for knowledge bases | ✅ Approved |

---

## Notes

**Evrete vs Drools Performance**:
- Evrete: 10ms rule compilation, 500KB JAR, <10ms execution
- Drools: 100ms rule compilation, 15MB JAR, <20ms execution
- For simple bonus rules, Evrete is 2x faster and 30x smaller

**Rule Priority**: When multiple rules match, highest priority executes. Priority assigned manually in rule builder UI (default: 0). Example: VIP bonus (priority 10) overrides welcome bonus (priority 5).

**Rule Testing**: Implement dry-run mode where product team can test rule against sample player data before deploying to production. This prevents accidental $10K bonus giveaways.

**Future Enhancement**: Implement A/B testing for bonus rules (50% players get Rule A, 50% get Rule B, measure conversion). Requires integration with A/B testing framework (P2-21).
