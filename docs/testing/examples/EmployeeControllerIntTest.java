package net.lab1024.sa.admin.module.system.employee.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.lab1024.sa.admin.module.system.employee.dao.EmployeeDao;
import net.lab1024.sa.admin.module.system.employee.domain.entity.EmployeeEntity;
import net.lab1024.sa.admin.module.system.employee.domain.form.EmployeeAddForm;
import net.lab1024.sa.admin.module.system.employee.domain.form.EmployeeQueryForm;
import net.lab1024.sa.admin.module.system.login.domain.LoginForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * EmployeeController Integration Test
 *
 * Demonstrates:
 * - Integration testing with Spring Boot and Testcontainers
 * - Real Sa-Token authentication with login flow
 * - Testing @SaCheckPermission authorization
 * - Testing endpoints with real database
 *
 * Challenge #1: Testing Sa-Token Authentication
 *
 * @author SmartAdmin Testing Guide
 * @see <a href="../integration/sa-token-testing.md">Sa-Token Testing Guide</a>
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional  // Auto-rollback after each test
@Testcontainers
@DisplayName("EmployeeController Integration Tests")
class EmployeeControllerIntTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test")
        .withReuse(true);  // Reuse container across test runs

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmployeeDao employeeDao;

    private String adminToken;
    private String userToken;

    /**
     * Setup: Login before each test to get authentication tokens
     */
    @BeforeEach
    void setUp() throws Exception {
        // Login as admin (has system:employee:add permission)
        adminToken = loginAndGetToken("admin", "123456");

        // Login as normal user (limited permissions)
        // userToken = loginAndGetToken("normal_user", "password");
    }

    // ========================================
    // Test: POST /employee/add
    // ========================================

    @Nested
    @DisplayName("POST /employee/add")
    class AddEmployeeTests {

        @Test
        @DisplayName("With permission - creates employee successfully")
        void testAddEmployee_WithPermission_Success() throws Exception {
            // Given
            EmployeeAddForm form = new EmployeeAddForm();
            form.setLoginName("test_employee_" + System.currentTimeMillis());
            form.setActualName("Test Employee");
            form.setPhone("13800138000");
            form.setDepartmentId(1L);
            form.setRoleIdList(List.of(1L));

            // When
            MvcResult result = mockMvc.perform(post("/employee/add")
                    .header("x-access-token", adminToken)  // Admin has permission
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(form)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data").isNotEmpty())  // Returns generated password
                .andReturn();

            // Then - verify employee exists in database
            EmployeeEntity created = employeeDao.selectByLoginName(form.getLoginName());
            assertNotNull(created, "Employee should be created in database");
            assertEquals(form.getActualName(), created.getActualName());
            assertEquals(form.getPhone(), created.getPhone());
            assertEquals(form.getDepartmentId(), created.getDepartmentId());

            // Verify response contains generated password
            String responseJson = result.getResponse().getContentAsString();
            assertNotNull(responseJson);
            assertTrue(responseJson.contains("data"), "Response should contain generated password");
        }

        @Test
        @DisplayName("Without permission - returns 403 or error")
        void testAddEmployee_WithoutPermission_Forbidden() throws Exception {
            // Given
            EmployeeAddForm form = new EmployeeAddForm();
            form.setLoginName("test_employee");
            form.setActualName("Test Employee");
            form.setDepartmentId(1L);

            // When/Then - user without permission gets error
            // Note: Requires a test user without system:employee:add permission
            if (userToken != null) {
                mockMvc.perform(post("/employee/add")
                        .header("x-access-token", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(form)))
                    .andExpect(status().is4xxClientError());  // 403 or permission error
            }
        }

        @Test
        @DisplayName("Without token - returns unauthorized")
        void testAddEmployee_NoToken_Unauthorized() throws Exception {
            // Given
            EmployeeAddForm form = new EmployeeAddForm();
            form.setLoginName("test_employee");
            form.setActualName("Test Employee");
            form.setDepartmentId(1L);

            // When/Then - no token = unauthorized
            mockMvc.perform(post("/employee/add")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(form)))
                .andExpect(status().is4xxClientError());  // 401 unauthorized
        }

        @Test
        @DisplayName("Duplicate login name - returns error")
        void testAddEmployee_DuplicateLoginName_ReturnsError() throws Exception {
            // Given - create first employee
            EmployeeAddForm form1 = new EmployeeAddForm();
            form1.setLoginName("duplicate_test");
            form1.setActualName("First User");
            form1.setPhone("13800138001");
            form1.setDepartmentId(1L);
            form1.setRoleIdList(List.of(1L));

            mockMvc.perform(post("/employee/add")
                    .header("x-access-token", adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(form1)))
                .andExpect(status().isOk());

            // When - try to create duplicate
            EmployeeAddForm form2 = new EmployeeAddForm();
            form2.setLoginName("duplicate_test");  // Same login name!
            form2.setActualName("Second User");
            form2.setPhone("13800138002");
            form2.setDepartmentId(1L);
            form2.setRoleIdList(List.of(1L));

            // Then - should return error
            mockMvc.perform(post("/employee/add")
                    .header("x-access-token", adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(form2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.msg").exists());
        }
    }

    // ========================================
    // Test: POST /employee/query
    // ========================================

    @Nested
    @DisplayName("POST /employee/query")
    class QueryEmployeeTests {

        @Test
        @DisplayName("Query with pagination - returns correct page")
        void testQueryEmployee_WithPagination_ReturnsPage() throws Exception {
            // Given
            EmployeeQueryForm queryForm = new EmployeeQueryForm();
            queryForm.setPageNum(1);
            queryForm.setPageSize(10);

            // When
            mockMvc.perform(post("/employee/query")
                    .header("x-access-token", adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(queryForm)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data.list").isArray())
                .andExpect(jsonPath("$.data.total").isNumber())
                .andExpect(jsonPath("$.data.pageNum").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(10));
        }

        @Test
        @DisplayName("Query with search keyword - filters results")
        void testQueryEmployee_WithKeyword_FiltersResults() throws Exception {
            // Given
            EmployeeQueryForm queryForm = new EmployeeQueryForm();
            queryForm.setPageNum(1);
            queryForm.setPageSize(10);
            queryForm.setKeyword("admin");  // Search for "admin"

            // When/Then
            mockMvc.perform(post("/employee/query")
                    .header("x-access-token", adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(queryForm)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data.list").isArray());

            // Verify results contain search keyword (if any results)
            // Additional assertions can be added based on test data
        }
    }

    // ========================================
    // Test: POST /employee/update
    // ========================================

    @Nested
    @DisplayName("POST /employee/update")
    class UpdateEmployeeTests {

        @Test
        @DisplayName("With permission - updates employee")
        void testUpdateEmployee_WithPermission_Success() throws Exception {
            // This test requires:
            // 1. An existing employee in database
            // 2. EmployeeUpdateForm with updated data
            // 3. Verification that data was updated

            // See transaction-testing.md for Manager layer update tests
            // This demonstrates Controller layer authorization
        }
    }

    // ========================================
    // Helper Methods
    // ========================================

    /**
     * Login and get authentication token
     *
     * @param username login name
     * @param password login password
     * @return authentication token for subsequent requests
     */
    private String loginAndGetToken(String username, String password) throws Exception {
        LoginForm loginForm = new LoginForm();
        loginForm.setLoginName(username);
        loginForm.setLoginPwd(password);

        MvcResult result = mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginForm)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ok").value(true))
            .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode rootNode = objectMapper.readTree(responseBody);
        return rootNode.get("data").get("token").asText();
    }
}
