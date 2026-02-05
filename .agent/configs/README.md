# .agent/configs/ - SmartAdmin 配置檔案

## 目錄用途

此目錄包含 SmartAdmin 專案的**標準配置檔案和測試工具**，用於：
- 靜態程式碼分析（Checkstyle, PMD, SpotBugs）
- 架構約束測試（ArchUnit）
- 本地開發環境（Docker Compose）
- SonarQube整合

## 檔案清單

### 1. ArchitectureTest.java
**用途**：ArchUnit架構約束測試
**檔案大小**：18.5 KB
**部署位置**：`{project}/sa-admin/src/test/java/net/lab1024/sa/admin/ArchitectureTest.java`

**測試內容**：
- Controller只能呼叫Service（不能直接呼叫Dao）
- Service可以呼叫Dao或Manager
- Manager才能使用 `@Transactional` 和 `@Cacheable`
- 禁止 `@Autowired` 欄位注入
- Boolean欄位命名規則（`deleted` 而非 `isDeleted`）
- Service必須使用 `io.vavr.control.Option`（而非 `java.util.Optional`）

**執行命令**：
```bash
cd smart-admin-api-java21-springboot3/
./gradlew :sa-admin:test --tests ArchitectureTest
```

**相關規則**：
- [F04-architecture-rules.md](../rules/foundation/F04-architecture-rules.md)
- [F03-manager-layer.md](../rules/foundation/F03-manager-layer.md)
- [F01-naming-conventions.md](../rules/foundation/F01-naming-conventions.md)

**修改指南**：
- ⚠️ 修改前先理解架構約束的原因
- ✅ 新增自定義規則時，記錄在對應的 `.agent/rules/` 檔案中
- ✅ 與團隊討論後再調整核心約束

---

### 2. checkstyle.xml
**用途**：Checkstyle程式碼風格檢查配置
**部署位置**：`{project}/config/checkstyle/checkstyle.xml`

**檢查項目**：
- 縮排（4個空格）
- 命名約定（駝峰式、大小寫）
- Import順序
- 空行和空白字元
- Javadoc 註解

**執行命令**：
```bash
./gradlew checkstyleMain checkstyleTest
```

**相關規則**：
- [Q01-checkstyle-rules.md](../rules/quality-tools/Q01-checkstyle-rules.md)
- [F01-naming-conventions.md](../rules/foundation/F01-naming-conventions.md)

**客製化**：
- 修改後執行 `./gradlew checkstyleMain` 驗證
- 更新 `Q01-checkstyle-rules.md` 記錄變更

---

### 3. pmd-ruleset.xml
**用途**：PMD程式碼品質分析配置
**部署位置**：`{project}/config/pmd/pmd-ruleset.xml`

**檢查項目**：
- 未使用的變量和方法
- 過度複雜的程式碼
- 潛在的錯誤模式
- 效能問題

**排除規則**：
- CallSuperInConstructor（空建構子可接受）
- AvoidReassigningParameters（使用局部變數複製）
- ShortClassName（工具內部類別）

**執行命令**：
```bash
./gradlew pmdMain pmdTest
```

**相關規則**：
- [Q02-pmd-rules.md](../rules/quality-tools/Q02-pmd-rules.md)

---

### 4. spotbugs-exclude.xml
**用途**：SpotBugs錯誤檢測排除配置
**部署位置**：`{project}/config/spotbugs/spotbugs-exclude.xml`

**排除項目**：
- EI_EXPOSE_REP/EI_EXPOSE_REP2（DTO/VO/Form不需防禦性複製）
- NP_NULL_ON_SOME_PATH（CompletableFuture.getNow(null)是合法的）
- ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD（@PostConstruct模式）
- CT_CONSTRUCTOR_THROW（內部類別建構子驗證模式）

**執行命令**：
```bash
./gradlew spotbugsMain spotbugsTest
```

**相關規則**：
- [Q03-spotbugs-rules.md](../rules/quality-tools/Q03-spotbugs-rules.md)

---

### 5. sonar-project.properties
**用途**：SonarQube整合配置
**部署位置**：`{project}/sonar-project.properties`

**配置內容**：
- 專案金鑰和名稱
- 原始碼路徑
- 測試路徑
- 覆蓋率報告路徑（JaCoCo）
- 排除檔案模式

