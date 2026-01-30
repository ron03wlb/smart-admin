package net.lab1024.sa.admin.module.business.oa.invoice.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.Collections;
import java.util.List;
import net.lab1024.sa.admin.BaseUnitTest;
import net.lab1024.sa.admin.module.business.oa.enterprise.dao.EnterpriseDao;
import net.lab1024.sa.admin.module.business.oa.enterprise.domain.vo.EnterpriseVO;
import net.lab1024.sa.admin.module.business.oa.invoice.InvoiceTestFixture;
import net.lab1024.sa.admin.module.business.oa.invoice.dao.InvoiceDao;
import net.lab1024.sa.admin.module.business.oa.invoice.domain.InvoiceAddForm;
import net.lab1024.sa.admin.module.business.oa.invoice.domain.InvoiceEntity;
import net.lab1024.sa.admin.module.business.oa.invoice.domain.InvoiceQueryForm;
import net.lab1024.sa.admin.module.business.oa.invoice.domain.InvoiceUpdateForm;
import net.lab1024.sa.admin.module.business.oa.invoice.domain.InvoiceVO;
import net.lab1024.sa.admin.module.business.oa.invoice.manager.InvoiceManager;
import net.lab1024.sa.foundation.domain.response.PageResult;
import net.lab1024.sa.foundation.domain.response.ResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

/**
 * InvoiceService Tests - P0 Critical Business Logic
 *
 * <p>Focus Areas: - CRUD operations - Enterprise association validation - Account number uniqueness
 * - Deleted flag checking - Error handling and validation
 *
 * @author Claude Code - Service/Manager Test Coverage Plan
 * @since 2026-01-30
 */
@DisplayName("InvoiceService Tests")
class InvoiceServiceTest extends BaseUnitTest {

  @InjectMocks private InvoiceService invoiceService;

  // DAO Mocks
  @Mock private InvoiceDao invoiceDao;
  @Mock private EnterpriseDao enterpriseDao;

  // Manager Mocks
  @Mock private InvoiceManager invoiceManager;

  // Test Fixtures
  private InvoiceTestFixture fixture;

  @BeforeEach
  void setUp() {
    fixture = new InvoiceTestFixture();
    InvoiceTestFixture.resetCounter();
  }

  @Nested
  @DisplayName("queryByPage() - 分页查询发票")
  class QueryByPageTests {

    @Test
    @DisplayName("正常分页查询 - 应返回分页结果")
    void queryByPage_ValidForm_ShouldReturnPageResult() {
      // Arrange
      InvoiceQueryForm form = InvoiceTestFixture.createQueryForm();
      List<InvoiceVO> invoiceList = InvoiceTestFixture.createVOList(5);

      when(invoiceDao.queryPage(any(Page.class), any(InvoiceQueryForm.class)))
          .thenReturn(invoiceList);

      // Act
      ResponseDTO<PageResult<InvoiceVO>> response = invoiceService.queryByPage(form);

      // Assert
      assertTrue(response.getOk());
      assertNotNull(response.getData());
      assertEquals(5, response.getData().getList().size());
      verify(invoiceDao, times(1)).queryPage(any(Page.class), any(InvoiceQueryForm.class));
    }

    @Test
    @DisplayName("无结果查询 - 应返回空列表")
    void queryByPage_NoResults_ShouldReturnEmptyList() {
      // Arrange
      InvoiceQueryForm form = InvoiceTestFixture.createQueryForm();

      when(invoiceDao.queryPage(any(Page.class), any(InvoiceQueryForm.class)))
          .thenReturn(Collections.emptyList());

      // Act
      ResponseDTO<PageResult<InvoiceVO>> response = invoiceService.queryByPage(form);

      // Assert
      assertTrue(response.getOk());
      assertNotNull(response.getData());
      assertTrue(response.getData().getList().isEmpty());
    }
  }

  @Nested
  @DisplayName("queryList() - 列表查询发票")
  class QueryListTests {

    @Test
    @DisplayName("按企业ID查询 - 应返回发票列表")
    void queryList_ByEnterpriseId_ShouldReturnList() {
      // Arrange
      Long enterpriseId = 1L;
      List<InvoiceVO> invoiceList = InvoiceTestFixture.createVOList(3);

      when(invoiceDao.queryPage(isNull(), any(InvoiceQueryForm.class))).thenReturn(invoiceList);

      // Act
      ResponseDTO<List<InvoiceVO>> response = invoiceService.queryList(enterpriseId);

      // Assert
      assertTrue(response.getOk());
      assertNotNull(response.getData());
      assertEquals(3, response.getData().size());
      verify(invoiceDao, times(1)).queryPage(isNull(), any(InvoiceQueryForm.class));
    }

