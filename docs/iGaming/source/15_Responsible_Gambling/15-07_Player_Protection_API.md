# 15-07 Player Protection API (統一保護工具 API)

**版本**: 1.0.0
**創建日期**: 2026-02-07
**狀態**: ✅ 完整

---

## 概述

本文檔定義負責任博彩模塊的統一 API 設計，提供 RESTful 端點供前端和第三方系統使用。

---

## API 總覽

### 玩家端 API

| 端點 | 方法 | 說明 |
|------|------|------|
| `/api/v1/player/protection/settings` | GET | 獲取所有保護設定 |
| `/api/v1/player/protection/settings` | PUT | 更新保護設定 |
| `/api/v1/player/protection/deposit-limits` | GET | 獲取存款限額 |
| `/api/v1/player/protection/deposit-limits` | PUT | 設定存款限額 |
| `/api/v1/player/protection/loss-limits` | GET | 獲取虧損限額 |
| `/api/v1/player/protection/loss-limits` | PUT | 設定虧損限額 |
| `/api/v1/player/protection/self-exclusion` | POST | 申請自我排除 |
| `/api/v1/player/protection/self-exclusion/revoke` | POST | 申請解除排除 |
| `/api/v1/player/protection/cooling-off` | POST | 啟動冷靜期 |
| `/api/v1/player/protection/session-limits` | GET | 獲取會話限制 |
| `/api/v1/player/protection/session-limits` | PUT | 設定會話限制 |
| `/api/v1/player/protection/activity-history` | GET | 獲取活動歷史 |
| `/api/v1/player/protection/usage` | GET | 獲取限額使用情況 |

### 管理端 API

| 端點 | 方法 | 說明 |
|------|------|------|
| `/api/v1/admin/protection/exclusions` | GET | 排除玩家列表 |
| `/api/v1/admin/protection/exclusions/{playerId}` | GET | 玩家排除詳情 |
| `/api/v1/admin/protection/exclusions/{playerId}` | POST | 運營商排除玩家 |
| `/api/v1/admin/protection/reports` | GET | 合規報告 |
| `/api/v1/admin/protection/reports/export` | POST | 導出報告 |
| `/api/v1/admin/protection/gamstop/sync` | POST | Gamstop 同步 |
| `/api/v1/admin/protection/gamstop/check` | POST | Gamstop 查詢 |

---

## 詳細 API 設計

### 1. 獲取保護設定總覽

**請求**
```http
GET /api/v1/player/protection/settings
Authorization: Bearer {token}
```

**響應**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "selfExclusion": {
      "active": false,
      "endTime": null
    },
    "coolingOff": {
      "active": false,
      "endTime": null
    },
    "depositLimits": {
      "daily": 1000.00,
      "weekly": 5000.00,
      "monthly": 15000.00,
      "pendingChanges": []
    },
    "lossLimits": {
      "daily": 500.00,
      "weekly": 2000.00,
      "monthly": null,
      "pendingChanges": []
    },
    "sessionLimits": {
      "durationMinutes": 60,
      "idleTimeoutMinutes": 30,
      "realityCheckIntervalMinutes": 30
    },
    "lastUpdated": "2026-02-07T10:30:00Z"
  }
}
```

### 2. 設定存款限額

**請求**
```http
PUT /api/v1/player/protection/deposit-limits
Authorization: Bearer {token}
Content-Type: application/json

{
  "dailyLimit": 500.00,
  "weeklyLimit": 2000.00,
  "monthlyLimit": 8000.00
}
```

**響應**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "currentLimits": {
      "daily": 500.00,
      "weekly": 2000.00,
      "monthly": 8000.00
    },
    "pendingChanges": [
      {
        "type": "MONTHLY",
        "oldValue": 5000.00,
        "newValue": 8000.00,
        "effectiveTime": "2026-02-08T10:30:00Z",
        "status": "PENDING"
      }
    ],
    "message": "日限額和週限額已立即生效，月限額提高將於 24 小時後生效"
  }
}
```

