package net.lab1024.sa.app.igaming.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.vavr.control.Option;
import java.util.List;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.game.dao.GameProviderDao;
import net.lab1024.sa.igaming.game.domain.entity.GameProviderEntity;
import net.lab1024.sa.igaming.game.domain.form.GameProviderAddForm;
import net.lab1024.sa.igaming.game.domain.form.GameProviderQueryForm;
import net.lab1024.sa.igaming.game.domain.form.GameProviderUpdateForm;
import net.lab1024.sa.igaming.game.domain.vo.GameProviderVO;
import net.lab1024.sa.igaming.game.manager.GameCacheManager;
import net.lab1024.sa.igaming.game.service.GameProviderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * GameProviderService unit tests.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GameProviderService 單元測試")
class GameProviderServiceTest {

  @Mock private GameProviderDao gameProviderDao;
  @Mock private GameCacheManager gameCacheManager;
  @InjectMocks private GameProviderService gameProviderService;

  @Nested
  @DisplayName("getProvider 查詢供應商")
  class GetProviderTest {

    @Test
    @DisplayName("存在且未刪除 — 返回 Some")
    void getProvider_exists() {
      GameProviderEntity entity = buildProvider();
      when(gameProviderDao.selectById(1L)).thenReturn(entity);

      Option<GameProviderVO> result = gameProviderService.getProvider(1L);
      assertThat(result.isDefined()).isTrue();
      assertThat(result.get().getProviderCode()).isEqualTo("mock");
    }

    @Test
    @DisplayName("不存在 — 返回 None")
    void getProvider_notFound() {
      when(gameProviderDao.selectById(99L)).thenReturn(null);

      Option<GameProviderVO> result = gameProviderService.getProvider(99L);
      assertThat(result.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("已刪除 — 返回 None")
    void getProvider_deleted() {
      GameProviderEntity entity = buildProvider();
      entity.setDeleted(true);
      when(gameProviderDao.selectById(1L)).thenReturn(entity);

      Option<GameProviderVO> result = gameProviderService.getProvider(1L);
      assertThat(result.isEmpty()).isTrue();
    }
  }

  @Nested
  @DisplayName("queryProviders 分頁查詢")
  class QueryProvidersTest {

    @Test
    @DisplayName("分頁查詢 — 返回成功")
    void queryProviders_success() {
      GameProviderQueryForm form = new GameProviderQueryForm();
      form.setPageNum(1L);
      form.setPageSize(10L);

      GameProviderVO vo = new GameProviderVO();
      vo.setProviderCode("mock");
      when(gameProviderDao.queryPage(any(Page.class), any())).thenReturn(List.of(vo));

      ResponseDTO<PageResult<GameProviderVO>> result = gameProviderService.queryProviders(form);
      assertThat(result.getOk()).isTrue();
    }
  }

  @Nested
  @DisplayName("addProvider 新增供應商")
  class AddProviderTest {

    @Test
    @DisplayName("新增 — 成功")
    void addProvider_success() {
      GameProviderAddForm form = new GameProviderAddForm();
      form.setProviderCode("new-gp");
      form.setProviderName("New GP");
      form.setApiUrl("https://api.newgp.com");
      form.setApiKey("secret-key");

      ResponseDTO<GameProviderVO> result = gameProviderService.addProvider(form);
      assertThat(result.getOk()).isTrue();
      verify(gameProviderDao).insert(any(GameProviderEntity.class));
    }
  }

  @Nested
  @DisplayName("updateProvider 更新供應商")
  class UpdateProviderTest {

    @Test
    @DisplayName("更新存在的供應商 — 成功")
    void updateProvider_success() {
      GameProviderEntity entity = buildProvider();
      when(gameProviderDao.selectById(1L)).thenReturn(entity);

      GameProviderUpdateForm form = new GameProviderUpdateForm();
      form.setProviderId(1L);
      form.setProviderName("Updated Name");

      ResponseDTO<Void> result = gameProviderService.updateProvider(form);
      assertThat(result.getOk()).isTrue();
      verify(gameProviderDao).updateById(entity);
      verify(gameCacheManager).evictGameCache(entity.getTenantId());
    }

    @Test
    @DisplayName("更新不存在的供應商 — 失敗")
    void updateProvider_notFound() {
      when(gameProviderDao.selectById(99L)).thenReturn(null);

      GameProviderUpdateForm form = new GameProviderUpdateForm();
      form.setProviderId(99L);

      ResponseDTO<Void> result = gameProviderService.updateProvider(form);
      assertThat(result.getOk()).isFalse();
    }
  }

  @Nested
  @DisplayName("enable/disable 啟停用")
  class ToggleProviderTest {

    @Test
    @DisplayName("啟用供應商 — 成功")
    void enableProvider_success() {
      GameProviderEntity entity = buildProvider();
      entity.setEnabled(false);
      when(gameProviderDao.selectById(1L)).thenReturn(entity);

      ResponseDTO<Void> result = gameProviderService.enableProvider(1L);
      assertThat(result.getOk()).isTrue();
      assertThat(entity.getEnabled()).isTrue();
    }

    @Test
    @DisplayName("停用供應商 — 成功")
    void disableProvider_success() {
      GameProviderEntity entity = buildProvider();
      when(gameProviderDao.selectById(1L)).thenReturn(entity);

      ResponseDTO<Void> result = gameProviderService.disableProvider(1L);
      assertThat(result.getOk()).isTrue();
      assertThat(entity.getEnabled()).isFalse();
    }
  }

  private GameProviderEntity buildProvider() {
    GameProviderEntity entity = new GameProviderEntity();
    entity.setProviderId(1L);
    entity.setTenantId(1L);
    entity.setProviderCode("mock");
    entity.setProviderName("Mock Provider");
    entity.setApiUrl("https://mock.api.com");
    entity.setEnabled(true);
    entity.setDeleted(false);
    return entity;
  }
}
