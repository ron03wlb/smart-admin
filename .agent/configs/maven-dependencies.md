# Maven 依賴配置指南

> SmartAdmin Java 21 + Spring Boot 3.5.4 + PostgreSQL 技術棧

## 一、核心依賴清單

### 1. 數據庫驅動

#### PostgreSQL（主數據庫）
```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <version>42.7.5</version>
    <scope>runtime</scope>
</dependency>
```

**關鍵配置**:
- JDBC URL: `jdbc:postgresql://localhost:5432/smart_admin_v3`
- 驅動類: `org.postgresql.Driver`
- 支持 JSONB、數組類型、窗口函數等 PostgreSQL 特性

#### Redis（緩存）
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-pool2</artifactId>
</dependency>
```

---

### 2. ORM 框架

#### MyBatis Plus
```xml
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-boot-starter</artifactId>
    <version>3.5.12</version>
</dependency>
```

**核心功能**:
- CRUD 自動生成（BaseMapper）
- LambdaQueryWrapper（類型安全查詢）
- 分頁插件（支持 PostgreSQL）
- 邏輯刪除、樂觀鎖、自動填充
- 自定義 TypeHandler（JSONB、數組）

---

### 3. 函數式編程（Vavr）

#### Vavr 核心庫
```xml
<dependency>
    <groupId>io.vavr</groupId>
    <artifactId>vavr</artifactId>
    <version>0.10.4</version>
</dependency>
```

**核心功能**:
- **Option**: 替代 null 檢查和 Java Optional
- **Try**: 函數式異常處理，替代 try-catch
- **Either**: 業務邏輯分支（Left=錯誤, Right=成功）
- **不可變集合**: List、Map、Set（線程安全）
- **模式匹配**: Match/Case
- **函數組合**: Function1-8、compose、andThen

#### Vavr Jackson 整合
```xml
<dependency>
    <groupId>io.vavr</groupId>
    <artifactId>vavr-jackson</artifactId>
    <version>0.10.3</version>
</dependency>
```

**用途**: 支持 Vavr 類型的 JSON 序列化/反序列化（Option、Either、List 等）

**配置**:
```java
@Bean
public ObjectMapper objectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new VavrModule());
    return mapper;
}
```

---

### 4. Web 框架

#### Spring Boot Web（傳統 MVC）
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

**注意**:
- ✅ 使用傳統 Spring MVC（同步阻塞 I/O）
- ❌ **不使用** `spring-boot-starter-webflux`（響應式 WebFlux）
- 函數式編程由 **Vavr** 提供，不依賴 Reactor

#### 驗證框架
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

---

### 5. 工具類庫

#### Lombok
```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <scope>provided</scope>
</dependency>
```

#### Apache Commons
```xml
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-lang3</artifactId>
    <version>3.14.0</version>
</dependency>

<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-collections4</artifactId>
    <version>4.4</version>
</dependency>
```

#### Hutool
```xml
<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-all</artifactId>
    <version>5.8.25</version>
</dependency>
```

---

### 6. 安全框架

#### JWT 認證
```xml
<dependency>
    <groupId>com.auth0</groupId>
    <artifactId>java-jwt</artifactId>
    <version>4.4.0</version>
</dependency>
```

#### 加密工具
```xml
<dependency>
    <groupId>org.bouncycastle</groupId>
    <artifactId>bcprov-jdk18on</artifactId>
    <version>1.77</version>
</dependency>
```

---

## 二、測試依賴

### 1. 單元測試

#### JUnit 5 + Spring Boot Test
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

#### Mockito
```xml
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <scope>test</scope>
</dependency>
```

---

### 2. 架構測試（ArchUnit）

```xml
<dependency>
    <groupId>com.tngtech.archunit</groupId>
    <artifactId>archunit-junit5</artifactId>
    <version>1.3.0</version>
    <scope>test</scope>
</dependency>
```

**用途**: 自動化架構約束測試
- 分層架構（Controller → Service → Manager → Domain）
- 命名規範（Controller/Service 後綴）
- 依賴注入（禁止 @Autowired 字段注入）
- Vavr 使用規範（Service 層推薦 Option 代替 Optional）
- PostgreSQL 約束（禁止 MySQL 驅動）

**測試文件**: `.agent/configs/ArchitectureTest.java`

---

### 3. 測試容器（Testcontainers）

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <version>1.19.7</version>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <version>1.19.7</version>
    <scope>test</scope>
</dependency>
```

