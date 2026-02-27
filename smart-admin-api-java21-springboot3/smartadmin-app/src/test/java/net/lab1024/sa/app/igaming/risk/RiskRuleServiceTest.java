package net.lab1024.sa.app.igaming.risk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.risk.dao.RiskRuleParamDao;
import net.lab1024.sa.igaming.risk.domain.entity.RiskRuleParamEntity;
import net.lab1024.sa.igaming.risk.domain.form.RiskRuleAddForm;
import net.lab1024.sa.igaming.risk.domain.form.RiskRuleUpdateForm;
import net.lab1024.sa.igaming.risk.domain.vo.RiskRuleVO;
import net.lab1024.sa.igaming.risk.service.RiskRuleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("RiskRuleService 測試")
class RiskRuleServiceTest {

  @Mock private RiskRuleParamDao riskRuleParamDao;
  @InjectMocks private RiskRuleService riskRuleService;

  @Nested
  @DisplayName("getById 測試")
  class GetByIdTest {

    @Test
    @DisplayName("查詢存在的規則 - 返回成功")
    void get_existing_rule() {
      RiskRuleParamEntity entity = new RiskRuleParamEntity();
      entity.setRuleParamId(1L);
      entity.setRuleName("Velocity Check");
      entity.setDeleted(false);
      when(riskRuleParamDao.selectById(1L)).thenReturn(entity);

      ResponseDTO<RiskRuleVO> result = riskRuleService.getById(1L);

      assertThat(result.getOk()).isTrue();
      assertThat(result.getData().getRuleName()).isEqualTo("Velocity Check");
    }

    @Test
    @DisplayName("查詢不存在的規則 - 返回錯誤")
    void get_nonexistent_rule() {
      when(riskRuleParamDao.selectById(999L)).thenReturn(null);

      ResponseDTO<RiskRuleVO> result = riskRuleService.getById(999L);

      assertThat(result.getOk()).isFalse();
    }

    @Test
    @DisplayName("查詢已刪除的規則 - 返回錯誤")
    void get_deleted_rule() {
      RiskRuleParamEntity entity = new RiskRuleParamEntity();
      entity.setRuleParamId(1L);
      entity.setDeleted(true);
      when(riskRuleParamDao.selectById(1L)).thenReturn(entity);

      ResponseDTO<RiskRuleVO> result = riskRuleService.getById(1L);

      assertThat(result.getOk()).isFalse();
    }
  }

  @Nested
  @DisplayName("add 測試")
  class AddTest {

    @Test
    @DisplayName("新增規則 - 預設 enabled=true, deleted=false")
    void add_rule_defaults() {
      RiskRuleAddForm form = new RiskRuleAddForm();
      form.setRuleType(1);
      form.setRuleName("Amount Check");
      form.setWeight(BigDecimal.ONE);

      ResponseDTO<String> result = riskRuleService.add(form);

      assertThat(result.getOk()).isTrue();
      ArgumentCaptor<RiskRuleParamEntity> captor =
          ArgumentCaptor.forClass(RiskRuleParamEntity.class);
      verify(riskRuleParamDao).insert(captor.capture());
      assertThat(captor.getValue().getEnabled()).isTrue();
      assertThat(captor.getValue().getDeleted()).isFalse();
    }
  }

  @Nested
  @DisplayName("update 測試")
  class UpdateTest {

    @Test
    @DisplayName("更新存在的規則 - 部分更新")
    void update_existing_rule() {
      RiskRuleParamEntity entity = new RiskRuleParamEntity();
      entity.setRuleParamId(1L);
      entity.setRuleName("Old Name");
      entity.setDeleted(false);
      when(riskRuleParamDao.selectById(1L)).thenReturn(entity);

      RiskRuleUpdateForm form = new RiskRuleUpdateForm();
      form.setRuleParamId(1L);
      form.setRuleName("New Name");

      ResponseDTO<String> result = riskRuleService.update(form);

      assertThat(result.getOk()).isTrue();
      verify(riskRuleParamDao).updateById(entity);
      assertThat(entity.getRuleName()).isEqualTo("New Name");
    }

    @Test
    @DisplayName("更新不存在的規則 - 返回錯誤")
    void update_nonexistent_rule() {
      when(riskRuleParamDao.selectById(999L)).thenReturn(null);

      RiskRuleUpdateForm form = new RiskRuleUpdateForm();
      form.setRuleParamId(999L);

      ResponseDTO<String> result = riskRuleService.update(form);

      assertThat(result.getOk()).isFalse();
    }
  }

  @Nested
  @DisplayName("delete 測試")
  class DeleteTest {

    @Test
    @DisplayName("軟刪除存在的規則")
    void delete_existing_rule() {
      RiskRuleParamEntity entity = new RiskRuleParamEntity();
      entity.setRuleParamId(1L);
      entity.setDeleted(false);
      when(riskRuleParamDao.selectById(1L)).thenReturn(entity);

      ResponseDTO<String> result = riskRuleService.delete(1L);

      assertThat(result.getOk()).isTrue();
      assertThat(entity.getDeleted()).isTrue();
      verify(riskRuleParamDao).updateById(entity);
    }

    @Test
    @DisplayName("刪除不存在的規則 - 返回錯誤")
    void delete_nonexistent_rule() {
      when(riskRuleParamDao.selectById(999L)).thenReturn(null);

      ResponseDTO<String> result = riskRuleService.delete(999L);

      assertThat(result.getOk()).isFalse();
    }
  }
}