**執行命令**：
```bash
./gradlew sonar
```

**相關規則**：
- [W01-sonarqube-rules.md](../rules/workflows/W01-sonarqube-rules.md)
- [Q06-jacoco-coverage-rules.md](../rules/quality-tools/Q06-jacoco-coverage-rules.md)

**前置條件**：
- 本地執行 SonarQube（Docker）
- 配置 `sonar.host.url` 和 `sonar.login`

---

### 6. docker-compose.yml
**用途**：本地開發環境容器編排
**部署位置**：`{project}/docker-compose.yml` 或 `.agent/configs/docker-compose.yml`

**服務**：
- PostgreSQL 15（主資料庫）
- Redis 7（快取和會話）
- SonarQube（可選，品質分析）

**啟動命令**：
```bash
docker-compose up -d
```

**相關檔案**：
- `init-scripts/` - 資料庫初始化腳本

**注意事項**：
- 修改埠號避免衝突
- 生產環境使用獨立的配置

---

### 7. init-scripts/
**用途**：資料庫初始化SQL腳本
**部署位置**：與 `docker-compose.yml` 相同目錄

**內容**：
- Schema建立
- 基礎資料插入
- 權限設定

**使用方式**：
- Docker Compose自動執行（首次啟動）
- 或手動執行：`psql -U postgres -d smartadmin -f init-scripts/01-schema.sql`

## 部署指南

### 初次設置

**1. 複製配置檔案到專案**：
```bash
# 從 SmartAdmin 根目錄執行
cp .agent/configs/checkstyle.xml smart-admin-api-java21-springboot3/config/checkstyle/
cp .agent/configs/pmd-ruleset.xml smart-admin-api-java21-springboot3/config/pmd/
cp .agent/configs/spotbugs-exclude.xml smart-admin-api-java21-springboot3/config/spotbugs/
cp .agent/configs/sonar-project.properties smart-admin-api-java21-springboot3/
cp .agent/configs/ArchitectureTest.java smart-admin-api-java21-springboot3/sa-admin/src/test/java/net/lab1024/sa/admin/
```

**2. 啟動本地環境**：
```bash
cd .agent/configs
docker-compose up -d
```

**3. 驗證配置**：
```bash
cd smart-admin-api-java21-springboot3/
./gradlew clean build
```

### 更新現有配置

**同步策略**：
- `.agent/configs/` 中的檔案是**標準範本**
- 專案中的檔案可根據需求客製化
- 定期比對並合併上游更新

**比對工具**：
```bash
diff .agent/configs/checkstyle.xml smart-admin-api-java21-springboot3/config/checkstyle/checkstyle.xml
```

### CI/CD 整合

**GitHub Actions 範例**：
```yaml
- name: Copy configs
  run: |
    cp .agent/configs/checkstyle.xml config/checkstyle/
    cp .agent/configs/pmd-ruleset.xml config/pmd/

- name: Run quality checks
  run: ./gradlew check
```

## 維護指南

### 修改流程

1. **修改 `.agent/configs/` 中的標準範本**
2. **更新對應的 `.agent/rules/` 文檔**（說明變更原因）
3. **在專案中測試配置**
4. **提交變更並通知團隊**

### 版本控制

**檔案版本標記**：
- 在檔案頭部註解中新增版本號和更新日期
- 例如：`<!-- Version: 1.2.0 | Updated: 2026-02-04 -->`

**相容性**：
- 向下相容舊版專案（使用 `@SuppressWarnings` 或排除規則）
- 在 CHANGELOG 中記錄破壞性變更

## 常見問題

### Q: 為什麼有些規則被排除？
**A**: 參考 [Q02-pmd-rules.md](../rules/quality-tools/Q02-pmd-rules.md) 和 [Q03-spotbugs-rules.md](../rules/quality-tools/Q03-spotbugs-rules.md) 的「核准的抑制」部分。

### Q: 如何新增自定義ArchUnit規則？
**A**:
1. 修改 `ArchitectureTest.java`
2. 記錄在 [F04-architecture-rules.md](../rules/foundation/F04-architecture-rules.md)
3. 更新本 README.md

### Q: 配置衝突如何解決？
**A**:
1. 優先參考 `.agent/configs/` 的標準範本
2. 若需客製化，在專案配置中覆蓋
3. 記錄變更原因
