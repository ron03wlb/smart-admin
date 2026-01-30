# Phase 2: Conflict Detection and Dependency Analysis

**Phase**: 2/4
**Status**: ✅ Production Ready (v1.0.0 - MVP Simplified Version)
**Duration**: 10-14 hours
**Complexity**: High

---

## Overview

This phase implements **multi-layer conflict detection** to identify potential issues when executing multiple plans concurrently. The MVP version focuses on **file-level conflict detection**, with module-level and dependency-level detection planned for future releases.

### Objectives (v1.0.0 MVP)

1. ✅ Detect file-level conflicts (HIGH severity)
2. ⏳ Detect module-level conflicts (MEDIUM severity) - Phase 2
3. ⏳ Detect dependency relationship conflicts (CRITICAL severity) - Phase 3
4. ✅ Generate conflict analysis report
5. ✅ Provide resolution strategies

### Success Criteria (MVP)

- [ ] File-level conflict detection accuracy ≥ 95%
- [ ] Zero false negatives for direct file conflicts
- [ ] Generate actionable conflict resolution strategies
- [ ] Complete conflict analysis report

---

## Conflict Detection Layers

### Layer 1: File-Level Conflicts (v1.0.0 MVP - ✅ Implemented)

**Definition**: Two or more plans attempt to modify the same file.

**Severity**: **HIGH** - Direct conflicts that can cause merge issues or data loss.

**Detection Algorithm**:

```python
def detect_file_conflicts(plans: List[Plan]) -> List[FileConflict]:
    """
    Detect file-level conflicts across plans.

    Args:
        plans: List of Plan objects to analyze

    Returns:
        List of FileConflict objects with conflicting plans
    """
    file_map = defaultdict(list)  # file_path -> [plan_ids]

    # Step 1: Extract affected files from each plan
    for plan in plans:
        affected_files = extract_affected_files(plan.content, plan.file_path)

        for file_path in affected_files:
            # Normalize file path (resolve relative paths, expand ~)
            normalized_path = normalize_file_path(file_path, base_dir=os.path.dirname(plan.file_path))
            file_map[normalized_path].append(plan.id)

    # Step 2: Identify conflicts (files modified by 2+ plans)
    conflicts = []
    for file_path, plan_ids in file_map.items():
        if len(plan_ids) > 1:
            conflicts.append(FileConflict(
                file_path=file_path,
                conflicting_plan_ids=plan_ids,
                severity=ConflictSeverity.HIGH,
                conflict_type=ConflictType.FILE_LEVEL
            ))

    return conflicts
```

**Affected Files Extraction**:

```python
def extract_affected_files(content: str, plan_file_path: str) -> Set[str]:
    """
    Extract all files that will be affected by the plan execution.

    Extraction strategies:
    1. Explicit file references in plan content
    2. Code blocks with file path comments
    3. File path patterns (e.g., src/main/java/net/lab1024/sa/...)
    4. Inferred files from entity/module names

    Args:
        content: Plan content (markdown)
        plan_file_path: Path to the plan file (for context)

    Returns:
        Set of normalized file paths
    """
    affected_files = set()

    # Strategy 1: Extract from code blocks with file path comments
    # Pattern: ```java
    #          // File: src/main/java/net/lab1024/sa/.../Product.java
    code_block_pattern = r'```[a-z]*\n(?:\/\/|#)\s*File:\s*([^\n]+)'
    matches = re.findall(code_block_pattern, content, re.MULTILINE)
    affected_files.update(matches)

    # Strategy 2: Extract from explicit file references
    # Pattern: `src/main/java/net/lab1024/sa/.../Product.java`
    file_ref_pattern = r'`([^`]+\.(java|vue|ts|xml|yml|yaml|properties|sql))`'
    matches = re.findall(file_ref_pattern, content)
    affected_files.update([m[0] for m in matches])

    # Strategy 3: Extract from markdown link references
    # Pattern: [ProductController](src/main/java/.../ProductController.java)
    link_pattern = r'\[([^\]]+)\]\(([^)]+\.(java|vue|ts|xml))\)'
    matches = re.findall(link_pattern, content)
    affected_files.update([m[1] for m in matches])

    # Strategy 4: Infer files from entity/class names
    # Example: "Create ProductEntity class" -> ProductEntity.java
    entity_pattern = r'(?:Create|Implement|Update)\s+(\w+(?:Entity|Controller|Service|Manager|Dao|Mapper))\s+(?:class|interface)'
    matches = re.findall(entity_pattern, content)
    for class_name in matches:
        # Infer file path from class name
        inferred_path = infer_file_path_from_class_name(class_name, plan_file_path)
        if inferred_path:
            affected_files.add(inferred_path)

    # Strategy 5: Extract from "Files to Modify" section
    # Pattern: ### Files to Modify
    #          - src/main/java/.../Product.java
    #          - src/main/resources/mapper/ProductMapper.xml
    files_section_pattern = r'###\s*Files\s+to\s+(?:Modify|Create|Delete)\s*\n((?:-\s*[^\n]+\n?)+)'
    matches = re.findall(files_section_pattern, content, re.IGNORECASE)
    for match in matches:
        file_list = re.findall(r'-\s*([^\n]+)', match)
        affected_files.update(file_list)

    # Normalize and filter
    normalized_files = set()
    for file_path in affected_files:
        file_path = file_path.strip()
        if file_path and is_valid_file_reference(file_path):
            normalized_files.add(normalize_file_path(file_path))

    return normalized_files
