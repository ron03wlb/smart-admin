# .claude 系統維護檢查清單

**Purpose**: 定期維護檢查清單，確保 .claude 系統健康和一致性

**Version**: 1.0.0
**Last Updated**: 2026-02-01

---

## 每週檢查（5-10 分鐘）

執行時間：每週一早上 9:00

- [ ] **依賴圖驗證**
  ```bash
  python3 .claude/scripts/validate-dependency-graph.py
  ```
  **預期結果**: 無循環依賴，所有依賴關係有效

- [ ] **README 一致性檢查**
  ```bash
  .claude/scripts/validate-readme-consistency.sh
  ```
  **預期結果**: 所有計數一致，無名稱衝突

- [ ] **Git 狀態檢查**
  ```bash
  git status .claude/
  ```
  **預期結果**: 無未跟踪的臨時文件，無遺漏的修改

- [ ] **Metrics 目錄大小**
  ```bash
  du -sh .claude/metrics/
  ```
  **預期結果**: < 500KB（超過則清理 90 天前的 raw logs）

- [ ] **Examples 覆蓋率**
  ```bash
  find .claude/skills -type d -name examples | wc -l
  ```
  **目標**: ≥17 個 skills (52%)

---

## 每月檢查（30 分鐘）

執行時間：每月 1 號下午

- [ ] **版本同步檢查**
  ```bash
  .claude/scripts/sync-skill-versions.sh --dry-run
  ```
  **預期結果**: 所有 config.yml 版本與 VERSIONS.yml 一致

- [ ] **生成依賴圖**
  ```bash
  python3 .claude/scripts/validate-dependency-graph.py --output=mermaid > docs/skill-dependencies.md
  ```
  **用途**: 可視化依賴關係，便於架構審查

- [ ] **審查 settings.local.json**
  - 檢查規則數量（應 ≤35 條）
  - 移除臨時規則（for/if/do 等）
  - 驗證腳本路徑有效性

- [ ] **驗證 examples/ 覆蓋率**
  ```bash
  # 檢查哪些 P0/P1 skills 缺少 examples
  for skill in .claude/skills/foundation/*/ .claude/skills/extended/*/; do
    if [ ! -d "$skill/examples" ]; then
      echo "Missing examples: $(basename $skill)"
    fi
  done
  ```
  **目標**: 所有 P0/P1 skills 都有 examples

- [ ] **清理過期 metrics**
  ```bash
  find .claude/metrics/raw -name "*.json" -mtime +90 -delete
  ```
  **保留策略**: 90 天內的 raw logs

---

## 季度檢查（2 小時）

執行時間：每季度第一個月的 1 號

- [ ] **完整交叉引用審查**
  ```bash
  .claude/scripts/validate-cross-references.sh
  ```
  **檢查項目**:
  - 所有內部連結有效
  - 外部 URL 可訪問
  - 文件引用正確

- [ ] **更新 META.md 版本**
  - 檢查 Component Versions 表格
  - 更新 .claude/ System 版本號
  - 記錄主要變更

- [ ] **審查 deprecated skills**
  - 檢查 lifecycle/deprecated/ 中的 skills
  - 確認 removal_date 是否即將到期
  - 決定保留或移除

- [ ] **檢查 skill 使用率**
  ```bash
  node .claude/scripts/monitoring/generate-quarterly-report.js
  ```
  **分析**:
  - 哪些 skills 使用頻繁
  - 哪些 skills 從未使用（考慮 deprecate）
  - 平均執行時間趨勢

- [ ] **依賴關係優化**
  - 識別深度依賴鏈（>3 層）
  - 考慮拆分或合併 skills
  - 更新 skill-registry.yml

---

## 新 Skill 添加檢查清單

當添加新 skill 時，逐項檢查：

### 必需文件

- [ ] **SKILL.md** (使用模板 `.claude/skills/_shared/templates/SKILL.md.template`)
  - 完整的 frontmatter (name, description)
  - Trigger keywords 章節
  - Examples 章節

- [ ] **config.yml** (完整 metadata)
  ```yaml
  metadata:
    name: skill-name
    version: 1.0.0
    priority: P0/P1/P2
    type: atomic/composite/orchestrator
    category: backend/frontend/full-stack/domain/...
    status: stable/experimental
  ```

- [ ] **dependencies** 欄位 (config.yml)
  ```yaml
  dependencies:
    skills: []           # 依賴的其他 skills
    tools: []            # 外部工具依賴
    required_files: []   # 必需的項目文件
  ```

