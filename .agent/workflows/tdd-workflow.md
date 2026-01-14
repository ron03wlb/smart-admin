---
trigger: on_demand
description: 測試驅動開發工作流程（TDD）
tags: [tdd, testing, junit5, mockito, test-driven-development]
required_rules:
  - rules/10-architecture-rules.md
  - rules/01-naming-conventions.md
  - rules/08-vavr-fundamentals.md

execution_order:
  - step: write_failing_test
    description: 編寫失敗測試（RED）
    apply_rules: [01-naming, 10-architecture]
    expected_output: 測試類文件，測試失敗
    validation: 測試編譯通過但執行失敗

  - step: implement_minimal_code
    description: 最小化實現（GREEN）
    apply_rules: [01-naming, 08-vavr, 10-architecture]
    expected_output: 實現類文件，測試通過
    validation: mvn test 通過

  - step: refactor_code
    description: 重構優化（REFACTOR）
    apply_rules: [02-oop, 08-vavr-advanced]
    expected_output: 優化後的代碼
    validation: mvn test 仍通過，代碼質量提升

  - step: verify_quality
    description: 質量門禁檢查（VERIFY）
    commands:
      - mvn test
      - mvn jacoco:report
      - mvn test -Dtest=ArchitectureTest
    quality_gates:
      - coverage: ">= 80%"
      - archunit: "all pass"
      - checkstyle: "0 errors"

last_updated: 2025-01-13
---

# 測試驅動開發工作流程（TDD）

---

## 🤖 AI 執行指南

### 何時應用此 Workflow
- ✅ 用戶要求 "使用 TDD 開發功能"
- ✅ 用戶說 "先寫測試"
- ✅ 需要高質量的代碼實現
- ✅ 開發複雜業務邏輯

### 執行檢查清單
在開始之前確認：
- [ ] 開發環境已初始化（參考 init.md）
- [ ] 理解用戶需求（功能、邊界條件、異常情況）
- [ ] 確定測試範圍（單元測試/集成測試）

### 執行流程決策樹
```
用戶要求: "使用 TDD 開發用戶查詢功能"
  ├─ 1️⃣ 確認需求
  │   ├─ 查詢單個用戶? → 是
  │   ├─ 需要處理不存在? → 是
  │   └─ 需要過濾條件? → 是（只返回活躍用戶）
  │
  ├─ 2️⃣ RED：編寫測試
  │   ├─ 測試文件: UserServiceTest.java
  │   ├─ 測試方法: shouldReturnSome_whenUserExists()
  │   ├─ 測試方法: shouldReturnNone_whenUserNotExists()
  │   └─ 測試方法: shouldFilterInactiveUsers()
  │
  ├─ 3️⃣ GREEN：最小實現
  │   ├─ Service 方法: Option<User> findById(Long id)
  │   ├─ 應用規則: 08-vavr（返回 Option）
  │   └─ 應用規則: 10-architecture（注入 Mapper）
  │
  ├─ 4️⃣ REFACTOR：重構
  │   ├─ 提取方法: findActiveById()
  │   └─ 使用鏈式調用: findById().filter(User::isActive)
  │
  └─ 5️⃣ VERIFY：質量檢查
      ├─ mvn test（所有測試通過）
      ├─ mvn jacoco:report（覆蓋率 >= 80%）
      └─ mvn test -Dtest=ArchitectureTest（架構正確）
```

---

## Red-Green-Refactor 循環

### 1. RED 階段 (寫失敗測試)
```java
@Test
void shouldCreateUser_whenValidInput() {
    // Given
    UserCreateDTO dto = new UserCreateDTO("john@example.com", "John");
    
    // When
    User result = userService.createUser(dto);
    
    // Then
    assertThat(result).isNotNull();
    assertThat(result.getEmail()).isEqualTo("john@example.com");
    verify(userRepository).save(any(User.class));
}
```
**執行**: `mvn test -Dtest=UserServiceTest#shouldCreateUser_whenValidInput`
**預期**: 編譯失敗或測試失敗

### 2. GREEN 階段 (最小實現)
```java
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    
    public User createUser(UserCreateDTO dto) {
        User user = new User();
        user.setEmail(dto.getEmail());
        user.setName(dto.getName());
        return userRepository.save(user);
    }
}
```
**執行**: `mvn test`
**預期**: 測試通過

### 3. REFACTOR 階段
```java
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;  // MapStruct
    
    @Transactional(rollbackFor = Exception.class)
    public User createUser(UserCreateDTO dto) {
        User user = userMapper.toEntity(dto);
        return userRepository.save(user);
    }
}
```
**執行**: `mvn test`
**預期**: 測試仍通過，代碼更乾淨

## Spring Boot Test Slices

### @WebMvcTest - Controller 測試
```java
@WebMvcTest(UserController.class)
class UserControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private UserService userService;
    
    @Test
    void shouldReturnUser_whenExists() throws Exception {
        when(userService.findById(1L))
            .thenReturn(Optional.of(new User(1L, "John")));
        
        mockMvc.perform(get("/api/users/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("John"));
    }
}
```

### @DataJpaTest - Repository 測試
```java
@DataJpaTest
class UserRepositoryTest {
    @Autowired private TestEntityManager em;
    @Autowired private UserRepository repository;
    
    @Test
    void shouldFindByEmail() {
        em.persistAndFlush(new User("john@test.com", "John"));
        
        Optional<User> found = repository.findByEmail("john@test.com");
        
        assertThat(found).isPresent()
            .get().extracting(User::getName).isEqualTo("John");
    }
}
```

### @SpringBootTest - 整合測試
```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ApplicationIT {
    @Autowired private TestRestTemplate restTemplate;
    
    @Test
    void shouldCreateAndRetrieveUser() {
        ResponseEntity<User> response = restTemplate
            .postForEntity("/api/users", createDTO, User.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }
}
```

## JaCoCo 覆蓋率配置
```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <configuration>
        <excludes>
            <exclude>**/generated/**</exclude>
            <exclude>**/*MapperImpl.class</exclude>
            <exclude>**/Application.class</exclude>
        </excludes>
    </configuration>
    <executions>
        <execution>
            <id>check</id>
            <phase>verify</phase>
            <goals><goal>check</goal></goals>
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
                            <limit>
                                <counter>BRANCH</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.70</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

## 測試命名規範

### Given-When-Then 格式 (推薦)
```java
givenValidUser_whenCreateUser_thenUserIsSaved()
givenInvalidEmail_whenCreateUser_thenThrowsValidationException()
givenExistingId_whenFindById_thenReturnsUser()
```

### @Nested 分組
```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Nested
    @DisplayName("Create User")
    class CreateUser {
        @Test void shouldSaveUser_whenValidInput() { }
        @Test void shouldThrow_whenEmailExists() { }
    }
    
    @Nested
    @DisplayName("Find User")
    class FindUser {
        @Test void shouldReturn_whenExists() { }
        @Test void shouldReturnEmpty_whenNotFound() { }
    }
}
```