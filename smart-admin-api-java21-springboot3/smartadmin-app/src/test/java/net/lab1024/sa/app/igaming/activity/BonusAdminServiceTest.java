package net.lab1024.sa.app.igaming.activity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.igaming.activity.dao.PlayerBonusRecordDao;
import net.lab1024.sa.igaming.activity.domain.entity.PlayerBonusRecordEntity;
import net.lab1024.sa.igaming.activity.domain.form.BonusForfeitForm;
import net.lab1024.sa.igaming.activity.domain.form.WageringProgressQueryForm;
import net.lab1024.sa.igaming.activity.domain.vo.PlayerBonusRecordVO;
import net.lab1024.sa.igaming.activity.manager.BonusLifecycleManager;
import net.lab1024.sa.igaming.activity.service.BonusAdminService;
import net.lab1024.sa.igaming.common.constant.BonusRecordStatusEnum;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * BonusAdminService unit tests.
 *
 * @author iGaming Team
 * @since 2026-03-01
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BonusAdminService 單元測試")
class BonusAdminServiceTest {

  @Mock private PlayerBonusRecordDao playerBonusRecordDao;
  @Mock private BonusLifecycleManager bonusLifecycleManager;
  @InjectMocks private BonusAdminService bonusAdminService;

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

  @Test
  @DisplayName("queryBonusRecords — 成功返回分頁結果")
  void queryBonusRecords_success() {
    PlayerBonusRecordVO vo = new PlayerBonusRecordVO();
    vo.setRecordId(1L);
    vo.setBonusAmount(new BigDecimal("100"));
    when(playerBonusRecordDao.queryPage(any(), any())).thenReturn(List.of(vo));

    WageringProgressQueryForm form = new WageringProgressQueryForm();
    form.setPageNum(1L);
    form.setPageSize(10L);

    ResponseDTO<PageResult<PlayerBonusRecordVO>> result = bonusAdminService.queryBonusRecords(form);

    assertThat(result.getOk()).isTrue();
  }

  @Test
  @DisplayName("queryBonusRecords — Tenant 為空返回錯誤")
  void queryBonusRecords_tenantNull() {
    tenantContextMock.when(TenantContext::getTenantId).thenReturn(null);

    WageringProgressQueryForm form = new WageringProgressQueryForm();
    form.setPageNum(1L);
    form.setPageSize(10L);

    ResponseDTO<PageResult<PlayerBonusRecordVO>> result = bonusAdminService.queryBonusRecords(form);

    assertThat(result.getOk()).isFalse();
  }

  @Test
  @DisplayName("getBonusDetail — 成功返回獎金詳情")
  void getBonusDetail_success() {
    PlayerBonusRecordEntity entity = new PlayerBonusRecordEntity();
    entity.setRecordId(1L);
    entity.setPlayerId(200L);
    entity.setBonusAmount(new BigDecimal("50"));
    entity.setStatus(BonusRecordStatusEnum.ACTIVE.getValue());
    when(playerBonusRecordDao.selectById(1L)).thenReturn(entity);

    ResponseDTO<PlayerBonusRecordVO> result = bonusAdminService.getBonusDetail(1L);

    assertThat(result.getOk()).isTrue();
    assertThat(result.getData()).isNotNull();
    assertThat(result.getData().getRecordId()).isEqualTo(1L);
  }

  @Test
  @DisplayName("getBonusDetail — 不存在返回錯誤")
  void getBonusDetail_notFound() {
    when(playerBonusRecordDao.selectById(999L)).thenReturn(null);

    ResponseDTO<PlayerBonusRecordVO> result = bonusAdminService.getBonusDetail(999L);

    assertThat(result.getOk()).isFalse();
  }

  @Test
  @DisplayName("forfeitBonus — 成功沒收獎金")
  void forfeitBonus_success() {
    PlayerBonusRecordEntity entity = new PlayerBonusRecordEntity();
    entity.setRecordId(1L);
    entity.setStatus(BonusRecordStatusEnum.ACTIVE.getValue());
    when(playerBonusRecordDao.selectById(1L)).thenReturn(entity);

    BonusForfeitForm form = new BonusForfeitForm();
    form.setRecordId(1L);
    form.setReason("Fraud detected");

    ResponseDTO<String> result = bonusAdminService.forfeitBonus(form);

    assertThat(result.getOk()).isTrue();
    verify(bonusLifecycleManager).forfeitBonus(1L);
  }

  @Test
  @DisplayName("triggerExpireBatch — 成功觸發批次過期")
  void triggerExpireBatch_success() {
    when(bonusLifecycleManager.expireActiveBonuses(100)).thenReturn(5);

    ResponseDTO<Integer> result = bonusAdminService.triggerExpireBatch(100);

    assertThat(result.getOk()).isTrue();
    assertThat(result.getData()).isEqualTo(5);
  }
}
