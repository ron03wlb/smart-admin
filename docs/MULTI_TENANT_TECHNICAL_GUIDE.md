# SmartAdmin 多商户技术实现详细指南

**文档版本**: v1.0
**创建日期**: 2026-01-21
**关联文档**: [MULTI_TENANT_REQUIREMENTS.md](./MULTI_TENANT_REQUIREMENTS.md)

---

## 📋 目录

- [1. 核心组件实现](#1-核心组件实现)
- [2. 数据隔离实现方案](#2-数据隔离实现方案)
- [3. 登录流程改造](#3-登录流程改造)
- [4. 缓存隔离实现](#4-缓存隔离实现)
- [5. 权限体系集成](#5-权限体系集成)
- [6. 错误处理与异常](#6-错误处理与异常)
- [7. 测试策略](#7-测试策略)
- [8. 部署与配置](#8-部署与配置)

---

## 1. 核心组件实现

### 1.1 TenantContext - 租户上下文管理

**文件路径**: `sa-base/src/main/java/net/lab1024/sa/base/foundation/tenant/TenantContext.java`

```java
package net.lab1024.sa.base.foundation.tenant;

import lombok.extern.slf4j.Slf4j;

/**
 * 租户上下文 - 基于 ThreadLocal 存储当前请求的租户信息
 *
 * @author SmartAdmin Team
 */
@Slf4j
public class TenantContext {

    /**
     * 租户 ID 存储
     */
    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();

    /**
     * 是否绕过租户过滤（用于平台管理员跨租户查询）
     */
    private static final ThreadLocal<Boolean> BYPASS_TENANT_FILTER = ThreadLocal.withInitial(() -> false);

    /**
     * 设置当前租户 ID
     *
     * @param tenantId 租户 ID
     */
    public static void setTenantId(Long tenantId) {
        if (tenantId == null) {
            log.warn("Attempting to set null tenantId");
            return;
        }
        TENANT_ID.set(tenantId);
        log.debug("TenantContext set: tenantId={}", tenantId);
    }

    /**
     * 获取当前租户 ID
     *
     * @return 租户 ID，可能为 null（平台管理员或未登录场景）
     */
    public static Long getTenantId() {
        return TENANT_ID.get();
    }

    /**
     * 获取当前租户 ID（必须存在）
     *
     * @return 租户 ID
     * @throws IllegalStateException 如果租户 ID 不存在
     */
    public static Long getRequiredTenantId() {
        Long tenantId = TENANT_ID.get();
        if (tenantId == null) {
            throw new IllegalStateException("TenantId is required but not found in context");
        }
        return tenantId;
    }

    /**
     * 是否设置了租户 ID
     *
     * @return true=已设置，false=未设置
     */
    public static boolean hasTenantId() {
        return TENANT_ID.get() != null;
    }

    /**
     * 设置绕过租户过滤（用于平台管理员跨租户查询）
     */
    public static void bypassTenantFilter() {
        BYPASS_TENANT_FILTER.set(true);
        log.debug("TenantContext bypass enabled");
    }

    /**
     * 是否绕过租户过滤
     *
     * @return true=绕过，false=正常过滤
     */
    public static boolean isBypassTenantFilter() {
        return BYPASS_TENANT_FILTER.get();
    }

    /**
     * 执行无租户过滤的操作（用于平台管理员跨租户查询）
     *
     * @param runnable 操作
     */
    public static void runWithoutTenantFilter(Runnable runnable) {
        boolean originalBypass = isBypassTenantFilter();
        try {
            bypassTenantFilter();
            runnable.run();
        } finally {
            BYPASS_TENANT_FILTER.set(originalBypass);
        }
    }

    /**
     * 执行无租户过滤的操作，并返回结果
     *
     * @param supplier 操作
     * @param <T>      返回类型
     * @return 操作结果
     */
    public static <T> T callWithoutTenantFilter(java.util.function.Supplier<T> supplier) {
        boolean originalBypass = isBypassTenantFilter();
        try {
            bypassTenantFilter();
            return supplier.get();
        } finally {
            BYPASS_TENANT_FILTER.set(originalBypass);
        }
    }

    /**
     * 清理当前线程的租户上下文（请求结束时调用）
     */
    public static void clear() {
        TENANT_ID.remove();
        BYPASS_TENANT_FILTER.remove();
        log.debug("TenantContext cleared");
    }
}
```

---

### 1.2 TenantInterceptor - 租户拦截器

**文件路径**: `sa-admin/src/main/java/net/lab1024/sa/admin/interceptor/TenantInterceptor.java`

```java
package net.lab1024.sa.admin.interceptor;

import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.constant.AdminSwaggerTagConst;
import net.lab1024.sa.admin.module.system.tenant.constant.TenantErrorCode;
import net.lab1024.sa.admin.module.system.tenant.domain.entity.TenantEntity;
import net.lab1024.sa.admin.module.system.tenant.service.TenantService;
import net.lab1024.sa.base.common.domain.RequestUser;
import net.lab1024.sa.base.common.exception.BusinessException;
import net.lab1024.sa.base.foundation.tenant.TenantContext;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 租户拦截器
 * <p>
 * 从 Token 中提取租户 ID，验证租户状态，并设置到 ThreadLocal
 *
 * @author SmartAdmin Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantInterceptor implements HandlerInterceptor {

    private final TenantService tenantService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. 检查是否登录
        if (!StpUtil.isLogin()) {
            // 未登录，不设置租户上下文（由 @NoNeedLogin 接口处理）
            return true;
        }

        // 2. 获取用户类型
        Object userTypeObj = StpUtil.getExtra("userType");
        String userType = userTypeObj != null ? userTypeObj.toString() : "employee";

        // 3. 平台管理员：不设置租户 ID，启用绕过模式
        if ("platform_admin".equals(userType)) {
            TenantContext.bypassTenantFilter();
            log.debug("Platform admin detected, bypass tenant filter");
            return true;
        }

        // 4. 商户用户：提取并验证租户 ID
        Long tenantId = getTenantIdFromToken();
        if (tenantId == null) {
            log.error("TenantId not found in token for user: {}", StpUtil.getLoginId());
            throw new BusinessException(TenantErrorCode.TENANT_ID_MISSING);
        }

        // 5. 验证租户状态
        TenantEntity tenant = tenantService.getById(tenantId);
        if (tenant == null) {
            throw new BusinessException(TenantErrorCode.TENANT_NOT_EXIST);
        }
        if (tenant.getStatus() == 4) { // 4=停用
            throw new BusinessException(TenantErrorCode.TENANT_DISABLED);
        }
        if (tenant.getStatus() == 3) { // 3=过期
            throw new BusinessException(TenantErrorCode.TENANT_EXPIRED);
        }

        // 6. 设置租户上下文
        TenantContext.setTenantId(tenantId);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 清理 ThreadLocal，防止内存泄漏
        TenantContext.clear();
    }

    /**
     * 从 Token 中提取租户 ID
     *
     * @return 租户 ID
     */
    private Long getTenantIdFromToken() {
        try {
            Object tenantIdObj = StpUtil.getExtra("tenantId");
            if (tenantIdObj == null) {
                return null;
            }
            if (tenantIdObj instanceof Long) {
                return (Long) tenantIdObj;
            }
            return Long.parseLong(tenantIdObj.toString());
        } catch (Exception e) {
            log.error("Failed to parse tenantId from token", e);
            return null;
        }
    }
}
```

**拦截器注册**:

```java
// MvcConfig.java
@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Resource
    private TenantInterceptor tenantInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 租户拦截器（在 AdminInterceptor 之后执行）
        registry.addInterceptor(tenantInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/login/**",
                        "/swagger-ui/**",
                        "/v3/api-docs/**"
                );
    }
}
```

---

### 1.3 TenantMybatisInterceptor - MyBatis 拦截器

**文件路径**: `sa-base/src/main/java/net/lab1024/sa/base/foundation/tenant/TenantMybatisInterceptor.java`

```java
package net.lab1024.sa.base.foundation.tenant;

import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.sql.Connection;
import java.util.Properties;

/**
 * 租户 MyBatis 拦截器
 * <p>
 * 自动为 SQL 添加 tenant_id 条件，实现数据隔离
 *
 * @author SmartAdmin Team
 */
@Slf4j
@Intercepts({
    @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})
})
public class TenantMybatisInterceptor implements Interceptor {

    /**
     * 租户字段名
     */
    private static final String TENANT_ID_COLUMN = "tenant_id";

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        StatementHandler statementHandler = PluginUtils.realTarget(invocation.getTarget());
        MetaObject metaObject = SystemMetaObject.forObject(statementHandler);

        // 1. 获取 MappedStatement
        MappedStatement mappedStatement = (MappedStatement) metaObject.getValue("delegate.mappedStatement");
        SqlCommandType sqlCommandType = mappedStatement.getSqlCommandType();

        // 2. 获取 BoundSql
        BoundSql boundSql = (BoundSql) metaObject.getValue("delegate.boundSql");
        String originalSql = boundSql.getSql();

        // 3. 检查是否需要注入租户 ID
        if (!shouldInjectTenantId(sqlCommandType, originalSql)) {
            return invocation.proceed();
        }

        // 4. 获取租户 ID
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            // 未设置租户 ID，不注入（平台管理员或未登录场景）
            return invocation.proceed();
        }

        // 5. 改写 SQL
        String newSql = rewriteSql(originalSql, sqlCommandType, tenantId);
        metaObject.setValue("delegate.boundSql.sql", newSql);

        log.debug("Tenant SQL rewrite: tenantId={}, originalSql={}, newSql={}", tenantId, originalSql, newSql);

        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // 预留配置参数
    }

    /**
     * 是否需要注入租户 ID
     */
    private boolean shouldInjectTenantId(SqlCommandType sqlCommandType, String sql) {
        // 1. 只处理 SELECT、UPDATE、DELETE（INSERT 在业务层处理）
        if (sqlCommandType == SqlCommandType.INSERT) {
            return false;
        }

        // 2. 绕过租户过滤
        if (TenantContext.isBypassTenantFilter()) {
            return false;
        }

        // 3. 排除不需要租户隔离的表
        String lowerSql = sql.toLowerCase();
        if (lowerSql.contains("t_tenant") ||           // 租户表本身
            lowerSql.contains("t_platform_admin") ||   // 平台管理员表
            lowerSql.contains("t_tenant_package") ||   // 套餐表
            lowerSql.contains("t_dict") ||             // 字典表（平台共享）
            lowerSql.contains("t_area")) {             // 地区表（平台共享）
            return false;
        }

        // 4. 已经包含 tenant_id 条件，不重复注入
        if (lowerSql.contains("tenant_id")) {
            return false;
        }

        return true;
    }

    /**
     * 改写 SQL，添加 tenant_id 条件
     */
    private String rewriteSql(String originalSql, SqlCommandType sqlCommandType, Long tenantId) {
        try {
            Statement statement = CCJSqlParserUtil.parse(originalSql);

            if (statement instanceof Select) {
                Select select = (Select) statement;
                PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
                Expression where = plainSelect.getWhere();
                Expression tenantCondition = buildTenantCondition(tenantId);

                if (where != null) {
                    plainSelect.setWhere(new net.sf.jsqlparser.expression.operators.conditional.AndExpression(where, tenantCondition));
                } else {
                    plainSelect.setWhere(tenantCondition);
                }
                return select.toString();
            } else if (statement instanceof Update) {
                Update update = (Update) statement;
                Expression where = update.getWhere();
                Expression tenantCondition = buildTenantCondition(tenantId);

                if (where != null) {
                    update.setWhere(new net.sf.jsqlparser.expression.operators.conditional.AndExpression(where, tenantCondition));
                } else {
                    update.setWhere(tenantCondition);
                }
                return update.toString();
            } else if (statement instanceof Delete) {
                Delete delete = (Delete) statement;
                Expression where = delete.getWhere();
                Expression tenantCondition = buildTenantCondition(tenantId);

                if (where != null) {
                    delete.setWhere(new net.sf.jsqlparser.expression.operators.conditional.AndExpression(where, tenantCondition));
                } else {
                    delete.setWhere(tenantCondition);
                }
                return delete.toString();
            }

            return originalSql;
        } catch (JSQLParserException e) {
            log.warn("Failed to parse SQL, skip tenant injection: {}", originalSql, e);
            return originalSql;
        }
    }

    /**
     * 构建租户条件: tenant_id = ?
     */
    private Expression buildTenantCondition(Long tenantId) {
        EqualsTo equalsTo = new EqualsTo();
        equalsTo.setLeftExpression(new Column(TENANT_ID_COLUMN));
        equalsTo.setRightExpression(new LongValue(tenantId));
        return equalsTo;
    }
}
```

**拦截器注册**:

```java
// MyBatisPlusConfig.java
@Configuration
public class MyBatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 租户拦截器（最先执行）
        interceptor.addInnerInterceptor(new TenantMybatisInterceptor());

        // 分页拦截器
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));

        return interceptor;
    }
}
```

---

## 2. 数据隔离实现方案

### 2.1 方案对比

| 隔离方式 | 实现复杂度 | 性能影响 | 安全性 | 推荐度 |
|---------|----------|---------|--------|--------|
| **MyBatis 拦截器（自动）** | 低 | 低 | 高 | ⭐⭐⭐⭐⭐ |
| **DataScope 注解（半自动）** | 中 | 低 | 高 | ⭐⭐⭐⭐ |
| **手动传参（显式）** | 高 | 无 | 中 | ⭐⭐⭐ |

---

### 2.2 推荐组合方案

#### 层级 1: MyBatis 拦截器（防御层）

**作用**: 作为最后一道防线，自动为所有 SQL 添加 tenant_id 条件

**优点**:
- 自动化，开发者无需关心
- 防止遗漏导致的数据泄露
- 对现有代码零侵入

**缺点**:
- 复杂 SQL 可能改写失败
- 性能有一定开销

---

#### 层级 2: @TenantIsolation 注解（推荐层）

**作用**: 在需要租户隔离的方法上显式标注

**定义注解**:

```java
package net.lab1024.sa.common.tenant.annotation;

import java.lang.annotation.*;

/**
 * 租户隔离注解
 * <p>
 * 标注在 Service/Dao 方法上，表示该方法需要租户隔离
 *
 * @author SmartAdmin Team
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TenantIsolation {

    /**
     * 是否启用租户隔离（默认 true）
     */
    boolean enabled() default true;

    /**
     * 备注
     */
    String remark() default "";
}
```

**使用示例**:

```java
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeDao employeeDao;

    @TenantIsolation(remark = "查询员工列表，需要租户隔离")
    public PageResult<EmployeeVO> query(EmployeeQueryForm form) {
        Page<?> page = SmartPageUtil.convert2PageQuery(form);
        List<EmployeeEntity> list = employeeDao.selectPage(page,
            Wrappers.<EmployeeEntity>lambdaQuery()
                // tenant_id 由 MyBatis 拦截器自动注入
                .like(StringUtils.isNotBlank(form.getKeyword()), EmployeeEntity::getActualName, form.getKeyword())
        );
        return SmartPageUtil.convert2PageResult(page, list, EmployeeVO.class);
    }
}
```

---

#### 层级 3: ArchUnit 测试（强制层）

**作用**: 编译时检查，确保所有需要隔离的方法都正确标注

**测试代码**:

```java
package net.lab1024.sa.admin;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import net.lab1024.sa.common.tenant.annotation.TenantIsolation;
import org.junit.Test;
import org.springframework.stereotype.Service;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

/**
 * 租户隔离架构测试
 */
public class TenantIsolationArchitectureTest {

    private final JavaClasses classes = new ClassFileImporter()
            .importPackages("net.lab1024.sa.admin");

    /**
     * 规则: 所有 Service 层的查询方法必须标注 @TenantIsolation
     */
    @Test
    public void serviceShouldHaveTenantIsolation() {
        ArchRule rule = methods()
                .that().areDeclaredInClassesThat().areAnnotatedWith(Service.class)
                .and().haveNameMatching("(query|list|get|find|select).*")
                .and().arePublic()
                .should().beAnnotatedWith(TenantIsolation.class)
                .orShould().beDeclaredInClassesThat().haveSimpleNameEndingWith("Manager") // Manager 层例外
                .because("Service layer query methods must have @TenantIsolation to ensure data isolation");

        rule.check(classes);
    }
}
```

---

### 2.3 特殊场景处理

#### 场景 1: 平台管理员跨租户查询

```java
@Service
@RequiredArgsConstructor
public class PlatformEmployeeService {

    private final EmployeeDao employeeDao;

    /**
     * 平台管理员查询所有租户的员工
     */
    public List<EmployeeVO> queryAllTenants(EmployeeQueryForm form) {
        // 方式 1: 临时绕过租户过滤
        return TenantContext.callWithoutTenantFilter(() -> {
            List<EmployeeEntity> list = employeeDao.selectList(
                Wrappers.<EmployeeEntity>lambdaQuery()
                    .like(StringUtils.isNotBlank(form.getKeyword()), EmployeeEntity::getActualName, form.getKeyword())
            );
            return SmartBeanUtil.copyList(list, EmployeeVO.class);
        });
    }

    /**
     * 平台管理员查询指定租户的员工
     */
    public List<EmployeeVO> queryByTenant(Long tenantId, EmployeeQueryForm form) {
        List<EmployeeEntity> list = employeeDao.selectList(
            Wrappers.<EmployeeEntity>lambdaQuery()
                .eq(EmployeeEntity::getTenantId, tenantId) // 显式指定租户 ID
                .like(StringUtils.isNotBlank(form.getKeyword()), EmployeeEntity::getActualName, form.getKeyword())
        );
        return SmartBeanUtil.copyList(list, EmployeeVO.class);
    }
}
```

---

#### 场景 2: INSERT 语句自动填充 tenant_id

**方式 1: MyBatis Plus MetaObjectHandler（推荐）**

```java
@Component
public class TenantMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        // 自动填充 tenant_id
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            this.strictInsertFill(metaObject, "tenantId", Long.class, tenantId);
        }

        // 其他字段填充...
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
```

**Entity 定义**:

```java
@TableName("t_employee")
@Data
public class EmployeeEntity {

    @TableId(type = IdType.AUTO)
    private Long employeeId;

    @TableField(fill = FieldFill.INSERT)  // 自动填充
    private Long tenantId;

    private String loginName;
    private String actualName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
```

---

**方式 2: Service 层显式设置**

```java
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeDao employeeDao;

    @TenantIsolation
    public ResponseDTO<String> add(EmployeeAddForm form) {
        EmployeeEntity entity = SmartBeanUtil.copy(form, EmployeeEntity.class);

        // 显式设置 tenant_id
        entity.setTenantId(TenantContext.getRequiredTenantId());

        employeeDao.insert(entity);
        return ResponseDTO.ok();
    }
}
```

---

## 3. 登录流程改造

### 3.1 登录表单扩展

**LoginForm.java**:

```java
@Data
public class LoginForm {

    @NotBlank(message = "商户编码不能为空")
    @Length(max = 50, message = "商户编码最长50字符")
    private String tenantCode;

    @NotBlank(message = "登录账号不能为空")
    @Length(max = 50, message = "登录账号最长50字符")
    private String loginName;

    @NotBlank(message = "密码不能为空")
    private String loginPwd;

    @NotBlank(message = "验证码不能为空")
    private String captchaCode;

    private String captchaUuid;
}
```

---

### 3.2 登录流程

**LoginService.java**:

```java
@Service
@RequiredArgsConstructor
public class LoginService {

    private final TenantService tenantService;
    private final EmployeeDao employeeDao;
    private final LoginManager loginManager;

    public LoginVO login(LoginForm form) {
        // 1. 验证验证码
        validateCaptcha(form.getCaptchaUuid(), form.getCaptchaCode());

        // 2. 验证商户
        TenantEntity tenant = tenantService.getByCode(form.getTenantCode());
        if (tenant == null) {
            throw new BusinessException(TenantErrorCode.TENANT_NOT_EXIST);
        }
        if (tenant.getStatus() == 4) {
            throw new BusinessException(TenantErrorCode.TENANT_DISABLED);
        }
        if (tenant.getStatus() == 3) {
            throw new BusinessException(TenantErrorCode.TENANT_EXPIRED);
        }

        // 3. 查询员工（需要加上 tenant_id 条件）
        EmployeeEntity employee = employeeDao.selectOne(
            Wrappers.<EmployeeEntity>lambdaQuery()
                .eq(EmployeeEntity::getTenantId, tenant.getTenantId())
                .eq(EmployeeEntity::getLoginName, form.getLoginName())
        );

        if (employee == null) {
            throw new BusinessException(EmployeeErrorCode.LOGIN_FAIL);
        }

        // 4. 验证密码
        if (!loginManager.verifyPassword(form.getLoginPwd(), employee.getLoginPwd())) {
            throw new BusinessException(EmployeeErrorCode.LOGIN_FAIL);
        }

        // 5. 检查员工状态
        if (employee.getDisabledFlag()) {
            throw new BusinessException(EmployeeErrorCode.ACCOUNT_DISABLED);
        }
        if (employee.getDeletedFlag()) {
            throw new BusinessException(EmployeeErrorCode.ACCOUNT_NOT_EXIST);
        }

        // 6. 生成 Token（包含 tenantId）
        StpUtil.login("employee_" + employee.getEmployeeId(), new SaLoginModel()
            .setExtra("userType", "employee")
            .setExtra("tenantId", tenant.getTenantId())
            .setExtra("employeeId", employee.getEmployeeId())
            .setExtra("loginName", employee.getLoginName())
            .setExtra("administratorFlag", employee.getAdministratorFlag())
        );

        // 7. 构建返回信息
        return buildLoginVO(employee, tenant);
    }

    private LoginVO buildLoginVO(EmployeeEntity employee, TenantEntity tenant) {
        LoginVO loginVO = new LoginVO();
        loginVO.setToken(StpUtil.getTokenValue());
        loginVO.setEmployeeId(employee.getEmployeeId());
        loginVO.setLoginName(employee.getLoginName());
        loginVO.setActualName(employee.getActualName());
        loginVO.setTenantId(tenant.getTenantId());
        loginVO.setTenantName(tenant.getTenantName());
        return loginVO;
    }
}
```

---

### 3.3 平台管理员登录

**PlatformLoginController.java**:

```java
@RestController
@RequestMapping("/platform/login")
@Tag(name = "平台管理-登录")
@RequiredArgsConstructor
public class PlatformLoginController {

    private final PlatformLoginService platformLoginService;

    @PostMapping("")
    @NoNeedLogin
    @Operation(summary = "平台管理员登录")
    public ResponseDTO<PlatformLoginVO> login(@RequestBody @Valid PlatformLoginForm form) {
        return platformLoginService.login(form);
    }
}
```

**PlatformLoginService.java**:

```java
@Service
@RequiredArgsConstructor
public class PlatformLoginService {

    private final PlatformAdminDao platformAdminDao;

    public ResponseDTO<PlatformLoginVO> login(PlatformLoginForm form) {
        // 1. 查询平台管理员
        PlatformAdminEntity admin = platformAdminDao.selectOne(
            Wrappers.<PlatformAdminEntity>lambdaQuery()
                .eq(PlatformAdminEntity::getLoginName, form.getLoginName())
        );

        if (admin == null) {
            return ResponseDTO.userErrorParam("账号或密码错误");
        }

        // 2. 验证密码
        if (!verifyPassword(form.getLoginPwd(), admin.getLoginPwd())) {
            return ResponseDTO.userErrorParam("账号或密码错误");
        }

        // 3. 检查状态
        if (admin.getDisabledFlag()) {
            return ResponseDTO.userErrorParam("账号已被停用");
        }

        // 4. 生成 Token（不包含 tenantId）
        StpUtil.login("platform_admin_" + admin.getAdminId(), new SaLoginModel()
            .setExtra("userType", "platform_admin")
            .setExtra("adminId", admin.getAdminId())
            .setExtra("loginName", admin.getLoginName())
            .setExtra("roleType", admin.getRoleType())
        );

        // 5. 返回登录信息
        PlatformLoginVO loginVO = new PlatformLoginVO();
        loginVO.setToken(StpUtil.getTokenValue());
        loginVO.setAdminId(admin.getAdminId());
        loginVO.setLoginName(admin.getLoginName());
        loginVO.setActualName(admin.getActualName());
        return ResponseDTO.ok(loginVO);
    }
}
```

---

## 4. 缓存隔离实现

### 4.1 缓存 Key 设计规范

**格式**: `tenant:{tenantId}:{module}:{business}:{id}`

**示例**:
```
tenant:1001:employee:detail:123         // 租户 1001 的员工 123 详情
tenant:1001:department:tree             // 租户 1001 的部门树
tenant:1001:role:permissions:5          // 租户 1001 的角色 5 的权限列表
platform:tenant:list                    // 平台级别缓存（无 tenantId）
```

---

### 4.2 缓存工具封装

**TenantCacheKeyBuilder.java**:

```java
@Component
public class TenantCacheKeyBuilder {

    /**
     * 构建租户缓存 key
     *
     * @param module   模块名（如 employee）
     * @param business 业务名（如 detail）
     * @param id       业务 ID
     * @return 缓存 key
     */
    public static String buildKey(String module, String business, Object id) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            // 平台级别缓存
            return String.format("platform:%s:%s:%s", module, business, id);
        }
        return String.format("tenant:%d:%s:%s:%s", tenantId, module, business, id);
    }

    /**
     * 构建租户缓存 key（无 ID）
     */
    public static String buildKey(String module, String business) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return String.format("platform:%s:%s", module, business);
        }
        return String.format("tenant:%d:%s:%s", tenantId, module, business);
    }
}
```

---

### 4.3 缓存使用示例

**EmployeeManager.java**:

```java
@Service
@RequiredArgsConstructor
public class EmployeeManager {

    private final EmployeeDao employeeDao;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 获取员工详情（带缓存）
     */
    public EmployeeEntity getById(Long employeeId) {
        // 1. 构建缓存 key
        String cacheKey = TenantCacheKeyBuilder.buildKey("employee", "detail", employeeId);

        // 2. 尝试从缓存获取
        EmployeeEntity cached = (EmployeeEntity) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // 3. 从数据库查询
        EmployeeEntity employee = employeeDao.selectById(employeeId);
        if (employee != null) {
            // 4. 写入缓存（30 分钟）
            redisTemplate.opsForValue().set(cacheKey, employee, 30, TimeUnit.MINUTES);
        }

        return employee;
    }

    /**
     * 更新员工（清理缓存）
     */
    @Transactional(rollbackFor = Throwable.class)
    public void update(EmployeeEntity employee) {
        employeeDao.updateById(employee);

        // 清理缓存
        String cacheKey = TenantCacheKeyBuilder.buildKey("employee", "detail", employee.getEmployeeId());
        redisTemplate.delete(cacheKey);
    }
}
```

---

## 5. 权限体系集成

### 5.1 StpInterface 实现

```java
@Component
public class StpInterfaceImpl implements StpInterface {

    @Resource
    private RoleService roleService;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        String userType = StpUtil.getExtra("userType").toString();

        if ("platform_admin".equals(userType)) {
            // 平台管理员：返回所有平台权限
            Long adminId = StpUtil.getExtra("adminId").asLong();
            return getPlatformAdminPermissions(adminId);
        } else if ("employee".equals(userType)) {
            // 商户用户：返回本商户的角色权限
            Long employeeId = StpUtil.getExtra("employeeId").asLong();
            Long tenantId = StpUtil.getExtra("tenantId").asLong();
            return getEmployeePermissions(employeeId, tenantId);
        }

        return Collections.emptyList();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        String userType = StpUtil.getExtra("userType").toString();

        if ("platform_admin".equals(userType)) {
            return Arrays.asList("platform_admin");
        } else if ("employee".equals(userType)) {
            Long employeeId = StpUtil.getExtra("employeeId").asLong();
            return roleService.getRoleCodesByEmployee(employeeId);
        }

        return Collections.emptyList();
    }

    private List<String> getPlatformAdminPermissions(Long adminId) {
        // 平台管理员权限列表
        return Arrays.asList(
            "tenant:add", "tenant:edit", "tenant:delete",
            "platform_admin:add", "platform_admin:edit"
        );
    }

    private List<String> getEmployeePermissions(Long employeeId, Long tenantId) {
        // 从角色菜单中加载权限（需要加上 tenantId 过滤）
        return roleService.getPermissionsByEmployee(employeeId, tenantId);
    }
}
```

---

## 6. 错误处理与异常

### 6.1 租户错误码

**TenantErrorCode.java**:

```java
public enum TenantErrorCode implements ErrorCode {

    TENANT_NOT_EXIST(40001, "商户不存在"),
    TENANT_DISABLED(40002, "商户已被停用"),
    TENANT_EXPIRED(40003, "商户已过期，请联系管理员续费"),
    TENANT_ID_MISSING(40004, "租户ID缺失，请重新登录"),
    TENANT_MISMATCH(40005, "租户信息不匹配，可能存在安全风险"),
    TENANT_CODE_DUPLICATE(40006, "商户编码已存在"),
    TENANT_EXCEED_EMPLOYEE_LIMIT(40007, "员工数量已达上限"),
    TENANT_EXCEED_STORAGE_LIMIT(40008, "存储空间已达上限"),
    ;

    private final int code;
    private final String msg;

    TenantErrorCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMsg() {
        return msg;
    }
}
```

---

### 6.2 异常处理

**GlobalExceptionHandler.java（扩展）**:

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 处理租户相关异常
     */
    @ExceptionHandler(TenantException.class)
    public ResponseDTO<Object> handleTenantException(TenantException e) {
        log.warn("Tenant exception: code={}, msg={}", e.getCode(), e.getMessage());
        return ResponseDTO.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理 ThreadLocal 租户 ID 缺失异常
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseDTO<Object> handleIllegalStateException(IllegalStateException e) {
        if (e.getMessage().contains("TenantId")) {
            log.error("TenantId missing in context", e);
            return ResponseDTO.error(TenantErrorCode.TENANT_ID_MISSING);
        }
        return ResponseDTO.error(ErrorCode.SYSTEM_ERROR);
    }
}
```

---

## 7. 测试策略

### 7.1 单元测试

**TenantContextTest.java**:

```java
@SpringBootTest
public class TenantContextTest {

    @Test
    public void testSetAndGetTenantId() {
        TenantContext.setTenantId(1001L);
        assertEquals(1001L, TenantContext.getTenantId());
        TenantContext.clear();
    }

    @Test
    public void testBypassTenantFilter() {
        TenantContext.setTenantId(1001L);

        List<String> result = TenantContext.callWithoutTenantFilter(() -> {
            // 模拟跨租户查询
            return Arrays.asList("data1", "data2");
        });

        assertEquals(2, result.size());
        assertEquals(1001L, TenantContext.getTenantId()); // 租户 ID 应该恢复
    }
}
```

---

### 7.2 集成测试

**EmployeeServiceTenantTest.java**:

```java
@SpringBootTest
public class EmployeeServiceTenantTest {

    @Resource
    private EmployeeService employeeService;

    @Resource
    private EmployeeDao employeeDao;

    @Test
    @Transactional
    public void testTenantIsolation() {
        // 1. 创建两个租户的员工
        EmployeeEntity employee1 = createEmployee(1001L, "zhangsan");
        EmployeeEntity employee2 = createEmployee(1002L, "lisi");

        // 2. 设置租户上下文为 1001
        TenantContext.setTenantId(1001L);

        // 3. 查询员工列表
        List<EmployeeEntity> list = employeeDao.selectList(null);

        // 4. 验证只能查到租户 1001 的数据
        assertEquals(1, list.size());
        assertEquals("zhangsan", list.get(0).getLoginName());

        // 5. 清理
        TenantContext.clear();
    }

    private EmployeeEntity createEmployee(Long tenantId, String loginName) {
        EmployeeEntity employee = new EmployeeEntity();
        employee.setTenantId(tenantId);
        employee.setLoginName(loginName);
        employee.setActualName(loginName);
        employeeDao.insert(employee);
        return employee;
    }
}
```

---

## 8. 部署与配置

### 8.1 配置开关

**application.yml**:

```yaml
# 多租户配置
tenant:
  # 是否启用多租户模式
  enabled: true
  # 默认租户 ID（单租户模式下使用）
  default-tenant-id: 1
  # 租户表前缀（可选）
  table-prefix: t_
```

---

### 8.2 数据库初始化

**init-tenant-tables.sql**:

```sql
-- 1. 创建租户表
CREATE TABLE t_tenant (...);
CREATE TABLE t_tenant_config (...);
CREATE TABLE t_tenant_package (...);
CREATE TABLE t_platform_admin (...);

-- 2. 为现有表添加 tenant_id 字段
ALTER TABLE t_employee ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 0;
ALTER TABLE t_department ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 0;
-- ... 其他表

-- 3. 创建默认租户（迁移现有数据）
INSERT INTO t_tenant (tenant_id, tenant_code, tenant_name, status)
VALUES (1, 'default', '默认商户', 1);

-- 4. 更新现有数据的 tenant_id
UPDATE t_employee SET tenant_id = 1 WHERE tenant_id = 0;
UPDATE t_department SET tenant_id = 1 WHERE tenant_id = 0;
-- ... 其他表

-- 5. 添加索引
ALTER TABLE t_employee ADD INDEX idx_tenant_id (tenant_id);
ALTER TABLE t_department ADD INDEX idx_tenant_id (tenant_id);
-- ... 其他表
```

---

**文档结束**

---

## 附录

### A. 完整代码示例

完整代码实现请参考项目目录：
- `sa-base/src/main/java/net/lab1024/sa/base/foundation/tenant/`
- `sa-admin/src/main/java/net/lab1024/sa/admin/module/system/tenant/`

### B. 常见问题

**Q1: MyBatis 拦截器改写 SQL 失败怎么办？**
A: 复杂 SQL 可以手动添加 tenant_id 条件，或使用 @TenantIsolation(enabled=false) 禁用自动注入。

**Q2: 如何在单元测试中设置租户上下文？**
A: 在 @Before 方法中调用 `TenantContext.setTenantId(1001L)`，在 @After 方法中调用 `TenantContext.clear()`。

**Q3: 缓存如何清理？**
A: 使用 Redis SCAN 命令扫描 `tenant:{tenantId}:*` 模式的 key 并删除。

---

**联系方式**: 如有疑问请联系技术负责人