### 3. 申請自我排除

**請求**
```http
POST /api/v1/player/protection/self-exclusion
Authorization: Bearer {token}
Content-Type: application/json

{
  "duration": "6M",
  "reason": "需要休息一段時間",
  "coolingOffConfirmed": true
}
```

**響應**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "exclusionId": 12345,
    "status": "ACTIVE",
    "startTime": "2026-02-07T10:30:00Z",
    "endTime": "2026-08-07T10:30:00Z",
    "duration": "6M",
    "message": "自我排除已生效，將於 2026-08-07 結束"
  }
}
```

**期限選項**
| 值 | 說明 |
|----|------|
| `24H` | 24 小時 |
| `7D` | 7 天 |
| `30D` | 30 天 |
| `6M` | 6 個月 |
| `1Y` | 1 年 |
| `5Y` | 5 年 |
| `PERMANENT` | 永久 |

### 4. 啟動冷靜期

**請求**
```http
POST /api/v1/player/protection/cooling-off
Authorization: Bearer {token}
Content-Type: application/json

{
  "duration": "D7",
  "reason": "短暫休息"
}
```

**響應**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "status": "ACTIVE",
    "startTime": "2026-02-07T10:30:00Z",
    "endTime": "2026-02-14T10:30:00Z",
    "message": "冷靜期已啟動，將於 2026-02-14 自動結束"
  }
}
```

### 5. 獲取限額使用情況

**請求**
```http
GET /api/v1/player/protection/usage
Authorization: Bearer {token}
```

**響應**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "depositLimits": {
      "daily": {
        "limit": 1000.00,
        "used": 350.00,
        "remaining": 650.00,
        "percentage": 35,
        "resetTime": "2026-02-08T00:00:00Z"
      },
      "weekly": {
        "limit": 5000.00,
        "used": 1200.00,
        "remaining": 3800.00,
        "percentage": 24,
        "resetTime": "2026-02-12T00:00:00Z"
      },
      "monthly": {
        "limit": 15000.00,
        "used": 4500.00,
        "remaining": 10500.00,
        "percentage": 30,
        "resetTime": "2026-03-01T00:00:00Z"
      }
    },
    "lossLimits": {
      "daily": {
        "limit": 500.00,
        "current": 120.00,
        "remaining": 380.00,
        "percentage": 24,
        "resetTime": "2026-02-08T00:00:00Z"
      },
      "weekly": {
        "limit": 2000.00,
        "current": 450.00,
        "remaining": 1550.00,
        "percentage": 22.5,
        "resetTime": "2026-02-12T00:00:00Z"
      },
      "monthly": null
    },
    "session": {
      "currentDuration": 45,
      "limitMinutes": 60,
      "nextRealityCheck": "2026-02-07T11:00:00Z"
    }
  }
}
```

### 6. 獲取活動歷史

**請求**
```http
GET /api/v1/player/protection/activity-history
Authorization: Bearer {token}
```

**查詢參數**
| 參數 | 類型 | 必填 | 說明 |
|------|------|------|------|
| `startDate` | String | 否 | 開始日期 (YYYY-MM-DD) |
| `endDate` | String | 否 | 結束日期 (YYYY-MM-DD) |
| `type` | String | 否 | 活動類型: EXCLUSION, LIMIT_CHANGE, COOLING_OFF, REALITY_CHECK |
| `page` | Integer | 否 | 頁碼，預設 1 |
| `pageSize` | Integer | 否 | 每頁筆數，預設 20 |

**響應**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "total": 45,
    "page": 1,
    "pageSize": 20,
    "records": [
      {
        "id": 1001,
        "type": "LIMIT_CHANGE",
        "subType": "DEPOSIT_LIMIT",
        "action": "DECREASE",
        "oldValue": "1500.00",
        "newValue": "1000.00",
        "effectiveTime": "2026-02-06T15:30:00Z",
        "createdAt": "2026-02-06T15:30:00Z"
      },
      {
        "id": 1002,
        "type": "REALITY_CHECK",
        "sessionDuration": 60,
        "netResult": -150.00,
        "response": "CONTINUE",
        "createdAt": "2026-02-05T20:00:00Z"
      }
    ]
  }
}
```

