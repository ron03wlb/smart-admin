package net.lab1024.sa.igaming.integration.payment;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.vavr.control.Option;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.common.mq.kafka.constant.IgamingKafkaConst;
import net.lab1024.sa.common.mq.kafka.event.DomainEvent;
import net.lab1024.sa.common.mq.kafka.event.DomainEventPublisher;
import net.lab1024.sa.igaming.activity.dao.PromotionRuleDao;
import net.lab1024.sa.igaming.activity.domain.entity.PlayerBonusRecordEntity;
import net.lab1024.sa.igaming.activity.domain.entity.PromotionRuleEntity;
import net.lab1024.sa.igaming.activity.manager.BonusDistributionManager;
import net.lab1024.sa.igaming.common.constant.DomainEventTypeConst;
import net.lab1024.sa.igaming.common.constant.PaymentOrderStatusEnum;
import net.lab1024.sa.igaming.common.constant.PaymentOrderTypeEnum;
import net.lab1024.sa.igaming.common.constant.PromotionTypeEnum;
import net.lab1024.sa.igaming.common.constant.WalletTypeEnum;
import net.lab1024.sa.igaming.integration.payment.domain.form.DepositCallbackForm;
import net.lab1024.sa.igaming.integration.payment.domain.vo.DepositResultVO;
import net.lab1024.sa.igaming.wallet.dao.WalletDao;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletEntity;
import net.lab1024.sa.igaming.wallet.domain.form.WalletBonusCreditForm;
import net.lab1024.sa.igaming.wallet.payment.dao.PaymentOrderDao;
import net.lab1024.sa.igaming.wallet.payment.domain.entity.PaymentOrderEntity;
import net.lab1024.sa.igaming.wallet.payment.service.PaymentService;
import net.lab1024.sa.igaming.wallet.service.WalletService;
import org.springframework.stereotype.Service;

