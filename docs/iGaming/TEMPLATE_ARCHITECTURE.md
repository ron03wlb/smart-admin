# [模塊名稱] 架構

> **Business Requirements**: [需求文檔標題](../../requirements/XX_Category/Requirements_File.md)
> **Canonical Source**: [source-archive/XX_Category/XX-YY](../../source-archive/XX_Category/XX-YY_Source_File.md)
> **View Type**: Technical Architecture
> **Target Audience**: Architects, Backend Developers, DevOps Engineers

---

## 翻譯模板說明

**用途**：此模板展示 Architecture 文檔的標準繁體中文翻譯結構。

**翻譯規則應用**：
- ✅ **Rule 1**: 技術術語保留英文（Controller, Service, Manager, Dao, API 等）
- ✅ **Rule 2**: 業務術語首次出現標註英文，後續僅用繁體中文
- ✅ **Rule 3**: 代碼片段完全保留英文（Java/SQL/YAML）
- ✅ **Rule 4**: Mermaid 圖表標籤使用繁體中文，類名保持英文
- ✅ **Rule 5**: SQL 表名/欄位名保持英文，註釋用繁體中文

---

## 1. 架構概述（Architecture Overview）

### 1.1 系統架構圖

```text
+-----------------------------------------------------------+
|  Controller 層（API 入口）                                  |
|  - PlayerController: 玩家管理接口                           |
|  - @SaCheckPermission: 權限檢查                            |
|  - ResponseDTO: 統一響應格式                               |
+-------------------+---------------------------------------+
                    |
+-------------------v---------------------------------------+
|  Service 層（業務邏輯）                                      |
|  - PlayerService: 玩家業務邏輯                              |
|  - 使用 Vavr Option 處理可選值                              |
|  - 單表 CRUD 可直接調用 Dao                                 |
+-------------------+---------------------------------------+
                    |
+-------------------v---------------------------------------+
|  Manager 層（事務管理）                                      |
|  - PlayerManager: 多表事務操作                              |
|  - @Transactional(rollbackFor = Throwable.class)         |
|  - @Cacheable: 緩存管理                                    |
+-------------------+---------------------------------------+
                    |
+-------------------v---------------------------------------+
|  Dao 層（數據訪問）                                         |
|  - PlayerDao: MyBatis Mapper                             |
|  - Multi-Tenant: Row-Level Security (RLS)                |
+-------------------+---------------------------------------+
                    |
+-------------------v---------------------------------------+
|  Database 層（PostgreSQL）                                |
|  - t_player: 玩家表                                        |
|  - AES-256-GCM 加密：PII 敏感字段                           |
|  - HMAC-SHA256 盲索引：加密後查詢                           |
+-----------------------------------------------------------+
```

**說明**：
- 架構層名稱使用中英混合：「Controller 層（API 入口）」
- 技術術語保持英文：Controller, Service, Manager, Dao, Multi-Tenant, RLS
- 功能描述使用繁體中文：「玩家管理接口」、「業務邏輯」

### 1.2 核心組件交互流程

```mermaid
flowchart TD
    A[客戶端請求] -->|HTTP POST| B[PlayerController]
    B -->|權限檢查| C{@SaCheckPermission}
    C -->|未授權| D[返回 403 Forbidden]
    C -->|已授權| E[PlayerService.createPlayer]
    E -->|單表插入| F[PlayerDao.insert]
    E -->|需要事務| G[PlayerManager.createWithWallet]
    G -->|@Transactional| H[PlayerDao.insert]
    G -->|@Transactional| I[WalletDao.insert]
    H --> J[PostgreSQL]
    I --> J
    J -->|成功| K[返回 ResponseDTO.ok]
    J -->|失敗| L[事務回滾]
    L --> M[返回 ResponseDTO.error]

    style A fill:#4CAF50,color:#fff
    style K fill:#2196F3,color:#fff
    style D fill:#FF5252,color:#fff
    style M fill:#FF5252,color:#fff
```

