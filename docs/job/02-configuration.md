# Snail-Job 配置参考手册

> 完整的 Snail-Job 配置说明，包含 Docker、应用、多环境配置

## 📋 目录

- [Docker Compose 配置](#docker-compose-配置)
- [应用配置详解](#应用配置详解)
- [多环境配置策略](#多环境配置策略)
- [安全配置](#安全配置)
- [性能调优配置](#性能调优配置)

---

## Docker Compose 配置

### 环境变量

**文件位置**: `docker/.env`

| 变量名 | 默认值 | 说明 | 环境 |
|--------|--------|------|------|
| `SNAIL_JOB_SERVER_PORT` | 1788 | RPC 服务端口 | 所有 |
| `SNAIL_JOB_ADMIN_PORT` | 8082 | Web 控制台端口 | dev/test |
| `SNAIL_JOB_ADMIN_USER` | admin | 管理员账号 | 所有 |
| `SNAIL_JOB_ADMIN_PASSWORD` | SmartAdmin@2024 | 管理员密码 | dev/test |
| `SNAIL_JOB_NAMESPACE` | dev | 默认命名空间 | 生产需配置 |
| `SNAIL_JOB_TOKEN` | - | 客户端认证 Token | 生产必须 |

### docker-compose.yml 配置

#### Snail-Job Server 服务

```yaml
snail-job-server:
  image: aizuda/snail-job-server:1.8.1
  container_name: sa21-snail-job-server
  restart: unless-stopped
  environment:
    # === 数据库配置 ===
    SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/${POSTGRES_DB:-smart_admin}
    SPRING_DATASOURCE_USERNAME: ${POSTGRES_USER:-smartadmin}
    SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD:-SmartAdmin@2024}
    SPRING_DATASOURCE_DRIVER_CLASS_NAME: org.postgresql.Driver

    # === Redis 配置 ===
    SPRING_DATA_REDIS_HOST: redis
    SPRING_DATA_REDIS_PORT: 6379
    SPRING_DATA_REDIS_DATABASE: 2  # 使用独立的数据库编号

    # === 服务器配置 ===
    SERVER_PORT: 8080               # Web 控制台端口（内部）
    SNAIL_JOB_PORT: 1788            # RPC 通信端口

    # === 管理员账号 ===
    SNAIL_JOB_ADMIN_USERNAME: ${SNAIL_JOB_ADMIN_USER:-admin}
    SNAIL_JOB_ADMIN_PASSWORD: ${SNAIL_JOB_ADMIN_PASSWORD:-SmartAdmin@2024}

  ports:
    - "${SNAIL_JOB_SERVER_PORT:-1788}:1788"  # RPC 端口映射
    - "${SNAIL_JOB_ADMIN_PORT:-8082}:8080"   # 控制台端口映射

  volumes:
    - snail_job_logs:/opt/snail-job/logs     # 日志持久化

  depends_on:
    postgres:
      condition: service_healthy
    redis:
      condition: service_healthy

  healthcheck:
    test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
    interval: 30s
    timeout: 10s
    retries: 3
    start_period: 60s

  networks:
    - sa21-network
```

#### 数据库初始化服务

```yaml
snail-job-init:
  image: postgres:16-alpine
  container_name: sa21-snail-job-init
  depends_on:
    postgres:
      condition: service_healthy
  entrypoint: >
    /bin/sh -c "
    PGPASSWORD=$${POSTGRES_PASSWORD} psql -h postgres -U $${POSTGRES_USER} -d $${POSTGRES_DB} -f /docker-entrypoint-initdb.d/snail-job-schema.sql;
    echo 'Snail-Job initialized! Admin Console: http://localhost:8082 (admin/SmartAdmin@2024)';
    "
  environment:
    POSTGRES_USER: ${POSTGRES_USER:-smartadmin}
    POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-SmartAdmin@2024}
    POSTGRES_DB: ${POSTGRES_DB:-smart_admin}
  volumes:
    - ./snail-job/init-schema.sql:/docker-entrypoint-initdb.d/snail-job-schema.sql:ro
  networks:
    - sa21-network
```

### Volume 配置

```yaml
volumes:
  snail_job_logs:
    name: sa21-snail-job-logs
    driver: local
```

**日志目录**:
- 容器内: `/opt/snail-job/logs`
- 宿主机: Docker volume `sa21-snail-job-logs`

**查看日志**:
```bash
docker logs -f sa21-snail-job-server
```

---

## 应用配置详解

### 配置文件位置

SmartAdmin 使用多环境配置文件：

```
sa-admin/src/main/resources/
├── dev/sa-base.yaml        # 开发环境
├── test/sa-base.yaml       # 测试环境
├── pre/sa-base.yaml        # 预发布环境（可选）
└── prod/sa-base.yaml       # 生产环境
```

### 完整配置项

#### 开发环境 (dev/sa-base.yaml)

```yaml
# ===========================================
# Snail-Job Task Scheduler Configuration
# ===========================================
snail-job:
  # 是否启用 Snail-Job（默认: false）
  enabled: true

  # 服务器连接配置
  server:
    # 服务器地址（IP 或域名）
    host: localhost

    # RPC 通信端口（默认: 1788）
    # 注意：这是 RPC 端口，不是 Web 控制台端口（8082）
    port: 1788

    # 命名空间 unique_id（用于环境隔离）
    # 必须与控制台中创建的命名空间一致
    namespace: dev

    # 分组名称（对应控制台中的执行器分组）
    group-name: smart-admin

    # 认证 Token（生产环境必须配置）
    # 开发环境可留空
    token: ""

  # 客户端配置
  client:
    # 客户端绑定地址（默认自动检测）
    # 必须是服务器能访问到的地址
    host: localhost

    # 客户端 RPC 端口（默认: 1789）
    port: 1789

  # 日志配置
  logging:
    # Logback 配置文件路径
    config: classpath:logback-spring.xml

  # 执行器线程池配置（可选）
  executor:
    # 核心线程数（默认: 10）
    core-pool-size: 10

    # 最大线程数（默认: 50）
    max-pool-size: 50

    # 队列容量（默认: 200）
    queue-capacity: 200

    # 线程空闲时间（秒，默认: 60）
    keep-alive-seconds: 60

  # 重试配置（可选）
  retry:
    # 是否启用分布式重试（默认: true）
    enabled: true

    # 最大重试次数（默认: 3）
    max-attempts: 3

    # 初始重试间隔（毫秒，默认: 1000）
    initial-interval: 1000

    # 最大重试间隔（毫秒，默认: 60000）
    max-interval: 60000

    # 重试间隔倍数（默认: 2.0，指数退避）
    multiplier: 2.0
```

#### 测试环境 (test/sa-base.yaml)

```yaml
snail-job:
  enabled: true
  server:
    host: snail-job-test.internal  # 内网地址
    port: 1788
    namespace: test
    group-name: smart-admin
    token: ""
  client:
    port: 1789
```

#### 生产环境 (prod/sa-base.yaml)

```yaml
snail-job:
  enabled: true
  server:
    # 使用环境变量（K8s ConfigMap 或 Docker Compose .env）
    host: ${SNAIL_JOB_SERVER_HOST:snail-job-server}
    port: ${SNAIL_JOB_SERVER_PORT:1788}
    namespace: ${SNAIL_JOB_NAMESPACE:prod}
    group-name: smart-admin

    # 生产环境必须配置 Token
    token: ${SNAIL_JOB_TOKEN}

  client:
    # 生产环境必须明确指定客户端地址
    host: ${SERVER_IP}
    port: 1789

  # 生产环境线程池调优
  executor:
    core-pool-size: 20
    max-pool-size: 100
    queue-capacity: 500
    keep-alive-seconds: 120

  # 生产环境重试策略
  retry:
    enabled: true
    max-attempts: 5
    initial-interval: 2000
    max-interval: 300000  # 5 分钟
    multiplier: 2.0
```

---

## 多环境配置策略

### 环境切换

#### Gradle 构建

```bash
# 开发环境（默认）
./gradlew :sa-admin:bootRun

# 测试环境
./gradlew :sa-admin:bootRun -Penv=test

# 生产环境
./gradlew :sa-admin:build -Penv=prod
```

#### IDE 启动

**IntelliJ IDEA**:
1. 编辑运行配置
2. VM Options: `-Dspring.profiles.active=dev`
3. 或设置环境变量: `ENV=dev`

### 环境隔离策略

| 环境 | 命名空间 | Token | 数据库 | Redis DB |
|------|----------|-------|--------|----------|
| 开发 (dev) | dev | 无 | smart_admin | 2 |
| 测试 (test) | test | 可选 | smart_admin_test | 3 |
| 预发布 (pre) | pre | 必须 | smart_admin_pre | 4 |
| 生产 (prod) | prod | 必须 | smart_admin_prod | 5 |

**最佳实践**:
- 每个环境使用独立的命名空间
- 生产环境强制 Token 认证
- 使用独立的 Redis 数据库编号
- 不同环境使用不同的数据库

---

## 安全配置

### Token 生成与管理

#### 生成强 Token

```bash
# 方法 1: OpenSSL
openssl rand -hex 32

# 方法 2: Java
java -c "System.out.println(java.util.UUID.randomUUID().toString().replace(\"-\", \"\"));"

# 输出示例: 7f3c9e8b5d2a1f4e6c8d9a0b3e5f7c1d
```

#### Token 配置

**控制台配置**:
1. 登录 Snail-Job 控制台
2. 导航到 **系统管理** → **分组管理**
3. 编辑 `smart-admin` 分组
4. 设置 **Token**: `7f3c9e8b5d2a1f4e6c8d9a0b3e5f7c1d`
5. 保存

**应用配置**:
```yaml
# prod/sa-base.yaml
snail-job:
  server:
    token: ${SNAIL_JOB_TOKEN}  # 从环境变量读取
```

**环境变量**:
```bash
# .env 文件
SNAIL_JOB_TOKEN=7f3c9e8b5d2a1f4e6c8d9a0b3e5f7c1d

# 或 K8s Secret
kubectl create secret generic snail-job-token \
  --from-literal=token=7f3c9e8b5d2a1f4e6c8d9a0b3e5f7c1d
```

### 网络安全

#### 防火墙配置

**开发环境**:
```bash
# 允许本地访问
# RPC 端口: 1788
# Web 控制台: 8082
```

**生产环境**:
```bash
# 仅允许应用服务器访问 RPC 端口
iptables -A INPUT -p tcp --dport 1788 -s 10.0.0.0/24 -j ACCEPT
iptables -A INPUT -p tcp --dport 1788 -j DROP

# Web 控制台仅内网访问
iptables -A INPUT -p tcp --dport 8082 -s 10.0.0.0/24 -j ACCEPT
iptables -A INPUT -p tcp --dport 8082 -j DROP
```

#### HTTPS 配置

**Nginx 反向代理**:

```nginx
server {
    listen 443 ssl http2;
    server_name snail-job.example.com;

    ssl_certificate /etc/ssl/certs/snail-job.crt;
    ssl_certificate_key /etc/ssl/private/snail-job.key;

    location / {
        proxy_pass http://localhost:8082;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### 权限最小化

#### 数据库权限

```sql
-- 创建专用数据库用户
CREATE USER snail_job_user WITH PASSWORD 'strong_password_here';

-- 仅授予必要权限
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO snail_job_user;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO snail_job_user;

-- 撤销创建表权限（生产环境）
REVOKE CREATE ON SCHEMA public FROM snail_job_user;
```

#### Redis 权限

```bash
# redis.conf
requirepass strong_redis_password

# 使用独立 Redis 数据库
select 2
```

---

## 性能调优配置

### 线程池调优

根据任务特性调整线程池配置：

#### CPU 密集型任务

```yaml
snail-job:
  executor:
    core-pool-size: 8    # CPU 核心数
    max-pool-size: 16    # CPU 核心数 * 2
    queue-capacity: 100
```

#### IO 密集型任务

```yaml
snail-job:
  executor:
    core-pool-size: 20   # CPU 核心数 * 2
    max-pool-size: 100   # CPU 核心数 * 10
    queue-capacity: 500
```

#### 混合型任务

```yaml
snail-job:
  executor:
    core-pool-size: 10
    max-pool-size: 50
    queue-capacity: 200
```

### 重试策略调优

#### 快速失败场景

```yaml
snail-job:
  retry:
    max-attempts: 2
    initial-interval: 500
    max-interval: 5000
    multiplier: 2.0
```

#### 可靠重试场景

```yaml
snail-job:
  retry:
    max-attempts: 10
    initial-interval: 5000
    max-interval: 600000  # 10 分钟
    multiplier: 2.0
```

### 数据库连接池

```yaml
spring:
  datasource:
    hikari:
      # Snail-Job Server 推荐配置
      minimum-idle: 10
      maximum-pool-size: 50
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

---

## 配置验证

### 启动验证清单

✅ **配置文件验证**
```bash
# 检查 YAML 语法
yamllint sa-admin/src/main/resources/dev/sa-base.yaml
```

✅ **端口可用性**
```bash
# 检查端口是否被占用
netstat -an | grep 1788
netstat -an | grep 8082
```

✅ **网络连通性**
```bash
# 测试 RPC 端口
telnet localhost 1788

# 测试 Web 控制台
curl http://localhost:8082/actuator/health
```

✅ **Token 配置**
```bash
# 检查环境变量
echo $SNAIL_JOB_TOKEN

# 检查配置加载
grep -r "SNAIL_JOB_TOKEN" /etc/environment
```

---

## 常见配置问题

### Q1: 配置不生效

**现象**: 修改配置后应用行为未改变

**排查**:
1. 确认修改了正确的环境配置文件（dev/test/prod）
2. 重启应用加载新配置
3. 检查日志确认配置加载：`grep "snail-job" logs/smart-admin.log`

### Q2: Token 认证失败

**现象**: `Authentication failed: Invalid token`

**解决**:
1. 检查应用配置中的 token 与控制台分组配置是否一致
2. 确认 token 无多余空格或换行符
3. 生产环境确认环境变量已正确设置

### Q3: 端口冲突

**现象**: `Address already in use: 1788`

**解决**:
```bash
# 查找占用端口的进程
lsof -i :1788

# 修改配置使用其他端口
snail-job:
  server:
    port: 1789  # 改为其他端口
```

---

## 下一步

- [API 使用示例](./03-api-usage.md) - 代码实战
- [监控运维指南](./06-monitoring.md) - 生产环境监控
- [故障排查手册](./04-troubleshooting.md) - 问题诊断

---

**配置参考完成！** 如有问题，请参阅 [故障排查手册](./04-troubleshooting.md)
