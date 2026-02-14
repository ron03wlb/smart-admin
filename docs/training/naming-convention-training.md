# SmartAdmin 命名規範培訓材料

**版本**: 1.0.0
**最後更新**: 2026-02-02
**維護團隊**: SmartAdmin Architecture Team
**目標受眾**: 新入職開發者、現有團隊成員

---

## 📚 培訓目標

完成本培訓後，您將能夠：

1. ✅ 理解 SmartAdmin 單數命名標準的原因和重要性
2. ✅ 正確命名 Entity 類、數據庫表、Package、Java 字段
3. ✅ 識別和修正命名違規
4. ✅ 使用自動化工具檢查命名規範
5. ✅ 理解豁免清單及其應用場景

---

## 🎯 快速參考卡片（必背）

### 核心原則：單數形式

| 層級 | 規範 | ✅ 正確 | ❌ 錯誤 |
|------|------|---------|---------|
| **Entity 類** | 單數 + Entity 後綴 | `PlayerEntity` | `PlayersEntity` |
| **數據庫表** | `t_` + 單數 | `t_player` | `t_players` |
| **Package** | 單數小寫 | `player.domain` | `players.domain` |
| **Java 字段** | 單數（集合用複數） | `Player player`<br>`List<Player> players` | `Players player` |
| **API 路徑** | **複數**（RESTful） | `/api/v1/players` | `/api/v1/player` |

### 豁免清單（僅 3 個）

| 表名模式 | 原因 | 範例 |
|---------|------|------|
| `t_*_metrics` | 不可數名詞 | `t_liteflow_execution_metrics` |
| `t_*_statistics` | 慣用複數 | `t_user_statistics` |
| `t_*_analytics` | 慣用複數 | `t_player_analytics` |

### 記憶口訣

```
一表一物用單數，
Entity 類也單數，
API 路徑 RESTful 複數例外，
統計分析三豁免。
```

---

## 📖 第一部分：為什麼使用單數命名？

### 1.1 核心理念

**數據庫表名代表「一行記錄」，而非「整個集合」**

```java
// ✅ 正確理解
t_player  // 表示「一位玩家」的數據
PlayerEntity  // 表示「一位玩家」的實體對象

// ❌ 錯誤理解
t_players  // 容易誤解為「所有玩家」的集合
```

### 1.2 好處

**1. 概念清晰**
- `INSERT INTO t_player` → 插入「一位」玩家 ✅
- `INSERT INTO t_players` → 插入「多位」玩家？❌ 混淆

**2. 代碼一致性**
```java
// Entity 類名 = 表名（單數）
@TableName("t_player")
public class PlayerEntity {  // 類名也是單數
    // ...
}
```

**3. 避免不規則複數**
```java
// 不規則複數會造成困擾
t_person  → t_people? t_persons?
t_child   → t_children? t_childs?

// 單數統一規則
t_person  // 清晰明確
t_child   // 清晰明確
```

**4. 符合 ORM 慣例**
- MyBatis Plus: `@TableName` 映射單數表名
- JPA/Hibernate: 默認單數命名
- ActiveRecord: 單數 Model → 複數表（但我們統一用單數）

### 1.3 業界對比

| 框架/項目 | 表命名標準 | SmartAdmin 選擇 |
|----------|----------|----------------|
| Rails ActiveRecord | 複數 | ❌ 不採用 |
| Django | 複數 | ❌ 不採用 |
| Laravel | 複數 | ❌ 不採用 |
| Spring JPA | 單數（推薦） | ✅ 採用 |
| MyBatis Plus | 單數（推薦） | ✅ 採用 |

**結論**: SmartAdmin 遵循 Java 生態主流標準（單數）。

---

## 📝 第二部分：命名規範詳解

### 2.1 Entity 類命名

**規則**: `{業務名稱}Entity`（單數）

**範例**:
```java
// ✅ 正確
public class PlayerEntity { }
public class WalletEntity { }
public class TransactionEntity { }
public class BonusEntity { }

// ❌ 錯誤
public class PlayersEntity { }      // 複數
public class Player { }             // 缺少 Entity 後綴
public class PlayerEntityClass { }  // 多餘 Class 後綴
```

