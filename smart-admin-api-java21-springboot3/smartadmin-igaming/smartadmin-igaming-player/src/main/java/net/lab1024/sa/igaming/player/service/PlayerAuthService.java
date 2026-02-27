package net.lab1024.sa.igaming.player.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.security.encrypt.BlindIndexService;
import net.lab1024.sa.common.security.service.PasswordEncryptService;
import net.lab1024.sa.common.token.player.StpPlayerUtil;
import net.lab1024.sa.igaming.common.code.PlayerErrorCode;
import net.lab1024.sa.igaming.common.constant.KycLevelEnum;
import net.lab1024.sa.igaming.common.constant.PlayerStatusEnum;
import net.lab1024.sa.igaming.common.constant.VipLevelEnum;
import net.lab1024.sa.igaming.common.constant.WalletTypeEnum;
import net.lab1024.sa.igaming.player.dao.PlayerDao;
import net.lab1024.sa.igaming.player.domain.entity.PlayerEntity;
import net.lab1024.sa.igaming.player.domain.form.PlayerLoginForm;
import net.lab1024.sa.igaming.player.domain.form.PlayerRegisterForm;
import net.lab1024.sa.igaming.player.domain.vo.PlayerAuthVO;
import net.lab1024.sa.igaming.player.manager.PlayerRegistrationManager;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletEntity;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * Player Authentication Service — handles registration, login, and logout.
 *
 * <p>Uses Argon2id for password hashing, AES-256-GCM for PII encryption (via TypeHandler), and
 * HMAC-SHA256 blind indexes for email/phone lookup.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Service
@RequiredArgsConstructor
public class PlayerAuthService {

  private final PlayerDao playerDao;
  private final PlayerRegistrationManager playerRegistrationManager;
  private final PasswordEncryptService passwordEncryptService;
  private final BlindIndexService blindIndexService;

  /**
   * Register a new player.
   *
   * @param form registration form
   * @return PlayerAuthVO with token on success
   */
  public ResponseDTO<PlayerAuthVO> register(PlayerRegisterForm form) {
    // Check username uniqueness
    PlayerEntity existingByUsername =
        playerDao.selectOne(
            Wrappers.<PlayerEntity>lambdaQuery()
                .eq(PlayerEntity::getUsername, form.getUsername())
                .eq(PlayerEntity::getDeleted, false));
    if (existingByUsername != null) {
      return ResponseDTO.userErrorParam(PlayerErrorCode.PLAYER_ALREADY_EXISTS.getMsg());
    }

    // Check email uniqueness via blind index
    if (form.getEmail() != null && !form.getEmail().isBlank()) {
      String emailIdx = blindIndexService.computeIndex(form.getEmail());
      PlayerEntity existingByEmail =
          playerDao.selectOne(
              Wrappers.<PlayerEntity>lambdaQuery()
                  .eq(PlayerEntity::getEmailBlindIdx, emailIdx)
                  .eq(PlayerEntity::getDeleted, false));
      if (existingByEmail != null) {
        return ResponseDTO.userErrorParam(PlayerErrorCode.EMAIL_ALREADY_EXISTS.getMsg());
      }
    }

    // Build player entity
    PlayerEntity player = new PlayerEntity();
    player.setUsername(form.getUsername());
    player.setPasswordHash(passwordEncryptService.encrypt(form.getPassword()));
    if (form.getEmail() != null && !form.getEmail().isBlank()) {
      player.setEmailEncrypted(form.getEmail());
      player.setEmailBlindIdx(blindIndexService.computeIndex(form.getEmail()));
    }
    if (form.getPhone() != null && !form.getPhone().isBlank()) {
      player.setPhoneEncrypted(form.getPhone());
      player.setPhoneBlindIdx(blindIndexService.computeIndex(form.getPhone()));
    }
    player.setStatus(PlayerStatusEnum.ACTIVE.getValue());
    player.setKycLevel(KycLevelEnum.L0.getValue());
    player.setVipLevel(VipLevelEnum.BRONZE.getValue());
    player.setRegistrationIp(form.getRegistrationIp());
    player.setDeleted(false);

    // Build wallet entity (CASH wallet with zero balance)
    WalletEntity wallet = new WalletEntity();
    wallet.setCurrencyCode("USD");
    wallet.setWalletType(WalletTypeEnum.CASH.getValue());
    wallet.setBalance(BigDecimal.ZERO);
    wallet.setLockedAmount(BigDecimal.ZERO);
    wallet.setDeleted(false);

    // Atomic registration (player + wallet)
    try {
      playerRegistrationManager.registerPlayer(player, wallet);
    } catch (DuplicateKeyException e) {
      return ResponseDTO.userErrorParam(PlayerErrorCode.PLAYER_ALREADY_EXISTS.getMsg());
    }

    // Sa-Token login
    StpPlayerUtil.login(player.getPlayerId());

    PlayerAuthVO authVO = new PlayerAuthVO();
    authVO.setPlayerId(player.getPlayerId());
    authVO.setUsername(player.getUsername());
    authVO.setTokenValue(StpPlayerUtil.getTokenValue());
    authVO.setVipLevel(player.getVipLevel());
    return ResponseDTO.ok(authVO);
  }

  /**
   * Player login.
   *
   * @param form login form
   * @return PlayerAuthVO with token on success
   */
  public ResponseDTO<PlayerAuthVO> login(PlayerLoginForm form) {
    PlayerEntity player =
        playerDao.selectOne(
            Wrappers.<PlayerEntity>lambdaQuery()
                .eq(PlayerEntity::getUsername, form.getUsername())
                .eq(PlayerEntity::getDeleted, false));
    if (player == null) {
      return ResponseDTO.userErrorParam(PlayerErrorCode.INVALID_CREDENTIALS.getMsg());
    }

    // Check account status
    if (player.getStatus().equals(PlayerStatusEnum.LOCKED.getValue())) {
      return ResponseDTO.userErrorParam(PlayerErrorCode.PLAYER_LOCKED.getMsg());
    }
    if (player.getStatus().equals(PlayerStatusEnum.SUSPENDED.getValue())) {
      return ResponseDTO.userErrorParam(PlayerErrorCode.PLAYER_SUSPENDED.getMsg());
    }
    if (player.getStatus().equals(PlayerStatusEnum.CLOSED.getValue())) {
      return ResponseDTO.userErrorParam(PlayerErrorCode.PLAYER_CLOSED.getMsg());
    }

    // Verify password
    if (!passwordEncryptService.matches(form.getPassword(), player.getPasswordHash())) {
      return ResponseDTO.userErrorParam(PlayerErrorCode.INVALID_CREDENTIALS.getMsg());
    }

    // Update last login info
    player.setLastLoginTime(OffsetDateTime.now(ZoneOffset.UTC));
    playerDao.updateById(player);

    // Sa-Token login
    StpPlayerUtil.login(player.getPlayerId());

    PlayerAuthVO authVO = new PlayerAuthVO();
    authVO.setPlayerId(player.getPlayerId());
    authVO.setUsername(player.getUsername());
    authVO.setTokenValue(StpPlayerUtil.getTokenValue());
    authVO.setVipLevel(player.getVipLevel());
    return ResponseDTO.ok(authVO);
  }

  /**
   * Player logout.
   *
   * @return success response
   */
  public ResponseDTO<Void> logout() {
    StpPlayerUtil.logout();
    return ResponseDTO.ok();
  }
}
