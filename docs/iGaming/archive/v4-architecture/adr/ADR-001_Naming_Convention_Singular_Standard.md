# ADR-001: 採用單數形式命名標準

## 狀態

✅ **已接受** (Accepted)

## 背景 (Context)

在開發 iGaming 模塊時，我們發現 SmartAdmin 官方標準和 iGaming 文檔之間存在命名標準的不一致：

### 衝突情況

| 層級 | SmartAdmin 標準 | iGaming 文檔 | 衝突程度 |
|------|----------------|-------------|---------|
| **Entity 類名** | 單數（`PlayerEntity`） | 未明確規定 | ⚠️ 空白區域 |
| **數據庫表名** | 單數（`t_player`） | 複數（`players`） | 🔴 **直接衝突** |
| **API 路徑** | 未明確規定 | 複數（`/api/v1/players`） | ⚠️ 空白區域 |
| **Package 名稱** | 單數（`player.service`） | 未明確規定 | ⚠️ 空白區域 |

### 現有異常案例

**SmartAdmin 代碼庫中存在的命名異常**：

1. **t_goods** (商品表)
   - 問題：使用複數形式，違反 SmartAdmin 單數標準
   - 影響：佔總表數的 5%（2/42）

2. **t_liteflow_execution_metrics** (LiteFlow 執行指標表)
   - 問題：使用複數形式
   - 特殊性：`metrics` 本身是不可數名詞，語義上更適合複數

### 問題分析

1. **一致性問題**：開發 iGaming 模塊時，不確定應該遵循哪個標準
2. **維護成本**：兩套標準並存，增加認知負擔和錯誤率
3. **架構測試衝突**：ArchUnit 測試會強制執行 SmartAdmin 標準，導致 iGaming 模塊違規

## 決策 (Decision)

我們決定**優先採用 SmartAdmin 單數標準**，並明確以下命名規範：

### 1. Entity 類命名

**規則**：使用單數形式 + `Entity` 後綴

```java
// ✅ 正確
public class PlayerEntity { }
public class WalletEntity { }
public class TransactionEntity { }

// ❌ 錯誤
public class PlayersEntity { }
public class WalletsEntity { }
```

### 2. 數據庫表命名

**規則**：`t_` 前綴 + 單數形式

```sql
-- ✅ 正確
CREATE TABLE t_player ( ... );
CREATE TABLE t_wallet ( ... );
CREATE TABLE t_transaction ( ... );

-- ❌ 錯誤
CREATE TABLE t_players ( ... );
CREATE TABLE t_wallets ( ... );
```

**特殊情況豁免**：

| 表名 | 原因 | 規則 |
|------|------|------|
| `t_liteflow_execution_metrics` | `metrics` 是不可數名詞，表示聚合統計數據 | 保持複數 |
| `t_*_statistics` | `statistics` 慣用複數形式 | 保持複數 |
| `t_*_analytics` | `analytics` 慣用複數形式 | 保持複數 |

### 3. API 路徑命名

**規則**：複數形式（遵循 RESTful 標準）

```
✅ 正確：/api/v1/players, /api/v1/wallets
✅ 原因：RESTful API 資源集合應使用複數
```

**重要**：API 路徑的複數形式**不影響** Entity 和 Table 的單數命名！

### 4. Package 命名

**規則**：全部小寫 + 單數形式

```java
// ✅ 正確
package net.lab1024.sa.admin.module.business.player.domain.entity;

// ❌ 錯誤
package net.lab1024.sa.admin.module.business.players.domain.entities;
```

### 5. 命名對照表

| 業務概念 | API 路徑 | Entity 類 | 數據庫表 | Package |
|---------|---------|----------|---------|---------|
| 玩家 | `/api/v1/players` | `PlayerEntity` | `t_player` | `player.domain` |
| 錢包 | `/api/v1/wallets` | `WalletEntity` | `t_wallet` | `wallet.domain` |
| 交易 | `/api/v1/transactions` | `TransactionEntity` | `t_transaction` | `transaction.domain` |
| 紅利 | `/api/v1/bonuses` | `BonusEntity` | `t_bonus` | `bonus.domain` |
| 遊戲 | `/api/v1/games` | `GameEntity` | `t_game` | `game.domain` |

## 後果 (Consequences)

### 積極影響 ✅

1. **一致性提升**
   - 統一 SmartAdmin 和 iGaming 的命名標準
   - 減少開發者認知負擔
   - 降低命名錯誤率

