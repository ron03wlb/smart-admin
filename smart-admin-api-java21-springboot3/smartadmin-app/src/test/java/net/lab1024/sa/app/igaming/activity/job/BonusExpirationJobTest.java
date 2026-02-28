package net.lab1024.sa.app.igaming.activity.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import net.lab1024.sa.igaming.activity.job.BonusExpirationJob;
import net.lab1024.sa.igaming.activity.manager.BonusLifecycleManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link BonusExpirationJob}.
 *
 * @author iGaming Team
 * @since 2026-02-28
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BonusExpirationJob 單元測試")
class BonusExpirationJobTest {

  @Mock private BonusLifecycleManager bonusLifecycleManager;

  @InjectMocks private BonusExpirationJob bonusExpirationJob;

  @Nested
  @DisplayName("run 方法測試")
  class RunTests {

    @Test
    @DisplayName("應呼叫 expireActiveBonuses 並回傳包含過期筆數的結果")
    void run_shouldCallExpireActiveBonusesAndReturnResultWithCount() {
      // given
      when(bonusLifecycleManager.expireActiveBonuses(100)).thenReturn(5);

      // when
      String result = bonusExpirationJob.run(null);

      // then
      assertThat(result).contains("5");
      verify(bonusLifecycleManager).expireActiveBonuses(100);
    }
  }

  @Nested
  @DisplayName("getClassName 方法測試")
  class GetClassNameTests {

    @Test
    @DisplayName("應回傳完整類別名稱 (FQCN)")
    void getClassName_shouldReturnFullyQualifiedClassName() {
      // when
      String className = bonusExpirationJob.getClassName();

      // then
      assertThat(className).isEqualTo("net.lab1024.sa.igaming.activity.job.BonusExpirationJob");
    }
  }
}
