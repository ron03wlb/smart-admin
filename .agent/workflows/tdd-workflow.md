---
trigger: on_demand
description: 測試驅動開發工作流程（TDD）
tags: [tdd, testing, junit5, mockito, test-driven-development]
required_rules:
  - rules/10-architecture-rules.md
  - rules/01-naming-conventions.md
last_updated: 2025-01-12
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