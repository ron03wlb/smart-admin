package net.lab1024.sa.igaming.player.manager;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.mq.kafka.constant.IgamingKafkaConst;
import net.lab1024.sa.common.mq.kafka.event.DomainEvent;
import net.lab1024.sa.common.mq.kafka.event.DomainEventPublisher;
import net.lab1024.sa.igaming.common.constant.DomainEventTypeConst;
import net.lab1024.sa.igaming.player.dao.PlayerDao;
import net.lab1024.sa.igaming.player.domain.entity.PlayerEntity;
import net.lab1024.sa.igaming.wallet.dao.WalletDao;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Player Registration Manager — handles cross-table registration transaction.
 *
 * <p>Atomically creates both t_player and t_wallet records. All public methods MUST be annotated
 * with {@code @Transactional(rollbackFor = Throwable.class)} per SmartAdmin architecture rules.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Service
@RequiredArgsConstructor
public class PlayerRegistrationManager {

  private final PlayerDao playerDao;
  private final WalletDao walletDao;
  private final DomainEventPublisher domainEventPublisher;

  /**
   * Register a player and create their wallet atomically.
   *
   * @param player player entity to insert
   * @param wallet wallet entity to insert (playerId will be set after player insert)
   * @return the inserted player entity with generated ID
   */
  @Transactional(rollbackFor = Throwable.class)
  public PlayerEntity registerPlayer(PlayerEntity player, WalletEntity wallet) {
    playerDao.insert(player);
    wallet.setPlayerId(player.getPlayerId());
    walletDao.insert(wallet);

    publishPlayerRegistered(player);

    return player;
  }

  @SuppressWarnings("FutureReturnValueIgnored")
  private void publishPlayerRegistered(PlayerEntity player) {
    ObjectNode payload = JsonNodeFactory.instance.objectNode();
    payload.put("playerId", player.getPlayerId());
    payload.put("username", player.getUsername());
    domainEventPublisher.publish(
        IgamingKafkaConst.Topic.PLAYER_EVENTS,
        DomainEvent.builder()
            .eventType(DomainEventTypeConst.PLAYER_REGISTERED)
            .aggregateType("Player")
            .aggregateId(String.valueOf(player.getPlayerId()))
            .payload(payload)
            .build());
  }
}