**說明**：
- 節點標籤使用繁體中文：「客戶端請求」、「權限檢查」、「事務回滾」
- 類名/方法名保持英文：`PlayerController`, `PlayerService.createPlayer`, `@SaCheckPermission`
- 技術術語保持英文：HTTP POST, PostgreSQL, ResponseDTO

---

## 2. 分層架構詳解（Layered Architecture Details）

### 2.1 Controller 層

**職責**：
- 接收 HTTP 請求，驗證請求參數
- 權限檢查（@SaCheckPermission）
- 調用 Service 層處理業務邏輯
- 返回統一格式響應（ResponseDTO）

**SmartAdmin 規範**：
- ✅ Controller **僅調用** Service 層（禁止直接調用 Dao/Manager）
- ✅ 使用 ResponseDTO.ok(data) 返回成功響應
- ✅ 使用 @RequiredArgsConstructor + private final 構造器注入（禁用 @Autowired）

**代碼示例**：

```java
package net.lab1024.sa.business.player.controller;

import lombok.RequiredArgsConstructor;
import net.lab1024.sa.business.player.domain.form.PlayerAddForm;
import net.lab1024.sa.business.player.domain.vo.PlayerVO;
import net.lab1024.sa.business.player.service.PlayerService;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import org.springframework.web.bind.annotation.*;
import cn.dev33.satoken.annotation.SaCheckPermission;

/**
 * 玩家管理 Controller
 *
 * @author SmartAdmin Team
 * @since 2026-02-11
 */
@RestController
@RequestMapping("/api/player")
@RequiredArgsConstructor
public class PlayerController {

    private final PlayerService playerService;

    /**
     * 創建玩家帳戶
     *
     * @param form 玩家註冊表單
     * @return 玩家 VO
     */
    @PostMapping("/create")
    @SaCheckPermission("player:create")
    public ResponseDTO<PlayerVO> createPlayer(@RequestBody PlayerAddForm form) {
        return playerService.createPlayer(form)
            .map(ResponseDTO::ok)
            .getOrElse(() -> ResponseDTO.error(UserErrorCode.PLAYER_CREATE_FAILED));
    }

    /**
     * 查詢玩家資料
     *
     * @param playerId 玩家 ID
     * @return 玩家 VO
     */
    @GetMapping("/{playerId}")
    @SaCheckPermission("player:view")
    public ResponseDTO<PlayerVO> getPlayer(@PathVariable Long playerId) {
        return playerService.getPlayer(playerId)
            .map(ResponseDTO::ok)
            .getOrElse(() -> ResponseDTO.error(UserErrorCode.PLAYER_NOT_FOUND));
    }
}
```

**說明**：
- 類註釋使用繁體中文：「玩家管理 Controller」
- 方法註釋使用繁體中文：「創建玩家帳戶」、「查詢玩家資料」
- 所有代碼元素保持英文：類名、方法名、變量名、注解

### 2.2 Service 層

**職責**：
- 核心業務邏輯處理
- **單表 CRUD 可直接調用 Dao**（無需 Manager）
- **需要 @Transactional 時委託給 Manager 層**
- 使用 Vavr Option/Try/Either 處理可選值和異常

**SmartAdmin 規範**：
- ✅ Service **禁止使用** @Transactional（必須在 Manager 層）
- ✅ Service **禁止使用** @Cacheable（必須在 Manager 層）
- ✅ Service **必須使用** io.vavr.control.Option（禁用 java.util.Optional）
- ✅ Service **可以直接調用** Dao 進行單表 CRUD 操作

**代碼示例**：

