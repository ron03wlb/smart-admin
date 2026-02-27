package net.lab1024.sa.app.igaming.activity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import net.lab1024.sa.igaming.activity.dao.PlayerBonusRecordDao;
import net.lab1024.sa.igaming.activity.domain.entity.PlayerBonusRecordEntity;
import net.lab1024.sa.igaming.activity.domain.entity.PromotionRuleEntity;
import net.lab1024.sa.igaming.common.constant.BonusRecordStatusEnum;
import net.lab1024.sa.igaming.common.constant.PromotionStatusEnum;
import net.lab1024.sa.igaming.wallet.dao.WalletBonusExtDao;
import net.lab1024.sa.igaming.wallet.dao.WalletDao;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletBonusExtEntity;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletEntity;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletTransactionEntity;
import net.lab1024.sa.igaming.wallet.manager.WalletManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

/**
 * BonusDistributionManager unit tests.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BonusDistributionManager 單元測試")
@SuppressWarnings("unchecked")
class BonusDistributionManagerTest {

  @Mock private PlayerBonusRecordDao playerBonusRecordDao;
  @Mock private WalletBonusExtDao walletBonusExtDao;
  @Mock private WalletDao walletDao;
  @Mock private WalletManager walletManager;

  @InjectMocks
  private net.lab1024.sa.igaming.activity.manager.BonusDistributionManager bonusDistributionManager;

  @Nested
  @DisplayName("distributeBonus")
  class DistributeBonusTest {

    @Test
    @DisplayName("成功分發獎金")
    void distributeBonus_success() {
      PromotionRuleEntity rule = buildRule();
      WalletEntity bonusWallet = buildBonusWallet();
      when(walletDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(bonusWallet);

      WalletTransactionEntity txEntity = new WalletTransactionEntity();
      txEntity.setBalanceAfter(new BigDecimal("100.00"));
      when(walletManager.credit(any(WalletEntity.class), any(WalletTransactionEntity.class)))
          .thenReturn(txEntity);

      PlayerBonusRecordEntity result =
          bonusDistributionManager.distributeBonus(1L, rule, "claim-001", 1L);

      assertThat(result).isNotNull();
      assertThat(result.getBonusAmount()).isEqualByComparingTo("100.00");
      assertThat(result.getWageringRequired()).isEqualByComparingTo("2000.00");
      assertThat(result.getStatus()).isEqualTo(BonusRecordStatusEnum.ACTIVE.getValue());
      verify(playerBonusRecordDao).insert(any(PlayerBonusRecordEntity.class));
      verify(walletBonusExtDao).insert(any(WalletBonusExtEntity.class));
    }

    @Test
    @DisplayName("DuplicateKey — Layer 2 冪等性")
    void distributeBonus_duplicateKey() {
      PromotionRuleEntity rule = buildRule();
      doThrow(new DuplicateKeyException("Duplicate"))
          .when(playerBonusRecordDao)
          .insert(any(PlayerBonusRecordEntity.class));

      assertThatThrownBy(() -> bonusDistributionManager.distributeBonus(1L, rule, "claim-dup", 1L))
          .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    @DisplayName("BONUS 錢包不存在 — 拋出異常")
    void distributeBonus_walletNotFound() {
      PromotionRuleEntity rule = buildRule();
      when(walletDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

      assertThatThrownBy(() -> bonusDistributionManager.distributeBonus(1L, rule, "claim-002", 1L))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("BONUS wallet not found");
    }
  }

  private PromotionRuleEntity buildRule() {
    PromotionRuleEntity rule = new PromotionRuleEntity();
    rule.setRuleId(1L);
    rule.setTenantId(1L);
    rule.setPromotionCode("FIRST100");
    rule.setPromotionType(1);
    rule.setStatus(PromotionStatusEnum.ACTIVE.getValue());
    rule.setMaxBonus(new BigDecimal("100.00"));
    rule.setWageringMultiplier(new BigDecimal("20.00"));
    rule.setBonusExpiryDays(30);
    rule.setStartTime(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));
    rule.setEndTime(OffsetDateTime.now(ZoneOffset.UTC).plusDays(30));
    return rule;
  }

  private WalletEntity buildBonusWallet() {
    WalletEntity wallet = new WalletEntity();
    wallet.setWalletId(1L);
    wallet.setPlayerId(1L);
    wallet.setBalance(BigDecimal.ZERO);
    wallet.setLockedAmount(BigDecimal.ZERO);
    wallet.setDeleted(false);
    return wallet;
  }
}
