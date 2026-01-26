# Phase 1: Backend Layer Generation

**Purpose**: Generate complete backend CRUD implementation following SmartAdmin layered architecture.

**Integration**: This phase consolidates logic from `smartadmin-mybatis` skill.

---

## Execution Order

1. **Entity** (Domain layer)
2. **Dao** (Data access layer) - Integrated from smartadmin-mybatis
3. **Manager** (Transaction layer)
4. **Service** (Business logic layer)
5. **Controller** (API layer)

---

## 1. Generate Entity

**File**: `sa-admin/src/main/java/net/lab1024/sa/admin/module/business/{module}/domain/entity/{Entity}Entity.java`

**Pattern** (from smartadmin-mybatis):
```java
package net.lab1024.sa.admin.module.business.{module}.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * {EntityName} Entity
 *
 * @author {author}
 * @date {date}
 */
@Data
@TableName("t_{table_name}")
public class {Entity}Entity {

    /**
     * Primary Key
     */
    @TableId(type = IdType.AUTO)
    private Long {entity}Id;

    // Generated fields based on entity specification

    /**
     * Deleted flag (0: not deleted, 1: deleted)
     */
    @TableLogic
    private Boolean deletedFlag;

    /**
     * Created by
     */
    private Long createUserId;

    /**
     * Create time
     */
    private LocalDateTime createTime;

    /**
     * Updated by
     */
    private Long updateUserId;

    /**
     * Update time
     */
    private LocalDateTime updateTime;
}
```

**Key Features** (from smartadmin-mybatis):
- ✅ Use `@TableName` for table mapping
- ✅ Use `@TableId(type = IdType.AUTO)` for auto-increment primary key
- ✅ Use `@TableLogic` for soft delete field
- ✅ Field naming: `deletedFlag` NOT `isDeleted` (ArchUnit enforced)
- ✅ Timestamp fields: `createTime`, `updateTime` (LocalDateTime)

---

## 2. Generate Dao

**File**: `sa-admin/src/main/java/net/lab1024/sa/admin/module/business/{module}/dao/{Entity}Dao.java`

**Pattern** (from smartadmin-mybatis):
```java
package net.lab1024.sa.admin.module.business.{module}.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import net.lab1024.sa.admin.module.business.{module}.domain.entity.{Entity}Entity;
import net.lab1024.sa.admin.module.business.{module}.domain.form.{Entity}QueryForm;
import net.lab1024.sa.admin.module.business.{module}.domain.vo.{Entity}VO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * {EntityName} Dao
 *
 * @author {author}
 * @date {date}
 */
@Mapper
public interface {Entity}Dao extends BaseMapper<{Entity}Entity> {

    /**
     * Query with pagination
     */
    List<{Entity}VO> query(
        @Param("page") Page page,
        @Param("queryForm") {Entity}QueryForm queryForm
    );

    /**
     * Query by ID
     */
    {Entity}VO queryById(@Param("{entity}Id") Long {entity}Id);
}
```

**Query Strategy** (from smartadmin-mybatis references):

### Simple Queries: Use LambdaQueryWrapper
```java
// For simple conditions (AND only, no complex JOIN)
default List<{Entity}VO> querySimple({Entity}QueryForm queryForm) {
    LambdaQueryWrapper<{Entity}Entity> wrapper = Wrappers.lambdaQuery();
    wrapper.eq(StringUtils.isNotBlank(queryForm.get{Field}()),
               {Entity}Entity::get{Field}, queryForm.get{Field}());
    return selectList(wrapper);
}
```

### Complex Queries: Use XML Mapper
```java
// For complex queries (JOIN, GROUP BY, subqueries)
// Implemented in Mapper.xml
List<{Entity}VO> query(
    @Param("page") Page page,
    @Param("queryForm") {Entity}QueryForm queryForm
);
```

