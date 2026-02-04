# SmartAdmin CRUD Generator - Quick Reference

## 生成架構

```
Backend (9 files)           Frontend (3 files)        Tests (2 files)
─────────────────           ──────────────────        ───────────────
Entity.java                 list.vue                  ServiceTest.java
Dao.java                    form-modal.vue            IntegrationTest.java
Manager.java                api.ts
Service.java
Controller.java
AddForm.java
UpdateForm.java
QueryForm.java
VO.java
```

## 分層規則

| 層級 | 規則 | 實作 |
|------|------|------|
| Controller | 不直接存取 Dao | 只呼叫 Service |
| Service | Vavr Option/Try | `Option<Entity>` 返回 |
| Service | 構造器注入 | `@RequiredArgsConstructor` |
| Manager | @Transactional | 跨表操作 |
| Dao | LambdaQueryWrapper | 型別安全查詢 |
| Entity | BIGSERIAL PK | `@TableId(type = IdType.AUTO)` |

## 命名規範

| 類型 | 格式 | 範例 |
|------|------|------|
| Entity | `{Name}Entity` | `EmployeeEntity` |
| Dao | `{Name}Dao` | `EmployeeDao` |
| Manager | `{Name}Manager` | `EmployeeManager` |
| Service | `{Name}Service` | `EmployeeService` |
| Controller | `{Name}Controller` | `EmployeeController` |
| AddForm | `{Name}AddForm` | `EmployeeAddForm` |
| UpdateForm | `{Name}UpdateForm` | `EmployeeUpdateForm` |
| QueryForm | `{Name}QueryForm` | `EmployeeQueryForm` |
| VO | `{Name}VO` | `EmployeeVO` |

## 關鍵 Import

```java
// Foundation v4.0.0 imports
import net.lab1024.sa.foundation.domain.response.ResponseDTO;
import net.lab1024.sa.foundation.domain.response.PageResult;
import net.lab1024.sa.base.mybatis.util.SmartPageUtil;
import net.lab1024.sa.util.SmartBeanUtil;
import io.vavr.control.Option;
```

## QueryForm 注意事項

```java
// ⚠️ 必須添加 @EqualsAndHashCode(callSuper = false)
@EqualsAndHashCode(callSuper = false)
public class EmployeeQueryForm extends PageParam {
    // fields...
}
```

## 相關規則

- [F04-architecture-rules.md](../../../../rules/foundation/F04-architecture-rules.md)
- [F01-naming-conventions.md](../../../../rules/foundation/F01-naming-conventions.md)
- [F03-manager-layer.md](../../../../rules/foundation/F03-manager-layer.md)
