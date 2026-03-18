package net.lab1024.sa.igaming.activity.turnover.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.control.Option;
import java.math.BigDecimal;
import net.lab1024.sa.igaming.activity.turnover.dao.TurnoverRiskActionRuleDao;
import net.lab1024.sa.igaming.activity.turnover.dao.TurnoverRuleChangeLogDao;
import net.lab1024.sa.igaming.activity.turnover.domain.entity.TurnoverRiskActionRuleEntity;
import net.lab1024.sa.igaming.activity.turnover.domain.entity.TurnoverRuleChangeLogEntity;
import net.lab1024.sa.igaming.activity.turnover.service.TurnoverTestFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

/**
 * Unit tests for {@link TurnoverRiskActionRuleManager}.
 *
 * @author iGaming Team
 * @since 2026-03-15
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TurnoverRiskActionRuleManager 單元測試")
class TurnoverRiskActionRuleManagerTest {

  @Mock private TurnoverRiskActionRuleDao riskActionRuleDao;
  @Mock private TurnoverRuleChangeLogDao changeLogDao;
  @Mock private ObjectMapper objectMapper;

  private TurnoverRiskActionRuleManager manager;

  @BeforeEach
  void setUp() {
    manager = new TurnoverRiskActionRuleManager(riskActionRuleDao, changeLogDao, objectMapper);
  }

  // ===== getActiveRule() 測試 =====

  @Nested
  @DisplayName("getActiveRule() 測試")
  class GetActiveRuleTests {

    @Test
    @DisplayName("當規則存在時應該返回Option.some")
    void whenRuleExists_ShouldReturnSome() {
      // Arrange
      TurnoverRiskActionRuleEntity entity =
          TurnoverTestFixture.createRiskActionRule(1, 1, new BigDecimal("100.00"));
      when(riskActionRuleDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(entity);

      // Act
      Option<TurnoverRiskActionRuleEntity> result = manager.getActiveRule(1L, 1);

      // Assert
      assertThat(result.isDefined()).isTrue();
      assertThat(result.get().getRiskLevel()).isEqualTo(1);
      assertThat(result.get().getActionType()).isEqualTo(1);
    }

    @Test
    @DisplayName("當規則不存在時應該返回Option.none")
    void whenRuleNotFound_ShouldReturnNone() {
      // Arrange
      when(riskActionRuleDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

      // Act
      Option<TurnoverRiskActionRuleEntity> result = manager.getActiveRule(1L, 999);

      // Assert
      assertThat(result.isEmpty()).isTrue();
    }
  }

  // ===== addRule() 測試 =====

  @Nested
  @DisplayName("addRule() 測試")
  class AddRuleTests {

    @Test
    @DisplayName("應該成功新增規則並記錄變更日誌")
    void shouldAddRuleSuccessfully() throws JsonProcessingException {
      // Arrange
      TurnoverRiskActionRuleEntity entity =
          TurnoverTestFixture.createRiskActionRule(1, 1, new BigDecimal("100.00"));
      when(riskActionRuleDao.insert(any(TurnoverRiskActionRuleEntity.class))).thenReturn(1);
      when(objectMapper.writeValueAsString(any())).thenReturn("{\"ruleId\":1}");

      // Act
      manager.addRule(entity, 1001L, "Admin", "Initial setup");

      // Assert
      verify(riskActionRuleDao, times(1)).insert(any(TurnoverRiskActionRuleEntity.class));
      assertThat(entity.getDeleted()).isFalse();

      // Verify change log
      ArgumentCaptor<TurnoverRuleChangeLogEntity> captor =
          ArgumentCaptor.forClass(TurnoverRuleChangeLogEntity.class);
      verify(changeLogDao, times(1)).insert(captor.capture());

      TurnoverRuleChangeLogEntity changeLog = captor.getValue();
      assertThat(changeLog.getRuleType()).isEqualTo(4); // RiskAction
      assertThat(changeLog.getOperationType()).isEqualTo(1); // INSERT
      assertThat(changeLog.getOldValue()).isNull();
      assertThat(changeLog.getNewValue()).isEqualTo("{\"ruleId\":1}");
    }
  }

  // ===== updateRule() 測試 =====

  @Nested
  @DisplayName("updateRule() 測試")
  class UpdateRuleTests {

    @Test
    @DisplayName("應該成功更新規則並記錄變更日誌")
    void shouldUpdateRuleSuccessfully() throws JsonProcessingException {
      // Arrange
      TurnoverRiskActionRuleEntity oldEntity =
          TurnoverTestFixture.createRiskActionRule(1, 1, new BigDecimal("100.00"));
      oldEntity.setRuleId(1L);
      oldEntity.setDeleted(false);

      TurnoverRiskActionRuleEntity newEntity =
          TurnoverTestFixture.createRiskActionRule(1, 2, new BigDecimal("50.00"));
      newEntity.setRuleId(1L);

      when(riskActionRuleDao.selectById(1L)).thenReturn(oldEntity);
      when(riskActionRuleDao.updateById(any(TurnoverRiskActionRuleEntity.class))).thenReturn(1);
      when(objectMapper.writeValueAsString(oldEntity)).thenReturn("{\"action\":1}");
      when(objectMapper.writeValueAsString(newEntity)).thenReturn("{\"action\":2}");

      // Act
      manager.updateRule(newEntity, 1001L, "Admin", "Action adjustment");

      // Assert
      verify(riskActionRuleDao, times(1)).updateById(any(TurnoverRiskActionRuleEntity.class));

      ArgumentCaptor<TurnoverRuleChangeLogEntity> captor =
          ArgumentCaptor.forClass(TurnoverRuleChangeLogEntity.class);
      verify(changeLogDao, times(1)).insert(captor.capture());

      TurnoverRuleChangeLogEntity changeLog = captor.getValue();
      assertThat(changeLog.getOperationType()).isEqualTo(2); // UPDATE
      assertThat(changeLog.getOldValue()).isEqualTo("{\"action\":1}");
      assertThat(changeLog.getNewValue()).isEqualTo("{\"action\":2}");
    }

    @Test
    @DisplayName("當規則不存在時應該拋出異常")
    void whenRuleNotFound_ShouldThrowException() {
      // Arrange
      TurnoverRiskActionRuleEntity entity =
          TurnoverTestFixture.createRiskActionRule(1, 1, new BigDecimal("100.00"));
      entity.setRuleId(999L);
      when(riskActionRuleDao.selectById(999L)).thenReturn(null);

      // Act & Assert
      assertThatThrownBy(() -> manager.updateRule(entity, 1001L, "Admin", "Update"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Rule not found");

      verify(riskActionRuleDao, never()).updateById(any(TurnoverRiskActionRuleEntity.class));
    }

    @Test
    @DisplayName("當版本衝突時應該拋出樂觀鎖異常")
    void whenVersionConflict_ShouldThrowOptimisticLockException() {
      // Arrange
      TurnoverRiskActionRuleEntity oldEntity =
          TurnoverTestFixture.createRiskActionRule(1, 1, new BigDecimal("100.00"));
      oldEntity.setRuleId(1L);
      oldEntity.setDeleted(false);

      TurnoverRiskActionRuleEntity newEntity =
          TurnoverTestFixture.createRiskActionRule(1, 2, new BigDecimal("50.00"));
      newEntity.setRuleId(1L);

      when(riskActionRuleDao.selectById(1L)).thenReturn(oldEntity);
      when(riskActionRuleDao.updateById(any(TurnoverRiskActionRuleEntity.class))).thenReturn(0);

      // Act & Assert
      assertThatThrownBy(() -> manager.updateRule(newEntity, 1001L, "Admin", "Update"))
          .isInstanceOf(OptimisticLockingFailureException.class)
          .hasMessageContaining("Version conflict");
    }
  }

  // ===== deleteRule() 測試 =====

  @Nested
  @DisplayName("deleteRule() 測試")
  class DeleteRuleTests {

    @Test
    @DisplayName("應該成功軟刪除規則並記錄變更日誌")
    void shouldDeleteRuleSuccessfully() throws JsonProcessingException {
      // Arrange
      TurnoverRiskActionRuleEntity entity =
          TurnoverTestFixture.createRiskActionRule(1, 1, new BigDecimal("100.00"));
      entity.setRuleId(1L);
      entity.setDeleted(false);

      when(riskActionRuleDao.selectById(1L)).thenReturn(entity);
      when(riskActionRuleDao.updateById(any(TurnoverRiskActionRuleEntity.class))).thenReturn(1);
      when(objectMapper.writeValueAsString(any())).thenReturn("{\"ruleId\":1}");

      // Act
      manager.deleteRule(1L, 1001L, "Admin", "No longer needed");

      // Assert
      verify(riskActionRuleDao, times(1)).updateById(any(TurnoverRiskActionRuleEntity.class));
      assertThat(entity.getDeleted()).isTrue();

      ArgumentCaptor<TurnoverRuleChangeLogEntity> captor =
          ArgumentCaptor.forClass(TurnoverRuleChangeLogEntity.class);
      verify(changeLogDao, times(1)).insert(captor.capture());

      TurnoverRuleChangeLogEntity changeLog = captor.getValue();
      assertThat(changeLog.getOperationType()).isEqualTo(3); // DELETE
    }

    @Test
    @DisplayName("當規則已刪除時應該拋出異常")
    void whenRuleAlreadyDeleted_ShouldThrowException() {
      // Arrange
      TurnoverRiskActionRuleEntity entity =
          TurnoverTestFixture.createRiskActionRule(1, 1, new BigDecimal("100.00"));
      entity.setRuleId(1L);
      entity.setDeleted(true);

      when(riskActionRuleDao.selectById(1L)).thenReturn(entity);

      // Act & Assert
      assertThatThrownBy(() -> manager.deleteRule(1L, 1001L, "Admin", "Delete"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("already deleted");
    }
  }

  // ===== evictCache() 測試 =====

  @Test
  @DisplayName("evictCache 應該執行無異常")
  void evictCacheShouldExecuteWithoutException() {
    // Act & Assert
    manager.evictCache(1L);
  }
}
