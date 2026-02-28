package net.lab1024.sa.app.igaming.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.vavr.control.Option;
import io.vavr.control.Try;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import net.lab1024.sa.igaming.common.constant.HealthStatusEnum;
import net.lab1024.sa.igaming.game.adapter.GPAdapterFactory;
import net.lab1024.sa.igaming.game.adapter.GameProviderAdapter;
import net.lab1024.sa.igaming.game.dao.GameProviderDao;
import net.lab1024.sa.igaming.game.domain.entity.GameProviderEntity;
import net.lab1024.sa.igaming.game.service.GpHealthCheckService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * GpHealthCheckService unit tests — game provider health checking.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GpHealthCheckService 單元測試")
class GpHealthCheckServiceTest {

  @Mock private GameProviderDao gameProviderDao;
  @Mock private GPAdapterFactory gpAdapterFactory;

  @InjectMocks private GpHealthCheckService gpHealthCheckService;

  @Test
  @DisplayName("GP 回報正常 → 狀態 HEALTHY + lastHealthCheck 更新")
  void checkAllProviders_allHealthy() {
    GameProviderEntity provider = buildProvider("GP_A");
    when(gameProviderDao.selectEnabledProviders()).thenReturn(List.of(provider));

    GameProviderAdapter adapter = mock(GameProviderAdapter.class);
    when(gpAdapterFactory.getAdapter("GP_A")).thenReturn(Option.of(adapter));
    when(adapter.getBalance(0L, 0L)).thenReturn(Try.success(new BigDecimal("1000.00")));

    int result = gpHealthCheckService.checkAllProviders();

    assertThat(result).isEqualTo(1);
    assertThat(provider.getHealthStatus()).isEqualTo(HealthStatusEnum.HEALTHY.getValue());
    assertThat(provider.getLastHealthCheck()).isNotNull();
    verify(gameProviderDao).updateById(provider);
  }

  @Test
  @DisplayName("GP 拋出異常 → 狀態 DOWN + lastHealthCheck 更新")
  void checkAllProviders_providerDown() {
    GameProviderEntity provider = buildProvider("GP_B");
    when(gameProviderDao.selectEnabledProviders()).thenReturn(List.of(provider));

    GameProviderAdapter adapter = mock(GameProviderAdapter.class);
    when(gpAdapterFactory.getAdapter("GP_B")).thenReturn(Option.of(adapter));
    when(adapter.getBalance(0L, 0L))
        .thenReturn(Try.failure(new RuntimeException("Connection timeout")));

    int result = gpHealthCheckService.checkAllProviders();

    assertThat(result).isEqualTo(1);
    assertThat(provider.getHealthStatus()).isEqualTo(HealthStatusEnum.DOWN.getValue());
    assertThat(provider.getLastHealthCheck()).isNotNull();
    verify(gameProviderDao).updateById(provider);
  }

  @Test
  @DisplayName("適配器不存在 → 跳過，不更新")
  void checkAllProviders_noAdapter() {
    GameProviderEntity provider = buildProvider("GP_C");
    when(gameProviderDao.selectEnabledProviders()).thenReturn(List.of(provider));
    when(gpAdapterFactory.getAdapter("GP_C")).thenReturn(Option.none());

    int result = gpHealthCheckService.checkAllProviders();

    assertThat(result).isEqualTo(1);
    verify(gameProviderDao, never()).updateById(any(GameProviderEntity.class));
  }

  @Test
  @DisplayName("無啟用供應商 → 返回 0")
  void checkAllProviders_emptyList() {
    when(gameProviderDao.selectEnabledProviders()).thenReturn(Collections.emptyList());

    int result = gpHealthCheckService.checkAllProviders();

    assertThat(result).isEqualTo(0);
    verify(gameProviderDao, never()).updateById(any(GameProviderEntity.class));
  }

  // --- Helpers ---

  private GameProviderEntity buildProvider(String providerCode) {
    GameProviderEntity provider = new GameProviderEntity();
    provider.setProviderCode(providerCode);
    provider.setEnabled(true);
    provider.setTenantId(1L);
    return provider;
  }
}
