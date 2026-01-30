# XML Mapper Best Practices

Complete guide to writing efficient and maintainable MyBatis XML mappers for SmartAdmin.

## When to Use XML Mappers

**Use XML Mappers for:**
- Multi-table JOIN queries
- Complex WHERE conditions with dynamic SQL
- Subqueries and UNION
- Performance-critical queries requiring manual optimization
- Complex aggregations with GROUP BY
- Custom result mapping

**Use LambdaQueryWrapper for:**
- Single table CRUD
- Simple conditions (≤ 5 fields)
- Standard pagination queries

## File Structure

### Directory Layout

```
resources/mapper/
├── business/
│   ├── order/
│   │   └── OrderMapper.xml
│   └── goods/
│       └── GoodsMapper.xml
├── system/
│   ├── employee/
│   │   └── EmployeeMapper.xml
│   └── department/
│       └── DepartmentMapper.xml
└── support/
    └── dict/
        └── DictMapper.xml
```

### XML Header

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="net.lab1024.sa.admin.module.system.employee.dao.EmployeeDao">

    <!-- SQL statements here -->

</mapper>
```

## Basic Query Patterns

### Simple SELECT

```xml
<select id="getByLoginName" resultType="net.lab1024.sa.admin.module.system.employee.domain.entity.EmployeeEntity">
    SELECT *
    FROM t_employee
    WHERE login_name = #{loginName}
      AND deleted_flag = #{deletedFlag}
</select>
```

### SELECT with Multiple Parameters

```xml
<select id="queryEmployee" resultType="net.lab1024.sa.admin.module.system.employee.domain.vo.EmployeeVO">
    SELECT
        e.*,
        d.department_name
    FROM t_employee e
    LEFT JOIN t_department d ON d.department_id = e.department_id
    WHERE e.deleted_flag = false
      AND e.actual_name = #{queryForm.actualName}
      AND e.department_id IN
          <foreach collection="departmentIdList" open="(" close=")" separator="," item="item">
              #{item}
          </foreach>
