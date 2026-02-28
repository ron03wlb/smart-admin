package net.lab1024.sa.app.igaming.risk.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import net.lab1024.sa.igaming.risk.job.RiskProposalSLAJob;
import net.lab1024.sa.igaming.risk.manager.RiskProposalManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link RiskProposalSLAJob}.
 *
 * @author iGaming Team
 * @since 2026-02-28
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RiskProposalSLAJob 單元測試")
class RiskProposalSLAJobTest {

  @Mock private RiskProposalManager riskProposalManager;

  @InjectMocks private RiskProposalSLAJob riskProposalSLAJob;

  @Nested
  @DisplayName("run 方法測試")
  class RunTests {

    @Test
    @DisplayName("應呼叫 escalateOverdueProposals 並回傳包含升級筆數的結果")
    void run_shouldCallEscalateOverdueProposalsAndReturnResultWithCount() {
      // given
      when(riskProposalManager.escalateOverdueProposals()).thenReturn(2);

      // when
      String result = riskProposalSLAJob.run(null);

      // then
      assertThat(result).contains("2");
      verify(riskProposalManager).escalateOverdueProposals();
    }
  }

  @Nested
  @DisplayName("getClassName 方法測試")
  class GetClassNameTests {

    @Test
    @DisplayName("應回傳完整類別名稱 (FQCN)")
    void getClassName_shouldReturnFullyQualifiedClassName() {
      // when
      String className = riskProposalSLAJob.getClassName();

      // then
      assertThat(className).isEqualTo("net.lab1024.sa.igaming.risk.job.RiskProposalSLAJob");
    }
  }
}