```

**File Path Inference**:

```python
def infer_file_path_from_class_name(class_name: str, plan_file_path: str) -> Optional[str]:
    """
    Infer file path from class name based on SmartAdmin conventions.

    Examples:
        ProductEntity -> smart-admin-api/sa-admin/src/main/java/net/lab1024/sa/admin/module/business/product/domain/entity/ProductEntity.java
        ProductController -> .../controller/ProductController.java
        ProductService -> .../service/ProductService.java
    """
    # Extract base name (remove suffix like Entity, Controller, etc.)
    base_name = re.sub(r'(Entity|Controller|Service|Manager|Dao|Mapper|Form|VO)$', '', class_name)

    # Determine layer from suffix
    if class_name.endswith('Entity'):
        layer = 'domain/entity'
    elif class_name.endswith('Controller'):
        layer = 'controller'
    elif class_name.endswith('Service'):
        layer = 'service'
    elif class_name.endswith('Manager'):
        layer = 'manager'
    elif class_name.endswith('Dao'):
        layer = 'dao'
    elif class_name.endswith('Mapper'):
        layer = 'dao'
    elif class_name.endswith('Form'):
        layer = 'domain/form'
    elif class_name.endswith('VO'):
        layer = 'domain/vo'
    else:
        return None

    # Attempt to extract module from plan file path or content
    module = extract_module_from_plan(plan_file_path)
    if not module:
        # Cannot infer without module context
        return None

    # Construct file path
    base_path = "smart-admin-api/sa-admin/src/main/java/net/lab1024/sa/admin/module"
    file_path = f"{base_path}/business/{module}/{layer}/{class_name}.java"

    return file_path
```

### Layer 2: Module-Level Conflicts (v1.1.0 - ⏳ Future Release)

**Definition**: Two or more plans operate on the same business module simultaneously.

**Severity**: **MEDIUM** - May cause integration issues or test failures.

**Detection Strategy** (Planned):

```python
def detect_module_conflicts(plans: List[Plan]) -> List[ModuleConflict]:
    """
    Detect module-level conflicts across plans.

    Module identification:
    - Java package: net.lab1024.sa.admin.module.business.{module}.*
    - Vue module: smart-admin-web/src/views/{module}/*
    """
    module_map = defaultdict(list)  # module_name -> [plan_ids]

    for plan in plans:
        modules = extract_affected_modules(plan.content, plan.file_path)
        for module in modules:
            module_map[module].append(plan.id)

    conflicts = []
    for module, plan_ids in module_map.items():
        if len(plan_ids) > 1:
            conflicts.append(ModuleConflict(
                module_name=module,
                conflicting_plan_ids=plan_ids,
                severity=ConflictSeverity.MEDIUM,
                conflict_type=ConflictType.MODULE_LEVEL
            ))

    return conflicts
