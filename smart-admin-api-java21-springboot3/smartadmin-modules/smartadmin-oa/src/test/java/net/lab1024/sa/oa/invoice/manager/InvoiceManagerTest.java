package net.lab1024.sa.oa.invoice.manager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import net.lab1024.sa.oa.invoice.dao.InvoiceDao;
import net.lab1024.sa.oa.invoice.domain.InvoiceAddForm;
import net.lab1024.sa.oa.invoice.domain.InvoiceEntity;
import net.lab1024.sa.oa.invoice.domain.InvoiceUpdateForm;
import net.lab1024.sa.support.datatracer.constant.DataTracerTypeEnum;
import net.lab1024.sa.support.datatracer.service.DataTracerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * InvoiceManager 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>新增發票事務
 *   <li>更新發票事務
 *   <li>刪除發票事務
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InvoiceManager 單元測試")
class InvoiceManagerTest {

  @Mock private InvoiceDao invoiceDao;

  @Mock private DataTracerService dataTracerService;

  @InjectMocks private InvoiceManager invoiceManager;

  // ==================== createInvoiceTransaction 測試 ====================

  @Nested
  @DisplayName("createInvoiceTransaction 新增發票事務測試")
  class CreateInvoiceTransactionTest {

    @Test
    @DisplayName("正常情況：應該插入數據並記錄追蹤")
    void shouldInsertAndTrace() {
      // Given
      InvoiceAddForm createForm = new InvoiceAddForm();
      createForm.setInvoiceHeads("測試發票");
      createForm.setAccountNumber("1234567890");
      Long enterpriseId = 1L;

      when(dataTracerService.getChangeContent(any(InvoiceEntity.class))).thenReturn("content");

      // When
      invoiceManager.createInvoiceTransaction(createForm, enterpriseId);

      // Then
      verify(invoiceDao).insert(any(InvoiceEntity.class));
      verify(dataTracerService)
          .addTrace(eq(enterpriseId), eq(DataTracerTypeEnum.OA_ENTERPRISE), any(String.class));
    }
  }

  // ==================== updateInvoiceTransaction 測試 ====================

  @Nested
  @DisplayName("updateInvoiceTransaction 更新發票事務測試")
  class UpdateInvoiceTransactionTest {

    @Test
    @DisplayName("正常情況：應該更新數據並記錄追蹤")
    void shouldUpdateAndTrace() {
      // Given
      InvoiceUpdateForm updateForm = new InvoiceUpdateForm();
      updateForm.setInvoiceId(1L);
      updateForm.setInvoiceHeads("更新後發票");

      InvoiceEntity invoiceDetail = new InvoiceEntity();
      invoiceDetail.setInvoiceId(1L);
      invoiceDetail.setInvoiceHeads("原發票");

      Long enterpriseId = 1L;

      when(dataTracerService.getChangeContent(any(InvoiceEntity.class), any(InvoiceEntity.class)))
          .thenReturn("content");

      // When
      invoiceManager.updateInvoiceTransaction(updateForm, invoiceDetail, enterpriseId);

      // Then
      verify(invoiceDao).updateById(any(InvoiceEntity.class));
      verify(dataTracerService)
          .addTrace(eq(enterpriseId), eq(DataTracerTypeEnum.OA_ENTERPRISE), any(String.class));
    }
  }

  // ==================== deleteInvoiceTransaction 測試 ====================

  @Nested
  @DisplayName("deleteInvoiceTransaction 刪除發票事務測試")
  class DeleteInvoiceTransactionTest {

    @Test
    @DisplayName("正常情況：應該刪除數據並記錄追蹤")
    void shouldDeleteAndTrace() {
      // Given
      Long invoiceId = 1L;
      InvoiceEntity invoiceDetail = new InvoiceEntity();
      invoiceDetail.setInvoiceId(invoiceId);
      invoiceDetail.setEnterpriseId(1L);

      when(dataTracerService.getChangeContent(any(InvoiceEntity.class))).thenReturn("content");

      // When
      invoiceManager.deleteInvoiceTransaction(invoiceId, invoiceDetail);

      // Then
      verify(invoiceDao).deleteInvoice(invoiceId, Boolean.TRUE);
      verify(dataTracerService)
          .addTrace(eq(1L), eq(DataTracerTypeEnum.OA_ENTERPRISE), any(String.class));
    }
  }
}
