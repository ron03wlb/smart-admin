package net.lab1024.sa.igaming.game.adapter.mock;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.vavr.control.Try;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.igaming.common.constant.WalletTypeEnum;
import net.lab1024.sa.igaming.game.adapter.GameProviderAdapter;
import net.lab1024.sa.igaming.game.domain.dto.BetRequest;
import net.lab1024.sa.igaming.game.domain.dto.BetResult;
import net.lab1024.sa.igaming.game.domain.dto.GameRoundDetail;
import net.lab1024.sa.igaming.game.domain.dto.RollbackRequest;
import net.lab1024.sa.igaming.game.domain.dto.RollbackResult;
import net.lab1024.sa.igaming.game.domain.dto.SettleRequest;
import net.lab1024.sa.igaming.game.domain.dto.SettleResult;
import net.lab1024.sa.igaming.wallet.dao.WalletDao;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletEntity;
import org.springframework.stereotype.Component;

/**
 * Mock GP adapter for POC and testing.
 *
 * <p>Simulates GP behavior locally. Amounts ending in {@code .01} trigger failure (matching
 * MockPspAdapter pattern).
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MockGameProviderAdapter implements GameProviderAdapter {

  private final WalletDao walletDao;

  @Override
  public String getProviderCode() {
    return "mock";
  }

  @Override
  public Try<String> authenticate(Long playerId, Long tenantId) {
    return Try.success("mock_token_" + playerId);
  }

  @Override
  public Try<BetResult> bet(BetRequest request) {
    if (shouldSimulateFailure(request.getAmount())) {
      return Try.failure(new RuntimeException("Mock GP: simulated bet failure"));
    }
    BetResult result = new BetResult();
    result.setTransactionRef("mock_bet_" + request.getTransactionId());
    result.setRoundId(request.getRoundId());
    result.setAmount(request.getAmount());
    result.setStatus("OK");
    return Try.success(result);
  }

  @Override
  public Try<SettleResult> settle(SettleRequest request) {
    if (shouldSimulateFailure(request.getPayoutAmount())) {
      return Try.failure(new RuntimeException("Mock GP: simulated settle failure"));
    }
    SettleResult result = new SettleResult();
    result.setTransactionRef("mock_settle_" + request.getTransactionId());
    result.setRoundId(request.getRoundId());
    result.setPayoutAmount(request.getPayoutAmount());
    result.setStatus("OK");
    return Try.success(result);
  }

  @Override
  public Try<RollbackResult> rollback(RollbackRequest request) {
    RollbackResult result = new RollbackResult();
    result.setOriginalTransactionId(request.getOriginalTransactionId());
    result.setStatus("OK");
    return Try.success(result);
  }

  @Override
  public Try<BigDecimal> getBalance(Long playerId, Long tenantId) {
    return Try.of(
        () -> {
          WalletEntity wallet =
              walletDao.selectOne(
                  Wrappers.<WalletEntity>lambdaQuery()
                      .eq(WalletEntity::getPlayerId, playerId)
                      .eq(WalletEntity::getWalletType, WalletTypeEnum.CASH.getValue())
                      .eq(WalletEntity::getDeleted, false));
          if (wallet == null) {
            return BigDecimal.ZERO;
          }
          return wallet.getBalance().subtract(wallet.getLockedAmount());
        });
  }

  @Override
  public Try<GameRoundDetail> queryRound(String roundId) {
    GameRoundDetail detail = new GameRoundDetail();
    detail.setRoundId(roundId);
    detail.setStatus("COMPLETED");
    detail.setBetAmount(BigDecimal.TEN);
    detail.setPayoutAmount(BigDecimal.TEN);
    return Try.success(detail);
  }

  private boolean shouldSimulateFailure(BigDecimal amount) {
    return amount != null
        && amount.remainder(BigDecimal.ONE).compareTo(new BigDecimal("0.01")) == 0;
  }
}
