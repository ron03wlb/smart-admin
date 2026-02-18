package net.lab1024.sa.app.igaming.wallet.payment;

import static org.assertj.core.api.Assertions.assertThat;

import io.vavr.control.Option;
import java.util.List;
import net.lab1024.sa.igaming.wallet.payment.psp.PaymentProviderAdapter;
import net.lab1024.sa.igaming.wallet.payment.psp.PspAdapterFactory;
import net.lab1024.sa.igaming.wallet.payment.psp.mock.MockPspAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * PspAdapterFactory unit tests.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@DisplayName("PspAdapterFactory 單元測試")
class PspAdapterFactoryTest {

  private PspAdapterFactory factory;

  @BeforeEach
  void setUp() {
    factory = new PspAdapterFactory(List.of(new MockPspAdapter()));
  }

  @Test
  @DisplayName("getAdapter 存在的 PSP 返回 Some")
  void getAdapter_exists() {
    Option<PaymentProviderAdapter> result = factory.getAdapter("mock");
    assertThat(result.isDefined()).isTrue();
    assertThat(result.get().getPspCode()).isEqualTo("mock");
  }

  @Test
  @DisplayName("getAdapter 不存在的 PSP 返回 None")
  void getAdapter_notFound() {
    Option<PaymentProviderAdapter> result = factory.getAdapter("nonexistent");
    assertThat(result.isEmpty()).isTrue();
  }
}
