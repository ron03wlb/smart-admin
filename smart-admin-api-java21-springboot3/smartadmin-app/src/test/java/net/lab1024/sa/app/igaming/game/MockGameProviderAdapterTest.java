package net.lab1024.sa.app.igaming.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.vavr.control.Try;
import java.math.BigDecimal;
import net.lab1024.sa.igaming.game.adapter.mock.MockGameProviderAdapter;
import net.lab1024.sa.igaming.game.domain.dto.BetRequest;
import net.lab1024.sa.igaming.game.domain.dto.BetResult;
import net.lab1024.sa.igaming.game.domain.dto.RollbackRequest;
import net.lab1024.sa.igaming.game.domain.dto.RollbackResult;
import net.lab1024.sa.igaming.game.domain.dto.SettleRequest;
import net.lab1024.sa.igaming.game.domain.dto.SettleResult;
import net.lab1024.sa.igaming.wallet.dao.WalletDao;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * MockGameProviderAdapter unit tests.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MockGameProviderAdapter 單元測試")
class MockGameProviderAdapterTest {

  @Mock private WalletDao walletDao;
  @InjectMocks private MockGameProviderAdapter adapter;

  @Nested
  @DisplayName("bet 下注")
  class BetTest {

    @Test
    @DisplayName("正常金額 — 成功")
    void bet_success() {
      BetRequest req = new BetRequest();
      req.setTransactionId("tx1");
      req.setRoundId("round1");
      req.setAmount(new BigDecimal("10.00"));

      Try<BetResult> result = adapter.bet(req);
      assertThat(result.isSuccess()).isTrue();
      assertThat(result.get().getStatus()).isEqualTo("OK");
    }

    @Test
    @DisplayName("金額 .01 結尾 — 模擬失敗")
    void bet_simulatedFailure() {
      BetRequest req = new BetRequest();
      req.setTransactionId("tx2");
      req.setRoundId("round2");
      req.setAmount(new BigDecimal("10.01"));

      Try<BetResult> result = adapter.bet(req);
      assertThat(result.isFailure()).isTrue();
    }
  }

  @Nested
  @DisplayName("settle 結算")
  class SettleTest {

    @Test
    @DisplayName("正常金額 — 成功")
    void settle_success() {
      SettleRequest req = new SettleRequest();
      req.setTransactionId("tx3");
      req.setRoundId("round3");
      req.setPayoutAmount(new BigDecimal("20.00"));

      Try<SettleResult> result = adapter.settle(req);
      assertThat(result.isSuccess()).isTrue();
      assertThat(result.get().getStatus()).isEqualTo("OK");
    }
  }

  @Nested
  @DisplayName("rollback 取消")
  class RollbackTest {

    @Test
    @DisplayName("rollback — 成功")
    void rollback_success() {
      RollbackRequest req = new RollbackRequest();
      req.setOriginalTransactionId("tx1");

      Try<RollbackResult> result = adapter.rollback(req);
      assertThat(result.isSuccess()).isTrue();
      assertThat(result.get().getStatus()).isEqualTo("OK");
    }
  }

  @Nested
  @DisplayName("getBalance 餘額查詢")
  class GetBalanceTest {

    @Test
    @DisplayName("有錢包 — 返回可用餘額")
    @SuppressWarnings("unchecked")
    void getBalance_withWallet() {
      WalletEntity wallet = new WalletEntity();
      wallet.setBalance(new BigDecimal("100.00"));
      wallet.setLockedAmount(new BigDecimal("20.00"));
      when(walletDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(wallet);

      Try<BigDecimal> result = adapter.getBalance(1L, 1L);
      assertThat(result.isSuccess()).isTrue();
      assertThat(result.get()).isEqualByComparingTo("80.00");
    }

    @Test
    @DisplayName("無錢包 — 返回零")
    @SuppressWarnings("unchecked")
    void getBalance_noWallet() {
      when(walletDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

      Try<BigDecimal> result = adapter.getBalance(1L, 1L);
      assertThat(result.isSuccess()).isTrue();
      assertThat(result.get()).isEqualByComparingTo("0");
    }
  }
}