```java
package net.lab1024.sa.business.player.service;

import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.business.player.dao.PlayerDao;
import net.lab1024.sa.business.player.domain.entity.PlayerEntity;
import net.lab1024.sa.business.player.domain.form.PlayerAddForm;
import net.lab1024.sa.business.player.domain.vo.PlayerVO;
import net.lab1024.sa.business.player.manager.PlayerManager;
import net.lab1024.sa.common.core.util.SmartBeanUtil;
import org.springframework.stereotype.Service;

/**
 * 玩家業務邏輯 Service
 *
 * @author SmartAdmin Team
 * @since 2026-02-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerService {

    private final PlayerDao playerDao;
    private final PlayerManager playerManager;

    /**
     * 查詢玩家（單表查詢 - 直接調用 Dao）
     *
     * @param playerId 玩家 ID
     * @return 玩家 VO（使用 Vavr Option）
     */
    public Option<PlayerVO> getPlayer(Long playerId) {
        return playerDao.selectById(playerId)
            .map(entity -> SmartBeanUtil.copy(entity, PlayerVO.class));
    }

    /**
     * 創建玩家並初始化錢包（多表操作 - 委託給 Manager）
     *
     * @param form 玩家註冊表單
     * @return 玩家 VO
     */
    public Option<PlayerVO> createPlayer(PlayerAddForm form) {
        // 多表事務操作，委託給 Manager 層處理
        return playerManager.createPlayerWithWallet(form);
    }

    /**
     * 更新玩家信息（單表更新 - 直接調用 Dao）
     *
     * @param playerId 玩家 ID
     * @param form 更新表單
     * @return 是否成功
     */
    public Try<Boolean> updatePlayer(Long playerId, PlayerUpdateForm form) {
        return Try.of(() -> {
            PlayerEntity entity = SmartBeanUtil.copy(form, PlayerEntity.class);
            entity.setPlayerId(playerId);
            return playerDao.updateById(entity) > 0;
        });
    }
}
```

**說明**：
- Service 使用 Vavr 的 Option 和 Try 類型（強制規範）
- 單表操作直接調用 Dao：`playerDao.selectById()`
- 多表事務操作委託給 Manager：`playerManager.createPlayerWithWallet()`
- 方法註釋使用繁體中文，代碼元素保持英文

### 2.3 Manager 層

**職責**：
- **多表事務操作**（@Transactional）
- **緩存管理**（@Cacheable）
- 複雜業務編排（涉及多個 Service/Dao）

**SmartAdmin 規範**：
- ✅ Manager 是**唯一可以使用** @Transactional 的層
- ✅ @Transactional **必須指定** rollbackFor = Throwable.class
- ✅ Manager 是**唯一可以使用** @Cacheable 的層
- ✅ Manager 使用 @Component 註解（不是 @Service）

**代碼示例**：

```java
package net.lab1024.sa.business.player.manager;

import io.vavr.control.Option;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.business.player.dao.PlayerDao;
import net.lab1024.sa.business.player.domain.entity.PlayerEntity;
import net.lab1024.sa.business.player.domain.form.PlayerAddForm;
import net.lab1024.sa.business.player.domain.vo.PlayerVO;
import net.lab1024.sa.business.wallet.dao.WalletDao;
import net.lab1024.sa.business.wallet.domain.entity.WalletEntity;
import net.lab1024.sa.common.core.util.SmartBeanUtil;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 玩家業務管理 Manager（事務層）
 *
 * @author SmartAdmin Team
 * @since 2026-02-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlayerManager {

    private final PlayerDao playerDao;
    private final WalletDao walletDao;

    /**
     * 創建玩家並初始化錢包（多表事務操作）
     *
     * @param form 玩家註冊表單
     * @return 玩家 VO
     */
    @Transactional(rollbackFor = Throwable.class)
    public Option<PlayerVO> createPlayerWithWallet(PlayerAddForm form) {
        // 1. 創建玩家記錄
        PlayerEntity player = SmartBeanUtil.copy(form, PlayerEntity.class);
        playerDao.insert(player);

        // 2. 初始化玩家錢包
        WalletEntity wallet = WalletEntity.builder()
            .playerId(player.getPlayerId())
            .balance(BigDecimal.ZERO)
            .frozenBalance(BigDecimal.ZERO)
            .build();
        walletDao.insert(wallet);

        // 3. 返回玩家 VO
        return Option.of(SmartBeanUtil.copy(player, PlayerVO.class));
    }

    /**
     * 查詢玩家詳情（帶緩存）
     *
     * @param playerId 玩家 ID
     * @return 玩家 VO
     */
    @Cacheable(value = "player", key = "#playerId")
    public Option<PlayerVO> getPlayerCached(Long playerId) {
        return playerDao.selectById(playerId)
            .map(entity -> SmartBeanUtil.copy(entity, PlayerVO.class));
    }
}
```

