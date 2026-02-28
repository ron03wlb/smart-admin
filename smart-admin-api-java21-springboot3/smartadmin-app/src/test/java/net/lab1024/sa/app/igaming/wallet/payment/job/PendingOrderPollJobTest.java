package net.lab1024.sa.app.igaming.wallet.payment.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import net.lab1024.sa.igaming.wallet.payment.job.PendingOrderPollJob;
import net.lab1024.sa.igaming.wallet.payment.service.ReconciliationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link PendingOrderPollJob}.
 *
 * @author iGaming Team
 * @since 2026-02-28
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PendingOrderPollJob 單元測試")
class PendingOrderPollJobTest {

  @Mock private ReconciliationService reconciliationService;

  @InjectMocks private PendingOrderPollJob pendingOrderPollJob;

  @Nested
  @DisplayName("run 方法測試")
  class RunTests {

    @Test
    @DisplayName("應呼叫 reconcilePendingOrders 並回傳包含處理筆數的結果")
    void run_shouldCallReconcilePendingOrdersAndReturnResultWithCount() {
      // given
      when(reconciliationService.reconcilePendingOrders(30)).thenReturn(4);

      // when
      String result = pendingOrderPollJob.run(null);

      // then
      assertThat(result).contains("4");
      verify(reconciliationService).reconcilePendingOrders(30);
    }
  }

  @Nested
  @DisplayName("getClassName 方法測試")
  class GetClassNameTests {

    @Test
    @DisplayName("應回傳完整類別名稱 (FQCN)")
    void getClassName_shouldReturnFullyQualifiedClassName() {
      // when
      String className = pendingOrderPollJob.getClassName();

      // then
      assertThat(className)
          .isEqualTo("net.lab1024.sa.igaming.wallet.payment.job.PendingOrderPollJob");
    }
  }
}