```

### Layer 3: Dependency Relationship Conflicts (v1.2.0 - ⏳ Future Release)

**Definition**: Plan execution order violates dependency relationships.

**Severity**: **CRITICAL** - Can cause execution failures or incorrect behavior.

**Detection Strategy** (Planned):

```python
def detect_dependency_conflicts(plans: List[Plan]) -> DirectedGraph:
    """
    Build dependency graph and detect circular dependencies.

    Returns:
        DirectedGraph with nodes=plans, edges=dependencies
    """
    graph = DirectedGraph()

    for plan in plans:
        graph.add_node(plan.id)
        dependencies = extract_dependencies(plan)

        for dep_plan_id in dependencies:
            graph.add_edge(plan.id, dep_plan_id)

    # Detect circular dependencies
    cycles = graph.find_cycles()
    if cycles:
        raise CircularDependencyError(
            message=f"Circular dependencies detected: {cycles}",
            cycles=cycles
        )

    return graph
```

---

## Conflict Resolution Strategies (v1.0.0 MVP)

### Strategy 1: Serial Execution Groups

**When to Use**: HIGH severity file-level conflicts

**Resolution**:
```python
def resolve_file_conflicts_serial(conflicts: List[FileConflict], plans: List[Plan]) -> List[SerialGroup]:
    """
    Group conflicting plans into serial execution groups.

    Example:
        Plan A, B, C all modify Product.java
        -> Create SerialGroup([Plan A, Plan B, Plan C])
    """
    # Build conflict graph
    conflict_graph = build_conflict_graph(conflicts, plans)

    # Find connected components (groups of mutually conflicting plans)
    serial_groups = []
    for component in conflict_graph.find_connected_components():
        plan_group = [plans[pid] for pid in component]
        serial_groups.append(SerialGroup(
            plans=plan_group,
            reason=f"File conflicts: {get_conflicting_files(component, conflicts)}"
        ))

    return serial_groups
```

### Strategy 2: Conflict Warning (MEDIUM Severity)

**When to Use**: Module-level conflicts (future)

**Resolution**:
```python
def resolve_module_conflicts_warning(conflicts: List[ModuleConflict]) -> List[ConflictWarning]:
    """
    Generate warnings for module-level conflicts.

    Allow parallel execution but flag as risky.
    """
    warnings = []
    for conflict in conflicts:
        warnings.append(ConflictWarning(
            severity=ConflictSeverity.MEDIUM,
            message=f"Multiple plans operate on module '{conflict.module_name}'",
            affected_plan_ids=conflict.conflicting_plan_ids,
            recommendation="Monitor execution closely; consider running tests after each plan"
        ))

    return warnings
```

### Strategy 3: Topological Sort (CRITICAL Severity)

**When to Use**: Dependency conflicts (future)

**Resolution**:
```python
def resolve_dependency_conflicts_topological(graph: DirectedGraph) -> List[int]:
    """
    Perform topological sort to determine safe execution order.

    Returns:
        List of plan IDs in dependency-safe order
    """
    try:
        return graph.topological_sort()
    except CycleDetectedError as e:
        # Cannot resolve circular dependencies automatically
        raise ConflictResolutionError(
            message="Circular dependencies detected - manual resolution required",
            cycles=e.cycles
        )
```

---

## Conflict Analysis Report (v1.0.0 MVP)

### Report Format

```python
def generate_conflict_report(
    plans: List[Plan],
    file_conflicts: List[FileConflict]
) -> str:
    """
    Generate comprehensive conflict analysis report.
    """
    total_conflicts = len(file_conflicts)
    affected_plans = len(set(pid for c in file_conflicts for pid in c.conflicting_plan_ids))
    conflicted_files = set(c.file_path for c in file_conflicts)

    report = f"""
╔══════════════════════════════════════════════════════════════════
║ Conflict Analysis Report
╠══════════════════════════════════════════════════════════════════
║ Total Plans: {len(plans)}
║ Affected Plans: {affected_plans}
║ Total Conflicts: {total_conflicts}
║
║ Conflict Breakdown:
║   - File-level Conflicts: {len(file_conflicts)} (HIGH)
║   - Module-level Conflicts: 0 (MEDIUM) [Not implemented in v1.0.0]
║   - Dependency Conflicts: 0 (CRITICAL) [Not implemented in v1.0.0]
╠══════════════════════════════════════════════════════════════════
║ File-Level Conflicts ({len(file_conflicts)}):
"""

    for i, conflict in enumerate(file_conflicts, 1):
        report += f"║\n║ Conflict #{i}: {conflict.file_path}\n"
        report += f"║   Conflicting Plans:\n"

        for plan_id in conflict.conflicting_plan_ids:
            plan = next((p for p in plans if p.id == plan_id), None)
            if plan:
                report += f"║     - [{plan.type}] {plan.name}\n"

        report += f"║   Resolution: Serialize execution (one at a time)\n"

    if total_conflicts == 0:
        report += "║\n║ ✓ No conflicts detected - all plans can run in parallel\n"

    report += """╠══════════════════════════════════════════════════════════════════
║ Resolution Strategy:
"""

    if file_conflicts:
        serial_groups = resolve_file_conflicts_serial(file_conflicts, plans)
        report += f"║   - Serial Execution Groups: {len(serial_groups)}\n"
        for i, group in enumerate(serial_groups, 1):
            report += f"║     Group {i}: {len(group.plans)} plans (sequential)\n"

    report += "╚══════════════════════════════════════════════════════════════════\n"
    return report
