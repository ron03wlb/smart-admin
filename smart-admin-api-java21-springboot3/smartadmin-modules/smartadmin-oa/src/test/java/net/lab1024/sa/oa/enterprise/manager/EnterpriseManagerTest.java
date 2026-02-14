package net.lab1024.sa.oa.enterprise.manager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import net.lab1024.sa.oa.enterprise.dao.EnterpriseDao;
import net.lab1024.sa.oa.enterprise.domain.entity.EnterpriseEntity;
import net.lab1024.sa.oa.enterprise.domain.form.EnterpriseCreateForm;
import net.lab1024.sa.oa.enterprise.domain.form.EnterpriseUpdateForm;
import net.lab1024.sa.support.datatracer.constant.DataTracerTypeEnum;
import net.lab1024.sa.support.datatracer.domain.form.DataTracerForm;
import net.lab1024.sa.support.datatracer.service.DataTracerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * EnterpriseManager 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>新增企業事務
 *   <li>更新企業事務
 *   <li>刪除企業事務
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EnterpriseManager 單元測試")
class EnterpriseManagerTest {

  @Mock private EnterpriseDao enterpriseDao;

  @Mock private DataTracerService dataTracerService;

  @InjectMocks private EnterpriseManager enterpriseManager;

  // ==================== createEnterpriseTransaction 測試 ====================

  @Nested
  @DisplayName("createEnterpriseTransaction 新增企業事務測試")
  class CreateEnterpriseTransactionTest {

    @Test
    @DisplayName("正常情況：應該插入數據並記錄追蹤")
    void shouldInsertAndTrace() {
      // Given
      EnterpriseCreateForm createForm = new EnterpriseCreateForm();
      createForm.setEnterpriseName("測試企業");

      // When
      enterpriseManager.createEnterpriseTransaction(createForm);

      // Then
      verify(enterpriseDao).insert(any(EnterpriseEntity.class));
      verify(dataTracerService).insert(any(), any(DataTracerTypeEnum.class));
    }
  }

  // ==================== updateEnterpriseTransaction 測試 ====================

  @Nested
  @DisplayName("updateEnterpriseTransaction 更新企業事務測試")
  class UpdateEnterpriseTransactionTest {

    @Test
    @DisplayName("正常情況：應該更新數據並記錄追蹤")
    void shouldUpdateAndTrace() {
      // Given
      EnterpriseUpdateForm updateForm = new EnterpriseUpdateForm();
      updateForm.setEnterpriseId(1L);
      updateForm.setEnterpriseName("更新後企業");

      EnterpriseEntity enterpriseDetail = new EnterpriseEntity();
      enterpriseDetail.setEnterpriseId(1L);
      enterpriseDetail.setEnterpriseName("原企業");

      when(dataTracerService.getChangeContent(any(EnterpriseEntity.class))).thenReturn("content");

      // When
      enterpriseManager.updateEnterpriseTransaction(updateForm, enterpriseDetail);

      // Then
      verify(enterpriseDao).updateById(any(EnterpriseEntity.class));
      verify(dataTracerService).addTrace(any(DataTracerForm.class));
    }
  }

  // ==================== deleteEnterpriseTransaction 測試 ====================

  @Nested
  @DisplayName("deleteEnterpriseTransaction 刪除企業事務測試")
  class DeleteEnterpriseTransactionTest {

    @Test
    @DisplayName("正常情況：應該刪除數據並記錄追蹤")
    void shouldDeleteAndTrace() {
      // Given
      Long enterpriseId = 1L;

      // When
      enterpriseManager.deleteEnterpriseTransaction(enterpriseId);

      // Then
      verify(enterpriseDao).deleteEnterprise(enterpriseId, Boolean.TRUE);
      verify(dataTracerService).delete(enterpriseId, DataTracerTypeEnum.OA_ENTERPRISE);
    }
  }
}
