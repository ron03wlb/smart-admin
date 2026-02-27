package net.lab1024.sa.app.igaming.activity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.vavr.control.Option;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.activity.dao.PromotionRuleDao;
import net.lab1024.sa.igaming.activity.domain.entity.PromotionRuleEntity;
import net.lab1024.sa.igaming.activity.domain.form.PromotionRuleAddForm;
import net.lab1024.sa.igaming.activity.domain.form.PromotionRuleQueryForm;
import net.lab1024.sa.igaming.activity.domain.form.PromotionRuleUpdateForm;
import net.lab1024.sa.igaming.activity.domain.vo.PromotionRuleVO;
import net.lab1024.sa.igaming.activity.manager.PromotionCacheManager;
import net.lab1024.sa.igaming.activity.service.PromotionRuleService;
import net.lab1024.sa.igaming.common.code.ActivityErrorCode;
import net.lab1024.sa.igaming.common.constant.PromotionStatusEnum;
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
 * PromotionRuleService unit tests.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PromotionRuleService 單元測試")
class PromotionRuleServiceTest {

  @Mock private PromotionRuleDao promotionRuleDao;
  @Mock private PromotionCacheManager promotionCacheManager;
  @InjectMocks private PromotionRuleService promotionRuleService;

  @Nested
  @DisplayName("getRule")
  class GetRuleTest {

