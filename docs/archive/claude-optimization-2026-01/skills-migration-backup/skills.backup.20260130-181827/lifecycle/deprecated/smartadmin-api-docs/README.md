# [已廢棄] smartadmin-api-docs

## ⚠️ 廢棄通知

- **軟廢棄時間**：2026-01-27
- **硬廢棄時間**：2026-06-30（屆時移除）
- **廢棄原因**：功能已整合至 `smartadmin-crud-generator`（包含 Knife4j 文檔生成）

---

## 🔄 遷移路徑

**新技能**：`smartadmin-crud-generator`（包含 API 文檔生成）

### 主要差異

| 特性 | 舊技能（api-docs） | 新技能（crud-generator） |
|------|------------------|------------------------|
| 文檔生成 | 手動編寫 | 自動生成（註解驅動） |
| 工具 | Swagger 2 | Knife4j 4（基於 Swagger 3） |
| 維護成本 | 高（需手動同步） | 低（代碼即文檔） |
| 測試集成 | 無 | 支持 API 測試生成 |
| 執行時間 | 5 分鐘 | 3 分鐘（docs-only 模式） |

---

## 🔧 遷移步驟

### Step 1: 使用新技能生成新模塊（包含 Knife4j 註解）

```bash
# 舊命令（已廢棄）
/api-docs ProductController

# 新命令（推薦）
/crud Product --docs-only
```

---

### Step 2: 將現有 API 添加 Knife4j 註解

**舊方式（手動編寫文檔）**：
```java
// 無註解，需手動編寫 API 文檔
@RestController
@RequestMapping("/api/employee")
public class EmployeeController {
    @PostMapping("/add")
    public ResponseDTO<Long> add(@RequestBody EmployeeAddForm form) {
        return ResponseDTO.ok(employeeService.add(form));
    }
}
```

**新方式（註解驅動）**：
```java
@RestController
@RequestMapping("/api/employee")
@Tag(name = "員工管理", description = "員工 CRUD 接口")
public class EmployeeController {

    @PostMapping("/add")
    @Operation(summary = "新增員工", description = "創建新的員工記錄")
    @ApiOperationSupport(order = 1)
    public ResponseDTO<Long> add(
        @RequestBody
        @Parameter(description = "員工信息", required = true)
        EmployeeAddForm form
    ) {
        return ResponseDTO.ok(employeeService.add(form));
    }
}
```

**Form 類添加註解**：
```java
@Schema(description = "員工新增表單")
public class EmployeeAddForm {

    @Schema(description = "員工姓名", required = true, example = "張三")
    @NotBlank(message = "姓名不能為空")
    @Size(max = 50, message = "姓名長度不能超過50")
    private String name;

    @Schema(description = "部門ID", required = true, example = "1")
    @NotNull(message = "部門ID不能為空")
    private Long departmentId;
}
```

---

### Step 3: 驗證文檔可訪問性

**訪問 Knife4j 文檔**：
```
http://localhost:1024/doc.html
```

**驗證內容**：
- 接口列表（Controller 分組）
- 請求參數（Field 描述、示例）
- 響應格式（ResponseDTO 結構）
- 在線調試（Try it out）

---

### Step 4: 移除舊的手動文檔文件

```bash
# 刪除舊的手動編寫的文檔
rm docs/api/employee-api.md
rm docs/api/product-api.md
```

---

## 📚 Knife4j 註解速查

### Controller 層註解

| 註解 | 說明 | 示例 |
|-----|------|------|
| `@Tag` | API 分組 | `@Tag(name = "員工管理")` |
| `@Operation` | 接口描述 | `@Operation(summary = "新增員工")` |
| `@ApiOperationSupport` | 接口排序 | `@ApiOperationSupport(order = 1)` |

### 參數層註解

| 註解 | 說明 | 示例 |
|-----|------|------|
| `@Parameter` | 參數描述 | `@Parameter(description = "員工ID")` |
| `@Schema` | 對象描述 | `@Schema(description = "員工姓名")` |

---

## 📚 相關資源

- **新技能文檔**：[smartadmin-crud-generator](../../foundation/full-stack/smartadmin-crud-generator/README.md)
- **Knife4j 官方文檔**：[Knife4j Documentation](https://doc.xiaominfo.com/)
- **OpenAPI 3.0 規範**：[OpenAPI Specification](https://swagger.io/specification/)

---

## 📞 需要幫助？

如果遷移過程中遇到問題，請：
1. 查看 [Knife4j Documentation](https://doc.xiaominfo.com/)
2. 查看 [smartadmin-crud-generator examples](../../foundation/full-stack/smartadmin-crud-generator/examples/)
3. 提交 Issue：[GitHub Issues](https://github.com/1024-lab/smart-admin/issues)

---

**Last Updated**: 2026-01-30
