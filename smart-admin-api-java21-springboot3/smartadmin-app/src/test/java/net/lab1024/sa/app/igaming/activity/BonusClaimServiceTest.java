package net.lab1024.sa.app.igaming.activity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.vavr.control.Option;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.igaming.activity.dao.PlayerBonusRecordDao;
import net.lab1024.sa.igaming.activity.dao.PromotionRuleDao;
import net.lab1024.sa.igaming.activity.domain.entity.PlayerBonusRecordEntity;
import net.lab1024.sa.igaming.activity.domain.entity.PromotionRuleEntity;
import net.lab1024.sa.igaming.activity.domain.form.BonusClaimForm;
import net.lab1024.sa.igaming.activity.domain.vo.BonusClaimResultVO;
import net.lab1024.sa.igaming.activity.domain.vo.WageringProgressVO;
import net.lab1024.sa.igaming.activity.manager.BonusDistributionManager;
import net.lab1024.sa.igaming.activity.manager.PromotionCacheManager;
import net.lab1024.sa.igaming.activity.service.BonusClaimService;
import net.lab1024.sa.igaming.common.code.ActivityErrorCode;
import net.lab1024.sa.igaming.common.constant.BonusRecordStatusEnum;
import net.lab1024.sa.igaming.common.constant.PromotionStatusEnum;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

