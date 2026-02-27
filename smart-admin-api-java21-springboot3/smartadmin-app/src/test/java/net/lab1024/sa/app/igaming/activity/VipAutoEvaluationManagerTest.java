package net.lab1024.sa.app.igaming.activity;

import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import net.lab1024.sa.igaming.common.constant.VipLevelEnum;
import net.lab1024.sa.igaming.game.dao.GameRoundDao;
import net.lab1024.sa.igaming.player.dao.PlayerDao;
import net.lab1024.sa.igaming.player.domain.entity.PlayerEntity;
import net.lab1024.sa.igaming.player.manager.VipLevelManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * VipAutoEvaluationManager unit tests.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VipAutoEvaluationManager 單元測試")
class VipAutoEvaluationManagerTest {

  @Mock private GameRoundDao gameRoundDao;
  @Mock private PlayerDao playerDao;
  @Mock private VipLevelManager vipLevelManager;

  @InjectMocks
  private net.lab1024.sa.igaming.activity.manager.VipAutoEvaluationManager vipAutoEvaluationManager;

  @Nested
  @DisplayName("evaluateVipLevel")
  class EvaluateVipLevelTest {

    @Test
    @DisplayName("升級 BRONZE → SILVER")
    void evaluate_upgrade_bronzeToSilver() {
      PlayerEntity player = buildPlayer(VipLevelEnum.BRONZE);
      when(playerDao.selectById(1L)).thenReturn(player);
      when(gameRoundDao.sumWeightedTurnoverByPlayer(1L, 1L)).thenReturn(new BigDecimal("1500"));

      vipAutoEvaluationManager.evaluateVipLevel(1L, 1L);

      verify(vipLevelManager).updateVipLevel(eq(player), eq(VipLevelEnum.SILVER), anyString());
    }

    @Test
    @DisplayName("升級 SILVER → GOLD")
    void evaluate_upgrade_silverToGold() {
      PlayerEntity player = buildPlayer(VipLevelEnum.SILVER);
      when(playerDao.selectById(1L)).thenReturn(player);
      when(gameRoundDao.sumWeightedTurnoverByPlayer(1L, 1L)).thenReturn(new BigDecimal("15000"));

      vipAutoEvaluationManager.evaluateVipLevel(1L, 1L);

      verify(vipLevelManager).updateVipLevel(eq(player), eq(VipLevelEnum.GOLD), anyString());
    }

    @Test
    @DisplayName("升級 → DIAMOND")
    void evaluate_upgrade_toDiamond() {
      PlayerEntity player = buildPlayer(VipLevelEnum.GOLD);
      when(playerDao.selectById(1L)).thenReturn(player);
      when(gameRoundDao.sumWeightedTurnoverByPlayer(1L, 1L)).thenReturn(new BigDecimal("250000"));

      vipAutoEvaluationManager.evaluateVipLevel(1L, 1L);

      verify(vipLevelManager).updateVipLevel(eq(player), eq(VipLevelEnum.DIAMOND), anyString());
    }

    @Test
    @DisplayName("等級不變 — 不觸發更新")
    void evaluate_noChange() {
      PlayerEntity player = buildPlayer(VipLevelEnum.SILVER);
      when(playerDao.selectById(1L)).thenReturn(player);
      when(gameRoundDao.sumWeightedTurnoverByPlayer(1L, 1L)).thenReturn(new BigDecimal("5000"));

      vipAutoEvaluationManager.evaluateVipLevel(1L, 1L);

      verify(vipLevelManager, never()).updateVipLevel(any(), any(), anyString());
    }

    @Test
    @DisplayName("玩家不存在 — 忽略")
    void evaluate_playerNotFound() {
      when(playerDao.selectById(1L)).thenReturn(null);

      vipAutoEvaluationManager.evaluateVipLevel(1L, 1L);

      verify(gameRoundDao, never()).sumWeightedTurnoverByPlayer(anyLong(), anyLong());
    }

    @Test
    @DisplayName("玩家已刪除 — 忽略")
    void evaluate_playerDeleted() {
      PlayerEntity player = buildPlayer(VipLevelEnum.BRONZE);
      player.setDeleted(true);
      when(playerDao.selectById(1L)).thenReturn(player);

      vipAutoEvaluationManager.evaluateVipLevel(1L, 1L);

      verify(gameRoundDao, never()).sumWeightedTurnoverByPlayer(anyLong(), anyLong());
    }

    @Test
    @DisplayName("turnover 為 null — 視為 BRONZE")
    void evaluate_nullTurnover() {
      PlayerEntity player = buildPlayer(VipLevelEnum.SILVER);
      when(playerDao.selectById(1L)).thenReturn(player);
      when(gameRoundDao.sumWeightedTurnoverByPlayer(1L, 1L)).thenReturn(null);

      vipAutoEvaluationManager.evaluateVipLevel(1L, 1L);

      // SILVER(2) → BRONZE(1) = level change
      verify(vipLevelManager).updateVipLevel(eq(player), eq(VipLevelEnum.BRONZE), anyString());
    }

    @Test
    @DisplayName("精確閾值 999.99 — 仍為 BRONZE")
    void evaluate_justBelowSilver() {
      PlayerEntity player = buildPlayer(VipLevelEnum.BRONZE);
      when(playerDao.selectById(1L)).thenReturn(player);
      when(gameRoundDao.sumWeightedTurnoverByPlayer(1L, 1L)).thenReturn(new BigDecimal("999.99"));

      vipAutoEvaluationManager.evaluateVipLevel(1L, 1L);

      verify(vipLevelManager, never()).updateVipLevel(any(), any(), anyString());
    }

    @Test
    @DisplayName("精確閾值 1000 — 升級 SILVER")
    void evaluate_exactSilver() {
      PlayerEntity player = buildPlayer(VipLevelEnum.BRONZE);
      when(playerDao.selectById(1L)).thenReturn(player);
      when(gameRoundDao.sumWeightedTurnoverByPlayer(1L, 1L)).thenReturn(new BigDecimal("1000"));

      vipAutoEvaluationManager.evaluateVipLevel(1L, 1L);

      verify(vipLevelManager).updateVipLevel(eq(player), eq(VipLevelEnum.SILVER), anyString());
    }

    @Test
    @DisplayName("升級 → PLATINUM")
    void evaluate_upgrade_toPlatinum() {
      PlayerEntity player = buildPlayer(VipLevelEnum.GOLD);
      when(playerDao.selectById(1L)).thenReturn(player);
      when(gameRoundDao.sumWeightedTurnoverByPlayer(1L, 1L)).thenReturn(new BigDecimal("50000"));

      vipAutoEvaluationManager.evaluateVipLevel(1L, 1L);

      verify(vipLevelManager).updateVipLevel(eq(player), eq(VipLevelEnum.PLATINUM), anyString());
    }
  }

  private PlayerEntity buildPlayer(VipLevelEnum vipLevel) {
    PlayerEntity player = new PlayerEntity();
    player.setPlayerId(1L);
    player.setUsername("testplayer");
    player.setVipLevel(vipLevel.getValue());
    player.setDeleted(false);
    return player;
  }
}
