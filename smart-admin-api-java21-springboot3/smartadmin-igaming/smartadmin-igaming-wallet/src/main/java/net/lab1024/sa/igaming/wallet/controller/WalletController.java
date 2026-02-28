package net.lab1024.sa.igaming.wallet.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.common.code.WalletErrorCode;
import net.lab1024.sa.igaming.common.constant.IgamingSwaggerTagConst;
import net.lab1024.sa.igaming.wallet.domain.form.WalletCreateForm;
import net.lab1024.sa.igaming.wallet.domain.form.WalletCreditForm;
import net.lab1024.sa.igaming.wallet.domain.form.WalletDebitForm;
import net.lab1024.sa.igaming.wallet.domain.form.WalletLockForm;
import net.lab1024.sa.igaming.wallet.domain.form.WalletQueryForm;
import net.lab1024.sa.igaming.wallet.domain.form.WalletTransactionQueryForm;
import net.lab1024.sa.igaming.wallet.domain.vo.WalletLockVO;
import net.lab1024.sa.igaming.wallet.domain.vo.WalletTransactionVO;
import net.lab1024.sa.igaming.wallet.domain.vo.WalletVO;
import net.lab1024.sa.igaming.wallet.service.WalletService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Wallet Controller — REST API endpoints for wallet operations.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@RestController
@Tag(name = IgamingSwaggerTagConst.WALLET)
@RequiredArgsConstructor
public class WalletController {

  private final WalletService walletService;

  @Operation(summary = "Query wallets with pagination")
  @PostMapping("/igaming/wallet/query")
  @SaCheckPermission("wallet:balance:query")
  public ResponseDTO<PageResult<WalletVO>> queryWallets(
      @RequestBody @Valid WalletQueryForm queryForm) {
    return walletService.queryWallets(queryForm);
  }

  @Operation(summary = "Get wallet by ID")
  @GetMapping("/igaming/wallet/get/{walletId}")
  @SaCheckPermission("wallet:balance:query")
  public ResponseDTO<WalletVO> getWallet(@PathVariable Long walletId) {
    return walletService
        .getWallet(walletId)
        .map(ResponseDTO::ok)
        .getOrElse(() -> ResponseDTO.userErrorParam(WalletErrorCode.WALLET_NOT_FOUND.getMsg()));
  }

  @Operation(summary = "Create a new wallet")
  @PostMapping("/igaming/wallet/create")
  @SaCheckPermission("wallet:balance:operate")
  public ResponseDTO<WalletVO> createWallet(@RequestBody @Valid WalletCreateForm form) {
    return walletService.createWallet(form);
  }

  @Operation(summary = "Credit (deposit/win/bonus) funds to wallet")
  @PostMapping("/igaming/wallet/credit")
  @SaCheckPermission("wallet:credit:operate")
  public ResponseDTO<WalletTransactionVO> credit(@RequestBody @Valid WalletCreditForm form) {
    return walletService.credit(form);
  }

  @Operation(summary = "Debit (withdraw/bet) funds from wallet")
  @PostMapping("/igaming/wallet/debit")
  @SaCheckPermission("wallet:debit:operate")
  public ResponseDTO<WalletTransactionVO> debit(@RequestBody @Valid WalletDebitForm form) {
    return walletService.debit(form);
  }

  @Operation(summary = "Lock (freeze) funds in wallet")
  @PostMapping("/igaming/wallet/lock")
  @SaCheckPermission("wallet:lock:operate")
  public ResponseDTO<WalletLockVO> lockFunds(@RequestBody @Valid WalletLockForm form) {
    return walletService.lockFunds(form);
  }

  @Operation(summary = "Unlock (unfreeze) funds in wallet")
  @PostMapping("/igaming/wallet/unlock/{lockId}")
  @SaCheckPermission("wallet:lock:operate")
  public ResponseDTO<String> unlockFunds(@PathVariable Long lockId) {
    return walletService.unlockFunds(lockId);
  }

  @Operation(summary = "Query wallet transactions with pagination")
  @PostMapping("/igaming/wallet/transaction/query")
  @SaCheckPermission("wallet:transaction:query")
  public ResponseDTO<PageResult<WalletTransactionVO>> queryTransactions(
      @RequestBody @Valid WalletTransactionQueryForm queryForm) {
    return walletService.queryTransactions(queryForm);
  }
}
