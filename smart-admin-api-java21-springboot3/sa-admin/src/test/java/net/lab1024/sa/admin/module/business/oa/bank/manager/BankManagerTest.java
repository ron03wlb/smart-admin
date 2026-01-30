package net.lab1024.sa.admin.module.business.oa.bank.manager;

import static org.mockito.Mockito.*;

import net.lab1024.sa.admin.BaseUnitTest;
import net.lab1024.sa.admin.module.business.oa.bank.dao.BankDao;
import net.lab1024.sa.admin.module.business.oa.bank.domain.BankCreateForm;
import net.lab1024.sa.admin.module.business.oa.bank.domain.BankEntity;
import net.lab1024.sa.admin.module.business.oa.bank.domain.BankUpdateForm;
import net.lab1024.sa.base.module.support.datatracer.constant.DataTracerTypeEnum;
import net.lab1024.sa.base.module.support.datatracer.service.DataTracerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

/**
 * BankManager Unit Tests
 *
 * <p>Test Coverage: 3 @Transactional methods (create, update, delete)
 *
 * <p>Focus Areas: 1. Transaction pattern with DataTracerService.addTrace 2. Insert + DataTracer
 * integration 3. Update with origin entity comparison 4. Soft delete + DataTracer logging
 *
 * @author Claude Code
 * @since 2026-01-30
 */
@DisplayName("BankManager Unit Tests")
class BankManagerTest extends BaseUnitTest {

  @Mock private BankDao bankDao;

  @Mock private DataTracerService dataTracerService;

  private BankManager bankManager;

  // Test constants
  private static final Long TEST_BANK_ID = 1001L;
  private static final Long TEST_ENTERPRISE_ID = 2001L;
  private static final String TEST_BANK_NAME = "測試銀行";
  private static final String TEST_ACCOUNT_NUMBER = "6222000000001234567";

  @BeforeEach
  void setUp() {
    bankManager = new BankManager(bankDao, dataTracerService);
  }

  // ==================== createBankTransaction() Tests ====================

  @Nested
  @DisplayName("createBankTransaction() Tests - Insert + DataTracer Pattern")
  class CreateBankTransactionTests {

    @Test
    @DisplayName("Should insert bank and add trace")
    void createBank_ValidForm_InsertsAndTraces() {
      // Given
      BankCreateForm createForm = new BankCreateForm();
      createForm.setAccountName(TEST_BANK_NAME);
      createForm.setAccountNumber(TEST_ACCOUNT_NUMBER);

      when(bankDao.insert(any(BankEntity.class)))
          .thenAnswer(
              invocation -> {
                BankEntity entity = invocation.getArgument(0);
                entity.setBankId(TEST_BANK_ID);
                return 1;
              });
      doNothing()
          .when(dataTracerService)
          .addTrace(eq(TEST_ENTERPRISE_ID), eq(DataTracerTypeEnum.OA_ENTERPRISE), anyString());

      // When
      bankManager.createBankTransaction(createForm, TEST_ENTERPRISE_ID);

      // Then
      verify(bankDao, times(1)).insert(any(BankEntity.class));
      verify(dataTracerService, times(1))
          .addTrace(eq(TEST_ENTERPRISE_ID), eq(DataTracerTypeEnum.OA_ENTERPRISE), anyString());
    }

    @Test
    @DisplayName("Should insert before data tracer (transaction order)")
    void createBank_TransactionOrder_InsertBeforeTrace() {
      // Given
      BankCreateForm createForm = new BankCreateForm();
      createForm.setAccountName(TEST_BANK_NAME);

      when(bankDao.insert(any(BankEntity.class))).thenReturn(1);
      doNothing()
          .when(dataTracerService)
          .addTrace(anyLong(), any(DataTracerTypeEnum.class), anyString());

      // When
      bankManager.createBankTransaction(createForm, TEST_ENTERPRISE_ID);

      // Then - verify insert called before dataTracer
      var inOrder = inOrder(bankDao, dataTracerService);
      inOrder.verify(bankDao).insert(any(BankEntity.class));
      inOrder
          .verify(dataTracerService)
          .addTrace(anyLong(), any(DataTracerTypeEnum.class), anyString());
    }

