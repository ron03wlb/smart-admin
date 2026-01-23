# SmartAdmin 多商户（Multi-Tenant）功能需求分析文档

**文档版本**: v1.0
**创建日期**: 2026-01-21
**文档类型**: 需求分析与技术方案
**状态**: 待评审

---

## 📋 目录

- [1. 执行摘要](#1-执行摘要)
- [2. 业务场景分析](#2-业务场景分析)
- [3. 核心需求定义](#3-核心需求定义)
- [4. 技术方案选型](#4-技术方案选型)
- [5. 数据模型设计](#5-数据模型设计)
- [6. 功能模块规划](#6-功能模块规划)
- [7. 权限体系设计](#7-权限体系设计)
- [8. 安全与隔离策略](#8-安全与隔离策略)
- [9. 实施路线图](#9-实施路线图)
- [10. 风险评估与挑战](#10-风险评估与挑战)
- [11. 非功能性需求](#11-非功能性需求)
- [12. 开放问题与决策点](#12-开放问题与决策点)

---

## 1. 执行摘要

### 1.1 项目背景

SmartAdmin 当前是一个单租户（Single-Tenant）企业管理系统，所有用户、部门、角色共享同一个组织空间。为了支持 SaaS 化部署和多企业客户场景，需要引入多商户（Multi-Tenant）架构。

### 1.2 目标

- **商户隔离**: 不同商户的数据完全隔离，互不可见
- **独立管理**: 每个商户拥有独立的用户、部门、角色体系
- **灵活配置**: 商户级别的功能开关、配额限制、个性化配置
- **平台管控**: 平台超级管理员可管理所有商户
- **向后兼容**: 最小化对现有代码的侵入性修改

### 1.3 核心价值

| 价值维度 | 说明 |
|---------|------|
| **商业价值** | 支持 SaaS 订阅模式，降低部署成本，快速获客 |
| **技术价值** | 统一代码库管理多租户，降低运维复杂度 |
| **扩展价值** | 为企业级客户提供独立空间，保障数据安全 |

---

## 2. 业务场景分析

### 2.1 典型使用场景

#### 场景 1: SaaS 平台运营商
**角色**: 平台运营方
**需求**:
- 为不同企业客户开通独立商户账号
- 每个商户有独立的员工、部门、角色体系
- 平台管理员可以查看所有商户的运营数据
- 根据订阅套餐限制商户的功能权限和配额（如员工数量、存储空间）

**示例**: 某 SaaS 公司提供企业 OA 服务，为"ABC 公司"和"XYZ 公司"分别开通商户账号，两家公司的数据完全隔离。

---

#### 场景 2: 集团企业多子公司管理
**角色**: 集团企业 IT 部门
**需求**:
- 集团总部为各子公司创建独立商户
- 子公司独立管理自己的员工和部门
- 集团总部可以跨商户查看报表和审计数据
- 部分共享数据（如集团通讯录、公告）

**示例**: 某集团公司有 10 家子公司，每家子公司是一个独立商户，但集团 HR 可以查看所有子公司的员工统计数据。

---

#### 场景 3: 代理商/分销商体系
**角色**: 软件代理商
**需求**:
- 为每个终端客户开通独立商户
- 代理商可以管理名下所有商户
- 提供白标（White-label）能力，每个商户可自定义 Logo、主题色
- 按商户统计使用量和计费

**示例**: 某软件代理商为 50 家企业提供 OA 服务，每家企业是一个独立商户，代理商负责统一运维和计费。

---

### 2.2 用户角色定义

| 角色 | 权限范围 | 典型操作 |
|-----|---------|---------|
| **平台超级管理员** | 所有商户 | 创建商户、删除商户、查看所有数据、配置平台参数 |
| **平台运营人员** | 所有商户（只读） | 查看商户运营数据、导出报表、审计日志 |
| **商户管理员** | 单个商户 | 管理本商户的员工、部门、角色、权限 |
| **商户普通用户** | 单个商户 | 使用本商户的业务功能，受权限控制 |

---

### 2.3 业务流程

#### 商户生命周期管理

```
[商户申请] → [平台审核] → [商户开通] → [商户运营] → [续费/升级] → [商户停用/删除]
    ↓           ↓            ↓           ↓            ↓             ↓
  表单提交    人工审核     初始化数据   正常使用    套餐变更      数据归档
```

#### 用户登录流程（多商户）

```
用户访问登录页
    ↓
输入: 商户标识 + 用户名 + 密码
    ↓
验证商户状态（是否启用、是否过期）
    ↓
验证用户凭证
    ↓
加载用户权限（仅限当前商户）
    ↓
生成 Token（包含 tenantId）
    ↓
返回 Token + 用户信息
```

---

## 3. 核心需求定义

### 3.1 功能性需求

#### FR-01: 商户管理
- **FR-01-01**: 平台管理员可以创建、编辑、删除商户
- **FR-01-02**: 商户信息包含：商户名称、商户编码、联系人、联系方式、状态（启用/停用）、到期时间
- **FR-01-03**: 商户可以设置 Logo、主题色、系统名称（白标能力）
- **FR-01-04**: 商户状态管理：正常、试用、过期、停用、已删除
- **FR-01-05**: 商户配额管理：员工数量上限、存储空间上限、API 调用频率限制

#### FR-02: 数据隔离
- **FR-02-01**: 所有业务数据必须按商户隔离（员工、部门、角色、业务数据）
- **FR-02-02**: 商户用户只能访问本商户的数据
- **FR-02-03**: 平台管理员可以跨商户查看数据
- **FR-02-04**: 数据库层面的强制隔离（通过 tenant_id 字段）
- **FR-02-05**: 缓存数据也需要按商户隔离（缓存 key 包含 tenantId）

#### FR-03: 用户与权限
- **FR-03-01**: 每个商户有独立的员工体系（员工属于特定商户）
- **FR-03-02**: 每个商户有独立的角色和权限配置
- **FR-03-03**: 平台管理员独立于商户体系，拥有超级权限
- **FR-03-04**: 用户登录时需要指定商户（通过商户标识或域名绑定）
- **FR-03-05**: 用户 Token 必须包含 tenantId 信息

#### FR-04: 商户初始化
- **FR-04-01**: 新商户开通时自动创建默认管理员账号
- **FR-04-02**: 自动创建默认角色（管理员、普通员工）
- **FR-04-03**: 自动创建默认部门结构（可选）
- **FR-04-04**: 初始化商户配置（如密码策略、会话超时）

#### FR-05: 商户配置
- **FR-05-01**: 商户级别的功能开关（如是否启用公告、是否启用审批流）
- **FR-05-02**: 商户级别的参数配置（如密码复杂度、登录失败锁定次数）
- **FR-05-03**: 商户级别的菜单配置（根据订阅套餐显示不同菜单）
- **FR-05-04**: 商户级别的通知配置（邮件服务器、短信服务）

#### FR-06: 数据迁移与导入
- **FR-06-01**: 支持将现有单租户数据迁移到指定商户
- **FR-06-02**: 支持商户数据导出（用于备份或迁移）
- **FR-06-03**: 支持商户数据清理（删除商户时的数据处理）

---

### 3.2 非功能性需求

#### NFR-01: 性能要求
- **NFR-01-01**: 商户列表查询响应时间 < 500ms（1000 个商户规模）
- **NFR-01-02**: 用户登录响应时间 < 1s
- **NFR-01-03**: 数据隔离不应显著影响查询性能（增加索引优化）
- **NFR-01-04**: 支持 10,000+ 商户规模（需要数据库分片）

#### NFR-02: 安全要求
- **NFR-02-01**: 商户数据严格隔离，不允许跨租户数据泄露
- **NFR-02-02**: SQL 注入防护（使用 MyBatis 参数化查询）
- **NFR-02-03**: API 层面强制校验 tenantId（防止绕过）
- **NFR-02-04**: 敏感操作审计日志（商户创建、删除、数据导出）

#### NFR-03: 可维护性
- **NFR-03-01**: 最小化对现有代码的侵入（使用 MyBatis 拦截器自动注入 tenant_id）
- **NFR-03-02**: 向后兼容单租户模式（保留单租户部署选项）
- **NFR-03-03**: 提供数据库迁移脚本（添加 tenant_id 字段）
- **NFR-03-04**: 提供商户数据隔离验证工具（ArchUnit 规则）

#### NFR-04: 可扩展性
- **NFR-04-01**: 支持动态添加商户（无需重启服务）
- **NFR-04-02**: 支持商户级别的配置热更新
- **NFR-04-03**: 支持未来扩展到数据库分片架构

---

## 4. 技术方案选型

### 4.1 多租户架构模式对比

| 模式 | 说明 | 优点 | 缺点 | 适用场景 |
|-----|------|------|------|---------|
| **独立数据库** | 每个商户独立数据库 | 数据隔离性最强，便于备份恢复 | 维护成本高，资源利用率低 | 大型企业客户，数据敏感性极高 |
| **共享数据库独立 Schema** | 同一数据库，每个商户独立 Schema | 较好的数据隔离，成本适中 | Schema 数量有限，连接池复杂 | 中型企业客户，100-1000 租户 |
| **共享数据库共享表（推荐）** | 同一数据库和表，通过 tenant_id 区分 | 维护简单，资源利用率高，成本低 | 数据隔离依赖应用层，需严格代码审查 | SaaS 场景，1000+ 租户 |

---

### 4.2 推荐方案：共享数据库共享表 + tenant_id 隔离

#### 选择理由

基于 SmartAdmin 现有架构分析，推荐采用**共享数据库共享表**方案，理由如下：

1. **成本效益**: 单一数据库降低运维成本，适合 SaaS 场景
2. **现有架构支持**:
   - 已有 DataScope 基础设施可复用
   - MyBatis Plus 拦截器机制成熟
   - ThreadLocal 请求上下文可存储 tenantId
3. **扩展性**: 初期共享表，后期可按需迁移大租户到独立数据库
4. **开发效率**: 最小化代码改动，利用 AOP 和拦截器自动注入 tenant_id

---

### 4.3 核心技术实现

#### 4.3.1 Tenant ID 注入机制

**方案 A: MyBatis 拦截器（推荐）**

```java
@Intercepts({
    @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})
})
public class TenantInterceptor implements Interceptor {
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        StatementHandler handler = (StatementHandler) invocation.getTarget();
        BoundSql boundSql = handler.getBoundSql();
        String sql = boundSql.getSql();

        // 自动为所有 SQL 添加 tenant_id 条件
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            sql = addTenantIdCondition(sql, tenantId);
        }

        // 重写 SQL
        return invocation.proceed();
    }
}
```

**优点**:
- 自动化程度高，开发者无需手动添加 tenant_id 条件
- 对现有代码侵入性最小
- 统一拦截，不容易遗漏

**缺点**:
- SQL 解析和改写有性能开销
- 复杂 SQL（如子查询、联表查询）处理困难

---

**方案 B: DataScope 扩展**

```java
@DataScope(
    dataScopeType = DataScopeTypeEnum.TENANT,
    whereInType = DataScopeWhereInTypeEnum.TENANT_ID
)
public PageResult<EmployeeVO> query(EmployeeQueryForm form) {
    // 自动注入: WHERE tenant_id = ?
}
```

**优点**:
- 复用现有 DataScope 基础设施
- 可选择性应用（细粒度控制）
- 易于测试和调试

**缺点**:
- 需要在每个 Dao 方法上添加注解（有遗漏风险）
- 增加开发者心智负担

---

**方案 C: BaseMapper 扩展 + 手动传参**

```java
public interface EmployeeDao extends TenantBaseMapper<EmployeeEntity> {
    // TenantBaseMapper 强制所有方法包含 tenantId 参数
    List<EmployeeEntity> selectList(@Param("tenantId") Long tenantId,
                                    @Param("ew") Wrapper<EmployeeEntity> wrapper);
}
```

**优点**:
- 显式传参，不容易出错
- 性能最优（无拦截器开销）

**缺点**:
- 代码侵入性大，所有 Dao 方法需要修改
- 开发效率低

---

#### 4.3.2 推荐组合方案

**Tier 1: MyBatis 拦截器（防御层）**
- 作为最后一道防线，自动为所有 SQL 添加 tenant_id 条件
- 防止开发者遗漏导致的数据泄露

**Tier 2: DataScope 注解（推荐层）**
- 在需要租户隔离的 Service/Dao 方法上显式标注
- 提高代码可读性，明确隔离意图

**Tier 3: ArchUnit 测试（强制层）**
- 强制所有 Dao 方法必须使用 @TenantIsolation 注解或 MyBatis 拦截器
- 编译时检查，确保不遗漏

---

### 4.4 请求上下文传递

#### ThreadLocal 存储 Tenant ID

```java
public class TenantContext {
    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();

    public static void setTenantId(Long tenantId) {
        TENANT_ID.set(tenantId);
    }

    public static Long getTenantId() {
        return TENANT_ID.get();
    }

    public static void clear() {
        TENANT_ID.remove();
    }
}
```

#### AdminInterceptor 扩展

```java
@Override
public boolean preHandle(HttpServletRequest request, ...) {
    // 1. 解析 Token
    String token = request.getHeader("x-access-token");

    // 2. 从 Token 中提取 tenantId
    Long tenantId = StpUtil.getExtra("tenantId").asLong();

    // 3. 设置到 ThreadLocal
    TenantContext.setTenantId(tenantId);

    // 4. 验证商户状态
    TenantEntity tenant = tenantService.getById(tenantId);
    if (tenant == null || tenant.getDisabled()) {
        throw new BusinessException(TenantErrorCode.TENANT_DISABLED);
    }

    // 5. 继续原有逻辑
    return true;
}

@Override
public void afterCompletion(HttpServletRequest request, ...) {
    TenantContext.clear(); // 清理 ThreadLocal
}
```

---

### 4.5 Token 设计

#### Token Payload 扩展

```json
{
  "loginId": "employee_123",
  "loginType": "employee",
  "tokenValue": "...",
  "extra": {
    "tenantId": 1001,              // 新增：商户 ID
    "employeeId": 123,
    "userName": "zhangsan",
    "administratorFlag": false,
    "departmentId": 10
  }
}
```

#### 登录时设置 tenantId

```java
// LoginService.java
public LoginVO login(LoginForm form) {
    // 1. 验证商户
    TenantEntity tenant = tenantService.getByCode(form.getTenantCode());
    if (tenant == null) {
        throw new BusinessException(TenantErrorCode.TENANT_NOT_EXIST);
    }

    // 2. 验证用户
    EmployeeEntity employee = employeeDao.selectByLoginName(
        form.getLoginName(),
        tenant.getTenantId()  // 加上商户过滤
    );

    // 3. 生成 Token
    StpUtil.login(employee.getEmployeeId());
    StpUtil.getSession().set("tenantId", tenant.getTenantId());  // 存储 tenantId

    // 4. 返回登录信息
    return buildLoginVO(employee, tenant);
}
```

---

## 5. 数据模型设计

### 5.1 新增核心表

#### 5.1.1 商户表（t_tenant）

```sql
CREATE TABLE t_tenant (
    tenant_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '商户ID',
    tenant_code VARCHAR(50) UNIQUE NOT NULL COMMENT '商户编码（唯一标识，用于登录）',
    tenant_name VARCHAR(100) NOT NULL COMMENT '商户名称',
    tenant_type TINYINT NOT NULL DEFAULT 1 COMMENT '商户类型: 1=企业版 2=专业版 3=免费版',

    -- 联系信息
    contact_name VARCHAR(50) COMMENT '联系人姓名',
    contact_phone VARCHAR(20) COMMENT '联系电话',
    contact_email VARCHAR(100) COMMENT '联系邮箱',

    -- 状态管理
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1=正常 2=试用 3=过期 4=停用 5=已删除',
    expire_time DATETIME COMMENT '到期时间',

    -- 配额限制
    max_employee_count INT DEFAULT 50 COMMENT '员工数量上限',
    max_storage_size BIGINT DEFAULT 10737418240 COMMENT '存储空间上限(字节, 默认10GB)',

    -- 白标配置
    logo_url VARCHAR(500) COMMENT '商户Logo URL',
    theme_color VARCHAR(20) DEFAULT '#1890ff' COMMENT '主题色',
    system_name VARCHAR(100) COMMENT '系统名称（白标）',

    -- 域名绑定（可选）
    domain VARCHAR(200) UNIQUE COMMENT '绑定域名（如 abc.mycompany.com）',

    -- 审计字段
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_tenant_code (tenant_code),
    INDEX idx_status (status),
    INDEX idx_expire_time (expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户表';
```

---

#### 5.1.2 商户配置表（t_tenant_config）

```sql
CREATE TABLE t_tenant_config (
    config_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '配置ID',
    tenant_id BIGINT NOT NULL COMMENT '商户ID',
    config_key VARCHAR(100) NOT NULL COMMENT '配置键',
    config_value TEXT COMMENT '配置值（JSON格式）',
    config_type VARCHAR(50) COMMENT '配置类型: system=系统配置 business=业务配置',
    remark VARCHAR(500) COMMENT '备注',

    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY uk_tenant_key (tenant_id, config_key),
    INDEX idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户配置表';
```

**常见配置示例**:
```json
{
  "password_policy": {
    "min_length": 8,
    "require_special_char": true,
    "max_failed_attempts": 5
  },
  "session_timeout": 7200,
  "enable_oa_notice": true,
  "enable_approval_flow": false
}
```

---

#### 5.1.3 商户套餐表（t_tenant_package）

```sql
CREATE TABLE t_tenant_package (
    package_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '套餐ID',
    package_name VARCHAR(100) NOT NULL COMMENT '套餐名称',
    package_code VARCHAR(50) UNIQUE NOT NULL COMMENT '套餐编码',

    -- 配额
    max_employee_count INT NOT NULL COMMENT '员工数量上限',
    max_storage_size BIGINT NOT NULL COMMENT '存储空间上限(字节)',
    max_api_calls_per_day INT COMMENT 'API调用次数上限（每天）',

    -- 功能权限（JSON）
    feature_flags JSON COMMENT '功能开关配置',

    -- 价格
    price_monthly DECIMAL(10,2) COMMENT '月付价格',
    price_yearly DECIMAL(10,2) COMMENT '年付价格',

    sort INT DEFAULT 0 COMMENT '排序',
    disabled_flag TINYINT DEFAULT 0 COMMENT '是否禁用: 0=否 1=是',

    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_package_code (package_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户套餐表';
```

---

#### 5.1.4 平台管理员表（t_platform_admin）

```sql
CREATE TABLE t_platform_admin (
    admin_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '管理员ID',
    login_name VARCHAR(50) UNIQUE NOT NULL COMMENT '登录账号',
    login_pwd VARCHAR(100) NOT NULL COMMENT '登录密码（加密）',
    actual_name VARCHAR(50) NOT NULL COMMENT '真实姓名',
    phone VARCHAR(20) COMMENT '手机号',
    email VARCHAR(100) COMMENT '邮箱',

    -- 平台管理员角色
    role_type TINYINT NOT NULL DEFAULT 1 COMMENT '角色类型: 1=超级管理员 2=运营人员 3=审计人员',

    disabled_flag TINYINT DEFAULT 0 COMMENT '是否禁用: 0=否 1=是',
    deleted_flag TINYINT DEFAULT 0 COMMENT '是否删除: 0=否 1=是',

    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_login_name (login_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台管理员表';
```

---

### 5.2 现有表改造

#### 5.2.1 需要添加 tenant_id 的表

**核心业务表**（必须添加）:
```sql
-- 员工表
ALTER TABLE t_employee ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '商户ID' AFTER employee_id;
ALTER TABLE t_employee ADD INDEX idx_tenant_id (tenant_id);

-- 部门表
ALTER TABLE t_department ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '商户ID' AFTER department_id;
ALTER TABLE t_department ADD INDEX idx_tenant_id (tenant_id);

-- 角色表
ALTER TABLE t_role ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '商户ID' AFTER role_id;
ALTER TABLE t_role ADD INDEX idx_tenant_id (tenant_id);

-- 菜单表（商户级别菜单定制）
ALTER TABLE t_menu ADD COLUMN tenant_id BIGINT DEFAULT NULL COMMENT '商户ID（NULL表示平台菜单）';
ALTER TABLE t_menu ADD INDEX idx_tenant_id (tenant_id);

-- 业务表（以商品表为例）
ALTER TABLE t_goods ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '商户ID' AFTER goods_id;
ALTER TABLE t_goods ADD INDEX idx_tenant_id (tenant_id);

-- OA 相关表
ALTER TABLE t_oa_enterprise ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '商户ID';
ALTER TABLE t_oa_notice ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '商户ID';
```

---

#### 5.2.2 不需要添加 tenant_id 的表

**平台共享数据**（无需隔离）:
- 系统配置表（如果是平台级别配置）
- 字典表（平台统一维护）
- 地区表（中国省市区数据）
- 日志表（通过其他字段关联商户）

**关系表**（通过主表隔离）:
- `t_role_employee`（通过 role_id 和 employee_id 间接隔离）
- `t_role_menu`（通过 role_id 间接隔离）

---

### 5.3 数据迁移策略

#### 5.3.1 单租户到多租户迁移

**步骤**:
1. **创建默认商户**: 将现有数据迁移到 ID=1 的默认商户
2. **批量更新 tenant_id**:
   ```sql
   UPDATE t_employee SET tenant_id = 1 WHERE tenant_id = 0;
   UPDATE t_department SET tenant_id = 1 WHERE tenant_id = 0;
   -- ... 其他表
   ```
3. **验证数据完整性**: 确保所有数据都有正确的 tenant_id
4. **调整应用配置**: 开启多租户模式

---

## 6. 功能模块规划

### 6.1 模块结构

```
sa-admin/
├── module/
│   ├── system/
│   │   └── tenant/                    # 新增：商户管理模块
│   │       ├── controller/
│   │       │   ├── TenantController.java             # 商户 CRUD
│   │       │   ├── TenantPackageController.java      # 套餐管理
│   │       │   └── PlatformAdminController.java      # 平台管理员
│   │       ├── service/
│   │       │   ├── TenantService.java
│   │       │   ├── TenantPackageService.java
│   │       │   └── PlatformAdminService.java
│   │       ├── manager/
│   │       │   ├── TenantManager.java                # 商户缓存、事务
│   │       │   └── TenantInitManager.java            # 商户初始化
│   │       ├── dao/
│   │       │   ├── TenantDao.java
│   │       │   ├── TenantConfigDao.java
│   │       │   ├── TenantPackageDao.java
│   │       │   └── PlatformAdminDao.java
│   │       └── domain/
│   │           ├── entity/
│   │           │   ├── TenantEntity.java
│   │           │   ├── TenantConfigEntity.java
│   │           │   ├── TenantPackageEntity.java
│   │           │   └── PlatformAdminEntity.java
│   │           ├── form/
│   │           │   ├── TenantAddForm.java
│   │           │   ├── TenantUpdateForm.java
│   │           │   └── TenantQueryForm.java
│   │           └── vo/
│   │               ├── TenantVO.java
│   │               └── TenantDetailVO.java
│   └── business/
│       └── [现有业务模块无需改动]
└── interceptor/
    └── TenantInterceptor.java         # 新增：租户拦截器

sa-base/
└── foundation/
    └── tenant/                        # 新增：租户基础设施
        ├── TenantContext.java         # ThreadLocal 上下文
        ├── TenantDataScopeStrategy.java  # 租户隔离策略
        └── TenantMybatisInterceptor.java # MyBatis 拦截器

sa-common/
└── tenant/                            # 新增：租户通用组件
    ├── annotation/
    │   └── TenantIsolation.java       # 租户隔离注解
    └── exception/
        └── TenantErrorCode.java       # 租户错误码
```

---

### 6.2 功能清单

#### 6.2.1 商户管理

| 功能编号 | 功能名称 | 说明 | 优先级 |
|---------|---------|------|--------|
| MT-001 | 商户列表查询 | 分页查询商户，支持按名称、状态筛选 | P0 |
| MT-002 | 商户详情查询 | 查看商户详细信息、配额使用情况 | P0 |
| MT-003 | 创建商户 | 创建新商户，自动初始化默认数据 | P0 |
| MT-004 | 编辑商户 | 修改商户基本信息 | P0 |
| MT-005 | 启用/停用商户 | 控制商户登录权限 | P0 |
| MT-006 | 删除商户 | 软删除商户（数据归档） | P1 |
| MT-007 | 商户配额管理 | 查看和调整员工数量、存储空间上限 | P1 |
| MT-008 | 商户套餐绑定 | 为商户分配或变更套餐 | P1 |
| MT-009 | 商户白标配置 | 上传 Logo、设置主题色、系统名称 | P2 |
| MT-010 | 域名绑定 | 为商户绑定独立域名 | P2 |

---

#### 6.2.2 套餐管理

| 功能编号 | 功能名称 | 说明 | 优先级 |
|---------|---------|------|--------|
| MP-001 | 套餐列表查询 | 查询所有可用套餐 | P1 |
| MP-002 | 创建套餐 | 定义套餐名称、配额、价格 | P1 |
| MP-003 | 编辑套餐 | 修改套餐信息 | P1 |
| MP-004 | 删除套餐 | 删除未被使用的套餐 | P1 |
| MP-005 | 套餐功能配置 | 配置套餐包含的功能模块 | P1 |

---

#### 6.2.3 平台管理员

| 功能编号 | 功能名称 | 说明 | 优先级 |
|---------|---------|------|--------|
| PA-001 | 平台管理员登录 | 独立登录入口（不属于任何商户） | P0 |
| PA-002 | 平台管理员列表 | 查询所有平台管理员 | P0 |
| PA-003 | 创建平台管理员 | 添加新管理员 | P0 |
| PA-004 | 编辑平台管理员 | 修改管理员信息 | P0 |
| PA-005 | 删除平台管理员 | 删除管理员账号 | P0 |
| PA-006 | 跨租户数据查询 | 查看所有商户的运营数据 | P1 |
| PA-007 | 审计日志查询 | 查看平台操作日志 | P1 |

---

#### 6.2.4 商户初始化

| 功能编号 | 功能名称 | 说明 | 优先级 |
|---------|---------|------|--------|
| TI-001 | 创建默认管理员 | 商户开通时自动创建管理员账号 | P0 |
| TI-002 | 创建默认角色 | 创建"管理员"、"普通员工"角色 | P0 |
| TI-003 | 创建默认部门 | 创建"总经办"等默认部门（可选） | P1 |
| TI-004 | 初始化菜单权限 | 根据套餐分配菜单权限 | P0 |
| TI-005 | 发送欢迎邮件 | 向商户管理员发送开通通知 | P2 |

---

#### 6.2.5 数据隔离

| 功能编号 | 功能名称 | 说明 | 优先级 |
|---------|---------|------|--------|
| DI-001 | MyBatis 拦截器 | 自动为 SQL 添加 tenant_id 条件 | P0 |
| DI-002 | DataScope 扩展 | 支持 TenantIsolation 注解 | P0 |
| DI-003 | ThreadLocal 上下文 | 存储当前请求的 tenantId | P0 |
| DI-004 | 缓存隔离 | 缓存 key 包含 tenantId | P0 |
| DI-005 | ArchUnit 测试 | 强制所有 Dao 方法包含租户隔离 | P1 |

---

#### 6.2.6 用户登录

| 功能编号 | 功能名称 | 说明 | 优先级 |
|---------|---------|------|--------|
| UL-001 | 商户标识输入 | 登录页增加商户编码输入框 | P0 |
| UL-002 | 域名自动识别 | 通过域名自动识别商户（可选） | P2 |
| UL-003 | Token 包含 tenantId | 登录成功后 Token 包含商户信息 | P0 |
| UL-004 | 商户状态验证 | 验证商户是否过期/停用 | P0 |
| UL-005 | 平台管理员登录 | 独立登录入口（/platform/login） | P0 |

---

## 7. 权限体系设计

### 7.1 权限层级

```
平台权限（Platform）
    ↓
商户权限（Tenant）
    ↓
角色权限（Role）
    ↓
用户权限（User）
```

---

### 7.2 权限设计

#### 7.2.1 平台管理员权限

**超级管理员**:
- 所有商户的 CRUD 操作
- 所有套餐的管理
- 所有平台管理员的管理
- 跨租户数据查询
- 系统配置管理

**运营人员**:
- 商户列表查询（只读）
- 商户数据统计（只读）
- 审计日志查询（只读）

**审计人员**:
- 审计日志查询
- 敏感操作记录查询

---

#### 7.2.2 商户内部权限

**商户管理员**:
- 本商户员工、部门、角色的完全管理权限
- 本商户业务数据的完全管理权限
- 商户配置修改（如白标设置）
- **无法访问其他商户数据**

**商户普通员工**:
- 受角色权限控制
- 只能访问本商户数据
- 按照 Sa-Token 权限规则进行细粒度控制

---

### 7.3 Sa-Token 集成

#### 7.3.1 多账号类型支持

```java
// 平台管理员登录
StpUtil.login("platform_admin_" + adminId, new SaLoginModel()
    .setExtra("userType", "platform_admin")
    .setExtra("adminId", adminId)
);

// 商户用户登录
StpUtil.login("employee_" + employeeId, new SaLoginModel()
    .setExtra("userType", "employee")
    .setExtra("tenantId", tenantId)
    .setExtra("employeeId", employeeId)
);
```

---

#### 7.3.2 权限校验

```java
// Controller 层权限注解
@SaCheckPermission("tenant:add")        // 平台管理员权限
@SaCheckPermission("employee:add")      // 商户内权限
```

---

#### 7.3.3 权限加载逻辑

```java
// StpInterfaceImpl.java
@Override
public List<String> getPermissionList(Object loginId, String loginType) {
    String userType = StpUtil.getExtra("userType").toString();

    if ("platform_admin".equals(userType)) {
        // 平台管理员：返回所有平台权限
        return platformAdminService.getPermissions(adminId);
    } else if ("employee".equals(userType)) {
        // 商户用户：返回本商户的角色权限
        Long tenantId = StpUtil.getExtra("tenantId").asLong();
        return roleService.getPermissionsByEmployee(employeeId, tenantId);
    }

    return Collections.emptyList();
}
```

---

## 8. 安全与隔离策略

### 8.1 数据隔离保障

| 层级 | 隔离机制 | 实现方式 |
|-----|---------|---------|
| **数据库层** | tenant_id 字段 + 索引 | 所有核心表添加 tenant_id 字段 |
| **ORM 层** | MyBatis 拦截器 | 自动注入 tenant_id 条件 |
| **Service 层** | DataScope 注解 | 显式标注租户隔离意图 |
| **API 层** | Interceptor 验证 | 验证 Token 中的 tenantId |
| **缓存层** | 缓存 key 前缀 | key 格式: `tenant:{tenantId}:...` |

---

### 8.2 安全防护措施

#### 8.2.1 防止跨租户数据访问

**场景**: 攻击者篡改 Token 中的 tenantId 试图访问其他商户数据

**防护**:
1. **Token 签名验证**: 使用 Sa-Token 的 JWT 签名，防止 Token 被篡改
2. **Interceptor 二次验证**: 从数据库加载用户信息，验证 tenantId 一致性
3. **数据库层强制过滤**: MyBatis 拦截器强制所有查询包含 tenant_id 条件

```java
// AdminInterceptor.java
Long tokenTenantId = StpUtil.getExtra("tenantId").asLong();
EmployeeEntity employee = employeeDao.selectById(employeeId);

if (!employee.getTenantId().equals(tokenTenantId)) {
    throw new BusinessException(TenantErrorCode.TENANT_MISMATCH);
}
```

---

#### 8.2.2 SQL 注入防护

**风险**: tenant_id 作为参数拼接到 SQL 中，可能存在注入风险

**防护**:
1. **使用参数化查询**: MyBatis #{tenantId} 而非 ${tenantId}
2. **类型校验**: tenantId 必须是 Long 类型
3. **白名单校验**: 验证 tenantId 存在且状态正常

---

#### 8.2.3 敏感操作审计

**需要审计的操作**:
- 商户创建/删除
- 商户数据导出
- 跨租户数据查询
- 平台管理员登录
- 商户状态变更

**审计日志表**:
```sql
CREATE TABLE t_platform_audit_log (
    log_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    operator_id BIGINT NOT NULL COMMENT '操作人ID',
    operator_type VARCHAR(20) NOT NULL COMMENT '操作人类型: platform_admin/employee',
    tenant_id BIGINT COMMENT '关联商户ID',
    operation VARCHAR(100) NOT NULL COMMENT '操作类型',
    operation_detail TEXT COMMENT '操作详情（JSON）',
    ip VARCHAR(50) COMMENT 'IP地址',
    user_agent VARCHAR(500) COMMENT '用户代理',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',

    INDEX idx_operator (operator_id, operator_type),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台审计日志表';
```

---

## 9. 实施路线图

### 9.1 阶段划分

#### Phase 1: 基础设施（2 周）

**目标**: 搭建多租户基础架构，不影响现有功能

**任务**:
- [x] 数据库设计：创建商户相关表（t_tenant, t_tenant_config, t_platform_admin）
- [x] 数据迁移：为现有表添加 tenant_id 字段，迁移数据到默认商户
- [x] TenantContext：实现 ThreadLocal 上下文管理
- [x] TenantInterceptor：实现商户拦截器，提取 tenantId
- [x] MyBatis 拦截器：实现自动注入 tenant_id 条件（初步版本）
- [x] ArchUnit 测试：添加租户隔离验证规则
- [x] 单元测试：验证隔离机制有效性

**交付物**:
- 数据库迁移脚本
- 租户基础设施代码
- 测试报告

---

#### Phase 2: 平台管理功能（3 周）

**目标**: 实现平台管理员和商户管理功能

**任务**:
- [x] 平台管理员模块：登录、CRUD、权限管理
- [x] 商户管理模块：商户 CRUD、状态管理、配额管理
- [x] 套餐管理模块：套餐 CRUD、功能配置
- [x] 商户初始化：自动创建默认数据（管理员、角色、部门）
- [x] 前端页面：平台管理后台（Vue3 + Ant Design Vue）
- [x] API 文档：Swagger 接口文档

**交付物**:
- 平台管理后台
- 商户管理 API
- 接口文档

---

#### Phase 3: 用户登录改造（1 周）

**目标**: 改造登录流程，支持商户标识

**任务**:
- [x] LoginForm 扩展：添加 tenantCode 字段
- [x] LoginService 改造：验证商户、加载 tenantId 到 Token
- [x] 前端登录页：添加商户编码输入框
- [x] Token 验证：AdminInterceptor 验证 tenantId
- [x] 兼容性测试：确保现有功能不受影响

**交付物**:
- 新版登录页
- 登录 API 改造
- 测试报告

---

#### Phase 4: 数据隔离全面应用（2 周）

**目标**: 确保所有业务模块应用租户隔离

**任务**:
- [x] Employee/Department/Role 模块改造：Service 层添加 tenant_id 过滤
- [x] 业务模块改造：Goods、OA 等模块应用租户隔离
- [x] 缓存隔离：改造所有缓存 key，包含 tenantId
- [x] 全量回归测试：验证所有功能正常
- [x] 性能测试：验证隔离机制不影响性能

**交付物**:
- 全模块租户隔离
- 测试报告
- 性能测试报告

---

#### Phase 5: 高级功能（3 周，可选）

**目标**: 实现高级多租户功能

**任务**:
- [ ] 白标配置：Logo、主题色、系统名称定制
- [ ] 域名绑定：支持独立域名访问
- [ ] 商户数据导出：支持商户数据备份
- [ ] 配额监控：员工数量、存储空间使用告警
- [ ] 多语言支持：商户级别语言配置
- [ ] 审计日志：平台操作日志查询

**交付物**:
- 高级功能模块
- 用户手册

---

### 9.2 关键里程碑

| 里程碑 | 时间 | 标志 |
|-------|------|------|
| M1: 基础设施完成 | Week 2 | TenantContext + MyBatis 拦截器可用 |
| M2: 平台管理完成 | Week 5 | 可以创建商户和平台管理员 |
| M3: 登录改造完成 | Week 6 | 用户可以通过商户编码登录 |
| M4: 数据隔离完成 | Week 8 | 所有模块应用租户隔离 |
| M5: 高级功能完成 | Week 11 | 白标、域名绑定等功能可用 |

---

## 10. 风险评估与挑战

### 10.1 技术风险

| 风险 | 影响 | 概率 | 缓解措施 |
|-----|------|------|---------|
| **数据泄露** | 高 | 中 | 1. 多层隔离验证<br>2. ArchUnit 强制测试<br>3. 代码审查 |
| **性能下降** | 中 | 中 | 1. tenant_id 字段添加索引<br>2. 缓存优化<br>3. 性能测试 |
| **SQL 改写失败** | 高 | 低 | 1. MyBatis 拦截器充分测试<br>2. 复杂 SQL 人工审查<br>3. 提供手动注入机制 |
| **缓存失效** | 中 | 低 | 1. 缓存 key 统一加 tenantId 前缀<br>2. 缓存清理逻辑调整 |
| **现有功能破坏** | 高 | 中 | 1. 充分回归测试<br>2. 灰度发布<br>3. 保留单租户模式开关 |

---

### 10.2 业务风险

| 风险 | 影响 | 概率 | 缓解措施 |
|-----|------|------|---------|
| **需求变更** | 中 | 高 | 1. 阶段性交付<br>2. 敏捷迭代<br>3. 及时沟通 |
| **数据迁移失败** | 高 | 低 | 1. 迁移脚本充分测试<br>2. 数据备份<br>3. 回滚方案 |
| **用户体验下降** | 中 | 中 | 1. 登录流程优化（域名自动识别）<br>2. 用户培训<br>3. 操作手册 |

---

### 10.3 挑战

#### 挑战 1: 复杂 SQL 改写
**问题**: 联表查询、子查询、窗口函数等复杂 SQL 的 tenant_id 注入困难

**解决方案**:
1. MyBatis 拦截器只处理简单 SQL
2. 复杂 SQL 使用 @TenantIsolation 注解手动处理
3. 编写 SQL 改写单元测试

---

#### 挑战 2: 缓存一致性
**问题**: 商户配置变更后，缓存可能未及时更新

**解决方案**:
1. 使用 Redis Pub/Sub 通知所有实例清理缓存
2. 配置变更触发缓存失效
3. 设置合理的缓存过期时间

---

#### 挑战 3: 跨商户查询
**问题**: 平台管理员需要跨商户查询数据，但 MyBatis 拦截器会自动注入 tenant_id

**解决方案**:
1. TenantContext 提供 `bypassTenantFilter()` 方法
2. 平台管理员查询时临时关闭租户过滤
3. 操作记录到审计日志

```java
// 跨商户查询示例
TenantContext.bypassTenantFilter(() -> {
    return employeeDao.selectAll(); // 查询所有商户的员工
});
```

---

## 11. 非功能性需求

### 11.1 性能要求

| 指标 | 目标 | 验证方式 |
|-----|------|---------|
| 商户列表查询 | < 500ms（1000 租户） | JMeter 压测 |
| 用户登录 | < 1s | JMeter 压测 |
| 业务查询（单租户） | 性能无明显下降（< 10%） | 对比测试 |
| 数据库连接数 | 不因租户数量增加而线性增长 | 监控连接池 |

---

### 11.2 可用性要求

- **服务可用性**: 99.9%（允许每月 43 分钟维护窗口）
- **数据可靠性**: 99.999%（数据不丢失）
- **故障恢复**: RTO < 4 小时，RPO < 1 小时

---

### 11.3 可扩展性要求

- 支持 10,000+ 商户规模
- 支持单商户 10,000+ 员工规模
- 支持未来扩展到数据库分片架构

---

### 11.4 兼容性要求

- 向后兼容单租户模式（通过配置开关）
- 兼容现有 API（保留原有接口签名）
- 前端无感知（除登录页外）

---

## 12. 开放问题与决策点

### 12.1 需要确认的问题

#### Q1: 商户标识形式
**问题**: 用户登录时如何指定商户？

**选项**:
- A. 手动输入商户编码（tenant_code）
- B. 域名自动识别（abc.mycompany.com → tenant_code=abc）
- C. 用户名包含商户标识（abc/zhangsan）

**推荐**: A + B 组合（优先域名识别，否则手动输入）

---

#### Q2: 平台管理员与商户管理员分离程度
**问题**: 平台管理员是否可以作为某个商户的管理员？

**选项**:
- A. 完全分离（平台管理员不属于任何商户）
- B. 可以兼职（平台管理员可以管理指定商户）

**推荐**: A（职责清晰，权限隔离）

---

#### Q3: 数据删除策略
**问题**: 商户删除后，数据如何处理？

**选项**:
- A. 软删除（标记 deleted_flag=1，数据保留）
- B. 归档（移动到归档表/数据库）
- C. 硬删除（彻底删除，不可恢复）

**推荐**: A（30 天后可选择性归档或删除）

---

#### Q4: 是否支持单租户部署模式
**问题**: 是否保留单租户部署选项？

**选项**:
- A. 只支持多租户模式（简化代码）
- B. 通过配置开关支持单租户模式（兼容性更好）

**推荐**: B（向后兼容，降低升级风险）

**实现**: `application.yml` 中添加 `tenant.enabled=true/false`

---

#### Q5: 商户间数据共享
**问题**: 是否需要支持商户间共享数据（如集团通讯录）？

**选项**:
- A. 完全隔离（不支持共享）
- B. 支持共享（通过 tenant_id=0 表示平台共享数据）

**推荐**: A（Phase 1），B（Phase 2 可选）

---

#### Q6: 菜单权限粒度
**问题**: 菜单是平台统一维护，还是每个商户独立配置？

**选项**:
- A. 平台统一（所有商户共享菜单，通过套餐控制可见性）
- B. 商户独立（每个商户可以自定义菜单）

**推荐**: A（Phase 1），B（Phase 2 可选）

---

### 12.2 技术决策待确认

| 决策点 | 选项 | 推荐 | 理由 |
|-------|------|------|------|
| MyBatis 拦截器实现方式 | JSQLParser / 手写解析 | JSQLParser | 成熟度高，社区支持好 |
| 缓存隔离策略 | Key 前缀 / 独立 Redis DB | Key 前缀 | 简单、通用 |
| 商户状态检查频率 | 每次请求 / 缓存 5 分钟 | 缓存 5 分钟 | 平衡性能和实时性 |
| 商户配额检查时机 | 实时检查 / 定时任务 | 实时检查 + 定时告警 | 确保准确性 |

---

## 13. 附录

### 13.1 参考资料

- [Alibaba Java 开发手册](https://github.com/alibaba/p3c)
- [MyBatis Plus 官方文档](https://baomidou.com/)
- [Sa-Token 官方文档](https://sa-token.cc/)
- [Multi-Tenancy Patterns (Microsoft)](https://docs.microsoft.com/en-us/azure/architecture/guide/multitenant/overview)

---

### 13.2 术语表

| 术语 | 英文 | 说明 |
|-----|------|------|
| 多租户 | Multi-Tenancy | 单个应用实例服务多个独立客户（租户） |
| 租户 | Tenant | 独立的业务实体（商户、企业） |
| 数据隔离 | Data Isolation | 确保不同租户的数据互不可见 |
| 白标 | White-label | 允许租户自定义品牌元素（Logo、主题色） |
| SaaS | Software as a Service | 软件即服务，云端交付模式 |

---

### 13.3 联系人

| 角色 | 姓名 | 联系方式 | 职责 |
|-----|------|---------|------|
| 产品负责人 | TBD | - | 需求确认、优先级决策 |
| 技术负责人 | TBD | - | 技术方案审查、风险评估 |
| 开发负责人 | TBD | - | 开发计划、代码审查 |
| 测试负责人 | TBD | - | 测试计划、质量保障 |

---

**文档结束**

---

## 📝 变更记录

| 版本 | 日期 | 作者 | 变更说明 |
|-----|------|------|---------|
| v1.0 | 2026-01-21 | Claude Code | 初始版本，完成需求分析 |

---

**下一步行动**:
1. ✅ 评审本需求文档
2. ⬜ 确认开放问题与决策点
3. ⬜ 批准实施计划
4. ⬜ 启动 Phase 1 开发
