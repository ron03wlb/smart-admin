# 07-03-04 領域 API 設計 (Domain API Design)

> ⚠️ **超大文檔警告**: 本文檔共 1,618 行，建議拆分為 3 個子文檔
> **拆分建議**: 1) 玩家 API 2) 遊戲 API 3) 財務 API

> **版本**: 4.0.0
> **最後更新**: 2026-02-04
> **來源**: 12-05-01_API_Design_Examples.md §1-2, §5

---

## 📋 目錄

- [OpenAPI 3.0 規範示例](#openapi-30-規範示例)
- [實際業務 API 案例](#實際業務-api-案例)
- [測試用例](#測試用例)

---
## 📜 OpenAPI 3.0 規範示例

### 1. 玩家管理 API (Players API)

**完整 OpenAPI 3.0 定義**:

```yaml
openapi: 3.0.3
info:
  title: SmartAdmin iGaming Platform API
  version: 1.0.0
  description: |
    SmartAdmin iGaming Platform 統一 API 文檔

    **核心功能**:
    - 玩家管理 (Players)
    - 錢包與交易 (Wallets & Transactions)
    - 活動與紅利 (Promotions & Bonuses)
    - 遊戲集成 (Game Integration)

    **認證方式**: JWT Bearer Token (Sa-Token)

  contact:
    name: SmartAdmin Architecture Team
    email: tech@smartadmin.com
  license:
    name: Apache 2.0
    url: https://www.apache.org/licenses/LICENSE-2.0.html

servers:
  - url: https://api.smartadmin.com/v1
    description: 生產環境
  - url: https://staging-api.smartadmin.com/v1
    description: 測試環境
  - url: http://localhost:1024/api/v1
    description: 本地開發環境

tags:
  - name: Players
    description: 玩家管理相關API
  - name: Bonuses
    description: 紅利與活動相關API
  - name: Wagering
    description: 流水查詢相關API
  - name: Transactions
    description: 交易記錄相關API

# 全局安全配置
security:
  - bearerAuth: []

# 安全方案定義
components:
  securitySchemes:
    bearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT
      description: |
        使用 Sa-Token 生成的 JWT Token

        **獲取方式**: POST /api/v1/auth/login

        **有效期**:
        - Access Token: 15 分鐘
        - Refresh Token: 7 天

  schemas:
    # 通用響應格式
    ResponseDTO:
      type: object
      required:
        - code
        - message
        - timestamp
        - trace_id
      properties:
        code:
          type: integer
          description: 業務錯誤碼 (1000=成功, 4xxx=客戶端錯誤, 5xxx=服務端錯誤)
          example: 1000
        message:
          type: string
          description: 人類可讀的響應消息
          example: "Success"
        data:
          type: object
          description: 響應數據 (成功時包含)
          nullable: true
        error_detail:
          type: object
          description: 錯誤詳情 (失敗時包含)
          nullable: true
        timestamp:
          type: string
          format: date-time
          description: ISO 8601 格式時間戳
          example: "2026-01-31T10:00:00Z"
        trace_id:
          type: string
          description: 分佈式追蹤 ID (用於日誌查詢)
          example: "abc-123-def-456"

    # 分頁響應格式
    PageResponse:
      allOf:
        - $ref: '#/components/schemas/ResponseDTO'
        - type: object
          properties:
            data:
              type: object
              properties:
                items:
                  type: array
                  items:
                    type: object
                pagination:
                  $ref: '#/components/schemas/PaginationMeta'

    PaginationMeta:
      type: object
      required:
        - page
        - page_size
        - total_count
        - total_pages
        - has_next
        - has_previous
      properties:
        page:
          type: integer
          description: 當前頁碼 (從 1 開始)
          example: 1
        page_size:
          type: integer
          description: 每頁數量
          example: 20
        total_count:
          type: integer
          description: 總記錄數
          example: 1543
        total_pages:
          type: integer
          description: 總頁數
          example: 78
        has_next:
          type: boolean
          description: 是否有下一頁
          example: true
        has_previous:
          type: boolean
          description: 是否有上一頁
          example: false

    # 玩家實體
    Player:
      type: object
      required:
        - player_id
        - tenant_id
        - username
        - email
        - status
        - kyc_status
        - created_at
      properties:
        player_id:
          type: integer
          format: int64
          description: 玩家唯一標識
          example: 123456
        tenant_id:
          type: integer
          format: int64
          description: 租戶 ID (多租戶隔離)
          example: 1
        username:
          type: string
          description: 用戶名 (唯一)
          minLength: 4
          maxLength: 20
          pattern: '^[a-zA-Z0-9_-]+$'
          example: "john_doe"
        email:
          type: string
          format: email
          description: 郵箱地址 (唯一)
          example: "john@example.com"
        phone:
          type: string
          description: 手機號碼
          nullable: true
          example: "+886912345678"
        status:
          type: string
          enum: [ACTIVE, SUSPENDED, SELF_EXCLUDED, BANNED]
          description: |
            玩家狀態:
            - ACTIVE: 正常
            - SUSPENDED: 暫停 (管理員操作)
            - SELF_EXCLUDED: 自我排除 (玩家主動)
            - BANNED: 封禁 (違規處理)
          example: "ACTIVE"
        kyc_status:
          type: string
          enum: [NOT_VERIFIED, PENDING, VERIFIED, REJECTED]
          description: |
            KYC 驗證狀態:
            - NOT_VERIFIED: 未驗證
            - PENDING: 審核中
            - VERIFIED: 已驗證
            - REJECTED: 被拒絕
          example: "VERIFIED"
        vip_level:
          type: integer
          description: VIP 等級 (0-10)
          minimum: 0
          maximum: 10
          example: 5
        registration_ip:
          type: string
          format: ipv4
          description: 註冊 IP 地址
          example: "203.0.113.42"
        last_login_at:
          type: string
          format: date-time
          description: 最後登錄時間
          nullable: true
          example: "2026-01-31T09:30:00Z"
        created_at:
          type: string
          format: date-time
          description: 創建時間
          example: "2026-01-15T14:22:00Z"
        updated_at:
          type: string
          format: date-time
          description: 最後更新時間
          example: "2026-01-31T10:00:00Z"

    # 創建玩家表單
    CreatePlayerForm:
      type: object
      required:
        - username
        - password
        - email
        - currency
      properties:
        username:
          type: string
          minLength: 4
          maxLength: 20
          pattern: '^[a-zA-Z0-9_-]+$'
          example: "john_doe"
        password:
          type: string
          format: password
          minLength: 8
          maxLength: 32
          description: 密碼 (8-32 字符，需包含字母和數字)
          example: "Password123"
        email:
          type: string
          format: email
          example: "john@example.com"
        phone:
          type: string
          nullable: true
          example: "+886912345678"
        currency:
          type: string
          enum: [USD, EUR, CNY, TWD, THB, VND]
          description: 錢包幣種
          example: "USD"
        referral_code:
          type: string
          description: 推薦碼 (可選)
          nullable: true
          example: "REF123ABC"

    # 更新玩家表單
    UpdatePlayerForm:
      type: object
      properties:
        email:
          type: string
          format: email
          example: "newemail@example.com"
        phone:
          type: string
          nullable: true
          example: "+886987654321"
        status:
          type: string
          enum: [ACTIVE, SUSPENDED, SELF_EXCLUDED, BANNED]
          example: "ACTIVE"

    # 錯誤響應
    ErrorResponse:
      allOf:
        - $ref: '#/components/schemas/ResponseDTO'
        - type: object
          properties:
            code:
              type: integer
              example: 4201
            message:
              type: string
              example: "餘額不足"
            error_detail:
              type: object
              properties:
                required_amount:
                  type: number
                  format: double
                  example: 10000.00
                available_balance:
                  type: number
                  format: double
                  example: 5000.00
                currency:
                  type: string
                  example: "USD"

  parameters:
    # 路徑參數
    PlayerIdParam:
      name: id
      in: path
      required: true
      description: 玩家 ID
      schema:
        type: integer
        format: int64
        example: 123456

    # 查詢參數
    PageParam:
      name: page
      in: query
      description: 頁碼 (從 1 開始)
      schema:
        type: integer
        minimum: 1
        default: 1
        example: 1

    PageSizeParam:
      name: page_size
      in: query
      description: 每頁數量
      schema:
        type: integer
        minimum: 1
        maximum: 100
        default: 20
        example: 20

    SortParam:
      name: sort
      in: query
      description: |
        排序規則 (格式: field:order)

        **示例**:
        - `created_at:desc` - 按創建時間降序
        - `username:asc` - 按用戶名升序
        - `vip_level:desc,created_at:desc` - 多字段排序
      schema:
        type: string
        default: "created_at:desc"
        example: "created_at:desc"

    StatusParam:
      name: status
      in: query
      description: 玩家狀態過濾
      schema:
        type: string
        enum: [ACTIVE, SUSPENDED, SELF_EXCLUDED, BANNED]
        example: "ACTIVE"

    # 請求頭參數
    TenantIdHeader:
      name: X-Tenant-ID
      in: header
      required: true
      description: 租戶 ID (多租戶隔離)
      schema:
        type: integer
        format: int64
        example: 1

    RequestIdHeader:
      name: X-Request-ID
      in: header
      required: true
      description: 請求唯一標識 (UUID v4)
      schema:
        type: string
        format: uuid
        example: "550e8400-e29b-41d4-a716-446655440000"

    IdempotencyKeyHeader:
      name: X-Idempotency-Key
      in: header
      required: false
      description: 冪等性鍵 (用於 POST/PATCH 防重複提交)
      schema:
        type: string
        format: uuid
        example: "660f9500-f39c-52e5-b827-557766551111"

  responses:
    # 標準錯誤響應
    BadRequest:
      description: 請求參數錯誤
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/ErrorResponse'
          example:
            code: 4009
            message: "參數驗證失敗"
            error_detail:
              field: "username"
              message: "用戶名必須為 4-20 個字符"
            timestamp: "2026-01-31T10:00:00Z"
            trace_id: "abc-123-def-456"

    Unauthorized:
      description: 未認證 (Token 無效或過期)
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/ErrorResponse'
          example:
            code: 4001
            message: "未認證"
            error_detail:
              reason: "Token 已過期"
            timestamp: "2026-01-31T10:00:00Z"
            trace_id: "abc-123-def-456"

    Forbidden:
      description: 無權限
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/ErrorResponse'
          example:
            code: 4003
            message: "無權限"
            error_detail:
              required_permission: "player:update"
            timestamp: "2026-01-31T10:00:00Z"
            trace_id: "abc-123-def-456"

    NotFound:
      description: 資源未找到
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/ErrorResponse'
          example:
            code: 4004
            message: "玩家未找到"
            error_detail:
              player_id: 123456
            timestamp: "2026-01-31T10:00:00Z"
            trace_id: "abc-123-def-456"

    InternalServerError:
      description: 服務器內部錯誤
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/ErrorResponse'
          example:
            code: 5000
            message: "服務器內部錯誤"
            error_detail:
              error: "NullPointerException"
            timestamp: "2026-01-31T10:00:00Z"
            trace_id: "abc-123-def-456"

# API 端點定義
paths:
  /players:
    get:
      tags:
        - Players
      summary: 獲取玩家列表
      description: |
        分頁查詢玩家列表，支持過濾、排序和搜索

        **權限要求**: `player:read`

        **過濾條件**:
        - status: 玩家狀態
        - kyc_status: KYC 驗證狀態
        - vip_level: VIP 等級

        **排序字段**:
        - created_at: 創建時間 (默認)
        - username: 用戶名
        - vip_level: VIP 等級
        - last_login_at: 最後登錄時間
      operationId: listPlayers
      parameters:
        - $ref: '#/components/parameters/PageParam'
        - $ref: '#/components/parameters/PageSizeParam'
        - $ref: '#/components/parameters/SortParam'
        - $ref: '#/components/parameters/StatusParam'
        - name: kyc_status
          in: query
          description: KYC 驗證狀態過濾
          schema:
            type: string
            enum: [NOT_VERIFIED, PENDING, VERIFIED, REJECTED]
        - name: vip_level
          in: query
          description: VIP 等級過濾
          schema:
            type: integer
            minimum: 0
            maximum: 10
        - name: search
          in: query
          description: 搜索關鍵字 (搜索用戶名或郵箱)
          schema:
            type: string
            example: "john"
        - $ref: '#/components/parameters/TenantIdHeader'
        - $ref: '#/components/parameters/RequestIdHeader'
      responses:
        '200':
          description: 成功獲取玩家列表
          content:
            application/json:
              schema:
                allOf:
                  - $ref: '#/components/schemas/PageResponse'
                  - type: object
                    properties:
                      data:
                        type: object
                        properties:
                          items:
                            type: array
                            items:
                              $ref: '#/components/schemas/Player'
              example:
                code: 1000
                message: "Success"
                data:
                  items:
                    - player_id: 123456
                      tenant_id: 1
                      username: "john_doe"
                      email: "john@example.com"
                      status: "ACTIVE"
                      kyc_status: "VERIFIED"
                      vip_level: 5
                      created_at: "2026-01-15T14:22:00Z"
                      updated_at: "2026-01-31T10:00:00Z"
                  pagination:
                    page: 1
                    page_size: 20
                    total_count: 1543
                    total_pages: 78
                    has_next: true
                    has_previous: false
                timestamp: "2026-01-31T10:00:00Z"
                trace_id: "abc-123-def-456"
        '400':
          $ref: '#/components/responses/BadRequest'
        '401':
          $ref: '#/components/responses/Unauthorized'
        '500':
          $ref: '#/components/responses/InternalServerError'

    post:
      tags:
        - Players
      summary: 創建玩家
      description: |
        創建新玩家賬戶

        **權限要求**: `player:create`

        **業務規則**:
        - 用戶名必須唯一
        - 郵箱必須唯一
        - 密碼需包含字母和數字
        - 自動創建對應幣種的錢包

        **冪等性**: 使用 `X-Idempotency-Key` 防止重複創建
      operationId: createPlayer
      parameters:
        - $ref: '#/components/parameters/TenantIdHeader'
        - $ref: '#/components/parameters/RequestIdHeader'
        - $ref: '#/components/parameters/IdempotencyKeyHeader'
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreatePlayerForm'
            example:
              username: "john_doe"
              password: "Password123"
              email: "john@example.com"
              phone: "+886912345678"
              currency: "USD"
              referral_code: "REF123ABC"
      responses:
        '201':
          description: 玩家創建成功
          headers:
            Location:
              description: 新創建玩家的 URL
              schema:
                type: string
                example: "/api/v1/players/123456"
          content:
            application/json:
              schema:
                allOf:
                  - $ref: '#/components/schemas/ResponseDTO'
                  - type: object
                    properties:
                      data:
                        $ref: '#/components/schemas/Player'
              example:
                code: 1001
                message: "玩家創建成功"
                data:
                  player_id: 123456
                  tenant_id: 1
                  username: "john_doe"
                  email: "john@example.com"
                  status: "ACTIVE"
                  kyc_status: "NOT_VERIFIED"
                  vip_level: 0
                  created_at: "2026-01-31T10:00:00Z"
                  updated_at: "2026-01-31T10:00:00Z"
                timestamp: "2026-01-31T10:00:00Z"
                trace_id: "abc-123-def-456"
        '400':
          $ref: '#/components/responses/BadRequest'
        '401':
          $ref: '#/components/responses/Unauthorized'
        '409':
          description: 玩家已存在 (用戶名或郵箱重複)
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
              example:
                code: 4013
                message: "用戶名已存在"
                error_detail:
                  field: "username"
                  value: "john_doe"
                timestamp: "2026-01-31T10:00:00Z"
                trace_id: "abc-123-def-456"
        '500':
          $ref: '#/components/responses/InternalServerError'

  /players/{id}:
    get:
      tags:
        - Players
      summary: 獲取單個玩家
      description: |
        根據玩家 ID 獲取玩家詳細信息

        **權限要求**: `player:read`

        **資源所有權驗證**: 只能查詢自己租戶下的玩家
      operationId: getPlayer
      parameters:
        - $ref: '#/components/parameters/PlayerIdParam'
        - $ref: '#/components/parameters/TenantIdHeader'
        - $ref: '#/components/parameters/RequestIdHeader'
      responses:
        '200':
          description: 成功獲取玩家信息
          content:
            application/json:
              schema:
                allOf:
                  - $ref: '#/components/schemas/ResponseDTO'
                  - type: object
                    properties:
                      data:
                        $ref: '#/components/schemas/Player'
              example:
                code: 1000
                message: "Success"
                data:
                  player_id: 123456
                  tenant_id: 1
                  username: "john_doe"
                  email: "john@example.com"
                  phone: "+886912345678"
                  status: "ACTIVE"
                  kyc_status: "VERIFIED"
                  vip_level: 5
                  registration_ip: "203.0.113.42"
                  last_login_at: "2026-01-31T09:30:00Z"
                  created_at: "2026-01-15T14:22:00Z"
                  updated_at: "2026-01-31T10:00:00Z"
                timestamp: "2026-01-31T10:00:00Z"
                trace_id: "abc-123-def-456"
        '401':
          $ref: '#/components/responses/Unauthorized'
        '403':
          $ref: '#/components/responses/Forbidden'
        '404':
          $ref: '#/components/responses/NotFound'
        '500':
          $ref: '#/components/responses/InternalServerError'

    patch:
      tags:
        - Players
      summary: 更新玩家
      description: |
        部分更新玩家信息

        **權限要求**: `player:update`

        **可更新字段**:
        - email: 郵箱地址
        - phone: 手機號碼
        - status: 玩家狀態 (僅管理員)

        **不可更新字段**:
        - player_id: 玩家 ID
        - username: 用戶名
        - tenant_id: 租戶 ID
        - created_at: 創建時間
      operationId: updatePlayer
      parameters:
        - $ref: '#/components/parameters/PlayerIdParam'
        - $ref: '#/components/parameters/TenantIdHeader'
        - $ref: '#/components/parameters/RequestIdHeader'
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/UpdatePlayerForm'
            example:
              email: "newemail@example.com"
              phone: "+886987654321"
      responses:
        '200':
          description: 玩家更新成功
          content:
            application/json:
              schema:
                allOf:
                  - $ref: '#/components/schemas/ResponseDTO'
                  - type: object
                    properties:
                      data:
                        $ref: '#/components/schemas/Player'
              example:
                code: 1000
                message: "玩家更新成功"
                data:
                  player_id: 123456
                  tenant_id: 1
                  username: "john_doe"
                  email: "newemail@example.com"
                  phone: "+886987654321"
                  status: "ACTIVE"
                  kyc_status: "VERIFIED"
                  vip_level: 5
                  updated_at: "2026-01-31T10:05:00Z"
                timestamp: "2026-01-31T10:05:00Z"
                trace_id: "abc-123-def-789"
        '400':
          $ref: '#/components/responses/BadRequest'
        '401':
          $ref: '#/components/responses/Unauthorized'
        '403':
          $ref: '#/components/responses/Forbidden'
        '404':
          $ref: '#/components/responses/NotFound'
        '500':
          $ref: '#/components/responses/InternalServerError'

    delete:
      tags:
        - Players
      summary: 刪除玩家
      description: |
        軟刪除玩家 (僅標記為已刪除，不實際刪除數據)

        **權限要求**: `player:delete`

        **業務規則**:
        - 只能刪除餘額為 0 的玩家
        - 只能刪除無未完成交易的玩家
        - 實際執行軟刪除 (deleted = true)
      operationId: deletePlayer
      parameters:
        - $ref: '#/components/parameters/PlayerIdParam'
        - $ref: '#/components/parameters/TenantIdHeader'
        - $ref: '#/components/parameters/RequestIdHeader'
      responses:
        '204':
          description: 玩家刪除成功 (無響應體)
        '401':
          $ref: '#/components/responses/Unauthorized'
        '403':
          $ref: '#/components/responses/Forbidden'
        '404':
          $ref: '#/components/responses/NotFound'
        '422':
          description: 無法刪除 (業務規則違反)
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
              example:
                code: 4010
                message: "無法刪除玩家"
                error_detail:
                  reason: "玩家餘額不為 0"
                  balance: 5000.00
                  currency: "USD"
                timestamp: "2026-01-31T10:00:00Z"
                trace_id: "abc-123-def-456"
        '500':
          $ref: '#/components/responses/InternalServerError'
```

---

## 💼 實際業務 API 案例

### 案例 1: 玩家管理 API (Player Management)

#### 1.1 Controller 層實現

```java
package net.lab1024.sa.admin.module.business.player.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.foundation.domain.response.ResponseDTO;
import net.lab1024.sa.foundation.domain.page.PageResult;
import net.lab1024.sa.admin.module.business.player.domain.form.CreatePlayerForm;
import net.lab1024.sa.admin.module.business.player.domain.form.PlayerQueryForm;
import net.lab1024.sa.admin.module.business.player.domain.form.UpdatePlayerForm;
import net.lab1024.sa.admin.module.business.player.domain.vo.PlayerVO;
import net.lab1024.sa.admin.module.business.player.service.PlayerService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 玩家管理 Controller
 *
 * @author SmartAdmin Architecture Team
 * @date 2026-01-31
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/players")
@RequiredArgsConstructor
@Tag(name = "Players", description = "玩家管理相關API")
public class PlayerController {

    private final PlayerService playerService;

    /**
     * 獲取玩家列表
     */
    @GetMapping
    @Operation(
        summary = "獲取玩家列表",
        description = "分頁查詢玩家列表，支持過濾、排序和搜索"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "成功獲取玩家列表",
            content = @Content(schema = @Schema(implementation = PageResult.class))
        ),
        @ApiResponse(responseCode = "400", description = "請求參數錯誤"),
        @ApiResponse(responseCode = "401", description = "未認證"),
        @ApiResponse(responseCode = "500", description = "服務器內部錯誤")
    })
    public ResponseDTO<PageResult<PlayerVO>> listPlayers(
            @Parameter(description = "查詢條件") @Valid PlayerQueryForm queryForm) {
        log.info("查詢玩家列表: {}", queryForm);
        PageResult<PlayerVO> pageResult = playerService.queryPage(queryForm);
        return ResponseDTO.ok(pageResult);
    }

    /**
     * 獲取單個玩家
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "獲取單個玩家",
        description = "根據玩家 ID 獲取玩家詳細信息"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "成功獲取玩家信息",
            content = @Content(schema = @Schema(implementation = PlayerVO.class))
        ),
        @ApiResponse(responseCode = "401", description = "未認證"),
        @ApiResponse(responseCode = "403", description = "無權限"),
        @ApiResponse(responseCode = "404", description = "玩家未找到"),
        @ApiResponse(responseCode = "500", description = "服務器內部錯誤")
    })
    public ResponseDTO<PlayerVO> getPlayer(
            @Parameter(description = "玩家 ID", required = true, example = "123456")
            @PathVariable Long id) {
        log.info("查詢玩家詳情: playerId={}", id);
        return playerService.findById(id)
                .map(ResponseDTO::ok)
                .getOrElse(() -> ResponseDTO.userErrorParam("玩家未找到"));
    }

    /**
     * 創建玩家
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "創建玩家",
        description = "創建新玩家賬戶，自動創建對應幣種的錢包"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "玩家創建成功",
            content = @Content(schema = @Schema(implementation = PlayerVO.class))
        ),
        @ApiResponse(responseCode = "400", description = "請求參數錯誤"),
        @ApiResponse(responseCode = "401", description = "未認證"),
        @ApiResponse(responseCode = "409", description = "玩家已存在 (用戶名或郵箱重複)"),
        @ApiResponse(responseCode = "500", description = "服務器內部錯誤")
    })
    public ResponseDTO<PlayerVO> createPlayer(
            @Parameter(description = "創建玩家表單", required = true)
            @Valid @RequestBody CreatePlayerForm form) {
        log.info("創建玩家: username={}, email={}", form.getUsername(), form.getEmail());
        return playerService.createPlayer(form)
                .map(ResponseDTO::ok)
                .getOrElseGet(error -> ResponseDTO.error(error.getCode(), error.getMessage()));
    }

    /**
     * 更新玩家
     */
    @PatchMapping("/{id}")
    @Operation(
        summary = "更新玩家",
        description = "部分更新玩家信息"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "玩家更新成功",
            content = @Content(schema = @Schema(implementation = PlayerVO.class))
        ),
        @ApiResponse(responseCode = "400", description = "請求參數錯誤"),
        @ApiResponse(responseCode = "401", description = "未認證"),
        @ApiResponse(responseCode = "403", description = "無權限"),
        @ApiResponse(responseCode = "404", description = "玩家未找到"),
        @ApiResponse(responseCode = "500", description = "服務器內部錯誤")
    })
    public ResponseDTO<PlayerVO> updatePlayer(
            @Parameter(description = "玩家 ID", required = true, example = "123456")
            @PathVariable Long id,
            @Parameter(description = "更新玩家表單", required = true)
            @Valid @RequestBody UpdatePlayerForm form) {
        log.info("更新玩家: playerId={}, form={}", id, form);
        return playerService.updatePlayer(id, form)
                .map(ResponseDTO::ok)
                .getOrElseGet(error -> ResponseDTO.error(error.getCode(), error.getMessage()));
    }

    /**
     * 刪除玩家
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "刪除玩家",
        description = "軟刪除玩家 (僅標記為已刪除，不實際刪除數據)"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "玩家刪除成功 (無響應體)"),
        @ApiResponse(responseCode = "401", description = "未認證"),
        @ApiResponse(responseCode = "403", description = "無權限"),
        @ApiResponse(responseCode = "404", description = "玩家未找到"),
        @ApiResponse(responseCode = "422", description = "無法刪除 (業務規則違反)"),
        @ApiResponse(responseCode = "500", description = "服務器內部錯誤")
    })
    public ResponseDTO<Void> deletePlayer(
            @Parameter(description = "玩家 ID", required = true, example = "123456")
            @PathVariable Long id) {
        log.info("刪除玩家: playerId={}", id);
        return playerService.deletePlayer(id)
                .map(success -> ResponseDTO.<Void>ok())
                .getOrElseGet(error -> ResponseDTO.error(error.getCode(), error.getMessage()));
    }
}
```

#### 1.2 Service 層接口

```java
package net.lab1024.sa.admin.module.business.player.service;

import io.vavr.control.Either;
import io.vavr.control.Option;
import net.lab1024.sa.foundation.domain.page.PageResult;
import net.lab1024.sa.foundation.domain.code.ErrorCode;
import net.lab1024.sa.admin.module.business.player.domain.form.CreatePlayerForm;
import net.lab1024.sa.admin.module.business.player.domain.form.PlayerQueryForm;
import net.lab1024.sa.admin.module.business.player.domain.form.UpdatePlayerForm;
import net.lab1024.sa.admin.module.business.player.domain.vo.PlayerVO;

/**
 * 玩家服務接口
 *
 * @author SmartAdmin Architecture Team
 * @date 2026-01-31
 */
public interface PlayerService {

    /**
     * 分頁查詢玩家列表
     */
    PageResult<PlayerVO> queryPage(PlayerQueryForm queryForm);

    /**
     * 根據 ID 查詢玩家
     *
     * @param playerId 玩家 ID
     * @return Option.Some(PlayerVO) 如果找到，Option.None 如果未找到
     */
    Option<PlayerVO> findById(Long playerId);

    /**
     * 創建玩家
     *
     * @param form 創建表單
     * @return Either.Right(PlayerVO) 如果成功，Either.Left(ErrorCode) 如果失敗
     */
    Either<ErrorCode, PlayerVO> createPlayer(CreatePlayerForm form);

    /**
     * 更新玩家
     *
     * @param playerId 玩家 ID
     * @param form 更新表單
     * @return Either.Right(PlayerVO) 如果成功，Either.Left(ErrorCode) 如果失敗
     */
    Either<ErrorCode, PlayerVO> updatePlayer(Long playerId, UpdatePlayerForm form);

    /**
     * 刪除玩家 (軟刪除)
     *
     * @param playerId 玩家 ID
     * @return Either.Right(true) 如果成功，Either.Left(ErrorCode) 如果失敗
     */
    Either<ErrorCode, Boolean> deletePlayer(Long playerId);
}
```

#### 1.3 DTO/VO/Form 定義

```java
package net.lab1024.sa.admin.module.business.player.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 玩家 VO
 *
 * @author SmartAdmin Architecture Team
 * @date 2026-01-31
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "玩家信息")
public class PlayerVO {

    @Schema(description = "玩家唯一標識", example = "123456")
    private Long playerId;

    @Schema(description = "租戶 ID (多租戶隔離)", example = "1")
    private Long tenantId;

    @Schema(description = "用戶名 (唯一)", example = "john_doe")
    private String username;

    @Schema(description = "郵箱地址 (唯一)", example = "john@example.com")
    private String email;

    @Schema(description = "手機號碼", example = "+886912345678")
    private String phone;

    @Schema(description = "玩家狀態 (ACTIVE, SUSPENDED, SELF_EXCLUDED, BANNED)", example = "ACTIVE")
    private String status;

    @Schema(description = "KYC 驗證狀態 (NOT_VERIFIED, PENDING, VERIFIED, REJECTED)", example = "VERIFIED")
    private String kycStatus;

    @Schema(description = "VIP 等級 (0-10)", example = "5")
    private Integer vipLevel;

    @Schema(description = "註冊 IP 地址", example = "203.0.113.42")
    private String registrationIp;

    @Schema(description = "最後登錄時間", example = "2026-01-31T09:30:00")
    private LocalDateTime lastLoginAt;

    @Schema(description = "創建時間", example = "2026-01-15T14:22:00")
    private LocalDateTime createdAt;

    @Schema(description = "最後更新時間", example = "2026-01-31T10:00:00")
    private LocalDateTime updatedAt;
}
```

```java
package net.lab1024.sa.admin.module.business.player.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 創建玩家表單
 *
 * @author SmartAdmin Architecture Team
 * @date 2026-01-31
 */
@Data
@Schema(description = "創建玩家表單")
public class CreatePlayerForm {

    @NotBlank(message = "用戶名不能為空")
    @Size(min = 4, max = 20, message = "用戶名必須為 4-20 個字符")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "用戶名只能包含字母、數字、下劃線和連字符")
    @Schema(description = "用戶名", example = "john_doe", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotBlank(message = "密碼不能為空")
    @Size(min = 8, max = 32, message = "密碼必須為 8-32 個字符")
    @Schema(description = "密碼 (8-32 字符，需包含字母和數字)", example = "Password123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @NotBlank(message = "郵箱不能為空")
    @Email(message = "郵箱格式不正確")
    @Schema(description = "郵箱地址", example = "john@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @Schema(description = "手機號碼", example = "+886912345678")
    private String phone;

    @NotBlank(message = "幣種不能為空")
    @Pattern(regexp = "^(USD|EUR|CNY|TWD|THB|VND)$", message = "不支持的幣種")
    @Schema(description = "錢包幣種", example = "USD", requiredMode = Schema.RequiredMode.REQUIRED)
    private String currency;

    @Schema(description = "推薦碼 (可選)", example = "REF123ABC")
    private String referralCode;
}
```

---

### 案例 2: 紅利發放 API (Bonus Issuance)

#### 2.1 OpenAPI 定義

```yaml
paths:
  /bonuses/issue:
    post:
      tags:
        - Bonuses
      summary: 發放紅利
      description: |
        向玩家發放紅利

        **權限要求**: `bonus:issue`

        **業務規則**:
        - 檢查玩家狀態 (必須為 ACTIVE)
        - 檢查 KYC 狀態 (必須為 VERIFIED)
        - 檢查活動有效期
        - 計算流水要求 (wagering_requirement)
        - 冪等性檢查 (防重複發放)

        **冪等性**: 使用 `X-Idempotency-Key` 防止重複發放
      operationId: issueBonus
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              required:
                - player_id
                - promotion_id
                - bonus_amount
                - currency
              properties:
                player_id:
                  type: integer
                  format: int64
                  description: 玩家 ID
                  example: 123456
                promotion_id:
                  type: integer
                  format: int64
                  description: 活動 ID
                  example: 789
                bonus_amount:
                  type: number
                  format: double
                  description: 紅利金額
                  minimum: 0.01
                  example: 100.00
                currency:
                  type: string
                  enum: [USD, EUR, CNY, TWD, THB, VND]
                  description: 幣種
                  example: "USD"
                wagering_multiplier:
                  type: number
                  format: double
                  description: 流水倍數 (默認 20x)
                  default: 20.0
                  example: 20.0
                reason:
                  type: string
                  description: 發放原因
                  example: "首存紅利"
      responses:
        '201':
          description: 紅利發放成功
          content:
            application/json:
              schema:
                type: object
                properties:
                  code:
                    type: integer
                    example: 1001
                  message:
                    type: string
                    example: "紅利發放成功"
                  data:
                    type: object
                    properties:
                      bonus_id:
                        type: string
                        example: "bonus_abc123def456"
                      player_id:
                        type: integer
                        example: 123456
                      bonus_amount:
                        type: number
                        example: 100.00
                      currency:
                        type: string
                        example: "USD"
                      wagering_requirement:
                        type: number
                        description: 流水要求 (bonus_amount × multiplier)
                        example: 2000.00
                      remaining_wagering:
                        type: number
                        description: 剩餘流水要求
                        example: 2000.00
                      status:
                        type: string
                        enum: [PENDING, ACTIVE, COMPLETED, EXPIRED, CANCELLED]
                        example: "ACTIVE"
                      expires_at:
                        type: string
                        format: date-time
                        example: "2026-02-07T10:00:00Z"
                  timestamp:
                    type: string
                    example: "2026-01-31T10:00:00Z"
                  trace_id:
                    type: string
                    example: "abc-123-def-456"
        '400':
          description: 請求參數錯誤
        '403':
          description: 玩家狀態不符 (未 KYC、已禁用等)
        '409':
          description: 重複發放
        '422':
          description: 業務規則違反 (活動已過期等)
```

#### 2.2 Controller 實現

```java
@PostMapping("/issue")
@ResponseStatus(HttpStatus.CREATED)
@Operation(summary = "發放紅利", description = "向玩家發放紅利，自動計算流水要求")
public ResponseDTO<BonusVO> issueBonus(
        @Valid @RequestBody IssueBonusForm form,
        @RequestHeader("X-Idempotency-Key") String idempotencyKey) {
    log.info("發放紅利: playerId={}, promotionId={}, amount={}, idempotencyKey={}",
            form.getPlayerId(), form.getPromotionId(), form.getBonusAmount(), idempotencyKey);

    return bonusService.issueBonus(form, idempotencyKey)
            .map(ResponseDTO::ok)
            .getOrElseGet(error -> ResponseDTO.error(error.getCode(), error.getMessage()));
}
```

---

### 案例 3: 流水查詢 API (Wagering Query)

#### 3.1 OpenAPI 定義

```yaml
paths:
  /wagering/progress/{player_id}:
    get:
      tags:
        - Wagering
      summary: 查詢玩家流水進度
      description: |
        查詢玩家當前所有活躍紅利的流水進度

        **權限要求**: `wagering:read`

        **返回信息**:
        - 紅利基本信息
        - 流水要求總額
        - 已完成流水
        - 剩餘流水
        - 完成百分比
        - 過期時間
      operationId: getWageringProgress
      parameters:
        - name: player_id
          in: path
          required: true
          description: 玩家 ID
          schema:
            type: integer
            format: int64
            example: 123456
      responses:
        '200':
          description: 成功獲取流水進度
          content:
            application/json:
              schema:
                type: object
                properties:
                  code:
                    type: integer
                    example: 1000
                  message:
                    type: string
                    example: "Success"
                  data:
                    type: object
                    properties:
                      player_id:
                        type: integer
                        example: 123456
                      active_bonuses:
                        type: array
                        items:
                          type: object
                          properties:
                            bonus_id:
                              type: string
                              example: "bonus_abc123"
                            promotion_name:
                              type: string
                              example: "首存紅利"
                            bonus_amount:
                              type: number
                              example: 100.00
                            currency:
                              type: string
                              example: "USD"
                            wagering_requirement:
                              type: number
                              example: 2000.00
                            completed_wagering:
                              type: number
                              example: 1230.50
                            remaining_wagering:
                              type: number
                              example: 769.50
                            completion_percentage:
                              type: number
                              description: 完成百分比 (0-100)
                              example: 61.53
                            status:
                              type: string
                              example: "ACTIVE"
                            expires_at:
                              type: string
                              format: date-time
                              example: "2026-02-07T10:00:00Z"
```

#### 3.2 Controller 實現

```java
@GetMapping("/progress/{playerId}")
@Operation(summary = "查詢玩家流水進度", description = "查詢玩家當前所有活躍紅利的流水進度")
public ResponseDTO<WageringProgressVO> getWageringProgress(
        @Parameter(description = "玩家 ID", required = true)
        @PathVariable Long playerId) {
    log.info("查詢流水進度: playerId={}", playerId);

    return wageringService.getProgress(playerId)
            .map(ResponseDTO::ok)
            .getOrElse(() -> ResponseDTO.userErrorParam("玩家未找到"));
}
```

---

## 🧪 測試用例

### MockMvc 集成測試

```java
package net.lab1024.sa.admin.module.business.player.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.lab1024.sa.admin.module.business.player.domain.form.CreatePlayerForm;
import net.lab1024.sa.admin.module.business.player.domain.vo.PlayerVO;
import net.lab1024.sa.admin.module.business.player.service.PlayerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 玩家 Controller 測試
 *
 * @author SmartAdmin Architecture Team
 * @date 2026-01-31
 */
@WebMvcTest(PlayerController.class)
@DisplayName("玩家管理 API 測試")
class PlayerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PlayerService playerService;

    @Test
    @DisplayName("成功創建玩家 - 返回 201 Created")
    void testCreatePlayer_Success() throws Exception {
        // Given
        CreatePlayerForm form = new CreatePlayerForm();
        form.setUsername("john_doe");
        form.setPassword("Password123");
        form.setEmail("john@example.com");
        form.setCurrency("USD");

        PlayerVO expectedPlayer = PlayerVO.builder()
                .playerId(123456L)
                .username("john_doe")
                .email("john@example.com")
                .status("ACTIVE")
                .kycStatus("NOT_VERIFIED")
                .vipLevel(0)
                .build();

        when(playerService.createPlayer(any(CreatePlayerForm.class)))
                .thenReturn(io.vavr.control.Either.right(expectedPlayer));

        // When & Then
        mockMvc.perform(post("/api/v1/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-ID", "1")
                        .header("X-Request-ID", "550e8400-e29b-41d4-a716-446655440000")
                        .content(objectMapper.writeValueAsString(form)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.data.player_id").value(123456))
                .andExpect(jsonPath("$.data.username").value("john_doe"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("創建玩家失敗 - 用戶名重複 - 返回 409 Conflict")
    void testCreatePlayer_DuplicateUsername() throws Exception {
        // Given
        CreatePlayerForm form = new CreatePlayerForm();
        form.setUsername("existing_user");
        form.setPassword("Password123");
        form.setEmail("user@example.com");
        form.setCurrency("USD");

        when(playerService.createPlayer(any(CreatePlayerForm.class)))
                .thenReturn(io.vavr.control.Either.left(() -> 4013));

        // When & Then
        mockMvc.perform(post("/api/v1/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-ID", "1")
                        .header("X-Request-ID", "550e8400-e29b-41d4-a716-446655440000")
                        .content(objectMapper.writeValueAsString(form)))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(4013))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("創建玩家失敗 - 參數驗證失敗 - 返回 400 Bad Request")
    void testCreatePlayer_ValidationFailed() throws Exception {
        // Given
        CreatePlayerForm form = new CreatePlayerForm();
        form.setUsername("abc"); // 太短 (最少 4 個字符)
        form.setPassword("123"); // 太短 (最少 8 個字符)
        form.setEmail("invalid-email"); // 格式錯誤

        // When & Then
        mockMvc.perform(post("/api/v1/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-ID", "1")
                        .header("X-Request-ID", "550e8400-e29b-41d4-a716-446655440000")
                        .content(objectMapper.writeValueAsString(form)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(4009))
                .andExpect(jsonPath("$.message").value("參數驗證失敗"))
                .andExpect(jsonPath("$.data.username").exists())
                .andExpect(jsonPath("$.data.password").exists())
                .andExpect(jsonPath("$.data.email").exists());
    }
}
```

---