**特殊情況**:
```java
// ✅ 不規則複數 - 仍用單數
public class PersonEntity { }   // 不是 PeopleEntity
public class ChildEntity { }    // 不是 ChildrenEntity

// ✅ 縮寫詞 - 全大寫或首字母大寫
public class VIPPlayerEntity { }  // 或 VipPlayerEntity（推薦）
public class APILogEntity { }     // 或 ApiLogEntity（推薦）
```

### 2.2 數據庫表命名

**規則**: `t_{業務名稱}`（單數，小寫，下劃線分隔）

**範例**:
```sql
-- ✅ 正確
CREATE TABLE t_player (...);
CREATE TABLE t_wallet (...);
CREATE TABLE t_transaction (...);
CREATE TABLE t_vip_level (...);

-- ❌ 錯誤
CREATE TABLE t_players (...);        -- 複數
CREATE TABLE player (...);           -- 缺少 t_ 前綴
CREATE TABLE t_Player (...);         -- 大小寫混用
CREATE TABLE t_vip-level (...);      -- 使用連字符
```

**@TableName 註解**:
```java
@Data
@TableName("t_player")  // ✅ 單數
public class PlayerEntity {
    @TableId(type = IdType.AUTO)
    private Long playerId;
    // ...
}
```

### 2.3 Package 命名

**規則**: 全小寫，單數，點分隔

**範例**:
```java
// ✅ 正確
net.lab1024.sa.admin.module.business.player.domain.entity
net.lab1024.sa.admin.module.business.wallet.service
net.lab1024.sa.admin.module.business.transaction.manager

// ❌ 錯誤
net.lab1024.sa.admin.module.business.players.domain.entity  // 複數
net.lab1024.sa.admin.module.business.Player.domain.entity   // 大寫
net.lab1024.sa.admin.module.business.player_domain.entity   // 下劃線
```

**組織結構**:
```
src/main/java/net/lab1024/sa/admin/module/business/
├── player/              (✅ 單數)
│   ├── controller/
│   ├── domain/
│   │   ├── entity/      PlayerEntity
│   │   ├── form/        PlayerAddForm, PlayerUpdateForm
│   │   └── vo/          PlayerVO
│   ├── service/         PlayerService
│   ├── manager/         PlayerManager
│   └── dao/             PlayerDao
```

### 2.4 Java 字段命名

**規則**:
- 單個對象 → 單數
- 集合/數組 → 複數

**範例**:
```java
public class OrderEntity {
    // ✅ 正確 - 單個對象用單數
    private Long orderId;
    private Player player;          // 關聯單個玩家
    private Wallet wallet;          // 關聯單個錢包

    // ✅ 正確 - 集合用複數
    private List<OrderItem> orderItems;      // 訂單項目列表
    private Set<String> tags;                // 標籤集合
    private Map<String, Object> metadata;    // 元數據字典

    // ❌ 錯誤
    private Player players;         // 單個對象用複數
    private List<OrderItem> item;   // 集合用單數
}
```

**Boolean 字段**:
```java
// ✅ 正確
private Boolean deleted;        // 不是 isDeleted
private Boolean enabled;        // 不是 isEnabled
private Boolean verified;       // 不是 isVerified

// ❌ 錯誤（SmartAdmin 禁止）
private Boolean isDeleted;      // MyBatis Plus 序列化問題
private Boolean isEnabled;
```

### 2.5 API 路徑命名

**規則**: RESTful 資源集合使用**複數**（例外）

**範例**:
```java
@RestController
@RequestMapping("/api/v1/players")  // ✅ API 路徑用複數
public class PlayerController {

    // ✅ 正確 - RESTful 標準
    @GetMapping                         // GET /api/v1/players
    public ResponseDTO<List<PlayerVO>> list() { }

    @GetMapping("/{id}")                // GET /api/v1/players/123
    public ResponseDTO<PlayerVO> detail(@PathVariable Long id) { }

    @PostMapping                        // POST /api/v1/players
    public ResponseDTO<Void> add(@RequestBody PlayerAddForm form) { }

    // ✅ 子資源也用複數
    @GetMapping("/{playerId}/wallets")  // GET /api/v1/players/123/wallets
    public ResponseDTO<List<WalletVO>> getWallets(@PathVariable Long playerId) { }
}
```

**為什麼 API 用複數？**
- RESTful 標準慣例（資源集合用複數）
- 語義清晰：`/players` 表示「所有玩家的集合」
- 業界廣泛採用（GitHub API, Twitter API 等）

