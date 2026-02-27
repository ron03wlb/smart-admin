package net.lab1024.sa.igaming.game.adapter;

import io.vavr.control.Option;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * GP Adapter Factory — resolves adapter by provider code.
 *
 * <p>Spring auto-discovers all {@link GameProviderAdapter} beans. Factory maps providerCode to
 * adapter instance.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Component
public class GPAdapterFactory {

  private final Map<String, GameProviderAdapter> adapterMap;

  public GPAdapterFactory(List<GameProviderAdapter> adapters) {
    this.adapterMap =
        adapters.stream()
            .collect(Collectors.toMap(GameProviderAdapter::getProviderCode, Function.identity()));
  }

  /**
   * Get adapter by provider code.
   *
   * @param providerCode GP identifier (e.g., "pgsoft", "mock")
   * @return Option.some(adapter) or Option.none() if not registered
   */
  public Option<GameProviderAdapter> getAdapter(String providerCode) {
    return Option.of(adapterMap.get(providerCode));
  }
}
