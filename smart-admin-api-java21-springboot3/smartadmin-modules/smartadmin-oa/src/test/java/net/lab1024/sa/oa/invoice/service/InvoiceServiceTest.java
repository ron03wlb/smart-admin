package net.lab1024.sa.oa.invoice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.oa.enterprise.dao.EnterpriseDao;
import net.lab1024.sa.oa.enterprise.domain.vo.EnterpriseVO;
import net.lab1024.sa.oa.invoice.dao.InvoiceDao;
import net.lab1024.sa.oa.invoice.domain.InvoiceAddForm;
import net.lab1024.sa.oa.invoice.domain.InvoiceEntity;
import net.lab1024.sa.oa.invoice.domain.InvoiceQueryForm;
import net.lab1024.sa.oa.invoice.domain.InvoiceUpdateForm;
import net.lab1024.sa.oa.invoice.domain.InvoiceVO;
import net.lab1024.sa.oa.invoice.manager.InvoiceManager;
import net.lab1024.sa.support.datatracer.service.DataTracerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * InvoiceService 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>發票信息 CRUD 操作
 *   <li>企業存在性校驗
 *   <li>發票賬號重複校驗
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InvoiceService 單元測試")
class InvoiceServiceTest {

  @Mock private InvoiceDao invoiceDao;

  @Mock private EnterpriseDao enterpriseDao;

  @Mock private DataTracerService dataTracerService;

  @Mock private InvoiceManager invoiceManager;

  @InjectMocks private InvoiceService invoiceService;

  // ==================== queryByPage 測試 ====================

  @Nested
  @DisplayName("queryByPage 分頁查詢測試")
  class QueryByPageTest {

