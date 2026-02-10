# API 通用模式 (Common API Patterns)

> **Business Requirements**: N/A — Pure technical infrastructure document
> **Canonical Source**: [09-03-03 Common Patterns](../../source-archive/09_Technical_Infrastructure/09-03-03_Common_Patterns.md)
> **View**: Technical Architecture (Development & DevOps)

---

## 1. 統一錯誤碼體系

### 1.1 錯誤碼格式

格式: `XYZZ`
- **X**: 錯誤類別（1=成功, 4=客戶端錯誤, 5=服務端錯誤）
- **Y**: 子類別
- **ZZ**: 具體錯誤

### 1.2 標準錯誤碼表

#### 成功 (1xxx)

| Code | 說明 | HTTP Status |
|------|------|-------------|
| **1000** | 成功 | 200 OK |
| **1001** | 已創建 | 201 Created |
| **1002** | 已接受 | 202 Accepted |

#### 認證與授權錯誤 (40xx)

| Code | 說明 | HTTP Status | 場景 |
|------|------|-------------|------|
| **4001** | 未認證 | 401 | Token 無效/過期 |
| **4002** | Token 過期 | 401 | JWT 過期 |
| **4003** | 無權限 | 403 | RBAC 權限不足 |
| **4005** | MFA 必需 | 403 | 需要二次驗證 |

#### 請求錯誤 (400x-402x)

| Code | 說明 | HTTP Status | 場景 |
|------|------|-------------|------|
| **4009** | 參數驗證失敗 | 400 | 必填字段缺失 |
| **4010** | 業務規則違反 | 422 | 餘額不足、流水未達標 |
| **4011** | 重複請求 | 409 | 冪等性檢查失敗 |
| **4012** | 並發衝突 | 409 | 樂觀鎖版本衝突 |
| **4029** | 請求過多 | 429 | 超過限流閾值 |

#### 業務錯誤 (41xx-49xx)

| Code Range | 領域 | 範例 |
|-----------|------|------|
| **41xx** | 玩家相關 | 4101 玩家未找到, 4102 已禁用, 4103 KYC 未驗證 |
| **42xx** | 錢包相關 | 4201 餘額不足, 4202 錢包未找到, 4203 已鎖定 |
| **43xx** | 交易相關 | 4301 交易失敗, 4303 超過限額 |
| **44xx** | 遊戲相關 | 4401 遊戲不可用, 4403 下注金額無效 |
| **45xx** | 活動相關 | 4501 活動已過期, 4504 流水未達標 |

#### 服務端錯誤 (50xx)

| Code | 說明 | HTTP Status | 場景 |
|------|------|-------------|------|
| **5000** | 服務器內部錯誤 | 500 | 未知異常 |
| **5001** | 數據庫錯誤 | 500 | SQL 執行失敗 |
| **5002** | 上游服務錯誤 | 502 | GP/PSP 異常 |
| **5003** | 服務不可用 | 503 | 維護模式 |

---

## 2. 分頁排序過濾

### 2.1 Offset 分頁（小數據集推薦）

```http
GET /api/v1/players?page=1&page_size=20
```

**響應**:

```json
{
  "code": 1000,
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

### 2.2 Cursor 分頁（大數據集/實時流推薦）

```http
GET /api/v1/transactions?cursor=eyJpZCI6MTIzNDU2fQ&limit=50
```

**響應**:

```json
{
  "code": 1000,
  "data": {
    "items": [...],
    "pagination": {
      "next_cursor": "eyJpZCI6MTIzNTA2fQ",
      "has_more": true
    }
  }
}
```

### 2.3 排序語法

```http
GET /api/v1/players?sort=created_at:desc
GET /api/v1/players?sort=vip_level:desc,created_at:desc
```

### 2.4 過濾語法

```http
# 精確匹配
GET /api/v1/players?status=active&kyc_status=verified

# 範圍查詢
GET /api/v1/transactions?created_at_gte=2026-01-01&created_at_lte=2026-01-31

# IN 查詢
GET /api/v1/players?status_in=active,suspended
```

---

## 3. SpringDoc OpenAPI 配置

```java
@Configuration
public class SpringDocConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SmartAdmin iGaming Platform API")
                        .version("1.0.0")
                        .description("""
                                SmartAdmin iGaming Platform 統一 API 文檔
                                **認證方式**: JWT Bearer Token (Sa-Token)
                                """))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }

    @Bean
    public GroupedOpenApi playerApi() {
        return GroupedOpenApi.builder()
                .group("01-Players")
                .displayName("玩家管理")
                .pathsToMatch("/api/v1/players/**")
                .build();
    }

    @Bean
    public GroupedOpenApi bonusApi() {
        return GroupedOpenApi.builder()
                .group("02-Bonuses")
                .displayName("紅利與活動")
                .pathsToMatch("/api/v1/bonuses/**", "/api/v1/promotions/**")
                .build();
    }
}
```

### 3.1 application.yml 配置

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
```

---

## 4. 統一異常處理器

```java
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseDTO<Map<String, String>> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.warn("參數驗證失敗: path={}", request.getRequestURI());
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            errors.put(fieldName, error.getDefaultMessage());
        });
        return ResponseDTO.error(UserErrorCode.PARAM_ERROR.getCode(), "參數驗證失敗", errors);
    }

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ResponseDTO<Object> handleBusinessException(
            BusinessException ex, HttpServletRequest request) {
        log.warn("業務異常: path={}, code={}", request.getRequestURI(), ex.getCode());
        return ResponseDTO.error(ex.getCode(), ex.getMessage(), ex.getErrorDetail());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseDTO<Object> handleException(Exception ex, HttpServletRequest request) {
        log.error("服務器內部錯誤: path={}", request.getRequestURI(), ex);
        return ResponseDTO.error(SystemErrorCode.SYSTEM_ERROR.getCode(), "服務器內部錯誤", null);
    }
}
```

### 4.1 業務異常類

```java
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

## 5. 性能優化模式

### 5.1 HTTP 緩存

```http
# 靜態資源（遊戲圖片、前端資源）
Cache-Control: public, max-age=31536000, immutable

# 準靜態資源（遊戲列表、VIP 等級）
Cache-Control: public, max-age=3600, must-revalidate
ETag: "33a64df551425fcc55e4d42a148795d9f25f89d4"

# 動態資源（玩家餘額、交易記錄）
Cache-Control: no-cache, no-store, must-revalidate
```

### 5.2 批量操作

```http
POST /api/v1/players/batch-get
{"player_ids": [123, 456, 789]}

POST /api/v1/bonuses/batch-create
{"bonuses": [{"player_id": 123, "amount": 1000}, {"player_id": 456, "amount": 2000}]}
```

---

## 相關文檔

- [API Design Principles](./API_Design_Principles.md) - API 設計原則
- [Domain APIs](./Domain_APIs.md) - 領域 API 設計
- [Authentication Architecture](./Authentication_Architecture.md) - 認證架構
