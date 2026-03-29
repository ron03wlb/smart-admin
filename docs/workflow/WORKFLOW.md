# SmartAdmin Claude Code 自動化開發工作流程

> 從需求到 PR，只需兩個人工操作。

---

## 快速參考

| 指令 | 用途 |
|------|------|
| `/openspec:propose "需求"` | 開始新功能（Phase 1） |
| `/ralph-loop "..." --max-iterations 25 --completion-promise "SMARTADMIN_DONE"` | 啟動自主實作（Phase 3） |
| `/cancel-ralph` | 取消迴圈 |
| `/openspec:verify` | 手動驗證規格合規 |
| `openspec archive` | 手動封存規格 |

---

## 完整工作流程

### Phase 0：環境初始化（一次性）

```bash
# 安裝 OpenSpec
npm install -g @fission-ai/openspec@latest

# 驗證安裝
openspec --version
gh auth status

# 確認 ralph-loop 已啟用（應顯示 true）
cat .claude/settings.json | python3 -c \
  "import sys,json; d=json.load(sys.stdin); \
   print('ralph-loop:', d.get('plugins',{}).get('ralph-loop@claude-plugins-official','NOT FOUND'))"
```

---

### Phase 1：規格定義（人工觸發）

```bash
/openspec:propose "功能需求描述"
```

**產出**（在 `.openspec/` 目錄）：
- `proposal.md` — 提案概覽
- `design.md` — 架構設計
- `tasks.md` — 微任務清單（2-5 分鐘粒度）
- `requirements.md` — GIVEN/WHEN/THEN 驗收場景

**驗收**：審查 `tasks.md`，確認任務粒度合理後繼續。

---

### Phase 2：設計精煉

整合進 ralph-loop prompt，不需獨立觸發。

---

### Phase 3：TDD 自主實作（全自動）

複製 `docs/workflow/RALPH_LOOP_TEMPLATE.md` 中的指令，替換 `<feature-name>` 後執行。

**自動執行序列（每次迭代）**：
```
implementer subagent
  → TDD（RED → GREEN → REFACTOR）
  → PostToolUse hooks（spotless + tsc）
  → Stop hooks（ArchUnit + 全測試套件）
  → ralph-loop 判斷：通過？繼續 | 失敗？重試
```

**完成條件**：輸出 `<promise>SMARTADMIN_DONE</promise>`（由 Claude 在全部任務完成後輸出）

---

### Phase 4：品質關卡（全自動，hooks 驅動）

| Hook | 觸發 | 內容 |
|------|------|------|
| PostToolUse | Edit/Write .java | `./gradlew spotlessApply` |
| PostToolUse | Edit/Write .ts/.tsx | `npx tsc --noEmit` |
| PreToolUse | Bash | 攔截 DROP/rm -rf/git push --force |
| Stop | Session 結束 | ArchUnit 架構驗證 |
| Stop | Session 結束 | OpenSpec 合規檢查 |
| Stop | Session 結束 | 全測試套件 + JaCoCo 覆蓋率 |

---

### Phase 5：封存 + 自動交付

由 ralph-loop 在 promise 達成前自動執行：

```bash
openspec archive         # 封存 spec delta
git push origin HEAD     # 推送功能分支
gh pr create \
  --title "feat: <feature-name>" \
  --body-file .openspec/design.md \
  --draft                # 建立 Draft PR
```

**最終驗收**：PR Review（唯一人工確認點）

---

## 驗收矩陣

| Phase | 機制 | 自動/人工 | 失敗行為 |
|-------|------|---------|---------|
| Phase 0 | `openspec --version` + `gh auth status` | 人工 | 重新安裝 |
| Phase 1 | 人工審查 `tasks.md` | **人工** | 重新 `/openspec:propose` |
| Phase 3 | ArchUnit + 全測試通過 | 自動 | ralph-loop 重試 |
| Phase 4 | hooks 全部 exit 0 | 自動 | 阻擋，強制修復 |
| Phase 5 | Draft PR 建立 | 自動 | 手動 `git push + gh pr create` |
| Final | **PR Review** | **人工** | 修改後重新 push |

---

## 成本控制

| 策略 | 設定 |
|------|------|
| 迭代上限 | `--max-iterations 25`（小功能 10-15，大功能 30-35） |
| 模型分層 | 規劃/審查用 Opus，實作用 Sonnet |
| 定期壓縮 | 長 session 每 30 分鐘執行 `/compact` |

---

## 疑難排解

**ralph-loop 卡住不動**
→ 檢查 `.claude/ralph-loop.local.md` 是否存在
→ 執行 `/cancel-ralph` 後重新啟動

**ArchUnit 一直失敗**
→ 確認 `@Transactional` 只在 Manager 層
→ 確認 Controller 沒有直接呼叫 Dao

**OpenSpec archive 失敗**
→ 確認 `.openspec/specs/` 目錄存在
→ 手動執行 `openspec archive`
