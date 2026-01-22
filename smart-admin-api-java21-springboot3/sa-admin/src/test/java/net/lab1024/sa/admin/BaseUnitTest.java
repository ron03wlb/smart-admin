package net.lab1024.sa.admin;

import static org.junit.jupiter.api.Assertions.*;

import net.lab1024.sa.common.core.domain.ResponseDTO;
import net.lab1024.sa.common.core.code.ErrorCode;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Base class for pure unit tests
 *
 * <p>Provides common assertion helpers for ResponseDTO and error handling. Extends this class when
 * writing unit tests with @Mock and @InjectMocks.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * @ExtendWith(MockitoExtension.class)
 * class EmployeeServiceTest extends BaseUnitTest {
 *     @Mock private EmployeeDao employeeDao;
 *     @InjectMocks private EmployeeService employeeService;
 *
 *     @Test
 *     void testAddEmployee_Success() {
 *         ResponseDTO<String> response = employeeService.addEmployee(form);
 *         assertOk(response);
 *     }
 * }
 * }</pre>
 *
 * @author SmartAdmin Testing Framework
 * @since 2025-01-22
 */
@ExtendWith(MockitoExtension.class)
public abstract class BaseUnitTest {

  /**
   * Assert that ResponseDTO indicates success
   *
   * @param response ResponseDTO to check
   * @param <T> Response data type
   */
  protected <T> void assertOk(ResponseDTO<T> response) {
    assertNotNull(response, "Response should not be null");
    assertTrue(response.getOk(), "Expected success but got error: " + response.getMsg());
  }

  /**
   * Assert that ResponseDTO indicates success with specific data
   *
   * @param response ResponseDTO to check
   * @param expectedData Expected data value
   * @param <T> Response data type
   */
  protected <T> void assertOkWithData(ResponseDTO<T> response, T expectedData) {
    assertOk(response);
    assertEquals(expectedData, response.getData(), "Response data does not match expected");
  }

  /**
   * Assert that ResponseDTO indicates error with specific error code
   *
   * @param response ResponseDTO to check
   * @param expectedCode Expected error code
   * @param <T> Response data type
   */
  protected <T> void assertError(ResponseDTO<T> response, ErrorCode expectedCode) {
    assertNotNull(response, "Response should not be null");
    assertFalse(response.getOk(), "Expected error but got success");
    assertEquals(expectedCode.getCode(), response.getCode(), "Error code does not match");
  }

  /**
   * Assert that ResponseDTO indicates error with message containing specific text
   *
   * @param response ResponseDTO to check
   * @param messageFragment Fragment that should be in error message
   * @param <T> Response data type
   */
  protected <T> void assertErrorContains(ResponseDTO<T> response, String messageFragment) {
    assertNotNull(response, "Response should not be null");
    assertFalse(response.getOk(), "Expected error but got success");
    assertNotNull(response.getMsg(), "Error message should not be null");
    assertTrue(
        response.getMsg().contains(messageFragment),
        "Error message '" + response.getMsg() + "' does not contain '" + messageFragment + "'");
  }

  /**
   * Assert that ResponseDTO indicates error (without checking specific code)
   *
   * @param response ResponseDTO to check
   * @param <T> Response data type
   */
  protected <T> void assertError(ResponseDTO<T> response) {
    assertNotNull(response, "Response should not be null");
    assertFalse(response.getOk(), "Expected error but got success: " + response.getData());
  }

  /**
   * Assert that ResponseDTO indicates success and data is not null
   *
   * @param response ResponseDTO to check
   * @param <T> Response data type
   * @return The data from the response (for further assertions)
   */
  protected <T> T assertOkAndGetData(ResponseDTO<T> response) {
    assertOk(response);
    assertNotNull(response.getData(), "Response data should not be null");
    return response.getData();
  }
}
