package net.lab1024.sa.admin.module.business.oa.enterprise.manager;

import static org.mockito.Mockito.*;

import net.lab1024.sa.admin.BaseUnitTest;
import net.lab1024.sa.admin.module.business.oa.enterprise.dao.EnterpriseDao;
import net.lab1024.sa.admin.module.business.oa.enterprise.domain.entity.EnterpriseEntity;
import net.lab1024.sa.admin.module.business.oa.enterprise.domain.form.EnterpriseCreateForm;
import net.lab1024.sa.admin.module.business.oa.enterprise.domain.form.EnterpriseUpdateForm;
import net.lab1024.sa.base.module.support.datatracer.constant.DataTracerTypeEnum;
import net.lab1024.sa.base.module.support.datatracer.domain.form.DataTracerForm;
import net.lab1024.sa.base.module.support.datatracer.service.DataTracerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

/**
 * EnterpriseManager Unit Tests
 *
 * <p>Test Coverage: 3 @Transactional methods (create, update, delete)
 *
 * <p>Focus Areas: 1. Transaction pattern with DataTracerService (insert/addTrace/delete) 2. Insert
 * + DataTracer.insert pattern 3. Update with DataTracerForm (diffOld/diffNew) 4. Delete +
 * DataTracer.delete pattern
 *
 * @author Claude Code
 * @since 2026-01-30
 */
@DisplayName("EnterpriseManager Unit Tests")
class EnterpriseManagerTest extends BaseUnitTest {

  @Mock private EnterpriseDao enterpriseDao;

  @Mock private DataTracerService dataTracerService;

  private EnterpriseManager enterpriseManager;

  // Test constants
  private static final Long TEST_ENTERPRISE_ID = 1001L;
  private static final String TEST_ENTERPRISE_NAME = "測試企業";
  private static final String TEST_UNIFIED_SOCIAL_CREDIT_CODE = "91110000000000000A";

  @BeforeEach
  void setUp() {
    enterpriseManager = new EnterpriseManager(enterpriseDao, dataTracerService);
  }

  // ==================== createEnterpriseTransaction() Tests ====================

  @Nested
  @DisplayName("createEnterpriseTransaction() Tests - Insert + DataTracer.insert Pattern")
  class CreateEnterpriseTransactionTests {

    @Test
    @DisplayName("Should insert enterprise and record data tracer")
    void createEnterprise_ValidForm_InsertsAndTraces() {
      // Given
      EnterpriseCreateForm createForm = new EnterpriseCreateForm();
      createForm.setEnterpriseName(TEST_ENTERPRISE_NAME);
      createForm.setUnifiedSocialCreditCode(TEST_UNIFIED_SOCIAL_CREDIT_CODE);

      when(enterpriseDao.insert(any(EnterpriseEntity.class)))
          .thenAnswer(
              invocation -> {
                EnterpriseEntity entity = invocation.getArgument(0);
                entity.setEnterpriseId(TEST_ENTERPRISE_ID);
                return 1;
              });
      doNothing()
          .when(dataTracerService)
          .insert(TEST_ENTERPRISE_ID, DataTracerTypeEnum.OA_ENTERPRISE);

      // When
      enterpriseManager.createEnterpriseTransaction(createForm);

      // Then
      verify(enterpriseDao, times(1)).insert(any(EnterpriseEntity.class));
      verify(dataTracerService, times(1))
          .insert(TEST_ENTERPRISE_ID, DataTracerTypeEnum.OA_ENTERPRISE);
    }

    @Test
    @DisplayName("Should insert before data tracer (transaction order)")
    void createEnterprise_TransactionOrder_InsertBeforeTrace() {
      // Given
      EnterpriseCreateForm createForm = new EnterpriseCreateForm();
      createForm.setEnterpriseName(TEST_ENTERPRISE_NAME);

      when(enterpriseDao.insert(any(EnterpriseEntity.class)))
          .thenAnswer(
              invocation -> {
                EnterpriseEntity entity = invocation.getArgument(0);
                entity.setEnterpriseId(TEST_ENTERPRISE_ID);
                return 1;
              });
      doNothing().when(dataTracerService).insert(anyLong(), any(DataTracerTypeEnum.class));

      // When
      enterpriseManager.createEnterpriseTransaction(createForm);

      // Then - verify insert called before dataTracer
      var inOrder = inOrder(enterpriseDao, dataTracerService);
      inOrder.verify(enterpriseDao).insert(any(EnterpriseEntity.class));
      inOrder.verify(dataTracerService).insert(anyLong(), any(DataTracerTypeEnum.class));
    }

