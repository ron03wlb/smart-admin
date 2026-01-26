---
trigger: always_on
description: Commit Message Conventions (Based on Conventional Commits)
tags: [git, commit, conventional-commits, version-control]
positioning: current-standard
ai_role: code_reviewer_and_generator
auto_apply: true
ask_before_fix: false
prerequisites: []
conflicts_with: []
related_rules:
  - 00-INDEX.md
commitlint_rule: conventional
last_updated: 2025-01-20
---

# Commit Message Conventions

Based on [Conventional Commits](https://www.conventionalcommits.org/) specification.

---

## 🤖 AI Directive Block

### When to Apply This Rule
- ✅ User requests commit message generation
- ✅ Before user executes `git commit`
- ✅ During Code Review when checking commit history
- ✅ Detecting non-compliant commit messages

### Mandatory Enforcement Checklist
- [ ] Format complies with `<type>(<scope>): <subject>`
- [ ] Type is one of the 11 allowed types
- [ ] Scope uses correct module name
- [ ] Subject uses English, lowercase start, no period at end
- [ ] Subject length does not exceed 72 characters
- [ ] Issue association uses correct format (Closes/Fixes/Refs #number)

### AI Decision Tree
```
Commit Message Generation Flow:
  ├─ 1️⃣ Identify Change Type
  │   ├─ New feature? → feat
  │   ├─ Bug fix? → fix
  │   ├─ Refactoring? → refactor
  │   ├─ Documentation? → docs
  │   ├─ Testing? → test
  │   ├─ Performance optimization? → perf
  │   ├─ Formatting? → style
  │   ├─ Build system? → build
  │   ├─ CI/CD? → ci
  │   ├─ Miscellaneous? → chore
  │   └─ Revert? → revert
  │
  ├─ 2️⃣ Determine Scope (Module)
  │   ├─ sa-admin, sa-base, sa-common
  │   ├─ smart-admin-web, smart-app
  │   └─ docker, docs
  │
  └─ 3️⃣ Write Subject
      ├─ English, lowercase start
      ├─ Start with imperative verb (add, fix, update, remove)
      └─ No more than 72 characters
```

---

## [Mandatory] Format Specification

### Basic Format
```
<type>(<scope>): <subject>

[optional body]

[optional footer]
```

### Type Definition

| Type | Description | Example |
|------|-------------|---------|
| `feat` | New feature (Feature) | `feat(sa-admin): add user login validation` |
| `fix` | Bug fix | `fix(sa-base): resolve null pointer in UserService` |
| `docs` | Documentation only changes | `docs(sa-admin): update API documentation` |
| `style` | Formatting changes (not affecting code execution) | `style(sa-base): format code with spotless` |
| `refactor` | Refactoring (not new feature, not bug fix) | `refactor(sa-common): extract validation logic` |
| `perf` | Performance optimization | `perf(sa-admin): optimize database query` |
| `test` | Adding or modifying tests | `test(sa-base): add unit tests for UserService` |
| `build` | Build system or dependency changes | `build(sa-admin): upgrade spring boot to 3.2` |
| `ci` | CI configuration changes | `ci: add github actions workflow` |
| `chore` | Miscellaneous (not modifying src or test) | `chore: update .gitignore` |
| `revert` | Reverting previous commit | `revert: revert "feat(sa-admin): add login"` |

### Scope Definition (Modules)

| Scope | Description |
|-------|-------------|
| `sa-admin` | Admin management module |
| `sa-base` | Base module |
| `sa-common` | Common module |
| `smart-admin-web` | Frontend web application |
| `smart-app` | Mobile application |
| `docker` | Docker configuration |
| `docs` | Project documentation |

**Cross-module changes**: Omit scope or use comma-separated
```
feat: add global error handling
feat(sa-admin,sa-base): add shared validation
```

---

## [Mandatory] Subject Rules

1. **Language**: English
2. **Case**: Lowercase start
3. **Tense**: Imperative verb form
4. **Length**: No more than 72 characters
5. **Ending**: No period

### Verb Suggestions

| Action | Verbs |
|--------|-------|
| Add | add, create, implement, introduce |
| Modify | update, change, modify, adjust |
| Remove | remove, delete, drop |
| Fix | fix, resolve, correct |
| Refactor | refactor, restructure, reorganize |
| Optimize | optimize, improve, enhance |

---

## [Recommended] Body Specification

- Used to explain **why** the change was made
- Each line no more than 72 characters
- Use `-` for bullet points

```
fix(sa-base): resolve null pointer in UserService

- Add null check before accessing user object
- Update unit tests to cover edge cases
- Related to production incident on 2025-01-15
```

---

## [Mandatory] Footer Specification (GitHub Issues)

### Issue Association Format

| Keyword | Purpose | Effect |
|---------|---------|--------|
| `Closes #123` | Close Issue | Automatically closes Issue after merge |
| `Fixes #123` | Fix Bug Issue | Automatically closes Issue after merge |
| `Refs #123` | Reference Issue | Only creates link, does not close |

### Breaking Change

```
feat(sa-admin)!: change user API response format

BREAKING CHANGE: The user API now returns camelCase instead of snake_case.

Closes #456
```

---

## Error Pattern Detection

### Pattern 1: Type Error
```bash
# ❌ Wrong
git commit -m "Fix: resolve login issue"      # Capitalized
git commit -m "fixed login issue"             # Missing type
git commit -m "feature(sa-admin): add login"  # Wrong type

# ✅ Correct
git commit -m "fix(sa-admin): resolve login issue"
```

### Pattern 2: Subject Format Error
```bash
# ❌ Wrong
git commit -m "feat(sa-admin): Add user login."   # Capitalized start, period ending
git commit -m "feat(sa-admin): added user login"  # Past tense

# ✅ Correct
git commit -m "feat(sa-admin): add user login"
```

### Pattern 3: Scope Error
```bash
# ❌ Wrong
git commit -m "feat(admin): add login"      # Wrong module name
git commit -m "feat(SA-ADMIN): add login"   # Capitalized

# ✅ Correct
git commit -m "feat(sa-admin): add user login"
```

---

## Complete Examples

### Simple Change
```
feat(sa-admin): add user login validation

Closes #123
```

### Complex Change
```
fix(sa-base): resolve null pointer in UserService

- Add null check before accessing user object
- Update unit tests to cover edge cases
- Add logging for debugging

Fixes #456
Refs #789
```

### Breaking Change
```
feat(sa-admin)!: migrate to new authentication flow

BREAKING CHANGE: JWT token format has been updated.
Old tokens will be invalidated after deployment.

- Update token generation logic
- Add migration script for existing sessions
- Update API documentation

Closes #321
```

---

## Commitlint + Husky Configuration

### 1. Install Dependencies

```bash
# In project root directory
npm init -y
npm install --save-dev @commitlint/cli @commitlint/config-conventional husky
```

### 2. Create commitlint.config.js

```javascript
// commitlint.config.js
module.exports = {
  extends: ['@commitlint/config-conventional'],
  rules: {
    'type-enum': [
      2,
      'always',
      [
        'feat',
        'fix',
        'docs',
        'style',
        'refactor',
        'perf',
        'test',
        'build',
        'ci',
        'chore',
        'revert'
      ]
    ],
    'scope-enum': [
      2,
      'always',
      [
        'sa-admin',
        'sa-base',
        'sa-common',
        'smart-admin-web',
        'smart-app',
        'docker',
        'docs'
      ]
    ],
    'scope-empty': [1, 'never'],
    'subject-case': [2, 'always', 'lower-case'],
    'subject-max-length': [2, 'always', 72],
    'body-max-line-length': [2, 'always', 100]
  }
};
```

### 3. Setup Husky

```bash
# Initialize husky
npx husky init

# Create commit-msg hook
echo "npx --no -- commitlint --edit \$1" > .husky/commit-msg
```

### 4. package.json Configuration

```json
{
  "scripts": {
    "prepare": "husky"
  }
}
```

### Verify Installation

```bash
# Test commitlint
echo "feat(sa-admin): add login" | npx commitlint

# Test wrong format
echo "Add login feature" | npx commitlint  # Should fail
```

---

## Checklist

**Format Check**:
- [ ] Correct Type (one of 11)
- [ ] Correct Scope (module name)
- [ ] Subject in English, lowercase start, no period
- [ ] Subject length ≤ 72 characters

**Content Check**:
- [ ] Subject clearly describes change
- [ ] Body explains why (if needed)
- [ ] Footer correctly associates Issue

**Tool Validation**:
```bash
# Local validation
npx commitlint --from HEAD~1

# CI validation
npx commitlint --from origin/main --to HEAD
```

---

## Related Specifications

- **AI Decision Matrix**: [00-INDEX.md](./00-ai-decision-matrix.md)
- **Naming Conventions**: [foundation/01-naming-conventions.md](./01-naming-conventions.md)
