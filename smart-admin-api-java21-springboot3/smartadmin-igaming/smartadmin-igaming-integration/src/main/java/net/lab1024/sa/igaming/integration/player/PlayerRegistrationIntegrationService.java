package net.lab1024.sa.igaming.integration.player;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.mq.kafka.constant.IgamingKafkaConst;
import net.lab1024.sa.common.mq.kafka.event.DomainEvent;
import net.lab1024.sa.common.mq.kafka.event.DomainEventPublisher;
import net.lab1024.sa.igaming.common.constant.DomainEventTypeConst;
import net.lab1024.sa.igaming.common.constant.WalletTypeEnum;
import net.lab1024.sa.igaming.integration.player.domain.form.PlayerRegistrationIntegrationForm;
import net.lab1024.sa.igaming.integration.player.domain.vo.PlayerRegistrationResultVO;
import net.lab1024.sa.igaming.player.domain.form.PlayerRegisterForm;
import net.lab1024.sa.igaming.player.domain.vo.PlayerAuthVO;
import net.lab1024.sa.igaming.player.service.PlayerAuthService;
import net.lab1024.sa.igaming.wallet.domain.form.WalletCreateForm;
import net.lab1024.sa.igaming.wallet.domain.vo.WalletVO;
import net.lab1024.sa.igaming.wallet.service.WalletService;
import org.springframework.stereotype.Service;

