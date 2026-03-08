package net.lab1024.sa.app.igaming.player;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.Optional;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.mq.kafka.constant.IgamingKafkaConst;
import net.lab1024.sa.common.mq.kafka.event.DomainEvent;
import net.lab1024.sa.common.mq.kafka.event.DomainEventPublisher;
import net.lab1024.sa.igaming.common.constant.PlayerStatusEnum;
import net.lab1024.sa.igaming.player.dao.PlayerAuditLogDao;
import net.lab1024.sa.igaming.player.dao.PlayerDao;
import net.lab1024.sa.igaming.player.domain.entity.PlayerAuditLogEntity;
import net.lab1024.sa.igaming.player.domain.entity.PlayerEntity;
import net.lab1024.sa.igaming.player.manager.PlayerStateManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * PlayerStateManager unit tests.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PlayerStateManager 單元測試")
class PlayerStateManagerTest {

  @Mock private PlayerDao playerDao;
  @Mock private PlayerAuditLogDao playerAuditLogDao;
  @Mock private DomainEventPublisher domainEventPublisher;
  private PlayerStateManager playerStateManager;

  @BeforeEach
  void setUp() {
    playerStateManager =
        new PlayerStateManager(playerDao, playerAuditLogDao, Optional.of(domainEventPublisher));
  }

  // ==================== Valid Transitions ====================

  @Nested
  @DisplayName("合法狀態轉換")
  class ValidTransitionsTest {

    @Test
    @DisplayName("ACTIVE → LOCKED 成功")
    void active_to_locked() {
      PlayerEntity player = buildPlayer(PlayerStatusEnum.ACTIVE);
      when(playerDao.updateById(any(PlayerEntity.class))).thenReturn(1);
      when(playerAuditLogDao.insert(any(PlayerAuditLogEntity.class))).thenReturn(1);

      ResponseDTO<Void> result =
          playerStateManager.transitionStatus(
              player, PlayerStatusEnum.LOCKED, "admin", "suspicious activity");

      assertThat(result.getOk()).isTrue();
      verify(playerDao).updateById(player);
      verify(playerAuditLogDao).insert(any(PlayerAuditLogEntity.class));
      verify(domainEventPublisher)
          .publish(eq(IgamingKafkaConst.Topic.PLAYER_EVENTS), any(DomainEvent.class));
    }

    @Test
    @DisplayName("ACTIVE → SUSPENDED 成功")
    void active_to_suspended() {
      PlayerEntity player = buildPlayer(PlayerStatusEnum.ACTIVE);
      when(playerDao.updateById(any(PlayerEntity.class))).thenReturn(1);
      when(playerAuditLogDao.insert(any(PlayerAuditLogEntity.class))).thenReturn(1);

      ResponseDTO<Void> result =
          playerStateManager.transitionStatus(
              player, PlayerStatusEnum.SUSPENDED, "admin", "rule violation");

      assertThat(result.getOk()).isTrue();
    }

    @Test
    @DisplayName("ACTIVE → CLOSED 成功")
    void active_to_closed() {
      PlayerEntity player = buildPlayer(PlayerStatusEnum.ACTIVE);
      when(playerDao.updateById(any(PlayerEntity.class))).thenReturn(1);
      when(playerAuditLogDao.insert(any(PlayerAuditLogEntity.class))).thenReturn(1);

      ResponseDTO<Void> result =
          playerStateManager.transitionStatus(
              player, PlayerStatusEnum.CLOSED, "admin", "account closure");

      assertThat(result.getOk()).isTrue();
    }

    @Test
    @DisplayName("LOCKED → ACTIVE 成功")
    void locked_to_active() {
      PlayerEntity player = buildPlayer(PlayerStatusEnum.LOCKED);
      when(playerDao.updateById(any(PlayerEntity.class))).thenReturn(1);
      when(playerAuditLogDao.insert(any(PlayerAuditLogEntity.class))).thenReturn(1);

      ResponseDTO<Void> result =
          playerStateManager.transitionStatus(
              player, PlayerStatusEnum.ACTIVE, "admin", "issue resolved");

      assertThat(result.getOk()).isTrue();
    }

