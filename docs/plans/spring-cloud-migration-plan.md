# SmartAdmin Spring Cloud 升級設計方案

## 執行概要

**當前架構**: 模組化單體 (Modular Monolith)
**目標架構**: Spring Cloud 微服務
**升級策略**: 漸進式遷移，保持系統可用性
**核心挑戰**: Sa-Token集成、分佈式事務、JetCache跨服務同步

---

## 一、專案現狀分析

### 1.1 模組結構

```
smart-admin (模組化單體)
├── sa-base (基礎設施層)
│   ├── 公共域模型: ResponseDTO, PageResult, ErrorCode
│   ├── 認證框架: Sa-Token 1.44.0
│   ├── ORM框架: MyBatis Plus 3.5.12
│   └── 所有Spring依賴導出
│
├── sa-common (共享服務層 - 9個子模組)
│   ├── cache (JetCache: Caffeine本地 + Redis遠端)
│   ├── mq (Spring Kafka)
│   ├── redis-lock (Lock4j + Redisson)
│   ├── api-encrypt, captcha, repeat-submit
│   ├── data-masking, security-protect
│   └── core (共享數據模型)
│
└── sa-admin (業務應用層)
    ├── system模組
    │   ├── employee, department, position, role
    │   ├── login, menu, datascope, message
    │   └── support/*
    └── business模組
        ├── goods, category
        └── oa (enterprise, bank, invoice, notice)
```

### 1.2 技術棧

| 組件 | 版本 | 關鍵特性 |
|------|------|---------|
| Java | 21 | 最新LTS |
| Spring Boot | 3.5.4 | 最新穩定版 |
| 認證授權 | Sa-Token 1.44.0 | 非Spring Security |
| 快取 | JetCache 2.7.7 | 本地+遠端二級快取 |
| 分散式鎖 | Lock4j 2.2.7 + Redisson 3.50.0 | |
| 訊息佇列 | Spring Kafka | Dev啟用，Prod可選 |
| 資料庫 | PostgreSQL + MyBatis Plus | **無外鍵約束** |
| API文件 | Knife4j 4.6.0 | Swagger增強 |
| Web伺服器 | Undertow | 替代Tomcat |
| 日誌 | Log4j2 | 非Logback |

### 1.3 關鍵依賴關係

**跨模組呼叫**:
```java
// EmployeeService 的9處跨模組依賴
EmployeeService
├── DepartmentDao (跨模組)
├── RoleEmployeeDao (跨模組)
├── PositionDao (跨模組)
├── DepartmentCacheManager (跨模組)
├── SecurityPasswordService (跨模組)
└── LoginManager (跨模組)
```

**事務邊界**:
```java
// EmployeeManager.java (強一致性)
@Transactional(rollbackFor = Throwable.class)
public void saveEmployee(EmployeeEntity employee, List<Long> roleIdList) {
    employeeDao.insert(employee);  // 表1: t_employee
    roleIdList.forEach(roleId ->
        roleEmployeeDao.insert(...)); // 表2: t_role_employee
}

// GoodsService.java (弱一致性 - 透過快取驗證)
@Transactional(rollbackFor = Exception.class)
public ResponseDTO<String> add(GoodsAddForm form) {
    // 1. 快取驗證分類存在性 (120分鐘TTL)
    Optional<CategoryEntity> category = queryCategory(form.getCategoryId());
    // 2. 插入商品
    goodsDao.insert(goodsEntity);
}
```

**快取策略**:
```java
// CategoryCacheManager.java
@Cached(
    cacheType = CacheType.BOTH,  // 本地(Caffeine) + 遠端(Redis)
    localExpire = 30,            // 本地30分鐘
    expire = 120,                // 遠端120分鐘
    timeUnit = TimeUnit.MINUTES
)
public CategoryEntity queryCategory(Long categoryId) {
    return categoryDao.selectById(categoryId);
}
```

### 1.4 資料一致性現狀

**強一致性場景** (本地事務保障):
- Employee ↔ Role 綁定 (EmployeeManager)
- Role ↔ Menu 權限分配 (RoleMenuManager)