</select>
```

## Dynamic SQL

### `<if>` Conditional SQL

```xml
<select id="queryEmployee" resultType="EmployeeVO">
    SELECT *
    FROM t_employee
    <where>
        <if test="queryForm.keyword != null and queryForm.keyword != ''">
            AND (
                INSTR(actual_name, #{queryForm.keyword})
                OR INSTR(phone, #{queryForm.keyword})
                OR INSTR(login_name, #{queryForm.keyword})
            )
        </if>
        <if test="departmentIdList != null and departmentIdList.size > 0">
            AND department_id IN
            <foreach collection="departmentIdList" open="(" close=")" separator="," item="item">
                #{item}
            </foreach>
        </if>
        <if test="queryForm.disabledFlag != null">
            AND disabled_flag = #{queryForm.disabledFlag}
        </if>
        <if test="queryForm.deletedFlag != null">
            AND deleted_flag = #{queryForm.deletedFlag}
        </if>
    </where>
    ORDER BY create_time DESC
</select>
```

**Key Points:**
- `<where>` automatically removes leading AND/OR
- Check `!= null` AND `!= ''` for String parameters
- Use `collection.size > 0` for Collection parameters

### `<choose>` Switch-Case Logic

```xml
<select id="queryOrders" resultType="OrderVO">
    SELECT *
    FROM t_order
    <where>
        <choose>
            <when test="queryForm.status == 1">
                AND status = 'PENDING'
            </when>
            <when test="queryForm.status == 2">
                AND status = 'PROCESSING'
            </when>
            <when test="queryForm.status == 3">
                AND status IN ('COMPLETED', 'CANCELLED')
            </when>
            <otherwise>
                AND status IS NOT NULL
            </otherwise>
        </choose>
    </where>
</select>
```

### `<foreach>` Collection Iteration

**IN clause:**
```xml
<select id="getEmployeesByIds" resultType="EmployeeVO">
    SELECT *
    FROM t_employee
    WHERE employee_id IN
    <foreach collection="employeeIds" open="(" close=")" separator="," item="id">
        #{id}
    </foreach>
      AND deleted_flag = false
</select>
```

**Batch INSERT:**
```xml
<insert id="batchInsert">
    INSERT INTO t_employee (actual_name, department_id, phone)
    VALUES
    <foreach collection="employees" separator="," item="item">
        (#{item.actualName}, #{item.departmentId}, #{item.phone})
    </foreach>
</insert>
```

### `<set>` Dynamic UPDATE

```xml
<update id="updateEmployee">
    UPDATE t_employee
    <set>
        <if test="actualName != null and actualName != ''">
            actual_name = #{actualName},
        </if>
        <if test="phone != null">
            phone = #{phone},
        </if>
        <if test="email != null">
            email = #{email},
        </if>
        update_time = NOW()
    </set>
    WHERE employee_id = #{employeeId}
</update>
```

**Key Point:** `<set>` automatically removes trailing commas.

## JOIN Queries

### LEFT JOIN

```xml
<select id="queryEmployee" resultType="EmployeeVO">
    SELECT
        e.employee_id,
        e.actual_name,
        e.phone,
        d.department_name,
        p.position_name
    FROM t_employee e
    LEFT JOIN t_department d ON d.department_id = e.department_id
    LEFT JOIN t_position p ON p.position_id = e.position_id
    WHERE e.deleted_flag = false
</select>
```

### Multiple JOINs with Filters

```xml
<select id="queryOrderWithDetails" resultType="OrderVO">
    SELECT
        o.order_id,
        o.order_no,
        o.amount,
        u.user_name,
        p.product_name
    FROM t_order o
    LEFT JOIN t_user u ON u.user_id = o.user_id
    LEFT JOIN t_product p ON p.product_id = o.product_id
    <where>
        <if test="queryForm.orderNo != null">
            AND o.order_no = #{queryForm.orderNo}
        </if>
        <if test="queryForm.userId != null">
            AND o.user_id = #{queryForm.userId}
        </if>
        <if test="queryForm.startDate != null">
            AND o.create_time >= #{queryForm.startDate}
        </if>
        <if test="queryForm.endDate != null">
            AND o.create_time &lt;= #{queryForm.endDate}
        </if>
    </where>
    ORDER BY o.create_time DESC
</select>
```

**XML Escaping:**
- `<` → `&lt;`
- `>` → `&gt;`
- `&` → `&amp;`

## ResultMap

### Basic ResultMap

```xml
<resultMap id="EmployeeResultMap" type="net.lab1024.sa.admin.module.system.employee.domain.vo.EmployeeVO">
    <id property="employeeId" column="employee_id"/>
    <result property="actualName" column="actual_name"/>
    <result property="departmentName" column="department_name"/>
    <result property="positionName" column="position_name"/>
</resultMap>

<select id="queryEmployee" resultMap="EmployeeResultMap">
    SELECT
        e.employee_id,
        e.actual_name,
        d.department_name,
        p.position_name
    FROM t_employee e
    LEFT JOIN t_department d ON d.department_id = e.department_id
    LEFT JOIN t_position p ON p.position_id = e.position_id
</select>
```

### Nested ResultMap (One-to-Many)

```xml
<resultMap id="DepartmentWithEmployeesMap" type="DepartmentVO">
    <id property="departmentId" column="department_id"/>
    <result property="departmentName" column="department_name"/>
    <collection property="employees" ofType="EmployeeVO">
        <id property="employeeId" column="employee_id"/>
        <result property="actualName" column="actual_name"/>
        <result property="phone" column="phone"/>
    </collection>
</resultMap>

<select id="getDepartmentWithEmployees" resultMap="DepartmentWithEmployeesMap">
    SELECT
        d.department_id,
        d.department_name,
        e.employee_id,
        e.actual_name,
        e.phone
    FROM t_department d
    LEFT JOIN t_employee e ON e.department_id = d.department_id
    WHERE d.department_id = #{departmentId}
      AND e.deleted_flag = false
</select>
```

## Aggregation Queries

### COUNT with GROUP BY

```xml
<select id="countEmployeeByDepartment" resultType="java.util.Map">
    SELECT
        department_id AS departmentId,
        COUNT(*) AS employeeCount
    FROM t_employee
    WHERE deleted_flag = false
    GROUP BY department_id
</select>
```

### SUM and AVG

```xml
<select id="getOrderStatistics" resultType="OrderStatisticsVO">
    SELECT
        COUNT(*) AS orderCount,
        SUM(amount) AS totalAmount,
        AVG(amount) AS avgAmount,
        MAX(amount) AS maxAmount,
        MIN(amount) AS minAmount
    FROM t_order
    WHERE user_id = #{userId}
      AND create_time >= #{startDate}
</select>
```

## Pagination

### MyBatis Plus Pagination

```xml
<!-- No special syntax needed - MyBatis Plus handles it automatically -->
<select id="queryEmployee" resultType="EmployeeVO">
    SELECT *
    FROM t_employee
    <where>
        <if test="queryForm.keyword != null">
            AND actual_name LIKE CONCAT('%', #{queryForm.keyword}, '%')
        </if>
    </where>
    ORDER BY create_time DESC
</select>
```

**In Dao:**
```java
List<EmployeeVO> queryEmployee(Page<?> page, @Param("queryForm") EmployeeQueryForm queryForm);
```

**MyBatis Plus automatically adds:**
```sql
LIMIT #{offset}, #{pageSize}
```

## Performance Optimization

### SELECT Specific Columns

```xml
<!-- ❌ Bad - Fetches all columns -->
<select id="getEmployeeList" resultType="EmployeeVO">
    SELECT *
    FROM t_employee
</select>

<!-- ✅ Good - Only fetch needed columns -->
<select id="getEmployeeList" resultType="EmployeeVO">
    SELECT employee_id, actual_name, phone
    FROM t_employee
</select>
```

### Use EXISTS Instead of COUNT

```xml
<!-- ❌ Slow - Counts all matching rows -->
<select id="checkEmployeeExists" resultType="int">
    SELECT COUNT(*)
    FROM t_employee
    WHERE login_name = #{loginName}
</select>

<!-- ✅ Fast - Stops at first match -->
<select id="checkEmployeeExists" resultType="int">
    SELECT EXISTS(
        SELECT 1
        FROM t_employee
        WHERE login_name = #{loginName}
        LIMIT 1
    )
</select>
```

### Index Hints

```xml
<select id="queryOrders" resultType="OrderVO">
    SELECT *
    FROM t_order USE INDEX (idx_user_id_create_time)
    WHERE user_id = #{userId}
      AND create_time >= #{startDate}
    ORDER BY create_time DESC
</select>
```

### Avoid SELECT IN Subquery

```xml
<!-- ❌ Slow - Subquery executed for each row -->
<select id="getEmployeesInActiveDepartments" resultType="EmployeeVO">
    SELECT *
    FROM t_employee
    WHERE department_id IN (
        SELECT department_id
        FROM t_department
        WHERE active_flag = true
    )
</select>

<!-- ✅ Fast - Single JOIN -->
<select id="getEmployeesInActiveDepartments" resultType="EmployeeVO">
    SELECT e.*
    FROM t_employee e
    INNER JOIN t_department d ON d.department_id = e.department_id
    WHERE d.active_flag = true
</select>
```

## SQL Injection Prevention

### ✅ Safe Parameter Binding

```xml
<!-- ✅ SAFE - Uses prepared statement -->
<select id="getByLoginName" resultType="EmployeeEntity">
    SELECT *
    FROM t_employee
    WHERE login_name = #{loginName}
</select>
```

### ❌ Unsafe String Concatenation

```xml
<!-- ❌ DANGEROUS - SQL Injection risk! -->
<select id="searchByKeyword" resultType="EmployeeVO">
    SELECT *
    FROM t_employee
    WHERE actual_name LIKE '%${keyword}%'
</select>

<!-- ✅ SAFE - Use #{} instead -->
<select id="searchByKeyword" resultType="EmployeeVO">
    SELECT *
    FROM t_employee
    WHERE actual_name LIKE CONCAT('%', #{keyword}, '%')
</select>
```

### When to Use `${}`

**Only use `${}` for:**
- Table names (dynamic table routing)
- Column names (dynamic sorting)
- SQL keywords (ASC/DESC)

```xml
<!-- ✅ Acceptable - But validate input first! -->
<select id="dynamicSort" resultType="EmployeeVO">
    SELECT *
    FROM t_employee
    ORDER BY ${sortColumn} ${sortOrder}
</select>
```

**Always validate:**
```java
// In Service layer
if (!Arrays.asList("create_time", "actual_name").contains(sortColumn)) {
    throw new BusinessException("Invalid sort column");
}
if (!Arrays.asList("ASC", "DESC").contains(sortOrder)) {
    throw new BusinessException("Invalid sort order");
}
```

## Best Practices

### 1. Use `<where>` for Dynamic Conditions

```xml
<!-- ✅ Good - Automatically handles AND/OR -->
<select id="queryEmployee" resultType="EmployeeVO">
    SELECT * FROM t_employee
    <where>
        <if test="name != null">
            AND actual_name = #{name}
        </if>
        <if test="deptId != null">
            AND department_id = #{deptId}
        </if>
    </where>
</select>
```

### 2. Use INSTR for Keyword Search

```xml
<!-- SmartAdmin pattern for multi-field search -->
<if test="queryForm.keyword != null and queryForm.keyword != ''">
    AND (
        INSTR(actual_name, #{queryForm.keyword})
        OR INSTR(phone, #{queryForm.keyword})
        OR INSTR(login_name, #{queryForm.keyword})
    )
</if>
```

### 3. Always Filter Soft Deletes

```xml
<!-- ✅ Good - Explicitly filter deleted records -->
<select id="queryEmployee" resultType="EmployeeVO">
    SELECT *
    FROM t_employee
    WHERE deleted_flag = false
</select>

<!-- OR use parameter -->
<select id="queryEmployee" resultType="EmployeeVO">
    SELECT *
    FROM t_employee
    <where>
        <if test="queryForm.deletedFlag != null">
            AND deleted_flag = #{queryForm.deletedFlag}
        </if>
    </where>
</select>
```

### 4. Use Meaningful Result Type

```xml
<!-- ❌ Bad - Generic Map -->
<select id="getEmployee" resultType="java.util.HashMap">
    SELECT * FROM t_employee
</select>

<!-- ✅ Good - Type-safe VO -->
<select id="getEmployee" resultType="net.lab1024.sa.admin.module.system.employee.domain.vo.EmployeeVO">
    SELECT * FROM t_employee
</select>
```

## Common Patterns

### Standard Pagination Query

```xml
<select id="queryEmployee" resultType="EmployeeVO">
    SELECT
        e.*,
        d.department_name
    FROM t_employee e
    LEFT JOIN t_department d ON d.department_id = e.department_id
    <where>
        <if test="queryForm.keyword != null and queryForm.keyword != ''">
            AND (
                INSTR(e.actual_name, #{queryForm.keyword})
                OR INSTR(e.phone, #{queryForm.keyword})
                OR INSTR(e.login_name, #{queryForm.keyword})
            )
        </if>
        <if test="departmentIdList != null and departmentIdList.size > 0">
            AND e.department_id IN
            <foreach collection="departmentIdList" open="(" close=")" separator="," item="item">
                #{item}
            </foreach>
        </if>
        <if test="queryForm.disabledFlag != null">
            AND e.disabled_flag = #{queryForm.disabledFlag}
        </if>
        <if test="queryForm.deletedFlag != null">
            AND e.deleted_flag = #{queryForm.deletedFlag}
        </if>
    </where>
    ORDER BY e.create_time DESC
</select>
```

## Anti-Patterns

### ❌ Anti-Pattern 1: SELECT * in Production

```xml
<!-- ❌ Bad -->
<select id="getOrders" resultType="OrderVO">
    SELECT * FROM t_order
</select>

<!-- ✅ Good -->
<select id="getOrders" resultType="OrderVO">
    SELECT order_id, order_no, user_id, amount, status, create_time
    FROM t_order
</select>
```

### ❌ Anti-Pattern 2: Missing WHERE on UPDATE/DELETE

```xml
<!-- ❌ DANGEROUS - Could update all rows! -->
<update id="updateEmployee">
    UPDATE t_employee
    SET actual_name = #{actualName}
</update>

<!-- ✅ Safe - Always include WHERE -->
<update id="updateEmployee">
    UPDATE t_employee
    SET actual_name = #{actualName}
    WHERE employee_id = #{employeeId}
</update>
```

### ❌ Anti-Pattern 3: Hardcoded Strings

```xml
<!-- ❌ Bad - Magic numbers/strings -->
<select id="getPendingOrders" resultType="OrderVO">
    SELECT * FROM t_order WHERE status = 1
</select>

<!-- ✅ Good - Use Enum values -->
<select id="getPendingOrders" resultType="OrderVO">
    SELECT * FROM t_order WHERE status = #{statusEnum.value}
</select>
```

## Related Documentation

- [Dao Architecture Guide](dao-architecture.md)
- [LambdaQueryWrapper Guide](lambda-wrapper-guide.md)
- [Performance Optimization](performance-optimization.md)
