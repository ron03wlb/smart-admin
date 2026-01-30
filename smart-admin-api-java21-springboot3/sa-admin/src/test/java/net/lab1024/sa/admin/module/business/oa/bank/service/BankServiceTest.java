package net.lab1024.sa.admin.module.business.oa.bank.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.*;
import net.lab1024.sa.admin.module.business.oa.bank.BankTestFixture;
import net.lab1024.sa.admin.module.business.oa.bank.dao.BankDao;
import net.lab1024.sa.admin.module.business.oa.bank.domain.BankCreateForm;
import net.lab1024.sa.admin.module.business.oa.bank.domain.BankEntity;
import net.lab1024.sa.admin.module.business.oa.bank.domain.BankQueryForm;
import net.lab1024.sa.admin.module.business.oa.bank.domain.BankUpdateForm;
import net.lab1024.sa.admin.module.business.oa.bank.domain.BankVO;
import net.lab1024.sa.admin.module.business.oa.bank.manager.BankManager;
import net.lab1024.sa.admin.module.business.oa.enterprise.dao.EnterpriseDao;
import net.lab1024.sa.admin.module.business.oa.enterprise.domain.entity.EnterpriseEntity;
import net.lab1024.sa.base.module.support.datatracer.service.DataTracerService;
import net.lab1024.sa.foundation.domain.response.PageResult;
import net.lab1024.sa.foundation.domain.response.ResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * BankService 单元测试
 *
 * <p>测试覆盖范围：
 *
 * <ul>
 *   <li>银行信息查询（分页查询、列表查询、详情查询）
 *   <li>银行信息CRUD操作（创建、更新、删除）
 *   <li>企业验证
 *   <li>账号唯一性验证
 *   <li>银行信息存在性验证
 * </ul>
 *
 * @author Claude Code (Service/Manager Test Coverage Plan)
 * @since 2026-01-30
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BankService 单元测试")
class BankServiceTest {

  @Mock private BankDao bankDao;

  @Mock private EnterpriseDao enterpriseDao;

  @Mock private DataTracerService dataTracerService;

  @Mock private BankManager bankManager;

  @InjectMocks private BankService bankService;

  @BeforeEach
  void setUp() {
    BankTestFixture.resetCounter();
  }

  @Nested
  @DisplayName("queryByPage() - 分页查询银行信息")
  class QueryByPageTests {

    @Test
    @DisplayName("正常分页查询 - 应返回分页结果")
    void queryByPage_ValidForm_ShouldReturnPageResult() {
      // Arrange
      BankQueryForm form = BankTestFixture.createQueryForm();
      List<BankVO> voList = BankTestFixture.createVOList(5);
      Page<BankVO> page = new Page<>(1, 10);
      page.setRecords(voList);
      page.setTotal(5);

      when(bankDao.queryPage(any(Page.class), eq(form))).thenReturn(voList);

      // Act
      ResponseDTO<PageResult<BankVO>> response = bankService.queryByPage(form);

      // Assert
      assertTrue(response.getOk());
      assertNotNull(response.getData());
      assertEquals(5, response.getData().getList().size());
      assertEquals(Boolean.FALSE, form.getDeletedFlag());
      verify(bankDao, times(1)).queryPage(any(Page.class), eq(form));
    }

    @Test
    @DisplayName("空结果查询 - 应返回空列表")
    void queryByPage_NoResults_ShouldReturnEmptyList() {
      // Arrange
      BankQueryForm form = BankTestFixture.createQueryForm();
      List<BankVO> emptyList = Collections.emptyList();

      when(bankDao.queryPage(any(Page.class), eq(form))).thenReturn(emptyList);

      // Act
      ResponseDTO<PageResult<BankVO>> response = bankService.queryByPage(form);

      // Assert
      assertTrue(response.getOk());
      assertNotNull(response.getData());
      assertTrue(response.getData().getList().isEmpty());
    }
  }

  @Nested
  @DisplayName("queryList() - 查询银行信息列表")
  class QueryListTests {

    @Test
    @DisplayName("正常查询列表 - 应返回银行列表")
    void queryList_ValidEnterpriseId_ShouldReturnList() {
      // Arrange
      Long enterpriseId = 1L;
      List<BankVO> voList = BankTestFixture.createVOList(3);

      when(bankDao.queryPage(isNull(), any(BankQueryForm.class))).thenReturn(voList);

      // Act
      ResponseDTO<List<BankVO>> response = bankService.queryList(enterpriseId);

      // Assert
      assertTrue(response.getOk());
      assertNotNull(response.getData());
      assertEquals(3, response.getData().size());
      verify(bankDao, times(1)).queryPage(isNull(), any(BankQueryForm.class));
    }

