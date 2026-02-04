---
name: concurrency-safety-auditor
description: Concurrency safety audit with risk rating (8 patterns detection)
priority: P1
category: quality
---

# Concurrency Safety Auditor

Audit code for concurrency safety issues with ⭐⭐⭐⭐⭐ risk rating system. Detects 8 common concurrency patterns and generates fix recommendations.

## When to Use

- Checking for race conditions
- Auditing thread safety
- Detecting deadlock patterns
- Reviewing concurrent collections usage

## Patterns Detected

1. **Check-then-act** (non-atomic operations)
2. **Double-checked locking** (broken singleton)
3. **ConcurrentHashMap misuse** (compound actions)
4. **Unsafe publication** (partially constructed objects)
5. **Volatile misuse** (compound operations)
6. **Lock ordering** (potential deadlock)
7. **Thread-local leaks** (memory leaks in pools)
8. **Executor shutdown** (resource leaks)

## Risk Rating Formula

```
Risk Score = (Probability × 0.4) + (Impact × 0.35) + (Actual Harm × 0.25)
```

## Related Rules

- [P04-concurrency-rules.md](../../../rules/technology/patterns/P04-concurrency-rules.md)

## Example Session

**User:** Audit OrderService for concurrency issues

**AI Actions:**
1. Scan for concurrent patterns
2. Identify check-then-act operations
3. Rate risks (⭐⭐⭐⭐⭐)
4. Generate fix recommendations
