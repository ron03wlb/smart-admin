# SmartAdmin Git Hooks

This directory contains Git hooks for SmartAdmin project automation.

## Pre-Commit Hook

**Purpose**: Automatically validate `.claude/skills/` consistency before each commit.

### Installation

Configure Git to use the custom hooks directory:

```bash
git config core.hooksPath .githooks
```

**On Linux/Mac**, make the hook executable:
```bash
chmod +x .githooks/pre-commit
```

### How It Works

1. **Detects skills-related changes**: Only runs when `.claude/skills/`, `CLAUDE.md`, `.claude/META.md`, or `.claude/skills/VERSIONS.yml` are modified
2. **Runs validation**: Executes `.claude/scripts/validate-skill-consistency.sh`
3. **Blocks invalid commits**: Prevents commits if validation fails

### Validation Checks

- ✅ Skill count consistency (filesystem vs registry vs README)
- ✅ Registry completeness (all skills registered)
- ✅ Config.yml coverage (all skills have config.yml)
- ⚠️ Knowledge coverage statistics (warning only)
- ⚠️ Dependency graph integrity (warning only)

### Bypassing the Hook (Not Recommended)

In emergency situations only:
```bash
git commit --no-verify
```

**Warning**: Bypassing the hook may introduce inconsistencies that will need manual fixing later.

### Testing the Hook

1. Make a test change:
   ```bash
   # Break consistency (for testing)
   sed -i 's/total_skills: 35/total_skills: 33/' .claude/skills/skill-registry.yml
   git add .claude/skills/skill-registry.yml
   git commit -m "test"
   # Should fail with validation error
   ```

2. Fix and retry:
   ```bash
   # Restore correct value
   sed -i 's/total_skills: 33/total_skills: 35/' .claude/skills/skill-registry.yml
   git add .claude/skills/skill-registry.yml
   git commit -m "test"
   # Should succeed
   ```

### Troubleshooting

**Hook not running?**
- Verify: `git config core.hooksPath` should output `.githooks`
- Re-run: `git config core.hooksPath .githooks`

**Hook failing unexpectedly?**
- Run validation manually: `bash .claude/scripts/validate-skill-consistency.sh`
- Check error messages for specific issues

**Need to disable temporarily?**
```bash
# Disable
git config --unset core.hooksPath

# Re-enable
git config core.hooksPath .githooks
```

## Future Hooks

Additional hooks can be added to this directory:
- `pre-push`: Run full test suite before push
- `commit-msg`: Enforce commit message conventions
- `post-merge`: Sync dependencies after merge

---

**Last Updated**: 2026-02-02
**Maintainer**: SmartAdmin Team
