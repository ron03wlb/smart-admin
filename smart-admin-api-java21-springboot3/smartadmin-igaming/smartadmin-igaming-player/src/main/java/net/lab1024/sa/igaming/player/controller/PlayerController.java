package net.lab1024.sa.igaming.player.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.enumeration.BaseEnum;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.common.code.PlayerErrorCode;
import net.lab1024.sa.igaming.common.constant.IgamingSwaggerTagConst;
import net.lab1024.sa.igaming.common.constant.KycDocumentTypeEnum;
import net.lab1024.sa.igaming.common.constant.PlayerStatusEnum;
import net.lab1024.sa.igaming.common.constant.VipLevelEnum;
import net.lab1024.sa.igaming.player.domain.form.PlayerQueryForm;
import net.lab1024.sa.igaming.player.domain.form.PlayerUpdateForm;
import net.lab1024.sa.igaming.player.domain.vo.PlayerVO;
import net.lab1024.sa.igaming.player.service.KycVerificationService;
import net.lab1024.sa.igaming.player.service.PlayerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Player Controller — REST API endpoints for player management.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@RestController
@Tag(name = IgamingSwaggerTagConst.PLAYER)
@RequiredArgsConstructor
public class PlayerController {

  private final PlayerService playerService;
  private final KycVerificationService kycVerificationService;

  @Operation(summary = "Get player by ID")
  @GetMapping("/igaming/player/get/{playerId}")
  @SaCheckPermission("player:info:query")
  public ResponseDTO<PlayerVO> getPlayer(@PathVariable Long playerId) {
    return playerService
        .getPlayer(playerId)
        .map(ResponseDTO::ok)
        .getOrElse(() -> ResponseDTO.userErrorParam(PlayerErrorCode.PLAYER_NOT_FOUND.getMsg()));
  }

  @Operation(summary = "Query players with pagination")
  @PostMapping("/igaming/player/query")
  @SaCheckPermission("player:info:query")
  public ResponseDTO<PageResult<PlayerVO>> queryPlayers(
      @RequestBody @Valid PlayerQueryForm queryForm) {
    return playerService.queryPlayers(queryForm);
  }

  @Operation(summary = "Update player profile")
  @PutMapping("/igaming/player/update")
  @SaCheckPermission("player:info:operate")
  public ResponseDTO<Void> updatePlayer(@RequestBody @Valid PlayerUpdateForm form) {
    return playerService.updatePlayer(form);
  }

  @Operation(summary = "Change player status")
  @PostMapping("/igaming/player/status/change")
  @SaCheckPermission("player:info:operate")
  public ResponseDTO<Void> changePlayerStatus(
      @RequestParam Long playerId,
      @RequestParam Integer newStatus,
      @RequestParam String operator,
      @RequestParam String reason) {
    PlayerStatusEnum statusEnum = findEnum(PlayerStatusEnum.values(), newStatus);
    if (statusEnum == null) {
      return ResponseDTO.userErrorParam(PlayerErrorCode.INVALID_STATUS_VALUE.getMsg());
    }
    return playerService.changePlayerStatus(playerId, statusEnum, operator, reason);
  }

  @Operation(summary = "Change player VIP level")
  @PostMapping("/igaming/player/vip/change")
  @SaCheckPermission("player:info:operate")
  public ResponseDTO<Void> changeVipLevel(
      @RequestParam Long playerId, @RequestParam Integer newLevel, @RequestParam String reason) {
    VipLevelEnum levelEnum = findEnum(VipLevelEnum.values(), newLevel);
    if (levelEnum == null) {
      return ResponseDTO.userErrorParam(PlayerErrorCode.INVALID_VIP_LEVEL_VALUE.getMsg());
    }
    return playerService.changeVipLevel(playerId, levelEnum, reason);
  }

  @Operation(summary = "Submit KYC L1 document")
  @PostMapping("/igaming/player/kyc/submit")
  @SaCheckPermission("player:kyc:operate")
  public ResponseDTO<Void> submitKycDocument(
      @RequestParam Long playerId,
      @RequestParam Integer documentType,
      @RequestParam String documentUrl) {
    KycDocumentTypeEnum typeEnum = findEnum(KycDocumentTypeEnum.values(), documentType);
    if (typeEnum == null) {
      return ResponseDTO.userErrorParam(PlayerErrorCode.INVALID_DOCUMENT_TYPE.getMsg());
    }
    return kycVerificationService.submitL1Document(playerId, typeEnum, documentUrl);
  }

  @Operation(summary = "Check withdrawal eligibility by KYC level")
  @GetMapping("/igaming/player/kyc/check/{playerId}")
  @SaCheckPermission("player:kyc:query")
  public ResponseDTO<Void> checkWithdrawalEligibility(
      @PathVariable Long playerId, @RequestParam BigDecimal amount) {
    return kycVerificationService.checkWithdrawalEligibility(playerId, amount);
  }

  private static <E extends BaseEnum> E findEnum(E[] values, Integer value) {
    return Arrays.stream(values).filter(e -> e.getValue().equals(value)).findFirst().orElse(null);
  }
}
