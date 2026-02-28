package net.lab1024.sa.app.igaming.activity.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import net.lab1024.sa.igaming.activity.job.VipEvaluationJob;
import net.lab1024.sa.igaming.activity.manager.VipAutoEvaluationManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link VipEvaluationJob}.
 *
 * @author iGaming Team
 * @since 2026-02-28
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VipEvaluationJob 單元測試")
class VipEvaluationJobTest {

  @Mock private VipAutoEvaluationManager vipAutoEvaluationManager;

  @InjectMocks private VipEvaluationJob vipEvaluationJob;

  @Nested
  @DisplayName("run 方法測試")
  class RunTests {

    @Test
    @DisplayName("應呼叫 evaluateAllActivePlayers 並回傳包含評估人數的結果")
    void run_shouldCallEvaluateAllActivePlayersAndReturnResultWithCount() {
      // given
      when(vipAutoEvaluationManager.evaluateAllActivePlayers()).thenReturn(10);

      // when
      String result = vipEvaluationJob.run(null);

      // then
      assertThat(result).contains("10");
      verify(vipAutoEvaluationManager).evaluateAllActivePlayers();
    }
  }

  @Nested
  @DisplayName("getClassName 方法測試")
  class GetClassNameTests {

    @Test
    @DisplayName("應回傳完整類別名稱 (FQCN)")
    void getClassName_shouldReturnFullyQualifiedClassName() {
      // when
      String className = vipEvaluationJob.getClassName();

      // then
      assertThat(className).isEqualTo("net.lab1024.sa.igaming.activity.job.VipEvaluationJob");
    }
  }
}