/**
 * Player registration integration service - orchestrates end-to-end player onboarding.
 *
 * <p>This service coordinates the complete player registration flow by orchestrating multiple
 * domain services:
 *
 * <ol>
 *   <li><b>Player Registration</b>: Create player account with Argon2id password hashing
 *   <li><b>Wallet Creation</b>: Create CASH wallet (main playable balance)
 *   <li><b>Bonus Wallet Creation</b>: Create BONUS wallet (promotional credits)
 *   <li><b>Event Publishing</b>: Publish PLAYER_REGISTERED event to Kafka
 *   <li><b>Bonus Eligibility</b>: Check first deposit bonus eligibility
 * </ol>
 *
 * <p><b>Integration Flow:</b>
 *
 * <pre>
 * PlayerRegistrationIntegrationService
 *   ├── PlayerAuthService.register()        (Player module)
 *   ├── WalletService.createWallet()        (CASH wallet, Wallet module)
 *   ├── WalletService.createWallet()        (BONUS wallet, Wallet module)
 *   ├── DomainEventPublisher.publish()      (Kafka event)
 *   └── Activity module check (placeholder)
 * </pre>
 *
 * <p><b>Kafka Event Chain:</b>
 *
 * <pre>
 * PLAYER_REGISTERED → WALLET_CREATED → BONUS_ELIGIBLE
 * </pre>
 *
 * @author iGaming Team
 * @since 2026-03-18
 * @see PlayerAuthService#register(PlayerRegisterForm)
 * @see WalletService#createWallet(WalletCreateForm)
 * @see DomainEventPublisher#publish(String, DomainEvent)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerRegistrationIntegrationService {

  private final PlayerAuthService playerAuthService;
  private final WalletService walletService;
  private final DomainEventPublisher domainEventPublisher;

  /**
   * Register player with automatic wallet creation.
   *
   * <p>This method orchestrates the complete registration flow, ensuring that all necessary
   * components are created atomically. If any step fails, subsequent steps are not executed.
   *
   * @param form player registration integration form
   * @return registration result with player info, wallets, and bonus eligibility status
   */
  public ResponseDTO<PlayerRegistrationResultVO> registerPlayerWithWallet(
      PlayerRegistrationIntegrationForm form) {

    log.info("Starting integrated player registration: username={}", form.getUsername());

    // ==================================================================================
    // Step 1: Register player account (Player module)
    // ==================================================================================
    PlayerRegisterForm playerForm = new PlayerRegisterForm();
    playerForm.setUsername(form.getUsername());
    playerForm.setPassword(form.getPassword());
    playerForm.setEmail(form.getEmail());
    playerForm.setPhone(form.getPhone());
    playerForm.setRegistrationIp(form.getRegistrationIp());

    ResponseDTO<PlayerAuthVO> playerResult = playerAuthService.register(playerForm);
    if (!playerResult.getOk()) {
      log.error("Player registration failed: {}", playerResult.getMsg());
      return ResponseDTO.error(playerResult);
    }

    PlayerAuthVO player = playerResult.getData();
    Long playerId = player.getPlayerId();

    log.info(
        "Player registered successfully: playerId={}, username={}", playerId, player.getUsername());

    // ==================================================================================
    // Step 2: Query existing CASH wallet (created by PlayerAuthService)
    // ==================================================================================
    // Note: PlayerAuthService.register() already creates CASH wallet atomically with player
    // We query the existing wallet instead of creating a duplicate
    ResponseDTO<WalletVO> cashWalletResult =
        walletService.getWallet(playerId, WalletTypeEnum.CASH.getValue());
    if (!cashWalletResult.getOk()) {
      log.error(
          "CASH wallet query failed: playerId={}, error={}", playerId, cashWalletResult.getMsg());
      return ResponseDTO.error(cashWalletResult);
    }

    WalletVO cashWallet = cashWalletResult.getData();
    log.info("CASH wallet retrieved: walletId={}, playerId={}", cashWallet.getWalletId(), playerId);

    // ==================================================================================
    // Step 3: Create BONUS wallet (Wallet module)
    // ==================================================================================
    WalletCreateForm bonusWalletForm = new WalletCreateForm();
    bonusWalletForm.setPlayerId(playerId);
    bonusWalletForm.setWalletType(WalletTypeEnum.BONUS.getValue());
    bonusWalletForm.setCurrencyCode(form.getCurrencyCode());

    ResponseDTO<WalletVO> bonusWalletResult = walletService.createWallet(bonusWalletForm);
    if (!bonusWalletResult.getOk()) {
      log.error(
          "BONUS wallet creation failed: playerId={}, error={}",
          playerId,
          bonusWalletResult.getMsg());
      return ResponseDTO.error(bonusWalletResult);
    }

    WalletVO bonusWallet = bonusWalletResult.getData();
    log.info("BONUS wallet created: walletId={}, playerId={}", bonusWallet.getWalletId(), playerId);

    // ==================================================================================
    // Step 4: Publish PLAYER_REGISTERED event (Kafka)
    // ==================================================================================
    publishPlayerRegisteredEvent(playerId, player.getUsername(), form.getReferralCode());

    // ==================================================================================
    // Step 5: Check first deposit bonus eligibility (Activity module - placeholder)
    // ==================================================================================
    // TODO: Integrate with Activity module's BonusDistributionManager
    // For Sprint 2 Phase 1, we'll return true by default
    // In Phase 2, this will connect to actual bonus eligibility logic
    boolean firstDepositBonusEligible = true;

    // ==================================================================================
    // Build result VO
    // ==================================================================================
    List<WalletVO> wallets = new ArrayList<>();
    wallets.add(cashWallet);
    wallets.add(bonusWallet);

    PlayerRegistrationResultVO result = new PlayerRegistrationResultVO();
    result.setPlayer(player);
    result.setWallets(wallets);
    result.setFirstDepositBonusEligible(firstDepositBonusEligible);
    result.setReferralCode(form.getReferralCode());

    log.info(
        "Player registration completed: playerId={}, wallets={}, bonusEligible={}",
        playerId,
        wallets.size(),
        firstDepositBonusEligible);

    return ResponseDTO.ok(result);
  }

  /**
   * Publish PLAYER_REGISTERED domain event to Kafka.
   *
   * <p>Event payload includes:
   *
   * <ul>
   *   <li>playerId - Player identifier
   *   <li>username - Player username
   *   <li>referralCode - Referral code (if applicable)
   * </ul>
   *
   * <p>This event triggers downstream processes:
   *
   * <ul>
   *   <li>Wallet module: Wallet creation confirmation
   *   <li>Activity module: First deposit bonus eligibility check
   *   <li>Agent module: Referral commission processing (if referral code provided)
   * </ul>
   *
   * @param playerId player identifier
   * @param username player username
   * @param referralCode referral code (nullable)
   */
  private void publishPlayerRegisteredEvent(Long playerId, String username, String referralCode) {
    ObjectNode payload = JsonNodeFactory.instance.objectNode();
    payload.put("playerId", playerId);
    payload.put("username", username);
    if (referralCode != null) {
      payload.put("referralCode", referralCode);
    }

    DomainEvent event =
        DomainEvent.builder()
            .eventType(DomainEventTypeConst.PLAYER_REGISTERED)
            .aggregateType("Player")
            .aggregateId(String.valueOf(playerId))
            .payload((JsonNode) payload)
            .build();

    domainEventPublisher.publish(IgamingKafkaConst.Topic.PLAYER_EVENTS, event);

    log.info(
        "PLAYER_REGISTERED event published: eventId={}, playerId={}", event.getEventId(), playerId);
  }
}
