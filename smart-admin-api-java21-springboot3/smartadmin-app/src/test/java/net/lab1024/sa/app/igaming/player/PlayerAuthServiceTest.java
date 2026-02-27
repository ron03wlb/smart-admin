package net.lab1024.sa.app.igaming.player;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.security.encrypt.BlindIndexService;
import net.lab1024.sa.common.security.service.PasswordEncryptService;
import net.lab1024.sa.common.token.player.StpPlayerUtil;
import net.lab1024.sa.igaming.common.constant.PlayerStatusEnum;
import net.lab1024.sa.igaming.player.dao.PlayerDao;
import net.lab1024.sa.igaming.player.domain.entity.PlayerEntity;
import net.lab1024.sa.igaming.player.domain.form.PlayerLoginForm;
import net.lab1024.sa.igaming.player.domain.form.PlayerRegisterForm;
import net.lab1024.sa.igaming.player.domain.vo.PlayerAuthVO;
import net.lab1024.sa.igaming.player.manager.PlayerRegistrationManager;
import net.lab1024.sa.igaming.player.service.PlayerAuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * PlayerAuthService unit tests.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PlayerAuthService 單元測試")
class PlayerAuthServiceTest {

  @Mock private PlayerDao playerDao;
  @Mock private PlayerRegistrationManager playerRegistrationManager;
  @Mock private PasswordEncryptService passwordEncryptService;
  @Mock private BlindIndexService blindIndexService;

  @InjectMocks private PlayerAuthService playerAuthService;

  // ==================== register ====================

  @Nested
  @DisplayName("register 玩家註冊")
  class RegisterTest {

    @Test
    @DisplayName("成功註冊玩家")
    void register_success() {
      PlayerRegisterForm form = buildRegisterForm();
      when(playerDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
      when(passwordEncryptService.encrypt("Password123")).thenReturn("$argon2id$hash");
      when(blindIndexService.computeIndex("test@email.com")).thenReturn("emailidx");
      when(blindIndexService.computeIndex("1234567890")).thenReturn("phoneidx");
      when(playerRegistrationManager.registerPlayer(any(), any()))
          .thenAnswer(
              invocation -> {
                PlayerEntity p = invocation.getArgument(0);
                p.setPlayerId(100L);
                return p;
              });

      try (MockedStatic<StpPlayerUtil> mocked = mockStatic(StpPlayerUtil.class)) {
        mocked.when(() -> StpPlayerUtil.getTokenValue()).thenReturn("mock-token-123");

        ResponseDTO<PlayerAuthVO> result = playerAuthService.register(form);

        assertThat(result.getOk()).isTrue();
        assertThat(result.getData().getPlayerId()).isEqualTo(100L);
        assertThat(result.getData().getUsername()).isEqualTo("testplayer");
        assertThat(result.getData().getTokenValue()).isEqualTo("mock-token-123");
      }
    }

    @Test
    @DisplayName("用戶名重複拒絕註冊")
    void register_duplicateUsername_rejected() {
      PlayerRegisterForm form = buildRegisterForm();
      when(playerDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(new PlayerEntity());

      ResponseDTO<PlayerAuthVO> result = playerAuthService.register(form);

      assertThat(result.getOk()).isFalse();
      verify(playerRegistrationManager, never()).registerPlayer(any(), any());
    }

    @Test
    @DisplayName("Email 重複拒絕註冊")
    void register_duplicateEmail_rejected() {
      PlayerRegisterForm form = buildRegisterForm();
      // First call (username check) returns null, second call (email check) returns existing
      when(playerDao.selectOne(any(LambdaQueryWrapper.class)))
          .thenReturn(null)
          .thenReturn(new PlayerEntity());
      when(blindIndexService.computeIndex("test@email.com")).thenReturn("emailidx");

      ResponseDTO<PlayerAuthVO> result = playerAuthService.register(form);

      assertThat(result.getOk()).isFalse();
      verify(playerRegistrationManager, never()).registerPlayer(any(), any());
    }
  }

  // ==================== login ====================

  @Nested
  @DisplayName("login 玩家登入")
  class LoginTest {

    @Test
    @DisplayName("成功登入")
    void login_success() {
      PlayerLoginForm form = buildLoginForm();
      PlayerEntity player = buildActivePlayer();
      when(playerDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(player);
      when(passwordEncryptService.matches("Password123", "$argon2id$hash")).thenReturn(true);

      try (MockedStatic<StpPlayerUtil> mocked = mockStatic(StpPlayerUtil.class)) {
        mocked.when(() -> StpPlayerUtil.getTokenValue()).thenReturn("login-token-456");

        ResponseDTO<PlayerAuthVO> result = playerAuthService.login(form);

        assertThat(result.getOk()).isTrue();
        assertThat(result.getData().getPlayerId()).isEqualTo(1L);
        assertThat(result.getData().getTokenValue()).isEqualTo("login-token-456");
      }
    }

    @Test
    @DisplayName("密碼錯誤拒絕登入")
    void login_wrongPassword_rejected() {
      PlayerLoginForm form = buildLoginForm();
      PlayerEntity player = buildActivePlayer();
      when(playerDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(player);
      when(passwordEncryptService.matches("Password123", "$argon2id$hash")).thenReturn(false);

      ResponseDTO<PlayerAuthVO> result = playerAuthService.login(form);

      assertThat(result.getOk()).isFalse();
    }

    @Test
    @DisplayName("鎖定帳號拒絕登入")
    void login_lockedAccount_rejected() {
      PlayerLoginForm form = buildLoginForm();
      PlayerEntity player = buildActivePlayer();
      player.setStatus(PlayerStatusEnum.LOCKED.getValue());
      when(playerDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(player);

      ResponseDTO<PlayerAuthVO> result = playerAuthService.login(form);

      assertThat(result.getOk()).isFalse();
    }

    @Test
    @DisplayName("用戶不存在拒絕登入")
    void login_playerNotFound_rejected() {
      PlayerLoginForm form = buildLoginForm();
      when(playerDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

      ResponseDTO<PlayerAuthVO> result = playerAuthService.login(form);

      assertThat(result.getOk()).isFalse();
    }
  }

  // ==================== helpers ====================

  private PlayerRegisterForm buildRegisterForm() {
    PlayerRegisterForm form = new PlayerRegisterForm();
    form.setUsername("testplayer");
    form.setPassword("Password123");
    form.setEmail("test@email.com");
    form.setPhone("1234567890");
    form.setRegistrationIp("127.0.0.1");
    return form;
  }

  private PlayerLoginForm buildLoginForm() {
    PlayerLoginForm form = new PlayerLoginForm();
    form.setUsername("testplayer");
    form.setPassword("Password123");
    return form;
  }

  private PlayerEntity buildActivePlayer() {
    PlayerEntity player = new PlayerEntity();
    player.setPlayerId(1L);
    player.setUsername("testplayer");
    player.setPasswordHash("$argon2id$hash");
    player.setStatus(PlayerStatusEnum.ACTIVE.getValue());
    player.setVipLevel(1);
    player.setDeleted(false);
    return player;
  }
}
