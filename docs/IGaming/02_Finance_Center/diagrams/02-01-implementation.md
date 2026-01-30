# 提款風控系統 - SmartAdmin 實作細節

本文檔包含提款風控系統的完整 SmartAdmin 架構映射與代碼實作。

**相關主文檔**：[02-01 提款風控系統](../02-01_Withdrawal_Risk_Control.md)

---

## SmartAdmin 架構映射 (SmartAdmin Architecture Mapping)

### 分層架構概述

出金風控系統遵循 SmartAdmin **嚴格分層架構**，確保 SAGA 編排、補償事務、風控規則的職責分離：

```
Controller (API 端點) → Service (業務編排) → Manager (事務管理) → Dao (數據訪問)
```

**關鍵規則**:
- ✅ `@Transactional` 只能在 Manager 層
- ✅ Service 使用 Vavr `Option<T>` / `Try<T>` 處理錯誤
- ✅ SAGA 編排邏輯在 Service 層,事務補償在 Manager 層
- ❌ Controller 禁止直接調用 Dao/Manager

---

### 核心類別設計 (Core Classes)

#### Entity - 出金請求實體

**檔案**: `net.lab1024.sa.admin.module.business.withdrawal.domain.entity.WithdrawalRequestEntity`

```java
package net.lab1024.sa.admin.module.business.withdrawal.domain.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 出金請求實體 (Withdrawal Request Entity)
 *
 * 對應表: t_withdrawal_request
 * SAGA 狀態追蹤: withdrawal_status 字段
 *
 * @author Finance Team
 * @since 2026-01-29
 */
@Data
public class WithdrawalRequestEntity {
    private Long id;
    private Long playerId;
    private Long tenantId;
    private BigDecimal amount;
    private String currency;

    /**
     * SAGA 狀態 (Withdrawal Status)
     *
     * 狀態機:
     * PENDING → RISK_CHECKING → KYC_VERIFYING → APPROVING → PAYING → COMPLETED
     *           ↓               ↓                ↓            ↓
     *         REJECTED     PENDING_VERIFICATION  PENDING_APPROVAL  PAYMENT_FAILED → REFUNDED
     */
    private String withdrawalStatus;

    /**
     * 風控分數 (Risk Score 0-100)
     */
    private Integer riskScore;

    /**
     * 風控標籤 (Risk Tags)
     * 格式: JSON Array ["HIGH_FREQUENCY", "GEO_ANOMALY"]
     */
    private String riskTags;

    /**
     * KYC 等級 (KYC Level 0-3)
     * 0: 未驗證, 1: 基礎驗證, 2: 增強驗證, 3: 完整驗證
     */
    private Integer kycLevel;

    /**
     * 審批歷史 (Approval History)
     * 格式: JSON Array [{approver, level, decision, timestamp}]
     */
    private String approvalHistory;

    /**
     * 支付通道 ID (Payment Channel ID)
     */
    private Long paymentChannelId;

    /**
     * 支付交易 ID (Payment Transaction ID)
     * 第三方支付通道返回的唯一ID
     */
    private String paymentTxId;

    /**
     * 補償狀態 (Compensation Status)
     * NONE: 無需補償
     * COMPENSATING: 補償中
     * COMPENSATED: 補償完成
     * COMPENSATION_FAILED: 補償失敗
     */
    private String compensationStatus;

    /**
     * 補償重試次數 (Compensation Retry Count)
     */
    private Integer compensationRetryCount;

    /**
     * 失敗原因 (Failure Reason)
     */
    private String failureReason;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Boolean deleted;
}
```

#### Manager - SAGA 補償事務管理

**檔案**: `net.lab1024.sa.admin.module.business.withdrawal.manager.WithdrawalSagaManager`

