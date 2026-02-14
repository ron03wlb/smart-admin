package net.lab1024.sa.oa.bank.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.oa.bank.dao.BankDao;
import net.lab1024.sa.oa.bank.domain.BankCreateForm;
import net.lab1024.sa.oa.bank.domain.BankEntity;
import net.lab1024.sa.oa.bank.domain.BankQueryForm;
import net.lab1024.sa.oa.bank.domain.BankUpdateForm;
import net.lab1024.sa.oa.bank.domain.BankVO;
import net.lab1024.sa.oa.bank.manager.BankManager;
import net.lab1024.sa.oa.enterprise.dao.EnterpriseDao;
import net.lab1024.sa.oa.enterprise.domain.entity.EnterpriseEntity;
import net.lab1024.sa.support.datatracer.service.DataTracerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * BankService 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>銀行信息 CRUD 操作
 *   <li>企業存在性校驗
 *   <li>銀行賬號重複校驗
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BankService 單元測試")
class BankServiceTest {

  @Mock private BankDao bankDao;

  @Mock private EnterpriseDao enterpriseDao;

  @Mock private DataTracerService dataTracerService;

  @Mock private BankManager bankManager;

  @InjectMocks private BankService bankService;

  // ==================== queryByPage 測試 ====================

  @Nested
  @DisplayName("queryByPage 分頁查詢測試")
  class QueryByPageTest {

    @Test
    @DisplayName("正常情況：應該返回分頁結果")
    void shouldReturnPageResult() {
      // Given
      BankQueryForm queryForm = new BankQueryForm();
      queryForm.setPageNum(1L);
      queryForm.setPageSize(10L);

      BankVO bankVO = createTestBankVO(1L, "工商銀行");
      when(bankDao.queryPage(any(), any())).thenReturn(Collections.singletonList(bankVO));

      // When
      ResponseDTO<PageResult<BankVO>> result = bankService.queryByPage(queryForm);

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
    @DisplayName("正常情況：應該返回銀行列表")
    void shouldReturnBankList() {
      // Given
      Long enterpriseId = 1L;
      BankVO bankVO = createTestBankVO(1L, "工商銀行");
      when(bankDao.queryPage(eq(null), any())).thenReturn(Collections.singletonList(bankVO));

      // When
      ResponseDTO<List<BankVO>> result = bankService.queryList(enterpriseId);

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
    @DisplayName("正常情況：應該返回銀行詳情")
    void shouldReturnBankDetail() {
      // Given
      Long bankId = 1L;
      BankVO bankVO = createTestBankVO(bankId, "工商銀行");
      when(bankDao.getDetail(bankId, Boolean.FALSE)).thenReturn(bankVO);

      // When
      ResponseDTO<BankVO> result = bankService.getDetail(bankId);

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData().getBankName()).isEqualTo("工商銀行");
    }

    @Test
    @DisplayName("異常情況：銀行不存在時應返回錯誤")
    void shouldReturnErrorWhenNotFound() {
      // Given
      Long bankId = 999L;
      when(bankDao.getDetail(bankId, Boolean.FALSE)).thenReturn(null);

      // When
      ResponseDTO<BankVO> result = bankService.getDetail(bankId);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("银行信息不存在");
    }
  }

  // ==================== createBank 測試 ====================

  @Nested
  @DisplayName("createBank 新增銀行測試")
  class CreateBankTest {

    @Test
    @DisplayName("正常情況：應該成功新增銀行")
    void shouldCreateBankSuccess() {
      // Given
      BankCreateForm createForm = createTestCreateForm(1L, "1234567890");
      EnterpriseEntity enterprise = createTestEnterpriseEntity(1L, "測試企業");

      when(enterpriseDao.selectById(1L)).thenReturn(enterprise);
      when(bankDao.queryByAccountNumber(1L, "1234567890", null, Boolean.FALSE)).thenReturn(null);

      // When
      ResponseDTO<String> result = bankService.createBank(createForm);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(bankManager).createBankTransaction(createForm, 1L);
    }

    @Test
    @DisplayName("異常情況：企業不存在時應返回錯誤")
    void shouldReturnErrorWhenEnterpriseNotFound() {
      // Given
      BankCreateForm createForm = createTestCreateForm(999L, "1234567890");
      when(enterpriseDao.selectById(999L)).thenReturn(null);

      // When
      ResponseDTO<String> result = bankService.createBank(createForm);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("企业不存在");
      verify(bankManager, never()).createBankTransaction(any(), any());
    }

    @Test
    @DisplayName("異常情況：企業已刪除時應返回錯誤")
    void shouldReturnErrorWhenEnterpriseDeleted() {
      // Given
      BankCreateForm createForm = createTestCreateForm(1L, "1234567890");
      EnterpriseEntity deletedEnterprise = createTestEnterpriseEntity(1L, "已刪除企業");
      deletedEnterprise.setDeletedFlag(true);

      when(enterpriseDao.selectById(1L)).thenReturn(deletedEnterprise);

      // When
      ResponseDTO<String> result = bankService.createBank(createForm);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("企业不存在");
    }

