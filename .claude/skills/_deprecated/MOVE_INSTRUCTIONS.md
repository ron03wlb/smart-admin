# Instructions for Completing Directory Move

## Background

During the CRUD Pipeline Consolidation (v2.0.0), we encountered Windows file permission issues when attempting to move deprecated skill directories. This file contains instructions for completing the move when file locks are resolved.

## Directories to Move

The following skill directories need to be moved from `.claude/skills/` to `.claude/skills/_deprecated/`:

1. `smartadmin-mybatis/`
2. `smartadmin-vue-crud/`
3. `smartadmin-api-docs/`

## Prerequisites

Before moving, ensure:
- [ ] All files in these directories are closed in VSCode and other editors
- [ ] No processes are running that might lock these files
- [ ] You have completed any pending work that references these directories

## Move Commands

### Option 1: Using Git (Recommended)

```bash
cd C:\Workspace\open_source\smart-admin

# Move all three directories
git mv .claude/skills/smartadmin-mybatis .claude/skills/_deprecated/
git mv .claude/skills/smartadmin-vue-crud .claude/skills/_deprecated/
git mv .claude/skills/smartadmin-api-docs .claude/skills/_deprecated/

# Verify the move
git status

# Commit the move
git add .
git commit -m "refactor(skills): Move deprecated CRUD skills to _deprecated/

- Moved smartadmin-mybatis to _deprecated/ (consolidated into smartadmin-crud-generator phase-1)
- Moved smartadmin-vue-crud to _deprecated/ (consolidated into smartadmin-crud-generator phase-2)
- Moved smartadmin-api-docs to _deprecated/ (consolidated into smartadmin-crud-generator phase-3)

Related: CRUD Pipeline Consolidation v2.0.0"
```

### Option 2: Using Bash (Alternative)

```bash
cd /c/Workspace/open_source/smart-admin/.claude/skills

# Move directories
mv smartadmin-mybatis _deprecated/
mv smartadmin-vue-crud _deprecated/
mv smartadmin-api-docs _deprecated/

# Verify
ls -la _deprecated/

# Stage the changes in git
cd /c/Workspace/open_source/smart-admin
git add .claude/skills/_deprecated/
git add .claude/skills/  # Removes old directories from tracking
git commit -m "refactor(skills): Move deprecated CRUD skills to _deprecated/"
```

### Option 3: Using Windows File Explorer (Fallback)

If command-line approaches fail:

1. Close VSCode and all other applications that might have these directories open
2. Use Windows File Explorer to manually drag and drop:
   - `C:\Workspace\open_source\smart-admin\.claude\skills\smartadmin-mybatis` → `_deprecated\`
   - `C:\Workspace\open_source\smart-admin\.claude\skills\smartadmin-vue-crud` → `_deprecated\`
   - `C:\Workspace\open_source\smart-admin\.claude\skills\smartadmin-api-docs` → `_deprecated\`
3. Re-open VSCode
4. Stage and commit the changes in git

## Verification

After moving, verify:

```bash
# Check _deprecated directory contains the moved skills
ls .claude/skills/_deprecated/
# Expected: smartadmin-mybatis  smartadmin-vue-crud  smartadmin-api-docs  README.md  MOVE_INSTRUCTIONS.md

# Check main skills directory no longer contains them
ls .claude/skills/ | grep -E "(mybatis|vue-crud|api-docs)"
# Expected: (no output)

# Verify git status
git status
# Should show renamed/moved files
```

## Post-Move Cleanup

After successfully moving:

1. Delete this instruction file:
   ```bash
   rm .claude/skills/_deprecated/MOVE_INSTRUCTIONS.md
   ```

2. Update the _deprecated/README.md to remove the "Note" about pending directory move

3. Verify backward compatibility still works:
   ```bash
   # Test that skill-aliases.json still routes correctly
   # (Manual testing in Claude Code CLI)
   ```

## Troubleshooting

### "Permission denied" error

**Cause**: Files are locked by VSCode, file system indexing, or antivirus software

**Solutions**:
1. Close VSCode completely
2. Wait 30 seconds for file handles to release
3. Temporarily disable antivirus scanning on the project directory
4. Restart Windows Explorer: `taskkill /f /im explorer.exe && start explorer.exe`

### "Directory not empty" error

**Cause**: Nested .git files or symlinks

**Solution**:
```bash
# Check for hidden files
ls -la .claude/skills/smartadmin-mybatis/

# If safe, force move
mv -f smartadmin-mybatis _deprecated/
```

### Git shows "deleted" instead of "renamed"

**Cause**: Git didn't detect the move as a rename

**Solution**:
```bash
# Stage as rename explicitly
git add _deprecated/smartadmin-mybatis
git rm -r smartadmin-mybatis
```

---

**Created**: 2026-01-27
**Status**: Pending file lock resolution
**Priority**: Medium (can be deferred to next development session)
