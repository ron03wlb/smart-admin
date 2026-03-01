package net.lab1024.sa.app.igaming.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.igaming.common.constant.ReconciliationStatusEnum;
import net.lab1024.sa.igaming.game.dao.ReconciliationDao;
import net.lab1024.sa.igaming.game.domain.entity.ReconciliationEntity;
import net.lab1024.sa.igaming.game.domain.form.ReconciliationTriggerForm;
import net.lab1024.sa.igaming.game.domain.vo.ReconciliationVO;
import net.lab1024.sa.igaming.game.manager.ReconciliationManager;
import net.lab1024.sa.igaming.game.service.ReconciliationAdminService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * ReconciliationAdminService unit tests.
 *
 * @author iGaming Team
 * @since 2026-03-01
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReconciliationAdminService 單元測試")
class ReconciliationAdminServiceTest {

  @Mock private ReconciliationManager reconciliationManager;
  @Mock private ReconciliationDao reconciliationDao;
  @InjectMocks private ReconciliationAdminService reconciliationAdminService;

  private MockedStatic<TenantContext> tenantContextMock;

  @BeforeEach
  void setUp() {
    tenantContextMock = Mockito.mockStatic(TenantContext.class);
    tenantContextMock.when(TenantContext::getTenantId).thenReturn(1L);
  }

  @AfterEach
  void tearDown() {
    tenantContextMock.close();
  }

  @Test
  @DisplayName("成功觸發每日對帳")
  void triggerDailyReconciliation_success() {
    ReconciliationTriggerForm form = new ReconciliationTriggerForm();
    form.setDate(LocalDate.of(2026, 2, 28));

    ResponseDTO<String> result = reconciliationAdminService.triggerDailyReconciliation(form);

    assertThat(result.getOk()).isTrue();
    verify(reconciliationManager).dailyBatchReconciliation(LocalDate.of(2026, 2, 28));
  }

  @Test
  @DisplayName("Tenant 為空 — 返回錯誤")
  void triggerDailyReconciliation_tenantNull() {
    tenantContextMock.when(TenantContext::getTenantId).thenReturn(null);
    ReconciliationTriggerForm form = new ReconciliationTriggerForm();
    form.setDate(LocalDate.of(2026, 2, 28));

    ResponseDTO<String> result = reconciliationAdminService.triggerDailyReconciliation(form);

    assertThat(result.getOk()).isFalse();
    verifyNoInteractions(reconciliationManager);
  }

  @Test
  @DisplayName("成功觸發缺失結算輪詢")
  void triggerMissingSettlementPoll_success() {
    when(reconciliationManager.pollMissingSettlements()).thenReturn(3);

    ResponseDTO<Integer> result = reconciliationAdminService.triggerMissingSettlementPoll();

    assertThat(result.getOk()).isTrue();
    assertThat(result.getData()).isEqualTo(3);
  }

  @Test
  @DisplayName("查詢對帳狀態 — 返回記錄")
  void queryReconciliationStatus_success() {
    LocalDate date = LocalDate.of(2026, 2, 28);
    ReconciliationEntity entity = new ReconciliationEntity();
    entity.setReconciliationId(1L);
    entity.setReconciliationDate(date);
    entity.setProviderCode("GP001");
    entity.setTotalGpTransactions(100);
    entity.setTotalPlatformTransactions(100);
    entity.setMatchedCount(98);
    entity.setMismatchCount(1);
    entity.setMissingCount(1);
    entity.setStatus(ReconciliationStatusEnum.MISMATCH.getValue());

    when(reconciliationDao.selectList(any())).thenReturn(List.of(entity));

    ResponseDTO<List<ReconciliationVO>> result =
        reconciliationAdminService.queryReconciliationStatus(date);

    assertThat(result.getOk()).isTrue();
    assertThat(result.getData()).hasSize(1);
    assertThat(result.getData().get(0).getProviderCode()).isEqualTo("GP001");
    assertThat(result.getData().get(0).getMatchedCount()).isEqualTo(98);
  }

  @Test
  @DisplayName("查詢對帳狀態 — 無記錄返回空列表")
  void queryReconciliationStatus_noRecords() {
    LocalDate date = LocalDate.of(2026, 1, 1);
    when(reconciliationDao.selectList(any())).thenReturn(Collections.emptyList());

    ResponseDTO<List<ReconciliationVO>> result =
        reconciliationAdminService.queryReconciliationStatus(date);

    assertThat(result.getOk()).isTrue();
    assertThat(result.getData()).isEmpty();
  }
}
