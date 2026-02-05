## Conflict Detection

### Overview

Batch Plan Executor 實施三層衝突檢測機制。

**v1.0.0 MVP**: 僅實施文件級別衝突檢測
**Future Releases**: 模塊級別和依賴關係衝突檢測

### Layer 1: File-Level Conflicts (v1.0.0 ✅)

**定義**: 兩個或多個方案嘗試修改同一個文件。

**嚴重性**: **HIGH** - 可能導致合併衝突或數據丟失。

**檢測機制**:

```python
# Step 1: Extract affected files from each plan
for plan in plans:
    affected_files = extract_affected_files(plan.content)
    # Strategy 1: Code blocks with file path comments
    # Strategy 2: Explicit file references (backticks)
    # Strategy 3: Markdown links
    # Strategy 4: Inferred from class names (Entity, Controller, etc.)

# Step 2: Detect conflicts (files modified by 2+ plans)
file_map = {}
for plan, files in plan_files:
    for file_path in files:
        if file_path not in file_map:
            file_map[file_path] = []
        file_map[file_path].append(plan.id)

conflicts = [
    FileConflict(file_path=fp, conflicting_plan_ids=pids)
    for fp, pids in file_map.items()
    if len(pids) > 1
]
```

**文件提取策略**:

1. **Strategy 1: Code Blocks with File Path Comments**
   ```java
   ```java
   // File: src/main/java/.../ProductController.java
   @RestController
   public class ProductController { ... }
   ```
   ```

2. **Strategy 2: Explicit File References**
   ```markdown
   Modify `src/main/java/.../ProductService.java` to add caching.
   ```

3. **Strategy 3: Markdown Links**
   ```markdown
   See [ProductEntity](src/main/java/.../ProductEntity.java) for details.
   ```

4. **Strategy 4: Class Name Inference**
   ```markdown
   Create ProductEntity class
   → Infer: src/main/java/.../domain/entity/ProductEntity.java
   ```

**衝突解決策略**:

- **Serial Execution Groups**: 將衝突方案分組，串行執行
- **User Confirmation**: 高衝突場景需要用戶確認

**示例報告**:

```
╔══════════════════════════════════════════════════════════════════
║ Conflict Analysis Report
╠══════════════════════════════════════════════════════════════════
║ Total Conflicts: 2
║ File-level Conflicts: 2 (HIGH)
╠══════════════════════════════════════════════════════════════════
║ Conflict #1: ProductController.java
║   Conflicting Plans:
║     - [crud] product-crud
║     - [refactoring] controller-vavr-migration
║   Resolution: Serialize execution (product-crud → vavr-migration)
║
║ Conflict #2: product-api.ts
║   Conflicting Plans:
║     - [crud] product-crud
║     - [integration] api-update
║   Resolution: Serialize execution
╚══════════════════════════════════════════════════════════════════
```

### Layer 2: Module-Level Conflicts (v1.1.0 ⏳)

**定義**: 兩個或多個方案操作同一業務模組。

**嚴重性**: **MEDIUM** - 可能導致集成問題或測試失敗。

**檢測機制** (Planned):

```python
# Extract affected modules from plan content
modules = extract_affected_modules(plan)
# Example: net.lab1024.sa.business.product.*
#          smart-admin-web/src/views/product/*

# Detect module conflicts
module_map = {}
for plan, modules in plan_modules:
    for module in modules:
        if module not in module_map:
            module_map[module] = []
        module_map[module].append(plan.id)

conflicts = [
    ModuleConflict(module=m, conflicting_plan_ids=pids)
    for m, pids in module_map.items()
    if len(pids) > 1
]
```

**解決策略**:
- **Warning Only**: 允許並行執行，但標記為風險
- **Suggested Testing**: 建議在每個方案後運行測試

### Layer 3: Dependency Relationship Conflicts (v1.2.0 ⏳)

**定義**: 方案執行順序違反依賴關係。

**嚴重性**: **CRITICAL** - 可能導致執行失敗或錯誤行為。

**檢測機制** (Planned):

```python
# Build dependency graph
graph = DirectedGraph()
for plan in plans:
    graph.add_node(plan.id)
    for dep in plan.dependencies:
        graph.add_edge(plan.id, dep)

# Detect circular dependencies
cycles = graph.find_cycles()
if cycles:
    raise CircularDependencyError(cycles)

# Perform topological sort for safe execution order
execution_order = graph.topological_sort()
```

**解決策略**:
- **Topological Sort**: 自動調整執行順序
- **Circular Dependency Detection**: 檢測並報告循環依賴

---