    @Test
    @DisplayName("空结果查询 - 应返回空列表")
    void queryList_NoResults_ShouldReturnEmptyList() {
      // Arrange
      Long enterpriseId = 1L;
      List<BankVO> emptyList = Collections.emptyList();

      when(bankDao.queryPage(isNull(), any(BankQueryForm.class))).thenReturn(emptyList);

      // Act
      ResponseDTO<List<BankVO>> response = bankService.queryList(enterpriseId);

      // Assert
      assertTrue(response.getOk());
      assertNotNull(response.getData());
      assertTrue(response.getData().isEmpty());
    }
  }

  @Nested
  @DisplayName("getDetail() - 查询银行信息详情")
  class GetDetailTests {

    @Test
    @DisplayName("正常查询详情 - 应返回银行详情")
    void getDetail_ExistingBankId_ShouldReturnDetail() {
      // Arrange
      Long bankId = 100L;
      BankVO vo = BankTestFixture.createVO(bankId);

      when(bankDao.getDetail(bankId, Boolean.FALSE)).thenReturn(vo);

      // Act
      ResponseDTO<BankVO> response = bankService.getDetail(bankId);

      // Assert
      assertTrue(response.getOk());
      assertNotNull(response.getData());
      assertEquals(bankId, response.getData().getBankId());
      verify(bankDao, times(1)).getDetail(bankId, Boolean.FALSE);
    }

    @Test
    @DisplayName("银行信息不存在 - 应返回错误")
    void getDetail_NonExistingBankId_ShouldReturnError() {
      // Arrange
      Long bankId = 999L;

      when(bankDao.getDetail(bankId, Boolean.FALSE)).thenReturn(null);

      // Act
      ResponseDTO<BankVO> response = bankService.getDetail(bankId);

      // Assert
      assertFalse(response.getOk());
      assertEquals("银行信息不存在", response.getMsg());
    }
  }

  @Nested
  @DisplayName("createBank() - 新建银行信息")
  class CreateBankTests {

    @Test
    @DisplayName("正常创建银行信息 - 应返回成功")
    void createBank_ValidForm_ShouldReturnSuccess() {
      // Arrange
      BankCreateForm form = BankTestFixture.createCreateForm(1L);
      EnterpriseEntity enterprise = new EnterpriseEntity();
      enterprise.setEnterpriseId(1L);
      enterprise.setDeletedFlag(false);

      when(enterpriseDao.selectById(form.getEnterpriseId())).thenReturn(enterprise);
      when(bankDao.queryByAccountNumber(
              form.getEnterpriseId(), form.getAccountNumber(), null, Boolean.FALSE))
          .thenReturn(null);
      doNothing().when(bankManager).createBankTransaction(form, form.getEnterpriseId());

      // Act
      ResponseDTO<String> response = bankService.createBank(form);

      // Assert
      assertTrue(response.getOk());
      verify(enterpriseDao, times(1)).selectById(form.getEnterpriseId());
      verify(bankDao, times(1))
          .queryByAccountNumber(
              form.getEnterpriseId(), form.getAccountNumber(), null, Boolean.FALSE);
      verify(bankManager, times(1)).createBankTransaction(form, form.getEnterpriseId());
    }

    @Test
    @DisplayName("企业不存在 - 应返回错误")
    void createBank_EnterpriseNotFound_ShouldReturnError() {
      // Arrange
      BankCreateForm form = BankTestFixture.createCreateForm(999L);

      when(enterpriseDao.selectById(form.getEnterpriseId())).thenReturn(null);

      // Act
      ResponseDTO<String> response = bankService.createBank(form);

      // Assert
      assertFalse(response.getOk());
      assertEquals("企业不存在", response.getMsg());
      verify(bankManager, never()).createBankTransaction(any(), anyLong());
    }

    @Test
    @DisplayName("企业已删除 - 应返回错误")
    void createBank_EnterpriseDeleted_ShouldReturnError() {
      // Arrange
      BankCreateForm form = BankTestFixture.createCreateForm(1L);
      EnterpriseEntity deletedEnterprise = new EnterpriseEntity();
      deletedEnterprise.setEnterpriseId(1L);
      deletedEnterprise.setDeletedFlag(true);

      when(enterpriseDao.selectById(form.getEnterpriseId())).thenReturn(deletedEnterprise);

      // Act
      ResponseDTO<String> response = bankService.createBank(form);

      // Assert
      assertFalse(response.getOk());
      assertEquals("企业不存在", response.getMsg());
      verify(bankManager, never()).createBankTransaction(any(), anyLong());
    }

