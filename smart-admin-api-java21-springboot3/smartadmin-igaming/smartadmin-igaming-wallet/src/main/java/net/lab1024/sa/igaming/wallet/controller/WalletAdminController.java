package net.lab1024.sa.igaming.wallet.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.common.constant.IgamingSwaggerTagConst;
import net.lab1024.sa.igaming.wallet.domain.form.WalletAdminAdjustForm;
import net.lab1024.sa.igaming.wallet.domain.form.WalletFreezeForm;
import net.lab1024.sa.igaming.wallet.domain.vo.WalletTransactionVO;
import net.lab1024.sa.igaming.wallet.service.WalletAdminService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Wallet Admin Controller — admin endpoints for wallet management.
 *
 * @author iGaming Team
 * @since 2026-03-01
 */
@RestController
@Tag(name = IgamingSwaggerTagConst.WALLET)
@RequiredArgsConstructor
public class WalletAdminController {

  private final WalletAdminService walletAdminService;

  @SaCheckPermission("wallet:admin:operate")
  @Operation(summary = "Freeze all wallets for a player")
  @PostMapping("/igaming/admin/wallet/freeze")
  public ResponseDTO<Integer> freeze(@RequestBody @Valid WalletFreezeForm form) {
    return walletAdminService.freezePlayerWallets(form);
  }

  @SaCheckPermission("wallet:admin:operate")
  @Operation(summary = "Admin manual balance adjustment")
  @PostMapping("/igaming/admin/wallet/adjust")
  public ResponseDTO<WalletTransactionVO> adjust(@RequestBody @Valid WalletAdminAdjustForm form) {
    return walletAdminService.adminAdjust(form);
  }
}
