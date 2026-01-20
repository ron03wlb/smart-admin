# 常見問題與故障排除

> 整理了開發過程中常見的問題及解決方案

---

## 環境配置問題

### Q1: Port 1024 已被佔用

**問題描述**: 啟動應用時報錯 "Port 1024 is already in use"

**解決方案**:
```yaml
# 修改 sa-admin/src/main/resources/dev/application.yaml
server:
  port: 8080  # 改為其他可用埠號
```

或者找出佔用 1024 埠的進程並終止:
```bash
# macOS/Linux
lsof -i :1024
kill -9 <PID>

# Windows
netstat -ano | findstr :1024
taskkill /PID <PID> /F
```

---

### Q2: PostgreSQL 連線失敗

**問題描述**: 應用啟動時報錯 "Unable to connect to database"

**診斷步驟**:
```bash
# 1. 確認 PostgreSQL 執行狀態
cd .agent/configs
docker-compose ps postgres

# 2. 檢視 PostgreSQL 日誌
docker-compose logs postgres

# 3. 測試連線
docker exec -it smartadmin-postgres psql -U smartadmin -d smart_admin_v3
```

**常見原因**:
1. Docker 服務未啟動
2. PostgreSQL 容器未運行
3. 資料庫配置錯誤 (用戶名/密碼/資料庫名)
4. Port 衝突 (5432)

**解決方案**:
```bash
# 重啟 PostgreSQL
docker-compose restart postgres

# 重新初始化資料庫
docker-compose down
docker-compose up -d postgres
```

---

### Q3: Redis 連線失敗

**問題描述**: 應用啟動時報錯 "Unable to connect to Redis"

**解決方案**:
```bash
# 啟動 Redis
cd .agent/configs
docker-compose up -d redis

# 測試 Redis 連線
docker exec -it smartadmin-redis redis-cli ping
# 預期返回: PONG
```

**臨時解決方案** (開發階段可選):
如果不需要 Redis 功能，可以在配置中暫時禁用:
```yaml
# application.yaml
spring:
  redis:
    enabled: false  # 臨時禁用
```

---

### Q4: Maven 依賴下載緩慢

**問題描述**: Maven 下載依賴非常慢或超時

**解決方案**: 配置阿里雲鏡像

```xml
<!-- 檔案: ~/.m2/settings.xml -->
<settings>
  <mirrors>
    <mirror>
      <id>aliyun</id>
      <mirrorOf>central</mirrorOf>
      <name>Aliyun Maven</name>
      <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
  </mirrors>
</settings>
```

---

## 編譯與測試問題

### Q5: ArchUnit 測試失敗

**問題描述**: `mvn test -Dtest=ArchitectureTest` 報告違規

**常見違規類型與解決方案**:

#### 違規 1: Service 使用了 java.util.Optional
```
Error: Service layer should use Vavr Option, not java.util.Optional
```

**解決**:
```java
// ❌ 錯誤
import java.util.Optional;
public Optional<User> findById(Long id) { ... }

// ✅ 正確
import io.vavr.control.Option;
public Option<User> findById(Long id) { ... }
```

#### 違規 2: 使用了字段注入
```
Error: Field injection is not allowed, use constructor injection
```

**解決**:
```java
// ❌ 錯誤
@Service
public class UserService {
    @Autowired
    private UserDao userDao;
}

// ✅ 正確
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserDao userDao;
}
```

#### 其他常見違規

**違規 3: Controller 直接訪問 Repository**
- 錯誤：Controller 直接注入 Mapper/Dao
- 正確：必須通過 Service 層訪問
- 詳細修正方案：[10-architecture-rules.md §錯誤模式檢測](../rules/10-architecture-rules.md)

**違規 4: @Transactional 位置錯誤**
- 錯誤：在 Controller 或 Service 層使用 @Transactional
- 正確：只能在 Manager 層使用 @Transactional
- 詳細修正方案：[09-manager-layer.md](../rules/09-manager-layer.md)

**完整診斷**:
```bash
# 查看詳細違規報告
mvn test -Dtest=ArchitectureTest

# 查看具體測試方法
mvn test -Dtest=ArchitectureTest#layerDependencies
mvn test -Dtest=ArchitectureTest#serviceUsesVavrOption
mvn test -Dtest=ArchitectureTest#noFieldInjection
```

---

### Q6: JaCoCo 覆蓋率不足

**問題描述**: `mvn jacoco:check` 報告覆蓋率低於 80%

**解決方案**:
```bash
# 1. 生成覆蓋率報告
mvn clean test jacoco:report

# 2. 查看報告
open target/site/jacoco/index.html

# 3. 補充測試用例
# 針對未覆蓋的類和方法編寫測試
```