    @Test
    @DisplayName("正常情況：應該返回分頁結果")
    void shouldReturnPageResult() {
      // Given
      InvoiceQueryForm queryForm = new InvoiceQueryForm();
      queryForm.setPageNum(1L);
      queryForm.setPageSize(10L);

      InvoiceVO invoiceVO = createTestInvoiceVO(1L, "發票1");
      when(invoiceDao.queryPage(any(), any())).thenReturn(Collections.singletonList(invoiceVO));

      // When
      ResponseDTO<PageResult<InvoiceVO>> result = invoiceService.queryByPage(queryForm);

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData().getList()).hasSize(1);
    }
  }

  // ==================== queryList 測試 ====================

  @Nested
  @DisplayName("queryList 列表查詢測試")
  class QueryListTest {

    @Test
    @DisplayName("正常情況：應該返回發票列表")
    void shouldReturnInvoiceList() {
      // Given
      Long enterpriseId = 1L;
      InvoiceVO invoiceVO = createTestInvoiceVO(1L, "發票1");
      when(invoiceDao.queryPage(any(), any())).thenReturn(Collections.singletonList(invoiceVO));

      // When
      ResponseDTO<List<InvoiceVO>> result = invoiceService.queryList(enterpriseId);

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData()).hasSize(1);
    }
  }

  // ==================== getDetail 測試 ====================

  @Nested
  @DisplayName("getDetail 查詢詳情測試")
  class GetDetailTest {

    @Test
    @DisplayName("正常情況：應該返回發票詳情")
    void shouldReturnInvoiceDetail() {
      // Given
      Long invoiceId = 1L;
      InvoiceVO invoiceVO = createTestInvoiceVO(invoiceId, "發票1");
      when(invoiceDao.getDetail(invoiceId, Boolean.FALSE)).thenReturn(invoiceVO);

      // When
      ResponseDTO<InvoiceVO> result = invoiceService.getDetail(invoiceId);

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData().getInvoiceHeads()).isEqualTo("發票1");
    }

    @Test
    @DisplayName("異常情況：發票不存在時應返回錯誤")
    void shouldReturnErrorWhenNotFound() {
      // Given
      Long invoiceId = 999L;
      when(invoiceDao.getDetail(invoiceId, Boolean.FALSE)).thenReturn(null);

      // When
      ResponseDTO<InvoiceVO> result = invoiceService.getDetail(invoiceId);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("发票信息不存在");
    }
  }

  // ==================== createInvoice 測試 ====================

  @Nested
  @DisplayName("createInvoice 新增發票測試")
  class CreateInvoiceTest {

    @Test
    @DisplayName("正常情況：應該成功新增發票")
    void shouldCreateInvoiceSuccess() {
      // Given
      InvoiceAddForm createForm = createTestAddForm(1L, "1234567890");
      EnterpriseVO enterpriseVO = createTestEnterpriseVO(1L, "測試企業");

      when(enterpriseDao.getDetail(1L, Boolean.FALSE)).thenReturn(enterpriseVO);
      when(invoiceDao.queryByAccountNumber(1L, "1234567890", null, Boolean.FALSE)).thenReturn(null);

      // When
      ResponseDTO<String> result = invoiceService.createInvoice(createForm);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(invoiceManager).createInvoiceTransaction(createForm, 1L);
    }

    @Test
    @DisplayName("異常情況：企業不存在時應返回錯誤")
    void shouldReturnErrorWhenEnterpriseNotFound() {
      // Given
      InvoiceAddForm createForm = createTestAddForm(999L, "1234567890");
      when(enterpriseDao.getDetail(999L, Boolean.FALSE)).thenReturn(null);

      // When
      ResponseDTO<String> result = invoiceService.createInvoice(createForm);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("企业不存在");
      verify(invoiceManager, never()).createInvoiceTransaction(any(), any());
    }

    @Test
    @DisplayName("異常情況：發票賬號重複時應返回錯誤")
    void shouldReturnErrorWhenAccountNumberDuplicate() {
      // Given
      InvoiceAddForm createForm = createTestAddForm(1L, "1234567890");
      EnterpriseVO enterpriseVO = createTestEnterpriseVO(1L, "測試企業");
      InvoiceEntity existingInvoice = createTestInvoiceEntity(2L, "1234567890");

      when(enterpriseDao.getDetail(1L, Boolean.FALSE)).thenReturn(enterpriseVO);
      when(invoiceDao.queryByAccountNumber(1L, "1234567890", null, Boolean.FALSE))
          .thenReturn(existingInvoice);

      // When
      ResponseDTO<String> result = invoiceService.createInvoice(createForm);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("发票信息账号重复");
    }
  }

  // ==================== updateInvoice 測試 ====================

  @Nested
  @DisplayName("updateInvoice 更新發票測試")
  class UpdateInvoiceTest {

    @Test
    @DisplayName("正常情況：應該成功更新發票")
    void shouldUpdateInvoiceSuccess() {
      // Given
      InvoiceUpdateForm updateForm = createTestUpdateForm(1L, 1L, "9876543210");
      EnterpriseVO enterpriseVO = createTestEnterpriseVO(1L, "測試企業");
      InvoiceEntity invoiceEntity = createTestInvoiceEntity(1L, "1234567890");

      when(enterpriseDao.getDetail(1L, Boolean.FALSE)).thenReturn(enterpriseVO);
      when(invoiceDao.selectById(1L)).thenReturn(invoiceEntity);
      when(invoiceDao.queryByAccountNumber(1L, "9876543210", 1L, Boolean.FALSE)).thenReturn(null);

      // When
      ResponseDTO<String> result = invoiceService.updateInvoice(updateForm);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(invoiceManager).updateInvoiceTransaction(updateForm, invoiceEntity, 1L);
    }

    @Test
    @DisplayName("異常情況：發票不存在時應返回錯誤")
    void shouldReturnErrorWhenInvoiceNotFound() {
      // Given
      InvoiceUpdateForm updateForm = createTestUpdateForm(999L, 1L, "1234567890");
      EnterpriseVO enterpriseVO = createTestEnterpriseVO(1L, "測試企業");

      when(enterpriseDao.getDetail(1L, Boolean.FALSE)).thenReturn(enterpriseVO);
      when(invoiceDao.selectById(999L)).thenReturn(null);

      // When
      ResponseDTO<String> result = invoiceService.updateInvoice(updateForm);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("发票信息不存在");
    }

    @Test
    @DisplayName("異常情況：發票已刪除時應返回錯誤")
    void shouldReturnErrorWhenInvoiceDeleted() {
      // Given
      InvoiceUpdateForm updateForm = createTestUpdateForm(1L, 1L, "1234567890");
      EnterpriseVO enterpriseVO = createTestEnterpriseVO(1L, "測試企業");
      InvoiceEntity deletedInvoice = createTestInvoiceEntity(1L, "1234567890");
      deletedInvoice.setDeletedFlag(true);

      when(enterpriseDao.getDetail(1L, Boolean.FALSE)).thenReturn(enterpriseVO);
      when(invoiceDao.selectById(1L)).thenReturn(deletedInvoice);

      // When
      ResponseDTO<String> result = invoiceService.updateInvoice(updateForm);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("发票信息不存在");
    }
  }

  // ==================== deleteInvoice 測試 ====================

  @Nested
  @DisplayName("deleteInvoice 刪除發票測試")
  class DeleteInvoiceTest {

    @Test
    @DisplayName("正常情況：應該成功刪除發票")
    void shouldDeleteInvoiceSuccess() {
      // Given
      Long invoiceId = 1L;
      InvoiceEntity invoiceEntity = createTestInvoiceEntity(invoiceId, "1234567890");

      when(invoiceDao.selectById(invoiceId)).thenReturn(invoiceEntity);

      // When
      ResponseDTO<String> result = invoiceService.deleteInvoice(invoiceId);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(invoiceManager).deleteInvoiceTransaction(invoiceId, invoiceEntity);
    }

    @Test
    @DisplayName("異常情況：發票不存在時應返回錯誤")
    void shouldReturnErrorWhenInvoiceNotFound() {
      // Given
      Long invoiceId = 999L;
      when(invoiceDao.selectById(invoiceId)).thenReturn(null);

      // When
      ResponseDTO<String> result = invoiceService.deleteInvoice(invoiceId);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("发票信息不存在");
      verify(invoiceManager, never()).deleteInvoiceTransaction(any(), any());
    }
  }

  // ==================== Helper Methods ====================

  private InvoiceVO createTestInvoiceVO(Long id, String invoiceHeads) {
    InvoiceVO vo = new InvoiceVO();
    vo.setInvoiceId(id);
    vo.setInvoiceHeads(invoiceHeads);
    return vo;
  }

  private InvoiceEntity createTestInvoiceEntity(Long id, String accountNumber) {
    InvoiceEntity entity = new InvoiceEntity();
    entity.setInvoiceId(id);
    entity.setAccountNumber(accountNumber);
    entity.setDeletedFlag(false);
    return entity;
  }

  private InvoiceAddForm createTestAddForm(Long enterpriseId, String accountNumber) {
    InvoiceAddForm form = new InvoiceAddForm();
    form.setEnterpriseId(enterpriseId);
    form.setAccountNumber(accountNumber);
    form.setInvoiceHeads("測試發票");
    return form;
  }

  private InvoiceUpdateForm createTestUpdateForm(
      Long invoiceId, Long enterpriseId, String accountNumber) {
    InvoiceUpdateForm form = new InvoiceUpdateForm();
    form.setInvoiceId(invoiceId);
    form.setEnterpriseId(enterpriseId);
    form.setAccountNumber(accountNumber);
    return form;
  }

  private EnterpriseVO createTestEnterpriseVO(Long id, String name) {
    EnterpriseVO vo = new EnterpriseVO();
    vo.setEnterpriseId(id);
    vo.setEnterpriseName(name);
    return vo;
  }
}
