# Concurrency Safety Auditor - Quick Reference

**Version**: 1.0.0
**Last Updated**: 2026-02-02
**Skill**: concurrency-safety-auditor (P1 - Extended/Quality)

---

## Command Quick Reference

### Basic Commands

| Command | Purpose | Example |
|---------|---------|---------|
| Full Audit | Audit entire codebase | /concurrency-audit |
| Package Scan | Audit specific package | /concurrency-audit --package net.lab1024.sa.base |
| Severity Filter | High severity only | /concurrency-audit --severity HIGH |
| Generate Report | Create detailed report | /concurrency-audit --report |
| Fix Suggestions | Show fix code examples | /concurrency-audit --show-fixes |

### Rapid Development Workflow

Time estimate: 15-30 minutes per audit

| Step | Action | Time |
|------|--------|------|
| 1. Run Audit | Execute concurrency audit | ~5 min |
| 2. Review Report | Analyze risk ratings and patterns | ~5 min |
| 3. Prioritize Fixes | Focus on HIGH/CRITICAL severity | ~3 min |
| 4. Apply Fixes | Implement suggested fixes | ~10 min |
| 5. Re-audit | Verify fixes | ~5 min |

---

## 8 Concurrency Pattern Detection (Decision Matrix)

### Pattern 1: Check-Then-Act

**Trigger Keywords**: "containsKey", "check then put", "if contains"

**Severity**: ⚠️ HIGH

**Problem**:
```java
// ❌ Non-atomic operations
if (map.containsKey(key)) {  // Check
    map.put(key, value);     // Act (race condition)
}
```

**Fix**:
```java
// ✅ Atomic operation
map.putIfAbsent(key, value);
```

**Risk Formula**:
- Probability: 0.1-5% (depends on concurrency level)
- Impact: 3-7 (data corruption, duplicate processing)
- Actual Harm: 0-5 (observed production issues)

**Detection**: SpotBugs pattern `NP_CHECK_THEN_ACT`

**Time to Fix**: 5-10 minutes

---

### Pattern 2: Double-Checked Locking (DCL)

**Trigger Keywords**: "singleton", "lazy init", "double check"

**Severity**: 🚨 CRITICAL

**Problem**:
```java
// ❌ Missing volatile
private static Singleton instance;

public static Singleton getInstance() {
    if (instance == null) {  // First check
        synchronized (Singleton.class) {
            if (instance == null) {  // Second check
                instance = new Singleton();  // Memory visibility issue!
            }
        }
    }
    return instance;
}
```

**Fix Option 1: Add volatile**
```java
// ✅ Volatile ensures visibility
private static volatile Singleton instance;
```

**Fix Option 2: Holder Pattern (Recommended)**
```java
// ✅ Thread-safe lazy init
private static class Holder {
    static final Singleton INSTANCE = new Singleton();
}

public static Singleton getInstance() {
    return Holder.INSTANCE;
}
```

**Risk Formula**:
- Probability: 1-10% (depends on JVM, CPU)
- Impact: 8-10 (crash, corrupt data)
- Actual Harm: 5-8 (hard to reproduce, severe when occurs)

**Detection**: SpotBugs pattern `DC_DOUBLECHECK`

**Time to Fix**: 10-15 minutes

---

### Pattern 3: Inconsistent Synchronization

**Trigger Keywords**: "synchronized", "inconsistent lock", "mixed access"

**Severity**: 🚨 CRITICAL

**Problem**:
```java
// ❌ Inconsistent synchronization
private int count = 0;

public synchronized void increment() {
    count++;
}

public int getCount() {  // Unsynchronized read!
    return count;
}
```

**Fix**:
```java
// ✅ All accesses synchronized
public synchronized int getCount() {
    return count;
}
```

**Risk Formula**:
- Probability: 5-20% (high concurrency)
- Impact: 7-9 (incorrect business logic)
- Actual Harm: 3-7 (data inconsistency)

**Detection**: SpotBugs pattern `IS_INCONSISTENT_SYNC`

**Time to Fix**: 5-10 minutes

---

### Pattern 4: Non-Atomic Composite Action

**Trigger Keywords**: "i++", "compound operation", "read-modify-write"

**Severity**: ⚠️ MEDIUM

