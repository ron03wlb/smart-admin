package net.lab1024.sa.igaming.integration.withdrawal;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.vavr.control.Option;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.mq.kafka.constant.IgamingKafkaConst;
import net.lab1024.sa.common.mq.kafka.event.DomainEvent;
import net.lab1024.sa.common.mq.kafka.event.DomainEventPublisher;
import net.lab1024.sa.igaming.activity.dao.PlayerBonusRecordDao;
import net.lab1024.sa.igaming.activity.domain.entity.PlayerBonusRecordEntity;
import net.lab1024.sa.igaming.common.code.PlayerErrorCode;
import net.lab1024.sa.igaming.common.code.WalletErrorCode;
import net.lab1024.sa.igaming.common.constant.BonusRecordStatusEnum;
import net.lab1024.sa.igaming.common.constant.DomainEventTypeConst;
import net.lab1024.sa.igaming.common.constant.PlayerStatusEnum;
import net.lab1024.sa.igaming.common.constant.WalletTypeEnum;
import net.lab1024.sa.igaming.integration.withdrawal.domain.form.WithdrawalRequestForm;
import net.lab1024.sa.igaming.integration.withdrawal.domain.vo.WithdrawalResultVO;
import net.lab1024.sa.igaming.player.dao.PlayerDao;
import net.lab1024.sa.igaming.player.domain.entity.PlayerEntity;
import net.lab1024.sa.igaming.wallet.dao.WalletDao;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletEntity;
import net.lab1024.sa.igaming.wallet.payment.domain.form.WithdrawRequestForm;
import net.lab1024.sa.igaming.wallet.payment.domain.vo.PaymentOrderVO;
import net.lab1024.sa.igaming.wallet.payment.service.PaymentService;
import org.springframework.stereotype.Service;