**說明**：
- Manager 使用 @Component 註解（SmartAdmin 標準）
- @Transactional 必須指定 rollbackFor = Throwable.class
- @Cacheable 僅在 Manager 層使用
- 方法註釋使用繁體中文：「創建玩家並初始化錢包」

### 2.4 Dao 層

**職責**：
- 數據庫 CRUD 操作（MyBatis Mapper）
- 複雜 SQL 查詢
- Multi-Tenant Row-Level Security (RLS) 支持

**代碼示例**：

```java
package net.lab1024.sa.business.player.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.vavr.control.Option;
import net.lab1024.sa.business.player.domain.entity.PlayerEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 玩家 Dao（MyBatis Mapper）
 *
 * @author SmartAdmin Team
 * @since 2026-02-11
 */
@Mapper
public interface PlayerDao extends BaseMapper<PlayerEntity> {

    /**
     * 根據 ID 查詢玩家（使用 Vavr Option）
     *
     * @param playerId 玩家 ID
     * @return 玩家實體（Option）
     */
    default Option<PlayerEntity> selectById(Long playerId) {
        return Option.of(this.selectById(playerId));
    }

    /**
     * 根據郵箱盲索引查詢玩家
     *
     * @param emailIndex 郵箱 HMAC-SHA256 索引
     * @return 玩家實體（Option）
     */
    Option<PlayerEntity> selectByEmailIndex(@Param("emailIndex") String emailIndex);
}
```

---

## 3. 數據安全架構（Data Security Architecture）

### 3.1 加密層次結構

```text
+-------------------------------------------------------+
|  Application Layer                                     |
|  - AES-256-GCM: 列級加密（Column-Level Encryption）    |
|  - Argon2id: 密碼哈希（Password Hashing）              |
|  - HMAC-SHA256: 盲索引生成（Blind Index Generation）   |
+-------------------+-----------------------------------+
                    |
+-------------------v-----------------------------------+
|  Key Management Layer                                 |
|  - AWS KMS: 主密鑰（KEK - Key Encryption Key）         |
|  - Per-Player DEK（Data Encryption Key）              |
|  - HashiCorp Vault: 盲索引密鑰                         |
+-------------------+-----------------------------------+
                    |
+-------------------v-----------------------------------+
|  Storage Layer                                        |
|  - encrypted_* 欄位（VARBINARY）                       |
|  - *_index 欄位（CHAR(64) - Blind Index）              |
|  - TLS 1.3 傳輸加密                                    |
+-------------------------------------------------------+
```

### 3.2 PII 數據分類

| PII 字段 | 範例 | 風險等級 | 加密方式 |
|---------|------|---------|---------|
| **真實姓名** | "David Beckham" | High | AES-256-GCM |
| **電話號碼** | "+886912345678" | Critical | AES-256-GCM + Blind Index |
| **電子郵件** | "player@example.com" | Critical | AES-256-GCM + Blind Index |
| **銀行帳號** | "1234567890" | Critical | AES-256-GCM + Blind Index |
| **身份證號** | "A123456789" | Critical | AES-256-GCM + Blind Index |
| **密碼** | "P@ssw0rd123" | Critical | **Argon2id 哈希**（不可逆） |
| **IP 地址** | "1.2.3.4" | Medium | HMAC-SHA256（Blind Index） |