---

## 管理端 API

### 7. 獲取排除玩家列表

**請求**
```http
GET /api/v1/admin/protection/exclusions
Authorization: Bearer {admin_token}
```

**查詢參數**
| 參數 | 類型 | 說明 |
|------|------|------|
| `status` | String | ACTIVE, COMPLETED, PENDING_REVOCATION |
| `type` | String | SELF, OPERATOR, GAMSTOP |
| `page` | Integer | 頁碼 |
| `pageSize` | Integer | 每頁筆數 |

**響應**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "total": 128,
    "page": 1,
    "pageSize": 20,
    "records": [
      {
        "id": 5001,
        "playerId": 10001,
        "playerName": "player***",
        "email": "p***@email.com",
        "exclusionType": "SELF",
        "duration": "6M",
        "startTime": "2026-01-15T10:00:00Z",
        "endTime": "2026-07-15T10:00:00Z",
        "status": "ACTIVE",
        "gamstopSynced": true,
        "createdAt": "2026-01-15T10:00:00Z"
      }
    ]
  }
}
```

### 8. 運營商排除玩家

**請求**
```http
POST /api/v1/admin/protection/exclusions/{playerId}
Authorization: Bearer {admin_token}
Content-Type: application/json

{
  "duration": "1Y",
  "reason": "疑似問題博彩行為",
  "notifyPlayer": true
}
```

**響應**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "exclusionId": 5002,
    "playerId": 10002,
    "status": "ACTIVE",
    "message": "玩家已被排除，並已發送通知"
  }
}
```

### 9. 生成合規報告

**請求**
```http
GET /api/v1/admin/protection/reports
Authorization: Bearer {admin_token}
```

**查詢參數**
| 參數 | 類型 | 說明 |
|------|------|------|
| `reportType` | String | MONTHLY, QUARTERLY, ANNUAL |
| `period` | String | 報告週期 (YYYY-MM 或 YYYY-Q1) |
| `format` | String | JSON, CSV, PDF |

**響應**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "reportId": "RPT-2026-02",
    "period": "2026-02",
    "generatedAt": "2026-02-07T12:00:00Z",
    "summary": {
      "selfExclusions": {
        "new": 45,
        "active": 128,
        "completed": 12,
        "revoked": 3
      },
      "depositLimits": {
        "playersWithLimits": 3456,
        "limitBreaches": 234,
        "averageLimit": 1250.00
      },
      "lossLimits": {
        "playersWithLimits": 1234,
        "limitBreaches": 89
      },
      "realityChecks": {
        "total": 12456,
        "continueRate": 0.78,
        "averageResponseTime": 8.5
      },
      "gamstopSync": {
        "checksPerformed": 2345,
        "matchesFound": 12,
        "syncFailures": 0
      }
    },
    "downloadUrl": "/api/v1/admin/protection/reports/RPT-2026-02/download"
  }
}
```

### 10. Gamstop 查詢

**請求**
```http
POST /api/v1/admin/protection/gamstop/check
Authorization: Bearer {admin_token}
Content-Type: application/json

{
  "playerId": 10001,
  "firstName": "John",
  "lastName": "Doe",
  "dateOfBirth": "1990-05-15",
  "postcode": "SW1A 1AA"
}
```

**響應**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "excluded": false,
    "checkTime": "2026-02-07T12:00:00Z",
    "reference": "GS-CHK-123456"
  }
}
```

---

## 錯誤碼