**弱一致性場景** (快取驗證):
- Goods → Category (120分鐘快取)
- Enterprise → Employee (無事務保護 ⚠️)

**架構問題**:
1. 部分Service層使用 `rollbackFor = Exception.class` (應為Throwable.class)
2. `EnterpriseService.addEmployee()` 無事務註解
3. `synchronized` 僅提供單機併發控制

---

## 二、Spring Cloud 組件選型

### 2.1 推薦技術棧

| 組件類型 | 推薦選擇 | 版本 | 選型理由 |
|---------|---------|------|---------|
| **服務註冊發現** | Nacos | 2.3.2 | • 統一註冊+配置中心<br>• AP/CP模式切換<br>• Spring Boot 3相容<br>• Alibaba生態契合 |
| **配置中心** | Nacos Config | 2.3.2 | • 與註冊中心統一<br>• 動態刷新<br>• 命名空間隔離 |
| **API閘道** | Spring Cloud Gateway | 4.1.x | • 回應式架構<br>• 原生Spring生態<br>• Sa-Token整合友善 |
| **服務呼叫** | OpenFeign | 4.1.x | • 宣告式REST客戶端<br>• Resilience4j整合 |
| **非同步訊息** | Spring Cloud Stream | 4.1.x | • 抽象Kafka/RabbitMQ<br>• 事件驅動架構 |
| **熔斷限流** | Resilience4j | 2.x | • Hystrix繼任者<br>• Spring Boot 3原生 |
| **分散式追蹤** | Micrometer Tracing + Zipkin | 1.2.x | • Spring Boot 3可觀測性<br>• OpenTelemetry支援 |
| **負載平衡** | Spring Cloud LoadBalancer | 4.1.x | • Spring Cloud預設<br>• 替代Ribbon |
| **分散式事務** | Seata | 2.0.x | • AT模式(最小改動)<br>• Alibaba生態 |

### 2.2 對比分析: Nacos vs Eureka vs Consul

| 特性 | Nacos ⭐推薦 | Eureka | Consul |
|-----|-----------|--------|--------|
| **配置管理** | ✅ 內建 | ❌ 需Config Server | ⚠️ KV儲存 |
| **CAP模型** | AP/CP可切換 | AP | CP |
| **健康檢查** | HTTP/TCP/MySQL | HTTP | HTTP/TCP/Script |
| **多資料中心** | ✅ 命名空間 | ⚠️ 有限 | ✅ 原生 |
| **Spring Boot 3** | ✅ 積極維護 | ⚠️ 停止更新 | ✅ 社群支援 |
| **運維複雜度** | 中 (基於Java) | 低 | 中 (基於Go) |

**決策**: **Nacos** - 統一註冊+配置，降低運維開銷

---

## 三、架構演進路線圖

### Phase 1: 基礎設施準備 (不中斷服務)

**目標**: 建立Spring Cloud基礎設施，閘道代理模式運行

**關鍵任務**:
1. 部署Nacos叢集 (3節點，高可用)
2. 部署Spring Cloud Gateway (代理模式轉發到單體)
3. 配置分散式追蹤 (Micrometer + Zipkin)
4. 遷移配置到Nacos (雙運行模式: 本地YAML + Nacos)
5. 建立CI/CD流水線 (Gradle + Docker + Kubernetes)

**驗收標準**:
- ✅ 閘道轉發100%流量到單體，延遲<10ms
- ✅ 所有請求鏈路在Zipkin可見
- ✅ Nacos配置變更即時生效

**配置遷移範例**:
```yaml
# 當前: sa-base/src/main/resources/dev/sa-base.yaml
# 目標: Nacos配置中心

Namespace: dev
├── sa-admin.yaml (應用配置)
├── sa-gateway.yaml (閘道配置)
├── sa-common-redis.yaml (共享-Redis)
├── sa-common-kafka.yaml (共享-Kafka)
└── sa-common-datasource.yaml (共享-資料庫)
```

**回滾策略**: 移除閘道，直接存取單體應用

---

### Phase 2: 垂直拆分 - 低耦合模組

**目標**: 提取獨立業務模組，驗證微服務化可行性

#### 2.1 第一個微服務: OA Service