    @Test
    @DisplayName("Should use generated enterprise ID in data tracer")
    void createEnterprise_GeneratedId_UsedInTracer() {
      // Given
      Long generatedId = 9999L;
      EnterpriseCreateForm createForm = new EnterpriseCreateForm();
      createForm.setEnterpriseName(TEST_ENTERPRISE_NAME);

      when(enterpriseDao.insert(any(EnterpriseEntity.class)))
          .thenAnswer(
              invocation -> {
                EnterpriseEntity entity = invocation.getArgument(0);
                entity.setEnterpriseId(generatedId);
                return 1;
              });
      doNothing().when(dataTracerService).insert(anyLong(), any(DataTracerTypeEnum.class));

      // When
      enterpriseManager.createEnterpriseTransaction(createForm);

      // Then
      verify(dataTracerService, times(1))
          .insert(eq(generatedId), eq(DataTracerTypeEnum.OA_ENTERPRISE));
    }
  }

  // ==================== updateEnterpriseTransaction() Tests ====================

  @Nested
  @DisplayName(
      "updateEnterpriseTransaction() Tests - Update + DataTracerForm (diffOld/diffNew) Pattern")
  class UpdateEnterpriseTransactionTests {

    @Test
    @DisplayName("Should update enterprise and add trace with DataTracerForm")
    void updateEnterprise_ValidForm_UpdatesAndTraces() {
      // Given
      EnterpriseUpdateForm updateForm = new EnterpriseUpdateForm();
      updateForm.setEnterpriseId(TEST_ENTERPRISE_ID);
      updateForm.setEnterpriseName("Updated Enterprise Name");

      EnterpriseEntity enterpriseDetail = new EnterpriseEntity();
      enterpriseDetail.setEnterpriseId(TEST_ENTERPRISE_ID);
      enterpriseDetail.setEnterpriseName(TEST_ENTERPRISE_NAME);

      when(enterpriseDao.updateById(any(EnterpriseEntity.class))).thenReturn(1);
      when(dataTracerService.getChangeContent(any())).thenReturn("mock content");
      doNothing().when(dataTracerService).addTrace(any(DataTracerForm.class));

      // When
      enterpriseManager.updateEnterpriseTransaction(updateForm, enterpriseDetail);

      // Then
      verify(enterpriseDao, times(1)).updateById(any(EnterpriseEntity.class));
      verify(dataTracerService, times(1)).addTrace(any(DataTracerForm.class));
    }

    @Test
    @DisplayName("Should update before data tracer (transaction order)")
    void updateEnterprise_TransactionOrder_UpdateBeforeTrace() {
      // Given
      EnterpriseUpdateForm updateForm = new EnterpriseUpdateForm();
      updateForm.setEnterpriseId(TEST_ENTERPRISE_ID);

      EnterpriseEntity enterpriseDetail = new EnterpriseEntity();
      enterpriseDetail.setEnterpriseId(TEST_ENTERPRISE_ID);

      when(enterpriseDao.updateById(any(EnterpriseEntity.class))).thenReturn(1);
      when(dataTracerService.getChangeContent(any())).thenReturn("mock content");
      doNothing().when(dataTracerService).addTrace(any(DataTracerForm.class));

      // When
      enterpriseManager.updateEnterpriseTransaction(updateForm, enterpriseDetail);

      // Then - verify update called before dataTracer
      var inOrder = inOrder(enterpriseDao, dataTracerService);
      inOrder.verify(enterpriseDao).updateById(any(EnterpriseEntity.class));
      inOrder.verify(dataTracerService).addTrace(any(DataTracerForm.class));
    }

    @Test
    @DisplayName("Should call getChangeContent twice (old and new entity)")
    void updateEnterprise_ChangeContent_CalledForOldAndNew() {
      // Given
      EnterpriseUpdateForm updateForm = new EnterpriseUpdateForm();
      updateForm.setEnterpriseId(TEST_ENTERPRISE_ID);

      EnterpriseEntity enterpriseDetail = new EnterpriseEntity();
      enterpriseDetail.setEnterpriseId(TEST_ENTERPRISE_ID);
      enterpriseDetail.setEnterpriseName(TEST_ENTERPRISE_NAME);

      when(enterpriseDao.updateById(any(EnterpriseEntity.class))).thenReturn(1);
      when(dataTracerService.getChangeContent(any())).thenReturn("mock content");
      doNothing().when(dataTracerService).addTrace(any(DataTracerForm.class));

      // When
      enterpriseManager.updateEnterpriseTransaction(updateForm, enterpriseDetail);

      // Then - verify getChangeContent called twice (old and new)
      verify(dataTracerService, times(2)).getChangeContent(any());
    }

