---
trigger: on_demand
description: Test-Driven Development (TDD) workflow
tags: [tdd, testing, junit5, mockito, test-driven-development]
required_rules:
  - rules/foundation/F04-architecture-rules.md
  - rules/foundation/F01-naming-conventions.md
  - rules/technology/functional/P01-vavr-fundamentals.md

execution_order:
  - step: write_failing_test
    description: Write failing test (RED)
    apply_rules: [01-naming, 10-architecture]
    expected_output: Test class file, test fails
    validation: Test compiles but execution fails

  - step: implement_minimal_code
    description: Minimal implementation (GREEN)
    apply_rules: [01-naming, 08-vavr, 10-architecture]
    expected_output: Implementation class file, test passes
    validation: mvn test passes

  - step: refactor_code
    description: Refactor optimization (REFACTOR)
    apply_rules: [02-oop, 08-vavr-advanced]
    expected_output: Optimized code
    validation: mvn test still passes, code quality improved

  - step: verify_quality
    description: Quality Gate check (VERIFY)
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

# Test-Driven Development (TDD) Workflow

---

## 🤖 AI Execution Guide

### When to Apply This Workflow
- ✅ User requests "develop feature using TDD"
- ✅ User says "write test first"
- ✅ Need high-quality code implementation
- ✅ Developing complex business logic

### Execution Checklist
Confirm before starting:
- [ ] Development environment initialized (reference 01-environment-setup.md)
- [ ] Understand user requirements (functionality, boundary conditions, exceptional cases)
- [ ] Determine test scope (unit test/integration test)

### Execution Flow Decision Tree
```
User request: "develop user query feature using TDD"
  ├─ 1️⃣ Confirm requirements
  │   ├─ Query single user? → Yes
  │   ├─ Handle non-existent? → Yes
  │   └─ Need filter conditions? → Yes (only return active users)
  │
  ├─ 2️⃣ RED: Write test
  │   ├─ Test file: UserServiceTest.java
  │   ├─ Test method: shouldReturnSome_whenUserExists()
  │   ├─ Test method: shouldReturnNone_whenUserNotExists()
  │   └─ Test method: shouldFilterInactiveUsers()
  │
  ├─ 3️⃣ GREEN: Minimal implementation
  │   ├─ Service method: Option<User> findById(Long id)
  │   ├─ Apply rule: 08-vavr (return Option)
  │   └─ Apply rule: 10-architecture (inject Mapper)
  │
  ├─ 4️⃣ REFACTOR: Refactor
  │   ├─ Extract method: findActiveById()
  │   └─ Use method chaining: findById().filter(User::isActive)
  │
  └─ 5️⃣ VERIFY: Quality check
      ├─ mvn test (all tests pass)
      ├─ mvn jacoco:report (coverage >= 80%)
      └─ mvn test -Dtest=ArchitectureTest (architecture correct)
```

---

## Red-Green-Refactor Cycle

### 1. RED Phase (Write Failing Test)
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
**Execute**: `mvn test -Dtest=UserServiceTest#shouldCreateUser_whenValidInput`
**Expected**: Compilation fails or test fails

### 2. GREEN Phase (Minimal Implementation)
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
**Execute**: `mvn test`
**Expected**: Test passes

### 3. REFACTOR Phase
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
**Execute**: `mvn test`
**Expected**: Test still passes, code cleaner

## Spring Boot Test Slices

### @WebMvcTest - Controller Tests
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

### @DataJpaTest - Repository Tests
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

### @SpringBootTest - Integration Tests
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

## JaCoCo Coverage Configuration
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

## Test Naming Conventions

### Given-When-Then Format (Recommended)
```java
givenValidUser_whenCreateUser_thenUserIsSaved()
givenInvalidEmail_whenCreateUser_thenThrowsValidationException()
givenExistingId_whenFindById_thenReturnsUser()
```

### @Nested Grouping
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