    @Test
    @DisplayName("账号重复 - 应返回错误")
    void createBank_DuplicateAccountNumber_ShouldReturnError() {
      // Arrange
      BankCreateForm form = BankTestFixture.createCreateForm(1L);
      EnterpriseEntity enterprise = new EnterpriseEntity();
      enterprise.setEnterpriseId(1L);
      enterprise.setDeletedFlag(false);
      BankEntity existingBank = BankTestFixture.createEntity();

      when(enterpriseDao.selectById(form.getEnterpriseId())).thenReturn(enterprise);
      when(bankDao.queryByAccountNumber(
              form.getEnterpriseId(), form.getAccountNumber(), null, Boolean.FALSE))
          .thenReturn(existingBank);

      // Act
      ResponseDTO<String> response = bankService.createBank(form);

      // Assert
      assertFalse(response.getOk());
      assertEquals("银行信息账号重复", response.getMsg());
      verify(bankManager, never()).createBankTransaction(any(), anyLong());
    }
  }

  @Nested
  @DisplayName("updateBank() - 编辑银行信息")
  class UpdateBankTests {

    @Test
    @DisplayName("正常更新银行信息 - 应返回成功")
    void updateBank_ValidForm_ShouldReturnSuccess() {
      // Arrange
      Long bankId = 100L;
      BankUpdateForm form = BankTestFixture.createUpdateForm(bankId, 1L);
      EnterpriseEntity enterprise = new EnterpriseEntity();
      enterprise.setEnterpriseId(1L);
      enterprise.setDeletedFlag(false);
      BankEntity existingBank = BankTestFixture.createEntity();
      existingBank.setBankId(bankId);
      existingBank.setDeletedFlag(false);

      when(enterpriseDao.selectById(form.getEnterpriseId())).thenReturn(enterprise);
      when(bankDao.selectById(bankId)).thenReturn(existingBank);
      when(bankDao.queryByAccountNumber(
              form.getEnterpriseId(), form.getAccountNumber(), bankId, Boolean.FALSE))
          .thenReturn(null);
      doNothing()
          .when(bankManager)
          .updateBankTransaction(form, existingBank, form.getEnterpriseId());

      // Act
      ResponseDTO<String> response = bankService.updateBank(form);

      // Assert
      assertTrue(response.getOk());
      verify(enterpriseDao, times(1)).selectById(form.getEnterpriseId());
      verify(bankDao, times(1)).selectById(bankId);
      verify(bankDao, times(1))
          .queryByAccountNumber(
              form.getEnterpriseId(), form.getAccountNumber(), bankId, Boolean.FALSE);
      verify(bankManager, times(1))
          .updateBankTransaction(form, existingBank, form.getEnterpriseId());
    }

    @Test
    @DisplayName("企业不存在 - 应返回错误")
    void updateBank_EnterpriseNotFound_ShouldReturnError() {
      // Arrange
      Long bankId = 100L;
      BankUpdateForm form = BankTestFixture.createUpdateForm(bankId, 999L);

      when(enterpriseDao.selectById(form.getEnterpriseId())).thenReturn(null);

      // Act
      ResponseDTO<String> response = bankService.updateBank(form);

      // Assert
      assertFalse(response.getOk());
      assertEquals("企业不存在", response.getMsg());
      verify(bankManager, never()).updateBankTransaction(any(), any(), anyLong());
    }

    @Test
    @DisplayName("企业已删除 - 应返回错误")
    void updateBank_EnterpriseDeleted_ShouldReturnError() {
      // Arrange
      Long bankId = 100L;
      BankUpdateForm form = BankTestFixture.createUpdateForm(bankId, 1L);
      EnterpriseEntity deletedEnterprise = new EnterpriseEntity();
      deletedEnterprise.setEnterpriseId(1L);
      deletedEnterprise.setDeletedFlag(true);

      when(enterpriseDao.selectById(form.getEnterpriseId())).thenReturn(deletedEnterprise);

      // Act
      ResponseDTO<String> response = bankService.updateBank(form);

      // Assert
      assertFalse(response.getOk());
      assertEquals("企业不存在", response.getMsg());
      verify(bankManager, never()).updateBankTransaction(any(), any(), anyLong());
    }

    @Test
    @DisplayName("银行信息不存在 - 应返回错误")
    void updateBank_BankNotFound_ShouldReturnError() {
      // Arrange
      Long bankId = 999L;
      BankUpdateForm form = BankTestFixture.createUpdateForm(bankId, 1L);
      EnterpriseEntity enterprise = new EnterpriseEntity();
      enterprise.setEnterpriseId(1L);
      enterprise.setDeletedFlag(false);

      when(enterpriseDao.selectById(form.getEnterpriseId())).thenReturn(enterprise);
      when(bankDao.selectById(bankId)).thenReturn(null);

      // Act
      ResponseDTO<String> response = bankService.updateBank(form);

      // Assert
      assertFalse(response.getOk());
      assertEquals("银行信息不存在", response.getMsg());
      verify(bankManager, never()).updateBankTransaction(any(), any(), anyLong());
    }

