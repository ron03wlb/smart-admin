package net.lab1024.sa.igaming.activity.turnover.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.control.Option;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.activity.turnover.domain.form.TurnoverGameWeightRuleAddForm;
import net.lab1024.sa.igaming.activity.turnover.domain.form.TurnoverGameWeightRuleQueryForm;
import net.lab1024.sa.igaming.activity.turnover.domain.form.TurnoverGameWeightRuleUpdateForm;
import net.lab1024.sa.igaming.activity.turnover.domain.vo.TurnoverGameWeightRuleVO;
import net.lab1024.sa.igaming.activity.turnover.service.TurnoverGameWeightRuleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Unit tests for {@link TurnoverGameWeightRuleController}.
 *
 * <p>Tests REST API endpoints using MockMvc.
 *
 * @author iGaming Team
 * @since 2026-03-15
 */
@WebMvcTest(TurnoverGameWeightRuleController.class)
@DisplayName("TurnoverGameWeightRuleController API 測試")
class TurnoverGameWeightRuleControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockBean private TurnoverGameWeightRuleService gameWeightRuleService;

  private static final String BASE_URL = "/igaming/activity/turnover/game-weight";

  // ===== GET /get/{ruleId} 測試 =====

  @Nested
  @DisplayName("GET /get/{ruleId} - 獲取單個規則")
  class GetRuleTests {

    @Test
    @DisplayName("當規則存在時應該返回規則詳情")
    void whenRuleExists_ShouldReturnRuleDetails() throws Exception {
      // Arrange
      TurnoverGameWeightRuleVO vo = createSampleVO(1L, 1, new BigDecimal("100.00"));
      when(gameWeightRuleService.getRule(1L)).thenReturn(Option.of(vo));

      // Act & Assert
      mockMvc
          .perform(get(BASE_URL + "/get/1"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.ok").value(true))
          .andExpect(jsonPath("$.data.ruleId").value(1))
          .andExpect(jsonPath("$.data.gameCategory").value(1))
          .andExpect(jsonPath("$.data.weightPercentage").value(100.00));
    }

    @Test
    @DisplayName("當規則不存在時應該返回用戶錯誤")
    void whenRuleNotFound_ShouldReturnUserError() throws Exception {
      // Arrange
      when(gameWeightRuleService.getRule(999L)).thenReturn(Option.none());

      // Act & Assert
      mockMvc
          .perform(get(BASE_URL + "/get/999"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.ok").value(false))
          .andExpect(jsonPath("$.msg").value("規則不存在"));
    }
  }

  // ===== POST /query 測試 =====

  @Nested
  @DisplayName("POST /query - 分頁查詢")
  class QueryRulesTests {

    @Test
    @DisplayName("應該返回分頁結果")
    void shouldReturnPagedResults() throws Exception {
      // Arrange
      List<TurnoverGameWeightRuleVO> list =
          Arrays.asList(
              createSampleVO(1L, 1, new BigDecimal("100.00")),
              createSampleVO(2L, 2, new BigDecimal("15.00")));
      PageResult<TurnoverGameWeightRuleVO> pageResult = new PageResult<>();
      pageResult.setPageNum(1L);
      pageResult.setPageSize(10L);
      pageResult.setTotal(2L);
      pageResult.setPages(1L);
      pageResult.setList(list);
      pageResult.setEmptyFlag(false);

      when(gameWeightRuleService.queryRules(any(TurnoverGameWeightRuleQueryForm.class)))
          .thenReturn(ResponseDTO.ok(pageResult));

      TurnoverGameWeightRuleQueryForm form = new TurnoverGameWeightRuleQueryForm();
      form.setPageNum(1L);
      form.setPageSize(10L);

      // Act & Assert
      mockMvc
          .perform(
              post(BASE_URL + "/query")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(form)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.ok").value(true))
          .andExpect(jsonPath("$.data.total").value(2))
          .andExpect(jsonPath("$.data.list.length()").value(2));
    }

    @Test
    @DisplayName("應該支持遊戲類別篩選")
    void shouldSupportGameCategoryFilter() throws Exception {
      // Arrange
      List<TurnoverGameWeightRuleVO> list =
          Arrays.asList(createSampleVO(1L, 1, new BigDecimal("100.00")));
      PageResult<TurnoverGameWeightRuleVO> pageResult = new PageResult<>();
      pageResult.setPageNum(1L);
      pageResult.setPageSize(10L);
      pageResult.setTotal(1L);
      pageResult.setPages(1L);
      pageResult.setList(list);
      pageResult.setEmptyFlag(false);

      when(gameWeightRuleService.queryRules(any(TurnoverGameWeightRuleQueryForm.class)))
          .thenReturn(ResponseDTO.ok(pageResult));

      TurnoverGameWeightRuleQueryForm form = new TurnoverGameWeightRuleQueryForm();
      form.setPageNum(1L);
      form.setPageSize(10L);
      form.setGameCategory(1);

      // Act & Assert
      mockMvc
          .perform(
              post(BASE_URL + "/query")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(form)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.list[0].gameCategory").value(1));
    }
  }

  // ===== POST /add 測試 =====

  @Nested
  @DisplayName("POST /add - 新增規則")
  class AddRuleTests {

    @Test
    @DisplayName("應該成功新增規則")
    void shouldAddRuleSuccessfully() throws Exception {
      // Arrange
      when(gameWeightRuleService.addRule(
              any(TurnoverGameWeightRuleAddForm.class), anyLong(), anyString(), anyString()))
          .thenReturn(ResponseDTO.ok());

      TurnoverGameWeightRuleAddForm form = createAddForm();

      // Act & Assert
      mockMvc
          .perform(
              post(BASE_URL + "/add")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(form))
                  .param("operatorId", "1001")
                  .param("operatorName", "Admin")
                  .param("changeReason", "Initial setup"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.ok").value(true));
    }

    @Test
    @DisplayName("當缺少必需參數時應該返回400")
    void whenMissingRequiredParams_ShouldReturn400() throws Exception {
      // Arrange
      TurnoverGameWeightRuleAddForm form = createAddForm();

      // Act & Assert - Missing operatorId
      mockMvc
          .perform(
              post(BASE_URL + "/add")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(form))
                  .param("operatorName", "Admin"))
          .andExpect(status().isBadRequest());
    }
  }

  // ===== PUT /update 測試 =====

  @Nested
  @DisplayName("PUT /update - 更新規則")
  class UpdateRuleTests {

    @Test
    @DisplayName("應該成功更新規則")
    void shouldUpdateRuleSuccessfully() throws Exception {
      // Arrange
      when(gameWeightRuleService.updateRule(
              any(TurnoverGameWeightRuleUpdateForm.class), anyLong(), anyString(), anyString()))
          .thenReturn(ResponseDTO.ok());

      TurnoverGameWeightRuleUpdateForm form = createUpdateForm();

      // Act & Assert
      mockMvc
          .perform(
              put(BASE_URL + "/update")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(form))
                  .param("operatorId", "1001")
                  .param("operatorName", "Admin")
                  .param("changeReason", "Weight adjustment"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.ok").value(true));
    }

    @Test
    @DisplayName("應該支持可選的變更原因")
    void shouldSupportOptionalChangeReason() throws Exception {
      // Arrange
      when(gameWeightRuleService.updateRule(
              any(TurnoverGameWeightRuleUpdateForm.class), eq(1001L), eq("Admin"), eq(null)))
          .thenReturn(ResponseDTO.ok());

      TurnoverGameWeightRuleUpdateForm form = createUpdateForm();

      // Act & Assert
      mockMvc
          .perform(
              put(BASE_URL + "/update")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(form))
                  .param("operatorId", "1001")
                  .param("operatorName", "Admin"))
          .andExpect(status().isOk());
    }
  }

  // ===== DELETE /delete/{ruleId} 測試 =====

  @Nested
  @DisplayName("DELETE /delete/{ruleId} - 刪除規則")
  class DeleteRuleTests {

    @Test
    @DisplayName("應該成功刪除規則")
    void shouldDeleteRuleSuccessfully() throws Exception {
      // Arrange
      when(gameWeightRuleService.deleteRule(eq(1L), anyLong(), anyString(), anyString()))
          .thenReturn(ResponseDTO.ok());

      // Act & Assert
      mockMvc
          .perform(
              delete(BASE_URL + "/delete/1")
                  .param("operatorId", "1001")
                  .param("operatorName", "Admin")
                  .param("changeReason", "No longer needed"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.ok").value(true));
    }

    @Test
    @DisplayName("當規則不存在時應該返回錯誤")
    void whenRuleNotFound_ShouldReturnError() throws Exception {
      // Arrange
      when(gameWeightRuleService.deleteRule(eq(999L), anyLong(), anyString(), anyString()))
          .thenReturn(ResponseDTO.userErrorParam("規則不存在"));

      // Act & Assert
      mockMvc
          .perform(
              delete(BASE_URL + "/delete/999")
                  .param("operatorId", "1001")
                  .param("operatorName", "Admin"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.ok").value(false));
    }
  }

  // ===== POST /evict-cache 測試 =====

  @Nested
  @DisplayName("POST /evict-cache - 清除緩存")
  class EvictCacheTests {

    @Test
    @DisplayName("應該成功清除緩存")
    void shouldEvictCacheSuccessfully() throws Exception {
      // Arrange
      when(gameWeightRuleService.evictCache()).thenReturn(ResponseDTO.ok());

      // Act & Assert
      mockMvc
          .perform(post(BASE_URL + "/evict-cache"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.ok").value(true));
    }
  }

  // ===== Helper Methods =====

  private TurnoverGameWeightRuleVO createSampleVO(
      Long ruleId, Integer gameCategory, BigDecimal weight) {
    TurnoverGameWeightRuleVO vo = new TurnoverGameWeightRuleVO();
    vo.setRuleId(ruleId);
    vo.setRuleCode("GW-TEST-" + ruleId);
    vo.setRuleName("Test Game Weight Rule " + ruleId);
    vo.setGameCategory(gameCategory);
    vo.setWeightPercentage(weight);
    vo.setPriority(100);
    vo.setStatus(1);
    vo.setEffectiveFrom(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));
    vo.setEffectiveTo(OffsetDateTime.now(ZoneOffset.UTC).plusDays(30));
    vo.setRemark("Test rule");
    vo.setCreateTime(OffsetDateTime.now(ZoneOffset.UTC));
    vo.setUpdateTime(OffsetDateTime.now(ZoneOffset.UTC));
    return vo;
  }

  private TurnoverGameWeightRuleAddForm createAddForm() {
    TurnoverGameWeightRuleAddForm form = new TurnoverGameWeightRuleAddForm();
    form.setRuleCode("GW-NEW-001");
    form.setRuleName("New Game Weight Rule");
    form.setGameCategory(1);
    form.setWeightPercentage(new BigDecimal("100.00"));
    form.setPriority(100);
    form.setEffectiveFrom(OffsetDateTime.now(ZoneOffset.UTC));
    form.setEffectiveTo(OffsetDateTime.now(ZoneOffset.UTC).plusDays(30));
    form.setRemark("New rule");
    return form;
  }

  private TurnoverGameWeightRuleUpdateForm createUpdateForm() {
    TurnoverGameWeightRuleUpdateForm form = new TurnoverGameWeightRuleUpdateForm();
    form.setRuleId(1L);
    form.setRuleCode("GW-TEST-001");
    form.setRuleName("Updated Game Weight Rule");
    form.setGameCategory(1);
    form.setWeightPercentage(new BigDecimal("80.00"));
    form.setPriority(100);
    form.setStatus(1);
    form.setEffectiveFrom(OffsetDateTime.now(ZoneOffset.UTC));
    form.setEffectiveTo(OffsetDateTime.now(ZoneOffset.UTC).plusDays(30));
    form.setRemark("Updated rule");
    form.setChangeReason("Test update");
    return form;
  }
}
