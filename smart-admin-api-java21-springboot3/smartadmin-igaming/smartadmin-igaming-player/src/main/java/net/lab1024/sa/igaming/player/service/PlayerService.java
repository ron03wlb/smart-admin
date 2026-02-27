package net.lab1024.sa.igaming.player.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.vavr.control.Option;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.mybatis.util.SmartPageUtil;
import net.lab1024.sa.common.security.encrypt.BlindIndexService;
import net.lab1024.sa.igaming.common.code.PlayerErrorCode;
import net.lab1024.sa.igaming.common.constant.PlayerStatusEnum;
import net.lab1024.sa.igaming.common.constant.VipLevelEnum;
import net.lab1024.sa.igaming.player.dao.PlayerDao;
import net.lab1024.sa.igaming.player.domain.entity.PlayerEntity;
import net.lab1024.sa.igaming.player.domain.form.PlayerQueryForm;
import net.lab1024.sa.igaming.player.domain.form.PlayerUpdateForm;
import net.lab1024.sa.igaming.player.domain.vo.PlayerVO;
import net.lab1024.sa.igaming.player.manager.PlayerStateManager;
import net.lab1024.sa.igaming.player.manager.VipLevelManager;
import org.springframework.stereotype.Service;

/**
 * Player Service — business logic layer with Vavr Option pattern.
 *
 * <p>Handles player CRUD operations and delegates transactional operations to Manager layer.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Service
@RequiredArgsConstructor
public class PlayerService {

  private final PlayerDao playerDao;
  private final BlindIndexService blindIndexService;
  private final PlayerStateManager playerStateManager;
  private final VipLevelManager vipLevelManager;

  /**
   * Get player by ID.
   *
   * @param playerId player ID
   * @return Option containing PlayerVO if found, None otherwise
   */
  public Option<PlayerVO> getPlayer(Long playerId) {
    return Option.of(playerDao.selectById(playerId))
        .filter(entity -> !entity.getDeleted())
        .map(this::toPlayerVO);
  }

  /**
   * Query players with pagination.
   *
   * @param queryForm query parameters
   * @return paginated player list
   */
  public ResponseDTO<PageResult<PlayerVO>> queryPlayers(PlayerQueryForm queryForm) {
    Page<?> page = SmartPageUtil.convert2PageQuery(queryForm);
    List<PlayerVO> list = playerDao.queryPage(page, queryForm);
    PageResult<PlayerVO> pageResult = SmartPageUtil.convert2PageResult(page, list);
    return ResponseDTO.ok(pageResult);
  }

  /**
   * Update player profile (email/phone).
   *
   * @param form update form
   * @return ResponseDTO indicating success or error
   */
  public ResponseDTO<Void> updatePlayer(PlayerUpdateForm form) {
    PlayerEntity player = playerDao.selectById(form.getPlayerId());
    if (player == null || player.getDeleted()) {
      return ResponseDTO.userErrorParam(PlayerErrorCode.PLAYER_NOT_FOUND.getMsg());
    }

    if (form.getEmail() != null) {
      String emailIdx = blindIndexService.computeIndex(form.getEmail());
      // Check email uniqueness via blind index
      PlayerEntity existing =
          playerDao.selectOne(
              com.baomidou.mybatisplus.core.toolkit.Wrappers.<PlayerEntity>lambdaQuery()
                  .eq(PlayerEntity::getEmailBlindIdx, emailIdx)
                  .eq(PlayerEntity::getDeleted, false)
                  .ne(PlayerEntity::getPlayerId, form.getPlayerId()));
      if (existing != null) {
        return ResponseDTO.userErrorParam(PlayerErrorCode.EMAIL_ALREADY_EXISTS.getMsg());
      }
      player.setEmailEncrypted(form.getEmail());
      player.setEmailBlindIdx(emailIdx);
    }

    if (form.getPhone() != null) {
      String phoneIdx = blindIndexService.computeIndex(form.getPhone());
      // Check phone uniqueness via blind index
      PlayerEntity existing =
          playerDao.selectOne(
              com.baomidou.mybatisplus.core.toolkit.Wrappers.<PlayerEntity>lambdaQuery()
                  .eq(PlayerEntity::getPhoneBlindIdx, phoneIdx)
                  .eq(PlayerEntity::getDeleted, false)
                  .ne(PlayerEntity::getPlayerId, form.getPlayerId()));
      if (existing != null) {
        return ResponseDTO.userErrorParam(PlayerErrorCode.PHONE_ALREADY_EXISTS.getMsg());
      }
      player.setPhoneEncrypted(form.getPhone());
      player.setPhoneBlindIdx(phoneIdx);
    }

    playerDao.updateById(player);
    return ResponseDTO.ok();
  }

  /**
   * Change player status (delegates to PlayerStateManager).
   *
   * @param playerId player ID
   * @param newStatus new status
   * @param operator operator name
   * @param reason change reason
   * @return ResponseDTO indicating success or error
   */
  public ResponseDTO<Void> changePlayerStatus(
      Long playerId, PlayerStatusEnum newStatus, String operator, String reason) {
    PlayerEntity player = playerDao.selectById(playerId);
    if (player == null || player.getDeleted()) {
      return ResponseDTO.userErrorParam(PlayerErrorCode.PLAYER_NOT_FOUND.getMsg());
    }
    return playerStateManager.transitionStatus(player, newStatus, operator, reason);
  }

  /**
   * Change VIP level (delegates to VipLevelManager).
   *
   * @param playerId player ID
   * @param newLevel new VIP level
   * @param reason change reason
   * @return ResponseDTO indicating success or error
   */
  public ResponseDTO<Void> changeVipLevel(Long playerId, VipLevelEnum newLevel, String reason) {
    PlayerEntity player = playerDao.selectById(playerId);
    if (player == null || player.getDeleted()) {
      return ResponseDTO.userErrorParam(PlayerErrorCode.PLAYER_NOT_FOUND.getMsg());
    }
    vipLevelManager.updateVipLevel(player, newLevel, reason);
    return ResponseDTO.ok();
  }

  /**
   * Convert PlayerEntity to PlayerVO with PII masking.
   *
   * @param entity player entity
   * @return player VO with masked email/phone
   */
  private PlayerVO toPlayerVO(PlayerEntity entity) {
    PlayerVO vo = new PlayerVO();
    vo.setPlayerId(entity.getPlayerId());
    vo.setUsername(entity.getUsername());
    vo.setEmail(maskEmail(entity.getEmailEncrypted()));
    vo.setPhone(maskPhone(entity.getPhoneEncrypted()));
    vo.setStatus(entity.getStatus());
    vo.setKycLevel(entity.getKycLevel());
    vo.setVipLevel(entity.getVipLevel());
    vo.setRegistrationIp(entity.getRegistrationIp());
    vo.setLastLoginTime(entity.getLastLoginTime());
    vo.setCreateTime(entity.getCreateTime());
    vo.setUpdateTime(entity.getUpdateTime());
    return vo;
  }

  /** Mask email for display: j***@example.com */
  public static String maskEmail(String email) {
    if (email == null || email.isEmpty()) {
      return null;
    }
    int atIdx = email.indexOf('@');
    if (atIdx <= 0) {
      return "***";
    }
    return email.charAt(0) + "***" + email.substring(atIdx);
  }

  /** Mask phone for display: 138****1234 */
  public static String maskPhone(String phone) {
    if (phone == null || phone.isEmpty()) {
      return null;
    }
    if (phone.length() <= 4) {
      return "****";
    }
    int visiblePrefix = Math.min(3, phone.length() - 4);
    return phone.substring(0, visiblePrefix) + "****" + phone.substring(phone.length() - 4);
  }
}