    @Test
    @DisplayName("无发票企业 - 应返回空列表")
    void queryList_NoInvoices_ShouldReturnEmptyList() {
      // Arrange
      Long enterpriseId = 999L;

      when(invoiceDao.queryPage(isNull(), any(InvoiceQueryForm.class)))
          .thenReturn(Collections.emptyList());

      // Act
      ResponseDTO<List<InvoiceVO>> response = invoiceService.queryList(enterpriseId);

      // Assert
      assertTrue(response.getOk());
      assertNotNull(response.getData());
      assertTrue(response.getData().isEmpty());
    }
  }

  @Nested
  @DisplayName("getDetail() - 查询发票详情")
  class GetDetailTests {

    @Test
    @DisplayName("正常查询 - 应返回发票详情")
    void getDetail_ValidId_ShouldReturnDetail() {
      // Arrange
      Long invoiceId = 100L;
      InvoiceVO invoiceVO = InvoiceTestFixture.createVO(invoiceId);

      when(invoiceDao.getDetail(invoiceId, Boolean.FALSE)).thenReturn(invoiceVO);

      // Act
      ResponseDTO<InvoiceVO> response = invoiceService.getDetail(invoiceId);

      // Assert
      assertTrue(response.getOk());
      assertNotNull(response.getData());
      assertEquals(invoiceId, response.getData().getInvoiceId());
    }

    @Test
    @DisplayName("发票不存在 - 应返回错误")
    void getDetail_InvoiceNotExists_ShouldReturnError() {
      // Arrange
      Long invoiceId = 999L;

      when(invoiceDao.getDetail(invoiceId, Boolean.FALSE)).thenReturn(null);

      // Act
      ResponseDTO<InvoiceVO> response = invoiceService.getDetail(invoiceId);

      // Assert
      assertFalse(response.getOk());
      assertNull(response.getData());
    }
  }

  @Nested
  @DisplayName("createInvoice() - 创建发票")
  class CreateInvoiceTests {

    @Test
    @DisplayName("正常创建发票 - 应返回成功")
    void createInvoice_ValidForm_ShouldReturnSuccess() {
      // Arrange
      InvoiceAddForm form = InvoiceTestFixture.createAddForm();
      EnterpriseVO enterpriseVO = new EnterpriseVO();
      enterpriseVO.setEnterpriseId(1L);
      enterpriseVO.setEnterpriseName("测试企业");

      when(enterpriseDao.getDetail(form.getEnterpriseId(), Boolean.FALSE)).thenReturn(enterpriseVO);
      when(invoiceDao.queryByAccountNumber(
              form.getEnterpriseId(), form.getAccountNumber(), null, Boolean.FALSE))
          .thenReturn(null);
      doNothing().when(invoiceManager).createInvoiceTransaction(form, form.getEnterpriseId());

      // Act
      ResponseDTO<String> response = invoiceService.createInvoice(form);

      // Assert
      assertTrue(response.getOk());
      verify(invoiceManager, times(1)).createInvoiceTransaction(form, form.getEnterpriseId());
    }

    @Test
    @DisplayName("企业不存在 - 应返回错误")
    void createInvoice_EnterpriseNotExists_ShouldReturnError() {
      // Arrange
      InvoiceAddForm form = InvoiceTestFixture.createAddForm();

      when(enterpriseDao.getDetail(form.getEnterpriseId(), Boolean.FALSE)).thenReturn(null);

      // Act
      ResponseDTO<String> response = invoiceService.createInvoice(form);

      // Assert
      assertFalse(response.getOk());
      verify(invoiceManager, never()).createInvoiceTransaction(any(), anyLong());
    }