**用途**: 集成測試時自動啟動 PostgreSQL 容器

**示例**:
```java
@Testcontainers
@SpringBootTest
class IntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("test_db")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
```

---

## 三、代碼質量插件

### 1. Maven Compiler Plugin

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.12.1</version>
    <configuration>
        <source>21</source>
        <target>21</target>
        <encoding>UTF-8</encoding>
        <parameters>true</parameters>
    </configuration>
</plugin>
```

---

### 2. Checkstyle（代碼風格檢查）

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-checkstyle-plugin</artifactId>
    <version>3.3.1</version>
    <configuration>
        <configLocation>.agent/configs/checkstyle.xml</configLocation>
        <encoding>UTF-8</encoding>
        <consoleOutput>true</consoleOutput>
        <failsOnError>true</failsOnError>
    </configuration>
    <executions>
        <execution>
            <id>validate</id>
            <phase>validate</phase>
            <goals>
                <goal>check</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

**核心規則**:
- 阿里巴巴 Java 規範（黃山版）
- 禁止 Service 層導入 `java.util.Optional`（推薦 Vavr Option）
- 強制使用 LambdaQueryWrapper
- 禁止字段注入（@Autowired）

---

### 3. PMD（靜態代碼分析）

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-pmd-plugin</artifactId>
    <version>3.21.2</version>
    <configuration>
        <rulesets>
            <ruleset>.agent/configs/pmd-ruleset.xml</ruleset>
        </rulesets>
        <printFailingErrors>true</printFailingErrors>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>check</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

**核心規則**:
- 未使用的變量/方法檢測
- 複雜度檢測（圈複雜度 > 10）
- 重複代碼檢測（CPD）

---

### 4. SpotBugs（Bug 檢測）

```xml
<plugin>
    <groupId>com.github.spotbugs</groupId>
    <artifactId>spotbugs-maven-plugin</artifactId>
    <version>4.8.3.1</version>
    <configuration>
        <effort>Max</effort>
        <threshold>Low</threshold>
        <xmlOutput>true</xmlOutput>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>check</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

**檢測範圍**:
- Null 指針風險
- 資源未關閉（文件、連接）
- 併發問題（同步、死鎖）
- 安全漏洞（SQL 注入、XSS）

---

### 5. JaCoCo（測試覆蓋率）

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <executions>
        <execution>
            <id>prepare-agent</id>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
        <execution>
            <id>check</id>
            <goals>
                <goal>check</goal>
            </goals>
            <configuration>
                <rules>
                    <rule>
                        <element>BUNDLE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.80</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

**覆蓋率要求**: ≥ 80%

---

### 6. SonarQube（代碼質量平台）

```xml
<plugin>
    <groupId>org.sonarsource.scanner.maven</groupId>
    <artifactId>sonar-maven-plugin</artifactId>
    <version>3.10.0.2594</version>
</plugin>
```

**執行命令**:
```bash
mvn clean verify sonar:sonar \
  -Dsonar.projectKey=smart-admin \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=<token>
```

**Quality Gate 標準**:
- 新增代碼覆蓋率 ≥ 80%
- 重複率 < 3%
- 無阻塞級別問題
- 技術債務比率 < 5%

---

## 四、完整 POM 示例

