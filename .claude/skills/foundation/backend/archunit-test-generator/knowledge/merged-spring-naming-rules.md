# Merged Rules: Spring Pattern + Naming Convention
# Merged from: spring-pattern-checker + naming-convention-checker (2026-03-27)

---

## Spring Pattern Rules (4 Core Rules)

### Rule 1: @Transactional Placement (CRITICAL)
- `@Transactional` ONLY in Manager layer (classes ending with `*Manager`)
- NEVER in Service, Controller, or Dao
- Must use `@Transactional(rollbackFor = Throwable.class)`
- `@Cacheable`/`@CacheEvict` also Manager-only

### Rule 2: Dependency Injection (CRITICAL)
- Constructor injection ONLY: `@RequiredArgsConstructor` + `private final`
- NEVER `@Autowired` field injection or setter injection

### Rule 3: Layered Architecture Calls (CRITICAL)
- Controller -> Service ONLY (cannot skip to Manager/Dao)
- Service -> Manager OR Dao (direct Dao OK for single-table CRUD)
- Manager -> Dao ONLY (never upward to Service)
- Service -> Service FORBIDDEN (extract to Manager)

### Rule 4: Spring Bean Naming (HIGH)
- `@RestController` -> `*Controller`
- `@Service` (business) -> `*Service`
- `@Service` (transaction/cache) -> `*Manager`
- `@Repository` / Mapper -> `*Dao`

## Quick Fix Matrix

| Violation | Fix | Time |
|-----------|-----|------|
| @Transactional in Service | Move to Manager | 5-10 min |
| @Autowired field injection | @RequiredArgsConstructor + final | 2-5 min |
| Controller -> Manager | Add Service layer | 10 min |
| Service -> Service | Extract to Manager | 15 min |
| Manager -> Service | Call Dao directly | 5 min |
| Wrong bean naming | Rename class | 2 min |

---

## Naming Convention Rules

### Table Name: Singular Only (BLOCKER)
- All `@TableName` values must be singular: `t_player` NOT `t_players`
- Irregular plurals: `categories` -> `category`, `entries` -> `entry`

### Exemptions (Uncountable Nouns - PASS)
- `t_*_metrics` / `t_metrics_*` -> PASS
- `t_*_statistics` / `t_statistics_*` -> PASS
- `t_*_analytics` / `t_analytics_*` -> PASS

### ArchUnit Test Template

```java
class TableNameSingularCondition extends ArchCondition<JavaClass> {
    @Override
    public void check(JavaClass javaClass, ConditionEvents events) {
        TableName annotation = javaClass.getAnnotation(TableName.class);
        String tableName = annotation.value();
        if (isExempt(tableName)) return;
        if (isPlural(tableName)) {
            events.add(SimpleConditionEvent.violated(javaClass,
                String.format("Table '%s' in %s should be singular", tableName, javaClass.getName())));
        }
    }
    private boolean isExempt(String name) {
        return name.matches("t_.*_(metrics|statistics|analytics)");
    }
    private boolean isPlural(String name) {
        return (name.endsWith("s") && !name.endsWith("ss"))
            || name.matches(".*(categories|entries|children|people).*");
    }
}
```

---

## References
- `CLAUDE.md`
- `CLAUDE.md`
- `CLAUDE.md`
- `.claude/shared/knowledge/smartadmin-patterns.md`