**Mapper.xml** (when needed):
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="net.lab1024.sa.admin.module.business.{module}.dao.{Entity}Dao">

    <select id="query" resultType="net.lab1024.sa.admin.module.business.{module}.domain.vo.{Entity}VO">
        SELECT
            t.{entity}_id,
            t.{field_name},
            ...
        FROM t_{table_name} t
        <where>
            t.deleted_flag = 0
            <if test="queryForm.{field} != null">
                AND t.{field} = #{queryForm.{field}}
            </if>
        </where>
        ORDER BY t.create_time DESC
    </select>

</mapper>
```

---

## 3. Generate Manager

**File**: `sa-admin/src/main/java/net/lab1024/sa/admin/module/business/{module}/manager/{Entity}Manager.java`

**Pattern**:
```java
package net.lab1024.sa.admin.module.business.{module}.manager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.business.{module}.dao.{Entity}Dao;
import net.lab1024.sa.admin.module.business.{module}.domain.entity.{Entity}Entity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {EntityName} Manager
 *
 * @author {author}
 * @date {date}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class {Entity}Manager {

    private final {Entity}Dao {entity}Dao;

    /**
     * Add {entity}
     */
    @Transactional(rollbackFor = Throwable.class)
    public void add({Entity}Entity entity) {
        {entity}Dao.insert(entity);
    }

    /**
     * Update {entity}
     */
    @Transactional(rollbackFor = Throwable.class)
    public void update({Entity}Entity entity) {
        {entity}Dao.updateById(entity);
    }

    /**
     * Delete {entity}
     */
    @Transactional(rollbackFor = Throwable.class)
    public void delete(Long {entity}Id) {
        {entity}Dao.deleteById({entity}Id);
    }

    /**
     * Batch delete {entity}s
     */
    @Transactional(rollbackFor = Throwable.class)
    public void batchDelete(List<Long> {entity}IdList) {
        {entity}Dao.deleteBatchIds({entity}IdList);
    }
}
```

**Key Rules** (ArchUnit enforced):
- ✅ `@Transactional(rollbackFor = Throwable.class)` on all write methods
- ✅ Only Manager layer can have `@Transactional`
- ✅ Constructor injection via `@RequiredArgsConstructor`
- ✅ NEVER use field injection (`@Autowired`)

---

## 4. Generate Service

**File**: `sa-admin/src/main/java/net/lab1024/sa/admin/module/business/{module}/service/{Entity}Service.java`

**Pattern**:
```java
package net.lab1024.sa.admin.module.business.{module}.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.vavr.control.Option;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.foundation.domain.response.PageResult;
import net.lab1024.sa.foundation.domain.response.ResponseDTO;
import net.lab1024.sa.admin.module.business.{module}.dao.{Entity}Dao;
import net.lab1024.sa.admin.module.business.{module}.domain.entity.{Entity}Entity;
import net.lab1024.sa.admin.module.business.{module}.domain.form.*;
import net.lab1024.sa.admin.module.business.{module}.domain.vo.{Entity}VO;
import net.lab1024.sa.admin.module.business.{module}.manager.{Entity}Manager;
import net.lab1024.sa.common.core.util.SmartBeanUtil;
import net.lab1024.sa.common.core.util.SmartPageUtil;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * {EntityName} Service
 *
 * @author {author}
 * @date {date}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class {Entity}Service {

    private final {Entity}Dao {entity}Dao;
    private final {Entity}Manager {entity}Manager;

    /**
     * Query with pagination
     */
    public ResponseDTO<PageResult<{Entity}VO>> query({Entity}QueryForm queryForm) {
        Page<?> page = SmartPageUtil.convert2PageQuery(queryForm);
        List<{Entity}VO> list = {entity}Dao.query(page, queryForm);
        PageResult<{Entity}VO> pageResult = SmartPageUtil.convert2PageResult(page, list);
        return ResponseDTO.ok(pageResult);
    }

    /**
     * Get by ID
     */
    public ResponseDTO<{Entity}VO> getById(Long {entity}Id) {
        return Option.of({entity}Dao.queryById({entity}Id))
            .map(ResponseDTO::ok)
            .getOrElse(() -> ResponseDTO.userErrorParam("{Entity} not found"));
    }

    /**
     * Add {entity}
     */
    public ResponseDTO<Void> add({Entity}AddForm addForm) {
        {Entity}Entity entity = SmartBeanUtil.copy(addForm, {Entity}Entity.class);
        {entity}Manager.add(entity);
        return ResponseDTO.ok();
    }

    /**
     * Update {entity}
     */
    public ResponseDTO<Void> update({Entity}UpdateForm updateForm) {
        // Validate existence
        return Option.of({entity}Dao.selectById(updateForm.get{Entity}Id()))
            .map(existingEntity -> {
                {Entity}Entity entity = SmartBeanUtil.copy(updateForm, {Entity}Entity.class);
                {entity}Manager.update(entity);
                return ResponseDTO.ok();
            })
            .getOrElse(() -> ResponseDTO.userErrorParam("{Entity} not found"));
    }

    /**
     * Delete {entity}
     */
    public ResponseDTO<Void> delete(Long {entity}Id) {
        return Option.of({entity}Dao.selectById({entity}Id))
            .map(entity -> {
                {entity}Manager.delete({entity}Id);
                return ResponseDTO.ok();
            })
            .getOrElse(() -> ResponseDTO.userErrorParam("{Entity} not found"));
    }

    /**
     * Batch delete {entity}s
     */
    public ResponseDTO<Void> batchDelete({Entity}BatchDeleteForm batchDeleteForm) {
        {entity}Manager.batchDelete(batchDeleteForm.get{Entity}IdList());
        return ResponseDTO.ok();
    }
}
```

**Key Rules** (ArchUnit enforced):
- ✅ Use `io.vavr.control.Option` (NOT `java.util.Optional`)
- ✅ All methods return `ResponseDTO`
- ✅ Use `SmartBeanUtil.copy()` for entity conversion
- ✅ Use `SmartPageUtil` for pagination
- ✅ Constructor injection only

---

## 5. Generate Controller

**File**: `sa-admin/src/main/java/net/lab1024/sa/admin/module/business/{module}/controller/{Entity}Controller.java`

**Pattern**:
```java
package net.lab1024.sa.admin.module.business.{module}.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.foundation.domain.response.PageResult;
import net.lab1024.sa.foundation.domain.response.ResponseDTO;
import net.lab1024.sa.admin.module.business.{module}.domain.form.*;
import net.lab1024.sa.admin.module.business.{module}.domain.vo.{Entity}VO;
import net.lab1024.sa.admin.module.business.{module}.service.{Entity}Service;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * {EntityName} Controller
 *
 * @author {author}
 * @date {date}
 */