**重要**: API 路徑複數 ≠ 數據庫表複數

```
API:    /api/v1/players    (複數) ✅ RESTful 慣例
        ↓
Controller: PlayerController (單數)
        ↓
Service:    PlayerService   (單數)
        ↓
Entity:     PlayerEntity    (單數)
        ↓
Table:      t_player        (單數) ✅ SmartAdmin 標準
```

---

## 🚫 第三部分：常見錯誤與修正

### 3.1 複數表名錯誤

**錯誤範例 1: 商品表**
```java
// ❌ 錯誤
@TableName("t_goods")
public class GoodsEntity { }
```

**修正**:
```java
// ✅ 正確
@TableName("t_good")
public class GoodsEntity {  // 類名保持 GoodsEntity（goods 本身是複數形式）
    private Long goodId;    // 字段用 goodId
    // ...
}
```

**數據庫遷移**:
```sql
-- V1.2__rename_goods_table.sql
ALTER TABLE t_goods RENAME TO t_good;
```

---

**錯誤範例 2: 用戶表**
```java
// ❌ 錯誤
@TableName("t_users")
public class UserEntity { }
```

**修正**:
```java
// ✅ 正確
@TableName("t_user")
public class UserEntity {
    private Long userId;
    // ...
}
```

---

**錯誤範例 3: 訂單表**
```java
// ❌ 錯誤
@TableName("t_orders")
public class OrderEntity { }
```

**修正**:
```java
// ✅ 正確
@TableName("t_order")
public class OrderEntity {
    private Long orderId;
    // ...
}
```

### 3.2 Package 複數錯誤

**錯誤範例**:
```
// ❌ 錯誤
net.lab1024.sa.admin.module.business.players.domain.entity
net.lab1024.sa.admin.module.business.orders.service
```

**修正**:
```
// ✅ 正確
net.lab1024.sa.admin.module.business.player.domain.entity
net.lab1024.sa.admin.module.business.order.service
```

### 3.3 Boolean 字段錯誤

**錯誤範例**:
```java
// ❌ 錯誤
@Data
public class PlayerEntity {
    private Boolean isDeleted;   // MyBatis Plus 序列化問題
    private Boolean isEnabled;
}
```

**修正**:
```java
// ✅ 正確
@Data
public class PlayerEntity {
    private Boolean deleted;     // SmartAdmin 標準
    private Boolean enabled;
}
```

**原因**: MyBatis Plus 與 Lombok 的 `@Data` 註解會為 `isXxx` 字段生成 `isIsXxx()` getter，導致序列化錯誤。

---

## 🛡️ 第四部分：豁免清單（3 個特例）

### 4.1 為什麼需要豁免？

某些詞彙在英文中**慣用複數形式**或**無單數形式**，強制使用單數會違背語言慣例。

### 4.2 豁免規則詳解

#### 豁免 1: `t_*_metrics`

**原因**: `metrics` 是不可數名詞，表示聚合統計數據，無對應單數形式。

**範例**:
```java
// ✅ 允許使用複數
@TableName("t_liteflow_execution_metrics")
public class LiteFlowExecutionMetricsEntity {
    private Long metricsId;
    private String chainName;
    private Long executionCount;
    private Long averageDuration;
    // ...
}
```

**語言慣例**:
- 英文中不說 "a metric"（表示單個數據點時用 "a data point"）
- 說 "performance metrics"、"execution metrics"（聚合數據）

---

#### 豁免 2: `t_*_statistics`

**原因**: `statistics` 在統計學領域慣用複數，表示多個統計數據的集合。

**範例**:
```java
// ✅ 允許使用複數
@TableName("t_user_statistics")
public class UserStatisticsEntity {
    private Long statisticsId;
    private Long userId;
    private Integer loginCount;
    private BigDecimal totalDeposit;
    // ...
}

@TableName("t_game_statistics")
public class GameStatisticsEntity {
    private Long statisticsId;
    private Long gameId;
    private Integer playCount;
    // ...
}
```

**語言慣例**:
- 英文中很少說 "a statistic"（個別統計值）
- 慣用 "user statistics"、"game statistics"（統計報表）

---

#### 豁免 3: `t_*_analytics`

**原因**: `analytics` 在商業智能領域慣用複數，表示多維度分析數據。

