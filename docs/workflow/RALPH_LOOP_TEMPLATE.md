# Ralph Loop 標準啟動 Prompt 模板

複製以下指令，將 `<feature-name>` 替換為功能名稱後執行。

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
  4. 輸出 <promise>SMARTADMIN_DONE</promise>

注意：只有在 promise 陳述完全為真時才輸出，不可謊報以逃出迴圈。" \
--max-iterations 25 \
--completion-promise "SMARTADMIN_DONE"
```

## 參數說明

| 參數 | 預設值 | 說明 |
|------|-------|------|
| `--max-iterations` | 25 | 硬上限，防止無限迴圈。小功能可設 10-15，大功能可設 30-35 |
| `--completion-promise` | `SMARTADMIN_DONE` | 完成信號，精確字串匹配 |

## 取消迴圈

```bash
/cancel-ralph
```

## 常見問題

**Q: 迴圈達到 max-iterations 但未完成怎麼辦？**
重新執行 `/ralph-loop`，它會看到之前的 git history 繼續迭代。

**Q: 測試一直失敗怎麼辦？**
確認 `.openspec/tasks.md` 任務粒度夠小（每個任務 2-5 分鐘），過大的任務容易導致卡住。