**選擇理由**:
- 自包含業務域 (企業、銀行、發票、通知)
- 最小跨模組依賴
- 低事務複雜度

**拆分範圍**:
```
sa-admin/module/business/oa/* → oa-service
資料庫: sa_oa (獨立PostgreSQL schema)
```

**依賴處理**:
```kotlin
// 保留依賴 (以庫形式引入)
dependencies {
    implementation(project(":sa-base"))
    implementation(project(":sa-common:cache"))
    implementation(project(":sa-common:core"))
}
```

**閘道路由配置**:
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: oa-service
          uri: lb://oa-service  # 負載平衡
          predicates:
            - Path=/api/oa/**
          filters:
            - StripPrefix=1
            - SaTokenAuthFilter  # 統一認證
```

**驗收標準**:
- ✅ OA介面回應時間<100ms (P95)
- ✅ 灰度發布10% → 50% → 100%
- ✅ 零資料遺失，48小時無錯誤日誌

#### 2.2 第二個微服務: Goods Service

**模組範圍**:
```
goods-service
├── goods (商品管理)
└── category (分類管理)  ⚠️ 與goods強耦合，暫不拆分
```

**關鍵挑戰**: 快取一致性

**解決方案**:
```java
// 分類更新時透過訊息匯流排失效快取
@Service
public class CategoryService {
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Transactional
    public void updateCategory(CategoryUpdateForm form) {
        categoryDao.updateById(entity);
        // 發布快取失效事件
        eventPublisher.publishEvent(
            new CacheInvalidationEvent("category:entity:" + id)
        );
    }
}

// 所有服務實例監聽
@Component
public class CacheInvalidationListener {
    @StreamListener("cache-invalidation")
    public void handle(CacheInvalidationEvent event) {
        jetCache.invalidate(event.getCacheKey());
    }
}
```

**驗收標準**:
- ✅ 分類快取同步延遲<1秒
- ✅ 商品建立成功率>99.9%

---

### Phase 3: 核心系統服務拆分

**目標**: 拆分高耦合核心模組

#### 3.1 UAC Service (用戶訪問控制)

**模組範圍**:
```
uac-service
├── employee (員工管理)
├── department (部門管理)
├── role (角色管理)
├── position (職位管理)
├── menu (選單管理)
├── login (登入認證)
└── datascope (資料權限)
```

**資料庫策略**:
- **Phase 3**: 共享資料庫 (避免分散式事務)
- **Phase 4**: 獨立資料庫 + Seata

**事務改造範例**:
```java
// 當前單體事務
@Transactional(rollbackFor = Throwable.class)
public void saveEmployee(EmployeeEntity employee, List<Long> roleIdList) {
    employeeDao.insert(employee);
    roleIdList.forEach(roleId ->
        roleEmployeeDao.insert(new RoleEmployeeEntity(roleId, employee.getEmployeeId())));
}

// 微服務階段 (共享資料庫，無需改動)
@Transactional(rollbackFor = Throwable.class)
public void saveEmployee(...) {
    // 同樣的程式碼，存取同一資料庫
}

// Phase 4 (獨立資料庫 + Seata)
@GlobalTransactional(rollbackFor = Throwable.class)
public void saveEmployee(EmployeeEntity employee, List<Long> roleIdList) {
    employeeDao.insert(employee);
    roleClient.batchAssignRoles(employee.getEmployeeId(), roleIdList); // Feign呼叫
}
```

#### 3.2 Support Service (基礎設施支撐)

**模組範圍**:
```
support-service
├── file (檔案管理 - S3整合)
├── dict (資料字典)
├── datatracer (資料追蹤)
├── operatelog (操作日誌)
├── loginlog (登入日誌)
├── heartbeat (心跳檢測)
└── helpdoc (幫助文件)
```

**驗收標準**:
- ✅ 所有核心API回應<200ms (P95)
- ✅ 熔斷器觸發率<1%
- ✅ 分散式快取命中率>90%

---

### Phase 4: 治理與最佳化

**目標**: 生產級微服務架構

**關鍵任務**:
1. **Seata分散式事務**:
   - AT模式處理Employee-Role綁定
   - TCC模式處理複雜業務
2. **資料庫實體分離**:
   - 每個微服務獨立PostgreSQL實例
   - 讀寫分離 + 主從複製
3. **閘道限流**:
   - 基於Token Bucket演算法
   - IP層級 + 用戶層級限流
4. **可觀測性完善**:
   - RED指標 (Rate, Errors, Duration)
   - Prometheus + Grafana監控
   - ELK日誌聚合
5. **混沌工程**:
   - 服務網格 (Istio可選)
   - 故障注入測試

**驗收標準**:
- ✅ 99.9% SLA
- ✅ 分散式事務成功率>99.5%
- ✅ 資源成本最佳化30%

---

## 四、關鍵技術難點與解決方案

### 4.1 Sa-Token 微服務整合

**挑戰**: Sa-Token非標準OAuth2，需自訂閘道整合

**架構方案**:
```
客戶端請求
    ↓
[API Gateway]
    ├─ SaTokenAuthFilter (GlobalFilter)
    │   ├─ 從Header提取: Authorization: Bearer {token}
    │   ├─ StpUtil.checkLogin() → Redis驗證
    │   ├─ 載入權限: LoginManager.getPermissions(userId)
    │   └─ 注入Header: X-User-Id, X-User-Type, X-Permissions
    ↓
[微服務層]
    ├─ UserContextInterceptor
    │   └─ 從Header恢復: AdminRequestUtil.setRequestUser()
    ├─ @SaCheckPermission 繼續使用 (驗證邏輯不變)
    └─ 業務邏輯
```

**閘道過濾器實作** (偽程式碼):
```java
@Component
public class SaTokenGatewayFilter implements GlobalFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String token = extractToken(exchange);
        try {
            StpUtil.checkLogin();  // Redis驗證
            Object loginId = StpUtil.getLoginId();

            // 注入用戶上下文到下游服務
            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header("X-User-Id", loginId.toString())
                .header("X-User-Type", StpUtil.getLoginIdAsString())
                .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        } catch (NotLoginException e) {
            return unauthorized(exchange);
        }
    }
}
```

**微服務配置** (共享Redis):
```yaml
sa-token:
  token-name: Authorization
  is-read-cookie: false
  redis:
    database: 1
    host: ${REDIS_HOST:redis.smart-admin.svc.cluster.local}
    port: 6379
```

**優勢**:
- ✅ `@SaCheckPermission` 註解無需修改
- ✅ 權限驗證邏輯複用
- ✅ 統一會話管理

---

### 4.2 JetCache 跨服務快取同步

**問題**: 本地快取(Caffeine)在服務實例間不同步

**當前配置**:
```java
@Cached(
    cacheType = CacheType.BOTH,  // 本地 + 遠端
    localExpire = 30,            // 本地30分鐘
    expire = 120                 // 遠端120分鐘
)
```

**解決方案**: Spring Cloud Bus + Kafka

**架構**:
```
分類服務更新資料
    ↓
CategoryService.update()
    ↓
發布事件: CacheInvalidationEvent
    ↓
Spring Cloud Bus (Kafka: cache-invalidation)
    ↓
所有服務實例訂閱
    ↓
JetCache.invalidate(cacheKey)
```

**實作程式碼**:
```java
// 發布端
@Service
public class CategoryService {
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @CacheInvalidate(name = "category:entity", key = "#id")
    public void update(Long id, CategoryUpdateForm form) {
        categoryDao.updateById(entity);

        // 廣播快取失效事件
        eventPublisher.publishEvent(
            new CacheInvalidationEvent(this, "category:entity:" + id)
        );
    }
}

// 訂閱端 (所有實例)
@Component
public class CacheInvalidationListener {
    @Resource
    private CacheService cacheService;

    @StreamListener("cache-invalidation")
    public void handle(CacheInvalidationEvent event) {
        cacheService.clear(event.getCacheKey());
    }
}
```

**配置**:
```yaml
spring:
  cloud:
    stream:
      kafka:
        binder:
          brokers: kafka:9092
      bindings:
        cache-invalidation:
          destination: cache-invalidation-topic
          group: ${spring.application.name}
```

**替代方案** (簡化版):
```java
// 只使用遠端快取，避免同步問題
@Cached(cacheType = CacheType.REMOTE)
```

---

### 4.3 分散式事務處理

**場景**: Employee + Role 綁定 (當前本地事務)

**Phase 3方案** (共享資料庫):
```java
// 無需修改，繼續使用 @Transactional
@Transactional(rollbackFor = Throwable.class)
public void saveEmployee(EmployeeEntity employee, List<Long> roleIdList) {
    employeeDao.insert(employee);
    roleEmployeeDao.batchInsert(roleIdList, employee.getEmployeeId());
}
```

**Phase 4方案** (獨立資料庫 + Seata AT模式):

**步驟1**: 新增Seata依賴
```kotlin
// build.gradle.kts
dependencies {
    implementation("com.alibaba.cloud:spring-cloud-starter-alibaba-seata:2023.0.1.0")
}
```

**步驟2**: 改造程式碼
```java
@GlobalTransactional(rollbackFor = Throwable.class, timeoutMills = 30000)
public void saveEmployee(EmployeeEntity employee, List<Long> roleIdList) {
    // Step 1: 本地資料庫插入員工
    employeeDao.insert(employee);

    // Step 2: Feign呼叫角色服務
    RoleAssignRequest request = new RoleAssignRequest(
        employee.getEmployeeId(), roleIdList);
    ResponseDTO<Void> result = roleClient.batchAssignRoles(request);

    if (!result.isSuccess()) {
        throw new BusinessException("角色分配失敗");
    }
}
```

**步驟3**: Seata Server配置
```yaml
seata:
  tx-service-group: smart-admin-tx-group
  service:
    vgroup-mapping:
      smart-admin-tx-group: default
    grouplist:
      default: seata-server:8091
  registry:
    type: nacos
    nacos:
      server-addr: nacos:8848
      group: SEATA_GROUP
```

**效能影響**:
- 回應時間: 100ms → 300ms (+200%)
- 吞吐量: 約-10%

**緩解措施**:
- 非同步化非關鍵路徑 (如日誌記錄)
- 最佳化Feign連接池 (maxConnections: 200)
- 使用事務補償模式 (Saga) 替代Seata

---

### 4.4 MyBatis Plus 跨服務資料存取

**問題**: 當前直接跨Dao呼叫，微服務需改為Feign

**改造範例**:

**Before (單體)**:
```java
// EmployeeService.java
@Autowired
private DepartmentDao departmentDao;

public EmployeeVO getDetail(Long employeeId) {
    EmployeeEntity employee = employeeDao.selectById(employeeId);
    DepartmentEntity dept = departmentDao.selectById(employee.getDepartmentId());
    // ... 組裝VO
}
```

**After (微服務)**:
```java
// EmployeeService.java
@Autowired
private DepartmentClient departmentClient;  // Feign客戶端

public EmployeeVO getDetail(Long employeeId) {
    EmployeeEntity employee = employeeDao.selectById(employeeId);

    // Feign遠端呼叫
    ResponseDTO<DepartmentVO> deptResp =
        departmentClient.getById(employee.getDepartmentId());

    if (!deptResp.isSuccess()) {
        throw new BusinessException("部門服務不可用");
    }
    // ... 組裝VO
}
```

**Feign客戶端定義**:
```java
@FeignClient(name = "uac-service", path = "/department")
public interface DepartmentClient {
    @GetMapping("/{id}")
    ResponseDTO<DepartmentVO> getById(@PathVariable("id") Long id);
}
```

**Resilience4j熔斷器**:
```java
@FeignClient(
    name = "uac-service",
    fallbackFactory = DepartmentClientFallbackFactory.class
)
public interface DepartmentClient {
    // ...
}

@Component
class DepartmentClientFallbackFactory
    implements FallbackFactory<DepartmentClient> {
    @Override
    public DepartmentClient create(Throwable cause) {
        return id -> {
            log.error("部門服務呼叫失敗", cause);
            return ResponseDTO.error(SystemErrorCode.SERVICE_UNAVAILABLE);
        };
    }
}
```

**配置**:
```yaml
spring:
  cloud:
    openfeign:
      client:
        config:
          default:
            connectTimeout: 2000
            readTimeout: 5000
      circuitbreaker:
        enabled: true

resilience4j:
  circuitbreaker:
    instances:
      uac-service:
        failureRateThreshold: 50
        waitDurationInOpenState: 10000
        slidingWindowSize: 100
```

---

## 五、微服務邊界設計

### 5.1 最終微服務拆分方案

```
┌─────────────────────────────────────────┐
│         API Gateway (sa-gateway)        │
│    Spring Cloud Gateway + Sa-Token      │
└─────────────────────────────────────────┘
              ↓ (路由轉發)
┌─────────────────────────────────────────────────────┐
│                  微服務層                          │
├─────────────────────────────────────────────────────┤
│                                                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────┐  │
│  │ uac-service  │  │ goods-service│  │ oa-service│ │
│  │ 用戶訪問控制  │  │   商品管理    │  │  OA系統   │ │
│  │              │  │              │  │          │  │
│  │• employee    │  │• goods       │  │• enterprise│
│  │• department  │  │• category    │  │• bank    │  │
│  │• role        │  │              │  │• invoice │  │
│  │• position    │  │              │  │• notice  │  │
│  │• menu        │  │              │  │          │  │
│  │• login       │  │              │  │          │  │
│  │• datascope   │  │              │  │          │  │
│  │              │  │              │  │          │  │
│  │ DB: sa_uac   │  │ DB: sa_goods │  │ DB: sa_oa│  │
│  └──────────────┘  └──────────────┘  └──────────┘  │
│                                                     │
│  ┌──────────────┐  ┌──────────────┐                │
│  │support-service│ │security-service│              │
│  │  基礎設施支撐 │  │   安全防護    │               │
│  │              │  │              │                │
│  │• file        │  │• password    │                │
│  │• dict        │  │• loginprotect│                │
│  │• datatracer  │  │• fileverify  │                │
│  │• operatelog  │  │              │                │
│  │• loginlog    │  │              │                │
│  │              │  │              │                │
│  │DB: sa_support│  │DB: sa_security│               │
│  └──────────────┘  └──────────────┘                │
└─────────────────────────────────────────────────────┘
              ↓ (依賴)
┌─────────────────────────────────────────────────────┐
│         sa-common (Spring Boot Starter庫)           │
├─────────────────────────────────────────────────────┤
│ cache, mq, redis-lock, api-encrypt, captcha,       │
│ repeat-submit, data-masking, security-protect, core │
└─────────────────────────────────────────────────────┘
```

### 5.2 資料庫策略

**Phase 2-3: 邏輯分離** (Schema隔離)
```sql
-- 單個PostgreSQL實例
CREATE SCHEMA sa_uac;
CREATE SCHEMA sa_goods;
CREATE SCHEMA sa_oa;
CREATE SCHEMA sa_support;

-- 跨Schema查詢仍可用，但禁止
SELECT * FROM sa_uac.t_employee e
JOIN sa_goods.t_goods g ON e.employee_id = g.create_user_id; -- 禁止!
```

**Phase 4: 實體分離** (獨立實例)
```
PostgreSQL叢集
├── db-uac.sa.local:5432     (UAC服務)
├── db-goods.sa.local:5432   (商品服務)
├── db-oa.sa.local:5432      (OA服務)
└── db-support.sa.local:5432 (支撐服務)
```

**遷移工具**:
```bash
# Flyway資料庫版本管理
./gradlew :sa-uac-service:flywayMigrate -Dflyway.url=jdbc:postgresql://db-uac:5432/sa_uac
```

---

## 六、風險評估與緩解

### 6.1 技術風險

| 風險 | 影響 | 機率 | 緩解措施 |
|------|------|------|---------|
| **Sa-Token與Spring Cloud不相容** | 高 | 低 | • 使用共享Redis會話<br>• 閘道早期測試<br>• 後備方案: 遷移到Spring Security OAuth2 |
| **JetCache本地快取髒資料** | 中 | 中 | • 實作快取失效匯流排<br>• 降低本地TTL至5分鐘<br>• 或改為REMOTE模式 |
| **MyBatis Plus跨服務查詢失效** | 高 | 高 | • Phase 2-3保持共享資料庫<br>• 引入OpenFeign替代跨Dao呼叫<br>• 非關鍵場景接受最終一致性 |
| **分散式事務失敗** | 高 | 低 | • 使用Seata 2PC<br>• 補償邏輯<br>• 監控+告警 |
| **網路延遲增加** | 中 | 中 | • HTTP連接池最佳化<br>• Resilience4j熔斷<br>• 熱點資料快取 |

### 6.2 效能風險

| 場景 | 單體基線 | 微服務預估 | 緩解措施 |
|------|---------|----------|---------|
| **登入** (無跨服務) | 50ms | 55ms (+10%) | 閘道開銷可接受 |
| **員工查詢** (Dept+Role) | 80ms | 150ms (+88%) | • 資料庫非規範化<br>• 回應快取(60s TTL) |
| **商品查詢** (分類快取命中) | 30ms | 35ms (+17%) | 本地快取命中，無遠端呼叫 |
| **分散式事務** | 100ms | 300ms (+200%) | • 非同步化非關鍵路徑<br>• Seata最佳化(xa_timeout=30s) |

**建議**: Phase 2-3保持共享資料庫，延遲效能劣化

### 6.3 運維風險

| 風險 | 緩解措施 |
|------|---------|
| **服務數量激增** (10+) | • 按領域分組(UAC, Goods, OA)<br>• sa-common保持庫形式 |
| **部署複雜度** | • Kubernetes自動化<br>• Helm Charts模板<br>• GitOps (ArgoCD) |
| **監控盲點** | • 分散式追蹤(Zipkin)<br>• 集中日誌(ELK)<br>• 服務網格可觀測性 |
| **Nacos單點故障** | • 3節點叢集<br>• 外部PostgreSQL持久化<br>• 健康檢查+自動重啟 |

---

## 七、實施優先級

### Phase 1: 基礎設施 (第1-4週)

**里程碑**: 閘道代理模式運行

**任務清單**:
- [ ] Nacos叢集部署 (3節點)
- [ ] Spring Cloud Gateway部署 (代理模式)
- [ ] Micrometer Tracing + Zipkin配置
- [ ] 配置遷移到Nacos (雙運行)
- [ ] CI/CD流水線 (Gradle + Jib + K8s)

**驗收標準**:
- ✅ 閘道轉發100%流量，延遲<10ms
- ✅ Zipkin顯示完整呼叫鏈
- ✅ Nacos配置變更1秒內生效

---

### Phase 2: 首批服務 (第5-8週)

**里程碑**: OA服務獨立部署

**任務清單**:
- [ ] 建立 `oa-service` 模組
- [ ] 資料庫Schema分離 (`sa_oa`)
- [ ] 閘道路由配置 (`/api/oa/**` → oa-service)
- [ ] 灰度發布 (10% → 50% → 100%)
- [ ] 監控+告警配置

**驗收標準**:
- ✅ OA介面<100ms (P95)
- ✅ 48小時零錯誤
- ✅ ArchUnit測試通過

---

### Phase 3: 核心服務 (第9-16週)

**里程碑**: UAC + Goods服務獨立

**任務清單**:
- [ ] 提取 `uac-service` (保持共享資料庫)
- [ ] 提取 `goods-service` (含category)
- [ ] 提取 `support-service`
- [ ] JetCache快取同步機制
- [ ] Resilience4j熔斷器
- [ ] 壓力測試 (1000 RPS)

**驗收標準**:
- ✅ 所有API<200ms (P95)
- ✅ 熔斷觸發率<1%
- ✅ 快取命中率>90%

---

### Phase 4: 治理完善 (第17-24週)

**里程碑**: 生產級微服務

**任務清單**:
- [ ] Seata分散式事務
- [ ] 資料庫實體分離
- [ ] 閘道限流策略
- [ ] Grafana監控大盤
- [ ] 混沌工程測試
- [ ] 效能最佳化

**驗收標準**:
- ✅ 99.9% SLA
- ✅ 分散式事務成功率>99.5%
- ✅ 成本最佳化30%

---

## 八、關鍵檔案清單

| 檔案路徑 | 重要性 | 說明 |
|---------|-------|------|
| `sa-admin/src/main/java/.../employee/service/EmployeeService.java` | ⭐⭐⭐⭐⭐ | 跨模組依賴典型(9處)，服務拆分邊界參考 |
| `sa-admin/src/main/java/.../employee/manager/EmployeeManager.java` | ⭐⭐⭐⭐⭐ | 事務管理模式，Seata改造參考 |
| `sa-base/build.gradle.kts` | ⭐⭐⭐⭐⭐ | 共享依賴定義，Spring Cloud版本相容性 |
| `sa-admin/src/main/java/.../category/manager/CategoryCacheManager.java` | ⭐⭐⭐⭐ | JetCache使用模式，快取同步策略 |
| `sa-base/src/main/resources/dev/sa-base.yaml` | ⭐⭐⭐⭐ | 配置遷移到Nacos參考 |

---

## 九、資源需求

### 9.1 技能要求

| 技能 | 優先級 | 當前差距評估 |
|------|-------|------------|
| Spring Cloud (Gateway, OpenFeign) | 關鍵 | 中等 (熟悉Spring Boot) |
| Nacos (註冊+配置) | 關鍵 | 高 (新技術) |
| Micrometer Tracing | 高 | 中等 (替代Sleuth) |
| Resilience4j | 高 | 高 (新模式) |
| Kubernetes | 關鍵 | 取決於現有運維 |
| Seata | 中等 | 高 (複雜主題) |

**建議**:
- **培訓**: 2週Spring Cloud + Kubernetes訓練營
- **外部諮詢**: Seata實施 (1個月)
- **招聘**: 1名高級SRE (Kubernetes專家)

### 9.2 基礎設施

**開發環境**:
```
資源需求:
- Nacos叢集: 3節點 × 2 CPU, 4GB RAM
- API Gateway: 2實例 × 2 CPU, 1GB RAM
- Redis: 1實例 × 2 CPU, 4GB RAM
- Kafka: 3 broker × 2 CPU, 4GB RAM
- PostgreSQL: 1實例 × 4 CPU, 8GB RAM
- Zipkin: 1實例 × 2 CPU, 2GB RAM
總計: ~30 CPU, 48GB RAM
```

**生產環境** (單區域):
```
- Nacos叢集: 3節點 × 4 CPU, 8GB RAM
- API Gateway: 3+ (彈性) × 4 CPU, 2GB RAM
- 微服務 (5服務×3副本): 15 pods × 2 CPU, 2GB RAM
- Redis叢集: 6節點 × 4 CPU, 8GB RAM
- Kafka叢集: 5 broker × 8 CPU, 16GB RAM
- PostgreSQL (主從): 2實例 × 16 CPU, 32GB RAM
- 監控棧: 10 CPU, 20GB RAM
總計: ~200 CPU, 300GB RAM
```

**雲成本估算** (AWS):
- 開發: ~$500/月
- 生產 (單區域): ~$3,000/月

---

## 十、總結

### 核心建議

1. ✅ **使用Nacos** (非Eureka) - 統一註冊+配置
2. ✅ **保留Sa-Token** - 無需遷移Spring Security
3. ✅ **保留JetCache** - 增加快取失效匯流排
4. ✅ **延遲資料庫拆分** - Phase 3保持共享，避免早期分散式事務
5. ✅ **先拆OA服務** - 低風險，高學習價值

### 成功指標

| 指標 | 目標 |
|------|------|
| **效能** | P95延遲<200ms (vs 單體100ms) |
| **可用性** | 99.9% SLA |
| **部署頻率** | >10次/天 |
| **成本** | 基礎設施<單體2倍 |

### 關鍵成功因素

- **團隊培訓** (Spring Cloud + Kubernetes)
- **雙運行模式** (安全回滾)
- **第一天可觀測性** (追蹤+日誌+指標)
- **漸進式流量遷移** (金絲雀發布)

本方案平衡**實用主義** (保留Sa-Token, JetCache) 與**現代化** (Spring Cloud, Kubernetes)，最小化技術債務，同時支援獨立擴展和部署。
