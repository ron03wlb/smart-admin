---
trigger: always_on
description: Commit Message 規範（基於 Conventional Commits）
tags: [git, commit, conventional-commits, version-control]
positioning: current-standard
ai_role: code_reviewer_and_generator
auto_apply: true
ask_before_fix: false
prerequisites: []
conflicts_with: []
related_rules:
  - rules/00-ai-decision-matrix.md
commitlint_rule: conventional
last_updated: 2025-01-20
---

# Commit Message 規範

基於 [Conventional Commits](https://www.conventionalcommits.org/) 規範。

---

## 🤖 AI 指令區塊

### 何時應用此規則
- ✅ 用戶要求生成 commit message
- ✅ 用戶執行 `git commit` 前
- ✅ Code Review 時檢查 commit history
- ✅ 檢測到不規範的 commit message

### 強制執行檢查清單
- [ ] 格式符合 `<type>(<scope>): <subject>`
- [ ] Type 在允許的 11 種類型中
- [ ] Scope 使用正確的模塊名稱
- [ ] Subject 使用英文、小寫開頭、無句號結尾
- [ ] Subject 長度不超過 72 字元
- [ ] Issue 關聯使用正確格式（Closes/Fixes/Refs #number）

### AI 決策樹
```
Commit Message 生成流程:
  ├─ 1️⃣ 識別變更類型
  │   ├─ 新功能? → feat
  │   ├─ Bug 修復? → fix
  │   ├─ 重構? → refactor
  │   ├─ 文件? → docs
  │   ├─ 測試? → test
  │   ├─ 效能優化? → perf
  │   ├─ 格式調整? → style
  │   ├─ 建置系統? → build
  │   ├─ CI/CD? → ci
  │   ├─ 雜項? → chore
  │   └─ 復原? → revert
  │
  ├─ 2️⃣ 確定 Scope（模塊）
  │   ├─ sa-admin, sa-base, sa-common
  │   ├─ smart-admin-web, smart-app
  │   └─ docker, docs
  │
  └─ 3️⃣ 撰寫 Subject
      ├─ 英文、小寫開頭
      ├─ 動詞原形開頭（add, fix, update, remove）
      └─ 不超過 72 字元
```

---

## 【強制】格式規範

### 基本格式
```
<type>(<scope>): <subject>

[optional body]

[optional footer]
```

### Type 定義

| Type | 說明 | 範例 |
|------|------|------|
| `feat` | 新增功能 (Feature) | `feat(sa-admin): add user login validation` |
| `fix` | 修補 Bug | `fix(sa-base): resolve null pointer in UserService` |
| `docs` | 僅修改文件 | `docs(sa-admin): update API documentation` |
| `style` | 格式修改（不影響程式運行） | `style(sa-base): format code with spotless` |
| `refactor` | 重構（非新功能、非 Bug 修復） | `refactor(sa-common): extract validation logic` |
| `perf` | 效能優化 | `perf(sa-admin): optimize database query` |
| `test` | 新增或修正測試 | `test(sa-base): add unit tests for UserService` |
| `build` | 建置系統或依賴變更 | `build(sa-admin): upgrade spring boot to 3.2` |
| `ci` | CI 設定變更 | `ci: add github actions workflow` |
| `chore` | 雜項（不修改 src 或 test） | `chore: update .gitignore` |
| `revert` | 復原先前的 commit | `revert: revert "feat(sa-admin): add login"` |

### Scope 定義（模塊）

| Scope | 說明 |
|-------|------|
| `sa-admin` | 後台管理模塊 |
| `sa-base` | 基礎模塊 |
| `sa-common` | 共用模塊 |
| `smart-admin-web` | 前端 Web 應用 |
| `smart-app` | 行動應用 |
| `docker` | Docker 配置 |
| `docs` | 專案文件 |

**跨模塊變更**: 省略 scope 或使用逗號分隔
```
feat: add global error handling
feat(sa-admin,sa-base): add shared validation
```

---

## 【強制】Subject 規則

1. **語言**: 英文
2. **大小寫**: 小寫開頭
3. **時態**: 動詞原形（命令式）
4. **長度**: 不超過 72 字元
5. **結尾**: 無句號

### 動詞建議

| 動作 | 動詞 |
|------|------|
| 新增 | add, create, implement, introduce |
| 修改 | update, change, modify, adjust |
| 刪除 | remove, delete, drop |
| 修復 | fix, resolve, correct |
| 重構 | refactor, restructure, reorganize |
| 優化 | optimize, improve, enhance |

---

## 【推薦】Body 規範

- 用於解釋 **為什麼** 做此變更
- 每行不超過 72 字元
- 使用 `-` 列點說明

```
fix(sa-base): resolve null pointer in UserService

- Add null check before accessing user object
- Update unit tests to cover edge cases
- Related to production incident on 2025-01-15
```

---

## 【強制】Footer 規範（GitHub Issues）

### Issue 關聯格式

| 關鍵字 | 用途 | 效果 |
|--------|------|------|
| `Closes #123` | 關閉 Issue | 合併後自動關閉 Issue |
| `Fixes #123` | 修復 Bug Issue | 合併後自動關閉 Issue |
| `Refs #123` | 參考 Issue | 僅建立連結，不關閉 |

### Breaking Change

```
feat(sa-admin)!: change user API response format

BREAKING CHANGE: The user API now returns camelCase instead of snake_case.

Closes #456
```

---

## 錯誤模式檢測

### 模式 1: Type 錯誤
```bash
# ❌ 錯誤
git commit -m "Fix: resolve login issue"      # 大寫
git commit -m "fixed login issue"             # 缺少 type
git commit -m "feature(sa-admin): add login"  # 錯誤 type

# ✅ 正確
git commit -m "fix(sa-admin): resolve login issue"
```

### 模式 2: Subject 格式錯誤
```bash
# ❌ 錯誤
git commit -m "feat(sa-admin): Add user login."   # 大寫開頭、句號結尾
git commit -m "feat(sa-admin): added user login"  # 過去式

# ✅ 正確
git commit -m "feat(sa-admin): add user login"
```

### 模式 3: Scope 錯誤
```bash
# ❌ 錯誤
git commit -m "feat(admin): add login"      # 錯誤模塊名
git commit -m "feat(SA-ADMIN): add login"   # 大寫

# ✅ 正確
git commit -m "feat(sa-admin): add user login"
```

---

## 完整範例

### 簡單變更
```
feat(sa-admin): add user login validation

Closes #123
```

### 複雜變更
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

## Commitlint + Husky 配置

### 1. 安裝依賴

```bash
# 在專案根目錄
npm init -y
npm install --save-dev @commitlint/cli @commitlint/config-conventional husky
```

### 2. 建立 commitlint.config.js

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

### 3. 設定 Husky

```bash
# 初始化 husky
npx husky init

# 建立 commit-msg hook
echo "npx --no -- commitlint --edit \$1" > .husky/commit-msg
```

### 4. package.json 配置

```json
{
  "scripts": {
    "prepare": "husky"
  }
}
```

### 驗證安裝

```bash
# 測試 commitlint
echo "feat(sa-admin): add login" | npx commitlint

# 測試錯誤格式
echo "Add login feature" | npx commitlint  # 應該失敗
```

---

## 檢查清單

**格式檢查**:
- [ ] Type 正確（11 種之一）
- [ ] Scope 正確（模塊名稱）
- [ ] Subject 英文、小寫開頭、無句號
- [ ] Subject 長度 ≤ 72 字元

**內容檢查**:
- [ ] Subject 清楚描述變更
- [ ] Body 解釋 why（如需要）
- [ ] Footer 正確關聯 Issue

**工具驗證**:
```bash
# 本地驗證
npx commitlint --from HEAD~1

# CI 驗證
npx commitlint --from origin/main --to HEAD
```

---

## 相關規範

- **AI 決策矩陣**: [rules/00-ai-decision-matrix.md](./00-ai-decision-matrix.md)
- **命名規範**: [rules/01-naming-conventions.md](./01-naming-conventions.md)