**Problem**:
```java
// ❌ Non-atomic operation
private int counter = 0;

public void increment() {
    counter++;  // Read, modify, write (3 operations)
}
```

**Fix**:
```java
// ✅ Atomic operation
private AtomicInteger counter = new AtomicInteger(0);

public void increment() {
    counter.incrementAndGet();
}
```

**Risk Formula**:
- Probability: 10-30% (high concurrency)
- Impact: 3-5 (counter inaccuracy)
- Actual Harm: 2-4 (acceptable in some cases)

**Detection**: Manual code review + SpotBugs `VO_VOLATILE_INCREMENT`

**Time to Fix**: 10-15 minutes

---

### Pattern 5: Unsafe Publication

**Trigger Keywords**: "this escape", "constructor leak", "early publish"

**Severity**: ⚠️ HIGH

**Problem**:
```java
// ❌ Publishing 'this' in constructor
public class EventListener {
    public EventListener(EventSource source) {
        source.registerListener(this);  // 'this' escapes!
    }
    // Other fields may not be initialized yet
}
```

**Fix**:
```java
// ✅ Factory method pattern
public static EventListener create(EventSource source) {
    EventListener listener = new EventListener();
    source.registerListener(listener);
    return listener;
}

private EventListener() {
    // Private constructor
}
```

**Risk Formula**:
- Probability: 1-5% (subtle bug)
- Impact: 7-9 (unpredictable behavior)
- Actual Harm: 3-6 (rare but severe)

**Detection**: Manual code review

**Time to Fix**: 15-20 minutes

---

### Pattern 6: Unsynchronized Lazy Init

**Trigger Keywords**: "lazy init", "if null then new", "first access create"

**Severity**: ⚠️ HIGH

**Problem**:
```java
// ❌ Race condition in lazy init
private Map<String, Object> cache;

public Map<String, Object> getCache() {
    if (cache == null) {
        cache = new HashMap<>();  // Multiple threads may create
    }
    return cache;
}
```

**Fix Option 1: Holder Pattern**
```java
// ✅ Thread-safe lazy init
private static class CacheHolder {
    static final Map<String, Object> CACHE = new HashMap<>();
}

public Map<String, Object> getCache() {
    return CacheHolder.CACHE;
}
```

**Fix Option 2: Synchronized**
```java
// ✅ Synchronized lazy init
public synchronized Map<String, Object> getCache() {
    if (cache == null) {
        cache = new HashMap<>();
    }
    return cache;
}
```

**Risk Formula**:
- Probability: 5-15% (concurrent access)
- Impact: 5-7 (duplicate objects, memory leak)
- Actual Harm: 2-5 (acceptable performance impact)

**Detection**: SpotBugs pattern `LI_LAZY_INIT_STATIC`

**Time to Fix**: 10-15 minutes

---

### Pattern 7: Wait Not in Loop

**Trigger Keywords**: "wait()", "notify()", "spurious wakeup"

**Severity**: ⚠️ MEDIUM

**Problem**:
```java
// ❌ wait() not in loop (spurious wakeup)
synchronized (lock) {
    if (!condition) {
        lock.wait();  // May wake up without notify()
    }
    // Process...
}
```

**Fix**:
```java
// ✅ wait() in while loop
synchronized (lock) {
    while (!condition) {
        lock.wait();
    }
    // Process...
}
```

**Risk Formula**:
- Probability: 1-5% (spurious wakeups rare)
- Impact: 5-7 (incorrect program state)
- Actual Harm: 2-4 (usually caught by logic)

**Detection**: SpotBugs pattern `WA_NOT_IN_LOOP`

**Time to Fix**: 5-10 minutes

---

### Pattern 8: Empty Synchronized Block

**Trigger Keywords**: "empty sync", "synchronized {}", "useless lock"

**Severity**: ⚠️ LOW

**Problem**:
```java
// ❌ Useless synchronization
synchronized (this) {
    // Empty or only trivial operations
}
```

**Fix**:
```java
// ✅ Remove unnecessary synchronization
// (No sync needed for trivial operations)
```

**Risk Formula**:
- Probability: N/A (no race condition)
- Impact: 1-2 (performance degradation)
- Actual Harm: 0-1 (minimal)

**Detection**: SpotBugs pattern `ESync_EMPTY_SYNC`

