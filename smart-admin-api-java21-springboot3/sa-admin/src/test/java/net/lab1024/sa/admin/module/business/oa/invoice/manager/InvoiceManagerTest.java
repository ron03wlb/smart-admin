package net.lab1024.sa.admin.module.business.oa.invoice.manager;

import static org.mockito.Mockito.*;

import net.lab1024.sa.admin.BaseUnitTest;
import net.lab1024.sa.admin.module.business.oa.invoice.dao.InvoiceDao;
import net.lab1024.sa.admin.module.business.oa.invoice.domain.InvoiceAddForm;
import net.lab1024.sa.admin.module.business.oa.invoice.domain.InvoiceEntity;
import net.lab1024.sa.admin.module.business.oa.invoice.domain.InvoiceUpdateForm;
import net.lab1024.sa.base.module.support.datatracer.constant.DataTracerTypeEnum;
import net.lab1024.sa.base.module.support.datatracer.service.DataTracerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

/**
 * InvoiceManager Unit Tests
 *
 * <p>Test Coverage: 3 @Transactional methods (create, update, delete)
 *
 * <p>Focus Areas: 1. Transaction pattern with DataTracerService.addTrace 2. Insert + DataTracer
 * integration 3. Update with origin entity comparison 4. Soft delete + DataTracer logging
 *
 * @author Claude Code
 * @since 2026-01-30
 */
@DisplayName("InvoiceManager Unit Tests")
class InvoiceManagerTest extends BaseUnitTest {

  @Mock private InvoiceDao invoiceDao;

  @Mock private DataTracerService dataTracerService;

  private InvoiceManager invoiceManager;

  // Test constants
  private static final Long TEST_INVOICE_ID = 1001L;
  private static final Long TEST_ENTERPRISE_ID = 2001L;
  private static final String TEST_INVOICE_HEADS = "測試發票抬頭";
  private static final String TEST_TAXPAYER_ID_NUMBER = "91110000000000000A";

  @BeforeEach
  void setUp() {
    invoiceManager = new InvoiceManager(invoiceDao, dataTracerService);
  }

  // ==================== createInvoiceTransaction() Tests ====================

  @Nested
  @DisplayName("createInvoiceTransaction() Tests - Insert + DataTracer Pattern")
  class CreateInvoiceTransactionTests {

    @Test
    @DisplayName("Should insert invoice and add trace")
    void createInvoice_ValidForm_InsertsAndTraces() {
      // Given
      InvoiceAddForm addForm = new InvoiceAddForm();
      addForm.setInvoiceHeads(TEST_INVOICE_HEADS);
      addForm.setTaxpayerIdentificationNumber(TEST_TAXPAYER_ID_NUMBER);

      when(invoiceDao.insert(any(InvoiceEntity.class)))
          .thenAnswer(
              invocation -> {
                InvoiceEntity entity = invocation.getArgument(0);
                entity.setInvoiceId(TEST_INVOICE_ID);
                return 1;
              });
      doNothing()
          .when(dataTracerService)
          .addTrace(eq(TEST_ENTERPRISE_ID), eq(DataTracerTypeEnum.OA_ENTERPRISE), anyString());

      // When
      invoiceManager.createInvoiceTransaction(addForm, TEST_ENTERPRISE_ID);

      // Then
      verify(invoiceDao, times(1)).insert(any(InvoiceEntity.class));
      verify(dataTracerService, times(1))
          .addTrace(eq(TEST_ENTERPRISE_ID), eq(DataTracerTypeEnum.OA_ENTERPRISE), anyString());
    }

    @Test
    @DisplayName("Should insert before data tracer (transaction order)")
    void createInvoice_TransactionOrder_InsertBeforeTrace() {
      // Given
      InvoiceAddForm addForm = new InvoiceAddForm();
      addForm.setInvoiceHeads(TEST_INVOICE_HEADS);

      when(invoiceDao.insert(any(InvoiceEntity.class))).thenReturn(1);
      doNothing()
          .when(dataTracerService)
          .addTrace(anyLong(), any(DataTracerTypeEnum.class), anyString());

      // When
      invoiceManager.createInvoiceTransaction(addForm, TEST_ENTERPRISE_ID);

      // Then - verify insert called before dataTracer
      var inOrder = inOrder(invoiceDao, dataTracerService);
      inOrder.verify(invoiceDao).insert(any(InvoiceEntity.class));
      inOrder
          .verify(dataTracerService)
          .addTrace(anyLong(), any(DataTracerTypeEnum.class), anyString());
    }

