# SmartAdmin Claude Code 完整執行流程

> 從需求到 PR，只需兩個人工操作：輸入需求 + PR Review。

---

## Phase 0：環境初始化（只做一次）

```bash
# 確認所有工具就緒
openspec --version      # 需要 ≥ 1.2.0
gh auth status          # 需要已登入
```

若 openspec 尚未安裝：
```bash
npm install -g @fission-ai/openspec@latest
gh auth login
```

---

## Phase 1：規格定義（人工輸入需求）

```bash
/openspec:propose "功能需求描述

範例：
實作 iGaming VIP 等級自動晉升功能：
- 每日計算有效投注額 (Valid Turnover)
- 達標自動晉升，發放晉升獎勵
- 支援多租戶隔離（tenantId 過濾）"
```

**產出** → `.openspec/` 目錄：
```
.openspec/
├── proposal.md      ← 提案概覽
├── design.md        ← 架構設計
├── tasks.md         ← 微任務清單（2-5 分鐘粒度）
└── requirements.md  ← GIVEN/WHEN/THEN 驗收場景
```

**人工驗收**：開啟 `.openspec/tasks.md`，確認任務粒度合理（每個任務 2-5 分鐘）。若不合理，重新 propose。

---

## Phase 2：建立功能分支

```bash
git checkout -b feat/<feature-name>
# 範例：git checkout -b feat/vip-auto-promotion
```

---

## Phase 3：啟動 TDD 自主實作迴圈

從 `docs/workflow/RALPH_LOOP_TEMPLATE.md` 複製模板，替換 `<feature-name>` 後執行：

```bash
/ralph-loop "按照 .openspec/tasks.md 中的規格逐步實作。

═══════════════ SmartAdmin 強制規則 ═══════════════
- 每個任務遵循 TDD：先寫失敗測試 → 最小實作 → 重構
- Service 方法返回值使用 io.vavr.control.Option（禁止 java.util.Optional）
- @Transactional 僅允許在 Manager 層（Service/Controller 層禁用）
- 所有 DB 查詢必須包含 tenantId 過濾（多租戶隔離）
- 布林欄位：deleted（非 isDeleted）
- 依賴注入：@RequiredArgsConstructor + private final（禁止 @Autowired）
- API 回應：ResponseDTO.ok(data)
════════════════════════════════════════════════

每完成一個任務執行：
  git add -A && git commit -m 'feat(<scope>): <task-description>'

測試失敗時：分析失敗原因 → 修復 → 重試（不可跳過）

全部任務完成後依序執行：
  1. openspec archive
  2. git push origin HEAD
  3. gh pr create --title 'feat: <feature-name>' --body-file .openspec/design.md --draft
  4. 輸出 <promise>SMARTADMIN_DONE</promise>" \
--max-iterations 25 \
--completion-promise "SMARTADMIN_DONE"
```

**每次迭代自動執行序列：**
```
Claude 執行任務
  → PostToolUse: spotless（.java）/ tsc（.ts/.tsx）
  → git commit（每個任務完成後）
  → Stop hook: ArchUnit 架構驗證
  → Stop hook: OpenSpec 合規檢查
  → Stop hook: 全測試套件
  → ralph-loop 判斷：
      測試通過 + promise 達成 → 繼續 Phase 5
      測試通過 + 未完成       → 繼續下一個任務
      測試失敗               → 分析修復，重試
```

**自動驗收條件**：
- ArchUnit 架構規則 100% 通過
- 全測試套件 BUILD SUCCESS
- `<promise>SMARTADMIN_DONE</promise>` 輸出

---

## Phase 4：品質關卡（hooks 全程自動）

無需手動操作，hooks 在 Phase 3 期間持續執行：

| 觸發 | Hook | 行為 |
|------|------|------|
| Edit/Write `.java` | spotless:apply | 格式不符即自動修正 |
| Edit/Write `.ts/.tsx` | tsc --noEmit | 型別錯誤即顯示 |
| 任何 Bash | PreToolUse 守衛 | DROP/rm -rf/git push --force 直接 BLOCKED |
| Session Stop | ArchUnit | 架構違規 → 阻擋退出，ralph-loop 重試 |
| Session Stop | 全測試 | 失敗 → 阻擋退出，ralph-loop 重試 |

---

## Phase 5：自動封存 + 交付（ralph-loop 完成後自動執行）

