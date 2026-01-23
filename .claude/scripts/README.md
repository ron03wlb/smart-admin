# AI Documentation Validation Scripts

**Purpose**: Automated tools to maintain AI documentation quality and prevent regressions.

**Last Updated**: 2026-01-24
**Version**: 1.0.0

---

## Available Scripts

### 1. validate-cross-references.sh

**Purpose**: Validate all markdown cross-references (links) in AI documentation.

**Usage**:
```bash
./.claude/scripts/validate-cross-references.sh
```

**What it checks**:
- All markdown links in CLAUDE.md, .claude/, .agent/
- Skips external links (http://, https://, mailto:)
- Skips anchor-only links (#section)
- Reports broken links with file paths

**Exit codes**:
- 0 = All links valid
- 1 = Broken links found

**Example output**:
```
Checking: CLAUDE.md
  ✓ README.md
  ✗ docs/missing.md (BROKEN)

Summary
Total links checked: 395
Valid links: 348
Broken links: 47

❌ Validation FAILED
```

**Compatibility**: macOS + Linux (uses sed, not grep -P)

---

### 2. check-duplication.sh

**Purpose**: Detect duplicated content blocks across AI documentation.

**Usage**:
```bash
./.claude/scripts/check-duplication.sh
```

**What it checks**:
- Code blocks (```...```) across all markdown files
- Long paragraphs (3+ consecutive lines)
- Ignores short phrases (<50 chars for code, <150 chars for paragraphs)
- Ignores cross-references (acceptable duplication)
- Uses MD5 hashing to detect exact duplicates

**Detection algorithm**:
1. Scan all markdown files in CLAUDE.md, .claude/, .agent/
2. Extract code blocks (between ``` delimiters)
3. Extract paragraphs (3+ consecutive non-empty lines)
4. Normalize whitespace for each block
5. Hash each block with MD5
6. Report blocks with matching hashes in different files

**Exit codes**:
- 0 = No duplication detected
- 1 = Duplication found

**Example output**:
```
Scanning files...
Found 87 markdown files in CLAUDE.md, .claude/, .agent/

Checking for duplicated code blocks...
  ✗ DUPLICATE FOUND:
    File: CLAUDE.md:45-52 (8 lines)
    File: .claude/shared/knowledge/smartadmin-patterns.md:120-127 (8 lines)

Checking for duplicated paragraphs...
  ✓ No duplicated paragraphs

Summary
Total blocks checked: 234
Duplicates found: 1

❌ DUPLICATION DETECTED
Please consolidate duplicated content into single source of truth.
See CONTENT_MAP.md for ownership rules.
```

**Compatibility**: macOS + Linux (uses portable sed/grep/awk)

---

## CI Integration

**GitHub Actions**: `.github/workflows/validate-docs.yml`

**Triggers**:
- Pull requests modifying CLAUDE.md, .claude/, or .agent/
- Manual workflow dispatch

**Jobs**:
1. **validate-docs** (blocking):
   - Cross-reference validation
   - Duplication check
   - Both must pass for PR to be mergeable
2. **markdown-lint** (optional):
   - Markdown linting with markdownlint-cli2
   - Non-blocking (continue-on-error: true)
   - Skips if .markdownlint.json not found

**How to run manually**:
1. Go to Actions tab in GitHub
2. Select "AI Documentation Validation" workflow
3. Click "Run workflow"
4. Select branch and click "Run workflow" button

---

## Development Workflow

### Before Committing Changes

**Run validation locally**:
```bash
# Check cross-references
./.claude/scripts/validate-cross-references.sh

# Check for duplication
./.claude/scripts/check-duplication.sh

# Both should exit 0 before committing
echo $?  # Should print 0
```

**Quick validation (both checks)**:
```bash
./.claude/scripts/validate-cross-references.sh && ./.claude/scripts/check-duplication.sh && echo "✅ All checks passed"
```

### If Validation Fails

**Broken links**:
- Check file paths are correct (case-sensitive on Linux)
- Verify files exist in repository
- Update links to match new file locations
- Use absolute paths from repository root

**Example fix**:
```markdown
# ❌ Broken
[Guide](.claude/guides/missing.md)

# ✅ Fixed
[Guide](.claude/shared/knowledge/smartadmin-patterns.md)
```

**Duplication detected**:
1. Identify which file is the source of truth (see CONTENT_MAP.md)
2. Remove duplicated content from secondary files
3. Replace with cross-reference link to source

**Example fix**:
```markdown
# ❌ Duplicated content in CLAUDE.md
ResponseDTO is the standard response wrapper...
(8 lines of duplicated content)

# ✅ Cross-reference instead
For ResponseDTO usage, see [SmartAdmin Patterns](.claude/shared/knowledge/smartadmin-patterns.md#responsedto-pattern).
```

---

## Maintenance

### Adding New Scripts

1. Create script in `.claude/scripts/` directory
2. Add shebang: `#!/bin/bash`
3. Make executable: `chmod +x script.sh`
4. Test on macOS and Linux (if available)
5. Document in this README.md
6. Add to CI workflow if appropriate

**Template**:
```bash
#!/bin/bash

# script-name.sh
# Brief description
# Compatible with macOS and Linux
#
# Version: 1.0.0
# Last Updated: YYYY-MM-DD

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "Script output here..."

# Exit 0 for success, 1 for failure
exit 0
```

### Updating Existing Scripts

1. Test changes locally
2. Verify macOS + Linux compatibility
3. Update documentation if behavior changes
4. Update version in script header
5. Update "Last Updated" date in this README

### Version Management

**Script versions**: Independent versioning in each script header
**README version**: Updated when adding/removing scripts or changing workflow

---

## Troubleshooting

### Script Permission Denied

**Problem**: `bash: ./.claude/scripts/check-duplication.sh: Permission denied`

**Solution**:
```bash
chmod +x .claude/scripts/*.sh
```

**Verification**:
```bash
ls -la .claude/scripts/*.sh
# Should show: -rwxr-xr-x (executable)
```

### macOS vs Linux Differences

**Problem**: Script works on Linux but fails on macOS
**Cause**: macOS uses BSD sed/grep, Linux uses GNU sed/grep

**Common issues**:

| Issue | Linux (GNU) | macOS/BSD (Portable) |
|-------|-------------|----------------------|
| Extended regex | `grep -E` or `grep -P` | `grep -E` only (no -P) |
| In-place edit | `sed -i 's/old/new/'` | `sed -i '' 's/old/new/'` |
| MD5 hashing | `md5sum` | `md5` |

**Solution**: Use portable syntax (avoid -P flag, use sed instead of grep for complex regex)

**Example**:
```bash
# ❌ Linux only (grep -P)
grep -oP '\[([^\]]+)\]\(([^)]+)\)' file.md

# ✅ Portable (sed)
sed -n 's/.*\[\([^]]*\)\](\([^)]*\)).*/\2/p' file.md
```

### CI Workflow Fails Locally Works

**Problem**: Scripts pass locally but fail in GitHub Actions

**Possible causes**:
1. **File permissions**: Scripts not executable in repository
   ```bash
   git ls-files --stage .claude/scripts/*.sh
   # Should show: 100755 (executable)

   # Fix:
   git update-index --chmod=+x .claude/scripts/*.sh
   ```

2. **Line endings**: CRLF vs LF
   ```bash
   # Check line endings
   file .claude/scripts/*.sh
   # Should show: "ASCII text" not "ASCII text, with CRLF line terminators"

   # Fix:
   git config core.autocrlf input
   dos2unix .claude/scripts/*.sh  # If available
   ```

3. **Path differences**: Relative vs absolute paths
   - Use `./` prefix for scripts in CI
   - Use absolute paths for file references

### No Markdown Files Found

**Problem**: `Found 0 markdown files in CLAUDE.md, .claude/, .agent/`

**Causes**:
- Script not run from repository root
- Incorrect find patterns

**Solution**:
```bash
# Run from repository root
cd /path/to/smart-admin
./.claude/scripts/check-duplication.sh

# Verify files exist
find . -type f \( -name "CLAUDE.md" -o -path "./.claude/*.md" -o -path "./.agent/*.md" \)
```

---

## Performance

**Script performance** (approximate, varies by repository size):

| Script | Files Scanned | Typical Runtime |
|--------|---------------|-----------------|
| validate-cross-references.sh | ~100 | 2-5 seconds |
| check-duplication.sh | ~100 | 5-10 seconds |

**Optimization tips**:
- Scripts use temporary files (not in-memory) for large datasets
- Temp files automatically cleaned up on exit
- Parallel processing not implemented (scripts are fast enough)

---

## Future Enhancements

**Planned improvements**:
- [ ] Fuzzy duplicate detection (near-matches, not just exact)
- [ ] Configurable thresholds (.claude/scripts/config.sh)
- [ ] JSON output format for tooling integration
- [ ] Performance optimization for large repositories (1000+ files)
- [ ] Whitelist for acceptable duplicates
- [ ] Integration with pre-commit hooks

**See**: [.claude/META.md](../.claude/META.md) for roadmap and contribution guidelines

---

## Contact

**Issues**: Report via GitHub Issues (include script output and error messages)

**Improvements**: See [.claude/META.md](../.claude/META.md) for contribution process

---

**Document Version**: 1.0.0
**Last Updated**: 2026-01-24