@RestController
@RequiredArgsConstructor
public class {Entity}Controller {

    private final {Entity}Service {entity}Service;

    @PostMapping("/{module}/{entity}/query")
    @SaCheckPermission("{module}:{entity}:query")
    public ResponseDTO<PageResult<{Entity}VO>> query(@RequestBody @Valid {Entity}QueryForm queryForm) {
        return {entity}Service.query(queryForm);
    }

    @GetMapping("/{module}/{entity}/get/{{{entity}Id}}")
    @SaCheckPermission("{module}:{entity}:query")
    public ResponseDTO<{Entity}VO> getById(@PathVariable Long {entity}Id) {
        return {entity}Service.getById({entity}Id);
    }

    @PostMapping("/{module}/{entity}/add")
    @SaCheckPermission("{module}:{entity}:add")
    public ResponseDTO<Void> add(@RequestBody @Valid {Entity}AddForm addForm) {
        return {entity}Service.add(addForm);
    }

    @PostMapping("/{module}/{entity}/update")
    @SaCheckPermission("{module}:{entity}:update")
    public ResponseDTO<Void> update(@RequestBody @Valid {Entity}UpdateForm updateForm) {
        return {entity}Service.update(updateForm);
    }

    @PostMapping("/{module}/{entity}/delete/{{{entity}Id}}")
    @SaCheckPermission("{module}:{entity}:delete")
    public ResponseDTO<Void> delete(@PathVariable Long {entity}Id) {
        return {entity}Service.delete({entity}Id);
    }

