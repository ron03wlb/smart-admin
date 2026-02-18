package net.lab1024.sa.igaming.wallet.payment.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.vavr.control.Option;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.igaming.common.constant.PaymentOrderStatusEnum;
import net.lab1024.sa.igaming.common.constant.PaymentOrderTypeEnum;
import net.lab1024.sa.igaming.wallet.payment.dao.PaymentOrderDao;
import net.lab1024.sa.igaming.wallet.payment.domain.dto.PspQueryResponse;
import net.lab1024.sa.igaming.wallet.payment.domain.entity.PaymentOrderEntity;
import net.lab1024.sa.igaming.wallet.payment.manager.PaymentManager;
import net.lab1024.sa.igaming.wallet.payment.psp.PaymentProviderAdapter;
import net.lab1024.sa.igaming.wallet.payment.psp.PspAdapterFactory;
import org.springframework.stereotype.Service;

/**
 * Reconciliation Service — polls PSP for pending order status (Layer 2 reconciliation).
 *
 * <p>Phase 1.5 only implements poll-based reconciliation for stale pending orders. Phase 2 will add
 * batch file reconciliation (Layer 3).
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReconciliationService {

  private final PaymentOrderDao paymentOrderDao;
  private final PaymentManager paymentManager;
  private final PspAdapterFactory pspAdapterFactory;
  private final PaymentService paymentService;

  /**
   * Reconcile pending payment orders that are older than the given threshold.
   *
   * <p>Queries all PENDING/PROCESSING orders older than {@code pendingMinutesThreshold} minutes,
   * polls the PSP for their actual status, and updates accordingly.
   *
   * @param pendingMinutesThreshold minimum age in minutes for orders to reconcile
   * @return count of orders processed
   */
  public int reconcilePendingOrders(int pendingMinutesThreshold) {
    OffsetDateTime cutoff = OffsetDateTime.now().minusMinutes(pendingMinutesThreshold);

    List<PaymentOrderEntity> staleOrders =
        paymentOrderDao.selectList(
            Wrappers.<PaymentOrderEntity>lambdaQuery()
                .in(
                    PaymentOrderEntity::getStatus,
                    PaymentOrderStatusEnum.PENDING.getValue(),
                    PaymentOrderStatusEnum.PROCESSING.getValue())
                .lt(PaymentOrderEntity::getCreateTime, cutoff));

    int processed = 0;
    for (PaymentOrderEntity order : staleOrders) {
      try {
        boolean reconciled = reconcileOrder(order);
        if (reconciled) {
          processed++;
        }
      } catch (Exception e) {
        log.warn("Failed to reconcile order {}: {}", order.getOrderNo(), e.getMessage(), e);
      }
    }

    log.info(
        "Reconciliation complete: {} of {} stale orders processed", processed, staleOrders.size());
    return processed;
  }

  private boolean reconcileOrder(PaymentOrderEntity order) {
    Option<PaymentProviderAdapter> adapterOpt = pspAdapterFactory.getAdapter(order.getPspCode());
    if (adapterOpt.isEmpty()) {
      log.warn("No adapter found for PSP code: {}", order.getPspCode());
      return false;
    }

    if (order.getPspTransactionId() == null) {
      log.debug("Order {} has no PSP transaction ID, skipping", order.getOrderNo());
      return false;
    }

    Option<PspQueryResponse> queryResult =
        adapterOpt.get().queryStatus(order.getPspTransactionId());
    if (queryResult.isEmpty()) {
      log.debug("PSP returned no status for order {}", order.getOrderNo());
      return false;
    }

    PspQueryResponse pspStatus = queryResult.get();
    String status = pspStatus.getStatus();

    if ("SUCCESS".equalsIgnoreCase(status)) {
      return reconcileSuccess(order);
    } else if ("FAILED".equalsIgnoreCase(status) || "REJECTED".equalsIgnoreCase(status)) {
      return reconcileFailure(order);
    }

    // Still pending at PSP — no action
    return false;
  }

  private boolean reconcileSuccess(PaymentOrderEntity order) {
    if (order.getOrderType().equals(PaymentOrderTypeEnum.DEPOSIT.getValue())) {
      paymentService.processDepositCallback(
          order.getOrderNo(), order.getPspTransactionId(), "reconciliation");
      return true;
    } else if (order.getOrderType().equals(PaymentOrderTypeEnum.WITHDRAWAL.getValue())) {
      paymentService.processWithdrawalCallback(
          order.getOrderNo(), order.getPspTransactionId(), "reconciliation", true);
      return true;
    }
    return false;
  }

  private boolean reconcileFailure(PaymentOrderEntity order) {
    if (order.getOrderType().equals(PaymentOrderTypeEnum.WITHDRAWAL.getValue())) {
      paymentService.processWithdrawalCallback(
          order.getOrderNo(), order.getPspTransactionId(), "reconciliation", false);
    } else {
      paymentManager.failOrder(order, PaymentOrderStatusEnum.FAILED);
    }
    return true;
  }
}
