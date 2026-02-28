package net.lab1024.sa.app.igaming.activity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import io.vavr.control.Option;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.common.token.player.StpPlayerUtil;
import net.lab1024.sa.igaming.activity.controller.BonusClaimController;
import net.lab1024.sa.igaming.activity.domain.form.BonusClaimForm;
import net.lab1024.sa.igaming.activity.domain.form.WageringProgressQueryForm;
import net.lab1024.sa.igaming.activity.domain.vo.BonusClaimResultVO;
import net.lab1024.sa.igaming.activity.domain.vo.PlayerBonusRecordVO;
import net.lab1024.sa.igaming.activity.domain.vo.WageringProgressVO;
import net.lab1024.sa.igaming.activity.service.BonusClaimService;
import net.lab1024.sa.igaming.common.code.ActivityErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * BonusClaimController unit tests — context extraction (StpPlayerUtil/TenantContext) and Option
 * handling.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BonusClaimController 單元測試")
class BonusClaimControllerTest {

  @Mock private BonusClaimService bonusClaimService;

  @InjectMocks private BonusClaimController bonusClaimController;

  @Nested
  @DisplayName("claimBonus")
  class ClaimBonusTests {

    @Test
    @DisplayName("mockStatic playerId + tenantId → 驗證 Service 呼叫參數正確")
    void claimBonus_success() {
      try (MockedStatic<StpPlayerUtil> stpMock = mockStatic(StpPlayerUtil.class);
          MockedStatic<TenantContext> tenantMock = mockStatic(TenantContext.class)) {

        stpMock.when(StpPlayerUtil::getLoginIdAsLong).thenReturn(100L);
        tenantMock.when(TenantContext::getTenantId).thenReturn(1L);

        BonusClaimForm form = new BonusClaimForm();
        BonusClaimResultVO resultVO = new BonusClaimResultVO();
        when(bonusClaimService.claimBonus(100L, form, 1L)).thenReturn(ResponseDTO.ok(resultVO));

        ResponseDTO<BonusClaimResultVO> result = bonusClaimController.claimBonus(form);

        assertThat(result.getOk()).isTrue();
        assertThat(result.getData()).isEqualTo(resultVO);
        verify(bonusClaimService).claimBonus(100L, form, 1L);
      }
    }
  }

  @Nested
  @DisplayName("queryBonusRecords")
  class QueryBonusRecordsTests {

    @Test
    @DisplayName("mockStatic playerId → 驗證 Service 呼叫")
    void queryBonusRecords_success() {
      try (MockedStatic<StpPlayerUtil> stpMock = mockStatic(StpPlayerUtil.class)) {
        stpMock.when(StpPlayerUtil::getLoginIdAsLong).thenReturn(100L);

        WageringProgressQueryForm form = new WageringProgressQueryForm();
        ResponseDTO<PageResult<PlayerBonusRecordVO>> expected = ResponseDTO.ok(new PageResult<>());
        when(bonusClaimService.queryBonusRecords(100L, form)).thenReturn(expected);

        ResponseDTO<PageResult<PlayerBonusRecordVO>> result =
            bonusClaimController.queryBonusRecords(form);

        assertThat(result).isEqualTo(expected);
        verify(bonusClaimService).queryBonusRecords(100L, form);
      }
    }
  }

  @Nested
  @DisplayName("getWageringProgress")
  class GetWageringProgressTests {

    @Test
    @DisplayName("進度存在 → ok(WageringProgressVO)")
    void getWageringProgress_found() {
      try (MockedStatic<StpPlayerUtil> stpMock = mockStatic(StpPlayerUtil.class)) {
        stpMock.when(StpPlayerUtil::getLoginIdAsLong).thenReturn(100L);

        WageringProgressVO vo = new WageringProgressVO();
        when(bonusClaimService.getWageringProgress(100L, 1L)).thenReturn(Option.some(vo));

        ResponseDTO<WageringProgressVO> result = bonusClaimController.getWageringProgress(1L);

        assertThat(result.getOk()).isTrue();
        assertThat(result.getData()).isEqualTo(vo);
      }
    }

    @Test
    @DisplayName("進度不存在 → BONUS_NOT_FOUND")
    void getWageringProgress_notFound() {
      try (MockedStatic<StpPlayerUtil> stpMock = mockStatic(StpPlayerUtil.class)) {
        stpMock.when(StpPlayerUtil::getLoginIdAsLong).thenReturn(100L);

        when(bonusClaimService.getWageringProgress(100L, 999L)).thenReturn(Option.none());

        ResponseDTO<WageringProgressVO> result = bonusClaimController.getWageringProgress(999L);

        assertThat(result.getOk()).isFalse();
        assertThat(result.getMsg()).isEqualTo(ActivityErrorCode.BONUS_NOT_FOUND.getMsg());
      }
    }
  }
}
