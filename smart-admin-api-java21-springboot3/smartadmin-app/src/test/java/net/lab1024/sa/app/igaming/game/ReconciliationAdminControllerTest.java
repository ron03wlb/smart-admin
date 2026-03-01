package net.lab1024.sa.app.igaming.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.game.controller.ReconciliationAdminController;
import net.lab1024.sa.igaming.game.domain.form.ReconciliationTriggerForm;
import net.lab1024.sa.igaming.game.domain.vo.ReconciliationVO;
import net.lab1024.sa.igaming.game.service.ReconciliationAdminService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * ReconciliationAdminController unit tests.
 *
 * @author iGaming Team
 * @since 2026-03-01
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReconciliationAdminController 單元測試")
class ReconciliationAdminControllerTest {

  @Mock private ReconciliationAdminService reconciliationAdminService;
  @InjectMocks private ReconciliationAdminController reconciliationAdminController;

  @Test
  @DisplayName("trigger — 委託 Service 成功")
  void trigger_success() {
    ReconciliationTriggerForm form = new ReconciliationTriggerForm();
    form.setDate(LocalDate.of(2026, 2, 28));
    when(reconciliationAdminService.triggerDailyReconciliation(any())).thenReturn(ResponseDTO.ok());

    ResponseDTO<String> result = reconciliationAdminController.trigger(form);
    assertThat(result.getOk()).isTrue();
  }

  @Test
  @DisplayName("poll — 委託 Service 成功")
  void poll_success() {
    when(reconciliationAdminService.triggerMissingSettlementPoll()).thenReturn(ResponseDTO.ok(5));

    ResponseDTO<Integer> result = reconciliationAdminController.poll();
    assertThat(result.getOk()).isTrue();
    assertThat(result.getData()).isEqualTo(5);
  }

  @Test
  @DisplayName("status — 委託 Service 成功")
  void status_success() {
    LocalDate date = LocalDate.of(2026, 2, 28);
    ReconciliationVO vo = new ReconciliationVO();
    vo.setProviderCode("GP001");
    when(reconciliationAdminService.queryReconciliationStatus(date))
        .thenReturn(ResponseDTO.ok(List.of(vo)));

    ResponseDTO<List<ReconciliationVO>> result = reconciliationAdminController.status(date);
    assertThat(result.getOk()).isTrue();
    assertThat(result.getData()).hasSize(1);
  }
}
