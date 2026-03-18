package net.lab1024.sa.igaming.integration.approval;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.mq.kafka.constant.IgamingKafkaConst;
import net.lab1024.sa.common.mq.kafka.event.DomainEvent;
import net.lab1024.sa.common.mq.kafka.event.DomainEventPublisher;
import net.lab1024.sa.igaming.common.constant.PaymentOrderStatusEnum;
import net.lab1024.sa.igaming.common.constant.PaymentOrderTypeEnum;
import net.lab1024.sa.igaming.common.constant.RiskProposalStatusEnum;
import net.lab1024.sa.igaming.risk.dao.RiskAssessmentDao;
import net.lab1024.sa.igaming.risk.dao.RiskProposalDao;
import net.lab1024.sa.igaming.risk.domain.entity.RiskAssessmentEntity;
import net.lab1024.sa.igaming.risk.domain.entity.RiskProposalEntity;
import net.lab1024.sa.igaming.wallet.dao.WalletLockDao;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletLockEntity;
import net.lab1024.sa.igaming.wallet.domain.form.WalletDebitForm;
import net.lab1024.sa.igaming.wallet.manager.WalletManager;
import net.lab1024.sa.igaming.wallet.payment.dao.PaymentOrderDao;
import net.lab1024.sa.igaming.wallet.payment.domain.entity.PaymentOrderEntity;
import net.lab1024.sa.igaming.wallet.service.WalletService;
import org.springframework.stereotype.Service;

