# SmartAdmin 微服務遷移實施指南
## 方案 A: 保守策略實施手冊

**文檔類型**: 技術實施指南
**版本**: 1.0.0
**創建日期**: 2026-02-02
**依賴文檔**: [架構分析報告](./ruoyi-cloud-plus-migration-analysis.md)

---

## 📋 實施總覽

### 實施時間表

```
總週期: 8 weeks
├── Phase 1: 基礎設施準備 (Week 1-2)
├── Phase 2: Job Service 拆分 (Week 3-4)
├── Phase 3: Resource Service 拆分 (Week 5-6)
└── Phase 4: 測試與驗證 (Week 7-8)
```

### 關鍵里程碑

| 里程碑 | 完成標準 | 時間點 |
|--------|---------|--------|
| **M1: 基礎設施就緒** | Gateway + Nacos 運行穩定 | Week 2 |
| **M2: 首個微服務上線** | Job Service 生產就緒 | Week 4 |
| **M3: 第二個微服務上線** | Resource Service 生產就緒 | Week 6 |
| **M4: 遷移完成** | 所有測試通過，文檔完整 | Week 8 |

---

## Phase 1: 基礎設施準備 (Week 1-2)

### 目標

搭建微服務基礎設施，為後續服務拆分提供運行環境。

### 任務清單

#### Week 1: 中間件部署

##### 任務 1.1: 部署 Nacos 2.5.3

**目的**: 提供服務註冊與配置中心

**步驟**:

1. **Docker 方式部署** (推薦)
   ```bash
   # 1. 創建 Nacos 數據目錄
   mkdir -p /opt/nacos/data
   mkdir -p /opt/nacos/logs

   # 2. 啟動 Nacos 容器 (單機模式)
   docker run -d \
     --name nacos-standalone \
     -p 8848:8848 \
     -p 9848:9848 \
     -p 9849:9849 \
     -e MODE=standalone \
     -e PREFER_HOST_MODE=hostname \
     -v /opt/nacos/data:/home/nacos/data \
     -v /opt/nacos/logs:/home/nacos/logs \
     --restart=always \
     nacos/nacos-server:v2.5.3

   # 3. 驗證啟動
   docker logs -f nacos-standalone

   # 4. 訪問控制台
   # http://localhost:8848/nacos
   # 默認賬號: nacos / nacos
   ```

2. **生產環境集群部署** (可選)
   ```yaml
   # docker-compose.yml
   version: '3.8'
   services:
     nacos1:
       image: nacos/nacos-server:v2.5.3
       environment:
         - PREFER_HOST_MODE=hostname
         - MODE=cluster
         - NACOS_SERVERS=nacos1:8848 nacos2:8848 nacos3:8848
         - MYSQL_SERVICE_HOST=mysql
         - MYSQL_SERVICE_DB_NAME=nacos_config
         - MYSQL_SERVICE_USER=root
         - MYSQL_SERVICE_PASSWORD=root
       ports:
         - "8848:8848"
         - "9848:9848"
     nacos2:
       # 配置同 nacos1
     nacos3:
       # 配置同 nacos1
     mysql:
       image: mysql:8.0.42
       environment:
         MYSQL_ROOT_PASSWORD: root
         MYSQL_DATABASE: nacos_config
   ```