**Time to Fix**: 2-5 minutes

---

## Risk Rating System

### Rating Scale

| Rating | Meaning | Criteria | Action |
|--------|---------|----------|--------|
| ⭐⭐⭐⭐⭐ | Production-grade | No known issues | No action needed |
| ⭐⭐⭐⭐ | Good | Minor improvements possible | Optional fix |
| ⭐⭐⭐ | Acceptable | Should improve | Schedule fix |
| ⭐⭐ | Problematic | Must fix soon | Priority fix |
| ⭐ | Dangerous | Fix immediately | Critical fix |

### Risk Score Formula

```
Risk Score = (Probability × 0.4) + (Impact × 0.35) + (Actual Harm × 0.25)
```

**Components**:
- **Probability** (0-10): Likelihood of race condition occurring
  - 0-2: Extremely rare (startup only, single-threaded context)
  - 3-5: Low (infrequent concurrent access)
  - 6-8: Medium (moderate concurrent access)
  - 9-10: High (heavy concurrent access)

- **Impact** (0-10): Severity if race condition occurs
  - 0-2: Minimal (log message lost)
  - 3-5: Moderate (counter inaccuracy)
  - 6-8: Significant (data corruption)
  - 9-10: Critical (crash, security breach)

- **Actual Harm** (0-10): Observed production issues
  - 0: Never observed
  - 1-3: Rare reports
  - 4-6: Occasional issues
  - 7-9: Frequent problems
  - 10: System-wide failure

### Example Calculation

**SmartReloadManager.register()** (Check-then-act):
- Probability: 0.1 (only at startup, Spring single-threaded init)
- Impact: 3 (single reload tag overwrite)
- Actual Harm: 0 (never observed)

**Risk Score** = (0.1 × 0.4) + (3 × 0.35) + (0 × 0.25) = **1.09 / 10** → **⭐⭐ (Low)**

---

## Common Errors and Quick Fixes

### Error 1: False Positive - Immutable Object

**Symptom**: "Inconsistent synchronization detected on immutable object"

**Cause**: Auditor doesn't recognize immutability

**Fix**:
```java
// Add @Immutable annotation or @ThreadSafe comment
/**
 * @ThreadSafe
 */
@Immutable
public final class ImmutableConfig {
    private final String value;
    // ...
}
```

---

### Error 2: Benign Race Condition

**Symptom**: "Check-then-act detected in benign scenario"

**Cause**: Auditor flags all check-then-act, even safe ones

**Example**:
```java
// Benign: Idempotent operation
if (cache.containsKey(key)) {
    cache.put(key, computeExpensiveValue());  // Safe to recompute
}
```

**Fix**: Add suppression comment or refactor to putIfAbsent

---

### Error 3: SpotBugs Configuration Missing

**Symptom**: "SpotBugs detector not found"

**Cause**: Missing SpotBugs dependency or plugin

**Fix**:
```gradle
// build.gradle
plugins {
    id 'com.github.spotbugs' version '5.0.13'
}

spotbugs {
    effort = 'max'
    reportLevel = 'low'
}
```

---

### Error 4: High False Positive Rate

**Symptom**: "Many false positives flagged"

**Cause**: Overly aggressive detection

**Fix**:
```bash
# Adjust severity threshold
/concurrency-audit --severity HIGH  # Only critical issues
```

---

### Error 5: Missing Actual Harm Data

**Symptom**: "Actual harm score is 0 for all patterns"

**Cause**: No production monitoring data

**Fix**:
1. Enable production error tracking (Sentry, APM)
2. Collect concurrency exception logs
3. Update risk assessment with real data

---

## Concurrent Collection Quick Reference

### Collection Selection Matrix

| Scenario | Recommended | Alternatives | Avoid |
|----------|-------------|--------------|-------|
| High concurrent R/W | ConcurrentHashMap | Collections.synchronizedMap | HashMap |
| Read-heavy | CopyOnWriteArrayList | Collections.synchronizedList | ArrayList |
| Ordered access | ConcurrentSkipListMap | TreeMap + sync | TreeMap |
| Queue | ConcurrentLinkedQueue | LinkedBlockingQueue | LinkedList |
| Set | ConcurrentHashMap.newKeySet() | Collections.synchronizedSet | HashSet |

