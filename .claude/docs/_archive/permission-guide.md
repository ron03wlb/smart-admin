# Permission Configuration Guide

This guide explains the permission system in `.claude/settings.local.json` and the security rationale behind each permission.

**Version:** 2.3.0
**Last Updated:** 2026-01-21

## Overview

The SmartAdmin Claude Code configuration uses consolidated permission patterns to allow necessary development operations while maintaining security boundaries.

**Philosophy:** Grant minimal necessary permissions, use wildcards carefully, document rationale.

## Permission Patterns

### 1. Version Control (`git *:*`)

**Rationale:** Essential for code commits, branch management, status checks, repository operations
**Scope:** All git commands (read and write)
**Risk:** Medium - can modify history, push code, but tracked and recoverable via git history
**Mitigation:** Git pre-commit hooks validate changes, all operations logged in git history

**Typical Usage:**
```bash
git status
git add .
git commit -m "feat: add new feature"
git push origin feature-branch
git log --oneline
git diff HEAD~1
git branch -a
git checkout -b new-feature
```

**Why wildcard:** Git has 100+ subcommands, listing individually is impractical. All git operations are reversible via history.

---

### 2. Build Tools

#### Gradle Unix (`./gradlew *:*`)

**Rationale:** Required for compiling, testing, running SmartAdmin backend on Unix-like systems
**Scope:** All Gradle commands via wrapper
**Risk:** Low - operates in project sandbox, wrapper ensures version consistency
**Mitigation:** Gradle wrapper ensures consistent version, build files version controlled

**Typical Usage:**
```bash
./gradlew clean build
./gradlew :sa-admin:test
./gradlew :sa-admin:bootRun
./gradlew :sa-base:compileJava
./gradlew test --tests ArchitectureTest
```

#### Gradle Windows (`gradlew.bat *:*`)

**Rationale:** Same as Unix Gradle, but for Windows systems
**Scope:** All Gradle commands via Windows batch wrapper
**Risk:** Low - same as Unix version
**Mitigation:** Same as Unix Gradle

**Typical Usage:**
```bash
gradlew.bat clean build
gradlew.bat :sa-admin:bootRun
gradlew.bat test
```

**Cross-platform:** Both patterns needed to support development on Windows and Unix-like systems.

---

### 3. Container Management

#### Docker (`docker *:*`)

**Rationale:** Needed for PostgreSQL, Redis, and development environment containers
**Scope:** All Docker commands
**Risk:** Medium - can affect containers and images, resource consumption
**Mitigation:** Docker daemon permissions, network isolation, container sandboxing

**Typical Usage:**
```bash
docker ps
docker ps -a
docker images
docker run -d --name smartadmin-postgres postgres:15
docker logs smartadmin-postgres
docker exec -it smartadmin-postgres psql -U postgres
docker stop smartadmin-postgres
docker rm smartadmin-postgres
```

#### Docker Compose (`docker-compose *:*`)

**Rationale:** Orchestrates multi-container development environment (database, cache, application)
**Scope:** All docker-compose commands
**Risk:** Medium - manages multiple containers, can affect system resources
**Mitigation:** docker-compose.yml version controlled, container isolation

**Typical Usage:**
```bash
docker-compose up -d
docker-compose down
docker-compose logs postgres
docker-compose ps
docker-compose restart redis
```

**Why separate from docker:** Some environments use standalone docker-compose command, not integrated into docker CLI.

---

### 4. System Utilities

#### Network Inspection (`netstat *:*`)

**Rationale:** Check port conflicts, service availability, network connections
**Scope:** Network statistics and connections (read-only)
**Risk:** Very low - read-only system information, no modifications possible
**Mitigation:** None needed - purely informational

**Typical Usage:**
```bash
netstat -ano | findstr :1024    # Windows: Check if port 1024 in use
netstat -tuln | grep :1024      # Unix: Check if port 1024 in use
netstat -ano                    # View all connections
```

#### HTTP Client (`curl *:*`)

**Rationale:** Test API endpoints, health checks, download resources
**Scope:** HTTP/HTTPS requests
**Risk:** Low - can make network requests, but limited to HTTP protocol
**Mitigation:** Network firewall, no direct system modification

**Typical Usage:**
```bash
curl http://localhost:1024/actuator/health
curl -X POST http://localhost:1024/api/employee/query -H "Content-Type: application/json" -d '{}'
curl -I https://example.com
```

---

### 5. Text Search (Cross-Platform)

#### Windows Search (`findstr *:*`)

**Rationale:** Code search, log analysis on Windows systems
**Scope:** File content search (Windows)
**Risk:** Very low - read-only operations
**Mitigation:** None needed - purely informational