    @Test
    @DisplayName("Should use correct enterprise ID in DataTracerForm")
    void updateEnterprise_DataTracerForm_UsesCorrectEnterpriseId() {
      // Given
      Long specificEnterpriseId = 8888L;
      EnterpriseUpdateForm updateForm = new EnterpriseUpdateForm();
      updateForm.setEnterpriseId(specificEnterpriseId);

      EnterpriseEntity enterpriseDetail = new EnterpriseEntity();
      enterpriseDetail.setEnterpriseId(specificEnterpriseId);

      when(enterpriseDao.updateById(any(EnterpriseEntity.class))).thenReturn(1);
      when(dataTracerService.getChangeContent(any())).thenReturn("mock content");
      doNothing()
          .when(dataTracerService)
          .addTrace(
              argThat(
                  form ->
                      form.getDataId().equals(specificEnterpriseId)
                          && form.getType() == DataTracerTypeEnum.OA_ENTERPRISE));

      // When
      enterpriseManager.updateEnterpriseTransaction(updateForm, enterpriseDetail);

      // Then
      verify(dataTracerService, times(1)).addTrace(any(DataTracerForm.class));
    }
  }

  // ==================== deleteEnterpriseTransaction() Tests ====================

  @Nested
  @DisplayName("deleteEnterpriseTransaction() Tests - Soft Delete + DataTracer.delete Pattern")
  class DeleteEnterpriseTransactionTests {

    @Test
    @DisplayName("Should soft delete enterprise and record data tracer")
    void deleteEnterprise_ValidId_SoftDeletesAndTraces() {
      // Given
      doNothing().when(enterpriseDao).deleteEnterprise(eq(TEST_ENTERPRISE_ID), eq(Boolean.TRUE));
      doNothing()
          .when(dataTracerService)
          .delete(TEST_ENTERPRISE_ID, DataTracerTypeEnum.OA_ENTERPRISE);

      // When
      enterpriseManager.deleteEnterpriseTransaction(TEST_ENTERPRISE_ID);

      // Then
      verify(enterpriseDao, times(1)).deleteEnterprise(eq(TEST_ENTERPRISE_ID), eq(Boolean.TRUE));
      verify(dataTracerService, times(1))
          .delete(TEST_ENTERPRISE_ID, DataTracerTypeEnum.OA_ENTERPRISE);
    }

    @Test
    @DisplayName("Should delete before data tracer (transaction order)")
    void deleteEnterprise_TransactionOrder_DeleteBeforeTrace() {
      // Given
      doNothing().when(enterpriseDao).deleteEnterprise(anyLong(), eq(Boolean.TRUE));
      doNothing().when(dataTracerService).delete(anyLong(), any(DataTracerTypeEnum.class));

      // When
      enterpriseManager.deleteEnterpriseTransaction(TEST_ENTERPRISE_ID);

      // Then - verify delete called before dataTracer
      var inOrder = inOrder(enterpriseDao, dataTracerService);
      inOrder.verify(enterpriseDao).deleteEnterprise(anyLong(), eq(Boolean.TRUE));
      inOrder.verify(dataTracerService).delete(anyLong(), any(DataTracerTypeEnum.class));
    }

    @Test
    @DisplayName("Should handle different enterprise IDs correctly")
    void deleteEnterprise_DifferentIds_HandlesCorrectly() {
      // Given
      Long specificEnterpriseId = 7777L;
      doNothing().when(enterpriseDao).deleteEnterprise(anyLong(), eq(Boolean.TRUE));
      doNothing().when(dataTracerService).delete(anyLong(), any(DataTracerTypeEnum.class));

      // When
      enterpriseManager.deleteEnterpriseTransaction(specificEnterpriseId);

      // Then
      verify(enterpriseDao, times(1)).deleteEnterprise(eq(specificEnterpriseId), eq(Boolean.TRUE));
      verify(dataTracerService, times(1))
          .delete(eq(specificEnterpriseId), eq(DataTracerTypeEnum.OA_ENTERPRISE));
      verify(enterpriseDao, never()).deleteEnterprise(eq(TEST_ENTERPRISE_ID), eq(Boolean.TRUE));
    }

    @Test
    @DisplayName("Should use DataTracerTypeEnum.OA_ENTERPRISE")
    void deleteEnterprise_DataTracerType_UsesOaEnterprise() {
      // Given
      doNothing().when(enterpriseDao).deleteEnterprise(anyLong(), eq(Boolean.TRUE));
      doNothing().when(dataTracerService).delete(anyLong(), any(DataTracerTypeEnum.class));

      // When
      enterpriseManager.deleteEnterpriseTransaction(TEST_ENTERPRISE_ID);

      // Then
      verify(dataTracerService, times(1)).delete(anyLong(), eq(DataTracerTypeEnum.OA_ENTERPRISE));
    }
  }
}