2. **架構測試通過**
   - 符合 ArchUnit 測試要求
   - 避免持續集成失敗
   - 強制執行命名規範

3. **可維護性提高**
   - 新加入團隊的開發者更容易理解
   - 代碼審查更加高效
   - 重構風險降低

4. **自動化工具支持**
   - 可以使用 ArchUnit 自動檢測違規
   - 可以集成到 CI/CD 流程
   - 可以通過 Pre-commit Hook 防止錯誤提交

### 消極影響 ❌

1. **需要修正現有異常**
   - 修正 `t_goods` → `t_good`（需數據庫遷移）
   - 影響範圍：1 個表 + 相關代碼
   - 遷移風險：低（測試環境先驗證）

2. **與 RESTful 慣例的認知差異**
   - API 使用複數（`/api/v1/players`）
   - Table 使用單數（`t_player`）
   - 需要在文檔中明確說明

3. **iGaming 文檔需要大量更新**
   - 約 190+ 處表名需要更新
   - 需要更新 15+ 份文檔
   - 工作量估計：4-6 小時

### 緩解措施 🛡️

1. **完善文檔**
   - 在 Terminology Standards 添加完整的代碼命名規範
   - 在 API Design Standard 添加命名規範說明
   - 創建 ADR 記錄此決策

2. **自動化檢查**
   - 擴展 ArchUnit 測試，添加 `tableNameMustBeSingular` 規則
   - 創建 Naming Convention Checker Skill
   - 集成到 CI/CD 流程
   - 添加 Pre-commit Hook

3. **團隊培訓**
   - 技術分享會議
   - 新人入職培訓材料
   - Wiki 頁面更新

4. **分階段執行**
   - Phase 1: 文檔更新（低風險）
   - Phase 2: 代碼修正（中風險，測試環境先驗證）
   - Phase 3: 自動化檢查器（防止未來回歸）

## 替代方案 (Alternatives)

### 方案 A：優先 iGaming 標準（全部使用複數）

**優勢**：
- ✅ 符合 RESTful 資源集合慣例
- ✅ 符合行業慣例（部分 ORM 使用複數表名）

**劣勢**：
- ❌ 違反 SmartAdmin 官方標準
- ❌ ArchUnit 測試失敗
- ❌ 需要為 iGaming 模塊創建例外規則
- ❌ 破壞 SmartAdmin 代碼庫的整體一致性

**結論**：不採用（與 SmartAdmin 核心理念衝突）

### 方案 B：分層處理（API 複數 + Table 單數）

**優勢**：
- ✅ 同時滿足 RESTful 和 SmartAdmin 標準
- ✅ API 層和數據庫層獨立演化

**劣勢**：
- ⚠️ 需要在文檔中明確說明
- ⚠️ 開發者需要理解分層命名邏輯

**結論**：✅ **採用**（本 ADR 選擇的方案）

### 方案 C：全部使用 SmartAdmin 標準（包括 API 路徑）

**優勢**：
- ✅ 100% 統一，無例外
- ✅ 最簡單的一致性

**劣勢**：
- ❌ 違反 RESTful 資源集合慣例
- ❌ 與行業標準不符
- ❌ API 用戶體驗不佳

**結論**：不採用（過於嚴格，損害 API 可用性）

## 實施計劃 (Implementation Plan)

### Phase 1: 文檔更新（4-6 小時）

- ✅ 更新 `00-03_Terminology_Standards.md` 第 8 節
- ✅ 批量更新 `00-05_Data_Model.md`（190+ 處）
- ✅ 更新 `12-05_API_Design_Standard.md`
- ✅ 創建 `ADR-001_Naming_Convention_Singular_Standard.md`

### Phase 2: 代碼修正（6-8 小時）

- ⏳ 創建數據庫遷移腳本（`V1.2__rename_goods_table.sql`）
- ⏳ 修改 `GoodsEntity.java`（@TableName 註解）
- ⏳ 修改 `GoodsMapper.xml`（批量替換 t_goods → t_good）
- ⏳ 添加 metrics 豁免規則到 `01-naming-conventions.md`

### Phase 3: 自動化檢查器（8-12 小時）

- ⏳ 擴展 ArchUnit 測試（`ArchitectureTest#tableNameMustBeSingular`）
- ⏳ 創建 Naming Convention Checker Skill
- ⏳ 集成 CI/CD 流程（GitHub Actions）
- ⏳ 創建 Pre-commit Hook

### Phase 4: 驗證與文檔化（2-4 小時）

