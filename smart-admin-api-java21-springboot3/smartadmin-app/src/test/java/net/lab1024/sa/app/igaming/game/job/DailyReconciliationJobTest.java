package net.lab1024.sa.app.igaming.game.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import net.lab1024.sa.igaming.game.job.DailyReconciliationJob;
import net.lab1024.sa.igaming.game.manager.ReconciliationManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link DailyReconciliationJob}.
 *
 * @author iGaming Team
 * @since 2026-02-28
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DailyReconciliationJob 單元測試")
class DailyReconciliationJobTest {

  @Mock private ReconciliationManager reconciliationManager;

  @InjectMocks private DailyReconciliationJob dailyReconciliationJob;

  @Nested
  @DisplayName("run 方法測試")
  class RunTests {

    @Test
    @DisplayName("應呼叫 dailyBatchReconciliation 並回傳包含對帳完成的結果")
    void run_shouldCallDailyBatchReconciliationAndReturnResult() {
      // when
      String result = dailyReconciliationJob.run(null);

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
      String className = dailyReconciliationJob.getClassName();

      // then
      assertThat(className).isEqualTo("net.lab1024.sa.igaming.game.job.DailyReconciliationJob");
    }
  }
}