```

### Example Output

```
╔══════════════════════════════════════════════════════════════════
║ Conflict Analysis Report
╠══════════════════════════════════════════════════════════════════
║ Total Plans: 8
║ Affected Plans: 3
║ Total Conflicts: 2
║
║ Conflict Breakdown:
║   - File-level Conflicts: 2 (HIGH)
║   - Module-level Conflicts: 0 (MEDIUM) [Not implemented in v1.0.0]
║   - Dependency Conflicts: 0 (CRITICAL) [Not implemented in v1.0.0]
╠══════════════════════════════════════════════════════════════════
║ File-Level Conflicts (2):
║
║ Conflict #1: smart-admin-api/sa-admin/src/main/java/net/lab1024/sa/admin/module/business/product/controller/ProductController.java
║   Conflicting Plans:
║     - [crud] product-crud
║     - [refactoring] controller-vavr-migration
║   Resolution: Serialize execution (one at a time)
║
║ Conflict #2: smart-admin-web/src/api/product/product-api.ts
║   Conflicting Plans:
║     - [crud] product-crud
║     - [integration] api-update
║   Resolution: Serialize execution (one at a time)
╠══════════════════════════════════════════════════════════════════
║ Resolution Strategy:
║   - Serial Execution Groups: 2
║     Group 1: 2 plans (sequential)
║     Group 2: 2 plans (sequential)
╚══════════════════════════════════════════════════════════════════
```

---

## Implementation Checklist (v1.0.0 MVP)

### Core Functions

- [ ] `detect_file_conflicts()` - File-level conflict detection
- [ ] `extract_affected_files()` - Extract files from plan content
- [ ] `infer_file_path_from_class_name()` - Infer file paths from class names
- [ ] `normalize_file_path()` - Normalize and resolve file paths
- [ ] `resolve_file_conflicts_serial()` - Generate serial execution groups
- [ ] `generate_conflict_report()` - Generate conflict analysis report

### Helper Functions

- [ ] `is_valid_file_reference()` - Validate file reference format
- [ ] `extract_module_from_plan()` - Extract module name from plan
- [ ] `build_conflict_graph()` - Build conflict relationship graph
- [ ] `get_conflicting_files()` - Get list of conflicting files for a group

### Data Structures

- [ ] `FileConflict` class
- [ ] `ConflictSeverity` enum (HIGH, MEDIUM, LOW, CRITICAL)
- [ ] `ConflictType` enum (FILE_LEVEL, MODULE_LEVEL, DEPENDENCY_LEVEL)
- [ ] `SerialGroup` class
- [ ] `ConflictWarning` class

---

## Testing (v1.0.0 MVP)

### Unit Tests

```python
def test_extract_affected_files_code_blocks():
    content = """
```java
// File: src/main/java/net/lab1024/sa/.../Product.java
public class Product {}
```
    """
    files = extract_affected_files(content, "test.md")
    assert "src/main/java/net/lab1024/sa/.../Product.java" in files

def test_extract_affected_files_inline_references():
    content = "Modify `src/main/java/.../ProductController.java` to add new endpoint"
    files = extract_affected_files(content, "test.md")
    assert "src/main/java/.../ProductController.java" in files

def test_detect_file_conflicts_no_conflicts():
    plans = [
        create_plan(id=1, files=["Product.java"]),
        create_plan(id=2, files=["Order.java"])
    ]
    conflicts = detect_file_conflicts(plans)
    assert len(conflicts) == 0

