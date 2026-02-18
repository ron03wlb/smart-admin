package net.lab1024.sa.app.igaming.wallet.payment;

import static org.assertj.core.api.Assertions.assertThat;

import io.vavr.control.Either;
import io.vavr.control.Option;
import java.math.BigDecimal;
import net.lab1024.sa.igaming.wallet.payment.domain.dto.PspDepositRequest;
import net.lab1024.sa.igaming.wallet.payment.domain.dto.PspDepositResponse;
import net.lab1024.sa.igaming.wallet.payment.domain.dto.PspQueryResponse;
import net.lab1024.sa.igaming.wallet.payment.domain.dto.PspWithdrawRequest;
import net.lab1024.sa.igaming.wallet.payment.domain.dto.PspWithdrawResponse;
import net.lab1024.sa.igaming.wallet.payment.psp.mock.MockPspAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * MockPspAdapter unit tests.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@DisplayName("MockPspAdapter 單元測試")
class MockPspAdapterTest {

  private MockPspAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = new MockPspAdapter();
  }

  @Test
  @DisplayName("getPspCode 返回 mock")
  void getPspCode() {
    assertThat(adapter.getPspCode()).isEqualTo("mock");
  }

  @Nested
  @DisplayName("deposit 存款模擬")
  class DepositTest {

    @Test
    @DisplayName("正常金額 → 成功")
    void deposit_success() {
      PspDepositRequest request =
          PspDepositRequest.builder()
              .orderId("PAY_001")
              .amount(new BigDecimal("100.0000"))
              .currency("USD")
              .build();

      Either<String, PspDepositResponse> result = adapter.deposit(request);

      assertThat(result.isRight()).isTrue();
      assertThat(result.get().getPspTransactionId()).startsWith("mock_dep_");
      assertThat(result.get().getRedirectUrl()).contains("PAY_001");
    }

    @Test
    @DisplayName("金額尾數 .01 → 模擬失敗")
    void deposit_failure_01() {
      PspDepositRequest request =
          PspDepositRequest.builder()
              .orderId("PAY_002")
              .amount(new BigDecimal("100.0100"))
              .currency("USD")
              .build();

      Either<String, PspDepositResponse> result = adapter.deposit(request);

      assertThat(result.isLeft()).isTrue();
      assertThat(result.getLeft()).contains("MOCK_DECLINED");
    }
  }

  @Nested
  @DisplayName("withdraw 提款模擬")
  class WithdrawTest {

    @Test
    @DisplayName("正常金額 → 成功")
    void withdraw_success() {
      PspWithdrawRequest request =
          PspWithdrawRequest.builder()
              .orderId("WD_001")
              .amount(new BigDecimal("50.0000"))
              .currency("USD")
              .build();

      Either<String, PspWithdrawResponse> result = adapter.withdraw(request);

      assertThat(result.isRight()).isTrue();
      assertThat(result.get().getPspTransactionId()).startsWith("mock_wd_");
    }

    @Test
    @DisplayName("金額尾數 .01 → 模擬拒絕")
    void withdraw_declined_01() {
      PspWithdrawRequest request =
          PspWithdrawRequest.builder()
              .orderId("WD_002")
              .amount(new BigDecimal("50.0100"))
              .currency("USD")
              .build();

      Either<String, PspWithdrawResponse> result = adapter.withdraw(request);

      assertThat(result.isLeft()).isTrue();
      assertThat(result.getLeft()).contains("MOCK_DECLINED");
    }
  }

  @Nested
  @DisplayName("queryStatus 狀態查詢")
  class QueryStatusTest {

    @Test
    @DisplayName("查詢返回 SUCCESS")
    void queryStatus_success() {
      Option<PspQueryResponse> result = adapter.queryStatus("mock_dep_123");

      assertThat(result.isDefined()).isTrue();
      assertThat(result.get().getStatus()).isEqualTo("SUCCESS");
    }
  }

  @Nested
  @DisplayName("verifyCallback 回調驗證")
  class VerifyCallbackTest {

    @Test
    @DisplayName("mock-test-signature 通過驗證")
    void verifyCallback_mockSignature() {
      assertThat(adapter.verifyCallback("{}", "mock-test-signature", System.currentTimeMillis()))
          .isTrue();
    }

    @Test
    @DisplayName("mock_ 前綴通過驗證")
    void verifyCallback_mockPrefix() {
      assertThat(adapter.verifyCallback("{}", "mock_abc123", System.currentTimeMillis())).isTrue();
    }

    @Test
    @DisplayName("無效簽名不通過")
    void verifyCallback_invalid() {
      assertThat(adapter.verifyCallback("{}", "invalid", System.currentTimeMillis())).isFalse();
    }
  }
}
