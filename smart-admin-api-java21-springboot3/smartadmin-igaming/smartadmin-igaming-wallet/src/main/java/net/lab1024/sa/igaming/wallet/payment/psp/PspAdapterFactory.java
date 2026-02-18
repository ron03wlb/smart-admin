package net.lab1024.sa.igaming.wallet.payment.psp;

import io.vavr.control.Option;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * PSP Adapter Factory — resolves adapter by pspCode.
 *
 * <p>Spring auto-discovers all {@link PaymentProviderAdapter} beans. Factory maps pspCode to
 * adapter instance.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Component
public class PspAdapterFactory {

  private final Map<String, PaymentProviderAdapter> adapterMap;

  public PspAdapterFactory(List<PaymentProviderAdapter> adapters) {
    this.adapterMap =
        adapters.stream()
            .collect(Collectors.toMap(PaymentProviderAdapter::getPspCode, Function.identity()));
  }

  /**
   * Get adapter by PSP code.
   *
   * @param pspCode PSP identifier (e.g., "stripe", "mock")
   * @return Option.some(adapter) or Option.none() if not registered
   */
  public Option<PaymentProviderAdapter> getAdapter(String pspCode) {
    return Option.of(adapterMap.get(pspCode));
  }
}