/**
 * First deposit bonus integration service - orchestrates deposit callback with bonus award.
 *
 * <p>This service coordinates the complete first deposit bonus flow:
 *
 * <ol>
 *   <li><b>PSP Callback Processing</b>: Validate signature and credit CASH wallet
 *   <li><b>First Deposit Detection</b>: Check if this is player's first successful deposit
 *   <li><b>Promotion Rule Matching</b>: Find active FIRST_DEPOSIT promotion
 *   <li><b>Bonus Distribution</b>: Award bonus to BONUS wallet via BonusDistributionManager
 *   <li><b>Event Publishing</b>: Publish FIRST_DEPOSIT_COMPLETED event to Kafka
 * </ol>
 *
 * <p><b>Integration Flow:</b>
 *
 * <pre>
 * FirstDepositBonusIntegrationService
 *   ├── PaymentService.processDepositCallback()    (Wallet module)
 *   ├── PaymentOrderDao.selectList()                (Check first deposit)
 *   ├── PromotionRuleDao.selectActiveRules()        (Activity module)
 *   ├── BonusDistributionManager.distributeBonus()  (Activity module)
 *   ├── WalletService.creditBonus()                 (Wallet module)
 *   └── DomainEventPublisher.publish()              (Kafka event)
 * </pre>
 *
 * <p><b>Kafka Event Chain:</b>
 *
 * <pre>
 * DEPOSIT_COMPLETED → FIRST_DEPOSIT_BONUS_AWARDED
 * </pre>
 *
 * @author iGaming Team
 * @since 2026-03-18
 * @see PaymentService#processDepositCallback(String, String, String)
 * @see BonusDistributionManager#distributeBonus(Long, PromotionRuleEntity, String, Long)
 * @see WalletService#creditBonus(WalletBonusCreditForm)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FirstDepositBonusIntegrationService {

  private final PaymentService paymentService;
  private final PaymentOrderDao paymentOrderDao;
  private final WalletDao walletDao;
  private final WalletService walletService;
  private final PromotionRuleDao promotionRuleDao;
  private final BonusDistributionManager bonusDistributionManager;
  private final DomainEventPublisher domainEventPublisher;

  /**
   * Process deposit callback with first deposit bonus check.
   *
   * <p>This method orchestrates the complete deposit processing flow, including automatic first
   * deposit bonus award if eligible. If any step fails, subsequent steps are not executed.
   *
   * @param form deposit callback form (PSP webhook data)
   * @return deposit result with bonus information
   */
  public ResponseDTO<DepositResultVO> processFirstDeposit(DepositCallbackForm form) {

    log.info(
        "Processing deposit callback: orderNo={}, pspTransactionId={}",
        form.getOrderNo(),
        form.getPspTransactionId());

    // ==================================================================================
    // Step 1: Process PSP callback → Credit CASH wallet (Wallet module)
    // ==================================================================================
    ResponseDTO<String> depositResult =
        paymentService.processDepositCallback(
            form.getOrderNo(), form.getPspTransactionId(), form.getCallbackPayload());

    if (!depositResult.getOk()) {
      log.error(
          "Deposit callback failed: orderNo={}, error={}",
          form.getOrderNo(),
          depositResult.getMsg());
      return ResponseDTO.error(depositResult);
    }

    // Load completed order
    PaymentOrderEntity order =
        paymentOrderDao.selectOne(
            Wrappers.<PaymentOrderEntity>lambdaQuery()
                .eq(PaymentOrderEntity::getOrderNo, form.getOrderNo()));

    if (order == null) {
      log.error("Payment order not found after callback: orderNo={}", form.getOrderNo());
      return ResponseDTO.userErrorParam("Payment order not found");
    }

    Long playerId = order.getPlayerId();
    Long walletId = order.getWalletId();
    BigDecimal depositAmount = order.getAmount();

    log.info(
        "Deposit completed: orderNo={}, playerId={}, amount={}",
        form.getOrderNo(),
        playerId,
        depositAmount);

    // ==================================================================================
    // Step 2: Check if this is first deposit
    // ==================================================================================
    boolean isFirstDeposit = checkIsFirstDeposit(playerId);
    log.info("First deposit check: playerId={}, isFirstDeposit={}", playerId, isFirstDeposit);

    // ==================================================================================
    // Step 3 & 4: Award first deposit bonus (if eligible)
    // ==================================================================================
    BigDecimal bonusAmount = BigDecimal.ZERO;
    Long bonusWalletId = null;
    BigDecimal bonusBalance = BigDecimal.ZERO;
    String promotionCode = null;

    if (isFirstDeposit) {
      try {
        Option<PlayerBonusRecordEntity> bonusRecordOpt =
            awardFirstDepositBonus(playerId, depositAmount);
        if (bonusRecordOpt.isDefined()) {
          PlayerBonusRecordEntity bonusRecord = bonusRecordOpt.get();
          bonusAmount = bonusRecord.getBonusAmount();
          promotionCode =
              bonusRecord.getRuleId().toString(); // Simplified, could load rule.ruleCode

          // Get BONUS wallet
          WalletEntity bonusWallet =
              walletDao.selectOne(
                  Wrappers.<WalletEntity>lambdaQuery()
                      .eq(WalletEntity::getPlayerId, playerId)
                      .eq(WalletEntity::getWalletType, WalletTypeEnum.BONUS.getValue())
                      .eq(WalletEntity::getDeleted, false));

          if (bonusWallet != null) {
            bonusWalletId = bonusWallet.getWalletId();
            bonusBalance = bonusWallet.getBalance();
          }

          log.info(
              "First deposit bonus awarded: playerId={}, bonusAmount={}, bonusWalletId={}",
              playerId,
              bonusAmount,
              bonusWalletId);
        }
      } catch (Exception e) {
        // Bonus award failed, but deposit succeeded - log error and continue
        log.error("Failed to award first deposit bonus: playerId={}", playerId, e);
      }
    }

    // ==================================================================================
    // Step 5: Build result VO
    // ==================================================================================
    // Get CASH wallet balance
    WalletEntity cashWallet = walletDao.selectById(walletId);
    BigDecimal cashBalance = (cashWallet != null) ? cashWallet.getBalance() : BigDecimal.ZERO;

    DepositResultVO result = new DepositResultVO();
    result.setOrderNo(form.getOrderNo());
    result.setPlayerId(playerId);
    result.setWalletId(walletId);
    result.setDepositAmount(depositAmount);
    result.setCashBalance(cashBalance);
    result.setIsFirstDeposit(isFirstDeposit);
    result.setBonusAmount(bonusAmount);
    result.setBonusWalletId(bonusWalletId);
    result.setBonusBalance(bonusBalance);
    result.setPromotionCode(promotionCode);

    // ==================================================================================
    // Step 6: Publish event (if first deposit)
    // ==================================================================================
    if (isFirstDeposit) {
      publishFirstDepositEvent(playerId, depositAmount, bonusAmount);
    }

    log.info(
        "Deposit processing completed: playerId={}, depositAmount={}, bonusAmount={}",
        playerId,
        depositAmount,
        bonusAmount);

    return ResponseDTO.ok(result);
  }

  /**
   * Check if this is player's first deposit.
   *
   * <p>A deposit is considered "first" if no previous COMPLETED deposit orders exist.
   *
   * @param playerId player identifier
   * @return true if first deposit, false otherwise
   */
  private boolean checkIsFirstDeposit(Long playerId) {
    long completedDepositCount =
        paymentOrderDao.selectCount(
            Wrappers.<PaymentOrderEntity>lambdaQuery()
                .eq(PaymentOrderEntity::getPlayerId, playerId)
                .eq(PaymentOrderEntity::getOrderType, PaymentOrderTypeEnum.DEPOSIT.getValue())
                .eq(PaymentOrderEntity::getStatus, PaymentOrderStatusEnum.SUCCESS.getValue()));

    return completedDepositCount == 1L; // Current deposit is the first
  }

  /**
   * Award first deposit bonus to player.
   *
   * <p>This method:
   *
   * <ol>
   *   <li>Finds active FIRST_DEPOSIT promotion rule
   *   <li>Calls BonusDistributionManager to create bonus record
   *   <li>Credits BONUS wallet via WalletService
   * </ol>
   *
   * @param playerId player identifier
   * @param depositAmount deposit amount (for bonus calculation)
   * @return Option of PlayerBonusRecordEntity if bonus awarded, Option.none() otherwise
   */
  private Option<PlayerBonusRecordEntity> awardFirstDepositBonus(
      Long playerId, BigDecimal depositAmount) {

    Long tenantId = TenantContext.getTenantId();

    // Find active FIRST_DEPOSIT promotion rule
    List<PromotionRuleEntity> activeRules = promotionRuleDao.selectActiveRules(tenantId);
    Option<PromotionRuleEntity> firstDepositRuleOpt =
        Option.ofOptional(
            activeRules.stream()
                .filter(
                    rule ->
                        rule.getPromotionType().equals(PromotionTypeEnum.FIRST_DEPOSIT.getValue()))
                .findFirst());

    if (firstDepositRuleOpt.isEmpty()) {
      log.info("No active FIRST_DEPOSIT promotion found: tenantId={}", tenantId);
      return Option.none();
    }

    PromotionRuleEntity rule = firstDepositRuleOpt.get();
    String claimId = "first_deposit_" + playerId + "_" + System.currentTimeMillis();

    // Distribute bonus (creates PlayerBonusRecordEntity + credits BONUS wallet)
    // NOTE: BonusDistributionManager.distributeBonus() handles the entire bonus distribution
    // flow, including creating the bonus record, wagering progress, AND crediting the BONUS
    // wallet. We do NOT need to call walletService.creditBonus() separately.
    PlayerBonusRecordEntity bonusRecord =
        bonusDistributionManager.distributeBonus(playerId, rule, depositAmount, claimId, tenantId);

    return Option.of(bonusRecord);
  }

  /**
   * Publish FIRST_DEPOSIT_COMPLETED domain event to Kafka.
   *
   * <p>Event payload includes:
   *
   * <ul>
   *   <li>playerId - Player identifier
   *   <li>depositAmount - Deposit amount
   *   <li>bonusAmount - Bonus amount awarded
   * </ul>
   *
   * <p>This event triggers downstream processes:
   *
   * <ul>
   *   <li>Activity module: Wagering progress tracking
   *   <li>Agent module: Referral commission (if applicable)
   *   <li>Risk module: First deposit risk scoring
   * </ul>
   *
   * @param playerId player identifier
   * @param depositAmount deposit amount
   * @param bonusAmount bonus amount
   */
  private void publishFirstDepositEvent(
      Long playerId, BigDecimal depositAmount, BigDecimal bonusAmount) {
    ObjectNode payload = JsonNodeFactory.instance.objectNode();
    payload.put("playerId", playerId);
    payload.put("depositAmount", depositAmount.toString());
    payload.put("bonusAmount", bonusAmount.toString());

    DomainEvent event =
        DomainEvent.builder()
            .eventType(DomainEventTypeConst.FIRST_DEPOSIT_COMPLETED)
            .aggregateType("Payment")
            .aggregateId(String.valueOf(playerId))
            .payload((JsonNode) payload)
            .build();

    domainEventPublisher.publish(IgamingKafkaConst.Topic.PLAYER_EVENTS, event);

    log.info(
        "FIRST_DEPOSIT_COMPLETED event published: eventId={}, playerId={}",
        event.getEventId(),
        playerId);
  }
}
