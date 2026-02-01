## FAQ

### Q1: 如何處理方案執行失敗？

**A**: 默認採用 `CONTINUE` 策略，跳過失敗方案繼續執行。

**配置選項**:

```bash
# 失敗時立即停止並回滾
/batch-execute --on-failure=ABORT_ALL

# 失敗時暫停等待用戶決策
/batch-execute --on-failure=PAUSE
```

**重試失敗的方案**:

```bash
# 查看執行報告，獲取 Batch ID
# 示例: batch-exec-20260129-153000

# 重試失敗的方案
/batch-execute --retry-failed batch-exec-20260129-153000
```

### Q2: 如何自定義並行數量？

**A**: 使用 `--max-concurrent` 參數。

```bash
# 最多 3 個並行（適用於資源受限環境）
/batch-execute --max-concurrent=3

# 最多 10 個並行（適用於高性能環境）
/batch-execute --max-concurrent=10
```

**配置文件**:

```yaml
execution:
  max_concurrent: 5  # Default
```

**注意**: v1.0.0 MVP 暫不支持並行執行，此參數在 v1.1.0 生效。

### Q3: 如何驗證方案映射是否正確？

**A**: 使用 Dry-run 模式驗證。

```bash
# 檢查 Skill 映射
/batch-execute --dry-run --validate-mappings
```

**報告示例**:

```
╔══════════════════════════════════════════════════════════════════
║ Skill Mapping Validation
╠══════════════════════════════════════════════════════════════════
║ Successfully Mapped: 7/8
║
║   ✓ product-crud → smartadmin-crud-generator (confidence: 1.0)
║   ✓ liteflow-migration → liteflow-rule-builder (confidence: 1.0)
║   ⚠ custom-feature → None (requires manual execution)
╚══════════════════════════════════════════════════════════════════
```

### Q4: 如何手動指定 Skill 映射？

**A**: 在方案的 YAML frontmatter 中添加 `skill:` 字段。

```yaml
---
name: custom-feature
type: custom
skill: smartadmin-crud-generator  # Manual override
---
```

### Q5: 支持哪些 Skill？

**A**: 支持所有 P0/P1 Skills（共 15 個）。

**P0 Skills (6)**:
- smartadmin-crud-generator
- smartadmin-integration-test
- test-fixture-generator
- vavr-refactoring-assistant
- archunit-test-generator
- security-hardening-pro

**P1 Skills (3)**:
- liteflow-rule-builder
- quality-gate-orchestrator
- fraud-detection-pattern-generator

**P2 Skills (6)**:
- smartadmin-performance-suite
- smartadmin-testing-suite
- db-migration-manager
- igame-feature-builder
- cicd-pipeline-builder
- (More...)

**完整列表**: 參考 [references/skill-mapping.md](references/skill-mapping.md)

### Q6: 如何處理無 Skill 映射的方案？

**A**: 無映射的方案會被標記為 "Requires Manual Execution"。

**選項**:

1. **手動執行**:
   ```bash
   # 手動執行該方案（不使用 batch executor）
   /execute docs/plans/custom/feature.md
   ```

2. **添加映射**:
   ```yaml
   # 在方案中添加 skill 字段
   ---
   skill: smartadmin-crud-generator
   ---
   ```

3. **配置映射**:
   ```yaml
   # config.yml
   skill_mapping:
     custom_mappings:
       'docs/plans/custom/feature.md': 'smartadmin-crud-generator'
   ```

### Q7: v1.0.0 支持並行執行嗎？

**A**: 不支持。v1.0.0 MVP 僅支持串行執行（Sequential Execution）。

**並行執行計劃**: v1.1.0（預計 2026-02-12）

**當前行為**:
- 所有方案串行執行（一次一個）
- 衝突方案自動分組串行
- 無衝突方案也串行執行（v1.0.0 限制）

### Q8: 如何估算執行時間？

**A**: Dry-run 模式會自動估算。

```bash
/batch-execute --dry-run --auto
```

**估算方法**:
- 基於方案類型的默認時間（CRUD: 25 min, Testing: 20 min, etc.）
- 根據方案大小調整（內容長度）
- 考慮衝突導致的串行執行

**報告示例**:

```
║ Time Estimation:
║   - Sequential Execution: 240 minutes
║   - Parallel Execution: 85 minutes (v1.1.0+)
║   - Time Saved: 155 minutes (65% reduction)
```

### Q9: Quality Gate 失敗時會怎樣？

**A**: 取決於配置的失敗策略。

**配置**:

```yaml
execution:
  failure_strategy: CONTINUE  # Default
  quality_gates:
    enabled: true
    run_architecture_test: true
    run_unit_tests: true
```

**行為**:
- `CONTINUE`: 記錄失敗，繼續執行其他方案
- `ABORT_ALL`: 立即停止所有執行
- `PAUSE`: 暫停並等待用戶決策

### Q10: 如何查看歷史執行記錄？

**A**: 查看日誌目錄。

```bash
# 查看日誌目錄
ls -la .claude/metrics/batch-executions/

# 示例輸出
batch-exec-20260129-153000.log
batch-exec-20260129-153000-pre.md
batch-exec-20260129-153000-post.md
batch-exec-20260129-153000.json
```

**JSON 格式** (Machine-readable):

```json
{
  "batch_id": "batch-exec-20260129-153000",
  "total_plans": 8,
  "successful": 7,
  "failed": 1,
  "execution_time_minutes": 125,
  "plans": [
    {
      "name": "product-crud",
      "status": "success",
      "duration_minutes": 25,
      "skill": "smartadmin-crud-generator"
    }
    // ... more plans
  ]
}
```

---