/**
 * Withdrawal approval service — handles risk proposal approval workflow and fund release.
 *
 * <p>This service orchestrates the manual approval workflow for withdrawal requests flagged by risk
 * assessment:
 *
 * <ol>
 *   <li>Validate risk proposal exists and is PENDING/ASSIGNED
 *   <li>Update proposal status to APPROVED/REJECTED
 *   <li>If APPROVED: Call PSP withdrawal → Update order status → Unlock and debit funds → Publish
 *       WITHDRAWAL_APPROVED event
 *   <li>If REJECTED: Unlock funds → Update order status → Publish WITHDRAWAL_REJECTED event
 * </ol>
 *
 * <p><b>Approval Flow (APPROVED):</b>
 *
 * <ul>
 *   <li>Update RiskProposal: status=APPROVED, resolvedAt=now
 *   <li>Load PaymentOrder by assessmentId
 *   <li>Call PSP withdrawal API (delegated to PSP adapter)
 *   <li>Update PaymentOrder: status=SUCCESS, pspTransactionId populated
 *   <li>Load WalletLock by referenceId (requestId)
 *   <li>Unlock funds: walletService.unlockFunds(lockId)
 *   <li>Debit wallet: walletManager.debit(...)
 *   <li>Publish WITHDRAWAL_APPROVED event
 * </ul>
 *
 * <p><b>Rejection Flow (REJECTED):</b>
 *
 * <ul>
 *   <li>Update RiskProposal: status=REJECTED, resolvedAt=now
 *   <li>Load PaymentOrder by assessmentId
 *   <li>Load WalletLock by referenceId (requestId)
 *   <li>Unlock funds: walletService.unlockFunds(lockId)
 *   <li>Update PaymentOrder: status=REJECTED
 *   <li>Publish WITHDRAWAL_REJECTED event
 * </ul>
 *
 * @author iGaming Team
 * @since 2026-03-18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawalApprovalService {

  private final RiskProposalDao riskProposalDao;
  private final RiskAssessmentDao riskAssessmentDao;
  private final PaymentOrderDao paymentOrderDao;
  private final WalletLockDao walletLockDao;
  private final WalletService walletService;
  private final WalletManager walletManager;
  private final DomainEventPublisher domainEventPublisher;

  /**
   * Approve withdrawal request.
   *
   * <p>This method approves a withdrawal request that was flagged for manual review. Upon approval,
   * the PSP withdrawal is initiated, funds are unlocked and debited, and the order status is
   * updated.
   *
   * @param proposalId risk proposal ID
   * @param reviewComment approval comment
   * @param operatorId operator ID (who approved)
   * @return approval result
   */
  public ResponseDTO<String> approveWithdrawal(
      Long proposalId, String reviewComment, Long operatorId) {
    log.info(
        "[WITHDRAWAL_APPROVAL] Starting approval workflow: proposalId={}, operatorId={}",
        proposalId,
        operatorId);

    // Step 1: Load and validate risk proposal
    RiskProposalEntity proposal = riskProposalDao.selectById(proposalId);
    if (proposal == null) {
      return ResponseDTO.userErrorParam("Risk proposal not found: " + proposalId);
    }

    if (proposal.getStatus().equals(RiskProposalStatusEnum.APPROVED.getValue())) {
      return ResponseDTO.userErrorParam("Proposal already approved");
    }

    if (proposal.getStatus().equals(RiskProposalStatusEnum.REJECTED.getValue())) {
      return ResponseDTO.userErrorParam("Proposal already rejected");
    }

    // Step 2: Update proposal status to APPROVED
    proposal.setStatus(RiskProposalStatusEnum.APPROVED.getValue());
    proposal.setReviewComment(reviewComment);
    proposal.setResolvedAt(OffsetDateTime.now(ZoneOffset.UTC));
    riskProposalDao.updateById(proposal);

    log.info(
        "[WITHDRAWAL_APPROVAL] Updated proposal status to APPROVED: proposalId={}", proposalId);

    // Step 3: Load payment order linked to this proposal
    PaymentOrderEntity order = loadPaymentOrderByAssessmentId(proposal.getAssessmentId());
    if (order == null) {
      log.error(
          "[WITHDRAWAL_APPROVAL] Payment order not found for assessmentId={}",
          proposal.getAssessmentId());
      return ResponseDTO.userErrorParam("Payment order not found");
    }

    // Step 4: TODO - Call PSP withdrawal API (skipped for now, requires PSP adapter implementation)
    // PspWithdrawResponse pspResponse = callPspWithdrawal(order);
    // order.setPspTransactionId(pspResponse.getTransactionId());

    // Step 5: Update order status to SUCCESS (will change to IN_PROGRESS when PSP implemented)
    order.setStatus(PaymentOrderStatusEnum.SUCCESS.getValue());
    paymentOrderDao.updateById(order);

    log.info(
        "[WITHDRAWAL_APPROVAL] Updated payment order status to SUCCESS: orderNo={}",
        order.getOrderNo());

    // Step 6: Unlock and debit funds
    WalletLockEntity lockEntity =
        walletLockDao.selectOne(
            Wrappers.<WalletLockEntity>lambdaQuery()
                .eq(WalletLockEntity::getReferenceId, order.getRequestId()));

    if (lockEntity == null) {
      log.error(
          "[WITHDRAWAL_APPROVAL] Lock entity not found for requestId={}", order.getRequestId());
      return ResponseDTO.userErrorParam("Lock entity not found");
    }

    // Unlock funds
    ResponseDTO<String> unlockResult = walletService.unlockFunds(lockEntity.getLockId());
    if (!unlockResult.getOk()) {
      log.error(
          "[WITHDRAWAL_APPROVAL] Failed to unlock funds: lockId={}, error={}",
          lockEntity.getLockId(),
          unlockResult.getMsg());
      return ResponseDTO.userErrorParam("Failed to unlock funds: " + unlockResult.getMsg());
    }

    log.info(
        "[WITHDRAWAL_APPROVAL] Unlocked funds: lockId={}, amount={}",
        lockEntity.getLockId(),
        lockEntity.getLockAmount());

    // Debit wallet
    WalletDebitForm debitForm = new WalletDebitForm();
    debitForm.setWalletId(order.getWalletId());
    debitForm.setAmount(order.getAmount());
    debitForm.setTransactionType(
        net.lab1024.sa.igaming.common.constant.TransactionTypeEnum.WITHDRAW.getValue());
    debitForm.setReferenceId(order.getOrderNo());
    debitForm.setRequestId(order.getRequestId() + "_debit"); // Append suffix for debit idempotency
    debitForm.setDescription("Withdrawal approved and processed");

    ResponseDTO<net.lab1024.sa.igaming.wallet.domain.vo.WalletTransactionVO> debitResult =
        walletService.debit(debitForm);
    if (!debitResult.getOk()) {
      log.error(
          "[WITHDRAWAL_APPROVAL] Failed to debit wallet: walletId={}, error={}",
          order.getWalletId(),
          debitResult.getMsg());
      return ResponseDTO.userErrorParam("Failed to debit wallet: " + debitResult.getMsg());
    }

    log.info(
        "[WITHDRAWAL_APPROVAL] Debited wallet: walletId={}, amount={}, transactionId={}",
        order.getWalletId(),
        order.getAmount(),
        debitResult.getData().getTransactionId());

    // Step 7: Publish WITHDRAWAL_APPROVED event
    publishWithdrawalApprovedEvent(order, proposal, operatorId);

    return ResponseDTO.ok(
        String.format(
            "Withdrawal approved and processed: orderNo=%s, amount=%s",
            order.getOrderNo(), order.getAmount()));
  }

  /**
   * Reject withdrawal request.
   *
   * <p>This method rejects a withdrawal request that was flagged for manual review. Upon rejection,
   * the locked funds are released back to the player's wallet.
   *
   * @param proposalId risk proposal ID
   * @param reviewComment rejection reason
   * @param operatorId operator ID (who rejected)
   * @return rejection result
   */
  public ResponseDTO<String> rejectWithdrawal(
      Long proposalId, String reviewComment, Long operatorId) {
    log.info(
        "[WITHDRAWAL_REJECTION] Starting rejection workflow: proposalId={}, operatorId={}",
        proposalId,
        operatorId);

    // Step 1: Load and validate risk proposal
    RiskProposalEntity proposal = riskProposalDao.selectById(proposalId);
    if (proposal == null) {
      return ResponseDTO.userErrorParam("Risk proposal not found: " + proposalId);
    }

    if (proposal.getStatus().equals(RiskProposalStatusEnum.APPROVED.getValue())) {
      return ResponseDTO.userErrorParam("Proposal already approved, cannot reject");
    }

    if (proposal.getStatus().equals(RiskProposalStatusEnum.REJECTED.getValue())) {
      return ResponseDTO.userErrorParam("Proposal already rejected");
    }

    // Step 2: Update proposal status to REJECTED
    proposal.setStatus(RiskProposalStatusEnum.REJECTED.getValue());
    proposal.setReviewComment(reviewComment);
    proposal.setResolvedAt(OffsetDateTime.now(ZoneOffset.UTC));
    riskProposalDao.updateById(proposal);

    log.info(
        "[WITHDRAWAL_REJECTION] Updated proposal status to REJECTED: proposalId={}", proposalId);

    // Step 3: Load payment order
    PaymentOrderEntity order = loadPaymentOrderByAssessmentId(proposal.getAssessmentId());
    if (order == null) {
      log.error(
          "[WITHDRAWAL_REJECTION] Payment order not found for assessmentId={}",
          proposal.getAssessmentId());
      return ResponseDTO.userErrorParam("Payment order not found");
    }

    // Step 4: Unlock funds
    WalletLockEntity lockEntity =
        walletLockDao.selectOne(
            Wrappers.<WalletLockEntity>lambdaQuery()
                .eq(WalletLockEntity::getReferenceId, order.getRequestId()));

    if (lockEntity == null) {
      log.error(
          "[WITHDRAWAL_REJECTION] Lock entity not found for requestId={}", order.getRequestId());
      return ResponseDTO.userErrorParam("Lock entity not found");
    }

    ResponseDTO<String> unlockResult = walletService.unlockFunds(lockEntity.getLockId());
    if (!unlockResult.getOk()) {
      log.error(
          "[WITHDRAWAL_REJECTION] Failed to unlock funds: lockId={}, error={}",
          lockEntity.getLockId(),
          unlockResult.getMsg());
      return ResponseDTO.userErrorParam("Failed to unlock funds: " + unlockResult.getMsg());
    }

    log.info(
        "[WITHDRAWAL_REJECTION] Unlocked funds: lockId={}, amount={}",
        lockEntity.getLockId(),
        lockEntity.getLockAmount());

    // Step 5: Update order status to REJECTED
    order.setStatus(PaymentOrderStatusEnum.REJECTED.getValue());
    paymentOrderDao.updateById(order);

    log.info(
        "[WITHDRAWAL_REJECTION] Updated payment order status to REJECTED: orderNo={}",
        order.getOrderNo());

    // Step 6: Publish WITHDRAWAL_REJECTED event
    publishWithdrawalRejectedEvent(order, proposal, operatorId);

    return ResponseDTO.ok(
        String.format(
            "Withdrawal rejected: orderNo=%s, reason=%s", order.getOrderNo(), reviewComment));
  }

  /**
   * Load payment order by assessment ID.
   *
   * <p>This method queries the RiskAssessmentEntity to retrieve the eventId (orderNo), then loads
   * the corresponding PaymentOrderEntity.
   *
   * @param assessmentId risk assessment ID
   * @return payment order entity, or null if not found
   */
  private PaymentOrderEntity loadPaymentOrderByAssessmentId(Long assessmentId) {
    // Step 1: Load risk assessment to get eventId (orderNo)
    RiskAssessmentEntity assessment = riskAssessmentDao.selectById(assessmentId);
    if (assessment == null) {
      log.error("[WITHDRAWAL_APPROVAL] Risk assessment not found: assessmentId={}", assessmentId);
      return null;
    }

    String orderNo = assessment.getEventId();
    if (orderNo == null || orderNo.isEmpty()) {
      log.error(
          "[WITHDRAWAL_APPROVAL] Risk assessment has no eventId: assessmentId={}", assessmentId);
      return null;
    }

    // Step 2: Load payment order by orderNo
    PaymentOrderEntity order =
        paymentOrderDao.selectOne(
            Wrappers.<PaymentOrderEntity>lambdaQuery()
                .eq(PaymentOrderEntity::getOrderNo, orderNo)
                .eq(PaymentOrderEntity::getOrderType, PaymentOrderTypeEnum.WITHDRAWAL.getValue()));

    if (order == null) {
      log.error(
          "[WITHDRAWAL_APPROVAL] Payment order not found: orderNo={}, assessmentId={}",
          orderNo,
          assessmentId);
    }

    return order;
  }

  /**
   * Publish WITHDRAWAL_APPROVED event to Kafka.
   *
   * @param order payment order
   * @param proposal risk proposal
   * @param operatorId operator ID
   */
  @SuppressWarnings("FutureReturnValueIgnored")
  private void publishWithdrawalApprovedEvent(
      PaymentOrderEntity order, RiskProposalEntity proposal, Long operatorId) {
    ObjectNode payload = JsonNodeFactory.instance.objectNode();
    payload.put("orderNo", order.getOrderNo());
    payload.put("playerId", order.getPlayerId());
    payload.put("amount", order.getAmount().toPlainString());
    payload.put("proposalId", proposal.getProposalId());
    payload.put("operatorId", operatorId);
    payload.put("reviewComment", proposal.getReviewComment());

    DomainEvent event =
        DomainEvent.builder()
            .eventType("WITHDRAWAL_APPROVED")
            .tenantId(order.getTenantId())
            .aggregateType("WITHDRAWAL")
            .aggregateId(order.getOrderNo())
            .payload(payload)
            .build();

    domainEventPublisher.publish(IgamingKafkaConst.Topic.RISK_EVENTS, event);

    log.debug(
        "[WITHDRAWAL_APPROVAL] Published WITHDRAWAL_APPROVED event: orderNo={}",
        order.getOrderNo());
  }

  /**
   * Publish WITHDRAWAL_REJECTED event to Kafka.
   *
   * @param order payment order
   * @param proposal risk proposal
   * @param operatorId operator ID
   */
  @SuppressWarnings("FutureReturnValueIgnored")
  private void publishWithdrawalRejectedEvent(
      PaymentOrderEntity order, RiskProposalEntity proposal, Long operatorId) {
    ObjectNode payload = JsonNodeFactory.instance.objectNode();
    payload.put("orderNo", order.getOrderNo());
    payload.put("playerId", order.getPlayerId());
    payload.put("amount", order.getAmount().toPlainString());
    payload.put("proposalId", proposal.getProposalId());
    payload.put("operatorId", operatorId);
    payload.put("reviewComment", proposal.getReviewComment());

    DomainEvent event =
        DomainEvent.builder()
            .eventType("WITHDRAWAL_REJECTED")
            .tenantId(order.getTenantId())
            .aggregateType("WITHDRAWAL")
            .aggregateId(order.getOrderNo())
            .payload(payload)
            .build();

    domainEventPublisher.publish(IgamingKafkaConst.Topic.RISK_EVENTS, event);

    log.debug(
        "[WITHDRAWAL_REJECTION] Published WITHDRAWAL_REJECTED event: orderNo={}",
        order.getOrderNo());
  }
}