    @Test
    @DisplayName("存在 — 返回 Option.some")
    void getRule_found() {
      PromotionRuleEntity entity = buildEntity();
      when(promotionRuleDao.selectById(1L)).thenReturn(entity);

      Option<PromotionRuleVO> result = promotionRuleService.getRule(1L);

      assertThat(result.isDefined()).isTrue();
      assertThat(result.get().getRuleId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("不存在 — 返回 Option.none")
    void getRule_notFound() {
      when(promotionRuleDao.selectById(1L)).thenReturn(null);

      Option<PromotionRuleVO> result = promotionRuleService.getRule(1L);

      assertThat(result.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("已刪除 — 返回 Option.none")
    void getRule_deleted() {
      PromotionRuleEntity entity = buildEntity();
      entity.setDeleted(true);
      when(promotionRuleDao.selectById(1L)).thenReturn(entity);

      Option<PromotionRuleVO> result = promotionRuleService.getRule(1L);

      assertThat(result.isEmpty()).isTrue();
    }
  }

  @Nested
  @DisplayName("queryRules")
  class QueryRulesTest {

    @Test
    @DisplayName("分頁查詢成功")
    void queryRules_success() {
      PromotionRuleQueryForm form = new PromotionRuleQueryForm();
      form.setPageNum(1L);
      form.setPageSize(10L);
      when(promotionRuleDao.queryPage(any(Page.class), any(PromotionRuleQueryForm.class)))
          .thenReturn(List.of());

      ResponseDTO<?> result = promotionRuleService.queryRules(form);

      assertThat(result.getOk()).isTrue();
    }
  }

  @Nested
  @DisplayName("addRule")
  class AddRuleTest {

    @Test
    @DisplayName("成功新增")
    void addRule_success() {
      try (MockedStatic<net.lab1024.sa.common.core.tenant.TenantContext> tc =
          Mockito.mockStatic(net.lab1024.sa.common.core.tenant.TenantContext.class)) {
        tc.when(net.lab1024.sa.common.core.tenant.TenantContext::getTenantId).thenReturn(1L);

        PromotionRuleAddForm form = buildAddForm();
        ResponseDTO<PromotionRuleVO> result = promotionRuleService.addRule(form);

        assertThat(result.getOk()).isTrue();
        verify(promotionRuleDao).insert(any(PromotionRuleEntity.class));
        verify(promotionCacheManager).evictCache(1L);
      }
    }

    @Test
    @DisplayName("代碼已存在 — DuplicateKeyException")
    void addRule_codeExists() {
      try (MockedStatic<net.lab1024.sa.common.core.tenant.TenantContext> tc =
          Mockito.mockStatic(net.lab1024.sa.common.core.tenant.TenantContext.class)) {
        tc.when(net.lab1024.sa.common.core.tenant.TenantContext::getTenantId).thenReturn(1L);
        when(promotionRuleDao.insert(any(PromotionRuleEntity.class)))
            .thenThrow(new DuplicateKeyException("Duplicate key"));

        PromotionRuleAddForm form = buildAddForm();
        ResponseDTO<PromotionRuleVO> result = promotionRuleService.addRule(form);

        assertThat(result.getOk()).isFalse();
        assertThat(result.getMsg()).contains(ActivityErrorCode.PROMOTION_CODE_EXISTS.getMsg());
      }
    }
  }

  @Nested
  @DisplayName("updateRule")
  class UpdateRuleTest {

    @Test
    @DisplayName("成功更新")
    void updateRule_success() {
      PromotionRuleEntity entity = buildEntity();
      when(promotionRuleDao.selectById(1L)).thenReturn(entity);

      PromotionRuleUpdateForm form = new PromotionRuleUpdateForm();
      form.setRuleId(1L);
      form.setPromotionCode("FIRST100");
      form.setPromotionName("Updated Name");
      form.setPromotionType(1);
      form.setStartTime(OffsetDateTime.now(ZoneOffset.UTC));
      form.setEndTime(OffsetDateTime.now(ZoneOffset.UTC).plusDays(30));

      ResponseDTO<Void> result = promotionRuleService.updateRule(form);

      assertThat(result.getOk()).isTrue();
      verify(promotionRuleDao).updateById(entity);
    }

    @Test
    @DisplayName("規則不存在 — 失敗")
    void updateRule_notFound() {
      when(promotionRuleDao.selectById(1L)).thenReturn(null);

      PromotionRuleUpdateForm form = new PromotionRuleUpdateForm();
      form.setRuleId(1L);
      form.setPromotionCode("FIRST100");
      form.setPromotionName("test");
      form.setPromotionType(1);
      form.setStartTime(OffsetDateTime.now(ZoneOffset.UTC));
      form.setEndTime(OffsetDateTime.now(ZoneOffset.UTC).plusDays(30));

      ResponseDTO<Void> result = promotionRuleService.updateRule(form);

      assertThat(result.getOk()).isFalse();
    }
  }

  @Nested
  @DisplayName("toggleStatus")
  class ToggleStatusTest {

    @Test
    @DisplayName("成功切換狀態")
    void toggleStatus_success() {
      PromotionRuleEntity entity = buildEntity();
      when(promotionRuleDao.selectById(1L)).thenReturn(entity);

      ResponseDTO<Void> result =
          promotionRuleService.toggleStatus(1L, PromotionStatusEnum.PAUSED.getValue());

      assertThat(result.getOk()).isTrue();
      verify(promotionRuleDao).updateById(entity);
    }

    @Test
    @DisplayName("無效狀態值 — 失敗")
    void toggleStatus_invalidStatus() {
      ResponseDTO<Void> result = promotionRuleService.toggleStatus(1L, 999);

      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("Invalid promotion status");
    }

    @Test
    @DisplayName("規則不存在 — 失敗")
    void toggleStatus_notFound() {
      when(promotionRuleDao.selectById(1L)).thenReturn(null);

      ResponseDTO<Void> result =
          promotionRuleService.toggleStatus(1L, PromotionStatusEnum.PAUSED.getValue());

      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains(ActivityErrorCode.PROMOTION_NOT_FOUND.getMsg());
    }

    @Test
    @DisplayName("規則已刪除 — 失敗")
    void toggleStatus_deleted() {
      PromotionRuleEntity entity = buildEntity();
      entity.setDeleted(true);
      when(promotionRuleDao.selectById(1L)).thenReturn(entity);

      ResponseDTO<Void> result =
          promotionRuleService.toggleStatus(1L, PromotionStatusEnum.PAUSED.getValue());

      assertThat(result.getOk()).isFalse();
    }
  }

  private PromotionRuleEntity buildEntity() {
    PromotionRuleEntity entity = new PromotionRuleEntity();
    entity.setRuleId(1L);
    entity.setTenantId(1L);
    entity.setPromotionCode("FIRST100");
    entity.setPromotionName("First Deposit 100%");
    entity.setPromotionType(1);
    entity.setStatus(PromotionStatusEnum.ACTIVE.getValue());
    entity.setStartTime(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));
    entity.setEndTime(OffsetDateTime.now(ZoneOffset.UTC).plusDays(30));
    entity.setMaxBonus(new BigDecimal("100.00"));
    entity.setWageringMultiplier(new BigDecimal("20.00"));
    entity.setMaxClaimsPerPlayer(1);
    entity.setBonusExpiryDays(30);
    entity.setDeleted(false);
    return entity;
  }

  private PromotionRuleAddForm buildAddForm() {
    PromotionRuleAddForm form = new PromotionRuleAddForm();
    form.setPromotionCode("FIRST100");
    form.setPromotionName("First Deposit 100%");
    form.setPromotionType(1);
    form.setStartTime(OffsetDateTime.now(ZoneOffset.UTC));
    form.setEndTime(OffsetDateTime.now(ZoneOffset.UTC).plusDays(30));
    form.setMaxBonus(new BigDecimal("100.00"));
    form.setWageringMultiplier(new BigDecimal("20.00"));
    return form;
  }
}