    @Test
    @DisplayName("Should use correct enterprise ID in trace")
    void createBank_EnterpriseId_UsedInTrace() {
      // Given
      Long specificEnterpriseId = 9999L;
      BankCreateForm createForm = new BankCreateForm();
      createForm.setAccountName(TEST_BANK_NAME);

      when(bankDao.insert(any(BankEntity.class))).thenReturn(1);
      doNothing()
          .when(dataTracerService)
          .addTrace(anyLong(), any(DataTracerTypeEnum.class), anyString());

      // When
      bankManager.createBankTransaction(createForm, specificEnterpriseId);

      // Then
      verify(dataTracerService, times(1))
          .addTrace(eq(specificEnterpriseId), eq(DataTracerTypeEnum.OA_ENTERPRISE), anyString());
    }
  }

  // ==================== updateBankTransaction() Tests ====================

  @Nested
  @DisplayName("updateBankTransaction() Tests - Update + DataTracer with Origin Entity")
  class UpdateBankTransactionTests {

    @Test
    @DisplayName("Should update bank and add trace with origin entity")
    void updateBank_ValidForm_UpdatesAndTraces() {
      // Given
      BankUpdateForm updateForm = new BankUpdateForm();
      updateForm.setBankId(TEST_BANK_ID);
      updateForm.setAccountName("Updated Bank Name");

      BankEntity originEntity = new BankEntity();
      originEntity.setBankId(TEST_BANK_ID);
      originEntity.setAccountName(TEST_BANK_NAME);

      when(bankDao.updateById(any(BankEntity.class))).thenReturn(1);
      doNothing()
          .when(dataTracerService)
          .addTrace(eq(TEST_ENTERPRISE_ID), eq(DataTracerTypeEnum.OA_ENTERPRISE), anyString());

      // When
      bankManager.updateBankTransaction(updateForm, originEntity, TEST_ENTERPRISE_ID);

      // Then
      verify(bankDao, times(1)).updateById(any(BankEntity.class));
      verify(dataTracerService, times(1))
          .addTrace(eq(TEST_ENTERPRISE_ID), eq(DataTracerTypeEnum.OA_ENTERPRISE), anyString());
    }

    @Test
    @DisplayName("Should update before data tracer (transaction order)")
    void updateBank_TransactionOrder_UpdateBeforeTrace() {
      // Given
      BankUpdateForm updateForm = new BankUpdateForm();
      updateForm.setBankId(TEST_BANK_ID);

      BankEntity originEntity = new BankEntity();
      originEntity.setBankId(TEST_BANK_ID);

      when(bankDao.updateById(any(BankEntity.class))).thenReturn(1);
      doNothing()
          .when(dataTracerService)
          .addTrace(anyLong(), any(DataTracerTypeEnum.class), anyString());

      // When
      bankManager.updateBankTransaction(updateForm, originEntity, TEST_ENTERPRISE_ID);

      // Then - verify update called before dataTracer
      var inOrder = inOrder(bankDao, dataTracerService);
      inOrder.verify(bankDao).updateById(any(BankEntity.class));
      inOrder
          .verify(dataTracerService)
          .addTrace(anyLong(), any(DataTracerTypeEnum.class), anyString());
    }

    @Test
    @DisplayName("Should use correct enterprise ID in trace")
    void updateBank_EnterpriseId_UsedInTrace() {
      // Given
      Long specificEnterpriseId = 8888L;
      BankUpdateForm updateForm = new BankUpdateForm();
      updateForm.setBankId(TEST_BANK_ID);

      BankEntity originEntity = new BankEntity();
      originEntity.setBankId(TEST_BANK_ID);

      when(bankDao.updateById(any(BankEntity.class))).thenReturn(1);
      doNothing()
          .when(dataTracerService)
          .addTrace(anyLong(), any(DataTracerTypeEnum.class), anyString());

      // When
      bankManager.updateBankTransaction(updateForm, originEntity, specificEnterpriseId);

      // Then
      verify(dataTracerService, times(1))
          .addTrace(eq(specificEnterpriseId), eq(DataTracerTypeEnum.OA_ENTERPRISE), anyString());
    }
  }

  // ==================== deleteBankTransaction() Tests ====================