**範例**:
```java
// ✅ 允許使用複數
@TableName("t_player_analytics")
public class PlayerAnalyticsEntity {
    private Long analyticsId;
    private Long playerId;
    private String behaviorPattern;
    private BigDecimal riskScore;
    private BigDecimal ltv;  // Lifetime Value
    // ...
}

@TableName("t_behavior_analytics")
public class BehaviorAnalyticsEntity {
    private Long analyticsId;
    private Long userId;
    private String eventType;
    // ...
}
```

**語言慣例**:
- Google Analytics, Adobe Analytics 等工具使用複數
- 數據倉庫標準傾向 `analytics` 而非 `analytic`

### 4.3 如何判斷是否適用豁免？

**檢查清單**:
1. ✅ 該詞彙在英文中慣用複數形式？
2. ✅ 業界標準或權威文檔支持使用複數？
3. ✅ 使用複數更符合業務語義（聚合數據、統計報表）？

**範例判斷**:

| 表名 | 是否豁免？ | 原因 |
|------|----------|------|
| `t_user_metrics` | ✅ 是 | 符合豁免規則 1 |
| `t_daily_statistics` | ✅ 是 | 符合豁免規則 2 |
| `t_web_analytics` | ✅ 是 | 符合豁免規則 3 |
| `t_users` | ❌ 否 | `user` 有明確單數形式 |
| `t_orders` | ❌ 否 | `order` 有明確單數形式 |
| `t_order_histories` | ❌ 否 | `history` 本身已是單數 |

### 4.4 新增豁免申請流程

**如果您認為需要新增豁免項目**:

1. **收集證據**:
   - 語言慣例證明（詞典、語法書）
   - 業界標準證明（ISO 標準、權威文檔）
   - 業務語義證明（為什麼複數更合理）

2. **提交 Issue**:
   - 標題: `[Naming Exemption] 申請豁免表名 t_xxx_yyy`
   - 標籤: `naming-exemption`
   - 內容: 包含上述三類證據

3. **架構團隊審查**:
   - 評估合理性
   - 投票決策（需 2/3 同意）

4. **更新文檔**（如批准）:
   - `.agent/rules/foundation/01-naming-conventions.md`
   - `ArchitectureTest.java`
   - 豁免清單文檔

**警告**: 豁免清單應保持精簡（目標 ≤ 5 個），避免過度膨脹。

---

## 🔧 第五部分：自動化檢查工具

### 5.1 本地檢查：ArchUnit 測試

**運行命令**:
```bash
cd smart-admin-api-java21-springboot3
./gradlew :sa-admin:test --tests ArchitectureTest#tableNameMustBeSingular
```

**預期結果**:
```
✅ BUILD SUCCESSFUL in 30s
5 actionable tasks: 2 executed, 3 up-to-date

ArchitectureTest > tableNameMustBeSingular() PASSED
```

**如果失敗**:
```
ArchitectureTest > tableNameMustBeSingular() FAILED
    java.lang.AssertionError: Architecture Violation [Priority: MEDIUM]
    Rule 'Entity 表名必須使用單數形式' was violated (1 times):
    表名 't_goods' 使用了複數形式，應改為單數形式 't_good' in (GoodsEntity.java:0)
```

**修正步驟**:
1. 打開提示的文件（例如 `GoodsEntity.java`）
2. 修改 `@TableName` 註解為單數形式
3. 創建數據庫遷移腳本（如需要）
4. 更新 Mapper XML 中的表名引用
5. 重新運行測試

### 5.2 Git Pre-commit Hook

**安裝步驟**:
```bash
# Step 1: 配置 Git Hooks 目錄
git config core.hooksPath .githooks

# Step 2: 驗證配置
git config core.hooksPath
# 輸出: .githooks

# Step 3: (Linux/Mac) 賦予執行權限
chmod +x .githooks/pre-commit
```

**測試 Hook**:
```bash
# 創建測試文件（故意違規）
echo '@TableName("t_players")' > TestEntity.java
git add TestEntity.java
git commit -m "test"

# 預期: Hook 阻止提交並顯示錯誤
```

**正常提交**:
```bash
# 修正後再次提交
echo '@TableName("t_player")' > TestEntity.java
git add TestEntity.java
git commit -m "fix: correct table name to singular"

# 預期: 提交成功
```

**緊急繞過**（僅限緊急情況）:
```bash
git commit --no-verify -m "emergency fix"
```

