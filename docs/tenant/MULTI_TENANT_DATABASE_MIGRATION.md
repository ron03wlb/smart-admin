# SmartAdmin 多商户数据库迁移指南

**文档版本**: v1.0
**创建日期**: 2026-01-21
**关联文档**: [MULTI_TENANT_REQUIREMENTS.md](./MULTI_TENANT_REQUIREMENTS.md)

---

## ⚠️ 重要提示

**在执行任何数据库迁移操作前，请务必完成以下检查：**

1. ✅ **数据备份**: 完整备份生产数据库
2. ✅ **在测试环境验证**: 先在测试环境执行完整迁移流程
3. ✅ **停机窗口**: 确认业务停机窗口（建议 2-4 小时）
4. ✅ **回滚准备**: 准备好回滚脚本和数据备份
5. ✅ **团队待命**: DBA 和开发团队待命，随时处理异常

---

## 📋 目录

- [1. 迁移概述](#1-迁移概述)
- [2. 迁移前准备](#2-迁移前准备)
- [3. 新增表创建](#3-新增表创建)
- [4. 现有表改造](#4-现有表改造)
- [5. 数据迁移](#5-数据迁移)
- [6. 索引优化](#6-索引优化)
- [7. 回滚方案](#7-回滚方案)
- [8. 迁移后验证](#8-迁移后验证)
- [9. 性能优化](#9-性能优化)

---

## 1. 迁移概述

### 1.1 迁移目标

将单租户 SmartAdmin 系统升级为多租户系统，需要完成以下数据库改造：

1. **新增多租户核心表**:
   - `t_tenant` - 商户表
   - `t_tenant_config` - 商户配置表
   - `t_tenant_package` - 商户套餐表
   - `t_platform_admin` - 平台管理员表
   - `t_platform_audit_log` - 平台审计日志表

2. **改造现有表**:
   - 为所有核心业务表添加 `tenant_id` 字段
   - 为 `tenant_id` 字段创建索引
   - 迁移现有数据到默认商户（tenant_id=1）

3. **数据完整性保障**:
   - 验证所有数据都有正确的 tenant_id
   - 验证外键关系完整性
   - 验证业务数据一致性

---

### 1.2 迁移时间估算

| 数据规模 | 估计时间 | 说明 |
|---------|---------|------|
| 小型（< 10 万条记录） | 30 分钟 | 快速迁移 |
| 中型（10-100 万条记录） | 1-2 小时 | 需要分批处理 |
| 大型（> 100 万条记录） | 2-4 小时 | 需要优化迁移策略 |

---

### 1.3 迁移策略

**在线迁移 vs 停机迁移**:

| 策略 | 优点 | 缺点 | 适用场景 |
|-----|------|------|---------|
| **停机迁移（推荐）** | 操作简单，数据一致性保障强 | 需要停机窗口 | 生产环境首次迁移 |
| **在线迁移** | 不影响业务 | 实现复杂，需要双写 | 大型生产系统，停机成本高 |

**本指南采用停机迁移策略**，适合大多数场景。

---

## 2. 迁移前准备

### 2.1 环境检查

#### 检查清单

```bash
# 1. MySQL 版本检查（推荐 5.7+）
mysql --version

# 2. 数据库连接检查
mysql -h localhost -u root -p -e "SELECT VERSION();"

# 3. 磁盘空间检查（至少保留 2 倍数据库大小的空间）
df -h

# 4. 数据库大小查询
mysql -u root -p -e "
SELECT
    table_schema AS 'Database',
    ROUND(SUM(data_length + index_length) / 1024 / 1024, 2) AS 'Size (MB)'
FROM information_schema.tables
WHERE table_schema = 'smart_admin'
GROUP BY table_schema;
"

# 5. 表记录数统计
mysql -u root -p smart_admin -e "
SELECT
    table_name AS 'Table',
    table_rows AS 'Rows'
FROM information_schema.tables
WHERE table_schema = 'smart_admin'
ORDER BY table_rows DESC;
"
```

---

### 2.2 数据备份

#### 完整备份（强烈推荐）

```bash
#!/bin/bash
# backup.sh - 数据库完整备份脚本

TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_DIR="/data/backup/smart_admin"
DB_NAME="smart_admin"
DB_USER="root"
DB_PASS="your_password"

# 创建备份目录
mkdir -p $BACKUP_DIR

# 备份数据库
mysqldump \
  -u $DB_USER \
  -p$DB_PASS \
  --single-transaction \
  --routines \
  --triggers \
  --events \
  --hex-blob \
  $DB_NAME \
  | gzip > $BACKUP_DIR/smart_admin_backup_$TIMESTAMP.sql.gz

# 验证备份文件
if [ -f "$BACKUP_DIR/smart_admin_backup_$TIMESTAMP.sql.gz" ]; then
    echo "✅ 备份成功: $BACKUP_DIR/smart_admin_backup_$TIMESTAMP.sql.gz"
    ls -lh $BACKUP_DIR/smart_admin_backup_$TIMESTAMP.sql.gz
else
    echo "❌ 备份失败!"
    exit 1
fi
```

**执行备份**:

```bash
chmod +x backup.sh
./backup.sh
```

---

#### 快速恢复测试

```bash
# 1. 创建测试数据库
mysql -u root -p -e "CREATE DATABASE smart_admin_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 2. 恢复备份到测试数据库
gunzip < /data/backup/smart_admin/smart_admin_backup_20260121_100000.sql.gz | mysql -u root -p smart_admin_test

# 3. 验证数据完整性
mysql -u root -p smart_admin_test -e "SELECT COUNT(*) FROM t_employee;"

# 4. 删除测试数据库
mysql -u root -p -e "DROP DATABASE smart_admin_test;"
```

---

### 2.3 依赖检查

#### 检查外键约束

```sql
-- 查询所有外键约束
SELECT
    CONSTRAINT_NAME,
    TABLE_NAME,
    COLUMN_NAME,
    REFERENCED_TABLE_NAME,
    REFERENCED_COLUMN_NAME
FROM
    information_schema.KEY_COLUMN_USAGE
WHERE
    REFERENCED_TABLE_SCHEMA = 'smart_admin'
    AND REFERENCED_TABLE_NAME IS NOT NULL
ORDER BY
    TABLE_NAME;
```

**注意**: 如果存在外键约束，迁移时需要注意顺序。

---

#### 检查触发器

```sql
-- 查询所有触发器
SHOW TRIGGERS FROM smart_admin;
```

**注意**: 触发器可能影响迁移，需要检查是否需要临时禁用。

---

## 3. 新增表创建

### 3.1 商户表（t_tenant）

```sql
-- ========================================
-- 商户表
-- ========================================
CREATE TABLE IF NOT EXISTS `t_tenant` (
    `tenant_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '商户ID',
    `tenant_code` VARCHAR(50) NOT NULL COMMENT '商户编码（唯一标识）',
    `tenant_name` VARCHAR(100) NOT NULL COMMENT '商户名称',
    `tenant_type` TINYINT NOT NULL DEFAULT 1 COMMENT '商户类型: 1=企业版 2=专业版 3=免费版',

    -- 联系信息
    `contact_name` VARCHAR(50) DEFAULT NULL COMMENT '联系人姓名',
    `contact_phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
    `contact_email` VARCHAR(100) DEFAULT NULL COMMENT '联系邮箱',

    -- 状态管理
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1=正常 2=试用 3=过期 4=停用 5=已删除',
    `expire_time` DATETIME DEFAULT NULL COMMENT '到期时间',

    -- 配额限制
    `max_employee_count` INT DEFAULT 50 COMMENT '员工数量上限',
    `max_storage_size` BIGINT DEFAULT 10737418240 COMMENT '存储空间上限(字节, 默认10GB)',

    -- 白标配置
    `logo_url` VARCHAR(500) DEFAULT NULL COMMENT '商户Logo URL',
    `theme_color` VARCHAR(20) DEFAULT '#1890ff' COMMENT '主题色',
    `system_name` VARCHAR(100) DEFAULT NULL COMMENT '系统名称（白标）',

    -- 域名绑定（可选）
    `domain` VARCHAR(200) DEFAULT NULL COMMENT '绑定域名',

    -- 审计字段
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`tenant_id`),
    UNIQUE KEY `uk_tenant_code` (`tenant_code`),
    UNIQUE KEY `uk_domain` (`domain`),
    KEY `idx_status` (`status`),
    KEY `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商户表';

-- 创建默认商户（用于迁移现有数据）
INSERT INTO `t_tenant` (
    `tenant_id`,
    `tenant_code`,
    `tenant_name`,
    `tenant_type`,
    `status`,
    `expire_time`,
    `max_employee_count`,
    `max_storage_size`
) VALUES (
    1,
    'default',
    '默认商户',
    1,
    1,
    '2099-12-31 23:59:59',
    10000,
    107374182400
);
```

---

### 3.2 商户配置表（t_tenant_config）

```sql
-- ========================================
-- 商户配置表
-- ========================================
CREATE TABLE IF NOT EXISTS `t_tenant_config` (
    `config_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '配置ID',
    `tenant_id` BIGINT NOT NULL COMMENT '商户ID',
    `config_key` VARCHAR(100) NOT NULL COMMENT '配置键',
    `config_value` TEXT DEFAULT NULL COMMENT '配置值（JSON格式）',
    `config_type` VARCHAR(50) DEFAULT 'system' COMMENT '配置类型: system=系统配置 business=业务配置',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',

    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`config_id`),
    UNIQUE KEY `uk_tenant_key` (`tenant_id`, `config_key`),
    KEY `idx_tenant_id` (`tenant_id`),
    KEY `idx_config_type` (`config_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商户配置表';
```

---

### 3.3 商户套餐表（t_tenant_package）

```sql
-- ========================================
-- 商户套餐表
-- ========================================
CREATE TABLE IF NOT EXISTS `t_tenant_package` (
    `package_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '套餐ID',
    `package_name` VARCHAR(100) NOT NULL COMMENT '套餐名称',
    `package_code` VARCHAR(50) NOT NULL COMMENT '套餐编码',

    -- 配额
    `max_employee_count` INT NOT NULL DEFAULT 50 COMMENT '员工数量上限',
    `max_storage_size` BIGINT NOT NULL DEFAULT 10737418240 COMMENT '存储空间上限(字节)',
    `max_api_calls_per_day` INT DEFAULT NULL COMMENT 'API调用次数上限（每天）',

    -- 功能权限（JSON）
    `feature_flags` JSON DEFAULT NULL COMMENT '功能开关配置',

    -- 价格
    `price_monthly` DECIMAL(10,2) DEFAULT NULL COMMENT '月付价格',
    `price_yearly` DECIMAL(10,2) DEFAULT NULL COMMENT '年付价格',

    `sort` INT DEFAULT 0 COMMENT '排序',
    `disabled_flag` TINYINT DEFAULT 0 COMMENT '是否禁用: 0=否 1=是',

    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`package_id`),
    UNIQUE KEY `uk_package_code` (`package_code`),
    KEY `idx_disabled_flag` (`disabled_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商户套餐表';

-- 创建默认套餐
INSERT INTO `t_tenant_package` (
    `package_code`,
    `package_name`,
    `max_employee_count`,
    `max_storage_size`,
    `max_api_calls_per_day`,
    `price_monthly`,
    `price_yearly`,
    `sort`
) VALUES
('basic', '基础版', 50, 5368709120, 5000, 299.00, 2999.00, 1),
('professional', '专业版', 100, 10737418240, 10000, 999.00, 9999.00, 2),
('enterprise', '企业版', 500, 53687091200, 50000, 4999.00, 49999.00, 3);
```

---

### 3.4 平台管理员表（t_platform_admin）

```sql
-- ========================================
-- 平台管理员表
-- ========================================
CREATE TABLE IF NOT EXISTS `t_platform_admin` (
    `admin_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '管理员ID',
    `login_name` VARCHAR(50) NOT NULL COMMENT '登录账号',
    `login_pwd` VARCHAR(100) NOT NULL COMMENT '登录密码（加密）',
    `actual_name` VARCHAR(50) NOT NULL COMMENT '真实姓名',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',

    -- 平台管理员角色
    `role_type` TINYINT NOT NULL DEFAULT 1 COMMENT '角色类型: 1=超级管理员 2=运营人员 3=审计人员',

    `disabled_flag` TINYINT DEFAULT 0 COMMENT '是否禁用: 0=否 1=是',
    `deleted_flag` TINYINT DEFAULT 0 COMMENT '是否删除: 0=否 1=是',

    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`admin_id`),
    UNIQUE KEY `uk_login_name` (`login_name`),
    KEY `idx_disabled_flag` (`disabled_flag`),
    KEY `idx_deleted_flag` (`deleted_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='平台管理员表';

-- 创建默认超级管理员（密码: Admin@123）
INSERT INTO `t_platform_admin` (
    `login_name`,
    `login_pwd`,
    `actual_name`,
    `role_type`
) VALUES (
    'superadmin',
    '$2a$10$8JdvBxNPYZf8sU5l.ZJCTO7lN8qGHUq5a8v/gX5qJPqMhqZYUYYg2',  -- Admin@123 的 BCrypt 加密
    '超级管理员',
    1
);
```

---

### 3.5 平台审计日志表（t_platform_audit_log）

```sql
-- ========================================
-- 平台审计日志表
-- ========================================
CREATE TABLE IF NOT EXISTS `t_platform_audit_log` (
    `log_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `operator_id` BIGINT NOT NULL COMMENT '操作人ID',
    `operator_type` VARCHAR(20) NOT NULL COMMENT '操作人类型: platform_admin/employee',
    `tenant_id` BIGINT DEFAULT NULL COMMENT '关联商户ID',
    `operation` VARCHAR(100) NOT NULL COMMENT '操作类型',
    `operation_detail` TEXT DEFAULT NULL COMMENT '操作详情（JSON）',
    `ip` VARCHAR(50) DEFAULT NULL COMMENT 'IP地址',
    `user_agent` VARCHAR(500) DEFAULT NULL COMMENT '用户代理',

    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',

    PRIMARY KEY (`log_id`),
    KEY `idx_operator` (`operator_id`, `operator_type`),
    KEY `idx_tenant_id` (`tenant_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='平台审计日志表';
```

---

## 4. 现有表改造

### 4.1 需要添加 tenant_id 的表清单

#### 核心业务表（必须添加）

```sql
-- 员工表
ALTER TABLE `t_employee`
ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '商户ID' AFTER `employee_id`;

-- 部门表
ALTER TABLE `t_department`
ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '商户ID' AFTER `department_id`;

-- 角色表
ALTER TABLE `t_role`
ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '商户ID' AFTER `role_id`;

-- 职位表（如果存在）
ALTER TABLE `t_position`
ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '商户ID' AFTER `position_id`;

-- 菜单表（商户级别菜单定制，NULL 表示平台菜单）
ALTER TABLE `t_menu`
ADD COLUMN `tenant_id` BIGINT DEFAULT NULL COMMENT '商户ID（NULL表示平台菜单）' AFTER `menu_id`;
```

---

#### 业务表（根据实际情况添加）

```sql
-- 商品表
ALTER TABLE `t_goods`
ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '商户ID' AFTER `goods_id`;

-- 商品分类表
ALTER TABLE `t_category`
ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '商户ID' AFTER `category_id`;

-- OA 企业表
ALTER TABLE `t_oa_enterprise`
ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '商户ID' AFTER `enterprise_id`;

-- OA 通知公告表
ALTER TABLE `t_oa_notice`
ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '商户ID' AFTER `notice_id`;

-- OA 银行表
ALTER TABLE `t_oa_bank`
ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '商户ID' AFTER `bank_id`;

-- OA 发票表
ALTER TABLE `t_oa_invoice`
ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '商户ID' AFTER `invoice_id`;
```

---

#### 不需要添加 tenant_id 的表

以下表是平台共享数据或通过关联表隔离，无需添加 tenant_id：

```sql
-- 平台共享数据
-- t_dict（字典表）
-- t_area（地区表）
-- t_config（平台配置表）

-- 关系表（通过主表隔离）
-- t_role_employee（通过 role_id 和 employee_id 间接隔离）
-- t_role_menu（通过 role_id 间接隔离）
-- t_role_data_scope（通过 role_id 间接隔离）

-- 日志表（通过业务字段关联商户）
-- t_login_log（通过 user_id 关联）
-- t_operate_log（通过 user_id 关联）
```

---

### 4.2 批量添加 tenant_id 脚本

**完整迁移脚本**: `migration-add-tenant-id.sql`

```sql
-- ========================================
-- SmartAdmin 多商户迁移脚本
-- 阶段 1: 为现有表添加 tenant_id 字段
-- ========================================

USE smart_admin;

-- 开始事务
START TRANSACTION;

-- ========================================
-- 核心业务表
-- ========================================

-- 员工表
ALTER TABLE `t_employee`
ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '商户ID' AFTER `employee_id`;

-- 部门表
ALTER TABLE `t_department`
ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '商户ID' AFTER `department_id`;

-- 角色表
ALTER TABLE `t_role`
ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '商户ID' AFTER `role_id`;

-- 职位表
ALTER TABLE `t_position`
ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '商户ID' AFTER `position_id`;

-- 菜单表
ALTER TABLE `t_menu`
ADD COLUMN `tenant_id` BIGINT DEFAULT NULL COMMENT '商户ID（NULL表示平台菜单）' AFTER `menu_id`;

-- ========================================
-- 业务表
-- ========================================

-- 商品表
ALTER TABLE `t_goods`
ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '商户ID' AFTER `goods_id`;

-- 商品分类表
ALTER TABLE `t_category`
ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '商户ID' AFTER `category_id`;

-- OA 企业表
ALTER TABLE `t_oa_enterprise`
ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '商户ID' AFTER `enterprise_id`;

-- OA 通知公告表
ALTER TABLE `t_oa_notice`
ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '商户ID' AFTER `notice_id`;

-- OA 银行表
ALTER TABLE `t_oa_bank`
ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '商户ID' AFTER `bank_id`;

-- OA 发票表
ALTER TABLE `t_oa_invoice`
ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '商户ID' AFTER `invoice_id`;

-- 提交事务
COMMIT;

-- 验证字段添加
SELECT
    TABLE_NAME,
    COLUMN_NAME,
    DATA_TYPE,
    COLUMN_DEFAULT,
    IS_NULLABLE
FROM
    INFORMATION_SCHEMA.COLUMNS
WHERE
    TABLE_SCHEMA = 'smart_admin'
    AND COLUMN_NAME = 'tenant_id'
ORDER BY
    TABLE_NAME;
```

**执行脚本**:

```bash
mysql -u root -p smart_admin < migration-add-tenant-id.sql
```

---

## 5. 数据迁移

### 5.1 迁移现有数据到默认商户

**迁移脚本**: `migration-data-to-default-tenant.sql`

```sql
-- ========================================
-- SmartAdmin 多商户迁移脚本
-- 阶段 2: 迁移现有数据到默认商户（tenant_id=1）
-- ========================================

USE smart_admin;

-- 开始事务
START TRANSACTION;

-- ========================================
-- 核心业务表数据迁移
-- ========================================

-- 员工表
UPDATE `t_employee` SET `tenant_id` = 1 WHERE `tenant_id` = 0;

-- 部门表
UPDATE `t_department` SET `tenant_id` = 1 WHERE `tenant_id` = 0;

-- 角色表
UPDATE `t_role` SET `tenant_id` = 1 WHERE `tenant_id` = 0;

-- 职位表
UPDATE `t_position` SET `tenant_id` = 1 WHERE `tenant_id` = 0;

-- 菜单表（保持 NULL，表示平台菜单）
-- UPDATE `t_menu` SET `tenant_id` = NULL WHERE `tenant_id` = 0;

-- ========================================
-- 业务表数据迁移
-- ========================================

-- 商品表
UPDATE `t_goods` SET `tenant_id` = 1 WHERE `tenant_id` = 0;

-- 商品分类表
UPDATE `t_category` SET `tenant_id` = 1 WHERE `tenant_id` = 0;

-- OA 企业表
UPDATE `t_oa_enterprise` SET `tenant_id` = 1 WHERE `tenant_id` = 0;

-- OA 通知公告表
UPDATE `t_oa_notice` SET `tenant_id` = 1 WHERE `tenant_id` = 0;

-- OA 银行表
UPDATE `t_oa_bank` SET `tenant_id` = 1 WHERE `tenant_id` = 0;

-- OA 发票表
UPDATE `t_oa_invoice` SET `tenant_id` = 1 WHERE `tenant_id` = 0;

-- 提交事务
COMMIT;

-- ========================================
-- 验证数据迁移结果
-- ========================================

-- 检查是否还有 tenant_id=0 的记录
SELECT
    't_employee' AS table_name,
    COUNT(*) AS count_tenant_0
FROM `t_employee`
WHERE `tenant_id` = 0

UNION ALL

SELECT
    't_department',
    COUNT(*)
FROM `t_department`
WHERE `tenant_id` = 0

UNION ALL

SELECT
    't_role',
    COUNT(*)
FROM `t_role`
WHERE `tenant_id` = 0

UNION ALL

SELECT
    't_goods',
    COUNT(*)
FROM `t_goods`
WHERE `tenant_id` = 0;

-- 应该全部返回 0，表示迁移完成
```

**执行脚本**:

```bash
mysql -u root -p smart_admin < migration-data-to-default-tenant.sql
```

---

### 5.2 验证数据完整性

```sql
-- ========================================
-- 数据完整性验证
-- ========================================

-- 1. 验证记录数是否一致（迁移前后）
SELECT
    't_employee' AS table_name,
    COUNT(*) AS total_count,
    SUM(CASE WHEN tenant_id = 1 THEN 1 ELSE 0 END) AS tenant_1_count,
    SUM(CASE WHEN tenant_id = 0 THEN 1 ELSE 0 END) AS tenant_0_count
FROM `t_employee`

UNION ALL

SELECT
    't_department',
    COUNT(*),
    SUM(CASE WHEN tenant_id = 1 THEN 1 ELSE 0 END),
    SUM(CASE WHEN tenant_id = 0 THEN 1 ELSE 0 END)
FROM `t_department`

UNION ALL

SELECT
    't_role',
    COUNT(*),
    SUM(CASE WHEN tenant_id = 1 THEN 1 ELSE 0 END),
    SUM(CASE WHEN tenant_id = 0 THEN 1 ELSE 0 END)
FROM `t_role`;

-- 2. 验证外键关系完整性
-- 验证员工的部门 ID 是否存在
SELECT
    e.employee_id,
    e.tenant_id AS employee_tenant_id,
    e.department_id,
    d.tenant_id AS department_tenant_id
FROM
    t_employee e
LEFT JOIN
    t_department d ON e.department_id = d.department_id
WHERE
    e.department_id IS NOT NULL
    AND (d.department_id IS NULL OR e.tenant_id != d.tenant_id);
-- 应该返回空结果

-- 3. 验证角色员工关系完整性
SELECT
    re.id,
    re.role_id,
    r.tenant_id AS role_tenant_id,
    re.employee_id,
    e.tenant_id AS employee_tenant_id
FROM
    t_role_employee re
INNER JOIN
    t_role r ON re.role_id = r.role_id
INNER JOIN
    t_employee e ON re.employee_id = e.employee_id
WHERE
    r.tenant_id != e.tenant_id;
-- 应该返回空结果
```

---

## 6. 索引优化

### 6.1 添加 tenant_id 索引

```sql
-- ========================================
-- SmartAdmin 多商户迁移脚本
-- 阶段 3: 为 tenant_id 字段添加索引
-- ========================================

USE smart_admin;

-- ========================================
-- 添加单列索引
-- ========================================

-- 员工表
ALTER TABLE `t_employee` ADD INDEX `idx_tenant_id` (`tenant_id`);

-- 部门表
ALTER TABLE `t_department` ADD INDEX `idx_tenant_id` (`tenant_id`);

-- 角色表
ALTER TABLE `t_role` ADD INDEX `idx_tenant_id` (`tenant_id`);

-- 职位表
ALTER TABLE `t_position` ADD INDEX `idx_tenant_id` (`tenant_id`);

-- 菜单表
ALTER TABLE `t_menu` ADD INDEX `idx_tenant_id` (`tenant_id`);

-- 商品表
ALTER TABLE `t_goods` ADD INDEX `idx_tenant_id` (`tenant_id`);

-- 商品分类表
ALTER TABLE `t_category` ADD INDEX `idx_tenant_id` (`tenant_id`);

-- OA 表
ALTER TABLE `t_oa_enterprise` ADD INDEX `idx_tenant_id` (`tenant_id`);
ALTER TABLE `t_oa_notice` ADD INDEX `idx_tenant_id` (`tenant_id`);
ALTER TABLE `t_oa_bank` ADD INDEX `idx_tenant_id` (`tenant_id`);
ALTER TABLE `t_oa_invoice` ADD INDEX `idx_tenant_id` (`tenant_id`);

-- ========================================
-- 添加复合索引（根据实际查询优化）
-- ========================================

-- 员工表：tenant_id + department_id
ALTER TABLE `t_employee` ADD INDEX `idx_tenant_dept` (`tenant_id`, `department_id`);

-- 员工表：tenant_id + disabled_flag
ALTER TABLE `t_employee` ADD INDEX `idx_tenant_disabled` (`tenant_id`, `disabled_flag`);

-- 角色表：tenant_id + role_code
ALTER TABLE `t_role` ADD INDEX `idx_tenant_code` (`tenant_id`, `role_code`);

-- 验证索引创建
SELECT
    TABLE_NAME,
    INDEX_NAME,
    COLUMN_NAME,
    INDEX_TYPE
FROM
    INFORMATION_SCHEMA.STATISTICS
WHERE
    TABLE_SCHEMA = 'smart_admin'
    AND COLUMN_NAME = 'tenant_id'
ORDER BY
    TABLE_NAME, INDEX_NAME;
```

**执行脚本**:

```bash
mysql -u root -p smart_admin < migration-add-indexes.sql
```

---

### 6.2 索引效果验证

```sql
-- 验证索引是否生效
EXPLAIN SELECT * FROM t_employee WHERE tenant_id = 1;

-- 应该看到 key='idx_tenant_id' 或 key='idx_tenant_dept'
```

---

## 7. 回滚方案

### 7.1 回滚脚本

**回滚脚本**: `rollback-migration.sql`

```sql
-- ========================================
-- SmartAdmin 多商户迁移回滚脚本
-- 警告: 执行前请确认需要回滚！
-- ========================================

USE smart_admin;

-- 开始事务
START TRANSACTION;

-- ========================================
-- 删除索引
-- ========================================

ALTER TABLE `t_employee` DROP INDEX IF EXISTS `idx_tenant_id`;
ALTER TABLE `t_employee` DROP INDEX IF EXISTS `idx_tenant_dept`;
ALTER TABLE `t_employee` DROP INDEX IF EXISTS `idx_tenant_disabled`;

ALTER TABLE `t_department` DROP INDEX IF EXISTS `idx_tenant_id`;
ALTER TABLE `t_role` DROP INDEX IF EXISTS `idx_tenant_id`;
ALTER TABLE `t_role` DROP INDEX IF EXISTS `idx_tenant_code`;
ALTER TABLE `t_position` DROP INDEX IF EXISTS `idx_tenant_id`;
ALTER TABLE `t_menu` DROP INDEX IF EXISTS `idx_tenant_id`;

ALTER TABLE `t_goods` DROP INDEX IF EXISTS `idx_tenant_id`;
ALTER TABLE `t_category` DROP INDEX IF EXISTS `idx_tenant_id`;

ALTER TABLE `t_oa_enterprise` DROP INDEX IF EXISTS `idx_tenant_id`;
ALTER TABLE `t_oa_notice` DROP INDEX IF EXISTS `idx_tenant_id`;
ALTER TABLE `t_oa_bank` DROP INDEX IF EXISTS `idx_tenant_id`;
ALTER TABLE `t_oa_invoice` DROP INDEX IF EXISTS `idx_tenant_id`;

-- ========================================
-- 删除 tenant_id 字段
-- ========================================

ALTER TABLE `t_employee` DROP COLUMN `tenant_id`;
ALTER TABLE `t_department` DROP COLUMN `tenant_id`;
ALTER TABLE `t_role` DROP COLUMN `tenant_id`;
ALTER TABLE `t_position` DROP COLUMN `tenant_id`;
ALTER TABLE `t_menu` DROP COLUMN `tenant_id`;

ALTER TABLE `t_goods` DROP COLUMN `tenant_id`;
ALTER TABLE `t_category` DROP COLUMN `tenant_id`;

ALTER TABLE `t_oa_enterprise` DROP COLUMN `tenant_id`;
ALTER TABLE `t_oa_notice` DROP COLUMN `tenant_id`;
ALTER TABLE `t_oa_bank` DROP COLUMN `tenant_id`;
ALTER TABLE `t_oa_invoice` DROP COLUMN `tenant_id`;

-- ========================================
-- 删除新增表
-- ========================================

DROP TABLE IF EXISTS `t_platform_audit_log`;
DROP TABLE IF EXISTS `t_platform_admin`;
DROP TABLE IF EXISTS `t_tenant_config`;
DROP TABLE IF EXISTS `t_tenant_package`;
DROP TABLE IF EXISTS `t_tenant`;

-- 提交事务
COMMIT;

-- 验证回滚完成
SHOW TABLES LIKE 't_tenant%';
SHOW TABLES LIKE 't_platform%';
-- 应该返回空结果
```

---

### 7.2 数据恢复

如果需要完全恢复到迁移前状态：

```bash
# 1. 停止应用服务
systemctl stop smart-admin

# 2. 删除当前数据库
mysql -u root -p -e "DROP DATABASE smart_admin;"

# 3. 重新创建数据库
mysql -u root -p -e "CREATE DATABASE smart_admin CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 4. 恢复备份
gunzip < /data/backup/smart_admin/smart_admin_backup_20260121_100000.sql.gz | mysql -u root -p smart_admin

# 5. 验证数据完整性
mysql -u root -p smart_admin -e "SELECT COUNT(*) FROM t_employee;"

# 6. 启动应用服务
systemctl start smart-admin
```

---

## 8. 迁移后验证

### 8.1 功能验证清单

#### 验证 1: 默认商户数据完整性

```sql
-- 验证默认商户的员工数量
SELECT COUNT(*) AS employee_count
FROM t_employee
WHERE tenant_id = 1;

-- 验证默认商户的部门数量
SELECT COUNT(*) AS department_count
FROM t_department
WHERE tenant_id = 1;

-- 验证默认商户的角色数量
SELECT COUNT(*) AS role_count
FROM t_role
WHERE tenant_id = 1;
```

---

#### 验证 2: 登录功能

```bash
# 测试商户用户登录
curl -X POST http://localhost:1024/login \
  -H "Content-Type: application/json" \
  -d '{
    "tenantCode": "default",
    "loginName": "admin",
    "loginPwd": "123456",
    "captchaCode": "1234",
    "captchaUuid": "test"
  }'

# 应该返回登录成功，包含 tenantId=1
```

---

#### 验证 3: 数据隔离

```bash
# 1. 创建测试商户
curl -X POST http://localhost:1024/tenant/add \
  -H "x-access-token: {平台管理员token}" \
  -H "Content-Type: application/json" \
  -d '{
    "tenantCode": "test_tenant",
    "tenantName": "测试商户",
    "tenantType": 1,
    "contactName": "测试",
    "contactPhone": "13800138000",
    "packageId": 1,
    "expireTime": "2027-01-01 00:00:00",
    "adminLoginName": "test_admin",
    "adminPassword": "Admin@123",
    "adminActualName": "测试管理员"
  }'

# 2. 测试商户管理员登录
curl -X POST http://localhost:1024/login \
  -H "Content-Type: application/json" \
  -d '{
    "tenantCode": "test_tenant",
    "loginName": "test_admin",
    "loginPwd": "Admin@123",
    "captchaCode": "1234",
    "captchaUuid": "test"
  }'

# 3. 查询员工列表（应该只看到测试商户的管理员）
curl -X POST http://localhost:1024/employee/query \
  -H "x-access-token: {测试商户token}" \
  -H "Content-Type: application/json" \
  -d '{
    "pageNum": 1,
    "pageSize": 10
  }'
```

---

#### 验证 4: 性能测试

```bash
# 使用 JMeter 或 ab 进行压力测试
ab -n 1000 -c 10 http://localhost:1024/employee/query

# 验证响应时间是否在可接受范围内（< 500ms）
```

---

## 9. 性能优化

### 9.1 慢查询优化

#### 启用慢查询日志

```sql
-- 启用慢查询日志
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 1;  -- 超过 1 秒的查询记录为慢查询
SET GLOBAL slow_query_log_file = '/var/log/mysql/slow-query.log';
```

#### 分析慢查询

```bash
# 使用 pt-query-digest 分析慢查询日志
pt-query-digest /var/log/mysql/slow-query.log
```

---

### 9.2 分区表优化（大数据量场景）

如果单表数据量超过 1000 万，考虑使用分区表：

```sql
-- 示例：按 tenant_id 进行 HASH 分区
ALTER TABLE t_employee
PARTITION BY HASH(tenant_id)
PARTITIONS 10;
```

---

### 9.3 读写分离

对于大规模多租户系统，建议配置主从复制：

```
Master (写)
   ↓
Slave 1 (读)
Slave 2 (读)
```

---

## 🎉 迁移完成

### 最终检查清单

- ✅ **数据备份已完成**
- ✅ **新增表已创建**（t_tenant、t_tenant_config、t_tenant_package、t_platform_admin）
- ✅ **现有表已添加 tenant_id 字段**
- ✅ **现有数据已迁移到默认商户（tenant_id=1）**
- ✅ **索引已创建**（idx_tenant_id）
- ✅ **数据完整性验证通过**
- ✅ **功能验证通过**（登录、查询、数据隔离）
- ✅ **性能验证通过**（响应时间 < 500ms）
- ✅ **回滚方案已准备**

恭喜！SmartAdmin 多商户迁移已成功完成！🎊

---

**文档结束**

---

## 附录

### A. 完整迁移脚本合集

所有迁移脚本已整理在 `/docs/migration-scripts/` 目录下：

```
/docs/migration-scripts/
├── 01-create-tenant-tables.sql          # 创建租户相关表
├── 02-add-tenant-id-columns.sql         # 为现有表添加 tenant_id
├── 03-migrate-data-to-default.sql       # 迁移数据到默认商户
├── 04-add-tenant-id-indexes.sql         # 添加 tenant_id 索引
├── 05-verify-migration.sql              # 验证迁移结果
└── rollback-migration.sql               # 回滚脚本
```

### B. 迁移时间线示例

```
T+0h: 停止应用服务
T+0.5h: 数据备份完成
T+1h: 新增表创建完成
T+1.5h: tenant_id 字段添加完成
T+2h: 数据迁移完成
T+2.5h: 索引创建完成
T+3h: 验证通过，启动应用服务
T+3.5h: 功能回归测试
T+4h: 迁移完成，恢复正常服务
```

---

**联系方式**: 如有迁移问题请联系 DBA 负责人