    @Test
    @DisplayName("異常情況：銀行賬號重複時應返回錯誤")
    void shouldReturnErrorWhenAccountNumberDuplicate() {
      // Given
      BankCreateForm createForm = createTestCreateForm(1L, "1234567890");
      EnterpriseEntity enterprise = createTestEnterpriseEntity(1L, "測試企業");
      BankEntity existingBank = createTestBankEntity(2L, "1234567890");

      when(enterpriseDao.selectById(1L)).thenReturn(enterprise);
      when(bankDao.queryByAccountNumber(1L, "1234567890", null, Boolean.FALSE))
          .thenReturn(existingBank);

      // When
      ResponseDTO<String> result = bankService.createBank(createForm);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("银行信息账号重复");
    }
  }

  // ==================== updateBank 測試 ====================

  @Nested
  @DisplayName("updateBank 更新銀行測試")
  class UpdateBankTest {

    @Test
    @DisplayName("正常情況：應該成功更新銀行")
    void shouldUpdateBankSuccess() {
      // Given
      BankUpdateForm updateForm = createTestUpdateForm(1L, 1L, "9876543210");
      EnterpriseEntity enterprise = createTestEnterpriseEntity(1L, "測試企業");
      BankEntity bankEntity = createTestBankEntity(1L, "1234567890");

      when(enterpriseDao.selectById(1L)).thenReturn(enterprise);
      when(bankDao.selectById(1L)).thenReturn(bankEntity);
      when(bankDao.queryByAccountNumber(1L, "9876543210", 1L, Boolean.FALSE)).thenReturn(null);

      // When
      ResponseDTO<String> result = bankService.updateBank(updateForm);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(bankManager).updateBankTransaction(updateForm, bankEntity, 1L);
    }

    @Test
    @DisplayName("異常情況：銀行不存在時應返回錯誤")
    void shouldReturnErrorWhenBankNotFound() {
      // Given
      BankUpdateForm updateForm = createTestUpdateForm(999L, 1L, "1234567890");
      EnterpriseEntity enterprise = createTestEnterpriseEntity(1L, "測試企業");

      when(enterpriseDao.selectById(1L)).thenReturn(enterprise);
      when(bankDao.selectById(999L)).thenReturn(null);

      // When
      ResponseDTO<String> result = bankService.updateBank(updateForm);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("银行信息不存在");
    }
  }

  // ==================== deleteBank 測試 ====================

  @Nested
  @DisplayName("deleteBank 刪除銀行測試")
  class DeleteBankTest {

    @Test
    @DisplayName("正常情況：應該成功刪除銀行")
    void shouldDeleteBankSuccess() {
      // Given
      Long bankId = 1L;
      BankEntity bankEntity = createTestBankEntity(bankId, "1234567890");

      when(bankDao.selectById(bankId)).thenReturn(bankEntity);

      // When
      ResponseDTO<String> result = bankService.deleteBank(bankId);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(bankManager).deleteBankTransaction(bankId, bankEntity);
    }

    @Test
    @DisplayName("異常情況：銀行不存在時應返回錯誤")
    void shouldReturnErrorWhenBankNotFound() {
      // Given
      Long bankId = 999L;
      when(bankDao.selectById(bankId)).thenReturn(null);

      // When
      ResponseDTO<String> result = bankService.deleteBank(bankId);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("银行信息不存在");
      verify(bankManager, never()).deleteBankTransaction(any(), any());
    }
  }

  // ==================== Helper Methods ====================

  private BankVO createTestBankVO(Long id, String bankName) {
    BankVO vo = new BankVO();
    vo.setBankId(id);
    vo.setBankName(bankName);
    return vo;
  }

  private BankEntity createTestBankEntity(Long id, String accountNumber) {
    BankEntity entity = new BankEntity();
    entity.setBankId(id);
    entity.setAccountNumber(accountNumber);
    entity.setDeletedFlag(false);
    return entity;
  }

  private BankCreateForm createTestCreateForm(Long enterpriseId, String accountNumber) {
    BankCreateForm form = new BankCreateForm();
    form.setEnterpriseId(enterpriseId);
    form.setAccountNumber(accountNumber);
    form.setBankName("工商銀行");
    return form;
  }

  private BankUpdateForm createTestUpdateForm(
      Long bankId, Long enterpriseId, String accountNumber) {
    BankUpdateForm form = new BankUpdateForm();
    form.setBankId(bankId);
    form.setEnterpriseId(enterpriseId);
    form.setAccountNumber(accountNumber);
    return form;
  }

  private EnterpriseEntity createTestEnterpriseEntity(Long id, String name) {
    EnterpriseEntity entity = new EnterpriseEntity();
    entity.setEnterpriseId(id);
    entity.setEnterpriseName(name);
    entity.setDeletedFlag(false);
    return entity;
  }
}