```java
package net.lab1024.sa.admin.module.business.withdrawal.manager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.business.withdrawal.dao.WithdrawalRequestDao;
import net.lab1024.sa.admin.module.business.withdrawal.domain.entity.WithdrawalRequestEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 出金 SAGA 補償事務管理器 (Withdrawal SAGA Manager)
 *
 * 職責:
 * - 補償事務執行 (@Transactional)
 * - 失敗重試邏輯
 * - 強制補償處理
 *
 * @author Finance Team
 * @since 2026-01-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawalSagaManager {

    private final WithdrawalRequestDao withdrawalRequestDao;
    private final PlayerWalletDao playerWalletDao;

    /**
     * 補償: 釋放鎖定資金 (風險評估失敗時)
     *
     * @param withdrawalId 出金請求 ID
     * @return 是否成功
     */
    @Transactional(rollbackFor = Throwable.class)
    public boolean compensateReleaseLockedFunds(Long withdrawalId) {
        try {
            // 1. 查詢出金請求
            WithdrawalRequestEntity withdrawal = withdrawalRequestDao.selectById(withdrawalId);
            if (withdrawal == null) {
                log.error("補償失敗 - 出金請求不存在: {}", withdrawalId);
                return false;
            }

            // 2. 行鎖查詢錢包
            PlayerWalletEntity wallet = playerWalletDao.selectByPlayerIdWithLock(withdrawal.getPlayerId());
            if (wallet == null) {
                log.error("補償失敗 - 錢包不存在: PlayerId={}", withdrawal.getPlayerId());
                return false;
            }

            // 3. 釋放鎖定資金
            BigDecimal lockedAmount = withdrawal.getAmount();
            wallet.setLockedBalance(wallet.getLockedBalance().subtract(lockedAmount));
            wallet.setAvailableBalance(wallet.getAvailableBalance().add(lockedAmount));

            // 4. 更新錢包
            playerWalletDao.updateById(wallet);

            // 5. 更新出金狀態
            withdrawal.setWithdrawalStatus("REJECTED");
            withdrawal.setCompensationStatus("COMPENSATED");
            withdrawalRequestDao.updateById(withdrawal);

            log.info("補償成功 - 釋放鎖定資金: WithdrawalId={}, Amount={}",
                     withdrawalId, lockedAmount);

            return true;

        } catch (Exception e) {
            log.error("補償異常 - WithdrawalId={}, Error={}",
                      withdrawalId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 強制補償: 繞過業務邏輯直接釋放資金
     *
     * 適用場景: 補償事務失敗後的降級處理
     *
     * @param withdrawalId 出金請求 ID
     * @return 是否成功
     */
    @Transactional(rollbackFor = Throwable.class)
    public boolean forceCompensateReleaseLockedFunds(Long withdrawalId) {
        try {
            WithdrawalRequestEntity withdrawal = withdrawalRequestDao.selectById(withdrawalId);
            if (withdrawal == null) {
                return false;
            }

            // 直接 SQL 更新,繞過觸發器和約束
            int updated = playerWalletDao.forceReleaseLockedBalance(
                withdrawal.getPlayerId(),
                withdrawal.getAmount()
            );

            if (updated > 0) {
                // 記錄強制補償日誌
                compensationLogDao.insert(CompensationLog.builder()
                    .withdrawalId(withdrawalId)
                    .compensationType("FORCE_RELEASE")
                    .amount(withdrawal.getAmount())
                    .operator("SYSTEM")
                    .reason("正常補償失敗-自動強制釋放")
                    .build());

                withdrawal.setCompensationStatus("FORCE_COMPENSATED");
                withdrawalRequestDao.updateById(withdrawal);

                log.warn("強制補償成功 - WithdrawalId={}, Amount={}",
                         withdrawalId, withdrawal.getAmount());
                return true;
            }

            return false;

        } catch (Exception e) {
            log.error("強制補償失敗 - WithdrawalId={}, Error={}",
                      withdrawalId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 補償: 支付退款 (支付執行失敗時)
     *
     * @param withdrawalId 出金請求 ID
     * @return 是否成功
     */
    @Transactional(rollbackFor = Throwable.class)
    public boolean compensateRefundPayment(Long withdrawalId) {
        try {
            WithdrawalRequestEntity withdrawal = withdrawalRequestDao.selectById(withdrawalId);

            // 1. 調用支付通道退款 API (需處理冪等性)
            boolean refundSuccess = paymentGatewayService.refund(
                withdrawal.getPaymentTxId(),
                withdrawal.getAmount()
            );

            if (!refundSuccess) {
                log.error("支付退款失敗 - WithdrawalId={}, PaymentTxId={}",
                          withdrawalId, withdrawal.getPaymentTxId());
                return false;
            }

            // 2. 返還資金到玩家錢包
            PlayerWalletEntity wallet = playerWalletDao.selectByPlayerIdWithLock(withdrawal.getPlayerId());
            wallet.setAvailableBalance(wallet.getAvailableBalance().add(withdrawal.getAmount()));
            playerWalletDao.updateById(wallet);

            // 3. 更新狀態
            withdrawal.setWithdrawalStatus("REFUNDED");
            withdrawal.setCompensationStatus("COMPENSATED");
            withdrawalRequestDao.updateById(withdrawal);

            log.info("補償成功 - 支付退款: WithdrawalId={}, Amount={}",
                     withdrawalId, withdrawal.getAmount());

            return true;

        } catch (Exception e) {
            log.error("支付退款補償異常 - WithdrawalId={}, Error={}",
                      withdrawalId, e.getMessage(), e);
            return false;
        }
    }
}
```