    @Test
    @DisplayName("Should use correct enterprise ID in trace")
    void createInvoice_EnterpriseId_UsedInTrace() {
      // Given
      Long specificEnterpriseId = 9999L;
      InvoiceAddForm addForm = new InvoiceAddForm();
      addForm.setInvoiceHeads(TEST_INVOICE_HEADS);

      when(invoiceDao.insert(any(InvoiceEntity.class))).thenReturn(1);
      doNothing()
          .when(dataTracerService)
          .addTrace(anyLong(), any(DataTracerTypeEnum.class), anyString());

      // When
      invoiceManager.createInvoiceTransaction(addForm, specificEnterpriseId);

      // Then
      verify(dataTracerService, times(1))
          .addTrace(eq(specificEnterpriseId), eq(DataTracerTypeEnum.OA_ENTERPRISE), anyString());
    }
  }

  // ==================== updateInvoiceTransaction() Tests ====================

  @Nested
  @DisplayName("updateInvoiceTransaction() Tests - Update + DataTracer with Origin Entity")
  class UpdateInvoiceTransactionTests {

    @Test
    @DisplayName("Should update invoice and add trace with origin entity")
    void updateInvoice_ValidForm_UpdatesAndTraces() {
      // Given
      InvoiceUpdateForm updateForm = new InvoiceUpdateForm();
      updateForm.setInvoiceId(TEST_INVOICE_ID);
      updateForm.setInvoiceHeads("Updated Invoice Heads");

      InvoiceEntity invoiceDetail = new InvoiceEntity();
      invoiceDetail.setInvoiceId(TEST_INVOICE_ID);
      invoiceDetail.setInvoiceHeads(TEST_INVOICE_HEADS);

      when(invoiceDao.updateById(any(InvoiceEntity.class))).thenReturn(1);
      doNothing()
          .when(dataTracerService)
          .addTrace(eq(TEST_ENTERPRISE_ID), eq(DataTracerTypeEnum.OA_ENTERPRISE), anyString());

      // When
      invoiceManager.updateInvoiceTransaction(updateForm, invoiceDetail, TEST_ENTERPRISE_ID);

      // Then
      verify(invoiceDao, times(1)).updateById(any(InvoiceEntity.class));
      verify(dataTracerService, times(1))
          .addTrace(eq(TEST_ENTERPRISE_ID), eq(DataTracerTypeEnum.OA_ENTERPRISE), anyString());
    }

    @Test
    @DisplayName("Should update before data tracer (transaction order)")
    void updateInvoice_TransactionOrder_UpdateBeforeTrace() {
      // Given
      InvoiceUpdateForm updateForm = new InvoiceUpdateForm();
      updateForm.setInvoiceId(TEST_INVOICE_ID);

      InvoiceEntity invoiceDetail = new InvoiceEntity();
      invoiceDetail.setInvoiceId(TEST_INVOICE_ID);

      when(invoiceDao.updateById(any(InvoiceEntity.class))).thenReturn(1);
      doNothing()
          .when(dataTracerService)
          .addTrace(anyLong(), any(DataTracerTypeEnum.class), anyString());

      // When
      invoiceManager.updateInvoiceTransaction(updateForm, invoiceDetail, TEST_ENTERPRISE_ID);

      // Then - verify update called before dataTracer
      var inOrder = inOrder(invoiceDao, dataTracerService);
      inOrder.verify(invoiceDao).updateById(any(InvoiceEntity.class));
      inOrder
          .verify(dataTracerService)
          .addTrace(anyLong(), any(DataTracerTypeEnum.class), anyString());
    }

    @Test
    @DisplayName("Should use correct enterprise ID in trace")
    void updateInvoice_EnterpriseId_UsedInTrace() {
      // Given
      Long specificEnterpriseId = 8888L;
      InvoiceUpdateForm updateForm = new InvoiceUpdateForm();
      updateForm.setInvoiceId(TEST_INVOICE_ID);

      InvoiceEntity invoiceDetail = new InvoiceEntity();
      invoiceDetail.setInvoiceId(TEST_INVOICE_ID);

      when(invoiceDao.updateById(any(InvoiceEntity.class))).thenReturn(1);
      doNothing()
          .when(dataTracerService)
          .addTrace(anyLong(), any(DataTracerTypeEnum.class), anyString());

      // When
      invoiceManager.updateInvoiceTransaction(updateForm, invoiceDetail, specificEnterpriseId);

      // Then
      verify(dataTracerService, times(1))
          .addTrace(eq(specificEnterpriseId), eq(DataTracerTypeEnum.OA_ENTERPRISE), anyString());
    }
  }

  // ==================== deleteInvoiceTransaction() Tests ====================