def test_detect_file_conflicts_with_conflicts():
    plans = [
        create_plan(id=1, files=["Product.java"]),
        create_plan(id=2, files=["Product.java", "Order.java"]),
        create_plan(id=3, files=["Product.java"])
    ]
    conflicts = detect_file_conflicts(plans)
    assert len(conflicts) == 1
    assert conflicts[0].file_path == "Product.java"
    assert set(conflicts[0].conflicting_plan_ids) == {1, 2, 3}

def test_resolve_file_conflicts_serial():
    conflicts = [
        FileConflict(file_path="Product.java", conflicting_plan_ids=[1, 2, 3])
    ]
    plans = [create_plan(id=i) for i in range(1, 4)]
    serial_groups = resolve_file_conflicts_serial(conflicts, plans)

    assert len(serial_groups) == 1
    assert len(serial_groups[0].plans) == 3
```

### Integration Tests

```python
def test_end_to_end_conflict_detection():
    """Test complete conflict detection workflow."""
    # Create test plans with overlapping files
    plan1 = create_plan_from_content("""
    # Product CRUD Implementation
    Files to modify:
    - src/main/java/.../ProductController.java
    - src/main/java/.../ProductService.java
    """)

    plan2 = create_plan_from_content("""
    # Controller Vavr Migration
    Files to modify:
    - src/main/java/.../ProductController.java
    - src/main/java/.../OrderController.java
    """)

    plans = [plan1, plan2]

    # Run conflict detection
    file_conflicts = detect_file_conflicts(plans)

    # Verify conflict detected
    assert len(file_conflicts) == 1
    assert "ProductController.java" in file_conflicts[0].file_path

    # Generate report
    report = generate_conflict_report(plans, file_conflicts)
    assert "Conflict #1" in report
    assert "ProductController.java" in report
```

---

## Future Enhancements (v1.1.0+)

### Module-Level Conflict Detection

```python
def extract_affected_modules(content: str, plan_file_path: str) -> Set[str]:
    """
    Extract affected modules from plan content.

    Strategies:
    1. Java package analysis: net.lab1024.sa.admin.module.business.{module}.*
    2. Vue path analysis: smart-admin-web/src/views/{module}/*
    3. Explicit module metadata in YAML frontmatter
    """
    modules = set()

    # Strategy 1: Extract from Java package names
    java_package_pattern = r'net\.lab1024\.sa\.admin\.module\.business\.(\w+)'
    matches = re.findall(java_package_pattern, content)
    modules.update(matches)

    # Strategy 2: Extract from Vue module paths
    vue_path_pattern = r'smart-admin-web/src/views/(\w+)'
    matches = re.findall(vue_path_pattern, content)
    modules.update(matches)

    # Strategy 3: Extract from YAML frontmatter
    frontmatter = extract_yaml_frontmatter(content)
    if frontmatter and 'module' in frontmatter:
        modules.add(frontmatter['module'])

    return modules
```

### Dependency Graph Analysis

```python
def extract_dependencies(plan: Plan) -> List[str]:
    """
    Extract dependencies from plan metadata or content.

    Strategies:
    1. Explicit dependencies in YAML frontmatter
    2. Implicit dependencies from "Prerequisites" section
    3. Inferred dependencies from file/module references
    """
    dependencies = []

    # Strategy 1: YAML frontmatter
    if plan.metadata and 'dependencies' in plan.metadata:
        dependencies.extend(plan.metadata['dependencies'])

    # Strategy 2: Prerequisites section
    prereq_pattern = r'###\s*Prerequisites\s*\n((?:-\s*[^\n]+\n?)+)'
    matches = re.findall(prereq_pattern, plan.content, re.IGNORECASE)
    for match in matches:
        prereq_list = re.findall(r'-\s*([^\n]+)', match)
        dependencies.extend(prereq_list)

    return dependencies
```

---

## Next Steps

After completing Phase 2, proceed to:
- **[Phase 3: Execution Planning](phase-3-execution-planning.md)** - Generate optimal execution plan with parallelization
- **[Phase 4: Parallel Execution](phase-4-parallel-execution.md)** - Execute plans with monitoring and progress tracking

---

**Phase Status**: ✅ Complete (v1.0.0 MVP - File-level conflicts only)
**Documentation Version**: v1.0.0
**Last Updated**: 2026-01-29
