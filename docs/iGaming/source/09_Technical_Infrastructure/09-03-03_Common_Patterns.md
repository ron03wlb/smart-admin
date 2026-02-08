# 07-03-03 API 通用模式 (Common API Patterns)

> **版本**: 4.0.0
> **最後更新**: 2026-02-04
> **來源**: 合併自 12-05_API_Design_Standard.md §3, §5, §8 + 12-05-01 §3, §4

---

## 📋 目錄

- [統一錯誤碼體系](#統一錯誤碼體系)
- [分頁排序過濾](#分頁排序過濾)
- [性能優化](#性能優化)
- [SpringDoc 配置](#springdoc-配置)
- [錯誤處理示例](#錯誤處理示例)

---
## 🔢 統一錯誤碼體系

### 1. 錯誤碼設計

**格式**: `XYZZ`
- **X**: 錯誤類別（1=成功, 4=客戶端錯誤, 5=服務端錯誤）
- **Y**: 子類別
- **ZZ**: 具體錯誤

### 2. 標準錯誤碼

#### 成功 (1xxx)

| Code | 說明 | HTTP Status |
|------|------|-------------|
| **1000** | 成功 | 200 OK |
| **1001** | 已創建 | 201 Created |
| **1002** | 已接受 | 202 Accepted |

#### 認證與授權錯誤 (40xx)

| Code | 說明 | HTTP Status | 場景 |
|------|------|-------------|------|
| **4001** | 未認證 | 401 | Token無效/過期 |
| **4002** | Token過期 | 401 | JWT過期 |
| **4003** | 無權限 | 403 | RBAC權限不足 |
| **4004** | 未找到 | 404 | 資源不存在 |
| **4005** | MFA必需 | 403 | 需要二次驗證 |

#### 請求錯誤 (400x-402x)

| Code | 說明 | HTTP Status | 場景 |
|------|------|-------------|------|
| **4009** | 參數驗證失敗 | 400 | 必填字段缺失、格式錯誤 |
| **4010** | 業務規則違反 | 422 | 餘額不足、流水未達標 |
| **4011** | 重複請求 | 409 | 冪等性檢查失敗 |
| **4012** | 並發衝突 | 409 | 樂觀鎖版本衝突 |
| **4013** | 資源已存在 | 409 | 用戶名重複、郵箱重複 |
| **4029** | 請求過多 | 429 | 超過限流閾值 |

#### 業務錯誤 (41xx-49xx)

##### 玩家相關 (41xx)

| Code | 說明 | HTTP Status |
|------|------|-------------|
| **4101** | 玩家未找到 | 404 |
| **4102** | 玩家已禁用 | 403 |
| **4103** | KYC未驗證 | 403 |
| **4104** | 玩家自我排除 | 403 |

##### 錢包相關 (42xx)

| Code | 說明 | HTTP Status |
|------|------|-------------|
| **4201** | 餘額不足 | 422 |
| **4202** | 錢包未找到 | 404 |
| **4203** | 錢包已鎖定 | 403 |
| **4204** | 信用額度不足 | 422 |

##### 交易相關 (43xx)

| Code | 說明 | HTTP Status |
|------|------|-------------|
| **4301** | 交易失敗 | 422 |
| **4302** | 支付渠道不可用 | 503 |
| **4303** | 超過交易限額 | 422 |
| **4304** | 交易已取消 | 409 |

##### 遊戲相關 (44xx)

| Code | 說明 | HTTP Status |
|------|------|-------------|
| **4401** | 遊戲不可用 | 503 |
| **4402** | 遊戲會話無效 | 400 |
| **4403** | 下注金額無效 | 422 |
| **4404** | GP通信失敗 | 502 |

##### 活動相關 (45xx)

| Code | 說明 | HTTP Status |
|------|------|-------------|
| **4501** | 活動已過期 | 410 |
| **4502** | 不符合活動條件 | 422 |
| **4503** | 紅利已用盡 | 422 |
| **4504** | 流水未達標 | 422 |

#### 服務端錯誤 (50xx)

| Code | 說明 | HTTP Status | 場景 |
|------|------|-------------|------|
| **5000** | 服務器內部錯誤 | 500 | 未知異常 |
| **5001** | 數據庫錯誤 | 500 | SQL執行失敗 |
| **5002** | 上游服務錯誤 | 502 | GP/PSP異常 |
| **5003** | 服務不可用 | 503 | 維護模式 |
| **5004** | 上游超時 | 504 | GP/PSP超時 |

---

## 📄 分頁排序過濾

### 1. 分頁標準

**Offset分頁（推薦用於小數據集）**：
```
GET /api/v1/players?page=1&page_size=20
```

**響應格式**：
```json
{
  "code": 1000,
  "message": "Success",
  "data": {
    "items": [...],
    "pagination": {
      "page": 1,
      "page_size": 20,
      "total_count": 1543,
      "total_pages": 78,
      "has_next": true,
      "has_previous": false
    }
  }
}
```

**Cursor分頁（推薦用於大數據集/實時流）**：
```
GET /api/v1/transactions?cursor=eyJpZCI6MTIzNDU2fQ&limit=50
```

**響應格式**：
```json
{
  "code": 1000,
  "message": "Success",
  "data": {
    "items": [...],
    "pagination": {
      "next_cursor": "eyJpZCI6MTIzNTA2fQ",
      "has_more": true
    }
  }
}
```

---

### 2. 排序標準

**語法**: `sort={field}:{order}`

**示例**：
```
# 單字段排序
GET /api/v1/players?sort=created_at:desc

# 多字段排序
GET /api/v1/players?sort=vip_level:desc,created_at:desc

# 默認排序
GET /api/v1/players  # 默認: created_at:desc
```

**排序方向**：
- `asc`: 升序
- `desc`: 降序（默認）

---

### 3. 過濾標準

**精確匹配**：
```
GET /api/v1/players?status=active&kyc_status=verified
```

**範圍查詢**：
```
GET /api/v1/transactions?created_at_gte=2026-01-01&created_at_lte=2026-01-31
GET /api/v1/players?balance_gt=1000&balance_lte=10000
```

**模糊搜索**：
```
GET /api/v1/players?username_like=john
GET /api/v1/games?name_contains=poker
```

**IN查詢**：
```
GET /api/v1/players?status_in=active,suspended
GET /api/v1/games?provider_id_in=1,2,3
```

**複雜過濾（使用JSON）**（可選高級功能）：
```
GET /api/v1/players?filter={"and":[{"field":"status","op":"eq","value":"active"},{"field":"balance","op":"gt","value":1000}]}
```

---

## ⚡ 性能優化

### 1. 緩存策略

**HTTP緩存頭**：
```http
# 靜態資源（遊戲圖片、前端資源）
Cache-Control: public, max-age=31536000, immutable

# 準靜態資源（遊戲列表、VIP等級）
Cache-Control: public, max-age=3600, must-revalidate
ETag: "33a64df551425fcc55e4d42a148795d9f25f89d4"

# 動態資源（玩家餘額、交易記錄）
Cache-Control: no-cache, no-store, must-revalidate
```

---

### 2. 壓縮

**Gzip壓縮（推薦）**：
```http
Accept-Encoding: gzip, deflate
Content-Encoding: gzip
```

**壓縮率**：
- JSON: ~70-80%壓縮率
- 建議閾值: >1KB才壓縮

---

### 3. 字段選擇

**Sparse Fieldsets**：
```text
GET /api/v1/players/{id}?fields=id,username,balance
```

**響應**：
```json
{
  "data": {
    "id": 123456,
    "username": "john_doe",
    "balance": 50000
  }
}
```

---

### 4. 批量操作

**批量查詢**：
```
POST /api/v1/players/batch-get
{
  "player_ids": [123, 456, 789]
}
```

**批量創建**：
```
POST /api/v1/bonuses/batch-create
{
  "bonuses": [
    {"player_id": 123, "amount": 1000},
    {"player_id": 456, "amount": 2000}
  ]
}
```

---

## 🔧 SpringDoc 配置

### 完整的 SpringDoc OpenAPI 配置

```java
package net.lab1024.sa.admin.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * SpringDoc OpenAPI 配置
 *
 * @author SmartAdmin Architecture Team
 * @date 2026-01-31
 */
@Configuration
public class SpringDocConfig {

    /**
     * 全局 OpenAPI 配置
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                // API 基本信息
                .info(new Info()
                        .title("SmartAdmin iGaming Platform API")
                        .version("1.0.0")
                        .description("""
                                SmartAdmin iGaming Platform 統一 API 文檔

                                **核心功能**:
                                - 玩家管理 (Players)
                                - 錢包與交易 (Wallets & Transactions)
                                - 活動與紅利 (Promotions & Bonuses)
                                - 遊戲集成 (Game Integration)

                                **認證方式**: JWT Bearer Token (Sa-Token)
                                """)
                        .contact(new Contact()
                                .name("SmartAdmin Architecture Team")
                                .email("tech@smartadmin.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))

                // 服務器配置
                .servers(List.of(
                        new Server()
                                .url("https://api.smartadmin.com/v1")
                                .description("生產環境"),
                        new Server()
                                .url("https://staging-api.smartadmin.com/v1")
                                .description("測試環境"),
                        new Server()
                                .url("http://localhost:1024/api/v1")
                                .description("本地開發環境")
                ))

                // 全局安全配置
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("""
                                                使用 Sa-Token 生成的 JWT Token

                                                **獲取方式**: POST /api/v1/auth/login

                                                **有效期**:
                                                - Access Token: 15 分鐘
                                                - Refresh Token: 7 天
                                                """)));
    }

    /**
     * API 分組: 玩家管理
     */
    @Bean
    public GroupedOpenApi playerApi() {
        return GroupedOpenApi.builder()
                .group("01-Players")
                .displayName("玩家管理")
                .pathsToMatch("/api/v1/players/**")
                .build();
    }

    /**
     * API 分組: 紅利與活動
     */
    @Bean
    public GroupedOpenApi bonusApi() {
        return GroupedOpenApi.builder()
                .group("02-Bonuses")
                .displayName("紅利與活動")
                .pathsToMatch("/api/v1/bonuses/**", "/api/v1/promotions/**")
                .build();
    }

    /**
     * API 分組: 流水查詢
     */
    @Bean
    public GroupedOpenApi wageringApi() {
        return GroupedOpenApi.builder()
                .group("03-Wagering")
                .displayName("流水查詢")
                .pathsToMatch("/api/v1/wagering/**")
                .build();
    }

    /**
     * API 分組: 交易記錄
     */
    @Bean
    public GroupedOpenApi transactionApi() {
        return GroupedOpenApi.builder()
                .group("04-Transactions")
                .displayName("交易記錄")
                .pathsToMatch("/api/v1/transactions/**")
                .build();
    }
}
```

### application.yml 配置

```yaml
springdoc:
  api-docs:
    enabled: true
    path: /api/v1/api-docs
  swagger-ui:
    enabled: true
    path: /api/v1/swagger-ui.html
    operations-sorter: method
    tags-sorter: alpha
    display-request-duration: true
    show-extensions: true
  group-configs:
    - group: 01-Players
      display-name: 玩家管理
      paths-to-match: /api/v1/players/**
    - group: 02-Bonuses
      display-name: 紅利與活動
      paths-to-match: /api/v1/bonuses/**, /api/v1/promotions/**
    - group: 03-Wagering
      display-name: 流水查詢
      paths-to-match: /api/v1/wagering/**
    - group: 04-Transactions
      display-name: 交易記錄
      paths-to-match: /api/v1/transactions/**
```

---

## ❌ 錯誤處理示例

### 統一異常處理器

```java
package net.lab1024.sa.admin.config;

import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.foundation.domain.response.ResponseDTO;
import net.lab1024.sa.foundation.domain.code.UserErrorCode;
import net.lab1024.sa.foundation.domain.code.SystemErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 統一異常處理器
 *
 * @author SmartAdmin Architecture Team
 * @date 2026-01-31
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 處理參數驗證異常 (400 Bad Request)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseDTO<Map<String, String>> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        log.warn("參數驗證失敗: path={}, errors={}", request.getRequestURI(), ex.getMessage());

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        return ResponseDTO.error(UserErrorCode.PARAM_ERROR.getCode(), "參數驗證失敗", errors);
    }

    /**
     * 處理業務異常 (422 Unprocessable Entity)
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ResponseDTO<Object> handleBusinessException(
            BusinessException ex,
            HttpServletRequest request) {

        log.warn("業務異常: path={}, code={}, message={}",
                request.getRequestURI(), ex.getCode(), ex.getMessage());

        return ResponseDTO.error(ex.getCode(), ex.getMessage(), ex.getErrorDetail());
    }

    /**
     * 處理未知異常 (500 Internal Server Error)
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseDTO<Object> handleException(
            Exception ex,
            HttpServletRequest request) {

        log.error("服務器內部錯誤: path={}, error={}",
                request.getRequestURI(), ex.getMessage(), ex);

        Map<String, String> errorDetail = new HashMap<>();
        errorDetail.put("error", ex.getClass().getSimpleName());
        errorDetail.put("message", ex.getMessage());

        return ResponseDTO.error(SystemErrorCode.SYSTEM_ERROR.getCode(), "服務器內部錯誤", errorDetail);
    }
}
```

### 業務異常類

```java
package net.lab1024.sa.foundation.domain.exception;

import lombok.Getter;

/**
 * 業務異常
 *
 * @author SmartAdmin Architecture Team
 * @date 2026-01-31
 */
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;
    private final Object errorDetail;

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.errorDetail = null;
    }

    public BusinessException(Integer code, String message, Object errorDetail) {
        super(message);
        this.code = code;
        this.errorDetail = errorDetail;
    }
}
```

---

