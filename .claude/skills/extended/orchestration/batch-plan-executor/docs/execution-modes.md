## Execution Modes

Batch Plan Executor 提供三種執行模式。

### Mode 1: Auto Mode (Fully Automatic)

**特點**:
- 零人工干預
- 自動解決可解決的衝突
- 高風險項會暫停並提示

**適用場景**: 低風險方案批量執行

**命令**:

```bash
/batch-execute --auto
/batch-execute --mode=auto
```

**行為**:
- 自動掃描所有配置目錄
- 自動檢測衝突
- 自動生成執行計劃
- 自動串行執行（v1.0.0）
- 高風險方案暫停並提示

**配置**:

```yaml
modes:
  auto:
    confirm_before_start: false
    stop_on_high_risk: true
    auto_resolve_conflicts: true
```

### Mode 2: Interactive Mode

**特點**:
- 每個方案執行前確認
- 顯示詳細衝突信息
- 允許調整執行順序

**適用場景**: 高風險方案或首次執行

**命令**:

```bash
/batch-execute --mode=interactive
/batch-execute --mode=interactive --allow-reorder
```

**行為**:
- 掃描並顯示所有方案
- 顯示衝突分析報告
- 每個方案執行前等待確認
- 允許跳過或調整順序

**交互流程**:

```
╔══════════════════════════════════════════════════════════════════
║ Interactive Execution - Plan #1/8
╠══════════════════════════════════════════════════════════════════
║ Plan: product-crud
║ Type: crud
║ Skill: smartadmin-crud-generator
║ Risk: MEDIUM (score: 3)
║   - File conflicts: 2
║   - Dependencies: 1
╠══════════════════════════════════════════════════════════════════
║ Actions:
║   [E] Execute now
║   [S] Skip this plan
║   [R] Reorder plans
║   [A] Abort batch execution
║   [V] View plan details
╚══════════════════════════════════════════════════════════════════
Your choice: _
```

**配置**:

```yaml
modes:
  interactive:
    confirm_each_plan: true
    show_conflict_details: true
    allow_reorder: true
    show_risk_factors: true
```

### Mode 3: Dry-run Mode (Simulation)

**特點**:
- 不執行實際操作
- 生成完整分析報告
- 驗證 Skill 映射

**適用場景**: 執行前風險評估

**命令**:

```bash
/batch-execute --dry-run
/batch-execute --dry-run --auto
/batch-execute --dry-run plan1.md plan2.md
```

**行為**:
- 掃描和分類方案
- 驗證 Skill 映射
- 檢測衝突
- 生成執行計劃
- 估算執行時間
- 風險評估
- 生成完整報告（不執行）

**報告內容**:
- Plan Inventory
- Skill Mapping Validation
- Conflict Analysis
- Execution Plan Preview
- Time Estimation
- Risk Assessment
- Recommendations

**配置**:

```yaml
modes:
  dry_run:
    generate_full_report: true
    estimate_execution_time: true
    validate_skill_mappings: true
```

**詳細文檔**: 參考 [MODE-dry-run.md](modes/MODE-dry-run.md)

---

