# Feature Specification: [Feature Name]

**Status**: proposed | applied | archived
**Created**: YYYY-MM-DD
**Version**: v1.0.0
**Author**: [Your Name]

---

## 1. Executive Summary

[簡要描述此功能的業務價值與技術目標]

---

## 2. SmartAdmin Architecture Mapping

### Controller Layer
- **Endpoint**: `POST /api/system/[module]/[action]`
- **Permission**: `@SaCheckPermission("[module]:[action]")`
- **ResponseDTO**: `ResponseDTO.ok(data)` | `ResponseDTO.error(errorCode)`

### Service Layer
- **Service Class**: `[Module]Service`
- **Return Type**: `Option<[Entity]>` (使用 Vavr Option)
- **Business Logic**: [描述業務邏輯]

### Manager Layer
- **Manager Class**: `[Module]Manager` (僅在需要 @Transactional 時)
- **Transaction Scope**: `@Transactional(rollbackFor = Throwable.class)`
- **Cache Strategy**: `@Cacheable("[cache-name]")` (如適用)

### Dao Layer
- **Mapper Interface**: `[Module]Dao extends BaseMapper<[Entity]>`
- **Query Methods**: [列出自訂查詢方法]

---

## 3. Database Schema

```sql
CREATE TABLE t_[module] (
    [module]_id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,  -- 多租戶隔離
    -- 其他欄位...
    deleted SMALLINT DEFAULT 0,  -- 軟刪除標記(注意:使用 deleted 而非 isDeleted)
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tenant ON t_[module](tenant_id);
```

---

## 4. Test Strategy

### Unit Tests
- **Coverage Target**: >85%
- **Test Class**: `[Module]ServiceTest`, `[Module]ManagerTest`

### Integration Tests
- **Testcontainers**: PostgreSQL 16
- **Test Class**: `[Module]IntegrationTest`

### Architecture Tests
- **ArchUnit Rules**: [列出相關架構規則]

---

## 5. Acceptance Criteria

- [ ] Controller 遵循 ResponseDTO 模式
- [ ] Service 使用 Vavr Option 而非 java.util.Optional
- [ ] Manager 層正確使用 @Transactional
- [ ] ArchitectureTest 全數通過
- [ ] 測試覆蓋率 >85%
- [ ] SpotBugs/PMD 無 P0 違規

---

## 6. Related Specs

- [相關規格文件連結]

---

**Spec Lifecycle**:
- Proposed: [Date]
- Applied: [Date]
- Archived: [Date]