**說明**：
- 技術術語保持英文：PII, AES-256-GCM, Blind Index, HMAC-SHA256, Argon2id
- 數據類別使用繁體中文：「真實姓名」、「電話號碼」

### 3.3 加密存儲格式

```text
[Version]:[IV]:[Ciphertext]:[AuthTag]

範例：
v1:a3f8d9e2c1b4:Y3J5cHRvZ3JhcGh5==:4a7b8c9d
```

| 字段 | 長度 | 描述 |
|------|------|------|
| Version | 2 bytes | 加密版本號（支持密鑰輪轉） |
| IV | 12 bytes | 初始化向量（每次加密隨機生成） |
| Ciphertext | Variable | AES-GCM 密文 |
| AuthTag | 16 bytes | GCM 認證標籤（防篡改） |

### 3.4 盲索引查詢流程

```sql
-- 寫入路徑（Write Path）：
-- 1. 輸入: +886912345678
-- 2. AES-256-GCM(phone) -> encrypted_phone (VARBINARY)
-- 3. HMAC-SHA256(phone, blind_key) -> phone_index (CHAR(64))
-- 4. 存儲: encrypted_phone + phone_index

INSERT INTO t_player (
    player_id,
    username,
    encrypted_phone,
    phone_index
) VALUES (
    1001,
    'david_beckham',
    -- AES-256-GCM 加密後的二進制數據
    decode('v1:a3f8d9:Y3J5cHRvZ3JhcGh5:4a7b8c9d', 'base64'),
    -- HMAC-SHA256 盲索引（查詢用）
    'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855'
);

-- 查詢路徑（Query Path）：
-- 1. 輸入: +886912345678
-- 2. HMAC-SHA256(phone, blind_key) -> computed_index
-- 3. WHERE phone_index = computed_index
-- 4. 解密 encrypted_phone 用於顯示

SELECT
    player_id,
    username,
    -- 在 Application 層解密
    encrypted_phone,
    phone_index
FROM t_player
WHERE phone_index = 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855';
```

**說明**：
- SQL 註釋使用繁體中文：「寫入路徑」、「查詢路徑」
- SQL 代碼保持英文：表名、欄位名、關鍵字
- 技術術語保持英文：HMAC-SHA256, AES-256-GCM

---

## 4. 性能優化（Performance Optimization）

### 4.1 Database 連接池配置

**HikariCP 參數調優**：

```yaml
# application.yml
spring:
  datasource:
    type: com.zaxxer.hikari.HikariDataSource
    hikari:
      # 連接池大小計算公式: connections = (core_count × 2) + effective_spindle_count
      # 假設: 8 核 CPU, 1 個 SSD (視為 1 spindle)
      # 推薦值: (8 × 2) + 1 = 17
      maximum-pool-size: 17
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      leak-detection-threshold: 60000
```

**說明**：
- 配置註釋使用繁體中文：「連接池大小計算公式」
- 技術參數保持英文：maximum-pool-size, connection-timeout
- 技術術語保持英文：HikariCP, SSD, spindle

### 4.2 索引設計

```sql
-- 玩家表索引設計
-- 盲索引查詢（高頻查詢）
CREATE INDEX idx_player_email_index ON t_player(email_index);
CREATE INDEX idx_player_phone_index ON t_player(phone_index);

-- KYC 狀態查詢
CREATE INDEX idx_player_kyc_status ON t_player(kyc_status);

-- Multi-Tenant 支持（租戶隔離）
CREATE INDEX idx_player_tenant_id ON t_player(tenant_id);

-- 複合索引（租戶 + KYC 狀態）
CREATE INDEX idx_player_tenant_kyc ON t_player(tenant_id, kyc_status);
```