    @Test
    @DisplayName("银行信息已删除 - 应返回错误")
    void updateBank_BankDeleted_ShouldReturnError() {
      // Arrange
      Long bankId = 100L;
      BankUpdateForm form = BankTestFixture.createUpdateForm(bankId, 1L);
      EnterpriseEntity enterprise = new EnterpriseEntity();
      enterprise.setEnterpriseId(1L);
      enterprise.setDeletedFlag(false);
      BankEntity deletedBank = BankTestFixture.createEntity();
      deletedBank.setBankId(bankId);
      deletedBank.setDeletedFlag(true);

      when(enterpriseDao.selectById(form.getEnterpriseId())).thenReturn(enterprise);
      when(bankDao.selectById(bankId)).thenReturn(deletedBank);

      // Act
      ResponseDTO<String> response = bankService.updateBank(form);

      // Assert
      assertFalse(response.getOk());
      assertEquals("银行信息不存在", response.getMsg());
      verify(bankManager, never()).updateBankTransaction(any(), any(), anyLong());
    }

    @Test
    @DisplayName("账号重复 - 应返回错误")
    void updateBank_DuplicateAccountNumber_ShouldReturnError() {
      // Arrange
      Long bankId = 100L;
      BankUpdateForm form = BankTestFixture.createUpdateForm(bankId, 1L);
      EnterpriseEntity enterprise = new EnterpriseEntity();
      enterprise.setEnterpriseId(1L);
      enterprise.setDeletedFlag(false);
      BankEntity existingBank = BankTestFixture.createEntity();
      existingBank.setBankId(bankId);
      existingBank.setDeletedFlag(false);
      BankEntity duplicateBank = BankTestFixture.createEntity();
      duplicateBank.setBankId(200L); // 不同ID但同账号

      when(enterpriseDao.selectById(form.getEnterpriseId())).thenReturn(enterprise);
      when(bankDao.selectById(bankId)).thenReturn(existingBank);
      when(bankDao.queryByAccountNumber(
              form.getEnterpriseId(), form.getAccountNumber(), bankId, Boolean.FALSE))
          .thenReturn(duplicateBank);

      // Act
      ResponseDTO<String> response = bankService.updateBank(form);

      // Assert
      assertFalse(response.getOk());
      assertEquals("银行信息账号重复", response.getMsg());
      verify(bankManager, never()).updateBankTransaction(any(), any(), anyLong());
    }
  }

  @Nested
  @DisplayName("deleteBank() - 删除银行信息")
  class DeleteBankTests {

    @Test
    @DisplayName("正常删除银行信息 - 应返回成功")
    void deleteBank_ValidId_ShouldReturnSuccess() {
      // Arrange
      Long bankId = 100L;
      BankEntity existingBank = BankTestFixture.createEntity();
      existingBank.setBankId(bankId);
      existingBank.setDeletedFlag(false);

      when(bankDao.selectById(bankId)).thenReturn(existingBank);
      doNothing().when(bankManager).deleteBankTransaction(bankId, existingBank);

      // Act
      ResponseDTO<String> response = bankService.deleteBank(bankId);

      // Assert
      assertTrue(response.getOk());
      verify(bankDao, times(1)).selectById(bankId);
      verify(bankManager, times(1)).deleteBankTransaction(bankId, existingBank);
    }

    @Test
    @DisplayName("银行信息不存在 - 应返回错误")
    void deleteBank_BankNotFound_ShouldReturnError() {
      // Arrange
      Long bankId = 999L;

      when(bankDao.selectById(bankId)).thenReturn(null);

      // Act
      ResponseDTO<String> response = bankService.deleteBank(bankId);

      // Assert
      assertFalse(response.getOk());
      assertEquals("银行信息不存在", response.getMsg());
      verify(bankManager, never()).deleteBankTransaction(anyLong(), any());
    }

    @Test
    @DisplayName("银行信息已删除 - 应返回错误")
    void deleteBank_BankAlreadyDeleted_ShouldReturnError() {
      // Arrange
      Long bankId = 100L;
      BankEntity deletedBank = BankTestFixture.createEntity();
      deletedBank.setBankId(bankId);
      deletedBank.setDeletedFlag(true);

      when(bankDao.selectById(bankId)).thenReturn(deletedBank);

      // Act
      ResponseDTO<String> response = bankService.deleteBank(bankId);

      // Assert
      assertFalse(response.getOk());
      assertEquals("银行信息不存在", response.getMsg());
      verify(bankManager, never()).deleteBankTransaction(anyLong(), any());
    }
  }
}
