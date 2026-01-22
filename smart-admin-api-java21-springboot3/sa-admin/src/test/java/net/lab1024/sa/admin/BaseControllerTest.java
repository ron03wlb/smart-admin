package net.lab1024.sa.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Base class for Controller layer tests using MockMvc
 *
 * <p>Provides MockMvc for testing REST endpoints without starting the full HTTP server. Use this
 * class when testing:
 *
 * <ul>
 *   <li>Request mapping and URL patterns
 *   <li>Request/Response serialization
 *   <li>@Valid validation on request bodies
 *   <li>HTTP status codes
 *   <li>@SaCheckPermission authorization (with mocked Sa-Token)
 * </ul>
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * @WebMvcTest(EmployeeController.class)
 * class EmployeeControllerTest extends BaseControllerTest {
 *     @MockBean private EmployeeService employeeService;
 *
 *     @Test
 *     void testAddEmployee_ValidRequest_ReturnsOk() throws Exception {
 *         EmployeeAddForm form = EmployeeTestFixture.createAddForm();
 *         when(employeeService.addEmployee(any())).thenReturn(ResponseDTO.ok());
 *
 *         mockMvc.perform(post("/employee/add")
 *                 .contentType(MediaType.APPLICATION_JSON)
 *                 .content(toJson(form)))
 *             .andExpect(status().isOk())
 *             .andExpect(jsonPath("$.ok").value(true));
 *     }
 * }
 * }</pre>
 *
 * <p><b>Note:</b> {@code @WebMvcTest} loads only the web layer (Controllers), so Service/Manager
 * dependencies must be mocked with {@code @MockBean}.
 *
 * @author SmartAdmin Testing Framework
 * @since 2025-01-22
 */
@WebMvcTest
public abstract class BaseControllerTest {

  @Autowired protected MockMvc mockMvc;

  @Autowired protected ObjectMapper objectMapper;

  /**
   * Convert object to JSON string for request body
   *
   * @param obj Object to convert
   * @return JSON string representation
   * @throws JsonProcessingException if serialization fails
   */
  protected String toJson(Object obj) throws JsonProcessingException {
    return objectMapper.writeValueAsString(obj);
  }

  /**
   * Convert JSON string to object (for response parsing)
   *
   * @param json JSON string
   * @param clazz Target class type
   * @param <T> Type parameter
   * @return Deserialized object
   * @throws JsonProcessingException if deserialization fails
   */
  protected <T> T fromJson(String json, Class<T> clazz) throws JsonProcessingException {
    return objectMapper.readValue(json, clazz);
  }
}