Claude 在輸出 promise 前自動執行：

```
openspec archive          → 封存 spec delta（時間戳歸檔）
git push origin HEAD      → 推送功能分支
gh pr create --draft      → 建立 Draft PR（內容來自 .openspec/design.md）
```

**最終人工驗收**：收到 Draft PR 通知後進行 Code Review，通過後 merge。

---

## 緊急操作

```bash
# 取消迴圈
/cancel-ralph

# 迭代數不夠（未完成就停了）→ 直接重新啟動，會接續 git history
/ralph-loop "..." --max-iterations 35 --completion-promise "SMARTADMIN_DONE"

# 手動驗證規格合規
/openspec:verify

# 手動封存（若自動封存失敗）
openspec archive && git push origin HEAD && gh pr create --draft
```

---

## 完整流程時序圖

```
開發者                    Claude Code               自動化系統
  │                           │                         │
  │ /openspec:propose "需求"  │                         │
  ├──────────────────────────>│                         │
  │                           │ 產出 tasks.md           │
  │<──────────────────────────┤                         │
  │                           │                         │
  │ 審查 tasks.md（人工）     │                         │
  │                           │                         │
  │ /ralph-loop "..." --max-iterations 25               │
  ├──────────────────────────>│                         │
  │                           │ TDD 實作 + commit       │
  │                           ├────────────────────────>│ spotless / tsc
  │                           │<────────────────────────┤
  │                           │ Session Stop            │
  │                           ├────────────────────────>│ ArchUnit + 全測試
  │                           │<────────────────────────┤ 通過 → 繼續
  │                           │ [迭代 N 次]             │ 失敗 → 重試
  │                           │                         │
  │                           │ 全部完成                 │
  │                           │ openspec archive        │
  │                           │ git push + gh pr create │
  │                           ├────────────────────────>│
  │<──────────────────────────┤                         │
  │ Draft PR 建立通知          │                         │
  │                           │                         │
  │ PR Review（人工）         │                         │
  │ Merge ✅                  │                         │
```

---

## 驗收矩陣

| Phase | 機制 | 自動/人工 | 失敗行為 |
|-------|------|---------|---------|
| Phase 0 | `openspec --version` + `gh auth status` | 人工 | 重新安裝 |
| Phase 1 | 人工審查 `tasks.md` 粒度 | **人工** | 重新 `/openspec:propose` |
| Phase 3 | ArchUnit + 全測試通過 | 自動 | ralph-loop 重試 |
| Phase 4 | hooks 全部 exit 0 | 自動 | 阻擋，強制修復 |
| Phase 5 | Draft PR 建立 | 自動 | 手動 `git push + gh pr create` |
| Final | **PR Review** | **人工** | 修改後重新 push |

---

## 快速指令參考

| 情境 | 指令 |
|------|------|
| 開始新功能 | `/openspec:propose "需求描述"` |
| 啟動自主實作 | `/ralph-loop "..." --max-iterations 25 --completion-promise "SMARTADMIN_DONE"` |
| 取消迴圈 | `/cancel-ralph` |
| 驗證規格合規 | `/openspec:verify` |
| 手動封存規格 | `openspec archive` |
| 查看費用 | `/cost` |
| 壓縮上下文 | `/compact` |

---

## 相關文件

- [`RALPH_LOOP_TEMPLATE.md`](./RALPH_LOOP_TEMPLATE.md) — ralph-loop 標準 prompt 模板
- [`WORKFLOW.md`](./WORKFLOW.md) — 工作流程架構說明
- [`../../.openspec/openspec.config.yml`](../../.openspec/openspec.config.yml) — OpenSpec 專案設定
- [`../superpowers/specs/2026-03-29-smartadmin-claude-workflow-design.md`](../superpowers/specs/2026-03-29-smartadmin-claude-workflow-design.md) — 設計規格文件

---

## 成本控制

| 策略 | 設定 |
|------|------|
| 迭代上限 | `--max-iterations 25`（小功能 10-15，大功能 30-35） |
| 模型分層 | 規劃/審查用 Opus，實作用 Sonnet（省 ~60% 費用） |
| 定期壓縮 | 長 session 每 30 分鐘執行 `/compact` |
| Subagent 隔離 | ralph-loop 每次迭代獨立 context，避免膨脹 |
