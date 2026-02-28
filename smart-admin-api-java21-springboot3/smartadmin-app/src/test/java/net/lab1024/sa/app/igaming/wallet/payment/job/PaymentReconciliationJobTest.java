package net.lab1024.sa.app.igaming.wallet.payment.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import net.lab1024.sa.igaming.wallet.payment.job.PaymentReconciliationJob;
import net.lab1024.sa.igaming.wallet.payment.manager.PaymentReconciliationManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link PaymentReconciliationJob}.
 *
 * @author iGaming Team
 * @since 2026-02-28
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentReconciliationJob 單元測試")
class PaymentReconciliationJobTest {

  @Mock private PaymentReconciliationManager reconciliationManager;

  @InjectMocks private PaymentReconciliationJob paymentReconciliationJob;

  @Nested
  @DisplayName("run 方法測試")
  class RunTests {

    @Test
    @DisplayName("應呼叫 dailyBatchReconciliation 並回傳包含對帳完成的結果")
    void run_shouldCallDailyBatchReconciliationAndReturnResult() {
      // when
      String result = paymentReconciliationJob.run(null);

      // then
      assertThat(result).contains("reconciliation completed");
      verify(reconciliationManager).dailyBatchReconciliation(any(LocalDate.class));
    }
  }

  @Nested
  @DisplayName("getClassName 方法測試")
  class GetClassNameTests {

    @Test
    @DisplayName("應回傳完整類別名稱 (FQCN)")
    void getClassName_shouldReturnFullyQualifiedClassName() {
      // when
      String className = paymentReconciliationJob.getClassName();

      // then
      assertThat(className)
          .isEqualTo("net.lab1024.sa.igaming.wallet.payment.job.PaymentReconciliationJob");
    }
  }
}