### 5.3 CI/CD 檢查：GitHub Actions

**觸發條件**: 當 PR 修改了 Entity 文件時自動運行

**查看結果**:
1. 打開 PR 頁面
2. 查看 "Checks" 標籤
3. 點擊 "Naming Convention Check"

**如果失敗**:
- PR 會自動添加評論，說明違規詳情
- 提供修正指南
- 阻止合併，直到修正完成

### 5.4 Claude Code Skill（可選）

**調用命令**（僅限 Claude Code）:
```
/naming-convention-checker
```

**功能**:
- 掃描所有 Entity 類
- 檢測複數表名
- 生成違規報告

---

## 💡 第六部分：實戰演練

### 練習 1: 創建新 Entity

**任務**: 創建一個玩家等級表

**步驟**:

1. **創建 Entity 類**:
```java
package net.lab1024.sa.admin.module.business.player.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_player_level")  // ✅ 單數: level（不是 levels）
public class PlayerLevelEntity {

    @TableId(type = IdType.AUTO)
    private Long levelId;

    private String levelName;      // VIP1, VIP2, etc.
    private Integer requiredPoints; // 所需積分
    private Boolean deleted;        // ✅ 不是 isDeleted

    // ...
}
```

2. **創建數據庫表**:
```sql
-- db/migration/V1.3__create_player_level_table.sql
CREATE TABLE t_player_level (
    level_id BIGSERIAL PRIMARY KEY,
    level_name VARCHAR(50) NOT NULL,
    required_points INT NOT NULL,
    deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

3. **創建 Mapper**:
```java
package net.lab1024.sa.admin.module.business.player.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.lab1024.sa.admin.module.business.player.domain.entity.PlayerLevelEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PlayerLevelDao extends BaseMapper<PlayerLevelEntity> {
}
```

4. **運行 ArchUnit 測試驗證**:
```bash
./gradlew :sa-admin:test --tests ArchitectureTest#tableNameMustBeSingular
```

**預期**: ✅ 測試通過

---

### 練習 2: 修正違規

**任務**: 修正一個使用複數表名的 Entity

**給定代碼**:
```java
@Data
@TableName("t_orders")  // ❌ 違規：複數
public class OrderEntity {
    @TableId(type = IdType.AUTO)
    private Long orderId;
    // ...
}
```

**修正步驟**:

1. **修改 Entity 類**:
```java
@Data
@TableName("t_order")  // ✅ 修正為單數
public class OrderEntity {
    @TableId(type = IdType.AUTO)
    private Long orderId;
    // ...
}
```

2. **創建遷移腳本**:
```sql
-- V1.4__rename_orders_table.sql
ALTER TABLE t_orders RENAME TO t_order;
```

3. **更新 Mapper XML**:
```xml
<!-- OrderMapper.xml -->
<select id="query" resultType="OrderEntity">
    SELECT * FROM t_order  <!-- 改為 t_order -->
    WHERE deleted = false
</select>

<update id="updateStatus">
    UPDATE t_order SET status = #{status}  <!-- 改為 t_order -->
    WHERE order_id = #{orderId}
</update>
```

4. **驗證修改**:
```bash
# 運行測試
./gradlew :sa-admin:test

# 運行數據庫遷移
./gradlew :sa-admin:flywayMigrate

# 運行 ArchUnit 測試
./gradlew :sa-admin:test --tests ArchitectureTest#tableNameMustBeSingular
```

**預期**: ✅ 所有測試通過

---

### 練習 3: 判斷豁免

**任務**: 判斷以下表名是否需要豁免

| 表名 | 是否豁免？ | 理由 |
|------|----------|------|
| `t_player_metrics` | ? | ? |
| `t_users` | ? | ? |
| `t_daily_statistics` | ? | ? |
| `t_order_histories` | ? | ? |
| `t_web_analytics` | ? | ? |

**答案**:
<details>
<summary>點擊查看答案</summary>

| 表名 | 是否豁免？ | 理由 |
|------|----------|------|
| `t_player_metrics` | ✅ 是 | 符合豁免規則 1 (`t_*_metrics`) |
| `t_users` | ❌ 否 | `user` 有明確單數形式，應改為 `t_user` |
| `t_daily_statistics` | ✅ 是 | 符合豁免規則 2 (`t_*_statistics`) |
| `t_order_histories` | ❌ 否 | `history` 本身已是單數，應改為 `t_order_history` |
| `t_web_analytics` | ✅ 是 | 符合豁免規則 3 (`t_*_analytics`) |

</details>

---

## ❓ 第七部分：常見問題 FAQ

### Q1: API 路徑為什麼用複數，而數據庫表用單數？

**A**: 兩個不同的標準：

- **API 路徑**（複數）: 遵循 RESTful 標準，`/api/v1/players` 表示「玩家資源集合」
- **數據庫表**（單數）: 遵循 SmartAdmin 標準，`t_player` 表示「一行玩家記錄」

**核心理念**:
- API 是「對外接口」，面向資源集合
- 數據庫表是「內部存儲」，面向單條記錄

**類比**:
```
圖書館 (API)
└─ 書架: "計算機書籍" (複數，集合概念)
    └─ 單本書: 《Java 編程》(單數，單個實體)