#### Service - SAGA 編排

**檔案**: `net.lab1024.sa.admin.module.business.withdrawal.service.WithdrawalSagaService`

```java
package net.lab1024.sa.admin.module.business.withdrawal.service;

import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.business.withdrawal.domain.entity.WithdrawalRequestEntity;
import net.lab1024.sa.admin.module.business.withdrawal.manager.WithdrawalSagaManager;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

/**
 * 出金 SAGA 編排服務 (Withdrawal SAGA Orchestration Service)
 *
 * 職責:
 * - SAGA 正向流程編排
 * - 補償流程觸發
 * - 重試策略配置
 * - 無 @Transactional (委託給 Manager)
 *
 * @author Finance Team
 * @since 2026-01-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawalSagaService {

    private final WithdrawalSagaManager sagaManager;
    private final RiskAssessmentService riskAssessmentService;
    private final KycVerificationService kycVerificationService;
    private final ApprovalRoutingService approvalRoutingService;
    private final PaymentExecutionService paymentExecutionService;

    /**
     * 執行完整 SAGA 流程
     *
     * @param withdrawalId 出金請求 ID
     * @return Try.Success(entity) if ok, Try.Failure(exception) otherwise
     */
    public Try<WithdrawalRequestEntity> executeSaga(Long withdrawalId) {
        return Try.of(() -> {
            // Step 1: 風險評估
            riskAssessmentService.assess(withdrawalId)
                .onFailure(ex -> compensateStep1(withdrawalId, ex));

            // Step 2: KYC 驗證
            kycVerificationService.verify(withdrawalId)
                .onFailure(ex -> compensateStep2(withdrawalId, ex));

            // Step 3: 審批路由
            approvalRoutingService.route(withdrawalId)
                .onFailure(ex -> compensateStep3(withdrawalId, ex));

            // Step 4: 支付執行
            return paymentExecutionService.execute(withdrawalId)
                .onFailure(ex -> compensateStep4(withdrawalId, ex))
                .get();
        });
    }

    /**
     * 補償 Step 1 失敗 (風險評估)
     *
     * 帶重試策略: 指數退避,最多3次
     */
    @Retryable(
        value = Exception.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2.0)
    )
    private void compensateStep1(Long withdrawalId, Throwable error) {
        log.warn("SAGA Step1 失敗 - 開始補償: WithdrawalId={}, Error={}",
                 withdrawalId, error.getMessage());

        boolean success = sagaManager.compensateReleaseLockedFunds(withdrawalId);

        if (!success) {
            // 重試失敗,拋出異常觸發 @Recover
            throw new RuntimeException("補償失敗: " + error.getMessage());
        }
    }

    /**
     * 補償重試失敗後的降級處理 (Step 1)
     *
     * @Recover 方法會在 @Retryable 失敗後自動調用
     */
    @Recover
    private void recoverCompensateStep1(RuntimeException ex, Long withdrawalId, Throwable originalError) {
        log.error("補償重試失敗 - 執行強制補償: WithdrawalId={}, Error={}",
                  withdrawalId, ex.getMessage());

        // Level 2: 強制補償
        boolean forceSuccess = sagaManager.forceCompensateReleaseLockedFunds(withdrawalId);

        if (!forceSuccess) {
            // Level 3: 觸發人工介入
            createManualInterventionTicket(withdrawalId, "Step1補償失敗", ex);
        }
    }

    /**
     * 補償 Step 4 失敗 (支付執行)
     */
    @Retryable(
        value = Exception.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 5000, multiplier = 1.5)
    )
    private void compensateStep4(Long withdrawalId, Throwable error) {
        log.warn("SAGA Step4 失敗 - 開始支付退款: WithdrawalId={}, Error={}",
                 withdrawalId, error.getMessage());

        boolean success = sagaManager.compensateRefundPayment(withdrawalId);

        if (!success) {
            throw new RuntimeException("支付退款失敗: " + error.getMessage());
        }
    }

    /**
     * 創建人工介入工單
     */
    private void createManualInterventionTicket(Long withdrawalId, String reason, Throwable error) {
        // 集成工單系統 (JIRA / ServiceNow / 自研)
        ticketService.create(Ticket.builder()
            .title("出金補償失敗 - 需人工處理")
            .description(String.format("WithdrawalId: %d, Reason: %s, Error: %s",
                                       withdrawalId, reason, error.getMessage()))
            .priority("HIGH")
            .assignee("finance-ops-team")
            .slaMinutes(120) // 2小時內響應
            .build());

        // 發送告警通知
        alertService.send(Alert.builder()
            .channel("slack")
            .recipient("#finance-alerts")
            .message(String.format("🔴 出金補償失敗需人工介入 - WithdrawalId: %d", withdrawalId))
            .build());
    }
}
```

