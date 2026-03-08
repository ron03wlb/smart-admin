# SmartAdmin Spring Skill - Installation & Usage Guide

## Installation Status

✅ **Installed Successfully**

```
.agents/skills/spring/
├── SKILL.md                              (Main skill file - 8.5KB)
├── README.md                             (Quick reference - 5.1KB)
├── INSTALL.md                            (This file)
└── references/
    ├── spring-rules-detailed.md          (Detailed rules - 25KB)
    └── quick-fix-guide.md                (Quick fixes - 11KB)
```

**Total Size**: ~50KB of comprehensive SmartAdmin Spring pattern knowledge

## Verification

Check if skill is properly installed:

```bash
# Method 1: List all skills
ls -la .agents/skills/ | grep spring

# Method 2: Check SKILL.md exists
test -f .agents/skills/spring/SKILL.md && echo "✅ Skill installed" || echo "❌ Skill missing"

# Method 3: Check skill metadata
head -5 .agents/skills/spring/SKILL.md
```

Expected output:
```yaml
---
name: spring
description: SmartAdmin Spring Pattern Checker - Validates code compliance...
---
```

## Usage

### Basic Usage

```bash
# Check current git changes
/spring

# Check specific module
/spring smartadmin-modules/smartadmin-system/src/main/java/net/lab1024/sa/system/

# Check specific file
/spring path/to/UserService.java
```

### When to Use

**Automatic triggers** (handled by Claude):
- After creating/modifying Spring Bean classes
- After code-reviewer agent completes
- When ArchitectureTest.java fails

**Manual triggers** (you call it):
- Before committing code with Spring annotations
- During refactoring of service/manager layers
- When unsure about layered architecture rules
- To learn SmartAdmin Spring conventions

### Example Session

```bash
# Scenario: You just created a new EmployeeService

user: I created a new EmployeeService with transaction logic

claude: Let me review the implementation...
        [reads code]
        Now running /spring to check SmartAdmin Spring compliance...
        [generates report]

        Found 2 violations:
        1. CRITICAL - @Transactional in Service layer (should be Manager)
        2. CRITICAL - Field injection with @Autowired (should be constructor)

        Here's how to fix...
```

## What It Checks

### 🚨 CRITICAL Rules (Must Fix)
1. **@Transactional Placement** - Only in Manager layer
2. **Dependency Injection** - Constructor injection only
3. **Layered Architecture** - Controller → Service → Manager → Dao

### ⚠️ HIGH Priority (Fix Before Merge)
4. **Spring Bean Naming** - Proper suffixes (Controller, Service, Manager, Dao)

## Integration with ArchitectureTest

This skill complements `ArchitectureTest.java`:

| Feature | ArchUnit | /spring |
|---------|----------|---------|
| Detects violations | ✅ | ✅ |
| Compile-time check | ✅ | ❌ |
| Runtime check | ❌ | ✅ |
| Detailed fixes | ❌ | ✅ |
| Before/after code | ❌ | ✅ |
| Doc links | ❌ | ✅ |

**Best practice**: Run both!

```bash
# 1. Run ArchUnit tests
./gradlew :smartadmin-app:test --tests ArchitectureTest

# 2. If failures, run /spring for detailed fixes
/spring
```

## Documentation Structure

### SKILL.md (8.5KB)
**Purpose**: Main skill instructions for Claude
**When to read**: Claude reads this automatically when skill triggers
**Contains**: Core rules, detection logic, report format

### README.md (5.1KB)
**Purpose**: Quick reference for humans
**When to read**: Need quick overview or usage examples
**Contains**: Quick start, examples, FAQ

### references/spring-rules-detailed.md (25KB)
**Purpose**: Comprehensive rule specifications
**When to read**: Need deep understanding of a specific rule
**Contains**: Detailed violations, rationale, detection logic

### references/quick-fix-guide.md (11KB)
**Purpose**: Fast fix templates
**When to read**: Have violations and need quick fixes
**Contains**: Step-by-step fixes for common violations

## Troubleshooting

### Skill Not Triggering

**Problem**: `/spring` command not recognized

**Solution**:
```bash
# Check skill is installed
ls -la .agents/skills/spring/SKILL.md

# Check YAML frontmatter
head -5 .agents/skills/spring/SKILL.md

# Verify name field matches
# Should be: name: spring
```

### Wrong Violations Reported

**Problem**: Skill reports false positives

**Solution**:
1. Check if code follows SmartAdmin conventions
2. Review `.agent/rules/` for official standards
3. Run ArchitectureTest to confirm:
   ```bash
   ./gradlew :smartadmin-app:test --tests ArchitectureTest
   ```
4. If ArchUnit passes but /spring fails, report issue

### Need More Detail

**Problem**: Violation report unclear

**Solution**:
```bash
# Read detailed rule specification
cat .agents/skills/spring/references/spring-rules-detailed.md | grep -A 50 "Rule 1:"

# Read quick fix guide
cat .agents/skills/spring/references/quick-fix-guide.md | grep -A 30 "@Transactional"
```

## Updating the Skill

If SmartAdmin rules change:

1. Update `SKILL.md` - Core rules
2. Update `references/spring-rules-detailed.md` - Detailed specs
3. Update `references/quick-fix-guide.md` - Fix templates
4. Test with sample violations
5. Run ArchitectureTest to verify alignment

## Support

**Questions?**
- Check [README.md](./README.md) for FAQ
- Review [SmartAdmin Patterns](../../.claude/shared/knowledge/smartadmin-patterns.md)
- See [Manager Layer Rules](../../.agent/foundation/F03-manager-layer.md)

**Issues?**
- Verify ArchitectureTest rules match skill rules
- Check git branch (rules may differ across versions)
- Review project CLAUDE.md for current standards

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2026-01-23 | Initial release with 4 core rules |

## License

Part of SmartAdmin framework - see project LICENSE

---

**Skill Author**: SmartAdmin Architecture Team  
**Last Updated**: 2026-01-23  
**Compatible with**: SmartAdmin v4.0.0+