```

---

### Q2: `goods` 本身就是複數，怎麼辦？

**A**: 區分「語法複數」和「語義複數」。

**方案 1: 使用單數形式**（SmartAdmin 推薦）
```java
@TableName("t_good")  // ✅ 單數形式
public class GoodsEntity {  // 類名保留 Goods（習慣用法）
    private Long goodId;
    // ...
}
```

**方案 2: 重新命名**（如果 `good` 不自然）
```java
@TableName("t_product")  // ✅ 使用同義詞
public class ProductEntity {
    private Long productId;
    // ...
}
```

**實際案例**: SmartAdmin 選擇方案 1，數據庫表用 `t_good`，類名保持 `GoodsEntity`。

---

### Q3: 集合字段用單數還是複數？

**A**: 集合用複數，單個對象用單數。

```java
public class OrderEntity {
    // ✅ 單個對象 - 單數
    private Player player;
    private Wallet wallet;

    // ✅ 集合 - 複數
    private List<OrderItem> orderItems;
    private Set<String> tags;
    private Map<String, Object> metadata;
}
```

**記憶方法**: 變量名要準確反映類型（單個 vs 多個）。

---

### Q4: 第三方庫使用複數表名怎麼辦？

**A**: 三種處理方式：

**方案 1: 修改第三方代碼**（不推薦）
- 風險高，升級困難

**方案 2: 使用視圖映射**（推薦）
```sql
-- 創建單數視圖映射到複數表
CREATE VIEW t_third_party_user AS
SELECT * FROM third_party_users;
```

**方案 3: 申請豁免**（需充分理由）
- 提交豁免申請 Issue
- 架構團隊評估

---

### Q5: 已有項目遷移成本太高怎麼辦？

**A**: 分階段遷移策略。

**Phase 1: 新代碼嚴格執行**
- 所有新 Entity 必須使用單數
- ArchUnit 測試強制檢查

**Phase 2: 重構時順便修正**
- 重構模塊時修正舊表名
- 不強制全量遷移

**Phase 3: 逐步清理存量**
- 季度審查識別高優先級表
- 制定遷移計劃

**原則**: 不為遷移而遷移，在自然重構時順便修正。

---

### Q6: 為什麼不能用 `isDeleted` 字段？

**A**: MyBatis Plus + Lombok 兼容性問題。

```java
// ❌ 錯誤
@Data
public class PlayerEntity {
    private Boolean isDeleted;  // Lombok 生成 isIsDeleted() getter
}

// Lombok 生成的代碼:
public Boolean getIsDeleted() { ... }  // ✅ 正確 getter
public Boolean isIsDeleted() { ... }   // ❌ MyBatis Plus 誤調用

// MyBatis Plus 查找 getter:
// 1. 找到 isIsDeleted() → 使用 ❌
// 2. JSON 序列化時字段名變成 "isDeleted" → 前端混亂
```

**解決方案**: 使用 `deleted` 字段
```java
// ✅ 正確
@Data
public class PlayerEntity {
    private Boolean deleted;  // Lombok 生成 getDeleted() 和 isDeleted()
}

// Lombok 生成的代碼:
public Boolean getDeleted() { ... }    // ✅ MyBatis Plus 使用
public Boolean isDeleted() { ... }     // ✅ 布爾判斷使用
```

---

### Q7: 測試通過了，但 PR 還是被阻止？

**A**: 可能的原因：

**原因 1: 僅運行了單元測試**
```bash
# ❌ 不完整
./gradlew :sa-admin:test