3. **驗收標準**
   - ✅ Nacos 控制台可訪問 (http://localhost:8848/nacos)
   - ✅ 可以創建命名空間 (dev/test/prod)
   - ✅ 可以創建配置文件
   - ✅ 健康檢查通過: `curl http://localhost:8848/nacos/v1/console/health/readiness`

**可能遇到的問題**:

| 問題 | 原因 | 解決方案 |
|------|------|----------|
| 端口衝突 | 8848 被佔用 | `netstat -ano | findstr 8848` 查找佔用進程 |
| 啟動失敗 | 內存不足 | 調整 JVM 參數: `-e JVM_XMS=512m -e JVM_XMX=512m` |
| 無法訪問控制台 | 防火牆阻擋 | Windows: `netsh advfirewall firewall add rule name="Nacos" dir=in action=allow protocol=TCP localport=8848` |

---

##### 任務 1.2: SmartAdmin 連接 Nacos

**目的**: 將現有單體應用註冊到 Nacos，驗證服務發現功能

**步驟**:

1. **添加依賴** (`smart-admin-api-java21-springboot3/sa-admin/build.gradle.kts`)
   ```kotlin
   dependencies {
       // Spring Cloud Alibaba Nacos Discovery
       implementation("com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-discovery:2023.0.3.3")

       // Spring Cloud Alibaba Nacos Config
       implementation("com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-config:2023.0.3.3")

       // Spring Cloud LoadBalancer (替代 Ribbon)
       implementation("org.springframework.cloud:spring-cloud-starter-loadbalancer:4.1.5")
   }
   ```

2. **配置文件修改** (`sa-admin/src/main/resources/dev/application.yaml`)
   ```yaml
   spring:
     application:
       name: smartadmin-monolith  # 服務名稱
     cloud:
       nacos:
         discovery:
           server-addr: localhost:8848  # Nacos 地址
           namespace: dev                # 命名空間 (環境隔離)
           group: DEFAULT_GROUP
           metadata:
             version: 4.0.0
             region: cn-shanghai
             preserved.register.source: SPRING_CLOUD  # 標識來源
         config:
           server-addr: localhost:8848
           file-extension: yaml
           namespace: dev
           group: DEFAULT_GROUP
           # 共享配置 (所有服務共用)
           shared-configs:
             - data-id: common-redis.yaml    # Redis 配置
               group: DEFAULT_GROUP
               refresh: true                 # 動態刷新
             - data-id: common-mybatis.yaml  # MyBatis 配置
               group: DEFAULT_GROUP
               refresh: true
             - data-id: common-sa-token.yaml # Sa-Token 配置
               group: DEFAULT_GROUP
               refresh: true
   ```

3. **啟動類添加註解** (`sa-admin/src/main/java/net/lab1024/sa/admin/AdminApplication.java`)
   ```java
   @SpringBootApplication
   @EnableDiscoveryClient  // 🆕 啟用服務發現
   public class AdminApplication {
       public static void main(String[] args) {
           SpringApplication.run(AdminApplication.class, args);
       }
   }
   ```

4. **創建 Nacos 共享配置**

   登錄 Nacos 控制台 (http://localhost:8848/nacos)，創建以下配置：

   **配置 1: common-redis.yaml**
   ```yaml
   # Data ID: common-redis.yaml
   # Group: DEFAULT_GROUP
   # 命名空間: dev
   spring:
     data:
       redis:
         host: localhost
         port: 6379
         password:  # 生產環境填寫密碼
         database: 0
         lettuce:
           pool:
             max-active: 8
             max-idle: 8
             min-idle: 0
             max-wait: -1ms
   ```

   **配置 2: common-mybatis.yaml**
   ```yaml
   # Data ID: common-mybatis.yaml
   mybatis-plus:
     configuration:
       map-underscore-to-camel-case: true
       log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl
     global-config:
       db-config:
         id-type: auto
         logic-delete-field: deletedFlag
         logic-delete-value: true
         logic-not-delete-value: false
   ```

5. **驗收標準**
   - ✅ SmartAdmin 啟動成功
   - ✅ Nacos 控制台顯示 `smartadmin-monolith` 服務
   - ✅ 服務狀態為 "健康"
   - ✅ 配置動態刷新測試通過

**測試配置動態刷新**:
```java
@RestController
@RefreshScope  // 🆕 配置刷新註解
public class ConfigTestController {

    @Value("${test.config.value:default}")
    private String configValue;

    @GetMapping("/test/config")
    public String getConfig() {
        return "Current config: " + configValue;
    }
}

// 測試步驟:
// 1. 訪問 http://localhost:1024/test/config (返回 "default")
// 2. 在 Nacos 控制台修改配置，添加 test.config.value: updated
// 3. 等待 10 秒 (Nacos 默認刷新間隔)
// 4. 再次訪問，應返回 "updated" (無需重啟)
```

---

#### Week 2: API 網關搭建

##### 任務 2.1: 創建 Gateway 模塊

**目的**: 搭建統一 API 入口，實現路由、認證、限流

**步驟**:

1. **創建模塊結構**
   ```bash
   mkdir -p ruoyi-gateway/src/main/java/net/lab1024/sa/gateway
   mkdir -p ruoyi-gateway/src/main/resources
   ```

2. **構建配置** (`ruoyi-gateway/build.gradle.kts`)
   ```kotlin
   dependencies {
       // Spring Cloud Gateway
       implementation("org.springframework.cloud:spring-cloud-starter-gateway")

       // Nacos 服務發現
       implementation("com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-discovery:2023.0.3.3")

       // Sa-Token Reactor (Gateway 專用)
       implementation("cn.dev33:sa-token-reactor-spring-boot3-starter:1.44.0")

       // Redis Reactive (限流用)
       implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")

       // Sentinel 流控
       implementation("com.alibaba.cloud:spring-cloud-starter-alibaba-sentinel:2023.0.3.3")

       // LoadBalancer
       implementation("org.springframework.cloud:spring-cloud-starter-loadbalancer")
   }
   ```

3. **Gateway 配置** (`ruoyi-gateway/src/main/resources/application.yml`)
   ```yaml
   server:
     port: 8080

   spring:
     application:
       name: ruoyi-gateway
     cloud:
       nacos:
         discovery:
           server-addr: localhost:8848
           namespace: dev
       gateway:
         routes:
           # SmartAdmin 單體路由
           - id: smartadmin-monolith
             uri: lb://smartadmin-monolith  # Nacos 負載均衡
             predicates:
               - Path=/api/**
             filters:
               - StripPrefix=1  # 移除 /api 前綴
               - name: RequestRateLimiter  # 限流
                 args:
                   redis-rate-limiter.replenishRate: 10   # 令牌桶: 每秒補充 10 個
                   redis-rate-limiter.burstCapacity: 20   # 最大突發 20 個
                   redis-rate-limiter.requestedTokens: 1  # 每次請求消耗 1 個

         # 全局過濾器
         default-filters:
           - name: Retry  # 重試機制
             args:
               retries: 3
               statuses: BAD_GATEWAY,GATEWAY_TIMEOUT
               methods: GET
               backoff:
                 firstBackoff: 10ms
                 maxBackoff: 50ms
                 factor: 2
   ```

4. **啟動類** (`ruoyi-gateway/src/main/java/net/lab1024/sa/gateway/GatewayApplication.java`)
   ```java
   @SpringBootApplication
   @EnableDiscoveryClient
   public class GatewayApplication {
       public static void main(String[] args) {
           SpringApplication.run(GatewayApplication.java, args);
       }
   }
   ```

5. **Sa-Token 認證過濾器** (`gateway/filter/AuthGlobalFilter.java`)
   ```java
   @Component
   public class AuthGlobalFilter implements GlobalFilter, Ordered {

       private static final List<String> WHITELIST = Arrays.asList(
           "/login",
           "/captcha",
           "/doc.html",
           "/swagger-resources/**",
           "/v3/api-docs/**"
       );

       @Override
       public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
           String path = exchange.getRequest().getURI().getPath();

           // 白名單放行
           if (isWhitelist(path)) {
               return chain.filter(exchange);
           }

           // Token 驗證
           String token = exchange.getRequest().getHeaders().getFirst(SaTokenConsts.HEADER_NAME);
           if (StringUtils.isBlank(token)) {
               return unauthorized(exchange, "缺少認證 Token");
           }

           try {
               // Sa-Token 驗證
               StpUtil.checkLogin(token);

               // 將用戶信息傳遞到下游服務
               ServerHttpRequest request = exchange.getRequest().mutate()
                   .header("X-User-Id", StpUtil.getLoginIdAsString())
                   .header("X-User-Name", StpUtil.getExtra("userName").toString())
                   .build();

               return chain.filter(exchange.mutate().request(request).build());
           } catch (NotLoginException e) {
               return unauthorized(exchange, "Token 無效或已過期");
           }
       }

       private boolean isWhitelist(String path) {
           return WHITELIST.stream().anyMatch(pattern ->
               new AntPathMatcher().match(pattern, path)
           );
       }

       private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
           exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
           exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

           String body = "{\"code\": 401, \"msg\": \"" + message + "\"}";
           DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body.getBytes());

           return exchange.getResponse().writeWith(Mono.just(buffer));
       }

       @Override
       public int getOrder() {
           return -100;  // 優先級最高
       }
   }
   ```

6. **驗收標準**
   - ✅ Gateway 啟動成功 (8080)
   - ✅ 通過 Gateway 訪問 SmartAdmin: `curl http://localhost:8080/api/employee/query`
   - ✅ 白名單路徑無需 Token: `curl http://localhost:8080/api/login`
   - ✅ 非白名單路徑需要 Token: 返回 401 Unauthorized
   - ✅ 限流測試: 使用 JMeter 發送 100 QPS，觸發限流

**JMeter 限流測試腳本**:
```xml
<!-- HTTP Request -->
<HTTPSamplerProxy>
  <stringProp name="HTTPSampler.domain">localhost</stringProp>
  <stringProp name="HTTPSampler.port">8080</stringProp>
  <stringProp name="HTTPSampler.path">/api/test</stringProp>
</HTTPSamplerProxy>

<!-- Thread Group -->
<ThreadGroup>
  <stringProp name="ThreadGroup.num_threads">100</stringProp>
  <stringProp name="ThreadGroup.ramp_time">1</stringProp>
  <stringProp name="ThreadGroup.loops">1</stringProp>
</ThreadGroup>

<!-- 預期結果: 約 80% 請求返回 429 Too Many Requests -->
```

---

### Phase 1 驗收檢查清單

完成 Week 1-2 後，進行以下全面檢查：

- [ ] **Nacos 部署**
  - [ ] Nacos 單機模式運行穩定
  - [ ] 控制台可訪問 (http://localhost:8848/nacos)
  - [ ] 創建 dev/test/prod 命名空間
  - [ ] 上傳共享配置 (common-redis/mybatis/sa-token.yaml)

- [ ] **SmartAdmin 服務註冊**
  - [ ] 啟動後在 Nacos 控制台顯示
  - [ ] 服務狀態為 "健康" (綠色)
  - [ ] 配置動態刷新測試通過

- [ ] **Gateway 部署**
  - [ ] Gateway 啟動成功 (8080)
  - [ ] 路由轉發正常 (Gateway → SmartAdmin)
  - [ ] Sa-Token 認證測試通過
  - [ ] 限流功能驗證通過 (JMeter)

- [ ] **文檔更新**
  - [ ] 更新部署文檔 (Nacos/Gateway 配置)
  - [ ] 更新架構圖 (新增 Nacos/Gateway)
  - [ ] 更新開發環境搭建指南

**如果驗收失敗，停止 Phase 2，回滾到 Phase 1 開始狀態。**

---

## Phase 2: Job Service 拆分 (Week 3-4)

### 目標

拆分首個微服務 (Job Service)，驗證微服務架構可行性。

### 為什麼選擇 Job Service 作為首個拆分目標？

**深度推理分析**:

1. **依賴關係分析**
   ```
   Job Service 依賴分析:
   ├── 直接依賴: 0 個業務模塊
   ├── 被依賴: 0 個業務模塊 (完全獨立)
   └── 技術依賴: Redis, MyBatis, Snail-Job

   對比:
   System Service 依賴分析:
   ├── 被依賴: Business, Support 等 12 個模塊 (高耦合)
   └── 拆分風險: 🔴 極高 (需要定義大量 RPC 接口)
   ```

2. **業務獨立性**
   - ✅ Job Service 是純技術基礎設施，無業務邏輯
   - ✅ 定時任務執行不依賴實時業務數據
   - ✅ 失敗影響範圍小 (不影響主業務流程)

3. **回滾成本**
   - ✅ 拆分失敗可快速回滾 (不影響核心業務)
   - ✅ 單獨部署/回滾，不需要停機

**結論**: Job Service 是**最優首個拆分目標**，風險最低，驗證價值最高。

---

### 任務清單

#### Week 3: 代碼遷移與配置

##### 任務 3.1: 創建 Job Service 模塊

1. **創建項目結構**
   ```bash
   mkdir -p ruoyi-job/src/main/java/net/lab1024/sa/job
   mkdir -p ruoyi-job/src/main/resources
   mkdir -p ruoyi-job/src/test/java
   ```

2. **構建配置** (`ruoyi-job/build.gradle.kts`)
   ```kotlin
   dependencies {
       // 🆕 SmartAdmin 公共模塊 (將來需要創建)
       implementation(project(":ruoyi-common:ruoyi-common-core"))
       implementation(project(":ruoyi-common:ruoyi-common-web"))
       implementation(project(":ruoyi-common:ruoyi-common-mybatis"))
       implementation(project(":ruoyi-common:ruoyi-common-redis"))

       // Snail-Job (定時任務引擎)
       implementation("com.aizuda:snail-job-client-starter:2.1.0")

       // Nacos 服務發現
       implementation("com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-discovery:2023.0.3.3")
   }
   ```

3. **代碼遷移**
   ```bash
   # 複製 Job 相關代碼
   cp -r smart-admin-api-java21-springboot3/sa-base/support/job/* \
         ruoyi-job/src/main/java/net/lab1024/sa/job/

   # 保留 Manager 層結構 (✅ 關鍵設計)
   # ruoyi-job/src/main/java/net/lab1024/sa/job/
   # ├── controller/    # JobController
   # ├── service/       # JobService
   # ├── manager/       # JobManager (✅ 保留事務層)
   # ├── dao/           # JobDao
   # └── domain/        # Entity, VO, Form
   ```

4. **配置文件** (`ruoyi-job/src/main/resources/application.yml`)
   ```yaml
   server:
     port: 9203

   spring:
     application:
       name: ruoyi-job
     datasource:
       # Schema 隔離 (Phase 1 策略)
       url: jdbc:postgresql://localhost:5432/smart_admin?currentSchema=job_schema
       username: smartadmin
       password: ${DATASOURCE_PASSWORD}  # 環境變量
       driver-class-name: org.postgresql.Driver
     cloud:
       nacos:
         discovery:
           server-addr: localhost:8848
           namespace: dev
         config:
           server-addr: localhost:8848
           file-extension: yaml
           namespace: dev
           shared-configs:
             - data-id: common-redis.yaml
               refresh: true
             - data-id: common-mybatis.yaml
               refresh: true

   # Snail-Job 配置
   snail-job:
     namespace: smartadmin-job
     group-name: job-group-001
     server-host: localhost:8088
   ```

5. **啟動類** (`ruoyi-job/src/main/java/net/lab1024/sa/job/JobApplication.java`)
   ```java
   @SpringBootApplication
   @EnableDiscoveryClient
   @MapperScan("net.lab1024.sa.job.dao")  // MyBatis Mapper 掃描
   public class JobApplication {
       public static void main(String[] args) {
           SpringApplication.run(JobApplication.class, args);
       }
   }
   ```

---

##### 任務 3.2: 數據庫 Schema 遷移

**目的**: 創建獨立 Schema，實現數據邏輯隔離

**步驟**:

1. **創建 Job Schema**
   ```sql
   -- 連接 PostgreSQL
   psql -U smartadmin -d smart_admin

   -- 創建 Schema
   CREATE SCHEMA IF NOT EXISTS job_schema;

   -- 授權
   GRANT ALL ON SCHEMA job_schema TO smartadmin;
   GRANT ALL ON ALL TABLES IN SCHEMA job_schema TO smartadmin;
   GRANT ALL ON ALL SEQUENCES IN SCHEMA job_schema TO smartadmin;
   ```

2. **遷移表結構**
   ```sql
   -- 方式 1: 直接移動表 (推薦)
   ALTER TABLE public.t_snail_job_definition SET SCHEMA job_schema;
   ALTER TABLE public.t_snail_job_execution_log SET SCHEMA job_schema;
   ALTER TABLE public.t_snail_job_task SET SCHEMA job_schema;

   -- 方式 2: 複製表 (保留原表)
   CREATE TABLE job_schema.t_snail_job_definition
       AS TABLE public.t_snail_job_definition;

   -- 驗證
   SELECT schemaname, tablename
   FROM pg_tables
   WHERE schemaname = 'job_schema';
   ```

3. **驗收標準**
   - ✅ job_schema 創建成功
   - ✅ 3 張表遷移完成
   - ✅ 數據完整性檢查: `SELECT COUNT(*) FROM job_schema.t_snail_job_definition`
   - ✅ 跨 Schema 查詢禁止: 嘗試 `SELECT * FROM public.t_employee` 應失敗

---

#### Week 4: 集成測試與上線

##### 任務 4.1: Gateway 路由配置

**目的**: 將 Job Service 路由添加到 API 網關

**步驟**:

1. **更新 Gateway 配置** (`ruoyi-gateway/src/main/resources/application.yml`)
   ```yaml
   spring:
     cloud:
       gateway:
         routes:
           # 🆕 Job Service 路由
           - id: ruoyi-job
             uri: lb://ruoyi-job  # Nacos 負載均衡
             predicates:
               - Path=/api/job/**
             filters:
               - StripPrefix=2  # 移除 /api/job 前綴
               - name: CircuitBreaker  # 🆕 熔斷器
                 args:
                   name: jobServiceCircuitBreaker
                   fallbackUri: forward:/fallback/job

           # 原有 SmartAdmin 路由
           - id: smartadmin-monolith
             uri: lb://smartadmin-monolith
             predicates:
               - Path=/api/**
               - Path=!/api/job/**  # 🆕 排除 Job 路由
   ```

2. **熔斷降級處理** (`gateway/controller/FallbackController.java`)
   ```java
   @RestController
   @RequestMapping("/fallback")
   public class FallbackController {

       @GetMapping("/job")
       public ResponseDTO<Void> jobFallback() {
           return ResponseDTO.error(
               SystemErrorCode.SERVICE_UNAVAILABLE,
               "定時任務服務暫時不可用，請稍後重試"
           );
       }
   }
   ```

3. **驗收標準**
   - ✅ 訪問 `http://localhost:8080/api/job/query` 路由到 Job Service
   - ✅ Job Service 停止後，返回降級響應
   - ✅ Job Service 恢復後，自動恢復正常

---

##### 任務 4.2: SmartAdmin 移除 Job 模塊

**目的**: 從單體應用中移除已拆分的 Job 模塊

**步驟**:

1. **移除依賴** (`sa-admin/build.gradle.kts`)
   ```kotlin
   dependencies {
       // ❌ 移除 Job 模塊
       // implementation(project(":sa-base:support:job"))
   }
   ```

2. **移除配置** (`sa-admin/application.yaml`)
   ```yaml
   # ❌ 移除 Snail-Job 配置
   # snail-job:
   #   namespace: smartadmin-job
   ```

3. **驗證清理**
   ```bash
   # 搜索殘留引用
   cd smart-admin-api-java21-springboot3/sa-admin
   grep -r "snail-job" src/
   grep -r "JobService" src/

   # 應該沒有任何結果 (除了 git 歷史)
   ```

4. **編譯測試**
   ```bash
   ./gradlew :sa-admin:clean :sa-admin:build

   # 預期結果: 編譯成功 (無 Job 模塊引用錯誤)
   ```

---

### Phase 2 驗收檢查清單

- [ ] **Job Service 獨立運行**
  - [ ] 啟動成功 (9203)
  - [ ] Nacos 控制台顯示服務
  - [ ] 定時任務執行正常

- [ ] **Manager 層驗證** (✅ 關鍵檢查)
  - [ ] JobManager 存在且包含 @Transactional 方法
  - [ ] ArchUnit 測試通過: `./gradlew :ruoyi-job:test --tests ArchitectureTest`
  - [ ] Manager 層事務測試通過 (見下方)

- [ ] **Gateway 路由**
  - [ ] `/api/job/**` 路由到 Job Service
  - [ ] 熔斷降級測試通過

- [ ] **SmartAdmin 清理**
  - [ ] 移除 Job 模塊依賴
  - [ ] SmartAdmin 啟動正常 (無 Job 引用錯誤)
  - [ ] 功能測試正常 (其他模塊不受影響)

**Manager 層事務測試**:
```java
@SpringBootTest
@Transactional
class JobManagerTest {

    @Autowired
    private JobManager jobManager;

    @Autowired
    private JobDao jobDao;

    @Test
    @Rollback(false)  // 驗證事務提交
    void testAddJobTransaction() {
        // Arrange
        JobEntity job = new JobEntity();
        job.setJobName("test-job");

        // Act
        jobManager.addJobTransaction(job);  // Manager 層事務方法

        // Assert
        JobEntity saved = jobDao.selectById(job.getJobId());
        assertNotNull(saved);
        assertEquals("test-job", saved.getJobName());
    }

    @Test
    void testTransactionRollback() {
        // Arrange
        JobEntity job = new JobEntity();
        job.setJobName(null);  // 違反非空約束

        // Act & Assert
        assertThrows(DataIntegrityViolationException.class, () -> {
            jobManager.addJobTransaction(job);
        });

        // 驗證回滾: 數據庫中不應存在該記錄
        long count = jobDao.selectCount(null);
        assertEquals(0, count);
    }
}
```

---

## Phase 3: Resource Service 拆分 (Week 5-6)

### 目標

拆分第二個微服務 (Resource Service)，進一步驗證架構穩定性。

### 任務清單 (類似 Phase 2)

- Week 5: 創建 Resource Service 模塊，代碼遷移
- Week 6: 集成測試，SmartAdmin 清理

**拆分內容**:
- File Service (文件上傳/下載/OSS)
- Mail Service (郵件發送)
- SMS Service (短信發送，可選)

**詳細步驟**: 參照 Phase 2，不再贅述。

---

## Phase 4: 測試與驗證 (Week 7-8)

### 目標

全面測試遷移後的系統，確保生產就緒。

### 測試清單

#### 功能測試

- [ ] **Job Service**
  - [ ] 創建定時任務
  - [ ] 執行定時任務
  - [ ] 查看執行日誌

- [ ] **Resource Service**
  - [ ] 文件上傳 (單文件/多文件)
  - [ ] 文件下載
  - [ ] 郵件發送

- [ ] **SmartAdmin 單體**
  - [ ] 用戶管理 (CRUD)
  - [ ] 角色管理
  - [ ] 商品管理

#### 性能測試

使用 JMeter 進行壓力測試:

```xml
<!-- JMeter 測試計劃 -->
<TestPlan>
  <ThreadGroup name="Gateway Stress Test">
    <stringProp name="ThreadGroup.num_threads">100</stringProp>
    <stringProp name="ThreadGroup.ramp_time">10</stringProp>
    <stringProp name="ThreadGroup.duration">300</stringProp>
  </ThreadGroup>

  <!-- 測試場景 -->
  <HTTPSamplerProxy name="Query User List">
    <stringProp name="HTTPSampler.path">/api/employee/query</stringProp>
  </HTTPSamplerProxy>

  <HTTPSamplerProxy name="Upload File">
    <stringProp name="HTTPSampler.path">/api/resource/upload</stringProp>
  </HTTPSamplerProxy>
</TestPlan>
```

**性能基準**:
- TPS ≥ 1000 (Gateway 轉發)
- P99 延遲 < 100ms (SmartAdmin 單體)
- P99 延遲 < 150ms (微服務 RPC 調用)
- 錯誤率 < 0.1%

#### 穩定性測試

**7×24h 穩定性測試**:
```bash
# 使用 Gatling 進行長時間壓測
gatling.sh -s SmartAdminSimulation -d 604800  # 7 天

# 監控指標
# - 內存使用穩定 (無內存洩漏)
# - CPU 使用 < 70%
# - 響應時間穩定 (無劣化)
# - 無服務重啟/崩潰
```

#### 架構驗證

**ArchUnit 測試**:
```bash
# Job Service 架構測試
./gradlew :ruoyi-job:test --tests ArchitectureTest

# Resource Service 架構測試
./gradlew :ruoyi-resource:test --tests ArchitectureTest

# SmartAdmin 單體架構測試
./gradlew :sa-admin:test --tests ArchitectureTest

# 預期結果: 所有測試通過 (Manager 層約束生效)
```

---

## 回滾方案

### 回滾決策矩陣

| 情況 | 回滾範圍 | 回滾步驟 |
|------|---------|---------|
| **Phase 1 失敗** | Nacos/Gateway | 停止 Nacos/Gateway，恢復原配置 |
| **Phase 2 失敗** | Job Service | 停止 Job Service，恢復 sa-admin 依賴 |
| **Phase 3 失敗** | Resource Service | 停止 Resource Service，恢復依賴 |
| **Phase 4 性能不達標** | 全部回滾 | 停止所有微服務，使用單體應用 |

### 快速回滾腳本

```bash
#!/bin/bash
# rollback.sh

echo "開始回滾..."

# 1. 停止微服務
docker stop ruoyi-job ruoyi-resource

# 2. 停止 Gateway
docker stop ruoyi-gateway

# 3. 停止 Nacos
docker stop nacos-standalone

# 4. 恢復 SmartAdmin 配置
cd smart-admin-api-java21-springboot3/sa-admin
git checkout application.yaml  # 恢復配置文件
git checkout build.gradle.kts  # 恢復依賴

# 5. 重啟 SmartAdmin
./gradlew :sa-admin:bootRun

echo "回滾完成，SmartAdmin 單體模式恢復運行"
```

---

## 文檔清單

完成遷移後，需更新以下文檔：

- [ ] [架構圖](../architecture/system-architecture.md) - 更新微服務拓撲
- [ ] [部署指南](../deployment/README.md) - 新增 Nacos/Gateway 部署
- [ ] [開發環境搭建](../development/setup.md) - 更新本地開發配置
- [ ] [API 文檔](http://localhost:8080/doc.html) - Gateway 路由說明
- [ ] [運維手冊](../operations/README.md) - 新增監控/告警配置

---

**文檔版本**: 1.0.0
**最後更新**: 2026-02-02
**維護**: 架構組
