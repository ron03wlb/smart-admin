package net.lab1024.sa.igaming.player.manager;

import lombok.RequiredArgsConstructor;
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
    return player;
  }
}
