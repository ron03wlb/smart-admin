package net.lab1024.sa.admin;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base class for Spring Boot integration tests
 *
 * <p>Provides Spring context with auto-rollback after each test. Use this class when testing
 * components that require:
 *
 * <ul>
 *   <li>Real Spring beans (not mocks)
 *   <li>Database access (via real DataSource)
 *   <li>@Transactional behavior verification
 *   <li>@Cached annotation testing (with Testcontainers Redis)
 * </ul>
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * @SpringBootTest
 * @Transactional  // Auto-rollback after each test
 * class EmployeeManagerIntTest extends BaseIntegrationTest {
 *     @Autowired private EmployeeManager employeeManager;
 *     @Autowired private EmployeeDao employeeDao;
 *
 *     @Test
 *     void testSaveEmployee_TransactionCommit() {
 *         employeeManager.saveEmployee(employee, roleIds);
 *         // Verify actual database state
 *     }
 * }
 * }</pre>
 *
 * <p><b>Note:</b> {@code @Transactional} on test class causes auto-rollback after each test,
 * keeping the database clean without manual cleanup.
 *
 * @author SmartAdmin Testing Framework
 * @since 2025-01-22
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional // Auto-rollback after each test
public abstract class BaseIntegrationTest {

  @Autowired protected DataSource dataSource;

  /**
   * Override this method to set up test data before each test
   *
   * <p>Example:
   *
   * <pre>{@code
   * @BeforeEach
   * void setUp() {
   *     // Create test department
   *     DepartmentEntity dept = new DepartmentEntity();
   *     dept.setDepartmentName("Test Department");
   *     departmentDao.insert(dept);
   *     testDepartmentId = dept.getDepartmentId();
   * }
   * }</pre>
   */
  protected void setUp() {
    // Subclasses can override if needed
  }

  /**
   * Override this method to clean up resources after each test
   *
   * <p><b>Note:</b> With {@code @Transactional} on the test class, database changes are
   * automatically rolled back, so manual cleanup is usually not needed.
   */
  protected void tearDown() {
    // Subclasses can override if needed
  }
}