/**
 * Withdrawal integration service — orchestrates withdrawal application and balance validation.
 *
 * <p>This service provides high-level withdrawal orchestration, integrating the Player module
 * (status check), Wallet module (balance check and withdrawal creation), and Activity module
 * (active bonus check). It adds extra business logic such as available balance calculation and risk
 * event publishing.
 *
 * <p><b>Architecture Note:</b> The actual risk check is triggered asynchronously via Kafka event
 * flow:
 *
 * <ol>
 *   <li>Player applies for withdrawal → WithdrawalIntegrationService validates and creates order
 *   <li>WithdrawalIntegrationService publishes WITHDRAWAL_REQUESTED event
 *   <li>RiskEventConsumer (Risk module) consumes event → calls WithdrawalRiskCheckService
 *   <li>WithdrawalRiskCheckService executes LiteFlow risk rules (KYC, AML, Velocity)
 *   <li>If high risk → create RiskProposal for manual approval
 *   <li>If low risk → auto-approve and trigger PSP withdrawal
 * </ol>
 *
 * <p>This integration service provides:
 *
 * <ul>
 *   <li>Player status validation (ACTIVE only)
 *   <li>Available balance calculation (balance - lockedAmount - active bonus wagers)
 *   <li>Active bonus blocking (cannot withdraw while bonus wagering is in progress)
 *   <li>Withdrawal order creation with fund locking
 *   <li>WITHDRAWAL_REQUESTED event publishing (triggers risk check)
 * </ul>
 *
 * @author iGaming Team
 * @since 2026-03-18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawalIntegrationService {

  private final PlayerDao playerDao;
  private final WalletDao walletDao;
  private final PlayerBonusRecordDao playerBonusRecordDao;
  private final PaymentService paymentService;
  private final DomainEventPublisher domainEventPublisher;

  /**
   * Request withdrawal — orchestrates withdrawal application with comprehensive validation.
   *
   * <p>This method performs the following steps:
   *
   * <ol>
   *   <li>Validate player status (ACTIVE required)
   *   <li>Load CASH wallet
   *   <li>Check for active bonuses (block withdrawal if any)
   *   <li>Calculate available balance (balance - lockedAmount)
   *   <li>Validate withdrawal amount against available balance
   *   <li>Delegate to PaymentService.createWithdrawal() (creates order + locks funds)
   *   <li>Publish WITHDRAWAL_REQUESTED event (triggers risk check)
   *   <li>Return integrated withdrawal result VO
   * </ol>
   *
   * <p><b>Business Rules:</b>
   *
   * <ul>
   *   <li>Player must be in ACTIVE status (not LOCKED, SUSPENDED, CLOSED)
   *   <li>Withdrawal amount must not exceed available balance (balance - lockedAmount)
   *   <li>Cannot withdraw while active bonuses exist (wagering requirement in progress)
   *   <li>Minimum withdrawal amount: 0.0001 (validated by form constraint)
   * </ul>
   *
   * @param form withdrawal request form
   * @return withdrawal result VO with order and balance information
   */
  public ResponseDTO<WithdrawalResultVO> requestWithdrawal(WithdrawalRequestForm form) {
    // Step 1: Validate player status
    PlayerEntity player = playerDao.selectById(form.getPlayerId());
    if (player == null || player.getDeleted()) {
      return ResponseDTO.userErrorParam(PlayerErrorCode.PLAYER_NOT_FOUND.getMsg());
    }

    // Check player is ACTIVE (not LOCKED, SUSPENDED, CLOSED)
    if (!player.getStatus().equals(PlayerStatusEnum.ACTIVE.getValue())) {
      String errorMsg = "Player is not active";
      if (player.getStatus().equals(PlayerStatusEnum.LOCKED.getValue())) {
        errorMsg = "Player account is locked";
      } else if (player.getStatus().equals(PlayerStatusEnum.SUSPENDED.getValue())) {
        errorMsg = "Player account is suspended";
      } else if (player.getStatus().equals(PlayerStatusEnum.CLOSED.getValue())) {
        errorMsg = "Player account is closed";
      }
      return ResponseDTO.userErrorParam(errorMsg);
    }

    // Step 2: Load CASH wallet
    WalletEntity wallet =
        walletDao.selectOne(
            Wrappers.<WalletEntity>lambdaQuery()
                .eq(WalletEntity::getPlayerId, form.getPlayerId())
                .eq(WalletEntity::getWalletType, WalletTypeEnum.CASH.getValue())
                .eq(WalletEntity::getDeleted, false));

    if (wallet == null) {
      return ResponseDTO.userErrorParam(WalletErrorCode.WALLET_NOT_FOUND.getMsg());
    }

    // Step 3: Check for active bonuses (block withdrawal if any)
    long activeBonusCount =
        playerBonusRecordDao.selectCount(
            Wrappers.<PlayerBonusRecordEntity>lambdaQuery()
                .eq(PlayerBonusRecordEntity::getPlayerId, form.getPlayerId())
                .eq(PlayerBonusRecordEntity::getStatus, BonusRecordStatusEnum.ACTIVE.getValue())
                .eq(PlayerBonusRecordEntity::getDeleted, false));

    if (activeBonusCount > 0) {
      return ResponseDTO.userErrorParam(
          "Cannot withdraw while bonus wagering requirement is in progress. Please complete or"
              + " forfeit active bonuses first.");
    }

    // Step 4: Calculate available balance (balance - lockedAmount)
    BigDecimal availableBalance = wallet.getBalance().subtract(wallet.getLockedAmount());

    // Step 5: Validate withdrawal amount
    if (form.getAmount().compareTo(availableBalance) > 0) {
      return ResponseDTO.userErrorParam(
          WalletErrorCode.INSUFFICIENT_BALANCE.getMsg()
              + ". Available: "
              + availableBalance.toPlainString());
    }

    // Step 6: Delegate to PaymentService.createWithdrawal()
    // Build Wallet layer withdrawal form
    WithdrawRequestForm walletForm = new WithdrawRequestForm();
    walletForm.setWalletId(wallet.getWalletId());
    walletForm.setAmount(form.getAmount());
    walletForm.setCurrencyCode(wallet.getCurrencyCode());
    walletForm.setPspCode(null); // Auto-route to default PSP
    walletForm.setRequestId(
        form.getRequestId() != null
            ? form.getRequestId()
            : "withdrawal_" + UUID.randomUUID().toString().substring(0, 16));
    walletForm.setDescription(form.getDescription());

    ResponseDTO<PaymentOrderVO> paymentResult = paymentService.createWithdrawal(walletForm);

    if (!paymentResult.getOk()) {
      // Payment service returned error — propagate the error response
      WithdrawalResultVO errorVO = new WithdrawalResultVO();
      errorVO.setPlayerId(form.getPlayerId());
      errorVO.setAmount(form.getAmount());
      return ResponseDTO.userErrorParam(paymentResult.getMsg());
    }

    PaymentOrderVO paymentOrder = paymentResult.getData();

    log.info(
        "Withdrawal order created: playerId={}, orderNo={}, amount={}",
        form.getPlayerId(),
        paymentOrder.getOrderNo(),
        form.getAmount());

    // Step 7: Publish WITHDRAWAL_REQUESTED event (triggers risk check)
    publishWithdrawalRequestedEvent(player, wallet, paymentOrder);

    // Step 8: Build integrated result VO
    // Reload wallet for fresh balance (after fund lock)
    wallet = walletDao.selectById(wallet.getWalletId());

    WithdrawalResultVO resultVO = new WithdrawalResultVO();
    resultVO.setOrderNo(paymentOrder.getOrderNo());
    resultVO.setPlayerId(form.getPlayerId());
    resultVO.setWalletId(wallet.getWalletId());
    resultVO.setAmount(form.getAmount());
    resultVO.setCurrencyCode(wallet.getCurrencyCode());
    resultVO.setStatus(paymentOrder.getStatus());
    resultVO.setCurrentBalance(wallet.getBalance());
    resultVO.setLockedAmount(wallet.getLockedAmount());
    resultVO.setAvailableBalance(wallet.getBalance().subtract(wallet.getLockedAmount()));
    resultVO.setPspCode(paymentOrder.getPspCode());
    resultVO.setRiskCheckRequired(true); // Always require risk check

    return ResponseDTO.ok(resultVO);
  }

  /**
   * Publish WITHDRAWAL_REQUESTED event to Kafka.
   *
   * <p>This event triggers the risk check flow in the Risk module. The RiskEventConsumer will:
   *
   * <ol>
   *   <li>Execute LiteFlow risk rules (KYC_LEVEL_CHECK, AML_TURNOVER_CHECK, VELOCITY_CHECK)
   *   <li>Calculate risk score
   *   <li>If high risk → create RiskProposal for manual approval
   *   <li>If low risk → auto-approve and continue PSP withdrawal
   * </ol>
   *
   * @param player player entity
   * @param wallet wallet entity
   * @param paymentOrder payment order VO
   */
  @SuppressWarnings("FutureReturnValueIgnored")
  private void publishWithdrawalRequestedEvent(
      PlayerEntity player, WalletEntity wallet, PaymentOrderVO paymentOrder) {
    ObjectNode payload = JsonNodeFactory.instance.objectNode();
    payload.put("orderNo", paymentOrder.getOrderNo());
    payload.put("playerId", player.getPlayerId());
    payload.put("walletId", wallet.getWalletId());
    payload.put("amount", paymentOrder.getAmount().toPlainString());
    payload.put("currencyCode", paymentOrder.getCurrencyCode());
    payload.put("pspCode", paymentOrder.getPspCode());
    payload.put("kycLevel", player.getKycLevel());

    // Include wallet balance for risk calculation
    payload.put("currentBalance", wallet.getBalance().toPlainString());
    payload.put("lockedAmount", wallet.getLockedAmount().toPlainString());

    DomainEvent event =
        DomainEvent.builder()
            .eventType(DomainEventTypeConst.WITHDRAWAL_REQUESTED)
            .tenantId(player.getTenantId())
            .aggregateType("WITHDRAWAL")
            .aggregateId(paymentOrder.getOrderNo())
            .payload(payload)
            .build();

    domainEventPublisher.publish(IgamingKafkaConst.Topic.RISK_EVENTS, event);

    log.debug(
        "Published WITHDRAWAL_REQUESTED event: orderNo={}, playerId={}, amount={}",
        paymentOrder.getOrderNo(),
        player.getPlayerId(),
        paymentOrder.getAmount());
  }

  /**
   * Get available withdrawal balance for a player.
   *
   * <p>This is a convenience method for frontend to display available balance before the player
   * submits a withdrawal request.
   *
   * <p>Available balance = CASH wallet balance - locked amount - active bonus requirements
   *
   * @param playerId player ID
   * @return available balance
   */
  public Option<BigDecimal> getAvailableWithdrawalBalance(Long playerId) {
    // Load CASH wallet
    WalletEntity wallet =
        walletDao.selectOne(
            Wrappers.<WalletEntity>lambdaQuery()
                .eq(WalletEntity::getPlayerId, playerId)
                .eq(WalletEntity::getWalletType, WalletTypeEnum.CASH.getValue())
                .eq(WalletEntity::getDeleted, false));

    if (wallet == null) {
      return Option.none();
    }

    // Calculate available balance (balance - lockedAmount)
    BigDecimal available = wallet.getBalance().subtract(wallet.getLockedAmount());

    // Check for active bonuses (if any active bonus, return 0 to prevent withdrawal)
    long activeBonusCount =
        playerBonusRecordDao.selectCount(
            Wrappers.<PlayerBonusRecordEntity>lambdaQuery()
                .eq(PlayerBonusRecordEntity::getPlayerId, playerId)
                .eq(PlayerBonusRecordEntity::getStatus, BonusRecordStatusEnum.ACTIVE.getValue())
                .eq(PlayerBonusRecordEntity::getDeleted, false));

    if (activeBonusCount > 0) {
      return Option.some(BigDecimal.ZERO);
    }

    return Option.some(available.max(BigDecimal.ZERO));
  }
}
