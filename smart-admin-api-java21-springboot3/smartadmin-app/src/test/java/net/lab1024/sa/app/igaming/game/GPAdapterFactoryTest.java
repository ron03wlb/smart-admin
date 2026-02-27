package net.lab1024.sa.app.igaming.game;

import static org.assertj.core.api.Assertions.assertThat;

import io.vavr.control.Option;
import java.util.List;
import net.lab1024.sa.igaming.game.adapter.GPAdapterFactory;
import net.lab1024.sa.igaming.game.adapter.GameProviderAdapter;
import net.lab1024.sa.igaming.game.adapter.mock.MockGameProviderAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * GPAdapterFactory unit tests.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GPAdapterFactory 單元測試")
class GPAdapterFactoryTest {

  @Mock private MockGameProviderAdapter mockAdapter;
  private GPAdapterFactory factory;

  @BeforeEach
  void setUp() {
    org.mockito.Mockito.lenient().when(mockAdapter.getProviderCode()).thenReturn("mock");
    factory = new GPAdapterFactory(List.of(mockAdapter));
  }

  @Test
  @DisplayName("getAdapter 存在的 GP 返回 Some")
  void getAdapter_exists() {
    Option<GameProviderAdapter> result = factory.getAdapter("mock");
    assertThat(result.isDefined()).isTrue();
    assertThat(result.get().getProviderCode()).isEqualTo("mock");
  }

  @Test
  @DisplayName("getAdapter 不存在的 GP 返回 None")
  void getAdapter_notFound() {
    Option<GameProviderAdapter> result = factory.getAdapter("nonexistent");
    assertThat(result.isEmpty()).isTrue();
  }
}