```xml
<project>
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.4</version>
    </parent>

    <groupId>net.lab1024.sa</groupId>
    <artifactId>smart-admin-api</artifactId>
    <version>3.28.3</version>

    <properties>
        <java.version>21</java.version>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>

        <!-- 核心依賴版本 -->
        <postgresql.version>42.7.5</postgresql.version>
        <mybatis-plus.version>3.5.12</mybatis-plus.version>
        <vavr.version>0.10.4</vavr.version>
        <hutool.version>5.8.25</hutool.version>

        <!-- 測試依賴版本 -->
        <archunit.version>1.3.0</archunit.version>
        <testcontainers.version>1.19.7</testcontainers.version>
    </properties>

    <dependencies>
        <!-- Spring Boot -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- PostgreSQL -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <version>${postgresql.version}</version>
            <scope>runtime</scope>
        </dependency>

        <!-- MyBatis Plus -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-boot-starter</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>

        <!-- Vavr -->
        <dependency>
            <groupId>io.vavr</groupId>
            <artifactId>vavr</artifactId>
            <version>${vavr.version}</version>
        </dependency>
        <dependency>
            <groupId>io.vavr</groupId>
            <artifactId>vavr-jackson</artifactId>
            <version>0.10.3</version>
        </dependency>

        <!-- 工具類 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>

        <!-- 測試 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.tngtech.archunit</groupId>
            <artifactId>archunit-junit5</artifactId>
            <version>${archunit.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>postgresql</artifactId>
            <version>${testcontainers.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>

            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.12.1</version>
                <configuration>
                    <source>21</source>
                    <target>21</target>
                </configuration>
            </plugin>

            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-checkstyle-plugin</artifactId>
                <version>3.3.1</version>
                <configuration>
                    <configLocation>.agent/configs/checkstyle.xml</configLocation>
                    <failsOnError>true</failsOnError>
                </configuration>
            </plugin>

            <plugin>
                <groupId>org.jacoco</groupId>
                <artifactId>jacoco-maven-plugin</artifactId>
                <version>0.8.11</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>prepare-agent</goal>
                        </goals>
                    </execution>
                    <execution>
                        <id>report</id>
                        <phase>test</phase>
                        <goals>
                            <goal>report</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## 五、已移除依賴

### ❌ MySQL 驅動（已遷移至 PostgreSQL）
```xml
<!-- 不再使用 -->
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
</dependency>
```

### ❌ Spring WebFlux（使用 Spring MVC + Vavr）
```xml
<!-- 不再使用 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

---

## 六、常用 Maven 命令

### 依賴管理
```bash
# 查看依賴樹
mvn dependency:tree

# 查看 PostgreSQL 依賴
mvn dependency:tree | grep postgresql

# 查看 Vavr 依賴
mvn dependency:tree | grep vavr

# 分析依賴衝突
mvn dependency:analyze
```

### 構建命令
```bash
# 編譯
mvn clean compile

# 運行測試
mvn test

# 打包（跳過測試）
mvn clean package -DskipTests

# 完整構建（包含質量檢查）
mvn clean verify
```

### 代碼質量檢查
```bash
# Checkstyle
mvn checkstyle:check

# PMD
mvn pmd:check

# SpotBugs
mvn spotbugs:check

# JaCoCo 覆蓋率報告
mvn jacoco:report

# 完整質量檢查
mvn clean verify checkstyle:check pmd:check spotbugs:check
```

---

## 七、依賴版本升級策略

### 主要依賴升級頻率

| 依賴 | 當前版本 | 升級頻率 | 備註 |
|------|----------|----------|------|
| Spring Boot | 3.5.4 | 每 6 個月 | 跟隨 Spring 發布週期 |
| PostgreSQL Driver | 42.7.5 | 每 3 個月 | 安全補丁及時更新 |
| MyBatis Plus | 3.5.12 | 每 6 個月 | 關注新特性和 Bug 修復 |
| Vavr | 0.10.4 | 按需升級 | 版本較穩定，關注重大更新 |
| ArchUnit | 1.3.0 | 每 6 個月 | 關注新規則支持 |

### 升級檢查命令
```bash
# 檢查依賴更新
mvn versions:display-dependency-updates

# 檢查插件更新
mvn versions:display-plugin-updates
```

---

## 八、總結

### 核心技術棧
- **Java 21** - 最新 LTS 版本
- **Spring Boot 3.5.4** - 傳統 MVC（非 WebFlux）
- **PostgreSQL 42.7.5** - 主數據庫（支持 JSONB、數組、CTE）
- **Vavr 0.10.4** - 函數式編程（Option、Try、Either）
- **MyBatis Plus 3.5.12** - ORM 框架

### 質量保障
- **ArchUnit 1.3.0** - 架構約束測試
- **JaCoCo** - 測試覆蓋率 ≥ 80%
- **Checkstyle + PMD + SpotBugs** - 代碼質量
- **SonarQube** - 持續質量監控

### 關鍵原則
- ✅ 使用 PostgreSQL 替代 MySQL
- ✅ 使用 Vavr 替代 Spring WebFlux
- ✅ 使用 Option 替代 Optional（Service 層）
- ✅ 使用 LambdaQueryWrapper 保證類型安全
- ✅ 自動化測試覆蓋率 ≥ 80%