    @Test
    @DisplayName("ACTIVE → SELF_EXCLUDED 成功")
    void active_to_selfExcluded() {
      PlayerEntity player = buildPlayer(PlayerStatusEnum.ACTIVE);
      when(playerDao.updateById(any(PlayerEntity.class))).thenReturn(1);
      when(playerAuditLogDao.insert(any(PlayerAuditLogEntity.class))).thenReturn(1);

      ResponseDTO<Void> result =
          playerStateManager.transitionStatus(
              player, PlayerStatusEnum.SELF_EXCLUDED, "player", "self-exclusion request");

      assertThat(result.getOk()).isTrue();
      verify(domainEventPublisher)
          .publish(eq(IgamingKafkaConst.Topic.PLAYER_EVENTS), any(DomainEvent.class));
    }

    @Test
    @DisplayName("PENDING_VERIFICATION → ACTIVE 成功")
    void pending_to_active() {
      PlayerEntity player = buildPlayer(PlayerStatusEnum.PENDING_VERIFICATION);
      when(playerDao.updateById(any(PlayerEntity.class))).thenReturn(1);
      when(playerAuditLogDao.insert(any(PlayerAuditLogEntity.class))).thenReturn(1);

      ResponseDTO<Void> result =
          playerStateManager.transitionStatus(
              player, PlayerStatusEnum.ACTIVE, "system", "verification complete");

      assertThat(result.getOk()).isTrue();
    }
  }

  // ==================== Invalid Transitions ====================

  @Nested
  @DisplayName("非法狀態轉換")
  class InvalidTransitionsTest {

    @Test
    @DisplayName("LOCKED → CLOSED 拒絕")
    void locked_to_closed_rejected() {
      PlayerEntity player = buildPlayer(PlayerStatusEnum.LOCKED);

      ResponseDTO<Void> result =
          playerStateManager.transitionStatus(
              player, PlayerStatusEnum.CLOSED, "admin", "attempted closure");

      assertThat(result.getOk()).isFalse();
      verify(playerDao, never()).updateById(any(PlayerEntity.class));
    }

    @Test
    @DisplayName("CLOSED → ACTIVE 拒絕 (CLOSED 是終態)")
    void closed_to_active_rejected() {
      PlayerEntity player = buildPlayer(PlayerStatusEnum.CLOSED);

      ResponseDTO<Void> result =
          playerStateManager.transitionStatus(
              player, PlayerStatusEnum.ACTIVE, "admin", "reopen attempt");

      assertThat(result.getOk()).isFalse();
      verify(playerDao, never()).updateById(any(PlayerEntity.class));
    }

    @Test
    @DisplayName("SELF_EXCLUDED → ACTIVE 拒絕 (SELF_EXCLUDED 是終態)")
    void selfExcluded_to_active_rejected() {
      PlayerEntity player = buildPlayer(PlayerStatusEnum.SELF_EXCLUDED);

      ResponseDTO<Void> result =
          playerStateManager.transitionStatus(
              player, PlayerStatusEnum.ACTIVE, "admin", "reopen attempt");

      assertThat(result.getOk()).isFalse();
      verify(playerDao, never()).updateById(any(PlayerEntity.class));
    }

    @Test
    @DisplayName("SUSPENDED → LOCKED 拒絕")
    void suspended_to_locked_rejected() {
      PlayerEntity player = buildPlayer(PlayerStatusEnum.SUSPENDED);

      ResponseDTO<Void> result =
          playerStateManager.transitionStatus(
              player, PlayerStatusEnum.LOCKED, "admin", "lock attempt");

      assertThat(result.getOk()).isFalse();
      verify(playerDao, never()).updateById(any(PlayerEntity.class));
    }
  }

  // ==================== helpers ====================

  private PlayerEntity buildPlayer(PlayerStatusEnum status) {
    PlayerEntity player = new PlayerEntity();
    player.setPlayerId(1L);
    player.setUsername("testplayer");
    player.setStatus(status.getValue());
    player.setDeleted(false);
    return player;
  }
}
