# Gradle 迁移指南

## ✅ 已完成的配置文件

所有 Gradle 配置文件已生成：

1. ✅ `gradle.properties` - 项目属性配置
2. ✅ `settings.gradle.kts` - 多模块配置
3. ✅ `gradle/libs.versions.toml` - 版本目录（统一管理所有依赖版本）
4. ✅ `build.gradle.kts` - 根构建脚本
5. ✅ `sa-base/build.gradle.kts` - Base 模块构建脚本
6. ✅ `sa-admin/build.gradle.kts` - Admin 模块构建脚本

## 📋 下一步操作

### 1. 初始化 Gradle Wrapper（必须）

在项目根目录运行以下命令生成 Gradle Wrapper 文件：

```bash
cd /Users/zhangxuanrong/Documents/Workspace/Java/smart-admin/smart-admin-api-java21-springboot3
gradle wrapper --gradle-version 8.11 --distribution-type all
```

这将生成：
- `gradlew` - Unix/Mac 启动脚本
- `gradlew.bat` - Windows 启动脚本
- `gradle/wrapper/gradle-wrapper.jar` - Wrapper JAR
- `gradle/wrapper/gradle-wrapper.properties` - Wrapper 配置

### 2. 验证配置

运行以下命令验证配置是否正确：

```bash
# 查看项目结构
./gradlew projects

# 查看依赖树
./gradlew :sa-admin:dependencies --configuration runtimeClasspath
```

### 3. 编译测试

```bash
# 清理并编译
./gradlew clean classes

# 完整构建
./gradlew clean build
```

### 4. 多环境构建测试

```bash
# 默认环境 (dev)
./gradlew clean build

# 测试环境
./gradlew clean build -Penv=test

# 预发布环境
./gradlew clean build -Penv=pre

# 生产环境
./gradlew clean build -Penv=prod
```

### 5. 验证构建产物

检查生成的 JAR 文件：

```bash
# 查看生成的 JAR 文件
ls -lh sa-admin/build/libs/

# 应该看到: sa-admin-dev-3.0.0.jar (或其他环境)

# 验证 JAR 内容
jar tf sa-admin/build/libs/sa-admin-dev-3.0.0.jar | grep application.yaml
```

### 6. 运行应用

```bash
# 运行打包后的应用
java -jar sa-admin/build/libs/sa-admin-dev-3.0.0.jar

# 或者直接通过 Gradle 运行
./gradlew :sa-admin:bootRun
```

## 🔧 常用命令对照

| 操作 | Maven | Gradle |
|------|-------|--------|
| 清理构建 | `mvn clean` | `./gradlew clean` |
| 编译 | `mvn compile` | `./gradlew classes` |
| 打包 | `mvn package` | `./gradlew build` |
| 打包（指定环境） | `mvn package -Pdev` | `./gradlew build -Penv=dev` |
| 查看依赖树 | `mvn dependency:tree` | `./gradlew dependencies` |
| 跳过测试打包 | `mvn package -DskipTests` | `./gradlew build -x test` |
| 运行应用 | `mvn spring-boot:run` | `./gradlew bootRun` |

## 📝 配置说明

### 环境配置

默认环境为 `dev`，在 `gradle.properties` 中配置：

```properties
env=dev
```

可以通过命令行参数覆盖：

```bash
./gradlew build -Penv=prod
```

### 资源过滤

YAML 文件中的 `@profiles.active@` 占位符会在构建时被替换为实际环境。

例如，`application.yaml` 中的：

```yaml
spring:
  profiles:
    active: '@profiles.active@'
```

构建 dev 环境时会变成：

```yaml
spring:
  profiles:
    active: 'dev'
```

### JAR 命名规范

构建产物命名格式：`sa-admin-{env}-{version}.jar`

示例：
- `sa-admin-dev-3.0.0.jar` (开发环境)
- `sa-admin-prod-3.0.0.jar` (生产环境)

## 🚨 注意事项

1. **首次构建可能较慢**：Gradle 需要下载依赖到本地缓存
2. **依赖冲突**：如遇到依赖问题，使用 `./gradlew dependencies` 检查依赖树
3. **资源文件**：确保 `src/main/resources/{env}/` 目录下的配置文件存在
4. **编码设置**：所有文件使用 UTF-8 编码（已在 gradle.properties 中配置）

## 📊 验证检查清单

构建完成后，验证以下项目：

- [ ] Gradle Wrapper 文件已生成
- [ ] `./gradlew projects` 显示两个模块
- [ ] 依赖树无错误
- [ ] 四个环境都能成功构建
- [ ] JAR 命名符合规范
- [ ] JAR 内包含正确的环境配置文件
- [ ] `application.yaml` 中的 `@profiles.active@` 已替换
- [ ] 应用能够正常启动
- [ ] 数据库连接正常
- [ ] Redis 连接正常
- [ ] Knife4j API 文档可访问 (http://localhost:1024/doc.html)
- [ ] 日志输出到正确目录
- [ ] IDE (IntelliJ IDEA) 能正确识别 Gradle 项目

## 🔄 与 Maven 并行运行

建议在迁移初期保留 Maven 配置（pom.xml 文件），确保 Gradle 构建稳定后再删除。

可以同时使用两套构建系统：
- Maven: `mvn clean package`
- Gradle: `./gradlew clean build`

对比两者的构建产物，确保一致性。

## 🎯 性能优化

Gradle 已启用以下优化（在 gradle.properties 中）：

- ✅ **并行构建**: `org.gradle.parallel=true`
- ✅ **构建缓存**: `org.gradle.caching=true`
- ✅ **Daemon 模式**: `org.gradle.daemon=true`

首次构建后，后续构建会显著加速（增量编译）。

## 📚 更多资源

- [Gradle 官方文档](https://docs.gradle.org/)
- [Spring Boot Gradle 插件](https://docs.spring.io/spring-boot/docs/current/gradle-plugin/reference/htmlsingle/)
- [Gradle Kotlin DSL](https://docs.gradle.org/current/userguide/kotlin_dsl.html)
- [Version Catalog](https://docs.gradle.org/current/userguide/platforms.html)

## ❓ 常见问题

### Q: 构建失败，提示找不到依赖
A: 检查网络连接，或尝试使用阿里云镜像（已在 build.gradle.kts 中配置）

### Q: IntelliJ IDEA 不识别 Gradle 项目
A: File -> Invalidate Caches / Restart，然后重新导入项目

### Q: 资源文件没有被正确过滤
A: 检查 `src/main/resources/{env}/` 目录结构是否正确

### Q: 如何回退到 Maven
A: 保留了所有 pom.xml 文件，可以随时使用 Maven 构建