  @Nested
  @DisplayName("deleteInvoiceTransaction() Tests - Soft Delete + DataTracer")
  class DeleteInvoiceTransactionTests {

    @Test
    @DisplayName("Should soft delete invoice and add trace")
    void deleteInvoice_ValidId_SoftDeletesAndTraces() {
      // Given
      InvoiceEntity invoiceDetail = new InvoiceEntity();
      invoiceDetail.setInvoiceId(TEST_INVOICE_ID);
      invoiceDetail.setEnterpriseId(TEST_ENTERPRISE_ID);
      invoiceDetail.setInvoiceHeads(TEST_INVOICE_HEADS);

      doNothing().when(invoiceDao).deleteInvoice(eq(TEST_INVOICE_ID), eq(Boolean.TRUE));
      doNothing()
          .when(dataTracerService)
          .addTrace(eq(TEST_ENTERPRISE_ID), eq(DataTracerTypeEnum.OA_ENTERPRISE), anyString());

      // When
      invoiceManager.deleteInvoiceTransaction(TEST_INVOICE_ID, invoiceDetail);

      // Then
      verify(invoiceDao, times(1)).deleteInvoice(eq(TEST_INVOICE_ID), eq(Boolean.TRUE));
      verify(dataTracerService, times(1))
          .addTrace(eq(TEST_ENTERPRISE_ID), eq(DataTracerTypeEnum.OA_ENTERPRISE), anyString());
    }

    @Test
    @DisplayName("Should delete before data tracer (transaction order)")
    void deleteInvoice_TransactionOrder_DeleteBeforeTrace() {
      // Given
      InvoiceEntity invoiceDetail = new InvoiceEntity();
      invoiceDetail.setInvoiceId(TEST_INVOICE_ID);
      invoiceDetail.setEnterpriseId(TEST_ENTERPRISE_ID);

      doNothing().when(invoiceDao).deleteInvoice(anyLong(), eq(Boolean.TRUE));
      doNothing()
          .when(dataTracerService)
          .addTrace(anyLong(), any(DataTracerTypeEnum.class), anyString());

      // When
      invoiceManager.deleteInvoiceTransaction(TEST_INVOICE_ID, invoiceDetail);

      // Then - verify delete called before dataTracer
      var inOrder = inOrder(invoiceDao, dataTracerService);
      inOrder.verify(invoiceDao).deleteInvoice(anyLong(), eq(Boolean.TRUE));
      inOrder
          .verify(dataTracerService)
          .addTrace(anyLong(), any(DataTracerTypeEnum.class), anyString());
    }

    @Test
    @DisplayName("Should use invoiceDetail's enterprise ID in trace")
    void deleteInvoice_InvoiceDetail_UsesEnterpriseIdFromDetail() {
      // Given
      Long specificEnterpriseId = 7777L;
      InvoiceEntity invoiceDetail = new InvoiceEntity();
      invoiceDetail.setInvoiceId(TEST_INVOICE_ID);
      invoiceDetail.setEnterpriseId(specificEnterpriseId);

      doNothing().when(invoiceDao).deleteInvoice(anyLong(), eq(Boolean.TRUE));
      doNothing()
          .when(dataTracerService)
          .addTrace(anyLong(), any(DataTracerTypeEnum.class), anyString());

      // When
      invoiceManager.deleteInvoiceTransaction(TEST_INVOICE_ID, invoiceDetail);

      // Then
      verify(dataTracerService, times(1))
          .addTrace(eq(specificEnterpriseId), eq(DataTracerTypeEnum.OA_ENTERPRISE), anyString());
    }

    @Test
    @DisplayName("Should handle different invoice IDs correctly")
    void deleteInvoice_DifferentInvoiceIds_HandlesCorrectly() {
      // Given
      Long specificInvoiceId = 5555L;
      InvoiceEntity invoiceDetail = new InvoiceEntity();
      invoiceDetail.setInvoiceId(specificInvoiceId);
      invoiceDetail.setEnterpriseId(TEST_ENTERPRISE_ID);

      doNothing().when(invoiceDao).deleteInvoice(anyLong(), eq(Boolean.TRUE));
      doNothing()
          .when(dataTracerService)
          .addTrace(anyLong(), any(DataTracerTypeEnum.class), anyString());

      // When
      invoiceManager.deleteInvoiceTransaction(specificInvoiceId, invoiceDetail);

      // Then
      verify(invoiceDao, times(1)).deleteInvoice(eq(specificInvoiceId), eq(Boolean.TRUE));
      verify(invoiceDao, never()).deleteInvoice(eq(TEST_INVOICE_ID), eq(Boolean.TRUE));
    }
  }
}
