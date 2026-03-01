package net.lab1024.sa.app.igaming.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.function.Supplier;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.common.redislock.LockService;
import net.lab1024.sa.igaming.common.config.IgamingProperties;
import net.lab1024.sa.igaming.common.constant.RoundStatusEnum;
import net.lab1024.sa.igaming.game.dao.GameRoundDao;
import net.lab1024.sa.igaming.game.domain.entity.GameRoundEntity;
import net.lab1024.sa.igaming.game.domain.form.ResettlementForm;
import net.lab1024.sa.igaming.game.domain.vo.CallbackResponseVO;
import net.lab1024.sa.igaming.game.manager.GameTransactionManager;
import net.lab1024.sa.igaming.game.service.GameRoundAdminService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * GameRoundAdminService unit tests.
 *
 * @author iGaming Team
 * @since 2026-03-01
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GameRoundAdminService 單元測試")
class GameRoundAdminServiceTest {

  @Mock private GameRoundDao gameRoundDao;
  @Mock private GameTransactionManager gameTransactionManager;
  @Mock private LockService lockService;
  @Spy private IgamingProperties igamingProperties = new IgamingProperties();
  @InjectMocks private GameRoundAdminService gameRoundAdminService;

  private MockedStatic<TenantContext> tenantContextMock;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    tenantContextMock = Mockito.mockStatic(TenantContext.class);
    tenantContextMock.when(TenantContext::getTenantId).thenReturn(1L);
    // Default: lock service delegates to supplier (transparent pass-through)
    lenient()
        .when(lockService.executeWithLock(anyString(), anyLong(), anyLong(), any(Supplier.class)))
        .thenAnswer(
            invocation -> {
              Supplier<?> supplier = invocation.getArgument(3);
              return supplier.get();
            });
  }

  @AfterEach
  void tearDown() {
    tenantContextMock.close();
  }

  @Test
  @DisplayName("成功重新結算 — 委託 GameTransactionManager")
  void resettle_success() {
    ResettlementForm form = buildForm();

    GameRoundEntity round = buildSettledRound();
    when(gameRoundDao.selectById(1L)).thenReturn(round);

    CallbackResponseVO responseVO = new CallbackResponseVO();
    responseVO.setTransactionId("tx-001");
    responseVO.setBalance(new BigDecimal("120.00"));
    responseVO.setStatus(RoundStatusEnum.ADJUSTED.getValue());
    when(gameTransactionManager.executeResettlement(1L, new BigDecimal("30.00"), "req-001", 1L))
        .thenReturn(ResponseDTO.ok(responseVO));

    ResponseDTO<CallbackResponseVO> result = gameRoundAdminService.resettle(form);
    assertThat(result.getOk()).isTrue();
    assertThat(result.getData().getStatus()).isEqualTo(RoundStatusEnum.ADJUSTED.getValue());
    verify(gameTransactionManager).executeResettlement(1L, new BigDecimal("30.00"), "req-001", 1L);
  }

  @Test
  @DisplayName("Round 不存在 — 返回錯誤")
  void resettle_roundNotFound() {
    ResettlementForm form = buildForm();
    when(gameRoundDao.selectById(1L)).thenReturn(null);

    ResponseDTO<CallbackResponseVO> result = gameRoundAdminService.resettle(form);
    assertThat(result.getOk()).isFalse();
    verifyNoInteractions(gameTransactionManager);
  }

  @Test
  @DisplayName("Round 狀態非 SETTLED — 返回錯誤")
  void resettle_roundNotSettled() {
    ResettlementForm form = buildForm();

    GameRoundEntity round = buildSettledRound();
    round.setStatus(RoundStatusEnum.OPEN.getValue());
    when(gameRoundDao.selectById(1L)).thenReturn(round);

    ResponseDTO<CallbackResponseVO> result = gameRoundAdminService.resettle(form);
    assertThat(result.getOk()).isFalse();
    verifyNoInteractions(gameTransactionManager);
  }

  @Test
  @DisplayName("鎖定取得失敗 — 返回 LOCK_ACQUISITION_FAILED")
  @SuppressWarnings("unchecked")
  void resettle_lockAcquisitionFailed() {
    ResettlementForm form = buildForm();

    GameRoundEntity round = buildSettledRound();
    when(gameRoundDao.selectById(1L)).thenReturn(round);

    when(lockService.executeWithLock(anyString(), anyLong(), anyLong(), any(Supplier.class)))
        .thenThrow(new IllegalStateException("Lock acquisition failed"));

    ResponseDTO<CallbackResponseVO> result = gameRoundAdminService.resettle(form);
    assertThat(result.getOk()).isFalse();
  }

  private ResettlementForm buildForm() {
    ResettlementForm form = new ResettlementForm();
    form.setRoundId(1L);
    form.setNewPayoutAmount(new BigDecimal("30.00"));
    form.setRequestId("req-001");
    return form;
  }

  private GameRoundEntity buildSettledRound() {
    GameRoundEntity round = new GameRoundEntity();
    round.setRoundId(1L);
    round.setPlayerId(1L);
    round.setTenantId(1L);
    round.setGpRoundId("round-001");
    round.setTransactionId("tx-001");
    round.setBetAmount(new BigDecimal("10.00"));
    round.setPayoutAmount(new BigDecimal("25.00"));
    round.setStatus(RoundStatusEnum.SETTLED.getValue());
    return round;
  }
}