    @Test
    @DisplayName("账号重复 - 应返回错误")
    void createInvoice_AccountNumberDuplicate_ShouldReturnError() {
      // Arrange
      InvoiceAddForm form = InvoiceTestFixture.createAddForm();
      EnterpriseVO enterpriseVO = new EnterpriseVO();
      enterpriseVO.setEnterpriseId(1L);

      InvoiceEntity existingInvoice = InvoiceTestFixture.createEntity();
      existingInvoice.setInvoiceId(100L);
      existingInvoice.setAccountNumber(form.getAccountNumber());

      when(enterpriseDao.getDetail(form.getEnterpriseId(), Boolean.FALSE)).thenReturn(enterpriseVO);
      when(invoiceDao.queryByAccountNumber(
              form.getEnterpriseId(), form.getAccountNumber(), null, Boolean.FALSE))
          .thenReturn(existingInvoice);

      // Act
      ResponseDTO<String> response = invoiceService.createInvoice(form);

      // Assert
      assertFalse(response.getOk());
      verify(invoiceManager, never()).createInvoiceTransaction(any(), anyLong());
    }
  }

  @Nested
  @DisplayName("updateInvoice() - 更新发票")
  class UpdateInvoiceTests {

    @Test
    @DisplayName("正常更新发票 - 应返回成功")
    void updateInvoice_ValidForm_ShouldReturnSuccess() {
      // Arrange
      Long invoiceId = 100L;
      InvoiceUpdateForm form = InvoiceTestFixture.createUpdateForm(invoiceId);

      EnterpriseVO enterpriseVO = new EnterpriseVO();
      enterpriseVO.setEnterpriseId(form.getEnterpriseId());

      InvoiceEntity existingInvoice = InvoiceTestFixture.createEntity();
      existingInvoice.setInvoiceId(invoiceId);
      existingInvoice.setDeletedFlag(false);

      when(enterpriseDao.getDetail(form.getEnterpriseId(), Boolean.FALSE)).thenReturn(enterpriseVO);
      when(invoiceDao.selectById(invoiceId)).thenReturn(existingInvoice);
      when(invoiceDao.queryByAccountNumber(
              form.getEnterpriseId(), form.getAccountNumber(), invoiceId, Boolean.FALSE))
          .thenReturn(null);
      doNothing()
          .when(invoiceManager)
          .updateInvoiceTransaction(form, existingInvoice, form.getEnterpriseId());

      // Act
      ResponseDTO<String> response = invoiceService.updateInvoice(form);

      // Assert
      assertTrue(response.getOk());
      verify(invoiceManager, times(1))
          .updateInvoiceTransaction(form, existingInvoice, form.getEnterpriseId());
    }

    @Test
    @DisplayName("企业不存在 - 应返回错误")
    void updateInvoice_EnterpriseNotExists_ShouldReturnError() {
      // Arrange
      Long invoiceId = 100L;
      InvoiceUpdateForm form = InvoiceTestFixture.createUpdateForm(invoiceId);

      when(enterpriseDao.getDetail(form.getEnterpriseId(), Boolean.FALSE)).thenReturn(null);

      // Act
      ResponseDTO<String> response = invoiceService.updateInvoice(form);

      // Assert
      assertFalse(response.getOk());
      verify(invoiceManager, never()).updateInvoiceTransaction(any(), any(), anyLong());
    }

    @Test
    @DisplayName("发票不存在 - 应返回错误")
    void updateInvoice_InvoiceNotExists_ShouldReturnError() {
      // Arrange
      Long invoiceId = 999L;
      InvoiceUpdateForm form = InvoiceTestFixture.createUpdateForm(invoiceId);

      EnterpriseVO enterpriseVO = new EnterpriseVO();
      enterpriseVO.setEnterpriseId(form.getEnterpriseId());

      when(enterpriseDao.getDetail(form.getEnterpriseId(), Boolean.FALSE)).thenReturn(enterpriseVO);
      when(invoiceDao.selectById(invoiceId)).thenReturn(null);

      // Act
      ResponseDTO<String> response = invoiceService.updateInvoice(form);

      // Assert
      assertFalse(response.getOk());
      verify(invoiceManager, never()).updateInvoiceTransaction(any(), any(), anyLong());
    }

