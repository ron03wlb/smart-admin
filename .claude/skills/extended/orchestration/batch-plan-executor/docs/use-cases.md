## Use Cases

### Use Case 1: LiteFlow Migration (8 Phases)

**場景**: LiteFlow 遷移計劃包含 8 個階段性方案，需要依序執行。

**挑戰**:
- 多個階段有依賴關係（Phase 2 依賴 Phase 1）
- 串行執行耗時長（預計 4 小時）
- 手動執行容易出錯

**解決方案**:

```bash
# Step 1: Dry-run 風險評估
/batch-execute --dry-run --scan-dir=docs/plans/liteflow/

# Step 2: 查看報告
# - 識別到 8 個方案
# - 檢測到依賴關係
# - 預計時間: 串行 240 分鐘

# Step 3: 執行
/batch-execute --scan-dir=docs/plans/liteflow/ --auto
```

**效果**:
- ✅ 自動檢測並遵循依賴關係
- ✅ v1.0.0: 串行執行，預計 240 分鐘
- ✅ v1.1.0+: 並行優化，預計 85 分鐘（65% 時間減少）
- ✅ 自動質量檢查（ArchUnit、編譯、測試）

### Use Case 2: Batch CRUD Generation

**場景**: 一次生成 5 個業務模組的 CRUD 功能。

**挑戰**:
- 重複性工作量大
- 手動執行耗時且枯燥
- 容易出現不一致

**解決方案**:

```bash
# 創建 5 個 Claude Code Plans
~/.claude/plans/product-crud.md
~/.claude/plans/order-crud.md
~/.claude/plans/customer-crud.md
~/.claude/plans/inventory-crud.md
~/.claude/plans/payment-crud.md

# 批量執行
/batch-execute --auto
```

**效果**:
- ✅ 自動識別為 `crud` 類型
- ✅ 映射到 `smartadmin-crud-generator`
- ✅ 無文件衝突（不同業務模組）
- ✅ v1.1.0+: 並行執行，5 個模組僅需 30 分鐘（vs 串行 125 分鐘）

### Use Case 3: Mixed Plan Types

**場景**: 同時執行 CRUD 生成、測試生成和業務方案。

**挑戰**:
- 多種方案類型混合
- 可能存在文件衝突
- 需要不同的 Skill

**解決方案**:

```bash
# Dry-run 檢查衝突
/batch-execute --dry-run \
  ~/.claude/plans/product-crud.md \
  .claude/skills/smartadmin-testing-suite/phases/phase-2-integration-tests.md \
  docs/plans/tenant/multi-tenant-setup.md

# 查看報告
# - Conflict: ProductController.java (product-crud vs integration-tests)
# - Resolution: Serial execution (product-crud → integration-tests)

# 執行
/batch-execute --mode=interactive \
  ~/.claude/plans/product-crud.md \
  .claude/skills/smartadmin-testing-suite/phases/phase-2-integration-tests.md \
  docs/plans/tenant/multi-tenant-setup.md
```

**效果**:
- ✅ 自動識別三種方案類型
- ✅ 檢測文件衝突並串行執行
- ✅ tenant-migration 需要手動執行（無 Skill 映射）

**詳細示例**: 參考 [examples/EXAMPLE-mixed-plans.md](examples/EXAMPLE-mixed-plans.md)

### Use Case 4: Pre-Execution Risk Assessment

**場景**: 在實際執行前評估方案衝突和風險。

**解決方案**:

```bash
# Dry-run 完整評估
/batch-execute --dry-run --auto
```

**報告內容**:
- ✅ 檢測到的衝突列表
- ✅ 執行計劃預覽
- ✅ 風險評估（HIGH/MEDIUM/LOW）
- ✅ 預計執行時間

---