**Typical Usage:**
```bash
findstr /s "Controller" *.java
findstr /i "error" application.log
findstr /n "TODO" src\*.java
```

#### Unix Search (`grep *:*`)

**Rationale:** Code search, log analysis on Unix-like systems
**Scope:** File content search (Unix)
**Risk:** Very low - read-only operations
**Mitigation:** None needed - purely informational

**Typical Usage:**
```bash
grep -r "Controller" src/
grep -i "error" application.log
grep -n "TODO" src/**/*.java
```

**Cross-platform:** Both patterns needed to support development on Windows and Unix-like systems.

---

### 6. File Operations (Cross-Platform)

#### Windows Directory Listing (`dir *:*`)

**Rationale:** File navigation, directory structure inspection on Windows
**Scope:** Directory listing (Windows)
**Risk:** Very low - read-only
**Mitigation:** None needed - purely informational

**Typical Usage:**
```bash
dir /s /b *.java
dir /ad src
dir /od build
```

#### Unix Directory Listing (`ls *:*`)

**Rationale:** File navigation, directory structure inspection on Unix-like systems
**Scope:** Directory listing (Unix)
**Risk:** Very low - read-only
**Mitigation:** None needed - purely informational

**Typical Usage:**
```bash
ls -la src/
ls -R src/
ls -lt build/
```

#### Unix Find (`find *:*`)

**Rationale:** Recursive file search on Unix-like systems
**Scope:** File search (Unix)
**Risk:** Very low - read-only operations
**Mitigation:** None needed - purely informational

**Typical Usage:**
```bash
find src/ -name "*.java"
find . -type f -mtime -1
find src/ -name "Controller.java"
```

#### Windows Delete (`del *:*`)

**Rationale:** Clean build artifacts, temporary files on Windows
**Scope:** File deletion (Windows)
**Risk:** Medium - can delete important files
**Mitigation:** Use with caution, prefer specific patterns, git tracks deletions

**Typical Usage:**
```bash
del /q build\classes\*.class
del /s /q target\*
del temp\*.log
```

**⚠️ Warning:** Deletion is irreversible for untracked files. Use carefully.

#### Unix Delete (`rm *:*`)

**Rationale:** Clean build artifacts, temporary files on Unix-like systems
**Scope:** File deletion (Unix)
**Risk:** Medium - can delete important files
**Mitigation:** Use with caution, avoid `rm -rf /`, git tracks deletions

**Typical Usage:**
```bash
rm build/classes/*.class
rm -rf target/
rm temp/*.log
```

**⚠️ Warning:** `rm -rf` can be destructive. Use with extreme caution.

**Cross-platform:** Both `del` and `rm` patterns needed for cross-platform file cleanup.

---

### 7. Ralph Loop (`claude.loops: 100`)

**Rationale:** Allows Ralph Loop plugin to iterate up to 100 times for complex multi-step tasks
**Scope:** Maximum iterations for automated task loops
**Risk:** Low - computational overhead, but time-bounded
**Mitigation:** Loop limit prevents infinite loops

**Typical Usage:**
- Complex refactoring tasks requiring multiple iterations
- Multi-file code generation with dependencies
- Iterative optimization workflows

**Why 100:** Balances capability (handle complex tasks) with safety (prevent runaway loops).

---

## Cross-Platform Considerations

The configuration supports both Windows and Unix-like systems by including patterns for both operating systems:

| Operation | Windows | Unix |
|-----------|---------|------|
| Build tool | `gradlew.bat` | `./gradlew` |
| Directory listing | `dir` | `ls` |
| File search | `findstr` | `grep` |
| Recursive find | N/A | `find` |
| File deletion | `del` | `rm` |

**Why both:** Enables seamless collaboration across team members using different operating systems.

---

## Security Boundaries

### What's Allowed ✅

- **Project-scoped operations:** Build, test, run within project directory
- **Version control operations:** All git commands (tracked and recoverable)
- **Read-only system inspection:** netstat, dir, ls, findstr, grep, find
- **Container management:** docker, docker-compose (isolated environment)
- **File operations within project:** Cleanup, deletion of build artifacts
- **HTTP requests:** API testing, health checks

### What's NOT Allowed ❌

- **System-wide modifications:** No `sudo`, `chmod`, `chown`, system package management
- **Network services:** No `netcat`, `telnet`, `ssh` server operations
- **Process manipulation:** No `kill`, `pkill`, `killall` (except container processes)
- **Environment variable changes:** No persistent `setx`, `export`
- **Credential access:** No reading `~/.ssh/`, `~/.aws/`, sensitive system files
- **Privilege escalation:** No `sudo`, `su`, `runas`
- **System service management:** No `systemctl`, `service`, `sc`
- **Kernel/driver operations:** No `modprobe`, `insmod`