**索引性能影響**：

| 查詢類型 | 無索引 | 有索引 | 提升倍數 |
|---------|-------|-------|---------|
| **郵箱查詢** | 850ms | 12ms | 70x |
| **電話查詢** | 920ms | 15ms | 61x |
| **KYC 狀態過濾** | 1200ms | 35ms | 34x |

---

## 5. 多租戶架構（Multi-Tenant Architecture）

### 5.1 Row-Level Security (RLS)

**PostgreSQL RLS 策略**：

```sql
-- 啟用 RLS
ALTER TABLE t_player ENABLE ROW LEVEL SECURITY;

-- 創建租戶隔離策略
CREATE POLICY tenant_isolation_policy ON t_player
    USING (tenant_id = current_setting('app.current_tenant_id')::BIGINT);

-- 創建管理員繞過策略（可查看所有租戶數據）
CREATE POLICY admin_bypass_policy ON t_player
    USING (current_setting('app.user_role') = 'ADMIN');
```

**Application 層設置租戶上下文**：

```java
@Component
@RequiredArgsConstructor
public class TenantContextInterceptor implements HandlerInterceptor {

    private final DataSource dataSource;

    @Override
    public boolean preHandle(HttpServletRequest request,
                              HttpServletResponse response,
                              Object handler) throws SQLException {
        // 從 JWT Token 解析租戶 ID
        Long tenantId = extractTenantIdFromToken(request);

        // 設置 PostgreSQL Session 變量
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(String.format(
                "SET app.current_tenant_id = %d", tenantId));
        }

        return true;
    }
}
```

**說明**：
- SQL 註釋使用繁體中文：「啟用 RLS」、「創建租戶隔離策略」
- 代碼註釋使用繁體中文：「從 JWT Token 解析租戶 ID」
- 技術術語保持英文：RLS, PostgreSQL, Session, JWT Token

---

## 6. 監控與告警（Monitoring & Alerting）

### 6.1 關鍵指標

| 指標名稱 | 告警閾值 | 監控工具 |
|---------|---------|---------|
| **API 響應時間 (P95)** | > 500ms | Prometheus + Grafana |
| **Database 連接池利用率** | > 80% | HikariCP Metrics |
| **加密操作 QPS** | > 10000 | Micrometer |
| **錯誤率** | > 1% | Spring Boot Actuator |
| **JVM Heap 使用率** | > 85% | JVM Metrics |

### 6.2 Prometheus Metrics 配置

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: ${spring.application.name}
      environment: ${spring.profiles.active}
```

---

## 模板使用說明

### 適用場景
此模板適用於翻譯所有 `docs/iGaming/architecture/` 下的架構文檔。

### 翻譯檢查清單
使用此模板翻譯時，請確認：
- [ ] 標題和章節標題使用繁體中文
- [ ] 架構圖文字描述使用中英混合（技術術語保持英文）
- [ ] Java/SQL/YAML 代碼完全保留英文
- [ ] 代碼註釋使用繁體中文
- [ ] Mermaid 圖表標籤使用繁體中文，類名保持英文
- [ ] 技術術語（Controller, Service, Manager, API 等）保持英文
- [ ] 業務術語首次出現標註英文，後續僅用繁體中文
- [ ] 表格中的技術指標保持英文（QPS, TPS, P95 等）

### 參考資源
- **翻譯詞彙表**: [docs/iGaming/TRANSLATION_GLOSSARY.md](TRANSLATION_GLOSSARY.md)
- **Ralph 規則**: [docs/ralph/guardrails.md P14](../ralph/guardrails.md)
- **實際範例**: [architecture/12_Security/Data_Security_Standard.md](architecture/12_Security/Data_Security_Standard.md)

---

**模板版本**: 1.0.0
**創建日期**: 2026-02-11
**用途**: iGaming Architecture 文檔繁體中文翻譯標準模板
