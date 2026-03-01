package net.lab1024.sa.igaming.wallet.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.common.core.util.SmartBeanUtil;
import net.lab1024.sa.common.redislock.LockService;
import net.lab1024.sa.igaming.common.code.WalletErrorCode;
import net.lab1024.sa.igaming.common.config.IgamingProperties;
import net.lab1024.sa.igaming.common.constant.TransactionTypeEnum;
import net.lab1024.sa.igaming.wallet.dao.WalletDao;
import net.lab1024.sa.igaming.wallet.dao.WalletTransactionDao;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletEntity;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletTransactionEntity;
import net.lab1024.sa.igaming.wallet.domain.form.WalletAdminAdjustForm;
import net.lab1024.sa.igaming.wallet.domain.form.WalletFreezeForm;
import net.lab1024.sa.igaming.wallet.domain.vo.WalletTransactionVO;
import net.lab1024.sa.igaming.wallet.manager.WalletManager;
import org.springframework.stereotype.Service;

/**
 * Wallet admin service — admin operations for wallet management.
 *
 * <p>Provides freeze and manual adjustment operations for admin users.
 *
 * @author iGaming Team
 * @since 2026-03-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletAdminService {

  private final WalletDao walletDao;
  private final WalletTransactionDao walletTransactionDao;
  private final WalletManager walletManager;
  private final LockService lockService;
  private final IgamingProperties igamingProperties;

  /**
   * Freeze all wallets for a player (soft-delete).
   *
   * @param form freeze form with playerId and reason
   * @return number of wallets frozen
   */
  public ResponseDTO<Integer> freezePlayerWallets(WalletFreezeForm form) {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      return ResponseDTO.userErrorParam("Tenant context not initialized");
    }

    int frozenCount = walletManager.freezePlayerWallets(form.getPlayerId());
    log.info(
        "Player wallets frozen: playerId={}, count={}, reason={}, tenantId={}",
        form.getPlayerId(),
        frozenCount,
        form.getReason(),
        tenantId);
    return ResponseDTO.ok(frozenCount);
  }

  /**
   * Admin manual balance adjustment.
   *
   * <p>Positive amount = credit, negative amount = debit. Uses distributed lock + idempotency
   * (requestId) pattern.
   *
   * @param form adjustment form
   * @return transaction VO
   */
  public ResponseDTO<WalletTransactionVO> adminAdjust(WalletAdminAdjustForm form) {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      return ResponseDTO.userErrorParam("Tenant context not initialized");
    }

    if (form.getAmount().compareTo(BigDecimal.ZERO) == 0) {
      return ResponseDTO.userErrorParam("Adjustment amount cannot be zero");
    }

    String lockKey = "wallet:lock:" + form.getWalletId();
    return lockService.executeWithLock(
        lockKey,
        igamingProperties.getLock().getWaitMs(),
        igamingProperties.getLock().getLeaseMs(),
        () -> {
          // Idempotency Layer 1: check requestId
          WalletTransactionEntity existing =
              walletTransactionDao.selectOne(
                  Wrappers.<WalletTransactionEntity>lambdaQuery()
                      .eq(WalletTransactionEntity::getRequestId, form.getRequestId()));
          if (existing != null) {
            return ResponseDTO.ok(SmartBeanUtil.copy(existing, WalletTransactionVO.class));
          }

          // Load wallet
          WalletEntity wallet = walletDao.selectById(form.getWalletId());
          if (wallet == null || wallet.getDeleted()) {
            return ResponseDTO.userErrorParam(WalletErrorCode.WALLET_NOT_FOUND.getMsg());
          }

          BigDecimal amount = form.getAmount();
          BigDecimal balanceBefore = wallet.getBalance();
          BigDecimal balanceAfter;

          if (amount.compareTo(BigDecimal.ZERO) > 0) {
            // Credit
            balanceAfter = balanceBefore.add(amount);
          } else {
            // Debit — check available balance
            BigDecimal available = balanceBefore.subtract(wallet.getLockedAmount());
            if (available.compareTo(amount.abs()) < 0) {
              return ResponseDTO.userErrorParam(WalletErrorCode.INSUFFICIENT_BALANCE.getMsg());
            }
            balanceAfter = balanceBefore.add(amount);
          }

          // Build transaction record
          WalletTransactionEntity transaction = new WalletTransactionEntity();
          transaction.setWalletId(wallet.getWalletId());
          transaction.setPlayerId(wallet.getPlayerId());
          transaction.setTransactionType(TransactionTypeEnum.ADJUSTMENT.getValue());
          transaction.setAmount(amount);
          transaction.setBalanceBefore(balanceBefore);
          transaction.setBalanceAfter(balanceAfter);
          transaction.setRequestId(form.getRequestId());
          transaction.setReferenceType("ADMIN_ADJUST");
          transaction.setReferenceId(String.valueOf(tenantId));
          transaction.setDescription(form.getReason());

          // Update wallet balance
          wallet.setBalance(balanceAfter);

          // Delegate to Manager
          WalletTransactionEntity result;
          if (amount.compareTo(BigDecimal.ZERO) > 0) {
            result = walletManager.credit(wallet, transaction);
          } else {
            result = walletManager.debit(wallet, transaction);
          }

          log.info(
              "Admin adjustment: walletId={}, amount={}, requestId={}, tenantId={}",
              form.getWalletId(),
              amount,
              form.getRequestId(),
              tenantId);
          return ResponseDTO.ok(SmartBeanUtil.copy(result, WalletTransactionVO.class));
        });
  }
}