**Why restricted:** These operations can compromise system security, affect other users, or cause irreversible damage.

---

## How to Add New Permissions

### Step-by-Step Process

1. **Identify the specific need**
   ```
   Example: Need npm commands for frontend development
   ```

2. **Choose appropriate scope:**
   - **Specific:** `Bash(npm install:*)` - Only npm install
   - **Moderate:** `Bash(npm run *:*)` - All npm run scripts
   - **Broad:** `Bash(npm *:*)` - All npm commands (use if justified)

3. **Assess risk and mitigation:**
   ```
   Risk: Medium - can install packages, modify node_modules
   Mitigation: package.json/package-lock.json version control, audit logs
   ```

4. **Update `.claude/settings.local.json`:**
   ```json
   {
     "allowedCommands": {
       "patterns": [
         // ... existing patterns ...

         // Package management (Frontend)
         "Bash(npm *:*)",
         "Bash(pnpm *:*)",
         "Bash(yarn *:*)"
       ]
     }
   }
   ```

5. **Document in this guide:**
   ```markdown
   ### 8. Package Management

   #### npm (`npm *:*`)

   **Rationale:** Required for frontend dependency management and build
   **Scope:** All npm commands
   **Risk:** Medium - can install packages, modify node_modules
   **Mitigation:** package.json/package-lock.json version control, npm audit

   **Typical Usage:**
   ```bash
   npm install
   npm run dev
   npm run build
   npm test
   ```
   ```

6. **Test the permission:**
   ```bash
   # Verify the command works
   npm --version
   npm install
   ```

7. **Update changelog:**
   ```bash
   git add .claude/settings.local.json .claude/docs/permission-guide.md .claude/docs/changelog.md
   git commit -m "chore(config): add npm permission for frontend development"
   ```

---

## Troubleshooting

### Problem: Permission Denied Error

**Symptoms:**
```
Error: Bash command blocked: [command]
Permission denied
```

**Solution:**
1. Check if command matches existing pattern in `.claude/settings.local.json`
2. Verify wildcard pattern covers the command
   - `git commit` is covered by `git *:*` ✅
   - `npm install` is NOT covered by `git *:*` ❌
3. Add specific pattern if needed (follow "How to Add New Permissions")
4. Check for typos in permission patterns
5. Restart Claude Code session to reload permissions

---

### Problem: Too Permissive Pattern

**Symptoms:**
- Security concern about overly broad wildcard
- Pattern allows commands that shouldn't be permitted

**Solution:**
1. **Narrow the scope:**
   ```json
   // Too broad
   "Bash(git *:*)"

   // More narrow
   "Bash(git status:*)",
   "Bash(git diff:*)",
   "Bash(git log:*)",
   "Bash(git add:*)",
   "Bash(git commit:*)",
   "Bash(git push:*)"
   ```

2. **Add constraints:**
   ```json
   // Generic
   "Bash(docker *:*)"

   // With constraints (if needed)
   "Bash(docker ps:*)",
   "Bash(docker logs:*)",
   "Bash(docker exec:*)"
   ```

3. **Document specific use cases** in this guide
4. **Review periodically** (quarterly) to ensure patterns are still appropriate

---

### Problem: Cross-Platform Incompatibility

**Symptoms:**
- Permission works on Windows but not Unix (or vice versa)
- Command not found on certain operating systems

**Solution:**
1. **Check if both OS patterns exist:**
   ```json
   "Bash(./gradlew *:*)",      // Unix
   "Bash(gradlew.bat *:*)"     // Windows
   ```

2. **Add missing pattern:**
   ```json
   // Add both OS variants
   "Bash(findstr *:*)",        // Windows
   "Bash(grep *:*)"            // Unix
   ```

3. **Test on both platforms** if possible
4. **Document cross-platform differences** in this guide

---

## Periodic Review

### Quarterly Review Process

**Recommended:** Review permissions every 3 months

**Checklist:**
- [ ] Review all patterns in `settings.local.json`
- [ ] Identify unused patterns (can they be removed?)
- [ ] Verify rationale still applies for each pattern
- [ ] Look for opportunities to narrow scope
- [ ] Check for new commands that need permission
- [ ] Update this guide with any changes
- [ ] Test permissions on both Windows and Unix (if applicable)

**Benefits:**
- Minimize permission creep
- Ensure security posture remains strong
- Remove obsolete permissions
- Keep documentation current

