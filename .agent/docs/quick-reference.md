# 快速參考

> 常用命令、開發流程檢查清單、相關文件連結

---

## 1. 環境設定快速檢查

```bash
# ✅ 前置條件
java -version      # Java 21+
mvn -version       # Maven 3.8+, Java 21
docker --version   # Docker installed

# ✅ 啟動資料庫
cd .agent/configs
docker-compose up -d postgres redis

# ✅ 驗證連線
docker exec -it smartadmin-postgres psql -U smartadmin -d smart_admin_v3

# ✅ 建置專案
cd smart-admin-api-java21-springboot3
mvn clean compile

# ✅ 執行架構測試
mvn test -Dtest=ArchitectureTest
```

---

## 2. 開發流程快速檢查

```bash
# 1️⃣ 建立功能分支
git checkout -b feature/your-feature

# 2️⃣ 開發並遵守規範
# - 使用 Vavr Option/Try 替代 null/try-catch
# - 使用 LambdaQueryWrapper 建構查詢
# - 遵守分層架構 (Controller → Service → Dao)

# 3️⃣ 執行測試和品質檢查
mvn clean verify
mvn test -Dtest=ArchitectureTest

# 4️⃣ 提交程式碼
git add .
git commit -m "feat(module): 功能描述"

# 5️⃣ 推送並建立 PR
git push origin feature/your-feature
```

---

## 3. 後端常用命令

### 建置與執行
```bash
# 進入 Java 21 專案目錄
cd smart-admin-api-java21-springboot3

# 建置專案
mvn clean package -P dev

# 執行應用
cd sa-admin && mvn spring-boot:run

# 執行測試
mvn test

# 存取 API 文件
# http://localhost:1024/swagger-ui.html
```

### 品質檢查
```bash
# Checkstyle 檢查
mvn checkstyle:check

# PMD 檢查
mvn pmd:check

# SpotBugs 檢查
mvn spotbugs:check

# JaCoCo 測試覆蓋率
mvn clean test jacoco:report

# 完整驗證
mvn verify
```

---

## 4. 前端常用命令

```bash
# 進入前端專案目錄
cd smart-admin-web

# 安裝依賴
npm install

# 本地開發
npm run localhost

# 開發環境
npm run dev

# 建置測試環境
npm run build:test

# 建置正式環境
npm run build:prod
```

---

## 5. Docker 相關命令

```bash
# 啟動所有服務
cd .agent/configs
docker-compose up -d

# 查看服務狀態
docker-compose ps

# 查看日誌
docker-compose logs postgres
docker-compose logs redis

# 停止服務
docker-compose down

# 進入 PostgreSQL
docker exec -it smartadmin-postgres psql -U smartadmin -d smart_admin_v3

# 進入 Redis
docker exec -it smartadmin-redis redis-cli
```

---

## 6. SmartAdmin 模式速查

### ResponseDTO 模式
```java
// 成功返回
return ResponseDTO.ok(data);
return ResponseDTO.ok();
return ResponseDTO.okMsg("操作成功");

// 錯誤返回
return ResponseDTO.error(UserErrorCode.PARAM_ERROR);
return ResponseDTO.userErrorParam("無效參數");

// 異常拋出
throw new BusinessException(ErrorCode.XXX);
```

### 分頁查詢模式
```java
// 標準分頁查詢
Page<Entity> page = SmartPageUtil.convert2PageQuery(queryForm);
List<Entity> list = dao.selectPage(page, wrapper).getRecords();
PageResult<VO> result = SmartPageUtil.convert2PageResult(page, list, VO.class);
```

### Bean 轉換模式
```java
// 單個對象轉換
Entity entity = SmartBeanUtil.copy(form, Entity.class);
VO vo = SmartBeanUtil.copy(entity, VO.class);

// 列表轉換
List<VO> voList = SmartBeanUtil.copyList(entityList, VO.class);
```

### Vavr Option 模式
```java
// Service 返回 Option
public Option<User> findById(Long id) {
    return Option.of(userMapper.selectById(id));
}

// Controller 處理 Option
return userService.findById(id)
    .map(UserVO::from)
    .fold(
        () -> ResponseDTO.error("不存在"),
        user -> ResponseDTO.ok(user)
    );
```

---

## 7. 相關文件連結

### 官方資源
- **官方文件**: https://smartadmin.vip
- **線上預覽**: https://preview.smartadmin.vip

### 內部文件
- **初始化指南**: [workflows/init.md](../workflows/init.md)
- **TDD 工作流程**: [workflows/tdd-workflow.md](../workflows/tdd-workflow.md)
- **Quality Gate**: [workflows/quality-gates-local-ci.md](../workflows/quality-gates-local-ci.md)
- **錯誤恢復**: [workflows/java-failure-recovery.md](../workflows/java-failure-recovery.md)

### 編碼規範
- **命名規範**: [rules/01-naming-conventions.md](../rules/01-naming-conventions.md)
- **架構規範**: [rules/10-architecture-rules.md](../rules/10-architecture-rules.md)
- **Vavr 基礎**: [rules/08-vavr-fundamentals.md](../rules/08-vavr-fundamentals.md)
- **PostgreSQL**: [rules/05-postgresql-advanced.md](../rules/05-postgresql-advanced.md)
- **MyBatis Plus**: [rules/09-mybatis-plus-core.md](../rules/09-mybatis-plus-core.md)

### 配置文件
- **技術棧與依賴**: [.claude/shared/knowledge/project-architecture.md](../../.claude/shared/knowledge/project-architecture.md)
- **Docker 環境**: [configs/docker-compose.yml](../configs/docker-compose.yml)
- **架構測試**: [configs/ArchitectureTest.java](../configs/ArchitectureTest.java)

---

## 8. 質量門禁標準

```yaml
Quality Gate 通過條件:
  ✅ ArchUnit:        100% 通過 (零容忍)
  ✅ Checkstyle:      0 errors
  ✅ PMD:             0 violations
  ✅ SpotBugs:        0 bugs
  ✅ 測試覆蓋率:       ≥ 80% (Line), ≥ 70% (Branch)
  ✅ SonarQube:       0 Blocker/Critical issues
```

---

**最後更新**: 2025-01-21
**返回**: [README.md](../README.md)
