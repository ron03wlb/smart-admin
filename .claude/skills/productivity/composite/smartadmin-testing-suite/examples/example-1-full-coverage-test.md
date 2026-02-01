# Example 1: Full Coverage Test Suite

## Scenario
為 Employee 模塊生成完整測試套件（單元 + 集成 + E2E）

## Input
```bash
User: "Generate full test suite for Employee module"
```

## Generated Tests

### 1. Unit Tests
```java
@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {
    @Test
    void shouldSaveEmployee() {
        // Given
        EmployeeForm form = EmployeeFixture.createForm();

        // When
        ResponseDTO<Long> result = employeeService.save(form);

        // Then
        assertThat(result.getOk()).isTrue();
    }
}
```

### 2. Integration Tests
```java
@SpringBootTest
@Testcontainers
class EmployeeIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @Test
    void shouldCreateAndQueryEmployee() {
        // Test full CRUD flow with real DB
    }
}
```

### 3. E2E Tests
```typescript
test('Employee CRUD flow', async ({ page }) => {
  await page.goto('/employee');
  await page.click('text=新增員工');
  await page.fill('#employeeName', '張三');
  await page.click('text=保存');
  await expect(page.locator('.success-message')).toBeVisible();
});
```

## Expected Result
```
Test Coverage Report:
- Unit Tests: 95% (45/47 methods)
- Integration Tests: 85% (12/14 flows)
- E2E Tests: 80% (8/10 user journeys)

Overall: 90% coverage ✅
```
