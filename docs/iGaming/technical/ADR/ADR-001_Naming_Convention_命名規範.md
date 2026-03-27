---
title: "ADR-001: 命名規範 (Singular Standard)"
status: accepted
date: 2026-01-15
deciders: Tech Lead, Architecture Team
---

# ADR-001: 命名規範 (Singular Standard)

## 狀態

已接受 (Accepted)

## 背景

專案初期各模組命名風格不一致：資料表有複數 (players) 也有單數 (player)，Entity 類名後綴不統一，Service/Manager 層命名混亂。需要統一全專案命名規範。

## 決策

### 資料庫命名

| 類型 | 規則 | 正確範例 | 錯誤範例 |
|------|------|---------|---------|
| 表名 | 單數 + t_ 前綴 | `t_player` | `players`, `t_players` |
| 欄位 | snake_case | `player_id`, `created_at` | `playerId`, `CreatedAt` |
| 索引 | idx_{table}_{columns} | `idx_player_tenant_status` | `index1` |
| 唯一約束 | uk_{table}_{columns} | `uk_player_tenant_email` | `unique_email` |
| 外鍵 | fk_{table}_{ref_table} | `fk_wallet_player` | `foreign_key_1` |
| Boolean | 肯定形式 | `deleted`, `enabled` | `is_deleted`, `is_enabled` |

### Java 命名

| 類型 | 規則 | 正確範例 | 錯誤範例 |
|------|------|---------|---------|
| Entity | 單數 + Entity | `PlayerEntity` | `Players`, `PlayerModel` |
| VO | 單數 + VO | `PlayerVO` | `PlayerDto`, `PlayerBean` |
| Form | 單數 + Form | `LoginForm` | `LoginRequest`, `LoginDTO` |
| Dao | 單數 + Dao | `PlayerDao` | `PlayerMapper`, `PlayerRepository` |
| Service | 單數 + Service | `PlayerService` | `PlayersService` |
| Manager | 單數 + Manager | `WalletManager` | `WalletMgr` |
| Controller | 單數 + Controller | `PlayerController` | `PlayersController` |

### 套件結構

```
com.igaming.{module}/
├── controller/    # API 端點
├── service/       # 業務邏輯
├── manager/       # 事務 + 快取
├── dao/           # 資料存取
├── entity/        # 資料庫實體
├── vo/            # 值物件 (回傳)
├── form/          # 表單物件 (輸入)
├── constant/      # 常數
├── enums/         # 列舉
└── config/        # 設定
```

## 後果

- 正向：全專案命名一致，新成員學習成本低
- 正向：ArchUnit 可自動驗證命名合規
- 負向：需遷移已有的不合規命名 (一次性成本)

## 合規驗證

```java
@ArchTest
static ArchRule entitiesShouldEndWithEntity =
    classes().that().resideInAPackage("..entity..")
        .should().haveSimpleNameEndingWith("Entity");

@ArchTest
static ArchRule tablesShouldBeSingular =
    // 透過 Flyway migration 檢查
    // CREATE TABLE 語句必須使用 t_ 前綴 + 單數名
```
