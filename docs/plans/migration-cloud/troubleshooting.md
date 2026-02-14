# SmartAdmin 微服務遷移故障排查手冊

**文檔類型**: 故障排查參考手冊
**目標讀者**: 開發工程師、DevOps 工程師、運維團隊
**文檔版本**: 1.0.0
**創建日期**: 2026-02-03

---

## 📋 文檔目的

本文檔旨在幫助您 **30 分鐘內解決 80% 的常見問題**：
1. ✅ 快速診斷問題根因
2. ✅ 提供可執行的解決方案
3. ✅ 預防措施避免問題再次發生

---

## 🔍 快速問題定位

**按照症狀快速跳轉**：

| 症狀 | 可能原因 | 跳轉章節 |
|------|---------|---------|
| 啟動日誌顯示 `Unable to connect to Nacos server` | Nacos 連接失敗 | [1.1](#11-客戶端無法連接到-nacos-server) |
| API 返回 `403 Forbidden` | 服務註冊失敗 | [1.2](#12-服務註冊失敗403-forbidden) |
| API 返回 `404 Not Found` | Gateway 路由未匹配 | [2.1](#21-404-not-found---路由未匹配) |
| 前端請求超時 | Feign 調用超時 | [3.1](#31-feign-調用超時) |
| 分散式事務回滾失敗 | Seata 配置錯誤 | [4.1](#41-事務回滾失敗) |
| P99 延遲超過 500ms | 性能問題 | [5.1](#51-p99-延遲超過-500ms) |

---

## 1. Nacos 連接問題

### 1.1 客戶端無法連接到 Nacos Server

#### 症狀
啟動日誌顯示：
```
ERROR [Nacos-Client] Unable to connect to Nacos server: localhost:8848
java.net.ConnectException: Connection refused
```

#### 診斷步驟

**步驟 1: 檢查 Nacos Server 是否運行**

```bash
# 檢查 Nacos Server 健康狀態
curl http://localhost:8848/nacos/v1/console/health/liveness

# 預期輸出
{
  "status": "UP"
}
```

如果無法訪問，Nacos Server 可能未啟動。

**步驟 2: 檢查網絡連接**

```bash
# 測試 8848 端口是否可達
telnet localhost 8848

# 或使用 nc
nc -zv localhost 8848
```

如果連接失敗，可能是防火牆阻止或 Nacos 未啟動。

**步驟 3: 檢查配置文件**

```yaml
# application.yml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848  # 確認地址正確
        namespace: dev                # 確認命名空間存在
        group: DEFAULT_GROUP
```

#### 解決方案

**方案 1: 啟動 Nacos Server**

```bash
# Docker 啟動
docker compose up -d nacos

# 檢查日誌
docker logs -f sa21-nacos

# 驗證啟動成功
curl http://localhost:8848/nacos/
```

**方案 2: 修正 `server-addr` 配置**

```yaml
# 錯誤示例
spring:
  cloud:
    nacos:
      discovery:
        server-addr: nacos:8848  # Docker 內部網絡

# 正確示例（本地開發）
spring:
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
```

**方案 3: 檢查防火牆規則**

```bash
# Linux 檢查防火牆
sudo firewall-cmd --list-ports

# 開放 8848 端口
sudo firewall-cmd --zone=public --add-port=8848/tcp --permanent
sudo firewall-cmd --reload

# Windows 檢查防火牆
netsh advfirewall firewall show rule name=all | findstr 8848
```

#### 預防措施

**建立健康檢查腳本**

```bash
#!/bin/bash
# nacos-health-check.sh

NACOS_URL="http://localhost:8848/nacos/v1/console/health/liveness"

while true; do
    RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" $NACOS_URL)
    if [ $RESPONSE -ne 200 ]; then
        echo "[$(date)] Nacos Server is down! HTTP Status: $RESPONSE"
        # 發送告警（Email、釘釘、企業微信）
    fi
    sleep 30
done
```

---

### 1.2 服務註冊失敗（403 Forbidden）

#### 症狀
啟動日誌顯示：
```
ERROR [Nacos-Client] Failed to register service, HTTP Status: 403
com.alibaba.nacos.api.exception.NacosException: 403 Forbidden
```

#### 診斷步驟

**步驟 1: 檢查 Nacos 鑑權配置**

```bash
# 訪問 Nacos Console
open http://localhost:8848/nacos

# 檢查是否啟用了鑑權
# nacos/conf/application.properties
nacos.core.auth.enabled=true
```

**步驟 2: 檢查用戶名密碼**

```yaml
# application.yml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
        username: nacos  # 檢查用戶名
        password: nacos  # 檢查密碼
```

#### 解決方案

**方案 1: 關閉 Nacos 鑑權（開發環境）**

```properties
# nacos/conf/application.properties
nacos.core.auth.enabled=false
```

**方案 2: 配置正確的用戶名密碼**

```yaml
# application.yml
spring:
  cloud:
    nacos:
      discovery:
        username: ${NACOS_USERNAME:nacos}
        password: ${NACOS_PASSWORD:nacos}
```

**方案 3: 創建服務帳號（生產環境推薦）**

```bash
# 使用 Nacos API 創建帳號
curl -X POST 'http://localhost:8848/nacos/v1/auth/users' \
  -d 'username=smartadmin&password=SmartAdmin@2024'

# 賦予權限
curl -X POST 'http://localhost:8848/nacos/v1/auth/roles' \
  -d 'role=smartadmin&username=smartadmin'
```

---

### 1.3 命名空間不存在

#### 症狀
服務無法註冊到指定命名空間，日誌顯示：
```
WARN [Nacos-Client] Namespace 'dev' does not exist, using default namespace
```

#### 診斷步驟

**檢查命名空間列表**

```bash
# 訪問 Nacos Console
open http://localhost:8848/nacos

# 命名空間管理 → 查看列表
```

#### 解決方案

**創建命名空間**

```bash
# 使用 Nacos API 創建命名空間
curl -X POST 'http://localhost:8848/nacos/v1/console/namespaces' \
  -d 'customNamespaceId=dev&namespaceName=開發環境&namespaceDesc=Development Environment'
```

或在 Nacos Console 手動創建：
1. 登入 http://localhost:8848/nacos
2. 命名空間管理 → 新建命名空間
3. 填寫：命名空間 ID = `dev`，命名空間名 = `開發環境`

---

## 2. API Gateway 路由問題

### 2.1 404 Not Found - 路由未匹配

#### 症狀
前端請求返回：
```json
{
  "timestamp": "2026-02-03T10:00:00.000+00:00",
  "status": 404,
  "error": "Not Found",
  "message": "No matching route found",
  "path": "/api/job/list"
}
```

#### 診斷步驟

**步驟 1: 檢查 Gateway 路由配置**

```yaml
# gateway/application.yml
spring:
  cloud:
    gateway:
      routes:
        - id: job-service
          uri: lb://job-service  # 確認服務名稱正確
          predicates:
            - Path=/api/job/**   # 確認路徑前綴正確
```

**步驟 2: 檢查服務是否註冊到 Nacos**

```bash
# 查詢 Nacos 服務列表
curl http://localhost:8848/nacos/v1/ns/instance/list?serviceName=job-service

# 預期輸出
{
  "dom": "job-service",
  "hosts": [
    {
      "ip": "192.168.1.100",
      "port": 9203,
      "healthy": true
    }
  ]
}
```

**步驟 3: 檢查 Gateway 日誌**

```bash
# 查看 Gateway 日誌
docker logs -f sa21-gateway | grep "job-service"

# 查找路由匹配日誌
```

#### 解決方案

**方案 1: 修正路由配置**

```yaml
# 錯誤示例
spring:
  cloud:
    gateway:
      routes:
        - id: job-service
          uri: lb://job-service
          predicates:
            - Path=/job/**  # 錯誤：缺少 /api 前綴

# 正確示例
spring:
  cloud:
    gateway:
      routes:
        - id: job-service
          uri: lb://job-service
          predicates:
            - Path=/api/job/**  # 正確：包含 /api 前綴
          filters:
            - StripPrefix=1     # 移除 /api 前綴後轉發
```

**方案 2: 檢查服務名稱一致性**

```yaml
# Job Service 配置
spring:
  application:
    name: job-service  # 確保與 Gateway 路由中的 uri 一致

# Gateway 路由配置
spring:
  cloud:
    gateway:
      routes:
        - id: job-service
          uri: lb://job-service  # 必須與服務名稱一致
```

**方案 3: 啟用 Gateway 日誌調試**

```yaml
# application.yml
logging:
  level:
    org.springframework.cloud.gateway: DEBUG
    reactor.netty: DEBUG
```

重啟 Gateway 後，查看詳細的路由匹配日誌。

#### 預防措施

**建立路由測試腳本**

```bash
#!/bin/bash
# gateway-route-test.sh

GATEWAY_URL="http://localhost:8080"

# 測試所有路由
echo "Testing Job Service..."
curl -s -o /dev/null -w "%{http_code}\n" $GATEWAY_URL/api/job/list

echo "Testing Resource Service..."
curl -s -o /dev/null -w "%{http_code}\n" $GATEWAY_URL/api/resource/list

# 預期輸出：200（路由成功）或 401（需要認證，但路由已匹配）
```

---

### 2.2 Gateway 超時（504 Gateway Timeout）

#### 症狀
前端請求返回：
```json
{
  "timestamp": "2026-02-03T10:00:00.000+00:00",
  "status": 504,
  "error": "Gateway Timeout",
  "message": "Response timeout"
}
```

#### 診斷步驟

**檢查 Gateway 超時配置**

```yaml
# gateway/application.yml
spring:
  cloud:
    gateway:
      httpclient:
        connect-timeout: 2000  # 連接超時 (ms)
        response-timeout: 5s   # 響應超時
```

#### 解決方案

**增加超時時間**

```yaml
# gateway/application.yml
spring:
  cloud:
    gateway:
      httpclient:
        connect-timeout: 5000
        response-timeout: 30s  # 增加到 30 秒
```

---

## 3. Feign 調用失敗

### 3.1 Feign 調用超時

#### 症狀
日誌顯示：
```
ERROR [Feign] [DepartmentClient#getById] feign.RetryableException: Read timed out executing GET http://uac-service/department/1
```

#### 診斷步驟

**步驟 1: 檢查 Feign 超時配置**

```yaml
# application.yml
spring:
  cloud:
    openfeign:
      client:
        config:
          default:
            connectTimeout: 2000  # 連接超時
            readTimeout: 5000     # 讀取超時
```

**步驟 2: 檢查目標服務是否正常**

```bash
# 直接調用目標服務（繞過 Feign）
curl http://localhost:9201/department/1
```

#### 解決方案

**方案 1: 增加 Feign 超時時間**

```yaml
# application.yml
spring:
  cloud:
    openfeign:
      client:
        config:
          default:
            connectTimeout: 5000
            readTimeout: 30000  # 增加到 30 秒
```

**方案 2: 針對特定服務配置超時**

```yaml
# application.yml
spring:
  cloud:
    openfeign:
      client:
        config:
          uac-service:  # 特定服務
            connectTimeout: 5000
            readTimeout: 60000  # 60 秒（針對慢查詢）
```

**方案 3: 優化目標服務性能**

- 添加數據庫索引
- 啟用緩存
- 優化 SQL 查詢

---

### 3.2 Feign 調用返回 404

#### 症狀
```
ERROR [Feign] [DepartmentClient#getById] feign.FeignException$NotFound: [404] during [GET] to [http://uac-service/department/1]
```

#### 診斷步驟

**檢查 Feign Client 配置**

```java
@FeignClient(name = "uac-service", path = "/department")
public interface DepartmentClient {
    @GetMapping("/{id}")
    ResponseDTO<DepartmentVO> getById(@PathVariable("id") Long id);
}
```

#### 解決方案

**修正路徑配置**

```java
// 錯誤示例
@FeignClient(name = "uac-service", path = "/api/department")  // 錯誤：多餘的 /api
public interface DepartmentClient {
    @GetMapping("/{id}")
    ResponseDTO<DepartmentVO> getById(@PathVariable("id") Long id);
}

// 正確示例
@FeignClient(name = "uac-service", path = "/department")
public interface DepartmentClient {
    @GetMapping("/{id}")
    ResponseDTO<DepartmentVO> getById(@PathVariable("id") Long id);
}
```

---

## 4. Seata 分散式事務失敗

### 4.1 事務回滾失敗

#### 症狀
分散式事務執行後，部分數據已提交，部分數據未回滾。

#### 診斷步驟

**步驟 1: 檢查 Seata Server 是否運行**

```bash
# 檢查 Seata Server 健康狀態
curl http://localhost:8091/health

# 檢查日誌
docker logs -f sa21-seata-server
```

**步驟 2: 檢查 Seata 配置**

```yaml
# application.yml
seata:
  enabled: true
  application-id: smartadmin
  tx-service-group: smartadmin-tx-group
  service:
    vgroup-mapping:
      smartadmin-tx-group: default
    grouplist:
      default: localhost:8091
```

**步驟 3: 檢查 @GlobalTransactional 註解**

```java
@Service
public class EmployeeManager {
    @GlobalTransactional(rollbackFor = Throwable.class, timeoutMills = 30000)
    public void saveEmployee(EmployeeEntity employee, List<Long> roleIdList) {
        employeeDao.insert(employee);
        roleClient.batchAssignRoles(employee.getEmployeeId(), roleIdList);
    }
}
```

#### 解決方案

**方案 1: 確保 Seata Server 正常運行**

```bash
# 啟動 Seata Server
docker compose up -d seata-server

# 檢查連接
telnet localhost 8091
```

**方案 2: 配置正確的事務組**

```yaml
# application.yml
seata:
  tx-service-group: smartadmin-tx-group  # 與 Seata Server 配置一致
```

**方案 3: 檢查數據庫 undo_log 表**

```sql
-- 檢查 undo_log 表是否存在
SELECT * FROM undo_log LIMIT 10;

-- 如果不存在，創建表
CREATE TABLE IF NOT EXISTS undo_log (
  branch_id BIGINT PRIMARY KEY,
  xid VARCHAR(128) NOT NULL,
  context VARCHAR(128) NOT NULL,
  rollback_info BYTEA NOT NULL,
  log_status INT NOT NULL,
  log_created TIMESTAMP NOT NULL,
  log_modified TIMESTAMP NOT NULL
);
```

---

## 5. 性能下降問題

### 5.1 P99 延遲超過 500ms

#### 症狀
Zipkin 追蹤顯示 P99 延遲超過 500ms，遠高於單體架構的 100ms。

#### 診斷步驟

**步驟 1: 使用 Zipkin 分析瓶頸**

```bash
# 訪問 Zipkin UI
open http://localhost:9411

# 查找慢請求
# 點擊 "Find Traces" → 設置 minDuration=500ms
```

**步驟 2: 檢查 Feign 連接池配置**

```yaml
# application.yml
spring:
  cloud:
    openfeign:
      httpclient:
        enabled: true
        max-connections: 200      # 最大連接數
        max-connections-per-route: 50  # 每個路由最大連接數
```

**步驟 3: 檢查數據庫連接池**

```yaml
# application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20  # 最大連接數
      minimum-idle: 5        # 最小空閒連接數
```

#### 解決方案

**方案 1: 優化 Feign 連接池**

```yaml
# application.yml
spring:
  cloud:
    openfeign:
      httpclient:
        enabled: true
        max-connections: 500      # 增加到 500
        max-connections-per-route: 100
```

**方案 2: 啟用緩存**

```java
@Service
public class DepartmentService {
    @Cacheable(cacheNames = "department", key = "#id")
    public DepartmentVO getById(Long id) {
        // 緩存熱點數據，減少 Feign 調用
    }
}
```

**方案 3: 使用異步調用**

```java
@Service
public class EmployeeService {
    @Async
    public CompletableFuture<DepartmentVO> getDepartmentAsync(Long deptId) {
        return CompletableFuture.completedFuture(
            departmentClient.getById(deptId).getData()
        );
    }
}
```

---

## 6. 診斷工具清單

### 6.1 Nacos 診斷工具

**Nacos Console**:
- URL: http://localhost:8848/nacos
- 功能: 服務列表、配置管理、命名空間

**Nacos API**:
```bash
# 查詢服務列表
curl http://localhost:8848/nacos/v1/ns/service/list?pageNo=1&pageSize=10

# 查詢服務實例
curl http://localhost:8848/nacos/v1/ns/instance/list?serviceName=job-service
```

---

### 6.2 Gateway 診斷工具

**Gateway Actuator**:
```bash
# 查詢路由列表
curl http://localhost:8080/actuator/gateway/routes

# 刷新路由
curl -X POST http://localhost:8080/actuator/gateway/refresh
```

---

### 6.3 Zipkin 追蹤工具

**Zipkin UI**:
- URL: http://localhost:9411
- 功能: 分散式追蹤、性能分析

**查詢慢請求**:
1. 訪問 http://localhost:9411
2. 點擊 "Find Traces"
3. 設置 minDuration=500ms
4. 分析瓶頸

---

### 6.4 日誌查看命令

```bash
# Gateway 日誌
docker logs -f sa21-gateway

# Job Service 日誌
docker logs -f sa21-job-service

# Nacos 日誌
docker logs -f sa21-nacos

# Seata 日誌
docker logs -f sa21-seata-server

# 過濾錯誤日誌
docker logs sa21-gateway | grep ERROR
```

---

## 7. 錯誤碼速查表

| 錯誤碼 | 含義 | 解決方案 | 章節 |
|--------|------|---------|------|
| `SCA-001` | Nacos 連接超時 | 檢查 Nacos Server 是否運行 | [1.1](#11-客戶端無法連接到-nacos-server) |
| `SCA-002` | Gateway 路由未匹配 | 檢查路由配置 | [2.1](#21-404-not-found---路由未匹配) |
| `SCA-003` | Feign 調用超時 | 增加超時時間 | [3.1](#31-feign-調用超時) |
| `SCA-004` | Seata 事務回滾失敗 | 檢查 undo_log 表 | [4.1](#41-事務回滾失敗) |
| `SCA-005` | 服務註冊失敗 (403) | 檢查用戶名密碼 | [1.2](#12-服務註冊失敗403-forbidden) |
| `SCA-006` | Gateway 超時 (504) | 增加超時配置 | [2.2](#22-gateway-超時504-gateway-timeout) |
| `SCA-007` | Feign 404 錯誤 | 檢查路徑配置 | [3.2](#32-feign-調用返回-404) |
| `SCA-008` | 命名空間不存在 | 創建命名空間 | [1.3](#13-命名空間不存在) |

---

## 8. 緊急聯繫方式

### 遇到無法解決的問題？

1. **查看文檔**: [README.md](./README.md) - 完整文檔導航
2. **快速鏈接**: [快速鏈接表](./README.md#🔍-快速鏈接表)
3. **GitHub Issues**: [smart-admin/issues](https://github.com/1024-lab/smart-admin/issues)
4. **Email**: [架構組郵箱]

### 生產環境緊急故障

- 📱 24/7 值班電話: [值班電話]
- 💬 緊急微信群: [SmartAdmin 緊急響應群]

---

## 📝 文檔維護

**文檔版本**: 1.0.0
**創建日期**: 2026-02-03
**最後更新**: 2026-02-03
**維護團隊**: SmartAdmin 架構組

**更新歷史**:
- v1.0.0 (2026-02-03): 初始版本，覆蓋 5 大類常見問題

---

**Happy Troubleshooting! 🔧**

希望本手冊能幫您快速解決問題！