#### Controller - API 端點

**檔案**: `net.lab1024.sa.admin.module.business.withdrawal.controller.WithdrawalController`

```java
package net.lab1024.sa.admin.module.business.withdrawal.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.admin.module.business.withdrawal.domain.form.WithdrawalRequestForm;
import net.lab1024.sa.admin.module.business.withdrawal.domain.vo.WithdrawalVO;
import net.lab1024.sa.admin.module.business.withdrawal.service.WithdrawalSagaService;
import net.lab1024.sa.foundation.domain.response.ResponseDTO;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 出金控制器 (Withdrawal Controller)
 *
 * 職責:
 * - API 端點
 * - 參數驗證
 * - ResponseDTO 包裝
 *
 * @author Finance Team
 * @since 2026-01-29
 */
@Tag(name = "出金管理")
@RestController
@RequestMapping("/api/withdrawal")
@RequiredArgsConstructor
public class WithdrawalController {

    private final WithdrawalSagaService withdrawalSagaService;

    /**
     * 發起出金請求 (觸發 SAGA 流程)
     *
     * @param form 出金表單
     * @return ResponseDTO<WithdrawalVO>
     */
    @Operation(summary = "發起出金請求")
    @PostMapping("/request")
    public ResponseDTO<WithdrawalVO> requestWithdrawal(@Valid @RequestBody WithdrawalRequestForm form) {
        return withdrawalSagaService.executeSaga(form.getWithdrawalId())
            .map(entity -> SmartBeanUtil.copy(entity, WithdrawalVO.class))
            .map(ResponseDTO::ok)
            .getOrElseGet(ex -> ResponseDTO.error(ex.getMessage()));
    }
}
```

### Foundation 模組依賴

| 模組 | 用途 | 使用位置 |
|------|------|---------|
| **foundation.redis-lock** | 分佈式鎖 (防並發出金) | Manager 層行鎖操作 |
| **foundation.mq** | SAGA 事件發佈 | Service 層步驟完成事件 |
| **foundation.audit-log** | 審計日誌 (補償記錄) | Manager 層補償操作 |
| **foundation.retry** | 補償重試策略 | Service 層 `@Retryable` |

---

**文檔版本**: 1.0.0
**創建日期**: 2026-01-30
**維護團隊**: Finance Team