  @Nested
  @DisplayName("deleteBankTransaction() Tests - Soft Delete + DataTracer")
  class DeleteBankTransactionTests {

    @Test
    @DisplayName("Should soft delete bank and add trace")
    void deleteBank_ValidId_SoftDeletesAndTraces() {
      // Given
      BankEntity bankDetail = new BankEntity();
      bankDetail.setBankId(TEST_BANK_ID);
      bankDetail.setEnterpriseId(TEST_ENTERPRISE_ID);
      bankDetail.setAccountName(TEST_BANK_NAME);

      doNothing().when(bankDao).deleteBank(eq(TEST_BANK_ID), eq(Boolean.TRUE));
      doNothing()
          .when(dataTracerService)
          .addTrace(eq(TEST_ENTERPRISE_ID), eq(DataTracerTypeEnum.OA_ENTERPRISE), anyString());

      // When
      bankManager.deleteBankTransaction(TEST_BANK_ID, bankDetail);

      // Then
      verify(bankDao, times(1)).deleteBank(eq(TEST_BANK_ID), eq(Boolean.TRUE));
      verify(dataTracerService, times(1))
          .addTrace(eq(TEST_ENTERPRISE_ID), eq(DataTracerTypeEnum.OA_ENTERPRISE), anyString());
    }

    @Test
    @DisplayName("Should delete before data tracer (transaction order)")
    void deleteBank_TransactionOrder_DeleteBeforeTrace() {
      // Given
      BankEntity bankDetail = new BankEntity();
      bankDetail.setBankId(TEST_BANK_ID);
      bankDetail.setEnterpriseId(TEST_ENTERPRISE_ID);

      doNothing().when(bankDao).deleteBank(anyLong(), eq(Boolean.TRUE));
      doNothing()
          .when(dataTracerService)
          .addTrace(anyLong(), any(DataTracerTypeEnum.class), anyString());

      // When
      bankManager.deleteBankTransaction(TEST_BANK_ID, bankDetail);

      // Then - verify delete called before dataTracer
      var inOrder = inOrder(bankDao, dataTracerService);
      inOrder.verify(bankDao).deleteBank(anyLong(), eq(Boolean.TRUE));
      inOrder
          .verify(dataTracerService)
          .addTrace(anyLong(), any(DataTracerTypeEnum.class), anyString());
    }

    @Test
    @DisplayName("Should use bankDetail's enterprise ID in trace")
    void deleteBank_BankDetail_UsesEnterpriseIdFromDetail() {
      // Given
      Long specificEnterpriseId = 7777L;
      BankEntity bankDetail = new BankEntity();
      bankDetail.setBankId(TEST_BANK_ID);
      bankDetail.setEnterpriseId(specificEnterpriseId);

      doNothing().when(bankDao).deleteBank(anyLong(), eq(Boolean.TRUE));
      doNothing()
          .when(dataTracerService)
          .addTrace(anyLong(), any(DataTracerTypeEnum.class), anyString());

      // When
      bankManager.deleteBankTransaction(TEST_BANK_ID, bankDetail);

      // Then
      verify(dataTracerService, times(1))
          .addTrace(eq(specificEnterpriseId), eq(DataTracerTypeEnum.OA_ENTERPRISE), anyString());
    }

    @Test
    @DisplayName("Should handle different bank IDs correctly")
    void deleteBank_DifferentBankIds_HandlesCorrectly() {
      // Given
      Long specificBankId = 5555L;
      BankEntity bankDetail = new BankEntity();
      bankDetail.setBankId(specificBankId);
      bankDetail.setEnterpriseId(TEST_ENTERPRISE_ID);

      doNothing().when(bankDao).deleteBank(anyLong(), eq(Boolean.TRUE));
      doNothing()
          .when(dataTracerService)
          .addTrace(anyLong(), any(DataTracerTypeEnum.class), anyString());

      // When
      bankManager.deleteBankTransaction(specificBankId, bankDetail);

      // Then
      verify(bankDao, times(1)).deleteBank(eq(specificBankId), eq(Boolean.TRUE));
      verify(bankDao, never()).deleteBank(eq(TEST_BANK_ID), eq(Boolean.TRUE));
    }
  }
}