# ✅ 完整
./gradlew :sa-admin:test --tests ArchitectureTest#tableNameMustBeSingular
```

**原因 2: 本地有未提交修改**
```bash
# 檢查本地修改
git status

# 確保所有修改已提交
git add .
git commit -m "fix: correct table names"
```

**原因 3: CI/CD 緩存問題**
- 關閉 PR 重新打開
- 或添加評論 `/recheck`

---

### Q8: 豁免清單會不會無限膨脹？

**A**: 有嚴格控制機制。

**控制措施**:
1. **目標上限**: ≤ 5 個豁免規則
2. **季度審查**: 每季度檢查豁免合理性
3. **審批流程**: 新增豁免需架構團隊 2/3 投票通過
4. **監控指標**: `exemption_count` 超過 10 觸發告警

**當前狀態**: 3 個豁免（metrics, statistics, analytics），遠低於上限。

---

## 📋 第八部分：新人入職檢查清單

### 入職第 1 天

- [ ] **閱讀本培訓材料**（預計 1 小時）
  - [ ] 理解單數命名原則
  - [ ] 記住快速參考卡片
  - [ ] 了解豁免清單

- [ ] **配置開發環境**
  ```bash
  # 配置 Git Hooks
  git config core.hooksPath .githooks

  # 驗證配置
  git config core.hooksPath
  ```

- [ ] **運行 ArchUnit 測試**（驗證環境）
  ```bash
  cd smart-admin-api-java21-springboot3
  ./gradlew :sa-admin:test --tests ArchitectureTest#tableNameMustBeSingular
  ```

### 入職第 1 週

- [ ] **完成實戰演練**（本文檔第六部分）
  - [ ] 練習 1: 創建新 Entity
  - [ ] 練習 2: 修正違規
  - [ ] 練習 3: 判斷豁免

- [ ] **Code Review 學習**
  - [ ] 審查最近 3 個 PR 的命名規範
  - [ ] 識別違規並提出修正建議

- [ ] **提交第一個 PR**
  - [ ] 確保通過所有檢查
  - [ ] 接受 Mentor 的命名規範反饋

### 入職第 1 個月

- [ ] **獨立開發新模塊**
  - [ ] 自主遵循命名規範
  - [ ] 所有 PR 通過 CI/CD 檢查

- [ ] **參加季度審查會議**（如適逢審查週期）
  - [ ] 了解審查流程
  - [ ] 提出改進建議

- [ ] **培訓考核**（通過後確認掌握）
  - [ ] 閱讀理解測試（選擇題）
  - [ ] 實戰編碼測試（創建 Entity）
  - [ ] Code Review 測試（識別違規）

---

## 📊 第九部分：培訓評估

### 評估方式

**1. 閱讀理解測試**（選擇題，10 分鐘）

範例題目:
1. SmartAdmin Entity 類命名應該使用？
   - A. 複數形式
   - B. 單數形式 ✅
   - C. 根據情況決定

2. 以下哪個表名符合 SmartAdmin 標準？
   - A. `t_users`
   - B. `t_user` ✅
   - C. `user`

3. API 路徑應該使用？
   - A. 單數形式
   - B. 複數形式 ✅
   - C. 與表名保持一致

4. 以下哪個表名適用豁免清單？
   - A. `t_orders`
   - B. `t_user_statistics` ✅
   - C. `t_players`

5. Boolean 字段應該命名為？
   - A. `isDeleted`
   - B. `deleted` ✅
   - C. `is_deleted`

---

**2. 實戰編碼測試**（30 分鐘）

**任務**: 創建一個完整的「玩家錢包」模塊

**要求**:
- Entity 類使用正確命名
- 數據庫表使用單數形式
- Mapper 正確引用表名
- 通過 ArchUnit 測試

**評分標準**:
- Entity 命名正確: 30 分
- 表命名正確: 30 分
- Mapper 正確: 20 分
- 測試通過: 20 分

---

**3. Code Review 測試**（20 分鐘）

**任務**: 審查以下代碼並識別所有命名違規

```java
// ❌ 錯誤範例（故意設計）
package net.lab1024.sa.admin.module.business.players.domain.entity;

@Data
@TableName("t_orders")
public class OrdersEntity {
    @TableId(type = IdType.AUTO)
    private Long orderId;