    @PostMapping("/{module}/{entity}/batchDelete")
    @SaCheckPermission("{module}:{entity}:delete")
    public ResponseDTO<Void> batchDelete(@RequestBody @Valid {Entity}BatchDeleteForm batchDeleteForm) {
        return {entity}Service.batchDelete(batchDeleteForm);
    }
}
```

**Key Rules**:
- ✅ Controller NEVER directly accesses Dao (must go through Service)
- ✅ All endpoints have `@SaCheckPermission`
- ✅ Use `@Valid` for request body validation
- ✅ Constructor injection only

---

## Domain Objects (Supporting Files)

### QueryForm
```java
@Data
public class {Entity}QueryForm extends PageParam {
    @Schema(description = "Keyword")
    private String keyword;

    // Generated fields based on queryable fields
}
```

### AddForm
```java
@Data
public class {Entity}AddForm {
    @Schema(description = "{Field description}", required = true)
    @NotBlank(message = "{Field} cannot be empty")
    private String {field};

    // Generated fields
}
```

### UpdateForm
```java
@Data
public class {Entity}UpdateForm {
    @Schema(description = "ID", required = true)
    @NotNull(message = "ID cannot be null")
    private Long {entity}Id;

    // Same fields as AddForm
}
```

### BatchDeleteForm
```java
@Data
public class {Entity}BatchDeleteForm {
    @Schema(description = "ID list", required = true)
    @NotEmpty(message = "ID list cannot be empty")
    private List<Long> {entity}IdList;
}
```

### VO (Value Object)
```java
@Data
public class {Entity}VO {
    private Long {entity}Id;

    // All entity fields for display

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

---

## Package Structure

```
sa-admin/src/main/java/net/lab1024/sa/admin/module/business/{module}/
├── controller/
│   └── {Entity}Controller.java
├── service/
│   └── {Entity}Service.java
├── manager/
│   └── {Entity}Manager.java
├── dao/
│   └── {Entity}Dao.java
└── domain/
    ├── entity/
    │   └── {Entity}Entity.java
    ├── form/
    │   ├── {Entity}QueryForm.java
    │   ├── {Entity}AddForm.java
    │   ├── {Entity}UpdateForm.java
    │   └── {Entity}BatchDeleteForm.java
    └── vo/
        └── {Entity}VO.java
```

---

## Validation Checklist

After generation, verify:
- [ ] All classes use correct package naming (v4.0.0+)
- [ ] Constructor injection (`@RequiredArgsConstructor`)
- [ ] Service uses Vavr `Option` (NOT `Optional`)
- [ ] Manager has `@Transactional(rollbackFor = Throwable.class)`
- [ ] Controller has `@SaCheckPermission` on all endpoints
- [ ] Boolean field is `deletedFlag` (NOT `isDeleted`)
- [ ] Run: `./gradlew :sa-admin:test --tests ArchitectureTest`

---

## Integration Notes

This phase consolidates:
- ✅ Entity generation patterns from smartadmin-mybatis
- ✅ Dao layer query strategies from smartadmin-mybatis references
- ✅ LambdaQueryWrapper vs XML Mapper decision logic
- ✅ SmartAdmin layered architecture compliance

**Performance optimization patterns** from smartadmin-mybatis/references/:
- Use LambdaQueryWrapper for simple queries (faster, type-safe)
- Use XML Mapper for complex JOINs (more readable, maintainable)
- Always add `WHERE deleted_flag = 0` for soft delete support
