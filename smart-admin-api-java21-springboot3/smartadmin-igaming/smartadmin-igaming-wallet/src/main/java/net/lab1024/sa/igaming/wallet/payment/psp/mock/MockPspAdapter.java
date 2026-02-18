package net.lab1024.sa.igaming.wallet.payment.psp.mock;

import io.vavr.control.Either;
import io.vavr.control.Option;
import java.util.UUID;
import net.lab1024.sa.igaming.wallet.payment.domain.dto.PspDepositRequest;
import net.lab1024.sa.igaming.wallet.payment.domain.dto.PspDepositResponse;
import net.lab1024.sa.igaming.wallet.payment.domain.dto.PspQueryResponse;
import net.lab1024.sa.igaming.wallet.payment.domain.dto.PspWithdrawRequest;
import net.lab1024.sa.igaming.wallet.payment.domain.dto.PspWithdrawResponse;
import net.lab1024.sa.igaming.wallet.payment.psp.PaymentProviderAdapter;
import org.springframework.stereotype.Component;

/**
 * Mock PSP Adapter for integration tests and POC.
 *
 * <p>Simulates PSP behavior:
 *
 * <ul>
 *   <li>Amounts ending in .01 → simulate failure
 *   <li>Amounts ending in .02 → simulate timeout (delay 10s)
 *   <li>All other amounts → simulate success
 * </ul>
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Component
public class MockPspAdapter implements PaymentProviderAdapter {

  @Override
  public String getPspCode() {
    return "mock";
  }

  @Override
  public Either<String, PspDepositResponse> deposit(PspDepositRequest request) {
    String amountStr = request.getAmount().toPlainString();

    if (amountStr.endsWith(".01") || amountStr.endsWith(".0100")) {
      return Either.left("MOCK_DECLINED: Insufficient funds");
    }

    if (amountStr.endsWith(".02") || amountStr.endsWith(".0200")) {
      try {
        Thread.sleep(10_000);
      } catch (InterruptedException ignored) {
        Thread.currentThread().interrupt();
      }
      return Either.left("MOCK_TIMEOUT: PSP response timeout");
    }

    return Either.right(
        PspDepositResponse.builder()
            .pspTransactionId("mock_dep_" + UUID.randomUUID())
            .redirectUrl("https://mock-psp.local/pay/" + request.getOrderId())
            .status("PENDING")
            .build());
  }

  @Override
  public Either<String, PspWithdrawResponse> withdraw(PspWithdrawRequest request) {
    String amountStr = request.getAmount().toPlainString();

    if (amountStr.endsWith(".01") || amountStr.endsWith(".0100")) {
      return Either.left("MOCK_DECLINED: Withdrawal rejected");
    }

    return Either.right(
        PspWithdrawResponse.builder()
            .pspTransactionId("mock_wd_" + UUID.randomUUID())
            .status("PROCESSING")
            .build());
  }

  @Override
  public Option<PspQueryResponse> queryStatus(String pspTransactionId) {
    return Option.some(
        PspQueryResponse.builder().pspTransactionId(pspTransactionId).status("SUCCESS").build());
  }

  @Override
  public boolean verifyCallback(String payload, String signature, long timestamp) {
    return "mock-test-signature".equals(signature) || signature.startsWith("mock_");
  }
}
