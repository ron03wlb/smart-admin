package net.lab1024.sa.app.igaming.game.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import net.lab1024.sa.igaming.game.job.MissingSettlementPollJob;
import net.lab1024.sa.igaming.game.manager.ReconciliationManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link MissingSettlementPollJob}.
 *
 * @author iGaming Team
 * @since 2026-02-28
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MissingSettlementPollJob 單元測試")
class MissingSettlementPollJobTest {

  @Mock private ReconciliationManager reconciliationManager;

  @InjectMocks private MissingSettlementPollJob missingSettlementPollJob;

  @Nested
  @DisplayName("run 方法測試")
  class RunTests {

    @Test
    @DisplayName("應呼叫 pollMissingSettlements 並回傳包含處理筆數的結果")
    void run_shouldCallPollMissingSettlementsAndReturnResultWithCount() {
      // given
      when(reconciliationManager.pollMissingSettlements()).thenReturn(7);

      // when
      String result = missingSettlementPollJob.run(null);

      // then
      assertThat(result).contains("7");
      verify(reconciliationManager).pollMissingSettlements();
    }
  }

  @Nested
  @DisplayName("getClassName 方法測試")
  class GetClassNameTests {

    @Test
    @DisplayName("應回傳完整類別名稱 (FQCN)")
    void getClassName_shouldReturnFullyQualifiedClassName() {
      // when
      String className = missingSettlementPollJob.getClassName();

      // then
      assertThat(className).isEqualTo("net.lab1024.sa.igaming.game.job.MissingSettlementPollJob");
    }
  }
}