/**
 * BonusClaimService unit tests.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BonusClaimService 單元測試")
class BonusClaimServiceTest {

  @Mock private PromotionRuleDao promotionRuleDao;
  @Mock private PlayerBonusRecordDao playerBonusRecordDao;
  @Mock private PromotionCacheManager promotionCacheManager;
  @Mock private BonusDistributionManager bonusDistributionManager;
  @InjectMocks private BonusClaimService bonusClaimService;

  private MockedStatic<TenantContext> tenantContextMock;

  @BeforeEach
  void setUp() {
    tenantContextMock = Mockito.mockStatic(TenantContext.class);
    tenantContextMock.when(TenantContext::getTenantId).thenReturn(1L);
  }

  @AfterEach
  void tearDown() {
    tenantContextMock.close();
  }

  @Nested
  @DisplayName("claimBonus")
  class ClaimBonusTest {

    @Test
    @DisplayName("成功領取")
    void claimBonus_success() {
      PromotionRuleEntity rule = buildRule();
      when(promotionCacheManager.getRuleByCode(1L, "FIRST100")).thenReturn(rule);

      PlayerBonusRecordEntity record = new PlayerBonusRecordEntity();
      record.setRecordId(100L);
      record.setBonusAmount(new BigDecimal("100.00"));
      record.setWageringRequired(new BigDecimal("2000.00"));
      record.setStatus(BonusRecordStatusEnum.ACTIVE.getValue());
      when(bonusDistributionManager.distributeBonus(eq(1L), eq(rule), eq("claim-001"), eq(1L)))
          .thenReturn(record);

      BonusClaimForm form = new BonusClaimForm();
      form.setPromotionCode("FIRST100");
      form.setClaimId("claim-001");

      ResponseDTO<BonusClaimResultVO> result = bonusClaimService.claimBonus(1L, form);

      assertThat(result.getOk()).isTrue();
      assertThat(result.getData().getRecordId()).isEqualTo(100L);
      assertThat(result.getData().getBonusAmount()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("規則不存在 — 失敗")
    void claimBonus_ruleNotFound() {
      when(promotionCacheManager.getRuleByCode(1L, "INVALID")).thenReturn(null);

      BonusClaimForm form = new BonusClaimForm();
      form.setPromotionCode("INVALID");
      form.setClaimId("claim-001");

      ResponseDTO<BonusClaimResultVO> result = bonusClaimService.claimBonus(1L, form);

      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains(ActivityErrorCode.PROMOTION_NOT_FOUND.getMsg());
    }

    @Test
    @DisplayName("規則已暫停 — 失敗")
    void claimBonus_ruleDisabled() {
      PromotionRuleEntity rule = buildRule();
      rule.setStatus(PromotionStatusEnum.PAUSED.getValue());
      when(promotionCacheManager.getRuleByCode(1L, "FIRST100")).thenReturn(rule);

      BonusClaimForm form = new BonusClaimForm();
      form.setPromotionCode("FIRST100");
      form.setClaimId("claim-001");

      ResponseDTO<BonusClaimResultVO> result = bonusClaimService.claimBonus(1L, form);

      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains(ActivityErrorCode.PROMOTION_DISABLED.getMsg());
    }

    @Test
    @DisplayName("規則已過期 — 失敗")
    void claimBonus_ruleExpired() {
      PromotionRuleEntity rule = buildRule();
      rule.setEndTime(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));
      when(promotionCacheManager.getRuleByCode(1L, "FIRST100")).thenReturn(rule);

      BonusClaimForm form = new BonusClaimForm();
      form.setPromotionCode("FIRST100");
      form.setClaimId("claim-001");

      ResponseDTO<BonusClaimResultVO> result = bonusClaimService.claimBonus(1L, form);

      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains(ActivityErrorCode.PROMOTION_EXPIRED.getMsg());
    }

    @Test
    @DisplayName("領取次數已滿 — Manager 拋 IllegalStateException")
    void claimBonus_maxClaims() {
      PromotionRuleEntity rule = buildRule();
      when(promotionCacheManager.getRuleByCode(1L, "FIRST100")).thenReturn(rule);
      when(bonusDistributionManager.distributeBonus(eq(1L), eq(rule), eq("claim-001"), eq(1L)))
          .thenThrow(new IllegalStateException(ActivityErrorCode.MAX_CLAIMS_REACHED.getMsg()));

      BonusClaimForm form = new BonusClaimForm();
      form.setPromotionCode("FIRST100");
      form.setClaimId("claim-001");

      ResponseDTO<BonusClaimResultVO> result = bonusClaimService.claimBonus(1L, form);

      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains(ActivityErrorCode.MAX_CLAIMS_REACHED.getMsg());
    }

    @Test
    @DisplayName("重複 claim_id — 冪等性")
    void claimBonus_duplicate() {
      PromotionRuleEntity rule = buildRule();
      when(promotionCacheManager.getRuleByCode(1L, "FIRST100")).thenReturn(rule);
      when(bonusDistributionManager.distributeBonus(eq(1L), eq(rule), eq("claim-dup"), eq(1L)))
          .thenThrow(new DuplicateKeyException("Duplicate"));

      BonusClaimForm form = new BonusClaimForm();
      form.setPromotionCode("FIRST100");
      form.setClaimId("claim-dup");

      ResponseDTO<BonusClaimResultVO> result = bonusClaimService.claimBonus(1L, form);

      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains(ActivityErrorCode.ALREADY_CLAIMED.getMsg());
    }
  }

  @Nested
  @DisplayName("getWageringProgress")
  class GetWageringProgressTest {

    @Test
    @DisplayName("成功取得流水進度")
    void getProgress_success() {
      PlayerBonusRecordEntity record = buildBonusRecord();
      when(playerBonusRecordDao.selectById(100L)).thenReturn(record);

      PromotionRuleEntity rule = buildRule();
      when(promotionRuleDao.selectById(1L)).thenReturn(rule);

      Option<WageringProgressVO> result = bonusClaimService.getWageringProgress(1L, 100L);

      assertThat(result.isDefined()).isTrue();
      assertThat(result.get().getRecordId()).isEqualTo(100L);
      assertThat(result.get().getProgressPercent()).isEqualByComparingTo("25.00");
    }

    @Test
    @DisplayName("記錄不存在 — 返回 none")
    void getProgress_notFound() {
      when(playerBonusRecordDao.selectById(100L)).thenReturn(null);

      Option<WageringProgressVO> result = bonusClaimService.getWageringProgress(1L, 100L);

      assertThat(result.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("非本人記錄 — 返回 none")
    void getProgress_wrongPlayer() {
      PlayerBonusRecordEntity record = buildBonusRecord();
      when(playerBonusRecordDao.selectById(100L)).thenReturn(record);

      Option<WageringProgressVO> result = bonusClaimService.getWageringProgress(999L, 100L);

      assertThat(result.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("已刪除記錄 — 返回 none")
    void getProgress_deleted() {
      PlayerBonusRecordEntity record = buildBonusRecord();
      record.setDeleted(true);
      when(playerBonusRecordDao.selectById(100L)).thenReturn(record);

      Option<WageringProgressVO> result = bonusClaimService.getWageringProgress(1L, 100L);

      assertThat(result.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("wageringRequired 為零 — 百分比為 100")
    void getProgress_zeroWagering() {
      PlayerBonusRecordEntity record = buildBonusRecord();
      record.setWageringRequired(BigDecimal.ZERO);
      when(playerBonusRecordDao.selectById(100L)).thenReturn(record);
      when(promotionRuleDao.selectById(1L)).thenReturn(buildRule());

      Option<WageringProgressVO> result = bonusClaimService.getWageringProgress(1L, 100L);

      assertThat(result.isDefined()).isTrue();
      assertThat(result.get().getProgressPercent()).isEqualByComparingTo("100");
    }
  }

  private PromotionRuleEntity buildRule() {
    PromotionRuleEntity rule = new PromotionRuleEntity();
    rule.setRuleId(1L);
    rule.setTenantId(1L);
    rule.setPromotionCode("FIRST100");
    rule.setPromotionName("First Deposit 100%");
    rule.setPromotionType(1);
    rule.setStatus(PromotionStatusEnum.ACTIVE.getValue());
    rule.setStartTime(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));
    rule.setEndTime(OffsetDateTime.now(ZoneOffset.UTC).plusDays(30));
    rule.setMaxBonus(new BigDecimal("100.00"));
    rule.setWageringMultiplier(new BigDecimal("20.00"));
    rule.setMaxClaimsPerPlayer(1);
    rule.setBonusExpiryDays(30);
    rule.setDeleted(false);
    return rule;
  }

  private PlayerBonusRecordEntity buildBonusRecord() {
    PlayerBonusRecordEntity record = new PlayerBonusRecordEntity();
    record.setRecordId(100L);
    record.setPlayerId(1L);
    record.setRuleId(1L);
    record.setClaimId("claim-001");
    record.setBonusAmount(new BigDecimal("100.00"));
    record.setWageringRequired(new BigDecimal("2000.00"));
    record.setWageringCompleted(new BigDecimal("500.00"));
    record.setStatus(BonusRecordStatusEnum.ACTIVE.getValue());
    record.setClaimedAt(OffsetDateTime.now(ZoneOffset.UTC));
    record.setDeleted(false);
    return record;
  }
}
