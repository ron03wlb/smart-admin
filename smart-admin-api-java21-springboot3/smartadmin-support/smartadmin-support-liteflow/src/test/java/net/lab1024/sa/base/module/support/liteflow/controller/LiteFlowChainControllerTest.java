package net.lab1024.sa.support.liteflow.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.lab1024.sa.common.core.domain.request.RequestUser;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.web.web.util.SmartRequestUtil;
import net.lab1024.sa.support.liteflow.domain.form.LiteFlowChainAddForm;
import net.lab1024.sa.support.liteflow.domain.form.LiteFlowChainQueryForm;
import net.lab1024.sa.support.liteflow.domain.form.LiteFlowChainUpdateForm;
import net.lab1024.sa.support.liteflow.domain.vo.LiteFlowChainVO;
import net.lab1024.sa.support.liteflow.service.LiteFlowChainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * LiteFlowChainController MockMvc 測試
 *
 * @author SmartAdmin Team
 * @since 2026-02-02
 */
@WebMvcTest(LiteFlowChainController.class)
@DisplayName("LiteFlowChainController MockMvc 測試")
class LiteFlowChainControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockBean private LiteFlowChainService chainService;

  private static final Long TEST_USER_ID = 1L;
  private static final String TEST_USER_NAME = "test-user";

  private RequestUser createTestRequestUser() {
    RequestUser requestUser = mock(RequestUser.class);
    when(requestUser.getUserId()).thenReturn(TEST_USER_ID);
    when(requestUser.getUserName()).thenReturn(TEST_USER_NAME);
    return requestUser;
  }

  @BeforeEach
  void setUp() {
    reset(chainService);
  }

  @Test
  @DisplayName("POST /liteflow/chain/queryPage - 分頁查詢流程")
  void testQueryPage() throws Exception {
    // Given
    LiteFlowChainQueryForm form = new LiteFlowChainQueryForm();
    form.setChainName("測試");
    form.setPageNum(1L);
    form.setPageSize(10L);

    PageResult<LiteFlowChainVO> pageResult = new PageResult<>();
    pageResult.setTotal(0L);
    pageResult.setList(java.util.Collections.emptyList());

    when(chainService.queryPage(any(LiteFlowChainQueryForm.class)))
        .thenReturn(ResponseDTO.ok(pageResult));

    // When & Then
    mockMvc
        .perform(
            post("/liteflow/chain/queryPage")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(form)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ok").value(true))
        .andExpect(jsonPath("$.data").exists())
        .andExpect(jsonPath("$.data.total").value(0));

    verify(chainService, times(1)).queryPage(any(LiteFlowChainQueryForm.class));
  }

  @Test
  @DisplayName("POST /liteflow/chain/add - 創建流程")
  void testAdd() throws Exception {
    // Given
    LiteFlowChainAddForm form = new LiteFlowChainAddForm();
    form.setChainName("測試流程");
    form.setChainCode("test-chain");
    form.setChainType(1);
    form.setChainData("THEN(a, b, c)");
    form.setRemark("測試");

    when(chainService.add(any(LiteFlowChainAddForm.class), anyLong(), anyString()))
        .thenReturn(ResponseDTO.ok());

    // When & Then
    try (MockedStatic<SmartRequestUtil> mockedStatic = mockStatic(SmartRequestUtil.class)) {
      mockedStatic.when(SmartRequestUtil::getRequestUser).thenReturn(createTestRequestUser());

      mockMvc
          .perform(
              post("/liteflow/chain/add")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(form)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.ok").value(true));

      verify(chainService, times(1))
          .add(any(LiteFlowChainAddForm.class), eq(TEST_USER_ID), eq(TEST_USER_NAME));
    }
  }

  @Test
  @DisplayName("POST /liteflow/chain/update - 更新流程")
  void testUpdate() throws Exception {
    // Given
    LiteFlowChainUpdateForm form = new LiteFlowChainUpdateForm();
    form.setChainId(1L);
    form.setChainName("更新流程");
    form.setChainData("THEN(d, e, f)");
    form.setRemark("更新");

    when(chainService.update(any(LiteFlowChainUpdateForm.class), anyLong(), anyString()))
        .thenReturn(ResponseDTO.ok());

    // When & Then
    try (MockedStatic<SmartRequestUtil> mockedStatic = mockStatic(SmartRequestUtil.class)) {
      mockedStatic.when(SmartRequestUtil::getRequestUser).thenReturn(createTestRequestUser());

      mockMvc
          .perform(
              post("/liteflow/chain/update")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(form)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.ok").value(true));

      verify(chainService, times(1))
          .update(any(LiteFlowChainUpdateForm.class), eq(TEST_USER_ID), eq(TEST_USER_NAME));
    }
  }

  @Test
  @DisplayName("GET /liteflow/chain/delete/{chainId} - 刪除流程")
  void testDelete() throws Exception {
    // Given
    Long chainId = 1L;

    when(chainService.delete(anyLong(), anyLong(), anyString())).thenReturn(ResponseDTO.ok());

    // When & Then
    try (MockedStatic<SmartRequestUtil> mockedStatic = mockStatic(SmartRequestUtil.class)) {
      mockedStatic.when(SmartRequestUtil::getRequestUser).thenReturn(createTestRequestUser());

      mockMvc
          .perform(get("/liteflow/chain/delete/" + chainId))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.ok").value(true));

      verify(chainService, times(1)).delete(eq(chainId), eq(TEST_USER_ID), eq(TEST_USER_NAME));
    }
  }

  @Test
  @DisplayName("GET /liteflow/chain/detail/{chainId} - 獲取流程詳情")
  void testGetDetail() throws Exception {
    // Given
    Long chainId = 1L;

    LiteFlowChainVO vo = new LiteFlowChainVO();
    vo.setChainId(chainId);
    vo.setChainName("測試流程");
    vo.setChainCode("test-chain");

    when(chainService.getDetail(anyLong())).thenReturn(ResponseDTO.ok(vo));

    // When & Then
    mockMvc
        .perform(get("/liteflow/chain/detail/" + chainId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ok").value(true))
        .andExpect(jsonPath("$.data").exists())
        .andExpect(jsonPath("$.data.chainId").value(chainId))
        .andExpect(jsonPath("$.data.chainName").value("測試流程"))
        .andExpect(jsonPath("$.data.chainCode").value("test-chain"));

    verify(chainService, times(1)).getDetail(eq(chainId));
  }

  @Test
  @DisplayName("POST /liteflow/chain/reloadAll - 重載所有流程")
  void testReloadAll() throws Exception {
    // Given
    when(chainService.reloadAll()).thenReturn(ResponseDTO.ok());

    // When & Then
    mockMvc
        .perform(post("/liteflow/chain/reloadAll"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ok").value(true));

    verify(chainService, times(1)).reloadAll();
  }

  @Test
  @DisplayName("POST /liteflow/chain/add - 參數驗證失敗")
  void testAdd_ValidationFailure() throws Exception {
    // Given
    LiteFlowChainAddForm form = new LiteFlowChainAddForm();
    // 缺少必填字段 chainName 和 chainCode

    // When & Then
    mockMvc
        .perform(
            post("/liteflow/chain/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(form)))
        .andExpect(status().isBadRequest());

    verify(chainService, never()).add(any(LiteFlowChainAddForm.class), anyLong(), anyString());
  }

  @Test
  @DisplayName("POST /liteflow/chain/add - 業務邏輯錯誤")
  void testAdd_BusinessError() throws Exception {
    // Given
    LiteFlowChainAddForm form = new LiteFlowChainAddForm();
    form.setChainName("測試流程");
    form.setChainCode("duplicate-chain");
    form.setChainType(1);
    form.setChainData("THEN(a)");

    when(chainService.add(any(LiteFlowChainAddForm.class), anyLong(), anyString()))
        .thenReturn(ResponseDTO.userErrorParam("流程編碼已存在"));

    // When & Then
    try (MockedStatic<SmartRequestUtil> mockedStatic = mockStatic(SmartRequestUtil.class)) {
      mockedStatic.when(SmartRequestUtil::getRequestUser).thenReturn(createTestRequestUser());

      mockMvc
          .perform(
              post("/liteflow/chain/add")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(form)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.ok").value(false))
          .andExpect(jsonPath("$.msg").value("流程編碼已存在"));
    }
  }
}
