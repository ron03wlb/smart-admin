package net.lab1024.sa.igaming.game.adapter;

import io.vavr.control.Try;
import java.math.BigDecimal;
import net.lab1024.sa.igaming.game.domain.dto.BetRequest;
import net.lab1024.sa.igaming.game.domain.dto.BetResult;
import net.lab1024.sa.igaming.game.domain.dto.GameRoundDetail;
import net.lab1024.sa.igaming.game.domain.dto.RollbackRequest;
import net.lab1024.sa.igaming.game.domain.dto.RollbackResult;
import net.lab1024.sa.igaming.game.domain.dto.SettleRequest;
import net.lab1024.sa.igaming.game.domain.dto.SettleResult;

/**
 * Game provider adapter interface — Strategy pattern for GP integration.
 *
 * <p>Each GP implements this interface. Spring auto-discovers all implementations. Use {@link
 * GPAdapterFactory} for O(1) lookup by provider code.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
public interface GameProviderAdapter {

  Try<String> authenticate(Long playerId, Long tenantId);

  Try<BetResult> bet(BetRequest request);

  Try<SettleResult> settle(SettleRequest request);

  Try<RollbackResult> rollback(RollbackRequest request);

  Try<BigDecimal> getBalance(Long playerId, Long tenantId);

  Try<GameRoundDetail> queryRound(String roundId);

  String getProviderCode();
}
