package net.lab1024.sa.app.igaming.activity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.activity.controller.BonusAdminController;
import net.lab1024.sa.igaming.activity.domain.form.BonusForfeitForm;
import net.lab1024.sa.igaming.activity.domain.form.WageringProgressQueryForm;
import net.lab1024.sa.igaming.activity.domain.vo.PlayerBonusRecordVO;
import net.lab1024.sa.igaming.activity.service.BonusAdminService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * BonusAdminController unit tests.
 *
 * @author iGaming Team
 * @since 2026-03-01
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BonusAdminController 單元測試")
class BonusAdminControllerTest {

  @Mock private BonusAdminService bonusAdminService;
  @InjectMocks private BonusAdminController bonusAdminController;

  @Test
  @DisplayName("query — 委託 Service 成功")
  void query_success() {
    PageResult<PlayerBonusRecordVO> pageResult = new PageResult<>();
    when(bonusAdminService.queryBonusRecords(any())).thenReturn(ResponseDTO.ok(pageResult));

    WageringProgressQueryForm form = new WageringProgressQueryForm();
    form.setPageNum(1L);
    form.setPageSize(10L);

    ResponseDTO<PageResult<PlayerBonusRecordVO>> result = bonusAdminController.query(form);

    assertThat(result.getOk()).isTrue();
  }

  @Test
  @DisplayName("getById — 委託 Service 成功")
  void getById_success() {
    PlayerBonusRecordVO vo = new PlayerBonusRecordVO();
    vo.setRecordId(1L);
    when(bonusAdminService.getBonusDetail(1L)).thenReturn(ResponseDTO.ok(vo));

    ResponseDTO<PlayerBonusRecordVO> result = bonusAdminController.getById(1L);

    assertThat(result.getOk()).isTrue();
    assertThat(result.getData().getRecordId()).isEqualTo(1L);
  }

  @Test
  @DisplayName("forfeit — 委託 Service 成功")
  void forfeit_success() {
    when(bonusAdminService.forfeitBonus(any())).thenReturn(ResponseDTO.ok());

    BonusForfeitForm form = new BonusForfeitForm();
    form.setRecordId(1L);
    form.setReason("Fraud detected");

    ResponseDTO<String> result = bonusAdminController.forfeit(form);

    assertThat(result.getOk()).isTrue();
  }

  @Test
  @DisplayName("triggerExpire — 委託 Service 成功")
  void triggerExpire_success() {
    when(bonusAdminService.triggerExpireBatch(anyInt())).thenReturn(ResponseDTO.ok(5));

    ResponseDTO<Integer> result = bonusAdminController.triggerExpire(100);

    assertThat(result.getOk()).isTrue();
    assertThat(result.getData()).isEqualTo(5);
  }
}
