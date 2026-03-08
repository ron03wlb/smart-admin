package net.lab1024.sa.app.igaming.player;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.vavr.control.Option;
import java.util.List;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.mybatis.util.SmartPageUtil;
import net.lab1024.sa.common.security.encrypt.BlindIndexService;
import net.lab1024.sa.igaming.common.constant.PlayerStatusEnum;
import net.lab1024.sa.igaming.common.constant.VipLevelEnum;
import net.lab1024.sa.igaming.player.dao.PlayerDao;
import net.lab1024.sa.igaming.player.domain.entity.PlayerEntity;
import net.lab1024.sa.igaming.player.domain.form.PlayerQueryForm;
import net.lab1024.sa.igaming.player.domain.form.PlayerUpdateForm;
import net.lab1024.sa.igaming.player.domain.vo.PlayerVO;
import net.lab1024.sa.igaming.player.manager.PlayerStateManager;
import net.lab1024.sa.igaming.player.manager.VipLevelManager;
import net.lab1024.sa.igaming.player.service.PlayerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * PlayerService unit tests.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PlayerService 單元測試")
class PlayerServiceTest {

  @Mock private PlayerDao playerDao;
  @Mock private BlindIndexService blindIndexService;
  @Mock private PlayerStateManager playerStateManager;
  @Mock private VipLevelManager vipLevelManager;

  @InjectMocks private PlayerService playerService;

  // ==================== getPlayer ====================

  @Nested
  @DisplayName("getPlayer 取得玩家")
  class GetPlayerTest {

    @Test
    @DisplayName("成功取得玩家")
    void getPlayer_success() {
      PlayerEntity player = buildPlayer();
      when(playerDao.selectById(1L)).thenReturn(player);

      Option<PlayerVO> result = playerService.getPlayer(1L);

      assertThat(result.isDefined()).isTrue();
      assertThat(result.get().getPlayerId()).isEqualTo(1L);
      assertThat(result.get().getUsername()).isEqualTo("testplayer");
    }

