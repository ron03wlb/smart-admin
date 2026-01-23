# SmartAdmin 多商户 API 接口设计文档

**文档版本**: v1.0
**创建日期**: 2026-01-21
**关联文档**: [MULTI_TENANT_REQUIREMENTS.md](./MULTI_TENANT_REQUIREMENTS.md)

---

## 📋 目录

- [1. 接口概览](#1-接口概览)
- [2. 商户管理接口](#2-商户管理接口)
- [3. 套餐管理接口](#3-套餐管理接口)
- [4. 平台管理员接口](#4-平台管理员接口)
- [5. 登录接口改造](#5-登录接口改造)
- [6. 数据模型定义](#6-数据模型定义)
- [7. 错误码定义](#7-错误码定义)
- [8. 接口调用示例](#8-接口调用示例)

---

## 1. 接口概览

### 1.1 接口分组

| 分组 | 路径前缀 | 权限要求 | 说明 |
|-----|---------|---------|------|
| 商户管理 | `/tenant` | platform_admin | 平台管理员管理商户 |
| 套餐管理 | `/tenant/package` | platform_admin | 平台管理员管理套餐 |
| 平台管理员 | `/platform/admin` | platform_admin | 平台管理员管理 |
| 平台登录 | `/platform/login` | NoNeedLogin | 平台管理员登录 |
| 商户登录 | `/login` | NoNeedLogin | 商户用户登录 |
| 商户配置 | `/tenant/config` | tenant_admin | 商户管理员配置本商户 |

---

### 1.2 通用响应格式

所有接口遵循 SmartAdmin ResponseDTO 标准格式：

```json
{
  "code": 1,
  "ok": true,
  "msg": "成功",
  "data": { ... }
}
```

---

### 1.3 通用请求头

| Header | 说明 | 必填 | 示例 |
|--------|------|------|------|
| x-access-token | 访问令牌 | 是（登录后） | `Bearer eyJhbGciOiJIUzI1...` |
| Content-Type | 内容类型 | 是 | `application/json` |

---

## 2. 商户管理接口

### 2.1 商户列表查询

**接口**: `POST /tenant/query`

**权限**: `@SaCheckPermission("tenant:query")`

**请求参数**:

```json
{
  "pageNum": 1,
  "pageSize": 10,
  "searchCount": true,
  "tenantName": "测试商户",
  "tenantCode": "test",
  "status": 1,
  "startTime": "2026-01-01 00:00:00",
  "endTime": "2026-01-31 23:59:59"
}
```

**参数说明**:

| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| pageNum | Integer | 是 | 页码 |
| pageSize | Integer | 是 | 每页大小 |
| tenantName | String | 否 | 商户名称（模糊查询） |
| tenantCode | String | 否 | 商户编码（模糊查询） |
| status | Integer | 否 | 状态：1=正常 2=试用 3=过期 4=停用 5=已删除 |
| startTime | String | 否 | 创建时间开始 |
| endTime | String | 否 | 创建时间结束 |

**响应示例**:

```json
{
  "code": 1,
  "ok": true,
  "msg": "成功",
  "data": {
    "pageNum": 1,
    "pageSize": 10,
    "total": 25,
    "list": [
      {
        "tenantId": 1001,
        "tenantCode": "abc_company",
        "tenantName": "ABC 科技有限公司",
        "tenantType": 1,
        "tenantTypeName": "企业版",
        "contactName": "张三",
        "contactPhone": "13800138000",
        "contactEmail": "zhangsan@abc.com",
        "status": 1,
        "statusName": "正常",
        "expireTime": "2027-01-01 00:00:00",
        "packageName": "专业版",
        "maxEmployeeCount": 100,
        "currentEmployeeCount": 35,
        "maxStorageSize": 10737418240,
        "currentStorageSize": 2147483648,
        "createTime": "2026-01-01 10:00:00",
        "updateTime": "2026-01-20 15:30:00"
      }
    ]
  }
}
```

---

### 2.2 商户详情查询

**接口**: `GET /tenant/{tenantId}`

**权限**: `@SaCheckPermission("tenant:detail")`

**路径参数**:

| 参数 | 类型 | 说明 |
|-----|------|------|
| tenantId | Long | 商户 ID |

**响应示例**:

```json
{
  "code": 1,
  "ok": true,
  "msg": "成功",
  "data": {
    "tenantId": 1001,
    "tenantCode": "abc_company",
    "tenantName": "ABC 科技有限公司",
    "tenantType": 1,
    "tenantTypeName": "企业版",
    "contactName": "张三",
    "contactPhone": "13800138000",
    "contactEmail": "zhangsan@abc.com",
    "status": 1,
    "statusName": "正常",
    "expireTime": "2027-01-01 00:00:00",
    "packageId": 1,
    "packageName": "专业版",
    "maxEmployeeCount": 100,
    "currentEmployeeCount": 35,
    "maxStorageSize": 10737418240,
    "currentStorageSize": 2147483648,
    "logoUrl": "https://cdn.example.com/logos/abc.png",
    "themeColor": "#1890ff",
    "systemName": "ABC 智能管理系统",
    "domain": "abc.mysaas.com",
    "createTime": "2026-01-01 10:00:00",
    "updateTime": "2026-01-20 15:30:00",
    "adminEmployee": {
      "employeeId": 10001,
      "loginName": "admin",
      "actualName": "管理员",
      "phone": "13800138000",
      "email": "admin@abc.com"
    }
  }
}
```

---

### 2.3 创建商户

**接口**: `POST /tenant/add`

**权限**: `@SaCheckPermission("tenant:add")`

**请求参数**:

```json
{
  "tenantCode": "abc_company",
  "tenantName": "ABC 科技有限公司",
  "tenantType": 1,
  "contactName": "张三",
  "contactPhone": "13800138000",
  "contactEmail": "zhangsan@abc.com",
  "packageId": 1,
  "expireTime": "2027-01-01 00:00:00",
  "adminLoginName": "admin",
  "adminPassword": "Admin@123",
  "adminActualName": "管理员",
  "adminPhone": "13800138000",
  "adminEmail": "admin@abc.com"
}
```

**参数说明**:

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|-----|------|------|---------|------|
| tenantCode | String | 是 | 1-50字符，字母数字下划线 | 商户编码（唯一） |
| tenantName | String | 是 | 1-100字符 | 商户名称 |
| tenantType | Integer | 是 | 1/2/3 | 商户类型：1=企业版 2=专业版 3=免费版 |
| contactName | String | 是 | 1-50字符 | 联系人姓名 |
| contactPhone | String | 是 | 手机号格式 | 联系电话 |
| contactEmail | String | 否 | 邮箱格式 | 联系邮箱 |
| packageId | Long | 是 | 存在的套餐 ID | 套餐 ID |
| expireTime | String | 是 | 日期时间格式 | 到期时间 |
| adminLoginName | String | 是 | 1-50字符 | 管理员登录名 |
| adminPassword | String | 是 | 8-20字符 | 管理员密码 |
| adminActualName | String | 是 | 1-50字符 | 管理员真实姓名 |
| adminPhone | String | 否 | 手机号格式 | 管理员手机号 |
| adminEmail | String | 否 | 邮箱格式 | 管理员邮箱 |

**响应示例**:

```json
{
  "code": 1,
  "ok": true,
  "msg": "商户创建成功",
  "data": {
    "tenantId": 1001,
    "tenantCode": "abc_company",
    "adminEmployeeId": 10001,
    "adminLoginName": "admin",
    "adminInitialPassword": "Admin@123"
  }
}
```

**业务逻辑**:
1. 验证商户编码唯一性
2. 验证套餐是否存在
3. 创建商户记录
4. 创建默认部门（总经办）
5. 创建默认角色（管理员、普通员工）
6. 创建管理员账号
7. 初始化菜单权限（根据套餐）
8. 发送欢迎邮件（可选）

---

### 2.4 编辑商户

**接口**: `POST /tenant/update`

**权限**: `@SaCheckPermission("tenant:update")`

**请求参数**:

```json
{
  "tenantId": 1001,
  "tenantName": "ABC 科技有限公司（新）",
  "contactName": "李四",
  "contactPhone": "13900139000",
  "contactEmail": "lisi@abc.com",
  "packageId": 2,
  "expireTime": "2028-01-01 00:00:00",
  "maxEmployeeCount": 200,
  "maxStorageSize": 21474836480
}
```

**参数说明**: 同创建接口，tenantCode 不可修改

**响应示例**:

```json
{
  "code": 1,
  "ok": true,
  "msg": "更新成功",
  "data": null
}
```

---

### 2.5 启用/停用商户

**接口**: `POST /tenant/updateStatus`

**权限**: `@SaCheckPermission("tenant:updateStatus")`

**请求参数**:

```json
{
  "tenantId": 1001,
  "status": 4
}
```

**参数说明**:

| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| tenantId | Long | 是 | 商户 ID |
| status | Integer | 是 | 状态：1=正常 4=停用 |

**响应示例**:

```json
{
  "code": 1,
  "ok": true,
  "msg": "状态更新成功",
  "data": null
}
```

**业务逻辑**:
- 停用商户后，该商户所有用户无法登录
- 正在登录的用户 Token 会被踢下线

---

### 2.6 删除商户

**接口**: `POST /tenant/delete/{tenantId}`

**权限**: `@SaCheckPermission("tenant:delete")`

**路径参数**:

| 参数 | 类型 | 说明 |
|-----|------|------|
| tenantId | Long | 商户 ID |

**响应示例**:

```json
{
  "code": 1,
  "ok": true,
  "msg": "删除成功",
  "data": null
}
```

**业务逻辑**:
- 软删除（标记 status=5）
- 30 天后可选择性归档或物理删除
- 删除后商户编码可被释放重新使用

---

### 2.7 商户配额使用统计

**接口**: `GET /tenant/quota/{tenantId}`

**权限**: `@SaCheckPermission("tenant:quota")`

**路径参数**:

| 参数 | 类型 | 说明 |
|-----|------|------|
| tenantId | Long | 商户 ID |

**响应示例**:

```json
{
  "code": 1,
  "ok": true,
  "msg": "成功",
  "data": {
    "tenantId": 1001,
    "tenantName": "ABC 科技有限公司",
    "employeeQuota": {
      "maxCount": 100,
      "currentCount": 35,
      "usageRate": 0.35,
      "isExceeded": false
    },
    "storageQuota": {
      "maxSize": 10737418240,
      "maxSizeFormatted": "10 GB",
      "currentSize": 2147483648,
      "currentSizeFormatted": "2 GB",
      "usageRate": 0.2,
      "isExceeded": false
    },
    "apiCallQuota": {
      "maxCallsPerDay": 10000,
      "currentCalls": 3250,
      "usageRate": 0.325,
      "isExceeded": false
    }
  }
}
```

---

### 2.8 商户白标配置

**接口**: `POST /tenant/branding`

**权限**: `@SaCheckPermission("tenant:branding")` 或 `tenant_admin`

**请求参数**:

```json
{
  "tenantId": 1001,
  "logoUrl": "https://cdn.example.com/logos/abc-new.png",
  "themeColor": "#1890ff",
  "systemName": "ABC 智能管理系统"
}
```

**参数说明**:

| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| tenantId | Long | 是 | 商户 ID |
| logoUrl | String | 否 | Logo URL（最大 500 字符） |
| themeColor | String | 否 | 主题色（16进制颜色码） |
| systemName | String | 否 | 系统名称（最大 100 字符） |

**响应示例**:

```json
{
  "code": 1,
  "ok": true,
  "msg": "白标配置更新成功",
  "data": null
}
```

---

## 3. 套餐管理接口

### 3.1 套餐列表查询

**接口**: `POST /tenant/package/query`

**权限**: `@SaCheckPermission("tenant_package:query")`

**请求参数**:

```json
{
  "pageNum": 1,
  "pageSize": 10,
  "packageName": "专业版",
  "disabledFlag": false
}
```

**响应示例**:

```json
{
  "code": 1,
  "ok": true,
  "msg": "成功",
  "data": {
    "pageNum": 1,
    "pageSize": 10,
    "total": 3,
    "list": [
      {
        "packageId": 1,
        "packageCode": "professional",
        "packageName": "专业版",
        "maxEmployeeCount": 100,
        "maxStorageSize": 10737418240,
        "maxStorageSizeFormatted": "10 GB",
        "maxApiCallsPerDay": 10000,
        "featureFlags": {
          "enable_oa_notice": true,
          "enable_approval_flow": true,
          "enable_advanced_report": true
        },
        "priceMonthly": 999.00,
        "priceYearly": 9999.00,
        "tenantCount": 15,
        "sort": 1,
        "disabledFlag": false,
        "createTime": "2026-01-01 10:00:00"
      }
    ]
  }
}
```

---

### 3.2 创建套餐

**接口**: `POST /tenant/package/add`

**权限**: `@SaCheckPermission("tenant_package:add")`

**请求参数**:

```json
{
  "packageCode": "enterprise",
  "packageName": "企业版",
  "maxEmployeeCount": 500,
  "maxStorageSize": 53687091200,
  "maxApiCallsPerDay": 50000,
  "featureFlags": {
    "enable_oa_notice": true,
    "enable_approval_flow": true,
    "enable_advanced_report": true,
    "enable_custom_fields": true,
    "enable_api_access": true
  },
  "priceMonthly": 4999.00,
  "priceYearly": 49999.00,
  "sort": 1
}
```

**参数说明**:

| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| packageCode | String | 是 | 套餐编码（唯一，1-50字符） |
| packageName | String | 是 | 套餐名称（1-100字符） |
| maxEmployeeCount | Integer | 是 | 员工数量上限 |
| maxStorageSize | Long | 是 | 存储空间上限（字节） |
| maxApiCallsPerDay | Integer | 否 | API 调用次数上限（每天） |
| featureFlags | JSON | 否 | 功能开关配置 |
| priceMonthly | BigDecimal | 否 | 月付价格 |
| priceYearly | BigDecimal | 否 | 年付价格 |
| sort | Integer | 否 | 排序（默认 0） |

**响应示例**:

```json
{
  "code": 1,
  "ok": true,
  "msg": "套餐创建成功",
  "data": {
    "packageId": 3
  }
}
```

---

### 3.3 编辑套餐

**接口**: `POST /tenant/package/update`

**权限**: `@SaCheckPermission("tenant_package:update")`

**请求参数**: 同创建接口，需包含 `packageId`

---

### 3.4 删除套餐

**接口**: `POST /tenant/package/delete/{packageId}`

**权限**: `@SaCheckPermission("tenant_package:delete")`

**业务规则**: 只能删除未被使用的套餐

---

## 4. 平台管理员接口

### 4.1 平台管理员列表查询

**接口**: `POST /platform/admin/query`

**权限**: `@SaCheckPermission("platform_admin:query")`

**请求参数**:

```json
{
  "pageNum": 1,
  "pageSize": 10,
  "loginName": "admin",
  "actualName": "张三",
  "roleType": 1,
  "disabledFlag": false
}
```

**响应示例**:

```json
{
  "code": 1,
  "ok": true,
  "msg": "成功",
  "data": {
    "list": [
      {
        "adminId": 1,
        "loginName": "superadmin",
        "actualName": "超级管理员",
        "phone": "13800138000",
        "email": "admin@platform.com",
        "roleType": 1,
        "roleTypeName": "超级管理员",
        "disabledFlag": false,
        "createTime": "2026-01-01 10:00:00"
      }
    ]
  }
}
```

---

### 4.2 创建平台管理员

**接口**: `POST /platform/admin/add`

**权限**: `@SaCheckPermission("platform_admin:add")`

**请求参数**:

```json
{
  "loginName": "operator01",
  "loginPwd": "Admin@123",
  "actualName": "运营专员01",
  "phone": "13900139000",
  "email": "operator01@platform.com",
  "roleType": 2
}
```

**参数说明**:

| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| loginName | String | 是 | 登录名（唯一，1-50字符） |
| loginPwd | String | 是 | 密码（8-20字符） |
| actualName | String | 是 | 真实姓名（1-50字符） |
| phone | String | 否 | 手机号 |
| email | String | 否 | 邮箱 |
| roleType | Integer | 是 | 角色类型：1=超级管理员 2=运营人员 3=审计人员 |

---

### 4.3 编辑平台管理员

**接口**: `POST /platform/admin/update`

**权限**: `@SaCheckPermission("platform_admin:update")`

---

### 4.4 删除平台管理员

**接口**: `POST /platform/admin/delete/{adminId}`

**权限**: `@SaCheckPermission("platform_admin:delete")`

**业务规则**: 不能删除自己

---

## 5. 登录接口改造

### 5.1 商户用户登录

**接口**: `POST /login`

**权限**: `@NoNeedLogin`

**请求参数**:

```json
{
  "tenantCode": "abc_company",
  "loginName": "zhangsan",
  "loginPwd": "Password@123",
  "captchaCode": "1234",
  "captchaUuid": "550e8400-e29b-41d4-a716-446655440000"
}
```

**参数说明**:

| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| tenantCode | String | 是 | 商户编码 |
| loginName | String | 是 | 登录名 |
| loginPwd | String | 是 | 密码 |
| captchaCode | String | 是 | 验证码 |
| captchaUuid | String | 是 | 验证码 UUID |

**响应示例**:

```json
{
  "code": 1,
  "ok": true,
  "msg": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "employeeId": 10001,
    "loginName": "zhangsan",
    "actualName": "张三",
    "tenantId": 1001,
    "tenantCode": "abc_company",
    "tenantName": "ABC 科技有限公司",
    "tenantLogo": "https://cdn.example.com/logos/abc.png",
    "tenantThemeColor": "#1890ff",
    "tenantSystemName": "ABC 智能管理系统",
    "administratorFlag": false,
    "permissions": ["employee:query", "department:query"],
    "menus": [...]
  }
}
```

---

### 5.2 平台管理员登录

**接口**: `POST /platform/login`

**权限**: `@NoNeedLogin`

**请求参数**:

```json
{
  "loginName": "superadmin",
  "loginPwd": "Admin@123",
  "captchaCode": "1234",
  "captchaUuid": "550e8400-e29b-41d4-a716-446655440000"
}
```

**响应示例**:

```json
{
  "code": 1,
  "ok": true,
  "msg": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "adminId": 1,
    "loginName": "superadmin",
    "actualName": "超级管理员",
    "roleType": 1,
    "roleTypeName": "超级管理员",
    "permissions": ["tenant:*", "platform_admin:*"],
    "menus": [...]
  }
}
```

---

### 5.3 获取登录信息

**接口**: `GET /login/info`

**权限**: 登录后

**响应示例（商户用户）**:

```json
{
  "code": 1,
  "ok": true,
  "msg": "成功",
  "data": {
    "userType": "employee",
    "employeeId": 10001,
    "loginName": "zhangsan",
    "actualName": "张三",
    "tenantId": 1001,
    "tenantCode": "abc_company",
    "tenantName": "ABC 科技有限公司"
  }
}
```

**响应示例（平台管理员）**:

```json
{
  "code": 1,
  "ok": true,
  "msg": "成功",
  "data": {
    "userType": "platform_admin",
    "adminId": 1,
    "loginName": "superadmin",
    "actualName": "超级管理员",
    "roleType": 1
  }
}
```

---

## 6. 数据模型定义

### 6.1 TenantVO

```java
@Data
public class TenantVO {
    private Long tenantId;
    private String tenantCode;
    private String tenantName;
    private Integer tenantType;
    private String tenantTypeName;
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private Integer status;
    private String statusName;
    private LocalDateTime expireTime;
    private Long packageId;
    private String packageName;
    private Integer maxEmployeeCount;
    private Integer currentEmployeeCount;
    private Long maxStorageSize;
    private Long currentStorageSize;
    private String logoUrl;
    private String themeColor;
    private String systemName;
    private String domain;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

---

### 6.2 TenantAddForm

```java
@Data
public class TenantAddForm {

    @NotBlank(message = "商户编码不能为空")
    @Length(max = 50, message = "商户编码最长50字符")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "商户编码只能包含字母、数字、下划线")
    private String tenantCode;

    @NotBlank(message = "商户名称不能为空")
    @Length(max = 100, message = "商户名称最长100字符")
    private String tenantName;

    @NotNull(message = "商户类型不能为空")
    private Integer tenantType;

    @NotBlank(message = "联系人姓名不能为空")
    @Length(max = 50, message = "联系人姓名最长50字符")
    private String contactName;

    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String contactPhone;

    @Email(message = "邮箱格式不正确")
    @Length(max = 100, message = "邮箱最长100字符")
    private String contactEmail;

    @NotNull(message = "套餐ID不能为空")
    private Long packageId;

    @NotNull(message = "到期时间不能为空")
    @Future(message = "到期时间必须是未来时间")
    private LocalDateTime expireTime;

    @NotBlank(message = "管理员登录名不能为空")
    @Length(max = 50, message = "管理员登录名最长50字符")
    private String adminLoginName;

    @NotBlank(message = "管理员密码不能为空")
    @Length(min = 8, max = 20, message = "管理员密码长度8-20字符")
    private String adminPassword;

    @NotBlank(message = "管理员姓名不能为空")
    @Length(max = 50, message = "管理员姓名最长50字符")
    private String adminActualName;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String adminPhone;

    @Email(message = "邮箱格式不正确")
    private String adminEmail;
}
```

---

### 6.3 TenantQueryForm

```java
@Data
public class TenantQueryForm extends PageParam {

    private String tenantName;

    private String tenantCode;

    private Integer status;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}
```

---

### 6.4 LoginForm

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

### 6.5 LoginVO

```java
@Data
public class LoginVO {
    private String token;
    private Long employeeId;
    private String loginName;
    private String actualName;
    private Long tenantId;
    private String tenantCode;
    private String tenantName;
    private String tenantLogo;
    private String tenantThemeColor;
    private String tenantSystemName;
    private Boolean administratorFlag;
    private List<String> permissions;
    private List<MenuVO> menus;
}
```

---

## 7. 错误码定义

### 7.1 租户相关错误码

```java
public enum TenantErrorCode implements ErrorCode {

    // 商户错误码 (40001-40099)
    TENANT_NOT_EXIST(40001, "商户不存在"),
    TENANT_DISABLED(40002, "商户已被停用，请联系平台管理员"),
    TENANT_EXPIRED(40003, "商户已过期，请联系平台管理员续费"),
    TENANT_ID_MISSING(40004, "租户ID缺失，请重新登录"),
    TENANT_MISMATCH(40005, "租户信息不匹配，可能存在安全风险"),
    TENANT_CODE_DUPLICATE(40006, "商户编码已存在，请更换"),
    TENANT_EXCEED_EMPLOYEE_LIMIT(40007, "员工数量已达上限，请升级套餐"),
    TENANT_EXCEED_STORAGE_LIMIT(40008, "存储空间已达上限，请升级套餐"),
    TENANT_DELETE_HAS_DATA(40009, "商户下还有数据，无法删除"),

    // 套餐错误码 (40100-40199)
    PACKAGE_NOT_EXIST(40100, "套餐不存在"),
    PACKAGE_CODE_DUPLICATE(40101, "套餐编码已存在"),
    PACKAGE_IN_USE(40102, "套餐正在被使用，无法删除"),

    // 平台管理员错误码 (40200-40299)
    PLATFORM_ADMIN_NOT_EXIST(40200, "平台管理员不存在"),
    PLATFORM_ADMIN_LOGIN_NAME_DUPLICATE(40201, "登录名已存在"),
    PLATFORM_ADMIN_CANNOT_DELETE_SELF(40202, "不能删除自己"),
    PLATFORM_ADMIN_SUPER_ADMIN_REQUIRED(40203, "只有超级管理员可以执行此操作"),

    // 登录错误码 (40300-40399)
    LOGIN_TENANT_CODE_INVALID(40300, "商户编码不存在"),
    LOGIN_ACCOUNT_PASSWORD_ERROR(40301, "账号或密码错误"),
    LOGIN_CAPTCHA_ERROR(40302, "验证码错误"),
    LOGIN_ACCOUNT_DISABLED(40303, "账号已被停用"),
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

## 8. 接口调用示例

### 8.1 完整流程示例（平台管理员创建商户）

#### 步骤 1: 平台管理员登录

```bash
POST /platform/login
Content-Type: application/json

{
  "loginName": "superadmin",
  "loginPwd": "Admin@123",
  "captchaCode": "1234",
  "captchaUuid": "550e8400-e29b-41d4-a716-446655440000"
}
```

**响应**:

```json
{
  "code": 1,
  "ok": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

---

#### 步骤 2: 查询可用套餐

```bash
POST /tenant/package/query
x-access-token: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "pageNum": 1,
  "pageSize": 100,
  "disabledFlag": false
}
```

**响应**: 返回套餐列表，选择 packageId=1

---

#### 步骤 3: 创建商户

```bash
POST /tenant/add
x-access-token: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "tenantCode": "new_company",
  "tenantName": "新科技公司",
  "tenantType": 1,
  "contactName": "王五",
  "contactPhone": "13700137000",
  "contactEmail": "wangwu@newcompany.com",
  "packageId": 1,
  "expireTime": "2027-01-01 00:00:00",
  "adminLoginName": "admin",
  "adminPassword": "Admin@123",
  "adminActualName": "管理员",
  "adminPhone": "13700137000",
  "adminEmail": "admin@newcompany.com"
}
```

**响应**:

```json
{
  "code": 1,
  "ok": true,
  "msg": "商户创建成功",
  "data": {
    "tenantId": 1002,
    "tenantCode": "new_company",
    "adminEmployeeId": 20001,
    "adminLoginName": "admin"
  }
}
```

---

### 8.2 商户用户登录流程

#### 步骤 1: 商户用户登录

```bash
POST /login
Content-Type: application/json

{
  "tenantCode": "new_company",
  "loginName": "admin",
  "loginPwd": "Admin@123",
  "captchaCode": "1234",
  "captchaUuid": "550e8400-e29b-41d4-a716-446655440000"
}
```

**响应**:

```json
{
  "code": 1,
  "ok": true,
  "msg": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "employeeId": 20001,
    "tenantId": 1002,
    "tenantName": "新科技公司"
  }
}
```

---

#### 步骤 2: 访问业务接口

```bash
POST /employee/query
x-access-token: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "pageNum": 1,
  "pageSize": 10
}
```

**说明**: 后端自动从 Token 中提取 tenantId=1002，只返回该商户的员工数据

---

## 9. 接口测试建议

### 9.1 Postman Collection

建议创建以下 Postman Collection:

1. **Platform Admin** - 平台管理员接口
2. **Tenant Management** - 商户管理接口
3. **Package Management** - 套餐管理接口
4. **Tenant User** - 商户用户接口
5. **Login** - 登录接口

### 9.2 测试数据准备

```sql
-- 创建平台管理员
INSERT INTO t_platform_admin (login_name, login_pwd, actual_name, role_type)
VALUES ('superadmin', '加密后的密码', '超级管理员', 1);

-- 创建默认套餐
INSERT INTO t_tenant_package (package_code, package_name, max_employee_count, max_storage_size)
VALUES ('basic', '基础版', 50, 5368709120);

-- 创建测试商户
INSERT INTO t_tenant (tenant_code, tenant_name, status, package_id, expire_time)
VALUES ('test_tenant', '测试商户', 1, 1, '2027-12-31 23:59:59');
```

---

**文档结束**

---

## 附录

### A. 接口权限矩阵

| 接口分组 | 平台超级管理员 | 平台运营人员 | 商户管理员 | 商户普通用户 |
|---------|--------------|------------|----------|------------|
| 商户管理 | ✅ 全部 | ✅ 只读 | ❌ | ❌ |
| 套餐管理 | ✅ 全部 | ✅ 只读 | ❌ | ❌ |
| 平台管理员 | ✅ 全部 | ❌ | ❌ | ❌ |
| 商户配置 | ✅ 全部 | ❌ | ✅ 本商户 | ❌ |
| 业务数据 | ✅ 跨租户 | ✅ 只读 | ✅ 本商户 | ✅ 受权限控制 |

---

### B. 接口版本控制

当前版本: **v1.0**

版本升级计划:
- v1.1: 增加商户数据导出接口
- v1.2: 增加商户配额告警接口
- v2.0: 支持域名自动识别登录

---

**联系方式**: 如有疑问请联系 API 设计负责人