    private Boolean isDeleted;
    private Player players;
    private List<OrderItem> item;
}
```

**預期答案**:
1. Package 使用複數 `players`（應為 `player`）
2. 表名使用複數 `t_orders`（應為 `t_order`）
3. 類名使用複數 `OrdersEntity`（應為 `OrderEntity`）
4. Boolean 字段 `isDeleted`（應為 `deleted`）
5. 單個對象字段使用複數 `players`（應為 `player`）
6. 集合字段使用單數 `item`（應為 `items`）

**評分**: 每識別一個違規得 16.67 分，滿分 100 分。

---

### 合格標準

- **閱讀理解**: ≥ 80 分
- **實戰編碼**: ≥ 70 分
- **Code Review**: ≥ 70 分

**總評**: 三項均合格視為通過培訓。

---

## 🎓 第十部分：持續學習資源

### 官方文檔

1. **SmartAdmin 命名規範**
   - [.agent/rules/foundation/01-naming-conventions.md](.agent/rules/foundation/01-naming-conventions.md)
   - 完整命名規則（Package, Class, Field, Table）

2. **ADR-001: 命名標準決策記錄**
   - [docs/IGaming/architecture-decisions/ADR-001-Naming-Convention-Singular-Standard.md](docs/IGaming/architecture-decisions/ADR-001-Naming-Convention-Singular-Standard.md)
   - 決策背景、原因、後果分析

3. **ArchUnit 測試代碼**
   - [.agent/configs/ArchitectureTest.java](.agent/configs/ArchitectureTest.java)
   - 查看 `tableNameMustBeSingular` 方法實現

4. **豁免清單詳解**
   - [.claude/skills/extended/quality/naming-convention-checker/references/naming-exceptions.md](.claude/skills/extended/quality/naming-convention-checker/references/naming-exceptions.md)
   - 詳細解釋三個豁免規則

### 外部參考

1. **數據庫設計最佳實踐**
   - [Database Naming Conventions](https://www.sqlshack.com/learn-sql-naming-conventions/)
   - 單數 vs 複數的業界討論

2. **RESTful API 設計**
   - [RESTful API Design Best Practices](https://restfulapi.net/resource-naming/)
   - 為什麼 API 路徑使用複數

3. **MyBatis Plus 官方文檔**
   - [MyBatis Plus TableName Annotation](https://baomidou.com/pages/223848/#tablename)
   - `@TableName` 註解用法

### 內部分享

- **技術分享會**: 每季度一次命名規範分享
- **Code Review 會議**: 每週五下午討論命名問題
- **Slack 頻道**: `#architecture-standards` 討論架構標準

---

## 📞 獲取幫助

### 遇到問題時

**1. 查閱文檔**
- 先檢查本培訓材料 FAQ 部分
- 查看 [01-naming-conventions.md](.agent/rules/foundation/01-naming-conventions.md)

**2. 運行測試**
```bash
./gradlew :sa-admin:test --tests ArchitectureTest#tableNameMustBeSingular
```

**3. 諮詢團隊**
- **Mentor**: 一對一指導
- **架構團隊**: 複雜問題諮詢
- **Slack**: `#architecture-help` 頻道

**4. 提交 Issue**
- 命名規範疑問: `[Naming Question]`
- 豁免申請: `[Naming Exemption]`
- 工具問題: `[Naming Tool]`

### 聯絡資訊

- **架構團隊郵箱**: architecture@smartadmin.com
- **Slack 頻道**: `#architecture-standards`
- **技術分享會**: 每季度最後一個週五 14:00-16:00

---

## 🎯 培訓總結

### 核心要點（必記）

1. **單數原則**: Entity、Table、Package 全部使用單數
2. **API 例外**: RESTful API 路徑使用複數
3. **豁免三項**: metrics, statistics, analytics
4. **Boolean 字段**: 用 `deleted` 不用 `isDeleted`
5. **自動檢查**: ArchUnit + Git Hooks + CI/CD

### 下一步行動

- [ ] 完成入職檢查清單
- [ ] 通過培訓評估
- [ ] 提交第一個符合規範的 PR
- [ ] 參加季度審查會議

---

**版本**: 1.0.0
**維護團隊**: SmartAdmin Architecture Team
**培訓負責人**: 架構團隊負責人
**更新週期**: 每半年審查並更新

**下次更新**: 2026-08-02
