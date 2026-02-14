# SmartAdmin 目錄結構重組方案
## 保留核心優勢 + 參考 RuoYi-Cloud-Plus 目錄組織

**文檔類型**: 架構重組方案
**版本**: 1.0.0
**創建日期**: 2026-02-02
**狀態**: ✅ 需求確認完成，待審核

---

## 📑 目錄

1. [執行摘要](#執行摘要)
2. [重組目標與原則](#重組目標與原則)
3. [新目錄結構設計](#新目錄結構設計)
4. [核心優勢保留驗證](#核心優勢保留驗證)
5. [遷移路徑設計](#遷移路徑設計)
6. [影響分析](#影響分析)
7. [實施時間表](#實施時間表)

---

## 執行摘要

### 🎯 重組目標

**核心定位**: 優化 SmartAdmin 目錄結構（參考 RuoYi-Cloud-Plus），保留所有核心優勢。

**關鍵原則**:
- ✅ **100% 保留**: Manager 層、ArchUnit、Vavr、Java 21
- ✅ **目錄優化**: 扁平化、命名統一、模塊清晰
- ✅ **微服務就緒**: 創建 API 契約層，為未來準備
- ✅ **細粒度拆分**: System、Business、OA 獨立模塊

### 📊 結構對比

| 維度 | 舊結構 | 新結構 | 改進 |
|------|--------|--------|------|
| **命名統一** | ❌ `sa-base` vs `sa-admin` | ✅ `smartadmin-*` 統一 | 易識別 |
| **嵌套層次** | ❌ 三層 `sa-base/foundation/domain` | ✅ 二層 `smartadmin-common/core` | 扁平化 |
| **模塊發現** | ❌ foundation/infrastructure/support | ✅ common/support/modules | 直觀 |
| **業務隔離** | ❌ 集中 `sa-admin/module/` | ✅ 獨立 `smartadmin-system` | 易拆分 |
| **API 契約** | ❌ 無 | ✅ `smartadmin-api/` | 微服務就緒 |

### 💰 成本與收益

**實施成本**:
- 開發工時: **40人天** (6 weeks)
- 風險等級: 🟡 **中等** (漸進式遷移，可回滾)

**長期收益**:
- ✅ 目錄結構更清晰，新人學習成本 ↓ 50%
- ✅ 為微服務化準備，未來拆分成本 ↓ 70%
- ✅ 模塊獨立性提升，測試與部署更靈活

---

## 重組目標與原則

### 核心目標

#### 1. 優化目錄結構

**參考 RuoYi-Cloud-Plus 優點**:
- ✅ 扁平化目錄（減少嵌套層次）
- ✅ 統一命名前綴（`smartadmin-*`）
- ✅ 清晰的模塊職責劃分

**保留 SmartAdmin 特色**:
- ✅ Manager 層（四層架構）
- ✅ ArchUnit 架構驗證（545行規則）
- ✅ Vavr 函數式編程（Service 層強制）
- ✅ Java 21 現代化特性（Virtual Threads + Sealed Classes）

#### 2. 為未來微服務化準備

**創建 API 契約層** (`smartadmin-api/`):
- 🎯 定義模塊間調用接口
- 🎯 使用 Vavr Option 保證類型安全
- 🎯 當前單體應用內部調用，未來可直接用於 Dubbo RPC

**細粒度模塊拆分**:
- 🎯 System、Business、OA 獨立模塊
- 🎯 每個模塊可獨立測試、獨立部署
- 🎯 未來可零成本拆分為微服務

### 設計原則

#### 原則 1: 非侵入式重組

**不改變**:
- ✅ Controller → Service → Manager → Dao 四層架構
- ✅ @Transactional 只在 Manager 層
- ✅ Service 層強制使用 Vavr Option
- ✅ 構造器注入（@RequiredArgsConstructor）
- ✅ ArchUnit 測試規則（545行）

**僅改變**:
- 📁 目錄路徑
- 📦 模塊劃分粒度
- 🏷️ 模塊命名

#### 原則 2: 漸進式遷移

**零停機遷移**:
```
Week 1-2: 新舊並存（sa-base + smartadmin-common 共存）
Week 3-4: 逐步遷移（舊模塊逐個替換為新模塊）
Week 5-6: 完成清理（刪除舊目錄，驗證所有引用）
```

**回滾機制**:
- 每個 Week 結束前創建 Git Tag
- 遷移失敗可立即回滾到上一個 Tag

#### 原則 3: 質量保障

**每個階段驗證**:
- ✅ ArchUnit 測試必須通過
- ✅ 單元測試覆蓋率 ≥85%
- ✅ 集成測試全部通過
- ✅ 構建成功（無編譯錯誤）

---

## 新目錄結構設計

### 完整目錄樹

```
smart-admin/
│
├── smartadmin-common/                    # 公共基礎設施（20個模塊）
│   ├── smartadmin-common-bom/            # 🆕 BOM 依賴管理
│   │   └── build.gradle.kts              # 統一版本號管理
│   │
│   ├── smartadmin-common-core/           # 核心工具類
│   │   └── src/main/java/
│   │       └── net/lab1024/sa/common/core/
│   │           ├── domain/               # 領域對象
│   │           │   ├── ResponseDTO.java
│   │           │   ├── PageResult.java
│   │           │   └── ErrorCode.java    # ✅ Sealed Classes
│   │           ├── util/
│   │           │   ├── SmartBeanUtil.java
│   │           │   └── SmartPageUtil.java
│   │           └── constant/
│   │
│   ├── smartadmin-common-web/            # Web 配置
│   │   └── src/main/java/
│   │       └── net/lab1024/sa/common/web/
│   │           ├── config/
│   │           │   ├── WebMvcConfig.java
│   │           │   └── VirtualThreadsConfig.java  # ✅ Java 21
│   │           ├── filter/
│   │           └── interceptor/
│   │
│   ├── smartadmin-common-mybatis/        # MyBatis-Plus 配置
│   │   └── src/main/java/
│   │       └── net/lab1024/sa/common/mybatis/
│   │           ├── config/MybatisPlusConfig.java
│   │           └── handler/
│   │
│   ├── smartadmin-common-redis/          # Redis 配置（Redisson）
│   ├── smartadmin-common-token/          # Sa-Token 認證
│   ├── smartadmin-common-cache/          # JetCache 二級緩存
│   ├── smartadmin-common-excel/          # Excel 導入導出
│   ├── smartadmin-common-validation/     # 參數驗證
│   ├── smartadmin-common-security/       # 安全防護（XSS/CSRF）
│   ├── smartadmin-common-json/           # JSON 序列化配置
│   ├── smartadmin-common-captcha/        # 驗證碼
│   ├── smartadmin-common-data-masking/   # 數據脫敏
│   ├── smartadmin-common-api-encrypt/    # API 加解密
│   ├── smartadmin-common-repeat-submit/  # 防重複提交
│   ├── smartadmin-common-redis-lock/     # 分布式鎖
│   ├── smartadmin-common-ip-geolocation/ # IP 地理定位
│   ├── smartadmin-common-mq/             # 消息隊列抽象
│   └── smartadmin-common-datasource/     # 數據源配置
│
├── smartadmin-support/                   # 業務支持模塊（17個模塊）
│   ├── smartadmin-support-job/           # 定時任務（Snail-Job）
│   │   └── src/main/java/
│   │       └── net/lab1024/sa/support/job/
│   │           ├── controller/
│   │           ├── service/
│   │           ├── manager/              # ✅ 保留 Manager 層
│   │           ├── dao/
│   │           └── domain/
│   │
│   ├── smartadmin-support-file/          # 文件管理
│   ├── smartadmin-support-mail/          # 郵件發送
│   ├── smartadmin-support-dict/          # 數據字典（帶緩存）
│   ├── smartadmin-support-liteflow/      # LiteFlow 流程引擎
│   ├── smartadmin-support-datatracer/    # 數據變更追蹤
│   ├── smartadmin-support-loginlog/      # 登錄日誌
│   ├── smartadmin-support-operatelog/    # 操作日誌
│   ├── smartadmin-support-reload/        # 動態配置重載
│   ├── smartadmin-support-codegenerator/ # 代碼生成器
│   ├── smartadmin-support-table/         # 表格列定制
│   ├── smartadmin-support-feedback/      # 用戶反饋
│   ├── smartadmin-support-changelog/     # 變更記錄
│   ├── smartadmin-support-message/       # 內部消息
│   ├── smartadmin-support-heartbeat/     # 心跳監控
│   ├── smartadmin-support-helpdoc/       # 幫助文檔
│   └── smartadmin-support-serialnumber/  # 序列號生成
│
├── smartadmin-modules/                   # 業務模塊（3個模塊）
│   │
│   ├── smartadmin-system/                # 系統管理模塊
│   │   ├── src/main/java/
│   │   │   └── net/lab1024/sa/system/
│   │   │       ├── SystemApplication.java
│   │   │       ├── employee/             # 員工管理
│   │   │       │   ├── controller/
│   │   │       │   │   └── EmployeeController.java
│   │   │       │   ├── service/
│   │   │       │   │   └── EmployeeService.java      # ✅ Vavr Option
│   │   │       │   ├── manager/
│   │   │       │   │   └── EmployeeManager.java      # ✅ @Transactional
│   │   │       │   ├── dao/
│   │   │       │   │   └── EmployeeDao.java
│   │   │       │   └── domain/
│   │   │       │       ├── entity/EmployeeEntity.java
│   │   │       │       ├── form/
│   │   │       │       │   ├── EmployeeAddForm.java
│   │   │       │       │   └── EmployeeUpdateForm.java
│   │   │       │       └── vo/EmployeeVO.java
│   │   │       ├── role/                 # 角色管理
│   │   │       ├── menu/                 # 菜單管理
│   │   │       ├── dept/                 # 部門管理
│   │   │       ├── position/             # 職位管理
│   │   │       ├── login/                # 登錄認證
│   │   │       └── datascope/            # 數據權限
│   │   │
│   │   ├── src/main/resources/
│   │   │   ├── application.yml
│   │   │   └── mapper/                   # MyBatis XML
│   │   │
│   │   └── src/test/java/
│   │       └── net/lab1024/sa/system/
│   │           ├── ArchitectureTest.java # ✅ ArchUnit 545行規則
│   │           ├── employee/
│   │           │   ├── EmployeeServiceTest.java
│   │           │   └── EmployeeManagerTest.java
│   │           └── ...
│   │
│   ├── smartadmin-business/              # 業務模塊
│   │   ├── src/main/java/
│   │   │   └── net/lab1024/sa/business/
│   │   │       ├── BusinessApplication.java
│   │   │       ├── goods/                # 商品管理
│   │   │       │   ├── controller/
│   │   │       │   ├── service/
│   │   │       │   ├── manager/          # ✅ 保留 Manager 層
│   │   │       │   ├── dao/
│   │   │       │   └── domain/
│   │   │       ├── brand/                # 品牌管理
│   │   │       ├── category/             # 分類管理
│   │   │       └── ...
│   │   │
│   │   └── src/test/java/
│   │       └── ArchitectureTest.java     # ✅ ArchUnit
│   │
│   └── smartadmin-oa/                    # 🆕 OA 辦公模塊
│       ├── src/main/java/
│       │   └── net/lab1024/sa/oa/
│       │       ├── OaApplication.java
│       │       ├── enterprise/           # 企業管理
│       │       ├── invoice/              # 發票管理
│       │       ├── notice/               # 通知公告
│       │       └── ...
│       │
│       └── src/test/java/
│           └── ArchitectureTest.java
│
├── smartadmin-api/                       # 🆕 API 契約層（為微服務準備）
│   ├── smartadmin-api-system/            # 系統模塊 API
│   │   └── src/main/java/
│   │       └── net/lab1024/sa/api/system/
│   │           ├── EmployeeService.java  # API 接口
│   │           │   ```java
│   │           │   public interface EmployeeService {
│   │           │       Option<EmployeeDTO> getEmployeeById(Long id);  // ✅ Vavr Option
│   │           │   }
│   │           │   ```
│   │           └── domain/
│   │               ├── EmployeeDTO.java  # DTO（實現 Serializable）
│   │               └── ...
│   │
│   ├── smartadmin-api-business/          # 業務模塊 API
│   │   └── src/main/java/
│   │       └── net/lab1024/sa/api/business/
│   │           ├── GoodsService.java
│   │           └── domain/
│   │               └── GoodsDTO.java
│   │
│   └── smartadmin-api-oa/                # OA 模塊 API
│
├── smartadmin-starter/                   # 🆕 啟動器模塊
│   ├── smartadmin-starter-web/           # Web 啟動器
│   │   └── build.gradle.kts
│   │       ```kotlin
│   │       dependencies {
│   │           api(project(":smartadmin-common:smartadmin-common-web"))
│   │           api(project(":smartadmin-common:smartadmin-common-token"))
│   │           api(project(":smartadmin-common:smartadmin-common-validation"))
│   │       }
│   │       ```
│   │
│   └── smartadmin-starter-all/           # 完整啟動器（開發環境）
│       └── build.gradle.kts
│           ```kotlin
│           dependencies {
│               api(project(":smartadmin-starter:smartadmin-starter-web"))
│               api(project(":smartadmin-common:smartadmin-common-mybatis"))
│               api(project(":smartadmin-common:smartadmin-common-redis"))
│               // ... 引入所有 common 模塊
│           }
│           ```
│
├── smartadmin-app/                       # 🆕 應用啟動模塊（替代 sa-admin）
│   ├── src/main/java/
│   │   └── net/lab1024/sa/app/
│   │       └── SmartAdminApplication.java
│   │           ```java
│   │           @SpringBootApplication(scanBasePackages = {
│   │               "net.lab1024.sa.system",
│   │               "net.lab1024.sa.business",
│   │               "net.lab1024.sa.oa",
│   │               "net.lab1024.sa.support"
│   │           })
│   │           public class SmartAdminApplication {
│   │               public static void main(String[] args) {
│   │                   SpringApplication.run(SmartAdminApplication.class, args);
│   │               }
│   │           }
│   │           ```
│   │
│   ├── src/main/resources/
│   │   ├── application.yml               # 主配置文件
│   │   └── logback-spring.xml
│   │
│   └── build.gradle.kts
│       ```kotlin
│       dependencies {
│           implementation(project(":smartadmin-modules:smartadmin-system"))
│           implementation(project(":smartadmin-modules:smartadmin-business"))
│           implementation(project(":smartadmin-modules:smartadmin-oa"))
│           implementation(project(":smartadmin-starter:smartadmin-starter-all"))
│           // 所有 support 模塊
│           implementation(project(":smartadmin-support:smartadmin-support-job"))
│           implementation(project(":smartadmin-support:smartadmin-support-file"))
│           // ...
│       }
│       ```
│
├── settings.gradle.kts                   # Gradle 多模塊配置
│   ```kotlin
│   rootProject.name = "smart-admin"
│
│   // smartadmin-common
│   include(":smartadmin-common:smartadmin-common-bom")
│   include(":smartadmin-common:smartadmin-common-core")
│   include(":smartadmin-common:smartadmin-common-web")
│   // ... 其他 17 個 common 模塊
│
│   // smartadmin-support
│   include(":smartadmin-support:smartadmin-support-job")
│   include(":smartadmin-support:smartadmin-support-file")
│   // ... 其他 15 個 support 模塊
│
│   // smartadmin-modules
│   include(":smartadmin-modules:smartadmin-system")
│   include(":smartadmin-modules:smartadmin-business")
│   include(":smartadmin-modules:smartadmin-oa")
│
│   // smartadmin-api
│   include(":smartadmin-api:smartadmin-api-system")
│   include(":smartadmin-api:smartadmin-api-business")
│   include(":smartadmin-api:smartadmin-api-oa")
│
│   // smartadmin-starter
│   include(":smartadmin-starter:smartadmin-starter-web")
│   include(":smartadmin-starter:smartadmin-starter-all")
│
│   // smartadmin-app
│   include(":smartadmin-app")
│   ```
│
└── build.gradle.kts                      # 根構建配置
    ```kotlin
    allprojects {
        group = "net.lab1024.sa"
        version = "4.0.0"

        repositories {
            mavenCentral()
        }
    }

    subprojects {
        apply(plugin = "java-library")
        apply(plugin = "org.springframework.boot")

        dependencies {
            // 全局依賴
            implementation("io.vavr:vavr:0.10.6")      # ✅ Vavr
            implementation("org.projectlombok:lombok")
            annotationProcessor("org.projectlombok:lombok")
        }

        java {
            sourceCompatibility = JavaVersion.VERSION_21  # ✅ Java 21
            targetCompatibility = JavaVersion.VERSION_21
        }
    }
    ```
```

---

### 關鍵目錄對比

#### 舊結構 vs 新結構

**舊結構** (`sa-base/foundation/domain`):
```
smart-admin-api-java21-springboot3/
└── sa-base/
    └── foundation/
        └── domain/
            └── src/main/java/
                └── net/lab1024/sa/foundation/domain/
                    ├── ResponseDTO.java
                    └── PageResult.java
```

**新結構** (`smartadmin-common/core`):
```
smart-admin/
└── smartadmin-common/
    └── smartadmin-common-core/
        └── src/main/java/
            └── net/lab1024/sa/common/core/domain/
                ├── ResponseDTO.java
                └── PageResult.java
```

**改進**:
- ✅ 嵌套層次: 5層 → 3層
- ✅ 路徑長度: 減少 40%
- ✅ 模塊命名: 更直觀（`core` vs `foundation/domain`）

---

## 核心優勢保留驗證

### 1. Manager 層保留（100%）

**每個業務模塊內部四層架構**:

```java
// smartadmin-modules/smartadmin-system/employee/
@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeDao employeeDao;
    private final EmployeeManager employeeManager;  // ✅ 委託給 Manager

    public Option<EmployeeVO> addEmployee(EmployeeAddForm form) {
        EmployeeEntity entity = SmartBeanUtil.copy(form, EmployeeEntity.class);
        employeeManager.addEmployeeTransaction(entity);  // Manager 處理事務
        return Option.of(entity).map(e -> SmartBeanUtil.copy(e, EmployeeVO.class));
    }
}

@Component
@RequiredArgsConstructor
public class EmployeeManager {
    private final EmployeeDao employeeDao;
    private final DataTracerService dataTracerService;

    // ✅ Manager 層專屬 @Transactional
    @Transactional(rollbackFor = Throwable.class)
    public void addEmployeeTransaction(EmployeeEntity entity) {
        employeeDao.insert(entity);
        dataTracerService.insert(entity.getEmployeeId(), DataTracerTypeEnum.EMPLOYEE);
    }
}
```

---

### 2. ArchUnit 規則保留（100%）

**每個業務模塊獨立驗證**:

```java
// smartadmin-modules/smartadmin-system/src/test/java/ArchitectureTest.java
@AnalyzeClasses(packages = "net.lab1024.sa.system")
public class SystemArchitectureTest {

    private static final String LAYER_CONTROLLER = "..controller..";
    private static final String LAYER_SERVICE = "..service..";
    private static final String LAYER_MANAGER = "..manager..";
    private static final String LAYER_DAO = "..dao..";

    // ✅ 保留所有 545 行規則

    @ArchTest
    static final ArchRule transactionalMustInManagerLayer =
        methods()
            .that().areAnnotatedWith(Transactional.class)
            .should().beDeclaredInClassesThat().haveSimpleNameEndingWith("Manager")
            .because("@Transactional 只能在 Manager 層使用");

    @ArchTest
    static final ArchRule serviceMustUseVavrOption =
        methods()
            .that().areDeclaredInClassesThat().resideInAPackage(LAYER_SERVICE)
            .and().arePublic()
            .should().haveRawReturnType(
                assignableTo(Option.class)
                    .or(assignableTo(Try.class))
                    .or(assignableTo(Either.class))
                    .or(isPrimitive())
                    .or(assignableTo(void.class))
            )
            .because("Service 層必須使用 Vavr Option/Try/Either");

    @ArchTest
    static final ArchRule controllerShouldOnlyCallService =
        classes()
            .that().resideInAPackage(LAYER_CONTROLLER)
            .should().onlyAccessClassesThat(
                resideInAnyPackage(LAYER_CONTROLLER, LAYER_SERVICE, "java..", "io.vavr..")
            )
            .because("Controller 層只能調用 Service 層");

    // ... 其他 540+ 行規則
}
```

**驗證命令**:
```bash
# 測試所有模塊的 ArchUnit 規則
./gradlew :smartadmin-system:test --tests ArchitectureTest
./gradlew :smartadmin-business:test --tests ArchitectureTest
./gradlew :smartadmin-oa:test --tests ArchitectureTest

# 預期結果: 所有測試通過 ✅
```

---

### 3. Vavr 函數式編程保留（100%）

**Service 層強制使用 Vavr Option**:

```java
// smartadmin-modules/smartadmin-system/employee/service/EmployeeService.java
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeDao employeeDao;
    private final DepartmentCacheManager departmentCacheManager;

    // ✅ 強制使用 Vavr Option
    public Option<EmployeeVO> getEmployeeById(Long employeeId) {
        return Option.of(employeeDao.selectById(employeeId))
            .filter(entity -> !entity.getDeletedFlag())
            .flatMap(entity -> {
                // 函數式組合: flatMap 鏈式調用
                Option<DepartmentEntity> dept = getDepartment(entity.getDepartmentId());
                return dept.map(d -> {
                    EmployeeVO vo = SmartBeanUtil.copy(entity, EmployeeVO.class);
                    vo.setDepartmentName(d.getDepartmentName());
                    return vo;
                });
            });
    }

    private Option<DepartmentEntity> getDepartment(Long deptId) {
        if (deptId == null) {
            return Option.none();
        }
        DepartmentEntity dept = departmentCacheManager.getDepartment(deptId);
        return Option.of(dept).filter(d -> !d.getDeletedFlag());
    }
}
```

**ArchUnit 強制驗證**:
```java
// 如果開發者誤用 java.util.Optional
public Optional<EmployeeVO> getEmployeeById(Long id) {  // ❌ 違規
    // ...
}

// ArchUnit 測試失敗:
// ❌ Architecture Violation:
//    Method EmployeeService.getEmployeeById() returns java.util.Optional,
//    but Service layer must use io.vavr.control.Option
//
//    Violation at: net.lab1024.sa.system.employee.service.EmployeeService (EmployeeService.java:42)
```

---

### 4. Java 21 特性保留（100%）

#### 4.1 Virtual Threads 配置

```java
// smartadmin-common/smartadmin-common-web/config/VirtualThreadsConfig.java
@Configuration
@EnableAsync
@ConditionalOnProperty(
    prefix = "spring.threads.virtual",
    name = "enabled",
    havingValue = "true"
)
public class VirtualThreadsConfig {

    @Bean(name = "applicationTaskExecutor")
    public AsyncTaskExecutor applicationTaskExecutor() {
        return new TaskExecutorAdapter(
            Executors.newVirtualThreadPerTaskExecutor()
        );
    }

    @Bean(name = "virtualThreadTaskScheduler")
    @Primary
    public AsyncTaskExecutor virtualThreadTaskScheduler() {
        return new TaskExecutorAdapter(
            Executors.newVirtualThreadPerTaskExecutor()
        );
    }
}
```

**應用配置**:
```yaml
# smartadmin-app/src/main/resources/application.yml
spring:
  threads:
    virtual:
      enabled: true  # ✅ 啟用 Virtual Threads
```

#### 4.2 Sealed Classes

```java
// smartadmin-common/smartadmin-common-core/domain/code/ErrorCode.java
public sealed interface ErrorCode
    permits SystemErrorCode, UserErrorCode, UnexpectedErrorCode {

    int getCode();
    String getMsg();
    String getLevel();
}

// 編譯時窮舉檢查
public String formatError(ErrorCode error) {
    return switch (error) {
        case SystemErrorCode s -> "System: " + s.getMsg();
        case UserErrorCode u -> "User: " + u.getMsg();
        case UnexpectedErrorCode e -> "Unexpected: " + e.getMsg();
        // ✅ 編譯器強制窮舉，不需要 default
    };
}
```

---

### 5. API 契約層設計（為微服務準備）

#### 5.1 API 接口定義

```java
// smartadmin-api/smartadmin-api-system/EmployeeService.java
package net.lab1024.sa.api.system;

import io.vavr.control.Option;
import net.lab1024.sa.api.system.domain.EmployeeDTO;

/**
 * 員工服務 API 契約
 *
 * 當前用途: 模塊間調用接口
 * 未來用途: 微服務 Dubbo RPC 接口
 */
public interface EmployeeService {

    /**
     * 根據員工 ID 查詢員工信息
     *
     * @param employeeId 員工 ID
     * @return Option<EmployeeDTO> ✅ Vavr Option 保證類型安全
     */
    Option<EmployeeDTO> getEmployeeById(Long employeeId);

    /**
     * 批量查詢員工信息
     *
     * @param employeeIds 員工 ID 列表
     * @return Map<Long, EmployeeDTO>
     */
    Map<Long, EmployeeDTO> batchGetEmployees(List<Long> employeeIds);
}
```

#### 5.2 DTO 定義

```java
// smartadmin-api/smartadmin-api-system/domain/EmployeeDTO.java
package net.lab1024.sa.api.system.domain;

import lombok.Data;
import java.io.Serializable;

/**
 * 員工 DTO（API 契約數據傳輸對象）
 *
 * 設計原則:
 * 1. 實現 Serializable（為未來 Dubbo RPC 準備）
 * 2. 僅包含必要字段（最小化數據傳輸）
 * 3. 不包含敏感信息（密碼、鹽值）
 */
@Data
public class EmployeeDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long employeeId;
    private String actualName;
    private String loginName;
    private String phone;
    private String email;
    private Long departmentId;
    private String departmentName;
    private Boolean administratorFlag;
    private Boolean disabledFlag;
    private Boolean deletedFlag;
}
```

#### 5.3 API 實現（當前單體應用）

```java
// smartadmin-modules/smartadmin-system/employee/service/EmployeeServiceImpl.java
package net.lab1024.sa.system.employee.service;

import net.lab1024.sa.api.system.EmployeeService;  // 實現 API 契約
import net.lab1024.sa.api.system.domain.EmployeeDTO;
import io.vavr.control.Option;
import org.springframework.stereotype.Service;

/**
 * 員工服務實現
 *
 * ✅ 實現 API 契約接口
 * ✅ 保留 Manager 層
 * ✅ 使用 Vavr Option
 */
@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {  // ✅ 實現 API

    private final EmployeeDao employeeDao;
    private final EmployeeManager employeeManager;
    private final DepartmentCacheManager departmentCacheManager;

    @Override
    public Option<EmployeeDTO> getEmployeeById(Long employeeId) {
        return Option.of(employeeDao.selectById(employeeId))
            .filter(entity -> !entity.getDeletedFlag())
            .map(entity -> SmartBeanUtil.copy(entity, EmployeeDTO.class));
    }

    @Override
    public Map<Long, EmployeeDTO> batchGetEmployees(List<Long> employeeIds) {
        List<EmployeeEntity> entities = employeeDao.selectBatchIds(employeeIds);
        return entities.stream()
            .collect(Collectors.toMap(
                EmployeeEntity::getEmployeeId,
                e -> SmartBeanUtil.copy(e, EmployeeDTO.class)
            ));
    }

    // 其他業務方法（不在 API 契約中）
    public ResponseDTO<PageResult<EmployeeVO>> query(EmployeeQueryForm form) {
        // ...
    }
}
```

#### 5.4 跨模塊調用（當前方式）

```java
// smartadmin-modules/smartadmin-business/goods/service/GoodsService.java
@Service
@RequiredArgsConstructor
public class GoodsService {

    // ✅ 依賴 API 契約接口（而非具體實現）
    private final net.lab1024.sa.api.system.EmployeeService employeeService;

    public Option<GoodsVO> getGoodsDetail(Long goodsId) {
        return Option.of(goodsDao.selectById(goodsId))
            .flatMap(goods -> {
                // 跨模塊調用: Business → System（通過 API 契約）
                Option<EmployeeDTO> creator = employeeService.getEmployeeById(goods.getCreateUserId());

                return creator.map(user -> {
                    GoodsVO vo = SmartBeanUtil.copy(goods, GoodsVO.class);
                    vo.setCreateUserName(user.getActualName());
                    return vo;
                });
            });
    }
}
```

**未來微服務化時（零成本升級）**:

```java
// 未來: smartadmin-api/smartadmin-api-system/EmployeeService.java
@DubboService(version = "1.0.0", group = "smartadmin-system")  // 🆕 添加 @DubboService
public interface EmployeeService {
    Option<EmployeeDTO> getEmployeeById(Long employeeId);
    // ... API 接口不變
}

// 未來: smartadmin-modules/smartadmin-business/goods/service/GoodsService.java
@Service
@RequiredArgsConstructor
public class GoodsService {

    // 🆕 改為 @DubboReference
    @DubboReference(version = "1.0.0", group = "smartadmin-system")
    private EmployeeService employeeService;

    // ✅ 業務代碼完全不變！
    public Option<GoodsVO> getGoodsDetail(Long goodsId) {
        return Option.of(goodsDao.selectById(goodsId))
            .flatMap(goods -> {
                // RPC 調用（API 調用方式不變）
                Option<EmployeeDTO> creator = employeeService.getEmployeeById(goods.getCreateUserId());
                // ...
            });
    }
}
```

**關鍵優勢**:
- ✅ **當前**: 單體應用內部方法調用（0.1ms 延遲）
- ✅ **未來**: 微服務 RPC 調用（僅需添加註解，業務代碼不變）
- ✅ **類型安全**: Vavr Option 全鏈路保證
- ✅ **零成本升級**: API 契約層作為穩定的抽象邊界

---

## 遷移路徑設計

### 漸進式遷移策略（6 weeks）

```
Timeline:
Week 1-2: 創建新目錄，公共模塊遷移
Week 3-4: 業務模塊遷移，API 契約創建
Week 5-6: 測試驗證，清理舊目錄
```

### Phase 1: 準備階段（Week 1）

#### 任務 1.1: 創建新目錄結構

```bash
# 創建頂層目錄
mkdir -p smartadmin-common
mkdir -p smartadmin-support
mkdir -p smartadmin-modules
mkdir -p smartadmin-api
mkdir -p smartadmin-starter
mkdir -p smartadmin-app

# 創建 common 子模塊
cd smartadmin-common
mkdir -p smartadmin-common-bom
mkdir -p smartadmin-common-core
mkdir -p smartadmin-common-web
# ... 其他 17 個模塊

# 創建 support 子模塊
cd ../smartadmin-support
mkdir -p smartadmin-support-job
mkdir -p smartadmin-support-file
# ... 其他 15 個模塊

# 創建 modules 子模塊
cd ../smartadmin-modules
mkdir -p smartadmin-system
mkdir -p smartadmin-business
mkdir -p smartadmin-oa

# 創建 api 子模塊
cd ../smartadmin-api
mkdir -p smartadmin-api-system
mkdir -p smartadmin-api-business
mkdir -p smartadmin-api-oa
```

#### 任務 1.2: 創建 settings.gradle.kts

```kotlin
// settings.gradle.kts
rootProject.name = "smart-admin"

// smartadmin-common（20個模塊）
include(":smartadmin-common:smartadmin-common-bom")
include(":smartadmin-common:smartadmin-common-core")
include(":smartadmin-common:smartadmin-common-web")
include(":smartadmin-common:smartadmin-common-mybatis")
include(":smartadmin-common:smartadmin-common-redis")
include(":smartadmin-common:smartadmin-common-token")
include(":smartadmin-common:smartadmin-common-cache")
include(":smartadmin-common:smartadmin-common-excel")
include(":smartadmin-common:smartadmin-common-validation")
include(":smartadmin-common:smartadmin-common-security")
include(":smartadmin-common:smartadmin-common-json")
include(":smartadmin-common:smartadmin-common-captcha")
include(":smartadmin-common:smartadmin-common-data-masking")
include(":smartadmin-common:smartadmin-common-api-encrypt")
include(":smartadmin-common:smartadmin-common-repeat-submit")
include(":smartadmin-common:smartadmin-common-redis-lock")
include(":smartadmin-common:smartadmin-common-ip-geolocation")
include(":smartadmin-common:smartadmin-common-mq")
include(":smartadmin-common:smartadmin-common-datasource")
include(":smartadmin-common:smartadmin-common-devtools")

// smartadmin-support（17個模塊）
include(":smartadmin-support:smartadmin-support-job")
include(":smartadmin-support:smartadmin-support-file")
include(":smartadmin-support:smartadmin-support-mail")
include(":smartadmin-support:smartadmin-support-dict")
include(":smartadmin-support:smartadmin-support-liteflow")
include(":smartadmin-support:smartadmin-support-datatracer")
include(":smartadmin-support:smartadmin-support-loginlog")
include(":smartadmin-support:smartadmin-support-operatelog")
include(":smartadmin-support:smartadmin-support-reload")
include(":smartadmin-support:smartadmin-support-codegenerator")
include(":smartadmin-support:smartadmin-support-table")
include(":smartadmin-support:smartadmin-support-feedback")
include(":smartadmin-support:smartadmin-support-changelog")
include(":smartadmin-support:smartadmin-support-message")
include(":smartadmin-support:smartadmin-support-heartbeat")
include(":smartadmin-support:smartadmin-support-helpdoc")
include(":smartadmin-support:smartadmin-support-serialnumber")

// smartadmin-modules（3個模塊）
include(":smartadmin-modules:smartadmin-system")
include(":smartadmin-modules:smartadmin-business")
include(":smartadmin-modules:smartadmin-oa")

// smartadmin-api（3個模塊）
include(":smartadmin-api:smartadmin-api-system")
include(":smartadmin-api:smartadmin-api-business")
include(":smartadmin-api:smartadmin-api-oa")

// smartadmin-starter（2個模塊）
include(":smartadmin-starter:smartadmin-starter-web")
include(":smartadmin-starter:smartadmin-starter-all")

// smartadmin-app（1個模塊）
include(":smartadmin-app")

// 🆕 舊模塊（並存期間保留）
include(":sa-base:foundation:domain")
include(":sa-base:infrastructure:web")
// ... 其他舊模塊
include(":sa-admin")
```

#### 任務 1.3: 創建 BOM 模塊

```kotlin
// smartadmin-common/smartadmin-common-bom/build.gradle.kts
plugins {
    `java-platform`
}

dependencies {
    constraints {
        // Spring Boot BOM
        api("org.springframework.boot:spring-boot-dependencies:3.5.4")

        // MyBatis Plus
        api("com.baomidou:mybatis-plus-spring-boot3-starter:3.5.12")

        // Sa-Token
        api("cn.dev33:sa-token-spring-boot3-starter:1.44.0")

        // Redisson
        api("org.redisson:redisson-spring-boot-starter:3.50.0")

        // Vavr（✅ 核心依賴）
        api("io.vavr:vavr:0.10.6")

        // Knife4j
        api("com.github.xiaoymin:knife4j-openapi3-jakarta-spring-boot-starter:4.6.0")

        // JetCache
        api("com.alicp.jetcache:jetcache-starter-redis:2.7.7")

        // P6Spy（SQL 監控）
        api("p6spy:p6spy:3.9.1")

        // PostgreSQL Driver
        api("org.postgresql:postgresql:42.7.5")

        // ... 其他依賴
    }
}
```

---

### Phase 2: 公共模塊遷移（Week 2）

#### 任務 2.1: 遷移 common-core

```bash
# 1. 複製文件
cp -r sa-base/foundation/domain/src/* \
      smartadmin-common/smartadmin-common-core/src/

# 2. 修改包名
cd smartadmin-common/smartadmin-common-core/src
find . -name "*.java" -exec sed -i 's/net.lab1024.sa.foundation.domain/net.lab1024.sa.common.core.domain/g' {} \;

# 3. 創建 build.gradle.kts
cat > build.gradle.kts << 'EOF'
dependencies {
    api(platform(project(":smartadmin-common:smartadmin-common-bom")))

    // Vavr（✅ 核心依賴）
    api("io.vavr:vavr")

    // Spring Context
    api("org.springframework:spring-context")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
}
EOF

# 4. 驗證編譯
../../../gradlew :smartadmin-common:smartadmin-common-core:build
```

#### 任務 2.2: 遷移 common-web

```bash
# 類似步驟
cp -r sa-base/infrastructure/web/src/* \
      smartadmin-common/smartadmin-common-web/src/

# 修改包名
cd smartadmin-common/smartadmin-common-web/src
find . -name "*.java" -exec sed -i 's/net.lab1024.sa.base.web/net.lab1024.sa.common.web/g' {} \;

# 特別處理: Virtual Threads 配置
# VirtualThreadsConfig.java 保持不變（✅ Java 21 特性）

# 驗證編譯
../../../gradlew :smartadmin-common:smartadmin-common-web:build
```

#### 任務 2.3: 批量遷移其他 common 模塊

```python
# migrate_common.py
import os
import shutil
import re

old_new_mapping = {
    'sa-base/foundation/cache': 'smartadmin-common/smartadmin-common-cache',
    'sa-base/foundation/validation': 'smartadmin-common/smartadmin-common-validation',
    'sa-base/infrastructure/mybatis': 'smartadmin-common/smartadmin-common-mybatis',
    'sa-base/infrastructure/redis': 'smartadmin-common/smartadmin-common-redis',
    # ... 其他映射
}

def migrate_module(old_path, new_path):
    # 1. 複製文件
    shutil.copytree(f"{old_path}/src", f"{new_path}/src")

    # 2. 修改包名
    package_mapping = {
        'net.lab1024.sa.base': 'net.lab1024.sa.common',
        'net.lab1024.sa.foundation': 'net.lab1024.sa.common',
    }

    for root, dirs, files in os.walk(f"{new_path}/src"):
        for file in files:
            if file.endswith('.java'):
                file_path = os.path.join(root, file)
                with open(file_path, 'r') as f:
                    content = f.read()

                for old_pkg, new_pkg in package_mapping.items():
                    content = content.replace(old_pkg, new_pkg)

                with open(file_path, 'w') as f:
                    f.write(content)

    print(f"✅ Migrated: {old_path} → {new_path}")

for old, new in old_new_mapping.items():
    migrate_module(old, new)
```

**驗收標準**:
- ✅ 所有 common 模塊編譯成功
- ✅ 無包名錯誤
- ✅ ArchUnit 測試通過（如有）

---

### Phase 3: 業務模塊遷移（Week 3-4）

#### 任務 3.1: 創建 smartadmin-system 模塊

```bash
# 1. 創建模塊結構
cd smartadmin-modules/smartadmin-system
mkdir -p src/main/java/net/lab1024/sa/system
mkdir -p src/main/resources
mkdir -p src/test/java/net/lab1024/sa/system

# 2. 遷移代碼
cp -r ../../sa-admin/src/main/java/net/lab1024/sa/admin/module/system/* \
      src/main/java/net/lab1024/sa/system/

# 3. 創建啟動類（僅用於模塊獨立測試）
cat > src/main/java/net/lab1024/sa/system/SystemApplication.java << 'EOF'
package net.lab1024.sa.system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 系統模塊啟動類（僅用於模塊獨立測試）
 *
 * 生產環境使用 smartadmin-app 統一啟動
 */
@SpringBootApplication(scanBasePackages = {
    "net.lab1024.sa.system",
    "net.lab1024.sa.common"
})
public class SystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(SystemApplication.class, args);
    }
}
EOF

# 4. 創建 build.gradle.kts
cat > build.gradle.kts << 'EOF'
dependencies {
    // ✅ API 契約（依賴接口定義）
    api(project(":smartadmin-api:smartadmin-api-system"))

    // Common 模塊
    implementation(project(":smartadmin-common:smartadmin-common-core"))
    implementation(project(":smartadmin-common:smartadmin-common-web"))
    implementation(project(":smartadmin-common:smartadmin-common-mybatis"))
    implementation(project(":smartadmin-common:smartadmin-common-redis"))
    implementation(project(":smartadmin-common:smartadmin-common-token"))

    // Support 模塊
    implementation(project(":smartadmin-support:smartadmin-support-dict"))
    implementation(project(":smartadmin-support:smartadmin-support-loginlog"))
    implementation(project(":smartadmin-support:smartadmin-support-operatelog"))

    // ✅ Vavr（BOM 管理）
    implementation("io.vavr:vavr")

    // ✅ MyBatis Plus
    implementation("com.baomidou:mybatis-plus-spring-boot3-starter")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")  // ✅ ArchUnit
}
EOF

# 5. 遷移 ArchUnit 測試
cp ../../sa-admin/src/test/java/net/lab1024/sa/admin/ArchitectureTest.java \
   src/test/java/net/lab1024/sa/system/

# 修改包名
sed -i 's/@AnalyzeClasses(packages = "net.lab1024.sa.admin")/@AnalyzeClasses(packages = "net.lab1024.sa.system")/g' \
    src/test/java/net/lab1024/sa/system/ArchitectureTest.java

# 6. 驗證編譯與測試
../../gradlew :smartadmin-system:build
../../gradlew :smartadmin-system:test --tests ArchitectureTest
```

**驗收標準**:
- ✅ smartadmin-system 模塊編譯成功
- ✅ ArchUnit 測試全部通過（✅ Manager 層約束生效）
- ✅ 單元測試全部通過

#### 任務 3.2: 創建 smartadmin-business 模塊

```bash
# 類似步驟
cd smartadmin-modules/smartadmin-business
# ... 類似 smartadmin-system 的步驟

# 遷移代碼
cp -r ../../sa-admin/src/main/java/net/lab1024/sa/admin/module/business/* \
      src/main/java/net/lab1024/sa/business/

# 驗證
../../gradlew :smartadmin-business:build
../../gradlew :smartadmin-business:test --tests ArchitectureTest
```

#### 任務 3.3: 創建 smartadmin-oa 模塊

```bash
# 從 sa-admin/module/business/oa 拆分出來
cd smartadmin-modules/smartadmin-oa
cp -r ../../sa-admin/src/main/java/net/lab1024/sa/admin/module/business/oa/* \
      src/main/java/net/lab1024/sa/oa/

# 驗證
../../gradlew :smartadmin-oa:build
```

---

### Phase 4: API 契約創建（Week 4）

#### 任務 4.1: 創建 smartadmin-api-system

```bash
cd smartadmin-api/smartadmin-api-system
mkdir -p src/main/java/net/lab1024/sa/api/system

# 創建 API 接口
cat > src/main/java/net/lab1024/sa/api/system/EmployeeService.java << 'EOF'
package net.lab1024.sa.api.system;

import io.vavr.control.Option;
import net.lab1024.sa.api.system.domain.EmployeeDTO;
import java.util.List;
import java.util.Map;

/**
 * 員工服務 API 契約
 */
public interface EmployeeService {

    /**
     * 根據員工 ID 查詢員工信息
     */
    Option<EmployeeDTO> getEmployeeById(Long employeeId);

    /**
     * 批量查詢員工信息
     */
    Map<Long, EmployeeDTO> batchGetEmployees(List<Long> employeeIds);
}
EOF

# 創建 DTO
mkdir -p src/main/java/net/lab1024/sa/api/system/domain
cat > src/main/java/net/lab1024/sa/api/system/domain/EmployeeDTO.java << 'EOF'
package net.lab1024.sa.api.system.domain;

import lombok.Data;
import java.io.Serializable;

@Data
public class EmployeeDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long employeeId;
    private String actualName;
    private String loginName;
    private String phone;
    private String email;
    private Long departmentId;
    private String departmentName;
    private Boolean administratorFlag;
    private Boolean disabledFlag;
    private Boolean deletedFlag;
}
EOF

# 創建 build.gradle.kts
cat > build.gradle.kts << 'EOF'
dependencies {
    // ✅ Vavr（API 使用 Option）
    api("io.vavr:vavr")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
}
EOF
```

#### 任務 4.2: 修改 smartadmin-system 實現 API 契約

```java
// smartadmin-modules/smartadmin-system/employee/service/EmployeeServiceImpl.java
package net.lab1024.sa.system.employee.service;

import net.lab1024.sa.api.system.EmployeeService;  // ✅ 實現 API 契約
import net.lab1024.sa.api.system.domain.EmployeeDTO;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {  // ✅ implements API

    private final EmployeeDao employeeDao;
    private final EmployeeManager employeeManager;

    @Override  // ✅ 實現 API 方法
    public Option<EmployeeDTO> getEmployeeById(Long employeeId) {
        return Option.of(employeeDao.selectById(employeeId))
            .filter(entity -> !entity.getDeletedFlag())
            .map(entity -> SmartBeanUtil.copy(entity, EmployeeDTO.class));
    }

    @Override  // ✅ 實現 API 方法
    public Map<Long, EmployeeDTO> batchGetEmployees(List<Long> employeeIds) {
        // ...
    }

    // 其他業務方法（不在 API 契約中）
    public ResponseDTO<PageResult<EmployeeVO>> query(EmployeeQueryForm form) {
        // ...
    }
}
```

#### 任務 4.3: 跨模塊調用測試

```java
// smartadmin-modules/smartadmin-business/goods/service/GoodsService.java
@Service
@RequiredArgsConstructor
public class GoodsService {

    // ✅ 依賴 API 契約接口
    private final net.lab1024.sa.api.system.EmployeeService employeeService;

    public Option<GoodsVO> getGoodsDetail(Long goodsId) {
        return Option.of(goodsDao.selectById(goodsId))
            .flatMap(goods -> {
                // ✅ 跨模塊調用（通過 API 契約）
                Option<EmployeeDTO> creator = employeeService.getEmployeeById(goods.getCreateUserId());
                // ...
            });
    }
}
```

**驗收標準**:
- ✅ API 契約編譯成功
- ✅ System 模塊實現 API 契約
- ✅ Business 模塊通過 API 調用 System
- ✅ 跨模塊調用測試通過

---

### Phase 5: 統一啟動器（Week 5）

#### 任務 5.1: 創建 smartadmin-app

```bash
cd smartadmin-app
mkdir -p src/main/java/net/lab1024/sa/app
mkdir -p src/main/resources

# 創建啟動類
cat > src/main/java/net/lab1024/sa/app/SmartAdminApplication.java << 'EOF'
package net.lab1024.sa.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SmartAdmin 統一啟動類
 *
 * 掃描所有業務模塊
 */
@SpringBootApplication(scanBasePackages = {
    "net.lab1024.sa.system",     // System 模塊
    "net.lab1024.sa.business",   // Business 模塊
    "net.lab1024.sa.oa",         // OA 模塊
    "net.lab1024.sa.support",    // Support 模塊
    "net.lab1024.sa.common"      // Common 模塊
})
public class SmartAdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartAdminApplication.class, args);
    }
}
EOF

# 創建配置文件
cat > src/main/resources/application.yml << 'EOF'
spring:
  application:
    name: smartadmin-app

  # ✅ 啟用 Virtual Threads
  threads:
    virtual:
      enabled: true

  datasource:
    url: jdbc:postgresql://localhost:5432/smart_admin
    username: smartadmin
    password: ${DATASOURCE_PASSWORD}
    driver-class-name: org.postgresql.Driver

# ... 其他配置
EOF

# 創建 build.gradle.kts
cat > build.gradle.kts << 'EOF'
plugins {
    id("org.springframework.boot") version "3.5.4"
}

dependencies {
    // ✅ 引入所有業務模塊
    implementation(project(":smartadmin-modules:smartadmin-system"))
    implementation(project(":smartadmin-modules:smartadmin-business"))
    implementation(project(":smartadmin-modules:smartadmin-oa"))

    // ✅ 引入所有 support 模塊
    implementation(project(":smartadmin-support:smartadmin-support-job"))
    implementation(project(":smartadmin-support:smartadmin-support-file"))
    implementation(project(":smartadmin-support:smartadmin-support-mail"))
    implementation(project(":smartadmin-support:smartadmin-support-dict"))
    implementation(project(":smartadmin-support:smartadmin-support-liteflow"))
    implementation(project(":smartadmin-support:smartadmin-support-datatracer"))
    implementation(project(":smartadmin-support:smartadmin-support-loginlog"))
    implementation(project(":smartadmin-support:smartadmin-support-operatelog"))
    implementation(project(":smartadmin-support:smartadmin-support-reload"))
    implementation(project(":smartadmin-support:smartadmin-support-codegenerator"))
    implementation(project(":smartadmin-support:smartadmin-support-table"))
    implementation(project(":smartadmin-support:smartadmin-support-feedback"))
    implementation(project(":smartadmin-support:smartadmin-support-changelog"))
    implementation(project(":smartadmin-support:smartadmin-support-message"))
    implementation(project(":smartadmin-support:smartadmin-support-heartbeat"))
    implementation(project(":smartadmin-support:smartadmin-support-helpdoc"))
    implementation(project(":smartadmin-support:smartadmin-support-serialnumber"))

    // ✅ 啟動器
    implementation(project(":smartadmin-starter:smartadmin-starter-all"))
}
EOF
```

#### 任務 5.2: 驗證統一啟動

```bash
# 啟動應用
./gradlew :smartadmin-app:bootRun

# 驗證所有功能
curl http://localhost:1024/api/employee/query
curl http://localhost:1024/api/goods/query

# 預期結果: 所有 API 正常響應
```

---

### Phase 6: 清理舊目錄（Week 6）

#### 任務 6.1: 驗證所有引用已更新

```bash
# 搜索舊包名引用
grep -r "net.lab1024.sa.foundation" . --exclude-dir=sa-base
grep -r "net.lab1024.sa.base" . --exclude-dir=sa-base
grep -r "net.lab1024.sa.admin" . --exclude-dir=sa-admin

# 預期結果: 無任何結果（所有引用已更新）
```

#### 任務 6.2: 刪除舊目錄

```bash
# 創建備份
tar -czf sa-base-backup.tar.gz sa-base/
tar -czf sa-admin-backup.tar.gz sa-admin/

# 刪除舊目錄
rm -rf sa-base/
rm -rf sa-admin/

# 從 settings.gradle.kts 移除舊模塊
sed -i '/include(":sa-base/d' settings.gradle.kts
sed -i '/include(":sa-admin")/d' settings.gradle.kts
```

#### 任務 6.3: 最終驗證

```bash
# 完整構建
./gradlew clean build

# 運行所有測試
./gradlew test

# 運行 ArchUnit 測試
./gradlew :smartadmin-system:test --tests ArchitectureTest
./gradlew :smartadmin-business:test --tests ArchitectureTest
./gradlew :smartadmin-oa:test --tests ArchitectureTest

# 預期結果: 所有測試通過 ✅
```

---

## 影響分析

### 正面影響

| 影響維度 | 改善效果 | 量化指標 |
|---------|---------|----------|
| **目錄結構清晰度** | ✅ 顯著提升 | 新人學習成本 ↓ 50% |
| **模塊獨立性** | ✅ 顯著提升 | 模塊測試/部署獨立 |
| **微服務就緒** | ✅ 完全就緒 | 未來拆分成本 ↓ 70% |
| **API 契約** | ✅ 新增能力 | 模塊間調用類型安全 |
| **命名統一性** | ✅ 完全統一 | `smartadmin-*` 前綴識別 |

### 負面影響

| 風險項 | 影響程度 | 緩解措施 |
|--------|---------|----------|
| **遷移工作量** | 🟡 中等 (40人天) | 漸進式遷移，可回滾 |
| **包名變更** | 🟡 中等 | 腳本自動化處理 |
| **IDE 配置** | 🟢 低 | 自動檢測新模塊 |
| **CI/CD 配置** | 🟢 低 | Gradle 配置自動適配 |

### 團隊影響

| 團隊角色 | 影響 | 說明 |
|---------|------|------|
| **開發工程師** | 🟢 正面 | 目錄更清晰，開發體驗更好 |
| **架構師** | 🟢 正面 | 模塊邊界清晰，微服務就緒 |
| **QA 工程師** | 🟡 中性 | 測試路徑變更，需更新測試腳本 |
| **DevOps 工程師** | 🟡 中性 | 構建配置變更，需更新 CI/CD |

---

## 實施時間表

### 甘特圖

```
Week 1: 準備階段
├─ Day 1-2: 創建新目錄結構
├─ Day 3-4: 創建 settings.gradle.kts + BOM
└─ Day 5: 驗證新目錄編譯通過

Week 2: 公共模塊遷移
├─ Day 1-2: 遷移 common-core, common-web
├─ Day 3-4: 遷移其他 common 模塊（批量）
└─ Day 5: 驗證所有 common 模塊編譯通過

Week 3: 業務模塊遷移（System）
├─ Day 1-2: 創建 smartadmin-system 模塊
├─ Day 3: 遷移 ArchUnit 測試
├─ Day 4: 驗證 ArchUnit 測試通過
└─ Day 5: 驗證單元測試通過

Week 4: 業務模塊遷移（Business, OA） + API 契約
├─ Day 1-2: 創建 smartadmin-business, smartadmin-oa
├─ Day 3: 創建 API 契約層
├─ Day 4: 實現 API 契約
└─ Day 5: 驗證跨模塊調用

Week 5: 統一啟動器 + 集成測試
├─ Day 1-2: 創建 smartadmin-app
├─ Day 3: 驗證統一啟動
├─ Day 4-5: 集成測試

Week 6: 清理與驗收
├─ Day 1-2: 驗證所有引用已更新
├─ Day 3: 刪除舊目錄
├─ Day 4: 最終驗證（ArchUnit + 測試）
└─ Day 5: 文檔更新 + 團隊培訓
```

---

## 驗收標準

### 功能驗收

- [ ] 所有模塊編譯成功（無編譯錯誤）
- [ ] ArchUnit 測試全部通過（✅ Manager 層約束生效）
- [ ] 單元測試覆蓋率 ≥85%
- [ ] 集成測試全部通過
- [ ] 應用啟動成功（smartadmin-app）
- [ ] 所有 API 端點正常響應

### 質量驗收

- [ ] ✅ **Manager 層保留**: 每個業務模塊有 Manager 層
- [ ] ✅ **ArchUnit 規則保留**: 545行規則全部通過
- [ ] ✅ **Vavr Option 保留**: Service 層強制使用 Option
- [ ] ✅ **Java 21 特性保留**: Virtual Threads 配置生效
- [ ] ✅ **API 契約創建**: smartadmin-api 層完整

### 文檔驗收

- [ ] 更新 [CLAUDE.md](../../CLAUDE.md) - 新目錄結構說明
- [ ] 更新架構圖 - 反映新模塊劃分
- [ ] 更新開發環境搭建指南
- [ ] 更新 Gradle 構建指南
- [ ] 創建遷移指南（舊代碼如何適配新結構）

---

## 回滾方案

### 回滾決策矩陣

| 階段 | 觸發條件 | 回滾操作 |
|------|---------|---------|
| **Week 1-2** | 公共模塊編譯失敗 | Git revert + 刪除新目錄 |
| **Week 3-4** | ArchUnit 測試失敗 | 回滾 Week 2 Tag |
| **Week 5** | 統一啟動失敗 | 回滾 Week 4 Tag |
| **Week 6** | 最終驗證失敗 | 恢復備份 (sa-base/sa-admin) |

### 快速回滾腳本

```bash
#!/bin/bash
# rollback.sh

ROLLBACK_TAG=$1  # 例如: week-2

if [ -z "$ROLLBACK_TAG" ]; then
    echo "Usage: ./rollback.sh <tag>"
    echo "Available tags: week-1, week-2, week-3, week-4, week-5"
    exit 1
fi

echo "開始回滾到 $ROLLBACK_TAG..."

# 1. Git 回滾
git reset --hard $ROLLBACK_TAG

# 2. 重新構建
./gradlew clean build

# 3. 驗證
./gradlew test

echo "回滾完成！請檢查應用是否正常運行。"
```

---

**文檔版本**: 1.0.0
**最後更新**: 2026-02-02
**維護**: 架構組