    @Test
    @DisplayName("发票已删除 - 应返回错误")
    void updateInvoice_InvoiceDeleted_ShouldReturnError() {
      // Arrange
      Long invoiceId = 100L;
      InvoiceUpdateForm form = InvoiceTestFixture.createUpdateForm(invoiceId);

      EnterpriseVO enterpriseVO = new EnterpriseVO();
      enterpriseVO.setEnterpriseId(form.getEnterpriseId());

      InvoiceEntity deletedInvoice = InvoiceTestFixture.createEntity();
      deletedInvoice.setInvoiceId(invoiceId);
      deletedInvoice.setDeletedFlag(true);

      when(enterpriseDao.getDetail(form.getEnterpriseId(), Boolean.FALSE)).thenReturn(enterpriseVO);
      when(invoiceDao.selectById(invoiceId)).thenReturn(deletedInvoice);

      // Act
      ResponseDTO<String> response = invoiceService.updateInvoice(form);

      // Assert
      assertFalse(response.getOk());
      verify(invoiceManager, never()).updateInvoiceTransaction(any(), any(), anyLong());
    }

    @Test
    @DisplayName("账号重复 - 应返回错误")
    void updateInvoice_AccountNumberDuplicate_ShouldReturnError() {
      // Arrange
      Long invoiceId = 100L;
      InvoiceUpdateForm form = InvoiceTestFixture.createUpdateForm(invoiceId);

      EnterpriseVO enterpriseVO = new EnterpriseVO();
      enterpriseVO.setEnterpriseId(form.getEnterpriseId());

      InvoiceEntity existingInvoice = InvoiceTestFixture.createEntity();
      existingInvoice.setInvoiceId(invoiceId);
      existingInvoice.setDeletedFlag(false);

      InvoiceEntity duplicateAccount = InvoiceTestFixture.createEntity();
      duplicateAccount.setInvoiceId(200L); // Different ID
      duplicateAccount.setAccountNumber(form.getAccountNumber());

      when(enterpriseDao.getDetail(form.getEnterpriseId(), Boolean.FALSE)).thenReturn(enterpriseVO);
      when(invoiceDao.selectById(invoiceId)).thenReturn(existingInvoice);
      when(invoiceDao.queryByAccountNumber(
              form.getEnterpriseId(), form.getAccountNumber(), invoiceId, Boolean.FALSE))
          .thenReturn(duplicateAccount);

      // Act
      ResponseDTO<String> response = invoiceService.updateInvoice(form);

      // Assert
      assertFalse(response.getOk());
      verify(invoiceManager, never()).updateInvoiceTransaction(any(), any(), anyLong());
    }
  }

  @Nested
  @DisplayName("deleteInvoice() - 删除发票")
  class DeleteInvoiceTests {

    @Test
    @DisplayName("正常删除发票 - 应返回成功")
    void deleteInvoice_ValidId_ShouldReturnSuccess() {
      // Arrange
      Long invoiceId = 100L;
      InvoiceEntity existingInvoice = InvoiceTestFixture.createEntity();
      existingInvoice.setInvoiceId(invoiceId);
      existingInvoice.setDeletedFlag(false);

      when(invoiceDao.selectById(invoiceId)).thenReturn(existingInvoice);
      doNothing().when(invoiceManager).deleteInvoiceTransaction(invoiceId, existingInvoice);

      // Act
      ResponseDTO<String> response = invoiceService.deleteInvoice(invoiceId);

      // Assert
      assertTrue(response.getOk());
      verify(invoiceManager, times(1)).deleteInvoiceTransaction(invoiceId, existingInvoice);
    }

    @Test
    @DisplayName("发票不存在 - 应返回错误")
    void deleteInvoice_InvoiceNotExists_ShouldReturnError() {
      // Arrange
      Long invoiceId = 999L;

      when(invoiceDao.selectById(invoiceId)).thenReturn(null);

      // Act
      ResponseDTO<String> response = invoiceService.deleteInvoice(invoiceId);

      // Assert
      assertFalse(response.getOk());
      verify(invoiceManager, never()).deleteInvoiceTransaction(anyLong(), any());
    }

    @Test
    @DisplayName("发票已删除 - 应返回错误")
    void deleteInvoice_InvoiceAlreadyDeleted_ShouldReturnError() {
      // Arrange
      Long invoiceId = 100L;
      InvoiceEntity deletedInvoice = InvoiceTestFixture.createEntity();
      deletedInvoice.setInvoiceId(invoiceId);
      deletedInvoice.setDeletedFlag(true);

      when(invoiceDao.selectById(invoiceId)).thenReturn(deletedInvoice);

      // Act
      ResponseDTO<String> response = invoiceService.deleteInvoice(invoiceId);

      // Assert
      assertFalse(response.getOk());
      verify(invoiceManager, never()).deleteInvoiceTransaction(anyLong(), any());
    }
  }
}
