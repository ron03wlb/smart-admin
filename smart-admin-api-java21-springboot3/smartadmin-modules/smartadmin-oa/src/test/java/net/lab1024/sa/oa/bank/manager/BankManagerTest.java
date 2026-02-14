package net.lab1024.sa.oa.bank.manager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import net.lab1024.sa.oa.bank.dao.BankDao;
import net.lab1024.sa.oa.bank.domain.BankCreateForm;
import net.lab1024.sa.oa.bank.domain.BankEntity;
import net.lab1024.sa.oa.bank.domain.BankUpdateForm;
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
 * BankManager 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>新增銀行事務
 *   <li>更新銀行事務
 *   <li>刪除銀行事務
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BankManager 單元測試")
class BankManagerTest {

  @Mock private BankDao bankDao;

  @Mock private DataTracerService dataTracerService;

  @InjectMocks private BankManager bankManager;

  // ==================== createBankTransaction 測試 ====================

  @Nested
  @DisplayName("createBankTransaction 新增銀行事務測試")
  class CreateBankTransactionTest {

    @Test
    @DisplayName("正常情況：應該插入數據並記錄追蹤")
    void shouldInsertAndTrace() {
      // Given
      BankCreateForm createForm = new BankCreateForm();
      createForm.setBankName("工商銀行");
      createForm.setAccountNumber("1234567890");
      Long enterpriseId = 1L;

      when(dataTracerService.getChangeContent(any(BankEntity.class))).thenReturn("content");

      // When
      bankManager.createBankTransaction(createForm, enterpriseId);

      // Then
      verify(bankDao).insert(any(BankEntity.class));
      verify(dataTracerService)
          .addTrace(eq(enterpriseId), eq(DataTracerTypeEnum.OA_ENTERPRISE), any(String.class));
    }
  }

  // ==================== updateBankTransaction 測試 ====================

  @Nested
  @DisplayName("updateBankTransaction 更新銀行事務測試")
  class UpdateBankTransactionTest {

    @Test
    @DisplayName("正常情況：應該更新數據並記錄追蹤")
    void shouldUpdateAndTrace() {
      // Given
      BankUpdateForm updateForm = new BankUpdateForm();
      updateForm.setBankId(1L);
      updateForm.setBankName("建設銀行");

      BankEntity bankDetail = new BankEntity();
      bankDetail.setBankId(1L);
      bankDetail.setBankName("工商銀行");

      Long enterpriseId = 1L;

      when(dataTracerService.getChangeContent(any(BankEntity.class), any(BankEntity.class)))
          .thenReturn("content");

      // When
      bankManager.updateBankTransaction(updateForm, bankDetail, enterpriseId);

      // Then
      verify(bankDao).updateById(any(BankEntity.class));
      verify(dataTracerService)
          .addTrace(eq(enterpriseId), eq(DataTracerTypeEnum.OA_ENTERPRISE), any(String.class));
    }
  }

  // ==================== deleteBankTransaction 測試 ====================

  @Nested
  @DisplayName("deleteBankTransaction 刪除銀行事務測試")
  class DeleteBankTransactionTest {

    @Test
    @DisplayName("正常情況：應該刪除數據並記錄追蹤")
    void shouldDeleteAndTrace() {
      // Given
      Long bankId = 1L;
      BankEntity bankDetail = new BankEntity();
      bankDetail.setBankId(bankId);
      bankDetail.setEnterpriseId(1L);

      when(dataTracerService.getChangeContent(any(BankEntity.class))).thenReturn("content");

      // When
      bankManager.deleteBankTransaction(bankId, bankDetail);

      // Then
      verify(bankDao).deleteBank(bankId, Boolean.TRUE);
      verify(dataTracerService)
          .addTrace(eq(1L), eq(DataTracerTypeEnum.OA_ENTERPRISE), any(String.class));
    }
  }
}