---

### Permission Audit Template

Use this template for quarterly audits:

```markdown
## Permission Audit - [Date]

### Reviewed Patterns
- ✅ `git *:*` - Still needed, rationale valid
- ✅ `./gradlew *:*` - Still needed, rationale valid
- ⚠️ `curl *:*` - Consider narrowing to specific domains
- ❌ `old-tool *:*` - No longer used, removed

### Changes Made
1. Removed: `old-tool *:*` - tool no longer in use
2. Narrowed: `curl *:*` → `curl localhost:*` - only local API testing needed
3. Added: `pnpm *:*` - new package manager adopted

### Security Posture
- Total patterns: 12 → 12 (no increase)
- High-risk patterns: 2 (docker, rm/del) - acceptable
- Cross-platform coverage: ✅ Complete

### Recommendations
- Continue quarterly reviews
- Monitor docker usage patterns
- Consider split curl into read-only patterns
```

---

## Best Practices

### 1. Start Narrow, Expand if Needed
```json
// Start with
"Bash(git status:*)"

// Expand if needed
"Bash(git *:*)"
```

### 2. Use Comments for Clarity
```json
{
  "patterns": [
    // Version control (essential for code management)
    "Bash(git *:*)",

    // Build tools (cross-platform)
    "Bash(./gradlew *:*)",      // Unix
    "Bash(gradlew.bat *:*)"     // Windows
  ]
}
```

### 3. Document WHY, Not Just WHAT
- ❌ "Allows git commands"
- ✅ "Essential for version control operations: commits, branches, history inspection. All operations logged and reversible via git history."

### 4. Group Related Permissions
```json
{
  "patterns": [
    // Version control
    "Bash(git *:*)",

    // Build tools
    "Bash(./gradlew *:*)",
    "Bash(gradlew.bat *:*)",

    // Container management
    "Bash(docker *:*)",
    "Bash(docker-compose *:*)"
  ]
}
```

### 5. Test Before Committing
```bash
# Test permission works
git status
./gradlew --version
docker ps

# If permission denied, add pattern and retest
```

---

## Security Incident Response

### If Unauthorized Command Executed

1. **Immediate Actions:**
   ```bash
   # Check git history for unauthorized changes
   git log --all --oneline

   # Review recent bash history
   history | tail -50

   # Check for suspicious file modifications
   git status
   git diff
   ```

2. **Investigate:**
   - How was permission granted?
   - Was it a misconfigured wildcard?
   - Was the command actually harmful?

3. **Remediate:**
   ```bash
   # Revert unauthorized changes
   git revert <commit-hash>

   # Narrow permission scope
   # Edit .claude/settings.local.json

   # Update permission guide
   # Document the incident and prevention
   ```

4. **Prevent:**
   - Review and narrow wildcard patterns
   - Add specific restrictions if possible
   - Update this guide with lessons learned
   - Consider additional security layers (pre-commit hooks, etc.)

---

## Summary

### Key Principles

1. **Minimal Permissions:** Grant only what's necessary
2. **Document Everything:** Explain WHY, not just WHAT
3. **Use Wildcards Wisely:** Balance convenience with security
4. **Cross-Platform Support:** Support both Windows and Unix
5. **Regular Review:** Audit permissions quarterly
6. **Test Thoroughly:** Verify permissions work as intended
7. **Track Changes:** Version control permission config
8. **Security First:** When in doubt, start narrow

### Permission Categories

| Category | Risk Level | Patterns | Justification |
|----------|------------|----------|---------------|
| Version Control | Medium | `git *:*` | Essential, tracked, reversible |
| Build Tools | Low | `./gradlew *:*`, `gradlew.bat *:*` | Sandboxed, version controlled |
| Containers | Medium | `docker *:*`, `docker-compose *:*` | Isolated, necessary for dev env |
| System Utils | Very Low | `netstat *:*`, `curl *:*` | Read-only or limited scope |
| Text Search | Very Low | `findstr *:*`, `grep *:*` | Read-only |
| File Ops | Low-Medium | `dir *:*`, `ls *:*`, `find *:*`, `del *:*`, `rm *:*` | Read-only + necessary cleanup |

### Quick Reference

- **Add permission:** Update `settings.local.json` + document here + test
- **Review permissions:** Quarterly audit using template above
- **Security concern:** Narrow scope, document mitigation
- **Cross-platform:** Add both Windows and Unix variants

---

**Version:** 2.3.0
**Last Updated:** 2026-01-21
**Next Review:** 2026-04-21 (quarterly)

For questions or security concerns, see [Maintenance Guide](maintenance-guide.md) or file an issue.
