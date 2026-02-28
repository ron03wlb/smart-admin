package net.lab1024.sa.app.igaming.config;

import java.util.function.Supplier;
import net.lab1024.sa.common.redislock.LockService;
import net.lab1024.sa.igaming.common.config.IgamingProperties;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Shared mock beans for iGaming integration tests.
 *
 * <p>Provides a pass-through {@link LockService} that executes the business logic supplier/runnable
 * directly without acquiring a distributed lock. Integration tests run single-threaded, so real
 * distributed locking is unnecessary.
 *
 * @author iGaming Team
 * @since 2026-02-28
 */
@Configuration
public class IntegrationTestMockBeans {

  @Bean
  public IgamingProperties igamingProperties() {
    return new IgamingProperties();
  }

  @Bean
  public LockService lockService() {
    LockService mock = Mockito.mock(LockService.class);

    // Pass-through: executeWithLock(String, long, long, Supplier<T>) → supplier.get()
    Mockito.when(
            mock.executeWithLock(
                Mockito.anyString(),
                Mockito.anyLong(),
                Mockito.anyLong(),
                Mockito.<Supplier<?>>any()))
        .thenAnswer(invocation -> invocation.getArgument(3, Supplier.class).get());

    // Pass-through: executeWithLock(String, long, long, Runnable) → runnable.run()
    Mockito.doAnswer(
            invocation -> {
              invocation.getArgument(3, Runnable.class).run();
              return null;
            })
        .when(mock)
        .executeWithLock(
            Mockito.anyString(), Mockito.anyLong(), Mockito.anyLong(), Mockito.any(Runnable.class));

    return mock;
  }
}
