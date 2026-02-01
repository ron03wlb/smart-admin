# Trigger Keywords Template

This template provides a standardized format for documenting skill trigger keywords.

## Template Structure

```markdown
## Trigger Keywords

This skill is automatically activated when the user's request contains:

**Primary Keywords** (High confidence):
- "keyword1" - Specific use case description
- "keyword2" - Specific use case description

**Secondary Keywords** (Medium confidence):
- "keyword3" - Context required
- "keyword4" - Context required

**Phrase Patterns**:
- "I need to [verb] + [domain]" - Example: "I need to generate CRUD for User entity"
- "How do I [action]" - Example: "How do I add ArchUnit test"

**Example User Requests**:
```
User: "keyword1 for Employee module"
User: "I need to keyword2"
```

**Note**: This skill can also be manually invoked via `/skill-name` command.
```

## Usage Instructions

1. **Extract from config.yml**: Read skill's `config.yml` to get existing triggers
2. **Analyze SKILL.md**: Review "Quick Start" / "Use Cases" sections for implicit triggers
3. **Position**: Add "## Trigger Keywords" section after "## Quick Start" in SKILL.md
4. **Customize**: Replace placeholder keywords with actual skill-specific triggers
5. **Verify**: Ensure no keyword conflicts with other skills

## Example

For `archunit-test-generator` skill:

```markdown
## Trigger Keywords

This skill is automatically activated when the user's request contains:

**Primary Keywords** (High confidence):
- "ArchUnit" - ArchUnit test generation or architecture enforcement
- "architecture test" - Generate tests for layered architecture validation
- "layer dependency" - Test layer dependencies (Controller → Service → Dao)

**Secondary Keywords** (Medium confidence):
- "enforce architecture" - Context: adding architectural constraints
- "test architecture" - Context: validating SmartAdmin patterns

**Phrase Patterns**:
- "I need to add ArchUnit test for [module]" - Example: "I need to add ArchUnit test for User module"
- "How do I enforce [architectural rule]" - Example: "How do I enforce transaction placement rules"

**Example User Requests**:
```
User: "Add ArchUnit test to validate Service layer dependencies"
User: "I need to enforce that @Transactional only appears in Manager layer"
User: "Generate architecture test for the Employee module"
```

**Note**: This skill can also be manually invoked via `/archunit-test-generator` command.
```

## Version History

- **v1.0.0** (2026-02-01): Initial template creation