| 錯誤碼 | 說明 |
|--------|------|
| `RG_001` | 玩家已被排除 |
| `RG_002` | 已在冷靜期中 |
| `RG_003` | 超過存款限額 |
| `RG_004` | 超過虧損限額 |
| `RG_005` | 無法解除永久排除 |
| `RG_006` | 排除期未滿 |
| `RG_007` | 無待生效的限額變更 |
| `RG_008` | 強制休息中 |
| `RG_009` | Gamstop 同步失敗 |
| `RG_010` | 限額設定無效（層級錯誤）|

---

## Controller 實現

```java
@RestController
@RequestMapping("/api/v1/player/protection")
@RequiredArgsConstructor
@Tag(name = "玩家保護", description = "負責任博彩相關 API")
public class PlayerProtectionController {

    private final PlayerProtectionService protectionService;
    private final DepositLimitService depositLimitService;
    private final LossLimitService lossLimitService;
    private final SelfExclusionService selfExclusionService;
    private final CoolingOffService coolingOffService;
    private final SessionManagementService sessionService;

    @GetMapping("/settings")
    @Operation(summary = "獲取保護設定總覽")
    public ResponseDTO<ProtectionSettingsVO> getSettings() {
        Long playerId = StpUtil.getLoginIdAsLong();
        return ResponseDTO.ok(protectionService.getSettings(playerId));
    }

    @PutMapping("/deposit-limits")
    @Operation(summary = "設定存款限額")
    public ResponseDTO<LimitUpdateResultVO> setDepositLimits(
            @Valid @RequestBody DepositLimitForm form) {
        Long playerId = StpUtil.getLoginIdAsLong();
        return depositLimitService.setDepositLimits(playerId, form);
    }

    @PutMapping("/loss-limits")
    @Operation(summary = "設定虧損限額")
    public ResponseDTO<LimitUpdateResultVO> setLossLimits(
            @Valid @RequestBody LossLimitForm form) {
        Long playerId = StpUtil.getLoginIdAsLong();
        return lossLimitService.setLossLimits(playerId, form);
    }

    @PostMapping("/self-exclusion")
    @Operation(summary = "申請自我排除")
    public ResponseDTO<ExclusionResultVO> requestSelfExclusion(
            @Valid @RequestBody SelfExclusionForm form) {
        Long playerId = StpUtil.getLoginIdAsLong();
        return selfExclusionService.requestSelfExclusion(playerId, form);
    }

    @PostMapping("/cooling-off")
    @Operation(summary = "啟動冷靜期")
    public ResponseDTO<CoolingOffResultVO> startCoolingOff(
            @Valid @RequestBody CoolingOffForm form) {
        Long playerId = StpUtil.getLoginIdAsLong();
        return coolingOffService.startCoolingOff(playerId, form);
    }

    @GetMapping("/usage")
    @Operation(summary = "獲取限額使用情況")
    public ResponseDTO<LimitUsageVO> getLimitUsage() {
        Long playerId = StpUtil.getLoginIdAsLong();
        return ResponseDTO.ok(protectionService.getLimitUsage(playerId));
    }

    @GetMapping("/activity-history")
    @Operation(summary = "獲取活動歷史")
    public ResponseDTO<PageResult<ActivityHistoryVO>> getActivityHistory(
            @Valid ActivityHistoryQueryForm queryForm) {
        Long playerId = StpUtil.getLoginIdAsLong();
        return ResponseDTO.ok(protectionService.getActivityHistory(playerId, queryForm));
    }
}
```

---

## 相關文檔

- [15-01_Self_Exclusion.md](15-01_Self_Exclusion.md) - 自我排除
- [15-02_Deposit_Limits.md](15-02_Deposit_Limits.md) - 存款限額
- [09-03-04_Domain_APIs.md](../09_Technical_Infrastructure/09-03-04_Domain_APIs.md) - API 設計規範

---

**返回**: [負責任博彩模塊](README.md) | [iGaming 首頁](../README.md)
