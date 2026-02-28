package net.lab1024.sa.igaming.wallet.payment.manager;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.igaming.common.constant.ReconciliationExceptionTypeEnum;
import net.lab1024.sa.igaming.common.constant.ReconciliationStatusEnum;
import net.lab1024.sa.igaming.wallet.payment.dao.PaymentOrderDao;
import net.lab1024.sa.igaming.wallet.payment.dao.PaymentReconciliationDao;
import net.lab1024.sa.igaming.wallet.payment.dao.ReconciliationExceptionDao;
import net.lab1024.sa.igaming.wallet.payment.domain.entity.PaymentOrderEntity;
import net.lab1024.sa.igaming.wallet.payment.domain.entity.PaymentReconciliationEntity;
import net.lab1024.sa.igaming.wallet.payment.domain.entity.ReconciliationExceptionEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Payment reconciliation manager — three-layer reconciliation logic.
 *
 * <p>Layer 1: Real-time transaction verification (called after PSP callback). Layer 2: Periodic
 * poll for pending orders (delegated to {@link
 * net.lab1024.sa.igaming.wallet.payment.service.ReconciliationService}). Layer 3: Daily batch
 * reconciliation (generates summary + exception records).
 *
 * @author iGaming Team
 * @since 2026-02-27
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentReconciliationManager {

  private final PaymentOrderDao paymentOrderDao;
  private final PaymentReconciliationDao reconciliationDao;
  private final ReconciliationExceptionDao exceptionDao;

  /**
   * Layer 1: Real-time transaction verification.
   *
   * <p>Called immediately after PSP callback to compare platform amount vs PSP-reported amount.
   * Creates an exception record if amounts don't match.
   *
   * @param orderNo platform order number
   * @param platformAmount amount recorded on platform side
   * @param pspAmount amount reported by PSP
   */
  @Transactional(rollbackFor = Throwable.class)
  public void verifyTransaction(String orderNo, BigDecimal platformAmount, BigDecimal pspAmount) {
    PaymentOrderEntity order =
        paymentOrderDao.selectOne(
            Wrappers.<PaymentOrderEntity>lambdaQuery().eq(PaymentOrderEntity::getOrderNo, orderNo));
    if (order == null) {
      log.warn("[Reconciliation] Order not found: orderNo={}", orderNo);
      return;
    }

    if (platformAmount.compareTo(pspAmount) == 0) {
      order.setReconciliationStatus(ReconciliationStatusEnum.VERIFIED.getValue());
      log.info("[Reconciliation] VERIFIED: orderNo={}", orderNo);
    } else {
      order.setReconciliationStatus(ReconciliationStatusEnum.MISMATCH.getValue());
      createExceptionRecord(
          null,
          orderNo,
          ReconciliationExceptionTypeEnum.AMOUNT_MISMATCH,
          platformAmount,
          pspAmount,
          order.getTenantId());
      log.warn(
          "[Reconciliation] MISMATCH: orderNo={}, platform={}, psp={}",
          orderNo,
          platformAmount,
          pspAmount);
    }
    paymentOrderDao.updateById(order);
  }

  /**
   * Layer 3: Daily batch reconciliation for a specific date.
   *
   * <p>Groups completed orders by PSP, calculates match/mismatch/missing counts, and creates
   * summary records in t_payment_reconciliation.
   *
   * @param date date to reconcile (typically yesterday)
   */
  @Transactional(rollbackFor = Throwable.class)
  public void dailyBatchReconciliation(LocalDate date) {
    OffsetDateTime dayStart = date.atStartOfDay().atOffset(ZoneOffset.UTC);
    OffsetDateTime dayEnd = date.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

    // Query all orders for the given date
    List<PaymentOrderEntity> orders =
        paymentOrderDao.selectList(
            Wrappers.<PaymentOrderEntity>lambdaQuery()
                .ge(PaymentOrderEntity::getCreateTime, dayStart)
                .lt(PaymentOrderEntity::getCreateTime, dayEnd));

    // Group by PSP code
    Map<String, List<PaymentOrderEntity>> byPsp =
        orders.stream().collect(Collectors.groupingBy(PaymentOrderEntity::getPspCode));

    for (Map.Entry<String, List<PaymentOrderEntity>> entry : byPsp.entrySet()) {
      String pspCode = entry.getKey();
      List<PaymentOrderEntity> pspOrders = entry.getValue();

      int matched = 0;
      int mismatch = 0;
      int missing = 0;
      BigDecimal totalPlatformAmount = BigDecimal.ZERO;

      for (PaymentOrderEntity order : pspOrders) {
        totalPlatformAmount = totalPlatformAmount.add(order.getAmount());
        Integer reconStatus = order.getReconciliationStatus();

        if (ReconciliationStatusEnum.VERIFIED.getValue().equals(reconStatus)
            || ReconciliationStatusEnum.COMPENSATED.getValue().equals(reconStatus)) {
          matched++;
        } else if (ReconciliationStatusEnum.MISMATCH.getValue().equals(reconStatus)) {
          mismatch++;
        } else {
          // PENDING — not yet verified
          missing++;
        }
      }

      // Determine tenant from first order
      Long tenantId = pspOrders.get(0).getTenantId();

      PaymentReconciliationEntity recon = new PaymentReconciliationEntity();
      recon.setTenantId(tenantId);
      recon.setReconciliationDate(date);
      recon.setPspCode(pspCode);
      recon.setTotalPspTransactions(pspOrders.size());
      recon.setTotalPlatformTransactions(pspOrders.size());
      recon.setMatchedCount(matched);
      recon.setMismatchCount(mismatch);
      recon.setMissingCount(missing);
      recon.setExtraCount(0);
      recon.setTotalPlatformAmount(totalPlatformAmount);
      recon.setTotalPspAmount(totalPlatformAmount); // Phase 2: replace with actual PSP report
      recon.setAmountDifference(BigDecimal.ZERO);
      recon.setStatus(
          mismatch > 0 || missing > 0
              ? ReconciliationStatusEnum.MISMATCH.getValue()
              : ReconciliationStatusEnum.RECONCILED.getValue());
      recon.setDeleted(false);
      reconciliationDao.insert(recon);

      log.info(
          "[Reconciliation] Daily batch for {} on {}: matched={}, mismatch={}, missing={}",
          pspCode,
          date,
          matched,
          mismatch,
          missing);
    }
  }

  /**
   * Mark an order as compensated (Layer 2 poll completed).
   *
   * @param orderNo platform order number
   */
  @Transactional(rollbackFor = Throwable.class)
  public void markCompensated(String orderNo) {
    PaymentOrderEntity order =
        paymentOrderDao.selectOne(
            Wrappers.<PaymentOrderEntity>lambdaQuery().eq(PaymentOrderEntity::getOrderNo, orderNo));
    if (order != null) {
      order.setReconciliationStatus(ReconciliationStatusEnum.COMPENSATED.getValue());
      paymentOrderDao.updateById(order);
    }
  }

  private void createExceptionRecord(
      Long reconciliationId,
      String orderNo,
      ReconciliationExceptionTypeEnum exceptionType,
      BigDecimal platformAmount,
      BigDecimal pspAmount,
      Long tenantId) {
    ReconciliationExceptionEntity exception = new ReconciliationExceptionEntity();
    exception.setTenantId(tenantId);
    exception.setReconciliationId(reconciliationId);
    exception.setOrderNo(orderNo);
    exception.setExceptionType(exceptionType.getValue());
    exception.setPlatformAmount(platformAmount);
    exception.setPspAmount(pspAmount);
    exception.setDifferenceAmount(platformAmount.subtract(pspAmount));
    exception.setResolutionStatus(1); // PENDING
    exception.setDeleted(false);
    exceptionDao.insert(exception);
  }
}