    @Test
    @DisplayName("玩家不存在返回 None")
    void getPlayer_notFound() {
      when(playerDao.selectById(999L)).thenReturn(null);

      Option<PlayerVO> result = playerService.getPlayer(999L);

      assertThat(result.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("已刪除玩家返回 None")
    void getPlayer_deleted() {
      PlayerEntity player = buildPlayer();
      player.setDeleted(true);
      when(playerDao.selectById(1L)).thenReturn(player);

      Option<PlayerVO> result = playerService.getPlayer(1L);

      assertThat(result.isEmpty()).isTrue();
    }
  }

  // ==================== queryPlayers ====================

  @Nested
  @DisplayName("queryPlayers 分頁查詢")
  class QueryPlayersTest {

    @Test
    @DisplayName("分頁查詢返回結果")
    void queryPlayers_success() {
      PlayerQueryForm queryForm = new PlayerQueryForm();
      queryForm.setPageNum(1L);
      queryForm.setPageSize(10L);

      PlayerVO vo = new PlayerVO();
      vo.setPlayerId(1L);
      vo.setUsername("player1");

      try (MockedStatic<SmartPageUtil> mocked = mockStatic(SmartPageUtil.class)) {
        Page<Object> page = new Page<>(1, 10);
        page.setTotal(1L);
        mocked.when(() -> SmartPageUtil.convert2PageQuery(queryForm)).thenReturn(page);
        when(playerDao.queryPage(any(Page.class), any(PlayerQueryForm.class)))
            .thenReturn(List.of(vo));
        PageResult<PlayerVO> pageResult = new PageResult<>();
        pageResult.setList(List.of(vo));
        pageResult.setTotal(1L);
        mocked
            .when(() -> SmartPageUtil.convert2PageResult(any(Page.class), any(List.class)))
            .thenReturn(pageResult);

        ResponseDTO<PageResult<PlayerVO>> result = playerService.queryPlayers(queryForm);

        assertThat(result.getOk()).isTrue();
        assertThat(result.getData().getList()).hasSize(1);
        assertThat(result.getData().getTotal()).isEqualTo(1L);
      }
    }

    @Test
    @DisplayName("空結果返回空列表")
    void queryPlayers_empty() {
      PlayerQueryForm queryForm = new PlayerQueryForm();
      queryForm.setPageNum(1L);
      queryForm.setPageSize(10L);

      try (MockedStatic<SmartPageUtil> mocked = mockStatic(SmartPageUtil.class)) {
        Page<Object> page = new Page<>(1, 10);
        page.setTotal(0L);
        mocked.when(() -> SmartPageUtil.convert2PageQuery(queryForm)).thenReturn(page);
        when(playerDao.queryPage(any(Page.class), any(PlayerQueryForm.class)))
            .thenReturn(List.of());
        PageResult<PlayerVO> pageResult = new PageResult<>();
        pageResult.setList(List.of());
        pageResult.setTotal(0L);
        mocked
            .when(() -> SmartPageUtil.convert2PageResult(any(Page.class), any(List.class)))
            .thenReturn(pageResult);

        ResponseDTO<PageResult<PlayerVO>> result = playerService.queryPlayers(queryForm);

        assertThat(result.getOk()).isTrue();
        assertThat(result.getData().getList()).isEmpty();
        assertThat(result.getData().getTotal()).isEqualTo(0L);
      }
    }
  }

  // ==================== updatePlayer ====================

  @Nested
  @DisplayName("updatePlayer 更新玩家")
  class UpdatePlayerTest {

    @Test
    @DisplayName("成功更新 Email")
    void updatePlayer_email_success() {
      PlayerEntity player = buildPlayer();
      when(playerDao.selectById(1L)).thenReturn(player);
      when(blindIndexService.computeIndex("new@email.com")).thenReturn("newidx");
      when(playerDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null); // no duplicate
      when(playerDao.updateById(any(PlayerEntity.class))).thenReturn(1);

      PlayerUpdateForm form = new PlayerUpdateForm();
      form.setPlayerId(1L);
      form.setEmail("new@email.com");

      ResponseDTO<Void> result = playerService.updatePlayer(form);

      assertThat(result.getOk()).isTrue();
      verify(playerDao).updateById(any(PlayerEntity.class));
    }

    @Test
    @DisplayName("玩家不存在拒絕更新")
    void updatePlayer_notFound() {
      when(playerDao.selectById(999L)).thenReturn(null);

      PlayerUpdateForm form = new PlayerUpdateForm();
      form.setPlayerId(999L);
      form.setEmail("new@email.com");

      ResponseDTO<Void> result = playerService.updatePlayer(form);

      assertThat(result.getOk()).isFalse();
    }

    @Test
    @DisplayName("成功更新手機號碼")
    void updatePlayer_phone_success() {
      PlayerEntity player = buildPlayer();
      when(playerDao.selectById(1L)).thenReturn(player);
      when(blindIndexService.computeIndex("09001234567")).thenReturn("phoneidx");
      when(playerDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null); // no duplicate
      when(playerDao.updateById(any(PlayerEntity.class))).thenReturn(1);

      PlayerUpdateForm form = new PlayerUpdateForm();
      form.setPlayerId(1L);
      form.setPhone("09001234567");

      ResponseDTO<Void> result = playerService.updatePlayer(form);

      assertThat(result.getOk()).isTrue();
      verify(playerDao).updateById(any(PlayerEntity.class));
    }

    @Test
    @DisplayName("手機號碼重複拒絕更新")
    void updatePlayer_duplicatePhone_rejected() {
      PlayerEntity player = buildPlayer();
      when(playerDao.selectById(1L)).thenReturn(player);
      when(blindIndexService.computeIndex("09001234567")).thenReturn("phoneidx");
      when(playerDao.selectOne(any(LambdaQueryWrapper.class)))
          .thenReturn(new PlayerEntity()); // duplicate exists

      PlayerUpdateForm form = new PlayerUpdateForm();
      form.setPlayerId(1L);
      form.setPhone("09001234567");

      ResponseDTO<Void> result = playerService.updatePlayer(form);

      assertThat(result.getOk()).isFalse();
    }

    @Test
    @DisplayName("Email 重複拒絕更新")
    void updatePlayer_duplicateEmail_rejected() {
      PlayerEntity player = buildPlayer();
      when(playerDao.selectById(1L)).thenReturn(player);
      when(blindIndexService.computeIndex("dup@email.com")).thenReturn("dupidx");
      when(playerDao.selectOne(any(LambdaQueryWrapper.class)))
          .thenReturn(new PlayerEntity()); // duplicate exists

      PlayerUpdateForm form = new PlayerUpdateForm();
      form.setPlayerId(1L);
      form.setEmail("dup@email.com");

      ResponseDTO<Void> result = playerService.updatePlayer(form);

      assertThat(result.getOk()).isFalse();
    }
  }

  // ==================== changePlayerStatus ====================

  @Nested
  @DisplayName("changePlayerStatus 變更玩家狀態")
  class ChangeStatusTest {

    @Test
    @DisplayName("成功變更狀態")
    void changeStatus_success() {
      PlayerEntity player = buildPlayer();
      when(playerDao.selectById(1L)).thenReturn(player);
      when(playerStateManager.transitionStatus(any(), any(), any(), any()))
          .thenReturn(ResponseDTO.ok());

      ResponseDTO<Void> result =
          playerService.changePlayerStatus(1L, PlayerStatusEnum.LOCKED, "admin", "test reason");

      assertThat(result.getOk()).isTrue();
      verify(playerStateManager).transitionStatus(any(), eq(PlayerStatusEnum.LOCKED), any(), any());
    }

    @Test
    @DisplayName("玩家不存在拒絕變更")
    void changeStatus_notFound() {
      when(playerDao.selectById(999L)).thenReturn(null);

      ResponseDTO<Void> result =
          playerService.changePlayerStatus(999L, PlayerStatusEnum.LOCKED, "admin", "test");

      assertThat(result.getOk()).isFalse();
    }
  }

  // ==================== changeVipLevel ====================

  @Nested
  @DisplayName("changeVipLevel 變更 VIP 等級")
  class ChangeVipTest {

    @Test
    @DisplayName("成功變更 VIP")
    void changeVip_success() {
      PlayerEntity player = buildPlayer();
      when(playerDao.selectById(1L)).thenReturn(player);

      ResponseDTO<Void> result =
          playerService.changeVipLevel(1L, VipLevelEnum.SILVER, "manual upgrade");

      assertThat(result.getOk()).isTrue();
      verify(vipLevelManager).updateVipLevel(any(), eq(VipLevelEnum.SILVER), any());
    }
  }

  // ==================== PII masking ====================

  @Nested
  @DisplayName("PII masking 脫敏")
  class MaskingTest {

    @Test
    @DisplayName("Email 脫敏")
    void maskEmail() {
      assertThat(PlayerService.maskEmail("john@example.com")).isEqualTo("j***@example.com");
      assertThat(PlayerService.maskEmail(null)).isNull();
      assertThat(PlayerService.maskEmail("")).isNull();
    }

    @Test
    @DisplayName("Phone 脫敏")
    void maskPhone() {
      assertThat(PlayerService.maskPhone("13812345678")).isEqualTo("138****5678");
      assertThat(PlayerService.maskPhone(null)).isNull();
      assertThat(PlayerService.maskPhone("")).isNull();
    }
  }

  // ==================== helpers ====================

  private PlayerEntity buildPlayer() {
    PlayerEntity player = new PlayerEntity();
    player.setPlayerId(1L);
    player.setUsername("testplayer");
    player.setEmailEncrypted("test@email.com");
    player.setPhoneEncrypted("13812345678");
    player.setStatus(PlayerStatusEnum.ACTIVE.getValue());
    player.setKycLevel(0);
    player.setVipLevel(1);
    player.setDeleted(false);
    return player;
  }
}