**目標**:
- Line Coverage: ≥ 80%
- Branch Coverage: ≥ 70%

---

### Q7: Checkstyle 檢查失敗

**問題描述**: `mvn checkstyle:check` 報告格式錯誤

**常見問題**:
1. 類名不符合 UpperCamelCase
2. 方法名不符合 lowerCamelCase
3. 常量不符合 UPPER_SNAKE_CASE
4. 缺少 Javadoc 註解
5. 行長度超過限制

**解決方案**:
```bash
# 查看詳細報告
mvn checkstyle:check

# 使用 IDE 自動格式化
# IDEA: Ctrl+Alt+L (Windows/Linux) 或 Cmd+Opt+L (macOS)
```

**參考**: [rules/01-naming-conventions.md](../rules/01-naming-conventions.md)

---

## 運行時問題

### Q8: NullPointerException

**問題描述**: 代碼運行時拋出 NPE

**SmartAdmin 推薦方案**: 使用 Vavr Option
```java
// ❌ 容易出現 NPE
User user = userMapper.selectById(id);
String name = user.getName();  // 如果 user 為 null，NPE!

// ✅ 使用 Option 避免 NPE
Option<User> userOpt = Option.of(userMapper.selectById(id));
String name = userOpt
    .map(User::getName)
    .getOrElse("未知用戶");
```

**參考**: [rules/08-vavr-fundamentals.md](../rules/08-vavr-fundamentals.md)

---

### Q9: 事務不生效

**問題描述**: 添加了 @Transactional 但事務沒有回滾

**常見原因**:
1. @Transactional 方法被同類內部調用 (無代理)
2. 異常被 catch 吞掉
3. rollbackFor 配置錯誤

**解決方案**:
```java
// ❌ 錯誤 1: 內部調用無效
@Service
public class UserService {
    public void methodA() {
        methodB();  // 內部調用，事務無效
    }

    @Transactional
    public void methodB() { ... }
}

// ✅ 正確: 通過注入的 Bean 調用
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserService self;  // 自我注入

    public void methodA() {
        self.methodB();  // 通過代理調用
    }

    @Transactional(rollbackFor = Exception.class)
    public void methodB() { ... }
}

// ❌ 錯誤 2: 異常被吞掉
@Transactional
public void add() {
    try {
        // 操作
    } catch (Exception e) {
        // 吞掉異常，事務不會回滾
    }
}

// ✅ 正確: 使用 Vavr Try
@Transactional(rollbackFor = Exception.class)
public Try<User> add() {
    return Try.of(() -> {
        // 操作，異常會自動傳播
    });
}
```

---

### Q10: SQL 注入風險

**問題描述**: SonarQube 報告 SQL 注入風險

**解決方案**: 使用 LambdaQueryWrapper
```java
// ❌ 有風險: 字符串拼接
String sql = "SELECT * FROM t_user WHERE name = '" + name + "'";

// ✅ 安全: LambdaQueryWrapper
LambdaQueryWrapper<User> wrapper = Wrappers.<User>lambdaQuery()
    .eq(User::getName, name);  // 自動參數綁定
List<User> users = userMapper.selectList(wrapper);
```

**參考**: [rules/09-mybatis-plus-core.md](../rules/09-mybatis-plus-core.md)

---

## 性能問題

### Q11: N+1 查詢問題

**問題描述**: 查詢列表時產生大量 SQL

**診斷**:
```bash
# 啟用 P6Spy 查看 SQL
# application.yaml
spring:
  datasource:
    driver-class-name: com.p6spy.engine.spy.P6SpyDriver
```

**解決方案**: 使用 JOIN 或批量查詢
```java
// ❌ N+1 問題
List<Order> orders = orderMapper.selectList(wrapper);
for (Order order : orders) {
    User user = userMapper.selectById(order.getUserId());  // N 次查詢
}

// ✅ 使用 JOIN
List<OrderVO> orders = orderMapper.selectOrdersWithUser(wrapper);
```

---

## 獲取幫助

如果以上方案無法解決您的問題:

1. **查看詳細規範**: [rules/](../rules/) 目錄下的相關規則文檔
2. **查看工作流程**: [workflows/](../workflows/) 目錄下的相關流程
3. **查看錯誤恢復流程**: [workflows/java-failure-recovery.md](../workflows/java-failure-recovery.md)
4. **官方文檔**: https://smartadmin.vip

---

**最後更新**: 2025-01-21
**返回**: [README.md](../README.md)