### ConcurrentHashMap Atomic Methods

| Method | Use Case | Example |
|--------|----------|---------|
| putIfAbsent | Add if not exists | `map.putIfAbsent(key, value)` |
| computeIfAbsent | Lazy compute value | `map.computeIfAbsent(key, k -> compute(k))` |
| computeIfPresent | Update if exists | `map.computeIfPresent(key, (k, v) -> update(v))` |
| merge | Merge values | `map.merge(key, 1, Integer::sum)` |
| replace | CAS replace | `map.replace(key, oldVal, newVal)` |

---

## Time Estimates (Production Data)

| Pattern | Detection Time | Fix Time | Verification Time | Total |
|---------|---------------|----------|-------------------|-------|
| Check-Then-Act | 2 min | 5-10 min | 3 min | 10-15 min |
| Double-Checked Locking | 2 min | 10-15 min | 5 min | 17-22 min |
| Inconsistent Sync | 3 min | 5-10 min | 3 min | 11-16 min |
| Non-Atomic Composite | 2 min | 10-15 min | 5 min | 17-22 min |
| Unsafe Publication | 5 min | 15-20 min | 5 min | 25-30 min |
| Unsynchronized Lazy Init | 2 min | 10-15 min | 3 min | 15-20 min |
| Wait Not in Loop | 2 min | 5-10 min | 5 min | 12-17 min |
| Empty Sync Block | 1 min | 2-5 min | 2 min | 5-8 min |

**Full Audit**: 15-30 minutes (depending on codebase size)

---

## SpotBugs Pattern Mapping

| SmartAdmin Pattern | SpotBugs Code | Severity | Documentation |
|-------------------|---------------|----------|---------------|
| Check-Then-Act | `NP_CHECK_THEN_ACT` | HIGH | [Link](https://spotbugs.readthedocs.io/) |
| Double-Checked Locking | `DC_DOUBLECHECK` | CRITICAL | [Link](https://spotbugs.readthedocs.io/) |
| Inconsistent Sync | `IS_INCONSISTENT_SYNC` | CRITICAL | [Link](https://spotbugs.readthedocs.io/) |
| Non-Atomic Composite | `VO_VOLATILE_INCREMENT` | MEDIUM | [Link](https://spotbugs.readthedocs.io/) |
| Unsynchronized Lazy Init | `LI_LAZY_INIT_STATIC` | HIGH | [Link](https://spotbugs.readthedocs.io/) |
| Wait Not in Loop | `WA_NOT_IN_LOOP` | MEDIUM | [Link](https://spotbugs.readthedocs.io/) |
| Empty Sync Block | `ESync_EMPTY_SYNC` | LOW | [Link](https://spotbugs.readthedocs.io/) |

---

## Audit Workflow Checklist

Pre-audit preparation:
- [ ] Install SpotBugs plugin: `./gradlew spotbugsMain`
- [ ] Identify high-concurrency modules (Manager layer, caching, scheduling)
- [ ] Review recent production concurrency exceptions

Audit execution:
- [ ] Run full concurrency audit: `/concurrency-audit`
- [ ] Review generated report (sorted by risk rating)
- [ ] Filter HIGH/CRITICAL severity issues
- [ ] Validate false positives (check @ThreadSafe annotations)

Fix implementation:
- [ ] Prioritize ⭐ and ⭐⭐ issues first
- [ ] Apply suggested fixes (use atomic methods, add volatile)
- [ ] Write concurrency unit tests (ExecutorService + CountDownLatch)
- [ ] Re-run audit to verify fixes

Post-fix verification:
- [ ] Run ArchUnit tests: `./gradlew :smartadmin-app:test --tests ArchitectureTest`
- [ ] Run full test suite: `./gradlew test`
- [ ] Update risk assessment with new ratings
- [ ] Document lessons learned

---

**See Also**:
- [Examples Guide](examples.md) - Real SmartAdmin concurrency fixes (SmartReloadManager)
- [Concurrency Patterns Reference](../references/concurrency-patterns.md) - Detailed pattern explanations
- [SpotBugs Documentation](https://spotbugs.readthedocs.io/) - Official bug descriptions
- [Java Concurrency in Practice](https://jcip.net/) - Authoritative concurrency guide
