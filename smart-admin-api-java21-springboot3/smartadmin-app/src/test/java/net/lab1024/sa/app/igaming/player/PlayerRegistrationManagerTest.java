package net.lab1024.sa.app.igaming.player;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import net.lab1024.sa.common.mq.kafka.constant.IgamingKafkaConst;
import net.lab1024.sa.common.mq.kafka.event.DomainEvent;
import net.lab1024.sa.common.mq.kafka.event.DomainEventPublisher;
import net.lab1024.sa.igaming.player.dao.PlayerDao;
import net.lab1024.sa.igaming.player.domain.entity.PlayerEntity;
import net.lab1024.sa.igaming.player.manager.PlayerRegistrationManager;
import net.lab1024.sa.igaming.wallet.dao.WalletDao;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * PlayerRegistrationManager unit tests.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PlayerRegistrationManager 單元測試")
class PlayerRegistrationManagerTest {

  @Mock private PlayerDao playerDao;
  @Mock private WalletDao walletDao;
  @Mock private DomainEventPublisher domainEventPublisher;
  @InjectMocks private PlayerRegistrationManager playerRegistrationManager;

  @Test
  @DisplayName("成功 — 建立玩家 + 錢包 + 發佈 PLAYER_REGISTERED 事件")
  void registerPlayer_success() {
    PlayerEntity player = new PlayerEntity();
    player.setUsername("newplayer");

    WalletEntity wallet = new WalletEntity();
    wallet.setBalance(BigDecimal.ZERO);

    // MyBatis-Plus insert sets the generated ID via reflection
    doAnswer(
            inv -> {
              PlayerEntity p = inv.getArgument(0);
              p.setPlayerId(100L);
              return 1;
            })
        .when(playerDao)
        .insert(any(PlayerEntity.class));

    PlayerEntity result = playerRegistrationManager.registerPlayer(player, wallet);

    assertThat(result.getPlayerId()).isEqualTo(100L);
    assertThat(wallet.getPlayerId()).isEqualTo(100L);
    verify(playerDao).insert(any(PlayerEntity.class));
    verify(walletDao).insert(any(WalletEntity.class));
    verify(domainEventPublisher)
        .publish(eq(IgamingKafkaConst.Topic.PLAYER_EVENTS), any(DomainEvent.class));
  }
}
