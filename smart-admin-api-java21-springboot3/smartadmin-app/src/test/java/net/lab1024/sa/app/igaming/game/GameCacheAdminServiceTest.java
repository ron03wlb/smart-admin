package net.lab1024.sa.app.igaming.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.igaming.game.domain.vo.GameVO;
import net.lab1024.sa.igaming.game.manager.GameCacheManager;
import net.lab1024.sa.igaming.game.service.GameCacheAdminService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * GameCacheAdminService unit tests.
 *
 * @author iGaming Team
 * @since 2026-03-01
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GameCacheAdminService 單元測試")
class GameCacheAdminServiceTest {

  @Mock private GameCacheManager gameCacheManager;
  @InjectMocks private GameCacheAdminService service;

  private MockedStatic<TenantContext> tenantContextMock;

  @BeforeEach
  void setUp() {
    tenantContextMock = Mockito.mockStatic(TenantContext.class);
    tenantContextMock.when(TenantContext::getTenantId).thenReturn(1L);
  }

  @AfterEach
  void tearDown() {
    tenantContextMock.close();
  }

  @Test
  @DisplayName("getGameList — 成功返回快取遊戲列表")
  void getGameList_success() {
    GameVO vo = new GameVO();
    vo.setGameId(1L);
    vo.setGameName("Test Slot");
    when(gameCacheManager.getGameListByTenant(1L)).thenReturn(List.of(vo));

    ResponseDTO<List<GameVO>> result = service.getGameList();

    assertThat(result.getOk()).isTrue();
    assertThat(result.getData()).hasSize(1);
    assertThat(result.getData().get(0).getGameName()).isEqualTo("Test Slot");
  }

  @Test
  @DisplayName("getGameList — Tenant 為空返回錯誤")
  void getGameList_tenantNull() {
    tenantContextMock.when(TenantContext::getTenantId).thenReturn(null);

    ResponseDTO<List<GameVO>> result = service.getGameList();

    assertThat(result.getOk()).isFalse();
  }

  @Test
  @DisplayName("getGamesByCategory — 成功返回分類遊戲列表")
  void getGamesByCategory_success() {
    GameVO vo = new GameVO();
    vo.setGameId(2L);
    vo.setCategory(1);
    when(gameCacheManager.getGamesByCategory(1L, 1)).thenReturn(List.of(vo));

    ResponseDTO<List<GameVO>> result = service.getGamesByCategory(1);

    assertThat(result.getOk()).isTrue();
    assertThat(result.getData()).hasSize(1);
  }

  @Test
  @DisplayName("evictCache — 成功清除快取")
  void evictCache_success() {
    ResponseDTO<String> result = service.evictCache();

    assertThat(result.getOk()).isTrue();
    verify(gameCacheManager).evictGameCache(1L);
  }
}