- ⏳ 運行完整測試套件
- ⏳ 創建遷移報告
- ⏳ 更新團隊文檔（README.md, CLAUDE.md, NEW-ENTITY-CHECKLIST.md）

### Phase 5: 持續監控（2-3 小時）

- ⏳ 設置監控指標
- ⏳ 建立季度審查機制
- ⏳ 制定團隊培訓計劃

**總計**：約 22-33 小時（3-4 工作日）

## 驗證標準 (Acceptance Criteria)

### 必須滿足 ✅

1. **所有 ArchUnit 測試通過**
   - 包括新增的 `tableNameMustBeSingular` 測試
   - 無命名規範違規

2. **t_goods 表成功重命名**
   - Flyway 遷移腳本執行成功
   - 所有單元測試和集成測試通過

3. **文檔完整性**
   - 15+ 份文檔更新完成
   - ADR 決策文檔已創建
   - Terminology Standards 第 8 節已完善

4. **自動化工具運行正常**
   - CI/CD 命名檢查流程無錯誤
   - Pre-commit Hook 可以攔截違規

5. **團隊培訓材料準備完成**
   - 新人入職培訓文檔
   - Wiki 頁面更新
   - NEW-ENTITY-CHECKLIST.md 創建

### 可選滿足 ⚪

1. **性能測試**
   - 數據庫遷移對性能無影響

2. **向後兼容性**
   - 舊代碼逐步遷移計劃

## 參考資料 (References)

### 內部文檔

- [.agent/rules/foundation/01-naming-conventions.md](.agent/rules/foundation/01-naming-conventions.md) - SmartAdmin 命名規範
- [docs/IGaming/../00_Foundation/concepts/00-03_Terminology_Standards.md](../00_Foundation/concepts/00-03_Terminology_Standards.md) - iGaming 術語標準
- [docs/IGaming/09_Technical_Infrastructure/09-03-01_Design_Principles.md](../09_Technical_Infrastructure/09-03-01_Design_Principles.md) - API 設計標準

### 外部參考

- [REST API Design Rulebook](https://www.oreilly.com/library/view/rest-api-design/9781449317904/) - RESTful API 設計最佳實踐
- [Alibaba Java Coding Guidelines](https://alibaba.github.io/Alibaba-Java-Coding-Guidelines/) - 阿里巴巴 Java 開發手冊
- [SmartAdmin Official Documentation](https://github.com/1024-lab/smart-admin) - SmartAdmin 官方文檔

### 行業標準

- **RESTful API**：資源集合使用複數（`/api/v1/users`）
- **Rails ActiveRecord**：表名使用複數（`users`, `orders`）
- **Spring Boot JPA**：表名可單數可複數（社區有爭議）
- **SmartAdmin**：明確使用單數（官方標準）

## 決策記錄 (Decision Log)

| 日期 | 變更內容 | 決策者 | 狀態 |
|------|---------|--------|------|
| 2026-02-02 | 初始版本，確定採用 SmartAdmin 單數標準 | Architecture Team | ✅ 已接受 |

---

## 附錄：命名規範快速參考

### ✅ 正確範例

```java
// Entity 類
@TableName("t_player")
public class PlayerEntity {
    private Long playerId;
}

// Controller
@RestController
@RequestMapping("/api/v1/players")  // API 用複數
public class PlayerController {
    public ResponseDTO<PlayerVO> getPlayer(Long id) {
        PlayerEntity player = playerService.queryById(id);  // Entity 用單數
        return ResponseDTO.ok(SmartBeanUtil.copy(player, PlayerVO.class));
    }
}

// Service
public class PlayerService {
    private final PlayerDao playerDao;  // DAO 用單數

    public List<Player> queryPlayers() {  // 集合變量用複數
        return playerDao.selectList();
    }
}
```

### ❌ 錯誤範例

```java
// ❌ 錯誤：Entity 使用複數
@TableName("t_players")  // 應該是 t_player
public class PlayersEntity {  // 應該是 PlayerEntity
    // ...
}

// ❌ 錯誤：API 使用單數
@RequestMapping("/api/v1/player")  // 應該是 /api/v1/players
public class PlayerController {
    // ...
}

// ❌ 錯誤：Package 使用複數
package net.lab1024.sa.admin.module.business.players.domain.entities;
// 應該是 player.domain.entity
```

---

**文檔版本**: 4.0.0
**最後更新**: 2026-02-02
**下次審閱**: 2026-05-02（每季度審閱）