- [ ] **examples/** 目錄
  - 至少 1 個範例 (P0/P1 優先)
  - 使用統一命名: `example-1-use-case.md`

### 註冊步驟

1. [ ] **添加到 VERSIONS.yml**
   ```yaml
   foundation/extended/productivity:
     skill-name:
       version: 1.0.0
       last_updated: YYYY-MM-DD
       status: stable
       changelog: "Initial release"
   ```

2. [ ] **添加到 skill-registry.yml**
   ```yaml
   skill-name:
     path: "category/subcategory/skill-name"
     priority: "P0"
     type: "atomic"
     # ... (完整元數據)
     depends_on: []
     depended_by: []
   ```

3. [ ] **更新 skills/README.md**
   - 增加 skill 計數
   - 添加到對應分類列表

4. [ ] **更新 00-INDEX.md Part 2** (如果有 trigger keywords)
   ```markdown
   | "trigger keyword" | skill-name | ... |
   ```

5. [ ] **更新 META.md** (如果 total 計數改變)
   ```markdown
   ├── skills/        # XX specialized skills (v3.0.0)
   ```

### 驗證步驟

- [ ] **運行依賴圖驗證**
  ```bash
  python3 .claude/scripts/validate-dependency-graph.py
  ```

- [ ] **運行 README 一致性檢查**
  ```bash
  .claude/scripts/validate-readme-consistency.sh
  ```

- [ ] **檢查 metadata 完整性**
  ```bash
  python3 << 'EOF'
  import yaml
  config = yaml.safe_load(open('.claude/skills/path/to/skill/config.yml'))
  required = ['name', 'version', 'priority', 'type', 'category', 'status']
  missing = [f for f in required if f not in config.get('metadata', {})]
  print("✅ Complete" if not missing else f"❌ Missing: {missing}")
  EOF
  ```

- [ ] **測試 skill 執行** (如果可能)
  ```bash
  # 根據 skill 類型執行測試
  ```

---

## 重大變更檢查清單

當進行架構級別變更時（如 v3.0.0 → v4.0.0）：

### 準備階段

- [ ] **創建功能分支**
  ```bash
  git checkout -b refactor/major-change-name
  ```

- [ ] **備份當前狀態**
  ```bash
  git tag -a v3.0.2-pre-refactor -m "Backup before major refactor"
  ```

- [ ] **文檔變更計畫**
  - 創建 RFC 或 Plan 文檔
  - 列出受影響的文件
  - 定義回滾策略

### 執行階段

- [ ] **分階段執行**（類似本次 Phase 1-4）
  - 每階段獨立提交
  - 每階段驗證通過
  - 失敗立即回滾

- [ ] **持續驗證**
  - 每次修改後運行全套檢查
  - 記錄驗證結果

### 完成階段

- [ ] **更新版本號**
  - `.claude/VERSION.md`
  - `.claude/META.md`
  - `CLAUDE.md` Change History

- [ ] **生成變更日誌**
  ```bash
  git log --oneline v3.0.2..HEAD > docs/changelogs/v4.0.0-changelog.md
  ```

- [ ] **審查和合併**
  - Code review（如果團隊協作）
  - 最終驗證
  - 合併到 master

---

## 緊急回滾程序

如果發現嚴重問題需要回滾：

### 快速回滾（<5 分鐘）

```bash
# 1. 查看最近的備份 tag
git tag -l '*-pre-*' | tail -3

# 2. 重置到備份點
git reset --hard v3.0.2-pre-refactor

# 3. 強制推送（謹慎！）
# git push origin refactor/branch-name --force
```

### 選擇性回滾（針對特定文件）

```bash
# 回滾單個文件
git checkout v3.0.2-pre-refactor -- .claude/skills/specific-skill/config.yml

# 回滾整個目錄
git checkout v3.0.2-pre-refactor -- .claude/skills/
```

### 回滾後驗證

- [ ] 運行全套檢查腳本
- [ ] 測試關鍵功能
- [ ] 確認文檔一致性

---

## 工具維護

定期更新維護工具本身：

- [ ] **validate-dependency-graph.py**
  - 添加新的驗證規則
  - 優化性能（大規模 dependency graph）

- [ ] **validate-readme-consistency.sh**
  - 兼容性測試（macOS/Linux）
  - 添加新的一致性檢查

- [ ] **monitoring scripts**
  - 更新 metrics schema
  - 優化報告格式

---

**維護負責人**: AI Documentation System
**聯繫方式**: 通過 GitHub Issues 反饋問題
**版本管理**: 遵循 Semantic Versioning (SemVer)

---

**最後審查**: 2026-02-01
**下次審查**: 2026-03-01
